# AwtGdi text justification state

## Purpose
AwtGdi で setTextJustification の状態保持を観測可能にし、SaveDC/RestoreDC で保持されることを検証する。

## Context
- AwtDc は text justification extra/count を保持し、AwtGdi のテキスト描画計算でも参照している。
- AwtGdi には layout/mapper flags/relabs などの bookkeeping getter があるが、text justification には対応する検証口がない。
- レンダリング変更ではなく、既存状態の公開範囲を package-private テスト用にそろえる。

## Tasks
- [x] AwtGdi に text justification extra/count の package-private getter を追加する。
- [x] AwtGdiTest に setTextJustification が SaveDC/RestoreDC に従うことを検証するテストを追加する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- text justification の状態保持が AwtGdiTest で直接検証できる。
- 既存レンダリング挙動を変えない。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q test`, `git diff --check` が成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## Summary
- AwtGdi に package-private の `getTextJustificationExtra()` と `getTextJustificationCount()` を追加し、AwtDc が保持する状態をテストから直接確認できるようにした。
- setTextJustification の値が SaveDC/RestoreDC で復元されることを AwtGdiTest に追加した。
- レンダリング計算そのものは変更せず、既存の DC 状態保持を検証可能にする範囲に留めた。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
