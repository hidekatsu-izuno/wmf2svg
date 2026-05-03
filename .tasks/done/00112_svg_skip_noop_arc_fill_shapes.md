purpose
- Suppress no-op immediate SvgGdi chord and pie output when selected styles cannot render.

context
- AwtGdi chord and immediate pie call drawShape, which fills with the selected brush and strokes with the selected pen.
- AwtGdi fillShape skips null, BS_NULL, and BS_HOLLOW brushes; strokeShape skips null and PS_NULL pens.
- SvgGdi chord and immediate pie currently emit SVG shape/path elements even when both selected styles are non-renderable.
- SvgGdi pie path recording must remain active before immediate no-op checks, because beginPath + pie should record geometry for later path operations.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Add no-op selected-style guards to immediate SvgGdi chord and pie output.
- [x] Preserve SvgGdi pie currentPath recording behavior.
- [x] Add SvgGdi regression tests for no-op chord/pie and visible single-side rendering.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- chord emits no SVG element when selected pen and brush are both no-op.
- immediate pie emits no SVG element when selected pen and brush are both no-op.
- chord and immediate pie still emit output when either pen or brush is renderable.
- beginPath + pie continues recording path geometry regardless of selected no-op styles.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Added no-op selected-style guards to immediate SvgGdi chord and pie output, matching AwtGdi drawShape behavior when both fill and stroke are non-renderable.
- Preserved SvgGdi pie currentPath recording by applying the immediate-output guard only after the currentPath branch.
- Added regression tests for no-op chord/pie, visible fill-only chord/pie, and path pie recording with no-op selected styles.
- Verified with focused SvgGdi tests and the full Maven test suite.
