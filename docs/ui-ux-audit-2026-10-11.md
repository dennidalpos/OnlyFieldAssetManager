# Revisione UI/UX adattiva — 11 ottobre 2026

## Decisioni

Revisione profonda approvata, con identità blu/turchese conservata. Quattro aree condivise da Android e Windows: Mappa, Inventario, Cablaggio e Progetto. Inventario raggruppa Dispositivi e Rack. Funzioni, dati e pacchetti restano compatibili.

L'audit preliminare ha confrontato sorgenti e immagini del 9 ottobre: etichette spezzate nella barra mobile, filtri permanenti sopra gli elenchi, navigazione disabilitata ma ancora ingombrante negli editor e intestazioni ridondanti sulla mappa Android. Le immagini precedenti sono una baseline, non una verifica della nuova versione.

## Interventi

| ID | Priorità | Intervento | Stato |
| --- | --- | --- | --- |
| UX-07 | P1 | Quattro aree, stato degli elenchi, editor mobile a finestra intera, filtri compatti, gerarchia mappa e footer documenti/wizard | Completato |
| UX-08 | P2 | Regressioni di navigazione/bozze, matrice visiva e prove native isolate | Completato |

La barra inferiore Android resta nelle schermate di consultazione e scompare negli editor a pagina intera e nello scanner. Inventario conserva vista, ricerca e filtri durante la sessione; gli stati salvabili e le posizioni degli elenchi sono isolati per progetto e schermata. Il cambio progetto li azzera. Windows usa la stessa organizzazione in rail/sidebar e conserva il controllo delle modifiche non salvate.

I filtri si aprono su richiesta; quelli attivi restano visibili e rimovibili. Le schede usano 12 dp di padding interno. La mappa compatta ha una sola intestazione, ricerca e aggiunta dirette; Elenco e strumenti secondari sono nel menu. Gli editor, Documenti e il wizard usano azioni persistenti con gestione della tastiera e delle barre di sistema.

## Verifica

Baseline DesktopUxLayoutTest, MasterDetailTest e MapNavigationUiTest: `BUILD SUCCESSFUL`, prima delle modifiche.

- Desktop: 34 test Compose superati, zero fallimenti/errori/saltati. Coperti navigazione, filtri/ricerca conservati, protezione della bozza, finestre 1024×768 e 1360×860, temi e testo 1,0/1,3, mappa, configuratori e pannello occupato.
- Localizzazione: 3 MessagesTest superati. Compilazione Android e APK instrumentation riusciti; ultima verifica Gradle `BUILD SUCCESSFUL in 25s`.
- Android reale: moto g86, API 36. AdaptiveNavigationNativeTest `OK (1 test)` con Room in memoria: quattro aree, filtro e ricerca dopo cambio vista, salvataggio con tastiera e footer Documenti raggiungibile.
- Matrice nativa: `OK (4 tests)`; progetti/inventario e mappa a 360/412/600/840/1024 dp simulati, temi chiaro/scuro, testo 1,0/1,3; footer dialogo con tastiera reale; configuratore denso a 360/412 dp nelle otto combinazioni. Queste densità sul telefono non equivalgono a tablet fisici.

La regressione desktop ha individuato un errore reale: SecondaryTabRow veniva misurata a larghezza zero quando il pannello elenco restava composto dietro l’editor. Le schede vengono ora composte soltanto con spazio sufficiente; la suite verifica la bozza conservata. Due errori iniziali della fixture Android (repository file senza chiave e selettore non univoco) sono stati corretti senza attribuirli al prodotto.

Comando finale: `.\gradlew.bat :pc:app:test --tests '*AdaptiveNavigationTest' --tests '*DesktopUxLayoutTest' --tests '*MasterDetailTest' --tests '*MapNavigationUiTest' --tests '*FloorMapUiTest' --tests '*SecondaryModulesUiTest' --tests '*ConfiguratorUxTest' --tests '*BusyDialogTest' :shared:core:test --tests '*MessagesTest' :mobile:app:assembleDebug --no-parallel --max-workers=1`.

Instrumentation con `adb install -r` e `adb shell am instrument -w -r -e class <classe> com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner`; `-e matrixEvidence true` abilita le immagini. Nessun connectedDebugAndroidTest o cancellazione dati.

Evidenze locali in `build/reports/ux-20261011`: XML finali, log nativi, immagini, APK precedente e backup prima/dopo. Confronto SHA-256 dei 13 file privati: nessuna aggiunta/rimozione, invariati database, preferenze, chiavi e dati; cambia solo `files/profileInstalled` (marcatore runtime).

RES-19 e RES-23 continuano a distinguere prove automatiche, larghezze simulate, rotazione reale, TalkBack e tablet fisici. Nessuna chiusura dedotta dalle sole immagini.

## Fonti ufficiali

Consultate nella sessione dell'11 ottobre 2026:

- [Accessibilità Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): target touch di almeno 48 dp e semantica.
- [Classi di finestra](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes): adattamento allo spazio disponibile.
- [Window insets](https://developer.android.com/develop/ui/compose/system/insets): tastiera e barre di sistema.
- [Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3): gerarchia di superfici, testo e azioni.
- [Layout elenco/dettaglio](https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail) e [layout Windows adattivi](https://learn.microsoft.com/en-us/windows/apps/design/layout/responsive-design).

Immagini della sessione elencate in `current-evidence.json`: 44 Android (inclusi avvio e tre flussi), 21 mappa/dialogo, 16 configuratore e 32 desktop. Nel dump configuratore sono presenti anche file storici: soltanto `dense-*` e `discard-*` appartengono a questa esecuzione. Ispezionati campioni compatti con testo 1,3, tastiera e desktop largo/scuro; nessuna sovrapposizione delle azioni nei campioni. Rimosso il solo pacchetto instrumentation, assente nella baseline; app principale riaperta con `Status: ok`.
