# Split EMF+ Backend Status

## Purpose
Update `docs/emfplus.md` so each EMF+ record/object status describes `WmfGdi`, `EmfGdi`, `SvgGdi`, and `AwtGdi` separately.

## Context
- `docs/emfplus.md` currently has one combined `wmf2svg status` bullet per EMF+ record/object section.
- Backend behavior differs:
  - `WmfGdi`: writes WMF records and can carry comments, but cannot natively serialize EMF+ drawing records as WMF drawing operations.
  - `EmfGdi`: writes EMF records and can preserve EMF+ comment payloads, but native EMF+ interpretation is mainly in rendering backends.
  - `SvgGdi`: parses EMF+ comments and translates/falls back to SVG where possible.
  - `AwtGdi`: parses EMF+ comments and renders common EMF+ operations through Java2D/ImageIO.

## Current Status
- Status: completed
- Next step: none.
- Required context to resume: keep the official record and object inventory unchanged; only refine the status details.

## Tasks
1. Review EMF+ handling in writer and rendering backends.
2. Update `docs/emfplus.md` status bullets to split `WmfGdi`, `EmfGdi`, `SvgGdi`, and `AwtGdi`.
3. Verify every EMF+ record/object status block has the four backend labels.
4. Append completion summary and move this task file to `.tasks/done/00219_split_emfplus_backend_status.md`.

## Goals
- `docs/emfplus.md` has backend-specific status for all EMF+ sections.
- Existing EMF+ record/object coverage remains intact.
- No unfinished markers remain.

## File List
- `.tasks/00219_split_emfplus_backend_status.md`
- `docs/emfplus.md`

## Completion Summary
- Updated `docs/emfplus.md` so every EMF+ record/object section has a `wmf2svg status` block split into `WmfGdi`, `EmfGdi`, `SvgGdi`, and `AwtGdi`.
- Added backend status explanation near the top of the document.
- Clarified that `WmfGdi` can only pass raw EMF+ comments through WMF escape/comment data and has no native EMF+ record model.
- Clarified that `EmfGdi` can preserve raw EMF+ comments as `EMR_COMMENT`, while native EMF+ interpretation is handled by rendering backends.
- Verified all 67 EMF+ status blocks have all four backend labels and all 211 `EMF_PLUS_*` constants remain present in the document.
