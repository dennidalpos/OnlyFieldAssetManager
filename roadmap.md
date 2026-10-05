# Evidenze e residui

Aggiornato al 5 ottobre 2026 dopo confronto con il codice e il tracker.

## Evidenze disponibili

- I moduli Gradle sono `:shared:core`, `:shared:exchange`, `:mobile:app` e `:pc:app`.
- Il codice implementa `.ofam` 1 e Room 2 (ripartenza greenfield del 5 ottobre 2026), storage desktop atomico, database Android cifrato, import/export, documenti, mappe e configuratore condiviso.
- La suite locale registrata prima di questa revisione copre core, exchange, Android JVM e Desktop; le prove strumentali e manuali hanno limiti espliciti sotto.
- Il workflow GitHub esegue le suite JVM/Compose, genera APK debug e ZIP portable, calcola SHA-256 e pubblica soltanto su tag `v*`.

## Residui aperti

| ID | Stato | Evidenza richiesta |
| --- | --- | --- |
| RES-13 | Aperto | Checklist foto, scansione, multitouch e lettore USB su hardware reale. |
| RES-20 | Aperto | Prova manuale Esporta e apri Windows con programmi esterni. |
| RES-22 | Aperto | Memoria e tempi degli import grandi su hardware. |
| RES-23 | Aperto | Pannello occupato Windows, focus/input e stampa nativa. |
| RES-19 | Parziale | Emulatore eseguito (UX-01, 02, 03, 05, 06); restano TalkBack, combinazioni mancanti, conferma import con molti avvisi e moto g86. |

Il dettaglio operativo e il criterio di chiusura sono in [PROJECT_STATUS.json](PROJECT_STATUS.json). La checklist hardware e in [docs/testing/hardware-checklist.md](docs/testing/hardware-checklist.md).

## RES-21: PDF Android completo e paginato — 5 ottobre 2026

- Sostituito il generatore troncato con un solo writer paginato per report e scheda rack. Inventario e nomi completi, liste rack senza interruzione, fronte/retro adattati alla pagina, porte/cavi, rete, alimentazione/badge, note e tutti gli allegati selezionati. Note/allegati generati anche senza inventario. Filtri comuni e credenziali escluse; disegni planimetrici/topologia/percorsi restano le opzioni Desktop già documentate.
- Baseline mirata JVM `RepositoryDispatchTest`: `BUILD SUCCESSFUL in 3s`, 3 test. Quattro nuove prove native hanno riprodotto i troncamenti e le sezioni mancanti. Dopo la correzione, `ANDROID_SERIAL=emulator-5554` e `:mobile:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.onlyfield.assetmanager.CompositePdfTest --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 13s`, 4 test superati. `LocalizedPdfTest` separato superato nelle tre lingue.
- Sette PDF nativi e 46 pagine ispezionate: report completo 21 pagine, note/allegati 6, rack 3. Nessun apparato/allegato perso; righe e margini verificati. Evidenze rigenerabili nei test e output ignorati `mobile/app/build/reports/pdf-native/`.
- RES-21 rimosso dal tracker, documentazione export e verifica aggiornata. Stampa fisica/annullamento rimangono in RES-19. Nessun cambiamento al database in questa correzione.

## Correzioni dell'audit — 5 ottobre 2026

Il [rapporto critico](docs/audit-2026-10-05.md) registra nove rilievi: tre P1 e sei P2, tutti corretti e verificati. I collaudi operativi ancora aperti sono in [PROJECT_STATUS.json](PROJECT_STATUS.json).

| ID | Priorità | Stato | Rilievo | Verifica disponibile |
| --- | --- | --- | --- | --- |
| AUD-01 | P1 | Chiuso | Verificatore inizializzato nell'import; fusione conserva protezione locale. | 6 regressioni Android JVM superate. |
| AUD-02 | P1 | Chiuso | Eliminato il fallback a file locali esterni. | 2 regressioni Android JVM superate. |
| AUD-03 | P1 | Chiuso | Room v2 e fallback generale unico. | SQLCipher nativo API 37 e moto g86 API 36; demo reimportato. |
| AUD-04 | P2 | Chiuso | Avvisi per payload assenti prima della conferma. | 5 regressioni Exchange e 2 Desktop superate. |
| AUD-05 | P2 | Chiuso | Tutti i media locali nel pacchetto cifrato, visualizzazione in memoria. | 5 regressioni e suite Desktop superate. |
| AUD-06 | P2 | Chiuso | Lettura limitata e metadati KDF validati. | 6 regressioni e suite dei tre moduli superate. |
| AUD-07 | P2 | Chiuso | Worker Android e Windows; elenco dal manifest. | 8 regressioni e 320 test completi superati. |
| AUD-08 | P2 | Chiuso | Selezione comune e contesto esterno limitato ai percorsi selezionati. | 4 regressioni e 302 test completi superati. |
| AUD-09 | P2 | Chiuso | Password locale distinta; recupero cestino esplicito. | 4 regressioni e 118 test Desktop superati. |

