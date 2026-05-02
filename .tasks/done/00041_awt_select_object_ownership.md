# AwtGdi selectObject ownership

## Purpose
AwtGdi.selectObject の型分岐を整理し、Awt 所有オブジェクトだけを選択状態に反映する挙動を検証する。

## Context
- AwtPatternBrush は AwtBrush を継承しているため、selectObject の AwtPatternBrush 分岐は AwtBrush 分岐に先に捕捉されて到達不能。
- AwtGdi は AwtBrush/AwtPen/AwtFont/AwtRegion のような Awt 実装だけを選択状態に反映する設計になっている。
- 既存レンダリング挙動を変えず、分岐を読みやすくし、pattern brush と foreign GdiObject の扱いをテストで固定する。

## Tasks
- [x] AwtGdi.selectObject から到達不能な AwtPatternBrush 分岐を削除する。
- [x] AwtGdiTest に pattern brush が brush として選択されることを検証するテストを追加する。
- [x] AwtGdiTest に foreign brush/pen/font が既存選択状態を変えないことを検証するテストを追加する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- selectObject の分岐が継承関係に沿って読みやすくなる。
- Awt 実装以外の GdiObject で選択状態が壊れないことを確認できる。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q test`, `git diff --check` が成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## Summary
- `AwtPatternBrush` は `AwtBrush` を継承しているため、AwtGdi.selectObject の到達不能な `AwtPatternBrush` 分岐を削除した。
- Pattern brush が通常の brush として選択されることを AwtGdiTest に追加した。
- Awt 実装ではない foreign brush/pen/font を selectObject に渡しても、既存の選択状態が変わらないことを AwtGdiTest に追加した。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
