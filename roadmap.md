# OnlyFieldAssetManager — Stato di Avanzamento e Registro Evidenze

Data aggiornamento: 3 ottobre 2026

## Stato Globale del Progetto

- **Fase Android (A00–A13)**: 100% Completata (14 step completati su 14).
- **Fase Windows (W00–W05)**: 100% Completata (6 step completati su 6: W00, W01, W02, W03, W04, W05 completati).
- **Toolchain**: Gradle 9.7.1, AGP 9.4.1, compileSdk 37, targetSdk 35, minSdk 34.
- **Rework UX (U01–U02)**: Completato il 3 ottobre 2026 (vedi `docs/07-ux-audit.md`).
- **Fase v1.1 (S/O/R/F)**: Completata il 3 ottobre 2026 (14 step su 14), vedi `docs/07-ux-audit.md`.
- **Stato Complessivo**: 36 step di fase e 3 interventi di manutenzione completati (39/39).
- **Test Totali Passati**: ultima suite completa 166 unit/Compose (27 mobile/app, 46 shared/core, 32 shared/exchange, 61 pc/app); 4 prove Android QA registrate, di cui 3 ripetute per MAP03.

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
| **F01** | Funzioni | Foto dalla fotocamera | Completato | 03/10/2026 | TakePicture + FileProvider nella cartella allegati, collegamento automatico ad apparato/rack/area/progetto, elenco nella scheda apparato e indicazione del collegamento su Android e Windows; scatto verificato su emulatore |
| **F02** | Funzioni | Scansione QR/barcode | Completato | 03/10/2026 | Scanner CameraX + ML Kit incluso (offline), CodeLookup condiviso (seriale, etichetta, alias, cavi, porte), nuovo serialNumber (contratto 1.8, DB v10) con pacchetti 1.7 importabili (ContractVersionTest), lettore USB su Windows con Invio; permesso, anteprima e migrazione verificati su emulatore |
| **F03** | Funzioni | Etichette QR proprie | Completato | 03/10/2026 | LabelCode ofam://<progetto>/<tipo>/<id> riconosciuto da CodeLookup (apparati, rack, cavi; altro progetto segnalato), foglio A4 condiviso LabelSheetPdf (ZXing 3.5.4) su Android e Windows; orientamento dei QR verificato sul PDF generato |
| **F04** | Funzioni | Fusione all'import con scelta per elemento | Completato | 03/10/2026 | ProjectMerger (fusione a tre vie per elemento, conflitti tieni mio/importato, senza base ogni differenza è conflitto) con base salvata a ogni export/import (Android sync_snapshots DB v11, Windows data/sync cifrato); UI uno-per-uno su entrambe le app; ProjectMergerTest (5 casi) + test Windows; migrazione verificata su emulatore |
| **F05** | Funzioni | Mappe su Android (chiude RES-07) | Completato | 03/10/2026 | Permesso INTERNET solo per «Allegati › Mappa…» (3×3 tessere con attribuzione salvate come allegato), messaggio chiaro senza rete e per fonti che rifiutano (401/403), URL CARTO Positron corretto; verificato su emulatore con e senza rete; chiude RES-07, apre RES-09 |

