# AwtGdi brush origin state

## Purpose
AwtGdi で setBrushOrgEx の状態保持を観測可能にし、SaveDC/RestoreDC で復元されることを検証する。

## Context
- AwtDc は brush origin X/Y を保持しており、AwtGdi の pattern brush TexturePaint アンカーでも参照される。
- AwtGdi には一部 DC 状態の package-private getter があるが、brush origin は未公開で直接検証できない。
- レンダリングロジックは変更せず、既存状態の保存復元をテスト可能にする。

## Tasks
- [x] AwtGdi に brush origin X/Y の package-private getter を追加する。
- [x] AwtGdiTest に setBrushOrgEx の old point と SaveDC/RestoreDC 復元を検証するテストを追加する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- brush origin の状態保持が AwtGdiTest で直接検証できる。
- 既存レンダリング挙動を変えない。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q test`, `git diff --check` が成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## Summary
- AwtGdi に package-private の `getBrushOrgX()` と `getBrushOrgY()` を追加し、AwtDc が保持する brush origin をテストから直接確認できるようにした。
- `setBrushOrgEx` が old point に旧値を返し、SaveDC/RestoreDC で brush origin が復元されることを AwtGdiTest に追加した。
- Pattern brush の描画ロジック自体は変更せず、既存状態の検証範囲を広げる実装に留めた。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`
