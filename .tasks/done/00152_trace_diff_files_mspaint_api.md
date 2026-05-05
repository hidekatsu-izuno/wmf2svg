# Trace Diff Files MSPaint API

## Purpose
Run Microsoft Paint under CDB/WinDbg for every file that still differs from the baseline, and record the load/draw API path Paint uses for each file.

## Context
- Remaining differing PNGs after the GDI+ direct-load change:
  - `image6.png`
  - `p0000001.png`
  - `p0000016.png`
  - `sample_03.png`
  - `sample_05.png`
  - `texts.png`
- Source WMFs live under `../wmf-testcase/data/src`.
- Prior WinDbg work for `p0000016.wmf` showed `gdiplus!GdipCreateBitmapFromFile` from `ImageProcessing!GdiplusHelpers::LoadFileImage`, plus `gdiplus!GdipDrawImageRectRectI` from `BasePaint!BasePaint::Image::Create`.
- The goal is to match Paint behavior, so do not introduce or recommend unobserved fallback renderers.

## Tasks
- [x] Prepare CDB trace command files.
  - Status: completed; created load and draw command files.
  - Next step: none.
  - Required context: CDB is at `C:\Program Files\WindowsApps\Microsoft.WinDbg_1.2603.20001.0_x64__8wekyb3d8bbwe\amd64\cdb.exe`.
- [x] Trace load API for each differing WMF.
  - Status: completed; all six files hit `gdiplus!GdipCreateBitmapFromFile` from `ImageProcessing!GdiplusHelpers::LoadFileImage`.
  - Next step: none.
  - Required context: GUI/debug launch requires Windows PowerShell.
- [x] Trace draw API for each differing WMF.
  - Status: completed; all six files hit `gdiplus!GdipDrawImageRectRectI` from `BasePaint!BasePaint::Image::Create`.
  - Next step: none.
  - Required context: if a breakpoint does not hit within the time limit, record it explicitly.
- [x] Interpret per-file API evidence.
  - Status: completed; all six files share the same observed Paint load/draw path, with draw destination/source rectangles matching the full image size.
  - Next step: none.
  - Required context: do not infer a renderer from pixel closeness alone.
- [x] Complete.
  - Status: completed.
  - Next step: none.
  - Required context: leave command files in `.tasks/done/` if useful for repeatability.

## Goals
- API evidence for all currently differing files.
- Clear per-file load/draw path summary.
- No fallback recommendation unless observed in Paint.

## File List
- `.tasks/done/00152_trace_diff_files_mspaint_api.md`
- `.tasks/done/00152_cdb_load_commands.txt`
- `.tasks/done/00152_cdb_draw_commands.txt`
- `.tasks/done/00152_cdb_draw_args_commands.txt`

## Summary
- Traced these currently differing WMFs with Store Paint under CDB:
  - `image6.wmf`
  - `p0000001.wmf`
  - `p0000016.wmf`
  - `sample_03.wmf`
  - `sample_05.wmf`
  - `texts.wmf`
- Load API result for all six files:
  - API: `gdiplus!GdipCreateBitmapFromFile`
  - Caller stack: `gdiplus!GdipCreateBitmapFromFile` -> `ImageProcessing!GdiplusHelpers::LoadFileImage+0xa6` -> `mspaint+0x158ec`
  - No observed hit on `GdipLoadImageFromFile`, `GdipCreateMetafileFromFile`, or `GdipCreateMetafileFromWmfFile` before this load hit.
- Draw API result for all six files:
  - API: `gdiplus!GdipDrawImageRectRectI`
  - Caller stack: `gdiplus!GdipDrawImageRectRectI` -> `BasePaint!BasePaint::Image::Create+0x2ac` -> `mspaint+0x14d89`
  - No observed first draw hit on `GdipDrawImageRectI`; the first draw hit was consistently `GdipDrawImageRectRectI`.
- Draw arguments observed at the first `GdipDrawImageRectRectI` hit:
  - `image6.wmf`: destination `(0,0,576,544)`, source `(0,0,576,544)`, source unit `2` (`UnitPixel`).
  - `p0000001.wmf`: destination `(0,0,4800,1792)`, source `(0,0,4800,1792)`, source unit `2`.
  - `p0000016.wmf`: destination `(0,0,8192,608)`, source `(0,0,8192,608)`, source unit `2`.
  - `sample_03.wmf`: destination `(0,0,200,200)`, source `(0,0,200,200)`, source unit `2`.
  - `sample_05.wmf`: destination `(0,0,100,100)`, source `(0,0,100,100)`, source unit `2`.
  - `texts.wmf`: destination `(0,0,8204,3735)`, source `(0,0,8204,3735)`, source unit `2`.
- Interpretation:
  - The remaining differing files do not appear to take different Paint loader/render APIs from `p0000016`; they share the same observed GDI+ load and full-size GDI+ draw path.
  - The remaining mismatch is likely after or around Paint's `BasePaint::Image::Create` image/canvas construction, alpha normalization, or PNG export behavior, not a different WMF renderer such as `PlayEnhMetaFile`.
  - Do not add a non-observed fallback renderer based on these traces.
