purpose
- Re-check `WmfGdi` for remaining insufficient WMF writer implementations and implement all confirmed standard WMF gaps.

context
- Previous passes fixed WMF comment/escape, save/restore state, viewport/window state, region creation, `RGN_COPY`, scale extents, and object slot reuse.
- The user asked again whether `WmfGdi` still has insufficient WMF implementation.
- `SvgGdi` and `AwtGdi` object-slot handling was investigated separately and did not require production changes.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: Existing untracked task record `.tasks/done/00005_svg_awt_object_lifecycle.md` is unrelated investigation output.
  action: Start this task.
- status: completed
  next step: Inventory completed under the strict one-MS-WMF-record policy.
  required context: Compare against `WmfParser` records and WMF/EMF boundary.
  action: Identify concrete gaps that can be represented by standard WMF records.
- status: completed
  next step: No additional confirmed one-record MS-WMF gaps found.
  required context: Keep record byte order compatible with existing parser helpers and tests.
  action: Patch `WmfGdi` and related tests.
- status: completed
  next step: Verification complete.
  required context: Use existing Maven commands.
  action: Run tests and record results.

goals
- Either produce code fixes for all confirmed remaining WMF-writer gaps or document why remaining unsupported methods are not standard WMF.
- Add tests for each implemented behavior.
- Leave task status resumable and finish by moving to `.tasks/done/`.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/wmf/WmfGdiTest.java`
- `.tasks/00006_wmf_gdi_remaining_records.md`

notes
- Strict policy confirmed by user: implement only operations that can be represented as a single MS-WMF-defined record in `WmfGdi`.
- `angleArc` and `arcTo` do not have dedicated MS-WMF record types. A temporary decomposition to `LINETO` + `ARC` + `MOVETO` was removed because it violates this policy.
- Remaining unsupported methods reviewed so far are EMF/GDI APIs with no single MS-WMF record counterpart, path APIs, ICM/color-space APIs, alpha/transparent/mask/plg blits, gradient fill, Bezier, brush origin, miter limit, meta region, arc direction, or non-copy clipping combination.

summary
- Re-applied the strict one-record policy for `WmfGdi`.
- Removed the temporary `angleArc` / `arcTo` decomposition because those APIs do not have dedicated MS-WMF record types.
- No additional production code changes are needed beyond the already committed WMF fixes.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.wmf.WmfGdiTest test`
  - `mvn -q test`
