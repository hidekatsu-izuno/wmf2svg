# SvgGdi Normalize Clip Rectangles

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: normalize mask rectangles used by clip operations.

## Context
- Existing local changes from tasks 00064 through 00079 are present and must be preserved.
- AwtGdi uses `toRectangle(left, top, right - left, bottom - top)` for `intersectClipRect` and `excludeClipRect`, normalizing reversed rectangles.
- SvgGdi currently writes mask `<rect>` attributes from signed logical deltas in `intersectClipRect` and `excludeClipRect`, which can produce invalid negative SVG `width` / `height`.
- This is mask rectangle geometry normalization only; it does not alter clip-combine semantics, SVG XOR/filter limitations, or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi clip rectangle handling.
   Next step: normalize mask rect attributes in SvgGdi.
   Required context: preserve mask fill colors and grouping behavior.
2. Status: completed. Update SvgGdi `intersectClipRect` and `excludeClipRect`.
   Next step: use transformed opposite corners and positive dimensions for mask rects.
   Required context: reuse the existing rectangle-attribute helper.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert reversed clip rects produce positive SVG dimensions.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00080_svg_normalize_clip_rects.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Avoid invalid negative SVG mask rect dimensions for clip operations.
- Match AwtGdi's normalized rectangle behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Reused the normalized rectangle attribute helper for clip mask rectangles.
- Matched AwtGdi's normalized rectangle handling for `intersectClipRect` and `excludeClipRect`.
- Added SvgGdi coverage for reversed clip rectangles.
- Did not change clip-combine semantics, SVG XOR/filter limitations, or font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
