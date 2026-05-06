# Refocus Metafile Docs on Specifications

## Purpose
Make `docs/emfplus.md`, `docs/wmf.md`, and `docs/emf.md` specification-first references. Keep wmf2svg-specific material only as support status or concise implementation notes.

## Context
- `docs/emfplus.md` currently elevates EMF+ object payloads and implementation constants to the same heading level as record APIs.
- The user wants the EMF+ specification explanation to be primary, with wmf2svg-specific information only as supplemental notes or support status.
- `docs/wmf.md` and `docs/emf.md` should be aligned to the same specification-first framing.

## Current Status
- Status: completed
- Next step: none.
- Required context to resume: preserve record sections and backend support status blocks; avoid presenting implementation constants as part of the official API inventory.

## Tasks
1. Refactor `docs/emfplus.md` so official EMF+ record sections remain primary and object payload/implementation details are supplemental.
2. Update `docs/wmf.md` and `docs/emf.md` scope/source wording to clarify specification-first intent.
3. Verify record heading counts and support status block counts remain consistent.
4. Append completion summary and move this task file to `.tasks/done/00220_refocus_metafile_docs_on_spec.md`.

## Goals
- `docs/emfplus.md` has primary sections only for official EMF+ records.
- EMF+ object payload details are described as referenced structures, not standalone record APIs.
- wmf2svg-specific details are limited to support status and implementation notes.
- WMF/EMF docs use the same specification-first framing.

## File List
- `.tasks/00220_refocus_metafile_docs_on_spec.md`
- `docs/emfplus.md`
- `docs/wmf.md`
- `docs/emf.md`

## Completion Summary
- Refocused `docs/emfplus.md` on the official EMF+ record set by replacing the former primary payload/object section and implementation-constant inventory with supplemental `Referenced EMF+ Structures` and `Implementation Notes`.
- Clarified in `docs/emfplus.md` that brush, pen, path, region, font, image, image-attribute, string-format, and custom-line-cap data are referenced structures used by records, not standalone record APIs.
- Updated `docs/wmf.md`, `docs/emf.md`, and `docs/emfplus.md` to separate specification sources from implementation references and to state that wmf2svg-specific details belong in support status or implementation notes.
- Verified heading/status counts: WMF 70/70, EMF 121/121, EMF+ 58/58.
- Verified no unfinished markers or old implementation-heavy section names remain.
