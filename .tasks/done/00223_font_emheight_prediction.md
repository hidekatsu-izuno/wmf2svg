# Font Emheight Prediction

## Purpose

Determine whether Java AWT font metrics can predict the `fontmetrics.ps1` ratio:

`System.Drawing.FontFamily.GetEmHeight(Regular) / (GetCellAscent(Regular) + GetCellDescent(Regular))`

using Windows fonts under `/mnt/c/Windows/Fonts` and, if useful, downloadable font files such as Google Fonts.

## Context

- `src/test/bin/fontmetrics.ps1` reports `font-emheight.<family> = <ratio>` only when GDI+ em height differs from ascent plus descent.
- Java AWT `FontMetrics` exposes runtime pixel metrics such as ascent, descent, leading, and height, but may not expose raw font design units directly.
- Java can load font files with `Font.createFont(...)`; direct table inspection may be required if AWT metrics do not contain enough information.
- Network access may be restricted; local Windows fonts are the primary data source.

## Tasks

1. Status: completed. Inspect `fontmetrics.ps1` and existing Java font utilities to identify the exact target ratio and current property usage.
   Next step: build a small temporary measurement harness.
   Required context: target ratio is `emHeight / (cellAscent + cellDescent)` for regular style.

2. Status: completed. Create temporary measurement scripts outside tracked source to compare System.Drawing values against Java AWT values for local Windows fonts.
   Next step: collected 120 local Windows font-family rows in `/tmp/font-emheight-*.tsv`.
   Required context: prefer `/tmp` for throwaway scripts and outputs.

3. Status: completed. Evaluate whether a formula based only on AWT `FontMetrics` can predict the target ratio.
   Next step: no exact AWT-only formula found; candidate errors are summarized below.
   Required context: exact prediction should be distinguished from approximate correlation.

4. Status: completed. If AWT `FontMetrics` is insufficient, identify the minimal font-file data needed and test a direct TrueType/OpenType-table formula.
   Next step: direct table values explain many rows, but GDI+ selection between hhea/OS/2 win/typo metrics is not represented by AWT `FontMetrics`.
   Required context: GDI+ cell ascent/descent may correspond to a specific table pair, not to rendered pixel metrics.

5. Status: completed. Summarize conclusion, reproducible commands, caveats, and implementation options.
   Next step: move the task to `.tasks/done/`.
   Required context: do not modify production code unless the investigation shows a clear requested implementation change.

## Goals

- Provide a clear yes/no answer for AWT `FontMetrics`-only prediction.
- Provide a practical formula if one exists.
- Provide evidence from local Windows fonts and any available downloaded fonts.
- Leave the repository source unchanged unless implementation is explicitly needed.

## File List

- `src/test/bin/fontmetrics.ps1`
- `src/main/java/net/arnx/wmf2svg/util/FontUtil.java`
- `.tasks/00223_font_emheight_prediction.md`
- Temporary files under `/tmp/font-emheight-*`

## Summary

AWT `FontMetrics` alone cannot reliably predict `fontmetrics.ps1`'s
`emHeight / (cellAscent + cellDescent)` ratio.

Measured 120 rows from local Windows fonts using temporary scripts under `/tmp`.
Candidate AWT formulas:

- `fontSize / (FontMetrics.getAscent() + FontMetrics.getDescent())`
  - mean absolute error: `0.038879845`
  - max absolute error: `0.180800000` on Calibri (`0.8192` expected, `1.0` predicted)
- `fontSize / FontMetrics.getHeight()`
  - mean absolute error: `0.028126992`
  - max absolute error: `0.199057579` on Bauhaus 93 (`0.883520276` expected, `0.684462697` predicted)
- large font size variants reduced rounding but did not fix the structural mismatch.

Representative rows:

- Arial: expected `0.895104895`; large-size `size/(ascent+descent)` predicts `0.895094880`, while `size/height` predicts `0.869633276`.
- Calibri: expected `0.8192`; large-size `size/(ascent+descent)` predicts `1.0`, while `size/height` predicts `0.819195386`.
- Bauhaus 93: expected `0.883520276`; large-size `size/(ascent+descent)` predicts `0.883517105`, while `size/height` predicts `0.684715774`.

Direct font table inspection showed the target often corresponds to:

`head.unitsPerEm / selectedDesignCellHeight`

where `selectedDesignCellHeight` may be `hhea.ascent - hhea.descent`,
`OS/2.usWinAscent + OS/2.usWinDescent`, or another GDI+/font-driver-selected
metric. In the 112 rows where the simple table reader handled the font:

- hhea ratio exactly matched 62 rows.
- OS/2 typo ratio exactly matched 26 rows.
- OS/2 win ratio exactly matched 90 rows.

This means reading font tables is closer to the real source of truth than AWT
pixel metrics, but a complete implementation still needs to model GDI+'s metric
selection behavior. For this repository, the most reliable options are either:

1. Continue using generated `font-emheight.*` properties from `fontmetrics.ps1`.
2. Add a font-table parser plus a conservative GDI-compatible selection rule,
   backed by fixtures for fonts such as Arial, Calibri, Bahnschrift, Bauhaus 93,
   Cascadia Code, and Comic Sans MS.

No production source files were changed.
