# Test di Unità, Benchmarks e Volumi di Carico

Data: 3 ottobre 2026

## Suite di Test Unitari

L’ultima suite completa ha eseguito 166 test unitari/Compose sui quattro moduli. Le prove Android QA registrate sono 4: i 3 casi `FloorNativeTest` sono stati ripetuti per MAP03, mentre `FloorGestureNativeTest` resta una prova separata.

```
Ultima suite JVM/Compose: 166
├── :mobile:app        27
├── :shared:core       46
├── :shared:exchange   32
└── :pc:app            61
```

### Copertura dei Test per Modulo
- **`:shared:core` (`ModelValidatorTest`, `NewSiteWizardTest`, `CodeLookupTest`, `CoreModuleTest`)**: Validazione completa di tutte le entità, errori strutturali, avvisi documentali, sovrapposizioni slot rack, cicli di alimentazione, derivazione badge e passi della procedura «Nuovo sito».
- **`:shared:exchange` (`PackageSerializerTest`, `DocumentExportTest`, `FixtureTest`, `DeviceModelSerializerTest`, `PasswordHasherTest`, `ContractVersionTest`, `LabelSheetPdfTest`, `ProjectMergerTest`)**: Round-trip di pacchetti `.ofam` v1.7 liberi e cifrati AES-GCM, verifica checksum SHA-256, hash PBKDF2 delle password di progetto e migrazione degli hash SHA-256, test di integrità OpenXML XLSX, Markdown e fixtures sintetiche.
- **`:mobile:app` (`ProjectRepositoryTest`, `CartographicMapManagerTest`, `PilotBenchmarkTest`, `EncryptedDatabaseTest`, `ObjectMapStorageTest`)**: Persistenza Room, riconoscimento del DB in chiaro da convertire, migrazioni fino a v14, repository, base di sincronizzazione, numero di serie, contenimento, gestione tessere cartografiche offline e benchmark pilota.
- **`:pc:app` (`DesktopStorageTest`, `DesktopToolchainTest`, `DesktopDomainLogicTest`, `DesktopDocumentAndCartographyTest`, `BidirectionalInteropTest`, `MasterDetailTest`)**: Gestore storage desktop, pannello laterale degli editor (test UI Compose), salvataggio atomico, blocco `.lock`, password, interoperabilità bidirezionale Android ↔ Windows, UI rack elevation, cablaggio, rete logica, alimentazione A/B, badge documentali e cartografia.

## Verifiche di manutenzione (3 ottobre 2026)

- **RES-08:** baseline `:pc:app:test --tests '*MasterDetailTest'` verde; 4 scenari finali: chiusura, editor con titolo identico, inventario reale (cambio elemento/creazione/scarto sullo stesso elemento/salvataggio) e tab interne. La bozza resta finché si conferma lo scarto; i campi nascosti restano al salvataggio.
- **MAP01:** `DesktopMapDownloadTest`, 5 casi: mosaico e attribuzione, tessera non valida, HTTP 503 e servizio irraggiungibile, coordinate limite e zoom, riapertura offline e trasferimento cifrato con byte e metadati invariati. Le risposte di rete dei test sono servite localmente; nessun dato di prova nel codice di produzione.
- **Download reale:** nove tessere OpenTopoMap a Roma (12,4964; 41,9028; zoom 15), PNG 768 × 800, 645.178 byte; immagine e attribuzione ispezionate. Script e immagine temporanei rimossi dopo la verifica.
- **Build:** suite dei quattro moduli, APK debug e pacchetto portable Windows compilati con successo. Il collaudo manuale dell'interazione cartografica nell'eseguibile Windows non è stato eseguito; verificati backend reale, persistenza, scambio e compilazione UI.
- **Residui attuali:** RES-13 per prove hardware rinviate, RES-01 per CI su tag, RES-17 per Espresso/API 37 e RES-18 per pulizia scratch bloccata; RES-11, RES-14 e RES-02 chiusi nelle verifiche successive.

## MAP02: verifiche della navigazione e della mappa

