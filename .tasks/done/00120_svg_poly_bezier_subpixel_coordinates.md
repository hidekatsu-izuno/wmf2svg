purpose
- Preserve subpixel transformed coordinates in SvgGdi immediate polyBezier output.

context
- AwtGdi polyBezier and polyBezierTo build Path2D.Double curves using transformed coordinates.
- SvgGdi immediate polyBezier output currently truncates transformed path coordinates to integers.
- SVG path coordinates can represent fractional coordinates directly.
- Polygon output is not part of this task because AwtGdi also uses java.awt.Polygon with integer coordinates there.
- This task must keep current-position updates and no-op pen behavior unchanged.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.appendBezier to emit formatted double path coordinates.
- [x] Add SvgGdi regression tests for subpixel mapped polyBezier coordinates while preserving existing integer path behavior.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- polyBezier and polyBezierTo preserve fractional transformed M/C coordinates when map scaling produces subpixel coordinates.
- Existing integer-coordinate Bezier path output remains stable.
- No-op pen polyBezierTo still updates the current position without output.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.appendBezier to emit formatted double M/C path coordinates instead of truncating transformed values to integers.
- Added regression tests for subpixel mapped polyBezier and polyBezierTo coordinates.
- Kept current-position updates, no-op pen behavior, XOR behavior, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
