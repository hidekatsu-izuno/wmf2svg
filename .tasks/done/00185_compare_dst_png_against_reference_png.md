# Compare Generated PNGs Against Reference PNGs

## Purpose
Compare PNG outputs in `etc/data/dst` against reference PNGs in `../wmf-testcase/data/png`, ignoring expected anti-aliasing differences from different rendering engines.

## Context
- User requested comparison between generated PNGs and reference PNGs.
- Generated PNGs are under `etc/data/dst`.
- Reference PNGs are under `../wmf-testcase/data/png`.
- Anti-aliasing differences should be ignored; geometry/large rendering differences should be reported.

## Tasks
1. Status: completed
   Next step: Listed matching PNG pairs and checked dimensions.
   Required context: Identify missing files and dimension mismatches before pixel comparison.

2. Status: completed
   Next step: Ran tolerant image comparisons for matching dimensions with ImageMagick RMSE plus 5% and 10% fuzz AE metrics.
   Required context: Use a fuzz/RMSE style metric to avoid flagging normal anti-aliasing.

3. Status: completed
   Next step: Summarized likely matches, borderline cases, and clear mismatches.
   Required context: Include thresholds used and concrete filenames.

4. Status: completed
   Next step: Append completion notes and move this file to `.tasks/done/00185_compare_dst_png_against_reference_png.md`.
   Required context: Record comparison method and results.

## Goals
- Report whether `etc/data/dst` PNGs match `../wmf-testcase/data/png` aside from anti-aliasing.
- Provide a concise mismatch list with metrics.

## File List
- `.tasks/00185_compare_dst_png_against_reference_png.md`
- `etc/data/dst/*.png`
- `../wmf-testcase/data/png/*.png`

## Completion Notes
- Compared 72 generated PNGs in `etc/data/dst` against 72 reference PNGs in `../wmf-testcase/data/png`; no missing basenames.
- Method: dimension check first, then ImageMagick `compare -metric RMSE`; for anti-alias tolerance used `compare -metric AE -fuzz 5%` and `-fuzz 10%`.
- Classification used for the summary: dimension mismatch is always a mismatch; same-size files were flagged when `10% fuzz` difference ratio was at least `0.02` or RMSE was at least `0.12`.
- Full TSV reports are in `/tmp/wmf2svg-png-compare.tsv` and `/tmp/wmf2svg-png-compare-fuzz10.tsv`.
- Dimension mismatches: `github_apache_poi_vector_image.png` (`706x578` vs `708x581`), `github_docx4j_gradient.png` (`1096x783` vs `1095x783`), `github_python_pptx_pic.png` (`1632x1056` vs `1636x1060`), `github_xceed_docx_signature_line.png` (`259x131` vs `257x129`), `nested_wmf.png` (`3345x2444` vs `3261x2357`).
- Same-size but likely beyond anti-alias-only differences by the chosen tolerance: `2264z_01_1.png`, `2doorvan.png`, `60677.png`, `Eg.png`, `derouleur.png`, `er-diagram.png`, `ex2.png`, `ex6.png`, `fulltest.png`, `github_npoi_wrench.png`, `input.png`, `japanese1.png`, `sample_03.png`, `text.png`, `texts.png`.
- Likely acceptable/AA-only by the chosen tolerance: 52 files.
