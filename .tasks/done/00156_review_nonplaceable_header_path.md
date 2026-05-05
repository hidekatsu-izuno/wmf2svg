# Review Nonplaceable Header Path

## Purpose
Review and adjust the WMF-to-PNG path for non-placeable WMFs based on the comparison between matching and differing non-placeable files.

## Context
- Placeable WMFs were restored to the original path and should not be changed in this task.
- Remaining visible differences are limited to non-placeable files.
- Investigation showed five visible-diff non-placeable files have no `SETMAPMODE` and a large mismatch between WMF `SETWINDOWEXT` canvas size and `.NET Bitmap(path)` size.
- Paint tracing showed `GdipCreateBitmapFromFile` followed by `GdipDrawImageRectRectI`, with source rectangles matching Paint's canvas dimensions for the traced diff files.
- Do not add speculative fallback rendering paths.

## Tasks
- [x] Update non-placeable load/draw behavior.
  - Status: completed; removed the eager `Bitmap(path)` rasterization helper and changed non-placeable rendering to draw the loaded GDI+ WMF image directly onto the resolved canvas.
  - Next step: none.
  - Required context: keep placeable load/draw behavior unchanged.
- [x] Run focused comparison on visible-diff non-placeable files.
  - Status: completed; direct non-placeable draw improves `p0000001`, `sample_03`, and `sample_05`, keeps `p0000016` and `texts` at the prior level, and worsens `image6` compared with the temporary source-rectangle variant.
  - Next step: none.
- [x] Regenerate and compare full `png2` set if focused results improve or stay bounded.
  - Status: completed; 57 conversions succeeded and 0 failed; remaining visible diffs are six non-placeable files.
  - Next step: none.
- [x] Summarize results.
  - Status: completed; summary appended below.
  - Next step: move task to `.tasks/done/`.

## Goals
- Non-placeable path avoids scaling a pre-rasterized `.NET Bitmap(path)` image when WMF canvas dimensions are available.
- Placeable results remain unchanged.
- Remaining diffs are listed with metrics after testing.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00156_review_nonplaceable_header_path.md`

## Summary

Updated `src/test/bin/wmf2png.ps1` so non-placeable WMFs no longer all go through the same pre-rasterized `Bitmap(path)` path.

Current behavior:

- Placeable WMFs keep the existing `Image.FromFile` load and `DrawImage(image, 0, 0, Width, Height)` path.
- Non-placeable WMFs with `SETMAPMODE` keep the `Bitmap(path)` load and source/destination rectangle draw path, because this preserves the previously matching `SETMAPMODE=8` group.
- Non-placeable WMFs without `SETMAPMODE` use `Image.FromFile` and draw directly onto the resolved WMF canvas, avoiding scaling a `.NET Bitmap(path)` image whose natural size differs from `SETWINDOWEXT`.

Focused results for the six visible-diff non-placeable files:

- `image6.png`: `AE=2557`, `AlphaAE=9402`
- `p0000001.png`: `AE=625`, `AlphaAE=874013`
- `p0000016.png`: `AE=4969`, `AlphaAE=1803220`
- `sample_03.png`: `AE=2347`, `AlphaAE=2871`
- `sample_05.png`: `AE=7300`, `AlphaAE=7500`
- `texts.png`: `AE=22`, `AlphaAE=22`

Focused regression check for `SETMAPMODE=8` files stayed at `AE=0` for:

- `2264z_01_1`
- `3816x_01_0`
- `3816x_01_1`
- `ex2`
- `ex6`
- `japanese1`
- `test14563_02_1`

Full regeneration:

- Generated `../wmf-testcase/data/png2` for all 57 WMFs.
- `generated_ok=57`, `generated_failed=0`.

Remaining full-set visible diffs:

- `image6.png` `576x544` `AE=2557` `AlphaAE=9402`
- `p0000001.png` `4800x1792` `AE=625` `AlphaAE=874013`
- `p0000016.png` `8192x608` `AE=4969` `AlphaAE=1803220`
- `sample_03.png` `200x200` `AE=2347` `AlphaAE=2871`
- `sample_05.png` `100x100` `AE=7300` `AlphaAE=7500`
- `texts.png` `8204x3735` `AE=22` `AlphaAE=22`

Post-format sanity check:

- Re-rendered `sample_05` after indentation cleanup; result stayed `AE=7300`.
