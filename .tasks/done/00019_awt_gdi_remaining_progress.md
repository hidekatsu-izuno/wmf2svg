purpose
- Continue reviewing `AwtGdi` for remaining implementation gaps and implement a safe, verifiable rendering improvement.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from earlier tasks must be preserved.
- Previous passes addressed object lifecycle, clipping return values, color bookkeeping, EMF+ image effects, bitmap ROP clipping, stretch interpolation, ROP2 fills/text, path fill-mode timing, `setPixel` growth, `extTextOut` clipping/opaque behavior, and text foreground growth.

tasks
- status: completed
  next step: Inspect stroke canvas-growth behavior.
  required context: Task moved to `.tasks/`.
  action: Started this task.
- status: completed
  next step: Add focused regression coverage.
  required context: `strokeShape` now grows for `stroke.createStrokedShape(shape)`.
  action: Implemented stroked bounds growth.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Added a thick line test that requires stroke bounds, not just line bounds, to expand the canvas.
  action: Added regression coverage.
- status: completed
  next step: Move task file to done.
  required context: Focused `AwtGdiTest` and full Maven test suite both passed.
  action: Verified implementation.

goals
- Ensure thick stroked primitives can expand the AWT output image to include their full painted outline.
- Keep broader semantic items deferred unless a direct Java2D implementation is clear.
- Record final decision and verification.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00019_awt_gdi_remaining_progress.md`

summary
- Implemented stroked-outline canvas growth in `strokeShape` by expanding for `stroke.createStrokedShape(shape)` instead of the un-stroked source shape.
- This preserves thick pen output that extends beyond the geometric centerline/path bounds and also benefits the ROP2 stroke path, which builds its mask after canvas growth.
- Added `testThickStrokeGrowsCanvasForStrokeBounds`.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
