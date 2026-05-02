# AwtGdi alternative-font properties

## Purpose
AwtGdiの論理フォント代替候補をハードコードせず、`SvgGdi.properties` の `alternative-font.*` から取得する。

## Context
- 現在の未コミット変更では `AwtGdi.LOGICAL_FONT_FALLBACKS` に9件の候補をハードコードしている。
- ユーザー要望は、この候補を `src/main/resources/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties` の `alternative-font` 設定から取得すること。
- AwtGdiはJava2Dの実フォント解決なので、取得した候補のうち `serif` などのgeneric familyは引き続き実フォント候補として扱わない。

## Tasks
- [x] `.tasks/todo/00055_awt_load_alternative_font_properties.md` を `.tasks/00055_awt_load_alternative_font_properties.md` へ移動して作業開始状態にする。
- [x] `AwtGdi` のハードコードされた `LOGICAL_FONT_FALLBACKS` を削除し、`SvgGdi.properties` を静的ロードする。
- [x] `alternative-font.<font>` からカンマ区切り候補を取得する処理へ差し替える。
- [x] 既存のgeneric family除外とJava2D既定フォールバックの挙動を維持する。
- [x] `AwtGdiTest` をプロパティ由来の候補を検証する形に調整する。
- [x] 関連テストを実行し、結果を確認する。
- [x] 完了時に変更内容と判断を追記し、`.tasks/done/00055_awt_load_alternative_font_properties.md` へ移動する。

## Goals
- AwtGdiに個別の論理フォント代替マップを持たない。
- `SvgGdi.properties` の `alternative-font.Fixedsys = Consolas, monospace` などがAwtGdiの解決にも使われる。
- `serif` や `monospace` などは実フォント候補として選ばれない。
- `mvn -q test` が成功する。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/done/00054_awt_font_fallbacks.md`

## Resume State
- Current status: completed; task file moved to `.tasks/done/00055_awt_load_alternative_font_properties.md`.
- Next step: none.
- Required context: previous AwtGdi fallback implementation is uncommitted and currently uses a hardcoded map.

## Completion Summary
- Removed the hardcoded AwtGdi fallback map.
- Added static loading of `/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties` and lookup of `alternative-font.<font>` for AwtGdi fallback candidates.
- Kept case-insensitive requested font matching and `@` prefix normalization for vertical font names.
- Split comma-separated `alternative-font` values and retained the existing behavior of skipping generic family names as real Java2D font candidates.
- Adjusted `AwtGdiTest` to verify fallback values sourced from the properties file, including `Modern -> Courier New`.
- Updated `.tasks/done/00054_awt_font_fallbacks.md` with a follow-up note that task 00055 replaced the hardcoded map.
- Verification: `mvn -q test -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest`, `mvn -q spotless:apply`, `mvn -q test`, and `git diff --check` passed.

## Follow-up
- Task `00056_font_property_util` moved the properties loading and font-property lookup from AwtGdi into `FontUtil`.
