# Mappa e planimetrie

La navigazione segue progetto, business unit, piano e mappa. Gli oggetti hanno coordinate normalizzate sul piano; zoom e panoramica non le modificano. Rack e apparati abilitati possono contenere oggetti. La mappa mostra le radici; l'apertura di un contenitore conserva contesto e percorso di navigazione.

I cavi collegano apparati o porte. Sulla mappa le estremita dei figli sono proiettate sulla radice visibile; collegamenti tra la stessa coppia sono aggregati senza perdere il singolo cavo. Estremita sconosciute e destinazioni fuori piano restano esplicite.

Un piano puo usare un'immagine o una pagina PDF scelta da un allegato. Android usa `PdfRenderer`, Desktop PDFBox; il rendering avviene fuori dal thread UI. Un file illeggibile lascia disponibile la mappa senza sfondo.

Le mappe cartografiche vengono scaricate solo su richiesta e salvate come allegati con attribuzione. L'uso successivo, l'export e lo scambio restano offline.

## Fonti

- [Android PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
- [Apache PDFBox 3](https://pdfbox.apache.org/3.0/getting-started.html)
