# SvgGdi Normalize ExtTextOut Rectangles

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: normalize `extTextOut` background and clip rectangles.

## Context
- Existing local changes from tasks 00064 through 00078 are present and must be preserved.
- AwtGdi uses `toRectangle(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1])` for `ETO_OPAQUE` and `ETO_CLIPPED`, normalizing reversed rectangles.
- SvgGdi currently writes `extTextOut` background and clip SVG rect attributes from signed logical deltas, which can produce invalid negative `width` / `height`.
- AwtGdi only applies the clip when the supplied rect is non-null; SvgGdi should avoid clipping when `ETO_CLIPPED` has no rect.
- This is rectangle geometry and null-guard normalization only; it does not alter text font fallback, SVG XOR/filter limitations, or font-file handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `extTextOut` rectangle handling.
   Next step: normalize direct SVG rect attributes in SvgGdi.
   Required context: preserve text placement and background/clip grouping.
2. Status: completed. Update SvgGdi `extTextOut` rect creation.
   Next step: use transformed opposite corners and positive dimensions for background and clip rects.
   Required context: keep estimated background behavior for null opaque rects.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert reversed `extTextOut` rects produce positive SVG dimensions.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00079_svg_normalize_exttextout_rect.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Avoid invalid negative SVG rect dimensions for `extTextOut` background and clip rectangles.
- Match AwtGdi's normalized rectangle behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's normalized rectangle handling for `extTextOut` `ETO_OPAQUE` and `ETO_CLIPPED` rectangles.
- Reversed text background and clip rectangles now produce min x/y and positive SVG width/height.
- Guarded `ETO_CLIPPED` so null rects do not create a clip, matching AwtGdi's `rect != null` condition.
- Added SvgGdi coverage for reversed `extTextOut` opaque and clip rectangles.
- Did not change text font fallback, SVG XOR/filter limitations, or font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
