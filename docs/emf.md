# EMF Record API Reference

This document summarizes the official Enhanced Metafile (EMF) record APIs and the current wmf2svg support status for each record. It is written as a coverage map for future full-support testing.

Specification source:
- Microsoft Open Specifications, [MS-EMF]: RecordType Enumeration: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-emf/1eec80ba-799b-4784-a9ac-91597d590ae1

Implementation reference:
- wmf2svg parser/backend status is cross-checked against `EmfConstants`, `EmfParser`, `EmfGdi`, `SvgGdi`, and `AwtGdi`.

## Format Overview

EMF is a 32-bit GDI metafile format. Each record starts with a 32-bit type and 32-bit byte size, followed by typed payload. Playback maintains an EMF object table and a device-context state containing transforms, mapping modes, clipping, paths, colors, text attributes, palettes, color spaces, and selected drawing objects.

The record sections below describe the EMF specification first: category, purpose, payload/effect, and test relevance. wmf2svg-specific details are limited to support status and implementation notes.

wmf2svg parses EMF records and replays supported operations to a `Gdi` backend. Some EMF records carry driver, OpenGL, color-management, or private data that has no direct SVG equivalent; support is listed separately by backend.

Backend status entries use the same support vocabulary but apply it to different output paths:

- `EmfGdi`: whether replay can be serialized back to native EMF records. Native EMF operations are usually written directly; private/device records and some advanced graphics states can be ignored, passed through, or approximate.
- `SvgGdi`: whether replay can be represented in SVG. Vector geometry and text are usually direct, while raster operations, color management, complex clipping, and font metrics can be approximate.
- `AwtGdi`: whether replay can be rendered through Java2D/ImageIO. Raster operations and clipping are often closer to GDI than SVG, while font availability, antialiasing, and host rendering still affect output.

## Support Status Legend

- Supported: parsed and replayed to a concrete `Gdi` operation.
- Partial: parsed, but rendering/writing can be approximate, backend-specific, or missing some flags.
- Parsed/ignored: recognized and intentionally ignored.
- Passed through/comment: preserved or forwarded as private/comment data rather than rendered directly.
- Unsupported/unknown: not recognized by the parser.

## Records

### EMR_HEADER (0x00000001)

- Category: control/header.
- Purpose: starts the EMF stream and describes bounds, frame, device metrics, and record counts.
- Key payload/effect: frame and bounds initialize output dimensions when lifecycle management is enabled.
- Rendering/test relevance: every EMF fixture should validate bounds and viewport initialization.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYBEZIER (0x00000002)

- Category: drawing.
- Purpose: strokes one or more cubic Bezier curves.
- Key payload/effect: 32-bit point array transformed by the current world transform.
- Rendering/test relevance: curve conversion and stroke fidelity.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYGON (0x00000003)

- Category: drawing.
- Purpose: draws and fills a closed polygon.
- Key payload/effect: 32-bit points, current pen, brush, and polygon fill mode.
- Rendering/test relevance: fill-rule and transform tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYLINE (0x00000004)

- Category: drawing.
- Purpose: strokes connected line segments.
- Key payload/effect: 32-bit point array and current pen.
- Rendering/test relevance: stroke join/cap tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYBEZIERTO (0x00000005)

- Category: drawing/current position.
- Purpose: strokes Bezier curves starting from the current drawing position.
- Key payload/effect: control/end points update path/current-position behavior through the backend.
- Rendering/test relevance: current-position and curve sequence tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYLINETO (0x00000006)

- Category: drawing/current position.
- Purpose: strokes line segments from the current drawing position through supplied points.
- Key payload/effect: point array and current pen.
- Rendering/test relevance: current-position line sequence tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYPOLYLINE (0x00000007)

- Category: drawing.
- Purpose: strokes multiple independent polylines.
- Key payload/effect: polyline counts and 32-bit point arrays.
- Rendering/test relevance: multi-part stroke tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYPOLYGON (0x00000008)

- Category: drawing.
- Purpose: draws and fills multiple closed polygons.
- Key payload/effect: polygon counts and 32-bit point arrays.
- Rendering/test relevance: compound polygon and overlap tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETWINDOWEXTEX (0x00000009)

- Category: coordinate state.
- Purpose: sets logical window extents.
- Key payload/effect: width and height.
- Rendering/test relevance: logical-to-page scaling tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETWINDOWORGEX (0x0000000A)

- Category: coordinate state.
- Purpose: sets logical window origin.
- Key payload/effect: x and y origin.
- Rendering/test relevance: coordinate offset tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETVIEWPORTEXTEX (0x0000000B)

- Category: coordinate state.
- Purpose: sets viewport extents.
- Key payload/effect: width and height.
- Rendering/test relevance: output scaling and axis direction tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETVIEWPORTORGEX (0x0000000C)

- Category: coordinate state.
- Purpose: sets viewport origin.
- Key payload/effect: x and y origin.
- Rendering/test relevance: output placement tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETBRUSHORGEX (0x0000000D)

- Category: brush state.
- Purpose: sets the brush origin used for hatch and pattern alignment.
- Key payload/effect: origin point.
- Rendering/test relevance: pattern phase/alignment tests.
- wmf2svg status:
  - EmfGdi: Supported, with backend-specific pattern alignment fidelity.
  - SvgGdi: Supported, with backend-specific pattern alignment fidelity.
  - AwtGdi: Supported, with backend-specific pattern alignment fidelity.

### EMR_EOF (0x0000000E)

