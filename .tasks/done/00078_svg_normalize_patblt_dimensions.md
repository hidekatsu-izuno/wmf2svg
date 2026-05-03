# SvgGdi Normalize PatBlt Dimensions

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: normalize `patBlt` rectangle dimensions.

## Context
- Existing local changes from tasks 00064 through 00077 are present and must be preserved.
- AwtGdi uses `toRectangle(x, y, width, height)` for `patBlt`, normalizing transformed corners with min coordinates and absolute dimensions.
- SvgGdi currently writes `x`, `y`, `width`, and `height` from the starting point and signed relative dimensions, which can produce invalid negative SVG attributes.
- This is rectangle geometry normalization only; it does not change ROP/filter semantics, XOR limitations, or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `patBlt` rectangle handling.
   Next step: normalize direct SVG rect attributes in SvgGdi.
   Required context: preserve brush/filter behavior.
2. Status: completed. Update SvgGdi `patBlt`.
   Next step: compute transformed opposite corners and emit min x/y with positive width/height.
   Required context: keep existing class/fill/filter setup unchanged.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert reversed `patBlt` dimensions produce positive SVG attributes.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00078_svg_normalize_patblt_dimensions.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Avoid invalid negative SVG rect dimensions for `patBlt`.
- Match AwtGdi's normalized rectangle geometry behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's normalized rectangle handling for direct SVG `patBlt` output.
- Negative `patBlt` width/height now produce min x/y and positive SVG width/height.
- Added SvgGdi coverage for negative `patBlt` extents.
- Did not change ROP/filter semantics, SVG XOR limitations, or font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
