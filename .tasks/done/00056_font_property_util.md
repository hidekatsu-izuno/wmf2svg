# Centralize font property lookup

## Purpose
`alternative-font`, `font-emheight`, `font-charset` などのフォント関連プロパティ取得を `FontUtil` にまとめ、SvgFontとAwtGdiで同じ取得処理を使う。

## Context
- AwtGdiは未コミット変更で `/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties` を直接ロードし、`alternative-font.*` を検索している。
- SvgFontは `SvgGdi.getProperty()` 経由で `font-charset.*`, `font-emheight.*`, `alternative-font.*` を取得している。
- `FontUtil` は既にフォント登録/ファイル判定の共通ユーティリティとして存在する。
- ユーザー要望は、font関連プロパティ取得処理を `FontUtil` に集約すること。

## Tasks
- [x] `.tasks/todo/00056_font_property_util.md` を `.tasks/00056_font_property_util.md` へ移動して作業開始状態にする。
- [x] `FontUtil` に `SvgGdi.properties` の静的ロードと font関連プロパティ取得メソッドを追加する。
- [x] `SvgFont` を `FontUtil` の `fontCharset`, `fontEmHeight`, `alternativeFonts` 相当のメソッド利用へ変更する。
- [x] `AwtGdi` からプロパティロード処理を削除し、`FontUtil.alternativeFonts` を使う。
- [x] 既存テストを必要に応じて調整し、`FontUtil` のプロパティ取得を直接検証するテストを追加する。
- [x] 関連テストと全体テストを実行する。
- [x] 完了時に変更内容と判断を追記し、`.tasks/done/00056_font_property_util.md` へ移動する。

## Goals
- `SvgGdi.properties` のロード処理が `FontUtil` に集約される。
- `alternative-font.*` のカンマ区切り分解と空白除去が共通化される。
- `font-charset.*` と `font-emheight.*` 取得も `FontUtil` 経由になる。
- SvgFont/AwtGdiの既存挙動とテストが維持される。

## File List
- `src/main/java/net/arnx/wmf2svg/util/FontUtil.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgFont.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/MainTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/done/00055_awt_load_alternative_font_properties.md`

## Resume State
- Current status: completed; task file moved to `.tasks/done/00056_font_property_util.md`.
- Next step: none.
- Required context: AwtGdi fallback changes are still uncommitted and should be updated in place rather than reverted.

## Completion Summary
- Added `FontUtil` loading for `/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties`.
- Added `FontUtil.fontCharset`, `FontUtil.fontEmHeight`, and `FontUtil.alternativeFonts` for font-related property lookup.
- Centralized case-insensitive property-key matching and comma-separated alternative-font splitting in `FontUtil`.
- Updated `SvgFont` to use `FontUtil` for charset overrides, emheight values, and fallback font families.
- Updated `AwtGdi` to use `FontUtil.alternativeFonts` and removed its direct `SvgGdi.properties` loading.
- Added `MainTest.testFontProperties` for direct `FontUtil` property lookup coverage.
- Updated `.tasks/done/00055_awt_load_alternative_font_properties.md` with a follow-up note.
- Verification: `mvn -q test -Dtest=net.arnx.wmf2svg.MainTest,net.arnx.wmf2svg.gdi.awt.AwtGdiTest,net.arnx.wmf2svg.gdi.svg.SvgGdiTest`, `mvn -q spotless:apply`, `mvn -q test`, and `git diff --check` passed.
