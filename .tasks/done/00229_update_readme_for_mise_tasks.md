# Update README For mise Tasks

## Purpose
Document project build, test, and publish workflows through mise task launcher commands.

## Context
- `mise.toml` defines `build`, `test`, and `publish` tasks.
- README still documents direct Maven commands.
- `publish` deploys artifacts and should be documented but not executed during verification.

## Tasks
- [x] Review README build and release command references.
- [x] Update README build instructions to use `mise run build`.
- [x] Add or clarify test and publish commands using `mise run test` and `mise run publish`.
- [x] Update hidden release checklist comments to use mise task commands.
- [x] Verify README contains the expected mise commands and no stale direct Maven release command.

## Goals
- README assumes mise for routine project commands.
- Users can discover build, test, and publish commands from README.
- Direct Maven implementation details stay behind mise task names where practical.

## File List
- `README.md`
- `.tasks/00229_update_readme_for_mise_tasks.md`

## Current Status
Complete.

## Next Step
No next step.

## Completion Notes
- Updated README Build section to document `mise run build`, `mise run test`, and `mise run publish`.
- Updated the hidden release checklist to use mise task commands.
- Verified README command references with `rg -n "mvn|mise run|mise" README.md`.
