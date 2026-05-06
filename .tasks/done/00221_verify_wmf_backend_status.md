# Verify WMF Backend Status

## Purpose
Verify whether `docs/wmf.md` backend statuses marked `Partial`, `Parsed/ignored`, or `Passed through/comment` for `WmfGdi` and `AwtGdi` are accurate, and implement missing support where the code is actually incomplete and the behavior is practical to add.

## Context
- User questioned `WmfGdi` and `AwtGdi` `Partial`/`Parsed` statuses in `docs/wmf.md`.
- Target docs: `docs/wmf.md`.
- Relevant implementations:
  - `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfParser.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfGdi.java`
  - `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`

## Current Status
- Status: completed
- Next step: none.
- Required context to resume: preserve WMF record inventory and support-status structure; only change implementation if an unimplemented method has a clear, testable behavior.

## Tasks
1. List `WmfGdi`/`AwtGdi` statuses currently marked `Partial`, `Parsed/ignored`, or `Passed through/comment`.
2. Inspect corresponding parser and backend methods.
3. Implement missing practical support if found.
4. Update `docs/wmf.md` statuses to match actual behavior.
5. Run focused tests or compile checks if code changes are made; otherwise run docs consistency checks.
6. Append completion summary and move this file to `.tasks/done/00221_verify_wmf_backend_status.md`.

## Goals
- `docs/wmf.md` no longer marks `WmfGdi` or `AwtGdi` as partial/parsed unless that is technically justified.
- Any newly implemented support is covered by focused verification.
- Worktree remains clean except intended changes.

## File List
- `.tasks/00221_verify_wmf_backend_status.md`
- `docs/wmf.md`
- Implementation files only if support is actually missing.

## Completion Summary
- Inspected `WmfGdi` and `AwtGdi` implementations for WMF records marked `Partial`, `Parsed/ignored`, or `Passed through/comment`.
- Corrected `docs/wmf.md` where `WmfGdi` was incorrectly marked partial/parsed even though it serializes native WMF records.
- Corrected `docs/wmf.md` where `AwtGdi` already implements ROP2, stretch interpolation, flood fill, PATBLT ROPs, region inversion, embedded bitmap ROPs, and embedded EMF/EMF+ escape replay.
- Left `AwtGdi` as partial/parsed only for `META_SETRELABS`, `META_SETLAYOUT`, and `META_SETMAPPERFLAGS`, because those are obsolete/no-effect or host/font/layout fidelity limitations rather than missing record handlers.
- No implementation files were changed.
- Verified WMF coverage remains 70 constants, 70 headings, and 70 status blocks.
