purpose
- Fix the incorrect display of sample_02 output by aligning SvgGdi coordinate handling with AwtGdi.

context
- The current generated /home/hidek/git/wmf2svg/etc/data/dst/sample_02.svg has viewBox "100 100 200 200" while its rectangles are emitted around 0,-75, so the visible drawing is outside the viewport.
- The raster PNG currently matches the checked sample PNG, but the SVG coordinate placement itself appears wrong.
- sample_02.wmf is located at /home/hidek/git/wmf-testcase/data/src/sample_02.wmf.
- The likely area is placeable header, window/viewport origin, or viewport scaling behavior, not XOR or font handling.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Inspect sample_02.wmf records to identify its placeable header and mapping/origin records.
- [x] Compare AwtGdi and SvgGdi handling of the relevant mapping/origin records.
- [x] Implement the smallest SvgGdi fix for sample_02 coordinate placement.
- [x] Add regression coverage for the sample_02-style mapping case.
- [x] Regenerate or inspect sample_02 output as needed.
- [x] Run focused tests.
- [x] Run the full Maven test suite.

goals
- sample_02 SVG content is placed inside its viewport instead of being shifted outside it.
- Existing placeable-header and viewport tests remain valid or are intentionally updated with clear AwtGdi-backed reasoning.
- No font-file dependency and no XOR behavior change.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java
- src/test/java/net/arnx/wmf2svg/gdi/wmf/WmfGdiTest.java

status
- current: complete.
- next step: none.

summary
- Found that sample_02.wmf has no placeable header and sets window extents before repeatedly offsetting the window origin.
- SvgGdi.footer used the final window origin for the root viewBox, shifting sample_02 content outside the viewport.
- Added stored window/viewport canvas origins captured when non-placeable window/viewport extents are set, matching AwtGdi's canvas sizing behavior.
- Added a regression test for the sample_02-style offset-window-origin sequence.
- Regenerated/inspected sample_02.svg; its viewBox is now "0 0 200 200" and the SVG renders visibly.
- Verified with focused SvgGdi/WmfGdi tests and the full Maven test suite.
