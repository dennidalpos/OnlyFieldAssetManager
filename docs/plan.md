# OnlyFieldAssetManager — Piano operativo di sviluppo

Aggiornato: 2 ottobre 2026. Eseguire gli step Android A00–A13 in Android Studio con Gemini; iniziare gli step Windows W00–W05 soltanto dopo il completamento di A13. Questo documento contiene il contesto da fornire agli agenti e le istruzioni di implementazione. Lo stato delle attività e le evidenze sono in roadmap.md.

## 1. Contesto operativo

Realizzare un editor offline per tecnici di networking: censire infrastrutture esistenti, aggiornarle dopo gli interventi e consultarne la documentazione durante i guasti. Prima consegnare un APK Android 14+ per smartphone/tablet; successivamente un editor Windows 11 x64 portable con parità funzionale entro i limiti hardware.

Applicare questi vincoli durante tutti gli step:

- Rappresentare progetto → BU → sede facoltativa → piano/area; distinguere appartenenza organizzativa e posizione. Consentire apparati senza posizione rilevata e collegamenti tra aree/sedi.
- Usare gli stessi dati per schede, inventario, planimetrie, rack fronte/retro, rete logica, alimentazione e documenti. Mantenere ID stabili dopo rinomina e spostamento.
- Distinguere posizione, cablaggio fisico, relazioni logiche e alimentazione. Distinguere tratti di cavo, passaggi nei pannelli, catene complessive e percorsi grafici condivisi.
- Conservare dati verificati, da verificare, in conflitto e non rilevati, con fonte/data pertinenti. Consentire il salvataggio delle incoerenze documentali con avvisi; rifiutare dati strutturalmente invalidi e pacchetti corrotti.
- Gestire una copia di lavoro alla volta attraverso export/import completo manuale. Usare Drive, USB o altri servizi soltanto come mezzi di trasferimento. Nessun server o account richiesto dall'app.
- Integrare le credenziali nel progetto. Password del progetto opzionale, senza secondo sblocco obbligatorio. Senza password le credenziali sono consultabili. Cambio/rimozione richiedono quella attuale; nessun recupero della password dimenticata.
- Conservare collegamenti con estremità scollegate e da verificare quando si eliminano porte/apparati. Sostituire un apparato eliminandolo e creandone uno nuovo, senza ereditare dati o riferimenti.
- Preservare originali di foto/allegati; includerli nel pacchetto completo. Non imporre un tetto numerico prefissato: gestire risorse, spazio e annullamento delle operazioni.
- Esportare PDF, XLSX, Markdown e report PDF composti; supportare stampa. PDF/XLSX/stampa rispettano filtri e colonne visibili e includono tutto il contenuto risultante, indipendentemente dal viewport. Escludere sempre i campi segreti; selezionare e riesaminare note/media prima della condivisione.
- Escludere componenti interni, singole fibre/coppie/giunzioni, storico interno, backup automatico, merge di copie, collaborazione simultanea, CSV, SVG, audio, trascrizione, discovery, monitoraggio e parsing automatico delle configurazioni.

## 2. Ambiente, struttura e confini del codice

Usare Android Studio con Gemini per l'intera fase Android. Verificare la disponibilità di Gemini nella versione installata e accedere alle sue funzioni di modifica/build/dispositivo secondo la configurazione effettiva. Le funzioni AI di sviluppo possono richiedere Internet; l'app deve funzionare offline.

Base tecnica Android: Kotlin, Jetpack Compose, ViewModel, coroutines/Flow e Room/SQLite. Usare le API Android per acquisizione, selezione dei file, condivisione e stampa. Scegliere e fissare versioni compatibili durante A00; non inventare versioni o dipendenze. Organizzare UI e accesso ai dati senza introdurre framework o livelli privi di necessità.

Creare in A00 la seguente struttura. Aprire la radice Gradle in Android Studio; mantenere il codice mobile e quello PC in cartelle separate.

