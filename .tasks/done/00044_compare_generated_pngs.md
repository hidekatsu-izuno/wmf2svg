# Compare generated PNGs against expected images

## Purpose
正解画像 `../wmf-testcase/data/png/*.png` と生成画像 `etc/data/dst/*.png` を比較し、残っている AwtGdi/変換出力の不具合候補を特定する。

## Context
- 正解 PNG: `../wmf-testcase/data/png/*.png`
- 生成 PNG: `etc/data/dst/*.png`
- 直近で `12e9.png` と `er-diagram.png` は修正済みだが、`etc/data/dst` 側が再生成済みとは限らない。
- 既存ワークツリーは commit 済みで clean の想定。無関係な変更は戻さない。

## Tasks
- [x] 正解 PNG と生成 PNG の対応ファイル一覧を作り、欠落・余剰・寸法差を確認する。
- [x] 画素差分を機械的に計測し、差が大きいファイルを抽出する。
- [x] 代表的な差分画像を目視確認し、実害のある不具合候補を分類する。
- [x] 必要なら最新コードで対象 PNG を再生成し、既知修正済み差分を除外する。
- [x] 不具合候補・再現ファイル・次の修正対象をまとめる。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- 残っている不具合候補 PNG の一覧が得られる。
- 各候補について、寸法差・描画欠落・色/アンチエイリアス差などの分類が分かる。

## File List
- 調査中心。必要に応じて一時差分画像は `target/png-compare/` に生成する。

## Summary
- `../wmf-testcase/data/png/*.png` と `etc/data/dst/*.png` はどちらも 57 件で、欠落・余剰はなかった。
- 既存 `etc/data/dst` と正解の寸法差は `bitblt.png` のみだった。
- 最新コードで `../wmf-testcase/data/src/*.{wmf,WMF}` を `target/png-current/` に全件再生成し、同じ比較を実施した。
- 最新コード再生成後も 57 件中完全一致は 7 件、寸法差は `bitblt.png` の 1 件、正規化 AE が 10% 超の大きな差は 8 件だった。
- 目視確認した高優先度候補:
  - `bitblt.png`: 正解 700x600、生成 701x601。下部の ROP セルにも大きな色差がある。
  - `er-diagram.png`: アイコンは最新コードで復活しているが、埋め込み EMF 全体の位置/スケールが正解より左上に寄っている。
  - `Symbols.png`: Symbol glyph sheet の文字・間隔が大きく異なる。Symbol font replacement/charset mapping 周辺の候補。
  - `text.png`: 回転/斜体/配置の text rendering が大きく異なり、一部 clipping/位置ずれが見える。
  - `Eg.png`: `Pixie` の大文字が太く大きくなり、背景 gradient/raster も差が大きい。
  - `ex2.png`: 密な線画で広範囲に線・パス差分が出ている。
  - `ex6.png`: 棒の下の繰り返し text label が正解と異なる/欠ける。
  - `texts.png`: 大量の rotated/aligned text の配置差が大きい。
- 低優先度または環境差混在候補:
  - `2doorvan.png`, `TEACHER1.png`: 主に stroke/antialias 差。
  - `japanese1.png`: 日本語フォント metrics/spacing 差。
  - `derouleur.png`, `B_6DB_CH01.png`, `fulltest.png`: text/stroke/connector の複合差。

## Generated Artifacts
- `target/png-compare/metrics.txt`: 既存 `etc/data/dst` との AE 比較。
- `target/png-current/*.png`: 最新コードで再生成した PNG。
- `target/png-compare/current-metrics.txt`: 最新コード再生成 PNG との AE 比較。
- `target/png-compare/current-*-triplet.png`: 代表候補の expected/generated/diff 横並び。

## Verification
- `find` で正解/生成 PNG の 57 件対応を確認。
- ImageMagick `identify -ping`, `compare -metric AE`, `compare -metric RMSE` で寸法・画素差を計測。
- 代表候補の triplet 画像を目視確認。
