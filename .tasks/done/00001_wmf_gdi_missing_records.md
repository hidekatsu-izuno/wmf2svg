purpose
- Identify WMF-writable gaps in WmfGdi and implement every missing method that can be represented by WMF records.

context
- User asked whether the WMF implementation for "AwsGdi" has insufficient areas and to implement all gaps.
- No AwsGdi class exists. The relevant WMF writer is `net.arnx.wmf2svg.gdi.wmf.WmfGdi`; `AwtGdi` is a raster renderer.
- `WmfGdi` contains many `UnsupportedOperationException`s, but several correspond to EMF-only APIs or GDI functions with no standard WMF record. Those should remain unsupported unless a safe WMF representation exists.
- Existing dirty files not owned by this task: `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`, `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`, and untracked `AGENTS.md`.

tasks
- status: completed
  next step: Classification complete.
  required context: Work only on WMF writer/parser/test files unless verification requires otherwise.
  action: Inspect unsupported WmfGdi methods and classify them as WMF-recordable or unsupported by WMF.
- status: completed
  next step: Code changes are in place.
  required context: Preserve existing record serialization style and little-endian helpers.
  action: Add constants/helpers and method bodies for WMF-recordable gaps.
- status: completed
  next step: Focused tests added.
  required context: Tests should validate serialized record contents and round-trip parse behavior where practical.
  action: Update `WmfGdiTest` or add focused WMF writer tests.
- status: completed
  next step: Verification complete.
  required context: Approved Maven commands are available.
  action: Verify WmfGdi changes and record results.

summary
- Implemented WMF-recordable gaps in `WmfGdi`: `comment(byte[])` now preserves WMF escape payloads, `selectClipRgn(null)` emits a null clipping region instead of throwing, and SaveDC/RestoreDC now preserves writer-side DC state.
- Updated `WmfGdi` to keep `WmfDc` state current for window/viewport origin and extent setters, so old-value parameters and later state-dependent calls are consistent.
- Updated `WmfDc` with clone support and missing window origin/extent state.
- Left EMF-only or non-WMF-recordable APIs unsupported. Microsoft MS-WMF RecordType enumeration does not define records for alpha blend, transparent blit, path operations, polybezier, ICM, color space, gradient fill, or arc direction.
- Added focused tests in `WmfGdiTest` for comment escape preservation, null clip region selection, DC state restoration, and old-value state updates.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.wmf.WmfGdiTest test`
  - `mvn -q test`

goals
- `WmfGdi` no longer throws for WMF-recordable operations that were missing.
- Unsupported methods left behind have a clear reason: no standard WMF representation or EMF-only behavior.
- Tests demonstrate the implemented WMF records are serialized correctly.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfConstants.java`
- `src/test/java/net/arnx/wmf2svg/gdi/wmf/WmfGdiTest.java`
