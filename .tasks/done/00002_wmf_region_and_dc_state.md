purpose
- Finish remaining WMF-writable gaps in `WmfGdi` found after the first WMF pass.

context
- User asked again whether `WmfGdi` has insufficient WMF implementation and to implement all gaps.
- The worktree is clean at start.
- Remaining `UnsupportedOperationException`s are mostly APIs without standard WMF records, but two WMF-writable quality gaps remain:
  - `createRectRgn` writes a short `META_CREATEREGION` record that this repo's `WmfParser` does not recreate.
  - `WmfDc.scaleWindowExtEx` and `scaleViewportExtEx` do not return old extents or update extents consistently.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: Do not change EMF-only unsupported APIs.
  action: Confirm target gaps and expected record/state format.
- status: completed
  next step: `createRectRgn` now writes parser-compatible region records.
  required context: Match the parser's `META_CREATEREGION` read layout and keep `WmfRectRegion` object behavior.
  action: Write a parser-compatible rectangular region record.
- status: completed
  next step: `WmfDc` scale methods now update extents and old values.
  required context: Return current extents in `old`, then update extents using multiplicand/divisor pairs when safe.
  action: Replace placeholder scale tracking with extent updates.
- status: completed
  next step: Focused tests added.
  required context: Verify record bytes and round-trip parse behavior where practical.
  action: Update `WmfGdiTest`.
- status: completed
  next step: Verification complete.
  required context: Maven test commands are approved.
  action: Verify no regressions.

summary
- `WmfGdi.createRectRgn` now emits a parser-compatible rectangular `META_CREATEREGION` record, instead of the previous short record that `WmfParser` skipped.
- `WmfGdi.extSelectClipRgn` now supports `RGN_COPY` by emitting `META_SELECTCLIPREGION`; other combine modes remain unsupported because WMF has no matching combine-region record.
- `WmfDc.scaleWindowExtEx` and `scaleViewportExtEx` now return old extents and update stored extents.
- Removed stale internal scale/offset fields in `WmfDc`.
- Added tests for region record bytes, region round-trip to SVG, `extSelectClipRgn(RGN_COPY)`, and scale old-value/state behavior.
- Remaining `UnsupportedOperationException`s correspond to APIs with no standard WMF record in this writer path, such as alpha/transparent/mask/plg blits, paths, Bezier records, ICM/color space, gradients, arc direction, and non-copy extended clip-region combines.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.wmf.WmfGdiTest test`
  - `mvn -q test`

goals
- `createRectRgn` output can be parsed back by `WmfParser` and used by region operations.
- Window/viewport scale operations update state and old values correctly.
- Unsupported APIs that remain are not standard WMF record types.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/wmf/WmfGdiTest.java`
