# Piano prodotto

Aggiornato al 6 ottobre 2026 dopo confronto con il codice.

## Obiettivo e limiti

OnlyFieldAssetManager gestisce inventario, posizione, cablaggio, rete logica, alimentazione, media e documenti di un progetto tecnico. Funziona offline; export e import sono manuali. Non include server, sincronizzazione automatica, cloud obbligatorio o merge automatico.

## Decisioni applicate

- Android 14+ e Windows 11 x64 portable condividono modello, regole, form, configuratore e formato di scambio.
- Progetto greenfield (5 ottobre 2026): contratto `.ofam` 1 e database Room 2, senza migrazioni né lettura dei formati 1.x precedenti. Decisione AUD-03 confermata: ricreazione dei dati di prova al cambio versione e reimport del demo, senza conservazione automatica. Lo schema cambia sempre insieme alla versione Room.
- Il formato `.ofam` è un archivio ZIP versione 1; ogni altra versione è rifiutata.
- La password del pacchetto cifra progetto e allegati con AES-GCM; la password di progetto usa PBKDF2-HMAC-SHA256. Su Windows la protezione comprende tutti i media locali: persistenza nel `.ofam` cifrato, lettura progressiva dopo sblocco, RAM limitata e deposito temporaneo cifrato con chiave effimera; nessuna copia temporanea in chiaro. Modifica interna autorizzata il 5 ottobre per mantenere i limiti degli import grandi. Le copie per programmi esterni vengono esportate soltanto su scelta esplicita dell'utente.
- Gli errori strutturali bloccano l'import; gli avvisi documentali non bloccano il salvataggio.
- I modelli sono separati dalle istanze. Le modifiche dei modelli vengono applicate solo con un'azione esplicita.
- La mappa usa coordinate normalizzate, contenitori annidati e percorsi cavo; immagini e PDF sono allegati offline.
- I documenti non esportano credenziali. Il testo utente resta invariato dalla localizzazione.
- Configuratori: disegno specifico del tipo prima dei campi, sezioni tecniche richiudibili dopo. Piano, rack e porte restano facoltativi; dall'inventario la sede richiede una scelta esplicita.
- Navigazione condivisa per gruppi Lavoro, Dati tecnici, Supporto e Progetto. Gli elenchi nominali sono ordinati solo nella presentazione; porte, VLAN e unità rack mantengono il loro ordine tecnico.
- Titolo operativo, corpo scorrevole e azioni persistenti distinguono gli editor. L'espansione delle sezioni non modifica i dati né il contratto di scambio.
- Mappa: `MapScene` unica per piano e contenitori; ogni contenitore è un solo oggetto, navigabile fino all'ultimo livello. Una linea per coppia di oggetti, con elenco completo dei cavi al tocco. Pannello dettagli non modale, in basso o laterale da 840 dp. Rack, mensola, armadio e cassetta sono contenitori predefiniti; i tipi predefiniti non vengono più duplicati.
- Collegamenti logici (WAN, VPN, Internet) agganciati al dispositivo: compaiono nel pannello della mappa (Altri dettagli) ma non nel percorso porta né sulla mappa, che mostrano solo la continuità fisica.
- Tracciamento fisico essenziale: porta → cavo → passanti (patch panel, presa, scatola di giunzione) → porta. Rimossi dorsali (`SharedPathSegment`), `connectedPortId`, tipo e note dei passaggi, connettori A/B, orientamento, caratteristiche nominali e velocità osservata del cavo. Il cavallotto è un cavo tra due porte; la giunta è una scatola passiva con passante fronte/retro.
- Collegamenti dal disegno dell'apparato: il tocco su una porta apre una scheda rapida (stato, percorso nei due versi, etichetta cavo suggerita, Collega a…, Inserisci passaggio, Scollega, Foto). La pagina porta completa resta in Dettagli porta.
- La foto è un'azione primaria e sempre presente su apparati, rack, porte e cavi (allegati con destinazione `PORT` inclusa). Nel pannello mappa la sezione Foto compare anche vuota; identificativi e dati tecnici sono in Altri dettagli.
- Preset hardware e logica delle porte nel core (`DevicePresets`, `PortLogic`, `PortArrangement`, `PassiveCabling`). Disposizione e supporto PoE personalizzati nei campi facoltativi del JSON hardware, senza cambio delle tabelle Room o della versione `.ofam`. Porte in tre passi (tipologia → quantità → etichetta); VLAN e PoE multipli sulle entità esistenti.

