# Piano prodotto

Aggiornato al 5 ottobre 2026 dopo confronto con il codice.

## Obiettivo e limiti

OnlyFieldAssetManager gestisce inventario, posizione, cablaggio, rete logica, alimentazione, media e documenti di un progetto tecnico. Funziona offline; export e import sono manuali. Non include server, sincronizzazione automatica, cloud obbligatorio o merge automatico.

## Decisioni applicate

- Android 14+ e Windows 11 x64 portable condividono modello, regole, form, configuratore e formato di scambio.
- Progetto greenfield (5 ottobre 2026): contratto `.ofam` e database Room ripartono da 1, senza migrazioni né lettura dei formati 1.x precedenti.
- Il formato `.ofam` è un archivio ZIP versione 1; ogni altra versione è rifiutata.
- La password del pacchetto cifra progetto e allegati con AES-GCM; la password di progetto usa PBKDF2-HMAC-SHA256.
- Gli errori strutturali bloccano l'import; gli avvisi documentali non bloccano il salvataggio.
- I modelli sono separati dalle istanze. Le modifiche dei modelli vengono applicate solo con un'azione esplicita.
- La mappa usa coordinate normalizzate, contenitori annidati e percorsi cavo; immagini e PDF sono allegati offline.
- I documenti non esportano credenziali. Il testo utente resta invariato dalla localizzazione.
- Configuratori: dati essenziali prima, sezioni tecniche richiudibili dopo. Piano, rack e porte restano facoltativi; dall'inventario la business unit richiede una scelta esplicita.
- Navigazione condivisa per gruppi Lavoro, Dati tecnici, Supporto e Progetto. Gli elenchi nominali sono ordinati solo nella presentazione; porte, VLAN e unità rack mantengono il loro ordine tecnico.
- Titolo operativo, corpo scorrevole e azioni persistenti distinguono gli editor. L'espansione delle sezioni non modifica i dati né il contratto di scambio.
- Mappa: `MapScene` unica per piano e contenitori; ogni contenitore è un solo oggetto, navigabile fino all'ultimo livello. Una linea per coppia di oggetti, con elenco completo dei cavi al tocco. Pannello dettagli non modale, in basso o laterale da 840 dp. Rack, mensola, armadio e cassetta sono contenitori predefiniti; i tipi predefiniti non vengono più duplicati.
- Collegamenti logici (WAN, VPN, Internet) agganciati al dispositivo: compaiono nel pannello della mappa (Altri dettagli) ma non nel percorso porta né sulla mappa, che mostrano solo la continuità fisica.
- Tracciamento fisico essenziale: porta → cavo → passanti (patch panel, presa, scatola di giunzione) → porta. Rimossi dorsali (`SharedPathSegment`), `connectedPortId`, tipo e note dei passaggi, connettori A/B, orientamento, caratteristiche nominali e velocità osservata del cavo. Il cavallotto è un cavo tra due porte; la giunta è una scatola passiva con passante fronte/retro.
- Collegamenti dal disegno dell'apparato: il tocco su una porta apre una scheda rapida (stato, percorso nei due versi, etichetta cavo suggerita, Collega a…, Inserisci passaggio, Scollega, Foto). La pagina porta completa resta in Dettagli porta.
- La foto è un'azione primaria e sempre presente su apparati, rack, porte e cavi (allegati con destinazione `PORT` inclusa). Nel pannello mappa la sezione Foto compare anche vuota; identificativi e dati tecnici sono in Altri dettagli.
- Preset hardware e logica delle porte solo nel codice (`DevicePresets`, `PortLogic`): nessun cambio a contratto o Room. Porte in tre passi (tipologia → quantità → etichetta); VLAN e PoE multipli sulle entità esistenti.

## Stato

Le funzionalita Android, Desktop e configuratore presenti nel codice sono completate. I residui operativi sono mantenuti esclusivamente in [PROJECT_STATUS.json](PROJECT_STATUS.json); lo storico verificabile e in [roadmap.md](roadmap.md).
