# SvgGdi Current Position After Polylines

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: keep the current position in sync after polyline-style drawing.

## Context
- Existing local changes from tasks 00064 through 00073 are present and must be preserved.
- AwtGdi advances the DC current position after `polyline` and after `polyBezierTo`, including when drawing into a current path.
- SvgGdi already advances the current position for normal `polyBezierTo` through `appendBezier`, but it does not advance it for `polyline` or for `polyBezierTo` while a current path is active.
- This is DC state bookkeeping only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi current-position behavior for `polyline` and `polyBezierTo`.
   Next step: update SvgGdi state after successful polyline/path-bezier operations.
   Required context: preserve existing SVG element output and path command output.
2. Status: completed. Update SvgGdi `polyline`.
   Next step: move current position to the last point when the input has at least one point.
   Required context: apply both current-path and direct SVG branches.
3. Status: completed. Update SvgGdi current-path `polyBezierTo`.
   Next step: move current position to the last point when the input has at least one point.
   Required context: normal direct `polyBezierTo` already updates via `appendBezier`.
4. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert a following `lineTo` starts from the updated point for `polyline` and path `polyBezierTo`.
   Required context: use SVG string assertions.
5. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
6. Status: completed. Append completion summary and move to `.tasks/done/00074_svg_current_position_after_polylines.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Match AwtGdi current-position side effects for polyline-style operations.
- Keep the change small, local, and regression-tested.
- Preserve existing SVG output model, XOR constraints, and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi current-position bookkeeping after `polyline` in SvgGdi.
- Matched AwtGdi current-position bookkeeping after current-path `polyBezierTo` in SvgGdi.
- Added SvgGdi tests verifying following `lineTo` starts from the updated current position.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
