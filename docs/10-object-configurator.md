# Configuratore

`shared/configurator` fornisce alle due app le viste Compose. Il core mantiene geometria, hardware, riconciliazione porte e tracciamento; la UI salva le operazioni annidate come una sola modifica annullabile.

Rack, apparati e cavi possono usare modelli di progetto. Un modello conserva la configurazione riutilizzabile, non posizione, identificativi operativi, collegamenti o credenziali. Applicarlo a un'istanza e sempre esplicito e conserva le porte riconosciute quando possibile.

`ConnectionGraph` segue cavi e passaggi interni. Una porta puo essere disponibile, avere un percorso completo, incompleto o un conflitto. Il colore accompagna sempre un testo; il verde indica solo continuita censita, non traffico reale.

Il contratto 1.11 conserva hardware, modelli e porte; Room 14 li persiste. Le versioni precedenti non ricevono lato o passaggi inventati.

## Dati essenziali e dettagli

In testa una riga di contesto mostra simbolo, tipo e posizione (`Terra › R1 › U10–11`); il nome è già nel titolo della finestra. **Dati essenziali** contiene nome, tipo, modello facoltativo ed etichetta fisica (per i rack l'altezza, per i cavi il mezzo). Il tipo imposta la categoria; la categoria compare in Hardware solo per tipi personalizzati, legacy e modelli.

Le sezioni seguono sempre questo ordine e, chiuse, mostrano un riepilogo:

1. **Posizione**: contenitore, business unit, piano, altezza U e, in rack, posizione U e lato. Se il piano cambia per un oggetto esistente compare un avviso. Dall'inventario la business unit deve essere selezionata (la sezione si apre per l'errore); piano, rack e porte non sono obbligatori.
2. **Porte** (`24 porte · 4 occupate`).
3. **Identificativi e rete**: IP, MAC, numero di serie, alias.
4. **Hardware**.
5. **Note e rilievo**.
6. Sezioni dell'host passate con `extraSections`, ad esempio **Foto e allegati** dalla mappa.
7. **Campi personalizzati**: una scheda per campo.
8. **Opzioni avanzate**: Salva come modello.

Il contesto esplicito di piano o contenitore viene conservato. Rack, cavi e modelli usano la stessa gerarchia con campi pertinenti. **Dispositivi nel rack** mostra il lato scelto con filtri Fronte/Retro, solo i dispositivi montati, le U libere come intervalli (`U libere: 1–9, 12–42`) e Aggiungi in una U libera. Tutti i campi a scelta usano `SelectField`: bordo ed etichetta come i campi di testo, letto come un solo pulsante "etichetta: valore".

## Preset e porte

`core.forms.DevicePresets` contiene i preset predefiniti. Sono solo codice: non entrano nei pacchetti e non cambiano il contratto. Ogni preset è un insieme di menu con valori:

| Famiglia | Valori |
| --- | --- |
| Switch | 5/8/16/24/48 RJ45; uplink 0/2/4 SFP o SFP+; PoE nessuno, tutte, metà |
| Patch panel | 12/24/48 RJ45, LC o SC, fronte/retro accoppiati |
| Prese | 1/2/4 porte accoppiate |
| Router, firewall | 4/8 LAN, 1/2 WAN, 0/1/2 SFP o SFP+ |
| AP, telecamere, telefoni, sensori | 1/2 RJ45 con PoE 802.3af/at/bt |
| Server, workstation | 1/2/4 NIC RJ45 o SFP+, porta di gestione |
| NAS, SAN | 1/2/4 LAN |
| NVR | 0/4/8/16 porte PoE e uplink |
| UPS, PDU, alimentatori | 6/8/12/24 prese C13 o Schuko |

Il preset si sceglie all'inserimento dalla mappa oppure nella sezione Porte; se esistono già gruppi di porte la sezione li riassume e i menu si aprono con Cambia preset. Applicarlo sostituisce i gruppi di porte; le porte collegate richiedono ancora il consenso esplicito. Il budget PoE proposto vale 15,4 W per ogni porta PoE (uscita PSE IEEE 802.3af) e resta modificabile.