Baseline eseguita con directory di build isolate dall'altra attività: `BUILD SUCCESSFUL in 1m 21s`, 278 test esistenti superati (Core 100, Exchange 48, Desktop 106, Android JVM 24), nessun errore né test saltato. Otto prove temporanee aggiuntive hanno riprodotto sette difetti; sono fallite asserendo il comportamento corretto e non sono conteggiate come test superati. Nell'audit iniziale non sono state applicate correzioni dell'app; le correzioni successive sono documentate sotto. Comandi, limiti e fonti ufficiali sono nel rapporto.

## Tracciamento fisico, scheda rapida porte e foto — 5 ottobre 2026

- **Greenfield v1**: `.ofam` riparte dalla versione "1" (rifiutati 1.7–1.11) e Room dalla versione 1, senza migrazioni né backup pre-aggiornamento; una versione diversa viene ricreata vuota (`fallbackToDestructiveMigration`, anche in downgrade). Un'identità diversa mantenendo v1 non attiva il fallback: difetto AUD-03, riprodotto nell'audit. Rimossi schemi 13/14, migrazioni, conversione da database in chiaro e relativi test. I telefoni di prova vanno reimportati dal demo.
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
- Ripartenza aggiornata dall'audit: risolvere AUD-03 e verificare la procedura di aggiornamento del database sui telefoni di prova prima di reimportare il demo; poi RES-19 (scheda rapida della porta su Android nativo, TalkBack, moto g86) e FOTO-04 in RES-13. Restano RES-17 e RES-01.

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

## EVO-10: moduli secondari sintetici — 5 ottobre 2026

- `configurator.SecondaryModule` (Credenziali, Configurazioni apparati, Badge documentali): visibili solo se usati; voce Altri moduli con l'elenco dei moduli nascosti (barra laterale e menu Sezioni su Windows, Altro su Android) e stato di sessione `showSecondary`; schede Configurazioni (Rete) e Badge (Alimentazione) filtrate con `visibleTabs`. Modello, export e `Ctrl+5` invariati; la videosorveglianza resta solo nel modello.
- Verifica: core 100, exchange 48, Desktop 106, Android JVM 24 test superati, test strumentali Android compilati; nuovo `SecondaryModulesUiTest` (moduli assenti dalla navigazione su un progetto vuoto, raggiungibili da Altri moduli; un modulo usato resta visibile). Android non verificato a vista (RES-19).

## AUD-01: protezione degli import Android — 5 ottobre 2026

- Creazione e sostituzione inizializzano il verificatore PBKDF2 con la password incoming nella transazione del progetto; sostituzione non protetta azzera il verificatore. La fusione conserva la protezione locale. Password pending privata, rilasciata alla conferma, annullamento e fusione.
- Baseline Android JVM: `:mobile:app:testDebugUnitTest --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 1m 7s`. Sei regressioni in `ImportedProtectionTest` (database su file chiuso e riaperto): password corretta/errata, modifica, sostituzione, rimozione della protezione, password assente senza perdita dei dati, fusione protetta/non protetta. Comando `:mobile:app:testDebugUnitTest --tests '*ImportedProtectionTest' --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 15s`, 6 test, 0 fallimenti/errori/saltati.

## AUD-02: confinamento degli allegati Android — 5 ottobre 2026

- Eliminato il fallback a percorsi locali derivati dal JSON; lettura solo dal percorso del progetto. Gli alias di pacchetto restano supportati durante l'estrazione dei payload presenti.
- `:mobile:app:testDebugUnitTest --tests '*AttachmentConfinementTest' --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 8s`, 2 test, nessun fallimento. Quattro percorsi esterni/relativi/traversal non leggono né esportano il file fittizio; un payload legittimo con alias legacy viene estratto e riesportato.

## AUD-04: allegati mancanti prima della conferma — 5 ottobre 2026

- Serializer comune: confronto catalogo/payload dopo la decifratura e confronto con le entry dichiarate nei checksum, con avvisi documentali per ogni file mancante. Riferimenti conservati, alias legacy supportati, payload presenti disponibili. Avvisi localizzati it/en/es visibili in elenchi scorrevoli nella conferma Android e Desktop; su Desktop anche il nuovo progetto e il pacchetto identico richiedono conferma se incompleti.
- Baseline Exchange/Desktop: `:shared:exchange:test :pc:app:test --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 1m 18s`; avvisi Kotlin preesistenti, nessun test fallito.
- `:shared:exchange:test --tests '*MissingPayloadTest' :pc:app:test --tests '*ImportPayloadReviewTest' --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 8s`, 5 test Exchange e 2 Desktop superati, incluso avviso visibile prima della conferma. Verifica visiva Android aggiunta a RES-19.
- AUD-03: cambio schema non applicato; richiesta decisione su Room v2 con ricreazione dei dati di prova oppure conservazione automatica. Corrette le indicazioni che promettevano ricreazione per identità diversa a versione invariata.

