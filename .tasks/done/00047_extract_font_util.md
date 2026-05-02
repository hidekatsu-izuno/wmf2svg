# Extract font utility

## Purpose
`Main` に置いた `-fontdir` 用のフォント登録処理を `util.FontUtil` に移し、CLI の責務をオプション処理に戻す。

## Context
- 現在 `Main` が `registerFonts`, `isFontFile`, `fontType` を持っている。
- ユーザー要望は `registerFonts`, `isFontFile`, `fontType` を新設 `util.FontUtil` へ移動すること。
- 直近の `-fontdir` 実装は未コミットのため、この変更と合わせて整理する。

## Tasks
- [x] `src/main/java/net/arnx/wmf2svg/util/FontUtil.java` を新設し、フォント登録/判定/タイプ判定を移す。
- [x] `Main` からフォント登録実装と不要 import を削除し、`FontUtil.registerFonts` を呼ぶ。
- [x] テストを `FontUtil` 参照に更新する。
- [x] focused test と diff check を実行する。
- [x] 変更内容を追記し、完了へ移動する。

## Goals
- `Main` の CLI 挙動は変えない。
- フォント処理の実装は `FontUtil` に集約される。
- 既存 `-fontdir` テストが通る。

## File List
- `src/main/java/net/arnx/wmf2svg/Main.java`
- `src/main/java/net/arnx/wmf2svg/util/FontUtil.java`
- `src/test/java/net/arnx/wmf2svg/MainTest.java`

## Summary
- `net.arnx.wmf2svg.util.FontUtil` を新設し、`registerFonts`, `isFontFile`, `fontType` を移動した。
- `Main` は `-fontdir` 指定時に `FontUtil.registerFonts(fontDir)` を呼ぶだけにした。
- `MainTest` は `FontUtil` を直接検証するように更新した。
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.MainTest test`, `mvn -q test`, `git diff --check`。
