purpose
- Preserve subpixel transformed coordinates in SvgGdi polyPolygon output.

context
- AwtGdi polyPolygon builds a Path2D.Double from each polygon contour.
- SvgGdi polyPolygon emits an SVG path, but currently truncates transformed contour coordinates to integers.
- SVG path coordinates can represent fractional coordinates directly.
- Single polygon output is not part of this task because AwtGdi uses java.awt.Polygon for polygon.
- This task must keep fill-rule, no-op style guards, and path command structure unchanged.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.polyPolygon to emit formatted double contour coordinates.
- [x] Add SvgGdi regression tests for subpixel mapped polyPolygon coordinates while preserving integer output behavior.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- polyPolygon preserves fractional transformed M/L coordinates when map scaling produces subpixel coordinates.
- Existing integer-coordinate polyPolygon output remains stable.
- No font-file dependency and no XOR behavior change.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.polyPolygon to emit formatted double contour coordinates instead of truncating transformed values to integers.
- Added regression tests for integer-coordinate and subpixel mapped polyPolygon path output.
- Kept fill-rule handling, no-op style guards, path command structure, XOR behavior, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
