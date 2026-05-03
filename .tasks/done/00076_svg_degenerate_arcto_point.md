# SvgGdi Degenerate ArcTo Point

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: match `arcTo` current-position behavior for degenerate arc frames.

## Context
- Existing local changes from tasks 00064 through 00075 are present and must be preserved.
- AwtGdi's `getArcPoint` projects points onto the arc frame formula even when the frame width or height is zero; this results in the frame center.
- SvgGdi's `getArcPoint` currently returns the input point when the frame width or height is zero.
- `arc` itself remains a no-op for zero-width/zero-height frames in both implementations.
- This is geometry state bookkeeping only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `getArcPoint` behavior for degenerate arc frames.
   Next step: update SvgGdi's degenerate point calculation.
   Required context: preserve normal non-degenerate arc behavior.
2. Status: completed. Update SvgGdi `getArcPoint`.
   Next step: return the frame center when either radius is zero.
   Required context: keep `arc` no-op guard unchanged.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert degenerate `arcTo` connects to the center and updates current position there.
   Required context: use a following `lineTo` to observe current-position state.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00076_svg_degenerate_arcto_point.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Match AwtGdi degenerate `arcTo` point projection.
- Avoid surprising lines to raw arc control points when the arc frame has no area.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's degenerate `arcTo` point projection by returning the arc frame center when either radius is zero.
- Kept `arc` no-op behavior for zero-width/zero-height frames unchanged.
- Added SvgGdi coverage verifying degenerate `arcTo` connects to and leaves the current position at the frame center.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
