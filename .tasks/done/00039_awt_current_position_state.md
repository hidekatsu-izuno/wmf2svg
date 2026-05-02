# AwtGdi current position state

## Purpose
AwtGdi の current position を package-private に観測可能にし、moveToEx と SaveDC/RestoreDC の状態保持を検証する。

## Context
- AwtDc は current position X/Y を保持しており、lineTo や TA_UPDATECP のテキスト描画で参照される。
- AwtGdi には current position の getter がなく、DC 状態としての保存復元を直接検証できない。
- レンダリングロジックは変更せず、既存状態をテスト可能にする。

## Tasks
- [x] AwtGdi に current position X/Y の package-private getter を追加する。
- [x] AwtGdiTest に moveToEx の old point と SaveDC/RestoreDC 復元を検証するテストを追加する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- current position の状態保持が AwtGdiTest で直接検証できる。
- 既存レンダリング挙動を変えない。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q test`, `git diff --check` が成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## Summary
- AwtGdi に package-private の `getCurrentX()` と `getCurrentY()` を追加し、AwtDc が保持する current position をテストから直接確認できるようにした。
- `moveToEx` が old point に旧値を返し、SaveDC/RestoreDC で current position が復元されることを AwtGdiTest に追加した。
- lineTo や TA_UPDATECP で使われる状態の検証範囲を広げたが、描画処理そのものは変更していない。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
