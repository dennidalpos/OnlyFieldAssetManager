# Flussi applicativi

## Creazione, navigazione e modifica

Il wizard crea progetto, sede e piano; la password locale è facoltativa. La navigazione principale condivisa ha quattro aree: Mappa, Inventario (Dispositivi e Rack), Cablaggio e Progetto. Rete, Alimentazione, Modelli, Allegati, Documenti, Credenziali e Cestino restano destinazioni raggiungibili nei gruppi pertinenti. Credenziali, Configurazioni e Badge vuoti sono raccolti in “Altri moduli”; i dati non vengono rimossi dal progetto.

Le modifiche partono da una bozza. Uscita o cambio contesto con dati modificati richiedono conferma; un salvataggio fallito lascia la bozza e l'errore per la riprova. Android usa editor a pagina intera; Windows affianca elenco e pannello quando c'è spazio. Le operazioni lunghe espongono lo stato occupato prima di accettare nuovi input.

## Import e fusione

- Il pacchetto ricevuto viene validato e confrontato con la copia locale dello stesso ID, anche se non è aperta. `STRUCTURAL_ERROR` blocca; gli avvisi documentali sono mostrati prima della conferma.
- La sostituzione richiede conferma. Annullare conserva la copia precedente. Su Android la sostituzione segue la protezione del pacchetto ricevuto; la fusione mantiene la protezione locale.
- La fusione a tre vie usa la base dell'ultimo scambio e chiede una scelta per i conflitti. Una base assente o illeggibile è segnalata e non viene riscritta dalla sola lettura.
- Il ripristino dal cestino richiede sede, piano, contenitori, porte e ID originali ancora disponibili. Un rifiuto conserva la voce e permette correzione/riprova.
- Per rimuovere sedi, apparati o VLAN referenziati da ambiti di rete, eliminare prima esplicitamente quei riferimenti: nessun retarget automatico.

## Media, mappe e documenti

Foto e allegati hanno un target esplicito (progetto, piano, rack, apparato, cavo o porta); i file oltre 32 MiB sono rifiutati prima del commit. Su Android una serie foto riparte solo dopo un salvataggio riuscito. I media del cestino restano locali e non entrano nel pacchetto scambiato.

La mappa mostra la scheda rapida di apparati, porte e collegamenti; le modifiche del layout delle porte restano in bozza finché non si salva. La cartografia viene scaricata solo su richiesta. I documenti applicano filtri di ambito e classificazione; vedi [mappa](08-floor-map.md), [configuratore](10-object-configurator.md) e [documenti](03-export-and-documents.md).

I dettagli di piattaforma sono nelle guide [Android](guide/android.md) e [Windows](guide/windows.md); i collaudi nativi ancora aperti sono nel [tracker](../PROJECT_STATUS.json).
