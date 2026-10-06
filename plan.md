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

Confermati: allegato 32 MiB, pacchetto 256 MiB, totale decompresso 512 MiB, 10.000 entry e costo PBKDF2 massimo 1.000.000. Rifiuto strutturale durante la lettura e prima della derivazione; stessa politica di dimensione sui pacchetti generati. Misure ai limiti concluse e registrate nel [report import grandi](docs/testing/import-benchmark-2026-10-05.md). AUD-13 chiuso: verifica prima del commit e rollback negli ingressi media locali. Android protetto verifica un budget conservativo senza conservare la password; verifica esatta vicino a 256 MiB ancora in AUD-23. AUD-14/15 chiusi: import/fusione reversibili, base coerente e comandi/letture Android ordinati; arresto improvviso e secondo guasto di rollback restano in AUD-24.
