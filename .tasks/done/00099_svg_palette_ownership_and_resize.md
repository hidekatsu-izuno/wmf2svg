# SvgGdi Palette Ownership And Resize

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make SVG palettes defensively own entries and honor `ResizePalette`.

## Context
- Existing local changes from tasks 00064 through 00098 are present and must be preserved.
- AwtPalette clones palette entries in its constructor and returns clones from `getEntries()`.
- AwtPalette also supports `resize(int)`, which affects later indexed DIB color lookup through the selected palette.
- SvgPalette currently keeps the caller-provided entries array directly, returns it directly, and SvgGdi `resizePalette()` is a no-op.
- This can make SVG output depend on later external mutation and can preserve palette entries that AwtGdi would truncate.
- This task should not affect SVG XOR behavior, font-file based measurement, or non-paletted bitmap rendering.

## Tasks
1. Status: completed. Compare AwtPalette ownership/resize behavior with SvgPalette/SvgGdi.
   Next step: update SvgPalette and SvgGdi resize handling.
   Required context: palette entries are used by selected palette indexed DIB decoding.
2. Status: completed. Implement defensive copying and resize support for SvgPalette.
   Next step: add focused tests.
   Required context: null entries should behave as an empty palette.
3. Status: completed. Wire SvgGdi `resizePalette()` to SvgPalette.
   Next step: add focused tests.
   Required context: ignore foreign palette implementations as current type guard does for set entries.
4. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: verify mutation safety and resized selected-palette DIB lookup.
5. Status: completed. Run focused and full Maven tests.
   Next step: append completion summary.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
6. Status: completed. Append completion summary and move to `.tasks/done/00099_svg_palette_ownership_and_resize.md`.
   Next step: finish.
   Required context: mention preserved XOR and font-file constraints.

## Goals
- Prevent caller mutation from altering SvgPalette entries.
- Make `ResizePalette` affect selected palette indexed DIB color lookup.
- Match AwtPalette behavior more closely while preserving existing type guards.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgPalette.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated `SvgPalette` to clone entries on construction and return clones from `getEntries()`.
- Added `SvgPalette.resize(int)` and wired SvgGdi `resizePalette()` for SVG palettes.
- Added focused SvgGdi tests for palette entry ownership, truncating resize, and extending resize behavior in selected-palette indexed DIB lookup.
- Preserved existing foreign-palette type guard behavior.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.
