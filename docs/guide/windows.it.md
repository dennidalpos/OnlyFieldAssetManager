# Uso su Windows

L'app Windows 11 x64 è portable. Tenere insieme l'eseguibile e la directory `data/` che contiene progetti, media e preferenze. Non rigenerare il runtime sopra dati locali: seguire la [procedura di rilascio](../06-release-and-delivery.md).

## Lavorare su un progetto

1. Avviare l'EXE e aprire un progetto locale o importare un `.ofam` ricevuto da Android.
2. Prima di sostituire una copia, leggere il confronto; per la fusione scegliere i conflitti rispetto alla base dell'ultimo scambio. Una copia protetta può chiedere la password locale.
3. Usare Mappa, Inventario, Cablaggio e Progetto. I moduli tecnici sono raggiungibili dalle destinazioni della sezione; quelli opzionali vuoti sono raccolti in Altri moduli.
4. Dopo una modifica, attendere l'esito del salvataggio. Il pannello occupato blocca gli input; un errore lascia la bozza disponibile per la riprova.
5. Esportare PDF, XLSX o Markdown dalla sezione Documenti; esportare `.ofam` per lo scambio manuale.

Il recupero di un salvataggio interrotto può richiedere la password locale di un progetto protetto. Conservare i backup finché il recupero non è concluso. La stampa fisica e alcune varianti visuali restano nella [matrice Windows](../testing/windows-native-matrix.md).

Dettagli: [storage](../04-desktop-storage-interop.md), [flussi](../07-workflows.md) e [documenti](../03-export-and-documents.md).
