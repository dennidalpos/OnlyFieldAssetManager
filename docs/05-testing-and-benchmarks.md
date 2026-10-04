# Verifica

## Suite automatica

La verifica JVM e Compose usa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest
```

Le suite coprono dominio, serializzazione/cifratura, fusione, migrazioni Room, storage, interoperabilita, configuratore, documenti e UI Desktop. Il workflow CI esegue lo stesso perimetro prima di creare gli artefatti.

## Progetto demo

[fixtures/demo/onlyfield-demo.ofam](../fixtures/demo/onlyfield-demo.ofam) si importa da Importa .ofam su Android e Windows. Si rigenera con:

```powershell
.\gradlew.bat :shared:exchange:demoPackage
```

Contenuto («Demo OnlyField»):

- Sede Nord e Sede Sud, ognuna con Piano terra e Primo piano.
- Per piano: rack RK da 42 U con patch panel PP (24 RJ45, U42) e due switch A/B (24 porte, metà PoE, 4 SFP+, U40 e U39). Sulla mappa: sei prese a muro PR da due porte e due access point.
- Ogni porta di presa arriva a una porta del patch panel (cavi H); il patch panel va agli switch (cavi PC). Gli AP sono collegati alla presa, quindi il percorso fino allo switch è completo.
- 6 switch su 8 sono cablati. Restano vuoti SW-S0-B e SW-S1-B. Ogni switch cablato ha un uplink in fibra verso un altro piano (FO-01…05, montanti e dorsale Nord–Sud), utile anche per provare monconi e Vai a.

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

La suite è stata ampliata dopo i problemi emersi durante l'integrazione. Tre aspettative obsolete di `MasterDetailTest` sono state aggiornate alle azioni Annulla modifiche e Salva modifiche; nessun controllo è stato disabilitato. I sei scenari di `ConfiguratorUxTest` coprono creazione essenziale, scelta esplicita della business unit, valori conservati dopo chiusura dei dettagli, errori che aprono la sezione, applicazione e ricerca dei modelli, riduzione di gruppi collegati con consenso e conservazione degli ID.

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
- `QuickAddTest`: switch da preset con porte e posizione sulla mappa, rack vuoto con altezza scelta, business unit obbligatoria finché non scelta;
- `ObjectPickerUiTest` riscritto: preset con Aggiungi immediato, tipo senza menu con un tocco, tipologia personalizzata.

`DesktopUxLayoutTest` e `ConfiguratorLayoutTest` sono stati aggiornati al nuovo flusso e alla sezione Altro. Totale 247 test (Core 88, Exchange 37, Desktop 94, Android JVM 28).

Controllo visivo:

- **Desktop**: render Compose a larghezza telefono di icone, mappa chiara e scura, pannello porte da 48, elevazione rack e finestra di inserimento.
- **Android**: emulatore Pixel 9 con APK debug. Verificati barra in basso, Altro, mappa con bordo e griglia, inserimento rapido di rack e switch in U42, elevazione e porte a tutta larghezza. Corretti durante la prova i pulsanti segmentati ancora tondi e uno switch da 24 porte diviso in due fasce.
- **Da fare**: la prova sul telefono reale rientra in RES-19.
