# Split EMF Backend Status

## Purpose
Update `docs/emf.md` so each EMF record describes wmf2svg status separately for `EmfGdi`, `SvgGdi`, and `AwtGdi`.

## Context
- `docs/emf.md` currently has one combined `wmf2svg status` bullet per EMF record.
- Backend implementations:
  - `EmfGdi`: writes EMF records, including many native EMF record forms and some writer-side approximations.
  - `SvgGdi`: converts replayed GDI operations into SVG, with limitations around raster operations, color management, clipping, and host font metrics.
  - `AwtGdi`: renders through Java2D/ImageIO, with stronger raster behavior but host font/rendering dependencies.

## Current Status
- Status: completed
- Next step: none.
- Required context to resume: keep the official record inventory unchanged; only refine the status details.

## Tasks
1. Review `EmfGdi`, `SvgGdi`, and `AwtGdi` behavior relevant to EMF records.
2. Update `docs/emf.md` status bullets to split `EmfGdi`, `SvgGdi`, and `AwtGdi`.
3. Verify every EMF record has exactly one status block and the three backend labels.
4. Append completion summary and move this task file to `.tasks/done/00218_split_emf_backend_status.md`.

## Goals
- `docs/emf.md` has backend-specific status for all EMF records.
- All `EMR_*` constants remain covered.
- No unfinished markers remain.

## File List
- `.tasks/00218_split_emf_backend_status.md`
- `docs/emf.md`

## Completion Summary
- Updated `docs/emf.md` so every EMF record has a `wmf2svg status` block split into `EmfGdi`, `SvgGdi`, and `AwtGdi`.
- Added a backend status explanation near the top of the document.
- Verified all 121 `EMR_*` constants remain present in the document.
- Verified all 121 EMF status blocks have `EmfGdi`, `SvgGdi`, and `AwtGdi` labels.
