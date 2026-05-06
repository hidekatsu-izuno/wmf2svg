# Split WMF Backend Status

## Purpose
Update `docs/wmf.md` so each WMF record describes wmf2svg status separately for `WmfGdi`, `SvgGdi`, and `AwtGdi`.

## Context
- `docs/wmf.md` currently has one combined `wmf2svg status` bullet per record.
- Backend implementations:
  - `WmfGdi`: writes WMF records, with some advanced/non-WMF operations omitted or approximated.
  - `SvgGdi`: converts GDI replay into SVG elements and images, with raster/ROP limitations.
  - `AwtGdi`: renders through Java2D/ImageIO, with stronger raster behavior but host font/rendering dependencies.

## Current Status
- Status: completed
- Next step: none.
- Required context to resume: keep the official record inventory unchanged; only refine the status details.

## Tasks
1. Review backend implementation behavior relevant to WMF records.
2. Update `docs/wmf.md` status bullets to split `WmfGdi`, `SvgGdi`, and `AwtGdi`.
3. Verify every WMF record still has exactly one status block and the three backend labels.
4. Append completion summary and move this task file to `.tasks/done/00217_split_wmf_backend_status.md`.

## Goals
- `docs/wmf.md` has backend-specific status for all WMF records.
- All `META_*` constants remain covered.
- No unfinished markers remain.

## File List
- `.tasks/00217_split_wmf_backend_status.md`
- `docs/wmf.md`

## Completion Summary
- Updated `docs/wmf.md` so every WMF record has a `wmf2svg status` block split into `WmfGdi`, `SvgGdi`, and `AwtGdi`.
- Added a backend status explanation near the top of the document.
- Added the missing `META_SAVEDC` record section while verifying the backend status structure.
- Verified all 70 WMF constants have headings and all 70 records have the three backend status labels.
