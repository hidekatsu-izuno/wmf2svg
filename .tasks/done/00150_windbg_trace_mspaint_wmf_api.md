# WinDbg Trace MSPaint WMF API

## Purpose
Use the newly installed WinDbg to identify which GDI+/GDI imaging APIs Microsoft Paint calls while opening `p0000016.wmf`.

## Context
- Option 5 ProcMon evidence showed Store Paint opens and memory-maps `p0000016.wmf` directly.
- Paint loads `gdiplus.dll`, `GDI32.dll`, `gdi32full.dll`, `windowscodecs.dll`, `WindowsCodecsExt.dll`, and Paint-specific image modules.
- ProcMon cannot prove API-level calls.
- WinDbg is now installed and should be used to test breakpoints on likely WMF load/playback APIs.
- Primary target: `../wmf-testcase/data/src/p0000016.wmf`.

## Tasks
- [x] Check WinDbg command-line availability.
  - Status: completed; Store WinDbg package includes `amd64\cdb.exe`, which is suitable for scripted logging.
  - Next step: none.
  - Required context: Store WinDbg may expose only an app execution alias.
- [x] Run Paint under debugger with candidate breakpoints.
  - Status: completed; CDB launched Store Paint with `p0000016.wmf` and hit `gdiplus!GdipCreateBitmapFromFile`; a separate run also hit `gdiplus!GdipDrawImageRectRectI`.
  - Next step: none.
  - Required context: GUI/debug launch may require escalated permission.
- [x] Interpret API evidence.
  - Status: completed; Paint calls GDI+ file bitmap loading from `ImageProcessing!GdiplusHelpers::LoadFileImage`, passing the WMF path directly.
  - Next step: none.
  - Required context: absence of a hit is only meaningful after confirming the module and symbol were loaded/resolved.
- [x] Recommend implementation direction.
  - Status: completed; next implementation experiment should prioritize direct GDI+ `GdipCreateBitmapFromFile`/`Bitmap.FromFile` behavior and the Paint canvas/post-processing path, not WIC Shell thumbnail or `SetWinMetaFileBits` first.
  - Next step: none.
  - Required context: avoid changing implementation until API evidence is collected.

## Goals
- Concrete WinDbg evidence for Paint's WMF API path.
- A concise recommendation for the next rendering experiment.
- Task summary appended and task moved to `.tasks/done/` when complete.

## File List
- `.tasks/done/00150_windbg_trace_mspaint_wmf_api.md`
- `.tasks/done/00150_cdb_commands.txt`
- `.tasks/done/00150_cdb_load_arg_commands.txt`

## Summary
- Found CDB in the Store WinDbg package:
  - `C:\Program Files\WindowsApps\Microsoft.WinDbg_1.2603.20001.0_x64__8wekyb3d8bbwe\amd64\cdb.exe`
- Ran Store Paint under CDB with `../wmf-testcase/data/src/p0000016.wmf`.
- Breakpoints successfully resolved for:
  - `gdiplus!GdipLoadImageFromFile`
  - `gdiplus!GdipLoadImageFromStream`
  - `gdiplus!GdipCreateBitmapFromFile`
  - `gdiplus!GdipCreateBitmapFromStream`
  - `gdiplus!GdipCreateMetafileFromFile`
  - `gdiplus!GdipCreateMetafileFromWmfFile`
  - `gdiplus!GdipCreateMetafileFromStream`
  - `gdiplus!GdipDrawImageRectI`
  - `gdiplus!GdipDrawImageRectRectI`
  - `GDI32!SetWinMetaFileBits`
  - `gdi32full!SetWinMetaFileBits`
  - `gdi32full!PlayEnhMetaFile`
- Observed API hits:
  - `gdiplus!GdipCreateBitmapFromFile`
    - Argument `filename`: `\\wsl.localhost\Ubuntu\home\hidek\git\wmf-testcase\data\src\p0000016.wmf`
    - Stack: `gdiplus!GdipCreateBitmapFromFile` -> `ImageProcessing!GdiplusHelpers::LoadFileImage+0xa6` -> `mspaint+0x158ec`
  - `gdiplus!GdipDrawImageRectRectI`
    - Stack: `gdiplus!GdipDrawImageRectRectI` -> `BasePaint!BasePaint::Image::Create+0x2ac` -> `mspaint+0x14d89`
- Interpretation:
  - Store Paint directly loads the WMF through GDI+ bitmap creation, not through Shell thumbnail extraction.
  - Paint's own `ImageProcessing.dll` wrapper is the immediate load caller.
  - The later image creation path uses GDI+ draw calls through `BasePaint.dll`.
  - No observed hit required starting from `SetWinMetaFileBits`/`PlayEnhMetaFile` in the captured path.
- Recommended next experiment:
  - Make the script's primary path mirror Paint more closely: direct GDI+ file bitmap load first, then match Paint's canvas sizing/background/post-processing behavior.
  - Treat `PlayEnhMetaFile` as a fallback/special-case experiment, not as the primary Paint-compatible route for `p0000016.wmf`.
