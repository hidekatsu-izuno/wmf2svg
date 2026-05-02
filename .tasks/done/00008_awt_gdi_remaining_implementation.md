purpose
- Review `AwtGdi` for remaining implementation gaps that should be handled in the Java2D renderer.

context
- User asked whether `AwtGdi` still has parts that should be implemented.
- Unlike `WmfGdi` and `EmfGdi`, `AwtGdi` is a rendering backend, so the criterion is whether a GDI operation can be represented faithfully enough with Java2D state/drawing.
- Existing task records from WMF/EMF investigations are uncommitted and unrelated to production code.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: Focus on review first; implement only confirmed renderer gaps.
  action: Start this task.
- status: completed
  next step: Inventory completed.
  required context: Compare with `SvgGdi`, parser calls, and existing tests.
  action: Identify gaps that should be implemented.
- status: completed
  next step: Clip-region return value gap implemented.
  required context: Preserve Java2D rendering semantics and existing behavior.
  action: Patch `AwtGdi` and tests.
- status: completed
  next step: Move task file to `.tasks/done/`.
  required context: Focused and full Maven tests passed.
  action: Record results and move task to done.

goals
- Provide a concrete answer on remaining `AwtGdi` implementation work.
- Implement confirmed gaps with tests, or document why remaining no-ops/unsupported methods are acceptable.
- Keep the task resumable and finish cleanly.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00008_awt_gdi_remaining_implementation.md`

summary
- Reviewed `AwtGdi` public methods and compared remaining no-op-like methods with Java2D renderer semantics.
- Found one implementable gap: `extSelectClipRgn` and `setMetaRgn` always returned `SIMPLEREGION`, even when the current clip was absent or complex.
- Implemented clip-region result classification from the current Java2D clip and preserved copy-mode rectangle clips as rectangle shapes so simple regions can be recognized.
- Added a regression test for `NULLREGION` and `SIMPLEREGION` return values.
- Remaining no-op-like methods are acceptable renderer limitations or have no direct `Graphics2D` state equivalent: palette realization/resizing, GDI object deletion side effects, color adjustment, and ICM/profile matching.
- Verification:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
