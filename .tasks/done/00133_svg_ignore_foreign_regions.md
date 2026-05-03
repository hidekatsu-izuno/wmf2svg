purpose
- Make SvgGdi handle foreign GdiRegion implementations safely, following AwtGdi's defensive region handling.

context
- AwtGdi only renders or clips regions that are its own AwtRegion instances.
- SvgGdi currently builds region elements through createRegionElement for any non-null GdiRegion, which can emit xlink:href="#null" for foreign region objects that were never registered in nameMap.
- This is SVG-safe cleanup only; it does not attempt exact XOR compositing and does not add any font dependency.

tasks
- [x] Add a SvgRegion guard/helper for renderable regions.
- [x] Update fillRgn, invertRgn, and extSelectClipRgn to ignore foreign regions.
- [x] Keep existing SvgRegion behavior for SvgRectRegion and SvgComplexRegion unchanged.
- [x] Add regression tests for foreign regions.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- SvgGdi no longer emits invalid #null region references for foreign GdiRegion objects.
- Existing region rendering and clipping behavior remains unchanged for SvgGdi-created regions.
- Verification results are recorded and the task is moved to done when complete.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

verification notes
- Focused tests passed:
  - mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test
- Full test suite passed:
  - mvn -q test

summary
- Added a SvgRegion guard for region rendering/clipping inputs.
- Updated fillRgn and invertRgn to skip foreign GdiRegion objects instead of creating invalid xlink:href="#null" output.
- Updated extSelectClipRgn to treat foreign regions as null input, matching AwtGdi's defensive handling.
- Added regression tests for foreign regions in fillRgn, invertRgn, and extSelectClipRgn.
