param(
	[Parameter(Mandatory = $true, Position = 0)]
	[string]$InputPath,

	[Parameter(Position = 1)]
	[string]$OutputPath
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

function Convert-PathForWindows([string]$Path, [switch]$ForOutput) {
	if ([string]::IsNullOrWhiteSpace($Path)) {
		return $Path
	}

	if ($Path -match "^[A-Za-z]:\\" -or $Path -match "^\\\\") {
		return $Path
	}

	if (Test-Path -LiteralPath $Path) {
		return (Convert-Path -LiteralPath $Path)
	}

	if ($Path.StartsWith("/")) {
		$converted = (& wsl.exe wslpath -w -- "$Path" 2>$null)
		if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($converted)) {
			return $converted.Trim()
		}
	}

	if ($ForOutput) {
		$parent = Split-Path -Parent $Path
		$name = Split-Path -Leaf $Path
		if (-not [string]::IsNullOrWhiteSpace($parent) -and (Test-Path -LiteralPath $parent)) {
			return (Join-Path (Convert-Path -LiteralPath $parent) $name)
		}
	}

	return $Path
}

function Read-UInt16LE([byte[]]$Bytes, [int]$Offset) {
	return [System.BitConverter]::ToUInt16($Bytes, $Offset)
}

function Read-Int16LE([byte[]]$Bytes, [int]$Offset) {
	return [System.BitConverter]::ToInt16($Bytes, $Offset)
}

function Read-UInt32LE([byte[]]$Bytes, [int]$Offset) {
	return [System.BitConverter]::ToUInt32($Bytes, $Offset)
}

function Test-WmfPlaceableHeader([string]$Path) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	return $bytes.Length -ge 22 -and (Read-UInt32LE $bytes 0) -eq [uint32]2596720087
}

function Test-EmfHeader([string]$Path) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	return $bytes.Length -ge 44 -and
		(Read-UInt32LE $bytes 0) -eq [uint32]1 -and
		(Read-UInt32LE $bytes 40) -eq [uint32]1179469088
}

function Test-WmfHasMapMode([string]$Path) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	if ($bytes.Length -lt 18) {
		return $false
	}

	$pos = 18
	while ($pos + 6 -le $bytes.Length) {
		$recordSizeWords = Read-UInt32LE $bytes $pos
		$recordSize = [int]($recordSizeWords * 2)
		if ($recordSize -lt 6 -or $pos + $recordSize -gt $bytes.Length) {
			break
		}

		$function = Read-UInt16LE $bytes ($pos + 4)
		if ($function -eq 0x0103) {
			return $true
		}

		$pos += $recordSize
	}

	return $false
}

function Get-WmfCanvasSize([string]$Path, [int]$GdiWidth, [int]$GdiHeight) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	if ($bytes.Length -lt 18) {
		return $null
	}

	if ($bytes.Length -ge 22 -and (Read-UInt32LE $bytes 0) -eq [uint32]2596720087) {
		$left = Read-Int16LE $bytes 6
		$top = Read-Int16LE $bytes 8
		$right = Read-Int16LE $bytes 10
		$bottom = Read-Int16LE $bytes 12
		$inch = [Math]::Max(1, [int](Read-UInt16LE $bytes 14))
		$widthUnits = [Math]::Max(1, [int]$right - [int]$left)
		$heightUnits = [Math]::Max(1, [int]$bottom - [int]$top)
		$roundingBias = if ($left -ge 0 -and $top -ge 0) { 0.5 } else { 0.499999 }
		return [pscustomobject]@{
			Width = [Math]::Max(1, [int][Math]::Floor(($widthUnits * 144.0 / $inch) + $roundingBias))
			Height = [Math]::Max(1, [int][Math]::Floor(($heightUnits * 144.0 / $inch) + $roundingBias))
		}
	}

	$pos = 18
	$windowWidth = 0
	$windowHeight = 0
	while ($pos + 6 -le $bytes.Length) {
		$recordSizeWords = Read-UInt32LE $bytes $pos
		$recordSize = [int]($recordSizeWords * 2)
		if ($recordSize -lt 6 -or $pos + $recordSize -gt $bytes.Length) {
			break
		}

		$function = Read-UInt16LE $bytes ($pos + 4)
		if ($function -eq 0x020C -and $recordSize -ge 10) {
			$windowHeight = Read-Int16LE $bytes ($pos + 6)
			$windowWidth = Read-Int16LE $bytes ($pos + 8)
		}

		$pos += $recordSize
	}

	$width = [Math]::Abs($windowWidth)
	$height = [Math]::Abs($windowHeight)
	if ($width -gt 0 -and $height -gt 0) {
		if ($GdiWidth -ge $width -and $GdiWidth -le $width + 1 -and
				$GdiHeight -ge $height -and $GdiHeight -le $height + 1) {
			$width = $GdiWidth
			$height = $GdiHeight
		}
		return [pscustomobject]@{
			Width = [Math]::Max(1, $width)
			Height = [Math]::Max(1, $height)
		}
	}

	return $null
}

