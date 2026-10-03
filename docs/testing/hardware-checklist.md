# Checklist hardware — RES-13

Data di preparazione: 3 ottobre 2026. Stato: **rinviato, nessuno scenario eseguito in questa consegna**.

Un telefono collegato, le prove su emulatore e le gesture sintetiche non costituiscono evidenza del collaudo manuale. Usare un progetto di prova senza credenziali reali; annotare risultati ed evidenze prima di chiudere RES-13.

## Identificazione della sessione

| Campo | Valore da compilare |
| --- | --- |
| Data e operatore | Da compilare |
| Telefono/tablet: marca, modello | Da compilare |
| Android e API, build del sistema | Da compilare |
| PC: modello, Windows/build, display e scala | Da compilare |
| App: versione e riferimento della build/APK/ZIP | Da compilare |
| Lingua selezionata | Da compilare |
| Lettore USB: marca/modello, modalità HID, adattatore | Da compilare |
| Progetto e piano di prova | Da compilare |

## Scenari e risultati

Per ogni riga registrare dispositivo/versione effettivamente usati, PASS/FAIL, nota, data e percorso dell'evidenza. Ripetere le righe quando cambia dispositivo o configurazione.

| ID | Scenario manuale e preparazione | Risultato atteso | Dispositivo/versione | Esito | Evidenza e note |
| --- | --- | --- | --- | --- | --- |
| FOTO-01 | Fotocamera reale: concedere il permesso, scattare e confermare una foto di apparato | Anteprima e associazione corrette; riapertura offline conserva la foto | Da compilare | NON ESEGUITO | Da compilare |
| FOTO-02 | Annullare lo scatto e rifiutare il permesso; riprovare concedendolo | Nessun allegato vuoto o modifica alla bozza; messaggio comprensibile e nuovo tentativo possibile | Da compilare | NON ESEGUITO | Da compilare |
| FOTO-03 | Foto di rack/cavo, rotazione dello schermo, export/import cifrato verso Windows | Destinazione corretta, orientamento leggibile e byte/metadati conservati | Da compilare | NON ESEGUITO | Da compilare |
| QR-01 | Scansionare con fotocamera reale un'etichetta QR OFAM di apparato/rack/cavo, offline | Apre l'entità giusta e conserva la protezione delle bozze | Da compilare | NON ESEGUITO | Da compilare |
| QR-02 | Scansionare barcode di seriale, codice sconosciuto e codice con corrispondenze multiple | Ricerca corretta; sconosciuto e scelta multipla espliciti; niente modifica automatica | Da compilare | NON ESEGUITO | Da compilare |
| QR-03 | Illuminazione ridotta, codice inclinato, permesso negato e uscita dallo scanner | Nessun blocco; recupero del permesso e navigazione funzionanti | Da compilare | NON ESEGUITO | Da compilare |
| TOUCH-01 | Due dita: pinch zoom e panoramica su piano con immagine/PDF, bordi e contenitori annidati | Posizioni coerenti e sfondo proporzionato; zoom/pan non spostano oggetti | Da compilare | NON ESEGUITO | Da compilare |
| TOUCH-02 | Alternare pinch, selezione e trascinamento di un oggetto; riaprire il progetto | Salvataggio al rilascio; posizione corretta, nessun salto o trascinamento involontario | Da compilare | NON ESEGUITO | Da compilare |
| USB-01 | Lettore fisico su Windows: acquisire QR e barcode nel campo di ricerca con terminatore configurato | Codice completo, una sola ricerca, entità corretta; testo/terminatore non corrompono altri campi | Da compilare | NON ESEGUITO | Da compilare |
| USB-02 | Scollegare/ricollegare il lettore; codice sconosciuto e scansioni consecutive | Nessun blocco; messaggi corretti e focus prevedibile | Da compilare | NON ESEGUITO | Da compilare |

Per eventuali scenari non applicabili indicare il motivo e concordare il perimetro residuo; non trasformare “NON ESEGUITO” in PASS. Un fallimento richiede un residuo con riproduzione, risultato atteso e build coinvolta.

## Chiusura

La checklist deve contenere dispositivo, versione, scenario ed esito per foto, QR/barcode, multitouch e lettore USB reali. Riportare i risultati in [QA](../05-testing-and-benchmarks.md) e [roadmap](../../roadmap.md); rimuovere RES-13 dal [tracker](../../PROJECT_STATUS.json) solo quando il criterio è soddisfatto.
