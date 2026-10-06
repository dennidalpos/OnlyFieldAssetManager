# Documenti ed export

## Output

`shared:exchange` genera XLSX OpenXML, Markdown ed etichette QR. Android aggiunge PDF composti e stampa; Desktop genera PDF e usa la stampa nativa.

Gli export descrivono inventario (con sede, gruppo e stato operativo), porte e cablaggio, rete, alimentazione, media e campi documentali. Le credenziali sono escluse. Il testo libero viene esportato come testo, evitando formule XLSX interpretate.

## Filtri comuni

`DocumentSelection` applica sede, piano effettivo (anche ereditato da rack/contenitori), categoria e classificazione a inventario, rack, rete, alimentazione, allegati e disegni. Rete e allegati con ambito progetto restano comuni; quelli associati a sedi o oggetti esclusi non compaiono. Le planimetrie riservate non sono disegnate senza inclusione esplicita. Nei percorsi XLSX/PDF e nella topologia sono presenti anche estremi e passanti esterni necessari a spiegare un percorso che tocca un apparato selezionato; non altre reti. I nodi esterni della topologia PDF sono grigi.

Il PDF Android usa la stessa selezione e pagina tutte le sei sezioni offerte: inventario, schede rack, porte e cablaggio, rete logica, alimentazione/badge, note e catalogo allegati. Le note e gli allegati sono indipendenti dall'inventario; nessun limite al numero di apparati o allegati. Nomi lunghi e note passano su più righe e pagine. Gli allegati sono elencati con nome, tipo, file e attribuzione; i loro file restano nel pacchetto `.ofam`.

Le schede rack Android includono fronte/retro scalati alla pagina, verso di numerazione e lista completa, anche per apparati senza U. L'export del singolo rack include inoltre gli apparati fuori rack dello stesso piano. Planimetrie, percorsi e topologia nel PDF sono opzioni Desktop, non offerte dal dialogo Android. PDF e stampa Android usano lo stesso generatore; i dialoghi e l'annullamento di stampa restano in RES-19.

## Excel

Fogli: Inventario apparati, Porte e cablaggio, Rete logica e VLAN, Alimentazione e badge, Note e osservazioni, **Percorsi**. Ogni foglio ha la prima riga in grassetto e bloccata e una larghezza di colonna leggibile. Il foglio Percorsi riporta una riga per percorso (`PathSchematics.all`) che tocca gli apparati esportati: apparato, porta e ubicazione dei due estremi (con fine aperta), passanti attraversati con le porte, etichette dei cavi, mezzi, lunghezza totale quando tutti i cavi ne hanno una, stato.

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

## Generazione e stampa in background

Android e Windows generano documenti fuori dal thread UI. Android mantiene le callback di stampa sul Main e distingue cancellazione ed errore; Windows conserva l’esito sincrono mantenendo attivo l’event loop. Un errore di stampa Windows è visibile, distinto dall’annullamento. La prova dei dialoghi nativi resta nei residui RES-19 e RES-23.

## Rilievi, note e avvisi — AUD-17

Observation.effectiveStatus considera il rilievo assente Da verificare; gli stati espliciti Verificato, Da verificare, In conflitto e Non rilevato restano distinti dallo stato operativo. Form, validazione, Markdown, XLSX e PDF Android/Windows condividono la regola. Anche le schede rack riportano il rilievo quando l'inventario è disattivato. Il riepilogo Markdown conta assenza, Da verificare e In conflitto fra le criticità.

Le note del rilievo di apparati, porte e cavi sono conservate; XLSX le presenta nel foglio Note e osservazioni, PDF nella sezione note quando selezionata. Markdown e XLSX includono gli avvisi documentali; nei PDF gli avvisi seguono le sezioni scelte. DocumentSelection valida i riferimenti del progetto originale e filtra gli avvisi per oggetti, sezioni e classificazioni esportati: escludere un estremo dal filtro non crea un falso avviso di cavo scollegato. I percorsi Desktop conservano gli avvisi dei segmenti esterni necessari al contesto. Credenziali e avvisi delle credenziali restano esclusi; un allegato da rivedere escluso con reviewRequiredConfirmed=false non compare nemmeno negli avvisi.

Regressioni it/en/es su rilievo assente, stati espliciti, note, filtri e sezioni disattivate. Il PDF Android reale è stato generato e il testo estratto su Pixel 9 API 37; questo non verifica la stampa fisica o la matrice UX. Fonti primarie consultate il 6 ottobre 2026: [PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument), [PdfRenderer.Page e getTextContents](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page). Evidenze e comandi in [verifica](05-testing-and-benchmarks.md).
