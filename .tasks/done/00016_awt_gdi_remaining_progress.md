purpose
- Continue reviewing `AwtGdi` for additional implementation gaps and implement any safe, verifiable improvement.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from previous tasks must be preserved.
- Scope remains concrete Java2D-renderable behavior or necessary renderer bookkeeping with tests.

tasks
- status: completed
  next step: Inspect remaining no-op/stored state paths.
  required context: Keep current uncommitted diffs intact.
  action: Start this task.
- status: completed
  next step: Implement and test `setPixel` canvas growth.
  required context: `setPixel` draws directly but did not call `ensureCanvasContains`, unlike other primitive drawing paths.
  action: Identified `setPixel` canvas growth as a safe implementation target.
- status: completed
  next step: Run focused and full Maven tests.
  required context: `setPixel` now calls `ensureCanvasContains` before filling its target pixel rectangle.
  action: Patched implementation and added `testSetPixelGrowsCanvas`.
- status: completed
  next step: Move task file to done.
  required context: Focused `AwtGdiTest` and full Maven test suite both passed.
  action: Verified implementation.

goals
- Advance one more concrete `AwtGdi` behavior if available.
- Avoid speculative emulation where Java2D has no equivalent.
- Record final decision and verification.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00016_awt_gdi_remaining_progress.md`

summary
- Implemented `setPixel` canvas growth by calling `ensureCanvasContains` on the target pixel rectangle before filling it.
- Added `testSetPixelGrowsCanvas` to verify that a pixel outside the initial 1x1 canvas expands the image and is painted.
- Reviewed remaining stored/no-op AWT state candidates; deferred layout, mapper flags, rel/abs mode, and color-space rendering because they need broader semantic emulation or have no direct Java2D rendering effect in the current design.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
