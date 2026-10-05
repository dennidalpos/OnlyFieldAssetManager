# Evidenze e residui

Aggiornato al 5 ottobre 2026 dopo confronto con il codice e il tracker.

## Evidenze disponibili

- I moduli Gradle sono `:shared:core`, `:shared:exchange`, `:mobile:app` e `:pc:app`.
- Il codice implementa `.ofam` 1 e Room 1 (ripartenza greenfield del 5 ottobre 2026), storage desktop atomico, database Android cifrato, import/export, documenti, mappe e configuratore condiviso.
- La suite locale registrata prima di questa revisione copre core, exchange, Android JVM e Desktop; le prove strumentali e manuali hanno limiti espliciti sotto.
- Il workflow GitHub esegue le suite JVM/Compose, genera APK debug e ZIP portable, calcola SHA-256 e pubblica soltanto su tag `v*`.

## Residui aperti

| ID | Stato | Evidenza richiesta |
| --- | --- | --- |
| RES-13 | Aperto | Checklist foto, scansione, multitouch e lettore USB su hardware reale. |
| RES-17 | Aperto | Suite UI Android verde su API 37 senza disabilitare controlli. |
| RES-19 | Parziale | Emulatore eseguito (UX-01, 02, 03, 05, 06); restano TalkBack, combinazioni mancanti e moto g86. |

Il dettaglio operativo e il criterio di chiusura sono in [PROJECT_STATUS.json](PROJECT_STATUS.json). La checklist hardware e in [docs/testing/hardware-checklist.md](docs/testing/hardware-checklist.md).

## Tracciamento fisico, scheda rapida porte e foto — 5 ottobre 2026

- **Greenfield v1**: `.ofam` riparte dalla versione "1" (rifiutati 1.7–1.11) e Room dalla versione 1, senza migrazioni né backup pre-aggiornamento; uno schema diverso viene ricreato vuoto (`fallbackToDestructiveMigration`, anche in downgrade). Rimossi schemi 13/14, migrazioni, conversione da database in chiaro e relativi test. I telefoni di prova vanno reimportati dal demo.
- **Modello snello**: rimossi dorsali (`SharedPathSegment` e sezione Percorsi in Cablaggio su Android e Windows), `Port.connectedPortId`, tipo e note dei passaggi, connettori A/B, orientamento, caratteristiche nominali e velocità osservata del cavo (anche dai modelli di cavo); il percorso porta non aggiunge più passi logici WAN/VPN. Export Markdown/XLSX: colonna Orientamento sostituita da Colore.
- **Correzione**: eliminare un apparato lasciava passaggi, VLAN, PoE e LAG orfani delle sue porte (errori strutturali e conflitti); ora vengono rimossi e il ripristino dal cestino ricrea i passaggi interni.
- **Passaggi intermedi**: nuovo tipo e preset Scatola di giunzione (1/2/4 passanti RJ45, LC o SC); `HardwareConfigurator.insertPassage`, `freePassages`, `disconnect`; `Device.isPassive()` unico per grafo e configuratore. Il demo instrada il primo cavo AP dei piani Nord attraverso GB-N0-01/GB-N1-01.
- **Informazioni primarie**: `PortSummaries` (stato, cavo, tratte nei due versi su un passante, estremi attivi, foto) e `CableLabels.suggest` (`SW-01/P5 – PP-02/P12`, sicura per Windows-1252), usata anche per le etichette PDF dei cavi senza codice. `portLabel` mostra l'etichetta fisica quando diversa dal nome.
- **Scheda rapida della porta** (`PortQuickDialog`): tocco su una porta nel pannello mappa, nell'editor e nella scheda dispositivo Android; Collega a… (apparato → porta libera dal disegno → mezzo ed etichetta), Inserisci passaggio (passante libero o nuova scatola), Scollega con conferma, Foto porta/cavo, Dettagli porta (`MapObjectDraft.focusPortId`).
- **Foto primaria**: destinazione allegati `PORT`; `MapActions.photo` e `LocalPhotoAction` (fotocamera su Android, scelta immagine con salvataggio immediato su Windows, `DesktopAppState.attachPhoto`). Pannello mappa: Foto accanto all'azione primaria, sezione Foto sempre visibile, identificativi, dati tecnici e collegamenti logici in Altri dettagli; scheda cavo con Foto e Inserisci passaggio. Scheda dispositivo Android: porte per prime, poi collocazione, foto e altri dettagli richiusi.
- **Verifica**: baseline verde prima delle modifiche; finale `gradlew test --rerun` `BUILD SUCCESSFUL in 34s`, 249 test superati (Core 90, Exchange 40, Desktop 95, Android JVM 24), test strumentali Android compilati. Nuovi test: inserimento passaggio e percorso completo, scollegamento, cestino senza orfani e ripristino, riepilogo porta nei due versi, rifiuto dei pacchetti 1.11, demo con giunzioni complete. Rendering Desktop del demo ispezionato (pannello, scheda porta collegata e libera, inserimento passaggio). Android nativo non eseguito: resta in RES-19.

