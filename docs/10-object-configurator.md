# Configuratore

`shared/configurator` fornisce alle due app le viste Compose. Il core mantiene geometria, hardware, riconciliazione porte e tracciamento; la UI salva le operazioni annidate come una sola modifica annullabile.

Rack, apparati e cavi possono usare modelli di progetto. Un modello conserva la configurazione riutilizzabile, non posizione, identificativi operativi, collegamenti o credenziali. Applicarlo a un'istanza e sempre esplicito e conserva le porte riconosciute quando possibile.

`ConnectionGraph` segue cavi e passaggi interni. Una porta puo essere disponibile, avere un percorso completo, incompleto o un conflitto. Il colore accompagna sempre un testo; il verde indica solo continuita censita, non traffico reale.

Il percorso è solo fisico: cavi e passaggi interni, senza passi logici WAN/VPN (restano nel pannello del dispositivo). `core.forms.PortSummaries` riassume per una porta ciò che serve per etichettare e collegare: stato, cavo e numero di foto della porta e del cavo. `core.forms.PathSchematics` costruisce lo schema del percorso (`PathSchematic`): oggetti attraversati (`PathStation`, con porta di ingresso e di uscita per i passanti) uniti dai cavi (`PathSegment`, con etichetta), su entrambi i lati di un passante; se un solo estremo è attivo il disegno parte da quello; un estremo passivo o senza porta è una fine aperta. `PathSchematicView` (condiviso) lo disegna in verticale: apparati attivi pieni, passivi vuoti, estremo sconosciuto rosso, cavo con etichetta, mezzo, colore e lunghezza; l'oggetto della porta è evidenziato, gli altri hanno Apri. Il modello non dipende da Compose ed è pensato per il PDF (EVO-08). `CableLabels.suggest` propone l'etichetta del cavo (`SW-01/P5 – PP-02/P12`), compatibile con la stampa delle etichette PDF.

Il contratto `.ofam` 1 conserva hardware, modelli e porte; Room 2 li persiste.

## Inserimento rapido

Il censimento parte da menu precompilati. `ObjectPickerDialog` usa lo stesso flusso ovunque: mappa, Aggiungi in una U libera, Nuovo apparato e Nuovo rack.

1. **Tipo**: elenco raggruppato per famiglia, con icona e ricerca. Un solo tipo possibile salta l'elenco.
2. **Disegno e menu**: il disegno del tipo scelto precede i dati e segue in tempo reale il preset. Il corpo scorre; Aggiungi e Indietro restano raggiungibili.

   - nome proposto (`SW-03`), sempre modificabile;
   - preset porte;
   - per i rack l'altezza, predefinita 42 U;
   - la sede, solo se il contesto non la dà e ce n'è più d'una.

Aggiungi salva subito come una sola modifica annullabile. Aggiungi e modifica apre l'editor completo; una bozza nuova lì è già lavoro non salvato, quindi chiudere chiede conferma.

Regole del flusso:

- Tutti i tipi passano dalla conferma visiva con nome proposto e Aggiungi, anche quando non hanno menu hardware.
- I cavi aprono sempre l'editor, perché servono gli estremi.
- Una tipologia personalizzata nasce insieme all'oggetto.
- Un rack nasce vuoto e si riempie man mano.

La logica pura è in `core.forms.QuickAdd`: bozza con nome, preset, altezza e sede; dispositivo in una U del rack. È coperta da `QuickAddTest`.

## Disegno, dati essenziali e dettagli

`DeviceDrawing` apre le schede e gli editor prima dei campi: frontale con connettori, sagoma per terminali, vista Fronte/Retro per i passanti. Il retro mostra terminazioni e cavi soltanto sulle porte occupate. Il rack apre direttamente l’elevazione interattiva, con scorrimento limitato per mantenere raggiungibili nome e comandi.

Le aree sono riquadri; sezioni espanse e selettori aperti cambiano sfondo. Dimensioni, profondità e profondità di montaggio non compaiono più nella UI. I valori esistenti restano nel modello, nelle copie e nei documenti. Altezza e posizione U si modificano solo nel contesto rack; le U restano anche nei modelli di apparati montabili.

Le porte espongono subito Modifica disposizione e Porte PoE. La configurazione si salva con il footer dell’editor; dalla scheda rapida Configura apparato crea una bozza locale con Salva modifiche e annullamento protetto. Collega e Foto sono azioni esplicite immediate fuori dall’editor; dentro un editor i collegamenti confluiscono nella sua sessione.