- Category: control.
- Purpose: terminates the EMF stream.
- Key payload/effect: no drawing payload; closes playback.
- Rendering/test relevance: every valid EMF fixture should end with this record.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETPIXELV (0x0000000F)

- Category: raster drawing.
- Purpose: sets a pixel at logical coordinates to a color.
- Key payload/effect: point and RGB color.
- Rendering/test relevance: pixel-level raster/SVG approximation tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETMAPPERFLAGS (0x00000010)

- Category: font state.
- Purpose: controls logical-to-physical font matching.
- Key payload/effect: font mapper flags.
- Rendering/test relevance: font substitution behavior.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETMAPPERFLAGS`.
  - SvgGdi: Partial; parsed, but exact font mapping depends on runtime fonts.
  - AwtGdi: Partial; parsed, but exact font mapping depends on runtime fonts.

### EMR_SETMAPMODE (0x00000011)

- Category: coordinate state.
- Purpose: sets map mode for logical-to-device coordinate conversion.
- Key payload/effect: map mode enumeration.
- Rendering/test relevance: coordinate-system coverage.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETBKMODE (0x00000012)

- Category: state.
- Purpose: sets transparent/opaque background mode.
- Key payload/effect: mix mode for text, hatches, and styled pens.
- Rendering/test relevance: text and hatch background tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETPOLYFILLMODE (0x00000013)

- Category: state.
- Purpose: sets polygon fill rule.
- Key payload/effect: alternate or winding fill mode.
- Rendering/test relevance: self-intersecting polygon tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETROP2 (0x00000014)

- Category: raster state.
- Purpose: sets foreground binary raster operation.
- Key payload/effect: ROP2 mode.
- Rendering/test relevance: non-copy stroke/fill blending tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETROP2`.
  - SvgGdi: Partial; parsed, with backend-specific ROP fidelity.
  - AwtGdi: Supported; all defined ROP2 modes are mapped to Java2D compositing/filter behavior.

### EMR_SETSTRETCHBLTMODE (0x00000015)

- Category: raster state.
- Purpose: sets bitmap stretch mode.
- Key payload/effect: stretch/filtering mode.
- Rendering/test relevance: scaled bitmap tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETSTRETCHBLTMODE`.
  - SvgGdi: Partial; parsed, with backend-specific scaling behavior.
  - AwtGdi: Supported; stretch modes are mapped to Java2D interpolation hints where applicable.

### EMR_SETTEXTALIGN (0x00000016)

- Category: text state.
- Purpose: sets horizontal/vertical text alignment and update-current-position flags.
- Key payload/effect: text alignment flags.
- Rendering/test relevance: baseline, centered, right-aligned, and CP text tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETCOLORADJUSTMENT (0x00000017)

- Category: color state.
- Purpose: sets color-adjustment parameters for bitmap operations.
- Key payload/effect: raw color-adjustment structure.
- Rendering/test relevance: bitmap color transform tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETCOLORADJUSTMENT`.
  - SvgGdi: Partial; state is stored, but SVG output does not fully apply GDI color-adjustment transforms.
  - AwtGdi: Partial; state is stored, but bitmap color-adjustment transforms are not fully applied.

### EMR_SETTEXTCOLOR (0x00000018)

- Category: text state.
- Purpose: sets current text color.
- Key payload/effect: RGB color.
- Rendering/test relevance: text color tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETBKCOLOR (0x00000019)

- Category: color state.
- Purpose: sets background color.
- Key payload/effect: RGB color.
- Rendering/test relevance: opaque text and hatch background tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_OFFSETCLIPRGN (0x0000001A)

- Category: clipping.
- Purpose: offsets the current clipping region.
- Key payload/effect: x and y deltas.
- Rendering/test relevance: clip movement tests.
- wmf2svg status:
  - EmfGdi: Supported where backend clipping supports it.
  - SvgGdi: Supported where backend clipping supports it.
  - AwtGdi: Supported where backend clipping supports it.

### EMR_MOVETOEX (0x0000001B)

- Category: drawing state.
- Purpose: sets current drawing position.
- Key payload/effect: logical point transformed by the current world transform.
- Rendering/test relevance: current-position tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETMETARGN (0x0000001C)

- Category: clipping.
- Purpose: combines current clipping region into the metaregion.
- Key payload/effect: no explicit geometry payload.
- Rendering/test relevance: advanced clipping-state tests.
- wmf2svg status:
  - EmfGdi: Supported at API level; exact region semantics are backend-dependent.
  - SvgGdi: Supported at API level; exact region semantics are backend-dependent.
  - AwtGdi: Supported at API level; exact region semantics are backend-dependent.

### EMR_EXCLUDECLIPRECT (0x0000001D)

- Category: clipping.
- Purpose: subtracts a rectangle from the clip.
- Key payload/effect: rectangle bounds.
- Rendering/test relevance: exclusion clipping tests.
- wmf2svg status:
  - EmfGdi: Supported where backend clipping can represent it.
  - SvgGdi: Supported where backend clipping can represent it.
  - AwtGdi: Supported where backend clipping can represent it.

### EMR_INTERSECTCLIPRECT (0x0000001E)

- Category: clipping.
- Purpose: intersects current clip with a rectangle.
- Key payload/effect: rectangle bounds.
- Rendering/test relevance: rectangular clipping tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SCALEVIEWPORTEXTEX (0x0000001F)

- Category: coordinate state.
- Purpose: scales viewport extents by ratios.
- Key payload/effect: numerator/denominator pairs.
- Rendering/test relevance: transform arithmetic tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SCALEWINDOWEXTEX (0x00000020)

