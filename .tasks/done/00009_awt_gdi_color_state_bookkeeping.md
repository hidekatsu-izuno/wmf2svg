purpose
- Implement non-rendering bookkeeping for AWT GDI color-management state.

context
- User asked to implement the remaining bookkeeping mentioned for `setICMMode`, ICM profile, and color adjustment.
- `AwtGdi` is a Java2D renderer, so this task does not add visual color conversion. It stores API state consistently with DC save/restore.
- Existing untracked done task files are unrelated to production changes.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: Use `AwtDc` for state that should follow save/restore.
  action: Start this task.
- status: completed
  next step: Patch `AwtDc` and `AwtGdi`.
  required context: `AwtDc.clone()` is used by `seveDC`; mutable byte arrays need deep-copy handling.
  action: Implement ICM mode, profile, and color adjustment state.
- status: completed
  next step: Add regression tests.
  required context: Tests should verify old ICM mode return and save/restore behavior without depending on rendering changes.
  action: Cover bookkeeping behavior.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Use available Maven commands.
  action: Verify and close task.

goals
- `setICMMode` returns the previous mode and stores the new mode.
- `setICMProfile` and `setColorAdjustment` store defensive copies.
- Saved/restored DC state restores the stored bookkeeping state.
- Existing rendering behavior remains unchanged.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00009_awt_gdi_color_state_bookkeeping.md`

summary
- Added ICM mode, ICM profile, and color adjustment bookkeeping to `AwtDc`.
- `setICMMode` now returns the previous mode and stores the new mode.
- `setICMProfile`, `colorMatchToTarget`, and `setColorAdjustment` now store defensive copies through the current DC.
- `AwtDc.clone()` deep-copies stored byte-array state so `saveDC`/`restoreDC` restores bookkeeping correctly.
- Added package-private accessors on `AwtGdi` for tests and covered old-value return, defensive copies, `colorMatchToTarget`, and saved DC restoration.
- Verification:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
