purpose
- Preserve SvgGdi polyline subpixel coordinates to better match AwtGdi Path2D.Double polyline rendering.

context
- AwtGdi polygon uses java.awt.Polygon and truncates mapped points to integers.
- AwtGdi polyline uses Path2D.Double and keeps mapped fractional coordinates.
- SvgGdi currently uses toSvgPoints for both polygon and polyline, which truncates mapped coordinates to integers.
- This task should change polyline output only; polygon should keep integer behavior to match AwtGdi.
- This task does not change exact XOR behavior and does not introduce font-file or font-metrics dependencies.

tasks
- [x] Add a SvgGdi polyline point formatter that preserves fractional mapped coordinates.
- [x] Use the fractional formatter for immediate polyline output and duplicate normalization.
- [x] Keep polygon and polyPolygon coordinate behavior unchanged.
- [x] Add focused SvgGdi regression tests for subpixel polyline coordinates and polygon contrast.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- Polyline SVG points preserve fractional coordinates under anisotropic mapping.
- Polygon SVG points remain integer coordinates under the same mapping.
- Existing outline-only polygon optimization still works for integer-mapped matching points.
- Existing non-polyline behavior remains unchanged.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Added a polyline-specific SVG point formatter that preserves fractional mapped coordinates.
- Switched immediate SvgGdi polyline output and normalized polyline matching to the new formatter.
- Kept polygon and polyPolygon coordinate behavior unchanged, preserving AwtGdi's integer polygon behavior.
- Added regression tests showing polyline keeps subpixel coordinates while polygon remains integer-mapped.
- Verified with focused SvgGdi tests and the full Maven test suite.

superseded
- Direct Win32 GDI probing in task 00130 showed that preserving fractional mapped coordinates is not clearly supported for GDI-compatible output.
- The polyline fractional-coordinate code and tests from this task were reverted before commit.
