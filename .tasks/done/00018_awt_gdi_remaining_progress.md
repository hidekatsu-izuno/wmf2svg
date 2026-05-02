purpose
- Continue reviewing `AwtGdi` for remaining implementation gaps and implement any safe, verifiable behavior.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from earlier tasks must be preserved.
- Previous passes addressed object lifecycle, clipping return values, color bookkeeping, EMF+ image effects, bitmap ROP clipping, stretch interpolation, ROP2 fills/text, path fill-mode timing, `setPixel` canvas growth, and `extTextOut` opaque/clipped restoration.

tasks
- status: completed
  next step: Inspect remaining text, clipping, path, and bitmap edge cases.
  required context: Task moved to `.tasks/`.
  action: Started this task.
- status: completed
  next step: Implement and test text foreground canvas growth.
  required context: `drawText` did not call `ensureCanvasContains` for the text foreground bounds when no opaque background was drawn.
  action: Identified text foreground canvas growth as a safe implementation target.
- status: completed
  next step: Run focused and full Maven tests.
  required context: `drawText` now expands the canvas for transformed text foreground bounds before drawing.
  action: Patched implementation and added a foreground text growth test.
- status: completed
  next step: Move task file to done.
  required context: Focused `AwtGdiTest` and full Maven test suite both passed.
  action: Verified implementation.

goals
- Advance one more concrete `AwtGdi` behavior if available.
- Keep deferred items documented when they require broader semantic emulation.
- Record final decision and verification.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00018_awt_gdi_remaining_progress.md`

summary
- Implemented text foreground canvas growth in `drawText` by expanding to the transformed text bounds before drawing.
- Added `testTextOutGrowsCanvasForForegroundText` to verify transparent foreground text outside the initial canvas is retained.
- Re-reviewed `ETO_PDY`, layout mirroring, mapper flags, rel/abs mode, and color-space correction; these remain deferred because they need broader parser/semantics work or no direct Java2D equivalent in the current design.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
