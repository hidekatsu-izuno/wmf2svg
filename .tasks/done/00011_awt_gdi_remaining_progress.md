purpose
- Continue reviewing `AwtGdi` for remaining safe implementation work and complete any confirmed gaps.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted AWT changes must be preserved.
- Focus on behavior that can be implemented within the current Java2D renderer design, with tests.

tasks
- status: completed
  next step: Move task file to `.tasks/`.
  required context: `.tasks/done/00008` through `00010` already cover recent AWT work.
  action: Start this task.
- status: completed
  next step: Inspect remaining renderer-state and drawing gaps.
  required context: Check partial implementations, not speculative Windows-only color-management features.
  action: Identify any safe next implementation target.
- status: completed
  next step: Patch implementation and tests if a confirmed gap exists.
  required context: Preserve existing uncommitted changes.
  action: Implement confirmed gap.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Use existing Maven commands.
  action: Verify and close task.

goals
- Advance any remaining AWT implementation gap that is concrete and verifiable.
- Document if only unsupported/speculative areas remain.
- Keep task state resumable.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00011_awt_gdi_remaining_progress.md`

summary
- Reviewed remaining AWT renderer-state and drawing paths after the prior clip, color-state, and EMF+ image-effect work.
- Found a concrete rendering gap in manual bitmap ROP composition: non-`SRCCOPY` bitmap ROP loops and `MaskBlt` ROP loops wrote directly to the backing image and did not honor the current clip region.
- Added `isInClip` checks to both manual composition loops.
- Added regression tests for clipped `dibBitBlt` ROP3 composition and clipped `maskBlt` composition.
- Remaining no-op-like areas are still not safe Java2D rendering targets: palette color correction/realization, Windows ICM color conversion, and Java object deletion side effects.
- Verification:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
