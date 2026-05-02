purpose
- Continue reviewing `AwtGdi` for remaining implementation gaps and implement a safe, verifiable rendering improvement.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from earlier tasks must be preserved.
- `setStretchBltMode` already controls interpolation for `stretchDIBits`/ROP composition paths, but other scaled `drawImage` paths still use the default interpolation.

tasks
- status: completed
  next step: Apply stretch interpolation hint to remaining scaled bitmap paths.
  required context: Task moved to `.tasks/`.
  action: Started this task.
- status: completed
  next step: Add focused regression coverage.
  required context: `drawAlphaBlend`, `drawDIBitsToDevice`, and `drawPlgBlt` now apply the stretch interpolation hint when scaling.
  action: Patched remaining scaled bitmap paths.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Added `testSetDIBitsToDeviceUsesStretchBltInterpolation`.
  action: Added focused regression coverage.
- status: completed
  next step: Move task file to done.
  required context: Focused `AwtGdiTest` and full Maven test suite both passed.
  action: Verified implementation.

goals
- Make scaled bitmap paths consistently honor `setStretchBltMode`.
- Keep changes scoped to Java2D rendering behavior.
- Record final decision and verification.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00020_awt_gdi_remaining_progress.md`

summary
- Applied `setStretchBltMode` interpolation hints to additional scaled bitmap paths:
  - `drawAlphaBlend`
  - `drawDIBitsToDevice`
  - `drawPlgBlt`
- Preserved previous `Graphics2D` interpolation hints and transforms after temporary changes.
- Added `testSetDIBitsToDeviceUsesStretchBltInterpolation` using anisotropic mapping to exercise scaling through `SetDIBitsToDevice`.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
