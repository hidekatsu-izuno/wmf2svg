# AwtGdi selected object state

## Purpose
AwtGdi の選択中 brush/pen/font を package-private に観測可能にし、selectObject と SaveDC/RestoreDC の状態保持を検証する。

## Context
- AwtDc は選択中の AwtBrush/AwtPen/AwtFont を保持し、描画処理で参照している。
- SaveDC は AwtDc を clone するため選択オブジェクトも復元対象だが、AwtGdiTest から直接検証する getter がない。
- レンダリングロジックは変更せず、既存の選択状態をテスト可能にする。

## Tasks
- [x] AwtGdi に選択中 brush/pen/font の package-private getter を追加する。
- [x] AwtGdiTest に selectObject と SaveDC/RestoreDC 復元を検証するテストを追加する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- 選択中 brush/pen/font の状態保持を AwtGdiTest で直接検証できる。
- 既存レンダリング挙動を変えない。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q test`, `git diff --check` が成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## Summary
- AwtGdi に package-private の `getSelectedBrush()`、`getSelectedPen()`、`getSelectedFont()` を追加した。
- `selectObject` で選択された brush/pen/font が SaveDC/RestoreDC で復元されることを AwtGdiTest に追加した。
- 描画処理そのものは変更せず、AwtDc が既に保持している選択オブジェクト状態の検証範囲を広げた。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
