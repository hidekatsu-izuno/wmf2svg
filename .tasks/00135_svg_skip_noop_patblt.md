purpose
- Decide whether SvgGdi should skip no-op PatBlt rectangles with zero device width or height, using real WMF/EMF playback before implementation.

context
- AwtGdi's patBlt computes device bounds and returns when the resulting pixel rectangle is empty.
- SvgGdi currently emits an SVG rect even when the mapped PatBlt width or height is zero.
- SVG zero-size rects are not visible, but they add dead output and can interact poorly with filters/masks.
- User explicitly requested actual WMF/EMF generation and rendering verification before making further SvgGdi changes.

tasks
- [x] Generate WMF/EMF probes with zero-width and zero-height PatBlt records plus a positive-size control.
- [x] Render the probes with direct Win32 GDI playback, not System.Drawing.
- [x] Record observed pixel output.
- [ ] If WMF/EMF playback confirms zero-size PatBlt is no-op, update SvgGdi to skip zero-size PatBlt output.
- [ ] Add focused SvgGdi regression tests.
- [ ] Run focused SvgGdi tests.
- [ ] Run the full Maven test suite.

goals
- SvgGdi only changes if direct WMF/EMF playback confirms the behavior.
- Positive-size PatBlt output remains unchanged.
- Verification results are recorded and the task is moved to done when complete.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: blocked; verification did not produce a stable positive-size control.
- next step: do not change SvgGdi for this candidate unless a stable PatBlt metafile playback path is found.

verification notes
- Generated WmfGdi/EmfGdi probes under `/tmp/wmf2svg-noop-patblt`:
  - `patblt-normal.wmf`, `patblt-width0.wmf`, `patblt-height0.wmf`
  - `patblt-normal.emf`, `patblt-width0.emf`, `patblt-height0.emf`
- Direct Win32 GDI playback script used `GetMetaFileW`/`PlayMetaFile` and `GetEnhMetaFileW`/`PlayEnhMetaFile` against a 40x40 DIB section initialized to gray.
- EmfGdi-generated `patblt-normal.emf` crashed during `PlayEnhMetaFile` before any pixel result could be collected.
- As a cross-check, generated direct Win32 GDI WMF/EMF files with `CreateMetaFileW`/`CreateEnhMetaFileW` and `PatBlt`:
  - `PatBlt` returned true for normal, zero-width, and zero-height records during recording.
  - Playback still crashed on the positive-size control (`direct-normal.emf` and `direct-normal.wmf`), so this environment does not provide a reliable rendered comparison for PatBlt.
- Because the positive-size control cannot be rendered reliably, zero-size PatBlt skipping is not implemented.