```text
OnlyFieldAssetManager/
├── AGENTS.md
├── README.md
├── plan.md
├── roadmap.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/                         # wrapper e catalogo delle versioni
├── mobile/
│   └── app/
│       ├── build.gradle.kts
│       └── src/
│           ├── main/               # UI Android, storage, acquisizione e stampa
│           ├── test/
│           └── androidTest/
├── pc/                             # riservata; nessun codice Windows prima di W00
├── shared/
│   ├── core/                       # modello e regole, senza API Android
│   └── exchange/                   # serializzazione/validazione, senza UI
├── docs/
│   ├── contract/                   # modello, formato, protezione e conversioni
│   ├── decisions/                  # scelte tecniche correnti motivate
│   ├── testing/                    # dispositivi, volumi, prove ed evidenze
│   └── release/                    # build, firma, aggiornamento e distribuzione
└── fixtures/                       # campioni sintetici condivisi e attesi
```

Generare wrapper e scaffolding con Android Studio/Gradle; non scrivere a mano file generati. Includere inizialmente soltanto i moduli mobile e comuni nelle impostazioni Gradle. Creare i sottopercorsi quando necessari allo step. In W00 aggiungere il modulo desktop in pc/app/.

Tenere regole e serializzazione comuni nei moduli shared/, compatibili con JVM e privi di Context, Uri, Room e componenti UI Android. Implementare storage, fotocamera, file picker e stampa nell'app pertinente. Non copiare il database Android come formato di scambio.

Generare .gitignore per build, cache, configurazioni locali, APK/distribuzioni, file temporanei e materiali di firma. Conservare fuori dal repository chiavi, password e progetti reali. Non modificare configurazioni IDE già presenti senza necessità. Non inizializzare Git né effettuare commit/push senza richiesta.

## 3. Procedura per Gemini e per gli agenti Windows

Eseguire uno step alla volta. Prima di ogni step leggere AGENTS.md, questo piano, roadmap.md e i contratti pertinenti. Controllare i prerequisiti, i chiamanti e le verifiche esistenti; eseguire la baseline disponibile prima di modificare il codice.

Passare all'agente questa istruzione, sostituendo il codice dello step:

```text
Implementa soltanto lo step <Axx/Wxx> di plan.md. Leggi prima AGENTS.md,
README.md, roadmap.md e i contratti coinvolti. Controlla lo stato reale del
progetto e verifica la baseline disponibile. Descrivi brevemente le modifiche,
esegui lo step e le verifiche richieste, poi riesamina l'intero diff.
Non aggiungere funzioni fuori step, non duplicare implementazioni e non
alterare verifiche per farle passare. Non inserire dati fittizi nei percorsi
di produzione; usa soltanto fixtures e test per i campioni sintetici.
Aggiorna roadmap.md con stato, comandi/esiti e limiti reali. Non segnare
completato uno step senza evidenza. Dopo due fallimenti consecutivi sullo
stesso errore fermati e riporta errore, tentativi e ipotesi.
Non effettuare commit o push. Non iniziare lo step successivo.
```

Riesaminare le modifiche proposte da Gemini secondo i controlli dell'IDE. Documentare le scelte non derivabili dal codice in docs/decisions/ e rimuovere le istruzioni sostituite. Aggiornare il contratto e i test quando cambia un dato; prima di modificare un formato già distribuito, definire compatibilità e conversione.

Usare dati e credenziali sintetici nei test e nelle conversazioni AI. Mostrare un errore esplicito per problemi di persistenza/import/export; non usare fallback silenziosi. Le funzionalità non disponibili nello step non devono apparire come completate.

## 4. Fase Android — Android Studio e Gemini

### A00 — Preparare ambiente e generare struttura

**Prerequisito:** nessuno step implementativo precedente.

**Attività:** verificare Android Studio, Gemini, SDK, JDK e dispositivi/emulatori disponibili. Generare la struttura della sezione 2 e un'app Compose minima con minSdk 34. Selezionare versioni stabili compatibili e registrarle in docs/decisions/toolchain.md. Configurare soltanto mobile e shared; riservare pc/. Creare docs/testing/devices-and-volumes.md distinguendo dispositivi disponibili e prove ancora da svolgere. Definire campioni e soglie iniziali di prestazione/leggibilità da misurare durante gli step e consolidare nel pilota A13.

