param(
	[Parameter(Mandatory = $true)]
	[string]$InputPath
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$bitmap = [System.Drawing.Bitmap]::new($InputPath)
try {
	Write-Host ("loaded {0}x{1}" -f $bitmap.Width, $bitmap.Height)
} finally {
	$bitmap.Dispose()
}