## Configuratore visivo — 5 ottobre 2026

- Colori per tipo coerenti su mappa, topologia e schede; selezione contrastante, nome completo e canvas contenuto nel riquadro.
- Inserimento uniforme: tipo → disegno/preset/nome → Aggiungi. Riquadri, menu aperti evidenziati e azioni principali raccolte.
- Dimensioni e profondità tolte dalla UI, valori precedenti conservati; U solo dove servono per rack e modelli montabili.
- Disegni Fronte/Retro interattivi; griglie una/due righe riordinabili senza alterare porte o collegamenti; supporto PoE selezionabile.
- Tratte fisse: selezione dei frontali e collegamento dei retro associati. Cavetti frontali distinti; nessuna deduzione di associazioni ambigue.
- Configurazione in bozza con Salva; Collega e Foto immediate fuori dagli editor. Evidenze e limiti Android in roadmap e PROJECT_STATUS.

## Evoluzione: censimento rete (intervista del 5 ottobre 2026)

**Profilo.** Tecnico singolo o integratore; progetti medi (3–20 sedi, 100–1000 apparati); rilievo manuale. Il rilievo si fa su Android, la rifinitura e la consegna su Windows con scambio `.ofam`. Al cliente si consegnano PDF di documentazione ed Excel di inventario.

**Valutazione.** Il modello fisico (porta → cavo → passanti → porta, `ConnectionGraph`) è adatto al censimento e va mantenuto. I limiti sono:

- la sola vista planimetrica;
- la gerarchia BU/sede/piano ambigua;
- l'inserimento lento dei cavi in blocco;
- il PDF Desktop solo testuale.

**Decisioni.**

- **Nessun modulo viene rimosso.** Videosorveglianza, credenziali, configurazioni e badge restano nel modello. Nella UI diventano sintetici: chiusi o nascosti se vuoti, espandibili in futuro.
- **Gerarchia Sede → Piano** (applicata). La vecchia BU è diventata la sede (`Site`), con gruppo e indirizzo facoltativi; il livello intermedio `Site` e `Device.siteId` sono stati rimossi. L'apparato appartiene a una sede; il piano resta facoltativo e la sua assenza produce un avviso documentale.
- **Stato operativo dell'apparato** (in servizio, spento, dismesso, da verificare). È distinto da `Observation`, che resta lo stato del rilievo.
- **Mappa visiva su tre viste:**
  - la planimetria esistente;
  - la topologia fisica degli apparati attivi, con passanti collassati e percorsi completi come archi, filtrabile per sede e piano;
  - lo schema del singolo percorso end-to-end.
- **Esclusioni.** Niente mappa geografica delle sedi, discovery automatica (LLDP/SNMP), import da Excel o altri strumenti, né lavoro su più telefoni in parallelo.

**Ordine di lavoro** (dettaglio in [PROJECT_STATUS.json](PROJECT_STATUS.json)):

1. Modello: EVO-01 gerarchia, EVO-02 stato operativo. Si fanno prima perché sono modifiche incompatibili.
2. Campo: EVO-03 cablaggio in blocco, EVO-04 ricerca globale con "Vai a", EVO-05 foto più rapide.
3. Viste: EVO-06 schema del percorso, EVO-07 topologia fisica.
4. Consegna: EVO-08 PDF Desktop con planimetrie, rack, percorsi e topologia; EVO-09 Excel con foglio dei percorsi end-to-end.
5. EVO-10 moduli secondari sintetici.

Stato al 5 ottobre 2026: EVO-01…EVO-10 completati; evidenze in [roadmap.md](roadmap.md).

## Stato

