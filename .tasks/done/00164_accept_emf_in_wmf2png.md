# 00164 Accept EMF input in wmf2png.ps1

## Purpose
Allow `src/test/bin/wmf2png.ps1` to accept EMF files while preserving the current WMF-specific behavior.

## Context
- `wmf2png.ps1` currently assumes WMF and parses WMF placeable/non-placeable records.
- `emf2png.ps1` already renders EMF via `System.Drawing.Image.FromFile` using the image natural size and a white background unless transparent is requested.
- `wmf2png.ps1` no longer exposes width/height/transparent options, and has MS Paint compatibility logic for WMF including DPI awareness.
- EMF must not be accidentally interpreted as non-placeable WMF records.

## Tasks
- [ ] Add a small file-type detector for EMF vs WMF.
- [ ] Route EMF inputs through an EMF-safe render path using `Image.FromFile` and natural image dimensions.
- [ ] Keep WMF placeable/non-placeable branching unchanged.
- [ ] Verify `wmf2png.ps1` can render `src/test/data/emf/fulltest.emf`.
- [ ] Re-run representative WMF checks, including `texts.wmf`, to confirm no WMF regression.

## Goals
- `wmf2png.ps1 <file.emf> <file.png>` succeeds.
- EMF output dimensions match the existing `emf2png.ps1` behavior for default options.
- Existing WMF parity state is preserved, especially `texts.png` exact match.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00164_accept_emf_in_wmf2png.md`

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: sample EMF is `src/test/data/emf/fulltest.emf`; representative WMF is `../wmf-testcase/data/src/texts.wmf`.

## Summary
- Added `Test-EmfHeader` to detect EMF by `ENHMETAHEADER` type/signature.
- Routed EMF inputs through `Image.FromFile` and natural image dimensions.
- Kept WMF-only placeable/mapmode/canvas parsing out of the EMF path.
- Kept the existing WMF rendering branches unchanged.
- EMF output uses a white clear, matching the default `emf2png.ps1` behavior.

## Verification
- `wmf2png.ps1 src/test/data/emf/fulltest.emf` succeeded.
- Output dimensions were `1061x794`, matching `emf2png.ps1`.
- `fulltest.emf` output compared against `emf2png.ps1` output with visible AE `0`.
- Representative WMF checks:
  - `texts`: visible AE `0`, alpha AE `0`
  - `2doorvan`: visible AE `0`, alpha AE `0`
  - `p0000001`: visible AE `625`, alpha AE `0` (existing remaining diff)
  - `sample_03`: visible AE `2347`, alpha AE `0` (existing remaining diff)
