purpose
- Match AwtGdi current-position behavior for SvgGdi immediate polyBezier.

context
- AwtGdi polyBezier draws a Bezier path but does not update the current position.
- AwtGdi polyBezierTo uses the current position and updates it to the last supplied point.
- SvgGdi immediate polyBezier and polyBezierTo share appendBezier, which currently updates the current position for both complete operations.
- This makes drawing after polyBezier start from the Bezier end in SvgGdi, unlike AwtGdi.
- This task must keep polyBezierTo current-position updates, incomplete polyBezierTo handling, and no-op pen handling intact.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.appendBezier so complete immediate polyBezier does not update the current position.
- [x] Add/update SvgGdi regression tests for polyBezier vs polyBezierTo current-position behavior.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- polyBezier output remains rendered but does not move the current position.
- polyBezier with PS_NULL emits no path and still does not move the current position.
- polyBezierTo continues to move the current position to the last point, including incomplete point lists.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.appendBezier so complete immediate polyBezier no longer updates the current position.
- Preserved polyBezierTo current-position updates for complete and incomplete point lists.
- Updated the no-op pen polyBezier test and added a rendered polyBezier regression test to verify the following lineTo starts from the original current position.
- Kept XOR behavior and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
