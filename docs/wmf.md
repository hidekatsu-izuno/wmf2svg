# WMF Record API Reference

This document summarizes the official Windows Metafile (WMF) record APIs and the current wmf2svg support status for each record. It is intended as a coverage map for future full-support tests, not as a replacement for the binary format specification.

Specification source:
- Microsoft Open Specifications, [MS-WMF]: RecordType Enumeration: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-wmf/77db8158-96df-4656-a36c-3066de3d5f59

Implementation reference:
- wmf2svg parser/backend status is cross-checked against `WmfConstants`, `WmfParser`, `WmfGdi`, `SvgGdi`, and `AwtGdi`.

## Format Overview

WMF is a 16-bit GDI command stream. Each record has a size, a 16-bit `RecordFunction`, and record-specific parameters. Playback mutates a device-context state: mapping, viewport/window transforms, selected pens/brushes/fonts/palettes/regions, text attributes, clipping, and raster-operation modes.

The record sections below describe the WMF specification first: category, purpose, payload/effect, and test relevance. wmf2svg-specific details are limited to support status and implementation notes.

wmf2svg parses WMF records and replays them to a `Gdi` backend. SVG, AWT raster output, and WMF writing can differ in how fully they represent a replayed GDI operation, so support is listed separately by backend.

Backend status entries use the same support vocabulary but apply it to different output paths:

- `WmfGdi`: whether replay can be serialized back to WMF records. Records that are already native WMF operations are usually written directly; operations with no WMF output effect or with device-dependent semantics may be ignored or approximate.
- `SvgGdi`: whether replay can be represented in SVG. Vector geometry and text are usually direct, while raster operations, palette effects, clipping, and font metrics can be approximate.
- `AwtGdi`: whether replay can be rendered through Java2D/ImageIO. Raster operations and clipping are often closer to GDI than SVG, while font availability, antialiasing, and host rendering still affect output.

## Support Status Legend

- Supported: parsed and replayed to a concrete `Gdi` operation.
- Partial: parsed, but rendering/writing can be approximate, backend-specific, or missing some flags.
- Parsed/ignored: recognized and intentionally ignored because it has no SVG/raster effect or is obsolete/reserved.
- Passed through/comment: preserved or forwarded as private data rather than interpreted as a drawing operation.
- Unsupported/unknown: not recognized by the parser.

## Records

### META_EOF (0x0000)

- Category: control.
- Purpose: terminates the WMF record stream.
- Key payload/effect: no drawing payload; closes playback.
- Rendering/test relevance: every valid WMF fixture should end with this record.
- wmf2svg status:
  - WmfGdi: Supported; parser stops after replaying footer logic.
  - SvgGdi: Supported; parser stops after replaying footer logic.
  - AwtGdi: Supported; parser stops after replaying footer logic.

### META_REALIZEPALETTE (0x0035)

- Category: palette.
- Purpose: maps the currently selected logical palette into the system palette model.
- Key payload/effect: no per-entry payload; affects subsequent palette-indexed colors.
- Rendering/test relevance: useful for palette-DIB and indexed color tests.
- wmf2svg status:
  - WmfGdi: Supported; replayed as `realizePalette`, with backend-dependent color effects.
  - SvgGdi: Supported; replayed as `realizePalette`, with backend-dependent color effects.
  - AwtGdi: Supported; replayed as `realizePalette`, with backend-dependent color effects.

### META_SETPALENTRIES (0x0037)

- Category: palette.
- Purpose: changes RGB values in a range of entries in a logical palette.
- Key payload/effect: palette handle, start index, count, and palette entries.
- Rendering/test relevance: test palette mutation before drawing indexed images or palette-selected objects.
- wmf2svg status:
  - WmfGdi: Supported; parsed and applied when the selected object is a `GdiPalette`.
  - SvgGdi: Supported; parsed and applied when the selected object is a `GdiPalette`.
  - AwtGdi: Supported; parsed and applied when the selected object is a `GdiPalette`.

### META_SETBKMODE (0x0102)

- Category: state.
- Purpose: sets the background mix mode used by text, hatched brushes, and non-solid pens.
- Key payload/effect: transparent or opaque background mode.
- Rendering/test relevance: affects text background rectangles and patterned strokes/fills.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETMAPMODE (0x0103)