- Category: coordinate state.
- Purpose: scales window extents by ratios.
- Key payload/effect: numerator/denominator pairs.
- Rendering/test relevance: logical scaling tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SAVEDC (0x00000021)

- Category: state stack.
- Purpose: saves device-context state.
- Key payload/effect: pushes current state.
- Rendering/test relevance: save/restore nesting tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_RESTOREDC (0x00000022)

- Category: state stack.
- Purpose: restores a saved device-context state.
- Key payload/effect: saved state index.
- Rendering/test relevance: restore transforms, clip, and object selections.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETWORLDTRANSFORM (0x00000023)

- Category: transform state.
- Purpose: replaces the current world transform.
- Key payload/effect: XFORM matrix.
- Rendering/test relevance: rotation, skew, scale, translation tests.
- wmf2svg status:
  - EmfGdi: Supported; parser applies transform to replayed geometry.
  - SvgGdi: Supported; parser applies transform to replayed geometry.
  - AwtGdi: Supported; parser applies transform to replayed geometry.

### EMR_MODIFYWORLDTRANSFORM (0x00000024)

- Category: transform state.
- Purpose: composes or resets the current world transform.
- Key payload/effect: XFORM matrix and modify mode.
- Rendering/test relevance: matrix-order tests.
- wmf2svg status:
  - EmfGdi: Supported for identity, left multiply, and right multiply modes.
  - SvgGdi: Supported for identity, left multiply, and right multiply modes.
  - AwtGdi: Supported for identity, left multiply, and right multiply modes.

### EMR_SELECTOBJECT (0x00000025)

- Category: object selection.
- Purpose: selects stock or created pen, brush, font, palette, or region objects.
- Key payload/effect: object table index or stock object id.
- Rendering/test relevance: object table and stock object tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_CREATEPEN (0x00000026)

- Category: object creation.
- Purpose: creates a logical pen.
- Key payload/effect: object id, pen style, width, and color.
- Rendering/test relevance: stroke style and width tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_CREATEBRUSHINDIRECT (0x00000027)

- Category: object creation.
- Purpose: creates a logical brush.
- Key payload/effect: object id, brush style, color, and hatch.
- Rendering/test relevance: fill style tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_DELETEOBJECT (0x00000028)

- Category: object lifecycle.
- Purpose: deletes an object table entry.
- Key payload/effect: object id.
- Rendering/test relevance: handle lifetime and reuse tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_ANGLEARC (0x00000029)

- Category: drawing.
- Purpose: draws a line to the start of a circular arc and then strokes the arc.
- Key payload/effect: center, radius, start angle, and sweep angle.
- Rendering/test relevance: angle-based arc geometry.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_ELLIPSE (0x0000002A)

- Category: drawing.
- Purpose: draws and fills an ellipse.
- Key payload/effect: bounding rectangle, current pen and brush.
- Rendering/test relevance: transformed ellipse tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_RECTANGLE (0x0000002B)

- Category: drawing.
- Purpose: draws and fills a rectangle.
- Key payload/effect: bounding rectangle.
- Rendering/test relevance: basic shape coverage.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_ROUNDRECT (0x0000002C)

- Category: drawing.
- Purpose: draws and fills a rounded rectangle.
- Key payload/effect: bounding rectangle and corner dimensions.
- Rendering/test relevance: rounded-corner tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_ARC (0x0000002D)

- Category: drawing.
- Purpose: strokes an elliptical arc.
- Key payload/effect: bounds and start/end radial points.
- Rendering/test relevance: arc endpoint conversion.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_CHORD (0x0000002E)

- Category: drawing.
- Purpose: draws and fills an elliptical chord.
- Key payload/effect: bounds, start/end points, current pen and brush.
- Rendering/test relevance: filled arc-closure tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_PIE (0x0000002F)

- Category: drawing.
- Purpose: draws and fills a pie wedge.
- Key payload/effect: bounds and two radial endpoints.
- Rendering/test relevance: wedge fill tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SELECTPALETTE (0x00000030)

- Category: palette.
- Purpose: selects a logical palette.
- Key payload/effect: palette object id.
- Rendering/test relevance: indexed color tests.
- wmf2svg status:
  - EmfGdi: Supported when the object is a palette.
  - SvgGdi: Supported when the object is a palette.
  - AwtGdi: Supported when the object is a palette.

### EMR_CREATEPALETTE (0x00000031)

- Category: object creation.
- Purpose: creates a logical palette.
- Key payload/effect: palette entries.
- Rendering/test relevance: palette lifecycle tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETPALETTEENTRIES (0x00000032)

- Category: palette.
- Purpose: updates palette entries.
- Key payload/effect: palette id, start index, count, and RGB entries.
- Rendering/test relevance: palette mutation tests.
- wmf2svg status:
  - EmfGdi: Supported when the object is a palette.
  - SvgGdi: Supported when the object is a palette.
  - AwtGdi: Supported when the object is a palette.

### EMR_RESIZEPALETTE (0x00000033)

- Category: palette.
- Purpose: resizes a logical palette.
- Key payload/effect: palette id and new count.
- Rendering/test relevance: palette table size tests.
- wmf2svg status:
  - EmfGdi: Supported when the object is a palette.
  - SvgGdi: Supported when the object is a palette.
  - AwtGdi: Supported when the object is a palette.

### EMR_REALIZEPALETTE (0x00000034)