function Set-PaintLikeDpiAwareness() {
	Add-Type -TypeDefinition @"
using System.Runtime.InteropServices;

public static class Wmf2PngDpiAwareness {
	[DllImport("shcore.dll")]
	public static extern int SetProcessDpiAwareness(int value);
}
"@

	# Paint is per-monitor DPI aware; GDI+ WMF text rasterization can differ if
	# the process stays DPI unaware when GDI+ is initialized.
	$result = [Wmf2PngDpiAwareness]::SetProcessDpiAwareness(2)
	if ($result -ne 0 -and $result -ne -2147024891) {
		throw "SetProcessDpiAwareness failed: $result"
	}
}

function Initialize-PaintLikeGdiplus() {
	Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class Wmf2PngGdiplusStartup {
	[StructLayout(LayoutKind.Sequential)]
	public struct GdiplusStartupInput {
		public uint GdiplusVersion;
		public IntPtr DebugEventCallback;
		public bool SuppressBackgroundThread;
		public bool SuppressExternalCodecs;
	}

	[DllImport("gdiplus.dll", ExactSpelling=true)]
	public static extern int GdiplusStartup(out IntPtr token, ref GdiplusStartupInput input, IntPtr output);
}
"@

	$input = New-Object Wmf2PngGdiplusStartup+GdiplusStartupInput
	$input.GdiplusVersion = 3
	$token = [IntPtr]::Zero
	$result = [Wmf2PngGdiplusStartup]::GdiplusStartup([ref]$token, [ref]$input, [IntPtr]::Zero)
	if ($result -ne 0) {
		throw "GdiplusStartup failed: $result"
	}
}

Set-PaintLikeDpiAwareness
Initialize-PaintLikeGdiplus

Add-Type -AssemblyName System.Drawing

Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;

public static class WmfBitmapHelper {
	public static System.Drawing.Bitmap LoadBitmapFromFile(string path) {
		return new System.Drawing.Bitmap(path);
	}
}
"@

$source = Convert-PathForWindows $InputPath
if (-not (Test-Path -LiteralPath $source)) {
	throw "Input file not found: $InputPath"
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
	$outputName = [System.IO.Path]::ChangeExtension($source, ".png")
} else {
	$outputName = Convert-PathForWindows $OutputPath -ForOutput
}

$outputDir = Split-Path -Parent $outputName
if (-not [string]::IsNullOrWhiteSpace($outputDir) -and -not (Test-Path -LiteralPath $outputDir)) {
	New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$image = $null
$bitmap = $null
$graphics = $null
$pngImage = $null

try {
	$isEmf = Test-EmfHeader $source
	$hasPlaceableHeader = -not $isEmf -and (Test-WmfPlaceableHeader $source)
	$hasMapMode = -not $isEmf -and -not $hasPlaceableHeader -and (Test-WmfHasMapMode $source)
	if ($isEmf) {
		$image = [System.Drawing.Image]::FromFile($source)
	} elseif ($hasMapMode) {
		$image = [WmfBitmapHelper]::LoadBitmapFromFile($source)
	} else {
		$image = [System.Drawing.Image]::FromFile($source)
	}
	$useTransparentBackground = -not $hasPlaceableHeader

	$canvasSize = if ($isEmf) { $null } else { Get-WmfCanvasSize $source $image.Width $image.Height }
	if ($canvasSize -ne $null) {
		$Width = $canvasSize.Width
		$Height = $canvasSize.Height
	} else {
		$Width = [Math]::Max(1, $image.Width)
		$Height = [Math]::Max(1, $image.Height)
	}

	$bitmap = New-Object System.Drawing.Bitmap($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
	$bitmap.SetResolution(96, 96)

	$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
	if (-not $hasPlaceableHeader) {
		$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
	}
	$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
	$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
	if ($hasPlaceableHeader) {
		$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
	} else {
		$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
	}
	$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

	if ($useTransparentBackground) {
		$graphics.Clear([System.Drawing.Color]::Transparent)
	}

	if ($isEmf) {
		$graphics.DrawImage($image, 0, 0, $Width, $Height)
	} elseif ($hasPlaceableHeader) {
		$graphics.DrawImage($image, 0, 0, $Width, $Height)
	} elseif ($hasMapMode) {
		$destination = New-Object System.Drawing.Rectangle(0, 0, $Width, $Height)
		$graphics.DrawImage($image, $destination, 0, 0, $image.Width, $image.Height, [System.Drawing.GraphicsUnit]::Pixel)
	} else {
		$graphics.DrawImage($image, 0, 0, $Width, $Height)
	}
	$pngImage = $bitmap

	$pngImage.Save($outputName, [System.Drawing.Imaging.ImageFormat]::Png)
	Write-Host ("{0} -> {1} ({2}x{3})" -f $source, $outputName, $Width, $Height)
} finally {
	if ($graphics -ne $null) {
		$graphics.Dispose()
	}
	if ($bitmap -ne $null) {
		$bitmap.Dispose()
	}
	if ($image -ne $null) {
		$image.Dispose()
	}
}