## Verifica delle correzioni audit — 5 ottobre 2026

- `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 1m 20s`. Core 100, Exchange 53, Desktop 108, Android JVM 32: **293 test, zero fallimenti, errori o saltati** (15 regressioni aggiunte).
- Tracker: rimossi AUD-01, AUD-02 e AUD-04; restano AUD-03 e AUD-05…AUD-09. RES-19 include la verifica visiva della conferma import Android con molti avvisi. Database SQLCipher su dispositivo e collaudo hardware non eseguiti.

## AUD-05: media locali Windows protetti — 5 ottobre 2026

- Decisione esplicita: la password comprende tutti i media locali. Persistenza nel pacchetto cifrato esistente, cache dei payload sbloccati in memoria, rendering immagini/PDF da byte array senza file temporanei in chiaro. Le vecchie copie vengono adeguate al primo sblocco corretto; le sorgenti e gli export espliciti restano nelle destinazioni utente. Esporta e apri richiede la destinazione di una copia in chiaro.
- `:pc:app:test --tests '*ProtectedMediaTest' --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 6s`, 5 test superati. Suite Desktop completa `:pc:app:test --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 29s`, 113 test senza fallimenti/errori/saltati.
- RES-20 registra la prova manuale con i programmi esterni; nessun collaudo di viewer esterni simulato.

## AUD-08: filtri documentali comuni — 5 ottobre 2026

- `exchange.DocumentSelection`: sede, piano ereditato, categoria, cataloghi associati e classificazione applicati nei generatori Markdown/XLSX/PDF. PDF e stampa Desktop usano il progetto selezionato anche per planimetrie e rack. Percorsi completi e topologia includono solo il contesto esterno necessario ai percorsi degli apparati selezionati.
- Quattro regressioni permanenti (`DocumentSelectionTest`, `FilteredReportTest`); corretta anche la rete di una sede senza apparati della categoria selezionata. Verifica completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 1m 23s`; 302 test (100/56/114/32), nessun fallimento, errore o test saltato.
- AUD-08 rimosso dal tracker. RES-21 registra troncamento e sezioni incomplete del PDF Android individuati nel codice; collaudo PDF nativo Android non eseguito.

## AUD-06: limiti comuni di import — 5 ottobre 2026

- Limiti confermati dall’utente: ZIP 256 MiB, file in chiaro 32 MiB, totale decompresso 512 MiB, 10.000 entry, PBKDF2 massimo 1.000.000. Lettura ZIP diretta da stream, conteggio reale e rifiuto durante la lettura; niente copia preventiva dell’intero input compresso. Validazione di salt/IV/costo prima della KDF. Export e salvataggi rispettano gli stessi limiti.
- `:shared:exchange:test --tests '*PackageImportLimitsTest' :pc:app:test --tests '*ProtectedMediaTest' :mobile:app:testDebugUnitTest --tests '*ImportedProtectionTest' --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 25s`. Suite dei tre moduli `:shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 1m 22s`, 208 test (62/114/32), nessun fallimento, errore o test saltato.
- AUD-06 rimosso dal tracker; RES-22 registra le misure di memoria/tempi su hardware ai limiti.

## AUD-09: sostituzione con password diverse — 5 ottobre 2026

- Richiesta della password della copia locale chiusa solo quando quella incoming non la apre. Recupero di cestino e payload prima della sostituzione; progetto attivo usa lo stato in memoria. Password errata, annullamento, cestino corrotto e fallimento della scrittura preservano la vecchia copia. La nuova usa protezione e password incoming.
- `:pc:app:test --tests '*ReplacementPasswordTest' --tests '*ProtectedMediaTest' --tests '*ProtectedTrashTest' --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 15s`. Suite completa Desktop: `BUILD SUCCESSFUL in 32s`, 118 test superati; AUD-09 rimosso dal tracker.

## AUD-07: worker e interfaccia responsiva — 5 ottobre 2026

- Android: `PackageExchange`/`DocumentExports` con dispatcher I/O iniettabile; stream ContentResolver sul worker; cancellazione propagata nei flussi modificati. `ProjectPrintDocumentAdapter` genera sul worker, notifica sul Main e termina con `onFinish`.
- Desktop: elenco dal solo manifest (anche cifrato), errori di lettura visibili. `DesktopIo` usa worker seriale e loop secondario AWT per conservare i risultati sincroni mantenendo gli eventi attivi; stato occupato, modifiche bloccate, overlay e menu disabilitati. I/O, ZIP/KDF, validazione, media, fusione e documenti sul worker; nessuna coda di modifiche concorrenti. Errore della stampa distinto da annullamento.
- Otto regressioni permanenti: tre Android, due metadati Windows, tre worker AWT (incluso progetto protetto da 500 apparati e scrittura atomica bloccata). Targeted: `BUILD SUCCESSFUL in 4s` e `BUILD SUCCESSFUL in 8s`. Suite finale completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 1m 26s`; 320 test (100/62/123/35), zero fallimenti/errori/saltati.
- AUD-07 rimosso; RES-23 registra il collaudo visivo Windows e stampa nativa, RES-19 include la stampa Android. AUD-03 resta aperto: decisione richiesta, nessuna modifica del database. ADB conferma il moto g86 collegato e autorizzato; nessun aggiornamento APK o cancellazione sul telefono in questa sessione.

