# 00167 Investigate p0000016 font difference

## Purpose
Determine whether the visible difference in `p0000016.png` is caused by font selection/substitution rather than only antialiasing.

## Context
- The user observed that the generated image shows a visible `Z`/glyph-shape difference.
- Current comparison files:
  - reference: `../wmf-testcase/data/png/p0000016.png`
  - generated: `/tmp/wmf2png-p0000016-diff/actual.png`
- Remaining AE for `p0000016` is `4969`.
- Prior analysis found matching high-level GDI+ APIs and DLL versions; DPI awareness did not remove this file's difference.

## Tasks
- [ ] Parse WMF font creation records from `p0000016.wmf`.
- [ ] Identify font face names, charset, pitch/family, weight, italic, and escapement/orientation.
- [ ] Check whether those font faces are installed or substituted on the current Windows environment.
- [ ] Compare visual diff regions against the parsed font records and summarize likely cause.

## Goals
- Evidence-based answer on whether font substitution is plausible.
- Paths to generated visual diff artifacts.
- No production code changes.

## File List
- `.tasks/00167_investigate_p0000016_font_difference.md`
- `/tmp/wmf2png-p0000016-diff/`

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: `p0000016.wmf` is `../wmf-testcase/data/src/p0000016.wmf`.

## Summary
- Parsed `CREATEFONTINDIRECT` records from `p0000016.wmf`.
- Requested fonts:
  - `Times New Roman`, regular and italic, heights `-384` and `-224`, charset `0`.
  - `Symbol`, regular and italic, height `-384`, charset `2`.
  - `MT Extra`, height `-384`, charset `2`.
  - `System`, height `16`, width `7`, weight `700`, charset `0`.
- Windows installed font check:
  - `Times New Roman` is installed (`times.ttf`, plus bold/italic variants).
  - `Symbol` is installed (`symbol.ttf`).
  - `MT Extra` is installed (`MTEXTRA.TTF`).
  - `System` is not listed as a modern installed TrueType family, but is a legacy logical/raster face.
- Text records use mostly one-character `EXTTEXTOUT` calls. Large diff regions line up mostly with glyphs rendered using `Times New Roman` regular/italic and some symbol-font entries.

## Decision
- A completely missing font is unlikely for the visible differences, because the suspicious special faces (`Symbol`, `MT Extra`) are installed.
- Font rendering/mapping can still be part of the difference, especially for legacy `System` and SYMBOL_CHARSET paths, but the current evidence points more to a subtle GDI+/WMF playback text-rendering state difference than to a simple missing-font substitution.