- Category: palette.
- Purpose: realizes the selected palette.
- Key payload/effect: no geometry payload.
- Rendering/test relevance: palette rendering tests.
- wmf2svg status:
  - EmfGdi: Supported with backend-dependent visible effect.
  - SvgGdi: Supported with backend-dependent visible effect.
  - AwtGdi: Supported with backend-dependent visible effect.

### EMR_EXTFLOODFILL (0x00000035)

- Category: raster drawing.
- Purpose: flood-fills an area from a start point.
- Key payload/effect: point, color, and fill mode.
- Rendering/test relevance: raster fill behavior.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_EXTFLOODFILL`.
  - SvgGdi: Partial; parsed and replayed, backend behavior may vary.
  - AwtGdi: Supported; rendered through the raster flood-fill implementation.

### EMR_LINETO (0x00000036)

- Category: drawing.
- Purpose: draws a line to a point and updates current position.
- Key payload/effect: endpoint and current pen.
- Rendering/test relevance: line/current-position tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_ARCTO (0x00000037)

- Category: drawing/current position.
- Purpose: strokes an arc and updates current position to its endpoint.
- Key payload/effect: bounds and start/end radial points.
- Rendering/test relevance: current-position arc tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYDRAW (0x00000038)

- Category: path drawing.
- Purpose: encodes mixed move, line, and Bezier commands.
- Key payload/effect: 32-bit points plus point-type array.
- Rendering/test relevance: mixed path command tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETARCDIRECTION (0x00000039)

- Category: state.
- Purpose: sets clockwise/counterclockwise arc direction.
- Key payload/effect: arc direction enum.
- Rendering/test relevance: arc sweep direction tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_SETMITERLIMIT (0x0000003A)

- Category: stroke state.
- Purpose: sets the miter join limit.
- Key payload/effect: floating-point miter limit.
- Rendering/test relevance: acute-angle stroke join tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_BEGINPATH (0x0000003B)

- Category: path state.
- Purpose: opens a path bracket.
- Key payload/effect: begins collecting path operations.
- Rendering/test relevance: path construction tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_ENDPATH (0x0000003C)

- Category: path state.
- Purpose: closes a path bracket and selects the path.
- Key payload/effect: finishes path capture.
- Rendering/test relevance: path replay tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_CLOSEFIGURE (0x0000003D)

- Category: path state.
- Purpose: closes the current path figure.
- Key payload/effect: implicit line to figure start.
- Rendering/test relevance: subpath closure tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_FILLPATH (0x0000003E)

- Category: path drawing.
- Purpose: fills the current path.
- Key payload/effect: current brush and fill rule.
- Rendering/test relevance: path fill tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_STROKEANDFILLPATH (0x0000003F)

- Category: path drawing.
- Purpose: strokes and fills the current path.
- Key payload/effect: current pen, brush, and fill rule.
- Rendering/test relevance: combined path paint order tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_STROKEPATH (0x00000040)

- Category: path drawing.
- Purpose: strokes the current path.
- Key payload/effect: current pen.
- Rendering/test relevance: path stroke tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_FLATTENPATH (0x00000041)

- Category: path state.
- Purpose: converts path curves into line segments.
- Key payload/effect: current path mutation.
- Rendering/test relevance: flattening tests when backends expose path differences.
- wmf2svg status:
  - EmfGdi: Supported at API level; backend may handle curves natively instead.
  - SvgGdi: Supported at API level; backend may handle curves natively instead.
  - AwtGdi: Supported at API level; backend may handle curves natively instead.

### EMR_WIDENPATH (0x00000042)

- Category: path state.
- Purpose: replaces path with the area that would be stroked by the current pen.
- Key payload/effect: path geometry mutation.
- Rendering/test relevance: widened path clipping/fill tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_WIDENPATH`.
  - SvgGdi: Supported; current path is widened using the selected pen before later path operations.
  - AwtGdi: Supported; current path is replaced by the Java2D stroked outline.

### EMR_SELECTCLIPPATH (0x00000043)

- Category: clipping.
- Purpose: combines the current path with the clipping region.
- Key payload/effect: region combine mode.
- Rendering/test relevance: path clipping tests.
- wmf2svg status:
  - EmfGdi: Supported where backend clipping can represent it.
  - SvgGdi: Supported where backend clipping can represent it.
  - AwtGdi: Supported where backend clipping can represent it.

### EMR_ABORTPATH (0x00000044)

- Category: path state.
- Purpose: discards an open or selected path.
- Key payload/effect: clears path state.
- Rendering/test relevance: negative path-bracket tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_COMMENT (0x00000046)

- Category: comment/private data.
- Purpose: carries private data, public comments, or embedded EMF+ records.
- Key payload/effect: byte payload.
- Rendering/test relevance: EMF+ integration and private-comment tests.
- wmf2svg status:
  - EmfGdi: Passed through/comment; serialized as `EMR_COMMENT` using the implementation constant `EMR_GDICOMMENT`.
  - SvgGdi: Passed through/comment; EMF+ and embedded EMF comments are interpreted when recognized, while generic private comments have no rendering effect.
  - AwtGdi: Passed through/comment; EMF+ and embedded EMF comments are interpreted when recognized, while generic private comments have no rendering effect.

### EMR_FILLRGN (0x00000047)

- Category: region drawing.
- Purpose: fills a region with a brush.
- Key payload/effect: region data and brush id.
- Rendering/test relevance: region fill tests.
- wmf2svg status:
  - EmfGdi: Supported when region and brush are resolved.
  - SvgGdi: Supported when region and brush are resolved.
  - AwtGdi: Supported when region and brush are resolved.

### EMR_FRAMERGN (0x00000048)

