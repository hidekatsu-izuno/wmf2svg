purpose
- Replace maven-shade-plugin with maven-jar-plugin while preserving the current final jar behavior.

context
- The current shade plugin is not needed for dependency bundling.
- It currently adds Main-Class net.arnx.wmf2svg.Main and excludes stub classes under android/** and com/**.
- The requested change is to achieve the same behavior using maven-jar-plugin.

tasks
- status: done
  next step: Updated pom.xml plugin configuration.
  context: Remove maven-shade-plugin and add maven-jar-plugin with archive manifest mainClass and excludes for android/** and com/**.
- status: done
  next step: Ran Maven package verification.
  context: Confirm build succeeds and target/wmf2svg-0.10.3.jar has Main-Class while excluding stub packages.
- status: done
  next step: Compared final jar behavior with prior observations.
  context: Verify java -jar still reaches Main usage output.
- status: done
  next step: Recorded summary and moved task to done.
  context: Include files changed and verification results.

goals
- pom.xml no longer uses maven-shade-plugin.
- The built jar remains executable with java -jar.
- The built jar does not include android/** or com/** stub classes.

file list
- pom.xml

summary
- Replaced maven-shade-plugin with maven-jar-plugin in pom.xml.
- Configured the jar manifest mainClass as net.arnx.wmf2svg.Main.
- Configured jar excludes for android/** and com/** so compiled stub classes are omitted from the final jar.
- Verified with mvn -q package.
- Verified target/wmf2svg-0.10.3.jar contains Main-Class: net.arnx.wmf2svg.Main.
- Verified jar listing does not contain android/ or com/ entries.
- Verified java -jar target/wmf2svg-0.10.3.jar reaches the command usage output.