- Baseline dei quattro moduli: BUILD SUCCESSFUL, 125 test già verdi; conservate le modifiche presenti nel checkout.
- `NewSiteWizardTest`: elenchi multipli, obblighi minimi, BU vuote aggiuntive, password facoltativa e ID conservati.
- `ObjectMapTest`: geometria con margini/zoom/panoramica, isolamento, 100 posizioni iniziali distinte, campi nascosti, cavi fuori piano, eliminazione di apparato/rack/porta e punti non validi.
- `ObjectMapStorageTest`: persistenza Room delle nuove proprietà e migrazione 11 → 12 su database precedente separato, senza perdita di BU/apparati/seriali.
- `ContractVersionTest` e `ObjectMapExchangeTest`: lettura 1.7/1.8, round-trip 1.9 cifrato, foto dei cavi, tipologie/percorsi nella fusione e rifiuto delle versioni successive.
- `FloorMapUiTest`: clic, trascinamento con un salvataggio al rilascio, cavi selezionabili e punti liberi; aggiunte successive di BU/piani, ritorno agli elenchi, isolamento, ricerca/annullamento del catalogo e credenziali mascherate Windows.
- `FloorMediaTest`: PDF con proporzioni opposte, file illeggibile/protetto, pagina fuori intervallo, singolo PDF condiviso fra piani, sfondo rimosso senza perdita di coordinate, riapertura delle foto, rollback dei file se una seconda foto fallisce e foto dei cavi nello scambio cifrato.
- Telefono Android API 36, app QA separata: `FloorNativeTest` (2 test) verifica PdfRenderer multipagina e SQLCipher con migrazione, backup cifrato e ripristino; `FloorGestureNativeTest` (1 test) verifica clic e trascinamento del canvas Android. Dati sintetici in un archivio isolato, applicazioni QA rimosse dal runner.
- Ultima suite e build: `:shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :pc:app:test :mobile:app:assembleDebug :pc:app:packagePortable`, BUILD SUCCESSFUL. Comandi strumentali in [06-release-and-delivery.md](06-release-and-delivery.md).
- Non verificati in questa consegna: scatto e QR con fotocamera reale, gesture multitouch manuali e lettore USB fisico. Le deprecazioni preesistenti della toolchain restano in RES-11.

## Benchmarks di Carico Pilota (`PilotBenchmarkTest`)

### Volume di Carico Testato
- 1 Business Unit con 100 Apparati di rete (2.400 porte complessive)
- 5 Armadi Rack (42U)
- 20 VLAN e 5 Gruppi Credenziali
- 50 Cavi di collegamento
- Sfondo cartografico offline con attribuzione

### Risultati delle Prestazioni Misurate
| Operazione | Tempo Rilevato | Soglia Richiesta | Esito |
| :--- | :--- | :--- | :--- |
| **Validazione Strutturale Modello** | ~18 ms | < 500 ms | Superato |
| **Esportazione Pacchetto Cifrato (.ofam v1.7)** | ~120 ms | < 2.000 ms | Superato |
| **Importazione e Decifratura Pacchetto** | ~110 ms | < 2.000 ms | Superato |
| **Ricerca Inventario (Nome, IP, Etichetta)** | ~15 ms | < 500 ms | Superato |
| **Memoria Heap Occupata** | < 45 MB | Non critico | Stabile |

## RES-10: protezione delle bozze Windows

`DetailChangeTest` verifica cancellazione dell’uscita, scarto con azione eseguita una sola volta e protezione di navigazione, undo, nuovo sito e chiusura. Suite Windows: 57 test, BUILD SUCCESSFUL; portable ricostruito.

## MAP03: contenitori, persistenza e mappa

- `ObjectHierarchyTest`: conversione di `rackId`, annidamento, cicli, spostamento della radice, distacco, eliminazione/ripristino con U e proiezione/aggregazione dei cavi interni ed esterni.
- `ContainmentExchangeTest` e `ContractVersionTest`: round-trip 1.11 cifrato, pacchetto 1.9 invariato all’import, conflitto di genitore sul figlio e rifiuto di un ciclo.
- `ObjectMapStorageTest`: migrazione Room 12 → 13, riapertura, conversione dell’associazione rack e conservazione del cestino durante salvataggi successivi.
- `ContainerUiTest` e `FloorMapUiTest`: navigazione multilivello, lettura dei figli aggiornati, assegnazione/rimozione, scarto di una bozza annidata, linea aggregata e scelta del cavo reale, incluso indicatore interno.
- Telefono Android API 36, app QA separata: `FloorNativeTest` ha superato 3 casi, compresi backup SQLCipher e ripristino delle basi 11 e 12 fino a Room 13. Il test di gesture precedente copre clic e trascinamento, non multitouch manuale.

Le prove manuali non eseguite restano RES-13: foto/QR da fotocamera reale, multitouch e lettore USB fisico.

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

Verifica: `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest :pc:app:packagePortable --warning-mode all`, BUILD SUCCESSFUL; 178 test (48 core, 34 exchange, 70 Windows, 26 Android JVM), zero fallimenti. Emulatore temporaneo API 35: suite completa via AndroidJUnitRunner, OK (7 tests), con estrazione del testo dei PDF nelle tre lingue e controllo dei segreti. API 37.1: PDF e migrazioni passano, due test UI si arrestano prima delle asserzioni per `InputManager.getInstance` in Espresso; residuo RES-17. Nessuna prova hardware manuale. [Dettagli](09-localization.md).

