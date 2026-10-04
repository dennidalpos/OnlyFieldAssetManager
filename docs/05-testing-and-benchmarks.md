# Verifica

## Suite automatica

La verifica JVM e Compose usa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest
```

Le suite coprono dominio, serializzazione/cifratura, fusione, migrazioni Room, storage, interoperabilita, configuratore, documenti e UI Desktop. Il workflow CI esegue lo stesso perimetro prima di creare gli artefatti.

## Limiti noti

- Il collaudo su telefono, multitouch, fotocamera, scanner e lettore USB richiede hardware reale: [checklist](testing/hardware-checklist.md).
- Su API 37 due test UI si arrestano in Espresso prima delle asserzioni; il dettaglio e RES-17 nel [tracker](../PROJECT_STATUS.json).
- Una release verificata richiede una vera esecuzione CI su tag: RES-01.

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
