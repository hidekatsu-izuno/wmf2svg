# Investigate ant.svg Missing Lines

## Purpose
Investigate why lines appear to be missing from generated `ant.svg`, compare against the correct PNG output, and fix SVG generation if the missing lines are caused by renderer behavior.

## Context
- Input file: `../wmf-testcase/data/src/ant.wmf`
- Existing outputs:
  - `etc/data/dst/ant.svg`
  - `etc/data/dst/ant.png`
  - reference PNG: `../wmf-testcase/data/png/ant.png`
- Recent SVG change for deferred embedded EMF replay is present in the worktree and must be preserved.
- Existing uncommitted changes in `.codex/rules/project.rules`, `MainTest.java`, and previous task files predate this request and must be preserved.

## Tasks
1. Status: completed
   Next step: Inspect generated `ant.svg`, PNG dimensions, and debug GDI command sequence.
   Required context: Identify which line-like commands are present in WMF and whether SVG contains corresponding elements.

2. Status: completed
   Next step: Compare SVG line/path rendering logic against AWT behavior for the suspicious command/style.
   Required context: Focus on pen selection, pen style, ROP2, polyline/line/path rendering, clip state, and viewBox bounds.

3. Status: completed
   Next step: Implement a focused fix if a renderer defect is confirmed.
   Required context: Keep edits scoped to SVG rendering and preserve PNG/AWT behavior unless shared parsing is wrong.

4. Status: completed
   Next step: Add or update regression coverage.
   Required context: Prefer a small deterministic GDI-level test if the defect can be isolated; otherwise use the `ant.wmf` fixture when available.

5. Status: completed
   Next step: Verify with targeted tests and regenerated `ant.svg`.
   Required context: Check root/viewBox and presence/visibility of line elements; run Maven tests relevant to changed code.

## Goals
- Determine whether `ant.svg` has missing line elements, hidden/invisible line styles, clipping, or an incorrect canvas/viewBox.
- Fix any confirmed SVG renderer defect.
- Verify behavior with tests and regenerated output.
- Append completion notes and move this task file to `.tasks/done/00178_investigate_ant_svg_missing_lines.md`.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfParser.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `etc/data/dst/ant.svg`
- `../wmf-testcase/data/src/ant.wmf`

## Completion Summary
- `vector-effect` is not SVG 1.0 compatible, so it was not used.
- Cause: `ant.svg` contained the polyline elements, but placeable WMF output declared a small physical viewport with a large logical `viewBox`, making `stroke-width: 1.0` render below a visible device-pixel width. Also, SVG pen CSS was cached by pen style/color/width, so a default pen created before later mapping changes could be reused after the mapping changed.
- Fix: For placeable WMF SVG output, enforce a minimum stroke width in SVG user units equivalent to one output CSS pixel. Also stop reusing pen CSS classes across newly created pens because their rendered width depends on current DC mapping.
- Verification:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest#testCosmeticPenUsesLogicalPixelWidthWithPlaceableHeader+testPlaceableThinPenUsesAtLeastOneOutputPixelWidth+testNonPlaceableCosmeticPenDoesNotForceNonScalingStroke test`
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`
  - Regenerated `etc/data/dst/ant.svg`; generated pen styles now include `stroke-width: 10.337539432176657` and no `vector-effect`.
