# SvgGdi ExtTextOut UpdateCP Alignment

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make explicit-`dx` `extTextOut` `TA_UPDATECP` advancement independent of alignment offsets.

## Context
- Existing local changes from tasks 00064 through 00092 are present and must be preserved.
- AwtGdi applies `TA_LEFT` / `TA_CENTER` / `TA_RIGHT` to the drawing position, but `TA_UPDATECP` moves from the original reference point plus the total text advance.
- SvgGdi explicit-`dx` horizontal `extTextOut` currently updates the current position from the alignment-adjusted drawing position.
- This causes `TA_RIGHT` / `TA_CENTER` with explicit `dx` to under-advance current position.
- This task should preserve task 00090 character-extra and task 00092 `ETO_PDY` behavior.
- This does not attempt exact SVG XOR behavior or introduce font-file based metrics.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `TA_UPDATECP` advancement under text alignment.
   Next step: update explicit-`dx` SvgGdi current-position update.
   Required context: keep coordinate-list drawing positions unchanged.
2. Status: completed. Update SvgGdi explicit-`dx` `TA_UPDATECP`.
   Next step: add focused tests.
   Required context: preserve `ETO_PDY` y movement.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00093_svg_exttextout_updatecp_alignment.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Align explicit-`dx` SvgGdi `TA_UPDATECP` advancement with AwtGdi under `TA_RIGHT` / `TA_CENTER`.
- Preserve existing SVG drawing positions.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated explicit-`dx` horizontal SvgGdi `extTextOut` so `TA_UPDATECP` advances from the original reference point plus total advance.
- Preserved the alignment-adjusted SVG coordinate-list drawing positions.
- Preserved `ETO_PDY` y movement from task 00092.
- Added SvgGdi coverage for `TA_RIGHT` explicit-`dx` text, asserting aligned drawing positions and reference-based current-position advancement.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
