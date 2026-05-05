# Trace Texts Paint Image State

## Purpose
Trace Paint's native GDI+ image/canvas state for `texts.wmf` to identify hidden state differences behind the remaining `texts.png` AE=22 mismatch.

## Context
- `texts.wmf` uses the same observed load/draw API names as successful and differing non-placeable files.
- Public `Graphics` quality/raster settings did not improve the checked variants.
- The remaining likely causes are hidden GDI+ image state, Paint canvas bitmap creation, pixel format, DPI/resolution, alpha/background initialization, or save/export state.
- This task is investigation-only unless a precise, verifiable change is found.

## Tasks
- [x] Collect local System.Drawing state for `texts.wmf`.
  - Status: completed; values recorded in the summary.
  - Next step: none.
  - Required context: use PowerShell/System.Drawing.
- [x] Create CDB command files for Paint image/canvas state.
  - Status: completed; created `.tasks/00160_cdb_image_state_commands.txt`.
  - Next step: none.
- [x] Run Paint/CDB trace for `texts.wmf`.
  - Status: completed; captured logs under `/tmp/wmf2png-00160-trace/`.
  - Next step: none.
- [x] Interpret findings against the current script.
  - Status: completed; compared Paint-observed state with `System.Drawing` state and ran a focused DPI experiment.
  - Next step: none.
- [x] Summarize results.
  - Status: completed; summary appended below.
  - Next step: move task to `.tasks/done/`.

## Goals
- Evidence on pixel format, DPI/resolution, canvas creation, clear/background, and save-related GDI+ calls.
- A grounded next step for the remaining `texts.png` 22px difference.

## File List
- `.tasks/00160_trace_texts_paint_image_state.md`
- `.tasks/00160_cdb_image_state_commands.txt`

## Summary

No production rendering code was changed.

Local `System.Drawing` state for `texts.wmf`:

- `Image.FromFile`:
  - `Width=8204`, `Height=3735`
  - `DpiX=127.6649`, `DpiY=127.5907`
  - `PixelFormat=Format32bppRgb`
  - `Flags=327683`
  - `RawFormat=b96b3cac-0728-11d3-9d7b-0000f81ef32e`
- `Bitmap(path)`:
  - `Width=8204`, `Height=3735`
  - `DpiX=96`, `DpiY=96`
  - `PixelFormat=Format32bppArgb`
  - `Flags=2`
  - `RawFormat=b96b3caa-0728-11d3-9d7b-0000f81ef32e`

Paint/CDB observations:

- `GdipCreateBitmapFromFile` returns image handle `000001ce64b06350` in one captured run.
- `GdipGetImagePixelFormat` is called on the same source image handle before `BasePaint::Image::Create` draw.
- By saving the out-param pointer and reading it at the draw breakpoint, Paint's source image pixel format was observed as `0x0026200A`, which is `Format32bppArgb`.
- `GdipGetImageHorizontalResolution` is called on the same source image handle. The saved out-param showed `0x43100000`, which is float `144.0`, before draw.
- The vertical DPI out-param was overwritten by the draw stack before it could be read reliably.
- `GdipDrawImageRectRectI` still uses:
  - source image handle `0000018cbbb06350` in the captured state run,
  - destination `(0,0,8204,3735)`,
  - source `(0,0,8204,3735)`,
  - source unit `2` (`UnitPixel`).
- Breakpoints on `GdipCreateBitmapFromScan0`, `GdipCreateBitmapFromGraphics`, `GdipCreateBitmapFromHBITMAP`, and `GdipGraphicsClear` did not hit before the first `GdipDrawImageRectRectI` in the entry-only canvas trace.

Focused DPI experiment:

- Rendered `texts.wmf` with current `Bitmap(path)` rectangle draw while changing source and destination bitmap resolution:
  - source 144 DPI / destination 96 DPI
  - source 144 DPI / destination 144 DPI
  - original source DPI / destination 144 DPI
- All remained `AE=22`, `AlphaAE=22`.

Interpretation:

- Paint's source image state before draw appears closer to `.NET Bitmap(path)` than `.NET Image.FromFile` for pixel format: both Paint and `Bitmap(path)` are `Format32bppArgb`, while `Image.FromFile` reports `Format32bppRgb`.
- The likely Paint horizontal DPI value of `144.0` is different from both `Image.FromFile` (`~127.66`) and `Bitmap(path)` (`96`), but simply changing source/output bitmap DPI to 144 did not affect the 22px difference.
- The remaining `texts.png` difference is not explained by public `Graphics` quality settings, source/output bitmap DPI, or a different exported canvas creation API before draw.
- The most plausible remaining explanation is still a tiny GDI+ WMF rasterization/internal state difference before or inside `GdipCreateBitmapFromFile`, or a native wrapper detail not visible through `System.Drawing`'s managed object state.

Trace artifacts:

- `/tmp/wmf2png-00160-trace/texts.image_state.log`
- `/tmp/wmf2png-00160-trace/texts.state_min.log`
- `/tmp/wmf2png-00160-trace/texts.state_values_at_draw.log`
- `/tmp/wmf2png-00160-trace/texts.dpi_values_at_draw.log`
- `/tmp/wmf2png-00160-trace/texts_src144_dst96.png`
- `/tmp/wmf2png-00160-trace/texts_src144_dst144.png`
- `/tmp/wmf2png-00160-trace/texts_src0_dst144.png`
