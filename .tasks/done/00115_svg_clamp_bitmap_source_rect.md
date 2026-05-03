purpose
- Clamp SvgGdi bitmap source rectangles to the decoded image bounds before emitting SVG.

context
- AwtGdi drawBitmap normalizes negative source extents, clamps source coordinates and sizes to the decoded bitmap bounds, and skips drawing when the effective source width or height is zero.
- SvgGdi appendPngToSvg currently uses the requested source rectangle directly in the SVG viewBox.
- Source rectangles outside the image can therefore emit empty SVG wrappers instead of matching AwtGdi's no-op behavior.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Add source-rectangle normalization and bounds clamping to SvgGdi bitmap SVG output.
- [x] Skip bitmap output when the clamped source rectangle is empty.
- [x] Add SvgGdi regression tests for out-of-bounds and partially out-of-bounds source rectangles.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- Bitmap calls with source rectangles entirely outside the image emit no image output.
- Partially out-of-bounds source rectangles emit a clamped viewBox.
- Existing in-bounds bitmap output remains unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Added source rectangle normalization and image-bounds clamping to SvgGdi bitmap SVG output.
- Skipped bitmap output when the clamped source rectangle is empty, matching AwtGdi drawBitmap behavior.
- Kept full-source bitmap output unwrapped so existing SRCINVERT transparent-mask merging continues to work.
- Added regression tests for out-of-bounds and partially out-of-bounds source rectangles.
- Verified with focused SvgGdi tests and the full Maven test suite.
