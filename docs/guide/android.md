# Uso su Android

Android 14+ conserva i progetti in Room/SQLCipher. Prima di installare una versione con schema Room diverso, esportare in `.ofam` i progetti da conservare: il fallback configurato ricrea il database quando manca una migrazione.

## Censire un progetto

1. Aprire o creare un progetto dalla sezione Progetto; aggiungere una sede e, se serve, un piano.
2. In Inventario aggiungere apparati e rack. Per un apparato indicare nome e sede; piano e collocazione possono essere completati in seguito.
3. Aprire Mappa: al livello progetto si parte dallo schema sedi, alla sede dalla topologia e al piano dalla planimetria. La barra delle viste espone anche Logica, Apparati ed Elenco dove applicabili.
4. Selezionare un apparato o una porta per aprire configuratore e scheda rapida. Registrare cavi, permutazioni e foto sul destinatario corretto.
5. In Progetto usare Documenti per PDF, XLSX o Markdown; usare lo scambio `.ofam` per trasferire una copia a Windows.

I dati sono locali. Il download della cartografia è un'azione esplicita che usa la rete; la planimetria e i dati già salvati restano consultabili offline. Le foto, la scansione e i gesti su hardware reale hanno ancora i collaudi indicati nella [checklist](../testing/hardware-checklist.md).

## Errori e recupero

- Un errore strutturale blocca salvataggio o import; un avviso documentale permette di proseguire dopo la revisione.
- In caso di salvataggio fallito, correggere l'errore e riprovare dalla bozza ancora aperta. Non interpretare la chiusura di un dialogo come conferma del commit.
- Per sostituzione o fusione, confrontare prima la copia ricevuta e scegliere i conflitti. La fusione a tre vie non risolve da sola modifiche concorrenti.

Dettagli: [flussi](../07-workflows.md), [contratto dei dati](../02-domain-data-contract.md) e [scambio](exchange-and-documents.md).
