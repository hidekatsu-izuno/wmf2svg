purpose
- Check whether recent AWT fixes also apply to `SvgGdi`, and implement matching SVG fixes if needed.

context
- Recent AWT fixes:
  - `patBlt` direct pixel path now accounts for non-zero canvas origin.
  - standalone EMF header with negative `frameTop` places extra frame-derived pixel above the bounds.
- User asks whether equivalent `SvgGdi` changes are needed.

tasks
- [x] Inspect `SvgGdi` EMF header, bounds/viewBox, and PATCOPY/patBlt handling.
- [x] Compare behavior against the recent AWT fixes and affected files.
- [x] Implement `SvgGdi` changes if the same issue exists.
- [x] Add/update focused tests if behavior changes.
- [x] Run focused and relevant full tests.

goals
- Determine whether `SvgGdi` needs changes.
- If changed, keep SVG and PNG origin/frame semantics consistent.
- Preserve existing SVG output behavior outside the affected cases.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java` if needed

status
- Current status: completed.
- Next step: none.
- Required context to continue: AWT changes are uncommitted and should not be reverted.

summary
- `patBlt` origin fix does not need a direct SVG counterpart because `SvgGdi.patBlt` emits logical SVG rectangles instead of writing directly into a raster backing image.
- The negative `frameTop` EMF header fix does apply to `SvgGdi`; updated `emfHeader` to move the SVG canvas Y origin upward by the extra frame-derived height when `frameTop < 0`.
- Added `testStandaloneEmfHeaderCanvasPlacesExtraFramePixelAboveNegativeFrame` to verify `viewBox="300 -617 191 116"` while the actual rectangle remains at `y="-616"`.
- Verification passed: focused `SvgGdiTest`, focused `AwtGdiTest`, and `mvn -q test`.
