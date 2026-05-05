# Fix text SVG Origin Bounds

## Purpose
Analyze why `text.svg` has an incorrect origin and visible range, then fix the converter if the issue is implementation-related.

## Context
- User reported that `text.svg` has wrong origin position and display bounds.
- Recent changes touched SVG canvas/viewBox calculation, EMF header canvas handling, EMF+ metafile images, and text/raster output paths.
- The repository has unrelated dirty changes from prior tasks; do not revert them.

## Tasks
1. Status: completed
   Next step: Done. Located the `text` source and generated outputs, then inspected SVG root attributes, text coordinates, and reference PNG dimensions.
   Required context: Determine whether the problem is canvas origin, canvas size, text transform, clipping, or font metrics.

2. Status: completed
   Next step: Done. Inspected the source WMF records that define mapping mode, window/viewport, and text records.
   Required context: Identify the expected logical coordinate system and output bounds.

3. Status: completed
   Next step: Done. Implemented a focused converter fix.
   Required context: Keep changes general and consistent with existing canvas and text handling.

4. Status: completed
   Next step: Done. Added a regression test.
   Required context: Use a deterministic text/mapping case that captures the observed failure.

5. Status: completed
   Next step: Done. Regenerated `text.svg` and relevant PNG output, then verified dimensions and viewBox.
   Required context: Confirm the fix addresses the sample and does not regress nearby output behavior.

6. Status: completed
   Next step: Done. Ran targeted tests and recorded completion notes.
   Required context: Include cause, fix, verification, and residual risks.

7. Status: completed
   Next step: Move this file to `.tasks/done/00189_fix_text_svg_origin_bounds.md`.
   Required context: Only after verification is complete.

## Goals
- Explain why `text.svg` has the wrong origin/range.
- Produce corrected SVG canvas and text placement behavior.
- Verify with tests and regenerated outputs.

## File List
- `.tasks/00189_fix_text_svg_origin_bounds.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Notes
- Cause: `text.wmf` has no placeable header and no explicit window/viewport extents, so PNG rendering uses the default `330x460` WMF canvas. SVG footer logic expanded that default canvas with positive overflow from a rotated opaque text background rectangle, producing `430x460`.
- Fix: For non-placeable WMFs without explicit extents, keep the default `330x460` canvas when content only overflows to the right/bottom. Continue using content bounds when content extends into negative coordinates, preserving large negative-origin samples such as `texts.wmf`.
- Regression: Added `testNoPlaceableHeaderDoesNotExpandDefaultCanvasForPositiveOverflow`.
- Verification: `mvn -q test -Dtest=SvgGdiTest,AwtGdiTest,WmfGdiTest` passed.
- Output check: regenerated `etc/data/dst/text.svg` and `.png`; SVG is now `width="330" height="460" viewBox="0 0 330 460"`, matching PNG dimensions. `texts.svg` remains `width="8204" height="3735" viewBox="-4100 -1866 8204 3735"`.
- PNG comparison: `etc/data/dst/text.png` and `../wmf-testcase/data/png/text.png` are both `330x460`; `compare -metric AE -fuzz 10%` reported `28654`, consistent with rendering/font differences rather than canvas size.
