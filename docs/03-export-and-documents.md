# Documenti ed export

## Output

`shared:exchange` genera XLSX OpenXML, Markdown ed etichette QR. Android aggiunge PDF composti e stampa; Desktop genera PDF e usa la stampa nativa.

Gli export descrivono inventario (con sede, gruppo e stato operativo), porte e cablaggio, rete, alimentazione, media e campi documentali. Le credenziali sono escluse. Il testo libero viene esportato come testo, evitando formule XLSX interpretate.

## Lingua e media

La lingua viene acquisita all'avvio della generazione. Etichette e intestazioni seguono la lingua selezionata; testo utente, codici e attribuzioni restano invariati.

Allegati, immagini e PDF restano nel progetto e sono inclusi nel pacchetto quando presenti. La cartografia e un'immagine allegata: il download e esplicito e l'attribuzione resta con il file.

Vedi [localizzazione](09-localization.md) e [mappa](08-floor-map.md).
