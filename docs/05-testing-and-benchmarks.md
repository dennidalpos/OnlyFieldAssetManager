# Verifica

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

Contenuto («Demo Comune», 356 apparati, 7 rack, 1001 cavi):

- **Comune – Municipio** (CED, Piano terra, Primo piano, Copertura). Il rack CED è il centro stella. Contiene: patch panel fibra PPF-COM-CED (24 LC), SW-COM-CORE (24 RJ45 + 12 SFP+), FW-COM-01, RTR-COM-01, ONT-COM-01, due server, NAS e UPS. La catena WAN è ONT → router → firewall → core.
- **Teatro comunale** (Piano terra): ONT e router propri con linea FTTH e VPN verso il firewall del Municipio (collegamenti logici). Il cavo della presa PR-TEA-PT-01 passa per la scatola di giunzione GB-TEA-PT-01.
- **Scuola media** (Piano terra, Primo piano, Copertura): il rack del piano terra è il centro stella della scuola.
- **Scuola materna** (Piano terra, Copertura).
- **Ogni piano** ha un rack RK da 42 U con:
  - due patch panel da 48 RJ45 (PP-…-A/B), cablati per 3/4 (72 porte su 96) verso 36 prese PR da due porte;
  - due switch da 48 porte PoE con 4 SFP+;
  - sui piani con dorsale, un patch panel fibra da 12 LC.
- **Sulla mappa**, per piano: le prese in quattro file e 16 apparati sulle prime prese (4 AP, 2 telecamere, 4 telefoni, 6 PC).
- **Dorsali**: gli switch dei piani salgono in fibra (cavi FO) al patch panel del centro stella e da lì al core. Nelle sedi a un solo rack lo switch B è collegato allo switch A con un DAC.
- **Ponti radio**, tutti di tipo Ponte radio con tratta radio tracciabile:
  - RAD-COM-01 sulla copertura del Municipio (alimentato da SW-COM-P1-A P40) verso RAD-MED-01 della Scuola media (tratta PR-COM-MED, 1200 m);
  - la media rilancia con RAD-MED-02 verso RAD-MAT-01 della materna (PR-MED-MAT, 450 m).

  I percorsi porta sono completi tra le sedi, ad esempio SW-COM-P1-A P40 → … → SW-MED-PT-A P40.

Il progetto è costruito con le stesse bozze delle app (`DemoSeed`, nei test di `:shared:exchange`); `DemoSeedTest` ne verifica struttura, cablaggio e import.

## Limiti noti

- Il collaudo su telefono, multitouch, fotocamera, scanner e lettore USB richiede hardware reale: [checklist](testing/hardware-checklist.md).
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
