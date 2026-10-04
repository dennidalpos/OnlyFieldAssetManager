# Mappa e planimetrie

La navigazione segue progetto, business unit, piano e mappa. Gli oggetti hanno coordinate normalizzate sul piano; zoom e panoramica non le modificano.

## Scena e contenitori

`core.model.MapScene` calcola ciò che una vista mostra, una sola volta per piano o contenitore, con indici precalcolati (`HierarchyIndex`, `CableEnds`).

- **Piano**: mostra solo le radici (rack e oggetti non contenuti). Ogni contenitore appare come un unico oggetto.
- **Contenitore**: il tocco su un contenitore con figli apre la sua vista, fino all'ultimo livello. I figli sono disposti automaticamente; nei rack l'ordine segue le unità, dall'alto. Il percorso di navigazione (`Piano › RACK-A › BOX`) e Indietro riportano ai livelli superiori; i livelli eliminati, spostati in un altro contenitore o su un altro piano vengono chiusi. La vista interna non modifica posizioni o percorsi salvati.
- Sono contenitori di serie rack, mensola, armadio e cassetta. Per i tipi personalizzati la proprietà si imposta nel catalogo; i tipi predefiniti non vengono più duplicati.

Ogni nodo ha un simbolo univoco (SW, AP, CAM, NVR, UPS…) non tradotto, un colore per famiglia (Rete, Sicurezza, Server, Alimentazione, Passivo, Strutture, Dispositivi finali) e una forma: cerchio per gli oggetti, quadrato con numero di figli per i contenitori. L'anello indica le porte occupate dell'intero sottoalbero. Le etichette sono abbreviate e nascoste se si sovrappongono; quella dell'oggetto selezionato resta visibile.

## Collegamenti semplificati

- Una sola linea per coppia di nodi visibili, con il numero di cavi se più di uno. I cavi interni a un contenitore non sono disegnati: compaiono aprendolo.
- Le estremità fuori vista (altro piano, fuori dal contenitore, sconosciute) diventano un tratto verso il bordo con un cerchio vuoto.
- Il mezzo è distinto anche senza colore: rame continuo, fibra tratteggiata, alimentazione a puntini, altro tratto-punto.
- I collegamenti senza percorso salvato sono archi: le linee da uno stesso nodo si annidano invece di passare sopra altri oggetti.
- Il tocco su un collegamento apre l'elenco completo dei cavi (porta A → porta B, piano se diverso, mezzo). Il cavo scelto si apre nel configuratore.

I percorsi salvati restano `CableRoute`. Il percorso predefinito è rettilineo; i percorsi salvati con il vecchio punto centrale `(.5, .5)` non vengono più fatti convergere al centro (`CableRoute.bends`). Sul piano si trascinano i punti del collegamento selezionato o il punto medio, che aggiunge una curva (al massimo 12 punti; oltre, il punto medio non fa nulla).

Trascinando un oggetto, il punto in cui lo si è afferrato resta sotto il dito. La pressione prolungata senza movimento non sposta nulla e non salva. Un secondo dito annulla lo spostamento in corso e passa a zoom e panoramica, senza salti quando si solleva un dito.

## Pannello e inserimento

`configurator.map.MapWorkspace` è condiviso dalle due app. Il pannello dei dettagli non è modale: in basso sotto 840 dp di larghezza, laterale da 840 dp, secondo le classi di finestra Android. Mostra:

- senza selezione: l'elenco degli oggetti della vista, selezionabili anche con lettore di schermo e tastiera, e la legenda richiudibile. Sotto 840 dp il pannello si apre con Elenco;
- per un oggetto: dati principali, pannello porte compatto (un unico elemento che apre le porte) e le azioni Modifica, Porte, Apri e Rimuovi dal contenitore;
- per il contenitore aperto: Aggiungi qui, Assegna esistente e l'elenco dei figli;
- per un collegamento: l'elenco completo dei cavi.

L'inserimento usa un'unica finestra: tipologia raggruppata per famiglia, poi valori del preset, poi i dati essenziali nel configuratore. Il nome proposto è automatico, ad esempio `SW-03`. La pressione prolungata su un punto vuoto del piano posiziona lì il nuovo oggetto.

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
