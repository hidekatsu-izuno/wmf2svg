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
	$image = [System.Drawing.Bitmap]::new($source)

	$Width = [Math]::Max(1, $image.Width)
	$Height = [Math]::Max(1, $image.Height)

	$bitmap = New-Object System.Drawing.Bitmap($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
	$bitmap.SetResolution(96, 96)

	$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
	$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
	$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
	$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
	$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

	$graphics.Clear([System.Drawing.Color]::Transparent)

	$destination = New-Object System.Drawing.Rectangle(0, 0, $Width, $Height)
	$graphics.DrawImage($image, $destination, 0, 0, $image.Width, $image.Height, [System.Drawing.GraphicsUnit]::Pixel)
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
