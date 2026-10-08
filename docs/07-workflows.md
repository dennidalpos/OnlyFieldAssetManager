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

**Ripristino dal cestino — AUD-26.** Per decisione del 6 ottobre, un apparato torna soltanto nel sito originale: se manca, resta nel cestino con i suoi media anche quando sono presenti altri siti. Ricreare un sito con un altro ID non lo rende quello originale. Rack associati a un piano richiedono sito/piano ancora disponibili; i rack senza piano restano ripristinabili. Credenziali ripristinate integralmente nelle due app. ID già attivo o metadati incoerenti producono un errore e conservano entrambe le versioni; nessuna sostituzione implicita. Tipi non supportati, compreso ATTACHMENT storico, restano nel cestino con i byte recuperabili. Una voce non più presente non produce successo. AUD-27 (6 ottobre): il ripristino richiede i piani originali nella stessa sede, contenitori/figli e montaggi ancora disponibili. Un contenitore spostato su un altro piano o un figlio ricollocato bloccano il ripristino. ID di porte attive, porte duplicate nel JSON e ID di collocazioni già presenti sono rifiutati; nessuna collocazione saltata o sostituita. Progetto, credenziali, base di scambio, cestino e media restano invariati su rifiuto; si può riprovare dopo aver ripristinato il contesto. Le foto delle porte di un apparato nel cestino restano locali anche quando un apparato attivo riusa l’ID della porta e non vengono esportate. Controlli nel progetto corrente; collisioni tra progetti Android tracciate separatamente in AUD-28.

**Rimozione media (AUD-19).** Un allegato rimosso non entra nello scambio; i suoi byte locali restano finché Annulla può recuperarlo. Foto di apparati/porte nel cestino restano locali, anche dopo sostituzione della copia, e vengono eliminate con l'oggetto soltanto alla rimozione definitiva. Lo svuotamento del cestino invalida l'undo precedente. L'eliminazione Android del progetto raccoglie solo i suoi file, senza toccare sorgenti utente o altri progetti. Senza siti, il ripristino di un apparato viene rifiutato conservando cestino e foto; sito originale e tipi supportati verificati da AUD-26/27.

Inventario, rack, cablaggio, rete, alimentazione, media e documenti sono strumenti del progetto. Scanner e lettore USB aprono il codice trovato; un codice sconosciuto non modifica dati. Eliminazione e cambio di elemento richiedono conferma. L'eliminazione è nel menu delle azioni secondarie, evidenziata in rosso e sempre confermata.

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

**Moduli secondari** (`configurator.SecondaryModule`): Credenziali, Configurazioni apparati (scheda di Rete) e Badge documentali (scheda di Alimentazione) restano nel modello e negli export, ma compaiono solo se il progetto li usa. Altrimenti una sola voce **Altri moduli**, in fondo alla barra laterale su Windows e in Progetto su Android, li elenca e li mostra per la sessione; **Nascondi i moduli non usati** li richiude. Su Windows `Ctrl+5` apre comunque le Credenziali. La videosorveglianza non ha una schermata propria.

**Android.** Cinque accessi: Mappa, Dispositivi, Rack, Cablaggio e Progetto. Barra inferiore nelle finestre compatte; rail da 600 dp di larghezza con almeno 480 dp di altezza. La scelta di una voce svuota lo stack sopra la mappa.

Progetto contiene:

- il controllo del progetto, solo se ci sono avvisi;
- gli altri gruppi, con icone;
- Azioni progetto: Esporta, Importa, password e Chiudi progetto.

La navigazione resta visibile e viene disabilitata mentre è aperto un editor a pagina intera o lo scanner, conservando la bozza. Le sottoschede sono fisse fino a tre; oltre diventano un menu Vista.

**Windows.** La barra laterale misura 208 dp e mostra le icone delle destinazioni. È estesa da 1200 dp di finestra; sotto tale soglia usa una rail da 104 dp con cinque accessi, indipendentemente dall’apertura dell’editor. Operazioni progetto elenca Esporta, Importa, password, poi Nuovo sito e Chiudi progetto.

Le scorciatoie Windows mantengono le associazioni: `Ctrl+1` Dispositivi, `Ctrl+2` Rack, `Ctrl+3` Modelli, `Ctrl+4` Mappa, `Ctrl+5` Credenziali, `Ctrl+6` Allegati, `Ctrl+7` Cablaggio, `Ctrl+8` Rete, `Ctrl+9` Alimentazione.

