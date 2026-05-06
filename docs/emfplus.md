# EMF+ Record API Reference

This document summarizes the official EMF+ record APIs and the current wmf2svg support status for each record. It is a coverage map for future full-support testing.

Specification sources:
- Microsoft Open Specifications, [MS-EMFPLUS]: RecordType Enumeration: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-emfplus/abffcb71-1b31-414f-b032-f1e00c57a48a
- Microsoft Open Specifications, [MS-EMFPLUS]: Graphics Object Types: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-emfplus/2b930b6c-b55b-4912-a1ae-bd6f2a4239b9

Implementation reference:
- wmf2svg parser/backend status is cross-checked against `EmfPlusConstants`, `EmfPlusParser`, and EMF+ handlers in `WmfGdi`, `EmfGdi`, `SvgGdi`, and `AwtGdi`.

## Format Overview

EMF+ records are embedded in EMF `EMR_COMMENT` records. An EMF+ comment starts with an `EMF+` marker and contains one or more EMF+ records. Each EMF+ record has a 16-bit type, 16-bit flags field, 32-bit size, 32-bit data size, and record-specific payload. `EmfPlusObject` can be continuable, with object data spanning multiple records.

The record sections below describe the EMF+ specification first: category, purpose, payload/effect, and test relevance. wmf2svg-specific details are limited to support status and implementation notes.

wmf2svg detects EMF+ comments, splits them with `EmfPlusParser`, and forwards records to backend handlers. EMF+ support is strongest in the AWT raster backend and is also used to improve SVG output through fallback/translation paths. Some EMF+ state and drawing semantics are inherently hard to map exactly to SVG, so support is listed separately by backend.

Backend status entries use the same support vocabulary but apply it to different output paths:

- `WmfGdi`: whether replay can be serialized to WMF. WMF has no native EMF+ record model, so EMF+ data is at best preserved as comments or degraded to ordinary GDI/WMF operations when a prior parser/backend has already translated it.
- `EmfGdi`: whether replay can be serialized to EMF. EMF+ records are stored in EMF comments; `EmfGdi` can preserve comment payloads, while native EMF+ rendering semantics are primarily handled by `SvgGdi` and `AwtGdi`.
- `SvgGdi`: whether EMF+ comments can be interpreted and represented as SVG. Vector operations, text, images, transforms, and clipping are translated where possible, with approximations for effects that SVG cannot express directly.
- `AwtGdi`: whether EMF+ comments can be interpreted and rendered through Java2D/ImageIO. This is the most direct raster rendering path, but output still depends on fonts, antialiasing, image decoding, and Java2D behavior.

## Support Status Legend

- Supported: parsed and rendered/replayed for common payloads.
- Partial: parsed, but rendering can be approximate, backend-specific, or missing advanced options.
- Parsed/ignored: recognized and intentionally ignored, often because the record is reserved or has no direct output effect.
- Passed through/comment: preserved or forwarded as comment/private data.
- Unsupported/unknown: not recognized by the parser/backend.

## Records

### EmfPlusHeader (0x4001)

- Category: control.
- Purpose: starts EMF+ data and declares EMF+ version/graphics capabilities.
- Key payload/effect: version and flags that describe the EMF+ stream.
- Rendering/test relevance: required first EMF+ record after the EMF header in valid EMF+ streams.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed as part of EMF+ comment scanning, with limited direct rendering effect.
  - AwtGdi: Partial; parsed as part of EMF+ comment scanning, with limited direct rendering effect.

### EmfPlusEndOfFile (0x4002)

- Category: control.
- Purpose: marks the end of EMF+ data.
- Key payload/effect: no drawing payload.
- Rendering/test relevance: stream termination and mixed EMF/EMF+ fixture tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed, with no drawing operation.
  - AwtGdi: Partial; parsed, with no drawing operation.

### EmfPlusComment (0x4003)

- Category: private data.
- Purpose: carries arbitrary private EMF+ data.
- Key payload/effect: application-defined byte payload.
- Rendering/test relevance: private-comment preservation/ignore tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Parsed/ignored for rendering.
  - AwtGdi: Parsed/ignored for rendering.

### EmfPlusGetDC (0x4004)

