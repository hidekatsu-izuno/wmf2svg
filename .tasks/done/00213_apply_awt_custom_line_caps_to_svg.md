# Apply AWT Custom Line Caps To SVG

## Purpose
Apply the recent AWT EMF+ custom line cap fixes to `SvgGdi` so SVG output supports the same arrow/cap cases.

## Context
- Recent `AwtGdi` changes added support for default path-based custom line caps, improved custom cap coordinate handling, and allowed custom caps on dashed lines.
- `SvgGdi` already has custom line cap rendering, but it still lacks the newer default custom cap path handling and excludes dashed custom caps.
- There are existing uncommitted `SvgGdi` changes for Dual EMF+ fallback; this task should build on them without reverting or disturbing them.

## Tasks
- [x] Status: completed. Compare AWT and SVG custom line cap parsing/rendering paths.
- [x] Status: completed. Update `SvgGdi` custom line cap support to match AWT where applicable.
- [x] Status: completed. Add SVG regression tests for dashed custom arrow caps and default path-based custom caps.
- [x] Status: completed. Verify targeted SVG tests and full tests.
- [x] Status: completed. Record decisions and move this task to done.

## Goals
- `SvgGdi` renders custom arrow caps for dashed EMF+ lines.
- `SvgGdi` reads and renders default path-based custom line caps.
- Existing SVG Dual EMF+ fallback behavior remains intact.

## File List
- `.tasks/00213_apply_awt_custom_line_caps_to_svg.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Summary
- Added default path-based EMF+ custom line cap parsing to `SvgGdi`.
- Allowed SVG custom start/end caps even when the pen uses dashed line styles.
- Routed simple open `DrawPath` paths through the same polyline custom-cap renderer used by `DrawLines`.
- Added SVG tests for dashed custom arrow start caps and default custom arrow end caps on `DrawPath`.

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest#testEmfPlusDashedDrawLinesUsesCustomArrowStartCap+testEmfPlusDrawPathUsesDefaultCustomArrowEndCap test`
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
- `mvn -q test`
