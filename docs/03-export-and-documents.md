# Documenti ed export

## Output

`shared:exchange` genera XLSX OpenXML, Markdown ed etichette QR. Android aggiunge PDF composti e stampa; Desktop genera PDF e usa la stampa nativa.

Gli export descrivono inventario (con sede, gruppo e stato operativo), porte e cablaggio, rete, alimentazione, media e campi documentali. Le credenziali sono escluse. Il testo libero viene esportato come testo, evitando formule XLSX interpretate.

## PDF di consegna Desktop

`pc.report.PdfReportWriter` (PDFBox) scrive il PDF A4 e la stessa pagina viene stampata (`PDFPageable`). Usa Arial dal sistema (DejaVu Sans su Linux) con i caratteri Unicode e ripiega su Helvetica, sostituendo i caratteri che il carattere non ha. `ReportContent` produce righe di testo, tabelle (`ReportLine.Row`, intestazione ripetuta a ogni pagina) e disegni (`ReportLine.Figure`). Sezioni scelte nel dialogo Documenti (`ReportSelection`):

- **Inventario**, testo come prima.
- **Planimetrie**: per ogni piano con oggetti o planimetria, lo sfondo (immagine o pagina PDF letta dall'allegato con `PlanMedia.bufferedImage`, compressa in JPEG) con oggetti e cavi come in `MapScene.area`: colori per famiglia, rame grigio, fibra arancione, radio tratteggiato blu, alimentazione rossa; apparati spenti o dismessi in grigio.
- **Schede rack**: elevazione fronte e retro affiancate con le U numerate secondo il verso del rack; gli apparati su entrambi i lati occupano le due colonne; quelli senza posizione U sono elencati.
- **Percorsi**: tabella Da, Passaggi, A, Cavi, Stato da `PathSchematics.all` (ogni percorso una volta, anche le fini aperte).
- **Topologia fisica**: `PhysicalTopology` con i terminali raccolti, ridotta per stare in una pagina.
- Cablaggio, rete logica, alimentazione e badge, note e allegati come testo.

## Lingua e media

La lingua viene acquisita all'avvio della generazione. Etichette e intestazioni seguono la lingua selezionata; testo utente, codici e attribuzioni restano invariati.

Allegati, immagini e PDF restano nel progetto e sono inclusi nel pacchetto quando presenti. La cartografia e un'immagine allegata: il download e esplicito e l'attribuzione resta con il file.

Vedi [localizzazione](09-localization.md) e [mappa](08-floor-map.md).