## Registro del Collaudo e della Build Finale

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:packagePortable :mobile:app:assembleDebug
# Esito: BUILD SUCCESSFUL (117 passed unit tests: 24 mobile/app, 30 shared/core, 24 shared/exchange, 39 pc/app)
```

## Registro Residui e Note di Monitoraggio

Il tracker `PROJECT_STATUS.json` contiene soltanto attività e residui aperti. Evidenze di completamento nelle sezioni precedenti e nel registro manutenzione.
- **RES-01 (Integrazione CI/CD Automation)**: Aggiunta opzionale di workflow GitHub Actions per la pubblicazione automatica di `app-debug.apk` e dell'eseguibile Windows Desktop ad ogni tag release.
- **RES-02 (Localizzazione Multi-lingua)**: Espansione del dizionario stringhe da Italiano a Inglese/Spagnolo per mercati internazionali.
- **RES-11 chiuso**: deprecazioni eliminate; evidenze nella manutenzione successiva.

## Note per la prossima sessione

- Stato storico del 03/10/2026: RES-08, MAP01 e MAP02 completati; evidenze successive sono registrate sotto. Nessun commit o push è eseguito da questa attività.
- Audit UX e interventi: `docs/07-ux-audit.md`; tracker macchina: `PROJECT_STATUS.json`.
- Residui nello stato storico sopra: RES-13 (collaudo hardware rinviato), RES-01 (CI su tag) e RES-02 (localizzazione allora in corso). Per lo stato corrente consultare `PROJECT_STATUS.json` e le evidenze successive.
- Repository remoto: `origin` = https://github.com/dennidalpos/OnlyFieldAssetManager (privato), branch `main`.

## Manutenzione dopo v1.1

- **RES-08 completato (03/10/2026)**: la conferma precede cambio elemento, creazione e cambio tab interne; scartare chiude il vecchio editor. I modelli inizializzano i campi per elemento e la modifica delle porte generate segnala il form modificato. `.\gradlew.bat :pc:app:test`: BUILD SUCCESSFUL, 42 test passati (4 scenari MasterDetailTest).
- **MAP01 completato, chiude RES-09 (03/10/2026)**: download Windows di nove tessere OpenTopoMap in background, anteprima PNG, salvataggio negli allegati e attribuzione nei pixel/metadati. CARTO rimosso su Android e Windows, zoom 1–17. Fonte/condizioni verificate su [OpenTopoMap](https://opentopomap.org/about#verwendung) e requisito chiave su [CARTO](https://github.com/CartoDB/basemap-styles). Download reale Roma: PNG 768 × 800, 645.178 byte, ispezionato. Backend, errori, riapertura offline e trasferimento cifrato coperti da 5 test; interazione manuale dell'eseguibile non verificata.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:packagePortable :mobile:app:assembleDebug
# BUILD SUCCESSFUL, 125 test passati; portable e APK debug generati.
.\gradlew.bat :pc:app:test :pc:app:packagePortable --warning-mode all
# BUILD SUCCESSFUL, 47 test Windows; deprecazione Gradle preesistente tracciata in RES-11.
```


## MAP02 — Navigazione BU/piano e mappa interattiva (03/10/2026)

Completato su Android e Windows: wizard con elenchi BU/piani, home del piano a mappa, menu Strumenti, catalogo generico e tipologie personalizzate, schede con foto e campi extra, oggetti trascinabili e cavi con percorsi/estremità. Sfondo a griglia o immagine/PDF offline con anteprime numerate e pagina per piano. Rimossi il passo Primo apparato, la home a tile e il posizionamento tramite clic sullo sfondo.

Contratto approvato 1.9 con lettura 1.7/1.8 e rifiuto di versioni non supportate; nuove geometrie/tipologie/foto incluse in fusione e cifratura. Room 11 → 12 additiva, con backup SQLCipher prima dell'upgrade. I campi nascosti e gli allegati preesistenti sono preservati; un errore nella seconda foto annulla anche la copia della prima.

