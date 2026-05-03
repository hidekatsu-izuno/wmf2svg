purpose
- Align AwtGdi integer-coordinate GDI rendering with actual Win32 GDI device-coordinate rasterization.

context
- User asked to verify AwtGdi places that preserve fractional mapped coordinates and fix all of them unless there is a special reason not to.
- Direct Win32 GDI live probes from task 00130 showed anisotropic-mapped half-coordinate Polyline and GradientFill output lands on rounded device pixels.
- AwtGdi currently uses Java2D double shapes for many integer-coordinate GDI records, so Java2D can preserve fractional coordinates where Win32 GDI rasterizes onto integer device pixels.
- EMF+ uses floating coordinates and should remain out of scope unless a GDI integer path shares the same helper.
- Font-file dependencies and exact XOR behavior must not be changed.

tasks
- [x] Extend/reuse direct Win32 GDI live probes for representative AwtGdi integer-coordinate primitives.
- [x] Add rounded device-coordinate helpers to AwtGdi for integer GDI logical coordinates.
- [x] Update AwtGdi immediate integer-coordinate primitives to use rounded device coordinates.
- [x] Update AwtGdi path construction for integer-coordinate GDI paths to use rounded device coordinates.
- [x] Keep EMF+ floating-coordinate rendering unchanged.
- [x] Add focused AwtGdi regression tests or adapt existing tests to cover rounded coordinate behavior.
- [x] Run focused AwtGdi/SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- AwtGdi integer GDI drawing no longer preserves fractional mapped coordinates where Win32 GDI rounds to device pixels.
- Existing SvgGdi fixes remain unchanged.
- EMF+ floating coordinate rendering remains unchanged.
- Verification evidence and decisions are recorded in this task file.

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

status
- current: complete.
- next step: none.

verification notes
- Direct Win32 GDI live probe rerun:
  - mapped half Polyline: nonwhite=3 bbox=1,2-3,2
  - rounded control Polyline: nonwhite=3 bbox=0,1-2,1
  - mapped half GradientFill: nonwhite=4 bbox=2,2-3,3
  - rounded control GradientFill: nonwhite=4 bbox=1,1-2,2
- Decision: integer-coordinate GDI records should be mapped to rounded device coordinates before Java2D rendering in AwtGdi.
- Focused tests passed:
  - mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test
  - mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test
- Full test suite passed:
  - mvn -q test

summary
- Added rounded device-coordinate helpers in AwtGdi and applied them to integer-coordinate GDI drawing paths: lines, polygons, polylines, Beziers, rectangles/ellipses/round rects, arcs, regions, GradientFill, brush origins, text origins, and bitmap destination placement.
- Left EMF+ floating-coordinate rendering unchanged by keeping the existing EMF+ transform helpers separate from the new integer-coordinate helpers.
- Added AwtGdi regression tests that assert mapped integer-coordinate polylines and GradientFill rectangles render on rounded device pixels.
