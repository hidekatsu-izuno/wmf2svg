# Verify EMF and EMF+ Backend Status Documentation

## Purpose

Verify and update `docs/emf.md` and `docs/emfplus.md` so their per-backend `wmf2svg status` entries reflect the current implementation instead of mechanically conservative labels.

## Context

- Target documentation:
  - `docs/emf.md`
  - `docs/emfplus.md`
- Relevant implementation files:
  - `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfGdi.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfPlusParser.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- Existing uncommitted context:
  - `docs/wmf.md` and `.tasks/done/00221_verify_wmf_backend_status.md` were updated by the previous WMF status verification task and must not be reverted.

## Tasks

1. Inspect EMF status entries currently marked `Partial`, `Parsed/ignored`, or pass-through/comment for `EmfGdi`, `SvgGdi`, and `AwtGdi`.
2. Cross-check those entries against current EMF writer, SVG backend, and AWT backend implementation behavior.
3. Update `docs/emf.md` where the status or explanation is inaccurate.
4. Inspect EMF+ status entries for `WmfGdi`, `EmfGdi`, `SvgGdi`, and `AwtGdi`, keeping official EMF+ record semantics primary and project-specific implementation details secondary.
5. Update `docs/emfplus.md` where the status or explanation is inaccurate.
6. Verify representative EMF and EMF+ record coverage, Markdown sanity, and absence of unfinished markers.
7. Append a completion summary here and move this file to `.tasks/done/00222_verify_emf_emfplus_backend_status.md`.

## Goals

- `docs/emf.md` has explicit per-backend status that matches implementation evidence.
- `docs/emfplus.md` has explicit per-backend status that matches implementation evidence and keeps EMF+ specification content primary.
- Remaining `Partial` or `Parsed/ignored` statuses are intentional and explained.
- No implementation files are changed unless verification finds a clearly scoped missing implementation.

## File List

- `.tasks/00222_verify_emf_emfplus_backend_status.md`
- `docs/emf.md`
- `docs/emfplus.md`

## Current Status

- Status: completed.
- Next step: none.
- Required context to resume: use the implementation files listed above as source of truth for wmf2svg-specific status; do not revert the existing uncommitted WMF documentation changes.

## Completion Summary

- Cross-checked `docs/emf.md` against `EmfGdi`, `SvgGdi`, and `AwtGdi`.
- Updated EMF status entries where `EmfGdi` serializes native records and where `AwtGdi` has concrete Java2D/raster implementations for ROP2, stretch mode, flood fill, widened paths, region inversion, and bitmap transfer records.
- Kept EMF `Partial` statuses for font mapper behavior, SVG raster/ROP fidelity, color-management conversion, extended pen details, and layout mirroring where implementation stores state or approximates output rather than fully emulating GDI.
- Cross-checked `docs/emfplus.md` against `EmfPlusParser` and EMF+ handlers in `SvgGdi` and `AwtGdi`.
- Updated EMF+ status entries for control records, rendering-origin state, Java2D rendering hints, compositing, terminal-server graphics state, and terminal-server clip handling.
- Verified `docs/emf.md` has 121 EMF headings, 121 status blocks, and 121 entries for each of `EmfGdi`, `SvgGdi`, and `AwtGdi`.
- Verified `docs/emfplus.md` has 58 EMF+ headings, 58 status blocks, and 58 entries for each of `WmfGdi`, `EmfGdi`, `SvgGdi`, and `AwtGdi`.
- No implementation files were changed; no Maven test run was required.
