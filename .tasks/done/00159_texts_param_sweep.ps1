param(
	[Parameter(Mandatory = $true)]
	[string]$InputPath,

	[Parameter(Mandatory = $true)]
	[string]$OutputDir
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Read-UInt16LE([byte[]]$Bytes, [int]$Offset) { [System.BitConverter]::ToUInt16($Bytes, $Offset) }
function Read-Int16LE([byte[]]$Bytes, [int]$Offset) { [System.BitConverter]::ToInt16($Bytes, $Offset) }
function Read-UInt32LE([byte[]]$Bytes, [int]$Offset) { [System.BitConverter]::ToUInt32($Bytes, $Offset) }

function Get-WmfCanvasSize([string]$Path, [int]$GdiWidth, [int]$GdiHeight) {
	$bytes = [System.IO.File]::ReadAllBytes($Path)
	$pos = 18
	$windowWidth = 0
	$windowHeight = 0
	while ($pos + 6 -le $bytes.Length) {
		$recordSizeWords = Read-UInt32LE $bytes $pos
		$recordSize = [int]($recordSizeWords * 2)
		if ($recordSize -lt 6 -or $pos + $recordSize -gt $bytes.Length) { break }
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
		if ($GdiWidth -ge $width -and $GdiWidth -le $width + 1 -and $GdiHeight -ge $height -and $GdiHeight -le $height + 1) {
			$width = $GdiWidth
			$height = $GdiHeight
		}
		return [pscustomobject]@{ Width = [Math]::Max(1, $width); Height = [Math]::Max(1, $height) }
	}
	return [pscustomobject]@{ Width = [Math]::Max(1, $GdiWidth); Height = [Math]::Max(1, $GdiHeight) }
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

$smoothingModes = @(
	[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias,
	[System.Drawing.Drawing2D.SmoothingMode]::HighQuality,
	[System.Drawing.Drawing2D.SmoothingMode]::HighSpeed,
	[System.Drawing.Drawing2D.SmoothingMode]::None
)
$pixelModes = @(
	[System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality,
	[System.Drawing.Drawing2D.PixelOffsetMode]::Half,
	[System.Drawing.Drawing2D.PixelOffsetMode]::None,
	[System.Drawing.Drawing2D.PixelOffsetMode]::HighSpeed
)
$interpolationModes = @(
	[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic,
	[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor,
	[System.Drawing.Drawing2D.InterpolationMode]::Bilinear,
	[System.Drawing.Drawing2D.InterpolationMode]::Bicubic,
	[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBilinear
)
$textHints = @(
	[System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit,
	[System.Drawing.Text.TextRenderingHint]::AntiAlias,
	[System.Drawing.Text.TextRenderingHint]::SingleBitPerPixelGridFit,
	[System.Drawing.Text.TextRenderingHint]::SingleBitPerPixel,
	[System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit,
	[System.Drawing.Text.TextRenderingHint]::SystemDefault
)
$compositingQualities = @(
	[System.Drawing.Drawing2D.CompositingQuality]::Default,
	[System.Drawing.Drawing2D.CompositingQuality]::HighQuality,
	[System.Drawing.Drawing2D.CompositingQuality]::HighSpeed,
	[System.Drawing.Drawing2D.CompositingQuality]::GammaCorrected,
	[System.Drawing.Drawing2D.CompositingQuality]::AssumeLinear
)

$source = [System.Drawing.Bitmap]::new($InputPath)
try {
	$canvas = Get-WmfCanvasSize $InputPath $source.Width $source.Height
	$index = 0
	foreach ($smoothing in $smoothingModes) {
		foreach ($pixel in $pixelModes) {
			foreach ($interpolation in $interpolationModes) {
				foreach ($textHint in $textHints) {
					foreach ($quality in $compositingQualities) {
						$index++
						$name = "{0:D4}_s-{1}_p-{2}_i-{3}_t-{4}_q-{5}.png" -f $index, $smoothing, $pixel, $interpolation, $textHint, $quality
						$out = Join-Path $OutputDir $name
						$bitmap = [System.Drawing.Bitmap]::new($canvas.Width, $canvas.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
						$graphics = $null
						try {
							$bitmap.SetResolution(96, 96)
							$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
							$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
							$graphics.CompositingQuality = $quality
							$graphics.SmoothingMode = $smoothing
							$graphics.PixelOffsetMode = $pixel
							$graphics.InterpolationMode = $interpolation
							$graphics.TextRenderingHint = $textHint
							$graphics.Clear([System.Drawing.Color]::Transparent)
							$destination = [System.Drawing.Rectangle]::new(0, 0, $canvas.Width, $canvas.Height)
							$graphics.DrawImage($source, $destination, 0, 0, $source.Width, $source.Height, [System.Drawing.GraphicsUnit]::Pixel)
							$bitmap.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
						} finally {
							if ($graphics -ne $null) { $graphics.Dispose() }
							$bitmap.Dispose()
						}
					}
				}
			}
		}
	}
} finally {
	$source.Dispose()
}
