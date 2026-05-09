# Move MainTest to JShell Script

## Purpose

Move `MainTest` out of the JUnit test suite because it is an operational sample-data conversion script, not an automated test.

## Context

`src/test/java/net/arnx/wmf2svg/MainTest.java` scans `../wmf-testcase/data/src` for `.wmf` and `.emf` files, creates `../wmf-testcase/data/dst`, and invokes `net.arnx.wmf2svg.Main` to generate SVG and PNG outputs. It skips when the external sample-data directory is absent, so it does not assert library behavior.

The replacement should be runnable through the Maven JShell plugin while keeping regular `mvn test` focused on real tests.

## Tasks

1. Status: completed
   Next step: Added `src/test/jshell/main-test.jsh` with the sample-data conversion flow.
   Required context: Preserve the logging property, source/destination directories, extension filter, and `Main.main` arguments from `MainTest`.

2. Status: completed
   Next step: Added Maven configuration for `com.github.johnpoth:jshell-maven-plugin`.
   Required context: The project targets Java 8 bytecode but JShell requires running Maven with JDK 9 or newer.

3. Status: completed
   Next step: Removed the old `MainTest` from JUnit tests.
   Required context: Keep unrelated test classes unchanged.

4. Status: completed
   Next step: Verified the Maven test suite and the JShell script entrypoint in the local environment.
   Required context: The sample-data directory may not exist; success can be observed as a skip message for the script and normal JUnit results for tests.

5. Status: in_progress
   Next step: Append completion notes and move this file to `.tasks/done/`.
   Required context: Include changed files, command results, and any environment limitations.

## Goals

- `mvn test` no longer runs `MainTest`.
- A script file can run the sample conversion via Maven JShell plugin.
- The script preserves existing behavior for SVG and PNG generation.
- Verification commands and any limitations are recorded.

## File List

- `pom.xml`
- `src/test/jshell/main-test.jsh`
- `src/test/java/net/arnx/wmf2svg/MainTest.java`
- `.tasks/00227_move_main_test_to_jshell_script.md`

## Completion Notes

- Added `src/test/jshell/main-test.jsh` with the previous sample conversion behavior from `MainTest`.
- Added `com.github.johnpoth:jshell-maven-plugin:1.4` configuration in `pom.xml`, pointing at the script and enabling the test classpath.
- Deleted `src/test/java/net/arnx/wmf2svg/MainTest.java` so regular JUnit runs no longer execute the sample conversion script.
- Verified `mvn -q test` completed successfully.
- Verified `mvn -q test-compile jshell:run` completed successfully. In this workspace, `../wmf-testcase/data/src` exists, so the script performed real conversions and emitted debug logging.
- Local Java version is OpenJDK 25.0.2, which supports JShell. Running this script requires Maven to run on JDK 9 or newer.
