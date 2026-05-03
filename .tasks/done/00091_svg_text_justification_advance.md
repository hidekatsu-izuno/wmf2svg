# SvgGdi Text Justification Advance

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: include SVG text-justification spacing in estimated text advances used for backgrounds and `TA_UPDATECP`.

## Context
- Existing local changes from tasks 00064 through 00090 are present and must be preserved.
- AwtGdi includes text justification extra spacing in text advances, so it affects bounds and `TA_UPDATECP` current-position updates.
- SvgGdi task 00088 preserves signed/fractional justification as SVG `word-spacing`, but width/current-position estimates still ignore the added spacing.
- SvgGdi should continue using existing estimates and SVG text; no font-file metrics should be introduced.
- This does not attempt exact SVG XOR behavior.

## Tasks
1. Status: completed. Compare AwtGdi text justification advance handling with SvgGdi estimates.
   Next step: add spacing to SvgGdi estimated advances.
   Required context: preserve signed/fractional `word-spacing` from task 00088.
2. Status: completed. Update SvgGdi text advance estimates.
   Next step: add focused tests.
   Required context: keep broader vertical and `ETO_PDY` behavior unchanged.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00091_svg_text_justification_advance.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Make SvgGdi `TA_UPDATECP` text advancement account for text justification spacing.
- Keep SVG `word-spacing` output and estimated layout in better agreement.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Added SvgGdi helpers to compute text justification advance from spaces using the existing signed/fractional `word-spacing` value.
- Included that advance in no-`dx` `textOut` / `extTextOut` estimated widths and `TA_UPDATECP` current-position updates.
- Included that advance in explicit horizontal `dx` `extTextOut` stepping and current-position updates.
- Preserved broader vertical and `ETO_PDY` behavior.
- Added SvgGdi tests for no-`dx` and explicit-`dx` text justification advancement.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
