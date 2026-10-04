# Flussi utente

## Progetto e modifiche

Il wizard crea progetto, business unit e almeno un piano; la password e facoltativa. Le app navigano progetto, business unit, piano e mappa. Le modifiche passano da una bozza: uscita, cambio elemento, cambio lingua o chiusura chiedono conferma se la bozza e sporca.

Android usa editor a pagina intera; Desktop usa elenco e pannello laterale con `Ctrl+S` ed `Esc`; nelle finestre strette il pannello occupa lo spazio disponibile mantenendo la bozza. Entrambe le app offrono annullamento a un passo e selettori di entita, senza scegliere implicitamente il primo elemento disponibile.

## Operazioni

Inventario, rack, cablaggio, rete, alimentazione, media e documenti sono strumenti del progetto. Scanner e lettore USB aprono il codice trovato; un codice sconosciuto non modifica dati. Eliminazione e cambio di elemento richiedono conferma.

Gli stessi flussi condividono form e regole. La mappa è un unico componente condiviso: il tocco su un contenitore lo apre, il tocco su un oggetto o un collegamento mostra i dettagli in un pannello non modale, e l'inserimento procede per tipologia, preset e dati essenziali. Dettagli in [08-floor-map.md](08-floor-map.md); configurazione tecnica e preset in [10-object-configurator.md](10-object-configurator.md).

## Navigazione e azioni

Le destinazioni hanno gli stessi gruppi e nomi sulle due piattaforme:

| Gruppo | Destinazioni, in ordine |
| --- | --- |
| Lavoro | Mappa, Dispositivi, Rack, Cablaggio |
| Dati tecnici | Rete, Alimentazione |
| Supporto | Modelli, Allegati, Credenziali, Documenti |
| Progetto | Struttura e impostazioni, Cestino |

La barra laterale Windows misura 208 dp. Si richiude nel menu Sezioni se lascerebbe meno di 360 dp all'elenco, tenendo conto del pannello aperto. Importazione, esportazione, password e chiusura sono raccolte in Operazioni progetto. Android mantiene queste operazioni fuori dall'elenco delle destinazioni.

Le scorciatoie Windows mantengono le associazioni: `Ctrl+1` Dispositivi, `Ctrl+2` Rack, `Ctrl+3` Modelli, `Ctrl+4` Mappa, `Ctrl+5` Credenziali, `Ctrl+6` Allegati, `Ctrl+7` Cablaggio, `Ctrl+8` Rete, `Ctrl+9` Alimentazione.

Gli elenchi nominali e i selettori usano il nome nella lingua corrente per ordinare copie di presentazione. L'ordine salvato resta invariato. VLAN, porte e unità rack conservano l'ordinamento tecnico; il cestino mostra prima le eliminazioni recenti. Inventario distingue assenza di dati da ricerca senza risultati e mostra nome, collocazione e un riepilogo breve.

## Editor e configuratori

Ogni editor ha un titolo operativo, ad esempio Aggiungi dispositivo, Modifica dispositivo · nome o Configura porte · nome. Intestazione e azioni restano separate dal corpo scorrevole. Il salvataggio usa Aggiungi, Salva modifiche o Applica secondo l'operazione; Annulla modifiche chiude la bozza, mentre l'annullamento dell'ultima operazione resta un comando distinto.

Le spaziature sono 8 dp fra elementi collegati, 16 dp fra campi e 24 dp fra sezioni; i margini sono 16 dp su Android e 24 dp negli editor desktop. Filtri e azioni vanno a capo quando necessario. I configuratori della mappa usano gli stessi contenitori degli altri editor.

Gli errori compaiono vicino al campo e nel riepilogo presso il salvataggio. Le sezioni con errori bloccanti si aprono automaticamente. Le righe checkbox sono interamente cliccabili, con un unico controllo semantico e altezza minima di 48 dp. Su Android il footer considera tastiera e barra di navigazione, consumando gli inset già applicati dal contenitore.

Riferimenti: [dialoghi Compose](https://developer.android.com/develop/ui/compose/components/dialog), [accessibilità predefinita](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [layout a flusso](https://developer.android.com/develop/ui/compose/layouts/flow), [gestione degli inset](https://developer.android.com/develop/ui/compose/system/insets-ui). Consultati il 4 ottobre 2026; gli esiti di verifica sono in [05-testing-and-benchmarks.md](05-testing-and-benchmarks.md).
