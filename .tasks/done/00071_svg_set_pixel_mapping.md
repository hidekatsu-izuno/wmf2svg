# SvgGdi SetPixel Mapping

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make `setPixel` honor the current logical-to-device mapping for its 1x1 pixel rectangle.

## Context
- Existing local changes from tasks 00064 through 00070 are present and must be preserved.
- AwtGdi implements `setPixel` by filling `toRectangle(x, y, 1, 1)`, so the logical 1x1 size follows the current mapping transform.
- SvgGdi currently emits `width="1"` and `height="1"` regardless of map/window/viewport scale.
- This change is SVG geometry only; it does not alter XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `setPixel` behavior.
   Next step: implement mapped pixel dimensions in SvgGdi.
   Required context: use existing `dc.toAbsolute*` and `dc.toRelative*` helpers.
2. Status: completed. Update SvgGdi `setPixel`.
   Next step: compute x/y/width/height from transformed `(x,y)` and `(x+1,y+1)`.
   Required context: handle negative extents by normalizing x/y and size.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert anisotropic map scales the emitted setPixel rectangle.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00071_svg_set_pixel_mapping.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Match AwtGdi logical mapping behavior for `setPixel`.
- Keep simple MM_TEXT output unchanged.
- Verify anisotropic mapping behavior.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's `setPixel` mapping behavior in SvgGdi.
- Changed SvgGdi `setPixel` to compute the rectangle from transformed `(x, y)` and `(x + 1, y + 1)`, normalizing negative extents.
- Added a SvgGdi test verifying anisotropic mapping emits a scaled logical pixel rectangle.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
