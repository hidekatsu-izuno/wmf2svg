# 00170 Remove emf2png and commit rendering changes

## Purpose
Remove the now-redundant `emf2png.ps1` script and commit all pending rendering changes.

## Context
- `wmf2png.ps1` now accepts EMF input.
- EMF output uses a transparent base as requested.
- GDI+ v3 pre-initialization resolved all WMF Paint reference differences in the 57-file test set.
- User asked to delete `emf2png.ps1` and commit all changes.

## Tasks
- [ ] Delete `src/test/bin/emf2png.ps1`.
- [ ] Verify `wmf2png.ps1` still renders a sample EMF.
- [ ] Stage all pending changes.
- [ ] Commit the staged changes.

## Goals
- `src/test/bin/emf2png.ps1` is removed.
- Pending task notes and `wmf2png.ps1` changes are committed.
- Working tree is clean after commit.

## File List
- `src/test/bin/emf2png.ps1`
- `src/test/bin/wmf2png.ps1`
- `.tasks/done/00166_test_bitmap_load_for_all_nonplaceable.md`
- `.tasks/done/00167_investigate_p0000016_font_difference.md`
- `.tasks/done/00168_explain_gdiplus_symbol_font_loading.md`
- `.tasks/done/00169_test_gdiplus_v3_startup.md`
- `.tasks/00170_remove_emf2png_and_commit.md`

## Status
- Current status: completed.
- Next step: commit staged changes.
- Required context to resume: sample EMF is `src/test/data/emf/fulltest.emf`.

## Summary
- Deleted `src/test/bin/emf2png.ps1`.
- Verified `wmf2png.ps1` renders `src/test/data/emf/fulltest.emf`.
- Verification output:
  - dimensions: `1061x794`
  - alpha range: `0..1`
