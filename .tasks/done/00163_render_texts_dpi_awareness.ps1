param(
	[Parameter(Mandatory = $true)]
	[string]$InputPath,

	[Parameter(Mandatory = $true)]
	[string]$OutputPath,

	[Parameter(Mandatory = $true)]
	[ValidateSet("Default", "Unaware", "SystemAware", "PerMonitorAware")]
	[string]$DpiAwareness
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class DpiAwarenessSetter {
	[DllImport("shcore.dll")]
	public static extern Int32 SetProcessDpiAwareness(Int32 value);

	[DllImport("shcore.dll")]
	public static extern Int32 GetProcessDpiAwareness(IntPtr hprocess, out Int32 value);
}
"@

if ($DpiAwareness -ne "Default") {
	$value = switch ($DpiAwareness) {
		"Unaware" { 0 }
		"SystemAware" { 1 }
		"PerMonitorAware" { 2 }
	}
	$result = [DpiAwarenessSetter]::SetProcessDpiAwareness($value)
	Write-Host ("SetProcessDpiAwareness({0})={1}" -f $value, $result)
}

$current = -1
[void][DpiAwarenessSetter]::GetProcessDpiAwareness([IntPtr]::Zero, [ref]$current)
Write-Host ("ProcessDpiAwareness={0}" -f $current)

Add-Type -AssemblyName System.Drawing

$image = $null
$bitmap = $null
$graphics = $null
try {
	$image = [System.Drawing.Bitmap]::new($InputPath)
	$bitmap = New-Object System.Drawing.Bitmap($image.Width, $image.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
	$bitmap.SetResolution(96, 96)
	$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
	$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
	$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
	$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
	$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
	$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
	$graphics.Clear([System.Drawing.Color]::Transparent)
	$destination = New-Object System.Drawing.Rectangle(0, 0, $image.Width, $image.Height)
	$graphics.DrawImage($image, $destination, 0, 0, $image.Width, $image.Height, [System.Drawing.GraphicsUnit]::Pixel)
	$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
	Write-Host ("saved {0}x{1}" -f $image.Width, $image.Height)
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
