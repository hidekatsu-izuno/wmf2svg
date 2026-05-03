purpose
- Preserve subpixel logical pixel dimensions in SvgGdi setPixel output.

context
- AwtGdi setPixel draws a transformed 1x1 logical rectangle using double-precision Rectangle2D.
- SvgGdi setPixel currently casts transformed coordinates and dimensions to int, so small mapped logical pixels can collapse to width or height 0.
- SVG can represent fractional rectangle dimensions directly.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.setPixel to emit formatted double coordinates and dimensions.
- [x] Add SvgGdi regression tests for subpixel mapped logical pixel size and existing integer-size output.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- setPixel preserves nonzero fractional width/height when map scaling makes one logical pixel smaller than one SVG unit.
- Existing integer-size setPixel output remains stable.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.setPixel to emit formatted double coordinates and dimensions instead of truncating transformed values to integers.
- Added a regression test for subpixel mapped logical pixel sizes while preserving the existing integer-size setPixel behavior.
- Kept XOR behavior and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
