# Agent Rules

## Exploration
- Understand code, dependencies, and constraints before asking questions

## Clarification
- Do not confirm when creating, updating, and renaming files under `.tasks/`.

## Planning
- Plan before execution (by plan mode); no work without a plan, and the plan is the task file whose "tasks" are its executable steps
- Ask questions until requirements are unambiguous
- Do not assume or plan with unresolved questions
- Do not create a task file for trivial operational requests such as committing, showing status, listing files, or running a specified command
- Save to `.tasks/<5digits seq>_<title>.md`
- Plan must include:
  - purpose
  - context
  - tasks (each independently executable)
  - goals (outputs, verification, or observable results)
  - file list (if applicable)
- Tasks must be resumable:
  - include current status, next step, and required context to continue
- Define structure, interfaces, and data before implementation
- Validate before execution:
  - tasks are complete and verifiable

## Execution
- Follow the plan strictly
- Ensure good design:
  - maintain separation of concerns and state from logic
  - prioritize readability and maintainability
  - ensure overall design consistency before introducing ad-hoc or hardcoded solutions
  - when design decisions are unclear, present up to three options with trade-offs before proceeding
  - callers should read as a high-level summary of the called logic
  - do not extract small, low-reuse logic into separate functions unless it improves readability
- For multi-file changes:
  - list target files
  - process one by one
  - verify each before proceeding
- Use sub-agents for verification, testing, and review to isolate context

## Re-Planning
- If the plan becomes invalid:
  - stop
  - update the plan
  - resume only after re-planning

## Completion
- Verify correctness, edge cases, and regressions (via sub-agent)
- Do not complete incomplete or placeholder implementations
- If not verifiable, mark incomplete
- On completion:
  - append a summary of changes and decisions to the task file
  - move to `.tasks/done/<5digits seq>_<title>.md`
