# SVG text fullwidth background estimate

## Purpose
SvgGdiのフォント非依存な文字背景推定で、日本語などの全角・CJK文字を半角文字より広い `1em` として扱う。

## Context
- 直前の未コミット変更 `00057_svg_text_background_estimate` で、背景幅は `0.5em * text.length()` として推定している。
- この推定はASCII中心には妥当だが、日本語・CJK・全角記号では背景幅が不足しやすい。
- SvgGdiは実フォントに依存しない方針なので、Java2D計測ではなくUnicode文字種で幅を推定する。

## Tasks
- [x] `.tasks/todo/00058_svg_text_fullwidth_background.md` を `.tasks/00058_svg_text_fullwidth_background.md` へ移動して作業開始状態にする。
- [x] `SvgGdi.estimateTextWidth` を文字ごとのem幅合計に変更する。
- [x] CJK、ひらがな、カタカナ、ハングル、全角フォーム等を `1em` と判定するヘルパを追加する。
- [x] ASCIIなど通常文字は従来通り `0.5em` として扱う。
- [x] `SvgGdiTest` に日本語文字列の背景幅テストを追加する。
- [x] 関連テストと全体テストを実行する。
- [x] 完了時に変更内容と判断を追記し、`.tasks/done/00058_svg_text_fullwidth_background.md` へ移動する。

## Goals
- `"ABCD"` の背景幅は従来通り `2em`。
- `"日本"` の背景幅は `2em`。
- `"A日"` の背景幅は `1.5em`。
- 実フォント計測には依存しない。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `.tasks/done/00057_svg_text_background_estimate.md`

## Resume State
- Current status: completed; task file moved to `.tasks/done/00058_svg_text_fullwidth_background.md`.
- Next step: none.
- Required context: previous text-background estimate changes are uncommitted and should be updated in place.

## Completion Summary
- Changed `SvgGdi.estimateTextWidth` to sum estimated em widths per Unicode code point.
- Added `isFullWidthTextCodePoint` to treat CJK ideographs, Japanese kana, Hangul, CJK punctuation, enclosed CJK, and halfwidth/fullwidth forms as `1em`.
- Kept non-fullwidth text at `0.5em`, preserving the existing ASCII estimate.
- Added tests for `"日本"` estimating to `2em` and `"A日"` estimating to `1.5em`.
- Updated `.tasks/done/00057_svg_text_background_estimate.md` with a follow-up note.
- Verification: `mvn -q test -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest`, `mvn -q spotless:apply`, `mvn -q test`, and `git diff --check` passed.
