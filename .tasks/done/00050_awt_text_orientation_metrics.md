# AWT Text Orientation Metrics

## Purpose
`text.png` の残差分を減らすため、AwtGdi の GDI text 描画で `lfOrientation` と文字幅/背景矩形計算の互換性を改善する。

## Context
- `text.png` は `-fontdir /mnt/c/Windows/Fonts/`、`-replace-symbol-font` なしでも AE が大きい。
- 既に負の WMF 境界と回転中心の大きなズレは修正済み。
- 残る実装候補は、`LOGFONT.lfOrientation` の未反映と、`TextOut`/`ExtTextOut` の文字幅・背景矩形が Java2D `FontMetrics` に寄りすぎている点。
- まず AwtGdi の既存 text パスに沿って、小さく検証可能な改善を入れる。

## Tasks
- [x] AwtGdi の text 描画で `lfOrientation` と `lfEscapement` の使い分けを確認する。
- [x] `lfOrientation` を glyph orientation として反映し、`lfEscapement` は baseline/vector 方向として扱う。
- [x] `TextAdvances` と背景矩形の幅計算を整理し、`lpdx` と `SetTextCharacterExtra` の扱いが描画と背景で一致するようにする。
- [x] `text.png` を Windows fontdir あり・symbol replacement なしで再生成し、AE と triplet を確認する。
- [x] focused test / full test / diff check を実行する。
- [x] 変更内容と残課題を追記し、完了へ移動する。

## Goals
- `text.png` の回転/背景/文字送り差分が改善する。
- 既存 text テストを壊さず、`lfOrientation` と文字幅処理の回帰テストが追加される。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `target/png-compare-fontdir/` (比較生成物)

## Summary
- `lfOrientation` を字形回転として反映する案を検証したが、`text.wmf` の `escapement=0, orientation=1700` の文字列は正解画像では横書きのままだったため、AwtGdi の WMF text 描画角には `lfEscapement` のみを使う方針に戻した。
- text bounds は描画フォントの `getStringBounds` も union し、`lpdx`/`SetTextCharacterExtra` を背景矩形・ROP2 範囲にも反映するようにした。
- 位置ズレの主因は、回転 transform を入れた後に `ensureTextCanvasContains` がキャンバスリサイズを起こすと、`Graphics2D` が作り直されて text 回転が落ちることだった。測定/拡張を先に実行し、その後に escapement 回転を入れて描画する順序に修正した。
- `text.png` は Windows fontdir あり・`-replace-symbol-font` なし相当で AE が `32013` から `28741` に改善した。赤い回転テキストも上端側に描画されるようになった。
- 残差分は、旧フォント名/bitmap font 代替、Java2D と GDI のフォントメトリクス・アンチエイリアス差が中心。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
