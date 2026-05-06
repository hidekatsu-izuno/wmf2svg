# Document WMF/EMF/EMF+ Record APIs

## Purpose
Document all official WMF, EMF, and EMF+ metafile record APIs in English Markdown so future full-support tests can be planned against an explicit coverage inventory.

## Context
- Official sources:
  - `[MS-WMF]` RecordType Enumeration and record sections.
  - `[MS-EMF]` RecordType Enumeration and record sections.
  - `[MS-EMFPLUS]` RecordType Enumeration and record sections.
- Implementation sources:
  - `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfConstants.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfParser.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfConstants.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfPlusConstants.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfPlusParser.java`
  - EMF+ handling in `AwtGdi` and `SvgGdi`

## Current Status
- Status: completed
- Next step: none.
- Required context to resume: the docs must include detailed per-record sections with purpose, key payload/effect, rendering/test relevance, and wmf2svg support status.

## Tasks
1. Gather official WMF/EMF/EMF+ record inventories from Microsoft Open Specifications.
2. Extract current wmf2svg handled records and behavior categories from constants, parsers, and EMF+ backends.
3. Write `docs/wmf.md`.
4. Write `docs/emf.md`.
5. Write `docs/emfplus.md`.
6. Verify record coverage, Markdown readability, and source notes.
7. Append completion summary and move this task file to `.tasks/done/00216_document_metafile_record_apis.md`.

## Goals
- `docs/wmf.md`, `docs/emf.md`, and `docs/emfplus.md` exist.
- All official record names from the three RecordType enumerations are covered.
- Support status is explicit for each record.
- No unfinished markers or stub sections remain.

## File List
- `.tasks/00216_document_metafile_record_apis.md`
- `docs/wmf.md`
- `docs/emf.md`
- `docs/emfplus.md`

## Completion Summary
- Added English record API references for WMF, EMF, and EMF+.
- Used Microsoft Open Specifications as the source of truth and cross-checked against current parser/constant files.
- Included detailed per-record sections with support status and test relevance.
- Added an EMF+ implementation-constant cross-reference so current `EmfPlusConstants` names remain searchable.
- Verified that all constants from `WmfConstants`, `EmfConstants`, and `EmfPlusConstants` appear in the new docs, and that no unfinished markers remain in docs.