Evidenze: 149 test dei quattro moduli, UI desktop reale Compose, migrazione Room, PDFBox multipagina/protetto, riapertura e scambio cifrato. Su telefono API 36, installazione QA separata: 2 test nativi per PdfRenderer e backup/migrazione/ripristino SQLCipher; 1 test del clic/trascinamento Android. Fixture isolate, dati dell'app personale preservati. Totale 152 test passati.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :pc:app:test :mobile:app:assembleDebug :pc:app:packagePortable
# BUILD SUCCESSFUL; 149 test unit/Compose, APK normale e portable generati.
```

Comandi dei 3 test nativi in [docs/06-release-and-delivery.md](docs/06-release-and-delivery.md). Non verificati: scatto/QR con fotocamera reale, lettore USB fisico e multitouch manuale (RES-13). Deprecazioni preesistenti RES-11. Documentazione e tracker sincronizzati per il cambio sessione; commit e push su `main` autorizzati dall'utente.

## RES-10 — Protezione globale Windows (3 ottobre 2026)

Guardia condivisa tra menu, pannelli e schede mappa. Cambio sezione, chiusura, uscita, nuovo progetto, apertura e undo attendono la conferma della bozza; foto/allegati inclusi. Verifica: `.\gradlew.bat :pc:app:test :pc:app:packagePortable`, BUILD SUCCESSFUL, 57 test Windows.

## MAP03 — Contenitori annidati e connessioni aggregate (3 ottobre 2026)

Completato su Android e Windows: relazione tipizzata figlio/genitore, rack sempre contenitore e opzione per tipologie di apparato; conversione compatibile di `rackId`; mappa con sole radici; navigazione in sottomodali, assegnazione, creazione e rimozione dei figli; cavi proiettati sulla radice, aggregati per coppia e indicatore per cavi interni. Eliminazione, cestino, undo, modifiche multiple, fusione e scambio usano la stessa gerarchia.

Contratto `.ofam` 1.10, lettura 1.7–1.9; Room 13 con migrazione 12 → 13 e backup SQLCipher della versione sorgente. Verifiche mirate JVM/Compose: `ObjectHierarchyTest`, `ContainmentExchangeTest`, `ContractVersionTest`, `ObjectMapStorageTest`, `ContainerUiTest` e `FloorMapUiTest`, BUILD SUCCESSFUL. Sul telefono Android API 36, l’app QA separata ha eseguito 3 casi `FloorNativeTest` senza errori, incluso backup/ripristino SQLCipher da v11 e v12.

## RES-12 — Consolidamento documentazione e tracker (3 ottobre 2026)

Contenuti validi del piano A00–W05 trasferiti in `plan.md` e documenti di dominio; aggiornati contratto 1.10, storage, UX, QA e release. Ritirato il piano storico e corretti i riferimenti. Il tracker contiene soltanto lavoro aperto: RES-13 per prove hardware manuali, RES-14 per cestino Windows cifrato, più miglioramenti futuri RES-01/02/11.

## RES-15 — Blocco concorrente Windows (3 ottobre 2026)

La UI acquisisce il lock prima di aprire, creare o sostituire la copia locale; la lettura locale avviene sotto blocco. Una seconda istanza non puo sovrascriverla. Apertura fallita e password errata preservano il progetto precedente e rilasciano i nuovi blocchi. I file .lock restano come marcatori: solo il lock del sistema operativo determina l'occupazione. Verifica: `:pc:app:test --tests "*WorkingCopyLockTest" --tests "*DesktopStorageTest" --tests "*DesktopAppStateTest"`, BUILD SUCCESSFUL, 14 test.

## RES-16 — Password e base di fusione (3 ottobre 2026)

Cambio password: pacchetti preparati prima delle sostituzioni, base originale ricifrata senza aggiornarne i contenuti, rollback della base su errore del salvataggio principale e pulizia dei temporanei. La UI cambia password soltanto dopo il successo. Una base danneggiata blocca il cambio; una base indisponibile al merge mostra un avviso e richiede revisione completa. Verifica: `:pc:app:test --tests "*PasswordRotationTest" --tests "*WorkingCopyLockTest" --tests "*DesktopAppStateTest"`, BUILD SUCCESSFUL, 8 test.

## RES-14 — Cestino Windows cifrato (3 ottobre 2026)

Cestino incluso nella voce interna `attachments/local/trash.json` della copia locale .ofam: cifrato insieme al progetto e salvato con una singola sostituzione atomica. Vecchi JSON migrati e rimossi dopo il salvataggio. Ripristino e undo conservano gerarchia e montaggio; corruzione segnalata senza sovrascrivere. Export e basi di fusione escludono il cestino. Verifica: `:pc:app:test`, BUILD SUCCESSFUL, 67 test Windows.

## RES-11 — Deprecazioni eliminate (3 ottobre 2026)

Registrazione esplicita di assemblePortable, MasterDetailTest migrato alla API Compose v2 e dipendenze desktop dichiarate direttamente: Material3 1.9.0 e ui-test-junit4 1.12.1, senza aggiornare versioni risolte. Riferimenti architetturali aggiornati a Compose 1.12.1. Verifica: `:pc:app:test :pc:app:packagePortable --warning-mode all`, BUILD SUCCESSFUL, 67 test; nessuna deprecazione rilevata. Le normali segnalazioni Kotlin nel core/export sono trattate nella localizzazione dei relativi file.

## RES-02 — Localizzazione (3 ottobre 2026)

Sistema/Italiano/English/Español persistenti su entrambe le app; 1490 chiavi per catalogo, UTF-8 e MessageFormat nel core. Tradotti UI, validazioni, etichette di dominio, PDF/XLSX/Markdown, QR e stampa; lingua catturata all'avvio dell'export, testi utente e codici conservati, segreti esclusi. Protezione delle bozze al cambio lingua Windows; selettore Android fuori dagli editor. Corrette omissioni dei piani diretti nei report e rimossi PDF fittizi di ripiego.

Verifica: `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest :pc:app:packagePortable --warning-mode all`, BUILD SUCCESSFUL; 178 test (48 core, 34 exchange, 70 Windows, 26 Android JVM), zero fallimenti. Emulatore temporaneo API 35: suite completa via AndroidJUnitRunner, OK (7 tests), con estrazione del testo dei PDF nelle tre lingue e controllo dei segreti. API 37.1: PDF e migrazioni passano, due test UI si arrestano prima delle asserzioni per `InputManager.getInstance` in Espresso; residuo RES-17. Nessuna prova hardware manuale. [Dettagli](docs/09-localization.md).

## RES-01 — CI implementata, criterio remoto aperto (3 ottobre 2026)

Workflow Windows su tag v* e avvio manuale, JDK 21/SDK 37.0, override del percorso JDK personale, suite prima di APK debug/ZIP portable/checksum. Azioni ufficiali fissate a SHA; permessi distinti, exit code espliciti, data/ rifiutata nello ZIP. Avvio manuale solo artefatti; pubblicazione su tag dopo verifica dei checksum.

Verifiche locali: actionlint 1.7.12 e PSScriptAnalyzer senza segnalazioni; script con APK/ZIP reali e SHA-256 validi; tre rifiuti attesi per APK/ZIP mancanti e ZIP contenente dati. Wrapper con override JDK: BUILD SUCCESSFUL. Non osservata alcuna esecuzione GitHub Actions su tag: RES-01 mantenuto PARTIAL. [Procedura e fonti](docs/06-release-and-delivery.md).

## RES-13 — Checklist hardware pronta, collaudo rinviato (3 ottobre 2026)

Preparata [checklist hardware](docs/testing/hardware-checklist.md) con dispositivo, sistema, versione app, scenario, esito ed evidenza per foto, QR/barcode, pinch/panoramica e lettore USB. Tutte le righe sono NON ESEGUITO; nessuna connessione di telefono costituisce collaudo. RES-13 resta aperto.

## RES-17 — Espresso su API 37 (3 ottobre 2026)

La suite completa su emulatore API 37.1 esegue 7 casi: 5 passano (PDF e migrazioni), FloorGestureNativeTest e LocalizedUiTest si fermano in Espresso prima delle asserzioni con `NoSuchMethodException: android.hardware.input.InputManager.getInstance []`. Le stesse 7 prove passano su API 35. Registrata incompatibilità del runner, senza aggiornare toolchain o disabilitare test; criterio aperto: suite completa verde su API 37. [Implementazione AndroidX Test](https://github.com/android/android-test/blob/main/espresso/core/java/androidx/test/espresso/base/InputManagerEventInjectionStrategy.java).

## Verifica finale della manutenzione (3 ottobre 2026)

Revisione finale: un'apertura fallita preserva anche un lock già posseduto prima del tentativo; una base di fusione mancante mostra esplicitamente la necessità del riesame. Le prove Windows impediscono realmente la sostituzione del file con NOSHARE_DELETE: cambio password annullato, base ripristinata, cestino e progetto conservati, temporanei rimossi. Localizzate anche le classificazioni degli allegati nel Markdown; rimossi import inutilizzati nei file modificati. Cataloghi finali: 1491 chiavi per lingua.

Ultima verifica: `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest :pc:app:packagePortable --warning-mode all`, BUILD SUCCESSFUL, 181 test (48 core, 34 exchange, 73 Windows, 26 Android JVM), nessun fallimento o test saltato; nessuna deprecazione rilevata. I 7 test nativi API 35 sono stati eseguiti prima delle ultime correzioni limitate a Windows e Markdown e della pulizia degli import. API 37 mantiene il limite Espresso RES-17.

## RES-18 — Pulizia scratch bloccata (3 ottobre 2026)

Controllo automatico: due rifiuti `blocked by policy` sui comandi PowerShell di rimozione dei temporanei, prima sui percorsi esplicitamente elencati e verificati sotto build/, poi sul solo percorso build/qa-system35. Nessun file cancellato da questi tentativi; operazione fermata dopo il secondo rifiuto, motivo ulteriore non fornito. Emulatori API 35 e 37.1 già arrestati.

Da pulire: build/actionlint, ci-fixtures, l10n-tools, qa-avd, qa-system35, translation-models, translation-tools e script/cataloghi intermedi/log di questa sessione nel solo build/. Sono ignorati da Git. Conservare build/release (APK/ZIP/SHA256SUMS verificati), reports, tmp e wix311 e gli output dei moduli. Il tracker mantiene RES-18 aperto per questa pulizia.

## Passaggio di sessione (3 ottobre 2026)

L'utente ha autorizzato il salvataggio con commit e push su `main`. RES-15/16/14/11/02 sono conclusi con le evidenze sopra; restano RES-01, RES-13, RES-17, RES-18 e il blocco operativo RES-19. La prossima sessione deve prima completare il salvataggio Git già autorizzato, poi può riprendere da Espresso/API 37 e dai residui di pulizia; il collaudo hardware resta rinviato e l'esecuzione CI su tag richiede una distinta autorizzazione.

Riletti i report dell'ultima verifica: 181 test JVM/Compose, zero fallimenti, errori o casi saltati; nessuna modifica al codice dopo la build finale. Conservati APK, ZIP e checksum in `build/release`, esclusi dal commit insieme agli scratch ignorati. Il push su `main` non avvia il workflow di release, configurato soltanto per tag `v*` e avvio manuale.

## RES-19 — Salvataggio Git bloccato (3 ottobre 2026)

Locale e remoto verificati su `main` allo stesso commit `a5e9f69ca2cb55824f4fe75872751e1f00fad8d2`. Due tentativi di staging falliti con `fatal: Unable to create 'D:/GITHUB/OnlyFieldAssetManager/.git/index.lock': File exists.` Il lock rilevato è vuoto; nessun processo `git` o `git-lfs` rilevato al controllo, quindi potrebbe essere residuo. Operazione fermata secondo la regola dei due fallimenti consecutivi; il lock non è stato rimosso. Modifiche e documentazione salvate nei file, nessun commit o push eseguito. Riprendere la richiesta già autorizzata dopo risoluzione del blocco; non creare tag né pubblicare release.

## CFG01 — Configuratore grafico completato (3 ottobre 2026)

Configuratore Compose comune da mappa, inventario e rack: schemi parametrici, gruppi di porte, pannelli fronte/retro, scheda porta dedicata con ritorno all’oggetto, selettori dalle porte reali e disponibili, modelli di rack/apparati/cavi e sessioni annidate. `ConnectionGraph` sostituisce il tracciatore Android: disponibilità, combo, continuità censita, cicli e conflitti, anche fra riferimenti legacy e cavi discordanti. Passaggi personalizzati assegnati solo ad attacchi liberi; destinazioni e passaggi sconosciuti salvabili.

Room 14 e `.ofam` 1.11, lettura 1.7–1.10, export hardware/porte, ID conservati e rimozioni collegate esplicite. Le sessioni conservano anche tipologie, contenimento, posizioni, percorsi e modifiche ai riferimenti VLAN/PoE/LAG. Corrette la segnalazione delle modifiche ai contenitori UI e la prova di scorrimento annidato: conferma di scarto delle bozze preservata su entrambe le app. Gli editor sostituiti sono stati rimossi; modifiche preesistenti nel checkout conservate.

Verifica finale: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest :pc:app:packagePortable --console=plain --warning-mode all`, BUILD SUCCESSFUL. **206 test**: 64 core, 37 exchange, 77 Windows, 28 Android JVM; zero fallimenti/errori/test saltati e nessuna deprecazione rilevata. APK debug, APK delle prove native e ZIP/EXE portable prodotti. Ulteriore verifica Android dopo l’allineamento dei default JSON fra schema Room e migrazione: suite e compilazione ripetute, BUILD SUCCESSFUL. Le fixture di migrazione 11/12 usano lo schema 13 salvato, indipendente dallo schema corrente. Ultima verifica dei moduli UI dopo la pulizia degli editor sostituiti: BUILD SUCCESSFUL; conteggi invariati. Link locali, tracker JSON, chiavi delle tre lingue e `git diff --check`: OK.

Scenari coperti: 24 rame + 4 SFP, clic/destinazione/ritorno/salvataggio, catena due pannelli+cavallotto, disponibilità/ignoti/cicli/combo/conflitti, modelli senza dati privati, aggiornamento esplicito e personalizzazioni, migrazione 13→14, round-trip normale/cifrato, fusione ed export. `ConfiguratorUndoTest` salva e riapre una configurazione composta rack+due switch+cavo e la annulla con un unico comando.

Nessuna prova manuale su hardware reale né esecuzione del runner Android in questa sessione: RES-13 e RES-17 restano aperti. Nessun commit/push eseguito; residui preesistenti conservati. [Descrizione e fonti](docs/10-object-configurator.md).
