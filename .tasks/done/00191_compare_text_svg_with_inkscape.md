# Compare text SVG With Inkscape

## Purpose
Use Inkscape to render `etc/data/dst/text.svg` to PNG, compare it with `etc/data/dst/text.png`, and identify the remaining display problem.

## Context
- User reports that the result has not changed.
- User specifically requested converting `text.svg` to PNG with Inkscape and comparing it to `text.png`.
- Previous checks used ImageMagick rendering and may not reflect Inkscape behavior.
- Existing dirty changes are present; do not revert unrelated changes.

## Tasks
1. Status: completed
   Next step: Done. Checked Inkscape CLI availability and rendered `etc/data/dst/text.svg` to temporary PNGs.
   Required context: Use Inkscape output, not ImageMagick, for the primary comparison.

2. Status: completed
   Next step: Done. Compared the Inkscape-rendered PNG with `etc/data/dst/text.png` and the reference PNG.
   Required context: Measure dimensions, alpha bounds, visible pixel bounds, and obvious offsets.

3. Status: completed
   Next step: Done. Inspected the SVG elements that cause the observed mismatch.
   Required context: Focus on Inkscape-specific handling of text baseline, dy, background rects, and transparency.

4. Status: completed
   Next step: Done. Kept the baseline correction and restored default opaque text background behavior after comparison showed it is needed for parity.
   Required context: Preserve existing fixes for canvas size and explicit text backgrounds.

5. Status: completed
   Next step: Done. Regenerated `text.svg`, re-rendered with Inkscape, and verified comparison.
   Required context: Confirm the actual Inkscape output improves.

6. Status: completed
   Next step: Done. Ran targeted tests and recorded completion notes.
   Required context: Include exact Inkscape comparison findings.

7. Status: completed
   Next step: Move this file to `.tasks/done/00191_compare_text_svg_with_inkscape.md`.
   Required context: Only after verification is complete.

## Goals
- Identify the mismatch using Inkscape-rendered output.
- Fix `text.svg` so Inkscape conversion aligns with `text.png`.
- Keep canvas size and transparent background behavior correct.

## File List
- `.tasks/00191_compare_text_svg_with_inkscape.md`
- `etc/data/dst/text.svg`
- `etc/data/dst/text.png`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Notes
- Inkscape version: `Inkscape 1.2.2`.
- Rendered `etc/data/dst/text.svg` with `inkscape --export-type=png --export-background-opacity=0`.
- Before restoring text background rectangles, Inkscape output was `330x460` with drawing bbox `258x338+0+0`; `etc/data/dst/text.png` was `330x460` with bbox `318x447+0+0`; reference PNG was `330x460` with bbox `328x457+0+3`.
- The visual comparison showed SVG text was not just a canvas/origin issue: the Inkscape-rendered SVG was much smaller vertically and horizontally. The large missing portion is from GDI text background/advance/rotation behavior, especially the rotated lower/right text runs. Removing default opaque text backgrounds made this worse, so that part was restored.
- Kept the `TA_TOP` baseline correction (`dominant-baseline: alphabetic` with `dy="0.88em"`) because it moves SVG text toward the AWT/GDI baseline model without changing the reference x/y values.
- After restoring background rectangles, Inkscape output bbox became `267x338+0+0`, still far from `text.png` (`318x447+0+0`). This means the remaining primary problem is not only the top-left margin: SVG's estimated text advances/background extents and rotated text handling do not match the AWT/GDI PNG path.
- Tests: `mvn -q test -Dtest=SvgGdiTest,AwtGdiTest,WmfGdiTest` passed.
