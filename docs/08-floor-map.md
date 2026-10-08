# Mappa e planimetrie

La navigazione segue progetto, sede, piano e mappa. Gli oggetti hanno coordinate normalizzate sul piano; zoom e panoramica non le modificano. Il canvas è ritagliato al proprio riquadro e lo stato della panoramica viene ricondotto ai limiti dopo il ridimensionamento. La panoramica è limitata: la pagina più piccola della vista resta tutta visibile, quella ingrandita si ferma ai bordi (`MapViewport.clamped`).

## Scena e contenitori

`core.model.MapScene` calcola ciò che una vista mostra, una sola volta per piano o contenitore, con indici precalcolati (`HierarchyIndex`, `CableEnds`).

- **Piano**: mostra solo le radici (rack e oggetti non contenuti). Ogni contenitore appare come un unico oggetto.
- **Contenitore**: il tocco su un contenitore con figli apre la sua vista, fino all'ultimo livello. I figli sono disposti automaticamente; nei rack l'ordine segue le unità, dall'alto. Il percorso di navigazione (`Piano › RACK-A › BOX`) e Indietro riportano ai livelli superiori; i livelli eliminati, spostati in un altro contenitore o su un altro piano vengono chiusi. La vista interna non modifica posizioni o percorsi salvati.
- Sono contenitori di serie rack, mensola, armadio e cassetta. Per i tipi personalizzati la proprietà si imposta nel catalogo; i tipi predefiniti non vengono più duplicati.

Gli apparati spenti o dismessi sono disegnati attenuati (`SceneNode.inactive`). Ogni nodo ha un simbolo univoco (SW, AP, CAM, NVR, UPS…) non tradotto, un colore per tipo di apparato (`MapStyle.glyph`, condiviso con topologia, rack e schede; famiglia come fallback per i tipi personalizzati) e una forma: cerchio per gli oggetti, quadrato con numero di figli per i contenitori. L'anello indica le porte occupate dell'intero sottoalbero. Le etichette sono abbreviate e nascoste se si sovrappongono; quella dell’oggetto selezionato resta completa e visibile. La selezione usa alone e doppio contorno contrastante ed è disegnata sopra gli altri nodi.

## Collegamenti semplificati

- Una sola linea per coppia di nodi visibili, con il numero di cavi se più di uno. I cavi interni a un contenitore non sono disegnati: compaiono aprendolo.
- Le estremità fuori vista (altro piano, fuori dal contenitore, sconosciute) diventano un tratto verso il bordo con un cerchio vuoto, uno per oggetto visibile. Un'etichetta accanto al cerchio indica dove prosegue: `→ SW-05 · Nord 1 · Sede Nord` se c'è un solo dispositivo remoto (piano e sede solo se diversi da quelli in vista), altrimenti `→ 3 dispositivi remoti`. `core.model.RemoteEnds` risolve dispositivo, porta, piano e sede dell'estremità; `SceneLink.remotes` li conserva per cavo.
- La scheda cavo ha come titolo l'etichetta del cavo o quella proposta (`CableLabels.suggest`) e mostra anche Estremità remota (porta, piano e sede sempre). Il cavo scelto ha Foto, Modifica cavo, Inserisci passaggio e Vai a, che apre il piano dell'altra estremità, anche di un'altra sede, con l'oggetto selezionato e i suoi contenitori aperti (`MapActions.goTo` e parametro `focus` di `MapWorkspace`). Scegliere un piano dall'elenco annulla la selezione in arrivo.
- Il mezzo è distinto anche senza colore: rame continuo, fibra tratteggiata, radio a tratti lunghi (viola), alimentazione a puntini, altro tratto-punto.
- I collegamenti senza percorso salvato sono archi: le linee da uno stesso nodo si annidano invece di passare sopra altri oggetti.
- Il tocco su un collegamento apre l'elenco completo dei cavi (porta A → porta B, piano se diverso, mezzo). Il cavo scelto si apre nel configuratore.

I percorsi salvati restano `CableRoute`. Il percorso predefinito è rettilineo; i percorsi salvati con il vecchio punto centrale `(.5, .5)` non vengono più fatti convergere al centro (`CableRoute.bends`). Sul piano si trascinano i punti del collegamento selezionato o il punto medio, che aggiunge una curva (al massimo 12 punti; oltre, il punto medio non fa nulla).

Trascinando un oggetto, il punto in cui lo si è afferrato resta sotto il dito. La pressione prolungata senza movimento non sposta nulla e non salva. Un secondo dito annulla lo spostamento in corso e passa a zoom e panoramica, senza salti quando si solleva un dito.

## Pannello e inserimento

`configurator.map.MapWorkspace` è condiviso dalle due app; il pannello è in `MapDetailPane`. Sotto 840 dp di workspace compare un riepilogo con nome, stato, Modifica, Foto ed Espandi; Espandi occupa l’area di lavoro e Riduci riporta alla mappa senza trascinamenti. Da 840 dp i dettagli sono laterali, larghi 360 dp. Il contenuto si sposta fra le due presentazioni conservando i dialoghi e le bozze aperte.

