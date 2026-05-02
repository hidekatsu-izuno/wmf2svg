# AwtGdi core drawing state bookkeeping

## Purpose
AwtGdi の基本描画状態を package-private に観測可能にし、SaveDC/RestoreDC で復元されることを検証する。

## Context
- AwtDc は背景、テキスト、塗り、ROP、stretch、arc、miter limit の状態を保持し、AwtGdi の描画処理でも参照している。
- AwtGdi には一部 DC 状態の getter があるが、基本描画状態の多くはテストから直接検証できない。
- レンダリングロジックを変えず、既存の DC clone/save/restore がこれらの状態を保持することを確認する。

## Tasks
- [x] AwtGdi に基本描画状態の package-private getter を追加する。
- [x] AwtGdiTest に基本描画状態が SaveDC/RestoreDC に従うことを検証するテストを追加する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- 背景、テキスト、poly fill、ROP2、stretch、arc、miter limit の状態保持を AwtGdiTest で直接検証できる。
- 既存レンダリング挙動を変えない。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q test`, `git diff --check` が成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## Summary
- AwtGdi に背景、テキスト、poly fill、ROP2、stretch、arc direction、miter limit の package-private getter を追加した。
- これらの基本描画状態が SaveDC/RestoreDC で復元されることを AwtGdiTest に追加した。
- 描画処理そのものは変更せず、AwtDc が既に保持している状態を検証可能にする範囲に留めた。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
