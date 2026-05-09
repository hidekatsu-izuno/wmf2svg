# Add Maven Tasks To mise.toml

## Purpose
Make common Maven commands available through mise task launcher commands:
`mise run build`, `mise run test`, and `mise run publish`.

## Context
- `mise.toml` currently defines Maven 3.9.15, Java 21, and ImageMagick.
- The Maven build uses normal lifecycle goals.
- README documents release publishing as `mvn -Prelease clean deploy`.

## Tasks
- [x] Add a `[tasks]` section to `mise.toml`.
- [x] Add `build = "mvn package"`.
- [x] Add `test = "mvn test"`.
- [x] Add `publish = "mvn -Prelease clean deploy"`.
- [x] Verify mise lists the new tasks.
- [x] Verify `mise run build` completes.
- [x] Verify `mise run test` completes.
- [x] Confirm `publish` command text without executing deployment.

## Goals
- `mise run build` packages the project with Maven.
- `mise run test` runs Maven tests.
- `mise run publish` invokes the existing release deployment command.
- Verification avoids running publish because it can deploy artifacts.

## File List
- `mise.toml`
- `.tasks/00228_add_mise_maven_tasks.md`

## Current Status
Complete.

## Next Step
No next step.

## Completion Notes
- Added `build`, `test`, and `publish` mise tasks.
- Verified `mise tasks` lists `build`, `publish`, and `test`.
- Verified `mise run build` completes successfully with Maven package and 430 passing tests.
- Verified `mise run test` completes successfully with 430 passing tests.
- Confirmed `publish` is configured as `mvn -Prelease clean deploy` without executing it.
