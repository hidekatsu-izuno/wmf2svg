purpose
- Suppress no-op SVG elements for basic shapes when selected pen and brush cannot render.

context
- AwtGdi drawShape fills with the selected brush and strokes with the selected pen, but both operations return without drawing for null/PS_NULL pen and null/BS_NULL/BS_HOLLOW brush.
- SvgGdi currently emits immediate rectangle, roundRect, and ellipse elements even when both selected tools are non-renderable.
- Path recording must remain unchanged because GDI path geometry is independent of the later fill/stroke style.
- This task must not change XOR behavior or introduce font-file dependencies.

tasks
- [x] Add selected pen/brush renderability helpers to SvgGdi.
- [x] Skip immediate rectangle, roundRect, and ellipse output when both selected tools are non-renderable.
- [x] Add SvgGdi tests for no-op rectangle, roundRect, and ellipse with null pen and null brush.
- [x] Run focused SvgGdi tests. Passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`.
- [x] Run the full Maven test suite. Passed with `mvn -q test`.

goals
- SvgGdi avoids no-op SVG geometry for basic immediate shapes when AwtGdi would draw nothing.
- Existing path construction behavior remains unchanged.
- Existing renderable pen or brush behavior remains intact.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete
- next step: none

summary
- Added selected pen/brush renderability helpers to SvgGdi.
- Immediate rectangle, roundRect, and ellipse output is skipped when selected pen is null/PS_NULL and selected brush is null/BS_NULL/BS_HOLLOW.
- Current path construction remains unchanged, preserving GDI's style-independent path geometry behavior.
- Added SvgGdi coverage for no-op basic shapes with null pen and null brush.
- Kept SVG constraints unchanged: no exact XOR emulation changes and no font-file dependency.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
