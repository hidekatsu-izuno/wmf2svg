purpose
- Continue reviewing `AwtGdi` for remaining implementation gaps and implement a safe, verifiable rendering improvement.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from earlier tasks must be preserved.
- Most direct primitive, text, clipping, bitmap, ROP, and canvas-growth gaps have already been addressed in previous tasks.

tasks
- status: completed
  next step: Implement region inversion canvas growth.
  required context: Task moved to `.tasks/`.
  action: Started this task.
- status: completed
  next step: Add focused regression coverage.
  required context: `xorFill` now calls `ensureCanvasContains(shape)`.
  action: Implemented region inversion canvas growth.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Added `testInvertRgnGrowsCanvas`.
  action: Added focused regression coverage.
- status: completed
  next step: Move task file to done.
  required context: Focused `AwtGdiTest` and full Maven test suite both passed.
  action: Verified implementation.

goals
- Make `invertRgn` consistent with other painted region operations in the auto-growing AWT canvas.
- Keep changes scoped to Java2D-renderable behavior.
- Record final decision and verification.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00021_awt_gdi_remaining_progress.md`

summary
- Reworked `xorFill` to use an explicit shape mask and pixel-wise XOR instead of relying on Java2D `setXORMode`, which did not produce reliable ARGB inversion in the tested path.
- `xorFill` now expands the canvas for the target shape before applying the masked XOR.
- Added `testInvertRgnGrowsCanvas` to verify both region-growth behavior and actual color inversion.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
