# Verifica

Remediation corrente ed esiti sono nel [report del 9 ottobre](repo-residuals-2026-10-09.md); il [report precedente](repo-residuals-2026-10-05.md) conserva la baseline storica. Le evidenze di seguito distinguono prove automatiche/native e collaudi manuali ancora aperti nel [tracker](../PROJECT_STATUS.json).

## Suite automatica

La verifica JVM e Compose usa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest
```

Le suite coprono dominio, serializzazione/cifratura, fusione, storage Room, interoperabilita, configuratore, documenti e UI Desktop. Il workflow CI esegue lo stesso perimetro prima di creare gli artefatti.

Le regressioni dell'audit sono permanenti: `ImportedProtectionTest` verifica la riapertura su database Room su file e le regole di sostituzione/fusione; `AttachmentConfinementTest` verifica percorsi esterni, traversal e allegati legittimi; `MissingPayloadTest` verifica catalogo, checksum, alias legacy e pacchetti cifrati; `ImportPayloadReviewTest` verifica conferma e avviso visibile nella UI Desktop. `ProtectedMediaTest` copre import protetto, rendering da memoria, riapertura, password errata, cambio/rimozione password, protezione dei file esistenti e salvataggio di nuovi media senza copie temporanee. Le prove JVM non sostituiscono le prove native SQLCipher, ora eseguite, né la verifica visiva Android ancora aperta in RES-19.

## Progetto demo

[fixtures/demo/onlyfield-demo.ofam](../fixtures/demo/onlyfield-demo.ofam) si importa da Importa .ofam su Android e Windows. Si rigenera con:

```powershell
.\gradlew.bat :shared:exchange:demoPackage
```

Contenuto aggiornato il 9 ottobre 2026 («Demo Comune», **366 apparati, 8 rack, 1007 cavi, 14 modelli, 7 allegati**):

- **Comune – Municipio** (CED, Piano terra, Primo piano, Copertura, Laboratorio collaudi). Il rack CED è il centro stella. Contiene: patch panel fibra PPF-COM-CED (24 LC), SW-COM-CORE (24 RJ45 + 12 SFP+), FW-COM-01, RTR-COM-01, ONT-COM-01, due server, NAS, UPS e PDU sul retro. La catena WAN è ONT → router → firewall → core.
- **Teatro comunale** (Piano terra): ONT e router propri con linea FTTH e VPN verso il firewall del Municipio (collegamenti logici). Il cavo della presa PR-TEA-PT-01 passa per la scatola di giunzione GB-TEA-PT-01.
- **Scuola media** (Piano terra, Primo piano, Copertura): il rack del piano terra è il centro stella della scuola.
- **Scuola materna** (Piano terra, Copertura).
- **I sei piani operativi** hanno ciascuno un rack RK da 42 U con:
  - due patch panel da 48 RJ45 (PP-…-A/B), cablati per 3/4 (72 porte su 96) verso 36 prese PR da due porte;
  - due switch da 48 porte PoE con 4 SFP+;
  - sui piani con dorsale, un patch panel fibra da 12 LC.
- **Sulla mappa**, per piano operativo: le prese in quattro file e 16 apparati sulle prime prese (4 AP, 2 telecamere, 4 telefoni, 6 PC). Il piano terra del Municipio e il laboratorio hanno una planimetria PNG sintetica incorporata.
- **Dorsali**: gli switch dei piani salgono in fibra (cavi FO) al patch panel del centro stella e da lì al core. Nelle sedi a un solo rack lo switch B è collegato allo switch A con un DAC.
- **Ponti radio**, tutti di tipo Ponte radio con tratta radio tracciabile:
  - RAD-COM-01 sulla copertura del Municipio (alimentato da SW-COM-P1-A P40) verso RAD-MED-01 della Scuola media (tratta PR-COM-MED, 1200 m);
  - la media rilancia con RAD-MED-02 verso RAD-MAT-01 della materna (PR-MED-MAT, 450 m).

  I percorsi porta sono completi tra le sedi, ad esempio SW-COM-P1-A P40 → … → SW-MED-PT-A P40.

### Casistiche del laboratorio

Il laboratorio aggiunge dieci apparati e un rack senza cambiare i percorsi comunali esistenti. Gli avvisi sono intenzionali e documentali; il pacchetto non contiene errori strutturali.

| Caso | Dove provarlo | Contenuto atteso |
| --- | --- | --- |
| Ricerca e applicazione modello | Modelli, filtro `DEMO Switch` | 12 switch: 8/16/24/48 RJ45 × senza PoE/metà/tutte; oltre alle prime sette opzioni. Modelli anche per rack 12U e cavo rame arancione. |
| Layout e override PoE | `SW-COM-LAB` | 8 RJ45 + 4 SFP+, due righe ordinate 2/1/4/3/6/5/8/7; P3 con 802.3bt, P4 senza PoE. |
| Porta completa | `SW-COM-LAB P1` | Cavo arancione → `AP-COM-LAB`; sorgente e ricevitore PoE documentati. |
| Porta conflittuale | `SW-COM-LAB P2` | → `CAM-COM-LAB`; osservazione della porta discordante, avviso `PHYSICAL_CONNECTION_CONFLICT`, senza doppio cablaggio. |
| Estremità aperta | `SW-COM-LAB P3`, `APERTO-LAB-01` | Porta scollegata da verificare, estremità B assente, avviso `DETACHED_CABLE_ENDPOINT`. |
| Passante incompleto | `SW-COM-LAB P6` | → fronte `PP-COM-LAB P1`; retro non cablato. |
| Porta libera | `SW-COM-LAB P7` | Disponibile per aggiungere una connessione; nessun cavo. |
| Dorsale ottica | `SW-COM-LAB X1` | AOC verso `SW-COM-CORE X5`; distinto da fibra, DAC e radio già presenti. |
| Rack e gerarchia | `RK-COM-LAB`, `ARM-COM-LAB` | Rack 12U con numerazione dall'alto; armadio → cassetta → alimentatore/sensore, due livelli di mappe interne. |
| Rete logica | VLAN e configurazione `SW-COM-LAB` | VLAN 10/20/30/90, quattro subnet/interfacce, porte access/trunk, LACP core/server, testo configurazione sintetico. |
| Video e alimentazione | `CAM-COM-LAB`, `NVR-COM-LAB`, `SRV-COM-01` | Canale NVR; due ingressi A/B del server; UPS → PDU, autonomia rilevata sintetica; alimentazione singola NVR con avviso di copertura parziale. |
| Rilievi e stati operativi | Inventario e filtri | Verificato/da verificare/conflitto/non rilevato; in servizio/spento/dismesso/da verificare. Il sensore non è rilevato; gli stati operativi dei tre apparati precedenti restano disponibili. |
| Foto/allegati e filtri documentali | Progetto, area, rack, apparato, cavo e porta | Sette PNG con payload/checksum, tutti i sei target; quattro condivisibili, uno riservato, due da revisionare. Campi extra condivisibile/riservato e annotazione da revisionare. |
| Mappa e percorso disegnato | Laboratorio, cavo `AP-COM-LAB-01` | Planimetria, annotazioni e cavo con due curve; mappa densa del Municipio per zoom/pan/selezione. |
| Navigazione e restyling | Allegato di progetto `Guida-demo` | Sei percorsi di prova: mappa densa, Espandi/Riduci e contesto conservato, mappe interne, configuratore 8/48 porte, percorsi fisici e moduli. Guida e PNG nella palette blu/turchese attuale. |

I PNG sono illustrazioni di prova, non fotografie o rilievi architettonici reali. `DemoMedia` li genera in memoria con il JDK, senza dipendenze nuove; fonte primaria consultata l'8 ottobre: [Java ImageIO](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageIO.html). Il seed usa il timestamp sintetico fisso della revisione, `2026-10-09T00:00:00Z`, per progetto, rilievi ed export; mantiene l'ID del progetto demo, mentre le altre entità vengono rigenerate. L'importazione nella propria copia locale segue il normale confronto/sostituzione dell'app.

Il progetto è costruito con le stesse bozze delle app (`DemoSeed`, nei test di `:shared:exchange`). Il comando di generazione valida la struttura e reimporta in memoria il pacchetto completo prima di scriverlo. `DemoSeedTest` verifica anche il file generato, immagini decodificabili, classificazioni, applicazione modello senza cambiare ID porte/cavi e scambio semplice/cifrato con checksum.

Verifica dell'8 ottobre: baseline 13 prove demo verdi; dopo l'ampliamento **18 prove demo + una prova PDF Desktop**, zero fallimenti/errori/saltati. Generazione `BUILD SUCCESSFUL in 6s`; prove demo e `:mobile:app:assembleDebugAndroidTest` `BUILD SUCCESSFUL in 45s`; PDF `BUILD SUCCESSFUL in 12s`. APK test compilato con fixture aggiornata; prove native Android di importazione non rieseguite. Fonte per il filtro mirato: [Gradle JVM testing](https://docs.gradle.org/current/userguide/java_testing.html). Report locali in `build/reports/demo-seed-20261008`; pacchetto 489.823 byte, SHA-256 `fabda66be2d7545a1357b8f3a2af14b2742b9e35b9fd6c557eacbe56eeb4dc33`. Anteprime sintetiche ispezionate; demo installata, dati e backup conservati. RES-13/19/23/24 restano aperti.

Verifica del 9 ottobre: baseline **18 prove demo** verdi (`BUILD SUCCESSFUL in 47s`); generazione `BUILD SUCCESSFUL in 7s`; finale `.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1` → `BUILD SUCCESSFUL in 39s`, **18 prove**, zero fallimenti/errori/saltati. Coperti struttura, scambio semplice/cifrato, media/checksum, percorsi, topologia e XLSX. Guida estratta dal pacchetto e ispezionata, testi senza tagli. Pacchetto aggiornato: 529.332 byte, SHA-256 `c1ee0a6b1d7f2f2eabb9ddd106d619fd53f81c31f839e44577f295b245dc603a`; report in `build/reports/demo-seed-20261009`. Aggiornata solo la fixture importabile: nessuna reimportazione nelle copie Android/Windows installate. Collaudo nativo della nuova revisione non eseguito; RES-13/19/23/24 restano aperti.

## Limiti noti

Gli audit del 9 ottobre e le successive remediation sono nel [report repository](repo-residuals-2026-10-09.md): AUD-45–64 completati; restano RES-13/19/23/24. Le prove iniziali seguenti sono storiche. Verifica mirata su dati sintetici: 7 probe JUnit che confermano difetti e 5 prove esistenti, zero errori/fallimenti/saltati; inoltre probe Gradle Sync. Un probe verde non dimostra la correzione. Evidenze in `build/reports/repo-audit-20261009`; nessuna suite generale o nuova qualificazione nativa eseguita. Prima della chiusura RES-19/23 completare le correzioni pertinenti indicate nelle dipendenze del tracker.

- Il collaudo su telefono, multitouch, fotocamera integrata e scansione tramite fotocamera richiede hardware reale (lettori USB e scanner esterni esclusi per decisione utente): [checklist](testing/hardware-checklist.md).
- RES-17 chiuso: suite nativa API 37 verde con Espresso 3.7.0; la checklist hardware e visiva rimane distinta.
- La pubblicazione su tag è verificata dalla release v1.0.1 (run 37238450362).

Le prove non eseguite non sono considerate superate.

## Ripresa della pulizia RES-24 — 5 ottobre 2026

Conservate e verificate con SHA-256 le 19 evidenze di `build/task-verification` in `build/reports/session-2026-10-05-cleanup/previous-verification`; rimossi 17 file e la sottocartella `pdf`. Sul moto g86 copiate nei report e poi rimosse le sette prove PDF e `/sdcard/ofam-task-final.png`; `adb -s ZY32LNCB8C uninstall com.onlyfield.assetmanager.test` ha restituito `Success`. Verifica finale: solo il pacchetto principale installato e cartella esterna dei PDF vuota. SHA-256 del backup pre-aggiornamento invariato (`c62fc20ebda6e438f175857c769c5600b7fc9da06b6f82ce6fa39d2794c6c418`).

RES-24 resta parziale: due log sono mantenuti aperti da `emulator-5554`, avviato nella sessione precedente. L'emulatore non è stato terminato dalla nuova sessione; la cartella si potrà rimuovere dopo la sua chiusura. Nessuna suite ripetuta per questa pulizia. Comandi ADB verificati sulla [documentazione ufficiale](https://developer.android.com/tools/adb).

## Import grandi RES-22 / AUD-10 — 5 ottobre 2026

Misure iniziali su Windows e moto g86 hanno riprodotto `OutOfMemoryError` con un pacchetto valido da 511 MiB decompressi sul telefono. La correzione autorizzata mantiene limiti e `.ofam` 1; lettura, estrazione ed export elaborano un allegato per volta, con deposito temporaneo cifrato e chiusura esplicita del pacchetto. Metodo, baseline e comandi in [report import grandi](testing/import-benchmark-2026-10-05.md). `StagedPayloadTest`: cinque regressioni superate su cifratura e rilascio, manifest invalido, tag alterato, interruzione e preservazione del file preesistente in caso di staging corrotto.

## Aggiornamento SQLCipher (AUD-03) — 5 ottobre 2026

AUD-03 è ora chiuso: `EncryptedSchemaUpgradeTest` usa lo schema storico del commit `f17d8a7`, ricostruito nel database SQLCipher isolato, e il percorso reale `EncryptedDatabase.open`. Upgrade v1 → v2, eliminazione delle tabelle precedenti, downgrade 14 → 2, riapertura a versione invariata e import completo verificati sul moto g86 API 36: 4 test superati. L'import esplicito nel database applicativo vuoto e la riapertura di «Demo Comune» hanno superato una quinta esecuzione dedicata. Emulatore API 37: tre scenari di versione superati (`BUILD SUCCESSFUL in 18s`). La prova JVM resta distinta da SQLCipher nativo; dettagli, backup e comandi in [roadmap](../roadmap.md).

## Completezza PDF Android (RES-21) — 5 ottobre 2026

Quattro regressioni native `CompositePdfTest` su Pixel 9, Android 17/API 37: 100 apparati, rack da 60 U e lista completa, 75 allegati con attribuzioni, nota più lunga di una pagina, sei sezioni, filtri, selezioni indipendenti e lingue it/en/es. Credenziali e allegati riservati esclusi. La prova iniziale ha riprodotto quattro fallimenti nel vecchio generatore; finale `:mobile:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.onlyfield.assetmanager.CompositePdfTest --no-parallel --max-workers=1`, con `ANDROID_SERIAL=emulator-5554`: `BUILD SUCCESSFUL in 13s`, 4 test senza fallimenti/errori/saltati. `LocalizedPdfTest` eseguito anche separatamente: superato con Room e PDF nativi nelle tre lingue.

Sette PDF generati nell'emulatore, 46 pagine renderizzate e ispezionate in `mobile/app/build/reports/pdf-native/`: report completo 21 pagine, sole note/allegati 6, rack completo 3, oltre a filtri e lingue. Il lettore nativo può frammentare il testo in singoli glifi e inserire spazi negli identificativi: le asserzioni ricompongono i frammenti senza perdere i controlli su ogni codice. RES-21 chiuso; la stampa fisica e i suoi dialoghi restano in RES-19.

Fonti ufficiali consultate: [PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument), [StaticLayout](https://developer.android.com/reference/android/text/StaticLayout), [PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer).

## Revisione UI/UX — 4 ottobre 2026

Baseline prima delle modifiche: `:pc:app:test` con filtri `*ConfiguratorUiTest`, `*ContainerUiTest`, `*LocalizedUiTest`: 7 test superati. Verifica finale:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:assemble :mobile:app:assembleDebug :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:compileDebugAndroidTestKotlin
```

Esito reale: `BUILD SUCCESSFUL in 34s`; 214 test, nessun fallimento né test saltato. Compilate entrambe le app e la sorgente dei test strumentali Android; la compilazione non equivale alla loro esecuzione.

| Suite | Test | Fallimenti | Saltati |
| --- | ---: | ---: | ---: |
| Core | 64 | 0 | 0 |
| Exchange | 37 | 0 | 0 |
| Desktop | 85 | 0 | 0 |
| Android JVM | 28 | 0 | 0 |

La suite è stata ampliata dopo i problemi emersi durante l'integrazione. Tre aspettative obsolete di `MasterDetailTest` sono state aggiornate alle azioni Annulla modifiche e Salva modifiche; nessun controllo è stato disabilitato. I sei scenari di `ConfiguratorUxTest` coprono creazione essenziale, scelta esplicita della sede, valori conservati dopo chiusura dei dettagli, errori che aprono la sezione, applicazione e ricerca dei modelli, riduzione di gruppi collegati con consenso e conservazione degli ID.

`DesktopUxLayoutTest` esegue navigazione, protezione della bozza, creazione e accesso diretto alle porte a 1360×860 e 1024×768. Sei catture Compose Desktop sono in `pc/app/build/reports/ux/`: navigazione, editor e porte per entrambe le dimensioni. Catture ispezionate: titolo/footer riconoscibili, elenco selezionato evidente e sezione Porte raggiunta senza scorrimento manuale. Sono rendering del test Compose, non screenshot della finestra nativa con differenti scale Windows.

Le prove del configuratore condiviso a 360 e 412 dp coprono interazioni e conservazione dei valori nel renderer Desktop. **Non verificati su Android nativo**: tastiera aperta, testo ingrandito, inset, temi chiaro/scuro e TalkBack. `adb devices` non elenca dispositivi collegati nella sessione. La matrice resta aperta come RES-19 nella [checklist](testing/hardware-checklist.md); RES-17 e gli altri residui preesistenti sono conservati.

## Mappa, contenitori e preset — 4 ottobre 2026

Baseline prima delle modifiche: `:shared:core:test :shared:exchange:test :pc:app:test` verde. Verifica finale con lo stesso comando della revisione UI/UX e `--rerun-tasks`: `BUILD SUCCESSFUL in 1m 35s`, 224 test, nessun fallimento né test saltato.

Correzioni dopo la revisione del codice (selezione porte, gesti della mappa, accessibilità): nuovi controlli su porte inesistenti in `PresetAndPortLogicTest`, selezione stabile dopo modifica della bozza in `ConfiguratorUiTest`, pressione prolungata e presa in `FloorMapUiTest`. `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest` verde, 225 test; APK debug, Desktop e test strumentali compilati.

| Suite | Test | Fallimenti | Saltati |
| --- | ---: | ---: | ---: |
| Core | 75 | 0 | 0 |
| Exchange | 37 | 0 | 0 |
| Desktop | 84 | 0 | 0 |
| Android JVM | 28 | 0 | 0 |

Nuovi test:

- `MapSceneTest`: radici e una linea per coppia, cavi interni, percorso rettilineo e vecchio punto centrale ignorato, estremità fuori vista, ordine per U nei rack, glifi univoci, porte del sottoalbero.
- `PresetAndPortLogicTest`: tutte le combinazioni dei preset producono gruppi validi; switch 48+4 con PoE su metà porte; patch panel accoppiato; numerazione continua; VLAN e PoE multipli; subnet ricavata.
- `MapNavigationUiTest`: apertura dei contenitori livello per livello, rimozione dal contenitore, assegnazione di un oggetto esistente.
- `ConfiguratorUiTest`: aggiornato al pannello porte, con un nuovo scenario preset → selezione multipla → VLAN.

`FloorMapUiTest` è adattato a `MapWorkspace` e ai collegamenti ad arco. `ContainerUiTest` è stato rimosso insieme al vecchio `ContainerBrowser`; la copertura passa a `MapNavigationUiTest`.

Problemi emersi e corretti durante l'integrazione:

- il movimento che superava la soglia di trascinamento veniva consumato senza spostare l'oggetto;
- un lambda `if … else { … }` passato come pressione prolungata;
- clic dei test fuori area dopo lo scorrimento del pannello.

