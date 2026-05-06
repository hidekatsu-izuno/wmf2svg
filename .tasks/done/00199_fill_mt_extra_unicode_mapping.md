purpose
- Fill the `MT Extra` worksheet in `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx` with glyph display values and Unicode mappings.

context
- The `MT Extra` sheet currently has code values from `0x21` through `0xFF`, but the `MT Extra`, `Code(Unicode)`, and `Noto Sans` columns are blank.
- The worksheet should match the existing workbook convention:
  - `Code(Hex)` contains the source font code value.
  - `MT Extra` contains the corresponding code character styled with the `MT Extra` font.
  - `Code(Unicode)` contains the mapped Unicode code point, where known.
  - `Noto Sans` contains the mapped Unicode character rendered with the Noto Sans style.
- The workbook previously needed repair because whole-part XML reserialization changed namespace prefixes, so updates must preserve existing XML text where possible.

tasks
- status: completed
  next step: Found Mozilla MathML `MT Extra Encoding` as the available published mapping. It notes multiple MT Extra versions; use only explicitly listed mappings and leave unknown codes blank.
  required context: Codes are `0x21` through `0xFF`; unknown mappings should be left blank rather than guessed.
- status: completed
  next step: Existing workbook uses shared strings for source glyph characters, CP1252-like source code characters, and separate source-font styles. The MT Extra styles are `30` and `31`.
  required context: Preserve existing package structure and namespace declarations.
- status: completed
  next step: Filled B cells for all `0x21`-`0xFF` codes and C/D cells for 41 Mozilla-listed MT Extra mappings.
  required context: B cells use the source code character with MT Extra style; C/D cells use Unicode string and Unicode glyph when mapped.
- status: completed
  next step: Validation passed: ZIP integrity OK, all XML parts parse, code rows match `0x21`-`0xFF`, B cells are filled for all 223 codes, 41 Unicode mappings match the Mozilla MT Extra table, and shared-string counts are consistent.
  required context: Verification must show populated B cells and mapped C/D cells where a mapping exists.
- status: completed
  next step: Completion summary appended and task file moved to `.tasks/done/`.
  required context: Include mapping source/derivation and validation results.

goals
- `MT Extra` worksheet has populated source glyph cells for `0x21` through `0xFF`.
- Known Unicode mappings are populated in `Code(Unicode)` and `Noto Sans`.
- Unknown mappings remain blank.
- Workbook remains openable and structurally valid.

file list
- `.tasks/00199_fill_mt_extra_unicode_mapping.md`
- `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`

completion summary
- Filled the `MT Extra` worksheet in `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`.
- B cells now contain the source code character for every code from `0x21` through `0xFF`, styled with the MT Extra font styles.
- C/D cells now contain 41 known Unicode mappings from Mozilla's `MT Extra Encoding` table.
- Left unknown mappings blank rather than guessing, because the source notes multiple incompatible MT Extra font versions.
- For table entries with multiple component/suffix alternatives, used the first listed Unicode code point as the display mapping.
- Verified ZIP integrity, XML parseability, exact code coverage, B-cell population count, C/D mapping values, and shared-string counts.