- Category: control/interoperability.
- Purpose: switches playback so following EMF records are processed until the next EMF+ record.
- Key payload/effect: changes EMF/EMF+ dual-stream interpretation.
- Rendering/test relevance: critical for dual EMF/EMF+ fallback tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported; backend handlers track `GetDC` transitions.
  - AwtGdi: Supported; backend handlers track `GetDC` transitions.

### EmfPlusMultiFormatStart (0x4005)

- Category: reserved.
- Purpose: reserved by the specification and not valid for normal use.
- Key payload/effect: none for conforming playback.
- Rendering/test relevance: negative test should not affect output.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EmfPlusMultiFormatSection (0x4006)

- Category: reserved.
- Purpose: reserved by the specification and not valid for normal use.
- Key payload/effect: none for conforming playback.
- Rendering/test relevance: negative test should not affect output.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EmfPlusMultiFormatEnd (0x4007)

- Category: reserved.
- Purpose: reserved by the specification and not valid for normal use.
- Key payload/effect: none for conforming playback.
- Rendering/test relevance: negative test should not affect output.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Parsed/ignored.
  - AwtGdi: Parsed/ignored.

### EmfPlusObject (0x4008)

- Category: object creation/update.
- Purpose: defines a reusable EMF+ graphics object.
- Key payload/effect: object id, object type, optional continuable object data, and typed object payload.
- Rendering/test relevance: prerequisite for pens, brushes, paths, regions, fonts, images, image attributes, string formats, and custom line caps.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for many common object types; advanced object options are partial.
  - AwtGdi: Supported for many common object types; advanced object options are partial.

### EmfPlusClear (0x4009)

- Category: drawing.
- Purpose: clears the output coordinate space with a color and alpha.
- Key payload/effect: ARGB color.
- Rendering/test relevance: background and transparency tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported in EMF+ handlers, with backend-specific alpha/background behavior.
  - AwtGdi: Supported in EMF+ handlers, with backend-specific alpha/background behavior.

### EmfPlusFillRects (0x400A)

- Category: drawing.
- Purpose: fills one or more rectangles with a brush.
- Key payload/effect: brush id or solid color flag, rectangle count, and rectangle array.
- Rendering/test relevance: solid and object brush rectangle tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common rectangle payloads.
  - AwtGdi: Supported for common rectangle payloads.

### EmfPlusDrawRects (0x400B)

- Category: drawing.
- Purpose: strokes one or more rectangles with a pen.
- Key payload/effect: pen id and rectangle array.
- Rendering/test relevance: pen stroke and transform tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common rectangle payloads.
  - AwtGdi: Supported for common rectangle payloads.

### EmfPlusFillPolygon (0x400C)

- Category: drawing.
- Purpose: fills a polygon with a brush.
- Key payload/effect: brush id or solid color, fill mode flag, and point array.
- Rendering/test relevance: fill-rule, compressed point, and transform tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common point encodings.
  - AwtGdi: Supported for common point encodings.

### EmfPlusDrawLines (0x400D)

- Category: drawing.
- Purpose: strokes connected line segments.
- Key payload/effect: pen id, point count, and point array.
- Rendering/test relevance: line joins, dash, custom caps, compressed point tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common point encodings and pen options.
  - AwtGdi: Supported for common point encodings and pen options.

### EmfPlusFillEllipse (0x400E)

- Category: drawing.
- Purpose: fills an ellipse with a brush.
- Key payload/effect: brush id or solid color and rectangle bounds.
- Rendering/test relevance: ellipse fill, transform, and brush tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusDrawEllipse (0x400F)

- Category: drawing.
- Purpose: strokes an ellipse with a pen.
- Key payload/effect: pen id and rectangle bounds.
- Rendering/test relevance: ellipse stroke and transform tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusFillPie (0x4010)

- Category: drawing.
- Purpose: fills a pie section of an ellipse.
- Key payload/effect: brush id or solid color, bounds, start angle, and sweep angle.
- Rendering/test relevance: wedge geometry and angle tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common payloads.
  - AwtGdi: Supported for common payloads.

### EmfPlusDrawPie (0x4011)

- Category: drawing.
- Purpose: strokes a pie section of an ellipse.
- Key payload/effect: pen id, bounds, start angle, and sweep angle.
- Rendering/test relevance: pie outline tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common payloads.
  - AwtGdi: Supported for common payloads.

