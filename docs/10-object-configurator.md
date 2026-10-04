# Configuratore

`shared/configurator` fornisce alle due app le viste Compose. Il core mantiene geometria, hardware, riconciliazione porte e tracciamento; la UI salva le operazioni annidate come una sola modifica annullabile.

Rack, apparati e cavi possono usare modelli di progetto. Un modello conserva la configurazione riutilizzabile, non posizione, identificativi operativi, collegamenti o credenziali. Applicarlo a un'istanza e sempre esplicito e conserva le porte riconosciute quando possibile.

`ConnectionGraph` segue cavi e passaggi interni. Una porta puo essere disponibile, avere un percorso completo, incompleto o un conflitto. Il colore accompagna sempre un testo; il verde indica solo continuita censita, non traffico reale.

Il contratto 1.11 conserva hardware, modelli e porte; Room 14 li persiste. Le versioni precedenti non ricevono lato o passaggi inventati.

## Dati essenziali e dettagli

All'apertura si compilano nome, tipo, modello facoltativo e collocazione. Il tipo imposta la categoria iniziale; la categoria resta modificabile in Hardware. Il contesto esplicito di piano o contenitore viene conservato. Dall'inventario la business unit deve essere selezionata; piano, rack e porte non sono obbligatori. In rack compaiono altezza, posizione U e lato.

Porte, Hardware, Identificativi e rete, Note e rilievo e Campi personalizzati sono sezioni richiudibili. Foto e allegati della mappa hanno una sezione dedicata. Salva come modello è un'azione secondaria. Rack, cavi e modelli usano la stessa gerarchia con campi pertinenti; le estremità del cavo e i contenuti del rack sono dettagli separati.

I gruppi di porte sono righe riepilogative, ad esempio `24 × RJ45 · 1G · Fronte`. Si espande un solo gruppo alla volta; quantità, lato, connettore e velocità sono immediatamente accessibili. Numerazione, funzione, PoE, accoppiamento e combo sono nei dettagli avanzati. Un nuovo gruppo si apre subito. Porte dall'inventario desktop apre e porta in vista la relativa sezione.

I selettori ordinano le entità per nome e mostrano la ricerca oltre sette opzioni; porte e posizioni mantengono l'ordine tecnico. I campi per caratteristiche personalizzate accettano testo libero e suggerimenti. Nessuna selezione, elenco privo di elementi e ricerca senza risultati hanno messaggi distinti.

La chiusura di una sezione conserva i valori nella bozza del chiamante. Gli errori bloccanti aprono la sezione interessata; gli errori dei campi e il riepilogo vicino al salvataggio spiegano cosa correggere. Ridurre gruppi collegati richiede ancora un consenso esplicito alla rimozione; gli ID delle porte conservate rimangono invariati. Uscita, cambio di destinazione e annullamento mantengono le protezioni della sessione e dell'Undo.

La revisione aggiunge stato e parametri di presentazione. Nessuna modifica a database, formato `.ofam`, regole del dominio o API di scambio. Verifiche e limiti: [05-testing-and-benchmarks.md](05-testing-and-benchmarks.md).

## Fonti

- [Semantica Compose](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Grafica Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [Dialoghi Compose](https://developer.android.com/develop/ui/compose/components/dialog)
- [Accessibilità predefinita Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