`MapUiState`, posseduto dagli host, conserva selezione, contenitori, zoom e centro normalizzato per piano durante cambio sezione, editor e ridimensionamento. Il cambio progetto azzera il contesto; piani rimossi, percorsi e selezioni non più validi vengono invalidati. Stato di sessione, senza scritture nel database o nel pacchetto.

L'area del piano è un foglio pieno con bordo, su sfondo distinto, e griglia 24 × 24 con una linea più marcata ogni 4 celle; resta visibile anche in tema scuro. Con una planimetria il bordo circonda l'immagine.

Nessun comando scorre in orizzontale:

- **Intestazione**: Indietro e un unico percorso sede/piano/contenitore. I livelli superiori sono in un menu a tendina.
- **Telefono**: Aggiungi oggetto (Aggiungi qui dentro un contenitore) a tutta larghezza con Elenco. Ricerca diretta; Scansiona codice, Topologia e Planimetria sono nel menu degli strumenti.
- **Finestre da 600 dp**: tutti i comandi in una riga.

Le miniature degli allegati vanno a capo.

Per un oggetto l'ordine è sempre lo stesso:

1. **Intestazione**: simbolo, nome, tipo e chiusura (annunciata come Chiudi).
2. **Azioni**: la primaria (Apri per i contenitori, altrimenti Modifica) e **Foto**, sempre presente e in evidenza (`MapActions.photo`); Porte e Rimuovi dal contenitore in Altre azioni (annullabile). **Sposta nel cestino** è nel menu secondario, rosso e con conferma, per dispositivi e rack (`MapActions.trash`); il cavo scelto ha Elimina nello stesso menu.
3. **Dati primari**: unità del rack, contenuto, posizione nel rack, etichetta fisica e stato operativo se diverso da In servizio.
4. **Porte**: il disegno dell'apparato con le porte occupate su totali; il tocco su una porta apre la scheda rapida (`PortQuickDialog`, vedi [10-object-configurator.md](10-object-configurator.md)). Le porte collegate senza foto della porta né del cavo hanno il segno • (`PortCell.photoMissing`, `core.forms.PhotoCoverage`); sotto il disegno il conteggio e **Apri la prima** portano alla scheda rapida della prima porta da fotografare.
5. **Collegamenti**, con il numero di elementi.
6. **Foto e allegati**: sempre visibile, con Aggiungi foto e, se vuota, l'invito a documentare oggetto e collegamenti.
7. **Altri dettagli**, chiuso: cavi interni, carico PoE, alias, IP, MAC, numero di serie e collegamenti logici.

Le sezioni senza dati non compaiono, tranne Foto e allegati.

Il contenitore aperto mostra la propria intestazione (Contenitore aperto) e la sezione Contenuto con Assegna esistente: una finestra con ricerca che indica tipo e posizione attuale di ogni oggetto. Senza selezione il pannello elenca gli oggetti della vista raggruppati per famiglia, con filtro oltre dieci elementi, e la legenda chiusa; sotto 840 dp si apre con Elenco. Per un collegamento ogni cavo è una scheda con estremità A e B e mezzo; il cavo scelto mostra Modifica cavo.

Per un dispositivo la sezione **Collegamenti logici**, in Altri dettagli, elenca le connessioni WAN, VPN, Internet e Altro (`WanVpnConnection`) di cui è estremità locale o remota: tipo, nome, operatore, banda e «verso» l'altra estremità (dispositivo e piano, oppure descrizione della sede). Il collegamento è agganciato al dispositivo, non alla porta. + Nuovo apre una finestra basata su `core.forms.WanForm` con tipo VPN e il dispositivo come estremità locale; il tocco su una riga modifica la connessione mantenendo il lato del dispositivo, note e accesso sottostante. Se l'altra estremità è un dispositivo del progetto, Vai a lo apre sul suo piano. L'eliminazione resta nella sezione Rete. `core.model.LogicalLinks` fornisce elenco ed etichette.

L'inserimento è rapido e usa un'unica finestra (`ObjectPickerDialog`, descritto in [10-object-configurator.md](10-object-configurator.md)). Il sottotitolo dice dove andrà l'oggetto (Sul piano Terra, In RACK-A). Aggiungi salva subito; Aggiungi e modifica apre l'editor completo. La pressione prolungata su un punto vuoto del piano posiziona lì il nuovo oggetto; senza punto scelto va nella posizione libera più vicina al centro, mai sul bordo. Nel passo porte i menu del preset occupano tutta la riga disponibile e l'elenco aperto ha la larghezza del campo.

## Ricerca nel progetto

**Cerca** (icona nella barra della mappa su Android; pulsante e `Ctrl+F` su Windows) apre `GlobalSearchDialog`, basato su `core.display.GlobalSearch`. La ricerca copre nome, etichetta, alias, IP, MAC e seriale degli apparati, nomi dei rack, porte ed etichette dei cavi (anche quelle proposte). Una porta si cerca insieme all'apparato (`SW-01 P5` o `SW-01/P5`), così un nome breve come `P1` non riempie l'elenco. I risultati sono ordinati per corrispondenza esatta, iniziale e parziale; ognuno indica tipo e posizione (`Sede › Piano › Rack`).

