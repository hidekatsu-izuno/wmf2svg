purpose
- Align SvgGdi integer-coordinate GDI drawing with actual Win32 GDI device-coordinate rasterization instead of preserving fractional mapped coordinates where that is not compatible.

context
- User asked to verify all places where AwtGdi/SvgGdi preserve fractional coordinates and fix them unless there is a special reason not to.
- Recent direct Win32 GDI live probing showed anisotropic-mapped half-coordinate Polyline and GradientFill output lands on integer device pixels after GDI rasterization.
- Many WMF/EMF GDI records store integer logical coordinates; mapping can produce fractional intermediate values, but GDI drawing APIs rasterize through device pixels.
- EMF+ records use floating point/page transforms and are out of scope unless a GDI integer record path shares the same formatter.
- Text/font metrics and exact XOR compositing must not be changed.

tasks
- [x] Enumerate SvgGdi integer-coordinate GDI output paths that emit fractional SVG coordinates.
- [x] Extend direct Win32 GDI live probes for representative primitives.
- [x] Add integer device-coordinate helpers for GDI logical coordinates where needed.
- [x] Update immediate line/path/polybezier/polypolygon/setpixel/floodfill/gradient triangle or other confirmed integer GDI paths to use integer device coordinates.
- [x] Keep EMF+ and other true floating-coordinate paths unchanged.
- [x] Update or remove subpixel-preservation tests that conflict with Win32 GDI behavior.
- [x] Add regression tests for integer-rounded SVG output under anisotropic mapping.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- SVG output for integer-coordinate GDI records does not introduce fractional coordinates when Win32 GDI would rasterize to integer device pixels.
- Existing confirmed-good fixes remain: empty selectClipPath handling and gradientFill null guards.
- EMF+ floating coordinate rendering remains untouched.
- Verification evidence and decisions are recorded in this task file.

file list
- .tasks/00131_svg_round_gdi_integer_coordinates.md
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Used direct Win32 GDI live probes from task 00130 as the basis for integer logical-coordinate mapping: transformed half coordinates rasterize to rounded device pixels.
- Added SvgGdi toDeviceX/toDeviceY helpers using Math.round over mapped device coordinates.
- Routed integer-coordinate GDI SVG output through the helpers for rectangles, lines, paths, polybeziers, polypolygons, arc/chord/pie endpoints, region frames, gradient vertices, setPixel/flood-fill seeds, text positions, and bitmap placement anchors.
- Kept EMF+ floating-coordinate paths untouched.
- Updated SvgGdi tests from subpixel-preservation expectations to rounded-device-coordinate expectations.
- Verified with focused SvgGdi tests and the full Maven test suite.