- Category: coordinate state.
- Purpose: selects the mapping mode used to transform logical coordinates.
- Key payload/effect: map mode such as text, metric, isotropic, or anisotropic.
- Rendering/test relevance: central for coordinate-system coverage.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETROP2 (0x0104)

- Category: raster state.
- Purpose: sets the foreground binary raster operation for line and fill drawing.
- Key payload/effect: ROP2 mode.
- Rendering/test relevance: many SVG backends can only approximate non-copy raster operations.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_SETROP2` record.
  - SvgGdi: Partial; parsed and replayed, with backend-specific rendering fidelity.
  - AwtGdi: Supported; applies all defined ROP2 modes for stroked and filled shapes.

### META_SETRELABS (0x0105)

- Category: reserved/obsolete state.
- Purpose: undefined by the WMF specification and required to be ignored by consumers.
- Key payload/effect: none for conforming playback.
- Rendering/test relevance: include as a negative test to ensure it does not alter output.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_SETRELABS` record when replayed.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored; the obsolete state is retained but has no drawing effect.

### META_SETPOLYFILLMODE (0x0106)

- Category: state.
- Purpose: sets polygon fill mode, typically alternate or winding.
- Key payload/effect: fill-rule selector.
- Rendering/test relevance: required for self-intersecting polygon tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETSTRETCHBLTMODE (0x0107)

- Category: raster state.
- Purpose: selects bitmap stretch mode for scaled blits.
- Key payload/effect: stretch filtering/color mode.
- Rendering/test relevance: compare nearest/filtered DIB scaling where backend supports it.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_SETSTRETCHBLTMODE` record.
  - SvgGdi: Partial; parsed and replayed, with backend-dependent filtering.
  - AwtGdi: Supported for rendering; maps halftone to bilinear interpolation and other modes to nearest-neighbor behavior.

### META_SETTEXTCHAREXTRA (0x0108)

- Category: text state.
- Purpose: adds extra spacing between characters during text output.
- Key payload/effect: signed spacing value.
- Rendering/test relevance: important for text layout fidelity.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_RESTOREDC (0x0127)

- Category: state stack.
- Purpose: restores a playback device context saved by `META_SAVEDC`.
- Key payload/effect: saved state index, often negative relative to current stack.
- Rendering/test relevance: should restore transform, clipping, selected objects, and drawing state.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_RESIZEPALETTE (0x0139)

- Category: palette.
- Purpose: changes the size of a logical palette.
- Key payload/effect: palette handle and new entry count.
- Rendering/test relevance: palette lifecycle and mutation coverage.
- wmf2svg status:
  - WmfGdi: Supported when the referenced object is a palette.
  - SvgGdi: Supported when the referenced object is a palette.
  - AwtGdi: Supported when the referenced object is a palette.

### META_DIBCREATEPATTERNBRUSH (0x0142)

- Category: object creation.
- Purpose: creates a pattern brush from a DIB.
- Key payload/effect: usage mode and DIB data.
- Rendering/test relevance: patterned fills using embedded bitmap data.
- wmf2svg status:
  - WmfGdi: Supported for DIB-backed pattern creation; actual tiling fidelity is backend-specific.
  - SvgGdi: Supported for DIB-backed pattern creation; actual tiling fidelity is backend-specific.
  - AwtGdi: Supported for DIB-backed pattern creation; actual tiling fidelity is backend-specific.

### META_SETLAYOUT (0x0149)

- Category: state.
- Purpose: sets layout orientation such as right-to-left mirroring.
- Key payload/effect: layout flags.
- Rendering/test relevance: text and coordinate mirroring tests.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_SETLAYOUT` record.
  - SvgGdi: Partial; parsed and replayed, with backend-specific behavior.
  - AwtGdi: Partial; stores layout state, but full GDI mirroring/orientation behavior is not applied to all drawing operations.

### META_SETBKCOLOR (0x0201)

- Category: color state.
- Purpose: sets the background color in the playback device context.
- Key payload/effect: RGB color.
- Rendering/test relevance: affects opaque text backgrounds and hatch/pattern interactions.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETTEXTCOLOR (0x0209)

- Category: text state.
- Purpose: sets the current text color.
- Key payload/effect: RGB color.
- Rendering/test relevance: basic text-color fixture coverage.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETTEXTJUSTIFICATION (0x020A)

