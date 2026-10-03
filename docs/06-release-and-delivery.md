# Rilascio, Build e Consegna

Data: 3 ottobre 2026

## Comandi Gradle di Verifica e Build

Tutti i comandi si eseguono dalla radice del repository con il wrapper Gradle:

```powershell
# Suite JVM/Room/Compose
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest

# APK Android debug e release
.\gradlew.bat :mobile:app:assembleDebug :mobile:app:assembleRelease

# Programma Windows portable x64: cartella con l'eseguibile nella radice + archivio ZIP
.\gradlew.bat :pc:app:packagePortable
```

`packagePortable` usa un JDK 21 dedicato (toolchain Gradle, scaricato automaticamente se assente) che contiene `jpackage`, indipendentemente dal JDK impostato in `org.gradle.java.home`. Il runtime Java è incluso nella cartella: sul PC di destinazione non serve installare nulla.

## Artefatti di Rilascio

- **Android APK Debug:** `mobile/app/build/outputs/apk/debug/app-debug.apk`
- **Windows portable (cartella):** `dist/OnlyFieldAssetManager/`
  ```
  OnlyFieldAssetManager/
    OnlyFieldAssetManager.exe   ← avvio con doppio clic
    app/                        ← librerie dell'applicazione
    runtime/                    ← Java runtime incluso
    data/                       ← creata al primo avvio: progetti salvati
  ```
- **Windows portable (archivio):** `dist/OnlyFieldAssetManager-portable-x64-1.0.0.zip` (stessa cartella, senza `data/`)
- **Contratto Dati Consolidato:** Versione `1.11`, legge `1.7`–`1.10`; versioni successive rifiutate (`docs/02-domain-data-contract.md`)

## Uso del Programma Portable

1. Estrarre lo ZIP (o copiare la cartella `dist/OnlyFieldAssetManager/`) in qualsiasi posizione, anche su chiavetta USB.
2. Avviare `OnlyFieldAssetManager.exe`.
3. I progetti vengono salvati automaticamente in `data\` accanto all'eseguibile; copiando la cartella si porta con sé anche il lavoro.
4. Se la cartella del programma non è scrivibile (es. `C:\Program Files`), i dati vengono salvati in `%USERPROFILE%\.onlyfield_asset_manager`. Il percorso in uso è sempre visibile nella barra di stato.

La ricostruzione con `packagePortable` sostituisce `app/` e `runtime/` ma conserva `data/`.


## Collaudo nativo Android senza sostituire l'app personale

Verificati su telefono API 36: `FloorNativeTest` (3 casi, inclusi backup/ripristino SQLCipher 11 → 13 e 12 → 13) e `FloorGestureNativeTest` (1 caso). Il runner installa e rimuove l'app QA separata. Le fixture della migrazione usano comunque un database isolato nella cache. Per ripetere il controllo, creare questo init script temporaneo in `mobile/app/build/qa-phone.init.gradle`:

```groovy
allprojects { project ->
    project.plugins.withId('com.android.application') {
        project.extensions.getByName('androidComponents').finalizeDsl { android ->
            android.defaultConfig.applicationId = 'com.onlyfield.assetmanager.qa'
        }
    }
}
```

Eseguire separatamente i filtri (il wrapper Windows può troncare un elenco di classi separato da virgole):

```powershell
.\gradlew.bat -I mobile/app/build/qa-phone.init.gradle :mobile:app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.onlyfield.assetmanager.FloorNativeTest'
.\gradlew.bat -I mobile/app/build/qa-phone.init.gradle :mobile:app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.onlyfield.assetmanager.FloorGestureNativeTest'
Remove-Item -LiteralPath mobile/app/build/qa-phone.init.gradle
# Ricostruire l'APK con l'identificativo normale dopo il collaudo QA.
.\gradlew.bat :mobile:app:assembleDebug
```

Controllare il conteggio nei report `mobile/app/build/outputs/androidTest-results/connected/debug/` dopo ogni filtro. Non sono stati collaudati scatto/QR reali, lettore USB fisico e multitouch manuale.

## CI su Windows: verifica e distribuzione

Il workflow [release.yml](../.github/workflows/release.yml) parte su push di tag `v*` o con avvio manuale. Runner `windows-2025`, JDK 21 Temurin per il launcher, SDK `platforms;android-37.0`, build-tools 37.0.0 e wrapper Gradle 9.7.1. Il progetto conserva i criteri del daemon JetBrains JDK 21 in `gradle/gradle-daemon-jvm.properties`: Gradle può approvvigionarlo automaticamente. Il parametro `-Dorg.gradle.java.home=$env:JAVA_HOME` supera il percorso personale in gradle.properties senza modificarlo.

Prima della distribuzione vengono eseguite le suite dei quattro moduli; seguono APK debug, compilazione APK strumentale e ZIP portable. La CI Windows non esegue i test su emulatore né il collaudo hardware. Le quattro azioni ufficiali checkout/setup-java/upload-artifact/download-artifact sono fissate a SHA delle release verificate il 3 ottobre 2026; il job build ha soltanto `contents: read`, quello publish `contents: write`. I comandi nativi controllano gli exit code.

Lo script [prepare-release.ps1](../.github/scripts/prepare-release.ps1) richiede un APK non vuoto e un unico ZIP con l'eseguibile, rifiuta qualsiasi cartella `data/` nell'archivio e prepara `build/release/` con APK, ZIP e `SHA256SUMS`. La cartella di output deve essere vuota; dopo un controllo locale, spostare i risultati o rimuovere soltanto quelli generati prima di ripetere il comando.

L'avvio manuale produce l'artefatto `OnlyFieldAssetManager-packages` conservato per 14 giorni e **non pubblica**, anche se si sceglie un tag. Su un push di tag, il job publish attende la build, scarica i pacchetti, ricontrolla SHA-256 e usa `gh release create --verify-tag --generate-notes`: non crea tag mancanti né sovrascrive una release esistente. Il tag non cambia automaticamente versionName/packageVersion. Nessuna esecuzione remota è stata avviata in questa consegna.

Verifiche locali: actionlint 1.7.12 e PSScriptAnalyzer senza segnalazioni; preparati i due artefatti reali, checksum verificati; tre casi negativi (APK assente, ZIP assente, ZIP con data/) rifiutati. Suite con override JDK: BUILD SUCCESSFUL, 43 task up-to-date. RES-01 resta aperto: manca una vera esecuzione su tag con URL del run/release e riscontro degli artefatti scaricati.

Fonti: [sintassi GitHub Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax), [pinning e sicurezza](https://docs.github.com/en/actions/reference/security/secure-use), [immagine Windows 2025](https://github.com/actions/runner-images/blob/main/images/windows/Windows2025-Readme.md), [gh release create](https://cli.github.com/manual/gh_release_create).

## Ultimo collaudo su emulatore

3 ottobre 2026: AVD temporaneo Google APIs API 35, app e APK androidTest normali installati esclusivamente sull'emulatore della sessione. `adb -s <seriale-emulatore> shell am instrument -w com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → OK (7 tests). Installazione da `assembleDebug` e `assembleDebugAndroidTest`; emulatori arrestati dopo le prove; la rimozione dei file temporanei è bloccata dal controllo automatico (RES-18). LocalizedPdfTest usa l'estrazione del testo PdfRenderer disponibile da API 35. Su API 37.1 due test UI sono bloccati da Espresso (RES-17). Le prove hardware restano nella [checklist](testing/hardware-checklist.md).
