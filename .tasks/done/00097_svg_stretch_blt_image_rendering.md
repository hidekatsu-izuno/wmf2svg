# SvgGdi Stretch Blt Image Rendering

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: reflect `SetStretchBltMode` in SVG bitmap scaling hints.

## Context
- Existing local changes from tasks 00064 through 00096 are present and must be preserved.
- AwtGdi uses `STRETCH_HALFTONE` as bilinear interpolation and non-halftone stretch modes as nearest-neighbor interpolation.
- SvgGdi stores stretch blit mode but currently does not expose that GDI state on normal GDI bitmap `<image>` output.
- SVG cannot guarantee identical raster interpolation across viewers, but `image-rendering` can express the intended quality hint.
- This task should not affect EMF+ image interpolation handling, exact SVG XOR behavior, or font-file based measurement.

## Tasks
1. Status: completed. Compare AwtGdi stretch blit interpolation handling with SvgGdi bitmap output.
   Next step: add a helper that maps GDI stretch mode to SVG `image-rendering`.
   Required context: `STRETCH_HALFTONE` should use `optimizeQuality`; other GDI stretch modes should use `pixelated`.
2. Status: completed. Apply the helper in normal GDI PNG image output.
   Next step: add focused tests.
   Required context: handle both direct `<image>` output and clipped-source nested image output.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: assert default and halftone stretch modes emit distinct `image-rendering` hints.
4. Status: completed. Run focused and full Maven tests.
   Next step: append completion summary.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00097_svg_stretch_blt_image_rendering.md`.
   Next step: finish.
   Required context: mention preserved XOR and font-file constraints.

## Goals
- Preserve GDI stretch mode intent in SvgGdi bitmap scaling output.
- Keep EMF+ interpolation handling separate and unchanged.
- Preserve SVG XOR limitations and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Added normal GDI bitmap `image-rendering` hints based on `SetStretchBltMode`.
- `STRETCH_HALFTONE` now emits `image-rendering="optimizeQuality"`; other stretch modes emit `image-rendering="pixelated"`.
- Applied the hint to both direct `<image>` output and clipped-source nested image output.
- Kept EMF+ image interpolation handling separate and unchanged.
- Added focused SvgGdi tests for default stretch mode, halftone stretch mode, and clipped-source output.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