Il risultato scelto apre la mappa sul suo piano, anche di un'altra sede, con i contenitori aperti e l'oggetto selezionato (lo stesso arrivo di Vai a). Porte e cavi selezionano l'apparato a cui appartengono. Un oggetto senza piano si apre nell'editor. Con il campo vuoto la finestra elenca gli ultimi otto elementi aperti nella sessione.

## Topologia fisica

**Topologia** (pulsante nell'intestazione su Windows; sopra l'elenco di sedi e piani e tra gli strumenti della mappa su Android) apre a schermo intero `configurator.map.TopologyDialog`, filtrato su sede e piano correnti.

- `core.forms.PhysicalTopology` usa solo gli apparati attivi: un collegamento unisce due apparati raggiunti da un percorso completo (`PathSchematics`), con i passanti nascosti; il numero di percorsi rende la linea più spessa, il ponte radio è tratteggiato. I percorsi che non arrivano a un altro apparato attivo sono contati sull'apparato come percorsi aperti (`⋯N`).
- Ogni rete collegata è una fascia di righe, a livelli in ampiezza dall'apparato più a monte (ONT o modem, router o firewall, poi lo switch con più collegamenti); gli apparati senza collegamenti chiudono il disegno. Le righe si adattano alla larghezza della finestra, quindi si scorre solo in verticale.
- I terminali con un solo collegamento verso un apparato di infrastruttura sono raccolti su di esso (`+N`); **Mostra i terminali** li disegna tutti.
- Filtri Sede e Piano: gli apparati fuori filtro all'altro capo di un collegamento restano visibili e attenuati. Apparati spenti o dismessi sono attenuati.
- Ogni nodo usa colore per tipo, icona e nome, coerenti con la mappa; stato e appartenenza al filtro restano distinti.
- Il tocco su un apparato chiude la topologia e lo seleziona sulla mappa del suo piano; senza piano apre l'editor.

## Planimetrie

Un piano può usare un'immagine o una pagina PDF scelta da un allegato. Android usa `PdfRenderer`, Desktop PDFBox; il rendering avviene fuori dal thread UI. Un file illeggibile lascia disponibile la mappa senza sfondo. La vista interna dei contenitori non usa lo sfondo.

Le mappe cartografiche vengono scaricate solo su richiesta e salvate come allegati con attribuzione. L'uso successivo, l'export e lo scambio restano offline.

## Fonti

- [Classi di dimensione della finestra](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)
- [Bottom sheet Material 3](https://m3.material.io/components/bottom-sheets/guidelines)
- [Grafica Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [Gesti multitouch Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch)
- [Android PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
- [Apache PDFBox 3](https://pdfbox.apache.org/3.0/getting-started.html)

## Ripristino delle collocazioni — AUD-27

Un piano mancante o trasferito a un’altra sede, un contenitore non disponibile, un figlio ricollocato o un ID di collocazione riutilizzato bloccano il ripristino. Le coordinate originali tornano soltanto quando il contesto è disponibile; nessun posizionamento viene scartato silenziosamente. La voce e i media restano nel cestino sul rifiuto.

## Riprova nei picker Windows — AUD-33

Nel workspace mappa, un errore di save mantiene aperto il picker oggetto con nome, preset, posizione o contenitore e mostra il messaggio nel dialogo. Anche la scelta della pagina PDF, l’immagine e la rimozione dello sfondo restano ripetibili dalla stessa schermata dopo aver risolto il guasto. Il picker si chiude dopo il salvataggio riuscito; un allegato appena importato resta disponibile sul rifiuto dell’assegnazione. Catalogo, copia locale e history precedenti sono conservati e la riprova registra una sola modifica annullabile. Prove e limiti nella roadmap; altri picker rapidi Windows corretti in AUD-34 e percorso asincrono mappa Android in AUD-35; editor/host restanti in AUD-36/37.

AUD-34 completato estende questa regola agli host Inventario/Rack/unità e alla pagina PDF da Allegati; callback di salvataggio invariati, nessun cambio di formato. AUD-35 completa il percorso asincrono mappa Android; AUD-36 riguarda editor completi e allegati Windows.

## Conferma del save Android — AUD-35

I form sede/piano, MapObjectPicker e PlanChooser della mappa conservano bozza e selezione fino all’esito positivo del comando asincrono. Il guasto compare nella schermata ancora aperta; la riprova conserva tipo/nome/posizione/pagina. Un’immagine importata resta selezionabile dopo un errore di assegnazione. Sei prove native su moto g86 API 36 e regressioni sessione/cancellazione verdi; la matrice visuale completa resta RES-19. Gli altri host Android sono AUD-37 e gli editor completi Windows AUD-36.


AUD-39 (7 ottobre): il form di download conserva coordinate, zoom e nome dopo errore della sorgente o salvataggio fallito; la stessa conferma consente di riprovare. Download e import allegati chiudono dopo il successo e non pubblicano esiti verso una bozza/sessione dismessa. Prova nativa con PNG e sorgente sintetici, repository reale; server pubblico non collaudato. Evidenze nella roadmap e in RES-19.
