# Compare 2264 WMF Renderers

## Purpose
Compare `2264z_01_1.wmf` PNG output from GDI+, `PlayEnhMetaFile`, `PlayMetaFile`, and any practical additional renderer path against the mspaint baseline.

## Context
- Baseline image: `../wmf-testcase/data/png/2264z_01_1.png`, treated as mspaint output.
- Current generated image location: `../wmf-testcase/data/png2/2264z_01_1.png`.
- The user wants a concise summary of how each renderer differs from mspaint, and wants additional feasible methods tried.
- Previous checks showed `PlayMetaFile` did not remove the extra blue/black opaque pixels seen in the high-DPI non-placeable path.

## Tasks
- [x] Generate comparable renderer outputs.
  - Status: completed.
  - Next step: none.
  - Required context: output dimensions should match the baseline `4625x2358`.
- [x] Measure differences against the mspaint baseline.
  - Status: completed.
  - Next step: none.
  - Required context: ImageMagick `compare`, `convert`, and `identify` are available.
- [x] Summarize practical implications.
  - Status: completed.
  - Next step: none.
  - Required context: visual invisibility may still hide sparse alpha/RGB differences.

## Summary
- Generated four `2264z_01_1.wmf` variants under `/tmp`: GDI+ DrawImage, PlayEnhMetaFile, PlayMetaFile, and GDI+ Image.Save.
- All four outputs were `4625x2358` RGBA PNGs.
- GDI+ Image.Save matched the mspaint baseline bit-for-bit for `2264z_01_1.png`.
- GDI+ DrawImage and PlayEnhMetaFile were close but had sparse alpha/RGB differences around the drawing bounds.
- PlayMetaFile produced the largest difference among the tested renderer paths.
- GDI+ Image.Save was also tried on `2doorvan.wmf`; it produced `318x116`, not the expected `477x173`, so it is not a drop-in general replacement without size handling.

## Goals
- A short table comparing methods against mspaint.
- Notes on what differs: extra opaque pixels, alpha differences, transparent RGB differences, or metadata-only differences.
- Mention whether any additional method improved the result.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00137_compare_2264_wmf_renderers.md`
