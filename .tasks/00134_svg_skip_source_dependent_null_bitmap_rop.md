purpose
- Align SvgGdi's source-less bitmap ROP handling with AwtGdi for source-dependent ROPs.

context
- AwtGdi only falls back to PatBlt for null/empty DIB BitBlt sources when the ROP can be rendered without a source bitmap: BLACKNESS, DSTINVERT, PATCOPY, PATINVERT, and WHITENESS.
- SvgGdi currently maps source-less MERGECOPY to BLACKNESS and source-less PATPAINT to WHITENESS.
- MERGECOPY and PATPAINT are source-dependent ROPs, so drawing black/white when the source image is missing can introduce visible output that AwtGdi would skip.
- This change does not attempt exact XOR compositing and does not affect font handling.

tasks
- [x] Generate WMF/EMF probes for source-less MERGECOPY and PATPAINT.
- [x] Render the generated WMF/EMF probes with direct Win32 GDI, not System.Drawing.
- [x] Record the observed pixel output and decide whether SvgGdi should change.
- [ ] Restrict SvgGdi source-less PatBlt fallback to source-independent ROPs only if the probes show no output.
- [ ] Update SvgGdi tests according to the observed WMF/EMF behavior.
- [ ] Keep source-independent ROP behavior unchanged.
- [ ] Run focused SvgGdi tests.
- [ ] Run the full Maven test suite.

goals
- SvgGdi no longer emits black/white rectangles for null/empty bitmap sources with source-dependent ROPs.
- PATCOPY, PATINVERT, DSTINVERT, BLACKNESS, and WHITENESS still render through PatBlt when no source bitmap is present.
- Verification results are recorded and the task is moved to done when complete.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: verification blocked a confident implementation.
- next step: decide whether to change SvgGdi based on the observed EMF create failure and WMF playback instability.

verification notes
- WmfGdi/EmfGdi probes were generated under /tmp/wmf2svg-null-source-rop.
- EmfGdi empty-DIB EMR_BITBLT probes crashed PlayEnhMetaFile, so they are not a reliable representation of a valid source-less record.
- Direct Win32 GDI CreateEnhMetaFile + BitBlt with hdcSrc = NULL:
  - MERGECOPY: BitBlt returned false.
  - PATPAINT: BitBlt returned false.
  - PATCOPY: BitBlt returned true, but replaying that probe crashed in this environment before pixel analysis.
- WmfGdi META_DIBBITBLT(null) probes crashed PlayMetaFile for MERGECOPY and also for PATCOPY when isolated, so WMF playback did not provide a stable pixel result.
- Decision for now: do not change SvgGdi behavior without an additional stable WMF/EMF playback method or user approval, because the direct verification did not produce a clean comparable rendered image for all cases.
