# SvgGdi Flood Fill Seed Mapping

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: map the `extFloodFill` seed rectangle as one logical pixel.

## Context
- Existing local changes from tasks 00064 through 00081 are present and must be preserved.
- AwtGdi converts the flood-fill seed to device coordinates before filling pixels.
- SvgGdi approximates flood fill by emitting a small seed `<rect>`, but currently writes fixed relative `1x1` dimensions that can be wrong under anisotropic or flipped mapping.
- SvgGdi `setPixel` was already updated to map `(x,y)` and `(x+1,y+1)` and emit a normalized rectangle.
- This is seed-geometry normalization only; it does not implement exact flood-fill raster behavior, SVG XOR/filter behavior, or font handling.

## Tasks
1. Status: completed. Compare AwtGdi flood-fill seed handling and SvgGdi seed output.
   Next step: make SvgGdi seed rect use mapped one-logical-pixel geometry.
   Required context: preserve existing brush/pattern output.
2. Status: completed. Update SvgGdi `appendFloodFillSeed`.
   Next step: compute transformed opposite corners and emit min x/y with positive width/height.
   Required context: match the existing `setPixel` geometry pattern.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert anisotropic mapping produces scaled seed dimensions.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00082_svg_flood_fill_seed_mapping.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Map SvgGdi flood-fill seed rectangles consistently with one logical pixel.
- Avoid invalid negative seed dimensions under flipped mapping.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Mapped SvgGdi flood-fill seed rectangles using transformed `(x,y)` and `(x+1,y+1)` corners.
- The seed rectangle now scales and normalizes like `setPixel` under anisotropic or flipped mapping.
- Added SvgGdi coverage for anisotropic `extFloodFill` seed geometry.
- Did not implement exact raster flood fill, change SVG XOR/filter behavior, or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
