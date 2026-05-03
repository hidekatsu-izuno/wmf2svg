purpose
- Align SvgGdi flood-fill seed rendering with AwtGdi null/hollow brush behavior.

context
- SvgGdi approximates FloodFill/ExtFloodFill with a mapped one-pixel seed rectangle because SVG cannot reproduce bitmap flood-fill semantics directly.
- AwtGdi still skips drawing when the selected brush is null/hollow.
- SvgGdi currently only checks for null and can emit a no-op seed rect for BS_NULL/BS_HOLLOW brushes.
- This task keeps the SVG flood-fill approximation but suppresses seed output for non-renderable brushes.

tasks
- [x] Update appendFloodFillSeed to use the existing renderable brush helper.
- [x] Preserve existing solid, hatch, and pattern seed rendering behavior.
- [x] Add SvgGdi tests for FloodFill/ExtFloodFill with a null brush.
- [x] Run focused SvgGdi tests. Passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`.
- [x] Run the full Maven test suite. Passed with `mvn -q test`.

goals
- FloodFill/ExtFloodFill do not emit seed rectangles when the selected brush is null/hollow.
- Existing flood-fill seed output with renderable brushes continues to work.
- No change is made to SVG's known inability to perform true bitmap flood fill.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete
- next step: none

summary
- SvgGdi flood-fill seed output now uses the same renderable brush helper as region fills.
- FloodFill and ExtFloodFill skip seed rectangles when the selected brush is null/hollow, matching AwtGdi's no-draw behavior for actual filled pixels.
- Existing renderable brush seed output remains covered by the mapped logical pixel size test.
- Kept SVG constraints unchanged: no true bitmap flood-fill emulation, no exact XOR emulation changes, and no font-file dependency.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