Gli elenchi nominali e i selettori usano il nome nella lingua corrente per ordinare copie di presentazione. L'ordine salvato resta invariato. VLAN, porte e unità rack conservano l'ordinamento tecnico; il cestino mostra prima le eliminazioni recenti. Inventario filtra anche per stato operativo (su Android il filtro compare quando esistono apparati non in servizio), distingue assenza di dati da ricerca senza risultati e mostra nome, collocazione e un riepilogo breve.

## Editor e configuratori

Ogni editor ha un titolo operativo, ad esempio Aggiungi dispositivo, Modifica dispositivo · nome o Configura porte · nome. Intestazione compatta e azioni Salva/Annulla restano separate dal corpo scorrevole. Form testuali centrati entro 640 dp; disegni tecnici a larghezza disponibile. Margini pagina 16/24 dp, campi distanti 16 dp e gruppi 24 dp. Il salvataggio usa Aggiungi, Salva modifiche o Applica secondo l'operazione; Annulla modifiche chiude la bozza, mentre l'annullamento dell'ultima operazione resta un comando distinto.

Le spaziature sono 8 dp fra elementi collegati, 16 dp fra campi e 24 dp fra sezioni; i margini sono 16 dp su Android e 24 dp negli editor desktop. Filtri e azioni vanno a capo quando necessario. I configuratori della mappa usano gli stessi contenitori degli altri editor.

Gli editor e i pannelli si aprono sempre dall'inizio: un nuovo oggetto, una nuova selezione o un altro editor non ereditano lo scorrimento precedente. Solo il ritorno da una porta riporta alla sezione Porte. Gli errori compaiono vicino al campo e nel riepilogo presso il salvataggio. Le sezioni con errori bloccanti si aprono automaticamente. Le righe checkbox sono interamente cliccabili, con un unico controllo semantico e altezza minima di 48 dp. Su Android il footer considera tastiera e barra di navigazione, consumando gli inset già applicati dal contenitore.

