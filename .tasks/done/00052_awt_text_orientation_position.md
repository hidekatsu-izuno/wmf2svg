# AWT Text Orientation Position

## Purpose
`text.png` 右側中央の 2 つの文字列の表示位置がおかしい問題を調査し、AwtGdi の text orientation/escapement 処理を修正する。

## Context
- 対象は `../wmf-testcase/data/src/text.wmf` から生成される `etc/data/dst/text.png`。
- 直近で `lfOrientation` と `lfEscapement` の分離対応を入れた。
- ユーザー指摘では、右側中央の 2 つの文字列の表示位置が不自然。
- 該当レコードは `escapement=0, orientation=1700` の Times New Roman 系 text と推定される。

## Tasks
- [x] 現在の `text.png` triplet を確認し、該当 2 文字列のズレを特定する。
- [x] `escapement=0, orientation=1700` の GDI 互換挙動を AwtGdi の描画順序・座標変換に照らして整理する。
- [x] 位置ズレの原因を修正する。
- [x] `text.png` を再生成し、該当 2 文字列の位置改善と AE を確認する。
- [x] focused test / full test / diff check を実行する。
- [x] 変更内容と残課題を追記し、完了へ移動する。

## Goals
- `text.png` 右側中央の 2 文字列が正解に近い位置へ戻る。
- `lfOrientation` 対応で回帰しないようテストを残す。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `target/png-compare-fontdir/` (比較生成物)

## Summary
- 右側中央の 2 つの文字列は `escapement=0, orientation=1700` の text レコードだった。
- `lfOrientation` を字形回転として反映すると、この 2 文字列が回転して誤った位置に出ることを確認した。
- `text.wmf` の正解画像ではこの 2 文字列は横書きのままなので、AwtGdi の WMF text 描画角は `lfEscapement` のみを使うよう修正した。
- `text.png` は Windows fontdir あり・`-replace-symbol-font` なし相当で AE が `28741` から `28719` に改善し、右側中央の 2 文字列は横書き位置に戻った。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `git diff --check`
- `mvn -q test` は実行中にユーザー指示で停止した。
