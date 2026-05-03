purpose
- Align SvgGdi PatBlt handling for pattern ROPs with AwtGdi null/hollow brush behavior.

context
- AwtGdi detects ROPs that use the pattern input and returns early when the selected brush is null/hollow.
- SvgGdi currently emits a rect with fill="none" for PatBlt when no renderable brush is selected, including PATCOPY/PATINVERT.
- This task improves SVG output cleanliness and GDI behavior consistency without changing exact XOR limitations or font behavior.

tasks
- [x] Add bitmap ROP pattern detection in SvgGdi matching AwtGdi's ROP3 logic.
- [x] Update patBlt to skip pattern ROPs when the selected brush is null/hollow.
- [x] Add SvgGdi tests for PATCOPY and PATINVERT with a null brush.
- [x] Run focused SvgGdi tests. Passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`.
- [x] Run the full Maven test suite. Passed with `mvn -q test`.

goals
- SvgGdi does not emit visible or no-op rects for pattern PatBlt operations with null/hollow brushes.
- Existing PatBlt behavior with renderable brushes remains intact.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete
- next step: none

summary
- SvgGdi patBlt now detects pattern-using ROP3 values with the same nibble comparison used by AwtGdi.
- Pattern PatBlt operations now return early when the selected brush is null/hollow, avoiding no-op SVG rect/filter output.
- Added tests for PATCOPY and PATINVERT with a null brush.
- Kept SVG constraints unchanged: no exact XOR emulation changes and no font-file dependency.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