- Category: region drawing.
- Purpose: draws a border around a region with a brush.
- Key payload/effect: region data, brush id, frame width, frame height.
- Rendering/test relevance: region outline tests.
- wmf2svg status:
  - EmfGdi: Supported with backend-dependent region geometry.
  - SvgGdi: Supported with backend-dependent region geometry.
  - AwtGdi: Supported with backend-dependent region geometry.

### EMR_INVERTRGN (0x00000049)

- Category: region raster operation.
- Purpose: inverts pixels inside a region.
- Key payload/effect: region data.
- Rendering/test relevance: raster-effect tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_INVERTRGN`.
  - SvgGdi: Partial; parsed and replayed, SVG/raster fidelity depends on backend.
  - AwtGdi: Supported; rendered by XOR/invert fill over the parsed region.

### EMR_PAINTRGN (0x0000004A)

- Category: region drawing.
- Purpose: paints a region using the current brush.
- Key payload/effect: region data.
- Rendering/test relevance: region paint tests.
- wmf2svg status:
  - EmfGdi: Supported when region parsing succeeds.
  - SvgGdi: Supported when region parsing succeeds.
  - AwtGdi: Supported when region parsing succeeds.

### EMR_EXTSELECTCLIPRGN (0x0000004B)

- Category: clipping.
- Purpose: combines a region with the current clipping region.
- Key payload/effect: optional region data and combine mode.
- Rendering/test relevance: complex region clip tests.
- wmf2svg status:
  - EmfGdi: Supported where backend clipping can represent the region.
  - SvgGdi: Supported where backend clipping can represent the region.
  - AwtGdi: Supported where backend clipping can represent the region.

### EMR_BITBLT (0x0000004C)

- Category: bitmap transfer.
- Purpose: transfers source pixels to a destination rectangle using a ROP.
- Key payload/effect: destination/source geometry, transform, raster op, optional bitmap data.
- Rendering/test relevance: bitmap placement and ROP coverage.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_BITBLT`.
  - SvgGdi: Partial; common embedded bitmap cases are handled, complex ROP/source cases vary.
  - AwtGdi: Supported; embedded bitmap and source-less pattern ROP cases are rendered through the bitmap ROP path.

### EMR_STRETCHBLT (0x0000004D)

- Category: bitmap transfer.
- Purpose: transfers and scales source pixels using a ROP.
- Key payload/effect: source and destination rectangles plus optional bitmap data.
- Rendering/test relevance: scaled bitmap coverage.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_STRETCHBLT`.
  - SvgGdi: Partial; parsed, with backend-dependent scaling/ROP support.
  - AwtGdi: Supported; embedded bitmap scaling and ROP handling are rendered through the bitmap path.

### EMR_MASKBLT (0x0000004E)

- Category: bitmap transfer.
- Purpose: transfers pixels with a mask bitmap and foreground/background ROPs.
- Key payload/effect: source bitmap, mask bitmap, geometry, and ROPs.
- Rendering/test relevance: masked image tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_MASKBLT`.
  - SvgGdi: Partial; parser reads mask blit data, backend fidelity can vary.
  - AwtGdi: Supported; mask and fallback non-mask cases are rendered through bitmap paths.

### EMR_PLGBLT (0x0000004F)

