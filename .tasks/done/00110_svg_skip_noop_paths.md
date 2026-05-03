purpose
- Suppress no-op SvgGdi path output when selected path styles cannot render.

context
- AwtGdi fillPath delegates to fillShape, which skips null, BS_NULL, and BS_HOLLOW brushes.
- AwtGdi strokePath delegates to strokeShape, which skips null and PS_NULL pens.
- SvgGdi appendPath currently can emit path elements even when the requested stroke or fill side is non-renderable.
- SvgGdi appendWidenedPath can also append a path with stroke="none" when the brush used to fill the widened stroke is non-renderable.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update appendPath to skip output when the requested stroke/fill combination has no renderable selected style.
- [x] Update appendWidenedPath to skip output when the widened path fill brush cannot render.
- [x] Add SvgGdi regression tests for no-op fillPath, strokePath, strokeAndFillPath, and widened fillPath behavior.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- fillPath with BS_NULL/BS_HOLLOW brush emits no path.
- strokePath with PS_NULL pen emits no path.
- strokeAndFillPath emits a path if either side is renderable, and emits none when both sides are no-op.
- widened fillPath emits no invisible path when its fill brush is no-op.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated appendPath so fillPath, strokePath, and strokeAndFillPath skip SVG output when the requested stroke/fill sides have no renderable selected pen or brush.
- Updated appendWidenedPath so widened fillPath skips invisible path output when the fill brush is null/hollow.
- Preserved path construction behavior and kept XOR and font handling unchanged.
- Added regression tests for no-op fillPath, strokePath, strokeAndFillPath, visible fill-only strokeAndFillPath, and widened fillPath behavior.
- Verified with focused SvgGdi tests and the full Maven test suite.
