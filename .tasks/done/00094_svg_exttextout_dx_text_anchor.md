# SvgGdi ExtTextOut Dx Text Anchor

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: avoid applying SVG `text-anchor` when explicit horizontal `extTextOut` coordinate lists already encode alignment.

## Context
- Existing local changes from tasks 00064 through 00093 are present and must be preserved.
- AwtGdi explicit-`dx` text drawing uses the supplied advances to place character origins after applying alignment to the drawing origin.
- SvgGdi explicit horizontal `dx` output already computes per-character SVG `x` origin coordinate lists with alignment offsets; it does not compute glyph outlines or font-derived character widths.
- SvgGdi still emits `text-anchor: end` / `middle` for `TA_RIGHT` / `TA_CENTER`, which can make the SVG renderer apply an additional font-dependent anchor adjustment to coordinate-list text.
- This task should preserve no-`dx`, vertical, `ETO_PDY`, and `TA_UPDATECP` behavior.
- This does not attempt exact SVG XOR behavior or introduce font-file based metrics.

## Tasks
1. Status: completed. Compare AwtGdi explicit-`dx` direct positioning with SvgGdi coordinate-list plus `text-anchor` output.
   Next step: suppress `text-anchor` for explicit horizontal coordinate-list output.
   Required context: coordinate-list x positions already include alignment offsets.
2. Status: completed. Update SvgGdi `extTextOut` style generation.
   Next step: add focused tests.
   Required context: keep no-`dx` `TA_RIGHT` / `TA_CENTER` anchor output unchanged.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00094_svg_exttextout_dx_text_anchor.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Avoid double alignment for explicit horizontal `dx` `extTextOut` SVG output.
- Preserve existing no-`dx` anchor behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated SvgGdi `extTextOut` style generation so explicit horizontal `dx` coordinate-list output does not also emit `text-anchor: end` / `middle`.
- This uses advances supplied by the metafile to place SVG text origins; it does not compute glyph widths from local font files.
- Preserved no-`dx`, vertical, `ETO_PDY`, and `TA_UPDATECP` behavior.
- Added SvgGdi coverage asserting aligned explicit-`dx` x coordinates remain while `text-anchor: end` is omitted.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
