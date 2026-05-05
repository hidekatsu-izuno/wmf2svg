# Investigate github_closedxml_sample_image PNG/SVG

## Purpose
Investigate why `github_closedxml_sample_image.png` has excessive surrounding padding and why `github_closedxml_sample_image.svg` renders incorrectly, then apply focused fixes.

## Context
- Input file is `../wmf-testcase/data/src/github_closedxml_sample_image.emf`.
- Current generated outputs are under `etc/data/dst/`.
- Reference PNG exists at `../wmf-testcase/data/png/github_closedxml_sample_image.png`.
- Recent EMF PNG fix locked AWT canvas to standalone EMF header bounds and made EMF raster output transparent.

## Tasks
1. Status: completed
   Next step: Inspected generated/reference geometry, SVG structure, EMF header bounds/frame, and debug record sequence.
   Required context: Determine whether the sample is standalone EMF, EMF+ only, or mixed EMF/EMF+, and whether the padding comes from EMF header bounds, frame, or ignored content.

2. Status: completed
   Next step: Traced why AWT output is blank/excessively padded and why SVG coordinates/viewBox differ from expected output.
   Required context: Identify whether EMF+ comments are parsed/suppressed differently for AWT and SVG.

3. Status: completed
   Next step: Compared expected behavior with reference PNG and nearby EMF samples.
   Required context: Establish a general rule, avoiding sample-specific cropping.

4. Status: completed
   Next step: Implemented a focused fix.
   Required context: Preserve earlier fixes for `github_epplus_code`, `github_apache_poi_vector_image`, `Circles`, and WMF placeable stroke behavior.

5. Status: completed
   Next step: Added regression coverage for nonzero standalone EMF header bounds in AWT and SVG.
   Required context: Prefer deterministic parser/AWT/SVG tests over image-specific assertions where possible.

6. Status: completed
   Next step: Verified with targeted tests, regenerated outputs, geometry checks, and broader Maven tests.
   Required context: Confirm PNG padding/transparency and SVG display improve without regressions.

## Goals
- Explain the concrete cause of the PNG padding/blank output and SVG display issue.
- Fix both outputs with a general implementation.
- Verify with tests and regenerated `github_closedxml_sample_image` outputs.
- Append completion notes and move this file to `.tasks/done/00183_investigate_github_closedxml_sample_image_outputs.md`.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfPlusParser.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `etc/data/dst/github_closedxml_sample_image.png`
- `etc/data/dst/github_closedxml_sample_image.svg`
- `../wmf-testcase/data/src/github_closedxml_sample_image.emf`

## Completion Notes
- Cause: this standalone EMF has nonzero header bounds `(723,1073)-(1646,1999)`. The previous EMF header canvas fix used the header size but did not use the header left/top as the canvas origin, so AWT drew the content outside the PNG and SVG used a `0 0 ...` viewBox that left the content shifted into the lower-right area.
- Fix: AWT now records the standalone EMF header left/top as `canvasMinX/Y`, so the canvas transform crops to the EMF bounds instead of keeping `(0,0)` as the raster origin.
- Fix: SVG now records standalone EMF header bounds and uses them for root `width`, `height`, and `viewBox`, while preserving the previous zero-origin behavior for samples like `github_epplus_code`.
- Tests: added AWT and SVG regression tests for nonzero standalone EMF header bounds, and updated `EmfParserTest.testHeaderSetsSvgBounds` for the corrected header bounds viewBox.
- Regenerated `etc/data/dst/github_closedxml_sample_image.png` and `.svg`.
- Geometry after regeneration: generated PNG `924x927`, bbox `923x926+0+0`; reference PNG `924x927`, bbox `922x926+1+0`.
- SVG after regeneration: `width="924"`, `height="927"`, `viewBox="723 1073 924 927"`.
- Verification: targeted AWT/SVG/EMF parser tests passed; full `mvn -q test` passed.
