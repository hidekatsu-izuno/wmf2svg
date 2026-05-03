# Improve SvgGdi From AwtGdi Again

## Purpose
Find and implement another SvgGdi improvement informed by AwtGdi behavior while preserving SVG-specific constraints.

## Context
- Existing local changes from task 00064 are present and should be preserved.
- SVG output cannot provide exact destination-dependent XOR composition; do not expand unsupported XOR claims.
- SvgGdi must remain independent of local font files.
- Prefer improvements that are representable as SVG geometry/state or deterministic embedded PNG generation.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi for another bounded behavior gap.
   Next step: inspect DC state, path/current-position behavior, and source-independent raster cases.
   Required context: avoid font metrics and exact destination XOR dependencies.
2. Status: completed. Select one improvement with clear observable behavior and update this plan if the selected scope differs from the initial file list.
   Next step: record selected behavior before editing.
   Required context: keep target files small and avoid unrelated refactors.
   Selected behavior: align SvgDc window/viewport origin offset bookkeeping with AwtDc. Setting an absolute origin should reset accumulated offsets, offset APIs should report the effective old origin, and scale APIs should report effective extents.
3. Status: completed. Implement the improvement.
   Next step: edit only the chosen SvgGdi/SvgDc files.
   Required context: preserve task 00064 changes.
4. Status: completed. Add focused tests.
   Next step: assert SVG/state behavior without environment-dependent rendering.
   Required context: use existing SvgGdiTest patterns.
5. Status: completed. Run focused and relevant full tests.
   Next step: run SvgGdiTest, then `mvn -q test` if focused tests pass.
   Required context: record commands and results.
6. Status: completed. Append completion summary and move to `.tasks/done/00065_improve_svg_gdi_from_awt.md`.
   Next step: finalize after verification.
   Required context: mention intentionally unchanged SVG limitations.

## Goals
- Deliver one additional concrete SvgGdi improvement.
- Preserve exact-XOR and font-file constraints.
- Keep implementation complete and covered by tests.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Selected SvgDc origin/extent state behavior based on AwtDc.
- Updated `SetWindowOrgEx` and `SetViewportOrgEx` handling so absolute origin changes reset accumulated offsets.
- Updated offset APIs to return the effective previous origin, not only the accumulated offset.
- Updated scale extent APIs to return the effective previous extent.
- Added SvgGdi tests for window origin reset, viewport origin reset, and effective previous extents from scale APIs.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
