# Compare Nonplaceable Success Failure

## Purpose
Compare non-placeable WMFs that currently match the Paint baseline against non-placeable WMFs that still differ, and identify likely behavioral differences to guide the next investigation.

## Context
- After restoring the placeable path, remaining differences are limited to six non-placeable files.
- The user asks to compare successful and failed non-placeable files.
- Do not change rendering code in this task; this is an investigation step.

## Tasks
- [x] Classify all non-placeable WMFs as matching or differing.
  - Status: completed; 19 non-placeable files match and 6 non-placeable files differ.
  - Next step: none.
  - Required context: non-placeable files have no Aldus placeable header.
- [x] Extract metadata for non-placeable WMFs.
  - Status: completed; collected WMF header/canvas fields, GDI+ image sizes, map modes, and alpha-aware compare metrics.
  - Next step: none.
  - Required context: use read-only scripts/commands.
- [x] Compare WMF record/function patterns.
  - Status: completed; counted key record classes including text, fonts, lines, rectangles, polygons, bitmaps, escape records, viewport records, and map mode records.
  - Next step: none.
- [x] Summarize findings.
  - Status: completed; observations appended below.
  - Next step: move task to `.tasks/done/`.

## Goals
- Clear list of non-placeable successes and failures.
- Evidence-backed differences between the groups.
- Next investigation target without adding speculative fallback rendering.

## File List
- `.tasks/00155_compare_nonplaceable_success_failure.md`

## Findings

All files in this investigation are standard non-placeable WMFs (`FileType=1`, `Version=0x0300`) and have no Aldus placeable header.

Using the same visible-pixel compare basis as the previous diff list, the remaining visible-diff non-placeable files are:

- `image6`: AE=1268, alpha AE=11988, canvas `576x544`, GDI+ load size `1311x813`, no `SETMAPMODE`.
- `p0000001`: AE=157318, alpha AE=969894, canvas `4800x1792`, GDI+ load size `1881x1012`, no `SETMAPMODE`.
- `p0000016`: AE=4969, alpha AE=1870670, canvas `8192x608`, GDI+ load size `1958x971`, no `SETMAPMODE`.
- `sample_03`: AE=4310, alpha AE=5123, canvas `200x200`, GDI+ load size `2750x1728`, no `SETMAPMODE`; has repeated `SETVIEWPORTEXT`, including a negative viewport extent, and `INTERSECTCLIPRECT`.
- `sample_05`: AE=7696, alpha AE=7599, canvas `100x100`, GDI+ load size `854x534`, no `SETMAPMODE`; contains only one `RECTANGLE` draw after setting window origin/ext and background mode.
- `texts`: AE=22, alpha AE=22, canvas `8204x3735`, GDI+ load size `8204x3735`, has `SETMAPMODE=8`; this is a small residual rasterization/edge difference rather than the large size/resampling pattern seen in the other five.

The normal-match group is not uniform if alpha is considered. `12e9`, `sample_01`, `sample_02`, and `sample_04` have visible AE=0 but alpha differences. So the previous "success" bucket means "no visible RGB difference under the default ImageMagick compare", not "byte/alpha identical".

The clearest correlation is:

- Full matches generally either have `SETMAPMODE=8` with GDI+ load size equal or nearly equal to `SETWINDOWEXT`, or are special cases where the visible pixels survive the current scaling path.
- Five of the six visible failures have no `SETMAPMODE` and their .NET/GDI+ bitmap load size differs greatly from the WMF `SETWINDOWEXT` canvas. Current output is therefore drawing a pre-rasterized bitmap scaled down/up to the WMF canvas, introducing antialias/alpha and geometry differences.
- `texts` is the exception: it has the expected anisotropic map mode and matching GDI+/canvas size, but differs by only 22 pixels. It should be investigated separately as a small rendering-detail issue, not grouped with the no-map-mode scaling failures.

Next useful target: trace or reproduce how Paint obtains the bitmap dimensions for no-`SETMAPMODE` non-placeable WMFs. The WinDbg observations showed Paint drawing source rectangles at the canvas sizes for the failed files, while `.NET Bitmap(path)` reports larger natural sizes such as `854x534` for `sample_05` and `2750x1728` for `sample_03`.