**Verifica/uscita:** sync Gradle riuscita, build debug e avvio su emulatore/dispositivo Android 14+. Registrare i comandi effettivi. Non dichiarare verificato hardware assente.

### A01 — Definire modello comune e contratto iniziale

**Prerequisito:** A00.

**Attività:** implementare identità UUID, entità organizzative, apparati/porte e osservazioni in shared/core/. Documentare cardinalità, estremità note/ignote/scollegate, rimozioni e distinzione tra warning documentali ed errori strutturali. Definire l'estensione e la versione iniziale del pacchetto: ZIP con JSON UTF-8, manifesto, allegati e checksum SHA-256; struttura JSON Schema e controlli delle relazioni separati. Definire come estendere il contratto nei successivi step senza perdere dati.

**Verifica/uscita:** test di identità, riferimenti, posizione assente, dati in conflitto e serializzazione iniziale. Campione sintetico con due BU, nomi/IP ripetuti in ambiti diversi, sede facoltativa e risultati attesi in fixtures/. Nessun contratto pubblico dedotto da una tabella Room.

### A02 — Persistenza locale e primo flusso di inventario

**Prerequisito:** A01.

**Attività:** implementare Room, migrazioni provate, creazione/apertura del progetto, BU/sedi/aree, apparati, porte, schede e ricerca per nome/IP/etichetta. Separare ID, nome tecnico, etichetta fisica e alias. Consentire apparati fuori rack e non localizzati. Applicare transazioni e indicatore di salvataggio soltanto dopo commit. Scrivere/verificare media prima dei riferimenti definitivi quando introdotti in A06.

**Verifica/uscita:** creare, rinominare, spostare, chiudere e riaprire un progetto offline mantenendo ID/dati. Verificare rollback, esito visibile degli errori e isolamento degli ambiti di ricerca. Provare migrazioni con copie sintetiche e senza alterare l'originale di prova.

### A03 — Export/import completo e confronto delle copie

**Prerequisito:** A02.

**Attività:** implementare shared/exchange/ e selezione file tramite Storage Access Framework. Esportare una fotografia coerente dello stato corrente, senza cestino o stato di undo. Validare manifesto, JSON, riferimenti, checksum, percorsi e risorse prima dell'import. Preparare l'import separatamente e sostituire la copia corrente soltanto al completamento. Usare ID di progetto/revisione/export e provenienza degli scambi; le date non provano da sole la successione.

**Verifica/uscita:** export → import su installazione Android distinta → modifica → export → reimport. Confrontare semanticamente dati e checksum, non l'identità binaria degli archivi. Mostrare sostituisci/annulla, copie identiche, precedenti/divergenti quando dimostrabili e provenienza incerta negli altri casi. Consentire una copia precedente dopo avviso. File troncato, spazio insufficiente e annullamento non devono danneggiare il progetto corrente.

### A04 — Credenziali e password del progetto

**Prerequisito:** A03.

**Attività:** implementare credenziali associate ad apparati/gruppi e progetto senza password. Scegliere librerie consolidate per database cifrato, file cifrati con autenticazione e derivazione dalla password, verificando licenza, manutenzione e integrazione Android/JVM. Valutare SQLCipher e libsodium o equivalenti documentati. Specificare protezione di database, journal, media, miniature e temporanei; non considerare sufficiente cifrare soltanto l'export. Documentare il contenitore protetto, i parametri crittografici e i vettori di prova sintetici senza progettare algoritmi propri.

**Verifica/uscita:** progetto aperto/protetto, password errata, archivio alterato/troncato, cambio/rimozione con password attuale e riapertura su un altro Android. Una conversione interrotta mantiene utilizzabile la versione precedente. Nessuna credenziale reale nei campioni e nessun segreto in log, ricerca ordinaria o badge. Gli export già archiviati conservano la protezione originaria. Nessun secondo sblocco obbligatorio o recupero password.

### A05 — Rack, modelli flessibili e primo PDF

**Prerequisito:** A04.

