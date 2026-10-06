# Flussi utente

## Progetto e modifiche

Il wizard crea progetto, sedi e almeno un piano; la password e facoltativa. Le app navigano progetto, sede, piano e mappa; le sedi sono elencate per gruppo. Le modifiche passano da una bozza: uscita, cambio elemento, cambio lingua o chiusura chiedono conferma se la bozza e sporca.

Android usa editor a pagina intera; Desktop usa elenco e pannello laterale con `Ctrl+S` ed `Esc`; nelle finestre strette il pannello occupa lo spazio disponibile mantenendo la bozza. Entrambe le app offrono annullamento a un passo e selettori di entita, senza scegliere implicitamente il primo elemento disponibile.

## Operazioni

**Confronto prima dell’import.** Il pacchetto viene confrontato con la copia locale dello stesso ID, anche se è chiusa o è aperto un altro progetto. Una versione precedente mostra un avviso; la sostituzione richiede conferma e annullare conserva la copia locale. Fusione disponibile solo per lo stesso progetto. Su Windows, se la copia chiusa usa una password diversa, occorre sbloccarla prima del confronto; la fusione conserva la password locale.

**Import Android.** Creazione e sostituzione usano la password del pacchetto ricevuto per le successive riaperture. Se il pacchetto non è protetto, la copia sostituita diventa non protetta. La fusione mantiene la password e lo stato di protezione locali.

AUD-18: annullamento, nuova richiesta e chiusura del ViewModel invalidano l'import in corso, compresa la fusione. Nessuna conferma o Review tardiva riapre il progetto; una seconda conferma non avvia un altro salvataggio. Il pacchetto appartiene al worker, poi alla Review o al comando di conferma/fusione, e viene chiuso al termine di ciascun percorso. Annullare prima del commit lascia la copia precedente; un commit già iniziato termina nel blocco non cancellabile e resta durevole, senza pubblicazione UI tardiva.

**Allegati mancanti.** Entrambe le app mostrano gli avvisi del pacchetto prima della conferma, in un elenco scorrevole. Su Windows anche un nuovo progetto o un pacchetto identico con avvisi passa dalla conferma. Annullare lascia la copia locale invariata; confermare conserva i riferimenti documentali e importa solo i payload disponibili.

**Aggiunta media.** Picker, foto, planimetrie e mappe verificano il limite di 32 MiB per file prima di accettare il catalogo; le copie da provider interrompono anche stream senza dimensione dichiarata. Un errore di copia, capacità o persistenza lascia invariati progetto e media precedenti e rimuove i soli file appena creati. Su Android la fotocamera si riapre dopo il commit riuscito; foto rifiutata o annullata termina la serie. Per decisione del 6 ottobre (AUD-23), i progetti Android protetti mantengono il budget prudenziale: vicino a 256 MiB un'aggiunta può essere rifiutata anche se il ZIP effettivo entrerebbe nel limite. Non viene richiesta o conservata una password aggiuntiva per questa verifica; il rifiuto conserva progetto e media precedenti.

**Errori dei comandi — AUD-21.** Il cambio password Android restituisce anche gli errori del database al dialogo; errore o cancellazione non mostrano successo. Se il progetto viene chiuso o cambiato, nessuna risposta tardiva raggiunge il dialogo precedente. Apertura e cestino segnalano gli errori e consentono di riprovare senza perdere stato. Tema e lingua Windows cambiano dopo il salvataggio: preferenze illeggibili o scrittura fallita producono un errore visibile e conservano il file precedente.

**Ripristino dal cestino — AUD-26.** Per decisione del 6 ottobre, un apparato torna soltanto nel sito originale: se manca, resta nel cestino con i suoi media anche quando sono presenti altri siti. Ricreare un sito con un altro ID non lo rende quello originale. Rack associati a un piano richiedono sito/piano ancora disponibili; i rack senza piano restano ripristinabili. Credenziali ripristinate integralmente nelle due app. ID già attivo o metadati incoerenti producono un errore e conservano entrambe le versioni; nessuna sostituzione implicita. Tipi non supportati, compreso ATTACHMENT storico, restano nel cestino con i byte recuperabili. Una voce non più presente non produce successo. Riferimenti secondari persi dopo la cancellazione, come collocazioni di apparati su piani rimossi o ID di porte riutilizzati, restano in AUD-27.

**Rimozione media (AUD-19).** Un allegato rimosso non entra nello scambio; i suoi byte locali restano finché Annulla può recuperarlo. Foto di apparati/porte nel cestino restano locali, anche dopo sostituzione della copia, e vengono eliminate con l'oggetto soltanto alla rimozione definitiva. Lo svuotamento del cestino invalida l'undo precedente. L'eliminazione Android del progetto raccoglie solo i suoi file, senza toccare sorgenti utente o altri progetti. Senza siti, il ripristino di un apparato viene rifiutato conservando cestino e foto; scelta del sito quando quello originale manca e gestione dei tipi non supportati ancora in AUD-26.