- Category: text state.
- Purpose: defines extra spacing to distribute across break characters for justified text.
- Key payload/effect: break extra and break count.
- Rendering/test relevance: test text layout with spaces and justification.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETWINDOWORG (0x020B)

- Category: coordinate state.
- Purpose: sets the logical window origin.
- Key payload/effect: x and y origin.
- Rendering/test relevance: validates logical-to-device coordinate offsets.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETWINDOWEXT (0x020C)

- Category: coordinate state.
- Purpose: sets the logical window extents.
- Key payload/effect: x and y extents.
- Rendering/test relevance: validates scaling and axis inversion.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETVIEWPORTORG (0x020D)

- Category: coordinate state.
- Purpose: sets the viewport origin.
- Key payload/effect: x and y device-space origin.
- Rendering/test relevance: validates output placement.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETVIEWPORTEXT (0x020E)

- Category: coordinate state.
- Purpose: sets the viewport extents.
- Key payload/effect: x and y viewport size.
- Rendering/test relevance: validates anisotropic scaling.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_OFFSETWINDOWORG (0x020F)

- Category: coordinate state.
- Purpose: offsets the logical window origin.
- Key payload/effect: horizontal and vertical deltas.
- Rendering/test relevance: stateful coordinate tests after previous origin settings.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_OFFSETVIEWPORTORG (0x0211)

- Category: coordinate state.
- Purpose: offsets the viewport origin.
- Key payload/effect: horizontal and vertical deltas.
- Rendering/test relevance: stateful output placement tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_LINETO (0x0213)

- Category: drawing.
- Purpose: draws a line from the current position to the specified point.
- Key payload/effect: endpoint coordinates and current pen.
- Rendering/test relevance: basic stroke and current-position coverage.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_MOVETO (0x0214)

- Category: drawing state.
- Purpose: sets the current output position.
- Key payload/effect: new current point.
- Rendering/test relevance: required before line, arc-to-like, and text-position sequences.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_OFFSETCLIPRGN (0x0220)

- Category: clipping.
- Purpose: moves the current clipping region by a delta.
- Key payload/effect: x and y offsets.
- Rendering/test relevance: clipping-region state tests.
- wmf2svg status:
  - WmfGdi: Supported where backend clipping supports region movement.
  - SvgGdi: Supported where backend clipping supports region movement.
  - AwtGdi: Supported where backend clipping supports region movement.

### META_FILLREGION (0x0228)

- Category: region drawing.
- Purpose: fills a region with a specified brush.
- Key payload/effect: region object and brush object.
- Rendering/test relevance: region object creation plus fill semantics.
- wmf2svg status:
  - WmfGdi: Supported when region and brush objects can be resolved.
  - SvgGdi: Supported when region and brush objects can be resolved.
  - AwtGdi: Supported when region and brush objects can be resolved.

### META_SETMAPPERFLAGS (0x0231)

