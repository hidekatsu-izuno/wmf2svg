param(
	[int]$ProcessId = $PID
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class ProcessStateProbe {
	[DllImport("kernel32.dll", SetLastError=true)]
	public static extern IntPtr OpenProcess(UInt32 dwDesiredAccess, bool bInheritHandle, UInt32 dwProcessId);

	[DllImport("kernel32.dll", SetLastError=true)]
	public static extern bool CloseHandle(IntPtr hObject);

	[DllImport("shcore.dll")]
	public static extern Int32 GetProcessDpiAwareness(IntPtr hprocess, out Int32 value);

	[DllImport("user32.dll")]
	public static extern IntPtr GetThreadDpiAwarenessContext();

	[DllImport("user32.dll")]
	public static extern Int32 GetAwarenessFromDpiAwarenessContext(IntPtr value);

	[DllImport("user32.dll", SetLastError=true)]
	public static extern bool SystemParametersInfo(UInt32 uiAction, UInt32 uiParam, ref UInt32 pvParam, UInt32 fWinIni);

	[DllImport("user32.dll", SetLastError=true)]
	public static extern bool SystemParametersInfo(UInt32 uiAction, UInt32 uiParam, IntPtr pvParam, UInt32 fWinIni);
}
"@

$PROCESS_QUERY_LIMITED_INFORMATION = [uint32]0x1000
$SPI_GETFONTSMOOTHING = [uint32]0x004A
$SPI_GETFONTSMOOTHINGTYPE = [uint32]0x200A
$SPI_GETFONTSMOOTHINGCONTRAST = [uint32]0x200C
$SPI_GETFONTSMOOTHINGORIENTATION = [uint32]0x2012

$handle = [ProcessStateProbe]::OpenProcess($PROCESS_QUERY_LIMITED_INFORMATION, $false, [uint32]$ProcessId)
$dpiAwareness = -999
$dpiResult = if ($handle -ne [IntPtr]::Zero) {
	[ProcessStateProbe]::GetProcessDpiAwareness($handle, [ref]$dpiAwareness)
} else {
	[Runtime.InteropServices.Marshal]::GetLastWin32Error()
}

$threadContext = [ProcessStateProbe]::GetThreadDpiAwarenessContext()
$threadAwareness = [ProcessStateProbe]::GetAwarenessFromDpiAwarenessContext($threadContext)

$fontSmoothing = [uint32]0
$fontType = [uint32]0
$fontContrast = [uint32]0
$fontOrientation = [uint32]0
[void][ProcessStateProbe]::SystemParametersInfo($SPI_GETFONTSMOOTHING, 0, [ref]$fontSmoothing, 0)
[void][ProcessStateProbe]::SystemParametersInfo($SPI_GETFONTSMOOTHINGTYPE, 0, [ref]$fontType, 0)
[void][ProcessStateProbe]::SystemParametersInfo($SPI_GETFONTSMOOTHINGCONTRAST, 0, [ref]$fontContrast, 0)
[void][ProcessStateProbe]::SystemParametersInfo($SPI_GETFONTSMOOTHINGORIENTATION, 0, [ref]$fontOrientation, 0)

if ($handle -ne [IntPtr]::Zero) {
	[void][ProcessStateProbe]::CloseHandle($handle)
}

[pscustomobject]@{
	ProcessId = $ProcessId
	ProcessDpiAwarenessResult = $dpiResult
	ProcessDpiAwareness = $dpiAwareness
	CurrentThreadDpiAwarenessContext = ("0x{0:X}" -f $threadContext.ToInt64())
	CurrentThreadDpiAwareness = $threadAwareness
	FontSmoothing = $fontSmoothing
	FontSmoothingType = $fontType
	FontSmoothingContrast = $fontContrast
	FontSmoothingOrientation = $fontOrientation
}
