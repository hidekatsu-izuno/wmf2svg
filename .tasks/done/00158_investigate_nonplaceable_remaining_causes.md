# Investigate Nonplaceable Remaining Causes

## Purpose
Investigate why some non-placeable WMFs still differ from Paint even though successful and differing non-placeable files use the same observed Paint load/draw API path.

## Context
- Both success and failure non-placeable groups hit `GdipCreateBitmapFromFile` and `GdipDrawImageRectRectI` from the same Paint wrapper stacks.
- Remaining visible differences after the current script are six non-placeable files.
- Prior evidence suggests the next causes may be WMF record/state details, GDI+ WMF interpretation, Paint canvas/image creation state, alpha normalization, or PNG export/post-processing.
- Do not change rendering code in this task unless the investigation identifies a precise, verifiable change.

## Tasks
- [x] Build a per-file non-placeable metadata matrix.
  - Status: completed; collected map mode, window/viewport records, GDI+ image size/DPI, bitmap size/DPI, record classes, and current diff metrics.
  - Next step: none.
  - Required context: include visible-diff, alpha-only, and full-match non-placeable files.
- [x] Compare render variants for remaining six files.
  - Status: completed; compared `image-direct`, `image-canvas-source`, and `bitmap-rect` variants for all six remaining visible-diff files.
  - Next step: none.
  - Required context: distinguish `Image.FromFile` direct draw, `Bitmap(path)` rectangle draw, and canvas-source rectangle draw.
- [x] Investigate alpha/export characteristics.
  - Status: completed; compared alpha distributions and RGB-ignoring-alpha metrics for diff, alpha-only, and matching examples.
  - Next step: none.
- [x] Check Paint/GDI+ state clues.
  - Status: completed; targeted trace showed `GdipGetImageWidth` is called from `BasePaint::Image::Create+0x54`; existing draw traces remain the stronger canvas argument evidence.
  - Next step: none.
- [x] Summarize likely causes and next concrete experiments.
  - Status: completed; findings appended below.
  - Next step: move task to `.tasks/done/`.

## Goals
- Rank plausible remaining causes with evidence.
- Identify which files belong to which cause bucket.
- Avoid broad fallbacks or filename-specific behavior.

## File List
- `.tasks/00158_investigate_nonplaceable_remaining_causes.md`
- `.tasks/00158_variant_render.ps1`
- `.tasks/00158_cdb_size_commands.txt`

## Summary

No production rendering code was changed in this task.

### Main Buckets

The six remaining visible-diff non-placeable files split into two different cause buckets:

- No `SETMAPMODE`, GDI+ natural image size differs from WMF canvas:
  - `image6`: canvas `576x544`, GDI+ image/bitmap `1311x813`.
  - `p0000001`: canvas `4800x1792`, GDI+ image/bitmap `1881x1012`.
  - `p0000016`: canvas `8192x608`, GDI+ image/bitmap `1958x971`.
  - `sample_03`: canvas `200x200`, GDI+ image/bitmap `2750x1728`, includes repeated `SETVIEWPORTEXT`, one negative viewport extent, and `INTERSECTCLIPRECT`.
  - `sample_05`: canvas `100x100`, GDI+ image/bitmap `854x534`, only one rectangle draw after basic window setup.
- `SETMAPMODE=8`, GDI+ natural image size matches WMF canvas:
  - `texts`: canvas and GDI+ image/bitmap are all `8204x3735`; remaining difference is only `AE=22`.

The full-match `SETMAPMODE=8` group also has matching canvas/GDI+ dimensions, which explains why the previous `Bitmap(path)` path is compatible with that group.

### Render Variant Evidence

Compared three controlled variants for the six remaining files:

- `image-direct`: `Image.FromFile`, `DrawImage(image, 0, 0, canvasWidth, canvasHeight)`.
- `image-canvas-source`: `Image.FromFile`, `DrawImage(image, destRect, 0, 0, canvasWidth, canvasHeight, UnitPixel)`.
- `bitmap-rect`: `Bitmap(path)`, `DrawImage(image, destRect, 0, 0, image.Width, image.Height, UnitPixel)`.

Results:

