# Fix text SVG Content Offset

## Purpose
Fix the remaining `text.svg` display issue where the SVG content has no upper-left margin while `text.png` is correct.

## Context
- User clarified that the previous fix did not improve the actual display.
- `text.png` is correct and has upper-left margin; `text.svg` currently has matching canvas size but content starts at the top-left.
- The source appears to be `../wmf-testcase/data/src/text.wmf`, a non-placeable WMF without explicit window/viewport extents.
- Previous task changed default canvas overflow handling; do not revert unrelated dirty changes.

## Tasks
1. Status: completed
   Next step: Done. Compared rendered content bounds for `text.png`, reference PNG, and SVG-rendered output.
   Required context: Determine the expected top-left margin and whether SVG differs due to canvas origin or text baseline.

2. Status: completed
   Next step: Done. Inspected WMF parser/default mapping behavior for non-placeable WMFs with no extents.
   Required context: Identify where PNG/AWT gets its default margin or coordinate offset.

3. Status: completed
   Next step: Done. Implemented a focused SVG-side fix.
   Required context: Preserve `texts.svg` and other negative-origin/default-canvas behavior.

4. Status: completed
   Next step: Done. Updated regression tests.
   Required context: Capture the default non-placeable text offset behavior.

5. Status: completed
   Next step: Done. Regenerated `text.svg` and verified display bounds against PNG.
   Required context: Confirm both canvas size and content offset match expectations.

6. Status: completed
   Next step: Done. Ran targeted tests and recorded completion notes.
   Required context: Include cause, fix, verification, and residual risks.

7. Status: completed
   Next step: Move this file to `.tasks/done/00190_fix_text_svg_content_offset.md`.
   Required context: Only after verification is complete.

## Goals
- Make `text.svg` display with the same upper-left margin as `text.png`.
- Keep `text.svg` canvas at `330x460`.
- Avoid regressing `texts.svg` and other no-placeable-header cases.

## File List
- `.tasks/00190_fix_text_svg_content_offset.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `etc/data/dst/text.svg`
- `etc/data/dst/text.png`

## Completion Notes
- Cause: `text.wmf` does not explicitly set background mode or background color, but SVG treated the default `BkMode=OPAQUE` as an instruction to emit estimated white text background rectangles. The first rectangle started at `x=0 y=0`, making the SVG's upper-left area opaque white while the PNG/reference output keeps that area transparent. SVG also placed `TA_TOP` text with `dominant-baseline: text-before-edge`, while AWT/PNG converts the top reference point to an alphabetic baseline.
- Fix: `SvgDc` now tracks whether text background behavior was explicitly touched by `SETBKMODE` or `SETBKCOLOR`. `SvgGdi` emits estimated text background rectangles only when `BkMode=OPAQUE` is explicit, while still honoring `ETO_OPAQUE`. For `TA_TOP` text, SVG now uses alphabetic baseline with `dy="0.88em"` to match the GDI/AWT top-reference behavior without changing the stored `x/y` reference coordinates.
- Regression: Updated `testNoPlaceableHeaderDoesNotExpandDefaultCanvasForPositiveOverflow` to assert default text keeps the `330x460` canvas, does not emit a background rect, and uses the baseline adjustment.
- Verification: `mvn -q test -Dtest=SvgGdiTest,AwtGdiTest,WmfGdiTest` passed.
- Output check: regenerated `etc/data/dst/text.svg` and `.png`; SVG remains `330x460 viewBox="0 0 330 460"`, its initial text no longer has a preceding white rect, and a transparent-background render has alpha `0` at the upper-left pixels.
