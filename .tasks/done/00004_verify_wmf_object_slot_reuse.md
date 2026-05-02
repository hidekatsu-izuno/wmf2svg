purpose
- Verify the source and runtime behavior for WMF object table slot reuse after `META_DELETEOBJECT`.

context
- User asked where the claim came from and whether it can be checked with PowerShell.
- The implementation currently assumes create-object records are assigned to the first free playback object slot.

tasks
- status: completed
  next step: Task file moved to `.tasks/`.
  required context: No source changes are intended for this investigation.
  action: Start the verification task.
- status: completed
  next step: Microsoft MS-WMF object table and object record type sections identified.
  required context: Prefer Microsoft WMF / MS-WMF documentation.
  action: Identify and cite the relevant source.
- status: completed
  next step: PowerShell / System.Drawing rendered the crafted WMF and sampled pixels.
  required context: The crafted WMF should delete object slot 0, create a replacement object, then select slot 0.
  action: Determine whether Windows playback uses the replacement object.
- status: completed
  next step: Evidence summarized below.
  required context: Include commands/results and update this task file.
  action: Report findings.

goals
- Explain the source of the slot reuse claim.
- Confirm or refute the behavior using Windows playback through PowerShell.
- Avoid production code changes unless the evidence contradicts the current implementation.

file list
- `.tasks/todo/00004_verify_wmf_object_slot_reuse.md`

summary
- Source: Microsoft MS-WMF section 3.1.4.1 says newly created graphics objects are assigned the lowest available WMF Object Table index, starting at 0, and that `META_DELETEOBJECT` returns the index to the available pool for later reuse.
- Source: Microsoft MS-WMF section 2.3.4 says object creation records must assign the lowest-numbered available index, and later records must refer to that assigned index.
- PowerShell verification created a WMF with: create red brush in slot 0, delete slot 0, create blue brush, select object index 0, draw rectangle.
- Windows `System.Drawing.Imaging.Metafile` playback rendered the rectangle center as RGB `0,0,255`, confirming slot 0 was reused.
- A contrast WMF selecting index 1 after the same records rendered RGB `255,255,255`, confirming the new brush was not assigned to a monotonic slot 1 in Windows playback.
