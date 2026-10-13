# Repository Instructions

`v1.3 · 2026-10-13` — Le regole globali dell'utente valgono anche qui. Prima di modificare, leggere il [tracker](PROJECT_STATUS.json), le [decisioni](plan.md), le [evidenze](roadmap.md) e la pagina tecnica pertinente.

## Scopo e stack

- Editor locale per censimento di reti e infrastrutture: Android 14+ e Windows 11 x64 portable. Scambio manuale `.ofam` v1; nessun server o sync.
- Kotlin/JVM 17, Compose Android/Desktop, Room v3/SQLCipher su Android, PDFBox su Windows. Versioni dipendenze in `gradle/libs.versions.toml`; packaging Windows con JDK 21.
- Riferimenti canonici: `plan.md` per le decisioni, `PROJECT_STATUS.json` per il lavoro aperto, `roadmap.md` per gli esiti, `docs/` per ambito.

## Direttive di progetto

- Mantenere `shared:core` e `shared:exchange` indipendenti da Android UI/`Context`. La UI condivisa è in `shared/configurator`; hardware nei JSON, non in nuove colonne Room senza decisione esplicita.
- Modifiche allo schema Room richiedono versione nuova e strategia di recupero. Conservare soltanto il fallback distruttivo bidirezionale documentato; non promettere migrazione automatica. Rifiutare ID di altri progetti prima di cancellare record.
- `MapWorkspace` usa lo `MapUiState` dell'host: camera, selezione e contenitori sopravvivono a cambio vista/dimensione senza entrare in Room o `.ofam`. Le viste restano nella barra `MapViewBar`; riusare `OnlyFieldTheme`, `AppSpacing`, `SectionCard`, `ListRow`, `FormRow` e `ContentDialog`. Form testuali entro 640 dp; liste larghe e canvas tecnici sono esclusi. Dialoghi a schermo intero su finestre compatte, `AlertDialog` per conferme. Conservare i campi non esposti con `copy()`, non riassegnare reti dopo rifiuti di ambito e chiudere gli editor solo dopo il save riuscito.
- Scambio: `STRUCTURAL_ERROR` blocca, `DOCUMENTARY_WARNING` consente la revisione. Usare `DocumentSelection` nei documenti e chiudere ogni `ProjectPackage`; rispettare i limiti 256 MiB ZIP, 32 MiB file, 512 MiB espansi, 10.000 entry e 1.000.000 iterazioni KDF.
- Preservare staging/backup cifrati, media recuperabili e dati locali. La protezione facoltativa usa AES-256-GCM/PBKDF2; le credenziali non entrano nei documenti. Windows usa RAM limitata e nessun temporaneo in chiaro per media protetti. Android usa worker, comandi ordinati e transazioni; Windows usa worker, `SecondaryLoop` e blocco degli edit occupati.
- `core.i18n.Messages`: messaggi it/en/es UTF-8, italiano predefinito; acquisire la lingua all'avvio della generazione e preservare il testo utente. Commenti sorgente sintetici e pratici. Eseguire test rapidi e completi solo quando emerge un problema o cambia il codice rilevante.

## Comandi e verifica

Da PowerShell nella radice del repository. Comandi storicamente eseguiti; **non rieseguiti** nella revisione documentale del 13 ottobre 2026.

| Scopo | Comando | Effetto/limite |
| --- | --- | --- |
| Suite JVM/Compose | `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1` | Solo offline; non verifica hardware o EXE reale. |
| Demo mirata | `.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1` | Verifica fixture versionata. |
| Rigenera demo | `.\gradlew.bat :shared:exchange:demoPackage --no-parallel --max-workers=1` | Aggiorna `fixtures/demo/onlyfield-demo.ofam`: data/ID progetto fissi, ID entità rigenerati; import separato nelle app. |
| Build Android | `.\gradlew.bat :mobile:app:assembleDebug` | Compila, non installa. |
| Portable | `.\gradlew.bat :pc:app:packagePortable` | Scrive `dist/`; controllare dati del runtime. |

I connected Gradle test Android disinstallano dati applicativi: richiedono autorizzazione specifica. Instrumentation valida solo se termina con `OK`. `prepare-release.ps1` richiede output vuoto; `dist/**/data`, backup, chiavi e report non sono scratch.

## Confini attuali

`RES-13`, `RES-19` e `RES-23` sono collaudi nativi parziali: non chiuderli con suite JVM o larghezze simulate. Rimuovere dal tracker una voce completata e registrare in `roadmap.md` comando, esito e limite reale. Non creare commit o push senza richiesta esplicita.
