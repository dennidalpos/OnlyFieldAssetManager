# Audit critico UI/UX

Data: 3 ottobre 2026

Questo documento raccoglie i problemi di usabilità rilevati sull'app Android (`:mobile:app`), sull'editor Windows (`:pc:app`) e sul pacchetto portable, ordinati per gravità. Per ciascun problema indica il punto nel codice e la fase del piano di rework che lo risolve (1a/1b/1c Windows, 2 Android).

Riferimento dei percorsi:
- Android: `mobile/app/src/main/java/com/onlyfield/assetmanager/`
- Windows: `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/`

## Sintesi

| Area | Diagnosi | Fase |
|---|---|---|
| Android | L'intera UI è una sola funzione `@Composable` di circa 2.300 righe. Non c'è navigazione e ogni funzione è un `AlertDialog`. Molte azioni agiscono sul "primo elemento" senza chiedere quale. Mancano le funzioni di creazione di base. | 2 |
| Windows | Il dato più grave: i dialog di modifica cancellano i campi che non mostrano. Inoltre i collegamenti tra entità si fanno digitando ID grezzi, le azioni di progetto stanno nell'ultima di 9 tab e manca la conferma sulle eliminazioni. | 1c |
| Portable | L'eseguibile esiste, ma solo in `pc/app/build/compose/binaries/main/app/`. I dati non sono portable e il JDK configurato non contiene `jpackage`. | 1a/1b |

---

## 1. Bloccanti / perdita dati

| # | Piattaforma | Problema | Dove | Correzione |
|---|---|---|---|---|
| B1 | Windows | Molti dialog di modifica creano un oggetto nuovo invece di usare `copy()`. Al salvataggio i campi senza controllo tornano al default. Esempi: per l'apparato si perdono `siteId`, `deviceModelId` e `rackSide`; per il rack `areaId` e `depthMm`; per il cavo velocità osservata, orientamento, percorsi e osservazione. Lo stesso vale per VLAN, Subnet, Interfaccia logica, WAN e Alimentazione. | `ui/InventorySection.kt:474-489`, `ui/RackSection.kt:388-394`, `ui/CablingSection.kt:259-270`, `ui/NetworkLogicalSection.kt`, `ui/PowerBadgeSection.kt` | Passare a `existing.copy(...)` e spostare la mappatura dal form all'entità in funzioni pure testate (1c) |
| B2 | Android | Diverse azioni usano in silenzio il primo elemento disponibile: "+ Colloca" usa il primo apparato, "+ Cavo Test" le prime due porte, "+ Int. Logica" il primo apparato con IP e VLAN fissi, e così Config, CCTV, Alimentazione, Planimetria (prima area) e Cartografia (prima area). | `MainActivity.kt:1257`, `:1609-1623`, `:1758-1762`, `:1808`, `:1849`, `:1952-1961`, `:1154`, `:1314` | Selettori espliciti per apparato, porta e area (2) |
| B3 | Android | "Svuota Cestino" cancella tutto in modo definitivo senza chiedere conferma. | `MainActivity.kt:2068` | Dialog di conferma esplicito (2) |
| B4 | Android | "Elimina" su un allegato è una cancellazione definitiva che non passa dal cestino. | `MainActivity.kt:1130` | Passaggio dal cestino e conferma (2) |
| B5 | Windows | Nel dialog di confronto import, "Sostituisci Copia" salva con la password del progetto aperto invece che con quella del pacchetto, e non aggiorna la validazione. | `DesktopApp.kt:690` | Correzione puntuale (1c) |
| B6 | Windows | Il pulsante "Carica Fixture Android" cerca `fixtures/...` relativo alla cartella di lavoro e non gestisce eccezioni: se l'app parte dall'exe va in crash. | `DesktopApp.kt:241-247` | Rimozione dalla UI di produzione (1b) |
| B7 | Android | Il ViewModel non è creato con `viewModels()` e i form usano `remember`: ruotando il dispositivo si perdono il progetto aperto e i dati dei form. | `MainActivity.kt:97`, `:130-166` | `viewModels { factory }` e `rememberSaveable` (2) |

