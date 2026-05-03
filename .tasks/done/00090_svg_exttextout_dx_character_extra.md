# SvgGdi ExtTextOut Dx Character Extra

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make explicit-`dx` horizontal `extTextOut` combine `setTextCharacterExtra` with the supplied advances.

## Context
- Existing local changes from tasks 00064 through 00089 are present and must be preserved.
- AwtGdi's `createTextAdvances` adds `dc.getTextCharacterExtra()` to each text advance even when `lpdx` is supplied.
- SvgGdi explicit-`dx` horizontal `extTextOut` currently uses the supplied `dx` values directly for SVG x positions and current-position updates.
- Task 00089 covered no-`dx` `extTextOut`; this task covers the explicit-`dx` horizontal path.
- Vertical and `ETO_PDY` handling are broader cases and should not be changed in this task.
- This does not attempt exact SVG XOR behavior or introduce font-file based metrics.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi explicit-`dx` `setTextCharacterExtra` handling.
   Next step: update horizontal SvgGdi explicit-`dx` advance calculation.
   Required context: preserve no-`dx` behavior from task 00089.
2. Status: completed. Update SvgGdi explicit-`dx` horizontal `extTextOut`.
   Next step: add focused tests.
   Required context: keep original supplied `dx` values intact and preserve vertical handling.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00090_svg_exttextout_dx_character_extra.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Align explicit-`dx` horizontal SvgGdi `extTextOut` more closely with AwtGdi advance handling.
- Preserve existing no-`dx`, vertical, and broader `ETO_PDY` behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated explicit-`dx` horizontal SvgGdi `extTextOut` to use an effective advance of supplied `dx` plus `setTextCharacterExtra`.
- Applied the effective advance consistently to text width, x-position stepping, alignment adjustment, and `TA_UPDATECP` current-position updates.
- Preserved no-`dx`, vertical, and broader `ETO_PDY` behavior.
- Added SvgGdi coverage asserting x positions and the following line start include both supplied advances and character extra.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