L’audit completo del repository del 5 ottobre identifica 11 nuovi rilievi oltre ai quattro collaudi/pulizie già aperti. Priorità, dipendenze e criteri di chiusura nel [tracker](PROJECT_STATUS.json); evidenze e limiti nel [report dei residui](docs/repo-residuals-2026-10-05.md). Le funzionalità dichiarate implementate restano distinte dalle correzioni e dai collaudi da eseguire.

Le funzionalità previste sono implementate; l’audit ha rilevato difetti e flussi da completare nei confini di persistenza, scambio e media. Lo stato aperto è mantenuto esclusivamente in [PROJECT_STATUS.json](PROJECT_STATUS.json); lo storico verificabile è in [roadmap.md](roadmap.md).

PDF Android: le sei sezioni offerte sono complete e paginate, inclusi inventario, rack e tutti gli allegati selezionati; RES-21 chiuso con prove native API 37. La stampa fisica resta un collaudo distinto.

## Decisione limiti pacchetti — 5 ottobre 2026

Confermati: allegato 32 MiB, pacchetto 256 MiB, totale decompresso 512 MiB, 10.000 entry e costo PBKDF2 massimo 1.000.000. Rifiuto strutturale durante la lettura e prima della derivazione; stessa politica di dimensione sui pacchetti generati. Misure ai limiti concluse e registrate nel [report import grandi](docs/testing/import-benchmark-2026-10-05.md). AUD-13 chiuso: verifica prima del commit e rollback negli ingressi media locali. Decisione confermata il 6 ottobre (AUD-23 chiuso): Android protetto mantiene il budget prudenziale, senza richiesta o persistenza aggiuntiva della password; accettato il possibile rifiuto vicino a 256 MiB anche se il ZIP effettivo entrerebbe nel limite. Verifica esatta esclusa dal requisito corrente. AUD-14/15 chiusi: import/fusione reversibili, base coerente e comandi/letture Android ordinati; recupero da arresto e secondo guasto completato con AUD-24.

## Correzioni audit — 6 ottobre 2026

AUD-16/17 completati: operazioni apparati Android delegate alle regole comuni e salvate con cestino in una transazione; rilievo assente documentato come Da verificare, con stati, note e avvisi pertinenti nei formati di consegna. Decisioni e limiti di prodotto invariati. AUD-25 completato con politica confermata per le quattro opzioni di fusione dei dati associati. Evidenze in roadmap; solo lavoro aperto nel tracker.

AUD-18 completato: cancellazione e proprietà dei pacchetti importati Android verificati durante lettura/KDF, confronto, ritorno dal worker, conferma e fusione. Import obsoleti non pubblicano stato; commit già iniziato termina senza UI tardiva. Restano invariati schema, formato e collaudi nativi.

AUD-19 completato: scambio dei soli media attivi, conservazione locale per undo/cestino e raccolta dei soli file di proprietà quando scadono. Media del cestino conservati anche nella sostituzione; rimozione definitiva/progetto con rollback su guasto. AUD-26 completato successivamente: ripristino bloccato se manca il sito originale; tipi non supportati conservati nel cestino con media, credenziali ripristinate tramite regole condivise. AUD-27 completa i controlli dei riferimenti secondari nel progetto corrente; collisioni tra progetti Android in AUD-28.

AUD-21 completato il 6 ottobre: callback password e cancellazione Android, errore e persistenza reversibile delle preferenze Windows; 415 test e APK verificati. Collaudo nativo messaggi/focus in RES-19/23.

Decisione AUD-26 del 6 ottobre: bloccare e conservare nel cestino quando manca il sito originale, senza scelta implicita di un altro sito. Applicata su entrambe le app; ID delle entità già attivi non vengono sovrascritti. 425 test e APK verificati in AUD-26; i riferimenti secondari sono stati verificati e corretti in AUD-27.

AUD-27 (6 ottobre): il ripristino richiede i piani originali nella stessa sede, contenitori/figli e montaggi ancora disponibili. Un contenitore spostato su un altro piano o un figlio ricollocato bloccano il ripristino. ID di porte attive, porte duplicate nel JSON e ID di collocazioni già presenti sono rifiutati; nessuna collocazione saltata o sostituita. Progetto, credenziali, base di scambio, cestino e media restano invariati su rifiuto; si può riprovare dopo aver ripristinato il contesto. Le foto delle porte di un apparato nel cestino restano locali anche quando un apparato attivo riusa l’ID della porta e non vengono esportate. Controlli nel progetto corrente; collisioni tra progetti Android tracciate separatamente in AUD-28.