## 2. Navigazione e struttura

| # | Piattaforma | Problema | Correzione |
|---|---|---|---|
| N1 | Android | Non c'è navigazione: l'app ha solo lo stato "lista progetti / dashboard" e il tasto Back esce dall'app. L'unico modo per tornare alla lista è un pulsante in fondo alla pagina. | Navigation Compose con route per ogni sezione (2) |
| N2 | Android | La dashboard ha 18 pulsanti dello stesso peso su 5 righe a scorrimento orizzontale, senza icone: quelli fuori schermo non si vedono. | Griglia di sezioni con icona e contatore; azioni di progetto nel menu overflow (2) |
| N3 | Android | Editor complessi (cablaggio, rete, planimetria, operazioni batch) sono compressi in `AlertDialog` ad altezza fissa (80–400 dp), a volte annidati. | Schermate dedicate (2) |
| N4 | Android | Rack, apparati, modelli e credenziali non si possono toccare: manca il dettaglio e manca la modifica. | Schermate di lista e dettaglio con modifica (2) |
| N5 | Android | Alcune funzioni non hanno UI: creazione di apparati, sedi, aree e porte; eliminazione di rack e apparati; rinomina ed eliminazione del progetto; elenco delle credenziali. | (2) |
| N6 | Windows | Le azioni di progetto (Nuovo, Apri, Esporta, Documenti, Password) stanno solo nell'ultima tab, "Storage & Progetto". | Toolbar sempre visibile, `MenuBar` con scorciatoie (1c) |
| N7 | Windows | 9 tab con emoji in una `ScrollableTabRow`. | `NavigationRail` laterale (1c) |
| N8 | Windows | Le etichette di alcune tab promettono funzioni assenti ("LAG & WAN/VPN", "Port-VLAN", "Videosorveglianza"). | Etichette allineate a ciò che esiste (1c) |
| N9 | Windows | `DesktopApp.kt` (827 righe) contiene stato, logica e dialog insieme, senza un contenitore di stato. | `DesktopAppState` e `ui/dialogs/` (1c) |

## 3. Input e collegamento tra entità

| # | Piattaforma | Problema | Correzione |
|---|---|---|---|
| I1 | Windows | Per collegare entità bisogna digitare ID grezzi: "ID Porta A/B", "ID Apparato", "ID Target", "ID Porta" (PoE). L'inventario però non mostra gli ID, quindi non c'è nulla da copiare. Punti: `ui/CablingSection.kt:185-201`, `:558-572`, `ui/NetworkLogicalSection.kt:281`, `:432`, `:505`, `ui/PowerBadgeSection.kt:147`, `:266`, `:374`. | Selettore con ricerca (1c) |
| I2 | Windows | I valori enum vengono troncati con `.take(4)`, così alcune scelte non sono selezionabili: ad esempio i mezzi CONSOLE/OTHER, alcuni tipi di alimentazione e alcune categorie badge. | Menu a tendina con tutti i valori (1c) |
| I3 | Entrambe | I numeri sono campi di testo senza tastiera numerica e i valori non validi vengono sostituiti in silenzio da un default (`?: 42`, `?: 10`, `?: 0.5f`). Non c'è validazione di IP, CIDR, MAC, VLAN 1–4094 né controllo della U rispetto all'altezza del rack. | Campo numerico con `isError` e messaggio di aiuto (1c, 2) |
| I4 | Android | Le scelte enum si fanno con pulsanti che ciclano tra i valori: classificazione, mezzo, orientamento. Il ciclo del mezzo non raggiunge mai CONSOLE/OTHER/UNKNOWN. | Menu a tendina (2) |
| I5 | Android | Valori demo precompilati nei campi (VLAN_GUEST, 192.168.10.0/24, Config_Base_v1, WAN_TIM_FTTH) e pulsanti di prova ("+ Cavo Test", "+ Mappa Telecamera Test"). | Rimozione (2) |
| I6 | Android | La categoria del modello è fissa a `NETWORK_SWITCH` e il tipo di credenziale è fisso a `PASSWORD`. | Selettori (2) |

