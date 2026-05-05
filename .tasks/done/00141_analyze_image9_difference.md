# Analyze image9 Difference

## Purpose
Analyze why latest `wmf2png.ps1` output for `image9.wmf` differs from the mspaint baseline and propose a fix strategy.

## Context
- Source: `../wmf-testcase/data/src/image9.wmf`
- Baseline: `../wmf-testcase/data/png/image9.png`
- Generated: `../wmf-testcase/data/png2/image9.png`
- Prior full comparison showed `image9.png` differs despite having a placeable header, with both RGB and alpha differences.

## Tasks
- [x] Inspect metadata and header characteristics.
  - Status: completed.
  - Next step: none.
  - Required context: placeable files usually use explicit canvas rendering in the current script.
- [x] Analyze pixel differences.
  - Status: completed.
  - Next step: none.
  - Required context: use ImageMagick against baseline and png2.
- [x] Test plausible alternate load/canvas strategies.
  - Status: completed.
  - Next step: none.
  - Required context: keep outputs under `/tmp`.
- [x] Present solution options.
  - Status: completed.
  - Next step: none.
  - Required context: do not implement unless requested.

## Goals
- Clear explanation of what differs for `image9`.
- Evidence-backed root-cause hypothesis.
- Practical fix proposal with tradeoffs.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00141_analyze_image9_difference.md`

## Summary
- `image9.wmf` is placeable. Header bbox is `(0,0)-(518,351)` with `inch=96`, so the Paint-like 144 dpi conversion gives `777x527`, matching both baseline and current output.
- GDI+ native load reports `518x351` at `96x96` dpi. Native `Image.Save(PNG)` therefore has the wrong size for Paint compatibility.
- Current `wmf2png.ps1` output differs from baseline by `AE=27433`, with `A=17981`, `RGB=27433`, and diff bbox `735x490+26+14`.
- The current output exactly matches a test render using `InterpolationMode=HighQualityBicubic`.
- A test render using the same explicit 777x527 transparent canvas but `InterpolationMode=NearestNeighbor` matches the mspaint baseline exactly for `image9` (`AE=0`). Transparent-white canvas also matches when the interpolation mode is nearest; opaque white does not.
- A global `NearestNeighbor` change fixes the observed placeable differences but is not a good blanket change because non-placeable files still represent a separate alpha/RGB behavior. The targeted fix should apply nearest-neighbor scaling only to placeable explicit-canvas rendering.