- Category: font state.
- Purpose: configures how logical fonts are mapped to physical fonts.
- Key payload/effect: font mapper flags.
- Rendering/test relevance: font substitution tests.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_SETMAPPERFLAGS` record.
  - SvgGdi: Partial; parsed and replayed, but exact host font mapping is environment-dependent.
  - AwtGdi: Partial; stores mapper flags, but Java font selection remains host-font and fallback dependent.

### META_SELECTPALETTE (0x0234)

- Category: palette.
- Purpose: selects a logical palette into the playback device context.
- Key payload/effect: palette handle and background flag semantics.
- Rendering/test relevance: palette-dependent DIB rendering.
- wmf2svg status:
  - WmfGdi: Supported when the object handle is a palette.
  - SvgGdi: Supported when the object handle is a palette.
  - AwtGdi: Supported when the object handle is a palette.

### META_POLYGON (0x0324)

- Category: drawing.
- Purpose: draws and fills a closed polygon.
- Key payload/effect: point count and point array, current pen, brush, and fill mode.
- Rendering/test relevance: polygon fill-rule and closure tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_POLYLINE (0x0325)

- Category: drawing.
- Purpose: draws connected line segments.
- Key payload/effect: point count and point array, current pen.
- Rendering/test relevance: stroke join/cap and coordinate tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SCALEWINDOWEXT (0x0410)

- Category: coordinate state.
- Purpose: scales logical window extents by multiplicand/divisor ratios.
- Key payload/effect: x and y numerator/denominator pairs.
- Rendering/test relevance: coordinate transform arithmetic and divide-by-edge cases.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SCALEVIEWPORTEXT (0x0412)

- Category: coordinate state.
- Purpose: scales viewport extents by multiplicand/divisor ratios.
- Key payload/effect: x and y numerator/denominator pairs.
- Rendering/test relevance: output transform scaling tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_EXCLUDECLIPRECT (0x0415)

- Category: clipping.
- Purpose: subtracts a rectangle from the current clipping region.
- Key payload/effect: rectangle bounds.
- Rendering/test relevance: clipping exclusion tests.
- wmf2svg status:
  - WmfGdi: Supported where backend clipping can represent the resulting region.
  - SvgGdi: Supported where backend clipping can represent the resulting region.
  - AwtGdi: Supported where backend clipping can represent the resulting region.

### META_INTERSECTCLIPRECT (0x0416)

- Category: clipping.
- Purpose: intersects the current clipping region with a rectangle.
- Key payload/effect: rectangle bounds.
- Rendering/test relevance: common rectangular clipping tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_ELLIPSE (0x0418)

- Category: drawing.
- Purpose: draws and fills an ellipse within a bounding rectangle.
- Key payload/effect: bounds, current pen, current brush.
- Rendering/test relevance: oval geometry, stroke width, and fill tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_FLOODFILL (0x0419)

- Category: raster drawing.
- Purpose: flood-fills an area from a start point using the current brush.
- Key payload/effect: start point and target color semantics.
- Rendering/test relevance: hard to represent in SVG; useful as a raster backend test.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_FLOODFILL` record.
  - SvgGdi: Partial; parsed and replayed, exact behavior depends on backend support.
  - AwtGdi: Supported; performs pixel flood fill against the raster canvas and current clip.

### META_RECTANGLE (0x041B)

- Category: drawing.
- Purpose: draws and fills a rectangle.
- Key payload/effect: rectangle bounds, current pen, current brush.
- Rendering/test relevance: basic shape, pen, brush, and coordinate coverage.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETPIXEL (0x041F)

- Category: raster drawing.
- Purpose: sets one pixel to a specified color.
- Key payload/effect: point and RGB color.
- Rendering/test relevance: pixel-level raster tests; SVG may approximate as a tiny shape.
- wmf2svg status:
  - WmfGdi: Supported, with backend-specific pixel representation.
  - SvgGdi: Supported, with backend-specific pixel representation.
  - AwtGdi: Supported, with backend-specific pixel representation.

### META_FRAMEREGION (0x0429)

- Category: region drawing.
- Purpose: draws a border around a region using a brush and dimensions.
- Key payload/effect: region, brush, frame width, and frame height.
- Rendering/test relevance: region stroke/fill boundary tests.
- wmf2svg status:
  - WmfGdi: Supported when region and brush objects can be resolved; backend geometry may be approximate.
  - SvgGdi: Supported when region and brush objects can be resolved; backend geometry may be approximate.
  - AwtGdi: Supported when region and brush objects can be resolved; backend geometry may be approximate.

### META_ANIMATEPALETTE (0x0436)

- Category: palette.
- Purpose: changes entries in an existing logical palette.
- Key payload/effect: palette handle, start index, and replacement entries.
- Rendering/test relevance: palette animation/mutation coverage.
- wmf2svg status:
  - WmfGdi: Supported at parser level; visible effect depends on palette-aware rendering.
  - SvgGdi: Supported at parser level; visible effect depends on palette-aware rendering.
  - AwtGdi: Supported at parser level; visible effect depends on palette-aware rendering.

### META_TEXTOUT (0x0521)

- Category: text drawing.
- Purpose: draws a character string at a specified position.
- Key payload/effect: text bytes and target coordinates, using current font/text/background state.
- Rendering/test relevance: baseline text fixture for charset, alignment, and color.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_POLYPOLYGON (0x0538)

- Category: drawing.
- Purpose: draws and fills multiple closed polygons.
- Key payload/effect: polygon counts and point arrays.
- Rendering/test relevance: compound fill-rule and overlapping polygon tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_EXTFLOODFILL (0x0548)