### EmfPlusDrawArc (0x4012)

- Category: drawing.
- Purpose: strokes an arc of an ellipse.
- Key payload/effect: pen id, bounds, start angle, and sweep angle.
- Rendering/test relevance: arc direction and angle tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusFillRegion (0x4013)

- Category: region drawing.
- Purpose: fills an EMF+ region with a brush.
- Key payload/effect: brush id or solid color and region id.
- Rendering/test relevance: region object decoding and combine-mode tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; common rectangular/path regions work better than complex combinations.
  - AwtGdi: Partial; common rectangular/path regions work better than complex combinations.

### EmfPlusFillPath (0x4014)

- Category: path drawing.
- Purpose: fills an EMF+ path object.
- Key payload/effect: brush id or solid color and path id.
- Rendering/test relevance: path fill, Bezier, close flag, and fill mode tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common path objects.
  - AwtGdi: Supported for common path objects.

### EmfPlusDrawPath (0x4015)

- Category: path drawing.
- Purpose: strokes an EMF+ path object.
- Key payload/effect: pen id and path id.
- Rendering/test relevance: path stroke, custom cap, dash, and transform tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common path and pen objects.
  - AwtGdi: Supported for common path and pen objects.

### EmfPlusFillClosedCurve (0x4016)

- Category: drawing.
- Purpose: fills a closed cardinal spline.
- Key payload/effect: brush id or solid color, tension, fill mode, and points.
- Rendering/test relevance: spline interpolation and fill tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed and rendered approximately through backend curve support.
  - AwtGdi: Partial; parsed and rendered approximately through backend curve support.

### EmfPlusDrawClosedCurve (0x4017)

- Category: drawing.
- Purpose: strokes a closed cardinal spline.
- Key payload/effect: pen id, tension, and points.
- Rendering/test relevance: spline stroke and closure tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed and rendered approximately.
  - AwtGdi: Partial; parsed and rendered approximately.

### EmfPlusDrawCurve (0x4018)

- Category: drawing.
- Purpose: strokes an open cardinal spline.
- Key payload/effect: pen id, tension, offset/segment count, and points.
- Rendering/test relevance: spline segment tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed and rendered approximately.
  - AwtGdi: Partial; parsed and rendered approximately.

### EmfPlusDrawBeziers (0x4019)

- Category: drawing.
- Purpose: strokes one or more Bezier splines.
- Key payload/effect: pen id and point array.
- Rendering/test relevance: Bezier stroke and compressed point tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common payloads.
  - AwtGdi: Supported for common payloads.

### EmfPlusDrawImage (0x401A)

- Category: image drawing.
- Purpose: draws an image object scaled into a destination rectangle.
- Key payload/effect: image id, source unit/rectangle, destination rectangle, optional image attributes.
- Rendering/test relevance: bitmap/metafile object decoding, source unit conversion, alpha tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common bitmap image objects; metafile images and advanced attributes are partial.
  - AwtGdi: Supported for common bitmap image objects; metafile images and advanced attributes are partial.

### EmfPlusDrawImagePoints (0x401B)

- Category: image drawing.
- Purpose: draws an image object into a destination parallelogram.
- Key payload/effect: image id, source rectangle/unit, destination points, optional image attributes.
- Rendering/test relevance: affine image transform tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; common bitmap payloads are handled, advanced transforms/attributes depend on backend.
  - AwtGdi: Partial; common bitmap payloads are handled, advanced transforms/attributes depend on backend.

### EmfPlusDrawString (0x401C)

- Category: text drawing.
- Purpose: draws a text string using an EMF+ font and string format.
- Key payload/effect: brush, font id, layout rectangle, string format id, and UTF-16 text.
- Rendering/test relevance: Unicode text, layout rectangle, alignment, vertical/RTL flags.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common font/string-format payloads; exact metrics depend on fonts/backend.
  - AwtGdi: Supported for common font/string-format payloads; exact metrics depend on fonts/backend.

### EmfPlusSetRenderingOrigin (0x401D)

- Category: state.
- Purpose: sets rendering origin for hatch and dither patterns.
- Key payload/effect: x and y origin.
- Rendering/test relevance: hatch pattern phase tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed, with backend-specific pattern effects.
  - AwtGdi: Partial; parsed, with backend-specific pattern effects.

