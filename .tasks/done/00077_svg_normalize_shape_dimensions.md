# SvgGdi Normalize Shape Dimensions

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: normalize direct SVG dimensions for rectangle-style shapes.

## Context
- Existing local changes from tasks 00064 through 00076 are present and must be preserved.
- AwtGdi draws rectangles, round rectangles, and ellipses from normalized `min` coordinates and absolute dimensions.
- SvgGdi currently writes direct SVG `width`, `height`, `rx`, and `ry` from signed logical deltas, which can produce invalid negative SVG attributes when points are reversed or mapping flips.
- Current-path geometry should remain unchanged because path points can represent reversed drawing naturally.
- This is geometry normalization only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi rectangle-style dimension handling.
   Next step: normalize direct SVG attributes in SvgGdi.
   Required context: preserve current-path behavior and valid positive-delta output.
2. Status: completed. Update SvgGdi `rectangle`, `roundRect`, and `ellipse`.
   Next step: use transformed corner min/max and absolute radii.
   Required context: do not introduce helper churn unless needed for readability.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert reversed coordinates produce non-negative SVG dimensions/radii.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00077_svg_normalize_shape_dimensions.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Avoid invalid negative SVG dimensions for rectangle-style shapes.
- Match AwtGdi's normalized rectangle-style geometry behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's normalized rectangle-style geometry for direct SVG `rectangle`, `roundRect`, and `ellipse` output.
- Reversed coordinates now produce min x/y and positive width/height/radii instead of invalid negative SVG attributes.
- Added SvgGdi coverage for reversed rectangle, ellipse, and round rectangle coordinates.
- Did not change current-path geometry, SVG XOR/filter limitations, or font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
