# Verification

[Retained Italian source](05-testing-and-benchmarks.it.md)

Open work belongs to the repository's declared SecondBrain tracker. Automated tests do not complete camera, physical rotation, TalkBack, tablet, executable or printing qualification.

## Commands

Run from a controlled checkout with the required JDK/toolchains and Android SDK:

~~~powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1
~~~

`:mobile:app:assembleDebug` builds an APK without installing it. `:pc:app:packagePortable` creates desktop output under dist and must not overwrite personal runtime data.

The demoPackage task rewrites a tracked fixture and its entity IDs. Run it only when intentionally updating the demo; inspect the resulting diff. It is not an ordinary read-only verification.

Connected Android tests may uninstall application data. They require specific authorization and a controlled profile. Compiling the instrumentation APK is not executing it; native instrumentation needs a final OK result.

## Evidence and limits

The 11 October 2026 handoff records 451 unit/Compose tests and 19 demo tests. Those are dated historical results, not a rerun on every documentation revision. Check current report revisions and environments before reusing them.

Use the smallest relevant module suite first; broaden only for new changes, failures or unresolved boundaries. Keep test outputs, fixtures and failed runs. Agent-managed operational files belong in the initialized central runtime; a fresh isolated checkout redirects source-relative Gradle outputs there. Environment variables alone do not redirect every task.

The [demo](testing/demo.md) is synthetic. [Large-import measurements](testing/import-benchmark-2026-10-05.md) are historical, not device-independent performance promises. Native gaps are in the [Android](testing/hardware-checklist.md) and [Windows](testing/windows-native-matrix.md) matrices.
