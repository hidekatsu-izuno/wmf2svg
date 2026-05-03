purpose
- Suppress no-op SvgGdi lineTo output when the selected pen cannot render.

context
- AwtGdi lineTo delegates to strokeShape, which skips drawing for null or PS_NULL pens while still updating the current position.
- SvgGdi lineTo currently emits an SVG line element whenever the segment is nonzero, even for PS_NULL pen.
- CurrentPath behavior must remain unchanged because path geometry is independent of later stroke style.
- This task must not change XOR behavior or introduce font-file dependencies.

tasks
- [x] Update immediate SvgGdi lineTo to skip SVG output when the selected pen is non-renderable.
- [x] Preserve current-position updates for skipped immediate lineTo calls.
- [x] Add SvgGdi tests for no-op lineTo output and current-position preservation.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- lineTo with PS_NULL pen produces no SVG line element.
- Subsequent drawing starts from the lineTo endpoint, matching AwtGdi behavior.
- lineTo inside currentPath continues recording geometry.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated immediate SvgGdi lineTo so PS_NULL or missing pens suppress SVG line output while still advancing the current position.
- Kept currentPath behavior unchanged so path construction continues to record line geometry independent of later stroke rendering.
- Added regression tests for skipped PS_NULL line output and current-position preservation before a subsequent visible line.
- Verified with focused SvgGdi tests and the full Maven test suite.