I gruppi di porte seguono tre passi: **tipologia** (`PortKind`: RJ45, SFP, SFP+, SFP28, QSFP28, LC, SC, console, C13, Schuko), **quantità**, **etichetta**. Le etichette possono essere brevi (`P1`, `X1`), di interfaccia (`Gi1/0/1`, `Te1/1/1`) o un prefisso libero, con anteprima dell'intervallo. Il prefisso libero si scrive e poi si conferma con Applica prefisso: solo allora la numerazione viene ricalcolata. La tipologia imposta connettore, mezzo, velocità e ruolo. Un nuovo gruppo continua la numerazione dei gruppi con lo stesso prefisso. I prefissi restano non vuoti perché i lettori del contratto 1.11 li richiedono. Lato, velocità, ruolo, accoppiamento e combo sono nei dettagli avanzati.

Il pannello porte (`PortPanel`) imita il frontale: porte dispari sopra e pari sotto, un blocco per connettore e lato. Pieno indica occupata, bordo libera; ⚡ indica PoE (pieno se erogato, tenue se solo supportato), il numero in basso la VLAN, ! una porta da verificare. Il tocco apre la porta. La pressione prolungata avvia la selezione multipla, con le azioni Seleziona tutte e Solo libere, e permette di applicare VLAN (access o trunk) e PoE a tutte le porte selezionate. All'inizio della selezione l'anteprima viene fissata nella sessione, così le porte non ancora salvate mantengono il proprio identificativo; `PortLogic` ignora comunque porte inesistenti e non crea righe orfane. La logica è in `core.forms.PortLogic` e usa le entità esistenti `PortVlanMembership`, `PoeMapping` e `Vlan`. Le VLAN mancanti vengono create a livello di progetto; la subnet si ricava da `Subnet.vlanId`.

La porta si apre con Torna all'oggetto, il titolo `SW-01 › P5` e lo stato del collegamento; ha tre schede:

- **Collegamento**: sezioni Destinazione (un solo elenco con ricerca e il filtro Solo questo piano, mezzo, Collega), Percorso e, per pannelli e prese, Passaggio nel pannello; Crea oggetto intermedio è un pulsante con l'elenco dei tipi;
- **VLAN e PoE**;
- **Hardware**: etichetta, connettore, velocità, modulo e PoE supportato.

Aprendo un oggetto collegato o nuovo dal configuratore compare un riquadro "Stai modificando X dentro Y" con Applica e torna e Torna senza applicare: il salvataggio dell'host salva l'oggetto di partenza. Una sola sessione resta annullabile. La selezione multipla delle porte apre un riquadro con sottosezioni VLAN e PoE.

I selettori ordinano le entità per nome e mostrano la ricerca oltre sette opzioni; porte e posizioni mantengono l'ordine tecnico. I campi per caratteristiche personalizzate accettano testo libero e suggerimenti. Nessuna selezione, elenco privo di elementi e ricerca senza risultati hanno messaggi distinti.

La chiusura di una sezione conserva i valori nella bozza del chiamante. Gli errori bloccanti aprono la sezione interessata e la segnalano con ! nell'intestazione; gli errori dei campi e il riepilogo nella barra di salvataggio dell'host spiegano cosa correggere (l'editor dei modelli, senza quel riepilogo, li elenca in fondo). Ridurre gruppi collegati richiede ancora un consenso esplicito alla rimozione; gli ID delle porte conservate rimangono invariati. Uscita, cambio di destinazione e annullamento mantengono le protezioni della sessione e dell'Undo.

Preset, pannello porte e modifiche multiple non cambiano database, formato `.ofam` o API di scambio. La sessione del configuratore ora fonde anche VLAN e subnet create durante la modifica. Verifiche e limiti: [05-testing-and-benchmarks.md](05-testing-and-benchmarks.md).

## Fonti

- [IEEE 802.3-2022, PoE af/at/bt](https://standards.ieee.org/ieee/802.3/10422/) e [IEEE 802.1Q-2022, VLAN](https://standards.ieee.org/ieee/802.1Q/10323/)

- [Semantica Compose](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Grafica Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [Dialoghi Compose](https://developer.android.com/develop/ui/compose/components/dialog)
- [Accessibilità predefinita Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
