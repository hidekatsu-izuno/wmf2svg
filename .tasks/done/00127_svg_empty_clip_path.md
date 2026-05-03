purpose
- Match SvgGdi empty path clipping behavior to AwtGdi without relying on font metrics or exact XOR compositing.

context
- AwtGdi selectClipPath consumes currentPath whenever it is non-null, even if the path is empty.
- AwtGdi RGN_COPY with an empty path sets an empty clip, so following drawing is clipped away.
- SvgGdi currently returns early for an empty currentPath, leaving the path active and skipping the empty clip effect.
- SVG still cannot represent exact XOR compositing; this task does not attempt to change XOR behavior.
- SvgGdi must not depend on font files or font metrics.

tasks
- [x] Update SvgGdi selectClipPath to consume empty currentPath.
- [x] Apply an empty SVG mask for empty RGN_COPY/RGN_AND clip path operations.
- [x] Leave empty RGN_OR/RGN_XOR/RGN_DIFF clip path operations visually unchanged while still consuming the path.
- [x] Add focused SvgGdi regression tests for empty clip path consumption and empty copy clipping.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- Empty selectClipPath no longer leaves currentPath active for later path operations.
- Empty RGN_COPY and RGN_AND path clips create an empty mask so subsequent SVG output is clipped.
- Empty OR, XOR, and DIFF path clips do not introduce extra visible clipping.
- Existing non-empty path clipping behavior remains unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Changed SvgGdi selectClipPath so a non-null empty currentPath is consumed like AwtGdi.
- Added empty mask handling for empty RGN_COPY and RGN_AND path clips, so subsequent SVG drawing is clipped by an empty clip.
- Left empty RGN_OR, RGN_XOR, and RGN_DIFF path clips visually unchanged while still clearing currentPath.
- Added regression tests for empty RGN_COPY and RGN_DIFF path clipping behavior.
- Verified with focused SvgGdi tests and the full Maven test suite.
