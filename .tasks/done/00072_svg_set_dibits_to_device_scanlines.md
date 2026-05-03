# SvgGdi SetDIBitsToDevice Scanlines

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make `setDIBitsToDevice` honor `startscan` and `scanlines`.

## Context
- Existing local changes from tasks 00064 through 00071 are present and must be preserved.
- AwtGdi decodes the DIB, clamps source X/width/height, computes the source Y from `startscan`, `scanlines`, and `sy`, then draws only that subimage.
- SvgGdi currently validates some bounds and delegates to `stretchDIBits`, which ignores `startscan`.
- SVG can represent this by embedding the converted PNG and using an SVG viewport/viewBox crop.
- This change must not alter exact XOR/filter limitations or font-file dependencies.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `setDIBitsToDevice` behavior.
   Next step: implement scanline-aware source cropping in SvgGdi.
   Required context: match AwtGdi's clamp behavior where practical.
2. Status: completed. Update SvgGdi `setDIBitsToDevice`.
   Next step: compute srcX/srcY/srcW/srcH and append cropped PNG output.
   Required context: use existing DIB conversion and `appendPngToSvg`.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: create a 2-row DIB and assert `startscan` selects the bottom row.
   Required context: assert SVG viewport crop because the PNG is embedded once and cropped by viewBox.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00072_svg_set_dibits_to_device_scanlines.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Honor `startscan` and `scanlines` in SvgGdi `setDIBitsToDevice`.
- Keep normal full-image behavior intact.
- Verify with deterministic embedded PNG assertions.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's `SetDIBitsToDevice` scanline handling in SvgGdi.
- SvgGdi now computes source X/Y/width/height using `startscan`, `scanlines`, and `sy`, converts the DIB to PNG, and emits a cropped SVG viewport.
- Added a SvgGdi test using a two-row top-down DIB to verify `startscan=0` selects the bottom scanline via `viewBox`.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
