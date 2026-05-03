purpose
- Suppress fully transparent SvgGdi alphaBlend image output.

context
- AwtGdi drawAlphaBlend returns without drawing when SourceConstantAlpha is zero.
- SvgGdi alphaBlend currently converts the source image and can emit an SVG image with opacity 0.
- A fully transparent alpha blend has no visible effect in SVG, and skipping it does not require exact XOR support or any font-file dependency.

tasks
- [x] Update SvgGdi.alphaBlend to return early when SourceConstantAlpha is zero.
- [x] Add SvgGdi regression tests for fully transparent and non-transparent alphaBlend output.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- alphaBlend with SourceConstantAlpha 0 emits no SVG image element.
- alphaBlend with nonzero SourceConstantAlpha continues to emit image output.
- Existing source-alpha handling remains unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.alphaBlend to skip fully transparent SourceConstantAlpha output, matching AwtGdi drawAlphaBlend behavior.
- Added regression tests for SourceConstantAlpha 0 producing no image and nonzero alpha continuing to emit image output.
- Kept source-alpha handling, XOR behavior, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
