purpose
- Preserve arc geometry when SvgGdi records arcs into a path.

context
- AwtGdi appends arc geometry to the active Path2D when arc is called inside beginPath/endPath.
- SvgGdi currently records only a line to the arc endpoint in currentPath, losing the curved segment before strokePath/fillPath.
- SVG can represent the resulting path without exact XOR support and without font-file or font-metrics dependencies.
- The existing SvgPath representation already supports cubic Beziers, so arc segments can be approximated as Bezier curves consistently with ellipse and round-rect path handling.

tasks
- [x] Add SvgPath support for appending open elliptical arc geometry as cubic Bezier segments.
- [x] Update SvgGdi.arc currentPath handling to append the arc geometry instead of only the endpoint.
- [x] Add SvgGdi regression tests proving path arcs emit curve commands and do not collapse to a single straight line.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- beginPath + arc + strokePath emits a path containing cubic curve commands for the arc.
- Path arcs connect from the current point to the arc start like AwtGdi Path2D append(..., true).
- Degenerate arcs continue to produce no arc geometry.
- Existing immediate arc output and text/font behavior remain unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Changed SvgGdi arc path recording to preserve open elliptical arc geometry instead of collapsing to a straight line to the endpoint.
- Added SvgPath arc support by approximating arc segments with cubic Beziers, matching the existing path representation used for ellipses and round rectangles.
- Kept immediate arc rendering, XOR handling, and font behavior unchanged.
- Added a regression test for beginPath + arc + strokePath output and verified with focused and full Maven tests.
