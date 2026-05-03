# SvgGdi ExtTextOut ETO_PDY

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make horizontal `extTextOut` honor `ETO_PDY` `(xAdvance, yAdvance)` pairs.

## Context
- Existing local changes from tasks 00064 through 00091 are present and must be preserved.
- AwtGdi treats `ETO_PDY` `lpdx` as x/y advance pairs and updates drawing/current position in both axes.
- SvgGdi horizontal `extTextOut` currently treats the entire `dx` array as x advances, so y advances incorrectly affect x positions and no SVG y list is emitted.
- SVG can represent per-character x/y positions with coordinate lists.
- This task focuses on horizontal `ETO_PDY`; vertical text and broader multi-byte `dx` normalization are left unchanged.
- This does not attempt exact SVG XOR behavior or introduce font-file based metrics.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `ETO_PDY` handling.
   Next step: update horizontal SvgGdi `extTextOut` coordinate-list generation.
   Required context: preserve task 00090 character-extra and task 00091 justification advance behavior.
2. Status: completed. Update SvgGdi horizontal `ETO_PDY`.
   Next step: add focused tests.
   Required context: keep non-`ETO_PDY`, vertical, and no-`dx` behavior unchanged.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00092_svg_exttextout_eto_pdy.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Align horizontal SvgGdi `ETO_PDY` text positioning with AwtGdi.
- Avoid treating y advances as x advances.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated horizontal SvgGdi `extTextOut` to treat `ETO_PDY` `dx` arrays as `(xAdvance, yAdvance)` pairs.
- Emitted separate SVG `x` and `y` coordinate lists for horizontal `ETO_PDY` text.
- Updated `TA_UPDATECP` current-position movement to include both summed x and y advances.
- Preserved non-`ETO_PDY`, vertical, and no-`dx` behavior.
- Added SvgGdi coverage asserting separate x/y lists and the following line start after both-axis advancement.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
