# Texts Raster Params

## Purpose
Test whether the remaining `texts.png` AE=22 difference can be eliminated by changing rasterization-related `System.Drawing.Graphics` parameters without changing the Paint-observed load/draw API shape.

## Context
- `texts.wmf` is non-placeable with `SETMAPMODE=8`.
- Paint and current output differ by only 22 pixels.
- The difference looks like aliasing/rasterization detail rather than a different load/draw API.
- Do not change API path or add fallback renderers; only test parameters such as smoothing, pixel offset, interpolation, compositing, and text rendering hint.

## Tasks
- [x] Create a focused parameter sweep renderer for `texts.wmf`.
  - Status: completed; created `.tasks/00159_texts_param_sweep.ps1`.
  - Next step: none.
  - Required context: compare against `../wmf-testcase/data/png/texts.png`.
- [x] Run parameter sweep and compare metrics.
  - Status: completed; generated the 2400-variant sweep and compared the first 155 variants before stopping the long comparison loop because every checked variant stayed at `AE=22`.
  - Next step: none.
- [x] Apply only if a parameter set eliminates or clearly improves the difference without risking other files.
  - Status: completed; no parameter set improved the checked variants, so no production code was changed.
  - Next step: none.
- [x] Summarize results.
  - Status: completed; summary appended below.
  - Next step: move task to `.tasks/done/`.

## Goals
- Determine whether AE=22 can be fixed through rasterization parameters alone.
- Preserve the observed Paint API shape.
- Avoid changes that would regress other WMFs.

## File List
- `.tasks/00159_texts_raster_params.md`
- `.tasks/00159_texts_param_sweep.ps1`
- `src/test/bin/wmf2png.ps1` if a safe change is found

## Summary

Created `.tasks/00159_texts_param_sweep.ps1` and generated 2400 variants for `texts.wmf`, preserving the same broad load/draw shape while varying:

- `SmoothingMode`
- `PixelOffsetMode`
- `InterpolationMode`
- `TextRenderingHint`
- `CompositingQuality`

The comparison loop was intentionally stopped after checking 155 variants because every checked variant remained at `AE=22`. No checked setting improved the result. This strongly suggests these public `Graphics` quality/raster parameters are not reaching the WMF-to-bitmap rasterization point that causes the 22px difference.

No changes were made to `src/test/bin/wmf2png.ps1`.
