purpose
- Continue the `AwtGdi` implementation review and advance any remaining safe, verifiable renderer work.

context
- User asked again whether there is still implementation work that can be progressed in `AwtGdi`.
- Existing uncommitted changes from tasks `00008` through `00011` must be preserved.
- Avoid speculative Windows-only behavior; prefer gaps where `AwtGdi` already stores state or has analogous behavior in another path.

tasks
- status: completed
  next step: Move task file to `.tasks/`.
  required context: Preserve current uncommitted production/test diffs.
  action: Start this task.
- status: completed
  next step: Inspect remaining state and drawing paths.
  required context: Focus on text spacing, clipping, image paths, and state already stored in `AwtDc`.
  action: Identify a concrete safe gap or document none found.
- status: completed
  next step: Implement and test any confirmed gap.
  required context: Keep changes local to AWT renderer/tests.
  action: Patch implementation and tests.
- status: completed
  next step: Run focused and full tests.
  required context: Use Maven commands already used for AWT.
  action: Verify and close task.

goals
- Advance another concrete `AwtGdi` implementation if available.
- If no safe gap remains, document the remaining categories and why they are not appropriate.
- Keep task status resumable.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00012_awt_gdi_remaining_progress.md`

summary
- Reviewed remaining stored renderer state and found that `setStretchBltMode` was stored but not applied to bitmap scaling.
- Added `AwtDc.getStretchBltMode()`.
- Applied `STRETCH_HALFTONE` as bilinear interpolation and other GDI stretch modes as nearest-neighbor interpolation in bitmap stretch paths, including `SRCCOPY` and manual ROP composition sources.
- Added a regression test that verifies `HALFTONE` produces interpolated pixels while `COLORONCOLOR` remains nearest-neighbor.
- Verification:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
