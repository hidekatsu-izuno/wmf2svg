purpose
- Preserve pie geometry when SvgGdi records pie calls into a path.

context
- AwtGdi pie appends Arc2D.PIE into currentPath when a path is active.
- SvgGdi currently emits an immediate pie element even when currentPath is active, so later strokePath/fillPath/selectClipPath do not control that pie.
- SvgPath already supports cubic Bezier path commands and now has arc segment support.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Add SvgPath support for appending pie geometry as a closed subpath.
- [x] Update SvgGdi.pie to record into currentPath when active.
- [x] Add SvgGdi regression tests proving beginPath + pie is emitted only by later path painting.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- beginPath + pie + fillPath emits one path containing curved pie geometry.
- beginPath + pie + abortPath emits no immediate pie output.
- Existing immediate pie behavior remains unchanged outside currentPath.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.pie so active paths record pie geometry instead of emitting immediate SVG output.
- Added SvgPath pie support as a closed subpath built from center, arc start, cubic Bezier arc segments, and close.
- Kept immediate pie rendering, XOR behavior, and font handling unchanged.
- Added regression tests for fillPath-controlled pie output and abortPath suppressing path-recorded pie output.
- Verified with focused SvgGdi tests and the full Maven test suite.
