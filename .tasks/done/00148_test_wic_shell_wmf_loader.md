# Test WIC Shell WMF Loader

## Purpose
Evaluate whether Windows Imaging Component or Shell thumbnail APIs can load WMF files in a way that exactly matches mspaint PNG output.

## Context
- Exact mspaint compatibility is required; the current GDI+/PlayEnhMetaFile approximations still leave differences.
- Proposed option 2 is to try Windows Imaging Component / Shell thumbnail loading paths.
- Primary target: `../wmf-testcase/data/src/p0000016.wmf`
- Baseline: `../wmf-testcase/data/png/p0000016.png`

## Tasks
- [x] Test WIC direct decode.
  - Status: completed.
  - Next step: none.
  - Required context: failure is useful evidence.
- [x] Test Shell thumbnail decode.
  - Status: completed.
  - Next step: none.
  - Required context: compare output to mspaint baseline.
- [x] Test related Shell/WIC options if available.
  - Status: completed.
  - Next step: none.
  - Required context: keep generated files under `/tmp`.
- [x] Summarize results and feasibility.
  - Status: completed.
  - Next step: none.

## Goals
- Evidence-backed answer for option 2.
- No production code changes unless the path proves promising.

## File List
- `.tasks/00148_test_wic_shell_wmf_loader.md`

## Summary
- Tested WIC direct decode through WPF `BitmapDecoder.Create`.
  - Result: failed because Windows did not expose an imaging component/decoder for this WMF input.
- Tested Shell thumbnail path through `IShellItemImageFactory.GetImage`.
  - `RESIZETOFIT` and `BIGGERIZEOK|SCALEUP` succeeded, but returned `8192x8192` square images even when `8192x608` was requested.
  - `THUMBNAILONLY` variants failed with HRESULT `0x8004B200`.
  - Successful Shell images were fully opaque (`alpha=255` for all pixels), unlike the mspaint baseline transparent PNG.
- Compared derived Shell outputs against `p0000016.png` baseline:
  - Cropping top `8192x608` from Shell square output: `AE=375321`, `A=4612330`, `RGB=375321`, `white=4617200`, `black=375321`.
  - Forcing Shell square output to resize to `8192x608`: `AE=2784520`, `A=4612330`, `RGB=2784520`, `white=4864010`, `black=2784520`.
- Conclusion: WIC/Shell thumbnail route is not a viable exact mspaint-compatible loader for this case. WIC does not directly decode the WMF, and Shell returns an opaque square preview rather than Paint's transparent canvas image.