Riferimenti: [barra di navigazione Compose](https://developer.android.com/develop/ui/compose/components/navigation-bar), [forme Material 3](https://m3.material.io/styles/shape), [dialoghi Compose](https://developer.android.com/develop/ui/compose/components/dialog), [accessibilità predefinita](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [layout a flusso](https://developer.android.com/develop/ui/compose/layouts/flow), [gestione degli inset](https://developer.android.com/develop/ui/compose/system/insets-ui). Consultati il 4 ottobre 2026; gli esiti di verifica sono in [05-testing-and-benchmarks.md](05-testing-and-benchmarks.md).

## Sostituire una copia Windows con password diversa

Decifrare il pacchetto con la password ricevuta. Se la copia locale chiusa usa un’altra password, inserirla nel dialogo dedicato prima del confronto. Esaminare versione e avvisi, poi scegliere sostituzione o fusione. Annullare lascia la vecchia copia. Dopo la sostituzione usare la password del pacchetto ricevuto; la fusione mantiene quella locale. AUD-14 verifica il rollback del flusso completo media/progetto/base su errore; recupero dopo arresto improvviso completato con AUD-24.

AUD-15: modifiche Android confermate dopo persistenza riuscita, comandi ravvicinati ordinati con cestino/password/media/import/export. Undo vale soltanto per la stessa sessione e revisione: dopo una modifica successiva o chiusura non sostituisce lo stato corrente. Una richiesta di chiusura/cambio progetto invalida solo la pubblicazione UI, senza annullare le modifiche già richieste. Durante il lavoro il dialogo occupato blocca gli input; verifica visiva nativa in RES-19. Gli errori mappa su snapshot obsoleto vengono mostrati e richiedono la riapertura dell'editor.

L'export Android accodato dopo un cambio password verifica la protezione persistita al momento del comando, prima di aprire il file di destinazione. Le etichette leggono il progetto richiesto, anche se la navigazione cambia durante l'attesa.

Sostituzione apparato (AUD-16): Android e Windows conservano collocazione, U, altezza e tipo di montaggio; il nuovo apparato ha identità nuova e non eredita dati, porte o collegamenti. I figli del vecchio contenitore risalgono al genitore; il cestino conserva i dati per ripristinare la gerarchia. La fusione usa le stesse scelte di campi e porte del core. Android persiste tutto con il cestino in una transazione: un errore lascia l'apparato precedente recuperabile e il progetto invariato. Le opzioni per credenziali/configurazioni/alimentazioni/campi extra sono applicate dal core secondo AUD-25: selezionate trasferiscono, disattivate conservano nel cestino.

**Salvataggio interrotto — AUD-24.** All’avvio viene tentato il recupero; la copia Windows protetta richiede prima la password locale. Un recupero fallito conserva dati/backup e impedisce di usare il progetto fino alla riprova riuscita. Dopo un guasto di pulizia post-commit si conservano i dati confermati e la pulizia viene ritentata al successivo accesso. Politica e limiti in [storage](04-desktop-storage-interop.md).

## Fusione dei dati associati — AUD-25

Le quattro opzioni, attive per impostazione iniziale e selezionabili in entrambe le app, trasferiscono credenziali, configurazioni, alimentazioni e campi extra DEVICE al superstite. Un’opzione disattivata conserva i record nel cestino del duplicato: non restano riferimenti attivi verso l’apparato rimosso. Nei conflitti si conservano entrambi i record con ID, segreti, note e classificazione, senza sovrascrittura o deduplicazione. Le alimentazioni trasferiscono anche il riferimento al duplicato come sorgente di altri apparati.

I file delle configurazioni trasferite conservano ID, classificazione e byte: il destinatario diventa il superstite o la porta copiata; senza copia delle porte il file viene associato al superstite. Foto e file esclusi dal trasferimento restano recuperabili nel cestino e non sono esportati come dati attivi. La fusione che crea un auto-riferimento o un ciclo di alimentazione viene rifiutata prima di modificare progetto/cestino; si corregge il collegamento e si riprova, secondo decisione confermata.

Le nuove voci DEVICE conservano i record associati nel JSON locale esistente. Il ripristino richiede ID liberi e riferimenti ancora disponibili; un rifiuto conserva la voce. Vecchie voci senza dati associati restano leggibili. Per ripristinare i nuovi record occorre questa versione dell’app: una versione precedente ignora il campo opzionale. Nessuna modifica allo schema Room v2 o allo scambio .ofam v1; il cestino locale non viene scambiato.

AUD-28: quando import o ripristino incontra un ID già usato da un altro progetto Android, il comando termina con errore prima del salvataggio. I progetti restano invariati; occorre correggere il conflitto prima di riprovare. Nessuna rinumerazione automatica dei dati ricevuti.

AUD-29: se nuove alimentazioni tra gli apparati rimasti rendono ciclici i record da ripristinare, il ripristino viene rifiutato prima di ricrearli, anche con più sorgenti. Progetto, cestino e file restano conservati; si correggono i collegamenti e si riprova. Controllo condiviso con la fusione, schema e formato invariati.

## Controllo completo delle alimentazioni — AUD-30

Validazione del catalogo, fusione e ripristino usano un unico controllo del grafo nel modello condiviso. Ogni sorgente concorre al controllo, indipendentemente dall’ordine; nessuna ricorsione e nessuna deduplicazione dei record. Più alimentazioni verso la stessa sorgente non costituiscono da sole un ciclo. Il controllo riguarda tutti i collegamenti, compresi auto-riferimenti e cicli nascosti dietro sorgenti alternative.

Un catalogo ciclico genera POWER_FEED_CYCLE_DETECTED come STRUCTURAL_ERROR, con un messaggio sul grafo del progetto; l’import non restituisce un pacchetto utilizzabile. Fusione e ripristino conservano progetto/cestino sul rifiuto e permettono riprova dopo correzione. Eliminato il precedente percorso che seguiva la sola prima sorgente; nessuna modifica allo schema Room v2 o allo scambio .ofam v1.

## Save fallito nei form Windows — AUD-32

Nei form di progetto, sede e area, un errore di salvataggio conserva i dati inseriti e lascia l’editor aperto. Nella mappa l’errore compare nel dialogo; nel pannello Progetto resta raggiungibile il messaggio globale. Correggere il guasto e premere nuovamente Salva, oppure annullare esplicitamente la bozza. Il dialogo occupato copre temporaneamente form e conferme durante l’I/O (AUD-31). La riprova dei picker nel workspace mappa è descritta nel paragrafo AUD-33 qui sotto.

AUD-33 estende la riprova al workspace mappa Windows: oggetto e pagina/immagine/sfondo mantengono il picker sul save fallito, con errore interno e selezione conservata. Dopo aver risolto il guasto si usa lo stesso comando di conferma. Gli altri picker rapidi Windows sono corretti in AUD-34; la conferma asincrona mappa Android in AUD-35. AUD-36 completa gli editor Windows; AUD-37 completa gli edit generici Android; AUD-38/39 completati; restano AUD-40 e i collaudi/pulizie RES-13/19/23/24.

AUD-34 completato: anche i picker rapidi Inventario, Rack e unità rack e la pagina PDF da Allegati conservano la bozza sul save fallito, mostrano l’errore e consentono riprova. La selezione del nuovo rack cambia dopo il successo. AUD-36 completato: anche editor completi, collocazione/sostituzione/modifica multipla e form allegato conservano le bozze fino al successo. Sul guasto si risolve lo storage e si conferma dallo stesso form. Modelli/Credenziali Windows corretti in AUD-38; collaudo EXE in RES-23.

## Riprova nella mappa Android — AUD-35

Nuova sede/piano e inserimento rapido di oggetti si chiudono dopo la conferma del salvataggio. In caso di errore restano nome, tipo e posizione e compare il messaggio nello stesso form/picker: correggere il guasto e confermare di nuovo oppure annullare la bozza. La stessa regola vale per pagina PDF, immagine e rimozione dello sfondo; un media appena importato rimane disponibile per riprovare l’assegnazione. I comandi restano bloccati durante la persistenza; il successo offre un solo undo. Chiusura/cambio progetto e cancellazione impediscono callback tardivi. AUD-37 completa picker/editor Inventario/Rack/unità, collocazione/batch, struttura/modelli e pagina PDF da Allegati; comandi specializzati corretti in AUD-39; cancellazioni apparato/rack in AUD-40.


## Riprova fuori mappa Android — AUD-37

Anche i picker/editor Inventario e Rack, l'inserimento in unità rack, collocazione e modifica multipla, sede/piano e Modelli e la pagina PDF da Allegati mantengono la bozza sul save fallito. L'errore si legge nello stesso form (scorrere se necessario), poi si può confermare di nuovo o annullare. Il successo chiude la bozza e offre una sola modifica annullabile. Il cambio di bozza/composizione scarta gli esiti tardivi; il cambio sessione/cancellazione resta protetto dal ViewModel. AUD-39 completa i comandi specializzati; AUD-38 completa Modelli/Credenziali Windows. Le cancellazioni apparato/rack restano AUD-40.


AUD-38 completato (7 ottobre): anche Modelli/Credenziali Windows conservano bozza e selezioni sul save fallito, con errore nel dialogo/pannello e riprova. Dieci regressioni nuove e 203 test Windows verdi; history e undo verificati. Collaudo della cornice DesktopApp/EXE ancora in RES-23. AUD-39 completa i comandi specializzati Android; evidenze nella roadmap.


AUD-39 completato (7 ottobre): sostituzione/fusione, import/classificazione allegato, credenziali e download Android attendono il successo. Sul guasto conservano campi, selezioni e navigazione con errore/riprova; sessione, cancellazione ed editor dismessi non ricevono esiti tardivi. 530 report JVM/Compose e 8 prove native verdi. Il collaudo usa sorgenti sintetiche e persistenza reale, senza reti pubbliche. AUD-40 completa le cancellazioni apparato/rack; RES-13/19/23/24 conservati. Evidenze nella roadmap.

AUD-40 (8 ottobre): le schede Android di apparato e rack restano aperte fino al commit della cancellazione. Il ritorno usa la scomparsa dell'oggetto dal progetto persistito, senza un nuovo callback; l'effetto controlla progetto e destinazione correnti. Sul guasto rimangono scheda/selezioni ed errore, con riprova. moveToTrash verifica la cancellazione coroutine e pubblica messaggio/undo solo nella sessione di origine ancora attiva. Le prove native usano AppRoot e SQLCipher non viene coinvolto: database Room e media sintetici isolati. Evidenze nella roadmap.

AUD-41 (8 ottobre): la continuazione della serie foto Android attende la fine del comando tramite Job.join, dopo commit, cleanup e rilascio dello stato occupato. La foto pendente conserva la sessione di origine; risultati di una precedente apertura dello stesso progetto, errori tardivi e callback cancellati vengono ignorati. Il callback Compose controlla lo scope della schermata prima di riaprire il launcher. Nove prove JVM mirate e cinque prove native con camera/permesso sintetici, FileProvider e repository reali verificano DEVICE/PORT/CABLE, annullamento, guasto/riprova, undo e scambio cifrato. Scatti reali rinviati per decisione utente: RES-13 resta parziale.

RES-23 (8 ottobre): l'editor completo del nuovo rack nell'EXE conserva bozza/nome su errore reale del filesystem e permette Ctrl+S dopo il rilascio. Un solo rack persistito dopo chiusura/riapertura del progetto sintetico. Altri editor e matrice focus/input restano nel tracker; dettagli nella roadmap.

## Salvataggio Alimentazioni/PoE/Badge Windows

Creazione e modifica conservano la bozza quando la sostituzione del pacchetto fallisce. Errore leggibile nel pannello/dialogo, selezioni e campi nascosti conservati; Nuovo/Modifica passa dalla conferma di scarto esistente. Rilasciato il blocco, Salva registra una sola modifica annullabile e chiude il form. AUD-44 verificato con dodici regressioni e collaudo della nuova alimentazione nell’EXE; dettagli nella roadmap.