Inventario, rack, cablaggio, rete, alimentazione, media e documenti sono strumenti del progetto. Scanner e lettore USB aprono il codice trovato; un codice sconosciuto non modifica dati. Eliminazione e cambio di elemento richiedono conferma. L'eliminazione non sta mai nel menu ⋮: è un pulsante cestino rosso visibile su schede, dettagli e pannello mappa.

Il censimento dei collegamenti parte dal disegno dell'apparato: il tocco su una porta apre la scheda rapida per collegarla a un altro apparato, inserire un passante (presa, patch panel, scatola di giunzione) o scollegarla, con l'etichetta del cavo proposta. La foto è sempre a un tocco per apparati, rack, porte e cavi: fotocamera su Android, scelta di una o più immagini su Windows (`LocalPhotoAction`), con salvataggio immediato come allegato. Su Android gli scatti sono in serie: dopo una foto confermata la fotocamera si riapre sullo stesso oggetto e annullarla chiude la serie (con più scatti compare «N foto salvate»).

Gli stessi flussi condividono form e regole. La mappa è un unico componente condiviso: il tocco su un contenitore lo apre, il tocco su un oggetto o un collegamento mostra i dettagli in un pannello non modale, e l’inserimento procede per tipologia, disegno con preset e nome proposto, conferma Aggiungi. Tutti i tipi seguono la conferma visiva. Le schede aprono con il disegno specifico del dispositivo; Modifica disposizione e Porte PoE lavorano in bozza con Salva, mentre Collega e Foto conservano la conferma esplicita. Dettagli in [08-floor-map.md](08-floor-map.md); configurazione tecnica e preset in [10-object-configurator.md](10-object-configurator.md).

**Media protetti Windows.** Anteprime e planimetrie restano disponibili dopo lo sblocco. Il pulsante Esporta e apri salva una copia in chiaro nella destinazione scelta e la apre nel programma associato. Chiudere il progetto rilascia i media in memoria; la copia locale rimane cifrata. Le vecchie copie protette con media in chiaro vengono adeguate al primo sblocco corretto.

## Navigazione e azioni

Le destinazioni hanno gli stessi gruppi e nomi sulle due piattaforme:

| Gruppo | Destinazioni, in ordine |
| --- | --- |
| Lavoro | Mappa, Dispositivi, Rack, Cablaggio |
| Dati tecnici | Rete, Alimentazione |
| Supporto | Modelli, Allegati, Documenti, Credenziali |
| Progetto | Struttura e impostazioni, Cestino |

**Moduli secondari** (`configurator.SecondaryModule`): Credenziali, Configurazioni apparati (scheda di Rete) e Badge documentali (scheda di Alimentazione) restano nel modello e negli export, ma compaiono solo se il progetto li usa. Altrimenti una sola voce **Altri moduli**, in fondo alla barra laterale su Windows e in Altro su Android, li elenca e li mostra per la sessione; **Nascondi i moduli non usati** li richiude. Su Windows `Ctrl+5` apre comunque le Credenziali. La videosorveglianza non ha una schermata propria.

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

## Sostituire una copia Windows con password diversa

Decifrare il pacchetto con la password ricevuta. Se la copia locale chiusa usa un’altra password, inserirla nel dialogo dedicato prima del confronto. Esaminare versione e avvisi, poi scegliere sostituzione o fusione. Annullare lascia la vecchia copia. Dopo la sostituzione usare la password del pacchetto ricevuto; la fusione mantiene quella locale. AUD-14 verifica il rollback del flusso completo media/progetto/base su errore; recupero dopo arresto improvviso ancora in AUD-24.

AUD-15: modifiche Android confermate dopo persistenza riuscita, comandi ravvicinati ordinati con cestino/password/media/import/export. Undo vale soltanto per la stessa sessione e revisione: dopo una modifica successiva o chiusura non sostituisce lo stato corrente. Una richiesta di chiusura/cambio progetto invalida solo la pubblicazione UI, senza annullare le modifiche già richieste. Durante il lavoro il dialogo occupato blocca gli input; verifica visiva nativa in RES-19. Gli errori mappa su snapshot obsoleto vengono mostrati e richiedono la riapertura dell'editor.

L'export Android accodato dopo un cambio password verifica la protezione persistita al momento del comando, prima di aprire il file di destinazione. Le etichette leggono il progetto richiesto, anche se la navigazione cambia durante l'attesa.

Sostituzione apparato (AUD-16): Android e Windows conservano collocazione, U, altezza e tipo di montaggio; il nuovo apparato ha identità nuova e non eredita dati, porte o collegamenti. I figli del vecchio contenitore risalgono al genitore; il cestino conserva i dati per ripristinare la gerarchia. La fusione usa le stesse scelte di campi e porte del core. Android persiste tutto con il cestino in una transazione: un errore lascia l'apparato precedente recuperabile e il progetto invariato. Le opzioni MergeDataChoices relative a credenziali/configurazioni/alimentazioni/campi extra non sono applicate dal core: residuo AUD-25.
