# Verifica

Per lo stato aperto usare [PROJECT_STATUS.json](../PROJECT_STATUS.json). Le prove automatiche non chiudono i collaudi di fotocamera, rotazione fisica, TalkBack, tablet, EXE e stampa reali.

## Comandi nativi

Da PowerShell nella radice del repository:

| Scopo | Comando | Effetto |
| --- | --- | --- |
| Suite JVM/Compose | `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1` | Nessun dato del telefono modificato. |
| Test mirato | `.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1` | Verifica fixture demo. |
| Genera demo | `.\gradlew.bat :shared:exchange:demoPackage --no-parallel --max-workers=1` | Riscrive la fixture versionata; verificare il diff. |
| Compila Android | `.\gradlew.bat :mobile:app:assembleDebug` | Produce APK debug, non installa. |
| Portable Windows | `.\gradlew.bat :pc:app:packagePortable` | Scrive in `dist/`; preservare i dati del runtime. |

I connected Gradle test Android disinstallano i dati applicativi: eseguirli solo con autorizzazione specifica e profilo controllato. La compilazione dell'APK dei test non equivale a eseguirli. Un'instrumentation nativa è valida solo con esito finale `OK`. Per Gradle `--tests`, consultare la [guida ufficiale](https://docs.gradle.org/current/userguide/java_testing.html).

## Evidenza disponibile

L'ultimo handoff dell'11 ottobre 2026 riporta 451 prove unit/Compose e 19 prove demo superate; è evidenza storica, non un'esecuzione di questa revisione documentale. Gli esiti nativi parziali e i limiti sono nelle matrici [Android](testing/hardware-checklist.md) e [Windows](testing/windows-native-matrix.md). Per una nuova modifica al codice scegliere prima il modulo mirato, poi allargare la verifica solo se emerge un problema o un confine fra moduli.

La [demo](testing/demo.md) usa dati sintetici. Le [misure degli import grandi](testing/import-benchmark-2026-10-05.md) sono storiche e non vanno confuse con prestazioni garantite su altri dispositivi.
