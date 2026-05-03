# SvgGdi Flatten Path

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make `FlattenPath` affect the stored current path instead of remaining a no-op.

## Context
- Existing local changes from tasks 00064 through 00094 are present and must be preserved.
- AwtGdi implements `flattenPath()` by replacing the current `Path2D` with a flattened path iterator.
- SvgGdi currently leaves `flattenPath()` as a no-op because SVG can render cubic Bezier commands directly.
- Direct SVG Bezier rendering is fine for normal drawing, but GDI `FlattenPath` changes the path data consumed by later operations such as `StrokePath`, `FillPath`, `WidenPath`, and `SelectClipPath`.
- This task should not attempt exact SVG XOR composition, exact stroked-region geometry, or any font-file based measurement.

## Tasks
1. Status: completed. Compare AwtGdi `flattenPath()` behavior with SvgGdi current-path storage.
   Next step: implement path-level flattening in `SvgPath`.
   Required context: flattening only needs geometric Bezier subdivision and should leave line, move, and close commands intact.
2. Status: completed. Update SvgGdi `flattenPath()` to replace cubic Beziers with line segments.
   Next step: add focused tests.
   Required context: use a conservative flatness tolerance similar in spirit to AwtGdi's `0.25`, without introducing external dependencies.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG path output should contain `L` segments and no `C` command after `flattenPath()`.
4. Status: completed. Run focused and full Maven tests.
   Next step: append completion summary.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00095_svg_flatten_path.md`.
   Next step: finish.
   Required context: mention preserved XOR and font-file constraints.

## Goals
- Honor `FlattenPath` as a current-path transformation in SvgGdi.
- Preserve ordinary Bezier SVG output when `FlattenPath` is not called.
- Preserve SVG XOR limitations and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated SvgGdi `flattenPath()` so it transforms the current path instead of remaining a no-op.
- Added `SvgPath.flatten(double)` with recursive cubic Bezier subdivision, preserving move, line, and close commands and replacing Beziers with line segments.
- Added focused SvgGdi coverage asserting a flattened Bezier path emits `L` segments and no `C` commands.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