AUD-22 completato: scaffolding/test tautologici e API Android di sola prova rimossi; ricerca/edit/tracciamento usano i percorsi condivisi reali. Fixture utile soltanto nelle risorse test. Confermata politica AUD-24: recupero automatico, richiesta password locale per Windows protetto e blocco/riprova su errore, conservando dati e backup; implementazione completata con journal locale cifrato, senza cambi a Room v2 o `.ofam` v1.

AUD-24 completato secondo la politica confermata: staging/backup cifrati e durevoli, recupero automatico con password locale Windows protetto, blocco e riprova su guasto. Generazione locale della base Room monotona anche per import identici; nessuna migrazione o cambio di formato di scambio. AUD-25: politica confermata — opzioni attive trasferiscono i dati associati al superstite, disattive li conservano nel cestino; conflitti conservano entrambi con ID e classificazioni originali.

AUD-25 implementato: quattro scelte disponibili su Android/Windows, ID/classificazione/entrambi i conflitti conservati, dati esclusi ripristinabili dal cestino. Confermato il rifiuto della fusione che crea auto-riferimenti o cicli, con progetto/cestino invariati e riprova dopo correzione. File delle configurazioni trasferiti con i record; nuove voci locali richiedono questa versione per ripristinare associatedData, senza schema o formato di scambio nuovi.

AUD-28 implementato: persistenza Android rifiuta collisioni tra progetti su tutte le tabelle del catalogo, prima delle cancellazioni. Import/sostituzione/fusione/ripristino conservano le copie precedenti e non rimappano ID. AUD-29 riprodotto e corretto: un ciclo ricreato dal cestino dopo modifiche alla rete blocca il ripristino e conserva i dati fino alla correzione, riusando il controllo della fusione.

AUD-30 implementato: controllo completo unico per validazione/fusione/ripristino; import rifiuta anche cicli nascosti dietro sorgenti alternative. Più alimentazioni e catene profonde restano ammesse quando acicliche; nessun nuovo formato o dipendenza. Verifica completa finale verde: 470 test, APK e APK test compilati; evidenze e checkpoint del 7 ottobre nella roadmap.

AUD-31 completato: il pannello occupato Windows è un dialogo sopra i form e le conferme, senza smontare le bozze. Collaudo EXE con 511 MiB e regressioni verdi; il save fallito del form è corretto in AUD-32.

AUD-32 completato: errore di save conserva le bozze nei sette form Windows di progetto/sede/area, con riprova dalla stessa schermata e una sola modifica annullabile. EXE isolato e 479 report verdi; nessuna nuova API o dipendenza. Riprova dei picker del workspace mappa completata con AUD-33.

AUD-33 completato: i picker della mappa Windows conservano bozza e selezione sul save fallito, con errore nel dialogo e riprova senza doppia modifica. Cinque regressioni, 484 report verdi, APK/EXE e collaudo nativo oggetto/pagina PDF; nessun nuovo formato o dipendenza. Gli altri picker rapidi Windows e le bozze asincrone mappa Android sono corretti in AUD-34/35; restano gli editor/host in AUD-36/37.

AUD-34 completato: picker Windows fuori mappa con bozze conservate fino al successo, errore interno e selezione rack dopo commit. 185 test Windows verdi; nessuna dipendenza o formato nuovo. AUD-36 separa il residuo da codice degli editor completi e del form allegato; AUD-35 completato.

AUD-35 completato: esito esplicito per gli edit asincroni della mappa Android, bozza conservata sul guasto e chiusura dopo successo; sessione/cancellazione e no-op verificati. 492 report JVM verdi e sei prove native su moto g86 API 36. Restano AUD-36/37 (editor/picker fuori dal perimetro corretto, evidenza da codice) e RES-13/19/23/24; nessuna nuova dipendenza, schema o formato.
