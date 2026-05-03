# SvgGdi RestoreDC Robustness

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make `restoreDC` robust when the save stack is empty or the requested restore depth exceeds the stack.

## Context
- Existing local changes from tasks 00064 through 00067 are present and must be preserved.
- AwtGdi guards `restoreDC` against an empty save stack, supports `savedDC == 0` as restore-all, and caps excessive negative restore depths.
- SvgGdi currently removes saved DC entries without guarding, so malformed or redundant RestoreDC records can throw.
- This is state-management robustness only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `restoreDC` behavior.
   Next step: implement guarded restoration.
   Required context: preserve current group/mask recreation after a successful restore.
2. Status: completed. Update SvgGdi `restoreDC`.
   Next step: add empty-stack guard, restore-all behavior, and capped restore count.
   Required context: avoid changing normal `restoreDC(-1)` output.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: test empty restore does not throw and over-depth restore restores available state.
   Required context: assert SVG output/state without rendering.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00068_svg_restore_dc_robustness.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Match AwtGdi's robust `restoreDC` handling in SvgGdi.
- Avoid exceptions for redundant or over-depth restore requests.
- Keep normal saved/restored drawing behavior intact.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's robust `restoreDC` handling in SvgGdi.
- Added an empty-stack guard.
- Added `savedDC == 0` restore-all behavior.
- Capped excessive negative restore depths to the available saved states.
- Kept group/mask recreation only after actual restoration.
- Added SvgGdi tests for empty restore and over-depth restore.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