## Revisione finale delle correzioni audit — 5 ottobre 2026

- Eliminati filtri documentali duplicati nei generatori; selezione comune invariata. Fusione Android sul dispatcher Default con cancellazione propagata.
- Riprodotto e corretto un nome ZIP con UTF-8 malformato: rifiuto strutturale `INVALID_ZIP_ARCHIVE`, settima regressione `PackageImportLimitsTest` superata (`BUILD SUCCESSFUL in 3s`).
- Suite completa `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 1m 24s`, **321 test** (100/63/123/35), zero fallimenti/errori/saltati. Diff senza errori di spaziatura; 15 documenti UTF-8 senza BOM e 72 collegamenti locali validi. Nessun commit o push; modifiche preesistenti al demo preservate.
- Stato finale: solo AUD-03 nel tracker, con decisione richiesta sulla conservazione dei dati; sette residui tracciati. Nessun collaudo hardware/manuale dichiarato completo.

## Passaggio di sessione — 5 ottobre 2026

- L'utente ha richiesto salvataggio, commit e push sul branch principale; branch verificato `main`, upstream e default remoto `origin/main`. Il checkpoint include anche le modifiche preesistenti al demo (identificativo del progetto stabile e fixture aggiornata).
- Tracker aggiornato con le decisioni confermate, la scelta ancora necessaria per AUD-03 e le verifiche da riprendere prima di intervenire sul telefono. Restano AUD-03 e sette residui; schema e dati del telefono invariati.
- Ultima verifica del codice: suite completa `BUILD SUCCESSFUL in 1m 24s`, 321 test senza fallimenti/errori/saltati, come documentato sopra. Per il checkpoint sono stati ricontrollati i report e `git diff --check`; nessuna ulteriore modifica al codice.

## Configuratore visivo e flussi essenziali — 5 ottobre 2026

