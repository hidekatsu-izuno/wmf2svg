# AWT 2doorvan Extra Line

## Purpose
`2doorvan.png` の生成画像に出ている余計な線を特定し、AwtGdi の描画実装を修正する。

## Context
- 正解画像: `../wmf-testcase/data/png/2doorvan.png`
- 生成画像: `etc/data/dst/2doorvan.png`
- Windows fontdir ありの比較で `2doorvan.png` は差分上位に残っている。
- ユーザー指摘では余計な線が出ているため、フォント差ではなく描画実装の不具合として扱う。

## Tasks
- [x] 正解/生成/diff の triplet を確認し、余計な線の位置と色を特定する。
- [x] `2doorvan.wmf` の該当描画命令をログまたは段階レンダリングで特定する。
- [x] AwtGdi の実装原因を修正する。
- [x] `2doorvan.png` を再生成して余計な線が消えたことを確認する。
- [x] focused test / full test / diff check を実行する。
- [x] 変更内容と残課題を追記し、完了へ移動する。

## Goals
- `2doorvan.png` の余計な線が解消される。
- 同種の描画命令に対する回帰テストが追加される。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `target/png-compare-fontdir/` (比較生成物)

## Summary
- `2doorvan.png` の余計な線は、車体上部の前席ドア付近から後端上部へ伸びる斜線だった。
- 原因は AwtGdi の `polyline` が Java `Polygon` に変換して stroke していたこと。`Polygon` は閉じた Shape なので、WMF `POLYLINE` まで終点から始点へ閉じる線を描いていた。
- `polyline` を open な `Path2D` で stroke するよう修正した。
- `2doorvan.png` の AE は `7333` から `5646` に改善し、余計な斜線は消えた。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
