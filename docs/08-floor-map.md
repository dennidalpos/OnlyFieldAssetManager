# Mappa e planimetrie

La navigazione segue progetto, business unit, piano e mappa. Gli oggetti hanno coordinate normalizzate sul piano; zoom e panoramica non le modificano. La panoramica è limitata: la pagina più piccola della vista resta tutta visibile, quella ingrandita si ferma ai bordi (`MapViewport.clamped`).

## Scena e contenitori

`core.model.MapScene` calcola ciò che una vista mostra, una sola volta per piano o contenitore, con indici precalcolati (`HierarchyIndex`, `CableEnds`).

- **Piano**: mostra solo le radici (rack e oggetti non contenuti). Ogni contenitore appare come un unico oggetto.
- **Contenitore**: il tocco su un contenitore con figli apre la sua vista, fino all'ultimo livello. I figli sono disposti automaticamente; nei rack l'ordine segue le unità, dall'alto. Il percorso di navigazione (`Piano › RACK-A › BOX`) e Indietro riportano ai livelli superiori; i livelli eliminati, spostati in un altro contenitore o su un altro piano vengono chiusi. La vista interna non modifica posizioni o percorsi salvati.
- Sono contenitori di serie rack, mensola, armadio e cassetta. Per i tipi personalizzati la proprietà si imposta nel catalogo; i tipi predefiniti non vengono più duplicati.

Ogni nodo ha un simbolo univoco (SW, AP, CAM, NVR, UPS…) non tradotto, un colore per famiglia (Rete, Sicurezza, Server, Alimentazione, Passivo, Strutture, Dispositivi finali) e una forma: cerchio per gli oggetti, quadrato con numero di figli per i contenitori. L'anello indica le porte occupate dell'intero sottoalbero. Le etichette sono abbreviate e nascoste se si sovrappongono; quella dell'oggetto selezionato resta visibile.

## Collegamenti semplificati

- Una sola linea per coppia di nodi visibili, con il numero di cavi se più di uno. I cavi interni a un contenitore non sono disegnati: compaiono aprendolo.
- Le estremità fuori vista (altro piano, fuori dal contenitore, sconosciute) diventano un tratto verso il bordo con un cerchio vuoto, uno per oggetto visibile. Un'etichetta accanto al cerchio indica dove prosegue: `→ SW-05 · Nord 1 · BU Nord` se c'è un solo dispositivo remoto (piano e BU solo se diversi da quelli in vista), altrimenti `→ 3 dispositivi remoti`. `core.model.RemoteEnds` risolve dispositivo, porta, piano e BU dell'estremità; `SceneLink.remotes` li conserva per cavo.
- La scheda cavo ha come titolo l'etichetta del cavo o quella proposta (`CableLabels.suggest`) e mostra anche Estremità remota (porta, piano e BU sempre). Il cavo scelto ha Foto, Modifica cavo, Inserisci passaggio e Vai a, che apre il piano dell'altra estremità, anche di un'altra BU, con l'oggetto selezionato e i suoi contenitori aperti (`MapActions.goTo` e parametro `focus` di `MapWorkspace`). Scegliere un piano dall'elenco annulla la selezione in arrivo.
- Il mezzo è distinto anche senza colore: rame continuo, fibra tratteggiata, alimentazione a puntini, altro tratto-punto.
- I collegamenti senza percorso salvato sono archi: le linee da uno stesso nodo si annidano invece di passare sopra altri oggetti.
- Il tocco su un collegamento apre l'elenco completo dei cavi (porta A → porta B, piano se diverso, mezzo). Il cavo scelto si apre nel configuratore.

I percorsi salvati restano `CableRoute`. Il percorso predefinito è rettilineo; i percorsi salvati con il vecchio punto centrale `(.5, .5)` non vengono più fatti convergere al centro (`CableRoute.bends`). Sul piano si trascinano i punti del collegamento selezionato o il punto medio, che aggiunge una curva (al massimo 12 punti; oltre, il punto medio non fa nulla).

Trascinando un oggetto, il punto in cui lo si è afferrato resta sotto il dito. La pressione prolungata senza movimento non sposta nulla e non salva. Un secondo dito annulla lo spostamento in corso e passa a zoom e panoramica, senza salti quando si solleva un dito.

