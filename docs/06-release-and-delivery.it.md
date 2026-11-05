# Build e rilascio

## Versioni e prerequisiti

Le versioni delle dipendenze sono in [libs.versions.toml](../gradle/libs.versions.toml); la versione app è nei due `build.gradle.kts`. Il codice Kotlin/JVM usa target 17; il packaging Windows richiede un toolchain JDK 21 con `jpackage`. L'app Android dichiara `minSdk=34`, `targetSdk=35`, `compileSdk=37`.

## Pacchetti

Da PowerShell nella radice:

```powershell
.\gradlew.bat :mobile:app:assembleDebug
.\gradlew.bat :pc:app:packagePortable
```

Il task Windows crea la cartella `dist/OnlyFieldAssetManager/` e uno ZIP portable. `data/` rimane accanto all'EXE per l'uso locale e non deve entrare nello ZIP. `createDistributable` rifiuta un runtime sorgente già contenente `data/`; conservare i dati prima di rigenerare. `prepare-release.ps1` richiede una destinazione vuota. Non usare un profilo reale per prove di packaging.

## CI

[release.yml](../.github/workflows/release.yml) gira manualmente oppure su tag `v*`: verifica le suite JVM/Compose, compila APK debug e ZIP portable, prepara gli artefatti e i checksum SHA-256. Solo il tag attiva il job di pubblicazione GitHub. Una build locale non pubblica nulla.

La release v1.0.1 è uno storico, non la qualifica automatica del checkout corrente. Vedi [verifica](05-testing-and-benchmarks.md) e [matrice Windows](testing/windows-native-matrix.md).

## Fonti

[Gradle Sync](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Sync.html), [Compose Desktop native distributions](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-native-distribution.html).
