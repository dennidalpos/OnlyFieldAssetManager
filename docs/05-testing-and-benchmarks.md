# Verifica

## Suite automatica

La verifica JVM e Compose usa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest
```

Le suite coprono dominio, serializzazione/cifratura, fusione, storage Room, interoperabilita, configuratore, documenti e UI Desktop. Il workflow CI esegue lo stesso perimetro prima di creare gli artefatti.

Le regressioni dell'audit sono permanenti: `ImportedProtectionTest` verifica la riapertura su database Room su file e le regole di sostituzione/fusione; `AttachmentConfinementTest` verifica percorsi esterni, traversal e allegati legittimi; `MissingPayloadTest` verifica catalogo, checksum, alias legacy e pacchetti cifrati; `ImportPayloadReviewTest` verifica conferma e avviso visibile nella UI Desktop. `ProtectedMediaTest` copre import protetto, rendering da memoria, riapertura, password errata, cambio/rimozione password, protezione dei file esistenti e salvataggio di nuovi media senza copie temporanee. Le prove JVM non sostituiscono il collaudo SQLCipher su dispositivo né la verifica visiva Android (AUD-03 e RES-19).

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
- Su API 37 due test UI si arrestano in Espresso prima delle asserzioni; il dettaglio e RES-17 nel [tracker](../PROJECT_STATUS.json).
- La pubblicazione su tag è verificata dalla release v1.0.1 (run 37238450362).

Le prove non eseguite non sono considerate superate.

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