## Ponte radio e demo comunale — 5 ottobre 2026

- **Ponte radio**: nuovo tipo e preset passante (LAN1 PoE fronte, RF1 retro), mezzo cavo `RADIO` e `LinkMedium.RADIO` (viola, tratti lunghi), icona dedicata. `PortTemplate` ha `rearPrefix`/`rearConnector`/`rearMedia` per un retro con nome e connettore propri. La tratta radio è un cavo tra le porte RF: il percorso resta completo tra le sedi.
- **Demo «Demo Comune»** (356 apparati, 7 rack, 1001 cavi):
  - Municipio con rack CED centro stella (fibra, core, firewall, router, ONT, server, NAS, UPS) e due piani;
  - Teatro con FTTH, VPN verso il Municipio e una scatola di giunzione;
  - Scuola media collegata in radio dal Municipio, con rilancio radio alla Scuola materna;
  - ogni piano con 2 patch panel da 48 cablati per 3/4 (72/96) verso 36 prese, due switch PoE da 48 e 16 apparati.
- **Telefono**: installato l'APK con database v1 e importato il demo precedente; salvata una copia grezza dei dati v14 in `build/phone-backup/` (fuori da Git).
- **Verifica**: `gradlew test --rerun` `BUILD SUCCESSFUL in 1m 16s`, 250 test superati (Core 90, Exchange 41, Desktop 95, Android JVM 24); `DemoSeedTest` controlla 3/4 delle porte cablate, i percorsi radio completi, dorsali, WAN e giunzione. Render Desktop di piano e copertura ispezionati.
- **Emulatore Pixel 9**: installato l'APK aggiornato e importato «Demo Comune» (356 apparati · 7 rack · 1001 cavi); mappa della copertura della Scuola media con i due ponti radio verificata.
- **Da completare sul telefono fisico**, che si è disconnesso:
  - eliminare le due cartelle foto orfane del 3 ottobre (`files/attachments`, presenti nella copia grezza);
  - installare l'APK aggiornato e importare «Demo Comune»;
  - chiedere all'utente se eliminare il vecchio «Demo OnlyField».

## Consegna per cambio sessione — 5 ottobre 2026

- Ramo `main`, commit e push su `origin/main` di tracciamento fisico, scheda rapida delle porte, foto primaria e ripartenza greenfield v1, con documentazione e tracker aggiornati.
- Verifica: 249 test superati, APK debug e portable Windows generati; demo rigenerato in `fixtures/demo/onlyfield-demo.ofam`.
- Seconda consegna dello stesso giorno: commit e push di ponte radio e «Demo Comune»; lavori sul telefono in sospeso (vedi sezione precedente).
- Ripartenza: sui telefoni di prova il database v1 si ricrea vuoto, quindi reimportare il demo; poi RES-19 (scheda rapida della porta su Android nativo, TalkBack, moto g86) e FOTO-04 in RES-13. Restano RES-17 e RES-01.

## UI/UX — 4 ottobre 2026

- Configuratori essenziali prima dei dettagli; gruppi di porte riepilogati e modificabili singolarmente, ricerca nei selettori, errori presso i campi e il salvataggio. Titoli operativi e azioni specifiche; sezioni richiuse conservano la bozza.
- Navigazione comune per gruppi; sidebar desktop 208 dp adattiva, scorciatoie numeriche conservate. Liste nominali ordinate solo nella presentazione, righe selezionate evidenti e azioni a capo.
- Editor della mappa allineati alla gerarchia degli altri editor. Nessuna modifica a database, contratto `.ofam` o regole del dominio; conservate le modifiche già presenti nel checkout.
- Baseline mirata: 7 test superati. Verifica finale: `BUILD SUCCESSFUL in 34s`; 214 test superati (Core 64, Exchange 37, Desktop 85, Android JVM 28). APK debug e modulo Desktop compilati; test strumentali Android compilati.
- Rendering Compose Desktop ispezionati a 1360×860 e 1024×768; catture in `pc/app/build/reports/ux/`. Verificati inserimento essenziale, modello, porte, gruppi collegati, scarto della bozza e localizzazione. Comando e perimetro in [docs/05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md).
- Android nativo con tastiera, scala testo e temi: non eseguito, RES-19. Il rendering condiviso a 360/412 dp non chiude questa verifica; RES-17 resta aperto.
- Pulizia degli scratch della revisione sotto `build/ux-audit/` bloccata dalla policy di esecuzione: due rifiuti `blocked by policy`, anche con percorso assoluto verificato e `Remove-Item -LiteralPath`. Nessun file eliminato; report e catture conservati. Rimozione da completare in RES-18.

