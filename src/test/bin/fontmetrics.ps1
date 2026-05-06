Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function Convert-FontNameForProperty([string]$Name) {
	return $Name.Replace(" ", "\u0020")
}

function Write-FontEmHeight([System.Drawing.FontFamily]$FontFamily) {
	if (-not $FontFamily.IsStyleAvailable([System.Drawing.FontStyle]::Regular)) {
		return
	}

	$emHeight = $FontFamily.GetEmHeight([System.Drawing.FontStyle]::Regular)
	$ascent = $FontFamily.GetCellAscent([System.Drawing.FontStyle]::Regular)
	$descent = $FontFamily.GetCellDescent([System.Drawing.FontStyle]::Regular)
	$cellHeight = $ascent + $descent

	if ($cellHeight -ne 0 -and $emHeight -ne $cellHeight) {
		$name = Convert-FontNameForProperty $FontFamily.Name
		$ratio = $emHeight / [double]$cellHeight
		Write-Output ("font-emheight.{0} =  {1}" -f $name, $ratio)
	}
}

$fontCollection = $null

try {
	$fontCollection = [System.Drawing.Text.InstalledFontCollection]::new()
	foreach ($fontFamily in $fontCollection.Families) {
		Write-FontEmHeight $fontFamily
	}
} finally {
	if ($fontCollection -ne $null) {
		$fontCollection.Dispose()
	}
}