- `image6`: best visible AE is `1268` with `image-canvas-source` or `bitmap-rect`; current `image-direct` gives `2557`.
- `p0000001`: best visible AE is `625` with `image-direct`; `image-canvas-source` worsens to `20417`, `bitmap-rect` to `157318`.
- `p0000016`: visible AE is `4969` for all three; alpha AE varies substantially.
- `sample_03`: best visible AE is `2347` with `image-direct`.
- `sample_05`: best visible AE is `7300` with `image-direct`, but alpha remains wrong.
- `texts`: `image-direct` and `bitmap-rect` both stay at `AE=22`; `image-canvas-source` is catastrophically wrong.

Interpretation: the remaining differences cannot be solved by one global `DrawImage` overload choice. Even among no-`SETMAPMODE` files, `image6` prefers a different variant from `p0000001`, `sample_03`, and `sample_05`.

### Alpha / Export Evidence

Alpha maps are effectively bilevel in the compared outputs: no partial alpha bucket was observed in the tested files. Differences are changes in the area classified as fully transparent vs fully opaque, not PNG encoder quantization or fractional alpha.

Current output examples:

- `sample_05`: Paint baseline alpha has `7500` transparent and `2500` opaque pixels; current output is `10000` opaque pixels. This is a transparency/background handling mismatch, not an antialias/export-only issue.
- `p0000016`: baseline alpha has `368404` opaque pixels; current output has `1928693` opaque pixels. Visible AE is only `4969`, so much of the remaining mismatch is alpha mask area outside visible RGB changes.
- `texts`: alpha mean and alpha map are almost identical; residual is only `22` pixels and should be treated as a tiny rasterization/detail issue.
- `12e9` and `sample_01` have visible AE `0` but alpha differences, confirming that some "success" files are only visibly equal, not alpha-identical.

Comparing with alpha disabled produced the same AE as visible AE for the tested files, which means hidden transparent RGB values are not the main explanation. The mismatches are in visible RGB and/or alpha mask area.

### Paint / GDI+ State Evidence

Prior task `00157` showed successful non-placeable files and differing non-placeable files share:

- `GdipCreateBitmapFromFile` from `ImageProcessing!GdiplusHelpers::LoadFileImage+0xa6`.
- `GdipDrawImageRectRectI` from `BasePaint::Image::Create+0x2ac`.
- Full destination/source rectangles using Paint's canvas size with source unit `2` (`UnitPixel`).

Additional targeted trace in this task showed `GdipGetImageWidth` is called from `BasePaint::Image::Create+0x54` for both success and failure examples. This supports the idea that Paint's image creation path queries GDI+ image dimensions early, but the available trace did not yet capture return values.

### Ranked Likely Causes

1. For `sample_05`, `p0000016`, and much of `p0000001`: alpha/background normalization around Paint canvas creation is the strongest lead. The alpha area differs by far more pixels than visible RGB.
2. For no-`SETMAPMODE` files generally: GDI+ WMF interpretation depends on ambiguous mapping state where WMF canvas and GDI+ natural size diverge. Paint still draws using canvas-sized source rectangles, but reproducing that from `System.Drawing` is not equivalent for every file.
3. For `sample_03`: viewport/clip state is suspicious (`SETVIEWPORTEXT`, negative viewport extent, `INTERSECTCLIPRECT`) and likely interacts with GDI+ mapping.
4. For `texts`: the issue is not the no-map/canvas-size class; it is a small rendering-detail difference in a large `SETMAPMODE=8` file.
5. PNG export format itself is unlikely to be the root cause because alpha is bilevel and hidden RGB is not the main source of differences.

### Next Experiments

- Trace return values for `GdipGetImageWidth` and `GdipGetImageHeight`, or inspect the GDI+ image object after load, to confirm whether Paint sees canvas dimensions or natural GDI+ dimensions before `BasePaint::Image::Create`.
- Trace canvas bitmap creation inside `BasePaint::Image::Create`, especially `GdipCreateBitmapFromScan0`, `GdipCreateBitmapFromGraphics`, `GdipCreateBitmapFromHBITMAP`, or related pixel-format/background initialization calls.
- For alpha-heavy files, test whether Paint clears with a transparent sentinel/color and post-processes a specific background color to alpha.
- For `sample_03`, isolate viewport/clip records with synthetic WMFs or trace GDI+ playback state if possible.
