purpose
- Match AwtGdi current-position updates for incomplete immediate SvgGdi polyBezierTo point lists.

context
- AwtGdi polyBezierTo updates the current position to the last supplied point whenever the point list is non-empty.
- SvgGdi immediate polyBezierTo delegates to appendBezier, which returns without updating the current position when the point list does not contain a complete Bezier segment.
- This can make the next line or connected drawing start from the old current position.
- The fix should not emit an SVG path for incomplete Bezier input; it should only preserve the state update.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.appendBezier so incomplete immediate polyBezierTo updates the current position to the last supplied point.
- [x] Add SvgGdi regression tests for incomplete polyBezierTo current-position updates.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- polyBezierTo with one or two points emits no Bezier path but moves the current position to the last point.
- Subsequent lineTo starts from the updated point.
- polyBezier behavior and complete polyBezierTo behavior remain unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.appendBezier so incomplete immediate polyBezierTo input updates the current position to the last supplied point without appending an SVG path.
- Added a regression test showing a following lineTo starts from the incomplete polyBezierTo endpoint and no Bezier path is emitted.
- Kept polyBezier behavior, complete polyBezierTo behavior, XOR behavior, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