**Attività:** implementare rack con U, numerazione, fronte/retro, montaggio verticale/su ripiano e apparati fuori rack. Creare modelli schematici configurabili per categorie di rete, pannelli, UPS/PDU, server/NAS/appliance, telecamere/NVR-DVR, ripiani, pannelli ciechi e oggetti personalizzati. Marca/modello commerciali facoltativi; nessuna grafica fotorealistica. Consentire struttura delle porte, gruppi, ingressi/uscite e risorse combo alternative. Salvare/trasferire i modelli separatamente con contratto documentato; escludere credenziali, seriali, IP/MAC reali, cablaggio ed evidenze. Le istanze mantengono la definizione usata e non cambiano aggiornando un modello. Realizzare il primo PDF leggibile di scheda/rack, completo e senza segreti.

**Verifica/uscita:** scegliere la stessa porta da schema ed elenco, accedere a label complete, riaprire un modello importato e preservare le istanze. Far consultare rack/scheda/PDF a un tecnico diverso dall'autore e registrare problemi prima dei domini successivi.

### A06 — Planimetrie, foto, allegati e annotazioni

**Prerequisito:** A05.

**Attività:** importare immagini e PDF multipagina con pagine assegnabili alle aree. Acquisire foto, preservare originali, usare miniature e caricamento progressivo. Gestire testo, frecce, forme e zone evidenziate come annotazioni separate dai collegamenti. Collocare rack/apparati e consentire navigazione al dettaglio. Sostituire lo sfondo preservando elementi riposizionabili. Classificare note/allegati come condivisibili, riservati o da riesaminare; integrare la protezione di A04 e l'export completo.

**Verifica/uscita:** riapertura offline senza riferimenti a file esterni mancanti, round-trip di originali/pagine/posizioni/annotazioni, annullamento acquisizione e gestione risorse insufficienti. Una linea libera non crea un collegamento. Nessun limite numerico prefissato e nessun GIS o calcolo automatico delle lunghezze.

### A07 — Cablaggio, pannelli e percorsi condivisi

**Prerequisito:** A06.

**Attività:** implementare rame Ethernet, DAC, AOC, fibra complessiva, console, altro e sconosciuto. Gestire estremità, mezzo/connettori, caratteristiche nominali, velocità osservata, etichette, colore, lunghezza rilevata con unità e osservazioni. Rappresentare pannelli, ponti e corrispondenze documentate senza presumere mapping 1:1. Comporre catene con passaggi ignoti. Definire percorsi a segmenti condivisi, utilizzabili anche parzialmente, con collegamenti tra aree/sedi. Associare orientamento ai collegamenti: nessuno, A→B, B→A, entrambe, con significato esplicito; non attribuirlo automaticamente al percorso comune.

**Verifica/uscita:** due cavi condividono un segmento mantenendo estremità/dati propri; modificarlo aggiorna entrambi i tracciati. Verificare pannelli passivi, catene incomplete, navigazione tra aree e persistenza. Escludere dettaglio delle singole fibre/giunzioni.

### A08 — Configurazioni, rete logica e videosorveglianza

**Prerequisito:** A07.

**Attività:** implementare configurazioni manuali, allegati originali datati, VLAN con ambito, subnet distinte, membership VLAN/porta tagged-untagged, access/trunk/native, L3, interfacce logiche e LAG. Rappresentare valori non rilevati e tutte le VLAN esplicitamente. Definire campi WAN/VPN/Internet e relativi endpoint: apparato oppure sede descritta; associare la VPN all'accesso sottostante quando noto. Associare telecamere a NVR/DVR o altro gestore quando noto. Aggiungere pochi campi extra tipizzati, con classificazione della riservatezza, preservati nel contratto.

**Verifica/uscita:** VLAN/IP ripetuti in ambiti diversi non si confondono; nessun vincolo VLAN/subnet uno-a-uno o copia automatica della configurazione attraverso i cavi. Una VPN non genera percorsi fisici presunti. Verificare telecamera esterna, dati discordanti e round-trip dei campi extra. Nessun parsing, accesso remoto o visione live.

### A09 — Alimentazione e badge documentali

**Prerequisito:** A08.

