# Scambio e documenti

## Scambiare un progetto

1. Esportare un pacchetto `.ofam` v1 dall'app sorgente. La password del pacchetto è facoltativa. Dopo l'import, la protezione della copia locale dipende dal flusso scelto: la sostituzione Android segue il pacchetto ricevuto, la fusione mantiene la protezione locale.
2. Copiare manualmente il file sull'altro dispositivo e importarlo dall'app. Verificare gli errori strutturali e gli avvisi documentali prima di confermare.
3. Se il progetto esiste già, confrontare la copia ricevuta. Scegliere sostituzione o fusione; nella fusione decidere i conflitti uno per uno.
4. Dopo la conferma, riaprire il progetto e controllare gli allegati necessari. Un allegato mancante resta un avviso, non una prova che il relativo file sia presente.

Lo scambio non è una sincronizzazione automatica. Le credenziali non entrano in PDF, XLSX o Markdown; la loro gestione nel progetto segue il contratto `.ofam` e le scelte di importazione.

## Generare documenti

- Impostare sede, piano, categoria e classificazione prima dell'export: `DocumentSelection` applica i filtri ai formati comuni.
- PDF: consultazione e stampa; Android e Windows hanno renderer diversi. Verificare il risultato finale quando contiene planimetrie o allegati.
- XLSX: inventario e percorsi fisici; il testo libero viene trattato come testo.
- Markdown: riepilogo leggibile, con escaping del testo dell'utente.

I limiti del pacchetto e i dettagli della protezione sono nel [contratto](../02-domain-data-contract.md); le sezioni dei documenti sono in [documenti](../03-export-and-documents.md).