I test di mappa e configuratore sono stati rieseguiti due volte con `--rerun`, senza instabilità.

Catture Compose Desktop (renderer dei test, non finestra nativa) ispezionate a 1280×760 e 400×820 dp: mappa del piano con legenda, apertura del rack con pannello in basso, preset e pannello porte. Durante l'ispezione sono emersi i collegamenti collineari sovrapposti, corretti con archi annidati, e la barra superiore su telefono, ora su due righe. `FloorGestureNativeTest` è aggiornato e compilato, ma **non eseguito** su dispositivo: RES-17/RES-19.

## Pannello, inserimento e configuratore — 4 ottobre 2026

Baseline prima delle modifiche: `:shared:core:test :pc:app:test` verde. Ogni fase è stata verificata prima del commit. Verifica finale: `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:compileDebugAndroidTestKotlin` verde, `git diff --check` senza errori.

| Suite | Test | Fallimenti | Saltati |
| --- | ---: | ---: | ---: |
| Core | 78 | 0 | 0 |
| Exchange | 37 | 0 | 0 |
| Desktop | 91 | 0 | 0 |
| Android JVM | 28 | 0 | 0 |

Nuovi test:

- `ObjectSummaryTest`: identificativi solo se compilati e con etichetta, posizione dal piano ai contenitori, U e lato nel rack.
- `ObjectPickerUiTest`: sottotitolo con posizione e passo, passo porte con Continua, Indietro e Configura le porte manualmente; cavi esclusi dentro un contenitore; tipologia personalizzata creata dal pulsante della finestra.
- `ConfiguratorLayoutTest`: riga di contesto, riepilogo delle sezioni chiuse, sezioni dell'host prima di Campi personalizzati e Opzioni avanzate, contenuto rack con sole U occupate e intervalli liberi, `unitRanges`.

Test aggiornati: `FloorMapUiTest` (Aggiungi oggetto, Annulla, Modifica cavo), `MapNavigationUiTest` (Rimuovi dal contenitore dal menu Altre azioni; Assegna esistente apre la finestra con ricerca), `ConfiguratorUiTest`, `ConfiguratorUxTest` e `DesktopUxLayoutTest` (i campi a scelta si cercano per descrizione "etichetta: valore"). Nessun controllo disabilitato.

Problema emerso e corretto: Tipologia personalizzata in fondo all'elenco lazy non era raggiungibile senza scorrere; ora resta fissa sotto l'elenco. **Non verificato su Android nativo**: TalkBack sul pannello e sul menu Altre azioni, testo ingrandito, 360/412 dp. Resta in RES-19.

Prova su Android nativo (moto g86 5G, Android 16, APK debug installato con `adb install -r`, dati conservati) in un progetto separato «Test rack fibra»: due rack inseriti (uno dal pulsante, uno con pressione prolungata), uno switch per rack con preset 24 RJ45 + 4 SFP+ da Aggiungi qui, fibra X1 ⟷ X1 dalla scheda porta. Verificati sottotitolo del picker, azione primaria e menu Altre azioni nel pannello, porte occupate 1/28, tratto in fibra tra i rack e scheda cavo con estremità A/B e Modifica cavo. Corretti durante la prova: intestazioni di sezione con la prima lettera tagliata e contenuto rack vuoto per dispositivi senza posizione U (caso aggiunto a `ConfiguratorLayoutTest`). Difetti non corretti registrati come F10. Non verificati TalkBack, testo ingrandito, tastiera e tema chiaro: RES-19 resta aperto.

## Restyling UI/UX (UX-R1…R8)

Baseline prima delle modifiche: `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest` verde. Stesso comando verde dopo ogni passo.

Nuovi test:

- `ConfiguratorTest.portGridFitsWidthAndWrapsInBalancedBands`;
- `QuickAddTest`: switch da preset con porte e posizione sulla mappa, rack vuoto con altezza scelta, sede obbligatoria finché non scelta;
- `ObjectPickerUiTest` riscritto: preset con Aggiungi immediato, tipo senza menu con un tocco, tipologia personalizzata.

`DesktopUxLayoutTest` e `ConfiguratorLayoutTest` sono stati aggiornati al nuovo flusso e alla sezione Altro. Totale 247 test (Core 88, Exchange 37, Desktop 94, Android JVM 28).

Controllo visivo:

- **Desktop**: render Compose a larghezza telefono di icone, mappa chiara e scura, pannello porte da 48, elevazione rack e finestra di inserimento.
- **Android**: emulatore Pixel 9 con APK debug. Verificati barra in basso, Altro, mappa con bordo e griglia, inserimento rapido di rack e switch in U42, elevazione e porte a tutta larghezza. Corretti durante la prova i pulsanti segmentati ancora tondi e uno switch da 24 porte diviso in due fasce.
- **Da fare**: la prova sul telefono reale rientra in RES-19.

## Tracciamento fisico e scheda rapida delle porte — 5 ottobre 2026

Baseline prima delle modifiche: `gradlew test` verde. Verifica finale con `gradlew test --rerun`: `BUILD SUCCESSFUL in 34s`, 249 test superati (Core 90, Exchange 40, Desktop 95, Android JVM 24), nessun fallimento; test strumentali Android compilati, APK debug e portable generati.

- **Core**: inserimento di una scatola di giunzione in un cavo con percorso completo, scollegamento, cestino senza orfani e ripristino dei passaggi, riepilogo porta nei due versi ed etichetta proposta.
- **Exchange**: versione "1" in export, rifiuto dei pacchetti 1.11, demo con giunzioni a percorso completo.
- **Desktop**: collegamento dalla scheda rapida nell'editor; collegamenti logici in Altri dettagli. Render del demo ispezionato (pannello mappa, porta collegata e libera, inserimento passaggio).
- **Da fare**: scheda rapida su Android nativo e foto porta/cavo (RES-19, FOTO-04 in RES-13).

Ponte radio e «Demo Comune»: `gradlew test --rerun` `BUILD SUCCESSFUL in 1m 16s`, 250 test superati (Core 90, Exchange 41, Desktop 95, Android JVM 24). `DemoSeedTest` verifica sedi, 3/4 delle porte cablate per piano, percorsi radio Municipio → Scuola media → Scuola materna, dorsali, catena WAN e giunzione del Teatro.

## Regressioni filtri documentali (AUD-08)

`DocumentSelectionTest` verifica selezione con piano ereditato, cataloghi rete/alimentazione/allegati, classificazione della planimetria e contesto esterno; genera Markdown e XLSX per sede, piano, categoria e combinazione. `FilteredReportTest` legge il PDF con PDFBox e verifica figure e topologia con gli stessi filtri. Suite completa: `BUILD SUCCESSFUL in 1m 23s`, Core 100, Exchange 56, Desktop 114, Android JVM 32: 302 test senza fallimenti/errori/saltati. PDF Android compilato, completezza e resa non verificate (RES-21).

## Limiti pacchetto (AUD-06)

Sei test `PackageImportLimitsTest`: dimensione ZIP (compresi dati finali), dimensione entry, totale decompresso, numero entry/directory, file altamente comprimibile oltre 32 MiB, limiti esatti validi in chiaro/cifrati, KDF fuori intervallo e salt/IV malformati, stream fermato al primo byte compresso eccedente. Suite Exchange/Desktop/Android JVM: `BUILD SUCCESSFUL in 1m 22s`, 62/114/32 test senza fallimenti/errori/saltati. Memoria e tempi vicini ai limiti da misurare su hardware (RES-22).

Revisione conclusiva: aggiunta la settima regressione per un nome ZIP con UTF-8 malformato, che prima causava `IllegalArgumentException`; ora restituisce `INVALID_ZIP_ARCHIVE` strutturale. `:shared:exchange:test --tests '*PackageImportLimitsTest' --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 3s`, sette test superati.

## Sostituzione con password diverse (AUD-09)

Quattro test `ReplacementPasswordTest`: copia chiusa con richiesta, password errata/annullamento/riapertura (incoming protetto e non protetto), copia già aperta, cestino corrotto, fallimento atomico reale Windows con `NOSHARE_DELETE`. Conservati cestino e payload delle foto eliminate. Regressioni con media/cestino: `BUILD SUCCESSFUL in 15s`; suite Desktop completa `:pc:app:test --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 32s`, 118 test senza fallimenti/errori/saltati.

## Operazioni fuori dal thread UI (AUD-07)

`RepositoryDispatchTest`: tre prove sul worker iniettato (letture, output, persistenza, fallimento di scrittura e cancellazione propagata). `StoredMetadataTest`: due prove di lettura del solo manifest, nome del progetto protetto e archivio corrotto visibile. `DesktopIoTest`: tre prove AWT con worker reale, eventi distribuiti durante l’attesa, modifica concorrente bloccata, salvataggio cifrato di 500 apparati e errore di sostituzione Windows senza perdita dello stato/undo.

Suite finale `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 1m 26s`, Core 100, Exchange 62, Desktop 123, Android JVM 35: **320 test**, zero fallimenti/errori/saltati. Pannello Windows e dialoghi di stampa nativi non verificati a vista (RES-23/RES-19); consumi ai limiti restano RES-22.

Verifica dopo la revisione finale con lo stesso comando: `BUILD SUCCESSFUL in 1m 24s`, Core 100, Exchange 63, Desktop 123, Android JVM 35: **321 test**, zero fallimenti/errori/saltati. `git diff --check` superato; 15 documenti UTF-8 senza BOM e 72 collegamenti locali verificati. Il tracker contiene solo AUD-03 e sette residui aperti o parziali.

## Configuratore visivo — 5 ottobre 2026

Baseline mirata: `ObjectMapTest`, `PresetAndPortLogicTest`, `ConfiguratorTest`, `BulkCablingTest`, 40 test superati (`BUILD SUCCESSFUL in 2s`). Verifiche ampliate dopo problemi emersi nei controlli UI.

- `VisualConfigurationTest` (8): riordino senza cambio di ID/cavi, dimensioni conservate, supporto PoE personalizzato, riuso del modello, riconciliazione dei gruppi rinominati, porte aggiunte/rimosse, retro liberi con frontali occupati, serie/continuazione, associazioni sconosciute o duplicate anche sul retro, JSON precedente e nuovo, fasce su larghezze diverse.
- `VisualHardwareExchangeTest` (1): export/import `.ofam` con disposizione, supporto PoE, porte e dati dimensionali conservati dal serializzatore comune alle due app.
- `VisualConfiguratorUiTest` (4): Save, annullamento protetto, PoE dal disegno, tratta fissa da frontale occupato. Verifiche preesistenti aggiornate per conferma visiva dell'inserimento e comandi fissi; sezioni rete aperte e identificativi richiudibili.
- `VisualLayoutTest` (4 combinazioni): switch 24/48 e patch panel 24/48; larghezze 360/560/1024 dp, temi chiaro/scuro, fronti e retro. `MapVisualBoundsTest` (1): 53 oggetti, zoom/trascinamento e assenza di disegno sul pannello esterno. PNG in `pc/app/build/reports/visual-configurator/`, ispezionati insieme ai render dell'editor e del picker in `pc/app/build/reports/ux/`.

Comando completo: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:compileKotlin :mobile:app:compileDebugKotlin --no-parallel --max-workers=1` → `BUILD SUCCESSFUL in 1m 35s`, **339 test** (108/64/132/35), zero fallimenti/errori/saltati. Compilazione delle due app riuscita.

Controllo conclusivo: `ObjectPickerUiTest` verifica anche la scelta esplicita della sede nel flusso reale dall’inventario senza contesto e con più sedi. `:pc:app:test :mobile:app:compileDebugKotlin --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 36s`, 133 test Desktop; totale verificato **340 test** (108/64/133/35). Render aggiornato della topologia in `visual-configurator/topology-current.png`.

I render Desktop a larghezza telefono non verificano il runtime Android: nuovi flussi, TalkBack, multitouch, trascinamento porte e foto su hardware restano RES-13/RES-19. Il problema Room AUD-03 non è modificato da questo intervento.

Controllo conclusivo delle immagini: `:pc:app:test --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 35s`, 133 test superati. Topologia aggiornata e retro con cavo attestato ispezionati nei PNG `visual-configurator/topology-current.png` e `visual-configurator/rear-560-false.png`. La cattura del dialogo dopo un’interazione non era affidabile nel renderer dei test; per la verifica grafica del retro cablato è stato usato il render isolato delle griglie. Le verifiche funzionali del dialogo restano superate.

## Suite nativa API 37 (RES-17) — 5 ottobre 2026

Espresso 3.6.1 chiamava per reflection `InputManager.getInstance`, assente su API 37; fallimento riprodotto prima delle asserzioni in `LocalizedUiTest` e `FloorGestureNativeTest`. Aggiornata soltanto la dipendenza esistente Espresso a 3.7.0, che usa `getSystemService`; nessun test disabilitato o controllo bypassato. Fonte: [note ufficiali AndroidX Test/Espresso 3.7.0](https://developer.android.com/jetpack/androidx/releases/test#espresso_3.7.0).

`ANDROID_SERIAL=emulator-5554` con `:mobile:app:connectedDebugAndroidTest --no-parallel --max-workers=1`: verifica conclusiva `BUILD SUCCESSFUL in 23s`, **13 test**, zero fallimenti/errori/saltati. Stessa suite tramite `adb -s ZY32LNCB8C shell am instrument -w -r com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner`: **13 test superati** in 8,947 s sul moto g86 API 36, senza attivare il parametro di reimport del database applicativo. Include PDF, SQLCipher, localizzazione e tocco/trascinamento nativo della mappa. La fixture storica ricrea anche gli indici; la lettura gestisce le tabelle che non ne dichiarano, correggendo un errore emerso nell'ultimo controllo.

Regressioni JVM complete dopo i problemi risolti: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 26s`, **340 test** (108/64/133/35), nessun fallimento/errore/saltato. RES-17 rimosso dal tracker. Hardware, matrice UX completa, TalkBack e stampa nativa rimangono in RES-13/RES-19/RES-23.

## Import grandi (AUD-10 / RES-22) — 5 ottobre 2026

L’OOM reale da 511 MiB sul moto g86 è corretto con lettura, estrazione ed export progressivi, cache limitata e staging temporaneo cifrato; limiti e `.ofam` v1 invariati. Sette fixture valide misurate su Windows e telefono, incluse ZIP/cifrato da 255 MiB, 10.000 entry e KDF massimo: persistenza SQLCipher e hash degli allegati superati. Collaudo EXE reale da 511 MiB completato e staging rimosso alla chiusura. Metodologia, risultati e fonti nel [report](testing/import-benchmark-2026-10-05.md).

Suite completa 345 test verdi, APK/portable generati; 14 test nativi verdi su API 36/37. Revisione risorse e rollback foto: `:shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 1m 38s`. AUD-10 e RES-22 rimossi dal tracker; hardware, matrice UX e stampa restano aperti.

## Chiusura AUD-11 e RES-20 — 5 ottobre 2026

Corretto «Esporta e apri»: filtro/suffisso del file originale, estensioni confrontate senza distinzione fra maiuscole e minuscole, nessun suffisso forzato per nomi senza estensione. Nessun cambiamento ai picker `.ofam` o dei documenti.

Collaudo autonomo nell’EXE Windows reale con progetto protetto sintetico: PNG esportato come `.PNG` e aperto in Foto; PDF aperto in Acrobat, tre pagine e SHA-256 identico all’originale. Annullamento del picker senza nuove scritture o variazioni degli hash; estensione non associata apre la scelta di app Windows, annullata senza blocco. Nessun PNG/PDF in `data/`; i pacchetti locali restano invariati. Gli errori interni di un viewer già avviato non vengono intercettati dall’app.