In testa una riga di contesto mostra simbolo, tipo e posizione (`Terra › R1 › U10–11`); il nome è già nel titolo della finestra. **Dati essenziali** contiene nome, tipo, modello, etichetta fisica e stato operativo (per i rack l'altezza, per i cavi il mezzo). Il modello compare solo se esistono modelli applicabili o ne è già impostato uno. Il tipo imposta la categoria; la categoria compare in Hardware solo per tipi personalizzati, legacy e modelli.

Dopo il disegno, le sezioni pertinenti al tipo seguono questo ordine e, chiuse, mostrano un riepilogo; l'intestazione è rettangolare perché titolo e riepilogo su due righe non vengano tagliati dagli angoli arrotondati:

1. **Posizione**: contenitore, sede, piano, altezza U e, in rack, posizione U e lato. Se il piano cambia per un oggetto esistente compare un avviso. Dall'inventario la sede deve essere selezionata (la sezione si apre per l'errore); piano, rack e porte non sono obbligatori.
2. **Porte** (`24 porte · 4 occupate`).
3. **Rete**, aperta per gli apparati pertinenti: IP e MAC. **Identificativi**, richiudibile: numero di serie e alias.
4. **Hardware**.
5. **Note e rilievo**.
6. Sezioni dell'host passate con `extraSections`, ad esempio **Foto e allegati** dalla mappa.
7. **Altro**: Campi personalizzati (una scheda per campo) e Opzioni avanzate (Salva come modello). L'editor dei modelli mostra solo Campi personalizzati.

Le viste di lettura mostrano solo i dati registrati: pannello della mappa, dettaglio dispositivo e rack su Android. I campi vuoti restano disponibili in modifica. Il dettaglio dispositivo Android e il pannello mappa usano `DeviceDrawing`; il tocco apre la scheda rapida della porta.

Il contesto esplicito di piano o contenitore viene conservato. Rack, cavi e modelli usano la stessa gerarchia con campi pertinenti. **Dispositivi nel rack**, subito visibile, mostra il lato scelto con filtri Fronte/Retro e l'elevazione `RackElevation`. È condivisa con il dettaglio rack Android e la sezione Rack Windows: larghezza piena, un blocco per apparato con icona e nome, U libere come righe con «+». Il tocco su una U libera apre l'inserimento rapido in quella U (`RackUnitPicker`). Seguono le U libere come intervalli (`U libere: 1–9, 12–42`) e i dispositivi ancora Senza posizione U, così il conteggio del riepilogo coincide. Tutti i campi a scelta usano `SelectField`: bordo ed etichetta come i campi di testo, letto come un solo pulsante "etichetta: valore".

## Preset e porte

`core.forms.DevicePresets` contiene i preset predefiniti. Sono solo codice: non entrano nei pacchetti e non cambiano il contratto. Ogni preset è un insieme di menu con valori:

| Famiglia | Valori |
| --- | --- |
| Switch | 5/8/16/24/48 RJ45; uplink 0/2/4 SFP o SFP+; PoE nessuno, tutte, metà |
| Patch panel | 12/24/48 RJ45, LC o SC, fronte/retro accoppiati |
| Prese | 1/2/4 porte accoppiate |
| Scatola di giunzione | 1/2/4 passanti RJ45, LC o SC, fronte/retro accoppiati |
| Ponte radio | LAN1 RJ45 PoE (fronte) e RF1 (retro) accoppiati; la tratta radio è un cavo con mezzo Radio |
| Router, firewall | 4/8 LAN, 1/2 WAN, 0/1/2 SFP o SFP+ |
| AP, telecamere, telefoni, sensori | 1/2 RJ45 con PoE 802.3af/at/bt |
| Server, workstation | 1/2/4 NIC RJ45 o SFP+, porta di gestione |
| NAS, SAN | 1/2/4 LAN |
| NVR | 0/4/8/16 porte PoE e uplink |
| UPS, PDU, alimentatori | 6/8/12/24 prese C13 o Schuko |

Il preset si sceglie all'inserimento rapido oppure nella sezione Porte; se esistono già gruppi di porte la sezione li riassume e i menu si aprono con Cambia preset. Applicarlo sostituisce i gruppi di porte; le porte collegate richiedono ancora il consenso esplicito. Il budget PoE proposto vale 15,4 W per ogni porta PoE (uscita PSE IEEE 802.3af) e resta modificabile.

