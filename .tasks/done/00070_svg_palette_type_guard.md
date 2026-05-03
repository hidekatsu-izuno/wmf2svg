# SvgGdi Palette Type Guard

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: guard palette operations against non-SvgPalette instances.

## Context
- Existing local changes from tasks 00064 through 00069 are present and must be preserved.
- AwtGdi uses `instanceof AwtPalette` in `selectPalette` and `setPaletteEntries`.
- SvgGdi currently casts directly to `SvgPalette` in `selectPalette`, and directly casts in `setPaletteEntries`.
- Robust type guarding is state-management only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi palette type handling.
   Next step: add SvgPalette guards.
   Required context: match AwtGdi's clear-on-foreign-select behavior.
2. Status: completed. Update SvgGdi palette methods.
   Next step: edit `selectPalette` and `setPaletteEntries`.
   Required context: preserve normal SvgPalette behavior. `setPaletteEntries` was already guarded; `selectPalette` now matches AwtGdi's guard and clear behavior.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: pass a non-SvgPalette implementation and verify no exception.
   Required context: use an in-test GdiPalette stub.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00070_svg_palette_type_guard.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Avoid ClassCastException when SvgGdi receives a foreign `GdiPalette`.
- Keep SvgPalette behavior unchanged.
- Verify with focused tests.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's guarded palette selection behavior in SvgGdi.
- Updated `selectPalette` so non-SvgPalette values clear the selected palette instead of throwing `ClassCastException`.
- Confirmed `setPaletteEntries` already had the SvgPalette guard.
- Added a SvgGdi test with a foreign `GdiPalette` implementation to verify no exception and selected palette clearing via indexed DIB output.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
