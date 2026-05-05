# 00165 Make EMF background transparent in wmf2png.ps1

## Purpose
Change EMF handling in `wmf2png.ps1` so EMF inputs use the same transparent base behavior as WMF inputs.

## Context
- EMF support was added to `wmf2png.ps1` in task 00164.
- That implementation matched `emf2png.ps1` default white background.
- User clarified that `emf2png.ps1` defaults should be ignored and EMF must use a transparent base like WMF.
- Current EMF path already avoids WMF record parsing and uses `Image.FromFile` natural dimensions.

## Tasks
- [ ] Update EMF background clearing to transparent.
- [ ] Verify `fulltest.emf` still renders successfully with the same dimensions.
- [ ] Verify the EMF output has transparent pixels.
- [ ] Re-check representative WMF output to ensure no regression.

## Goals
- `wmf2png.ps1 <file.emf> <file.png>` uses transparent background.
- Existing WMF behavior remains unchanged.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00165_make_emf_background_transparent.md`

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: sample EMF is `src/test/data/emf/fulltest.emf`; representative WMF is `../wmf-testcase/data/src/texts.wmf`.

## Summary
- Changed EMF handling in `wmf2png.ps1` to clear the output bitmap with transparent instead of white.
- Renamed the local flag from `useTransparentWhite` to `useTransparentBackground`.
- WMF-specific parsing and render branches were not changed.

## Verification
- `fulltest.emf` rendered successfully at `1061x794`.
- `fulltest.emf` output alpha range was `0..1`, confirming transparent pixels are present.
- `texts.wmf` remained exact against reference: visible AE `0`, alpha AE `0`.
- `2doorvan.wmf` remained exact against reference: visible AE `0`, alpha AE `0`.
