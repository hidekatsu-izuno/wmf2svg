# Restore GDIPlus Direct Load

## Purpose
Update `src/test/bin/wmf2png.ps1` to follow the WinDbg-confirmed Paint load path for WMF files, then regenerate and compare outputs against the Paint baseline.

## Context
- WinDbg evidence in `.tasks/done/00150_windbg_trace_mspaint_wmf_api.md` showed Store Paint calls `gdiplus!GdipCreateBitmapFromFile` directly with `p0000016.wmf`.
- Current `wmf2png.ps1` still contains a non-placeable `SetWinMetaFileBits` + `PlayEnhMetaFile` path.
- Prior task `00145` showed the PlayEnh path improved alpha for some non-placeable files but was not the actual Paint load path.
- Placeable WMFs should keep the established Paint-compatible explicit-canvas nearest-neighbor behavior.
- Full comparison after the GDI+ direct-load change showed larger alpha differences for some non-placeable files than the previous experimental `PlayEnhMetaFile` path, but fallback must not be used because it does not match the WinDbg-confirmed Paint API path.
- Source WMFs live under `../wmf-testcase/data/src`; baselines under `../wmf-testcase/data/png`; generated outputs under `../wmf-testcase/data/png2`.

## Tasks
- [x] Inspect current render branches and define the target flow.
  - Status: completed; current non-placeable resized/high-DPI branch uses `RenderNonPlaceableWithPlayEnh`, while equal-size non-placeable files save the loaded GDI+ image directly.
  - Next step: remove the PlayEnh primary branch and route non-placeable files through GDI+ direct image save or GDI+ DrawImage onto the resolved canvas.
  - Required context: keep placeable explicit-canvas behavior unchanged.
- [x] Edit `src/test/bin/wmf2png.ps1`.
  - Status: completed; removed the PlayEnh helper path and routed non-placeable equal-size output through the GDI+ loaded image directly.
  - Next step: none.
  - Required context: preserve single PNG save path and transparent PNG output behavior.
- [x] Run focused conversion checks.
  - Status: completed; `p0000016` regenerated at `8192x608` with `AE=4969`, and placeable `image9` regenerated at `777x527` with `AE=0`.
  - Next step: none.
  - Required context: PowerShell conversion runs on Windows side.
- [x] Run full conversion and comparison.
  - Status: completed; 57 succeeded, 0 failed. Remaining differences are accepted as evidence for the next Paint-internal tracing step, not a reason to add non-Paint fallback behavior.
  - Next step: none.
  - Required context: ImageMagick `compare`/`identify` are available.
- [x] Summarize and complete.
  - Status: completed.
  - Next step: none.
  - Required context: if full comparison worsens unexpectedly, update plan before further changes.

## Goals
- Script no longer prefers `PlayEnhMetaFile` for the Paint-confirmed path.
- Focused and full test results are recorded.
- Remaining differing files are listed with useful metrics.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00151_restore_gdiplus_direct_load.md`

## Summary
- Updated `src/test/bin/wmf2png.ps1` to remove the non-placeable `SetWinMetaFileBits` / `PlayEnhMetaFile` rendering path.
- The script now keeps the Paint-confirmed GDI+ file load path as the source image path:
  - `System.Drawing.Image.FromFile($source)` for load.
  - Non-placeable files whose resolved canvas matches the GDI+ image size save the loaded image directly.
  - Files needing an explicit canvas use `Graphics.DrawImage` from that loaded image.
- Focused checks:
  - `p0000016.wmf`: generated `8192x608`, baseline comparison `AE=4969`.
  - `image9.wmf`: generated `777x527`, baseline comparison `AE=0`.
- Full regeneration:
  - 57 succeeded, 0 failed.
- Remaining differences after full comparison:
  - `image6.png` `576x544` `AE=2557` `A=9402` `RGB=2557` `bbox=573x512+1+1`
  - `p0000001.png` `4800x1792` `AE=625` `A=874013` `RGB=625` `bbox=4297x1703+14+33`
  - `p0000016.png` `8192x608` `AE=4969` `A=1.80322e+06` `RGB=4969` `bbox=8049x482+140+75`
  - `sample_03.png` `200x200` `AE=2347` `A=2871` `RGB=2347` `bbox=144x144+7+7`
  - `sample_05.png` `100x100` `AE=7300` `A=7500` `RGB=7300` `bbox=98x98+1+1`
  - `texts.png` `8204x3735` `AE=22` `A=22` `RGB=22` `bbox=4216x1617+879+1429`
- Decision:
  - Do not add or restore fallback rendering that was not observed in Paint.
  - The next exact-match work should trace Paint's post-load canvas/image creation and alpha handling after `GdipCreateBitmapFromFile`, rather than substituting a different renderer.