- Category: bitmap transfer.
- Purpose: maps source pixels into a destination parallelogram with optional mask.
- Key payload/effect: three destination points, source bitmap, optional mask.
- Rendering/test relevance: affine image transform tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_PLGBLT`.
  - SvgGdi: Partial; parsed, with backend-dependent transformed image support.
  - AwtGdi: Supported; destination parallelogram transforms are rendered through the bitmap path.

### EMR_SETDIBITSTODEVICE (0x00000050)

- Category: bitmap transfer.
- Purpose: copies DIB scanlines to the destination.
- Key payload/effect: scanline range, destination geometry, DIB header and bits.
- Rendering/test relevance: DIB decoding and scanline placement.
- wmf2svg status:
  - EmfGdi: Supported for common embedded DIB data.
  - SvgGdi: Supported for common embedded DIB data.
  - AwtGdi: Supported for common embedded DIB data.

### EMR_STRETCHDIBITS (0x00000051)

- Category: bitmap transfer.
- Purpose: scales DIB pixels into a destination rectangle.
- Key payload/effect: source/destination rectangles, DIB data, color usage, and ROP.
- Rendering/test relevance: primary EMF scaled-DIB test record.
- wmf2svg status:
  - EmfGdi: Supported for common DIB data; filtering/ROP effects are backend-dependent.
  - SvgGdi: Supported for common DIB data; filtering/ROP effects are backend-dependent.
  - AwtGdi: Supported for common DIB data; filtering/ROP effects are backend-dependent.

### EMR_EXTCREATEFONTINDIRECTW (0x00000052)

- Category: object creation.
- Purpose: creates a Unicode logical font.
- Key payload/effect: object id and extended LOGFONT fields.
- Rendering/test relevance: text metrics, charset, style, and face-name tests.
- wmf2svg status:
  - EmfGdi: Supported; exact glyph output depends on installed fonts and replacement settings.
  - SvgGdi: Supported; exact glyph output depends on installed fonts and replacement settings.
  - AwtGdi: Supported; exact glyph output depends on installed fonts and replacement settings.

### EMR_EXTTEXTOUTA (0x00000053)

- Category: text drawing.
- Purpose: draws ANSI text using current font and text state.
- Key payload/effect: reference point, options, optional clipping/opaque rect, string bytes, dx array.
- Rendering/test relevance: ANSI text and codepage tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_EXTTEXTOUTW (0x00000054)

- Category: text drawing.
- Purpose: draws Unicode text using current font and text state.
- Key payload/effect: reference point, options, optional clipping/opaque rect, UTF-16 string, dx array.
- Rendering/test relevance: core Unicode text test record.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYBEZIER16 (0x00000055)

- Category: drawing.
- Purpose: strokes Bezier curves using 16-bit points.
- Key payload/effect: compact point array transformed to replay coordinates.
- Rendering/test relevance: compact coordinate decoding tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYGON16 (0x00000056)

- Category: drawing.
- Purpose: draws and fills a polygon using 16-bit points.
- Key payload/effect: compact point array.
- Rendering/test relevance: 16-bit polygon tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYLINE16 (0x00000057)

- Category: drawing.
- Purpose: strokes connected line segments using 16-bit points.
- Key payload/effect: compact point array.
- Rendering/test relevance: 16-bit polyline tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYBEZIERTO16 (0x00000058)

- Category: drawing/current position.
- Purpose: strokes Bezier curves from the current position using 16-bit points.
- Key payload/effect: compact control/end points.
- Rendering/test relevance: current-position compact Bezier tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYLINETO16 (0x00000059)

- Category: drawing/current position.
- Purpose: strokes lines from the current position using 16-bit points.
- Key payload/effect: compact endpoint array.
- Rendering/test relevance: current-position compact line tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYPOLYLINE16 (0x0000005A)

- Category: drawing.
- Purpose: strokes multiple polylines using 16-bit points.
- Key payload/effect: counts and compact point arrays.
- Rendering/test relevance: compact multi-polyline tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYPOLYGON16 (0x0000005B)

- Category: drawing.
- Purpose: draws and fills multiple polygons using 16-bit points.
- Key payload/effect: counts and compact point arrays.
- Rendering/test relevance: compact compound polygon tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_POLYDRAW16 (0x0000005C)

- Category: path drawing.
- Purpose: encodes mixed move, line, and Bezier commands with 16-bit points.
- Key payload/effect: compact points and point-type array.
- Rendering/test relevance: compact mixed path tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_CREATEMONOBRUSH (0x0000005D)

- Category: object creation.
- Purpose: creates a monochrome pattern brush.
- Key payload/effect: object id, usage, and bitmap/DIB data.
- Rendering/test relevance: monochrome pattern fill tests.
- wmf2svg status:
  - EmfGdi: Supported for bitmap data that can be decoded; backend pattern fidelity can vary.
  - SvgGdi: Supported for bitmap data that can be decoded; backend pattern fidelity can vary.
  - AwtGdi: Supported for bitmap data that can be decoded; backend pattern fidelity can vary.

### EMR_CREATEDIBPATTERNBRUSHPT (0x0000005E)

- Category: object creation.
- Purpose: creates a pattern brush from DIB data.
- Key payload/effect: object id, usage, and DIB payload.
- Rendering/test relevance: DIB pattern brush tests.
- wmf2svg status:
  - EmfGdi: Supported for common embedded DIB patterns.
  - SvgGdi: Supported for common embedded DIB patterns.
  - AwtGdi: Supported for common embedded DIB patterns.

### EMR_EXTCREATEPEN (0x0000005F)

- Category: object creation.
- Purpose: creates an extended pen with brush attributes.
- Key payload/effect: style, width, brush style/color, optional style entries.
- Rendering/test relevance: geometric pen and custom dash tests.
- wmf2svg status:
  - EmfGdi: Partial; basic extended pen data is parsed, advanced attributes may be approximate.
  - SvgGdi: Partial; basic extended pen data is parsed, advanced attributes may be approximate.
  - AwtGdi: Partial; basic extended pen data is parsed, advanced attributes may be approximate.

### EMR_POLYTEXTOUTA (0x00000060)

- Category: text drawing.
- Purpose: draws multiple ANSI text strings.
- Key payload/effect: array of text records.
- Rendering/test relevance: batched ANSI text tests.
- wmf2svg status:
  - EmfGdi: Supported by replaying each text item.
  - SvgGdi: Supported by replaying each text item.
  - AwtGdi: Supported by replaying each text item.

### EMR_POLYTEXTOUTW (0x00000061)

- Category: text drawing.
- Purpose: draws multiple Unicode text strings.
- Key payload/effect: array of text records.
- Rendering/test relevance: batched Unicode text tests.
- wmf2svg status:
  - EmfGdi: Supported by replaying each text item.
  - SvgGdi: Supported by replaying each text item.
  - AwtGdi: Supported by replaying each text item.

### EMR_SETICMMODE (0x00000062)

- Category: color management.
- Purpose: sets Image Color Management mode.
- Key payload/effect: ICM mode value.
- Rendering/test relevance: color-management state tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETICMMODE`.
  - SvgGdi: Partial; ICM mode is stored but SVG color conversion is not fully applied.
  - AwtGdi: Partial; ICM mode is stored but Java2D output does not fully emulate GDI ICM.

### EMR_CREATECOLORSPACE (0x00000063)

- Category: color management/object creation.
- Purpose: creates a logical color space from an ANSI profile structure.
- Key payload/effect: object id and color-space payload.
- Rendering/test relevance: color-space object lifecycle tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_CREATECOLORSPACE`.
  - SvgGdi: Partial; object is created from raw data, exact color conversion may be limited.
  - AwtGdi: Partial; object is created from raw data, exact color conversion may be limited.

### EMR_SETCOLORSPACE (0x00000064)

- Category: color management.
- Purpose: selects a logical color space.
- Key payload/effect: color-space object id.
- Rendering/test relevance: color profile selection tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETCOLORSPACE`.
  - SvgGdi: Partial; selected color space is stored but output color conversion is limited.
  - AwtGdi: Partial; selected color space is stored but output color conversion is limited.

