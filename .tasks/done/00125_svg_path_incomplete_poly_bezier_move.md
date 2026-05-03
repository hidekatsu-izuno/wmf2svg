purpose
- Match AwtGdi path-current-point behavior for incomplete SvgGdi path polyBezier input.

context
- AwtGdi records path polyBezier through appendBezier(points, false).
- When the point list is non-empty but lacks a complete Bezier segment, AwtGdi still moves the Path2D current point to the first point.
- SvgGdi SvgPath.addPolyBezier currently returns for fewer than 4 points, so the recorded path current point is not moved.
- A later path lineTo can therefore start from a different point than AwtGdi.
- This task must not update the GDI current position for polyBezier, matching AwtGdi.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgPath.addPolyBezier to record a moveTo for any non-empty point list before adding complete Bezier segments.
- [x] Add SvgGdi regression tests for incomplete path polyBezier affecting subsequent path lineTo.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- beginPath + incomplete polyBezier + lineTo uses the first polyBezier point as the recorded path line start.
- The GDI current position remains governed by later commands, not by polyBezier itself.
- Complete path polyBezier behavior remains unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgPath.addPolyBezier to move the recorded path current point for any non-empty point list before adding complete Bezier segments.
- Added a regression test showing incomplete path polyBezier changes the following path lineTo start point without updating GDI current position directly.
- Kept complete path polyBezier behavior, XOR behavior, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