**Attività:** implementare ingressi/percorsi A/B, prese PDU, UPS e passaggi intermedi. Registrare VA e W distinti; carico/autonomia soltanto se rilevati con fonte/data. Documentare PoE attraverso switch/iniettore e alimentazione pertinente. Derivare badge da dati espliciti per VLAN, mezzo, PoE, dipendenza UPS, copertura parziale e questioni aperte; distinguere etichette libere.

**Verifica/uscita:** distinguere sorgenti comuni/diverse, percorsi incompleti e cicli documentali segnalati. A/B non certifica indipendenza e il rack non implica dipendenza UPS. Nessuna autonomia calcolata. Badge coerenti tra schede, viste ed export.

### A10 — Eliminazione recuperabile, fusione e modifiche multiple

**Prerequisito:** A09.

**Attività:** implementare undo di sessione e cestino locale fino allo svuotamento esplicito, protetto come il progetto ed escluso dagli export. Definire ripristino con anteprima dei riferimenti coinvolti. Applicare estremità scollegate/da verificare dopo eliminazioni. Sostituire apparati con oggetti nuovi senza dati ereditati. Implementare fusione guidata di duplicati con scelta dell'ID superstite, dati, collegamenti e conflitti; mai fondere automaticamente per nome/IP. Consentire modifica multipla di campi ammessi con anteprima e transazione; non clonare identificatori, IP/MAC, credenziali o cablaggio implicitamente.

**Verifica/uscita:** provare eliminazione porta/apparato, ripristino dopo riavvio, svuotamento, annullamento fusione e rollback delle modifiche multiple. Nessun collegamento cancellato silenziosamente, riferimento invalido o eredità da sostituzione. Nessuno storico delle versioni del progetto.

### A11 — Documenti completi e stampa

**Prerequisito:** A10; estendere il PDF già funzionante da A05.

**Attività:** completare PDF, XLSX, Markdown di schede/note e PDF composti da contenuti selezionati. Generare dai dati filtrati, non dal ritaglio dello schermo. Gestire panoramica e pagine di dettaglio, carta/orientamento, scala leggibile, intestazioni con progetto/BU/luogo/titolo/data/autore. Mantenere filtri, colonne, avvisi e incertezza. Distinguere estremità fuori ambito da estremità ignote. Integrare anteprima e stampa Android. Escludere sempre campi segreti; richiedere riesame esplicito dei contenuti non classificati e selezione dei riservati. Scrivere testo libero XLSX come testo, senza interpretarlo come formule.

**Verifica/uscita:** viste dense e multipagina complete oltre zoom/scorrimento, tutte le righe filtrate, label leggibili e contenuti condivisi riesaminati. Controllare PDF/XLSX/Markdown/report/stampa con segreti sintetici e avvisi. Annullamento e permessi/spazio insufficienti producono esiti espliciti. Verificare leggibilità con un altro tecnico.

### A12 — Acquisizione cartografica e funzionamento offline

**Prerequisito:** A11.

**Attività:** selezionare una fonte che consenta acquisizione persistente, trasferimento, documenti e stampa. Registrare termini, attribuzione, disponibilità e costi prima dell'integrazione; ottenere scelta dell'utente per servizi a pagamento. Non usare il servizio raster standard tile.openstreetmap.org per la funzione offline. Acquisire uno sfondo autosufficiente e mantenerne l'attribuzione in app/pacchetti/documenti. Non introdurre sincronizzazione o servizi necessari al flusso principale.

**Verifica/uscita:** acquisizione online, riavvio in modalità aereo e trasferimento su un altro Android con sfondo/attribuzione invariati. Quando il servizio non è disponibile, immagini/PDF locali e progetti esistenti funzionano; la funzione online segnala il proprio errore. Se manca una fonte autorizzata, documentare il blocco: lo step resta aperto.

### A13 — Pilota Android, rilascio e consegna per Windows

**Prerequisito:** A00–A12 completati con evidenza.