## Consegna per cambio sessione — 4 ottobre 2026

- Ramo di consegna: `main`, verificato allineato a `origin/main` prima del salvataggio. Il commit raccoglie revisione UI/UX, manutenzione e consolidamento documentale già presenti nel checkout.
- Verifica disponibile: build Android/Desktop riuscite, 214 test superati e `git diff --check` senza errori. Nessuna modifica al codice dopo questa verifica.
- Tracker senza attività in corso. Ripartenza: checklist Android RES-19, suite UI RES-17, pulizia scratch RES-18; restano collaudo hardware RES-13 e CI su tag RES-01.

## Mappa, contenitori e configuratori — 4 ottobre 2026

- Scena della mappa nel core (`MapScene`, `HierarchyIndex`, `CableEnds`): calcolo unico per vista, ogni contenitore come un solo oggetto navigabile fino all'ultimo livello, una linea per coppia con elenco completo dei cavi, estremità fuori vista al bordo. Corretto il punto centrale di default che faceva convergere tutti i cavi al centro.
- UI della mappa condivisa in `shared/configurator` (`configurator.map`): rimossi i duplicati Android/Desktop di `FloorCanvas`, `ContainerBrowser` e catalogo. Pannello dettagli non modale e adattivo (840 dp), percorso di navigazione, inserimento tipologia → preset → dati essenziali, nome suggerito, pressione prolungata per posizionare. Simboli univoci, colori e forme per famiglia, mezzo distinto anche dal tratteggio.
- Configuratore: preset (switch, patch panel, prese, router/firewall, AP/telecamere, server, NAS, NVR, UPS/PDU); porte tipologia → quantità → etichetta; pannello porte libero/occupato con PoE, VLAN e selezione multipla; porta in tre schede; anteprima unificata nel core (`MapObjectDraft.preview`). La sessione fonde anche VLAN e subnet. Rack, mensola, armadio e cassetta sono contenitori predefiniti; non vengono più creati tipi duplicati.
- Nessuna modifica a contratto `.ofam` 1.11 o Room 14. Rimosse 11 chiavi di traduzione orfane, aggiunte le nuove in it/en/es.
- Verifica: `BUILD SUCCESSFUL`, 224 test superati (Core 75, Exchange 37, Desktop 84, Android JVM 28), APK debug e Desktop compilati, test strumentali compilati, `git diff --check` senza errori. Dettaglio in [docs/05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md). Verifica Android nativa ancora aperta: RES-19.


## Correzioni dalla revisione del codice — 4 ottobre 2026

- Selezione multipla delle porte: l'anteprima viene fissata all'inizio della selezione e la selezione si limita alle porte visibili; `PortLogic.setVlan/setPoe` ignorano porte inesistenti. Evitate righe VLAN/PoE orfane che bloccavano il salvataggio con errore strutturale.
- Gesti della mappa: presa conservata durante il trascinamento, nessun salvataggio con pressione prolungata ferma, nessun salto dopo lo zoom a due dita, limite di 12 punti senza effetti collaterali.
- Percorso di navigazione chiuso ai livelli spostati; "Assegna esistente" e `MapScene` senza scansioni ripetute; origine osservazione tradotta nell'anteprima.
- Accessibilità: elenco oggetti nel pannello (pulsante Elenco sotto 840 dp), legenda richiudibile, pannello porte compatto come unico elemento. Prefisso porte personalizzato selezionabile e confermato esplicitamente.
- Verifica: 225 test superati, APK debug, Desktop e test strumentali compilati. Verifica Android nativa ancora aperta: RES-19.

## Consegna per cambio sessione — 4 ottobre 2026 (sera)

- Ramo di consegna: `main`, allineato a `origin/main`. Commit di lavoro: `a6d5db9` (mappa, preset, pannello porte) e `0f01f28` (correzioni dalla revisione del codice).
- Verifica disponibile: 225 test superati, APK debug, Desktop e test strumentali compilati, `git diff --check` senza errori. Nessuna modifica al codice dopo questa verifica.
- Tracker senza attività in corso. Ripartenza: checklist Android RES-19 (inclusi zoom a due dita, TalkBack ed elenco oggetti), suite UI RES-17, pulizia scratch RES-18; restano collaudo hardware RES-13 e CI su tag RES-01.

## Pannello, inserimento e configuratore degli oggetti — 4 ottobre 2026

