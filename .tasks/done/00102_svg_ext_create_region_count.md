purpose
- Align SvgGdi ExtCreateRegion rectangle-count handling with AwtGdi.

context
- AwtGdi treats the extCreateRegion count argument as the number of rectangles to consume, falling back to the RGNDATA header count when count is not positive.
- SvgComplexRegion currently treats count like a byte limit and also requires a nonzero header size, so valid region data passed with count=1 can become an empty SVG region.
- This affects clipping/filling region fidelity and does not require changing XOR behavior or adding font-file dependencies.

tasks
- [x] Update SvgComplexRegion region-data parsing to use count as a rectangle count like AwtGdi.
- [x] Preserve fallback to the RGNDATA header count when count is zero or negative and clamp to available rectangles.
- [x] Add SvgGdi tests for count-limited and header-count ExtCreateRegion parsing with transforms.
- [x] Run focused SvgGdi tests. Passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`.
- [x] Run the full Maven test suite. Passed with `mvn -q test`.

goals
- SvgGdi extCreateRegion produces transformed rectangles when count is a rectangle count.
- SvgGdi still handles count=0 by using the data header count.
- Invalid or short data remains harmless and produces an empty region.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgComplexRegion.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete
- next step: none

summary
- SvgComplexRegion now parses ExtCreateRegion data with count as a rectangle count, matching AwtGdi.
- When count is zero or negative, parsing falls back to the RGNDATA header count and clamps to the rectangles actually present in the byte array.
- Added SvgGdi tests for translated count-limited region parsing and header-count fallback parsing.
- Kept SVG constraints unchanged: no exact XOR emulation changes and no font-file dependency.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
