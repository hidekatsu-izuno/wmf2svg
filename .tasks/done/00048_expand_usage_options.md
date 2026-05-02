# Expand usage options

## Purpose
CLI の `usage()` に全オプションの説明を表示する。

## Context
- `Main.usage()` は現在 `-fontdir` のみ説明している。
- README には `-compatible`, `-replace-symbol-font`, `-fontdir <dir>` が記載されている。
- 直近の `-fontdir` / `FontUtil` 変更は未コミットで、この変更も同じ CLI 整理として扱う。

## Tasks
- [x] `Main.usage()` に `-debug`, `-compatible`, `-replace-symbol-font`, `-fontdir <dir>` の説明を追加する。
- [x] 必要な focused test / diff check を実行する。
- [x] 変更内容を追記し、完了へ移動する。

## Goals
- 未知オプションや引数不足時に、利用可能な全オプションが分かる。

## File List
- `src/main/java/net/arnx/wmf2svg/Main.java`

## Summary
- `Main.usage()` に `-debug`, `-compatible`, `-replace-symbol-font`, `-fontdir <dir>` の説明を追加した。
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.MainTest test`, `git diff --check`。
