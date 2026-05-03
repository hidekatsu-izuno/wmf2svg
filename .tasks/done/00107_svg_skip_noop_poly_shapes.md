purpose
- Suppress no-op SVG output for polygon and stroke-only primitives when selected tools cannot render.

context
- AwtGdi drawShape/strokeShape skip output for non-renderable pen/brush selections.
- SvgGdi still emits SVG elements for arc, polyline, polyBezier, polygon, and polyPolygon in some null-pen/null-brush cases.
- Current-position updates and path recording must remain unchanged.
- This task must not change XOR behavior or introduce font-file dependencies.

tasks
- [x] Skip immediate arc output when the selected pen is non-renderable.
- [x] Skip immediate polyline and polyBezier output when the selected pen is non-renderable while preserving current-position updates.
- [x] Skip immediate polygon and polyPolygon output when selected pen and brush are both non-renderable.
- [x] Add focused SvgGdi tests for these no-op cases.
- [x] Run focused SvgGdi tests. Passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`.
- [x] Run the full Maven test suite. Passed with `mvn -q test`.

goals
- Stroke-only primitives do not emit no-op SVG elements with PS_NULL pen.
- Filled/stroked polygon primitives do not emit no-op SVG elements with PS_NULL pen and null/hollow brush.
- Geometry recording in currentPath and current-position behavior remain intact.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete
- next step: none

summary
- Immediate arc output is skipped when the selected pen is non-renderable.
- Immediate polyline and polyBezier output is skipped for PS_NULL pen while preserving current-position updates.
- Immediate polygon and polyPolygon output is skipped when selected pen and brush are both non-renderable.
- CurrentPath recording remains unchanged.
- Kept SVG constraints unchanged: no exact XOR emulation changes and no font-file dependency.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
