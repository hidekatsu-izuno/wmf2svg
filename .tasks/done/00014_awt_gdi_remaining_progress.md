purpose
- Continue reviewing `AwtGdi` for concrete implementation gaps and implement any remaining safe Java2D-renderable behavior.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from earlier AwtGdi/WMF/EMF tasks must be preserved.
- Scope remains limited to behavior that can be represented by the AWT renderer without speculative Windows-only emulation.

tasks
- status: completed
  next step: Inspect public GDI methods and stored DC state for another concrete gap.
  required context: Keep current uncommitted changes intact.
  action: Start this task.
- status: completed
  next step: Implement and test the confirmed gap.
  required context: Compare `AwtGdi` behavior with already stored state and tests.
  action: Identify a safe implementation target or document no remaining target.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Keep edits scoped to AWT implementation and focused tests.
  action: Patch implementation and tests.
- status: completed
  next step: Append summary and move task to done.
  required context: Use existing Maven test commands.
  action: Verify and close task.

goals
- Either advance one more concrete `AwtGdi` behavior or document why remaining candidates are not appropriate.
- Preserve previous uncommitted work.
- Leave a resumable task record and verification results.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00014_awt_gdi_remaining_progress.md`

summary
- Found one further concrete AwtGdi gap: text foreground drawing ignored the current `ROP2` mode, while line and filled-shape drawing now honor it.
- Added a text ROP2 path that renders the text foreground to a mask and combines the selected text color with destination pixels through the existing `applyRop2` logic.
- Refactored text foreground drawing so normal text, per-character advances, underline, and strikeout are shared between direct rendering and masked ROP2 rendering.
- Added `testTextOutUsesRop2Mode` and a color-count helper to verify text foreground ROP2 behavior.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`
