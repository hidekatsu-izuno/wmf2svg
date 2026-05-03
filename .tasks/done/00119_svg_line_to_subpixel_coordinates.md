purpose
- Preserve subpixel transformed coordinates in SvgGdi lineTo output.

context
- AwtGdi lineTo draws a Line2D.Double using transformed start and end coordinates.
- SvgGdi lineTo currently truncates transformed line coordinates to integers.
- SVG line attributes can represent fractional coordinates directly.
- This task must keep existing current-position behavior, no-op pen handling, and zero-length line handling unchanged.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Update SvgGdi.lineTo to emit formatted double coordinates.
- [x] Add SvgGdi regression tests for subpixel mapped lineTo coordinates while preserving existing integer coordinate behavior.
- [x] Run focused SvgGdi tests.
- [x] Run the full Maven test suite.

goals
- lineTo preserves fractional transformed x1/y1/x2/y2 values when map scaling produces subpixel coordinates.
- Existing integer-size lineTo output remains stable.
- No-op pen lineTo still updates the current position without output.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

summary
- Updated SvgGdi.lineTo to emit formatted double x1/y1/x2/y2 coordinates instead of truncating transformed values to integers.
- Added a regression test for subpixel mapped lineTo coordinates while preserving existing integer coordinate expectations.
- Kept current-position updates, no-op pen behavior, XOR behavior, and font behavior unchanged.
- Verified with focused SvgGdi tests and the full Maven test suite.