- Category: raster drawing.
- Purpose: flood-fills an area using explicit fill type semantics.
- Key payload/effect: point, color, and fill type.
- Rendering/test relevance: raster-only edge cases and boundary fill behavior.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_EXTFLOODFILL` record.
  - SvgGdi: Partial; parsed and replayed, backend behavior can differ.
  - AwtGdi: Supported; implements both border-fill and surface-fill flood modes on the raster canvas.

### META_ROUNDRECT (0x061C)

- Category: drawing.
- Purpose: draws and fills a rectangle with rounded corners.
- Key payload/effect: rectangle bounds and corner ellipse dimensions.
- Rendering/test relevance: corner-radius and stroke/fill tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_PATBLT (0x061D)

- Category: raster drawing.
- Purpose: paints a rectangle with the current brush using a raster operation.
- Key payload/effect: destination rectangle and ROP.
- Rendering/test relevance: pattern brush and raster-operation tests.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_PATBLT` record.
  - SvgGdi: Partial; parsed and replayed, but complex ROPs are backend-dependent.
  - AwtGdi: Supported; evaluates pattern/destination ROP truth tables on the raster canvas.

### META_ESCAPE (0x0626)

- Category: escape/comment.
- Purpose: carries driver-specific or special escape data.
- Key payload/effect: escape function and arbitrary byte data; can contain embedded EMF comments.
- Rendering/test relevance: include enhanced-metafile escape and ignored driver escape cases.
- wmf2svg status:
  - WmfGdi: Supported for pass-through; serializes escape/comment payloads as WMF `META_ESCAPE` records.
  - SvgGdi: Passed through/comment for enhanced metafile escape data, otherwise parsed/ignored or backend-specific.
  - AwtGdi: Supported for embedded EMF/EMF+ escape payloads; other driver-specific escapes have no generic raster effect.

### META_SAVEDC (0x001E)

- Category: state stack.
- Purpose: saves the current playback device context for later restoration.
- Key payload/effect: pushes mapping, clipping, selected objects, and drawing state.
- Rendering/test relevance: nested save/restore tests for transforms, clips, and object selections.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF save-state record.
  - SvgGdi: Supported; stores SVG backend drawing state for later `META_RESTOREDC`.
  - AwtGdi: Supported; stores Java2D/backend drawing state for later `META_RESTOREDC`.

### META_INVERTREGION (0x012A)

- Category: region raster operation.
- Purpose: inverts colors in a selected region.
- Key payload/effect: region object handle.
- Rendering/test relevance: raster-effect region tests.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_INVERTREGION` record.
  - SvgGdi: Partial; parsed and replayed, but SVG/raster behavior depends on backend support for inversion.
  - AwtGdi: Supported; performs white XOR fill over the region on the raster canvas.

### META_PAINTREGION (0x012B)

- Category: region drawing.
- Purpose: paints a region using the current brush.
- Key payload/effect: region object handle.
- Rendering/test relevance: region object and brush-state tests.
- wmf2svg status:
  - WmfGdi: Supported when the region object can be resolved.
  - SvgGdi: Supported when the region object can be resolved.
  - AwtGdi: Supported when the region object can be resolved.

### META_SELECTCLIPREGION (0x012C)

- Category: clipping.
- Purpose: selects a region as the current clipping region.
- Key payload/effect: region object handle.
- Rendering/test relevance: region clipping tests.
- wmf2svg status:
  - WmfGdi: Supported where backend clipping can represent the selected region.
  - SvgGdi: Supported where backend clipping can represent the selected region.
  - AwtGdi: Supported where backend clipping can represent the selected region.

### META_SELECTOBJECT (0x012D)

- Category: object selection.
- Purpose: selects a pen, brush, font, palette, or region object into the playback device context.
- Key payload/effect: object handle.
- Rendering/test relevance: object table, stock object, and handle reuse tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_SETTEXTALIGN (0x012E)

- Category: text state.
- Purpose: sets text alignment and current-position update behavior.
- Key payload/effect: text alignment flags.
- Rendering/test relevance: baseline, centered, right-aligned, and current-position text tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_CREATEREGION (0x06FF)

- Category: object creation.
- Purpose: creates a region object.
- Key payload/effect: region scan data.
- Rendering/test relevance: prerequisite for region fill, frame, paint, invert, and clipping tests.
- wmf2svg status:
  - WmfGdi: Supported for recognized region data; complex regions are backend-dependent.
  - SvgGdi: Supported for recognized region data; complex regions are backend-dependent.
  - AwtGdi: Supported for recognized region data; complex regions are backend-dependent.

### META_ARC (0x0817)

- Category: drawing.
- Purpose: draws an elliptical arc.
- Key payload/effect: bounding rectangle and start/end radial points.
- Rendering/test relevance: arc angle conversion and transform tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_PIE (0x081A)

- Category: drawing.
- Purpose: draws and fills a pie wedge from an ellipse and two radial endpoints.
- Key payload/effect: bounding rectangle, start/end points, current pen and brush.
- Rendering/test relevance: arc fill and wedge closure tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_CHORD (0x0830)

- Category: drawing.
- Purpose: draws and fills a chord bounded by an ellipse arc and a secant line.
- Key payload/effect: bounding rectangle and start/end radial points.
- Rendering/test relevance: arc closure, fill, and stroke tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_BITBLT (0x0922)

- Category: bitmap transfer.
- Purpose: transfers a bitmap block to the destination using a raster operation.
- Key payload/effect: source/destination geometry, source bitmap data or device context, and ROP.
- Rendering/test relevance: core bitmap placement and ROP coverage.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_BITBLT` record with embedded bitmap data.
  - SvgGdi: Partial; DIB-backed cases are handled, device-dependent or complex ROP cases can vary.
  - AwtGdi: Supported for embedded bitmap data; evaluates source/destination/pattern ROP truth tables when bitmap data is present.

