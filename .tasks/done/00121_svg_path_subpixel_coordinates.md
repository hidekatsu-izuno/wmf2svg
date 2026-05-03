purpose
- Preserve subpixel transformed coordinates in SvgGdi recorded path output.

context
- AwtGdi records paths in Path2D.Double using transformed move, line, and Bezier coordinates.
- SvgGdi records logical path points and converts them to SVG path data later.
- SvgGdi.toSvgPath currently truncates transformed path points through appendSvgPoint.
- SVG path coordinates can represent fractional coordinates directly.
- This task must keep path command structure, fill/stroke behavior, no-op style guards, and current-position behavior unchanged.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.appendSvgPoint to emit formatted double coordinates.
- [x] Add SvgGdi regression tests for subpixel mapped recorded path coordinates while preserving existing integer path output.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- strokePath/fillPath output preserves fractional transformed M/L/C coordinates when map scaling produces subpixel coordinates.
- Existing integer-coordinate path output remains stable.
- No font-file dependency and no XOR behavior change.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.appendSvgPoint to emit formatted double coordinates instead of truncating transformed path points to integers.
- Added a regression test for subpixel mapped recorded path output covering M, L, and C commands.
- Kept path command structure, fill/stroke behavior, no-op style guards, current-position behavior, XOR behavior, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