## RES-01 — CI implementata, criterio remoto aperto (3 ottobre 2026)

Workflow Windows su tag v* e avvio manuale, JDK 21/SDK 37.0, override del percorso JDK personale, suite prima di APK debug/ZIP portable/checksum. Azioni ufficiali fissate a SHA; permessi distinti, exit code espliciti, data/ rifiutata nello ZIP. Avvio manuale solo artefatti; pubblicazione su tag dopo verifica dei checksum.

Verifiche locali: actionlint 1.7.12 e PSScriptAnalyzer senza segnalazioni; script con APK/ZIP reali e SHA-256 validi; tre rifiuti attesi per APK/ZIP mancanti e ZIP contenente dati. Wrapper con override JDK: BUILD SUCCESSFUL. Non osservata alcuna esecuzione GitHub Actions su tag: RES-01 mantenuto PARTIAL. [Procedura e fonti](06-release-and-delivery.md).

## RES-13 — Checklist hardware pronta, collaudo rinviato (3 ottobre 2026)

Preparata [checklist hardware](testing/hardware-checklist.md) con dispositivo, sistema, versione app, scenario, esito ed evidenza per foto, QR/barcode, pinch/panoramica e lettore USB. Tutte le righe sono NON ESEGUITO; nessuna connessione di telefono costituisce collaudo. RES-13 resta aperto.

## RES-17 — Espresso su API 37 (3 ottobre 2026)

La suite completa su emulatore API 37.1 esegue 7 casi: 5 passano (PDF e migrazioni), FloorGestureNativeTest e LocalizedUiTest si fermano in Espresso prima delle asserzioni con `NoSuchMethodException: android.hardware.input.InputManager.getInstance []`. Le stesse 7 prove passano su API 35. Registrata incompatibilità del runner, senza aggiornare toolchain o disabilitare test; criterio aperto: suite completa verde su API 37. [Implementazione AndroidX Test](https://github.com/android/android-test/blob/main/espresso/core/java/androidx/test/espresso/base/InputManagerEventInjectionStrategy.java).

## Verifica finale della manutenzione (3 ottobre 2026)

Revisione finale: un'apertura fallita preserva anche un lock già posseduto prima del tentativo; una base di fusione mancante mostra esplicitamente la necessità del riesame. Le prove Windows impediscono realmente la sostituzione del file con NOSHARE_DELETE: cambio password annullato, base ripristinata, cestino e progetto conservati, temporanei rimossi. Localizzate anche le classificazioni degli allegati nel Markdown; rimossi import inutilizzati nei file modificati. Cataloghi finali: 1491 chiavi per lingua.

Ultima verifica: `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest :pc:app:packagePortable --warning-mode all`, BUILD SUCCESSFUL, 181 test (48 core, 34 exchange, 73 Windows, 26 Android JVM), nessun fallimento o test saltato; nessuna deprecazione rilevata. I 7 test nativi API 35 sono stati eseguiti prima delle ultime correzioni limitate a Windows e Markdown e della pulizia degli import. API 37 mantiene il limite Espresso RES-17.

## RES-18 — Pulizia scratch bloccata (3 ottobre 2026)

Controllo automatico: due rifiuti `blocked by policy` sui comandi PowerShell di rimozione dei temporanei, prima sui percorsi esplicitamente elencati e verificati sotto build/, poi sul solo percorso build/qa-system35. Nessun file cancellato da questi tentativi; operazione fermata dopo il secondo rifiuto, motivo ulteriore non fornito. Emulatori API 35 e 37.1 già arrestati.

Da pulire: build/actionlint, ci-fixtures, l10n-tools, qa-avd, qa-system35, translation-models, translation-tools e script/cataloghi intermedi/log di questa sessione nel solo build/. Sono ignorati da Git. Conservare build/release (APK/ZIP/SHA256SUMS verificati), reports, tmp e wix311 e gli output dei moduli. Il tracker mantiene RES-18 aperto per questa pulizia.

## Configuratore grafico (3 ottobre 2026)

`ConfiguratorTest`, `ConfiguratorExchangeTest` e `ConfiguratorStorageTest` coprono hardware, catene di connessione, combo, modelli, sessioni, esportazioni e migrazione Room 13 → 14. Suite core/exchange/Android JVM e build APK/portable riuscite. Le tre prove Compose del disegno/stato e del flusso clic → destinazione → ritorno → salvataggio passano; verificata anche la protezione delle bozze nel pannello Windows. Nessun collaudo manuale hardware nella sessione. [Evidenze e limite](10-object-configurator.md).