### META_DIBBITBLT (0x0940)

- Category: bitmap transfer.
- Purpose: transfers DIB pixels using a raster operation.
- Key payload/effect: destination geometry, source coordinates, ROP, and DIB data.
- Rendering/test relevance: DIB decoding and placement tests.
- wmf2svg status:
  - WmfGdi: Supported for embedded DIB data with backend-specific ROP limits.
  - SvgGdi: Supported for embedded DIB data with backend-specific ROP limits.
  - AwtGdi: Supported for embedded DIB data with backend-specific ROP limits.

### META_EXTTEXTOUT (0x0A32)

- Category: text drawing.
- Purpose: draws text with optional clipping/opaque rectangle and per-character spacing.
- Key payload/effect: position, string, options, optional rectangle, and optional dx array.
- Rendering/test relevance: most important WMF text fidelity record.
- wmf2svg status:
  - WmfGdi: Supported; exact font metrics and glyph substitution are environment-dependent.
  - SvgGdi: Supported; exact font metrics and glyph substitution are environment-dependent.
  - AwtGdi: Supported; exact font metrics and glyph substitution are environment-dependent.

### META_STRETCHBLT (0x0B23)

- Category: bitmap transfer.
- Purpose: transfers and scales bitmap pixels using a raster operation.
- Key payload/effect: source/destination rectangles, bitmap data/context, and ROP.
- Rendering/test relevance: bitmap scaling and ROP tests.
- wmf2svg status:
  - WmfGdi: Supported; serializes the native WMF `META_STRETCHBLT` record with embedded bitmap data.
  - SvgGdi: Partial; parsed and replayed, with backend-dependent source and ROP support.
  - AwtGdi: Supported for embedded bitmap data; scales with the selected stretch mode and evaluates bitmap ROP truth tables.

### META_DIBSTRETCHBLT (0x0B41)

- Category: bitmap transfer.
- Purpose: transfers and scales DIB pixels using a raster operation.
- Key payload/effect: DIB data, source rectangle, destination rectangle, and ROP.
- Rendering/test relevance: primary scaled-DIB coverage.
- wmf2svg status:
  - WmfGdi: Supported for embedded DIB data with backend-specific filtering/ROP behavior.
  - SvgGdi: Supported for embedded DIB data with backend-specific filtering/ROP behavior.
  - AwtGdi: Supported for embedded DIB data with backend-specific filtering/ROP behavior.

### META_SETDIBTODEV (0x0D33)

