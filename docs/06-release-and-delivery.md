# Rilascio, Build e Consegna

Data: 3 ottobre 2026

## Comandi Gradle di Verifica e Build

Tutti i comandi si eseguono dalla radice del repository con il wrapper Gradle:

```powershell
# Suite completa dei test unitari (94 test)
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
- **Contratto Dati Consolidato:** Versione `1.8`, compatibile in lettura con la `1.7` (`docs/02-domain-data-contract.md`)

## Uso del Programma Portable

1. Estrarre lo ZIP (o copiare la cartella `dist/OnlyFieldAssetManager/`) in qualsiasi posizione, anche su chiavetta USB.
2. Avviare `OnlyFieldAssetManager.exe`.
3. I progetti vengono salvati automaticamente in `data\` accanto all'eseguibile; copiando la cartella si porta con sé anche il lavoro.
4. Se la cartella del programma non è scrivibile (es. `C:\Program Files`), i dati vengono salvati in `%USERPROFILE%\.onlyfield_asset_manager`. Il percorso in uso è sempre visibile nella barra di stato.

La ricostruzione con `packagePortable` sostituisce `app/` e `runtime/` ma conserva `data/`.
