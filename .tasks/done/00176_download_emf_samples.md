# 00176 Download benign EMF samples

## Purpose
Download about 10 EMF sample files from different GitHub repositories into `../wmf-testcase/data/src/` for test coverage, while excluding repositories that appear to be security exploit, vulnerability research, fuzzing, or malware-oriented.

## Context
- User wants EMF samples from different GitHub repositories.
- EMF can be security-sensitive, so samples must be selected from ordinary document, graphics, plotting, or fixture repositories rather than security-test repositories.
- Downloaded files should be inspected without rendering or executing them.
- Target directory is outside this repo: `../wmf-testcase/data/src/`.

## Tasks
- [x] Find candidate EMF files from distinct GitHub repositories.
- [x] Exclude security, exploit, fuzzing, malware, CVE, or vulnerability-focused repositories and paths.
- [x] Download selected files into `../wmf-testcase/data/src/` with collision-safe names.
- [x] Statically inspect downloaded files for EMF headers, suspicious strings, file size outliers, and high-level record structure.
- [x] Record source URLs, local paths, and inspection results.

## Goals
- Around 10 EMF files from distinct non-security GitHub repositories.
- Files placed under `../wmf-testcase/data/src/`.
- No downloaded file shows obvious exploit/security-test indicators in static inspection.

## File List
- `.tasks/00176_download_emf_samples.md`
- `../wmf-testcase/data/src/*.emf`
- `/tmp/wmf2png-00176/`

## Status
- Current status: complete.
- Next step: none.
- Required context to resume: do not render downloaded EMFs during safety inspection; use static checks first.

## Summary
- Initial GitHub code search returned many Eclipse Modeling Framework text files with `.emf` extension; these were rejected because they were not Windows Enhanced Metafile images.
- Selected 10 non-security-oriented repositories. Apache POI contained fuzzer/crash-named EMFs; those paths were explicitly avoided.
- Downloaded files to `/tmp/wmf2png-00176/selected/`, then copied inspected files to `../wmf-testcase/data/src/` with `github_*.emf` names.
- Static checks only; no GDI/GDI+ rendering was used for safety inspection.
- All selected files had EMF header type `0x00000001`, signature `0x464d4520` at offset 40, `nBytes` matching file size, and a parseable record stream ending at `EMR_EOF`.
- Suspicious string scan found no hits for: `cve`, `exploit`, `shell`, `powershell`, `cmd.exe`, `calc.exe`, `http://`, `https://`, `javascript`, `script`, `meterpreter`, `payload`, `download`, `urlmon`, `winexec`, `createprocess`.

## Downloaded Files
- `../wmf-testcase/data/src/github_apache_poi_vector_image.emf`
  - Source: `https://github.com/apache/poi/blob/trunk/test-data/document/vector_image.emf`
  - Size: 7,348 bytes; records parsed: 279; EOF: yes.
- `../wmf-testcase/data/src/github_closedxml_sample_image.emf`
  - Source: `https://github.com/ClosedXML/ClosedXML/blob/develop/ClosedXML.Tests/Resource/Images/SampleImageEmf.emf`
  - Size: 4,056 bytes; records parsed: 103; EOF: yes.
- `../wmf-testcase/data/src/github_docx4j_gradient.emf`
  - Source: `https://github.com/plutext/docx4j/blob/master/docx4j-samples-docx4j/sample-docs/metafile-samples/gradient.emf`
  - Size: 67,648 bytes; records parsed: 152; EOF: yes.
- `../wmf-testcase/data/src/github_epplus_code.emf`
  - Source: `https://github.com/EPPlusSoftware/EPPlus/blob/develop/src/EPPlusTest/Resources/Code.emf`
  - Size: 62,056 bytes; records parsed: 23; EOF: yes. Header `nRecords` was 22, but the record stream was otherwise structurally valid and reached EOF.
- `../wmf-testcase/data/src/github_gooseberry_excel_streaming_sample_image.emf`
  - Source: `https://github.com/gooseberrysoft/excel-streaming/blob/master/tests/Gooseberry.ExcelStreaming.Tests/Resources/Images/SampleImageEmf.emf`
  - Size: 4,056 bytes; records parsed: 103; EOF: yes.
- `../wmf-testcase/data/src/github_libreoffice_test_draw_line.emf`
  - Source: `https://github.com/LibreOffice/core/blob/master/emfio/qa/cppunit/emf/data/TestDrawLine.emf`
  - Size: 2,032 bytes; records parsed: 21; EOF: yes.
- `../wmf-testcase/data/src/github_npoi_wrench.emf`
  - Source: `https://github.com/nissl-lab/npoi/blob/master/testcases/test-data/slideshow/wrench.emf`
  - Size: 6,184 bytes; records parsed: 153; EOF: yes.
- `../wmf-testcase/data/src/github_python_pptx_pic.emf`
  - Source: `https://github.com/scanny/python-pptx/blob/master/features/steps/test_files/pic.emf`
  - Size: 49,704 bytes; records parsed: 1,214; EOF: yes.
- `../wmf-testcase/data/src/github_reactos_enhmetafile_test.emf`
  - Source: `https://github.com/reactos/reactos/blob/master/modules/rostests/tests/enhmetafile/test.emf`
  - Size: 120,908 bytes; records parsed: 15; EOF: yes.
- `../wmf-testcase/data/src/github_xceed_docx_signature_line.emf`
  - Source: `https://github.com/xceedsoftware/DocX/blob/master/Xceed.Document.NET/Resources/SignatureLine.emf`
  - Size: 772 bytes; records parsed: 3; EOF: yes.
