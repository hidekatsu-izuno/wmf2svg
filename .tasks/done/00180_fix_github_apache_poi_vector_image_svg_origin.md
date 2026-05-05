# Fix github_apache_poi_vector_image SVG Origin

## Purpose
Investigate why `github_apache_poi_vector_image.svg` has an incorrect origin while the PNG output is correct, then apply a focused SVG-side or shared mapping fix.

## Context
- Input file is expected to be `../wmf-testcase/data/src/github_apache_poi_vector_image.emf`.
- Existing outputs are expected under `etc/data/dst/`.
- PNG being correct suggests raster/AWT handling is acceptable and the issue is likely SVG canvas/viewBox, EMF header bounds, mapping, or replay state.
- Preserve existing uncommitted changes from previous `2264z`, `ant`, and `Circles` work.

## Tasks
1. Status: completed
   Next step: Inspected input/output files, current SVG viewBox/canvas attributes, PNG dimensions, and debug command sequence.
   Required context: Source is standalone EMF. Current SVG has `viewBox="112 64 706 578"` while its emitted shape coordinates are around `0..704` and `0..576`; PNG is `706x578`.

2. Status: completed
   Next step: Compared SVG geometry against PNG/reference at a coarse level.
   Required context: The SVG geometry itself is transformed to device-space correctly; only the root viewBox origin is shifted by the later EMF window origin.

3. Status: completed
   Next step: Traced root cause to SVG bounds/mapping logic.
   Required context: `SvgGdi.setWindowExtEx()` overwrites `windowCanvasX/Y` each time an extent is set. Standalone EMF header establishes a `0,0` canvas first, then drawing records set `windowOrg=(112,64)` and another extent, which incorrectly replaces the root viewBox origin.

4. Status: completed
   Next step: Preserved the first non-placeable window canvas origin once established by an extent.
   Required context: `SvgGdi.setWindowExtEx()` no longer overwrites `windowCanvasX/Y` after the canvas origin has already been established.

5. Status: completed
   Next step: Added regression coverage.
   Required context: `SvgGdiTest.testLaterWindowOriginBeforeExtentDoesNotOverrideRootViewBoxOrigin` reproduces the standalone EMF header-origin pattern.

6. Status: completed
   Next step: Verified with targeted tests, regenerated `github_apache_poi_vector_image.svg`, and broader Maven tests.
   Required context: `SvgGdiTest` and full `mvn -q test` passed. Regenerated SVG now has `viewBox="0 0 706 578"` and PNG remains `706x578`.

## Goals
- Determine the concrete cause of the SVG origin error.
- Fix the generated SVG without regressing PNG or prior embedded EMF cases.
- Verify with tests and regenerated output.
- Append completion notes and move this file to `.tasks/done/00180_fix_github_apache_poi_vector_image_svg_origin.md`.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `etc/data/dst/github_apache_poi_vector_image.svg`
- `etc/data/dst/github_apache_poi_vector_image.png`
- `../wmf-testcase/data/src/github_apache_poi_vector_image.emf`

## Completion Summary
- Cause: Standalone EMF header setup establishes a `0,0` canvas, then later EMF records set `windowOrg=(112,64)` and another window extent for drawing. SVG geometry was correctly emitted in device-space near `0,0`, but `SvgGdi.setWindowExtEx()` overwrote the root canvas origin with `112,64`, producing `viewBox="112 64 706 578"`.
- Fix: Preserve the first non-placeable window canvas origin once set, so later drawing window changes do not shift the SVG root viewBox.
- Regression: Added a focused SVG test for a later nonzero window origin followed by another extent.
- Verification: Regenerated `etc/data/dst/github_apache_poi_vector_image.svg` and PNG; full Maven tests passed.