### EmfPlusSetAntiAliasMode (0x401E)

- Category: rendering state.
- Purpose: selects smoothing/anti-aliasing behavior.
- Key payload/effect: smoothing mode in flags/payload.
- Rendering/test relevance: raster comparison tests with anti-aliasing on/off.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; AWT maps common modes to rendering hints, SVG effect is indirect.
  - AwtGdi: Partial; AWT maps common modes to rendering hints, SVG effect is indirect.

### EmfPlusSetTextRenderingHint (0x401F)

- Category: text rendering state.
- Purpose: selects text rendering quality/hinting behavior.
- Key payload/effect: text rendering hint.
- Rendering/test relevance: AWT raster text comparisons.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; AWT maps common hints, SVG/font output is environment-dependent.
  - AwtGdi: Partial; AWT maps common hints, SVG/font output is environment-dependent.

### EmfPlusSetTextContrast (0x4020)

- Category: text rendering state.
- Purpose: sets gamma/contrast for antialiased text.
- Key payload/effect: contrast value.
- Rendering/test relevance: raster-only text antialiasing tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed, but visible effect is backend-dependent.
  - AwtGdi: Partial; parsed, but visible effect is backend-dependent.

### EmfPlusSetInterpolationMode (0x4021)

- Category: image rendering state.
- Purpose: selects interpolation mode for image scaling.
- Key payload/effect: interpolation mode enum.
- Rendering/test relevance: nearest/bilinear/bicubic image scaling tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; AWT maps common modes, SVG output depends on image-rendering support.
  - AwtGdi: Partial; AWT maps common modes, SVG output depends on image-rendering support.

### EmfPlusSetPixelOffsetMode (0x4022)

- Category: rendering state.
- Purpose: controls pixel center offset behavior.
- Key payload/effect: pixel offset mode.
- Rendering/test relevance: subpixel stroke/image placement tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed and applied where backend supports it.
  - AwtGdi: Partial; parsed and applied where backend supports it.

### EmfPlusSetCompositingMode (0x4023)

- Category: compositing state.
- Purpose: selects source-over or source-copy compositing behavior.
- Key payload/effect: compositing mode enum.
- Rendering/test relevance: alpha overlap tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; AWT maps common modes, SVG fidelity depends on generated compositing model.
  - AwtGdi: Partial; AWT maps common modes, SVG fidelity depends on generated compositing model.

### EmfPlusSetCompositingQuality (0x4024)

- Category: compositing state.
- Purpose: selects compositing quality.
- Key payload/effect: quality enum.
- Rendering/test relevance: raster comparison tests for alpha interpolation.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; AWT maps common quality hints.
  - AwtGdi: Partial; AWT maps common quality hints.

### EmfPlusSave (0x4025)

- Category: state stack.
- Purpose: saves EMF+ graphics state under an id.
- Key payload/effect: state id.
- Rendering/test relevance: nested state and transform/clip restore tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common state transitions.
  - AwtGdi: Supported for common state transitions.

### EmfPlusRestore (0x4026)

- Category: state stack.
- Purpose: restores a saved EMF+ graphics state.
- Key payload/effect: state id.
- Rendering/test relevance: restore transform, clip, compositing, and render-state tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common state transitions.
  - AwtGdi: Supported for common state transitions.

### EmfPlusBeginContainer (0x4027)

- Category: container state.
- Purpose: opens a graphics container with transforms.
- Key payload/effect: destination/source rectangles and unit.
- Rendering/test relevance: nested container transform tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported/partial; common state nesting works, exact unit semantics can be backend-dependent.
  - AwtGdi: Supported/partial; common state nesting works, exact unit semantics can be backend-dependent.

### EmfPlusBeginContainerNoParams (0x4028)

- Category: container state.
- Purpose: opens a graphics container without explicit transform parameters.
- Key payload/effect: container id/state.
- Rendering/test relevance: nested state tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common state nesting.
  - AwtGdi: Supported for common state nesting.

### EmfPlusEndContainer (0x4029)

- Category: container state.
- Purpose: closes a graphics container.
- Key payload/effect: container id/state restore.
- Rendering/test relevance: container restore tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common state nesting.
  - AwtGdi: Supported for common state nesting.

### EmfPlusSetWorldTransform (0x402A)