I gruppi di porte seguono tre passi: **tipologia** (`PortKind`: RJ45, SFP, SFP+, SFP28, QSFP28, LC, SC, console, C13, Schuko), **quantità**, **etichetta**. Le etichette possono essere brevi (`P1`, `X1`), di interfaccia (`Gi1/0/1`, `Te1/1/1`) o un prefisso libero, con anteprima dell'intervallo. Il prefisso libero si scrive e poi si conferma con Applica prefisso: solo allora la numerazione viene ricalcolata. La tipologia imposta connettore, mezzo, velocità e ruolo. I campi IP e MAC compaiono per apparati pertinenti oppure quando contengono valori da conservare/correggere. Un nuovo gruppo continua la numerazione dei gruppi con lo stesso prefisso. I prefissi restano non vuoti. Lato, velocità, ruolo, accoppiamento e combo sono nei dettagli avanzati.

Il pannello porte (`PortPanel`) imita il frontale: switch grandi con dispari sopra e pari sotto, patch panel su una riga, un blocco per gruppo e lato. Le celle interattive del disegno misurano 48 dp; i pannelli compatti conservano la resa sintetica.

**Modifica disposizione** sceglie una/due righe e scambia due celle dello stesso gruppo tramite tocco o trascinamento. L’ordine è solo grafico: nomi, ID e collegamenti rimangono invariati. Le fasce sulle finestre strette conservano le righe logiche. **Porte PoE** seleziona singole porte o gruppi e assegna/toglie il supporto af/at/bt; non modifica automaticamente la configurazione PoE già censita. I preset non sovrascrivono le personalizzazioni al salvataggio. Ordine e supporto personalizzato sono riutilizzabili nei modelli. `SchematicGeometry.arrangedGrid` suddivide il disegno in fasce mantenendo celle da 48 dp: 48 porte su un telefono stretto restano toccabili senza scorrimento orizzontale. Le viste compatte usano `portGrid` e nascondono la VLAN sotto 32 dp. Pieno indica occupata, bordo libera; ⚡ indica PoE (pieno se erogato, tenue se solo supportato), il numero in basso la VLAN, ! una porta da verificare. Il tocco apre la scheda rapida della porta (sotto). La pressione prolungata avvia la selezione multipla, con le azioni Seleziona tutte e Solo libere, e permette di applicare VLAN (access o trunk) e PoE a tutte le porte selezionate. All'inizio della selezione l'anteprima viene fissata nella sessione, così le porte non ancora salvate mantengono il proprio identificativo; `PortLogic` ignora comunque porte inesistenti e non crea righe orfane. La logica è in `core.forms.PortLogic` e usa le entità esistenti `PortVlanMembership`, `PoeMapping` e `Vlan`. Le VLAN mancanti vengono create a livello di progetto; la subnet si ricava da `Subnet.vlanId`.

### Scheda rapida della porta

`PortQuickDialog` si apre toccando una porta nel disegno dell'apparato: nel pannello della mappa, nell'editor e nella scheda dispositivo Android. Mostra in alto `SW-01 › P5` con l'etichetta fisica, il lato (per i passivi), lo stato e il connettore. Ogni modifica è una sola operazione annullabile; nell'editor entra nella sessione.

- **Porta libera**: Foto e **Collega a…** → apparato di destinazione (ricerca, Solo questo piano, prima quelli con porte dello stesso connettore; solo apparati con porte libere) → tocco su una porta libera del suo disegno → mezzo dedotto dalla porta ed etichetta cavo proposta → **Collega**.
- **Cablaggio in blocco** (`core.forms.BulkCabling`): scelta la porta di destinazione, **Porte in serie** collega in un'unica operazione annullabile le porte libere successive dei due apparati, sullo stesso lato (ad esempio fronte del patch panel), saltando quelle occupate; ogni cavo riceve l'etichetta proposta. Con **Poi passa alla porta successiva** la scheda resta aperta sulla porta libera seguente, con l'apparato di destinazione e la sua porta seguente già scelti: un tocco per cavo.
- **Foto dopo Collega**: subito dopo un collegamento singolo la scheda torna al riepilogo con l'invito «Collegato. Fotografa ora il cavo o la porta.» e **Foto cavo** in evidenza; nella modalità continua la riga «Collegato: etichetta» con Foto cavo resta in alto mentre si sceglie la coppia seguente. I pulsanti foto mostrano il numero di scatti già fatti.
- **Porta collegata**: etichetta, mezzo, colore e lunghezza del cavo; **Percorso** disegnato con `PathSchematicView`; **Foto porta**, **Foto cavo**, **Inserisci passaggio**, **Scollega** (rosso, con conferma: elimina il cavo con le sue foto).
- **Inserisci passaggio**: elenca i passanti liberi (stesso piano per primi, con il lato) oppure crea una **Nuova scatola di giunzione** accanto all'apparato; il cavo viene diviso in due tratte (`HardwareConfigurator.insertPassage`).
- **Dettagli porta** apre la pagina completa della porta.