`:pc:app:test :pc:app:packagePortable :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → `BUILD SUCCESSFUL in 1m 16s`; 133 test Desktop e 35 Android JVM verdi. Evidenze locali in `build/reports/native-windows`; AUD-11 e RES-20 rimossi dal tracker. APK aggiornato sul moto g86 (`install -r`: `Success`), «Demo Comune» conservata.

## Prova autonoma Windows RES-23 — 5 ottobre 2026

Pannello occupato visibile e leggibile durante import/persistenza da 511 MiB. Dopo la correzione AUD-11, picker media e ritorno dai viewer verificati. Dialogo nativo di stampa aperto dal flusso Documenti → Stampa; annullato con il suo pulsante Annulla: stato «Stampa annullata», overlay rilasciato e hash dei pacchetti invariati. `Get-PrintJob` non mostra lavori nella coda; nessuna stampa fisica inviata. Catture in `build/reports/native-windows/print-dialog.png` e `print-cancelled.png`.

RES-23 rimane parziale: matrice completa dei comandi e del focus durante salvataggio ed errore nativo di stampa non verificati. Nessuna chiusura implicita dei collaudi hardware o della matrice Android.

## Pulizia conclusiva e residui — 5 ottobre 2026

- Verificati gli SHA-256 delle sette fixture sul moto g86; rimosse le sole fixture della cartella esterna `import-benchmark` e la cartella vuota. Copiati e verificati i sette PDF nativi di ciascun dispositivo in `build/reports/import-benchmark/ZY32LNCB8C` e `emulator-5554`, poi rimossi dai dispositivi. Disinstallati entrambi gli APK test (`Success`); resta installata l’app principale. APK finale sul moto g86 installato con `-r`: demo conservata. Backup pre-Room-v2 con SHA-256 ancora `c62fc20ebda6e438f175857c769c5600b7fc9da06b6f82ce6fa39d2794c6c418`.
- Chiusi app Windows e viewer avviati per le prove; staging vuoto. Verificati gli SHA-256 di tutti i 16 payload della working copy Windows rispetto alla fixture originale. Conservate quattro piccole evidenze riproducibili in `build/reports/native-windows/fixtures`: pacchetto viewer protetto con password fittizia, pacchetto da 511 MiB salvato (ZIP 526.567 byte), PNG e PDF esportati; report hash in `windows-persisted-payloads.txt`.
- La pulizia locale è stata respinta due volte dal controllo automatico con `blocked by policy`, senza motivazione più precisa: prima comando composto con controllo dei percorsi, poi `Remove-Item -LiteralPath` sui tre percorsi assoluti verificati. Tentativi interrotti. RES-24 registra `build/import-benchmark`, `build/native-window-app`, `build/native-window-exports` e i sei helper `complete-import-docs.py`, `complete-print-docs.py`, `complete-viewer-docs.py`, `finalize-import.py`, `trim-viewer-fixture.py`, `check-windows-persistence.py`. Sono tutti output ignorati. I due log di `build/task-verification` restano bloccati dall’emulatore della sessione precedente, non terminato.
- Tracker: completati e rimossi AUD-10, AUD-11, RES-20, RES-22. Restano RES-13, RES-19, RES-23 e RES-24; gli esiti non eseguiti sono espliciti. Nessun commit o push richiesto/eseguito.

AUD-12: `LocalImportComparisonTest` verifica copia B con nessun progetto/A/B aperto, incoming precedente, annullamento/conferma, password locale diversa e fusione chiusa senza cambiare A. `RepositoryDispatchTest` verifica la selezione esclusiva dell’ID incoming senza scritture durante Review. Verifica dei quattro moduli del 6 ottobre: 350 test, zero fallimenti/errori/saltati. Collaudo UI Android nativo ancora in RES-19.

AUD-13: `MediaAdditionTest` Desktop verifica 33 MiB, scrittura .ofam bloccata, rollback di cache/file/catalogo e riapertura con/senza password. La classe Android omonima verifica provider oltre limite, aggiunta successiva, fotocamera rifiutata, database chiuso e cancellazione dopo commit. `MediaCapacityTest` copre stream senza dimensione, aggregate/entry/ZIP, scambio riuscito e margine conservativo protetto (AUD-23). Verifica completa: 359 test verdi; ulteriore prova al limite ridotto verde. Foto fisiche e UI native restano in RES-13/19/23.

AUD-20: `MediaUiDispatchTest` verifica l’elenco senza richiesta dei byte, il loader delle anteprime su thread diverso dalla composizione e il controllo di disponibilità senza decifrare uno staging protetto. Tre prove mirate verdi in 5s. Verifica conclusiva dei quattro moduli del 6 ottobre: BUILD SUCCESSFUL in 1m 12s; 363 test nei report (108/73/142/40), zero fallimenti/errori/saltati. Core e Android aggiornati; Exchange e Desktop rieseguiti.

AUD-14: ReversibleFilesTest e ImportAtomicityTest Android/Desktop verificano errore in estrazione, scrittura progetto/base, rollback dei byte esistenti e dei nuovi file, cestino e verificatore Android invariati, fusione Windows di copie aperte/chiuse e retry riuscito. Backup grandi cifrati; errore di pulizia dopo commit restituito come avviso. Verifica mirata: BUILD SUCCESSFUL in 18s, 6 test verdi. Arresto improvviso tra file/database ancora da coprire (AUD-24).

AUD-15: ProjectCommandTest usa un executor Room controllato per edit ravvicinati, errore del primo save, edit+cestino+password, undo obsoleto/valido e chiusura/cambio durante il salvataggio. ProjectReadTransactionTest sospende la query delle sedi e avvia un save concorrente: load/export restituiscono solo la versione precedente completa, poi la nuova. Verifica mirata Android BUILD SUCCESSFUL in 14s, 9 test verdi inclusi media/import. La readiness della prova fotocamera ora attende anche busy=null dopo il caricamento del cestino. Dialogo occupato compilato; resa nativa, focus e TalkBack non verificati (RES-19).

Verifica conclusiva AUD-14/15: .\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1 → BUILD SUCCESSFUL in 2m 3s. Report: 373 test (108 Core, 76 Exchange, 144 Desktop, 45 Android), zero fallimenti/errori/saltati. Matrice Windows estesa a 16 combinazioni di copia aperta/chiusa, replace/merge, lock progetto/base, con/senza password; cestino e byte verificati prima/dopo e dopo retry. Nessun collaudo hardware, installazione, commit o push. Tracker: 12 attività (10 P2, 2 P3), ID e dipendenze validi; git diff --check superato.

Ultima verifica dopo il controllo dell'export accodato: ProjectCommandTest aggiunge password cambiata prima dell'export, rifiuto senza password prima di creare il file e export cifrato della versione corretta. Quattro test mirati verdi in 12s. Comando ordinario dei quattro moduli rieseguito: BUILD SUCCESSFUL in 32s (Android rieseguito, altri aggiornati); report finali 374 test (108/76/144/46), zero fallimenti/errori/saltati. Documenti senza riferimenti correnti ad AUD-14/15 aperti; tracker invariato a 12 attività, prossimo AUD-16. Validati 18 Markdown UTF-8 senza BOM e 130 collegamenti locali.

## AUD-16 — Parità e rollback apparati — 6 ottobre 2026

Baseline ProjectRepositoryTest: BUILD SUCCESSFUL in 1m 13s. La nuova regressione ha riprodotto positionU=null, heightU=1 e OUT_OF_RACK dopo sostituzione Android, invece di U12, altezza 3 e SHELF_MOUNT. DeviceOperationsTest verifica parità con ProjectEdits, figli del contenitore, cestino/ripristino, porte con e senza fusione, cavi da verificare e campi nascosti. Errori SQL iniettati durante inserimento del nuovo apparato e del cestino per sostituzione/fusione: intero progetto e verifier invariati, retry riuscito. Comando: .\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.DeviceOperationsTest --tests com.onlyfield.assetmanager.ProjectRepositoryTest --no-parallel --max-workers=1 → BUILD SUCCESSFUL in 27s. Collaudi nativi invariati; AUD-25 registra le opzioni di fusione non implementate nel core.

## AUD-17 — Stati, note e avvisi dei documenti — 6 ottobre 2026

Baseline mirata LocalizedExportsTest/DocumentSelectionTest/FilteredReportTest verde in 7s. DocumentSurveyTest ha riprodotto il falso Verificato nel Markdown prima della correzione. DocumentSurveyTest e SurveyReportTest coprono tutti gli stati, note di apparati/porte/cavi, conteggio criticità, filtri/classificazione, credenziali escluse, avvisi delle sole sezioni stampate, riferimenti originali ed estremi esterni dei percorsi; le schede rack Desktop conservano gli stati anche senza inventario. Un nuovo scenario ha richiesto reviewRequiredConfirmed=false esplicito perché il valore predefinito è true; assertion di esclusione conservate.

La prima esecuzione nativa ha rilevato l'assenza del rilievo nell'elenco rack Android. Corretto il generatore usando la stessa riga apparato dell'inventario. Emulatore Pixel_9 avviato da questa sessione sulla porta 5580, API 37, senza finestra né salvataggio snapshot; installati soltanto APK di test su questo emulatore. Progetti sintetici e database in memoria, nessuna apertura/modifica della demo o del telefono collegato.

Comandi eseguiti:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1
$adbPath = 'C:\Users\Utente\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adbPath -s emulator-5580 install -r mobile/app/build/outputs/apk/debug/app-debug.apk
& $adbPath -s emulator-5580 install -r mobile/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adbPath -s emulator-5580 shell am instrument -w -r -e class 'com.onlyfield.assetmanager.SurveyPdfTest,com.onlyfield.assetmanager.LocalizedPdfTest,com.onlyfield.assetmanager.CompositePdfTest' com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

Esiti: build APK e test APK riuscita; dopo l'ultima correzione APK ricompilato in 4s. Esecuzione nativa finale OK (7 tests), 1,889s, zero fallimenti: due nuove prove e cinque regressioni esistenti, incluse paginazione e sezioni indipendenti. Lo script controlla esplicitamente OK (7 tests), perché adb può uscire con codice zero anche quando JUnit fallisce. Fonti: [PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument) e [estrazione testo PdfRenderer.Page](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page), consultate il 6 ottobre 2026.

Suite JVM finale: BUILD SUCCESSFUL in 2m 13s; 381 test nei report (108 Core, 78 Exchange, 146 Desktop, 49 Android), zero fallimenti/errori/saltati. Nel comando finale Core riusa il risultato verde eseguito in questa sessione; gli altri tre moduli rieseguiti. Nessuna assertion disattivata, nessun nuovo mock o dipendenza. RES-13/19/23 restano collaudi hardware/UX/stampa distinti. AUD-16/17 rimossi dal tracker; nuovo residuo AUD-25 conservato. Nessun commit o push.

## AUD-18 — Ciclo di vita import Android — 6 ottobre 2026

Job conservato e identità per richiesta; annullamento/nuova richiesta/chiusura ViewModel cancellano lettura e fusione. Pacchetto posseduto dal worker fino al trasferimento alla Review, quindi dal comando di conferma/fusione fino al completamento. Risultati scartati al ritorno dal dispatcher vengono chiusi; il busy si libera anche se il comando viene cancellato prima di iniziare. Conferma e fusione consumano lo stato prima dell'avvio, impedendo doppi salvataggi e riuso di pacchetti chiusi. Il commit già iniziato resta non cancellabile, come in AUD-14.

Baseline Android BUILD SUCCESSFUL in 2s (risultati aggiornati). La nuova prova cancelledWorkerReturnClosesThePackageBeforeReviewReceivesOwnership sul ViewModel precedente riproduce una Review dopo cancelImport (fallimento atteso); sorgente corrente ripristinato subito dopo. Otto nuove prove permanenti: cancellazione lettura e PBKDF2 reale, doppio import protetto, chiusura durante confronto, ritorno worker prima della Review, conferma lenta/doppia, cancellazione prima del commit, fusione annullata/ripetuta. Payload e rimozione dello staging cifrato verificati; nessun mock della cifratura o dipendenza nuova.

Comando: .\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.ImportLifecycleTest --tests com.onlyfield.assetmanager.ImportAtomicityTest --tests com.onlyfield.assetmanager.ProjectCommandTest --no-parallel --max-workers=1 → BUILD SUCCESSFUL in 14s; 13 test, zero fallimenti/errori/saltati. Fonti consultate il 6 ottobre: [withContext e risorse restituite](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html), [lifecycle ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis). Documenti architettura/flussi/verifica aggiornati; AUD-18 rimosso con dipendenze. Restano 10 attività (8 P2, 2 P3). Nessun collaudo nativo, commit/push o modifica ai progetti reali.

## AUD-19 — Media attivi, undo/cestino e raccolta di proprietà — 6 ottobre 2026

Export selezionato prima della lettura: soli media attivi, esclusi orfani, foto degli oggetti nel cestino e metadati locali. Alias supportati conservati; date di revisione stabili e base del progetto inviato. Copia locale con undo Android a un passo e storia Windows a 50 revisioni; byte raccolti alla scadenza, chiusura/cambio/password o riapertura. Foto del cestino conservate con metadati anche nella sostituzione/fusione; allegati serializzati nel cestino storico mantenuti. Rimozione definitiva elimina foto di apparati/porte e invalida undo; cancellazione Android del progetto comprende base, righe e directory di proprietà. Backup di rollback grandi cifrati, UUID e percorsi confinati, nessun collegamento seguito verso sorgenti/altre copie. Cambio password Windows riusa il blocco reversibile esistente: rimosso il rollback manuale duplicato.

Baseline Windows media/protezione/cestino verde in 8s. Tre prove nuove hanno riprodotto payload rimosso esportato, lettura dell'orfano da 33 MiB e entry non catalogate. La precedente assertion interop che pretendeva tre payload a fronte di un solo record attivo è stata sostituita da catalogo esatto, hash del payload attivo ed esclusione esplicita dei due orfani. Nessuna assertion di integrità eliminata.

Diciassette nuove prove media (9 Windows, 8 Android): hash/undo/export, scadenza a 50 revisioni, chiusura, cestino/ripristino/eliminazione, metadati e byte attraverso sostituzione chiusa, nessun sito, allegato serializzato storico, orfani oltre limite, sorgenti e altri progetti intatti, SQL abort e file Windows NOSHARE_DELETE con rollback/retry. Corrette due fixture di prova: import con avvisi attraversa la conferma reale; allegati di progetti diversi hanno UUID distinti. La suite completa ha poi trovato quattro regressioni Windows, corrette preservando tombstone ATTACHMENT e evitando riscritture al cambio di copia quando non c'è recupero da raccogliere.

Il ripristino senza siti perdeva la voce senza ricreare l'apparato: ora errore localizzato it/en/es, cestino e media conservati su entrambe le app. AUD-26 registra scelta del sito quando quello originale manca e tipi non supportati; senza siti esistono regressioni esplicite, con sito esistente si verifica anche la presenza dell'apparato ripristinato. Nessuna ricostruzione o ricollocazione automatica aggiunta.

Verifica completa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 2m 40s: 406 test (108 Core, 78 Exchange, 155 Windows, 65 Android), zero fallimenti/errori/saltati; APK compilato. XML conservati in build/reports/aud18-19-full. Ultima verifica mirata dopo la conservazione della data di revisione nell'export e della base selezionata:

```powershell
.\gradlew.bat :pc:app:test --tests com.onlyfield.assetmanager.pc.MediaLifecycleTest --tests com.onlyfield.assetmanager.pc.ReplacementPasswordTest --tests com.onlyfield.assetmanager.pc.LocalImportComparisonTest --tests com.onlyfield.assetmanager.pc.BidirectionalInteropTest :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.MediaLifecycleTest --tests com.onlyfield.assetmanager.ProjectCommandTest --tests com.onlyfield.assetmanager.ImportLifecycleTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 27s, 40 test (20/20), APK finale ricompilato; nessun test aggiuntivo per i soli documenti. Fonti ufficiali Java Files e Android path traversal nella documentazione storage. AUD-18/19 rimossi dal tracker e dalle dipendenze; AUD-26 aggiunto. Restano 10 attività (8 P2, 2 P3), prossimo AUD-21. Recupero dopo arresto/secondo guasto ancora AUD-24; collaudi nativi RES-13/19/23 e risorse storiche RES-24 invariati. Nessuna installazione, modifica a demo/dati utente, commit o push.

Controllo conclusivo AUD-18/19: 10 ID aperti univoci e dipendenze valide; 18 Markdown UTF-8 senza BOM, 137 collegamenti locali validi e fine riga senza duplicazioni. git diff --check superato. Pulizia respinta due volte dal controllo automatico: blocked by policy, nessun dettaglio ulteriore; secondo tentativo limitato alla rimozione non ricorsiva delle sole cartelle vuote. Il compilatore ha eliminato autonomamente il marker; restano .kotlin e .kotlin/sessions vuote, registrate in RES-24. Tentativi interrotti; nessun processo terminato, risorse storiche e report conservati. Nessun commit/push.

## AUD-21 — Errori, callback e preferenze — 6 ottobre 2026

Cambio password Android: errori di scrittura/rimozione restituiti una sola volta al dialogo e segnalati, cancellazione rilanciata; chiusura/cambio progetto impedisce callback e successo tardivi. Verificati apertura con JSON danneggiato, password errata, riprova dopo errore, rollback di ripristino/rimozione/svuotamento cestino, cancellazione di comandi in coda e chiusura durante cambio password. Nessuna modifica di schema Room/API UI.

Windows: preferenze lette una volta all'avvio sul worker. Errore I/O o sintassi Properties non valida visibili, file conservato e valori predefiniti solo per avviare l'app. Tema e lingua cambiano dopo scrittura reversibile; proprietà estranee conservate. Errore di pulizia post-commit distinto da save fallito. Tre regressioni su file malformato, directory al posto del file e reale blocco Windows NOSHARE_DELETE con riprova e byte invariati. Messaggi it/en/es. Le nuove prove hanno fallito sul codice precedente: callback password assente (1/8 Android), errore tema/lettura invisibile (3/3 Windows). Corrette le cause; nessuna asserzione rimossa.

Baseline: `:mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.ProjectCommandTest :pc:app:test --tests com.onlyfield.assetmanager.pc.PasswordRotationTest --no-parallel --max-workers=1`, BUILD SUCCESSFUL in 11s. Prima correzione: 15 prove mirate verdi in 16s; aggiunte poi prove per rimozione password, cancellazione in coda, chiusura e apertura protetta.

Verifica finale:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 2m 28s: **415 test** (108 core, 78 exchange, 158 Windows, 71 Android), zero fallimenti/errori/saltati; APK compilato. XML completi conservati in `build/reports/aud21-full`. Fonti ufficiali Kotlin Cancellation e Java 21 Properties consultate il 6 ottobre e collegate nella documentazione architettura/storage. AUD-21 rimosso dal tracker e dalle dipendenze; collaudo messaggi/focus nativi esplicitato in RES-19/23. Nessuna installazione, modifica dati/demo/backup o pulizia di risorse storiche; nessun commit/push.

## AUD-23 — Capacità protetta prudenziale confermata — 6 ottobre 2026

Decisione esplicita dell'utente: mantenere il limite prudenziale e documentarlo. Nessuna richiesta o persistenza aggiuntiva della password, nessuna modifica al budget o al formato. Un'aggiunta vicino al limite ZIP può essere rifiutata anche se il pacchetto effettivo sarebbe ammissibile; il rollback conserva dati/media precedenti. Non viene promessa capacità esatta al confine. Decisione consolidata in plan.md, contratto e workflow; AUD-23 rimosso dal tracker.

Le quattro prove MediaCapacityTest sono comprese nella suite exchange di AUD-21 (78 test, zero errori): inclusi rifiuto file da 33 MiB, limiti aggregati, export/reimport protetto e margine prudenziale con limite ridotto e ZIP cifrato reale. Non eseguita una nuova prova reale a 256 MiB. La chiusura è per scelta del requisito, senza presentarla come verifica esatta. Fonti del budget: zlib compressBound e specifica ZIP PKWARE, collegate nel report residui e consultate il 6 ottobre. Nessun test ripetuto per i soli documenti.

## AUD-26 — Ripristino senza ricollocazione implicita — 6 ottobre 2026

Decisione esplicita: bloccare il ripristino se manca il sito originale e conservare nel cestino. DEVICE non usa più il primo sito disponibile; null/ID originale assente generano errore. RACK registra il sito del piano nelle nuove voci usando originalSiteId esistente e richiede contesto ancora disponibile; rack senza piano resta valido. Nessuna modifica Room v2 o formato .ofam v1.

DEVICE/RACK/CREDENTIAL condividono ProjectEdits.restoreFromTrash: controllo progetto/ID serializzato, rifiuto di ID dell'entità già presente nello stesso catalogo, nessuna sovrascrittura. Credenziali ripristinate integralmente anche su Windows. ATTACHMENT storico e tipi sconosciuti generano errore prima di rimuovere la voce, byte conservati. Android legge voce/progetto, salva e rimuove nella stessa transazione; voce assente restituisce false e la UI segnala errore. Windows verifica la presenza nel cestino e calcola il risultato prima di cambiare stato; salvataggio reversibile preesistente conservato.

Cinque regressioni core hanno fallito sul codice precedente: sito diverso, tipi non supportati, credenziale non ricreata, ID duplicato e metadati incoerenti. Aggiunta prova sul contesto rack. Le prove Android e Windows verificano sito assente/diverso, conservazione progetto/cestino/media e riprova quando torna il sito originale, ATTACHMENT storico conservato, credenziali/collisioni senza perdita di segreti. Windows copre anche copie protette e byte della working copy invariati. Una chiamata errata openStored(file, password) nelle nuove prove non compilava: corretta usando importFile(file, password, compare=false), ingresso reale già esistente; nessuna modifica delle API produttive.

Verifica mirata:

```powershell
.\gradlew.bat :shared:core:test --tests com.onlyfield.assetmanager.core.TrashRestoreTest --tests com.onlyfield.assetmanager.core.ObjectHierarchyTest --tests com.onlyfield.assetmanager.core.ConfiguratorTest :pc:app:test --tests com.onlyfield.assetmanager.pc.MediaLifecycleTest --tests com.onlyfield.assetmanager.pc.ProtectedTrashTest :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.MediaLifecycleTest --tests com.onlyfield.assetmanager.DeviceOperationsTest --tests com.onlyfield.assetmanager.ProjectCommandTest --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 48s, **68 test** (32 core, 13 Windows, 23 Android). Verifica completa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 2m 24s: **425 test** (114/78/160/73), zero fallimenti/errori/saltati; APK compilato. XML conservati in build/reports/aud26-full. Fonte primaria consultata il 6 ottobre: [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt); gli esiti di rollback sono evidenze dei test del repository. AUD-26 rimosso dal tracker e dalle dipendenze. AUD-27 registra da codice i riferimenti secondari: piani/collocazioni mancanti e porte con ID riutilizzati, non riprodotti su persistenza. Nessuna chiusura implicita di questi casi o dei collaudi nativi. Nessuna installazione, modifica dati/demo/backup o pulizia delle risorse storiche; nessun commit/push.

Controllo conclusivo AUD-21/23/26: tracker con 8 ID aperti univoci, dipendenze valide e nessuno dei tre task completati; 18 Markdown UTF-8 senza BOM, 137 collegamenti locali validi e nessun CRLF duplicato. git diff --check superato. .kotlin e .kotlin/sessions rimangono vuote come già tracciato in RES-24; nessun nuovo tentativo di rimozione o processo terminato. Evidenze conservate, modifiche preesistenti preservate; nessun commit/push.

## Checkpoint per cambio sessione — 6 ottobre 2026

Su richiesta esplicita, salvare con commit e push su main il lavoro presente di AUD-16/17/18/19/21/23/26, inclusi documenti e regressioni. Prima del commit main è il branch predefinito, allineato a origin/main dopo fetch. Il punto di ripresa è AUD-27; tracker con 8 attività aperte/parziali (6 P2, 2 P3), completamenti conservati nella roadmap.

Decisioni confermate: AUD-23 mantiene il limite prudenziale senza password aggiuntiva; AUD-26 blocca il ripristino quando manca il sito originale, conservando cestino/media. AUD-27 deve convalidare riferimenti secondari e collisioni degli ID delle porte; i suoi casi restano da riprodurre. AUD-24/25 richiedono politica di recupero/fusione; AUD-22 è pulizia runtime/test. RES-13/19/23 conservano i collaudi hardware/UX/stampa incompleti, RES-24 le risorse storiche e le cartelle vuote già tracciate.

Evidenze finali conservate: 425 test (114/78/160/73) senza fallimenti/errori/saltati e APK compilato, BUILD SUCCESSFUL in 2m 24s; 68 prove mirate verdi. Nessuna suite ripetuta per il checkpoint, che modifica solo documentazione/tracker. I report XML in build/reports/aud26-full, aud21-full e aud18-19-full e gli artefatti locali sono ignorati: non vengono inclusi nel commit e non sono disponibili automaticamente in un altro checkout. Dati, demo, backup e risorse delle prove conservati; nessuna nuova installazione o rimozione.

Alla ripresa leggere README.md, PROJECT_STATUS.json, plan.md, questa roadmap e docs/repo-residuals-2026-10-05.md; verificare git status e HEAD/origin/main. Il commit effettivo si ricava da git log, senza ID autoreferenziale nel tracker. Non ritentare alla cieca le pulizie bloccate o terminare processi di altre sessioni.

## AUD-27 — Riferimenti secondari nel ripristino — 6 ottobre 2026

AUD-27 (6 ottobre): il ripristino richiede i piani originali nella stessa sede, contenitori/figli e montaggi ancora disponibili. Un contenitore spostato su un altro piano o un figlio ricollocato bloccano il ripristino. ID di porte attive, porte duplicate nel JSON e ID di collocazioni già presenti sono rifiutati; nessuna collocazione saltata o sostituita. Progetto, credenziali, base di scambio, cestino e media restano invariati su rifiuto; si può riprovare dopo aver ripristinato il contesto. Le foto delle porte di un apparato nel cestino restano locali anche quando un apparato attivo riusa l’ID della porta e non vengono esportate. Controlli nel progetto corrente; collisioni tra progetti Android tracciate separatamente in AUD-28.

Baseline mirata BUILD SUCCESSFUL in 16s. Cinque nuove regressioni core hanno riprodotto piano mancante/trasferito, porte attive/duplicate, contenitore/figlio mancante e collocazione con ID riutilizzato. La prova Windows ha inoltre trovato foto della porta perse durante sostituzione quando il suo ID veniva riutilizzato; corretto il riconoscimento dei media ancora nel cestino. Le prove Android e Windows passano per quattro scenari dopo sostituzione con stesso sito; Windows copre copie protette e non protette. Verificati byte, credenziali fittizie, base, cestino persistito, export senza foto del cestino e riprova riuscita.

Suite completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 3m 13s, 433 test (120/78/161/74), zero fallimenti/errori/saltati e APK compilato. Aggiunto poi il controllo del contenitore spostato su altro piano; ultima verifica mirata Core TrashRestoreTest/ObjectHierarchyTest/ConfiguratorTest, Windows MediaLifecycleTest/ProtectedTrashTest e Android MediaLifecycleTest/DeviceOperationsTest, con assembleDebug: BUILD SUCCESSFUL in 31s. XML mirati in build/reports/aud27-targeted. Il totale 433 descrive la suite precedente all’ultima regressione, non un nuovo totale completo.

AUD-27 rimosso dal tracker; AUD-28 aggiunto per chiavi Room globali tra progetti, da codice e non riprodotto. Aggiornati contratto, workflow, mappa, storage, piano e verifica; messaggi nativi ancora in RES-19. Restano 8 attività, prossimo AUD-22; politica AUD-24 proposta e in attesa di risposta. Nessun collaudo hardware, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-22 — Runtime e prove senza scaffolding — 6 ottobre 2026

Rimossi CoreModule/ExchangeModule con i due test di nome/dipendenza e i due Example Android (somma e package name). Loader fixture Windows spostato in DesktopStorageTest con risorsa processTestResources; nessun percorso relativo o fixture nel runtime. Eliminato cleanTempFolder senza chiamanti, che ignorava gli errori. Rimossi i cinque helper Android di sola prova, InventorySearch/SearchResult e la query Room inutilizzata. Le regressioni di persistenza usano ora ProjectEdits, GlobalSearch e ConnectionGraph già impiegati dalle app; mantenute le assertion su ricerca, seriali, planimetria, percorso e modifica in blocco. Nessuna dipendenza, schema storico SQLCipher, alias/hash supportato o fixture di versioni rifiutate rimossa. Cinque chiavi i18n inutilizzate per lingua eliminate; le etichette ancora presenti nella UI conservate. Commenti BU e versioni di contratto obsolete aggiornati.

Baseline: suite completa AUD-27 verde; prima verifica AUD-22 DesktopStorageTest e ProjectRepositoryTest con assembleDebugAndroidTest: BUILD SUCCESSFUL in 48s, 25 prove (8/17). Verifica finale: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 2m 36s, 431 test (120/77/161/73), zero fallimenti/errori/saltati; APK e APK test compilati. Il calo di tre test JVM riguarda soltanto le prove tautologiche; il test nativo di package name è stato rimosso, senza riesecuzione hardware. XML in build/reports/aud22-full; JAR principale senza fixture verificato, risorsa test presente.

Fonte ufficiale consultata il 6 ottobre: [Gradle ProcessResources](https://docs.gradle.org/current/dsl/org.gradle.language.jvm.tasks.ProcessResources.html). AUD-22 rimosso dal tracker; documenti di architettura/build/verifica e piano aggiornati. Restano 7 attività (6 P2, 1 P3), incluso AUD-28. AUD-24: politica di recupero confermata; AUD-25: politica proposta in attesa di risposta. Nessun collaudo hardware, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-24 — Recupero durevole — 6 ottobre 2026

Implementato `FileRecovery` condiviso per staging/backup cifrati e journal locale, con recupero automatico, password locale Windows protetto e blocco/riprova su errore. Windows coordina copia/base/cestino/media; Android verifica l’esito Room (compresi verificatore, cestino e generazione della base) prima di completare o ripristinare i file. Generazione monotona della base distingue anche import identici. Nessuna modifica allo schema Room v2 o allo scambio `.ofam` v1. Dettagli, fonti e limiti in [storage](04-desktop-storage-interop.md).

Baseline AUD-22: 431 prove verdi. Nuove prove: 12 del journal (arresto reale del processo, errore del marker dopo commit DB, password, backup corrotto/mancante, secondo guasto, modifiche esterne, header incompleto, blocchi grandi); 2 Windows (8 combinazioni fase/password con JVM separate e blocco/riprova); 3 Android (Room persistente chiuso e riaperto, import/cancellazione commit/rollback, catalogo identico, verificatore/base/cestino/media, blocco/riprova). Il rollback riscriveva un file ancora invariato e bloccato: corretto evitando riscritture inutili. Le prove di lifecycle attendono il nuovo recupero iniziale, conservando le assertion di cancellazione. Corrette le fixture di database su disco e i timestamp obbligatori delle nuove prove; nessuna assertion indebolita.

Verifica completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 3m 35s, **448 test** (120/89/163/76), zero fallimenti/errori/saltati; APK e APK test compilati. Ultimo controllo dei percorsi canonici: FileRecoveryTest/ReversibleFilesTest, Windows CrashRecoveryTest e Android CrashRecoveryTest → BUILD SUCCESSFUL in 29s, **20 prove**; XML in `build/reports/aud24-targeted`. Non eseguito collaudo nativo SQLCipher/Keystore né UI dell’EXE; tracciati in RES-13/19/23. Non simulata perdita di alimentazione o guasto hardware del disco.

## AUD-25 — Fusione dei dati associati — 6 ottobre 2026

Politica confermata implementata nel core e nei due dialoghi: quattro scelte per credenziali/configurazioni/alimentazioni/campi extra, trasferimento al superstite oppure conservazione nel cestino. ID, classificazioni, segreti e record in conflitto restano distinti. Lo snapshot DEVICE locale conserva i record esclusi; ripristino senza sovrascrivere ID o perdere contesto. File delle configurazioni trasferiti verso il superstite o la porta copiata, anche senza trasferire le porte. Auto-riferimenti e cicli, inclusi rami con più alimentazioni, rifiutano la fusione prima di cambiare progetto/cestino; correzione e riprova riuscite.

Riprodotti e corretti omissione delle quattro opzioni, perdita del file di configurazione in export (allegato DEVICE o PORT) e ciclo creato dalla fusione. Le prove percorrono le 16 combinazioni, conflitti, sorgente di altri apparati, dati riservati, riapertura, export, ripristino e rifiuto/riprova; Windows copre copie protette e non protette. Il callback effettivo DesktopAppState è verificato. Nuovi record locali richiedono questa versione per essere ripristinati; vecchie voci senza associatedData restano leggibili. Nessuna modifica Room v2 o .ofam v1.

Baseline AUD-24 verde. Suite completa prima degli ultimi controlli di ciclo/porta: BUILD SUCCESSFUL in 4m 54s, **454 test** (123/90/164/77), zero fallimenti/errori/saltati, APK e APK test compilati; XML build/reports/aud25-full-before-cycle. Comprende anche il tredicesimo test FileRecovery sugli staging temporanei occupati. Ultima verifica mirata DeviceMergeDataTest/TrashRestoreTest core, DeviceMergeDataTest/ProjectEditsTest/MasterDetailTest Windows e DeviceMergeDataTest/DeviceOperationsTest Android, con assembleDebug e assembleDebugAndroidTest: BUILD SUCCESSFUL in 1m 27s, **41 test** (18/18/5), zero fallimenti/errori/saltati; XML build/reports/aud25-targeted. Il totale 454 non include le ultime regressioni, documentate nella verifica mirata.

AUD-25 rimosso dal tracker; 5 attività (4 P2, 1 P3), prossimo AUD-28. Checkbox, messaggi e focus sui dispositivi reali restano in RES-19/23. Nessuna installazione, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-28 — Identità confinate tra progetti Android — 6 ottobre 2026

Una nuova regressione su Room isolato ha fallito sul codice precedente: saveProject accettava un apparato con ID già appartenente a un altro progetto. ProjectStore verifica ora l’appartenenza di tutti gli ID del catalogo nella transazione, prima di aggiornare progetto o cancellare righe. Siti, piani, apparati e porte risolvono il proprietario attraverso i genitori; 20 tabelle associate hanno projectId diretto. Query parametrizzate in blocchi di 900 ID, nessuna rimappatura, modifica schema o migrazione.

Cinque prove coprono le 24 tabelle su nuovi progetti e sostituzioni (48 casi), collisione oltre il primo blocco e correzione riuscita, import nuovo/sostituzione/fusione con sei tipi di collisione, ripristino di apparati/porte e quattro tipi associati con altro progetto attivo e successiva riprova. Verificati due cataloghi, credenziali fittizie, password, basi, cestino e byte dei media invariati. Gli errori arrivano prima dell’applicazione dei file del journal.

Mirata ProjectIdCollisionTest/ImportAtomicityTest/DeviceOperationsTest/MediaLifecycleTest Android: BUILD SUCCESSFUL in 1m 18s, 20 test senza fallimenti/errori/saltati; XML build/reports/aud28-targeted. Suite completa con core/exchange/Windows/Android, assembleDebug e assembleDebugAndroidTest: BUILD SUCCESSFUL in 5m 47s, **464 test** (125/90/166/83), zero fallimenti/errori/saltati; XML build/reports/aud28-full. Questo totale comprende gli ultimi controlli di AUD-25 e il tredicesimo test FileRecovery. Fonti Room/SQLite collegate in storage, consultate il 6 ottobre.

AUD-28 rimosso dal tracker e documenti aggiornati. Nuovo AUD-29 registra il controllo del grafo di alimentazione al ripristino, inizialmente da codice e da riprodurre; 5 attività (4 P2, 1 P3). Messaggio Android e collaudi nativi restano in RES-19; nessuna installazione, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-29 — Rete di alimentazione al ripristino — 7 ottobre 2026

Riprodotto con dati sintetici il ciclo ricreato dal cestino dopo nuove alimentazioni tra gli apparati rimasti: D→O e N→D conservati nel cestino, poi O→N nel catalogo. La regressione core ha fallito perché il ripristino non generava errore. DeviceTrashData.restore riusa ora il controllo completo della fusione, prima di ricreare i record; nessuna perdita o modifica del cestino. Il messaggio indica il ciclo e permette riprova dopo correzione.

Tre regressioni coprono core, callback Windows e repository Android, cicli semplici e con più sorgenti. Windows usa copie protette e non protette, rifiuto senza cambiare byte della copia/base/cestino/media, correzione e riapertura riuscite. Android verifica catalogo, verificatore, base, cestino e file invariati, riprova e nuova facade. Baseline completa AUD-28: 464 test verdi. Ultima mirata DeviceMergeDataTest/TrashRestoreTest core, DeviceMergeDataTest/MediaLifecycleTest/ProjectEditsTest Windows, DeviceMergeDataTest/MediaLifecycleTest/ProjectIdCollisionTest Android con assembleDebug e assembleDebugAndroidTest: BUILD SUCCESSFUL in 2m 55s, **65 test** (19/27/19), zero fallimenti/errori/saltati; XML build/reports/aud29-targeted. Nessuna nuova suite completa per questa sola guardia condivisa.

AUD-29 rimosso dal tracker. Nuovo AUD-30: il validatore dei pacchetti segueva la sola prima sorgente, distinto dai controlli completi di fusione/ripristino; da riprodurre e consolidare. Aggiornati contratto, storage, workflow, piano e checklist; messaggio nativo in RES-19/23. Nessuna installazione, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-30 — Cicli con sorgenti alternative nell’import — 7 ottobre 2026

La regressione core ha riprodotto un ciclo nascosto: A→X e A→B, B→Y e B→A veniva considerato valido perché il validatore seguiva solo la prima sorgente. Validazione, fusione e ripristino condividono ora un unico controllo iterativo di tutti gli archi nel modello. Eliminati il percorso precedente e la sua chiave i18n inutilizzata. POWER_FEED_CYCLE_DETECTED resta STRUCTURAL_ERROR; un messaggio unico descrive il grafo del progetto. Nessuna dipendenza, schema, formato o migrazione nuova.

Tre nuove prove: ordine e inversione delle sorgenti, grafo profondo da 10.000 archi senza ricorsione con collegamento ripetuto e ciclo finale/auto-riferimento, import plain/protetto rifiutato con pkg nullo e riprova del pacchetto corretto. Mirata ModelValidatorTest/DeviceMergeDataTest/TrashRestoreTest core e PackageSerializerTest exchange: BUILD SUCCESSFUL in 20s, 40 test (32/8), zero fallimenti/errori/saltati; XML build/reports/aud30-targeted. Il compilatore segnala una assertion !! ridondante preesistente in un altro metodo di PackageSerializerTest; nessun errore o controllo disabilitato.

Verifica finale completa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 6m 9s: **470 test** (128 core, 91 exchange, 167 Windows, 84 Android), zero fallimenti/errori/saltati; APK e APK test compilati. XML in build/reports/aud30-full. Fonte API Kotlin ArrayDeque consultata il 7 ottobre e collegata in architettura; esiti dei grafi verificati nelle prove del repository.

AUD-30 rimosso dal tracker; rimangono RES-13/19/23/24 (3 P2, 1 P3). I residui nuovi AUD-28/29/30 sono stati tracciati, riprodotti e risolti. Resa dei messaggi/focus e Keystore/SQLCipher nativi ancora da collaudare; nessuna installazione o modifica di app/demo/dati reali. Pulizie storiche bloccate non ritentate, nessun processo di altra sessione terminato.

## AUD-31 — Pannello occupato sopra i form Windows — 7 ottobre 2026

Durante RES-23, l’EXE aggiornato su Windows 11 Pro (10.0.26300), in storage isolato con fixture sintetica da 511 MiB, mostrava il form Nuova sede sopra BusyOverlay: menu disabilitati, progresso nascosto. Nessuna modifica concorrente o corruzione riprodotta. Il pannello usa ora Dialog non chiudibile durante l’operazione, composto dopo form e conferme; eliminati l’overlay precedente, il loop pointerInput e il Box ridondante. Nessuna modifica a schema, formato, preferenze o dipendenze.

Baseline DesktopIoTest/DesktopUxLayoutTest/MediaUiDispatchTest: 8 test verdi, BUILD SUCCESSFUL in 11s. BusyDialogTest ha riprodotto il difetto con 2 assertion fallite sull’assenza del dialogo occupato; corretto prima il setup della fixture che non soddisfaceva il wizard. Ultima mirata: 10 prove verdi, BUILD SUCCESSFUL in 14s; worker riuscito/fallito, Escape/Tab/Invio/clic e bozza conservata mentre il pannello si apre/chiude. Questa prova non sostituisce il save fallito del form, distinto in AUD-32.

Verifica completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:createDistributable --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 3m 9s. Report: **472 test** (128/91/169/84), zero fallimenti/errori/saltati; 169 Windows rieseguiti, altri moduli UP-TO-DATE sul codice invariato. XML in build/reports/aud31-full, mirata in aud31-targeted e regressione iniziale in aud31-red.

EXE corretto: pannello visibile sopra Nuova sede, Escape e clic su Annulla modifiche bloccati; salvataggio riuscito, comandi disponibili dopo commit. Ctrl+N finale ha aperto il wizard dopo il completamento e non costituisce prova di blocco durante quel save. Tre sedi persistite e 16 SHA-256 degli allegati verificati; catture e JSON in build/reports/res23-20261007. Fonte ufficiale [Compose Dialog](https://developer.android.com/develop/ui/compose/components/dialog), consultata il 7 ottobre. AUD-31 rimosso dal tracker, RES-23 resta parziale. Nessuna stampa fisica o modifica a dati/demo/backup reali.

## AUD-32 — Bozze Windows conservate sul save fallito — 7 ottobre 2026

Riprodotto nell’EXE con file .ofam isolato aperto senza condivisione della cancellazione: Nuova sede chiudeva la bozza sul save fallito. Sette regressioni hanno confermato lo stesso comportamento nei form di sede/piano della mappa e progetto/nuova sede/modifica sede/nuova area/modifica area nel pannello Progetto. I form ora chiudono soltanto quando update termina senza errore; nella mappa l’errore compare anche dentro il dialogo. Nessuna modifica al contratto di DesktopAppState.update, allo schema, al formato o alle dipendenze.

Le sette prove iniziali fallivano tutte per il campo della bozza scomparso (build/reports/aud32-red). Mirata con BusyDialogTest/DesktopIoTest/MasterDetailTest e le sette nuove regressioni: 16 test verdi, BUILD SUCCESSFUL in 16s (aud32-targeted). La verifica completa include anche le assertion successive su stato dirty e un solo elemento di history: errore filesystem reale, copia/progetto/history invariati, bozza conservata, rilascio del file, riprova persistita e undo.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:createDistributable --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 3m 1s: **479 test** nei report (128 core, 91 exchange, 176 Windows, 84 Android), zero fallimenti/errori/saltati. I 176 Windows sono rieseguiti; core/exchange/Android UP-TO-DATE sul codice invariato. EXE rigenerato; XML in build/reports/aud32-full.

EXE finale su Windows 11 Pro (10.0.26300), fixture sintetica da 511 MiB e storage separato: save fallito con bozza «Retried native draft» ancora aperta ed errore leggibile nel dialogo; dopo rilascio del file la stessa bozza salva correttamente. Tre sedi precedenti conservate, quarta sede persistita una sola volta e 16 SHA-256 degli allegati uguali alla fixture sorgente. Catture e JSON in build/reports/res23-20261007. Il vecchio hash del pacchetto rilevato prima della riapertura non era una baseline immediata del guasto finale: nessuna equivalenza byte nativa dedotta da quel confronto; le sette regressioni automatiche la verificano prima/dopo il guasto.

AUD-32 rimosso dal tracker. AUD-33 P2 traccia il pattern nei picker di oggetto/pagina, letto da codice e ancora da riprodurre. RES-13/19 restano aperti senza dispositivi ADB; RES-23 resta parziale per focus/password/recupero/fusione/stampa. Pulizia storica RES-24 non ritentata in attesa della risposta; inventario di 296 file, 1.291.597.555 byte, senza reparse point. Nessun commit/push e nessun processo di altre sessioni terminato.

Checkpoint finale AUD-31/32: app isolata chiusa e nessun processo residuo; i due backup telefono conservano gli SHA-256 iniziali. Il controllo automatico ha respinto la rimozione di build/tmp/res23-20261007 con «blocked by policy», senza motivazione ulteriore. Nessun ritentativo; nuovo scratch incluso in RES-24 con percorso e assenza di reparse point verificati. Evidenze conservate in build/reports.

## AUD-33 — Picker Windows della mappa dopo save fallito — 7 ottobre 2026

Cinque regressioni con .ofam isolato bloccato tramite NOSHARE_DELETE riproducono la perdita del picker: posizione da pressione lunga sulla mappa, inserimento nel rack, seconda pagina PDF, immagine e rimozione dello sfondo. Copia, progetto e history restavano integri ma il dialogo veniva chiuso dal chiamante. FloorHomeSection chiude ora soltanto se update termina senza errore; l’ID dell’allegato appena importato resta disponibile sul rifiuto. MapObjectPicker/ObjectPickerDialog e PlanChooser ricevono un messaggio error facoltativo e lo mostrano dentro il dialogo; callback di salvataggio invariati, default null per gli altri consumatori. Nessun nuovo schema, formato o dipendenza.

Baseline ObjectPickerUiTest/FloorMediaTest: 8 prove verdi, BUILD SUCCESSFUL in 11s (build/reports/aud33-baseline). Corretto il setup della nuova fixture (Rack senza siteId e posizione esplicita prima di aprire il contenitore), poi tutte le cinque regressioni fallivano sulla selezione scomparsa (aud33-red). Mirata finale: 22 prove verdi, BUILD SUCCESSFUL in 29s (aud33-targeted), inclusi AUD-31/32. Le prove verificano bozza/errore/preset/posizione/contenitore/pagina, copia e history invariati, riprova persistita e una sola modifica annullabile.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :pc:app:createDistributable --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 6m: **484 test** nei report (128 core, 91 exchange, 181 Windows, 84 Android), zero fallimenti/errori/saltati; 181 Windows e 84 Android rieseguiti, core/exchange UP-TO-DATE sul codice invariato. APK ed EXE rigenerati; XML in build/reports/aud33-full. Nessuna installazione o prova hardware Android: ADB non rileva dispositivi.

EXE su Windows 11 Pro (10.0.26300), piccolo pacchetto sintetico importato nel precedente storage isolato: picker Switch conserva nome/preset ed errore dopo guasto; dopo rilascio del file Aggiungi persiste un solo switch con 24 porte RJ45 e 4 SFP+. PlanChooser conserva la seconda pagina selezionata e l’errore; la stessa conferma dopo rilascio assegna la pagina corretta di un PDF da tre pagine. Per entrambi i guasti SHA-256 dell’intero .ofam invariato rispetto alla baseline rilevata subito prima; PDF finale uguale alla sorgente. Catture, fixture e verification.json in build/reports/aud33-native. Posizione/contenitore/immagine/rimozione dello sfondo coperti in Compose; la matrice nativa completa resta RES-23.

AUD-33 rimosso dal tracker; AUD-34/35 registrano separatamente i picker Windows fuori mappa e le bozze Android chiuse prima dell’esito asincrono, ancora da riprodurre. Fonti ufficiali [stato e remember in Compose](https://developer.android.com/develop/ui/compose/state) e [Dialog](https://developer.android.com/develop/ui/compose/components/dialog), consultate il 7 ottobre: lo stato remember viene perso quando il composable esce dalla composizione. Gli esiti di persistenza qui indicati derivano dai test del repository.

App isolata chiusa, nessun lock della sessione mantenuto; due backup telefono SHA-256 invariati. Riusato build/tmp/res23-20261007, già incluso in RES-24: pulizie precedentemente respinte non ritentate. Dati/demo/evidenze conservati, nessun processo altrui terminato e nessun commit/push.

## AUD-34 — Picker Windows fuori dalla mappa — 7 ottobre 2026

Completato: inventario, nuovo rack, inserimento in unità rack e pagina PDF da Allegati restano aperti sul salvataggio fallito. L’errore è nel picker, la selezione rack cambia solo dopo il successo e la riprova conserva la bozza. I callback Unit esistenti restano invariati; i soli host interni ricevono un lettore dell’errore corrente. RackUnitPicker inoltra il parametro opzionale di errore, come ObjectPickerDialog.

Baseline delle regressioni precedenti verde (12 test). Tre nuove prove iniziali riproducono la scomparsa della bozza/comando; il selettore della riga rack è stato corretto per usare l’azione accessibile, poiché il clic nel layout ristretto non apriva il picker. Finale: 4 regressioni nuove e 12 precedenti, BUILD SUCCESSFUL in 17s, zero fallimenti/errori/saltati. Guasto reale tramite NOSHARE_DELETE sul pacchetto locale; copia byte per byte, catalogo e history precedenti invariati, riprova dalla stessa schermata, lettura del pacchetto persistito e undo verificati.

Suite Windows completa e baseline Android ProjectCommandTest: BUILD SUCCESSFUL in 3m 32s, 185 test Windows e 10 Android senza fallimenti/errori/saltati. XML in build/reports/aud34-targeted e aud34-full; riproduzione iniziale in aud34-red. Nessun nuovo collaudo EXE: la matrice nativa resta RES-23. AUD-34 rimosso dal tracker; AUD-36 registra gli editor completi e il form allegato letti da codice, non riprodotti.

Fonte ufficiale [stato Compose](https://developer.android.com/develop/ui/compose/state), consultata il 7 ottobre: remember perde lo stato quando il composable esce dalla composizione. Le garanzie di persistenza qui riportate derivano dalle prove del repository.

## AUD-35 — Bozze mappa Android dopo esito asincrono — 7 ottobre 2026

Completato: nuova sede/piano, picker oggetto e scelta PDF/immagine/rimozione dello sfondo attendono l’esito del save. Sul guasto la bozza e la selezione restano composte e l’errore è nello stesso form/picker. La riprova usa i dati conservati. Anche un’immagine appena importata mantiene il riferimento al media fino all’esito dell’assegnazione.

ProjectViewModel.edit conserva la firma precedente e aggiunge un overload interno con onResult(String?): null indica successo, una stringa il guasto. Una modifica senza variazioni conferma il successo senza aggiungere undo. Dopo chiusura/cambio progetto non pubblica esiti o errori della vecchia sessione; ensureActive propaga la cancellazione prima delle pubblicazioni. La UI applica il callback solo se il suo scope Compose è ancora attivo. Nessun cambio di schema Room v2, formato .ofam v1 o dipendenze.

Baseline Android ProjectCommandTest: 10 test verdi prima della correzione. La prova nativa iniziale riproduce la scomparsa di tre bozze; il PDF inizialmente richiedeva scorrimento alla seconda pagina, poi riproduce separatamente la scomparsa del comando di conferma. Durante la verifica sono stati adattati i selettori alla tastiera, alla lista lazy e al menu ⋮ delle finestre strette, senza bypass del save o sostituzione del repository.

Finale mirato: 14 ProjectCommandTest e quattro prove native principali, BUILD SUCCESSFUL in 1m 3s. Ulteriori immagine/rimozione portano a **sei test nativi** su moto g86 Android 16/API 36: BUILD SUCCESSFUL in 59s, zero fallimenti/errori/saltati. Trigger SQLite BEFORE UPDATE su database Room in memoria, media sintetici in una cartella UUID nel cacheDir e renderer PDF Android reale. Integrità del catalogo persistito, nome/tipo/posizione/pagina, errore raggiungibile, riprova, chiusura dopo successo e una sola offerta undo verificati. Tastiera chiusa prima di Salva; campi/errori raggiunti con scorrimento. Il test non verifica la matrice TalkBack/focus completa o SQLCipher/Keystore: RES-13/19 restano aperti. Scratch nativo rimosso in finally; nessun accesso al database della demo. APK principale aggiornato dal runner; EXE non rigenerato.

Suite completa JVM/Compose: **492 test** (128 core, 91 exchange, 185 Windows, 88 Android), zero fallimenti/errori/saltati. Windows eseguito nel passaggio AUD-34, Android rieseguito integralmente in AUD-35, core/exchange UP-TO-DATE. Il comando combinato con le sei prove native era fallito soltanto per due selettori del menu adattivo; dopo la correzione la suite nativa è verde. Conferma finale:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 1s (43 task UP-TO-DATE). Prova nativa eseguita con ANDROID_SERIAL=ZY32LNCB8C e `:mobile:app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.onlyfield.assetmanager.FailedMapSaveNativeTest' --no-parallel --max-workers=1`. XML/log conservati in build/reports/aud35-native, aud35-targeted, aud35-full; riproduzioni iniziali in aud35-native-red e aud35-native-red-pdf. AUD-35 rimosso dal tracker; AUD-37 registra i picker/editor Android fuori mappa letti da codice, non riprodotti. Nessun commit/push o pulizia delle risorse storiche.

Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state), [eventi UI e responsabilità di ViewModel/UI](https://developer.android.com/topic/architecture/ui-layer/events), [cancellazione ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html). Le garanzie specifiche dell’app derivano dalle regressioni del repository.

## Stato telefono dopo collaudo e procedura conservativa — 7 ottobre 2026

Il controllo finale ha trovato com.onlyfield.assetmanager assente dopo connectedDebugAndroidTest. Reinstallando l’APK con -r, files/databases/shared_prefs erano assenti. La sessione non aveva verificato lo stato installato prima del primo test: errore del collaudo, distinto dalle regressioni di codice. L’utente ha confermato che sul moto g86 c’era soltanto Demo Comune. APK reinstallato manualmente, demo ripristinato dalla fixture canonica tramite EncryptedSchemaUpgradeTest#demoPackageIsImportedOnDisk con seedApplicationDemo=true: database vuoto richiesto, import SQLCipher su disco, riapertura e media verificati, **OK (1 test)** in 5,389s. Nessun backup grezzo sovrascritto: entrambi SHA-256 corrispondono a build/reports/res23-backup-before-20261007.json; confronto in build/reports/aud35-native/backup-integrity.json.

Le sei prove AUD-35 sono state rieseguite tramite installazione manuale e am instrument: **OK (6 tests)** in 48,663s; pacchetto principale ancora presente dopo la suite. Rimosso solo com.onlyfield.assetmanager.test (Success), app principale riavviata e presente. Questa procedura evita la disinstallazione automatica del runner Gradle. Non eseguire connectedDebugAndroidTest su un dispositivo che contiene dati da conservare. La sola separazione del database di prova non protegge dalla pulizia dei pacchetti eseguita dal runner.

Comandi verificati sul moto g86; installazioni con -r, senza uninstall del pacchetto principale:

```powershell
adb -s ZY32LNCB8C install -r mobile/app/build/outputs/apk/debug/app-debug.apk
adb -s ZY32LNCB8C install -r mobile/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.FailedMapSaveNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner
```

Verificare prima pacchetto/dati e backup esportabile; controllare il pacchetto principale dopo la prova. Il seedApplicationDemo è stato usato solo per questo recupero concordato e non deve essere incluso nei collaudi ordinari. I backup grezzi cifrati dipendono dal Keystore e non sono garanzia di recupero dopo una disinstallazione. Fonti primarie: [implementazione ufficiale del plugin APK installer UTP](https://android.googlesource.com/platform/tools/base/+/445534e2a5188fca7990ed6455fb83f9aa5bba2a/utp/android-test-plugin-host-apk-installer/src/main/java/com/android/tools/utp/plugins/host/apkinstaller/AndroidTestApkInstallerPlugin.kt), consultata il 7 ottobre: afterAll disinstalla i pacchetti con uninstallAfterTest. Rimozione e ripristino qui descritti sono osservazioni della sessione.

Chiusura documentale AUD-34/35: tracker con sei sole voci aperte/parziali e riferimenti/dipendenze validi; 18 Markdown UTF-8 senza BOM e 143 link locali validi; git diff --check superato. Rimossi quattro import inutilizzati nel solo scope Windows modificato; :pc:app:compileKotlin --no-parallel --max-workers=1, BUILD SUCCESSFUL in 2s. Nessun nuovo test ripetuto per la sola rimozione degli import.

## AUD-36 — Editor Windows e allegati dopo save fallito — 7 ottobre 2026

DeviceDialog e RackDialog (anche Aggiungi e modifica), collocazione, sostituzione, modifica multipla e form allegato si chiudono dopo il successo. Sul guasto rimangono campi, file e selezioni; il nuovo rack viene selezionato dopo la persistenza. Sostituzione e modifica multipla conservano cestino/history e selezione sul fallimento. Nessuna firma, dipendenza, schema Room o formato .ofam cambiato.

Baseline: 11 regressioni preesistenti, BUILD SUCCESSFUL in 52s. Riproduzione iniziale: gli editor apparato scompaiono dopo il guasto filesystem mentre copia e history restano integre. Otto regressioni FailedEditorSaveTest verificano creazione/modifica apparato e rack, collocazione, sostituzione, batch e allegato con file locale aperto NOSHARE_DELETE; riprova, riapertura e un solo undo verdi. Il file viene scelto tramite il vero JFileChooser, con sorgente sintetica in TemporaryFolder.

Comando: .\gradlew.bat :pc:app:test --no-parallel --max-workers=1. **BUILD SUCCESSFUL in 3m 8s; 193 test, zero fallimenti/errori/saltati**. XML in build/reports/aud36-full; baseline e prima verifica isolata in aud36-baseline e aud36-section-targeted.

Limite esplicito: il runner Compose con DesktopApp completo si blocca nel rendering Skiko durante il nuovo rack. Thread dump conservati in build/reports/aud36-renderer-thread-dump.txt e aud36-full-renderer-thread-dump.txt; fermati solo worker avviati da questa sessione. Azione accessibile e pausa del clock non risolvono la matrice completa. I test finali montano le sezioni e i veri form con DesktopAppState/DesktopIo/storage, senza la cornice globale; nessuna asserzione su bozza, errore, integrità o undo esclusa. Questa prova non sostituisce il collaudo EXE/focus/blocco input, ancora in RES-23.

Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state-hoisting) e [cancellazione Kotlin](https://kotlinlang.org/docs/cancellation-and-timeouts.html). L'esito della persistenza deriva dalle prove locali. AUD-36 rimosso dal tracker; AUD-38 registra Modelli/Credenziali Windows, stesso pattern da codice, non riprodotto. Nessun commit/push o pulizia storica.

## AUD-37 — Bozze Android fuori mappa — 7 ottobre 2026

Picker/editor Inventario, Rack e unità rack, collocazione/batch, sede/piano, creazione/modifica/applicazione modello e pagina PDF da Allegati attendono l'esito di edit prima di scartare la bozza. Sul guasto restano campi e selezioni con errore nello stesso form; la riprova salva una sola modifica annullabile. EditSave riusa il callback esistente, limita gli esiti alla composizione/bozza corrente e cancella l'errore quando cambia bozza. Le firme esistenti del ViewModel, Room v2 e .ofam v1 restano invariati; nessuna nuova dipendenza.

Baseline ProjectCommandTest: 14 test, BUILD SUCCESSFUL in 34s. L'editor apparato nativo perde la bozza sul guasto SQLite prima della correzione; nel medesimo tentativo il picker non viene raggiunto per un selettore del FAB. Due nuove regressioni JVM verificano che un editor dismesso non riceva successo/errore tardivi: 16 test mirati verdi, incluso il comportamento preesistente di undo/no-op/sessione/cancellazione.

FailedSectionSaveNativeTest: **OK (17 tests) in 117,658s** su moto g86 API 36, tramite `adb install -r` e `adb shell am instrument -w -e class com.onlyfield.assetmanager.FailedSectionSaveNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner`. Guasto BEFORE UPDATE nella sola Room in memoria; progetto/cache/PDF sintetici isolati. Ogni scenario verifica bozza/selezioni, errore raggiungibile, repository invariato sul fallimento, riprova senza duplicati, riapertura e un solo undo fino al progetto precedente. Tastiera chiusa prima del save; campi/errori raggiunti tramite scorrimento. Primo giro: 10 verdi e 7 errori nei selettori FAB; accesso all'albero semantico non aggregato corretto, 7 mirati verdi, poi intera matrice verde. Nessuna asserzione esclusa e nessun collaudo SQLCipher/Keystore/TalkBack rivendicato.

Verifica complessiva:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

**BUILD SUCCESSFUL in 2m 35s; 502 test nei report, zero fallimenti/errori/saltati** (128 core, 91 exchange, 193 Windows, 90 Android). Android rieseguito integralmente; core/exchange e Windows UP-TO-DATE, quest'ultimo già eseguito in AUD-36. XML in build/reports/aud37-full, log aud37-full-suite.log; baseline/mirati e prove native in aud37-baseline, aud37-targeted e aud37-native (red, primo giro, mirati, finale).

App Android aggiornata con -r; APK test disinstallato (Success), pacchetto principale verificato e app riavviata. Database e WAL originali SHA-256 invariati prima del riavvio, come i due backup storici: confronti in aud37-native. Cache di ogni scenario rimossa in finally. Quattro piccole fixture JUnit Windows lasciate dai worker interrotti (6708 byte totali, soli progetti Editor retry verificati nei ZIP e marker source.txt di questa sessione) conservate: pulizia respinta dal controllo automatico con blocked by policy, senza altra motivazione. Nessun ritentativo; percorsi esatti e inventario in aud36-scratch-inventory.json e RES-24. Recovery senza file; risorse storiche e processi altrui preservati.

Fonti ufficiali consultate il 7 ottobre: [stato e ciclo di vita Compose](https://developer.android.com/develop/ui/compose/state-hoisting), [cancellazione Kotlin](https://kotlinlang.org/docs/cancellation-and-timeouts.html). Le prove locali stabiliscono integrità ed esito del salvataggio. AUD-37 rimosso dal tracker; AUD-39 separa sostituzione/fusione, import/classificazione allegato, credenziali e download cartografico Android, letti da codice e non riprodotti. Restano anche AUD-38 e RES-13/19/23/24. Nessun EXE rigenerato o commit/push.


## AUD-38 — Modelli e Credenziali Windows — 7 ottobre 2026

Creazione/modifica/applicazione modello e creazione/modifica credenziale chiudono la bozza soltanto dopo il save riuscito; l'errore è nel form. DesktopApp inoltra l'errore corrente. Gli overload precedenti preservano i chiamanti, incluse le lambda finali; nessuna nuova dipendenza, schema o formato.

Baseline FailedEditorSaveTest: 8 prove verdi, BUILD SUCCESSFUL in 28s. Dieci regressioni nuove riproducono la bozza scomparsa, con file .ofam sintetico bloccato tramite NOSHARE_DELETE; catalogo, byte e history restano invariati sul guasto. Prima compilazione della fixture corretta con `arrayOf<Any>`; un comando di modifica respinto dal parser PowerShell ha causato un secondo giro sul codice invariato, poi applicate patch puntuali. Il controllo dei consumatori ha rilevato la lambda finale CredentialsSection nel test preesistente: overload compatibile mantenuto.

Finale mirato: 18 prove verdi, BUILD SUCCESSFUL in 41s. Suite ` .\gradlew.bat :pc:app:test --no-parallel --max-workers=1 `: **203 test, zero fallimenti/errori/saltati**, BUILD SUCCESSFUL in 4m 53s. Evidenze in build/reports/aud38-baseline, aud38-red, aud38-targeted e aud38-full. I dieci casi coprono sia FormDialog sia MasterDetailHost, selezioni e campi conservati, segreti esclusivamente sintetici, riprova senza duplicati, lettura del pacchetto persistito e un solo undo.

Correzione AUD-38 completata e rimossa dal tracker; il criterio nativo DesktopApp/EXE resta esplicitamente in RES-23, con il limite del runner già osservato in AUD-36. Nessun nuovo collaudo EXE, commit/push o pulizia delle risorse storiche. Prossimo AUD-39.

Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state) e [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting). Remember perde lo stato quando il form esce dalla composizione; esito e integrità specifici dell'app derivano dalle prove locali.


## AUD-39 — Comandi specializzati e credenziali Android — 7 ottobre 2026

Sostituzione/fusione, import/classificazione allegato, creazione/modifica credenziale e download cartografico conservano il form fino al successo. Sul guasto campi, selezioni e navigazione restano disponibili con errore e riprova. La sostituzione torna indietro dopo il successo. Gli overload precedenti del ViewModel restano compatibili; gli esiti nuovi sono limitati alla sessione, verificano la cancellazione e, per i media, arrivano dopo la pulizia. EditSave.submit riusa lo stesso controllo della composizione degli edit generici. Nessuna nuova dipendenza, schema Room v2 o formato .ofam v1.

Baseline ProjectCommandTest: 16 test verdi, BUILD SUCCESSFUL in 22s. Sei prove native iniziali riproducono bozze/comandi scomparsi e navigazione anticipata, con guasto BEFORE UPDATE nella sola Room in memoria; catalogo e media originali invariati. Red conservato in build/reports/aud39-native/red.txt (6 fallimenti, 46,45s). Il runner am instrument restituisce codice shell zero anche con test falliti: il comando di conferma verifica esplicitamente OK e termina con errore se manca.

FailedSpecializedSaveNativeTest: **OK (8 tests) in 71,349s**, moto g86 5G Android 16/API 36, tramite install -r e am instrument. Otto flussi coprono campi/selezioni conservati, errore raggiungibile, retry senza duplicati, catalogo/cestino/media integri sul guasto, persistenza dopo retry e undo nei flussi che già lo prevedono. Sostituzione/fusione mantengono il cestino transazionale e non ricevono un nuovo undo. Import usa un risultato del picker sintetico; download usa un PNG reale sintetico e una sorgente iniettata dal costruttore interno, provando errore della sorgente e persistenza SQLite senza reti pubbliche. Il costruttore applicativo precedente usa sempre CartographicMapManager reale. Repository, staging e salvataggio dei media restano reali. La prova non copre picker di sistema, server cartografico, SQLCipher/Keystore, TalkBack o matrice UX completa: RES-13/19 restano aperti.

SpecializedCommandTest aggiunge 18 regressioni: callback di guasto dopo cleanup, retry unico, chiusura sessione, cancellazione, editor dismesso, fusione rifiutata e sorgente import illeggibile. Mirata con ProjectCommandTest e MediaAdditionTest: **38 test verdi**, BUILD SUCCESSFUL in 25s. Prima compilazione/APK con i 16 ProjectCommandTest preesistenti: BUILD SUCCESSFUL in 34s. Report baseline/mirati in build/reports/aud39-baseline e aud39-targeted.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

**BUILD SUCCESSFUL in 2m 41s; 530 test nei report, zero fallimenti/errori/saltati** (128 core, 91 exchange, 203 Windows, 108 Android). Android rieseguito integralmente; Windows già verificato in AUD-38, core/exchange sul codice invariato UP-TO-DATE. XML e conteggi in build/reports/aud39-full; log aud39-full-suite.log. Prove native in aud39-native/first-fixed.txt.

Il telefono si è scollegato prima del controllo finale ed è stato ricollegato dall'utente. SHA-256 di database e WAL identici alla baseline prima della riapertura; due backup storici invariati, confronti in aud39-native. APK test rimosso (Success), app principale verificata e riaperta; nessuna disinstallazione del pacchetto principale. Cache di ogni scenario rimossa in finally e assenza delle cartelle special-save verificata. Nessuna pulizia storica, rigenerazione EXE o commit/push.

AUD-39 completato e rimosso dal tracker. AUD-40 registra separatamente la navigazione anticipata nelle conferme di cancellazione apparato/rack, letta da codice e non riprodotta. Collaudi generali RES-13/19/23/24 conservati. Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state), [eventi UI](https://developer.android.com/topic/architecture/ui-layer/events) e [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html). Le proprietà delle API vengono dalle fonti; esito e integrità dell'app dalle prove locali.

## AUD-40 — Cancellazione apparato/rack Android — 8 ottobre 2026

Riprodotto su moto g86 API 36 con guasto SQLite isolato: DEVICE/RACK rimangono nel database ma la conferma torna subito alla lista. Due prove native rosse; dieci nuove regressioni JVM riproducono anche l'errore tardivo dopo chiusura/cambio progetto (due rosse, otto verdi). Fixture sincronizzate tramite ObjectHierarchy, non dati applicativi reali.

Rimossi i due back anticipati; il ritorno esistente reagisce all'oggetto assente dopo reload del commit, con controllo di progetto/destinazione. Nessuna nuova firma/callback, dipendenza, schema o formato. moveToTrash rispetta ensureActive prima di pubblicare stato/messaggi e sopprime errori di una sessione chiusa.

Verifica mirata: `.\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.DeletionCommandTest --tests com.onlyfield.assetmanager.ProjectCommandTest --tests com.onlyfield.assetmanager.SpecializedCommandTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 44s, 44 test senza fallimenti/errori/saltati. Baseline dei comandi esistenti verde prima della correzione. Dieci regressioni: guasto/riprova, doppia richiesta con un solo cestino/undo, media e collocazione rack ripristinati, ID assente, chiusura/cambio progetto, altra schermata e cancellazione ViewModel.

Native: `adb -s ZY32LNCB8C install -r` per APK principale/test e `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.FailedDeletionNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → **OK (2 tests), 35,168s**. AppRoot/ConfirmHost/Scaffold/snackbar reali: annullamento conferma, guasto SQLite, scheda e selezioni conservate, errore visualizzato, riprova, un solo ritorno e undo toccato nella UI; database/media verificati. L'undo attende la durata dello snackbar di errore precedente. Cache delete-save-UUID rimossa in finally, database/WAL e due backup storici SHA-256 invariati prima della riapertura. Report `build/reports/aud40-red`, `aud40-targeted`, `aud40-native` e log `aud40-*.log`.

AUD-40 completato e rimosso dal tracker; restano RES-13/19/23/24. Matrici UX/TalkBack, hardware/SQLCipher/Keystore ed EXE/focus/stampa conservano i propri limiti. Fonti primarie consultate l'8 ottobre: [eventi e navigazione UI Android](https://developer.android.com/topic/architecture/ui-layer/events) e [ensureActive Kotlin](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html); esiti e integrità dell'app derivano dalle prove locali.

Suite completa AUD-40: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 2m 43s, 540 report senza fallimenti/errori/saltati** (128 core, 91 exchange, 203 Windows, 118 Android). Android rieseguito integralmente; moduli invariati UP-TO-DATE. XML e conteggi conservati in `build/reports/aud40-full`; log `build/reports/aud40-full-suite.log`.
Perimetro dei collaudi hardware (decisione utente, 8 ottobre 2026): usare la fotocamera integrata Android; lettori USB e scanner esterni sono esclusi definitivamente dalle prove richieste. Le funzionalità applicative restano disponibili; questa è una decisione sul collaudo. RES-13 conserva fotocamera/gesti e recupero SQLCipher/Keystore isolato, con distinzione fra riavvio del processo e del dispositivo.
## RES-13 — Recupero nativo SQLCipher/Keystore parziale — 8 ottobre 2026

Aggiunto EncryptedRecoveryNativeTest: EncryptedDatabase e ProjectRepository reali, con ContextWrapper che separa database, media e file delle chiavi avvolte in cache/native-recovery-UUID. Il provider AndroidKeyStore resta reale; nessuna chiave applicativa viene rimossa o esportata. Le verifiche aprono SQLCipher dopo chiusura del database, controllano fingerprint di progetto/base/verificatore password/cestino, byte media e blob della chiave avvolta. Nessuna modifica al runtime, schema o formato.

| Scenario | Esito verificato su moto g86 API 36 |
| --- | --- |
| Sostituzione media prima del commit | Rollback di catalogo/base/password/cestino e ripristino dei byte precedenti. |
| Sostituzione media dopo il commit | Catalogo/base aggiornati, cestino conservato e byte incoming mantenuti. |
| Cancellazione prima del commit | Progetto/base/verificatore/cestino/media precedenti recuperati. |
| Cancellazione dopo il commit | Progetto/base/media rimossi secondo il commit persistito. |
| File modificato esternamente durante rollback | Recupero bloccato, letture/scritture rifiutate, tutti i file di journal/backup SHA-256 invariati; riprova dopo ripristino del file riuscita. |

Comandi: assembleDebugAndroidTest → BUILD SUCCESSFUL in 4s, install -r del solo APK test, poi `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.EncryptedRecoveryNativeTest -e recoveryPhase prepare -e recoveryRun <UUID> com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` e stessa invocazione con recoveryPhase recover e medesimo UUID. **OK (1 test)** in entrambe le fasi, 20,718s / 13,918s; la seconda verifica esplicitamente un PID diverso. Il test senza argomenti esegue anche il percorso normale roundtrip, **OK (1 test), 35,566s**. Report `build/reports/res13-native/{prepare,recover,roundtrip}.txt`, run.json e prepare-pid.txt; build log res13-recovery-build.log.

Il journal è lasciato pendente intenzionalmente: l'interruzione prima del commit è un'eccezione sintetica nella transazione; dopo il commit manca l'acknowledgement. Il runner termina normalmente e il secondo processo recupera i file. Non è un arresto forzato, riavvio del dispositivo o perdita di alimentazione: queste prove restano non eseguite. Il test ordinario pulisce le proprie fixture; la modalità prepare le conserva soltanto fino alla modalità recover. Database/WAL e blob cifrati delle chiavi applicative confrontati prima della riapertura; backup storici conservati.

RES-13 resta PARTIAL per fotocamera integrata, foto porta/cavo e serie, scansione tramite fotocamera, gesti e riavvio fisico. Lettori USB/scanner esterni esclusi definitivamente dalle prove su indicazione utente; funzionalità applicative conservate. RES-19/23/24 invariati nel perimetro residuo. Fonti primarie consultate l'8 ottobre: [Android Keystore](https://developer.android.com/privacy-and-security/keystore) e [SQLCipher Android con Room](https://github.com/sqlcipher/sqlcipher-android). Le fonti descrivono le API; i cinque esiti provengono dalle prove native.

Controllo finale della sessione: SHA-256 di database/WAL, blob cifrati delle chiavi applicative e due backup storici invariati prima della riapertura. Cache delete-save/native-recovery assente; APK test rimosso con Success e MainActivity riaperta. RES-13 aggiornato a PARTIAL; tracker con sole quattro voci residue (3 P2, 1 P3), AUD-40 rimosso. Nessun EXE rigenerato, pulizia storica ritentata o commit/push.

## AUD-41 — Serie foto Android e risultati tardivi — 8 ottobre 2026

Durante RES-13 emerge dal codice che onPhotoResult richiama onSaved in finally, prima della fine di launchCommand. pendingCommands resta positivo e preparePhoto rifiuta lo scatto successivo. PhotoCommandTest riproduce il guasto e altri due problemi: errore/callback dopo chiusura e accettazione del risultato dopo chiusura/riapertura dello stesso progetto. Baseline MediaAdditionTest verde prima della modifica; nuova suite iniziale cinque test, tre fallimenti, XML/log in build/reports/aud41-red e aud41-red.log.

La continuazione attende Job.join: soltanto dopo rilascio del comando può preparare lo scatto successivo. PendingPhoto conserva progetto/allegato/file/sessione; errori e callback sono vincolati alla sessione ancora valida. Cancellazione rilanciata e file non committati rimossi; PhotoCapture ignora la continuazione dopo uscita dalla composizione. Nessuna firma pubblica, dipendenza, schema o formato modificato.

Mirata: `.\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.PhotoCommandTest --tests com.onlyfield.assetmanager.MediaAdditionTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 17s, nove test**, zero fallimenti/errori/saltati. Verificati prima continuazione, cleanup su errore, undo, esiti dopo chiusura/cancellazione e risultato appartenente a un'altra apertura dello stesso progetto.

Native: assembleDebug/assembleDebugAndroidTest → BUILD SUCCESSFUL in 14s; install -r e `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.PhotoSeriesNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → **OK (5 tests), 31,371s**, moto g86 API 36. Tre serie DEVICE/PORT/CABLE con due JPEG sintetici e terzo risultato annullato; guasto SQLite, arresto della serie e riprova; host smontato durante il save senza nuovo lancio. Byte/target, persistenza, export/import AES-GCM e undo verificati. Camera e risultato del permesso sono simulati solo nel registry di test, senza aprire la fotocamera; Compose/helper/launcher/FileProvider/repository e media sono reali, host minimo. Non è collaudo del sensore, di rotazione reale o delle schede rapide porta/cavo complete. Fixture cache/object_photos/native-series-UUID rimosse in finally.

Scatti reali rinviati su risposta esplicita dell'utente. RES-13 mantiene la checklist fisica; RES-19/23 mantengono matrici UX/TalkBack e Windows, RES-24 le pulizie storiche respinte. Fonti primarie consultate l'8 ottobre: [Job.join](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/join.html), [scope Compose](https://developer.android.com/develop/ui/compose/side-effects) e [TakePicture](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture). Esiti e integrità specifici dell'app derivano dalle prove locali.

Verifica finale: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 2m 45s**. 545 test nei report: 128 core, 91 exchange, 203 Windows, 123 Android; zero fallimenti/errori/saltati. Android rieseguito, moduli invariati UP-TO-DATE; APK test finale compilato. XML e conteggi conservati in `build/reports/aud41-full`, log `build/reports/aud41-full-suite.log`.

Database/WAL, chiavi avvolte applicative e due backup storici SHA-256 invariati prima della riapertura. Nessuna cache native-series residua; APK test rimosso con Success e MainActivity riaperta, evidenza `build/reports/aud41-native/final-integrity.txt`. AUD-41 completato e rimosso dal tracker. Restano quattro residui RES-13/19/23/24 (3 P2, 1 P3); prossima attività RES-19, scatti reali rimandati. Nessun EXE rigenerato, pulizia storica ritentata o commit/push.

Controllo documentale finale: 18 Markdown UTF-8 senza BOM, 143 link locali validi; quattro ID residui univoci, riferimenti/dipendenze validi e git diff --check senza errori.

## Scheda rapida e configuratore Android — AUD-42 / RES-19, 8 ottobre 2026

ConfiguratorMatrixNativeTest verifica sul moto g86 API 36 le otto combinazioni 360/412 dp, chiaro/scuro, testo 1,0/1,3. Tre test nativi passano: 16 configurazioni del footer (con/senza Dettagli), otto di disposizione/PoE/Salva/Annulla e otto di percorso, foto porta/cavo come callback, collegamento e scollegamento con conferma/annullamento. AUD-42 riprodotto senza Dettagli: Chiudi sovrapposto a Scollega; il footer condiviso è ora un unico gruppo verticale.

Host Compose e progetto in memoria isolati; LocalDensity cambia solo il contenuto del test, non le impostazioni del telefono. Nessuna persistenza, camera reale, TalkBack audio, tastiera, rotazione, mappa densa, topologia o cornice AppRoot completa collaudata. RES-19 e RES-13 restano parziali. L'opzione strumentale `-e matrixEvidence true` salva screenshot e alberi semantici; senza opzione non scrive evidenze sul dispositivo. 56 coppie PNG/testo correnti in build/reports/res19-native/final-evidence, campione controllato visivamente; log e regressione rossa conservati nella stessa directory dei report. CaptureToImage attende il rendering del dialogo, evitando fotogrammi transitori di UiAutomation.

Suite completa: BUILD SUCCESSFUL in 6m, 545 test nei report (128 core, 91 exchange, 203 Windows, 123 Android), nessun fallimento/errore/saltato; Windows e Android rieseguiti, core/exchange invariati UP-TO-DATE. XML in build/reports/aud42-full. Comandi e fonti ufficiali nella roadmap.

UX-04 completato sul componente Android: due ulteriori prove nella classe ConfiguratorMatrixNativeTest coprono ricerca fra 12 modelli e riduzione di gruppo collegato nelle otto combinazioni. Nessun ulteriore difetto applicativo. Classe finale: OK (5 tests), 87,536s, moto g86 API 36; log build/reports/res19-native/complete.txt. Popup catturato con isPopup, form con inset safeDrawing, tastiera chiusa; alberi completi di ogni root. 80 coppie finali PNG/albero completo generate sul telefono: copia e pulizia pendenti perché ADB non rileva più il dispositivo. I primi screenshot dello sfondo/alberi ridotti restano cronologia, non evidenza finale. Dopo la suite JVM già verde sono cambiati solo test nativo e raccolta evidenze; suite generale non ripetuta. RES-19 complessivo resta parziale.

## RES-23 — Editor rack nell'EXE, 8 ottobre 2026

`:pc:app:createDistributable --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 4s. App/EXE aggiornati nel solo scratch già esistente build/tmp/res23-20261007, dati/runtime conservati; due progetti sintetici verificati. Finestra 1348×854, tema chiaro, UI nativa con computer-use. Nuovo rack → Aggiungi e modifica apre DesktopApp/EditorFrame senza il limite del runner Skiko.

FileStream Open/Read/FileShare.Read sul solo pacchetto sintetico forza un errore reale della sostituzione: banner leggibile, editor/bozza conservati, nome RES23-RACK raggiungibile e focalizzabile, catalogo senza rack dopo l'errore. Handle rilasciato con Enter nella sessione helper; Ctrl+S dalla stessa schermata salva un solo rack. Ctrl+W, riapertura e Ctrl+2 ne verificano la persistenza. App chiusa con Alt+F4, finestra/processo e helper assenti.

17 media e tre pacchetti fuori dalla modifica SHA-256 invariati. Hash del pacchetto modificato diverso dalla baseline prima dell'avvio: non prova l'invariabilità byte durante il guasto, non rivendicata. Screenshot successo/riapertura, inventario e hash in build/reports/res23-20261008; errore/focus osservati nella sessione. Nessun nuovo difetto/runtime; altri editor, matrice focus/input, protezione/recupero, checkbox e stampa ancora aperti. Nessuna suite ripetuta per questa sola verifica nativa.

## RES-23 — Editor apparato nell’EXE, 8 ottobre 2026

EXE nello storage sintetico esistente build/tmp/res23-20261007, finestra 1348×854 e tema chiaro. Modifica dell’apparato con bozza RES23-DEVICE-RETRY: handle FileStream Open/Read/FileShare.Read sul solo pacchetto sintetico provoca il guasto della sostituzione. Editor, bozza e banner restano visibili; Tab passa al tipo e Ctrl+S resta disponibile. SHA-256 del pacchetto identico alla baseline immediata durante il guasto. Dopo rilascio, Ctrl+S salva; chiusura e riapertura confermano un solo apparato, 28 porte e rack RES23-RACK conservato. Screenshot e JSON in build/reports/res23-followup-20261008.

Scenario apparato completato; RES-23 resta PARTIAL per la matrice input durante operazioni lunghe, altri editor, checkbox della fusione, recupero protetto e errore nativo della stampa. Nessun difetto applicativo emerso, nessuna modifica al runtime o suite JVM ripetuta.

## RES-23 — Checkbox fusione nell’EXE, 8 ottobre 2026

Creato AP-01 nel solo progetto sintetico AUD-33 native. Il vero editor Unisci un duplicato mostra tutte le opzioni tramite scorrimento, descrizione e footer fissi. Credenziali risponde a clic sulla riga e Spazio; Tab raggiunge nell’ordine Configurazioni, Alimentazioni e Campi extra, e Spazio cambia ognuna delle selezioni. Escape apre la conferma di scarto; Scarta ritorna all’inventario con entrambi gli apparati conservati. Screenshot merge-options-before.png e merge-keyboard-options.png in build/reports/res23-followup-20261008.

Completata l’interazione dei quattro controlli a 1348×854, tema chiaro e scala standard. Non eseguiti testo ingrandito, applicazione/trasferimento dei dati o rifiuto per ciclo: restano in RES-23. Nessuna modifica al runtime o suite ripetuta.

## RES-23 — Scorciatoie e commit grande nell’EXE, 8 ottobre 2026

Progetto sintetico expanded-511 (535.835.495 byte ZIP espansi, 16 allegati): pannello leggibile durante apertura e salvataggio di RES23-INPUT-MATRIX. Ctrl+N e Ctrl+O non aprono wizard/picker durante l’apertura; Escape non rilascia il pannello. Durante il salvataggio con form Nuova sede ancora aperto, Ctrl+N e Ctrl+W non avviano il wizard né chiudono il progetto: screenshot prima/dopo con pannello e form in build/reports/res23-followup-20261008.

Ctrl+Z è stato ricevuto dopo il commit e ha avviato l’undo; non è evidenza del blocco durante il save. Undo riuscito e persistito: quattro sedi originali, nessuna RES23-INPUT-MATRIX. Ctrl+1 dopo il completamento torna all’inventario. Il clic finale durante apertura e Ctrl+1 non vengono conteggiati come prove di blocco perché hanno coinciso con il rilascio. La matrice mouse/tastiera/focus resta parziale; nessun difetto applicativo dimostrato e nessuna suite ripetuta.

## RES-23 — Annullamento stampa da tastiera, 8 ottobre 2026

EXE corrente, progetto sintetico AUD-33 native: Documenti e stampa → Stampa apre il vero dialogo Windows. Escape lo annulla, il pannello occupato viene rilasciato e lo stato mostra Stampa annullata. Ctrl+1 dalla finestra principale torna all’inventario; due apparati conservati. Tutti i 21 file dello storage (17 media e quattro pacchetti) mantengono lo SHA-256 della baseline immediata e la coda contiene zero lavori. Evidenze print-native-dialog.png, print-cancelled.png, print-integrity.json e print-result.json in build/reports/res23-followup-20261008.

Annullamento da tastiera completato. Non sono errore del motore di stampa o stampa fisica; RES-23 resta parziale. Fonte primaria consultata l’8 ottobre: [PrinterJob Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/print/PrinterJob.html), che distingue esito di printDialog ed esecuzione di print. Gli esiti specifici provengono dalla prova locale.

## Consolidamento tracker — 8 ottobre 2026

Il tracker conserva solo RES-13/19/23/24 (tre P2, un P3), tutti PARTIAL: descrizioni ridotte al lavoro mancante, cronologia conclusa nella roadmap e nelle evidenze. Nessuna voce residua chiusa integralmente da questa sessione; nessun nuovo difetto applicativo emerso. ADB devices -l non rileva dispositivi, quindi matrice Android, copia delle 80 coppie finali e pulizia del telefono restano in attesa USB. Pulizie storiche respinte non ritentate. I collaudi Windows completati sono registrati nelle sezioni precedenti; dettagli della sessione in build/reports/res23-followup-20261008.

Fonti ufficiali consultate l’8 ottobre: [ADB](https://developer.android.com/tools/adb), [verifica accessibilità Compose](https://developer.android.com/develop/ui/compose/accessibility/testing) e [PrinterJob Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/print/PrinterJob.html). Le API documentate non dimostrano il comportamento dell’app: i risultati e i limiti provengono dai collaudi locali.

Controllo finale della sessione: 18 Markdown UTF-8 senza BOM, 146 link locali validi, quattro ID residui univoci e riferimenti/dipendenze validi; git diff --check senza errori. Tutti i 17 media dello scratch conservano gli SHA-256 iniziali; nessun file .recovery/.tmp nello storage e nessun processo EXE della sessione residuo. Report final-integrity-summary.json e document-validation.json. Nessuna modifica ai sorgenti o dipendenze in questa sessione; suite/compilazione non ripetute perché non è emerso un nuovo difetto. Modifiche preesistenti conservate, nessun commit/push.

## RES-23 — Creazione modello nell’EXE, 8 ottobre 2026

DesktopApp/EditorFrame reali, storage sintetico res23-20261007, finestra 1348×854 chiara. Nuovo modello RES23-MODEL-NEW: guasto filesystem tramite handle Read/FileShare.Read sul pacchetto conserva nome, editor, errore interno e banner; SHA-256 immediato del pacchetto invariato. Rilascio e Ctrl+S dalla stessa bozza salvano un solo modello; due apparati e 29 porte conservati. Screenshot/JSON in build/reports/res23-editors-20261008. Nessun nuovo difetto o modifica al runtime; matrice restante RES-23 aperta.

### RES-23: modifica modello nell’EXE (8 ottobre 2026)

Sul progetto sintetico AUD-33, un handle senza condivisione della cancellazione provoca un errore reale nella sostituzione del pacchetto. Il pannello conserva il nuovo nome e l’errore; SHA-256 invariato. Dopo rilascio e Ctrl+S, il pacchetto contiene un solo modello rinominato con ID originale. Screenshot e riscontri JSON: `build/reports/res23-editors-20261008/edit-model-*`. Verifica del frame chiaro a 1348 × 854; non estesa ad altri layout.

### RES-23: applicazione modello nell’EXE (8 ottobre 2026)

Provato il percorso completo scelta AP-01 → bozza → guasto della sostituzione file → rilascio → Ctrl+S. La bozza rimane aperta con il nome modificato e il banner di errore; hash del pacchetto invariato al guasto. La riprova aggiorna una sola volta lo stesso apparato. Il modello senza porte elimina la sola porta del destinatario; confronto JSON integrale dello switch non destinatario invariato (28 porte). Evidenze `build/reports/res23-editors-20261008/apply-model-*`, frame chiaro 1348 × 854.

L’annullamento dell’applicazione ripristina AP-01 con una porta. Una seconda sequenza guasto/riprova, senza rinomina, salva AP-01 con ID originale e riferimento al modello; il confronto completo dello switch con la baseline dopo Annulla è identico. Il primo probe usava `name` al posto di `technicalName`; la verifica corretta è in `apply-model-persisted.json` e `apply-model-repeat-*`.

### RES-23: nuova credenziale nell’EXE (8 ottobre 2026)

Creazione con utente sintetico, segnaposto, tipo Password, AP-01 e gruppo di prova. Provocato un errore reale di sostituzione del pacchetto: bozza aperta e campi conservati, SHA-256 invariato. Riprova Ctrl+S dopo rilascio: una sola credenziale con valori e collegamento attesi. Il probe confronta il segnaposto senza stamparlo nei riscontri; gli screenshot lo mostrano mascherato. Evidenze `build/reports/res23-editors-20261008/new-credential-*`, frame chiaro 1348 × 854.

### RES-23: modifica credenziale nell’EXE (8 ottobre 2026)

Rinomina utente con blocco reale del pacchetto e riprova dopo rilascio. Errore e bozza restano nel pannello; file invariato al guasto. Il probe del pacchetto verifica una sola credenziale con stesso ID, nuovo utente e invarianti su segnaposto, tipo, apparato, gruppo e note. Riscontro senza valori segreti in `edit-credential-persisted.json`; screenshot mascherati in `build/reports/res23-editors-20261008`. Frame chiaro 1348 × 854; altri layout e superfici del frame restano da verificare.

### RES-23: preferenze EXE (8 ottobre 2026)

Tema scuro → cambio al chiaro con handle reale senza condivisione cancellazione su settings.properties: banner leggibile, tema precedente conservato, hash di impostazioni e progetto invariati. Ctrl+1 funziona dopo il guasto. Riprova dopo rilascio: tema chiaro persistito, banner rimosso e progetto invariato. Evidenze `build/reports/res23-editors-20261008/preferences-*`; profilo sintetico, frame standard. Lingua e recupero/password non collaudati qui.

## AUD-43 — Selettori Windows e focus, 8 ottobre 2026

Riprodotto nell’EXE: annullamento con Escape di apertura/esportazione lascia le scorciatoie e Alt+F4 inattivi fino a un clic. Il parent null di JFileChooser usa il frame Swing nascosto. I quattro selettori DesktopStorageHelper ora usano la finestra AWT attiva; API e formati invariati. NativePickerOwnershipTest apre realmente i selettori cartella/singolo/multipli/salvataggio e verifica proprietario, posizione, annullamento e ritorno del focus. Baseline quattro regressioni rosse e tre test esistenti verdi; mirata finale sette test verdi (6s). Suite Windows e createDistributable: 207 test, zero fallimenti/errori/saltati, BUILD SUCCESSFUL in 3m 18s. Comandi completi e fonti Oracle nella roadmap; report `build/reports/aud43-red`, `aud43-targeted`, `aud43-windows`.

EXE rigenerato, frame chiaro 1348 × 854, dati sintetici: apertura ed esportazione centrati, Escape e successive Ctrl+3/Ctrl+1/Ctrl+E/Alt+F4 efficaci senza clic di recupero. Quattro file invariati rispetto alla baseline immediata dopo apertura; la baseline precedente all’apertura include la riscrittura locale prevista e non è prova di invarianza byte dei selettori. Evidenze `build/reports/res23-editors-20261008/aud43-*`. Questo chiude AUD-43 e i due scenari di annullamento, non l’intera RES-23.

Pulizia nuova fixture respinta automaticamente con blocked by policy, non ritentata: `pc/app/build/compose/binaries/main/app/OnlyFieldAssetManager/data` (soli dati sintetici) e `build/reports/res23-editors-20261008/apply-model-entities-before.json` (probe invalido, non evidenza). Inventario `cleanup-blocked-inventory.json`; baseline corretta applicazione modello `apply-model-undo-baseline.json`. Percorsi aggiunti a RES-24. Runtime/exe e helper chiusi; report conservati.

### RES-23: preferenza lingua EXE (8 ottobre 2026)

Sistema/italiano → English con errore reale di sostituzione impostazioni: banner leggibile, UI italiana e SHA-256 precedenti conservati. Dopo rilascio, riprova persistita (language=en), menu e pagina iniziale inglesi, errore rimosso e testi utente preservati. Ripristino Italiano riuscito. Finestra chiara standard, profilo sintetico nel runtime generato; evidenze `build/reports/res23-editors-20261008/language-*`. Non è collaudo della pagina password o del recupero.

### RES-23: fusione reale, guasto/riprova e undo (8 ottobre 2026)

Sul solo progetto sintetico del nuovo EXE, tutte le opzioni selezionate: AP-01 → switch RES23-DEVICE-RETRY. Blocco reale sostituzione pacchetto: pannello e scelte conservati, hash invariato. Riprova Ctrl+S salva un solo apparato (28 porte), sposta la credenziale con ID/contenuto conservati e il duplicato nel cestino. UI Credenziali mostra lo switch. Ctrl+Z ripristina JSON integrale precedente, AP-01, collegamento originario e cestino vuoto. Evidenze `build/reports/res23-editors-20261008/merge-*`. Fixture senza configurazioni/alimentazioni/campi extra e senza porte del duplicato: questi trasferimenti, ciclo/ripristino e testo ingrandito restano aperti.

### AUD-44 — Alimentazioni/PoE/Badge Windows, chiuso l’8 ottobre 2026

Guasto reale della sostituzione del pacchetto: creazione/modifica dei tre editor chiudevano prima dell’esito, perdendo la bozza. Dodici regressioni, dialogo e pannello, riproducono la perdita dopo aver verificato file, progetto, cestino e history invariati. Fixture iniziale corretta con UUID valido prima della baseline rossa; nessun errore della fixture attribuito al prodotto. XML rosso: build/reports/res23-editors-20261008/aud44-red.xml.

PowerBadgeSection chiude ora solo dopo save riuscito, mostra errore interno e protegge Nuovo/Modifica tramite il guard esistente. Selezioni, ID e campi nascosti conservati; firma precedente disponibile, nessuna dipendenza/schema/formato modificato. Mirata FailedPowerSaveTest e FailedModelCredentialSaveTest: BUILD SUCCESSFUL in 34s, 22 test verdi. Suite Windows e createDistributable con init-script build/tmp/aud44-20261008/native-output.init.gradle, --no-parallel --max-workers=1: BUILD SUCCESSFUL in 3m 50s, 219 test, zero fallimenti/errori/saltati. XML in build/reports/aud44-windows, log in res23-editors-20261008.

Nel vero EXE isolato, nuova alimentazione AP-01 da switch: guasto mantiene nome/sorgente/errore e SHA-256 invariato; rilascio e Ctrl+S salvano una sola alimentazione; Ctrl+Z ripristina il progetto JSON integrale. Screenshot/JSON aud44-feed-*; EXE/helper chiusi. Le altre undici varianti sono verificate dalle regressioni, senza estendere il collaudo EXE a tutte. AUD-44 rimosso dal tracker; RES-23 resta parziale. Runtime build/tmp/aud44-native-20261008 e fixture sintetica conservati e inventariati per RES-24, senza alterare la fixture AUD-43 già respinta.

Fonti primarie consultate l’8 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state) e [distribuzioni native Compose](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html). Risultati applicativi da prove locali; outputBaseDir usato solo nello script di collaudo. Nessun commit/push.

### RES-19/24 — Evidenze Android recuperate e pulizia telefono, 8 ottobre 2026

Moto g86 API 36 nuovamente autorizzato via ADB. Copiati 176 file (11.234.842 byte) in build/reports/res19-native/complete-evidence-20261008, verificando ogni SHA-256 contro il telefono: 80 coppie finali PNG/albero completo e otto coppie footer precedenti, distinte nel manifest complete-evidence-integrity-20261008.json. Ogni nome finale e intestazione API/modello verificati; due campioni finali controllati visivamente. Non ripetuta la suite già verde; questi screenshot documentano host Compose isolati, non AppRoot, tastiera/TalkBack o persistenza completa.

Database, WAL, db_key.bin e recovery_key.bin coincidono con data-before.txt; entrambi i backup storici SHA-256 invariati. Riverificati percorso fisico e 176 hash immediatamente prima della rimozione: eliminati solo i file della cartella configurator-matrix-evidence e la directory vuota; disinstallato esclusivamente com.onlyfield.assetmanager.test (Success). App principale conservata. Hash dati/chiavi nuovamente invariati prima della riapertura; MainActivity riaperta con Status: ok. Report data-before-cleanup-20261008.txt e data-after-cleanup-20261008.txt.

Recupero evidenze e pulizia del telefono completati e rimossi dalle descrizioni aperte. RES-19 conserva la matrice UX non eseguita; RES-24 conserva solo le risorse Windows storiche respinte e gli eventuali scratch nuovi. Fonte ufficiale [ADB](https://developer.android.com/tools/adb), consultata l’8 ottobre. Nessun commit/push.

### RES-23 — Fusione rifiutata per ciclo e riprova EXE, 8 ottobre 2026

Fixture sintetica con AP-01 alimentato dallo switch. Unire AP-01 nello switch trasferendo Alimentazioni creerebbe un arco verso se stesso: il vero EXE mostra il messaggio localizzato, mantiene pannello/duplicato/scelte e SHA-256 del pacchetto invariato. Tutte le scelte di trasferimento restano raggiungibili e selezionate.

Dalla stessa schermata, escluso il trasferimento Alimentazioni, Ctrl+S salva un solo switch con 28 porte e trasferisce la credenziale; duplicato nel cestino, alimentazione esclusa rimossa insieme al duplicato. Ctrl+Z ripristina il progetto JSON integrale precedente, inclusi due apparati, alimentazione e collegamento credenziale. Non è trasferimento di un’alimentazione valida né correzione dei collegamenti: sono ancora scenari distinti. Report/screenshot merge-cycle-* in build/reports/res23-editors-20261008. Nessun difetto, modifica al runtime o suite ripetuta. Scala standard, tema chiaro 1348 × 854; testo ingrandito e ripristino dal cestino per ciclo ancora aperti.

### RES-23 — Password impostata manualmente nell’EXE, 8 ottobre 2026

Utente ha completato il salvataggio nel progetto sintetico AUD-33 del runtime AUD-44; frame mostra Protetto da password e Password del progetto impostata. Verifica filesystem: pacchetto progetto e base sync cifrati, project.json.enc presente e project.json assente; allegati del pacchetto cifrati. Nessun PDF in chiaro o file temporaneo/recovery nello storage. Password non acquisita o registrata. Inventario/prova password-native-* in build/reports/res23-editors-20261008.

Ctrl+W chiude il progetto e Continua apre il dialogo Pacchetto protetto. Richiesta all’utente prova di password errata, poi riprova corretta; ancora pendente, non conteggiata come successo. EXE lasciato aperto per il collaudo guidato. Il passaggio finale di impostazione è manuale secondo computer-use/confirmations.md; nessun segreto chiesto in chat.

### RES-23 — Riapertura corretta e PDF cifrato, 8 ottobre 2026

L’utente ha inserito la password corretta nel dialogo, senza comunicarla. DesktopApp riapre AUD-33 protetto: due apparati, modello e una alimentazione con AP-01/sorgente switch conservati nella UI. L’unico PDF allegato viene decifrato e renderizzato come planimetria dopo il caricamento asincrono; nessun PDF in chiaro o file tmp/recovery nello storage. Screenshot password-native-reopened-correct, password-native-pdf-rendered e password-native-power-retained.

Impostazione e riapertura corretta native completate; password errata/riprova, guasto del cambio password e recupero bloccato non verificati in questo flusso. Nessun confronto JSON integrale del pacchetto cifrato rivendicato, nessuna password acquisita. EXE chiuso, fixture protetta conservata per RES-24; serve la password dell’utente per riaprirla. Nessun difetto e nessuna suite ripetuta.

### RES-13 — Recovery dopo riavvio fisico, completato l’8 ottobre 2026

Sul moto g86 API 36, l’utente ha riavviato dal menu e sbloccato il telefono. Boot ID diverso verificato prima di recoveryPhase=recover, stesso recoveryRun=b91d2730-9698-4936-a271-3460d7f0c5cc: OK (1 test), 14,046s. Cinque scenari SQLCipher/Keystore isolati verificati dopo il riavvio: rollback/commit di update e delete, più rollback bloccato da modifica esterna e riprova; chiavi wrapped conservate, journal recuperati secondo l’esito Room. Preparazione precedente: OK (1 test), 20,781s.

Root recovery assente dopo il finally del test; database/WAL/chiavi principali e due backup storici SHA-256 invariati. Rimosso solo com.onlyfield.assetmanager.test (Success); hash principali ancora invariati prima della riapertura di MainActivity (Status: ok). Report boot-before/after, prepare/recover, data-after-recover/cleanup e run.json in build/reports/res13-reboot-20261008.

Riavvio normale con journal pendenti completato e rimosso dalle attività aperte. Restano arresto forzato/perdita improvvisa di alimentazione durante scrittura, fotocamera e gesti; questa prova non verifica UX AppRoot o TalkBack. Fonte primaria [Android Keystore](https://developer.android.com/privacy-and-security/keystore), consultata l’8 ottobre; esiti specifici dal test nativo.

## Revisione UI/UX — 8 ottobre 2026

Matrice mappa 360/412/600/840/1024 dp con entrambi i temi e testo 1,0/1,3 verificata su host PC e Android isolato. Footer porte e tastiera Android verificati; cancellazioni con errore/riprova conservate. Nove scenari nativi distinti verdi; dimensioni tramite LocalDensity. Vedere [esiti, comandi e limiti](ui-ux-audit-2026-10-08.md). Restano aperti RES-19/23 per TalkBack, rotazione reale, tablet fisici e intera AppRoot/EXE.

## Verifica finale della remediation — 9 ottobre 2026

AUD-45–51 completati, tracker ridotto ai soli RES-13/19/23/24 (3 P2, 1 P3). Criteri nativi non eseguiti trasferiti ai residui, senza dedurre la matrice AppRoot/EXE dai test.

`.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 7m 44s: **643 test**, 130 core + 101 exchange + 282 Windows + 130 Android JVM, zero fallimenti/errori/saltati. Conteggi conservati in build/reports/tracker-remediation-20261009/final-suite.json; i report Gradle dei successivi comandi mirati descrivono il relativo sottoinsieme.

Revisione finale: export PDF ordinario mantiene la chiusura dello stream; PrintAdapterTest e MarkdownEscapingTest rieseguiti, BUILD SUCCESSFUL. APK principale/test ricompilati; install -r e am instrument SurveyPdfTest,PrintPdfNativeTest → **OK (4 tests), 4,638s**, moto g86 5G Android API 36. Verificati anche gli stream chiusi. Destinazioni/fixture PDF pulite nel finally; database/preferenze principali (3 file) SHA-256 invariati prima della riapertura. Solo profileInstalled cambia con la nuova APK, non è un dato utente. APK test della sessione rimosso (Success), MainActivity riaperta (Status: ok). Evidenze final-native.txt/final-native-state.json.

`pwsh -NoProfile -File tools/testing/markdown-content.ps1` → PASS nelle tre lingue e nei sette casi ai bordi degli span. Usa ConvertFrom-Markdown già presente, senza dipendenze nuove. Per rigenerare le fixture eseguire prima `.\gradlew.bat :shared:exchange:test --tests '*MarkdownEscapingTest*' --no-parallel --max-workers=1`. `powershell -NoProfile -File tools/testing/portable-data.ps1` già verificato dopo il controllo preventivo del runtime, PASS; nessun packaging ripetuto sulla sorgente popolata.

Documentazione di dominio aggiornata ad ogni chiusura, riepiloghi README/plan/audit riallineati. Stato storico spostato fuori dalle descrizioni operative del tracker. Nessun commit/push o pubblicazione; nessuna pulizia storica respinta ritentata.

Pulizia finale della remediation: il controllo automatico ha respinto la rimozione dei soli build/tmp/remediation_docs.py, build/tmp/__pycache__/remediation_docs.cpython-313.pyc e della directory cache se vuota, con motivo blocked by policy. Nessuna rimozione eseguita o ritentata. Due file inventariati con dimensioni/SHA-256 in build/reports/tracker-remediation-20261009/cleanup-blocked-inventory.json; aggiunti a RES-24. I report restano conservati.

Controlli documentali finali: 21 Markdown renderizzati, 182 link locali validi; 55 file modificati/nuovi UTF-8 senza BOM. Tracker con quattro ID unici, riferimenti/dipendenze validi e nessun AUD completato; git diff --check superato. Diff finale rivisto, nessun lockfile/generated/vendor modificato. La sola pulizia respinta resta in RES-24.

## Secondo audit del repository — 9 ottobre 2026

Il [report corrente](repo-residuals-2026-10-09.md#secondo-audit-completo--9-ottobre-2026) conserva i nuovi rilievi AUD-52–62, probe in memoria, riferimenti puntuali e criteri di chiusura. Baseline del tracker alla rilevazione: **15 attività, 2 P1 / 11 P2 / 2 P3**; priorità iniziali AUD-52 e AUD-53. Stato corrente: AUD-52–64 completati e rimossi, **4 residui** nel [tracker](../PROJECT_STATUS.json). RES-13/19/23/24 conservati con i limiti dei collaudi reali.

Comando eseguito nella fase di analisi:

```powershell
.\gradlew.bat :shared:exchange:test --tests '*PackageSerializerTest*' --tests '*ContractVersionTest*' --tests '*PasswordHasherTest*' :mobile:app:testDebugUnitTest --tests '*ProjectCommandTest*' --tests '*ProjectRepositoryTest*' --no-parallel --max-workers=1
```

**BUILD SUCCESSFUL in 1m 21s**, **46 test** (12 exchange, 34 Android JVM), zero fallimenti/errori/saltati. I test esistenti verdi e i probe sono evidenze distinte: alla rilevazione i nuovi difetti richiedevano le regressioni indicate nei criteri di chiusura, ora completate nelle sezioni successive. Nessuna nuova suite generale eseguita per l’aggiornamento documentale; nessun nuovo collaudo nativo. I risultati della precedente remediation AUD-45–51 sopra sono conservati come storico. Verifiche finali di JSON, conteggi, riferimenti, link e diff nella [roadmap](../roadmap.md).

## AUD-52 — Piani con annotazioni

La cancellazione condivisa rifiuta anche i piani referenziati da annotazioni, senza mutare il progetto. PackageSerializerTest verifica rifiuto, conservazione della nota, cancellazione del piano vuoto e riapertura .ofam semplice/protetta. Baseline verde; finale .\gradlew.bat :shared:exchange:test --tests *PackageSerializerTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 5s, nessun fallimento. Prima compilazione della nuova fixture corretta per usare label e coordinate obbligatorie. Fonte ufficiale per verifica mirata: [Gradle JVM testing](https://docs.gradle.org/current/userguide/java_testing.html), consultata il 9 ottobre. Nessun collaudo nativo richiesto per il controllo condiviso.

## AUD-53 — Creazione Android atomica

ProjectRepository.createProject prepara il verificatore e salva progetto/inventario/protezione nella stessa transazione Room. Il wizard conserva ID alla riprova e blocca invii simultanei; un ID già esistente viene rifiutato. Baseline ImportedProtectionTest: BUILD SUCCESSFUL in 37s. Finale .\gradlew.bat :mobile:app:testDebugUnitTest --tests *ProjectCreationTest* --tests *ImportedProtectionTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 21s, 9 test, zero fallimenti/errori/saltati. Coperti creazione semplice/protetta, password errata, guasto dopo inserimento progetto, rollback verificatore/inventario, annullamento prima della transazione e riprova senza duplicati. Una prima fixture riutilizzava un ID sede tra progetti ed è stata corretta: il rifiuto ownership esistente resta intatto. Fonte: [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt), consultata il 9 ottobre. Prova Room JVM distinta da SQLCipher nativo; messaggi/focus del wizard protetto restano nella matrice RES-19.

## AUD-54 — Bozze wizard e rinomina al guasto

Il wizard Windows conserva lo stato in DesktopAppState durante la rimozione temporanea del dialogo per I/O; chiude e azzera i campi solo dopo commit riuscito. Rinomina Android usa EditSave: errore nel dialogo, campi conservati e callback ignorato dopo uscita dalla composizione. Baseline mirata verde; finale .\gradlew.bat :pc:app:test --tests *ProjectWizardSaveTest* :mobile:app:testDebugUnitTest --tests *ProjectCommandTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 26s. Prova Compose con storage inaccessibile e riprova protetta singola, prove Room di guasto/riprova e risultato tardivo. La compilazione Android della baseline includeva già il callback di rinomina appena aggiornato; la baseline Desktop era precedente. Collaudo EXE/focus e dialogo nativo Android restano in RES-23/19.

## AUD-55 — Coerenza pacchetti .ofam

Import rifiuta duplicati manifest/progetto/media, ID manifest discordante, payload plain/encrypted conflittuali, metadati crittografici in pacchetti semplici e progetto protetto in ZIP semplice. Ogni rifiuto è strutturale e chiude staging. Conservata compatibilità utilizzata: cifratura richiesta esplicitamente su progetto non protetto e vecchi allegati non cifrati dentro progetto cifrato restano ammessi. Baseline PackageSerializerTest/StagedPayloadTest verde; finale .\gradlew.bat :shared:exchange:test --tests *PackageMetadataTest* --tests *PackageSerializerTest* --tests *StagedPayloadTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 6s, zero fallimenti/errori/saltati. Nuove fixture includono duplicati reali, staging già presente, controlli validi semplici/protetti; nessun accesso a storage utente.

## AUD-56 — Letture Android rigorose

Rimossi i fallback dei cinque mapper: JSON VLAN/LAG illeggibile ed enum sconosciuti interrompono la lettura, senza costruire valori vuoti/predefiniti. Il comando edit segnala il guasto e non salva; righe e stato visibile precedente restano intatti. Baseline 37 prove verdi; finale .\gradlew.bat :mobile:app:testDebugUnitTest --tests *CorruptStoredDataTest* --tests *ProjectCommandTest* --tests *ProjectRepositoryTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 25s, 39 test, zero fallimenti/errori/saltati. Coperti i due JSON, tutti i 29 campi enum interessati, valori validi, riprova dopo correzione ed errore del comando. La prima fixture verificava la colonna legacy del modello mentre configurationJson corrente prevale; corretta per esercitare il formato legacy effettivamente letto. Fonte: [Kotlin enumValueOf](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/enum-value-of.html), consultata il 9 ottobre. Nessuna migrazione schema; collaudo messaggi/focus Android resta RES-19.

## AUD-57 — Validazione rete e riferimenti

ModelValidator riusa il controllo CIDR IPv4 dei form e controlla subnet.vlanId e scope VLAN/subnet per progetto/sede/apparato. Riferimenti inesistenti sono strutturali; destinazione SITE/DEVICE non rilevata resta documentale, con messaggi it/en/es. Baseline ModelValidatorTest/EntityFormsTest verde; verifica con NetworkValidationTest e DemoSeedTest: BUILD SUCCESSFUL in 34s; regressioni scope ampliate: BUILD SUCCESSFUL in 3s. Conservati limiti /0 e /32, incompletezza ammessa e demo valido; import semplici/protetti invalidi non restituiscono pacchetti. Fonte [RFC 4632](https://www.rfc-editor.org/rfc/rfc4632), consultata il 9 ottobre. Emersi e tracciati AUD-63 (VLAN referenziate: blocco richiesto dall’utente) e AUD-64 (scope nelle operazioni sede/apparato), da completare separatamente.

## AUD-63 — Blocco cancellazione VLAN referenziata

Applicata la decisione utente: ProjectEdits.deleteVlan rifiuta la cancellazione quando una subnet usa la VLAN, con messaggio it/en/es. Windows visualizza il rifiuto senza chiamare il salvataggio; Android lo riceve nel comando edit senza mutazione. Baseline Desktop ProjectEdits/FailedNetworkSave verde (28s); finale shared/exchange NetworkValidationTest + pc VlanDeletionUiTest + compileDebugKotlin: BUILD SUCCESSFUL in 13s. Android ProjectCommandTest: BUILD SUCCESSFUL in 20s. Verificati dati invariati, errore visibile, riprova dopo scollegamento esplicito della subnet, salvataggio singolo e .ofam semplice/protetto valido. Nessuna modifica automatica delle reti. Rimane distinto il collaudo visivo nativo RES-19/23.

## AUD-58 — Testo XLSX conservato

XLSX applica ST_Xstring al testo delle celle: protegge underscore iniziali delle sequenze letterali, codifica controlli/XML non validi e CR, conserva LF/tab, Unicode e spazi tramite xml:space=preserve. Originali e tipi numerici invariati. Baseline documentale verde; finale XlsxTextTest/LocalizedExportsTest/DocumentSelectionTest/DemoXlsxPathsTest: BUILD SUCCESSFUL in 6s, 7 test, zero fallimenti/errori/saltati. Tutte le parti XML parsate nelle tre lingue e testo ricostruito in un solo passaggio. Due tentativi iniziali della fixture cercavano erroneamente altezza rack nei fogli; dopo segnalazione e lettura del generatore, la prova numerica usa L2 (numero porte). Fonti consultate il 9 ottobre: [Microsoft ST_Xstring](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-oi29500/d34ae755-c53f-4a44-a363-c6dd3ee018a4) e [W3C XML 1.0](https://www.w3.org/TR/xml/#charsets). Apertura Excel nativa non eseguita.

## AUD-59 — Tile Windows prima del decode

Le tile Windows verificano 256x256 con ImageReader.getWidth/getHeight prima di read(0), con stream in memoria e reader.dispose in finally. Conservato limite 2 MiB anche per fetch iniettato. Baseline DesktopDocumentAndCartographyTest verde; finale TileDecodeTest + suite precedente: BUILD SUCCESSFUL in 5s, 6 test, zero fallimenti/errori/saltati. Sorgente HTTP locale della sessione chiusa al termine: tile valida, PNG 4096x4096 sotto il limite compresso, malformato e risposta oltre 2 MiB. Un PNG con solo header sovradimensionato verifica il rifiuto dimensioni prima dei pixel. Fonte [Oracle ImageReader](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageReader.html), consultata il 9 ottobre. Nessun download dal servizio pubblico né OOM provocato.

## AUD-60 — Errori planimetria PDF Windows espliciti

Planimetrie richieste ma illeggibili o senza payload interrompono PDF/stampa con errore; rimosse le due conversioni silenziose in sfondo assente. Documento/stream chiusi anche al fallimento. Sfondo assente, escluso dal filtro o sezione disattivata restano validi. Baseline DeliveryPdfTest/ReportPdfTest/FloorMediaTest verde; regressioni FloorPlanPdfFailureTest includono immagini/PDF corrotti, payload mancante dopo svuotamento del cache isolato, immagine e PDF validi, filtri e assenza. Dieci test, zero errori/fallimenti/skips: `:pc:app:test --tests '*FloorPlanPdfFailureTest*' --tests '*DeliveryPdfTest*' --tests '*ReportPdfTest*' --tests '*FloorMediaTest*' --no-parallel --max-workers=1`, BUILD SUCCESSFUL in 16s. Corretti un errore di compilazione della fixture e una prima simulazione che manteneva il payload nel cache. Fonti PDFBox 3 consultate il 9 ottobre. Interazione nativa stampa/focus resta RES-23.

## AUD-61 — Pieghe esplicite conservate

Rimossa l’euristica LEGACY_DEFAULT e la costante orfana: ogni punto intermedio salvato è una piega. Default a due punti ancora rettilineo. Baseline MapSceneTest verde; dopo modifica MapSceneTest e PackageSerializerTest verdi, DesktopStorageTest verde alla riprova con stato di protezione esplicito, ProjectRepositoryTest verde dopo correzione import della fixture (BUILD SUCCESSFUL in 21s). Verificati coordinate .2/.5/.8, estremi spostati e risalvataggio, working copy semplice/protetta, Room e .ofam semplice/protetto; stesso contenuto e ID. Nessuna migrazione Room/.ofam. Documentazione mappa aggiornata.

## AUD-64 — Cancellazione e fusione bloccate per ambiti referenziati

Policy conservativa confermata dall’utente: guard prima di cancellazione sede, cestino/sostituzione apparato e fusione di entrambi gli apparati quando referenziati da ambito VLAN/subnet. Nessuna rete modificata automaticamente, nessun record di cestino creato al rifiuto. Errori it/en/es; gestione rifiuto in inventario/mappa Windows e dialoghi sostituzione/fusione. NetworkScopeRetentionTest verifica entrambe le sorgenti VLAN/subnet, entrambi i dispositivi, sede vuota referenziata, target estraneo eliminabile, rimozione esplicita, ripristino e .ofam semplice/protetto. ProjectCommandTest verifica errori, Room/inventario/cestino invariati e riprova. Gradle mirato: BUILD SUCCESSFUL in 29s. NetworkScopeUiTest: messaggio Compose di cancellazione, zero callback di save/trash, riprova; fusione Windows semplice/protetta preserva file e undo/cestino, retry/undo riusciti: BUILD SUCCESSFUL in 7s. Baseline ha rilevato vecchia aspettativa di cancellazione VLAN referenziata in ProjectEditsTest; sostituita con rifiuto e disconnessione esplicita conformi ad AUD-63. Documentazione dominio aggiornata. Focus/matrice nativa resta RES-19/23.

## AUD-62 — Serializer senza consumatori eliminato

Ricerca completa dei consumatori conferma che DeviceModelSerializer era usato soltanto dal proprio test: entrambi eliminati. Modelli e fixture utili restano nel flusso del progetto. Baseline PackageSerializerTest/DeviceModelSerializerTest verde (BUILD SUCCESSFUL in 3s); dopo rimozione PackageSerializerTest e tutte le prove Demo verdi (BUILD SUCCESSFUL in 45s). Regressione .ofam semplice/protetto confronta l’intero progetto con modello, ID, metadati, template porte, PoE, hardware/layout/override e campi extra. Nessun consumer applicativo residuo, contratto/schema invariato. Documentazione dominio aggiornata; fonti dello scambio ZIP già consultate.

## Chiusura della remediation AUD-52–64 — 9 ottobre 2026

Completati e rimossi tutti i 13 task del secondo audit, inclusi AUD-63/64 emersi dalla validazione rete. Tracker con `remainingTasks=[]`; restano RES-13/19/23/24, **4 residui (0 P1 / 3 P2 / 1 P3)**. Documentazione dominio, workflow, export, mappa e linee guida allineata; fonti primarie nelle sezioni pertinenti.

Verifica completa: **679 test, zero fallimenti/errori/saltati**: core 130, exchange 115, Windows 292, Android JVM 142. Core/exchange verdi nella prima esecuzione completa. Questa aveva quattro fallimenti Windows: tre casi della stessa fixture interop con numero VLAN al posto dell’UUID, e un selettore Compose che trovava due messaggi ora visibili. Fixture corretta con `vlan.id`, assert del messaggio nel dialogo specifico, diagnostica spostata negli assert; nessuna validazione indebolita. Riprova mirata verde in 33s, incluso wizard protetto Android con guasto, invii rapidi e riprova senza duplicati.

Finale `.\gradlew.bat :pc:app:test :mobile:app:testDebugUnitTest :pc:app:assemble :mobile:app:assembleDebug --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 7m 2s**. Build Desktop e APK debug Android riuscite; nessuna installazione, rigenerazione portable o qualificazione hardware. Report XML iniziali falliti e finali conservati in `build/reports/tracker-followup-20261009`, riepilogo `verification.json` con hash APK.

RES-19/23 includono esplicitamente i controlli nativi di wizard, rinomina, rifiuti di riferimenti rete e sfondi PDF. RES-13 mantiene gli scatti rinviati e i limiti hardware; RES-24 conserva tutte le pulizie storiche respinte, senza ritentativi. Nessun commit/push richiesto o eseguito.
Controlli conclusivi: 21 Markdown e 221 collegamenti locali validi; 54 file modificati/nuovi verificati UTF-8 senza BOM. JSON, ID, riferimenti e dipendenze del tracker validi; `git diff --check` superato. Rimosso soltanto l’helper creato per questa sessione `build/tmp/task_updates_20261009.py`; nessuna pulizia storica ritentata. Diff completo rivisto, nessun lockfile/vendor/migrazione applicata modificato. Evidenza `build/reports/tracker-followup-20261009/document-validation.json`.
