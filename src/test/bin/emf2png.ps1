param(
	[Parameter(Mandatory = $true, Position = 0)]
	[string]$InputPath,

	[Parameter(Position = 1)]
	[string]$OutputPath,

	[int]$Width = 0,
	[int]$Height = 0,
	[int]$Dpi = 96,
	[switch]$Transparent
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

try {
	$image = [System.Drawing.Image]::FromFile($source)

	if ($Width -le 0 -and $Height -le 0) {
		$Width = [Math]::Max(1, [int][Math]::Ceiling($image.Width))
		$Height = [Math]::Max(1, [int][Math]::Ceiling($image.Height))
	} elseif ($Width -le 0) {
		$Width = [Math]::Max(1, [int][Math]::Round($Height * $image.Width / $image.Height))
	} elseif ($Height -le 0) {
		$Height = [Math]::Max(1, [int][Math]::Round($Width * $image.Height / $image.Width))
	}

	$bitmap = New-Object System.Drawing.Bitmap($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
	$bitmap.SetResolution($Dpi, $Dpi)

	$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
	$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
	$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
	$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
	$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

	if ($Transparent) {
		$graphics.Clear([System.Drawing.Color]::Transparent)
	} else {
		$graphics.Clear([System.Drawing.Color]::White)
	}

	$graphics.DrawImage($image, 0, 0, $Width, $Height)
	$bitmap.Save($outputName, [System.Drawing.Imaging.ImageFormat]::Png)

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
