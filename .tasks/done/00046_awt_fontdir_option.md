# AWT fontdir option

## Purpose
CLI で PNG/JPEG 出力時に AWT が使う追加フォントディレクトリを指定できるようにする。

## Context
- 現在の CLI オプションは `-debug`, `-compatible`, `-replace-symbol-font` のみ。
- AWT は `new Font(faceName, ...)` で環境に登録済みのフォントを解決するため、未登録フォントは代替される。
- Java では `GraphicsEnvironment.registerFont(Font.createFont(...))` で実行時に `.ttf` / `.otf` などを追加登録できる。

## Tasks
- [x] `Main` に `-fontdir <dir>` のオプション解析を追加し、指定値を保持する。
- [x] 指定ディレクトリ配下のフォントファイルを登録する処理を追加する。
- [x] SVG 出力では不要だが、CLI としては変換前に登録して PNG/JPEG の AWT font lookup に効かせる。
- [x] README のオプション説明を更新する。
- [x] focused test と必要な検証を実行する。
- [x] 変更内容を追記し、完了へ移動する。

## Goals
- `java -jar ... -fontdir /path/to/fonts input.wmf output.png` が利用できる。
- 不正な `-fontdir` 指定は usage または明確な例外で失敗する。
- 既存 CLI 呼び出しは維持される。

## File List
- `src/main/java/net/arnx/wmf2svg/Main.java`
- `src/test/java/net/arnx/wmf2svg/MainTest.java`
- `README.md`

## Summary
- CLI に `-fontdir <dir>` を追加した。PNG/JPEG 出力時、変換前に指定ディレクトリ内の `.ttf`, `.ttc`, `.otf`, `.pfa`, `.pfb` を `GraphicsEnvironment.registerFont` で登録する。
- 存在しないディレクトリは `FileNotFoundException` で失敗し、不正なフォントファイルは warning を出してスキップする。
- README にオプションと使用例を追記した。
- `MainTest` にフォント拡張子判定、非フォントファイル無視、存在しないディレクトリ拒否のテストを追加した。
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.MainTest test`, `mvn -q test`, `git diff --check`。
