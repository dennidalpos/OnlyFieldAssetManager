# OnlyFieldAssetManager — Stato di Avanzamento e Registro Evidenze

Data aggiornamento: 3 ottobre 2026

## Stato Globale del Progetto

- **Fase Android (A00–A13)**: 100% Completata (14 step completati su 14).
- **Fase Windows (W00–W05)**: 100% Completata (6 step completati su 6: W00, W01, W02, W03, W04, W05 completati).
- **Toolchain**: Gradle 9.7.1, AGP 9.4.1, compileSdk 37, targetSdk 35, minSdk 34.
- **Rework UX (U01–U02)**: Completato il 3 ottobre 2026 (vedi `docs/07-ux-audit.md`).
- **Fase v1.1 (S/O/R/F)**: In corso (9 step completati su 14), vedi `plan.md`.
- **Stato Complessivo**: 31 step completati su 36 (v1.0 chiusa, fase v1.1 in corso).
- **Test Unitari Totali Passati**: 103 test su 103 (22 mobile/app, 27 shared/core, 16 shared/exchange, 38 pc/app).

## Tabella Riassuntiva degli Step

| Step | Ambito | Descrizione | Stato | Data | Evidenze Principali |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **A00** | Android | Ambiente, struttura multi-modulo, Gradle wrapper e Compose minima | Completato | 02/10/2026 | APK Debug compilato, Gradle sync |
| **A01** | Android | Modello dati core, contratto pacchetto .ofam e fixtures | Completato | 02/10/2026 | Modello shared:core, PackageSerializer |
| **A02** | Android | Persistenza Room, DAO, Repository e ricerca inventario | Completato | 02/10/2026 | DB Room v1, UI ricerca Compose |
| **A03** | Android | Export/import SAF e confronto comparativo pacchetti | Completato | 02/10/2026 | SAF picker, ProjectComparison |
| **A04** | Android | Credenziali, cifratura PBKDF2/AES-GCM e password | Completato | 02/10/2026 | Cifratura pacchetti, Room Migration 1->2 |
| **A05** | Android | Rack fronte/retro, modelli e primo report PDF rack | Completato | 02/10/2026 | Layout U, PDF PdfExportManager, DB Migration 2->3 |
| **A06** | Android | Planimetrie, foto, allegati e annotazioni grafiche | Completato | 02/10/2026 | ZIP con allegati media, DB Migration 3->4 |
| **A07** | Android | Cablaggio fisico, percorsi condivisi e mapping pannelli | Completato | 02/10/2026 | Tracciamento catene cavi, DB Migration 4->5 |
| **A08** | Android | Rete logica, VLAN, CIDR subnet, SVI L3, LAG, WAN/VPN | Completato | 02/10/2026 | 9 nuove entità rete, DB Migration 5->6 |
| **A09** | Android | Alimentazione A/B, PDU, UPS, PoE e derivazione badge | Completato | 02/10/2026 | PowerFeed, PoeMapping, DB Migration 6->7 |
| **A10** | Android | Cestino locale, undo, sostituzione, fusione duplicati, batch edit | Completato | 02/10/2026 | TrashItem, replacement, merge, DB Migration 7->8 |
| **A11** | Android | Esportazione XLSX OpenXML, Markdown, PDF composti e Stampa | Completato | 02/10/2026 | XlsxExportManager, PrintAdapter, zero secret leakage |
| **A12** | Android | Acquisizione cartografica offline con attribuzione | Completato | 02/10/2026 | OpenTopoMap/CARTO, DB Migration 8->9 |
| **A13** | Android | Pilota Android (100 apparati), benchmark e consegna W00 | Completato | 02/10/2026 | Benchmark <20ms, APK finale, contratto v1.7 |
| **W00** | Windows | Modulo :pc:app, Compose Desktop 1.7.3 e JDK 21 | Completato | 02/10/2026 | App Desktop avviabile, test toolchain |
| **W01** | Windows | DesktopStorageManager, salvataggio atomico e blocco .lock | Completato | 02/10/2026 | Atomic tmp+replace, .lock channel, 11 test pc:app |
| **W02** | Windows | Inventario, rack, modelli, media e modifiche Desktop | Completato | 02/10/2026 | Visual Elevation 2D, DesktopDomainLogic, 19 test pc:app |
| **W03** | Windows | Cablaggio, rete logica e alimentazione su Desktop | Completato | 02/10/2026 | Tab Cablaggio, Rete Logica, Feed A/B, PoE, Badges, 22 test pc:app |
| **W04** | Windows | Documenti, stampa nativa e cartografia su Desktop | Completato | 02/10/2026 | DesktopDocumentManager, PrinterJob, Cartografia OpenTopoMap/CARTO, 26 test pc:app, correzioni UI-01..UI-07 |
| **W05** | Windows | Interoperabilità bidirezionale e pacchetto portable x64 | Completato | 02/10/2026 | BidirectionalInteropTest, createDistributable / AppImage portable x64, 29 test pc:app |
| **U01** | Windows | Rework UX editor e programma portable con .exe in radice | Completato | 03/10/2026 | `packagePortable` → `dist/`, dati portable, navigazione laterale, menu e scorciatoie, selettori, conferme, form che preservano i campi |
| **U02** | Android | Rework UX app: navigazione, schermate dedicate, selettori | Completato | 03/10/2026 | Back stack nel ViewModel, Back di sistema, rotazione, CRUD apparati/porte/aree/credenziali, snackbar con Annulla, export su stream corretto |
| **S01** | Sicurezza | Database Android cifrato | Completato | 03/10/2026 | Room + SQLCipher 4.19.1 (EncryptedDatabase), chiave casuale cifrata con chiave Keystore in no_backup/; migrazione del DB v9 in chiaro provata su emulatore (progetto esistente aperto, file non più leggibile come SQLite); allowBackup=false + data_extraction_rules |
| **S02** | Sicurezza | Password del progetto con PBKDF2 e salt | Completato | 03/10/2026 | PasswordHasher in :shared:exchange (PBKDF2-HMAC-SHA256, salt 16 byte, 600.000 iterazioni); hash SHA-256 legacy ricalcolati al primo sblocco; PasswordHasherTest + test di migrazione nel repository |
| **O01** | Avvio | Procedura guidata "Nuovo sito" condivisa | Completato | 03/10/2026 | core.onboarding.NewSiteWizard (passi, validazione, creazione via ProjectEdits) usato da Android NewSiteScreen e dalla finestra a passi Windows; NewSiteWizardTest + test Windows con password |
| **O02** | Avvio | Schermata iniziale chiara | Completato | 03/10/2026 | Android ProjectsScreen: azioni grandi al primo avvio e «Continua: «ultimo progetto»» in cima; Windows WelcomeCard con le stesse azioni; procedura verificata su emulatore |
| **O03** | Avvio | Home progetto Android: ricerca e azioni rapide | Completato | 03/10/2026 | Ricerca apparati in cima alla home (stessi campi dell'Inventario) con apertura diretta della scheda, azione rapida «Aggiungi apparato»; Foto/Scansiona collegate in F01/F02; verificata su emulatore |
| **R01** | Refactor UI | Android: modifiche a pagina intera | Completato | 03/10/2026 | EditScreen a pagina intera per tutti i 27 editor di entità, conferma su modifiche non salvate (LocalMarkDirty), configChanges per la rotazione; verificato su emulatore (rotazione, Indietro, insets) |
| **R02** | Refactor UI | Windows: pannello laterale (master-detail) | Completato | 03/10/2026 | MasterDetailHost + EditPanel per tutte le sezioni Windows (Ctrl+S, Esc, conferma su modifiche non salvate); MasterDetailTest (Compose UI test desktop); residuo RES-08 |
| **R03** | Refactor UI | Icone Material e tema scuro | Completato | 03/10/2026 | Material Symbols al posto delle emoji (drawable Android, SymbolIcons Windows), tema Android chiaro/scuro di sistema anche per la finestra, Windows «Visualizza › Tema scuro» salvato nella cartella dati; verificato in tema scuro su emulatore |
| **R04** | Refactor UI | Refactor repository Android | Completato | 03/10/2026 | ProjectRepository (1.141 righe) diviso in facciata + 6 classi per area, EntityMappers (945) in 5 file di mapper; rimossi 20 metodi senza chiamanti e lo stack di undo mai letto; 22 test Android invariati e verdi |
| **F01** | Funzioni | Foto dalla fotocamera | Da fare | — | — |
| **F02** | Funzioni | Scansione QR/barcode | Da fare | — | — |
| **F03** | Funzioni | Etichette QR proprie | Da fare | — | — |
| **F04** | Funzioni | Fusione all'import con scelta per elemento | Da fare | — | — |
| **F05** | Funzioni | Mappe su Android (chiude RES-07) | Da fare | — | — |

## Registro del Collaudo e della Build Finale

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:packagePortable :mobile:app:assembleDebug
# Esito: BUILD SUCCESSFUL (103 passed unit tests: 22 mobile/app, 27 shared/core, 16 shared/exchange, 38 pc/app)
```

## Registro Residui e Note di Monitoraggio

Nessun blocco operativo o anomalia residua rilevata durante il collaudo W05.
Segnalazioni per future iterazioni post-v1.0 (non bloccanti per il rilascio):
- **RES-01 (Integrazione CI/CD Automation)**: Aggiunta opzionale di workflow GitHub Actions per la pubblicazione automatica di `app-debug.apk` e dell'eseguibile Windows Desktop ad ogni tag release.
- **RES-02 (Localizzazione Multi-lingua)**: Espansione del dizionario stringhe da Italiano a Inglese/Spagnolo per mercati internazionali.
- ~~RES-03 (PDF Windows)~~: risolto il 03/10/2026 — PDF reale multipagina (`SimplePdfWriter`) e stampa su più pagine, stesso contenuto (`ReportContent`).
- ~~RES-04 (Allegati nei pacchetti)~~: risolto il 03/10/2026 — i file viaggiano nel `.ofam` (`AttachmentFiles`), cifrati nei progetti protetti.
- ~~RES-05 (Cestino Windows)~~: risolto il 03/10/2026 — cestino salvato su disco (progetti senza password) e «Annulla» (Ctrl+Z, 50 passi) per ogni modifica.
- ~~RES-06 (Messaggi di validazione)~~: risolto il 03/10/2026 — messaggi di validazione e di importazione in italiano.
- **RES-07 (Mappe Android)**: decisa il 03/10/2026 — permesso di rete solo per il download delle mappe; pianificata come F05.
- **RES-08 (Editor Windows)**: con un form modificato nel pannello laterale, la scelta di un altro elemento nella lista non chiede conferma (solo Annulla/Esc la chiedono).

## Note per la prossima sessione

- Audit UX e interventi: `docs/07-ux-audit.md`; tracker macchina: `PROJECT_STATUS.json`.
- Nessuna decisione aperta. Prossimo task: F01.
- Repository remoto: `origin` = https://github.com/dennidalpos/OnlyFieldAssetManager (privato), branch `main`.
