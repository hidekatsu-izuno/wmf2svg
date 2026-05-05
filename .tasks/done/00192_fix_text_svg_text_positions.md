# Fix text SVG Text Positions

## Purpose
Re-check `text.svg` with Inkscape output and fix incorrect text/background positions relative to `text.png`.

## Context
- User confirms text background rectangles are restored, but positions are still incorrect.
- Previous Inkscape comparison showed SVG-rendered content bbox (`267x338+0+0`) is much smaller than `text.png` (`318x447+0+0`).
- The remaining issue likely involves text baseline, background rectangle position/size, rotated text, or estimated text advances.
- Do not revert unrelated dirty changes.

## Tasks
1. Status: completed
   Next step: Done. Re-rendered `text.svg` with Inkscape and produced direct visual/computed comparisons against `text.png`.
   Required context: Measure per-region position differences, not just canvas size.

2. Status: completed
   Next step: Done. Inspected how SVG encodes TextOut background rectangles, text baseline, and rotations versus AWT/GDI behavior.
   Required context: Determine whether `dy`, `dominant-baseline`, rotation center, or text extent estimates are wrong.

3. Status: completed
   Next step: Done. Implemented a focused SVG fix.
   Required context: Keep text background rectangles and canvas size behavior intact.

4. Status: completed
   Next step: Done. Added regression coverage.
   Required context: Capture the specific position/rotation behavior being corrected.

5. Status: completed
   Next step: Done. Regenerated `text.svg`, rendered with Inkscape, and compared again to `text.png`.
   Required context: Verify the observed positional mismatch improves.

6. Status: completed
   Next step: Done. Ran targeted tests and recorded completion notes.
   Required context: Include exact before/after Inkscape metrics.

7. Status: completed
   Next step: Move this file to `.tasks/done/00192_fix_text_svg_text_positions.md`.
   Required context: Only after verification is complete.

## Goals
- Identify the remaining positional mismatch.
- Improve Inkscape-rendered `text.svg` alignment with `text.png`.
- Preserve restored text background rectangles.

## File List
- `.tasks/00192_fix_text_svg_text_positions.md`
- `etc/data/dst/text.svg`
- `etc/data/dst/text.png`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Notes
- Cause: The remaining mismatch was an origin/viewBox problem. In `text.wmf`, a rotated text run extends above logical y=0. The AWT PNG path expands the internal canvas upward and translates the rendered content down, while SVG footer bounds ignored `rotate(...)`, leaving `viewBox="0 0 330 460"`.
- Fix: SVG content bounds now account for simple `rotate(...)`, `translate(...)`, and `matrix(...)` transforms. For non-placeable WMFs without explicit extents, if negative overflow still fits within the default canvas, SVG now preserves the default size but shifts the viewBox origin to include the negative overflow, e.g. `viewBox="0 -106 330 460"`.
- Regression: Added `testNoPlaceableHeaderShiftsDefaultCanvasForRotatedNegativeOverflow`.
- Verification: `mvn -q test -Dtest=SvgGdiTest,AwtGdiTest,WmfGdiTest` passed.
- Output check: regenerated `etc/data/dst/text.svg`; it now has `width="330" height="460" viewBox="0 -106 330 460"`. Inkscape-rendered PNG bbox improved from `267x338+0+0` to `314x444+0+0`; `etc/data/dst/text.png` is `318x447+0+0`. The remaining few-pixel difference is consistent with SVG/Inkscape vs AWT font metrics and text extent estimation.
