# Flussi utente

## Progetto e modifiche

Il wizard crea progetto, sedi e almeno un piano; la password e facoltativa. Le app navigano progetto, sede, piano e mappa; le sedi sono elencate per gruppo. Le modifiche passano da una bozza: uscita, cambio elemento, cambio lingua o chiusura chiedono conferma se la bozza e sporca.

Android usa editor a pagina intera; Desktop usa elenco e pannello laterale con `Ctrl+S` ed `Esc`; nelle finestre strette il pannello occupa lo spazio disponibile mantenendo la bozza. Entrambe le app offrono annullamento a un passo e selettori di entita, senza scegliere implicitamente il primo elemento disponibile.

## Operazioni

Inventario, rack, cablaggio, rete, alimentazione, media e documenti sono strumenti del progetto. Scanner e lettore USB aprono il codice trovato; un codice sconosciuto non modifica dati. Eliminazione e cambio di elemento richiedono conferma. L'eliminazione non sta mai nel menu ⋮: è un pulsante cestino rosso visibile su schede, dettagli e pannello mappa.

Il censimento dei collegamenti parte dal disegno dell'apparato: il tocco su una porta apre la scheda rapida per collegarla a un altro apparato, inserire un passante (presa, patch panel, scatola di giunzione) o scollegarla, con l'etichetta del cavo proposta. La foto è sempre a un tocco per apparati, rack, porte e cavi: fotocamera su Android, scelta di una o più immagini su Windows (`LocalPhotoAction`), con salvataggio immediato come allegato. Su Android gli scatti sono in serie: dopo una foto confermata la fotocamera si riapre sullo stesso oggetto e annullarla chiude la serie (con più scatti compare «N foto salvate»).

Gli stessi flussi condividono form e regole. La mappa è un unico componente condiviso: il tocco su un contenitore lo apre, il tocco su un oggetto o un collegamento mostra i dettagli in un pannello non modale, e l'inserimento procede per tipologia, preset e dati essenziali. Dettagli in [08-floor-map.md](08-floor-map.md); configurazione tecnica e preset in [10-object-configurator.md](10-object-configurator.md).

## Navigazione e azioni

Le destinazioni hanno gli stessi gruppi e nomi sulle due piattaforme:

| Gruppo | Destinazioni, in ordine |
| --- | --- |
| Lavoro | Mappa, Dispositivi, Rack, Cablaggio |
| Dati tecnici | Rete, Alimentazione |
| Supporto | Modelli, Allegati, Documenti, Credenziali |
| Progetto | Struttura e impostazioni, Cestino |

**Android.** Una barra in basso porta alle destinazioni di Lavoro (Mappa, Dispositivi, Rack, Cablaggio) e ad Altro. La scelta di una voce svuota lo stack sopra la mappa.

Altro contiene:

- il controllo del progetto, solo se ci sono avvisi;
- gli altri gruppi, con icone;
- Azioni progetto: Esporta, Importa, password e Chiudi progetto.

La barra si nasconde mentre è aperto un editor a pagina intera o lo scanner, così un cambio di sezione non perde la bozza. Le sottoschede sono fisse fino a tre; oltre diventano un menu Vista.

**Windows.** La barra laterale misura 208 dp e mostra le icone delle destinazioni. Si richiude nel menu Sezioni se lascerebbe meno di 360 dp all'elenco, tenendo conto del pannello aperto. Operazioni progetto elenca Esporta, Importa, password, poi Nuovo sito e Chiudi progetto.

Le scorciatoie Windows mantengono le associazioni: `Ctrl+1` Dispositivi, `Ctrl+2` Rack, `Ctrl+3` Modelli, `Ctrl+4` Mappa, `Ctrl+5` Credenziali, `Ctrl+6` Allegati, `Ctrl+7` Cablaggio, `Ctrl+8` Rete, `Ctrl+9` Alimentazione.

Gli elenchi nominali e i selettori usano il nome nella lingua corrente per ordinare copie di presentazione. L'ordine salvato resta invariato. VLAN, porte e unità rack conservano l'ordinamento tecnico; il cestino mostra prima le eliminazioni recenti. Inventario filtra anche per stato operativo (su Android il filtro compare quando esistono apparati non in servizio), distingue assenza di dati da ricerca senza risultati e mostra nome, collocazione e un riepilogo breve.

## Editor e configuratori

Ogni editor ha un titolo operativo, ad esempio Aggiungi dispositivo, Modifica dispositivo · nome o Configura porte · nome. Intestazione e azioni restano separate dal corpo scorrevole. Il salvataggio usa Aggiungi, Salva modifiche o Applica secondo l'operazione; Annulla modifiche chiude la bozza, mentre l'annullamento dell'ultima operazione resta un comando distinto.

Le spaziature sono 8 dp fra elementi collegati, 16 dp fra campi e 24 dp fra sezioni; i margini sono 16 dp su Android e 24 dp negli editor desktop. Filtri e azioni vanno a capo quando necessario. I configuratori della mappa usano gli stessi contenitori degli altri editor.

Gli editor e i pannelli si aprono sempre dall'inizio: un nuovo oggetto, una nuova selezione o un altro editor non ereditano lo scorrimento precedente. Solo il ritorno da una porta riporta alla sezione Porte. Gli errori compaiono vicino al campo e nel riepilogo presso il salvataggio. Le sezioni con errori bloccanti si aprono automaticamente. Le righe checkbox sono interamente cliccabili, con un unico controllo semantico e altezza minima di 48 dp. Su Android il footer considera tastiera e barra di navigazione, consumando gli inset già applicati dal contenitore.

Riferimenti: [barra di navigazione Compose](https://developer.android.com/develop/ui/compose/components/navigation-bar), [forme Material 3](https://m3.material.io/styles/shape), [dialoghi Compose](https://developer.android.com/develop/ui/compose/components/dialog), [accessibilità predefinita](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [layout a flusso](https://developer.android.com/develop/ui/compose/layouts/flow), [gestione degli inset](https://developer.android.com/develop/ui/compose/system/insets-ui). Consultati il 4 ottobre 2026; gli esiti di verifica sono in [05-testing-and-benchmarks.md](05-testing-and-benchmarks.md).
