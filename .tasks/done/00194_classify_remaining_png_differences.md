purpose
- Identify which remaining large PNG differences between `../wmf-testcase/data/png` and `etc/data/dst` are likely not caused by font rendering differences.

context
- The reference data is `../wmf-testcase/data/png/*.png`.
- The comparison data is `etc/data/dst/*.png`.
- Anti-aliasing differences are acceptable.
- `github_docx4j_gradient.png` alpha issue was fixed before this task.

tasks
- [x] Recompute per-image difference metrics after the latest renderer output.
- [x] Inspect top difference images visually and numerically.
- [x] Classify large differences into likely font-only and likely non-font categories.
- [x] Report candidates with the observed reason.

goals
- Provide a short list of images where non-font causes likely remain.
- Include enough evidence to decide the next investigation target.

file list
- None expected unless a temporary comparison artifact is useful.

status
- Current status: completed.
- Next step: none.
- Required context to continue: use ImageMagick comparisons; ignore small anti-aliasing-style deltas.

summary
- Recomputed fuzz-tolerant differences after the `github_docx4j_gradient.png` alpha fix.
- Built `/tmp/wmf2svg-top-diff-sheet.png` for visual classification.
- Likely non-font candidates include `ex2.png`, `60677.png`, `nested_wmf.png`, `2264z_01_1.png`, `github_npoi_wrench.png`, `github_python_pptx_pic.png`, `B_6DB_CH01.png`, and possibly `fulltest.png`, `2doorvan.png`, `cell.png`, `github_reactos_enhmetafile_test.png`.
- Mostly font/text candidates include `text.png`, `texts.png`, `japanese1.png`, `japanese2.png`, `Symbols.png`, `Eg.png`, `er-diagram.png`, `input.png`, and much of `derouleur.png`.
