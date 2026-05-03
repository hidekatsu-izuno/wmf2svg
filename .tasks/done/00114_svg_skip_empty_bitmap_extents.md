purpose
- Suppress SvgGdi bitmap output when destination or source extents are empty.

context
- AwtGdi drawBitmap clamps source extents and returns when the effective source width or height is zero.
- Drawing to a zero-width or zero-height destination also has no visible effect.
- SvgGdi appendPngToSvg can currently emit image/svg wrappers with width or height 0.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Add empty source/destination extent guards before SvgGdi bitmap SVG output.
- [x] Add SvgGdi regression tests for zero destination and zero source extents.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- Bitmap calls with zero destination width or height emit no image element.
- Bitmap calls with zero source width or height emit no image element.
- Normal bitmap output remains unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Added guards in SvgGdi bitmap SVG output to skip zero destination or source extents.
- Added regression tests for zero destination extent, zero source extent, and a non-empty bitmap output control case.
- Kept bitmap ROP behavior, XOR constraints, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