Per patch panel, prese e scatole, **Tratta fissa sui retro** usa i frontali come etichette: risolve i rispettivi retro, controlla la loro disponibilità e mostra le porte fisiche con il lato prima di Collega. I frontali possono essere già occupati. **Cavetto frontale** si sceglie dal menu Collegamento. La stessa distinzione vale nella pagina completa e nel cablaggio in serie; le serie saltano i retro occupati. Associazioni mancanti, sconosciute o multiple non vengono dedotte né sovrascritte: si completano nei dettagli della porta.

La pagina completa si apre con Torna all’oggetto, il titolo `SW-01 › P5` e lo stato del collegamento; ha tre schede:

- **Collegamento**: sezioni Destinazione (un solo elenco con ricerca e il filtro Solo questo piano, mezzo, Collega), Percorso (lo stesso schema) e, per pannelli, prese e scatole, Passaggio nel pannello (porta passante o sconosciuto); Crea oggetto intermedio è un pulsante con l'elenco dei tipi;
- **VLAN e PoE**;
- **Hardware**: etichetta, connettore, velocità, modulo e PoE supportato.

Aprendo un oggetto collegato o nuovo dal configuratore compare un riquadro "Stai modificando X dentro Y" con Applica e torna e Torna senza applicare: il salvataggio dell'host salva l'oggetto di partenza. Una sola sessione resta annullabile. La selezione multipla delle porte apre un riquadro con sottosezioni VLAN e PoE.

I selettori ordinano le entità per nome e mostrano la ricerca oltre sette opzioni; porte e posizioni mantengono l'ordine tecnico. I campi per caratteristiche personalizzate accettano testo libero e suggerimenti. Nessuna selezione, elenco privo di elementi e ricerca senza risultati hanno messaggi distinti.

La chiusura di una sezione conserva i valori nella bozza del chiamante. Gli errori bloccanti aprono la sezione interessata e la segnalano con ! nell'intestazione; gli errori dei campi e il riepilogo nella barra di salvataggio dell'host spiegano cosa correggere (l'editor dei modelli, senza quel riepilogo, li elenca in fondo). Ridurre gruppi collegati richiede ancora un consenso esplicito alla rimozione; gli ID delle porte conservate rimangono invariati. Uscita, cambio di destinazione e annullamento mantengono le protezioni della sessione e dell'Undo.

Il database conserva le tabelle esistenti. Il JSON hardware aggiunge i campi facoltativi `portLayouts` e `portPoeOverrides`, descritti nel [contratto](02-domain-data-contract.md); la versione `.ofam` resta 1. La sessione del configuratore ora fonde anche VLAN e subnet create durante la modifica. Verifiche e limiti: [05-testing-and-benchmarks.md](05-testing-and-benchmarks.md).

## Fonti

- [IEEE 802.3-2022, PoE af/at/bt](https://standards.ieee.org/ieee/802.3/10422/) e [IEEE 802.1Q-2022, VLAN](https://standards.ieee.org/ieee/802.1Q/10323/)

- [Semantica Compose](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Grafica Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [Dialoghi Compose](https://developer.android.com/develop/ui/compose/components/dialog)
- [Accessibilità predefinita Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Forme Material 3](https://m3.material.io/styles/shape)

AUD-42 (8 ottobre): le azioni principali, Chiudi e Dettagli porta sono un unico gruppo verticale nel footer di PortQuickDialog. Non si affiancano più un blocco su più righe e un secondo blocco di altezza diversa negli slot di AlertDialog: senza Dettagli, Chiudi poteva sovrapporsi a Scollega. Salva e Indietro restano fissi durante la configurazione. Regressione nativa rossa prima della correzione, poi matrice 360/412 dp, temi chiaro/scuro e testo 1,0/1,3 verde, con e senza Dettagli. Perimetro e limiti in [roadmap.md](../roadmap.md).

UX-04 Android (8 ottobre): ricerca di un modello tra 12 e riduzione di un gruppo collegato verificate nelle otto combinazioni su host isolato. Il filtro non cambia la bozza; la scelta applica il modello. La riduzione richiede conferma, conserva gli ID delle porte rimaste e del cavo e lascia aperto l'estremo della porta rimossa. Nessuna modifica al runtime per questa verifica; tastiera aperta, TalkBack e editor completo restano in RES-19.
