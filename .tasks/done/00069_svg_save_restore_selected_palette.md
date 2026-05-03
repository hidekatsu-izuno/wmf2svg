# SvgGdi Save Restore Selected Palette

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: preserve the selected palette across `SaveDC` / `RestoreDC`.

## Context
- Existing local changes from tasks 00064 through 00068 are present and must be preserved.
- AwtGdi saves and restores `selectedPalette` with the DC state.
- SvgGdi has a `selectedPalette` field used by indexed DIB conversion, but `seveDC` currently saves only `SvgDc`.
- SVG output can reflect palette selection through deterministic embedded PNG conversion.
- This change must not alter exact XOR limitations or introduce font-file dependencies.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi selected palette save/restore behavior.
   Next step: implement saved state wrapper for SvgGdi.
   Required context: keep existing SvgDc clone behavior.
2. Status: completed. Update SvgGdi save/restore to include `selectedPalette`.
   Next step: add a small saved-state holder and use it in `seveDC` / `restoreLastDC`.
   Required context: preserve mask/group recreation behavior.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: use indexed DIB output to observe palette restoration.
   Required context: assert embedded PNG pixel color.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00069_svg_save_restore_selected_palette.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Make SvgGdi `SaveDC` / `RestoreDC` preserve selected palette state.
- Verify via DIB_PAL_COLORS conversion.
- Keep behavior deterministic and independent of renderer/font environment.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi selected palette save/restore behavior in SvgGdi.
- Replaced the raw saved `SvgDc` stack with a small saved-state holder containing the cloned DC and selected palette.
- Restored `selectedPalette` in `restoreLastDC`.
- Added a SvgGdi test using a 1-pixel indexed DIB to verify that `RestoreDC` restores the palette used for `DIB_PAL_COLORS` conversion.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
