purpose
- Resolve GitHub issue #25: WMF inputs generate extremely large SVG files with deeply nested masks in 0.10.4.
- Treat the issue attachment as untrusted. Download only to /tmp, inspect the ZIP and WMF binary structure statically, and proceed only if the archive contents are ordinary WMF samples with no path traversal, executables, scripts, or suspicious container behavior.

context
- User requested fixing https://github.com/hidekatsu-izuno/wmf2svg/issues/25.
- Issue reports `Dessin3.wmf` and `Dessin5.wmf` generating ~187-188 MiB SVG files with incremental `<mask>` definitions and nested `<g mask=...>` output, compared to much smaller output from 0.9.11.
- Current likely area is SVG clipping/mask state management in `SvgGdi` and WMF clip-region parsing in `WmfParser`.
- Attachment URL from issue: `https://github.com/user-attachments/files/28225439/wmfs_having_the_problem.zip`.

tasks
- [completed] Security triage for attachment.
  - status: completed
  - next step: none
  - required context: attachment is untrusted; use static inspection commands first.
- [completed] Static WMF structure analysis.
  - status: completed
  - next step: none
  - required context: no rendering or shell execution from attachment; parsing bytes is allowed after ZIP triage.
- [completed] Reproduce regression locally.
  - status: completed
  - next step: none
  - required context: expected bad signature is huge output with many masks/nested mask groups.
- [completed] Identify minimal design fix.
  - status: completed
  - next step: none
  - required context: keep behavior consistent with existing region and EMF+ tests.
- [completed] Implement focused code and test changes.
  - status: completed
  - next step: none
  - required context: for multi-file changes, process one file at a time and verify before proceeding.
- [completed] Verify and review.
  - status: completed
  - next step: none
  - required context: sub-agent was not used because available sub-agent tooling is restricted to explicit user requests; verification was isolated with targeted and full Maven commands.
- [completed] Finish task record.
  - status: completed
  - next step: move this file to `.tasks/done/00230_fix_issue25_wmf_mask_growth.md`.
  - required context: do this only after implementation and verification complete.

goals
- Attachment is handled as untrusted and documented with hashes and static inspection results.
- Current code no longer emits pathological mask growth for `Dessin3.wmf` and `Dessin5.wmf`.
- Regression test covers the relevant clipping behavior without depending on the untrusted attachment.
- Targeted tests pass, and issue sample output size/mask counts are reduced to a usable range.

file list
- `.tasks/00230_fix_issue25_wmf_mask_growth.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java` (likely)
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java` (likely)
- Additional files only if analysis shows the root cause is elsewhere.

summary
- Attachment triage:
  - Downloaded to `/tmp/wmf2svg-issue25/wmfs_having_the_problem.zip`.
  - ZIP SHA-256: `2742e404970f40d3befdb808fedd365ac29b05fb17851d747ef151ce8b047c91`.
  - ZIP contains only `Dessin3.wmf` and `Dessin5.wmf`; no absolute paths, traversal paths, scripts, executables, or mixed payloads were listed.
  - Extracted only those two WMF files to `/tmp/wmf2svg-issue25/samples/`.
  - `file` identifies both as Windows metafiles with placeable bounds.
  - `Dessin3.wmf` SHA-256: `9537f00194ddabf14c71af405efb9ac7a93c59bf03180c944db38e94c5533957`.
  - `Dessin5.wmf` SHA-256: `9bd79a4eaf89c8b5cdd6a13263484dec5deac0680aae538bf53e09229604a8bd`.
- Static WMF record analysis:
  - Both samples have 12,538 records and 10,362 `META_EXCLUDECLIPRECT` records.
  - Early records are dominated by an initial `META_INTERSECTCLIPRECT` followed by many `META_EXCLUDECLIPRECT` operations before drawing.
- Reproduction:
  - Before fix, `Dessin5.svg` generated from current code was 196,936,769 bytes with 10,406 `<mask>` elements and 10,406 mask groups.
- Decision:
  - Root cause was `SvgGdi.excludeClipRect` cloning the entire current mask for every exclude operation even when no drawing had occurred under the pending mask group.
  - The fix keeps the existing clone-and-new-group behavior once the current masked group has children, preserving previous drawing. For consecutive clipping before drawing, it appends the black exclusion rect to the existing pending mask instead.
- Changes:
  - Updated `SvgGdi.excludeClipRect`.
  - Added `SvgGdiTest.testConsecutiveExcludeClipRectsBeforeDrawingReusePendingMask`.
- Verification:
  - Targeted test: `mvn -q -Dtest=SvgGdiTest#testConsecutiveExcludeClipRectsBeforeDrawingReusePendingMask test`.
  - Full tests: `mvn -q test`.
  - Full verify: `mvn -q verify`.
  - After fix, `Dessin5.svg` is 857,358 bytes with 44 masks/groups.
  - After fix, `Dessin3.svg` is 1,911,214 bytes with 44 masks/groups.