## 4. Feedback, errori, azioni distruttive

| # | Piattaforma | Problema | Correzione |
|---|---|---|---|
| F1 | Entrambe | Nessuna azione "Elimina" chiede conferma, e lo stesso vale per Fusione, Sostituzione, modifica batch e rimozione della password. | Dialog di conferma (1c, 2) |
| F2 | Windows | La validazione mostra solo i primi 5 problemi, solo nella tab Progetto, con enum grezzi (`[STRUCTURAL_ERROR]`), con lo stesso colore per errori e avvisi e tutti etichettati come "avvisi". | Pannello completo raggruppato per severità e contatori nella barra di stato (1c) |
| F3 | Android | L'unico canale di feedback è il banner "Stato Persistenza", che non si chiude mai e mostra il testo grezzo delle eccezioni (`e.message`). | Snackbar con azione "Annulla" ed errori in italiano (2) |
| F4 | Entrambe | Non ci sono indicatori di avanzamento durante import ed export. | (1c, 2) |
| F5 | Android | La validazione dei form è silenziosa: con un campo obbligatorio vuoto il pulsante semplicemente non fa nulla. | Errori mostrati sul campo (2) |
| F6 | Android | Il pulsante "Annulla" copre solo le operazioni che passano dal cestino, e il suo stato abilitato può non essere aggiornato. | Annulla contestuale sulla Snackbar (2) |
| F7 | Windows | Con una password di import errata compare un errore ma non si può riprovare. | Nuovo tentativo nello stesso dialog (1c) |
| F8 | Android | Un progetto protetto da password si apre senza chiederla. | Richiesta della password all'apertura (2) |

## 5. Lingua, etichette, terminologia

- **ID ed enum grezzi visibili all'utente:**
  - Android: `ID: <uuid>` sulle card dei progetti, `Numerazione: TOP_TO_BOTTOM`, `[NETWORK_SWITCH]`, `Tipo: IMAGE`, `[PRIMARY_A]`, `[itemType]` nel cestino.
  - Windows: `[ID: …]`, `Apparato ID: …`, `Porta ID: …`, `cat.name` nei filtri.
  - Correzione: etichette italiane condivise in `:shared:core` (1c, 2).
- **Italiano mescolato a inglese e a termini di sviluppo:** "Floorplan", "Batch Edit", "Match", "Feeds Alimentazione", "Running Config", "(W03)", "Desktop Windows W02", "Zero Secret Leakage garantito".
- **Testi hardcoded:** non ci sono risorse di stringhe, né `strings.xml` né equivalenti desktop. La localizzazione resta fuori scope; nel frattempo la terminologia viene uniformata.
- **Colori fissi:** `Color.Gray`, `Color.Red` ed esadecimali ignorano il tema e la modalità scura (Android).

## 6. Funzioni finte o incomplete (fuori scope del rework UX, da pianificare)

- **Windows, PDF:** il "PDF" è testo semplice con un'intestazione `%PDF-1.4` e non è un PDF valido (`DesktopDocumentManager.kt:56-105`).
- **Windows, allegati:** i byte degli allegati non vengono mai inclusi nel pacchetto `.ofam`, perché si passa `attachments = emptyMap()`. Il selettore file per gli allegati filtra solo `.ofam`.
- **Windows, cestino:** vive solo in memoria e si perde alla chiusura. Solo apparati e rack passano dal cestino; tutto il resto viene cancellato in modo definitivo.
- **Windows, lock e cartella dati:** il lock di progetto non viene mai acquisito dalla UI. La cartella dati configurabile documentata in `04-desktop-storage-interop.md` non è raggiungibile dall'interfaccia.
- **Android, allegati:** un allegato è solo un nome digitato, con tipo fisso `image/jpeg`; non c'è fotocamera né selettore file.
- **Cartografia (entrambe):** è un segnaposto che non scarica tiles.
- **Planimetria:** si posiziona digitando coordinate X/Y e l'immagine di sfondo non viene disegnata (Windows).

