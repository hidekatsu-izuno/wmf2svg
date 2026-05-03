# SvgGdi TextOut UpdateCP Origin

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make `textOut` use the current position as its drawing origin when `TA_UPDATECP` is selected.

## Context
- Existing local changes from tasks 00064 through 00086 are present and must be preserved.
- AwtGdi's shared `drawText` path replaces `x` / `y` with the current position before drawing whenever `TA_UPDATECP` is active.
- SvgGdi `extTextOut` already does this, but SvgGdi `textOut` still uses the passed `x` / `y` as the drawing origin.
- Task 00086 added post-draw current-position advancement; this task completes the corresponding pre-draw origin behavior for `textOut`.
- SvgGdi should continue using existing text-width estimates and must not introduce font-file metrics.
- This does not attempt exact SVG XOR behavior.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi pre-draw `TA_UPDATECP` text origin handling.
   Next step: update SvgGdi `textOut`.
   Required context: preserve task 00086 post-draw current-position advancement.
2. Status: completed. Update SvgGdi `textOut` origin selection.
   Next step: update focused test coverage.
   Required context: match existing SvgGdi `extTextOut` behavior.
3. Status: completed. Update focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00087_svg_textout_updatecp_origin.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Align SvgGdi `textOut` `TA_UPDATECP` origin behavior with AwtGdi and SvgGdi `extTextOut`.
- Preserve current-position advancement from task 00086.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated SvgGdi `textOut` so `TA_UPDATECP` uses the current position as the drawing origin before calculating SVG coordinates, background, and rotation.
- Kept the post-draw current-position advancement from task 00086.
- Updated focused SvgGdi coverage to pass different `textOut` coordinates after setting current position and assert output starts from the current position.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
