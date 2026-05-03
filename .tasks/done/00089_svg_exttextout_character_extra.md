# SvgGdi ExtTextOut Character Extra

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make no-`dx` `extTextOut` honor `setTextCharacterExtra`.

## Context
- Existing local changes from tasks 00064 through 00088 are present and must be preserved.
- AwtGdi applies `dc.getTextCharacterExtra()` to text advances even when `ExtTextOut` has no explicit `lpdx`.
- SvgGdi `textOut` already emits an SVG `dx` list for `setTextCharacterExtra`.
- SvgGdi no-`dx` `extTextOut` currently estimates the base width but does not emit `dx` or advance current position by the extra spacing.
- SvgGdi should continue using existing text-width estimates and must not introduce font-file metrics.
- This does not attempt exact SVG XOR behavior.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `setTextCharacterExtra` handling for text output.
   Next step: update no-`dx` SvgGdi `extTextOut`.
   Required context: preserve explicit `dx` behavior for now.
2. Status: completed. Update SvgGdi no-`dx` `extTextOut`.
   Next step: add focused tests.
   Required context: match existing `textOut` approximation and keep font-file independence.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00089_svg_exttextout_character_extra.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Make no-`dx` SvgGdi `extTextOut` honor `setTextCharacterExtra`.
- Preserve existing explicit `dx` behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Added a shared SvgGdi helper for emitting character-extra SVG `dx` lists using the existing relative coordinate conversion.
- Updated no-`dx` `extTextOut` to emit `dx` for `setTextCharacterExtra` and include the extra spacing in width/current-position estimates.
- Kept explicit `dx` `extTextOut` behavior unchanged.
- Updated `textOut` estimated advance to use the signed character extra, aligning better with AwtGdi's signed advance handling.
- Added SvgGdi coverage for no-`dx` `extTextOut` with `setTextCharacterExtra` and `TA_UPDATECP`.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
