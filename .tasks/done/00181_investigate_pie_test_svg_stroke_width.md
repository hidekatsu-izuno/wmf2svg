# Investigate pie_test SVG Stroke Width

## Purpose
Investigate why `pie_test.svg` appears to have borders that are too thin while the PNG output is correct, then apply a focused fix if the SVG stroke-width calculation is wrong.

## Context
- Input file is expected to be `../wmf-testcase/data/src/pie_test.wmf`.
- Existing outputs are expected under `etc/data/dst/pie_test.svg` and `etc/data/dst/pie_test.png`.
- PNG being correct suggests AWT/GDI replay is acceptable and the issue is likely SVG pen width conversion, viewBox scaling, placeable header scaling, or CSS stroke generation.
- Preserve existing uncommitted changes from previous tasks.

## Tasks
1. Status: completed
   Next step: Inspected input/output files, SVG viewBox/width/height, pen CSS, and PNG dimensions.
   Required context: `pie_test.wmf` is placeable, SVG is `9.375in` with `viewBox="0 0 9000 9000"`, PNG is `900x900`. The default border pen `.pen0` is `stroke-width: 1.0`, while the explicit red pen is `45.0`.

2. Status: completed
   Next step: Traced the WMF command sequence for map mode, window/viewport extents, and pen creation/selection.
   Required context: Placeable header is `0..9000` at 1440 DPI. The rectangle uses the default pen before an explicit red pen is created for the pie.

3. Status: completed
   Next step: Compared SVG stroke conversion with AWT stroke conversion.
   Required context: `SvgPen.toStrokeWidth()` has a placeable minimum pixel-width branch, but the default pen CSS is generated during `init()` before `placeableHeader()` has populated `dc` viewport/canvas values, so the default pen misses the placeable minimum.

4. Status: completed
   Next step: Initialized placeable logical extents before default styles are created.
   Required context: This keeps the `ant` policy intact: placeable WMF pens use at least one output pixel worth of SVG user units, including the default pen.

5. Status: completed
   Next step: Added regression coverage.
   Required context: Added `testPlaceableDefaultPenUsesAtLeastOneOutputPixelWidth`; existing `ant`-style placeable thin pen tests still assert the same lower-bound behavior.

6. Status: completed
   Next step: Verified with targeted tests, regenerated `pie_test.svg`, and broader Maven tests.
   Required context: `SvgGdiTest` and full `mvn -q test` passed. Regenerated `pie_test.svg` has `.pen0` at `stroke-width: 10.0`, and `ant.svg` still has placeable black pens at `stroke-width: 10.337539432176657` with no `vector-effect`.

## Goals
- Determine the concrete cause of the thin SVG border.
- Fix SVG stroke width while keeping PNG unaffected.
- Verify with tests and regenerated output.
- Append completion notes and move this file to `.tasks/done/00181_investigate_pie_test_svg_stroke_width.md`.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgPen.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `etc/data/dst/pie_test.svg`
- `etc/data/dst/pie_test.png`
- `../wmf-testcase/data/src/pie_test.wmf`

## Completion Summary
- Cause: The `ant.svg` fix was correct for pens created after placeable setup, but `pie_test.svg` uses the default pen for the rectangle. `SvgGdi.placeableHeader()` called `init()` before the placeable viewport/canvas data was stored in `dc`, so the default `.pen0` CSS was generated as `stroke-width: 1.0` and missed the placeable minimum output-pixel width.
- Fix: Store placeable logical and target canvas dimensions before `init()`, and initialize `dc` with those extents before default styles are created. This aligns the default pen with the same placeable stroke-width rule used by `ant.svg`.
- Regression: Added coverage for a placeable default pen requiring a `10.0` user-unit stroke width.
- Verification: Regenerated `etc/data/dst/pie_test.svg` and PNG; `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` passed.
