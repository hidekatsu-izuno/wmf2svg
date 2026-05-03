# SvgGdi Polygon Input Guards

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make polygon-style methods tolerate null and empty point inputs.

## Context
- Existing local changes from tasks 00064 through 00074 are present and must be preserved.
- AwtGdi treats null or empty inputs to `polygon` and `polyline` as no-op via `toPolygon` / `toPolyline`.
- AwtGdi treats null or empty `polyPolygon` input as no-op, and its path append helper ignores null subarrays.
- SvgGdi currently passes some null or empty inputs into SVG point conversion loops, which can throw or emit empty geometry.
- This is input guarding only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi polygon-style input handling.
   Next step: add no-op guards in SvgGdi.
   Required context: preserve normal non-empty geometry behavior and current-position updates.
2. Status: completed. Update SvgGdi `polygon`, `polyline`, and `polyPolygon`.
   Next step: return for null/empty top-level inputs and skip null/empty polyPolygon parts.
   Required context: avoid changing existing output for valid inputs.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert null/empty polygon-style calls do not throw or emit empty geometry.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00075_svg_polygon_input_guards.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Match AwtGdi no-op behavior for invalid polygon-style inputs.
- Avoid malformed empty SVG geometry.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi no-op behavior for null/empty `polygon`, `polyline`, and `polyPolygon` inputs.
- Skipped null/empty `polyPolygon` parts and avoided emitting an empty `<path d="">`.
- Added SvgGdi coverage for null/empty polygon-style inputs.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