- Category: transform state.
- Purpose: replaces the world transform matrix.
- Key payload/effect: matrix.
- Rendering/test relevance: transform baseline tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusResetWorldTransform (0x402B)

- Category: transform state.
- Purpose: resets the world transform to identity.
- Key payload/effect: no matrix payload.
- Rendering/test relevance: transform reset tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusMultiplyWorldTransform (0x402C)

- Category: transform state.
- Purpose: multiplies current world transform by a matrix.
- Key payload/effect: matrix and matrix-order flag.
- Rendering/test relevance: append/prepend transform order tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common matrix operations.
  - AwtGdi: Supported for common matrix operations.

### EmfPlusTranslateWorldTransform (0x402D)

- Category: transform state.
- Purpose: applies a translation to the world transform.
- Key payload/effect: dx, dy, and matrix-order flag.
- Rendering/test relevance: translation transform tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusScaleWorldTransform (0x402E)

- Category: transform state.
- Purpose: applies scaling to the world transform.
- Key payload/effect: sx, sy, and matrix-order flag.
- Rendering/test relevance: scale and axis inversion tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusRotateWorldTransform (0x402F)

- Category: transform state.
- Purpose: applies rotation to the world transform.
- Key payload/effect: angle and matrix-order flag.
- Rendering/test relevance: rotated geometry/text/image tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusSetPageTransform (0x4030)

- Category: transform state.
- Purpose: sets page unit and scale.
- Key payload/effect: unit and scale factor.
- Rendering/test relevance: unit conversion tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; common units are converted, exact world/page semantics can vary.
  - AwtGdi: Partial; common units are converted, exact world/page semantics can vary.

### EmfPlusResetClip (0x4031)

- Category: clipping.
- Purpose: resets clip to infinite.
- Key payload/effect: clears current clip.
- Rendering/test relevance: clip reset tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported.
  - AwtGdi: Supported.

### EmfPlusSetClipRect (0x4032)

- Category: clipping.
- Purpose: combines current clip with a rectangle.
- Key payload/effect: combine mode and rectangle.
- Rendering/test relevance: rectangular clip combine tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common combine modes.
  - AwtGdi: Supported for common combine modes.

### EmfPlusSetClipPath (0x4033)

- Category: clipping.
- Purpose: combines current clip with a path object.
- Key payload/effect: combine mode and path id.
- Rendering/test relevance: path clipping tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; depends on path decoding and backend clip support.
  - AwtGdi: Partial; depends on path decoding and backend clip support.

### EmfPlusSetClipRegion (0x4034)

- Category: clipping.
- Purpose: combines current clip with a region object.
- Key payload/effect: combine mode and region id.
- Rendering/test relevance: region clip combine tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; complex region combinations are backend-dependent.
  - AwtGdi: Partial; complex region combinations are backend-dependent.

### EmfPlusOffsetClip (0x4035)

- Category: clipping.
- Purpose: translates the current clipping region.
- Key payload/effect: dx and dy.
- Rendering/test relevance: clip offset tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported where backend clip translation is available.
  - AwtGdi: Supported where backend clip translation is available.

### EmfPlusDrawDriverstring (0x4036)

- Category: text drawing.
- Purpose: draws glyphs using explicit character/glyph positions.
- Key payload/effect: brush, font, matrix, glyph/string options, glyph data, and positions.
- Rendering/test relevance: positioned glyph tests, cmap lookup, vertical text, realized advance.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; common positioned text is handled, exact glyph mapping depends on fonts/backend.
  - AwtGdi: Partial; common positioned text is handled, exact glyph mapping depends on fonts/backend.

### EmfPlusStrokeFillPath (0x4037)

- Category: path drawing.
- Purpose: strokes and fills a path.
- Key payload/effect: pen id, brush id or solid color, and path id.
- Rendering/test relevance: path paint-order and combined stroke/fill tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Supported for common path/pen/brush payloads.
  - AwtGdi: Supported for common path/pen/brush payloads.

### EmfPlusSerializableObject (0x4038)

- Category: serialized object/effects.
- Purpose: stores serialized image effects or extension data.
- Key payload/effect: GUID/type plus serialized data.
- Rendering/test relevance: image effect and unsupported extension tests.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial/parsed; records are recognized, but generic image effects are not fully rendered.
  - AwtGdi: Partial/parsed; records are recognized, but generic image effects are not fully rendered.