### EMR_DELETECOLORSPACE (0x00000065)

- Category: color management/object lifecycle.
- Purpose: deletes a logical color-space object.
- Key payload/effect: object id.
- Rendering/test relevance: color-space lifecycle tests.
- wmf2svg status:
  - EmfGdi: Supported at object-table level.
  - SvgGdi: Supported at object-table level.
  - AwtGdi: Supported at object-table level.

### EMR_GLSRECORD (0x00000066)

- Category: OpenGL/private drawing.
- Purpose: carries an OpenGL command.
- Key payload/effect: GL command bytes.
- Rendering/test relevance: negative/ignored OpenGL tests.
- wmf2svg status:
  - EmfGdi: Parsed/ignored.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EMR_GLSBOUNDEDRECORD (0x00000067)

- Category: OpenGL/private drawing.
- Purpose: carries an OpenGL command with output bounds.
- Key payload/effect: bounds and GL command bytes.
- Rendering/test relevance: ignored bounded OpenGL tests.
- wmf2svg status:
  - EmfGdi: Parsed/ignored.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EMR_PIXELFORMAT (0x00000068)

- Category: OpenGL/device state.
- Purpose: records pixel-format information for OpenGL output.
- Key payload/effect: pixel format descriptor.
- Rendering/test relevance: ignored device-state tests.
- wmf2svg status:
  - EmfGdi: Parsed/ignored.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EMR_DRAWESCAPE (0x00000069)

- Category: escape/private data.
- Purpose: sends driver-specific information intended to draw.
- Key payload/effect: escape function and data.
- Rendering/test relevance: private escape handling tests.
- wmf2svg status:
  - EmfGdi: Passed through/comment via escape handling; no generic rendering.
  - SvgGdi: Passed through/comment via escape handling; no generic rendering.
  - AwtGdi: Passed through/comment via escape handling; no generic rendering.

### EMR_EXTESCAPE (0x0000006A)

- Category: escape/private data.
- Purpose: sends driver-specific information not necessarily intended to draw.
- Key payload/effect: escape function and data.
- Rendering/test relevance: private escape handling tests.
- wmf2svg status:
  - EmfGdi: Passed through/comment via escape handling; no generic rendering.
  - SvgGdi: Passed through/comment via escape handling; no generic rendering.
  - AwtGdi: Passed through/comment via escape handling; no generic rendering.

### EMR_STARTDOC (0x0000006B)

- Category: spooler/print control.
- Purpose: records start-document information used by printing-oriented producers.
- Key payload/effect: print job metadata rather than vector drawing geometry.
- Rendering/test relevance: compatibility fixture should confirm it does not affect drawing output.
- wmf2svg status:
  - EmfGdi: Parsed/ignored; present in implementation constants for compatibility.
  - SvgGdi: Parsed/ignored; present in implementation constants for compatibility.
  - AwtGdi: Parsed/ignored; present in implementation constants for compatibility.

### EMR_SMALLTEXTOUT (0x0000006C)

- Category: text drawing.
- Purpose: draws a compact text string.
- Key payload/effect: position, flags, string, optional bounds.
- Rendering/test relevance: compact text fixture coverage.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_FORCEUFIMAPPING (0x0000006D)

- Category: font state.
- Purpose: forces font matching by UniversalFontId.
- Key payload/effect: font identity mapping hint.
- Rendering/test relevance: ignored font-mapping test.
- wmf2svg status:
  - EmfGdi: Parsed/ignored.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EMR_NAMEDESCAPE (0x0000006E)

- Category: escape/private data.
- Purpose: sends escape data to a named driver.
- Key payload/effect: driver name and escape payload.
- Rendering/test relevance: private named escape tests.
- wmf2svg status:
  - EmfGdi: Passed through/comment via named escape handling.
  - SvgGdi: Passed through/comment via named escape handling.
  - AwtGdi: Passed through/comment via named escape handling.

### EMR_COLORCORRECTPALETTE (0x0000006F)

- Category: color management/palette.
- Purpose: applies WCS color correction to palette entries.
- Key payload/effect: palette id, first entry, number of entries.
- Rendering/test relevance: palette color-correction tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_COLORCORRECTPALETTE`.
  - SvgGdi: Partial; palette object lookup is available, but WCS color correction is not applied to output colors.
  - AwtGdi: Partial; palette object lookup is available, but WCS color correction is not applied to output colors.

### EMR_SETICMPROFILEA (0x00000070)

- Category: color management.
- Purpose: selects an ANSI-named ICM profile.
- Key payload/effect: profile name and profile data.
- Rendering/test relevance: color profile parsing tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETICMPROFILEA`.
  - SvgGdi: Partial; parsed and replayed as profile data, exact color conversion may be limited.
  - AwtGdi: Partial; parsed and replayed as profile data, exact color conversion may be limited.

### EMR_SETICMPROFILEW (0x00000071)