- Inserimento uniforme da mappa, inventario e rack: tipo, disegno/preset/nome, Aggiungi. Disegno in apertura, elevazione rack interattiva, riquadri e menu aperti evidenziati. Comandi principali raccolti e persistenti; eliminazione distinta con conferma. IP/MAC aperti per i tipi pertinenti, identificativi e note richiudibili; dimensioni e profondità rimosse dalla UI conservando i valori esistenti.
- Registro condiviso di colori per tipo, icona e nome in mappa/topologia/schede; selezione con doppio bordo, alone, nome completo e nodo sopra gli altri. Canvas ritagliato e panoramica ricondotta ai limiti anche dopo il ridimensionamento.
- Disposizione una/due righe per gruppo/lato, scambio con due tocchi o trascinamento; supporto PoE selezionabile nel disegno e distinto dalla configurazione. Metadati facoltativi nel JSON hardware e nei modelli, senza colonne Room nuove; ID, collegamenti, VLAN e campi nascosti conservati. Salva/Annulla gestiscono la stessa bozza e sessione annullabile.
- Tratte fisse dai frontali ai retro univocamente associati, anche con frontali occupati; anteprima degli estremi fisici, conflitti e passaggi sconosciuti bloccanti, serie e continuazione sui retro liberi. Cavetti frontali distinti. Verificato anche il caso di due frontali associati allo stesso retro.
- Baseline mirata: 40 test, `BUILD SUCCESSFUL in 2s`. Le prove iniziali hanno rilevato comandi fuori dal corpo visibile e aspettative della vecchia inserzione automatica: corretti i comandi e aggiornate le verifiche al flusso approvato. Suite ampliate dopo i problemi.
- Verifica completa principale: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:compileKotlin :mobile:app:compileDebugKotlin --no-parallel --max-workers=1` → `BUILD SUCCESSFUL in 1m 35s`. **339 test**: Core 108, Exchange 64, Desktop 132, Android JVM 35; zero fallimenti, errori o test saltati. Include 18 nuove regressioni per identità/disposizione, PoE personalizzato e modello, compatibilità JSON/scambio `.ofam`, retro/conflitti/serie, Salva/Annulla e resa visiva.
- Controllo conclusivo: corretta la preselezione implicita della prima sede dall’inventario quando manca il contesto e le sedi sono più d’una. Nuova regressione sul flusso reale `inventoryDeviceDraft`; mappa e rack conservano la sede contestuale, la sede unica non richiede un menu. `:pc:app:test :mobile:app:compileDebugKotlin --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 36s`, Desktop 133 test. Totale delle suite verificate: **340 test** (108/64/133/35), 19 nuove regressioni, zero fallimenti/errori/saltati.
- Render Compose Desktop ispezionati: 24 porte/360 dp/chiaro, 48 porte/360 dp/scuro, patch panel fronte/retro 24/560 dp/chiaro e 48/1024 dp/scuro. File sotto `pc/app/build/reports/visual-configurator/`; editor e picker a 1024×768 e 1360×860 sotto `pc/app/build/reports/ux/`. Mappa densa di 53 oggetti: zoom/trascinamento e controllo pixel sul pannello esterno (`dense-map-zoom-pan.png`).
- Fonti ufficiali: [accessibilità Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [gesti multitouch Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch). Flussi, configuratore, mappa e contratto aggiornati. Le vecchie applicazioni possono perdere i metadati nuovi risalvando un pacchetto.
- Limiti: scambio verificato sul serializzatore comune; nuovi flussi, TalkBack, gesto PoE/riordino e multitouch non collaudati su Android nativo o telefono reale (RES-13/RES-19). AUD-03 resta separato e aperto; nessuna modifica dello schema o installazione sul telefono. Nessun commit o push.

Controllo conclusivo delle immagini: `:pc:app:test --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 35s`, 133 test superati. Topologia aggiornata e retro con cavo attestato ispezionati nei PNG `visual-configurator/topology-current.png` e `visual-configurator/rear-560-false.png`. La cattura del dialogo dopo un’interazione non era affidabile nel renderer dei test; per la verifica grafica del retro cablato è stato usato il render isolato delle griglie. Le verifiche funzionali del dialogo restano superate.

## Checkpoint del configuratore per cambio sessione — 5 ottobre 2026

- Commit e push richiesti dall’utente sul branch principale; verificati `main` e default remoto `origin/main`. Il checkpoint comprende sorgenti condivisi, integrazioni Android/Windows, regressioni, documentazione e tracker.
- `PROJECT_STATUS.json` aggiornato per la ripresa: configuratore implementato, ultima verifica 340 test senza fallimenti/errori/saltati e compilazione delle due app riuscita. Nessuna ulteriore modifica al codice dopo le verifiche; JSON, codifica UTF-8, collegamenti e diff ricontrollati prima del commit.
- Ripartire dalla decisione AUD-03 prima di aggiornare il telefono; collaudare poi i nuovi flussi Android in RES-13/RES-19. Restano AUD-03 e sette residui, senza chiusure hardware/manuali implicite. Nessuna modifica dello schema Room né installazione APK eseguita.
- I render restano negli output ignorati `pc/app/build/reports/visual-configurator/` e `pc/app/build/reports/ux/`; si rigenerano con la suite Desktop indicata sopra. I sorgenti dei test sono inclusi nel checkpoint.

## AUD-03: Room v2 e aggiornamento SQLCipher — 5 ottobre 2026

- Decisione esplicita: ricreare i dati di prova e reimportare il demo, senza migrazione automatica. Versione Room incrementata a 2; `.ofam` resta 1. Schema v2 generato da KSP, schema storico e fixture v1 conservati per la regressione.
- La baseline nativa ha riprodotto `Room cannot verify the data integrity` con lo schema v1 del commit `f17d8a7`. Il solo incremento ha poi esposto `A migration from 1 to 2 was required but not found`: il fallback per solo downgrade riattivava l'obbligo di migrazione in upgrade. Eliminata la chiamata ridondante; il fallback generale copre entrambi i versi.
- Emulatore Pixel 9 API 37: `:mobile:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.onlyfield.assetmanager.EncryptedSchemaUpgradeTest --no-parallel --max-workers=1`, `ANDROID_SERIAL=emulator-5554`, `BUILD SUCCESSFUL in 18s`, 3 test superati (upgrade con schema storico/reimport/riapertura, versione invariata, downgrade 14 → 2). Aggiunta poi la prova di import su disco con tutti i payload.
- Moto g86 API 36: APK e APK test installati con `adb -s ZY32LNCB8C install -r`. `adb ... shell am instrument -w -r -e class com.onlyfield.assetmanager.EncryptedSchemaUpgradeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner`: **4 test superati** con SQLCipher/Keystore reali e database temporanei isolati.
- Prima dell'installazione: backup grezzo completo `build/phone-backup/ofam-phone-before-room-v2-2026-10-05.tar`, 8 file verificati leggendo integralmente l'archivio, 10.895.872 byte; SHA-256 `c62fc20ebda6e438f175857c769c5600b7fc9da06b6f82ce6fa39d2794c6c418`. Include database/WAL/SHM, chiave avvolta, media e preferenze; dipende dal Keystore del telefono, non è un export portabile. Nessuna foto orfana eliminata.
- Fixture copiata in Download e reimportata tramite il repository con l'argomento esplicito `-e seedApplicationDemo true` sul solo metodo `EncryptedSchemaUpgradeTest#demoPackageIsImportedOnDisk`: **1 test superato**. Il metodo richiede database reale vuoto, non sostituisce progetti esistenti; importato «Demo Comune», 356 apparati, tutti i media presenti e riapertura riuscita. Questo verifica storage/import, non il dialogo SAF.
- AUD-03 rimosso dal tracker; contratto, storage, piano, audit, regole e verifica aggiornati. Nessuna migrazione né bypass del controllo di identità.

