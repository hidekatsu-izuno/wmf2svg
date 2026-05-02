purpose
- Check `EmfGdi` for missing implementations that can be represented as exactly one MS-EMF or EMF+ record, and implement confirmed gaps.

context
- User wants the same strict policy used for `WmfGdi`: only APIs with a one-record representation in the target metafile format should be implemented.
- For `EmfGdi`, both MS-EMF records and EMF+ records are in scope.
- Existing task records for WMF investigations are uncommitted and unrelated to production code.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: Work in strict one-record mode.
  action: Start this task.
- status: completed
  next step: Inventory completed.
  required context: Compare with `EmfParser`, EMF+ parser/writer helpers, and constants.
  action: Identify one-record MS-EMF/EMF+ gaps.
- status: completed
  next step: No confirmed one-record MS-EMF/EMF+ gaps found.
  required context: Preserve existing binary record patterns and object table semantics.
  action: Patch production code and focused tests.
- status: completed
  next step: Verification complete.
  required context: Use available Maven test commands.
  action: Record results and finish task.

goals
- Implement every confirmed one-record MS-EMF/EMF+ writer gap in `EmfGdi`.
- Leave non-one-record APIs unsupported or documented as out of scope.
- Move this task to `.tasks/done/` with a clear summary.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/emf`
- `.tasks/00007_emf_gdi_remaining_records.md`

notes
- `EmfGdi` already implements one-record MS-EMF equivalents for standard EMF drawing/state/object records, including `EMR_ANGLEARC`, `EMR_ARCTO`, path records, Bezier records, bitmap transfer records, `EMR_ALPHABLEND`, `EMR_TRANSPARENTBLT`, `EMR_GRADIENTFILL`, ICM/color-space records, and escape/comment records.
- Remaining explicit unsupported methods are not confirmed one-record MS-EMF/EMF+ gaps:
  - `animatePalette`: EMF has palette entry/realize records but no `AnimatePalette` record.
  - `createPatternBrush`: EMF has `EMR_CREATEDIBPATTERNBRUSHPT` and `EMR_CREATEMONOBRUSH`, both exposed as `dibCreatePatternBrush` and `createMonoBrush`; the generic DDB `CreatePatternBrush` API is not a distinct EMF record.
  - `dibBitBlt` / `dibStretchBlt`: EMF uses `EMR_BITBLT`, `EMR_STRETCHBLT`, `EMR_SETDIBITSTODEVICE`, and `EMR_STRETCHDIBITS`; the WMF-named DIBBITBLT/DIBSTRETCHBLT records do not exist in EMF.
  - `floodFill`: EMF has `EMR_EXTFLOODFILL`, already implemented as `extFloodFill`, but no separate `EMR_FLOODFILL`.
  - `offsetViewportOrgEx` / `offsetWindowOrgEx`: EMF has set-origin records but no offset-origin records.
  - `setRelAbs`: no EMF/EMF+ record counterpart.
  - `setTextCharacterExtra`: no MS-EMF record counterpart; EMF has `EMR_SETTEXTJUSTIFICATION` and text-output records instead.
- No production code changes are needed.

summary
- Reviewed `EmfGdi` under the strict one-record MS-EMF / EMF+ policy.
- Found no production implementation gaps: APIs with direct MS-EMF record counterparts are already implemented, and remaining unsupported methods do not have a distinct one-record representation in MS-EMF/EMF+ or are exposed through more specific existing methods.
- No Java files were changed.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.emf.EmfGdiTest,net.arnx.wmf2svg.gdi.emf.FulltestEmfGeneratorTest test`
  - `mvn -q test`