- Category: bitmap transfer.
- Purpose: copies DIB scanlines to a destination rectangle.
- Key payload/effect: scanline range, destination geometry, color usage, and DIB data.
- Rendering/test relevance: tests partial DIB scanline placement.
- wmf2svg status:
  - WmfGdi: Supported for embedded DIB data.
  - SvgGdi: Supported for embedded DIB data.
  - AwtGdi: Supported for embedded DIB data.

### META_STRETCHDIB (0x0F43)

- Category: bitmap transfer.
- Purpose: transfers DIB pixels to a destination rectangle with scaling and ROP.
- Key payload/effect: source/destination rectangles, color usage, ROP, and DIB data.
- Rendering/test relevance: broadest WMF DIB transfer fixture.
- wmf2svg status:
  - WmfGdi: Supported for common embedded DIB cases; complex ROP/filtering remains backend-dependent.
  - SvgGdi: Supported for common embedded DIB cases; complex ROP/filtering remains backend-dependent.
  - AwtGdi: Supported for common embedded DIB cases; complex ROP/filtering remains backend-dependent.

### META_CREATEPALETTE (0x00F7)

- Category: object creation.
- Purpose: creates a logical palette object.
- Key payload/effect: version and palette entries.
- Rendering/test relevance: prerequisite for palette-selection and indexed image tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_DELETEOBJECT (0x01F0)

- Category: object lifecycle.
- Purpose: deletes a pen, brush, font, palette, or region object.
- Key payload/effect: object handle.
- Rendering/test relevance: object table lifetime and handle reuse tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_CREATEPATTERNBRUSH (0x01F9)

- Category: object creation.
- Purpose: creates a pattern brush from bitmap data.
- Key payload/effect: bitmap/pattern payload.
- Rendering/test relevance: legacy pattern brush fixtures.
- wmf2svg status:
  - WmfGdi: Supported for DIB-like pattern data; backend pattern fidelity can vary.
  - SvgGdi: Supported for DIB-like pattern data; backend pattern fidelity can vary.
  - AwtGdi: Supported for DIB-like pattern data; backend pattern fidelity can vary.

### META_CREATEPENINDIRECT (0x02FA)

- Category: object creation.
- Purpose: creates a logical pen.
- Key payload/effect: pen style, width, and color.
- Rendering/test relevance: stroke style, width, dash, and null-pen tests.
- wmf2svg status:
  - WmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### META_CREATEFONTINDIRECT (0x02FB)

- Category: object creation.
- Purpose: creates a logical font.
- Key payload/effect: LOGFONT fields including size, weight, style, charset, and face name.
- Rendering/test relevance: text family, charset, style, and metric tests.
- wmf2svg status:
  - WmfGdi: Supported; exact glyph output depends on available fonts and symbol replacement settings.
  - SvgGdi: Supported; exact glyph output depends on available fonts and symbol replacement settings.
  - AwtGdi: Supported; exact glyph output depends on available fonts and symbol replacement settings.

### META_CREATEBRUSHINDIRECT (0x02FC)

- Category: object creation.
- Purpose: creates a logical brush.
- Key payload/effect: brush style, color, and hatch/pattern value.
- Rendering/test relevance: solid, null, hatch, and pattern fill coverage.
- wmf2svg status:
  - WmfGdi: Supported, with hatch/pattern fidelity dependent on backend.
  - SvgGdi: Supported, with hatch/pattern fidelity dependent on backend.
  - AwtGdi: Supported, with hatch/pattern fidelity dependent on backend.

## Coverage Checklist

- Control: `META_EOF`.
- Coordinate state: map mode, window/viewport origin/extents, scale and offset records.
- Drawing primitives: line, polygon, polyline, rectangle, roundrect, ellipse, arc, pie, chord.
- Text: `META_TEXTOUT`, `META_EXTTEXTOUT`, character extra, justification, alignment, font creation, colors.
- Objects: pen, brush, font, palette, region creation/selection/deletion.
- Regions and clipping: clip rectangles, clip region selection, fill/frame/paint/invert region.
- Bitmaps: `META_BITBLT`, `META_STRETCHBLT`, DIB blits, `META_SETDIBTODEV`, `META_STRETCHDIB`.
- Raster operations and palette behavior: ROP2, PATBLT, stretch mode, palette creation/selection/mutation.
- Escape handling: enhanced metafile escape and ignored driver-specific escape data.
