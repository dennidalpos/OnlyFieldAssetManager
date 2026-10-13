# Documenti ed export

`shared:exchange` genera XLSX, Markdown ed etichette QR. Android genera PDF tramite `PdfExportManager` e stampa con il framework Android; Windows usa `PdfReportWriter`/PDFBox e la stampa nativa. Entrambe le app escludono le credenziali dai documenti.

## Selezione e contenuto

`DocumentSelection` applica sede, piano, categoria e classificazione a inventario, rete, alimentazione, allegati e disegni. Un contenuto riservato richiede inclusione esplicita. Gli estremi esterni indispensabili a spiegare un percorso possono comparire come contesto, senza esportare altri record non scelti.

- **XLSX:** inventario, porte/cablaggio, rete, alimentazioni, note e percorsi completi. Il testo utente resta testo e non diventa formula.
- **Markdown:** riepilogo testuale con escaping di caratteri Markdown/HTML; i dati originali non cambiano.
- **PDF Android:** inventario, rack, porte/cablaggio, rete, alimentazione/badge, note e catalogo allegati, paginati. Planimetrie, percorsi e topologia disegnati sono opzioni del PDF Windows.
- **PDF Windows:** sezioni selezionabili con planimetrie, rack fronte/retro, percorsi e topologia. Uno sfondo richiesto ma illeggibile causa errore visibile.

Il rilievo assente è presentato come “Da verificare”; note e avvisi documentali seguono i filtri. La lingua viene acquisita all'avvio della generazione; nomi e note dell'utente restano invariati.

## Verifica

I test di export controllano filtri, caratteri speciali, paginazione e contenuti. Dialoghi di stampa, spooler e stampante fisica restano collaudi [Android](testing/hardware-checklist.md) e [Windows](testing/windows-native-matrix.md). Vedi [guida allo scambio](guide/exchange-and-documents.md).

## Fonti

[Android PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument), [PDFBox 3](https://pdfbox.apache.org/3.0/getting-started.html), [OpenXML ST_Xstring](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-oi29500/d34ae755-c53f-4a44-a363-c6dd3ee018a4).
