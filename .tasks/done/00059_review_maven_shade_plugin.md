purpose
- Determine whether the current maven-shade-plugin configuration in pom.xml is necessary.

context
- The user asked whether maven-shade-plugin is needed.
- Active file is pom.xml.
- No implementation change is requested yet.

tasks
- status: done
  next step: Inspected pom.xml plugin and dependency configuration.
  context: Check what artifact the shade plugin produces and whether dependencies need bundling.
- status: done
  next step: Inspected source entry points and documentation for executable jar usage.
  context: Determine whether Main-Class or command-line distribution relies on the shaded jar.
- status: done
  next step: Ran a focused Maven package check.
  context: Observe produced artifacts and confirm whether shade changes publication/runtime behavior.
- status: done
  next step: Summarized whether the plugin is necessary and when it could be removed.
  context: Provide a concise answer without changing build files.

goals
- Explain whether maven-shade-plugin is needed.
- Identify the observable artifact or behavior it supports.
- Leave code and build configuration unchanged unless the user asks for edits.

file list
- pom.xml
- README* if present
- src/main/** entry point files if present

summary
- maven-shade-plugin is not currently needed for dependency bundling because the only declared dependency is JUnit with test scope.
- It is currently needed for the produced distribution jar behavior: adding Main-Class net.arnx.wmf2svg.Main and excluding compiled stub classes under android/** and com/** from the final jar.
- Verified with mvn -q package. target/wmf2svg-0.10.3.jar has Main-Class and no android/com stub packages; target/original-wmf2svg-0.10.3.jar lacks Main-Class and contains those stub packages.
