# BU, piani e mappa interattiva

Data: 3 ottobre 2026

## Apertura e creazione

All'avvio selezionare un progetto, una BU e un piano. Gli elenchi consentono di aggiungere altre BU e piani senza ripetere «Nuovo sito». Il percorso corrente permette di risalire agli elenchi; su Android funziona anche Indietro. Le aree esistenti diventano voci dell'elenco Piani: possono rappresentare locali o zone, anche sotto una sede. Il nome della sede distingue i piani omonimi; gli ID rimangono invariati.

«Nuovo sito» comprende nome progetto/cliente facoltativo, elenco modificabile delle BU, piani per ciascuna BU e password facoltativa. Occorrono almeno una BU e un piano; altre BU possono restare vuote. Dopo la creazione selezionare il piano da aprire. La password è disponibile anche negli strumenti del progetto.

## Oggetti e foto

«Aggiungi» apre un catalogo ricercabile di apparati, rack e cavi. Non richiede marche o modelli: le famiglie generiche seguono i cataloghi ufficiali [Cisco](https://www.cisco.com/site/us/en/products/index.html) e [HPE](https://www.hpe.com/us/en/products/compute.html). «Tipologia personalizzata» registra nel progetto un nome e una categoria di base. I campi extra esistenti restano utilizzabili nella scheda.

La scheda precompila BU e piano e comprende informazioni, foto esistenti e nuove foto. Su Android si può scegliere dal telefono o scattare. I file vengono copiati nell'archivio del progetto al salvataggio; annullando il form, gli allegati esistenti restano invariati e gli scatti temporanei vengono rimossi. Un errore di lettura o salvataggio mantiene aperta la scheda. Modificare un oggetto conserva porte e campi non mostrati.

«QR» mantiene la lettura offline di QR e barcode, comprese le etichette OFAM. Android usa la fotocamera; Windows accetta il codice del lettore USB come input e «Apri». Un codice sconosciuto precompila il seriale di un nuovo apparato.

## Mappa e cavi

Ogni piano ha sempre una mappa. Senza planimetria viene mostrata una griglia; gli oggetti precedenti senza coordinate ricevono una disposizione iniziale deterministica. Un oggetto appena salvato compare in una posizione libera. Tocco/clic apre la scheda; trascinamento sposta l'oggetto e salva al rilascio. Sono disponibili zoom, panoramica e «Adatta alla vista».

I cavi sono linee selezionabili. Selezionare una linea per vedere le estremità e aprire «Scheda cavo e foto»; trascinare i punti liberi del percorso per modificarlo. Le estremità collegate agli apparati sul piano seguono la posizione dell'apparato; per spostarle si sposta l'apparato o si cambia il collegamento nella scheda. Il collegamento può indicare una porta, il solo apparato o un'estremità sconosciuta. Le destinazioni fuori piano mostrano BU e contesto del piano/sede. Lo stesso cavo può avere percorsi distinti sui piani attraversati.

Le coordinate sono relative alla superficie effettiva della pagina, conservando le proporzioni. Aggiungere o rimuovere uno sfondo conserva oggetti e posizioni. Inventario, rete, credenziali, documenti e altri strumenti sono accessibili dal menu Strumenti.

## Planimetrie offline

«Planimetria» permette di scegliere un'immagine o un PDF dal dispositivo oppure un allegato presente. Android usa il [selettore di sistema](https://developer.android.com/training/data-storage/shared/documents-files). Il file viene copiato nell'archivio del progetto, quindi resta disponibile anche se l'originale non è più accessibile.

Un'immagine diventa subito lo sfondo. Per un PDF sono mostrate anteprime numerate: scegliere la pagina del piano e confermare. Il conteggio viene letto dal file e la pagina è memorizzata per ciascun piano; un unico allegato PDF può servire più piani.

Rendering e anteprime usano [PdfRenderer Android](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer) e [PDFBox 3.0.8 Windows](https://pdfbox.apache.org/3.0/getting-started.html), fuori dal thread UI. File illeggibili, pagine mancanti e PDF protetti producono un messaggio esplicito; lo schema resta disponibile. Per PDF protetti occorre una copia senza password.

## Compatibilità e aggiornamento

Il contratto [`.ofam` 1.9](02-domain-data-contract.md) include tipologie, geometrie, pagine e foto, anche cifrate. Legge 1.7 e 1.8 e rifiuta versioni non supportate. Aggiornare entrambe le app prima di scambiare questi dati.

Android passa da Room 11 a 12 con sole aggiunte di colonne, nella transazione gestita da Room. Prima dell'aggiornamento il database SQLCipher viene aperto con la chiave dell'installazione, il WAL viene consolidato e viene creata una copia cifrata in `no_backup/onlyfield_asset_manager.db.v11.backup`. Se il backup fallisce, l'aggiornamento non parte. La copia è locale e non viene esportata: conserva la chiave protetta dal Keystore della stessa installazione.

Per un ripristino tecnico, conservare anche i dati e la chiave dell'installazione, chiudere l'app e ripristinare il backup con la versione compatibile; reinstallare l'app può perdere la chiave del Keystore. Per trasferimenti tra telefoni usare l'export `.ofam`.

Evidenze automatiche e controlli manuali ancora aperti: [test](05-testing-and-benchmarks.md), [roadmap](../roadmap.md) e [tracker](../PROJECT_STATUS.json).