**Attività:** concordare e registrare smartphone/tablet reali, campione ordinario e impegnativo, BU/porte/collegamenti/media e soglie misurabili di apertura, ricerca, salvataggio/import/export, memoria e leggibilità. Usare 5 rack e circa 100 apparati per BU come riferimento, non limite. Eseguire un rilievo/aggiornamento e consultazione durante un guasto simulato. Verificare interruzioni, spazio esaurito, password, pacchetti alterati e aggiornamento dell'APK senza perdita dati. Firmare il rilascio con chiave custodita dal titolare fuori dal repository; documentare build/aggiornamenti. Consolidare contratto v1, conversioni, campioni protetti/aperti e risultati attesi per il futuro editor PC.

**Verifica/uscita:** APK installabile su smartphone e tablet Android 14+, flussi offline e domini richiesti completi, nessun difetto critico aperto di perdita dati/protezione. Registrare risultati reali e limiti in docs/testing/ e docs/release/. La parità Windows e il trasferimento Android↔Windows completi restano non verificati fino a W05. Avviare W00 soltanto dopo questa consegna.

## 5. Fase Windows — IDE desktop e altri strumenti AI

Usare un IDE desktop e l'assistente AI scelto per Windows, fornendogli gli stessi documenti, moduli comuni, fixtures e risultati Android. Non richiedere Gemini in Android Studio per questa fase. Eseguire W00–W05 in sequenza.

### W00 — Configurare app PC e scegliere toolchain desktop

**Prerequisito:** A13 completato.

**Attività:** generare pc/app/ e integrare il modulo desktop nel progetto senza spostare o duplicare mobile/. Valutare prima Kotlin/JVM con Compose Desktop per riusare shared/core/ e shared/exchange/. Provare storage cifrato, rendering PDF, file picker, stampa e runtime incluso su Windows 11 x64. Registrare toolchain/IDE/assistente e comandi. Se serve un altro stack, motivarlo prima di riscrivere componenti e mantenere il contratto e gli stessi test di conformità.

**Verifica/uscita:** finestra desktop avviabile, build Android ancora valida e distribuzione iniziale da cartella su PC senza JDK/toolchain. Nessuna equivalenza dedotta dalla sola compilazione.

### W01 — Aprire, proteggere e trasferire i progetti Android

**Prerequisito:** W00.

**Attività:** implementare storage desktop, salvataggio transazionale, import/export e password rispettando il contratto Android. Rendere esplicita la cartella dati; gestire cartella/chiavetta non scrivibile o rimossa, temporanei, blocco della stessa copia locale e aggiornamenti che preservano i dati. Usare fixtures Android senza alterarle.

**Verifica/uscita:** aprire ogni campione Android protetto/aperto, modificare un campo, esportare e riaprire su Android mantenendo ID, dati e checksum. Verificare password errata, import interrotto, copia vecchia/incerta, spazio insufficiente e riavvio.

### W02 — Implementare inventario, rack, modelli e media

**Prerequisito:** W01.

**Attività:** adattare a mouse/tastiera le funzioni A02, A05, A06 e A10: ricerca, schede, rack fronte/retro, elenco/schema porte, modelli, campi extra, planimetrie, PDF multipagina, foto/allegati, annotazioni, undo/cestino, fusione e modifica multipla. Acquisizione adattata all'hardware; nessuna modifica del significato dei dati.

**Verifica/uscita:** eseguire gli stessi scenari Android e confrontare risultati/relazioni dopo scambi ripetuti. Nessun campo mobile perso o ricostruito arbitrariamente; nessuna clonazione di dati reali nei modelli.

### W03 — Implementare cablaggio, logica e alimentazione

**Prerequisito:** W02.

**Attività:** portare A07–A09 utilizzando regole comuni: pannelli/passaggi, catene incomplete, percorsi condivisi, direzioni, VLAN/subnet/LAG, WAN/VPN, videosorveglianza, A/B/PDU/UPS/PoE, osservazioni e badge. Riutilizzare fixtures e criteri di correttezza già provati.

**Verifica/uscita:** rappresentare/modificare tutti i domini e preservare dati ignoti/in conflitto negli scambi Android↔Windows. Eliminazioni/sostituzioni conservano le estremità da verificare secondo il contratto.

### W04 — Implementare documenti, stampa e cartografia

**Prerequisito:** W03.

