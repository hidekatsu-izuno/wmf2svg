purpose
- Continue reviewing `AwtGdi` for remaining implementation gaps and implement any safe, verifiable AWT-renderable behavior.

context
- User asked again whether there are more `AwtGdi` areas where implementation can proceed.
- Existing uncommitted changes from previous AWT/GDI tasks must be preserved.
- Prior review deferred broad semantic areas such as layout mirroring, mapper flags, rel/abs mode, and color-space transforms unless a scoped Java2D effect is clear.

tasks
- status: completed
  next step: Inspect remaining AwtGdi methods and adjacent renderer patterns.
  required context: Task moved to `.tasks/`.
  action: Started this task.
- status: completed
  next step: Implement and test `extTextOut` opaque/clipped behavior.
  required context: `ETO_OPAQUE` did not grow the canvas for its background rectangle, and `ETO_CLIPPED` did not restore a null previous clip.
  action: Identified scoped `extTextOut` fixes.
- status: completed
  next step: Run focused and full Maven tests.
  required context: `extTextOut` now grows for `ETO_OPAQUE` background rectangles and restores `ETO_CLIPPED` clips even when the previous clip was null.
  action: Patched implementation and tests.
- status: completed
  next step: Move task file to done.
  required context: Focused `AwtGdiTest` and full Maven test suite both passed.
  action: Verified implementation.

goals
- Advance one more concrete `AwtGdi` behavior if available.
- Avoid speculative emulation where Java2D has no equivalent.
- Record implementation decisions and verification.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00017_awt_gdi_remaining_progress.md`

summary
- Implemented two scoped `extTextOut` fixes:
  - `ETO_OPAQUE` background rectangles now call `ensureCanvasContains` before filling, so they can expand the AWT output image.
  - `ETO_CLIPPED` now restores the previous clip in a `finally` block, including the case where the previous clip was `null`.
- Added tests for opaque text background canvas growth and null-clip restoration after clipped text output.
- Re-reviewed remaining broad candidates; layout mirroring, mapper flags, rel/abs mode, and color-space correction remain deferred because they require wider semantic emulation or have no direct Java2D rendering equivalent in the current design.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
