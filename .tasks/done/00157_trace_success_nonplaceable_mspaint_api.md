# Trace Success Nonplaceable MSPaint API

## Purpose
Trace several successful non-placeable WMFs in Microsoft Paint with the same WinDbg/CDB breakpoint granularity used for the six remaining differing files, then compare API calls and draw arguments.

## Context
- The six visible-diff non-placeable files all previously hit `gdiplus!GdipCreateBitmapFromFile` and `gdiplus!GdipDrawImageRectRectI`.
- Successful non-placeable files have not yet been traced at the same granularity.
- The key question is whether successful non-placeable files use different Paint APIs or the same APIs with different arguments/state.
- Do not change rendering code in this task.

## Tasks
- [x] Select representative successful non-placeable WMFs.
  - Status: completed; selected `2264z_01_1`, `ex2`, `bitblt`, `text`, `sample_01`, and `12e9`.
  - Next step: none.
  - Required context: use prior non-placeable classification and current `png2` compare results.
- [x] Run Paint/CDB load traces.
  - Status: completed; all selected files hit `gdiplus!GdipCreateBitmapFromFile`.
  - Next step: none.
  - Required context: use the same command style as `.tasks/done/00152_cdb_load_commands.txt`.
- [x] Run Paint/CDB draw-argument traces.
  - Status: completed; all selected files hit `gdiplus!GdipDrawImageRectRectI` with full destination/source rectangles and `UnitPixel`.
  - Next step: none.
  - Required context: use the same command style as `.tasks/done/00152_cdb_draw_args_commands.txt`.
- [x] Compare with the six differing files.
  - Status: completed; API names, call stacks, and draw-argument shape match the previously traced six differing files.
  - Next step: none.
- [x] Summarize results.
  - Status: completed; findings appended below.
  - Next step: move task to `.tasks/done/`.

## Goals
- Evidence-backed answer on whether successful non-placeable files use different Paint APIs.
- Per-file comparison of load API and draw rectangle arguments.
- No implementation changes.

## File List
- `.tasks/00157_trace_success_nonplaceable_mspaint_api.md`
- `.tasks/00157_cdb_load_commands.txt`
- `.tasks/00157_cdb_draw_args_commands.txt`

## Summary

Selected successful non-placeable WMFs:

- `2264z_01_1`: `SETMAPMODE=8`, AE=0.
- `ex2`: `SETMAPMODE=8`, AE=0.
- `bitblt`: no `SETMAPMODE`, bitmap records, AE=0.
- `text`: no `SETMAPMODE`, text records, AE=0.
- `sample_01`: no `SETMAPMODE`, small rectangle sample, visible AE=0.
- `12e9`: no `SETMAPMODE`, visible AE=0.

Load trace result:

- All six selected success files hit `gdiplus!GdipCreateBitmapFromFile`.
- Caller stack matched the differing files:
  - `gdiplus!GdipCreateBitmapFromFile`
  - `ImageProcessing!GdiplusHelpers::LoadFileImage+0xa6`
  - `mspaint+0x158ec`
- No first-hit evidence of `GdipLoadImageFromFile`, `GdipCreateMetafileFromFile`, or `GdipCreateMetafileFromWmfFile` for these files.

Draw trace result:

- All six selected success files hit `gdiplus!GdipDrawImageRectRectI`.
- Caller stack matched the differing files:
  - `gdiplus!GdipDrawImageRectRectI`
  - `BasePaint!BasePaint::Image::Create+0x2ac`
  - `mspaint+0x14d89`
- Draw arguments were full destination/source rectangles with source unit `2` (`UnitPixel`):
  - `2264z_01_1`: destination `(0,0,4625,2358)`, source `(0,0,4625,2358)`, unit `2`.
  - `ex2`: destination `(0,0,6529,1478)`, source `(0,0,6529,1478)`, unit `2`.
  - `bitblt`: destination `(0,0,700,600)`, source `(0,0,700,600)`, unit `2`.
  - `text`: destination `(0,0,330,460)`, source `(0,0,330,460)`, unit `2`.
  - `sample_01`: destination `(0,0,200,200)`, source `(0,0,200,200)`, unit `2`.
  - `12e9`: destination `(0,0,1504,512)`, source `(0,0,1504,512)`, unit `2`.

Interpretation:

- The selected successful non-placeable files do not use a different observed Paint load/draw API from the six differing non-placeable files.
- Both success and failure groups use `GdipCreateBitmapFromFile` followed by `GdipDrawImageRectRectI` from the same Paint wrapper stacks.
- The visible differences are therefore not explained by a different high-level Paint API path. The remaining difference is more likely inside GDI+ WMF interpretation for the specific WMF records/state, Paint's canvas/image creation state, alpha handling, or export/post-processing behavior.
- The current implementation should not infer a separate renderer solely from success/failure classification; any branch should be justified by observed arguments/state or by targeted pixel evidence.

Trace artifacts:

- CDB stdout/stderr logs were written under `/tmp/wmf2png-00157-trace/`.
