# Download Additional Apache POI WMF/EMF Samples

## Purpose
Find additional Apache POI WMF/EMF test files that are useful and safe as rendering samples, then download non-duplicate valid files into `../wmf-testcase/data/src/`.

## Context
- User requested more Apache POI test WMF/EMF files if available.
- Destination is `../wmf-testcase/data/src/`, outside the current repository writable root.
- Exclude broken files, suspicious files, and files with security-related intent.
- Existing samples in `../wmf-testcase/data/src/` must not be overwritten accidentally.

## Tasks
1. Status: completed
   Next step: Searched Apache POI `test-data/slideshow` via GitHub contents API for WMF/EMF resources.
   Required context: Prefer Apache POI official repository/source URLs; identify file paths and purpose.

2. Status: completed
   Next step: Compared candidates against existing `../wmf-testcase/data/src/` files by basename and SHA-256.
   Required context: Avoid duplicate basenames and duplicate content where detectable.

3. Status: completed
   Next step: Filtered out security-test/crash/fuzzer files, duplicate `wrench.emf`, invalid-header `VHZ2NYFUYUUJNGLABL26ORTQZA76FJEW.emf`, and blank-rendering `61338.wmf`.
   Required context: Check magic/header signatures and Apache POI context around filenames/tests.

4. Status: completed
   Next step: Downloaded approved candidates to `/tmp/poi-wmf-emf`, validated headers/rendering, then copied into `../wmf-testcase/data/src/`.
   Required context: Writing to destination may require approval because it is outside the repo root.

5. Status: completed
   Next step: Verified downloaded files are readable and listed final additions.
   Required context: Run lightweight file/magic checks and avoid rendering all files unless needed.

6. Status: completed
   Next step: Append completion notes and move this file to `.tasks/done/00184_download_more_apache_poi_wmf_emf_samples.md`.
   Required context: Include sources, skipped candidates, and verification summary.

## Goals
- Add safe, valid Apache POI WMF/EMF test samples to `../wmf-testcase/data/src/`.
- Do not add known-broken, malicious/security-test, or duplicate files.
- Provide a concise list of downloaded files and sources.

## File List
- `.tasks/00184_download_more_apache_poi_wmf_emf_samples.md`
- `../wmf-testcase/data/src/`

## Completion Notes
- Source scanned: Apache POI `test-data/slideshow` via `https://api.github.com/repos/apache/poi/contents/test-data/slideshow?ref=trunk`.
- Excluded security/crash/minimized fuzz samples: `clusterfuzz-testcase-minimized-*.wmf`, `clusterfuzz-testcase-minimized-*.emf`, and `crash-7b60e9fe792eaaf1bba8be90c2b62f057cfff142.emf`.
- Excluded duplicate: `wrench.emf`, already present as `github_npoi_wrench.emf` with the same SHA-256.
- Excluded invalid/unsuitable candidates: `VHZ2NYFUYUUJNGLABL26ORTQZA76FJEW.emf` did not start with a standalone EMF header; `61338.wmf` rendered as a blank transparent PNG in this project.
- Added 8 files to `../wmf-testcase/data/src/`: `60677.wmf`, `64716_image1.wmf`, `64716_image2.wmf`, `64716_image3.wmf`, `empty-polygon-close.wmf`, `file-45.wmf`, `nested_wmf.emf`, `santa.wmf`.
- Verification: `file` recognizes the 7 WMF files and 1 EMF file; SHA-256 hashes were recorded during verification; all 8 added files rendered successfully to PNG under `/tmp/poi-wmf-emf-rendered`.
