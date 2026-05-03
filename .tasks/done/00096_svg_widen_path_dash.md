# SvgGdi Widen Path Dash

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: preserve selected pen dash style in SvgGdi's `WidenPath` approximation.

## Context
- Existing local changes from tasks 00064 through 00095 are present and must be preserved.
- AwtGdi implements `widenPath()` by creating a stroked shape from the current path using the selected pen, including dash style.
- SvgGdi approximates widened paths by rendering the original path as a wide stroke painted with the selected brush, and uses the same approximation for widened path clipping.
- That approximation currently omits `stroke-dasharray`, so a dashed widened path becomes continuous.
- This task should keep the existing widened-path approximation and only carry over dash style where SVG can express it.
- This task must not attempt exact stroked-region geometry, exact SVG XOR composition, or font-file based measurement.

## Tasks
1. Status: completed. Compare AwtGdi `widenPath()` pen handling with SvgGdi widened path output.
   Next step: add dash attribute helper for widened SVG paths.
   Required context: use SvgGdi/SvgPen's existing dash scale conventions for consistency with normal SVG pen output.
2. Status: completed. Update widened path rendering and widened path clipping to apply dash arrays.
   Next step: add focused tests.
   Required context: leave solid and null pens unchanged.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: assert dashed widened fill path and widened clip path include `stroke-dasharray`.
4. Status: completed. Run focused and full Maven tests.
   Next step: append completion summary.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00096_svg_widen_path_dash.md`.
   Next step: finish.
   Required context: mention preserved XOR and font-file constraints.

## Goals
- Preserve dash style when `WidenPath` is used before `FillPath` / `StrokeAndFillPath`.
- Preserve dash style when `WidenPath` is used before `SelectClipPath`.
- Keep existing SvgGdi widened path approximation scoped and predictable.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Added widened-path dash handling so SvgGdi's `WidenPath` approximation preserves selected dashed pen styles.
- Applied dash arrays both to widened path rendering (`FillPath` / `StrokeAndFillPath`) and widened path clipping (`SelectClipPath`).
- Kept the existing SVG approximation of widened geometry; no exact stroked-region conversion was introduced.
- Added focused SvgGdi tests for dashed widened fill paths and dashed widened clip paths.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
