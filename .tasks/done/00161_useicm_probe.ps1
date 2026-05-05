param(
	[Parameter(Mandatory = $true)]
	[string]$InputPath,

	[Parameter(Mandatory = $true)]
	[string]$OutputPath,

	[Parameter(Mandatory = $true)]
	[int]$UseIcm
	,

	[Parameter()]
	[ValidateSet("Bitmap", "Image")]
	[string]$LoadKind = "Bitmap"
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

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

Add-Type -AssemblyName System.Drawing

$image = $null
$bitmap = $null
$graphics = $null

try {
	$hasPlaceableHeader = Test-WmfPlaceableHeader $InputPath
	$hasMapMode = -not $hasPlaceableHeader -and (Test-WmfHasMapMode $InputPath)
	$useIcmBool = $UseIcm -ne 0
	if ($LoadKind -eq "Image") {
		$image = [System.Drawing.Image]::FromFile($InputPath, $useIcmBool)
	} else {
		$image = New-Object System.Drawing.Bitmap($InputPath, $useIcmBool)
	}

	$canvasSize = Get-WmfCanvasSize $InputPath $image.Width $image.Height
	if ($canvasSize -ne $null) {
		$width = $canvasSize.Width
		$height = $canvasSize.Height
	} else {
		$width = [Math]::Max(1, $image.Width)
		$height = [Math]::Max(1, $image.Height)
	}

	$bitmap = New-Object System.Drawing.Bitmap($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
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
	$graphics.Clear([System.Drawing.Color]::Transparent)

	if ($hasMapMode) {
		$destination = New-Object System.Drawing.Rectangle(0, 0, $width, $height)
		$graphics.DrawImage($image, $destination, 0, 0, $image.Width, $image.Height, [System.Drawing.GraphicsUnit]::Pixel)
	} else {
		$graphics.DrawImage($image, 0, 0, $width, $height)
	}

	$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
	Write-Host ("loadKind={0} useIcm={1} {2}x{3} source={4}x{5} mapMode={6}" -f $LoadKind, $useIcmBool, $width, $height, $image.Width, $image.Height, $hasMapMode)
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
