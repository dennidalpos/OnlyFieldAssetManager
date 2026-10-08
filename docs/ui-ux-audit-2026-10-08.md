# Revisione critica UI/UX — 8 ottobre 2026

## Perimetro e decisioni

Revisione profonda autorizzata: percorso mappa → oggetto → porte e collegamenti. Smartphone con scheda espandibile; tablet e PC con navigazione adattiva. Funzioni, database e scambio invariati. Il tema conserva colori e forme squadrate.

L'audit iniziale riguarda codice e screenshot preesistenti; questi ultimi non costituiscono collaudo della nuova versione. Baseline mirata DesktopUxLayoutTest, MapNavigationUiTest e MasterDetailTest: BUILD SUCCESSFUL.

## Rilievi e criteri di chiusura

| ID | Priorità | Problema | Criterio |
|---|---|---|---|
| UX-01 | P1 | Stato della vista locale al canvas; navigazione poco adattiva | Centro normalizzato, zoom, selezione e contenitori conservati durante cambi di vista/dimensione; progetti isolati; navigazione stabile aprendo editor |
| UX-02 | P2 | Dettagli sotto la mappa occupano troppo spazio; strumenti in competizione | Riepilogo compatto con Modifica/Foto/Espandi; dettagli espansi e Riduci; pannello laterale da 840 dp di workspace; ricerca e aggiunta dirette |
| UX-03 | P2 | Padding e conferme differenti, chiusure e aggiunte duplicate, cestino esposto | Spaziatura condivisa, form limitati, azioni persistenti, eliminazione nel menu confermata, icone accessibili e target touch da 48 dp |
| UX-04 | P2 | Evidenze incomplete su piattaforme e dimensioni | Regressioni funzionali e layout, screenshot, verifiche native disponibili; evidenziare esplicitamente i collaudi mancanti |

## Regole di presentazione

- Cinque accessi compatti: Mappa, Dispositivi, Rack, Cablaggio, Progetto. Android: rail con larghezza almeno 600 dp e altezza almeno 480 dp; PC: sidebar da 1200 dp, rail sotto tale soglia.
- Stato della mappa negli host, solo per la sessione. Centro in coordinate normalizzate indipendenti dal renderer e dall'arrivo asincrono dello sfondo. Nessuna serializzazione nel pacchetto o nel database.
- Modifica e Foto visibili; eliminazione nelle azioni secondarie, sempre confermata. Strumenti della mappa in un menu. Intestazione del contesto unica nel workspace.
- Spaziature 4/8/12/16/24 dp; pagina 16 dp compatta e 24 dp ampia. Form testuali entro 640 dp; disegni tecnici senza questo limite. Header compatti e footer raggiungibili con tastiera aperta.
- La scheda estesa del telefono sostituisce temporaneamente la vista canvas; Riduci ripristina centro, zoom e selezione. La modalità laterale conserva lo stesso stato.

## Verifica

Matrice pianificata: 360/412/600/840/1024 dp, testo 1,0/1,3 e chiaro/scuro; PC 1024×768 e 1360×860. Verificare anche ritorno dopo editor, eliminazione/selezione obsoleta, cambio progetto, errore/riprova, tastiera e rotazione. TalkBack audio/focus e collaudi reali non eseguiti rimangono nei residui RES-19/23, senza dedurre l'esito dai test Desktop.

Implementati UX-01/02/03. UX-04 concluso per le regressioni automatiche qui elencate; i collaudi manuali non svolti restano in RES-19/23.

- Suite JVM/Compose: 128 core, 96 exchange, 242 PC e 123 Android, senza errori né test saltati (589 complessivi); riepilogo conservato in `build/reports/ux-native-20261008/jvm-summary.json`. Dopo gli ultimi ritocchi sono state ripetute solo le regressioni interessate, non la suite intera.
- PC: 20 combinazioni della mappa (360/412/600/840/1024 dp × temi × testo 1,0/1,3), più DesktopApp a 1024×768 e 1360×860. Screenshot in `pc/app/build/reports/ux-map` e `pc/app/build/reports/ux`. Verificati Espandi/Riduci, ritorno alla vista, centro dopo ridimensionamento, isolamento del progetto e bozza cavo nel passaggio 1000→600→1000 dp.
- Android moto g86, API 36: nove scenari distinti con esito positivo distribuiti fra i report `instrumentation-final.txt`, `window-bounds.txt` e `regression-final.txt`. Comprendono 20 combinazioni della mappa, 8 combinazioni dei flussi porte, 16 del footer con/senza Dettagli, tastiera reale con campo finale/Salva raggiungibili, cancellazioni rack/apparato con errore e riprova. Matrice tramite `LocalDensity`, host Compose e progetti isolati: non equivale a tablet fisici o rotazione reale.
- Difetti riprodotti e corretti: footer fuori finestra nei dialoghi Android lunghi (Compose 1.7.5 misurava l’altezza del display), bozza porta persa passando a dettagli compatti, riepilogo cavo che ereditava le azioni del contenitore e nomi delle destinazioni Android difformi dal PC. `ContentDialog` limita l’altezza alla finestra corrente e gestisce IME/safe area; nessun aggiornamento di dipendenze.
- APK debug e APK test compilati; installazione con `adb install -r`, nessuna disinstallazione dell’app principale. Database e WAL confrontati con le evidenze hardware precedenti: identici. L’hash complessivo non è invariato: indice temporaneo SQLite (`-shm`) e `profileInstalled` cambiano con esecuzione/installazione, quindi non vengono dichiarati dati immutati sulla sola base dell’aggregato.
- Ritocco finale: altezza di riga del numero porta corretta per evitare sovrapposizione al simbolo con testo 1,3. Regressione nativa `OK (1 test)` in `port-label-final.txt`, 16 combinazioni e screenshot `port-label-final.png` ispezionato. APK test rimosso (`Success`), app principale riaperta (`Status: ok`); report di cleanup conservati.
- Non verificati: intera matrice nativa degli editor, rotazione fisica, TalkBack audio/focus, tutte le sequenze da tastiera nell’EXE e tablet reali. RES-19/23 restano aperti. Commit e push su main autorizzati dall’utente alla consegna per cambio sessione; esito nella cronologia Git.

### Comandi verificati

```powershell
.\gradlew.bat :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1
.\gradlew.bat :shared:core:test --tests '*MessagesTest' :pc:app:test --tests '*MapNavigationUiTest' --tests '*FloorMapUiTest' --tests '*DesktopUxLayoutTest' :mobile:app:assembleDebug --no-parallel --max-workers=1
```

Le prime suite core/exchange sono state eseguite nella verifica generale; i comandi qui sopra sono i passaggi finali riusciti. Native: `adb install -r` dei due APK, poi `adb shell am instrument -w -r -e class <classe o classe#metodo> -e matrixEvidence true com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner`. I report conservano classe, metodo ed esito; nessun `connectedDebugAndroidTest` usato.

## Fonti ufficiali

- [Android: navigazione adattiva](https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation).
- [Android: dimensioni della finestra](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes).
- [Compose: accessibilità e target touch](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
- [Microsoft: spaziatura dei contenuti](https://learn.microsoft.com/en-nz/windows/apps/design/style/spacing).

- [Compose DialogProperties](https://developer.android.com/reference/kotlin/androidx/compose/ui/window/DialogProperties): finestra Android edge-to-edge con gestione esplicita di safe area e IME nel contenuto.
