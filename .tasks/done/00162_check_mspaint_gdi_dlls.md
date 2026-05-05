# 00162 Check MSPaint GDI/GDI+ DLL versions

## Purpose
Verify whether Microsoft Paint is loading a different or older GDI/GDI+ DLL than the one used by the PowerShell/System.Drawing test renderer.

## Context
- Remaining WMF-to-PNG differences may be inside native GDI+ WMF rasterization.
- Paint is a Store app and may have package-local dependencies, but previous traces show calls into `gdiplus!GdipCreateBitmapFromFile`.
- Need evidence for loaded module paths and versions before assuming Paint and PowerShell use the same native DLLs.

## Tasks
- [ ] Capture loaded `gdiplus`, `gdi32`, `gdi32full`, and related module paths from a Paint session opening a representative WMF.
- [ ] Capture the same module paths from a PowerShell/System.Drawing render session.
- [ ] Compare file versions for the loaded DLLs.
- [ ] Summarize whether a different DLL version is a plausible explanation.

## Goals
- Full paths and version info for Paint-loaded and PowerShell-loaded GDI/GDI+ DLLs.
- Clear conclusion about whether Paint uses an older or package-local DLL.

## File List
- `.tasks/00162_check_mspaint_gdi_dlls.md`
- `/tmp/wmf2png-00162-dlls/` temporary command/log files

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: use `../wmf-testcase/data/src/texts.wmf` as representative because it has the tiny remaining aliasing difference.

## Summary
- CDB module load output for Store Paint opening `texts.wmf` showed:
  - `gdiplus.dll`: `C:\Windows\WinSxS\amd64_microsoft.windows.gdiplus_6595b64144ccf1df_1.1.26100.8328_none_6ef30b123dc73046\gdiplus.dll`
  - `GDI32.dll`: `C:\Windows\System32\GDI32.dll`
  - `gdi32full.dll`: `C:\Windows\System32\gdi32full.dll`
  - `windowscodecs.dll`: `C:\Windows\System32\windowscodecs.dll`
- PowerShell/System.Drawing rendering `texts.wmf` loaded the same `gdiplus.dll`, `GDI32.dll`, and `gdi32full.dll` paths.
- Direct version check:
  - `gdiplus.dll`: `10.0.26100.8328 (WinBuild.160101.0800)`
  - `GDI32.dll`: `10.0.26100.8328 (WinBuild.160101.0800)`
  - `gdi32full.dll`: `10.0.26100.8328 (WinBuild.160101.0800)`
  - `windowscodecs.dll`: `10.0.26100.8328 (WinBuild.160101.0800)`

## Decision
- Paint is not using an older or package-local GDI+ DLL for this path.
- DLL version/path mismatch is not a plausible explanation for the remaining raster differences.
