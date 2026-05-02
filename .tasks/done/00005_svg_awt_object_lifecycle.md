purpose
- Determine whether `SvgGdi` and `AwtGdi` need the same delete/create object-slot behavior as `WmfGdi`, and implement fixes if needed.

context
- `WmfGdi` required WMF object table slot reuse because WMF records refer to playback object indexes.
- `SvgGdi` and `AwtGdi` are renderer backends that maintain current object state directly, so the needed behavior might differ.
- The user asked whether the same handling is necessary for `SvgGdi` and `AwtGdi`.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: No code changes until the object lifecycle model is checked.
  action: Start the task.
- status: completed
  next step: Inspection shows parser-owned object tables handle WMF slot reuse before objects reach these backends.
  required context: Compare against parser behavior and `GdiObject` interfaces.
  action: Identify whether stale selected objects survive deletion or whether object IDs matter.
- status: completed
  next step: No production fix needed for slot reuse in renderer backends.
  required context: Preserve backend-specific design; avoid adding WMF index tables where they are not meaningful.
  action: Patch implementation and tests.
- status: completed
  next step: Focused SVG/AWT backend tests passed.
  required context: Use Maven tests available in the project.
  action: Confirm no regressions.

goals
- Give a concrete answer for `SvgGdi` and `AwtGdi`.
- Fix any confirmed lifecycle bugs.
- Add regression tests where the behavior is observable.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java`

summary
- `SvgGdi` and `AwtGdi` do not serialize WMF object indexes and do not own a WMF Object Table.
- `WmfParser` owns playback object slots for parsed WMF input; creation records already scan the object array for the first `null` slot, and `META_DELETEOBJECT` clears that slot before future creation records reuse it.
- `SvgGdi` and `AwtGdi` receive already-resolved `GdiObject` references from the parser, so `WmfGdi`'s writer-side first-free-ID allocation does not apply to them.
- No production code changes were needed.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest,net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`.
