# SvgGdi Select Region Object

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: selecting a region object should update the current clipping region.

## Context
- Existing local changes from tasks 00064, 00065, and 00066 are present and must be preserved.
- AwtGdi handles `selectObject(AwtRegion)` by calling `selectClipRgn`.
- SvgGdi currently handles brushes, fonts, and pens in `selectObject`, but not `SvgRegion`.
- SVG can represent this behavior with the existing mask-based clip machinery.
- This change must not alter exact XOR limitations or introduce font-file dependencies.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `selectObject` behavior.
   Next step: implement region selection parity.
   Required context: use existing `selectClipRgn` path.
2. Status: completed. Update SvgGdi to select SvgRegion objects as clips.
   Next step: edit only `SvgGdi.selectObject`.
   Required context: preserve brush/font/pen behavior.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: create a region, select it via `selectObject`, draw, and assert mask output.
   Required context: use SVG string assertions.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00067_svg_select_region_object.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Match AwtGdi region selection behavior in SvgGdi.
- Reuse existing SVG mask clipping implementation.
- Keep the change small and verified.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi `selectObject(region)` behavior in SvgGdi.
- Updated `SvgGdi.selectObject` so `SvgRegion` objects are selected through `selectClipRgn`.
- Added a SvgGdi test that selects a rectangle region via `selectObject` and verifies mask-based clipping output.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
