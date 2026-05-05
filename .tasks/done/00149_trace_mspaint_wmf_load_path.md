# Trace MSPaint WMF Load Path

## Purpose
Investigate the actual API/module path mspaint uses when loading WMF files, to identify a route that can exactly match mspaint output.

## Context
- Exact mspaint compatibility is required.
- Prior rendering attempts with GDI+, `PlayEnhMetaFile`, synthetic placeable headers, WIC, and Shell thumbnail paths did not produce complete matches.
- Proposed option 5 is to inspect mspaint's real load path via available tracing tools or Windows process evidence.
- Primary target: `../wmf-testcase/data/src/p0000016.wmf`.

## Tasks
- [x] Check available tracing tools.
  - Status: completed; only Windows Performance Recorder (`wpr.exe`) was found from the checked tracing/debugging tools.
  - Next step: none.
  - Required context: avoid assuming these tools are installed.
- [x] Launch mspaint with a WMF input and collect process evidence.
  - Status: completed; installed Sysinternals Process Monitor through `winget`, launched Store Paint with `p0000016.wmf`, captured process/module evidence and a short ProcMon trace.
  - Next step: none.
  - Required context: GUI launch may require escalated permission.
- [x] Interpret the evidence.
  - Status: completed; Store Paint loads its own Paint DLLs, WIC/WindowsCodecs, PhotoMetadataHandler, GDI/GDI+, D2D/DWrite, and OLE/Shell components. The target WMF is opened and memory-mapped directly by `mspaint.exe`.
  - Next step: none.
- [x] Propose next implementation experiment.
  - Status: completed; next exact-match candidate is to reproduce Paint's direct file-load route around WIC/OLE/GDI+ rather than Shell thumbnail extraction.
  - Next step: none.

## Goals
- Concrete evidence about mspaint's WMF loading stack.
- A practical next rendering path to test for exact match.
- Task summary moved to `.tasks/done/` when complete.

## File List
- `.tasks/done/00149_trace_mspaint_wmf_load_path.md`

## Summary
- Installed Sysinternals Process Monitor via `winget` because only `wpr.exe` was initially available.
- This environment uses Microsoft Store Paint:
  - Package: `Microsoft.Paint_11.2601.441.0_x64__8wekyb3d8bbwe`
  - Executable: `C:\Program Files\WindowsApps\Microsoft.Paint_11.2601.441.0_x64__8wekyb3d8bbwe\PaintApp\mspaint.exe`
  - Command line observed: WindowsApps alias `mspaint.exe` with `\\wsl.localhost\Ubuntu\home\hidek\git\wmf-testcase\data\src\p0000016.wmf`
- Loaded image/graphics-related modules included:
  - Paint app modules: `BasePaint.dll`, `ImageProcessing.dll`, `Microsoft.ImageCreation.dll`, `PaintTools.dll`, `PaintUI.dll`
  - Windows imaging/metadata: `windowscodecs.dll`, `WindowsCodecsExt.dll`, `PhotoMetadataHandler.dll`
  - Drawing/runtime: `GDI32.dll`, `gdi32full.dll`, `gdiplus.dll`, `d2d1.dll`, `DWrite.dll`, `dcomp.dll`
  - Shell/OLE: `SHELL32.dll`, `ole32.dll`, `OLEAUT32.dll`
- ProcMon showed `mspaint.exe` itself opening `p0000016.wmf` several times, then opening it with `Generic Read` and `CreateFileMapping`; the file was mapped successfully.
- Interpretation:
  - Paint is not just asking Explorer/Shell for a thumbnail.
  - WIC/WindowsCodecs and Paint's own image-processing modules are active during the load.
  - GDI/GDI+ are present in the process and remain plausible for WMF decode/playback.
  - Because ProcMon is file/registry/module oriented, it confirms loaded components and file access, but not exact API calls such as `GdipLoadImageFromFile`, `SetWinMetaFileBits`, or `PlayEnhMetaFile`.
- Recommended next experiment:
  - Try a direct native load path that mirrors Paint's process evidence: load WMF from file through GDI+/WIC-adjacent Windows imaging where possible, preserve 96 DPI output, and avoid Shell thumbnail APIs as the primary route.
  - If API-level proof is needed, install/use API Monitor or WinDbg with breakpoints on `gdiplus!GdipLoadImageFromFile`, `gdiplus!GdipDrawImageRectI`, `gdi32!SetWinMetaFileBits`, and `gdi32!PlayEnhMetaFile`.