## RES-17: Espresso e suite Android nativa — 5 ottobre 2026

- Riprodotto sui due test UI il blocco `NoSuchMethodException: android.hardware.input.InputManager.getInstance []`. Espresso 3.7.0 contiene la correzione ufficiale che usa `getSystemService`; aggiornata la sola dipendenza esistente, senza cambiare gli altri componenti o indebolire i test.
- `ANDROID_SERIAL=emulator-5554` e `.\gradlew.bat :mobile:app:connectedDebugAndroidTest --no-parallel --max-workers=1`: verifica conclusiva `BUILD SUCCESSFUL in 23s`, **13 test** (PDF, SQLCipher, renderer planimetrie, localizzazione UI e gesti), zero fallimenti/errori/saltati. Report in `mobile/app/build/reports/androidTests/connected/debug/` e copia in `mobile/app/build/reports/api37-verification/`. La fixture storica ricrea anche gli indici; corretta la lettura delle tabelle senza indici dopo un fallimento nell'ultimo controllo.
- Sul moto g86 API 36 aggiornato l'APK test, eseguita l'intera suite via `adb ... shell am instrument -w -r`: verifica conclusiva **13 test superati** in 8,947 s, senza il parametro di reimport. Database applicativo «Demo Comune» conservato; avviata MainActivity dopo il collaudo. Nessun collaudo TalkBack, foto, scanner, USB o stampa fisica dichiarato completo.
- Regressioni JVM finali `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`: `BUILD SUCCESSFUL in 26s`, **340 test** (108/64/133/35), zero fallimenti/errori/saltati.
- RES-17 rimosso dal tracker, documentazione e checklist aggiornate. Restano cinque residui: RES-13, RES-19, RES-20, RES-22, RES-23. Fonte ufficiale: [AndroidX Test/Espresso 3.7.0](https://developer.android.com/jetpack/androidx/releases/test#espresso_3.7.0).

Controllo conclusivo: 16 documenti UTF-8 senza BOM e 78 collegamenti locali validi; tracker e schema v2 validati. Corretto il riferimento obsoleto a RES-01 nel documento di rilascio dopo verifica della run e degli asset su GitHub. La pulizia delle risorse sul telefono, la chiusura dell'emulatore e la rimozione della directory temporanea locale sono state bloccate dal controllo automatico (`blocked by policy`, senza ulteriore motivazione); RES-24 registra `build/task-verification`, APK test, PDF, screenshot ed emulatore ancora presenti. Dopo due comandi bloccati la pulizia è sospesa; il blocco sembra riguardare le operazioni di rimozione, ma il motivo preciso non è esposto. App principale, Demo Comune e backup conservati. Nessun commit o push.

## Checkpoint Room v2 e PDF Android per cambio sessione — 5 ottobre 2026

- Salvataggio, commit e push sul branch principale richiesti dall'utente; verificati `main` e default remoto `origin/main`. Il checkpoint comprende Room v2, PDF Android completo, Espresso 3.7.0, regressioni native, documentazione e tracker.
- Nessuna modifica ai sorgenti dopo le verifiche finali: 340 test JVM verdi, 13 test nativi su emulatore API 37 e 13 sul moto g86 API 36. Per il checkpoint ricontrollati report, JSON, codifica, collegamenti locali e diff; nessuna suite ripetuta per sole modifiche documentali.
- Ripresa: prima RES-24 (pulizia bloccata dal controllo automatico), poi i cinque collaudi RES-13/19/20/22/23. Nessuna chiusura hardware o UX implicita. Backup ed evidenze nei percorsi locali ignorati indicati sopra: non vengono inclusi nel commit e non saranno disponibili in un nuovo checkout.

## Ripresa RES-24: pulizia delle prove — 5 ottobre 2026

- Copiate 19 evidenze locali in `build/reports/session-2026-10-05-cleanup/previous-verification`, con confronto SHA-256; rimossi i 17 file non bloccati e la sottocartella `pdf`.
- Moto g86: otto prove conservate in `build/reports/session-2026-10-05-cleanup/phone`, quindi rimossi solo i sette PDF elencati e lo screenshot. Disinstallato solo `com.onlyfield.assetmanager.test` (`Success`); verifica: pacchetto principale presente, cartella esterna PDF vuota, backup con SHA-256 invariato.
- RES-24 parziale: `emulator.log` ed `emulator-error.log` sono ancora aperti dall'emulatore della sessione precedente. L'errore iniziale del controllo hash standard era `The process cannot access the file ... emulator-error.log because it is being used by another process`; confronto completato con lettura condivisa. Il processo non è terminato perché non avviato dalla sessione corrente. Restano solo i due log e l'emulatore, senza nuove cancellazioni da autorizzare sul telefono.

## RES-22: misure e correzione autorizzata AUD-10 — 5 ottobre 2026

- Cinque fixture valide misurate su JVM Windows con heap 2 GiB. Moto g86 API 36, heap 256 MiB: 32 MiB in chiaro/cifrati e 10.000 entry superati; 511 MiB decompressi falliscono in `ByteArrayOutputStream.toByteArray` (`OutOfMemoryError`). Nessuna scrittura nel progetto applicativo durante il benchmark.
- L'utente ha autorizzato lettura progressiva e deposito temporaneo cifrato senza ridurre i limiti approvati né cambiare `.ofam`. AUD-10 tracciato durante la correzione. Prima prova corretta: SQLCipher isolato, 511 MiB importati e persistiti in 5.131 ms, Main massimo 18 ms; heap campionato 237.110.552 byte. Successiva riduzione delle copie, con misure conclusive da registrare.
- Cinque regressioni `StagedPayloadTest` e verifiche mirate storage/password/worker verdi (`BUILD SUCCESSFUL in 17s`). Baseline e metodologia nel [report](docs/testing/import-benchmark-2026-10-05.md); evidenze ignorate in `build/reports/import-benchmark`.
- Su richiesta dell'utente si prosegue solo con prove autonome. Foto, scanner fisico, USB HID e ascolto TalkBack rimangono non eseguiti: RES-13 e matrice RES-19 conservati.

## Chiusura AUD-10 e RES-22 — 5 ottobre 2026

- Risolto l’OOM da 511 MiB sul moto g86, mantenendo ZIP 256 MiB, file 32 MiB, totale 512 MiB, 10.000 entry e KDF massimo. Import/estrazione/export progressivi; payload grandi in staging cifrato con chiave effimera. Pacchetti e depositi chiusi su consumo, errore e annullamento; rollback foto indipendente.
- Sette fixture valide Windows/telefono superate, telefono con SQLCipher isolato e SHA-256 di tutti gli allegati. EXE Windows reale: 511 MiB importati e salvati, pannello occupato osservato, staging vuoto dopo la chiusura. [Report con misure e limiti](docs/testing/import-benchmark-2026-10-05.md).
- 345 test JVM verdi (108/69/133/35), APK/APK test/portable compilati; 14 test nativi su API 36 e 14 su API 37 superati. Revisione finale di rollback e risorse: `BUILD SUCCESSFUL in 1m 38s`. Contratto, storage, piano e AGENTS aggiornati; AUD-10 e RES-22 rimossi dal tracker.
- Durante il collaudo RES-20 emerso AUD-11: il picker «Esporta e apri» applica erroneamente il filtro/suffisso `.ofam` ai media. Annullamento già verificato senza modifica dei file locali.

## Chiusura AUD-11 e RES-20 — 5 ottobre 2026

Corretto «Esporta e apri»: filtro/suffisso del file originale, estensioni confrontate senza distinzione fra maiuscole e minuscole, nessun suffisso forzato per nomi senza estensione. Nessun cambiamento ai picker `.ofam` o dei documenti.

Collaudo autonomo nell’EXE Windows reale con progetto protetto sintetico: PNG esportato come `.PNG` e aperto in Foto; PDF aperto in Acrobat, tre pagine e SHA-256 identico all’originale. Annullamento del picker senza nuove scritture o variazioni degli hash; estensione non associata apre la scelta di app Windows, annullata senza blocco. Nessun PNG/PDF in `data/`; i pacchetti locali restano invariati. Gli errori interni di un viewer già avviato non vengono intercettati dall’app.

`:pc:app:test :pc:app:packagePortable :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → `BUILD SUCCESSFUL in 1m 16s`; 133 test Desktop e 35 Android JVM verdi. Evidenze locali in `build/reports/native-windows`; AUD-11 e RES-20 rimossi dal tracker. APK aggiornato sul moto g86 (`install -r`: `Success`), «Demo Comune» conservata.

## Prova autonoma Windows RES-23 — 5 ottobre 2026

Pannello occupato visibile e leggibile durante import/persistenza da 511 MiB. Dopo la correzione AUD-11, picker media e ritorno dai viewer verificati. Dialogo nativo di stampa aperto dal flusso Documenti → Stampa; annullato con il suo pulsante Annulla: stato «Stampa annullata», overlay rilasciato e hash dei pacchetti invariati. `Get-PrintJob` non mostra lavori nella coda; nessuna stampa fisica inviata. Catture in `build/reports/native-windows/print-dialog.png` e `print-cancelled.png`.

RES-23 rimane parziale: matrice completa dei comandi e del focus durante salvataggio ed errore nativo di stampa non verificati. Nessuna chiusura implicita dei collaudi hardware o della matrice Android.

## Pulizia conclusiva e residui — 5 ottobre 2026

- Verificati gli SHA-256 delle sette fixture sul moto g86; rimosse le sole fixture della cartella esterna `import-benchmark` e la cartella vuota. Copiati e verificati i sette PDF nativi di ciascun dispositivo in `build/reports/import-benchmark/ZY32LNCB8C` e `emulator-5554`, poi rimossi dai dispositivi. Disinstallati entrambi gli APK test (`Success`); resta installata l’app principale. APK finale sul moto g86 installato con `-r`: demo conservata. Backup pre-Room-v2 con SHA-256 ancora `c62fc20ebda6e438f175857c769c5600b7fc9da06b6f82ce6fa39d2794c6c418`.
- Chiusi app Windows e viewer avviati per le prove; staging vuoto. Verificati gli SHA-256 di tutti i 16 payload della working copy Windows rispetto alla fixture originale. Conservate quattro piccole evidenze riproducibili in `build/reports/native-windows/fixtures`: pacchetto viewer protetto con password fittizia, pacchetto da 511 MiB salvato (ZIP 526.567 byte), PNG e PDF esportati; report hash in `windows-persisted-payloads.txt`.
- La pulizia locale è stata respinta due volte dal controllo automatico con `blocked by policy`, senza motivazione più precisa: prima comando composto con controllo dei percorsi, poi `Remove-Item -LiteralPath` sui tre percorsi assoluti verificati. Tentativi interrotti. RES-24 registra `build/import-benchmark`, `build/native-window-app`, `build/native-window-exports` e i sei helper `complete-import-docs.py`, `complete-print-docs.py`, `complete-viewer-docs.py`, `finalize-import.py`, `trim-viewer-fixture.py`, `check-windows-persistence.py`. Sono tutti output ignorati. I due log di `build/task-verification` restano bloccati dall’emulatore della sessione precedente, non terminato.
- Tracker: completati e rimossi AUD-10, AUD-11, RES-20, RES-22. Restano RES-13, RES-19, RES-23 e RES-24; gli esiti non eseguiti sono espliciti. Nessun commit o push richiesto/eseguito.

Controllo conclusivo: 17 documenti Markdown UTF-8 senza BOM, 83 collegamenti locali validi, tracker coerente e AGENTS di 2.471 caratteri. Report JUnit correnti: 345 test, zero fallimenti/errori/saltati. Strumento benchmark compilato dopo la rimozione del generatore viewer occasionale (BUILD SUCCESSFUL in 2s). git diff --check superato. La sola pulizia locale resta bloccata e tracciata in RES-24.

## Checkpoint import grandi e media Windows per cambio sessione — 5 ottobre 2026

- Commit e push sul branch principale richiesti dall’utente. Verificati branch corrente `main`, default remoto `origin/main` e allineamento dopo `git fetch origin` (0 commit di divergenza).
- Checkpoint: lettura/estrazione/export progressivi con staging cifrato e risorse chiudibili, correzione del picker media Windows, benchmark/regressioni, documentazione e tracker. AUD-10, AUD-11, RES-20 e RES-22 completati e rimossi; restano RES-13, RES-19, RES-23 e RES-24.
- Nessuna modifica ai sorgenti per il checkpoint: verifiche precedenti confermate dai report, 345 test JVM verdi, 14 test nativi per dispositivo, sette fixture grandi superate e hash della persistenza verificati. Nessuna suite ripetuta per sole modifiche al tracker e a questa nota.
- Riprendere RES-24, quindi matrice Android RES-19 e comandi/focus/errore stampa RES-23. Solo prove autonome autorizzate; i collaudi fisici RES-13 restano non eseguiti. La pulizia locale già respinta due volte dal controllo automatico non è ritentata per il checkpoint.
- Conservare il checkout attuale: `build/reports`, `build/phone-backup` e le risorse temporanee residue sono ignorati da Git e non vengono inclusi nel commit o trasferiti dal push. Percorsi e blocchi sono descritti nel tracker; app principale e Demo Comune preservate.
