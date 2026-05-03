purpose
- Preserve subpixel logical pixel dimensions in SvgGdi flood fill seed output.

context
- AwtGdi flood fill operates on the rendered pixel surface.
- SvgGdi approximates flood fill by drawing the selected brush at the seed pixel.
- The seed rectangle already maps logical coordinates, but it truncates transformed coordinates and dimensions to integers.
- When one logical pixel maps to less than one SVG unit, the seed rectangle can collapse to width or height 0.
- SVG can represent fractional rectangle dimensions directly.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.appendFloodFillSeed to emit formatted double coordinates and dimensions.
- [x] Add SvgGdi regression tests for subpixel mapped flood fill seed size while preserving existing integer-size output.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- floodFill and extFloodFill seed output preserves nonzero fractional width/height when map scaling makes one logical pixel smaller than one SVG unit.
- Existing integer-size flood fill seed output remains stable.
- Null and hollow brush flood fill guards remain unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.appendFloodFillSeed to emit formatted double coordinates and dimensions instead of truncating transformed seed pixel values to integers.
- Added a regression test for subpixel mapped extFloodFill seed dimensions while preserving the existing integer-size mapping behavior.
- Kept XOR behavior and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
