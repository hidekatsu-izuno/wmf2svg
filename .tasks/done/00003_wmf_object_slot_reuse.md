purpose
- Fix remaining WMF writer correctness gap: object table slot reuse after `deleteObject`.

context
- User asked again to find and implement insufficient WMF support in `WmfGdi`.
- Current working tree contains the previous uncommitted WMF fixes from task 00002.
- WMF object creation records are assigned to the first free playback object slot. `WmfGdi` currently assigns new object IDs using `objects.size()`, even after `deleteObject` creates a free slot.
- This can make later `selectObject` records refer to an ID different from the slot that a WMF player/parser assigns to the newly-created object.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: Keep existing object classes and record formats.
  action: Confirm the create/delete/select object slot model.
- status: completed
  next step: Object allocation helper added and create methods updated.
  required context: All create-object methods should allocate the first null object slot and store the new object there.
  action: Add an object allocation helper and update brush/font/palette/pattern/pen/region creation.
- status: completed
  next step: Focused slot-reuse test added.
  required context: Verify byte-level select IDs and round-trip parse behavior after delete/recreate.
  action: Update `WmfGdiTest`.
- status: completed
  next step: Verification complete.
  required context: Maven test commands are approved.
  action: Verify no regressions.

summary
- Added first-free-slot allocation to `WmfGdi` object creation so new brushes, fonts, palettes, pattern brushes, pens, and regions reuse deleted WMF object slots.
- Updated all create-object paths to store objects by their assigned WMF object ID.
- Added a regression test proving a newly-created object after `deleteObject` reuses the deleted slot and later `META_SELECTOBJECT` writes that reused ID.
- This fixes a WMF playback/parser mismatch where the writer previously assigned monotonically increasing IDs while WMF players assign create records to the first free object slot.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.wmf.WmfGdiTest test`
  - `mvn -q test`

goals
- New objects created after deletion use the freed WMF slot ID.
- `selectObject` records after delete/recreate select the intended object when parsed or played back.
- Existing object lifecycle behavior remains compatible.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/wmf/WmfGdiTest.java`
