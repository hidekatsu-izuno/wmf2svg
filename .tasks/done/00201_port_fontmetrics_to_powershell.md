purpose
- Port `etc/cs/FontMetrics.cs` to a PowerShell script under `src/test/bin`.

context
- The source C# file is `etc/cs/FontMetrics.cs`.
- Existing test helper scripts live under `src/test/bin`.
- The PowerShell port should preserve the observable behavior of the C# utility as closely as practical.

tasks
- status: completed
  next step: `FontMetrics.cs` enumerates installed font families and prints `font-emheight.<font name> = <ratio>` when regular em-height differs from ascent + descent. Existing PowerShell scripts use strict mode and `System.Drawing`.
  required context: Source file is `etc/cs/FontMetrics.cs`; target directory is `src/test/bin`.
- status: completed
  next step: PowerShell port will take no parameters, load `System.Drawing`, enumerate `InstalledFontCollection`, escape spaces as `\u0020`, and skip fonts that cannot provide regular metrics.
  required context: Use .NET classes available from PowerShell where possible.
- status: completed
  next step: Created `src/test/bin/fontmetrics.ps1`.
  required context: Keep the script readable and consistent with existing helpers.
- status: completed
  next step: Verified `src/test/bin/fontmetrics.ps1` with Windows PowerShell; it runs successfully and emits `font-emheight.*` lines in the C# utility format.
  required context: Use available local PowerShell execution if possible.
- status: completed
  next step: Completion summary appended and task file moved to `.tasks/done/`.
  required context: Include script path and verification result.

goals
- A PowerShell version of `FontMetrics.cs` exists in `src/test/bin`.
- The script preserves the C# utility's output format and core behavior.
- The script is verified at least for syntax and a representative invocation.

file list
- `.tasks/00201_port_fontmetrics_to_powershell.md`
- `etc/cs/FontMetrics.cs`
- `src/test/bin`

completion summary
- Added `src/test/bin/fontmetrics.ps1`.
- The script ports `etc/cs/FontMetrics.cs` by enumerating installed fonts through `System.Drawing.Text.InstalledFontCollection`.
- It prints `font-emheight.<escaped font name> =  <ratio>` when regular em-height differs from ascent plus descent.
- It escapes spaces as `\u0020`, matching the original C# utility.
- Verified with `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./src/test/bin/fontmetrics.ps1`.
