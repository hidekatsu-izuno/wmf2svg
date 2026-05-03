# SvgGdi Normalize Rect Region

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: normalize rectangular regions created with reversed coordinates.

## Context
- Existing local changes from tasks 00064 through 00080 are present and must be preserved.
- AwtGdi creates rectangular regions through `toRectangle(left, top, right - left, bottom - top)`, normalizing reversed coordinates.
- SvgRectRegion currently emits SVG `width` and `height` as `right - left` and `bottom - top`, which can be negative.
- This is region rectangle geometry normalization only; it does not alter clip-combine semantics, SVG XOR/filter limitations, or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi rectangular region handling.
   Next step: normalize SvgRectRegion coordinates.
   Required context: preserve existing normal-coordinate output.
2. Status: completed. Update SvgRectRegion.
   Next step: store min/max coordinates or emit min/max attributes with positive dimensions.
   Required context: keep equality/hash behavior consistent with stored geometry.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert selecting a reversed rectangular region creates a positive-dimension mask rect.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00081_svg_normalize_rect_region.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Avoid invalid negative SVG dimensions for rectangular regions.
- Match AwtGdi's normalized rectangle-region behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgRectRegion.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's normalized rectangle-region behavior by storing min/max coordinates in `SvgRectRegion`.
- Reversed rectangular regions now emit positive SVG `width` and `height`.
- Added SvgGdi coverage for selecting a reversed rectangular region as a clip mask.
- Did not change clip-combine semantics, SVG XOR/filter limitations, or font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
