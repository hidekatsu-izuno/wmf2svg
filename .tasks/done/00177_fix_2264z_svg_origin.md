# Fix 2264z SVG Origin

## Purpose
Investigate and fix the incorrect origin position in generated `2264z_01_1.svg` while preserving the correct PNG output behavior.

## Context
- Input file: `../wmf-testcase/data/src/2264z_01_1.wmf`
- Existing outputs:
  - `etc/data/dst/2264z_01_1.svg` appears to have an incorrect origin.
  - `etc/data/dst/2264z_01_1.png` is reported correct.
- PNG generation uses the AWT rendering path; SVG generation uses the SVG rendering path.
- Existing uncommitted changes in `.codex/rules/project.rules` and `src/test/java/net/arnx/wmf2svg/MainTest.java` predate this task and must be preserved.

## Tasks
1. Status: completed
   Next step: Inspect `2264z_01_1.wmf`, generated SVG attributes, and debug output.
   Required context: Compare placeable header, viewport/window records, and generated SVG root/group transforms.

2. Status: completed
   Next step: Compare coordinate conversion between AWT and SVG implementations.
   Required context: Focus on origin/window/viewport calculations and placeable-header adjustments.

3. Status: completed
   Next step: Implement the smallest design-consistent fix in the SVG path.
   Required context: Keep PNG/AWT behavior unchanged unless investigation proves shared logic is wrong.

4. Status: completed
   Next step: Add or update focused regression coverage for the origin behavior.
   Required context: Prefer a deterministic unit or integration assertion using the `2264z_01_1.wmf` test fixture when available.

5. Status: completed
   Next step: Run targeted verification and inspect regenerated output.
   Required context: Use Maven tests and, if useful, regenerate `2264z_01_1.svg`/PNG for comparison.

## Goals
- Identify the concrete cause of the SVG origin mismatch.
- Fix SVG generation for `2264z_01_1.wmf` without regressing PNG output.
- Verify with focused tests or reproducible generation output.
- Append completion notes and move this task file to `.tasks/done/00177_fix_2264z_svg_origin.md`.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfParser.java`
- `src/test/java/net/arnx/wmf2svg/MainTest.java`
- `etc/data/dst/2264z_01_1.svg`
- `../wmf-testcase/data/src/2264z_01_1.wmf`

## Completion Summary
- Cause: `2264z_01_1.wmf` contains deferred embedded EMF comments. During SVG footer processing, replaying those embedded EMFs updated the outer WMF canvas-origin bookkeeping, overwriting the outer `SetWindowOrgEx(-2311, -1178)` origin with the embedded EMF origin.
- Fix: Added a `replayingPendingEmf` guard in `SvgGdi` so pending EMF replay still renders through its cloned DC, but does not change the outer SVG canvas origin tracking.
- Regression coverage: Added `testEmbeddedEmfReplayDoesNotOverrideOuterWindowOrigin`.
- Verification:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest#testEmbeddedEmfReplayDoesNotOverrideOuterWindowOrigin test`
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
  - Regenerated `/tmp/2264z_01_1_fixed.svg`; its root now has `viewBox="-2311 -1178 4625 2358"`.
- Independent review: sub-agent verification reported no findings and confirmed the targeted embedded-EMF tests passed.