## Pannello e inserimento

`configurator.map.MapWorkspace` è condiviso dalle due app; il pannello è in `MapDetailPane`. Il pannello dei dettagli non è modale: in basso sotto 840 dp di larghezza, laterale da 840 dp, secondo le classi di finestra Android.

L'area del piano è un foglio pieno con bordo, su sfondo distinto, e griglia 24 × 24 con una linea più marcata ogni 4 celle; resta visibile anche in tema scuro. Con una planimetria il bordo circonda l'immagine.

Nessun comando scorre in orizzontale:

- **Intestazione**: ‹ Indietro e il livello corrente. I livelli superiori sono in un menu a tendina.
- **Telefono**: Aggiungi oggetto (Aggiungi qui dentro un contenitore) a tutta larghezza con Elenco. Scansiona codice e Planimetria sono nel menu ⋮.
- **Finestre da 600 dp**: tutti i comandi in una riga.

Le miniature degli allegati vanno a capo.

Per un oggetto l'ordine è sempre lo stesso:

1. **Intestazione**: simbolo, nome, tipo e chiusura (annunciata come Chiudi).
2. **Azioni**: la primaria (Apri per i contenitori, altrimenti Modifica) e **Foto**, sempre presente e in evidenza (`MapActions.photo`); Porte e Rimuovi dal contenitore come secondarie (annullabile). In fondo **Sposta nel cestino**, rosso con icona e con conferma, per dispositivi e rack (`MapActions.trash`); il cavo scelto di un collegamento ha Elimina.
3. **Dati primari**: unità del rack, contenuto, posizione nel rack ed etichetta fisica.
4. **Porte**: il disegno dell'apparato con le porte occupate su totali; il tocco su una porta apre la scheda rapida (`PortQuickDialog`, vedi [10-object-configurator.md](10-object-configurator.md)).
5. **Collegamenti**, con il numero di elementi.
6. **Foto e allegati**: sempre visibile, con Aggiungi foto e, se vuota, l'invito a documentare oggetto e collegamenti.
7. **Altri dettagli**, chiuso: profondità, cavi interni, carico PoE, alias, IP, MAC, numero di serie e collegamenti logici.

Le sezioni senza dati non compaiono, tranne Foto e allegati.

Il contenitore aperto mostra la propria intestazione (Contenitore aperto) e la sezione Contenuto con Assegna esistente: una finestra con ricerca che indica tipo e posizione attuale di ogni oggetto. Senza selezione il pannello elenca gli oggetti della vista raggruppati per famiglia, con filtro oltre dieci elementi, e la legenda chiusa; sotto 840 dp si apre con Elenco. Per un collegamento ogni cavo è una scheda con estremità A e B e mezzo; il cavo scelto mostra Modifica cavo.

Per un dispositivo la sezione **Collegamenti logici**, in Altri dettagli, elenca le connessioni WAN, VPN, Internet e Altro (`WanVpnConnection`) di cui è estremità locale o remota: tipo, nome, operatore, banda e «verso» l'altra estremità (dispositivo e piano, oppure descrizione della sede). Il collegamento è agganciato al dispositivo, non alla porta. + Nuovo apre una finestra basata su `core.forms.WanForm` con tipo VPN e il dispositivo come estremità locale; il tocco su una riga modifica la connessione mantenendo il lato del dispositivo, note e accesso sottostante. Se l'altra estremità è un dispositivo del progetto, Vai a lo apre sul suo piano. L'eliminazione resta nella sezione Rete. `core.model.LogicalLinks` fornisce elenco ed etichette.

L'inserimento è rapido e usa un'unica finestra (`ObjectPickerDialog`, descritto in [10-object-configurator.md](10-object-configurator.md)). Il sottotitolo dice dove andrà l'oggetto (Sul piano Terra, In RACK-A). Aggiungi salva subito; Aggiungi e modifica apre l'editor completo. La pressione prolungata su un punto vuoto del piano posiziona lì il nuovo oggetto; senza punto scelto va nella posizione libera più vicina al centro, mai sul bordo. Nel passo porte i menu del preset occupano tutta la riga disponibile e l'elenco aperto ha la larghezza del campo.

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
