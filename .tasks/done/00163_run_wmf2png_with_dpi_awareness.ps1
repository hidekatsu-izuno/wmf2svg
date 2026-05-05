param(
	[Parameter(Mandatory = $true, Position = 0)]
	[string]$ScriptPath,

	[Parameter(Mandatory = $true, Position = 1)]
	[string]$InputPath,

	[Parameter(Mandatory = $true, Position = 2)]
	[string]$OutputPath,

	[Parameter()]
	[ValidateSet("SystemAware", "PerMonitorAware")]
	[string]$DpiAwareness = "SystemAware"
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

Add-Type -TypeDefinition @"
using System.Runtime.InteropServices;

public static class DpiAwarenessSetterForWmf2Png {
	[DllImport("shcore.dll")]
	public static extern int SetProcessDpiAwareness(int value);
}
"@

$value = if ($DpiAwareness -eq "SystemAware") { 1 } else { 2 }
$result = [DpiAwarenessSetterForWmf2Png]::SetProcessDpiAwareness($value)
if ($result -ne 0 -and $result -ne -2147024891) {
	throw "SetProcessDpiAwareness($value) failed: $result"
}

& $ScriptPath $InputPath $OutputPath
