# 00168 Explain GDI+ Symbol/MT Extra font loading behavior

## Purpose
Explain why `Symbol` and `MT Extra` can exist as installed fonts but still fail or behave differently in GDI+ WMF playback.

## Context
- `p0000016.wmf` requests `Symbol` and `MT Extra` with `charset=2` (`SYMBOL_CHARSET`).
- Installed font collection shows both `Symbol` and `MT Extra`.
- The user suspects the investigation result that these fonts are not effectively loaded/used by GDI+ is correct.
- Need distinguish installed-font visibility, GDI+ FontFamily creation, and WMF/GDI symbol charset byte-to-glyph mapping.

## Tasks
- [ ] Check whether `System.Drawing.FontFamily` / `Font` can create `Symbol` and `MT Extra`.
- [ ] Inspect registered font file names and basic font metadata if accessible.
- [ ] Summarize the difference between installed font presence and GDI+/WMF symbol charset rendering.

## Goals
- Clear explanation grounded in local evidence.
- No production code changes.

## File List
- `.tasks/00168_explain_gdiplus_symbol_font_loading.md`

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: target fonts are `Symbol` and `MT Extra`; WMF uses charset `2`.

## Summary
- `System.Drawing.FontFamily` can create `Symbol`, `MT Extra`, and `Times New Roman`.
- `System.Drawing.Font` can create regular and italic fonts for those three families.
- `System` cannot be created as a GDI+ `FontFamily`; it is a legacy logical/raster face rather than a normal installed TrueType family.
- Created `Font` objects reported `GdiCharSet=1` from the normal GDI+ constructor path, while the WMF `LOGFONT` requests `charset=2` (`SYMBOL_CHARSET`) for `Symbol` and `MT Extra`.

## Interpretation
- The issue is probably not that `Symbol` or `MT Extra` font files are missing.
- The sharper distinction is between:
  - family-name availability in GDI+ (`FontFamily("Symbol")` works), and
  - faithful WMF playback of an old `LOGFONT` with `SYMBOL_CHARSET` and byte-oriented `EXTTEXTOUT` records.
- GDI+ can see/create the font family, but WMF playback may still realize or map the symbol charset differently from Paint's path/native GDI state.
- `System` is a real example where the face can exist as a legacy GDI logical font but not as a GDI+ family.

## Decision
- Say "GDI+ cannot load the font" only with nuance. For `Symbol` and `MT Extra`, local evidence says GDI+ can create the families.
- The likely failure mode is charset/glyph mapping or font realization during WMF playback, not simple missing-font loading.