- Category: color management.
- Purpose: selects a Unicode-named ICM profile.
- Key payload/effect: UTF-16 profile name and profile data.
- Rendering/test relevance: Unicode profile-name parsing tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETICMPROFILEW`.
  - SvgGdi: Partial; parsed and replayed as profile data, exact color conversion may be limited.
  - AwtGdi: Partial; parsed and replayed as profile data, exact color conversion may be limited.

### EMR_ALPHABLEND (0x00000072)

- Category: bitmap transfer.
- Purpose: transfers pixels with alpha blending.
- Key payload/effect: source/destination geometry, blend function, and bitmap data.
- Rendering/test relevance: transparency and bitmap composition tests.
- wmf2svg status:
  - EmfGdi: Supported for common embedded bitmap cases; blending fidelity is backend-dependent.
  - SvgGdi: Supported for common embedded bitmap cases; blending fidelity is backend-dependent.
  - AwtGdi: Supported for common embedded bitmap cases; blending fidelity is backend-dependent.

### EMR_SETLAYOUT (0x00000073)

- Category: layout state.
- Purpose: sets drawing/text layout direction.
- Key payload/effect: layout flags.
- Rendering/test relevance: right-to-left/mirrored layout tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_SETLAYOUT`.
  - SvgGdi: Partial; layout state is stored, but full GDI mirroring/orientation semantics are not applied to every operation.
  - AwtGdi: Partial; layout state is stored, but full GDI mirroring/orientation semantics are not applied to every operation.

### EMR_TRANSPARENTBLT (0x00000074)

- Category: bitmap transfer.
- Purpose: transfers pixels treating a color as transparent.
- Key payload/effect: source/destination geometry, bitmap data, transparent color.
- Rendering/test relevance: color-key transparency tests.
- wmf2svg status:
  - EmfGdi: Supported for common embedded bitmap cases; color-key precision is backend-dependent.
  - SvgGdi: Supported for common embedded bitmap cases; color-key precision is backend-dependent.
  - AwtGdi: Supported for common embedded bitmap cases; color-key precision is backend-dependent.

### EMR_RESERVED_117 (0x00000075)

- Category: reserved.
- Purpose: reserved value between `EMR_TRANSPARENTBLT` and `EMR_GRADIENTFILL`.
- Key payload/effect: no drawing semantics for conforming playback.
- Rendering/test relevance: negative test should ensure no output change.
- wmf2svg status:
  - EmfGdi: Parsed/ignored; present in implementation constants for compatibility.
  - SvgGdi: Parsed/ignored; present in implementation constants for compatibility.
  - AwtGdi: Parsed/ignored; present in implementation constants for compatibility.

### EMR_GRADIENTFILL (0x00000076)

- Category: gradient drawing.
- Purpose: fills rectangles or triangles with gradients.
- Key payload/effect: TRIVERTEX array and gradient mesh records.
- Rendering/test relevance: linear and triangular gradient tests.
- wmf2svg status:
  - EmfGdi: Supported for parsed gradient modes, with backend-specific gradient fidelity.
  - SvgGdi: Supported for parsed gradient modes, with backend-specific gradient fidelity.
  - AwtGdi: Supported for parsed gradient modes, with backend-specific gradient fidelity.

### EMR_SETLINKEDUFIS (0x00000077)

- Category: font state.
- Purpose: sets linked font UniversalFontIds for character lookup.
- Key payload/effect: UFI list.
- Rendering/test relevance: ignored font-link tests.
- wmf2svg status:
  - EmfGdi: Parsed/ignored.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EMR_SETTEXTJUSTIFICATION (0x00000078)

- Category: text state.
- Purpose: controls extra spacing for justified text.
- Key payload/effect: break extra and break count.
- Rendering/test relevance: justified text tests.
- wmf2svg status:
  - EmfGdi: Supported.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EMR_COLORMATCHTOTARGETW (0x00000079)

- Category: color management.
- Purpose: enables/disables matching against a Unicode-named target color profile.
- Key payload/effect: action flag, profile name, and profile data.
- Rendering/test relevance: target profile parsing tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_COLORMATCHTOTARGETW`.
  - SvgGdi: Partial; parsed and replayed, exact color matching may be limited.
  - AwtGdi: Partial; parsed and replayed, exact color matching may be limited.

### EMR_CREATECOLORSPACEW (0x0000007A)

- Category: color management/object creation.
- Purpose: creates a logical color space from Unicode profile data.
- Key payload/effect: object id and color-space payload.
- Rendering/test relevance: Unicode color-space object tests.
- wmf2svg status:
  - EmfGdi: Supported; serialized as `EMR_CREATECOLORSPACEW`.
  - SvgGdi: Partial; object is created from raw data, exact color conversion may be limited.
  - AwtGdi: Partial; object is created from raw data, exact color conversion may be limited.

## Coverage Checklist

- Header/control: `EMR_HEADER`, `EMR_EOF`, `EMR_COMMENT`.
- Coordinate and transform state: map mode, window/viewport records, world transform records, save/restore.
- Drawing primitives: lines, Beziers, polygons, ellipses, rectangles, arcs, pies, chords, and 16-bit variants.
- Paths: begin/end/close/fill/stroke/flatten/widen/select clip/abort path plus `EMR_POLYDRAW`.
- Text: `EMR_EXTTEXTOUTA/W`, `EMR_POLYTEXTOUTA/W`, `EMR_SMALLTEXTOUT`, font creation, alignment, colors, justification.
- Objects: pens, brushes, palettes, fonts, color spaces, object selection/deletion.
- Regions and clipping: region fill/frame/invert/paint and rectangular/path/region clipping.
- Bitmaps: `EMR_BITBLT`, `EMR_STRETCHBLT`, `EMR_MASKBLT`, `EMR_PLGBLT`, DIB transfers, alpha, transparent blit.
- Color management: ICM mode/profile, color spaces, color match, palette correction.
- Private/device records: OpenGL, escapes, pixel format, UFI mapping, linked UFIs.