## 7. Pacchetto portable Windows

| # | Problema | Correzione |
|---|---|---|
| P1 | `createDistributable` produce `OnlyFieldAssetManager.exe` solo in `pc/app/build/compose/binaries/main/app/OnlyFieldAssetManager/`, una cartella ignorata da git e che non viene consegnata. La documentazione indicava un percorso sbagliato (`appImage/`). | Task `:pc:app:packagePortable` che produce `dist/OnlyFieldAssetManager/` e uno ZIP (1a) |
| P2 | I dati vanno in `%USERPROFILE%\.onlyfield_asset_manager`, quindi il programma non è portable. | Cartella `data\` accanto all'exe, con ripiego sul profilo utente se non è scrivibile (1b) |
| P3 | `org.gradle.java.home` punta alla JBR 25 di Android Studio, che non contiene `jpackage.exe`. | Toolchain JDK 21 dedicato al packaging (1a) |

---

## Esito del rework (3 ottobre 2026)

**Windows (U01).**
- Struttura:
  - navigazione laterale;
  - toolbar sempre visibile con Nuovo, Apri, Esporta, Documenti e Password;
  - menu File/Vai con scorciatoie (Ctrl+N, Ctrl+O, Ctrl+E, Ctrl+P, Ctrl+W, Ctrl+1…9);
  - barra di stato con i problemi di validazione e il percorso dei dati;
  - sezione Progetto per gestire business unit e aree.
- Collegamenti tra entità:
  - porte, apparati e target si scelgono con selettori dotati di ricerca;
  - le scelte enum mostrano tutti i valori con etichette in italiano.
- Moduli:
  - quelli di modifica conservano i campi non mostrati (B1);
  - la validazione è visibile sul campo.
- Rack: si possono scegliere solo le posizioni U libere.
- Planimetrie: gli elementi si posizionano con un clic sulla planimetria.
- Conferma richiesta su tutte le eliminazioni.
- Correzioni: B5 corretto e B6 rimosso.

**Android (U02).**
- Navigazione:
  - schermate dedicate, con back stack conservato nel ViewModel;
  - il tasto Indietro di sistema funziona e la rotazione non perde lo stato (B7).
- Home: una griglia di sezioni con contatori sostituisce i 18 pulsanti.
- Creazione e modifica: ora possibili per apparati, porte, sedi/aree, rack, modelli, cavi, percorsi, permutazioni, rete, alimentazione, badge e credenziali.
- Selezione degli elementi:
  - le azioni sul "primo elemento" sono state sostituite da selettori con ricerca (B2);
  - i pulsanti di prova e i valori demo sono stati rimossi.
- Feedback: una snackbar con «Annulla» segue ogni modifica, con indicatore di avanzamento durante import/export.
- Conferme: richieste per tutte le azioni distruttive (B3); gli allegati sono veri file scelti dal dispositivo (B4).
- Password: i progetti protetti la chiedono all'apertura.
- Esportazioni: lo stream viene aperto dentro la coroutine. In precedenza veniva chiuso prima della scrittura.

**Condiviso.**
- Le operazioni di modifica (`ProjectEdits`), i form e le etichette italiane sono in `:shared:core` e vengono usati da entrambe le app.

**Residui chiusi (3 ottobre 2026).**
- Il PDF di Windows è un vero PDF multipagina.
- Gli allegati viaggiano nei pacchetti, cifrati se il progetto è protetto.
- Il cestino di Windows è persistente e ogni modifica si annulla con Ctrl+Z.
- I messaggi di validazione sono in italiano.
- Su Windows le planimetrie mostrano l'immagine di sfondo e gli allegati si aprono con l'applicazione predefinita.

RES-07 (mappe su Android) è stato deciso il 3 ottobre 2026 ed è pianificato come F05.

## Fase v1.1: avvio

- **Nuovo sito (O01)**: una procedura guidata condivisa (`core.onboarding`) sostituisce il dialog «Nuovo progetto». I passi sono progetto/cliente → sede → prima area → primo apparato (saltabile) → password (facoltativa). Su Android è una schermata, dove il tasto Indietro torna al passo precedente; su Windows è una finestra a passi.
- **Schermata iniziale (O02)**: al primo avvio due azioni grandi, «Inizia un nuovo sito» e «Apri un pacchetto ricevuto (.ofam)», con una riga di spiegazione. Quando ci sono progetti, in cima compare «Continua: «ultimo progetto»» (il più recente). Android: `ProjectsScreen`; Windows: `WelcomeCard`.
- **Home progetto Android (O03)**: in alto un campo di ricerca sugli apparati (nome, IP, etichetta, alias, MAC; stessi campi dell'Inventario, filtrati sul progetto già in memoria) che apre direttamente la scheda, e l'azione rapida «+ Aggiungi apparato». Sotto restano la scheda di controllo e la griglia delle sezioni. Le azioni «Foto» e «Scansiona» si aggiungono con F01 e F02.

## Fase v1.1: refactor UI

- **Android, modifiche a pagina intera (R01)**: tutti gli editor di entità (inventario, sedi/rack/modelli, cablaggio, rete/alimentazione, allegati/credenziali) usano `EditScreen`, una pagina intera disegnata sopra la schermata corrente con «Annulla» e «Salva» nella barra in alto. Il form scorre sopra la tastiera (`imePadding`) e, se ci sono modifiche, l'uscita con Indietro o Annulla chiede conferma; i campi segnalano le modifiche tramite `LocalMarkDirty`. La rotazione non ricrea l'activity (`android:configChanges`, come indicato dalla guida Android per le app Compose), quindi i dati inseriti restano. Restano dialog solo le conferme, le password e le scelte brevi (`FormDialog`: rinomina progetto, password). Limite noto: se il sistema chiude il processo mentre un form è aperto, le modifiche non salvate si perdono.
- **Windows, pannello laterale (R02)**: ogni sezione è ospitata da `MasterDetailHost`: a sinistra la lista con ricerca, a destra l'editor aperto (`EditPanel`), che sostituisce i `FormDialog` delle sezioni. Ctrl+S salva ed Esc chiude; la chiusura di un form modificato chiede conferma. I form continuano a usare `core.forms` e a copiare l'entità esistente, quindi i campi nascosti restano. Restano dialog: nuovo sito, documenti, password, validazione e confronto all'import. Limite noto (RES-08): se il form è modificato e si sceglie un altro elemento nella lista, l'editor passa al nuovo elemento senza avviso.
- **Icone e tema scuro (R03)**: le emoji della navigazione sono sostituite dai Material Symbols Outlined (Apache 2.0, repository `google/material-design-icons`), come raccomanda la guida Android al posto della libreria `material-icons`, non più mantenuta. Su Android sono vector drawable in `res/drawable/ic_*.xml`, su Windows `ImageVector` in `pc.ui.SymbolIcons`. Android segue il tema del sistema (schemi Material3 chiaro/scuro e tema della finestra in `values`/`values-night`); Windows ha «Visualizza › Tema scuro», salvato in `settings.properties` nella cartella dati. I colori fissi rimasti riguardano solo superfici a sfondo proprio (blocchi del rack per categoria, canvas della planimetria) e sono leggibili in entrambi i temi.

## Fase v1.1: funzioni

- **Foto dalla fotocamera (F01)**: «Foto» nella home (foto del progetto), nella scheda apparato, nella scheda rack e nel menu di ogni area (Sedi e aree). Lo scatto usa l'app fotocamera di sistema (`ActivityResultContracts.TakePicture`) e scrive direttamente nella cartella allegati del progetto tramite `FileProvider` (`res/xml/file_paths.xml`); l'allegato è collegato all'elemento aperto (`targetType`/`targetId`). La scheda apparato elenca «Foto e allegati»; gli elenchi allegati di Android e Windows indicano l'elemento collegato (`ProjectIndex.attachmentTarget`). Le foto viaggiano nel `.ofam` come gli altri allegati. Se lo scatto viene annullato, il file vuoto viene eliminato.
