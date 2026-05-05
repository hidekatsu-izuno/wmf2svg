param(
	[Parameter(Mandatory = $true)]
	[string]$InputPath,

	[Parameter(Mandatory = $true)]
	[string]$OutputPath,

	[Parameter(Mandatory = $true)]
	[string]$Mode
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

$image = $null
$bitmap = $null
$graphics = $null
try {
	if ($Mode -eq "bitmap-rect") {
		$image = [System.Drawing.Bitmap]::new($InputPath)
	} else {
		$image = [System.Drawing.Image]::FromFile($InputPath)
	}
	$canvas = Get-WmfCanvasSize $InputPath $image.Width $image.Height
	$bitmap = [System.Drawing.Bitmap]::new($canvas.Width, $canvas.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
	$bitmap.SetResolution(96, 96)
	$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
	$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
	$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
	$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
	$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
	$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
	$graphics.Clear([System.Drawing.Color]::Transparent)
	if ($Mode -eq "image-direct") {
		$graphics.DrawImage($image, 0, 0, $canvas.Width, $canvas.Height)
	} elseif ($Mode -eq "image-canvas-source") {
		$destination = [System.Drawing.Rectangle]::new(0, 0, $canvas.Width, $canvas.Height)
		$graphics.DrawImage($image, $destination, 0, 0, $canvas.Width, $canvas.Height, [System.Drawing.GraphicsUnit]::Pixel)
	} elseif ($Mode -eq "bitmap-rect") {
		$destination = [System.Drawing.Rectangle]::new(0, 0, $canvas.Width, $canvas.Height)
		$graphics.DrawImage($image, $destination, 0, 0, $image.Width, $image.Height, [System.Drawing.GraphicsUnit]::Pixel)
	} else {
		throw "Unknown mode: $Mode"
	}
	$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
	if ($graphics -ne $null) { $graphics.Dispose() }
	if ($bitmap -ne $null) { $bitmap.Dispose() }
	if ($image -ne $null) { $image.Dispose() }
}
