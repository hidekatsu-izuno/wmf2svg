purpose
- Make SvgGdi widenPath use the pen selected at widen time.

context
- AwtGdi widenPath immediately creates a widened path from currentPath using the currently selected pen.
- SvgGdi currently marks SvgPath as widened and later uses dc.getPen() during fillPath/strokeAndFillPath/selectClipPath output.
- If the selected pen changes after widenPath, SvgGdi can use the wrong width or dash pattern.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Store the selected pen on SvgPath when widenPath is called.
- [x] Use the stored widened pen for widened path fill and clip output.
- [x] Add SvgGdi regression tests for pen changes after widenPath.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- widenPath followed by a pen change still uses the original pen width and dash style.
- widened selectClipPath also uses the original pen.
- Existing widened path behavior remains unchanged when the pen is not changed.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Stored the selected SvgPen on SvgPath when widenPath is called.
- Used the stored widened pen for widened fill output and widened clip path output.
- Added regression tests proving pen width and dash style are taken from the pen selected at widenPath time even if the selected pen changes before fillPath or selectClipPath.
- Kept XOR behavior and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
