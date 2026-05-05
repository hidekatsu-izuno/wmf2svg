# 00163 Compare process state for GDI+ WMF rasterization

## Purpose
Investigate why the same `gdiplus.dll` API calls can still produce small raster differences between Microsoft Paint and the PowerShell/System.Drawing renderer.

## Context
- Paint and PowerShell load the same GDI/GDI+ DLL versions.
- Both paths use `GdipCreateBitmapFromFile` and `GdipDrawImageRectRectI` for the relevant non-placeable WMF files.
- Remaining `texts.png` difference looks like a tiny aliasing/rasterization difference.
- Plausible remaining causes include GDI+ startup flags, process/thread DPI awareness, global font smoothing settings, and DC/font state used internally during WMF playback.

## Tasks
- [ ] Capture `GdiplusStartup` input for Paint and PowerShell.
- [ ] Capture process DPI awareness and system font smoothing/ClearType parameters for Paint/PowerShell context.
- [ ] Run controlled PowerShell render variants with DPI awareness changes, if feasible before GDI+ initialization.
- [ ] Compare variant outputs against Paint reference for `texts.png`.
- [ ] Apply the supported DPI awareness setting to `wmf2png.ps1` before `System.Drawing` initialization.
- [ ] Regenerate and compare all WMF test PNGs after the production script change.
- [ ] Summarize which process-state causes remain plausible.

## Goals
- Evidence table for startup flags and DPI/font smoothing state.
- Pixel comparison for any controlled state variant.
- Clear next recommendation without introducing speculative production fallback behavior.

## File List
- `.tasks/00163_compare_process_state_for_gdiplus_wmf.md`
- `.tasks/00163_*.ps1`
- `.tasks/00163_*.txt`
- `src/test/bin/wmf2png.ps1`
- `/tmp/wmf2png-00163-state/`

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: representative file is `../wmf-testcase/data/src/texts.wmf`; reference is `../wmf-testcase/data/png/texts.png`.

## Summary
- PowerShell default process DPI awareness:
  - `ProcessDpiAwareness=0`
  - font smoothing: enabled, ClearType type `2`, contrast `1200`, orientation `1`
- Paint process DPI awareness:
  - `ProcessDpiAwareness=2` (per-monitor aware)
  - font smoothing values matched PowerShell: enabled, ClearType type `2`, contrast `1200`, orientation `1`
- Paint CDB trace:
  - `GdiplusStartup` was called with at least two startup inputs before WMF load.
  - One call used `GdiplusVersion=3`.
  - Another used `GdiplusVersion=1`.
  - WMF load still hit `GdipCreateBitmapFromFile` from the same `gdiplus.dll` path/version as before.
- Controlled DPI awareness variants for `texts.wmf`:
  - default/unaware PowerShell: visible AE `22`
  - system aware: visible AE `0`
  - per-monitor aware: visible AE `0`
- Production change:
  - Added `Set-PaintLikeDpiAwareness` to `src/test/bin/wmf2png.ps1`.
  - It calls `SetProcessDpiAwareness(2)` before `System.Drawing` is loaded.
  - `E_ACCESSDENIED` is tolerated because Windows returns it when DPI awareness was already set by the host process.
- Full verification after the production change:
  - 57 WMFs generated successfully.
  - Remaining reference differences are now 5 files:
    - `image6`: visible AE `2557`, alpha AE `0`
    - `p0000001`: visible AE `625`, alpha AE `0`
    - `p0000016`: visible AE `4969`, alpha AE `0`
    - `sample_03`: visible AE `2347`, alpha AE `0`
    - `sample_05`: visible AE `7300`, alpha AE `0`
  - `texts.png` is now exact against the Paint reference.

## Decision
- The `texts.png` 22px raster difference was caused by process DPI awareness state, not by DLL version, ICM, public `Graphics` quality settings, or `GpBitmap` mutation.
- Matching Paint's per-monitor DPI awareness before GDI+ initialization is evidence-based and safe on the current test set.