- Pannello dettagli della mappa (`MapDetailPane`): ordine fisso intestazione → azioni → stato → identificativi → collegamenti → allegati; una sola azione primaria, Rimuovi dal contenitore nel menu Altre azioni con messaggio annullabile; righe etichetta/valore da `core.display.ObjectSummary`; Assegna esistente come finestra con ricerca, tipo e posizione attuale; schede cavo con estremità A/B e Modifica cavo; elenco per famiglia con filtro.
- Inserimento: sottotitolo con posizione (Sul piano, In contenitore) e passo; pulsanti coerenti (Annulla; Continua/Indietro; Crea e usa); simboli colorati; Tipologia personalizzata sempre visibile. Nel configuratore il preset applicato è riassunto con Cambia preset.
- Configuratore condiviso: riga di contesto, Dati essenziali (con etichetta fisica), sezioni in ordine fisso con riepilogo da chiuse (Posizione, Porte, Identificativi e rete, Hardware, Note, sezioni dell'host, Campi personalizzati, Opzioni avanzate); avviso di cambio piano; riquadro per la modifica annidata; un solo percorso nella scheda porta con sezioni Destinazione, Percorso, Passaggio; contenuto rack compatto con U libere a intervalli. Primitive condivise: `GlyphBadge`, `PaneHeader`, `SectionTitle`, `FactRows`, `ActionRow`, `SelectField`.
- Testi: chiavi semantiche `map.*`, `picker.*`, `config.*` in it/en/es; suggerimento della mappa indipendente dal dispositivo; IP e MAC tradotti; simboli senza testo annunciati con descrizione. Rimosse 8 chiavi orfane. Nessuna modifica a contratto `.ofam` 1.11 o Room 14.
- Verifica: 234 test superati (Core 78, Exchange 37, Desktop 91, Android JVM 28), APK debug e test strumentali compilati, `git diff --check` senza errori. Dettaglio in [docs/05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md). Verifica Android nativa ancora aperta: RES-19.

## Prova su Android e consegna per cambio sessione — 4 ottobre 2026 (notte)

- Prova sul moto g86 5G (Android 16) in un progetto separato «Test rack fibra»: due rack, uno switch per rack con preset, fibra X1 ⟷ X1 tra gli switch; pannello, picker, scheda porta e scheda cavo conformi. Corretti: intestazioni di sezione tagliate dagli angoli arrotondati e dispositivi del rack senza posizione U assenti dall'elenco (ora gruppo Senza posizione U, coperto da `ConfiguratorLayoutTest`).
- Verifica: 234 test superati (Core 78, Exchange 37, Desktop 91, Android JVM 28), APK debug e test strumentali compilati, `git diff --check` senza errori.
- Tracciati nel tracker: F06 estremità remote dei cavi tra piani e BU, F07 collegamenti logici VPN/WAN nel pannello, F08 percorso che prosegue via VPN, F09 VPN sulla mappa (rimandato), F10 difetti emersi dalla prova. RES-19 passa a PARTIAL.
- Ripartenza: F06 e F07; ramo `main` allineato a `origin/main` dopo il push di questa consegna.

## Difetti F10 dalla prova Android — 4 ottobre 2026

- Nuovo oggetto senza punto scelto: posizione libera più vicina al centro (`ObjectMap.placeNew`), non più nell'angolo.
- Percorso porta: mezzo e tipo di mappatura tradotti (`toDisplayString`, `mappingTypeLabel` spostata da Desktop a `core.display`; nuovo tipo Interno).
- Plurali: `Messages.plural` con chiavi `.one` in it/en/es per cavi, dispositivi, oggetti, porte, rack e percorsi; corretto «{0}cables» in spagnolo.
- Inserimento: menu del preset a tutta riga e popup largo quanto il campo.
- Verifica: 237 test superati (Core 81, Exchange 37, Desktop 91, Android JVM 28), con 3 nuovi test core; app Desktop e Android compilate. Prova su telefono non eseguita: ricade in RES-19.

## F06 — Estremità remote dei cavi tra piani e BU — 4 ottobre 2026

- Moncone verso il bordo con etichetta dell'estremità remota (dispositivo, piano e BU se diversi); scheda cavo con Estremità remota, Dorsale e Vai a. Arrivo sull'altro piano con contenitori aperti e oggetto selezionato; Desktop e Android collegati tramite `MapActions.goTo`.
- Core: `RemoteEnd`, `RemoteEnds`, `SceneLink.remotes`, `MapScene.buId`, `Project.backbones`; `floorBusinessUnit` spostata in `ObjectMap`. Selezione della mappa azzerata a ogni cambio di livello e quando la vista non mostra più il collegamento scelto. Nessuna modifica a `.ofam` 1.11 o Room 14.
- Verifica: 239 test superati (Core 82, Exchange 37, Desktop 92, Android JVM 28), APK debug e test strumentali compilati, `git diff --check` senza errori. Prova su Android: aggiunta a RES-19.

## F07 — Collegamenti logici nel pannello del dispositivo — 4 ottobre 2026

- Sezione Collegamenti logici nel pannello della mappa per ogni dispositivo: tutte le `WanVpnConnection` di cui è estremità, con tipo, operatore, banda, verso e Vai a. Finestra di creazione e modifica (`LogicalLinkDialog`) basata su `WanForm`: VPN predefinita, dispositivo come estremità locale, campi nascosti conservati.
- Core: `LogicalLinks` (elenco, lato opposto, etichetta). Nessuna modifica a `.ofam` 1.11 o Room 14.
- Verifica: 240 test superati (Core 82, Exchange 37, Desktop 93, Android JVM 28), APK debug e test strumentali compilati, `git diff --check` senza errori. Prova su Android: aggiunta a RES-19. Emerso RES-20 (estremità WAN/VPN non validate).

## F08 — Percorso porta che prosegue via collegamento logico — 4 ottobre 2026

- `ConnectionGraph.trace` chiude un percorso fisico pulito con un passo logico per ogni `WanVpnConnection` del dispositivo finale (`ChainStep.logical`, testo `config.viaLogical`); nessun passo per conflitti, cicli, passaggi sconosciuti o porte non collegate. Scheda porta con il passo in corsivo e il dispositivo remoto apribile.
- Verifica: 241 test superati (Core 83, Exchange 37, Desktop 93, Android JVM 28), APK debug e test strumentali compilati, `git diff --check` senza errori. Prova su Android: aggiunta a RES-19.

## RES-20 e decisione F09 — 4 ottobre 2026

- RES-20 chiuso: `ModelValidator` segnala come avvisi documentali `WAN_VPN_WITHOUT_ENDPOINTS` (nessun apparato né sede) e `WAN_VPN_SAME_DEVICE` (estremità coincidenti); testi it/en/es, test del validatore.
- F09 chiuso senza implementazione: le VPN restano nel pannello (F07) e nel percorso porta (F08); la mappa disegna solo cavi fisici. Decisione registrata in [plan.md](plan.md).
- Verifica: 242 test superati (Core 84, Exchange 37, Desktop 93, Android JVM 28), APK debug compilato, `git diff --check` senza errori.

## RES-18 — Pulizia scratch — 4 ottobre 2026

- Verificato che nessuno script, workflow o documento usa gli scratch sotto `build/`. Rimossi 66 elementi (circa 11 GB): log, script e cataloghi monouso, `ux-audit/`, `ci-fixtures/`, strumenti locali (`wix311`, `actionlint`, `l10n-tools`, `translation-tools`, `translation-models`) e immagini emulatore `qa-avd`/`qa-system35` (API 35). Conservati `build/release/` (output di `prepare-release.ps1`), `build/reports/` e `build/tmp/`. Nessun file tracciato da Git coinvolto.

## RES-01 su tag e consegna per cambio sessione — 4 ottobre 2026

- Tag annotato `v1.0.0` su `dedf863` pubblicato su origin. Run [37219874814](https://github.com/dennidalpos/OnlyFieldAssetManager/actions/runs/37219874814) fallita nel passo «Verify all JVM and Compose suites»: Gradle non trova la toolchain `{languageVersion=21, vendor=JetBrains}` e il JDK scaricato da foojay non ha `javac`, `javadoc` e `jar`. Job publish saltato, nessuna release. In locale la stessa suite passa (242 test).
- Tracker: nessuna attività aperta; residui RES-01 (CI su tag), RES-13, RES-17, RES-19. Ramo `main` allineato a `origin/main`.

## Restyling UI/UX (UX-R1…R8) — 4 ottobre 2026

- **UX-R1**:
  - tema condiviso `OnlyFieldTheme` con forme squadrate;
  - pulsanti con angoli da 4 dp e margini interni ridotti (wrapper in `configurator.theme`).
- **UX-R2**:
  - icone disegnate dei tipi predefiniti (`ObjectIcons`, `Glyph.typeId`) su badge, mappa ed elenchi;
  - icone di navigazione spostate in `shared/configurator`; rimossi i drawable inutilizzati.
- **UX-R3**:
  - area della mappa con bordo e griglia visibili anche in tema scuro;
  - comandi senza scorrimento orizzontale: menu ⋮, livelli in un menu, miniature a capo.
- **UX-R4**: editor e pannelli aperti dall'inizio. Scorrimento azzerato su nuova selezione, editor annidati e pannelli PC; Porte richiamata solo al ritorno da una porta.
- **UX-R5**:
  - porte adattate alla larghezza, a capo in fasce bilanciate (`SchematicGeometry.portGrid`);
  - elevazione rack condivisa a tutta larghezza (`RackElevation`) con U libere toccabili.
- **UX-R6**:
  - inserimento rapido tipo → menu precompilati → Aggiungi (`ObjectPickerDialog`, `core.forms.QuickAdd`) da mappa, U libera, inventario e rack;
  - Aggiungi e modifica apre l'editor completo; i rack nascono vuoti.
- **UX-R7**:
  - solo dati compilati nel pannello mappa e nei dettagli Android;
  - editor più snello: niente testo guida, Modello solo se utile, Campi personalizzati e Opzioni avanzate in «Altro»;
  - rimosse le chiavi i18n inutilizzate.
- **UX-R8**:
  - Android: barra in basso Mappa · Dispositivi · Rack · Cablaggio · Altro, nascosta con editor aperti; Altro a gruppi con icone e azioni progetto; sottoschede senza scorrimento;
  - Windows: icone nella barra laterale e menu Operazioni progetto riordinato.
- **Verifica**: 247 test superati (Core 88, Exchange 37, Desktop 94, Android JVM 28). APK debug provato su emulatore Pixel 9, dove sono stati corretti i pulsanti segmentati e la fascia porte. Dettaglio in [docs/05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md). La prova sul moto g86 è aggiunta a RES-19.
- Consegna per cambio sessione: tracker senza attività aperte; residui RES-19 (prova restyling su moto g86), RES-01 (CI su tag), RES-17, RES-13. Ramo `main` allineato a `origin/main`.

## RES-01, RES-19 su emulatore e correzioni dalla prova — 5 ottobre 2026

- **RES-01**:
  - criteri del daemon JVM rigenerati senza vincolo di vendor (`gradle/gradle-daemon-jvm.properties`);
  - lingua dei test JVM fissata a it-IT, perché i runner CI sono en-US e i test verificano i testi italiani;
  - run manuale 37236462330 su `main` verde: build ok, publish saltato come previsto.
- **RES-19 su emulatore Pixel 9 (API 37)**:
  - esiti in [docs/testing/hardware-checklist.md](docs/testing/hardware-checklist.md);
  - corretti: etichette della barra in basso troncate, pulsanti dell'inserimento impilati, righe del rack ad altezza fissa.
- **Segnalazioni dell'utente**:
  - la mappa trascinata usciva dallo schermo: ora la panoramica si ferma ai bordi della pagina (`MapViewport.clamped`);
  - l'eliminazione era nascosta nel menu ⋮ o assente. Ora c'è un pulsante cestino rosso visibile:
    - Android: dettagli e schede, comprese le liste Dispositivi e Rack;
    - Windows: lista dispositivi;
    - pannello mappa: dispositivi e rack (Sposta nel cestino) e cavo scelto (Elimina).
  - Nuovi test: `ObjectMapTest.panKeepsThePageOnScreen` e `FloorMapUiTest.selectedObjectHasVisibleConfirmedTrash`. Suite core, Desktop e Android JVM verde; APK provato su emulatore.

## Progetto demo importabile — 5 ottobre 2026

- `fixtures/demo/onlyfield-demo.ofam`: 2 BU × 2 piani, 44 apparati, 4 rack, 109 cavi. Contiene switch, patch panel, prese a muro e AP. 6 switch su 8 sono cablati e collegati in fibra con piani diversi. Generato da `DemoSeed` con `:shared:exchange:demoPackage`; dettaglio in [docs/05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md).
- Verifica: `DemoSeedTest` (4 test). Import provato sull'emulatore Pixel 9 con mappa del piano ed elevazione del rack.

## RES-01 chiuso: release v1.0.1 — 5 ottobre 2026

- Su richiesta dell'utente nuovo tag `v1.0.1` (versioni allineate: Android 1.0.1/versionCode 2, portable 1.0.1); `v1.0.0` resta senza release.
- Run su tag 37238450362: build e publish verdi. Release con `OnlyFieldAssetManager-debug.apk`, `OnlyFieldAssetManager-portable-x64-1.0.1.zip` e `SHA256SUMS`.
- APK 1.0.1 installato sul moto g86 con il progetto demo importato. Corretta la finestra di importazione: senza copia locale ora indica «Nuovo progetto locale».

## EVO-01: gerarchia Sede → Piano — 5 ottobre 2026

- Dall'intervista sul censimento di rete (decisioni in [plan.md](plan.md)): la vecchia business unit è diventata la sede (`Site`) in codice, `.ofam` 1 (`sites`, `siteId`), Room 1 (tabella `sites`) e UI («Sede»/«Site»/«Sede» in it/en/es). Modifica incompatibile ammessa dal greenfield.
- Rimossi il livello intermedio `Site` (sede dentro la BU, senza editor), `Device.siteId`, `SiteEntity` con le relative colonne, l'ambito VLAN `SITE`, il filtro export per sede e la modifica massiva della sede. `ProjectMerger` non annida più sedi nelle BU.
- Nuovi campi facoltativi `Site.group` e `Site.address`, modificabili negli editor della sede su Windows e Android. Elenchi ordinati per gruppo e poi per nome (`sitesForDisplay`, `displayName`); colonne Sede e Gruppo nell'inventario XLSX.
- L'avviso `UNPOSITIONED_DEVICE` ora segnala un apparato senza piano né rack. I codici `*_BU` sono diventati `*_SITE`.
- Fixture `v1_sample_project.json` appiattito; demo «Demo Comune» rigenerato con i gruppi Sedi comunali e Scuole.
- Verifica: core 90, exchange 41, Desktop 95, Android JVM 24 test superati; test strumentali Android compilati.

## EVO-02: stato operativo dell'apparato — 5 ottobre 2026

- `Device.operationalStatus` (`OperationalStatus`: in servizio, spento, dismesso, da verificare; predefinito in servizio), distinto da `Observation`. Persistito in `.ofam` 1 e Room 1 (colonna `operationalStatus`).
- Configuratore: scelta in Dati essenziali. Pannello mappa: stato tra i dati primari quando non è In servizio; nodi spenti o dismessi attenuati (`SceneNode.inactive`).
- Dispositivi: filtro per stato su Windows; su Android il filtro compare quando esistono apparati non in servizio; lo stato compare nel riepilogo della riga.
- Export: colonna Stato operativo in XLSX e Markdown (la colonna del rilievo si chiama ora Rilievo); report PDF Desktop con lo stato se diverso da In servizio. Il PDF Android non è stato modificato.
- Demo rigenerato con un PC spento, un telefono dismesso e una telecamera da verificare.
- Verifica: core 91, exchange 41, Desktop 95, Android JVM 24 test superati; nuovi controlli `MapSceneTest.switchedOffDeviceIsFaded`, persistenza Room in `ProjectRepositoryTest`, etichette in `DisplayLabelsTest`.

## EVO-03: cablaggio in blocco — 5 ottobre 2026

- `core.forms.BulkCabling`: serie di porte libere sullo stesso apparato e lato in ordine tecnico (`freeRun`), coppie (`pairs`, `maxCount`), collegamento con etichetta proposta (`connect`) e porta libera successiva (`nextFree`).
- Scheda rapida della porta: dopo la scelta della porta di destinazione compaiono **Porte in serie** (1, 2, 4, 8, 12, 16, 24, 48 fino al massimo disponibile) con anteprima del primo e dell'ultimo accoppiamento, e l'opzione **Poi passa alla porta successiva**, che mantiene l'apparato di destinazione e propone la coppia seguente. Un patch panel da 24 porte si cabla in un'unica operazione annullabile.
- Verifica: core 93, exchange 41, Desktop 97, Android JVM 24 test superati; nuovi `BulkCablingTest` (porte occupate saltate, lato unico, etichette, porta successiva) e `QuickCablingUiTest` (serie da 4 e modalità continua).

## EVO-04: ricerca nel progetto e Vai a — 5 ottobre 2026

- `core.display.GlobalSearch`: indice per versione del progetto su apparati (nome, etichetta, alias, IP, MAC, seriale), rack, porte (`APPARATO PORTA` o `APPARATO/PORTA`, oppure etichetta) e cavi (etichetta o proposta); ordine esatto → iniziale → parziale; destinazione sulla mappa e posizione `Sede › Piano › Rack`; recenti per id.
- `configurator.map.GlobalSearchDialog`, condiviso: Android con l'icona Cerca nella barra della mappa, Windows con il pulsante Cerca e `Ctrl+F`. Il risultato apre il piano con l'oggetto selezionato; senza piano apre l'editor. Recenti di sessione (otto).
- Verifica: core 97, exchange 41, Desktop 99, Android JVM 24 test superati; nuovi `GlobalSearchTest` e `GlobalSearchUiTest`.

## EVO-05: foto più rapide — 5 ottobre 2026

- Scatto in serie: Android riapre la fotocamera sullo stesso oggetto dopo ogni foto confermata finché non si annulla (`rememberPhotoCapture`); Windows accetta più immagini in una scelta (`DesktopStorageHelper.pickOpenFiles`, `attachPhotos`).
- Scheda rapida della porta: dopo Collega invito e Foto cavo in evidenza; in modalità continua Foto cavo del collegamento precedente resta a portata; conteggio scatti su Foto porta e Foto cavo (`PortSummary.cablePhotos`).
- Foto mancanti: `core.forms.PhotoCoverage` (porta collegata documentata dalla foto della porta o del cavo), segno • nel disegno porte, conteggio e Apri la prima nel pannello della mappa.
- Verifica: core 98, exchange 41, Desktop 101, Android JVM 24 test superati, test strumentali Android compilati; nuovi `PhotoCoverageTest` e due casi in `QuickCablingUiTest`. Serie di scatti su fotocamera reale non provata (RES-13).

## EVO-06: schema del percorso — 5 ottobre 2026

- `core.forms.PathSchematics`: percorso fisico completo da una porta, attraverso i passaggi interni dei passanti in entrambe le direzioni, con lettura dall'estremo attivo, fuoco sull'oggetto interrogato e fini aperte; sostituisce le tratte di `PortSummary` (`PathHop`, rimosso).
- `configurator.PathSchematicView`: disegno verticale condiviso nella scheda rapida della porta e nella scheda Collegamento della pagina porta, al posto degli elenchi testuali.
- Verifica: core 100, exchange 44, Desktop 102, Android JVM 24 test superati, test strumentali Android compilati; nuovi `PathSchematicTest`, `DemoPathSchematicTest` (demo: AP del teatro attraverso presa, scatola di giunzione e patch panel; ponte radio Municipio → Scuola media) e `PathSchematicUiTest`. Resa controllata su immagine Desktop del demo; Android non verificato a vista (RES-19).

## EVO-07: topologia fisica — 5 ottobre 2026

- `core.forms.PhysicalTopology`: grafo degli apparati attivi da `PathSchematics` (passanti collassati, percorsi contati, mezzi, percorsi aperti per apparato), filtri sede/piano con estremi esterni, livelli in ampiezza per rete collegata, righe a capo secondo la larghezza, terminali raccolti sull'apparato di infrastruttura.
- `configurator.map.TopologyDialog`, condiviso: filtri che vanno a capo, interruttore Mostra i terminali, nodi toccabili con descrizione per lo screen reader; aperto da FloorHome su Windows e Android, il tocco porta all'apparato sulla mappa.
- Demo: 118 apparati attivi, 16 collegamenti tra apparati di infrastruttura con i terminali raccolti (115 con tutti i terminali); due reti (Comune con le scuole via ponte radio, Teatro con la propria FTTH).
- Verifica: core 100, exchange 47, Desktop 103, Android JVM 24 test superati, test strumentali Android compilati; nuovi `DemoTopologyTest` e `TopologyUiTest`. Resa controllata su immagini Desktop a 1360 e 412 dp; su Android non verificata a vista (RES-19).

## EVO-08: PDF Desktop completo — 5 ottobre 2026

- `pc.report.PdfReportWriter` con PDFBox sostituisce `SimplePdfWriter` (solo testo) e la stampa Java2D: carattere Unicode di sistema con ripiego Helvetica, tabelle con intestazione ripetuta, piè di pagina, stampa dallo stesso documento con `PDFPageable`.
- Disegni: planimetrie con sfondo, oggetti e cavi (`MapScene`); elevazioni rack fronte/retro; tabella dei percorsi (`PathSchematics.all`, nuovo); topologia fisica. Nuove voci `ReportSelection`: planimetrie, percorsi, topologia.
- Verifica: core 100, exchange 47, Desktop 104, Android JVM 24 test superati, test strumentali Android compilati; nuovo `DeliveryPdfTest` (PDF del demo: tutte le sezioni disegnate, nessuna credenziale, pagine renderizzate in `pc/app/build/reports/delivery`); `ReportPdfTest` e `DesktopDocumentAndCartographyTest` leggono il testo con PDFBox. Il PDF del demo ha 80 pagine, la maggior parte per l'elenco dei 600 cavi. PDF Android invariato.

## EVO-09: Excel con percorsi end-to-end — 5 ottobre 2026

- `XlsxExportManager`: nuovo foglio Percorsi (estremi con porta e ubicazione, passanti, etichette, mezzi, lunghezza totale, stato) da `PathSchematics.all`, filtrato sugli apparati esportati; tutti i fogli con intestazione in grassetto, prima riga bloccata e larghezza predefinita 20.
- Verifica: core 100, exchange 48, Desktop 104, Android JVM 24 test superati, test strumentali Android compilati; nuovo `DemoXlsxPathsTest` (parti XML ben formate, foglio registrato, una riga per percorso). Il file del demo si apre in Excel (Microsoft 365, sola lettura via COM) senza riparazioni: Percorsi 457 righe, intestazione in grassetto e bloccata.
