# SvgGdi Text Update Current Position

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make text output advance the current position when `TA_UPDATECP` is selected.

## Context
- Existing local changes from tasks 00064 through 00085 are present and must be preserved.
- AwtGdi's shared text drawing path advances the current position after text output when `TA_UPDATECP` is active.
- SvgGdi currently updates the current position only for some `extTextOut` paths with explicit `dx`, while `textOut` and no-`dx` `extTextOut` leave it unchanged.
- SvgGdi should continue using existing text-width estimates and must not introduce font-file metrics.
- This does not attempt exact SVG XOR behavior.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `TA_UPDATECP` text handling.
   Next step: add SvgGdi current-position advancement for missing text paths.
   Required context: use existing estimated logical text advances.
2. Status: completed. Update SvgGdi `textOut`.
   Next step: add no-`dx` `extTextOut` update.
   Required context: horizontal advance uses estimated width plus character extra; vertical advance uses estimated text height.
3. Status: completed. Update SvgGdi no-`dx` `extTextOut`.
   Next step: add focused tests.
   Required context: preserve existing explicit `dx` updates.
4. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG path/polyline string assertions are sufficient.
5. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
6. Status: completed. Append completion summary and move to `.tasks/done/00086_svg_text_update_current_position.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Align SvgGdi current-position behavior more closely with AwtGdi for `TA_UPDATECP`.
- Preserve existing explicit `dx` behavior.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated SvgGdi `textOut` to advance the current position when `TA_UPDATECP` is active, using the existing estimated logical text advance.
- Updated no-`dx` SvgGdi `extTextOut` to advance the current position when `TA_UPDATECP` is active, while preserving existing explicit `dx` updates.
- Added focused tests that draw a line after `textOut` / no-`dx` `extTextOut` and assert the line starts at the advanced current position.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