**Attività:** portare A11/A12 con anteprima desktop, stampa Windows, PDF/XLSX/Markdown/report composti, selezione dei contenuti, filtri/colonne e attribuzione cartografica. Riutilizzare le regole di composizione; adattare soltanto backend e interazione specifici.

**Verifica/uscita:** stesso contenuto significativo e stessa esclusione dei segreti sui due editor. Verificare viste dense, pagine, riferimenti fuori ambito, attribuzioni, offline e errori delle periferiche/file. Non richiedere identità binaria dei PDF.

### W05 — Interoperabilità finale e distribuzione portable

**Prerequisito:** W00–W04 completati.

**Attività:** eseguire Android → Windows → Android e Windows → Android → Windows con più cicli su tutti i campioni, protetti/aperti e di versioni precedenti. Confrontare dati, modelli, relazioni, posizioni, osservazioni e checksum dei media. Verificare parità funzionale, volumi concordati e consultazione da un altro tecnico. Preparare pacchetto portable x64 con runtime/dipendenze inclusi e istruzioni di aggiornamento, trasferimento e gestione password.

**Verifica/uscita:** app Windows avviabile da cartella/chiavetta su PC distinto senza IDE/toolchain, APK ancora verificato e nessun difetto critico di perdita dati/integrità/protezione. Conservare evidenze e checklist di rilascio; nessun rilascio dichiarato riuscito sulla sola base del piano.

## 6. Verifiche e registrazione degli esiti

Dopo A00 usare il wrapper generato dalla radice. Comandi previsti, da adattare soltanto se la configurazione generata usa nomi differenti:

```powershell
.\gradlew.bat :mobile:app:assembleDebug :mobile:app:testDebugUnitTest :mobile:app:lintDebug
.\gradlew.bat :mobile:app:connectedDebugAndroidTest
.\gradlew.bat :shared:core:test :shared:exchange:test
```

Dopo W00, se si adotta Compose Desktop:

```powershell
.\gradlew.bat :pc:app:test :pc:app:createDistributable
```

Questi comandi sono istruzioni future: wrapper, moduli e runner non sono ancora presenti. Verificare i task realmente generati, aggiungere soltanto i task nativi necessari e registrarli dopo esecuzione. Non disabilitare test/lint né usare bypass. Le prove su dispositivi assenti sono non verificate.

Per ogni step aggiornare roadmap.md con stato, data, comandi ed esiti, dispositivo/volume, evidenze e difetti aperti. Mettere schermate/report sintetici pertinenti in docs/testing/; tenere output voluminosi e temporanei in percorsi ignorati. Riesaminare il diff e rimuovere residui temporanei prima di dichiarare concluso lo step. Non conservare piani alternativi, implementazioni duplicate o istruzioni sostituite.

## 7. Riferimenti operativi

Consultare i riferimenti ufficiali per lo step in esecuzione e verificare compatibilità/licenze prima di aggiungere una dipendenza:

- [Gemini in Android Studio](https://developer.android.com/studio/gemini/overview) e [Agent Mode](https://developer.android.com/studio/gemini/agent-mode): accesso, modifiche, build e prove sul dispositivo.
- [Architettura Android](https://developer.android.com/topic/architecture/recommendations) e [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files): organizzazione mobile e selezione dei file.
- [SQLite: atomic commit](https://www.sqlite.org/atomiccommit.html), [JSON Schema](https://json-schema.org/learn/getting-started-step-by-step) e [UUID](https://www.rfc-editor.org/rfc/rfc9562.html): persistenza e componenti del contratto.
- [SQLCipher](https://www.zetetic.net/sqlcipher/), [libsodium: cifratura file](https://doc.libsodium.org/secret-key_cryptography/secretstream) e [derivazione password](https://doc.libsodium.org/password_hashing/default_phf): candidati da verificare in A04.
- [Firma Android](https://developer.android.com/studio/publish/app-signing): conservazione della chiave e aggiornamenti.
- [Distribuzione Compose Desktop](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html): runtime incluso e immagine applicativa senza installer.
- [OSMF Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/) e [attribuzione OSM](https://www.openstreetmap.org/copyright): selezione della fonte e consegna degli sfondi.
