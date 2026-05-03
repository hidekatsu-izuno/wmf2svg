purpose
- Align SvgGdi color space selection bookkeeping with AwtGdi without changing SVG rendering semantics.

context
- AwtGdi stores the selected color space as the generic GdiColorSpace and returns the previously selected object from setColorSpace.
- SvgGdi currently only accepts SvgColorSpace in setColorSpace, so foreign GdiColorSpace objects are not tracked even though GDI bookkeeping should remember them.
- SVG output does not use color spaces for exact rendering here, and this task must not introduce XOR emulation or font-file dependencies.

tasks
- [x] Update SvgDc to store and return GdiColorSpace instead of SvgColorSpace for the selected color space.
- [x] Update SvgGdi.setColorSpace so it records any GdiColorSpace and returns the previous selected color space.
- [x] Add tests covering save/restore, foreign color space selection, and deleteColorSpace clearing/return behavior.
- [x] Run focused SvgGdi tests. First run failed because new tests called setColorSpace before SvgGdi.header initialized dc; adjusted tests to match existing SvgGdi lifecycle. Rerun passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`.
- [x] Run the full Maven test suite. Passed with `mvn -q test`.

goals
- setColorSpace accepts SvgColorSpace and foreign GdiColorSpace consistently with AwtGdi bookkeeping.
- SaveDC/RestoreDC restores selected color space by identity through SvgDc cloning.
- deleteColorSpace clears the selected object by identity while only reporting success for SvgColorSpace.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete
- next step: none

summary
- SvgDc now stores the selected color space as GdiColorSpace, matching AwtGdi bookkeeping rather than limiting the state to SvgColorSpace.
- SvgGdi.setColorSpace now records any GdiColorSpace and returns the previously selected object by identity.
- Added SvgGdi tests for SaveDC/RestoreDC color space restoration, foreign GdiColorSpace tracking, and deleteColorSpace success/clearing behavior.
- Kept SVG rendering constraints unchanged: no XOR emulation changes and no font-file or local font-metrics dependency.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
