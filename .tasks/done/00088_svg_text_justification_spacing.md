# SvgGdi Text Justification Spacing

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: preserve signed and fractional text justification spacing in SVG output.

## Context
- Existing local changes from tasks 00064 through 00087 are present and must be preserved.
- AwtGdi stores `setTextJustification` values and applies signed device-space extra spacing to space characters.
- SvgGdi approximates this with SVG `word-spacing`, which can represent signed values without font-file metrics.
- SvgGdi currently uses `Math.abs((int) dc.toRelativeX(breakExtra)) / breakCount`, losing negative compression and fractional spacing.
- This task keeps the existing SVG approximation but preserves sign and fractional values.
- This does not attempt exact SVG XOR behavior or introduce font-file based metrics.

## Tasks
1. Status: completed. Compare AwtGdi text justification spacing with SvgGdi `word-spacing` handling.
   Next step: update SvgDc/SvgGdi spacing type and calculation.
   Required context: preserve existing SVG approximation and save/restore cloning.
2. Status: completed. Update SvgDc text spacing storage.
   Next step: update SvgGdi spacing calculation and style output.
   Required context: no public API depends on the concrete type.
3. Status: completed. Update SvgGdi `setTextJustification` and style output.
   Next step: add focused tests.
   Required context: `breakCount <= 0` should reset spacing to zero.
4. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: string assertions are sufficient.
5. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
6. Status: completed. Append completion summary and move to `.tasks/done/00088_svg_text_justification_spacing.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Preserve signed text justification spacing in SvgGdi.
- Avoid integer truncation for fractional spacing values.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Changed SvgDc text spacing storage from `int` to `double` so fractional justification spacing is preserved.
- Updated SvgGdi `setTextJustification` to keep the signed `dc.toRelativeX(breakExtra) / breakCount` value instead of applying `abs` and integer truncation.
- Updated `textOut` and `extTextOut` style generation to format nonzero `word-spacing` values.
- Added SvgGdi tests covering negative fractional and positive fractional text justification spacing.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