### EmfPlusSetTSGraphics (0x4039)

- Category: terminal server state.
- Purpose: records terminal-server graphics device state.
- Key payload/effect: TS graphics state payload.
- Rendering/test relevance: compatibility fixture coverage.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed by EMF+ handlers with limited output effect.
  - AwtGdi: Partial; parsed by EMF+ handlers with limited output effect.

### EmfPlusSetTSClip (0x403A)

- Category: terminal server clipping.
- Purpose: records terminal-server clipping areas.
- Key payload/effect: TS clip rectangles/regions.
- Rendering/test relevance: compatibility fixture coverage.
- wmf2svg status:
  - WmfGdi: Passed through/comment only when raw EMF+ comment bytes are replayed; WMF has no native EMF+ record model.
  - EmfGdi: Passed through/comment as `EMR_COMMENT` when raw EMF+ comment bytes are replayed; native EMF+ interpretation is not performed by the writer.
  - SvgGdi: Partial; parsed by EMF+ handlers with limited output effect.
  - AwtGdi: Partial; parsed by EMF+ handlers with limited output effect.

## Referenced EMF+ Structures

The primary API inventory above is the official EMF+ record set. Some records reference structured payloads such as brushes, pens, paths, regions, fonts, images, image attributes, string formats, and custom line caps. These are official EMF+ data structures, but they are not separate record APIs; they are included here only to explain what future record-level tests need to cover.

- `EmfPlusObject` can define brush, pen, path, region, image, font, string-format, image-attribute, and custom-line-cap objects. Record tests should create these objects through `EmfPlusObject` and then exercise them through drawing, clipping, image, or text records.
- Brush payloads cover solid, hatch, texture, path-gradient, and linear-gradient fills, including transform, wrap, preset-color, blend-factor, focus-scale, and gamma-correction data. Support differs by backend and is captured in the `EmfPlusObject` and drawing-record status notes above.
- Pen payloads cover stroke width, brush source, line caps, joins, miter limits, dash styles, compound lines, transforms, and custom caps. These details should be tested through records such as `EmfPlusDrawLines`, `EmfPlusDrawPath`, and `EmfPlusStrokeFillPath`.
- Path and region payloads cover compressed or relative points, Bezier segments, close flags, markers, fill modes, rectangle or path region nodes, empty or infinite regions, and boolean combine nodes. These details matter most for path drawing and clipping records.
- Font and string-format payloads cover family, size, units, style flags, alignment, right-to-left and vertical layout, no-clip behavior, and hotkey-prefix settings. These details should be validated through `EmfPlusDrawString` and `EmfPlusDrawDriverstring`.
- Image and image-attribute payloads cover bitmap or metafile image data, pixel formats, compressed bitmap data, wrap and clamp behavior, source units, alpha, and image effects or serialized extension data. These details should be validated through `EmfPlusDrawImage`, `EmfPlusDrawImagePoints`, and `EmfPlusSerializableObject`.

## Implementation Notes

wmf2svg implementation constants use uppercase `EMF_PLUS_*` names, while this document uses the official `[MS-EMFPLUS]` record names for the primary record sections. Non-record constants such as object types, pixel formats, brush flags, pen flags, units, string-format flags, region-node types, and path-point types are implementation helpers for parsing the official structures listed above; they are intentionally not presented as separate APIs here.

## Coverage Checklist

- Stream/control: header, EOF, comments, `GetDC`, reserved multi-format records.
- Drawing primitives: clear, rectangles, polygons, lines, ellipses, pies, arcs, splines, Beziers.
- Paths and regions: path object creation, fill/draw/stroke-fill, region fill and clipping.
- Images: bitmap image objects, image attributes, draw image, draw image points, alpha/compositing.
- Text: font objects, string formats, draw string, driver string, text rendering hints.
- State: save/restore, containers, world/page transforms, rendering origin, antialiasing, interpolation, pixel offset, compositing.
- Clipping: reset, rectangle, path, region, offset, terminal-server clip records.
- Object payloads: brush, pen, path, region, font, image, image attributes, string format, custom line cap.
- Edge cases: continuable `EmfPlusObject`, compressed/relative points, solid-color flags, matrix order flags, unsupported serialized effects.
