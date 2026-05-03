purpose
- Preserve SvgGdi gradient rectangle subpixel coordinates to better match AwtGdi GradientPaint rendering.

context
- AwtGdi gradient rectangles use Rectangle2D and GradientPaint, preserving mapped fractional coordinates.
- SvgGdi gradient rectangles currently cast mapped coordinates and dimensions to int for both the gradient vector and rect bounds.
- SvgGdi gradient triangle coordinates already use double mapped coordinates.
- This task should not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Add null guards to SvgGdi gradientFill overloads to match AwtGdi behavior.
- [x] Preserve fractional coordinates for SVG gradient rectangle vectors.
- [x] Preserve fractional coordinates and dimensions for SVG gradient rectangle bounds.
- [x] Add focused SvgGdi regression tests for subpixel gradient rectangle output and null inputs.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- Gradient rectangle SVG output keeps fractional mapped coordinates under anisotropic mapping.
- Null gradient arrays are ignored without throwing.
- Existing invalid vertex index behavior remains guarded.
- Existing triangle gradient behavior remains unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Added null guards to both SvgGdi gradientFill overloads to match AwtGdi's ignore-null behavior.
- Changed SVG gradient rectangle vectors from integer casts to formatDouble mapped coordinates.
- Changed SVG gradient rectangle bounds and dimensions from integer casts to formatDouble mapped values.
- Added regression tests for subpixel gradient rectangle output and null gradient inputs.
- Verified with focused SvgGdi tests and the full Maven test suite.

superseded
- Direct Win32 GDI probing in task 00130 showed that preserving fractional mapped coordinates is not clearly supported for GDI-compatible output.
- The gradient rectangle fractional-coordinate code and subpixel test from this task were reverted before commit.
- The null input guards and null-input regression test remain because they match AwtGdi behavior independently of coordinate rounding.
