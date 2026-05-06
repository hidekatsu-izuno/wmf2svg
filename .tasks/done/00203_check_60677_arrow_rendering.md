# Check 60677 Arrow Rendering

## Purpose
Verify whether the arrows in `60677.png` and `60677.svg` are rendered incorrectly, identify the cause if they are, and fix the shared rendering path if needed.

## Context
- User reports that both `../wmf-testcase/data/dst/60677.png` and `../wmf-testcase/data/dst/60677.svg` appear to have incorrect arrows.
- The source is `../wmf-testcase/data/src/60677.wmf`.
- Reference PNG exists at `../wmf-testcase/data/png/60677.png`.
- Existing unrelated working tree changes must be preserved.
- Previous task fixed Japanese text advance handling and Dual EMF+ fallback behavior for 60677.

## Tasks
- [x] Status: completed. Compared reference PNG, generated PNG, and SVG output around the arrow regions. The reference has filled arrowheads; generated output had open/slanted heads.
- [x] Status: completed. Traced the bad geometry to GDI fallback paths whose open figures were split by `MoveToEx`/PolyDraw move commands before being closed.
- [x] Status: completed. Applied scoped path-continuation fixes for open figures in SVG/AWT path accumulation and EMF PolyDraw parsing; preserved closed-figure separation.
- [x] Status: completed. Verified with targeted tests, full `mvn -q test`, regenerated `../wmf-testcase/data/dst/60677.png` and `../wmf-testcase/data/dst/60677.svg`, and rechecked arrow crops against `../wmf-testcase/data/png/60677.png`.

## Goals
- Confirm whether both PNG and SVG arrow rendering are wrong.
- If wrong, make arrow rendering match the expected/reference behavior without regressing unrelated rendering.
- Record verification results before moving this task file to `.tasks/done/`.

## File List
- `.tasks/00203_check_60677_arrow_rendering.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfPlusConstants.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/emf/EmfParserTest.java`
- `../wmf-testcase/data/src/60677.wmf` (input, read-only)
- `../wmf-testcase/data/dst/60677.png` and `.svg` (generated outputs)
- `../wmf-testcase/data/png/60677.png` (reference output)

## Completion Summary
- `60677.svg` no longer contains malformed arrowhead path fragments like `M 814,726 M 826,732`; they are now continuous closed triangles.
- `60677.png` was regenerated with the fixed GDI fallback path behavior and its arrowhead crop matches the reference arrow direction/fill.
- AWT keeps Dual EMF+ fallback drawing because AWT cannot remove pixels after partial EMF+ rendering; the fallback path itself is now corrected.
- Verification: `mvn -q test` passed.
