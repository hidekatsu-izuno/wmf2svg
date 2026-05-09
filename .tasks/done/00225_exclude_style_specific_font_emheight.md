# Exclude Style Specific Font Emheight Entries

## Purpose

Remove `font-emheight.*` entries for installed-font families whose family name represents a style-specific face such as Bold or Italic, so `SvgGdi.properties` keeps base-family metrics instead of style-only family aliases.

## Context

- The previous task added currently generated Windows `font-emheight.*` entries.
- The user now wants Bold/Italic-style dedicated fonts excluded.
- The worktree already contains unrelated edits in `SvgGdi.properties`, `FontUtil.java`, and tests; do not revert or normalize those.
- Style-specific filtering should be conservative and based on the property key/family name.

## Tasks

1. Status: completed. Identify `font-emheight.*` entries whose family names contain style markers.
   Next step: found 74 candidate lines using decoded `\u0020` space matching.
   Required context: examples include `Bold`, `Italic`, `Light`, `Semibold`, `SemiLight`, `Demi`, `Black`, `Medium`, `Condensed`, and similar.

2. Status: completed. Remove the style-specific `font-emheight.*` lines from `SvgGdi.properties`.
   Next step: removed matching property lines only.
   Required context: preserve all unrelated ordering and existing worktree edits.

3. Status: completed. Verify that style-specific keys are gone and base-family keys remain.
   Next step: style-marker scan returned no remaining `font-emheight.*` matches; base examples remain.
   Required context: full Maven tests may still be blocked by pre-existing `FontUtil.java` changes.

4. Status: completed. Record summary and move this task to `.tasks/done/`.
   Next step: move this task to `.tasks/done/`.
   Required context: mention any test blockers separately.

## Goals

- Remove style-specific `font-emheight.*` entries.
- Keep base font-family metrics such as Arial, Calibri, Segoe UI, Yu Gothic, and Meiryo.
- Avoid touching unrelated modified files or unrelated property entries.

## File List

- `src/main/resources/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties`
- `.tasks/00225_exclude_style_specific_font_emheight.md`

## Summary

Removed 74 `font-emheight.*` property lines whose decoded family names contain
style-specific tokens as standalone words:

- `Bold`, `Italic`, `Light`, `Semibold`, `SemiBold`, `Semilight`, `SemiLight`
- `Demi`, `Black`, `Medium`, `Condensed`, `Heavy`
- `Extra Bold`, `Extra Light`, `ExtraBold`, `ExtraLight`, `Demibold`
- truncated variable-font suffixes observed from GDI+ family names: `Semib`,
  `Semil`, `Semibol`, `Semilig`

Verification:

- Re-ran the same style-marker scan against `SvgGdi.properties`; no matching
  `font-emheight.*` lines remained.
- Counted remaining `font-emheight.*` entries: 231.
- Spot-checked that base entries such as `font-emheight.Arial` and
  `font-emheight.Calibri` remain.

No Maven test was rerun in this task because the previous verification attempt
was blocked by pre-existing `FontUtil.java` edits that remove
`fontCharset(String)`.
