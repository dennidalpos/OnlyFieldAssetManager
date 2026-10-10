# Evidenze e residui

Aggiornato al 9 ottobre 2026 dopo il secondo audit; completamenti storici sotto, lavoro aperto nel tracker.

## Evidenze disponibili

- I moduli Gradle sono `:shared:core`, `:shared:exchange`, `:mobile:app` e `:pc:app`.
- Il codice implementa `.ofam` 1 e Room 2 (ripartenza greenfield del 5 ottobre 2026), storage desktop atomico, database Android cifrato, import/export, documenti, mappe e configuratore condiviso.
- La suite locale registrata prima di questa revisione copre core, exchange, Android JVM e Desktop; le prove strumentali e manuali hanno limiti espliciti sotto.
- Il workflow GitHub esegue le suite JVM/Compose, genera APK debug e ZIP portable, calcola SHA-256 e pubblica soltanto su tag `v*`.

## Attività aperte

Secondo audit del 9 ottobre completato: **AUD-52–64 verificati e rimossi dal tracker**. Restano **4 residui, 0 P1 / 3 P2 / 1 P3** (RES-13/19/23/24), con i vincoli dei collaudi nativi e delle pulizie preservati. Evidenze nel [report corrente](docs/repo-residuals-2026-10-09.md) nelle sezioni successive.

| ID | Stato | Evidenza richiesta |
| --- | --- | --- |
| RES-13 | Parziale | Fotocamera, scansione e gesti reali; interruzione improvvisa durante scrittura. |
| RES-19 | Parziale | Matrice Android integrale, rotazione reale e TalkBack audio/focus. |
| RES-23 | Parziale | Matrice Windows nativa, input/focus e operazioni ancora non collaudate. |
| RES-24 | Aperto | Residui operativi e pulizia controllata descritti nel tracker. |

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

## Audit completo e ordinamento dei residui — 5 ottobre 2026

- Revisione su `82fe4e2`: inventario di 325 file versionati, quattro moduli e configuratore condiviso; codice, flussi, scambio, storage, documenti, test, build/CI e documentazione. Perimetro e fonti ufficiali nel [report dei residui](docs/repo-residuals-2026-10-05.md).
- Tracker aggiornato con AUD-12…AUD-22 e i quattro residui preesistenti: **15 attività aperte/parziali, 4 P1, 9 P2, 2 P3**. Ogni attività ha priorità, evidenza, riferimenti, dipendenze e criteri di chiusura. Stato di ripresa ricentrato sui difetti; eliminati dal testo di ripresa i riferimenti al precedente commit/push. RES-13/19/23/24 non chiusi né duplicati.
- Quattro difetti riprodotti con dati sintetici: incoming precedente sostituisce una copia Windows chiusa senza Review; allegato da 33 MiB rifiutato impedisce il successivo rinomina; Markdown indica Verificato senza Observation; export Windows include i byte di un allegato rimosso dal catalogo. Le quattro assertion fallite costituiscono evidenza dei difetti, non verifiche superate. Report JUnit e riepilogo locale in `build/reports/repo-audit-2026-10-05`; scenari ed esiti nel report versionato.
- Le prove aggiuntive Android non sono state eseguite: due tentativi di registrare i sorgenti Kotlin dall’init Gradle hanno restituito `No tests found for given includes`. Tentativo interrotto dopo il secondo errore. I rilievi Android restano deduzioni dal codice, senza riproduzione o collaudo nativo impliciti.
- Baseline ordinaria prima delle modifiche documentali: `BUILD SUCCESSFUL in 33s`, Exchange rieseguito, altre suite aggiornate. Dopo la rimozione di tutti i sorgenti temporanei e dell’init: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1` → **`BUILD SUCCESSFUL in 1m 31s`**, Exchange/Desktop/Android JVM rieseguiti; Core aggiornato. Report correnti **345 test** (108/69/133/35), zero fallimenti/errori/saltati e nessuna prova RepoAudit nelle suite ordinarie. Non sono stati modificati controlli o sorgenti applicativi.
- Controllati JSON, unicità degli ID, ordinamento priorità, dipendenze, percorsi sorgente, codifica UTF-8 e collegamenti locali. README e piano rimandano al nuovo report; corretto nel piano il riferimento corrente al benchmark RES-22 già concluso. Modifica preesistente ad AGENTS preservata. Nessuna installazione, stampa, modifica di progetti reali, pulizia delle risorse precedenti, commit o push.

Revisione documentale conclusa il 6 ottobre: tracker datato correttamente, piano allineato ai difetti ancora aperti e AUD-18 formulato senza presumere l’ordine interno di cancellazione del ViewModel. Verifica conclusiva: 18 documenti Markdown UTF-8 senza BOM, 129 collegamenti locali validi, 15 attività con ID univoci e dipendenze valide; `git diff --check` superato. Nessun nuovo test per sole precisazioni documentali.

## AUD-12 — Confronto con la copia locale ricevuta — 6 ottobre 2026

- Confronto per ID incoming su Android e Windows, indipendente dal progetto aperto. La copia Windows chiusa passa dalla conferma anche se identica; una versione precedente mostra l’avviso. Password locale distinta da quella incoming e richiesta prima del confronto quando necessaria. Annullamento senza modifica del progetto o della copia locale.
- Fusione Windows della copia chiusa con password locale conservata e altro progetto invariato; ID diversi respinti. Rimosso il parametro interno Android che permetteva di selezionare il progetto aperto come confronto. Nessun cambio al formato o allo schema. Risorse della copia letta chiuse con use (API ufficiale: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/use.html).
- Baseline Desktop/Android: BUILD SUCCESSFUL in 54s. Regressioni mirate: 14 test superati, BUILD SUCCESSFUL in 27s. Le prime due assertion dei nuovi test confrontavano byte acquisiti prima dell’apertura locale, che rigenera il pacchetto: corretto il momento del baseline, nessun controllo rimosso. Verifica ordinaria dei quattro moduli: BUILD SUCCESSFUL in 1m 4s, 350 test (108 Core, 69 Exchange, 137 Desktop, 36 Android), zero fallimenti/errori/saltati.
- Documenti storage/flussi aggiornati; AUD-12 rimosso dal tracker e dalle dipendenze aperte. Collaudo Android nativo ancora in RES-19; atomicità media/progetto/base ancora in AUD-14. Nessun commit/push.

## AUD-13 — Aggiunta media con limiti e rollback — 6 ottobre 2026

- Windows: picker generico, foto/planimetrie e snapshot condividono la stessa persistenza con rollback della cache indipendente e dei soli file nuovi; limite verificato prima della decodifica e trasferimento progressivo nello staging cifrato. Un file da 33 MiB rifiutato lascia il progetto modificabile. Errori Windows di sostituzione .ofam iniettati con NOSHARE_DELETE: catalogo/cache/file precedenti conservati, successivo rinomina e riapertura riusciti, con e senza password.
- Android: provider copiati con limite anche senza metadati di dimensione; catalogo salvato soltanto dopo controllo di capacità. Picker, foto, planimetrie e mappe usano lo stesso controllo; cleanup non cancellabile dei soli file nuovi. La serie fotografica attende il commit; il proprietario del media viene marcato dentro il commit non cancellabile per evitare la cancellazione del file dopo un salvataggio già durevole. Query/copie picker e scrittura cartografica ora sul worker (parte Android di AUD-20).
- Regressioni permanenti MediaAdditionTest su entrambe le app e MediaCapacityTest. Prove mirate verdi in 20s; verifica ordinaria dei quattro moduli BUILD SUCCESSFUL in 1m 39s: 359 test (108/72/139/40), zero fallimenti/errori/saltati. Ulteriore regressione del margine protetto: quattro test MediaCapacityTest verdi, BUILD SUCCESSFUL in 3s. Nessun collaudo hardware o installazione implicito.
- AUD-13 rimosso, dipendenze aggiornate e documenti flussi/storage/piano allineati. AUD-23 P2 registra il margine conservativo Android protetto vicino al limite ZIP, riprodotto con limite ridotto; la capacità esatta a 256 MiB resta da verificare. AUD-14 ampliato ai payload Windows preesistenti nella sostituzione/fusione chiusa; AUD-19 include eventuali orfani storici oltre limite, che non vengono eliminati automaticamente.

## AUD-20 — Media senza letture pesanti nella composizione — 6 ottobre 2026

- Completata la parte Windows dopo il worker Android introdotto con AUD-13. Disponibilità da catalogo o esistenza del file, senza leggere/decifrare i byte. Anteprime di piano/editor e pagine PDF spostano anche la lettura sul worker; rimosso l’overload che materializzava i byte prima del rendering. Preview del file appena scelto con copia limitata prima della decodifica.
- MediaUiDispatchTest: elenco con callback di lettura che fallirebbe se invocata, thread di composizione diverso dal loader e disponibilità protetta indipendente dalla decifratura dello staging. Tre prove passate in 5s. Verifica finale ordinaria: BUILD SUCCESSFUL in 1m 12s, 363 test (108 Core, 73 Exchange, 142 Desktop, 40 Android), zero fallimenti/errori/saltati. Core/Android aggiornati; Exchange/Desktop rieseguiti. Nessuna suite aggiuntiva per i soli aggiornamenti documentali.
- AUD-20 rimosso dal tracker e dalle dipendenze. Restano 13 attività: 2 P1, 9 P2, 2 P3. Prossimo P1 AUD-14, inclusi i payload precedenti di open/prepareMedia e gli errori di pulizia dopo la scrittura durevole; poi AUD-15. AUD-23 registra il margine conservativo vicino al limite dei progetti Android protetti. Documentazione aggiornata per ogni task; collaudi fisici/native e pulizia storica restano aperti. Nessun commit/push o modifica ai progetti reali.

Revisione conclusiva: preservato il fallback storico «allegato» del picker Android quando il provider non dichiara il nome; suite Android rieseguita dopo questa correzione, BUILD SUCCESSFUL in 29s, 40 test verdi. Report correnti complessivi: 363 test, zero fallimenti/errori/saltati. Controllati 18 Markdown UTF-8 senza BOM, 130 collegamenti locali, 13 ID aperti univoci e dipendenze valide; git diff --check superato. Modifica preesistente ad AGENTS preservata; nessun file temporaneo di questa sessione lasciato nel sorgente.

## AUD-14 — Import e fusione reversibili — 6 ottobre 2026

- Media preparati prima del commit; rollback dei payload esistenti/nuovi, progetto e base su errore. Android salva progetto, verificatore e base in una transazione Room; cestino conservato. Windows salva insieme working copy, media, cestino e base, e aggiorna UI/storia soltanto dopo successo. Rimosso rememberSyncBase separato dagli import.
- Backup grandi in staging cifrato; pulizia post-commit con avviso, senza falso fallimento. Fusione importa soltanto gli allegati selezionati. Nessun cambio a schema/formato, nessun commit/push.
- Baseline BUILD SUCCESSFUL in 1m 16s; regressioni esistenti BUILD SUCCESSFUL in 1m 22s. Nuove prove: BUILD SUCCESSFUL in 18s, 6 test (3 Exchange, 2 Desktop, 1 Android), zero fallimenti. Errori iniettati in estrazione, salvataggio e base; byte, cestino, verifier e retry verificati. Collaudi nativi invariati.
- AUD-14 rimosso dal tracker; AUD-24 registra il recupero dopo arresto improvviso e gli errori ripetuti di rollback. Il rollback gestisce eccezioni durante il processo, non promette atomicità tra filesystem e Room in caso di arresto. Prossimo task AUD-15.

## AUD-15 — Comandi, undo e letture Android coerenti — 6 ottobre 2026

- Comandi ProjectViewModel serializzati: edit calcolati dalla copia persistita dopo il comando precedente, pubblicati dopo save riuscito. Sessione/revisione impediscono rollback e undo obsoleti, riaperture dopo close e pubblicazione sul progetto sbagliato. Edit già richiesti conservati dopo cambio/chiusura; mappe obsolete rifiutate. Cestino/password/media e export condividono l'ordine. Lettura ProjectStore completa in transazione Room.
- Dialogo occupato centrale per bloccare gli input, conferma import compresa. Cancellazione rilanciata e errori dei comandi segnalati dal coordinatore. AUD-18 resta per identità/cancellazione/proprietà del pacchetto; AUD-21 conserva errori Windows, callback password Android e prove mancanti. Nessuna chiusura dei collaudi nativi.
- Prove deterministiche con executor/query callback Room: save fallito seguito da edit, edit ravvicinati, edit+cestino+password, undo obsoleto e valido, close/cambio durante save, load ed export durante scrittura. BUILD SUCCESSFUL in 14s, 9 test mirati verdi. La prima prova fotocamera attende ora apertura e caricamento cestino completi; corretto il blocco indebito di una nuova richiesta di apertura durante comando vecchio. Nessuna assertion rimossa.
- Documenti architettura/flussi/verifica e tracker aggiornati; AUD-15 rimosso dalle attività e dipendenze. Restano 12 attività (10 P2, 2 P3), incluso AUD-24 emerso dal confine file/database; prossimo AUD-16. Nessun commit/push, installazione o modifica a progetti reali.

Verifica conclusiva AUD-14/15: .\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1 → BUILD SUCCESSFUL in 2m 3s. Report: 373 test (108 Core, 76 Exchange, 144 Desktop, 45 Android), zero fallimenti/errori/saltati. Matrice Windows estesa a 16 combinazioni di copia aperta/chiusa, replace/merge, lock progetto/base, con/senza password; cestino e byte verificati prima/dopo e dopo retry. Nessun collaudo hardware, installazione, commit o push. Tracker: 12 attività (10 P2, 2 P3), ID e dipendenze validi; git diff --check superato.

Ultima verifica dopo il controllo dell'export accodato: ProjectCommandTest aggiunge password cambiata prima dell'export, rifiuto senza password prima di creare il file e export cifrato della versione corretta. Quattro test mirati verdi in 12s. Comando ordinario dei quattro moduli rieseguito: BUILD SUCCESSFUL in 32s (Android rieseguito, altri aggiornati); report finali 374 test (108/76/144/46), zero fallimenti/errori/saltati. Documenti senza riferimenti correnti ad AUD-14/15 aperti; tracker invariato a 12 attività, prossimo AUD-16. Validati 18 Markdown UTF-8 senza BOM e 130 collegamenti locali.

## Passaggio di sessione — 6 ottobre 2026

Salvataggio su main richiesto dall'utente con commit e push. Tracker pronto per la ripresa da AUD-16, poi AUD-17: 12 attività aperte/parziali (10 P2, 2 P3), dipendenze e limiti dei collaudi conservati. Baseline finale già eseguita: 374 test verdi; nessun nuovo test per il solo passaggio documentale. Tutte le modifiche di codice/documentazione e le regressioni permanenti di AUD-12/13/14/15/20 fanno parte del salvataggio; output ignorati, backup e dati utente restano fuori dal commit.

## AUD-16 — Operazioni apparati Android condivise — 6 ottobre 2026

Sostituzione e fusione Android delegano a ProjectEdits; lettura/modifica/persistenza/cestino in una transazione. Eliminata la logica duplicata; conservati U, altezza, montaggio, regole di contenimento, porte e riferimenti, campi nascosti del superstite. Baseline verde; perdita di montaggio riprodotta prima della correzione. Tre nuove regressioni coprono parità, ripristino e quattro confini di guasto SQL con rollback/verifier/retry; prove nuove ed esistenti BUILD SUCCESSFUL in 27s. Documenti architettura/flussi/verifica aggiornati; AUD-16 rimosso dal tracker e dalle dipendenze. Residuo AUD-25: quattro opzioni MergeDataChoices non applicate dal core, nessun cambiamento della loro semantica. Prossimo AUD-17; nessun collaudo hardware, commit/push o modifica a progetti reali.

## AUD-17 — Rilievi e avvisi conservati nei documenti — 6 ottobre 2026

Regola comune Observation.effectiveStatus: assenza equivale a Da verificare, stati espliciti invariati e nessun rilievo fittizio persistito. Form e validazione coerenti; Markdown conta il dato assente fra le criticità. Markdown/XLSX/PDF Android e Windows conservano stato, note di apparati/porte/cavi e avvisi pertinenti ai filtri e alle sezioni. Le sole schede rack riportano lo stato anche senza inventario; corretta l'omissione Android individuata dalla prova nativa. Credenziali e allegati esclusi non compaiono negli avvisi. Nessuna modifica a schema/formato/dipendenze.

Baseline mirata verde, falso Verificato riprodotto, regressioni it/en/es completate. Suite JVM finale BUILD SUCCESSFUL in 2m 13s: 381 test (108/78/146/49), zero fallimenti/errori/saltati; Core aggiornato dopo esecuzione verde nella stessa sessione. APK e test APK compilati. PDF nativi reali su Pixel 9 API 37: OK (7 tests) in 1,889s, inclusi stati, note, avvisi, filtri, paginazione e sezioni indipendenti. Emulatore dedicato alla sessione; telefono e progetti reali non modificati. Comandi nella documentazione di verifica; collaudi UX/hardware/stampa invariati.

Documenti dominio/export/localizzazione/verifica e tracker aggiornati per AUD-17. Restano 11 attività aperte/parziali (9 P2, 2 P3), incluso AUD-25 emerso dalle opzioni di fusione non applicate; prossimo AUD-18, poi AUD-19. Nessun commit/push.

Controllo conclusivo AUD-16/17: 11 ID aperti univoci e dipendenze valide; 18 Markdown UTF-8 senza BOM, 133 collegamenti locali validi; git diff --check e fine riga superati. Emulatore della sessione terminato e scratch rimosso dopo il rilascio dei log; nessuna rimozione delle risorse storiche di RES-24. Nessun commit/push.

## AUD-18 — Ciclo di vita import Android — 6 ottobre 2026

Job conservato e identità per richiesta; annullamento/nuova richiesta/chiusura ViewModel cancellano lettura e fusione. Pacchetto posseduto dal worker fino al trasferimento alla Review, quindi dal comando di conferma/fusione fino al completamento. Risultati scartati al ritorno dal dispatcher vengono chiusi; il busy si libera anche se il comando viene cancellato prima di iniziare. Conferma e fusione consumano lo stato prima dell'avvio, impedendo doppi salvataggi e riuso di pacchetti chiusi. Il commit già iniziato resta non cancellabile, come in AUD-14.

Baseline Android BUILD SUCCESSFUL in 2s (risultati aggiornati). La nuova prova cancelledWorkerReturnClosesThePackageBeforeReviewReceivesOwnership sul ViewModel precedente riproduce una Review dopo cancelImport (fallimento atteso); sorgente corrente ripristinato subito dopo. Otto nuove prove permanenti: cancellazione lettura e PBKDF2 reale, doppio import protetto, chiusura durante confronto, ritorno worker prima della Review, conferma lenta/doppia, cancellazione prima del commit, fusione annullata/ripetuta. Payload e rimozione dello staging cifrato verificati; nessun mock della cifratura o dipendenza nuova.

Comando: .\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.ImportLifecycleTest --tests com.onlyfield.assetmanager.ImportAtomicityTest --tests com.onlyfield.assetmanager.ProjectCommandTest --no-parallel --max-workers=1 → BUILD SUCCESSFUL in 14s; 13 test, zero fallimenti/errori/saltati. Fonti consultate il 6 ottobre: [withContext e risorse restituite](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html), [lifecycle ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis). Documenti architettura/flussi/verifica aggiornati; AUD-18 rimosso con dipendenze. Restano 10 attività (8 P2, 2 P3). Nessun collaudo nativo, commit/push o modifica ai progetti reali.

## AUD-19 — Media attivi, undo/cestino e raccolta di proprietà — 6 ottobre 2026

Export selezionato prima della lettura: soli media attivi, esclusi orfani, foto degli oggetti nel cestino e metadati locali. Alias supportati conservati; date di revisione stabili e base del progetto inviato. Copia locale con undo Android a un passo e storia Windows a 50 revisioni; byte raccolti alla scadenza, chiusura/cambio/password o riapertura. Foto del cestino conservate con metadati anche nella sostituzione/fusione; allegati serializzati nel cestino storico mantenuti. Rimozione definitiva elimina foto di apparati/porte e invalida undo; cancellazione Android del progetto comprende base, righe e directory di proprietà. Backup di rollback grandi cifrati, UUID e percorsi confinati, nessun collegamento seguito verso sorgenti/altre copie. Cambio password Windows riusa il blocco reversibile esistente: rimosso il rollback manuale duplicato.

Baseline Windows media/protezione/cestino verde in 8s. Tre prove nuove hanno riprodotto payload rimosso esportato, lettura dell'orfano da 33 MiB e entry non catalogate. La precedente assertion interop che pretendeva tre payload a fronte di un solo record attivo è stata sostituita da catalogo esatto, hash del payload attivo ed esclusione esplicita dei due orfani. Nessuna assertion di integrità eliminata.

Diciassette nuove prove media (9 Windows, 8 Android): hash/undo/export, scadenza a 50 revisioni, chiusura, cestino/ripristino/eliminazione, metadati e byte attraverso sostituzione chiusa, nessun sito, allegato serializzato storico, orfani oltre limite, sorgenti e altri progetti intatti, SQL abort e file Windows NOSHARE_DELETE con rollback/retry. Corrette due fixture di prova: import con avvisi attraversa la conferma reale; allegati di progetti diversi hanno UUID distinti. La suite completa ha poi trovato quattro regressioni Windows, corrette preservando tombstone ATTACHMENT e evitando riscritture al cambio di copia quando non c'è recupero da raccogliere.

Il ripristino senza siti perdeva la voce senza ricreare l'apparato: ora errore localizzato it/en/es, cestino e media conservati su entrambe le app. AUD-26 registra scelta del sito quando quello originale manca e tipi non supportati; senza siti esistono regressioni esplicite, con sito esistente si verifica anche la presenza dell'apparato ripristinato. Nessuna ricostruzione o ricollocazione automatica aggiunta.

Verifica completa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 2m 40s: 406 test (108 Core, 78 Exchange, 155 Windows, 65 Android), zero fallimenti/errori/saltati; APK compilato. XML conservati in build/reports/aud18-19-full. Ultima verifica mirata dopo la conservazione della data di revisione nell'export e della base selezionata:

```powershell
.\gradlew.bat :pc:app:test --tests com.onlyfield.assetmanager.pc.MediaLifecycleTest --tests com.onlyfield.assetmanager.pc.ReplacementPasswordTest --tests com.onlyfield.assetmanager.pc.LocalImportComparisonTest --tests com.onlyfield.assetmanager.pc.BidirectionalInteropTest :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.MediaLifecycleTest --tests com.onlyfield.assetmanager.ProjectCommandTest --tests com.onlyfield.assetmanager.ImportLifecycleTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 27s, 40 test (20/20), APK finale ricompilato; nessun test aggiuntivo per i soli documenti. Fonti ufficiali Java Files e Android path traversal nella documentazione storage. AUD-18/19 rimossi dal tracker e dalle dipendenze; AUD-26 aggiunto. Restano 10 attività (8 P2, 2 P3), prossimo AUD-21. Recupero dopo arresto/secondo guasto ancora AUD-24; collaudi nativi RES-13/19/23 e risorse storiche RES-24 invariati. Nessuna installazione, modifica a demo/dati utente, commit o push.

Controllo conclusivo AUD-18/19: 10 ID aperti univoci e dipendenze valide; 18 Markdown UTF-8 senza BOM, 137 collegamenti locali validi e fine riga senza duplicazioni. git diff --check superato. Pulizia respinta due volte dal controllo automatico: blocked by policy, nessun dettaglio ulteriore; secondo tentativo limitato alla rimozione non ricorsiva delle sole cartelle vuote. Il compilatore ha eliminato autonomamente il marker; restano .kotlin e .kotlin/sessions vuote, registrate in RES-24. Tentativi interrotti; nessun processo terminato, risorse storiche e report conservati. Nessun commit/push.

## AUD-21 — Errori, callback e preferenze — 6 ottobre 2026

Cambio password Android: errori di scrittura/rimozione restituiti una sola volta al dialogo e segnalati, cancellazione rilanciata; chiusura/cambio progetto impedisce callback e successo tardivi. Verificati apertura con JSON danneggiato, password errata, riprova dopo errore, rollback di ripristino/rimozione/svuotamento cestino, cancellazione di comandi in coda e chiusura durante cambio password. Nessuna modifica di schema Room/API UI.

Windows: preferenze lette una volta all'avvio sul worker. Errore I/O o sintassi Properties non valida visibili, file conservato e valori predefiniti solo per avviare l'app. Tema e lingua cambiano dopo scrittura reversibile; proprietà estranee conservate. Errore di pulizia post-commit distinto da save fallito. Tre regressioni su file malformato, directory al posto del file e reale blocco Windows NOSHARE_DELETE con riprova e byte invariati. Messaggi it/en/es. Le nuove prove hanno fallito sul codice precedente: callback password assente (1/8 Android), errore tema/lettura invisibile (3/3 Windows). Corrette le cause; nessuna asserzione rimossa.

Baseline: `:mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.ProjectCommandTest :pc:app:test --tests com.onlyfield.assetmanager.pc.PasswordRotationTest --no-parallel --max-workers=1`, BUILD SUCCESSFUL in 11s. Prima correzione: 15 prove mirate verdi in 16s; aggiunte poi prove per rimozione password, cancellazione in coda, chiusura e apertura protetta.

Verifica finale:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 2m 28s: **415 test** (108 core, 78 exchange, 158 Windows, 71 Android), zero fallimenti/errori/saltati; APK compilato. XML completi conservati in `build/reports/aud21-full`. Fonti ufficiali Kotlin Cancellation e Java 21 Properties consultate il 6 ottobre e collegate nella documentazione architettura/storage. AUD-21 rimosso dal tracker e dalle dipendenze; collaudo messaggi/focus nativi esplicitato in RES-19/23. Nessuna installazione, modifica dati/demo/backup o pulizia di risorse storiche; nessun commit/push.

## AUD-23 — Capacità protetta prudenziale confermata — 6 ottobre 2026

Decisione esplicita dell'utente: mantenere il limite prudenziale e documentarlo. Nessuna richiesta o persistenza aggiuntiva della password, nessuna modifica al budget o al formato. Un'aggiunta vicino al limite ZIP può essere rifiutata anche se il pacchetto effettivo sarebbe ammissibile; il rollback conserva dati/media precedenti. Non viene promessa capacità esatta al confine. Decisione consolidata in plan.md, contratto e workflow; AUD-23 rimosso dal tracker.

Le quattro prove MediaCapacityTest sono comprese nella suite exchange di AUD-21 (78 test, zero errori): inclusi rifiuto file da 33 MiB, limiti aggregati, export/reimport protetto e margine prudenziale con limite ridotto e ZIP cifrato reale. Non eseguita una nuova prova reale a 256 MiB. La chiusura è per scelta del requisito, senza presentarla come verifica esatta. Fonti del budget: zlib compressBound e specifica ZIP PKWARE, collegate nel report residui e consultate il 6 ottobre. Nessun test ripetuto per i soli documenti.

## AUD-26 — Ripristino senza ricollocazione implicita — 6 ottobre 2026

Decisione esplicita: bloccare il ripristino se manca il sito originale e conservare nel cestino. DEVICE non usa più il primo sito disponibile; null/ID originale assente generano errore. RACK registra il sito del piano nelle nuove voci usando originalSiteId esistente e richiede contesto ancora disponibile; rack senza piano resta valido. Nessuna modifica Room v2 o formato .ofam v1.

DEVICE/RACK/CREDENTIAL condividono ProjectEdits.restoreFromTrash: controllo progetto/ID serializzato, rifiuto di ID dell'entità già presente nello stesso catalogo, nessuna sovrascrittura. Credenziali ripristinate integralmente anche su Windows. ATTACHMENT storico e tipi sconosciuti generano errore prima di rimuovere la voce, byte conservati. Android legge voce/progetto, salva e rimuove nella stessa transazione; voce assente restituisce false e la UI segnala errore. Windows verifica la presenza nel cestino e calcola il risultato prima di cambiare stato; salvataggio reversibile preesistente conservato.

Cinque regressioni core hanno fallito sul codice precedente: sito diverso, tipi non supportati, credenziale non ricreata, ID duplicato e metadati incoerenti. Aggiunta prova sul contesto rack. Le prove Android e Windows verificano sito assente/diverso, conservazione progetto/cestino/media e riprova quando torna il sito originale, ATTACHMENT storico conservato, credenziali/collisioni senza perdita di segreti. Windows copre anche copie protette e byte della working copy invariati. Una chiamata errata openStored(file, password) nelle nuove prove non compilava: corretta usando importFile(file, password, compare=false), ingresso reale già esistente; nessuna modifica delle API produttive.

Verifica mirata:

```powershell
.\gradlew.bat :shared:core:test --tests com.onlyfield.assetmanager.core.TrashRestoreTest --tests com.onlyfield.assetmanager.core.ObjectHierarchyTest --tests com.onlyfield.assetmanager.core.ConfiguratorTest :pc:app:test --tests com.onlyfield.assetmanager.pc.MediaLifecycleTest --tests com.onlyfield.assetmanager.pc.ProtectedTrashTest :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.MediaLifecycleTest --tests com.onlyfield.assetmanager.DeviceOperationsTest --tests com.onlyfield.assetmanager.ProjectCommandTest --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 48s, **68 test** (32 core, 13 Windows, 23 Android). Verifica completa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 2m 24s: **425 test** (114/78/160/73), zero fallimenti/errori/saltati; APK compilato. XML conservati in build/reports/aud26-full. Fonte primaria consultata il 6 ottobre: [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt); gli esiti di rollback sono evidenze dei test del repository. AUD-26 rimosso dal tracker e dalle dipendenze. AUD-27 registra da codice i riferimenti secondari: piani/collocazioni mancanti e porte con ID riutilizzati, non riprodotti su persistenza. Nessuna chiusura implicita di questi casi o dei collaudi nativi. Nessuna installazione, modifica dati/demo/backup o pulizia delle risorse storiche; nessun commit/push.

Controllo conclusivo AUD-21/23/26: tracker con 8 ID aperti univoci, dipendenze valide e nessuno dei tre task completati; 18 Markdown UTF-8 senza BOM, 137 collegamenti locali validi e nessun CRLF duplicato. git diff --check superato. .kotlin e .kotlin/sessions rimangono vuote come già tracciato in RES-24; nessun nuovo tentativo di rimozione o processo terminato. Evidenze conservate, modifiche preesistenti preservate; nessun commit/push.

## Checkpoint per cambio sessione — 6 ottobre 2026

Su richiesta esplicita, salvare con commit e push su main il lavoro presente di AUD-16/17/18/19/21/23/26, inclusi documenti e regressioni. Prima del commit main è il branch predefinito, allineato a origin/main dopo fetch. Il punto di ripresa è AUD-27; tracker con 8 attività aperte/parziali (6 P2, 2 P3), completamenti conservati nella roadmap.

Decisioni confermate: AUD-23 mantiene il limite prudenziale senza password aggiuntiva; AUD-26 blocca il ripristino quando manca il sito originale, conservando cestino/media. AUD-27 deve convalidare riferimenti secondari e collisioni degli ID delle porte; i suoi casi restano da riprodurre. AUD-24/25 richiedono politica di recupero/fusione; AUD-22 è pulizia runtime/test. RES-13/19/23 conservano i collaudi hardware/UX/stampa incompleti, RES-24 le risorse storiche e le cartelle vuote già tracciate.

Evidenze finali conservate: 425 test (114/78/160/73) senza fallimenti/errori/saltati e APK compilato, BUILD SUCCESSFUL in 2m 24s; 68 prove mirate verdi. Nessuna suite ripetuta per il checkpoint, che modifica solo documentazione/tracker. I report XML in build/reports/aud26-full, aud21-full e aud18-19-full e gli artefatti locali sono ignorati: non vengono inclusi nel commit e non sono disponibili automaticamente in un altro checkout. Dati, demo, backup e risorse delle prove conservati; nessuna nuova installazione o rimozione.

Alla ripresa leggere README.md, PROJECT_STATUS.json, plan.md, questa roadmap e docs/repo-residuals-2026-10-05.md; verificare git status e HEAD/origin/main. Il commit effettivo si ricava da git log, senza ID autoreferenziale nel tracker. Non ritentare alla cieca le pulizie bloccate o terminare processi di altre sessioni.

## AUD-27 — Riferimenti secondari nel ripristino — 6 ottobre 2026

AUD-27 (6 ottobre): il ripristino richiede i piani originali nella stessa sede, contenitori/figli e montaggi ancora disponibili. Un contenitore spostato su un altro piano o un figlio ricollocato bloccano il ripristino. ID di porte attive, porte duplicate nel JSON e ID di collocazioni già presenti sono rifiutati; nessuna collocazione saltata o sostituita. Progetto, credenziali, base di scambio, cestino e media restano invariati su rifiuto; si può riprovare dopo aver ripristinato il contesto. Le foto delle porte di un apparato nel cestino restano locali anche quando un apparato attivo riusa l’ID della porta e non vengono esportate. Controlli nel progetto corrente; collisioni tra progetti Android tracciate separatamente in AUD-28.

Baseline mirata BUILD SUCCESSFUL in 16s. Cinque nuove regressioni core hanno riprodotto piano mancante/trasferito, porte attive/duplicate, contenitore/figlio mancante e collocazione con ID riutilizzato. La prova Windows ha inoltre trovato foto della porta perse durante sostituzione quando il suo ID veniva riutilizzato; corretto il riconoscimento dei media ancora nel cestino. Le prove Android e Windows passano per quattro scenari dopo sostituzione con stesso sito; Windows copre copie protette e non protette. Verificati byte, credenziali fittizie, base, cestino persistito, export senza foto del cestino e riprova riuscita.

Suite completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 3m 13s, 433 test (120/78/161/74), zero fallimenti/errori/saltati e APK compilato. Aggiunto poi il controllo del contenitore spostato su altro piano; ultima verifica mirata Core TrashRestoreTest/ObjectHierarchyTest/ConfiguratorTest, Windows MediaLifecycleTest/ProtectedTrashTest e Android MediaLifecycleTest/DeviceOperationsTest, con assembleDebug: BUILD SUCCESSFUL in 31s. XML mirati in build/reports/aud27-targeted. Il totale 433 descrive la suite precedente all’ultima regressione, non un nuovo totale completo.

AUD-27 rimosso dal tracker; AUD-28 aggiunto per chiavi Room globali tra progetti, da codice e non riprodotto. Aggiornati contratto, workflow, mappa, storage, piano e verifica; messaggi nativi ancora in RES-19. Restano 8 attività, prossimo AUD-22; politica AUD-24 proposta e in attesa di risposta. Nessun collaudo hardware, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-22 — Runtime e prove senza scaffolding — 6 ottobre 2026

Rimossi CoreModule/ExchangeModule con i due test di nome/dipendenza e i due Example Android (somma e package name). Loader fixture Windows spostato in DesktopStorageTest con risorsa processTestResources; nessun percorso relativo o fixture nel runtime. Eliminato cleanTempFolder senza chiamanti, che ignorava gli errori. Rimossi i cinque helper Android di sola prova, InventorySearch/SearchResult e la query Room inutilizzata. Le regressioni di persistenza usano ora ProjectEdits, GlobalSearch e ConnectionGraph già impiegati dalle app; mantenute le assertion su ricerca, seriali, planimetria, percorso e modifica in blocco. Nessuna dipendenza, schema storico SQLCipher, alias/hash supportato o fixture di versioni rifiutate rimossa. Cinque chiavi i18n inutilizzate per lingua eliminate; le etichette ancora presenti nella UI conservate. Commenti BU e versioni di contratto obsolete aggiornati.

Baseline: suite completa AUD-27 verde; prima verifica AUD-22 DesktopStorageTest e ProjectRepositoryTest con assembleDebugAndroidTest: BUILD SUCCESSFUL in 48s, 25 prove (8/17). Verifica finale: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 2m 36s, 431 test (120/77/161/73), zero fallimenti/errori/saltati; APK e APK test compilati. Il calo di tre test JVM riguarda soltanto le prove tautologiche; il test nativo di package name è stato rimosso, senza riesecuzione hardware. XML in build/reports/aud22-full; JAR principale senza fixture verificato, risorsa test presente.

Fonte ufficiale consultata il 6 ottobre: [Gradle ProcessResources](https://docs.gradle.org/current/dsl/org.gradle.language.jvm.tasks.ProcessResources.html). AUD-22 rimosso dal tracker; documenti di architettura/build/verifica e piano aggiornati. Restano 7 attività (6 P2, 1 P3), incluso AUD-28. AUD-24: politica di recupero confermata; AUD-25: politica proposta in attesa di risposta. Nessun collaudo hardware, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-24 — Recupero durevole — 6 ottobre 2026

Implementato `FileRecovery` condiviso per staging/backup cifrati e journal locale, con recupero automatico, password locale Windows protetto e blocco/riprova su errore. Windows coordina copia/base/cestino/media; Android verifica l’esito Room (compresi verificatore, cestino e generazione della base) prima di completare o ripristinare i file. Generazione monotona della base distingue anche import identici. Nessuna modifica allo schema Room v2 o allo scambio `.ofam` v1. Dettagli, fonti e limiti in [storage](docs/04-desktop-storage-interop.md).

Baseline AUD-22: 431 prove verdi. Nuove prove: 12 del journal (arresto reale del processo, errore del marker dopo commit DB, password, backup corrotto/mancante, secondo guasto, modifiche esterne, header incompleto, blocchi grandi); 2 Windows (8 combinazioni fase/password con JVM separate e blocco/riprova); 3 Android (Room persistente chiuso e riaperto, import/cancellazione commit/rollback, catalogo identico, verificatore/base/cestino/media, blocco/riprova). Il rollback riscriveva un file ancora invariato e bloccato: corretto evitando riscritture inutili. Le prove di lifecycle attendono il nuovo recupero iniziale, conservando le assertion di cancellazione. Corrette le fixture di database su disco e i timestamp obbligatori delle nuove prove; nessuna assertion indebolita.

Verifica completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 3m 35s, **448 test** (120/89/163/76), zero fallimenti/errori/saltati; APK e APK test compilati. Ultimo controllo dei percorsi canonici: FileRecoveryTest/ReversibleFilesTest, Windows CrashRecoveryTest e Android CrashRecoveryTest → BUILD SUCCESSFUL in 29s, **20 prove**; XML in `build/reports/aud24-targeted`. Non eseguito collaudo nativo SQLCipher/Keystore né UI dell’EXE; tracciati in RES-13/19/23. Non simulata perdita di alimentazione o guasto hardware del disco.

AUD-24 rimosso dal tracker; 6 attività aperte (5 P2, 1 P3), prossimo AUD-25 con politica confermata. Nessuna installazione, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-25 — Fusione dei dati associati — 6 ottobre 2026

Politica confermata implementata nel core e nei due dialoghi: quattro scelte per credenziali/configurazioni/alimentazioni/campi extra, trasferimento al superstite oppure conservazione nel cestino. ID, classificazioni, segreti e record in conflitto restano distinti. Lo snapshot DEVICE locale conserva i record esclusi; ripristino senza sovrascrivere ID o perdere contesto. File delle configurazioni trasferiti verso il superstite o la porta copiata, anche senza trasferire le porte. Auto-riferimenti e cicli, inclusi rami con più alimentazioni, rifiutano la fusione prima di cambiare progetto/cestino; correzione e riprova riuscite.

Riprodotti e corretti omissione delle quattro opzioni, perdita del file di configurazione in export (allegato DEVICE o PORT) e ciclo creato dalla fusione. Le prove percorrono le 16 combinazioni, conflitti, sorgente di altri apparati, dati riservati, riapertura, export, ripristino e rifiuto/riprova; Windows copre copie protette e non protette. Il callback effettivo DesktopAppState è verificato. Nuovi record locali richiedono questa versione per essere ripristinati; vecchie voci senza associatedData restano leggibili. Nessuna modifica Room v2 o .ofam v1.

Baseline AUD-24 verde. Suite completa prima degli ultimi controlli di ciclo/porta: BUILD SUCCESSFUL in 4m 54s, **454 test** (123/90/164/77), zero fallimenti/errori/saltati, APK e APK test compilati; XML build/reports/aud25-full-before-cycle. Comprende anche il tredicesimo test FileRecovery sugli staging temporanei occupati. Ultima verifica mirata DeviceMergeDataTest/TrashRestoreTest core, DeviceMergeDataTest/ProjectEditsTest/MasterDetailTest Windows e DeviceMergeDataTest/DeviceOperationsTest Android, con assembleDebug e assembleDebugAndroidTest: BUILD SUCCESSFUL in 1m 27s, **41 test** (18/18/5), zero fallimenti/errori/saltati; XML build/reports/aud25-targeted. Il totale 454 non include le ultime regressioni, documentate nella verifica mirata.

Aggiornati contratto, storage, workflow, piano e verifica. AUD-25 rimosso dal tracker; 5 attività (4 P2, 1 P3), prossimo AUD-28. Checkbox, messaggi e focus sui dispositivi reali restano in RES-19/23. Nessuna installazione, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-28 — Identità confinate tra progetti Android — 6 ottobre 2026

Una nuova regressione su Room isolato ha fallito sul codice precedente: saveProject accettava un apparato con ID già appartenente a un altro progetto. ProjectStore verifica ora l’appartenenza di tutti gli ID del catalogo nella transazione, prima di aggiornare progetto o cancellare righe. Siti, piani, apparati e porte risolvono il proprietario attraverso i genitori; 20 tabelle associate hanno projectId diretto. Query parametrizzate in blocchi di 900 ID, nessuna rimappatura, modifica schema o migrazione.

Cinque prove coprono le 24 tabelle su nuovi progetti e sostituzioni (48 casi), collisione oltre il primo blocco e correzione riuscita, import nuovo/sostituzione/fusione con sei tipi di collisione, ripristino di apparati/porte e quattro tipi associati con altro progetto attivo e successiva riprova. Verificati due cataloghi, credenziali fittizie, password, basi, cestino e byte dei media invariati. Gli errori arrivano prima dell’applicazione dei file del journal.

Mirata ProjectIdCollisionTest/ImportAtomicityTest/DeviceOperationsTest/MediaLifecycleTest Android: BUILD SUCCESSFUL in 1m 18s, 20 test senza fallimenti/errori/saltati; XML build/reports/aud28-targeted. Suite completa con core/exchange/Windows/Android, assembleDebug e assembleDebugAndroidTest: BUILD SUCCESSFUL in 5m 47s, **464 test** (125/90/166/83), zero fallimenti/errori/saltati; XML build/reports/aud28-full. Questo totale comprende gli ultimi controlli di AUD-25 e il tredicesimo test FileRecovery. Fonti Room/SQLite collegate in storage, consultate il 6 ottobre.

AUD-28 rimosso dal tracker e documenti aggiornati. Nuovo AUD-29 registra il controllo del grafo di alimentazione al ripristino, inizialmente da codice e da riprodurre; 5 attività (4 P2, 1 P3). Messaggio Android e collaudi nativi restano in RES-19; nessuna installazione, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-29 — Rete di alimentazione al ripristino — 7 ottobre 2026

Riprodotto con dati sintetici il ciclo ricreato dal cestino dopo nuove alimentazioni tra gli apparati rimasti: D→O e N→D conservati nel cestino, poi O→N nel catalogo. La regressione core ha fallito perché il ripristino non generava errore. DeviceTrashData.restore riusa ora il controllo completo della fusione, prima di ricreare i record; nessuna perdita o modifica del cestino. Il messaggio indica il ciclo e permette riprova dopo correzione.

Tre regressioni coprono core, callback Windows e repository Android, cicli semplici e con più sorgenti. Windows usa copie protette e non protette, rifiuto senza cambiare byte della copia/base/cestino/media, correzione e riapertura riuscite. Android verifica catalogo, verificatore, base, cestino e file invariati, riprova e nuova facade. Baseline completa AUD-28: 464 test verdi. Ultima mirata DeviceMergeDataTest/TrashRestoreTest core, DeviceMergeDataTest/MediaLifecycleTest/ProjectEditsTest Windows, DeviceMergeDataTest/MediaLifecycleTest/ProjectIdCollisionTest Android con assembleDebug e assembleDebugAndroidTest: BUILD SUCCESSFUL in 2m 55s, **65 test** (19/27/19), zero fallimenti/errori/saltati; XML build/reports/aud29-targeted. Nessuna nuova suite completa per questa sola guardia condivisa.

AUD-29 rimosso dal tracker. Nuovo AUD-30: il validatore dei pacchetti segueva la sola prima sorgente, distinto dai controlli completi di fusione/ripristino; da riprodurre e consolidare. Aggiornati contratto, storage, workflow, piano e checklist; messaggio nativo in RES-19/23. Nessuna installazione, modifica dati/demo/backup, pulizia storica, commit o push.

## AUD-30 — Cicli con sorgenti alternative nell’import — 7 ottobre 2026

La regressione core ha riprodotto un ciclo nascosto: A→X e A→B, B→Y e B→A veniva considerato valido perché il validatore seguiva solo la prima sorgente. Validazione, fusione e ripristino condividono ora un unico controllo iterativo di tutti gli archi nel modello. Eliminati il percorso precedente e la sua chiave i18n inutilizzata. POWER_FEED_CYCLE_DETECTED resta STRUCTURAL_ERROR; un messaggio unico descrive il grafo del progetto. Nessuna dipendenza, schema, formato o migrazione nuova.

Tre nuove prove: ordine e inversione delle sorgenti, grafo profondo da 10.000 archi senza ricorsione con collegamento ripetuto e ciclo finale/auto-riferimento, import plain/protetto rifiutato con pkg nullo e riprova del pacchetto corretto. Mirata ModelValidatorTest/DeviceMergeDataTest/TrashRestoreTest core e PackageSerializerTest exchange: BUILD SUCCESSFUL in 20s, 40 test (32/8), zero fallimenti/errori/saltati; XML build/reports/aud30-targeted. Il compilatore segnala una assertion !! ridondante preesistente in un altro metodo di PackageSerializerTest; nessun errore o controllo disabilitato.

Verifica finale completa:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 6m 9s: **470 test** (128 core, 91 exchange, 167 Windows, 84 Android), zero fallimenti/errori/saltati; APK e APK test compilati. XML in build/reports/aud30-full. Fonte API Kotlin ArrayDeque consultata il 7 ottobre e collegata in architettura; esiti dei grafi verificati nelle prove del repository.

AUD-30 rimosso dal tracker; rimangono RES-13/19/23/24 (3 P2, 1 P3). I residui nuovi AUD-28/29/30 sono stati tracciati, riprodotti e risolti. Resa dei messaggi/focus e Keystore/SQLCipher nativi ancora da collaudare; nessuna installazione o modifica di app/demo/dati reali. Pulizie storiche bloccate non ritentate, nessun processo di altra sessione terminato.

## Checkpoint per cambio sessione — 7 ottobre 2026

Su richiesta esplicita dell’utente, salvare con commit e push su main il lavoro di AUD-22/24/25/27/28/29/30. Main è il branch predefinito e risulta allineato a origin/main dopo fetch; nessuna riscrittura della storia. L’ID effettivo si ricava dal git log, senza inserirlo autoreferenzialmente nel tracker. Non eseguire nuovi commit/push senza richiesta.

Punto di ripresa: nessun task software aperto; RES-13/19/23 per collaudi hardware, recovery nativo SQLCipher/Keystore e UX/focus, RES-24 per risorse storiche. Decisioni confermate: recupero automatico, password locale Windows protetto, blocco/riprova su errore; fusione trasferisce i dati selezionati al superstite e conserva gli esclusi nel cestino, ID/classificazione/entrambi i conflitti; blocco della fusione per ciclo con riprova. Ripristino e import ora condividono il controllo completo delle alimentazioni. Nessun cambio Room v2 o .ofam v1.

470 test finali verdi e APK/APK test compilati; report XML in build/reports/aud30-full e verifiche intermedie conservati, ignorati e non trasportati dal commit. Dati, app, Demo Comune, media e backup preservati. Non ritentare le pulizie già respinte e non terminare processi di altre sessioni; .kotlin/sessions è vuota dopo la rimozione automatica del marker da parte del compilatore.

Corretto anche il testo del tracker rimasto con codifica Windows durante uno script: i quattro residui originali sono preservati, aggiunte le nuove evidenze e riscritto in UTF-8 senza BOM. Prima di riprendere leggere README.md, tracker, plan.md, questa roadmap e report residui; verificare git status, HEAD/origin/main e disponibilità locale delle evidenze.

## AUD-31 — Pannello occupato sopra i form Windows — 7 ottobre 2026

Durante RES-23, l’EXE aggiornato su Windows 11 Pro (10.0.26300), in storage isolato con fixture sintetica da 511 MiB, mostrava il form Nuova sede sopra BusyOverlay: menu disabilitati, progresso nascosto. Nessuna modifica concorrente o corruzione riprodotta. Il pannello usa ora Dialog non chiudibile durante l’operazione, composto dopo form e conferme; eliminati l’overlay precedente, il loop pointerInput e il Box ridondante. Nessuna modifica a schema, formato, preferenze o dipendenze.

Baseline DesktopIoTest/DesktopUxLayoutTest/MediaUiDispatchTest: 8 test verdi, BUILD SUCCESSFUL in 11s. BusyDialogTest ha riprodotto il difetto con 2 assertion fallite sull’assenza del dialogo occupato; corretto prima il setup della fixture che non soddisfaceva il wizard. Ultima mirata: 10 prove verdi, BUILD SUCCESSFUL in 14s; worker riuscito/fallito, Escape/Tab/Invio/clic e bozza conservata mentre il pannello si apre/chiude. Questa prova non sostituisce il save fallito del form, distinto in AUD-32.

Verifica completa: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:createDistributable --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 3m 9s. Report: **472 test** (128/91/169/84), zero fallimenti/errori/saltati; 169 Windows rieseguiti, altri moduli UP-TO-DATE sul codice invariato. XML in build/reports/aud31-full, mirata in aud31-targeted e regressione iniziale in aud31-red.

EXE corretto: pannello visibile sopra Nuova sede, Escape e clic su Annulla modifiche bloccati; salvataggio riuscito, comandi disponibili dopo commit. Ctrl+N finale ha aperto il wizard dopo il completamento e non costituisce prova di blocco durante quel save. Tre sedi persistite e 16 SHA-256 degli allegati verificati; catture e JSON in build/reports/res23-20261007. Fonte ufficiale [Compose Dialog](https://developer.android.com/develop/ui/compose/components/dialog), consultata il 7 ottobre. AUD-31 rimosso dal tracker, RES-23 resta parziale. Nessuna stampa fisica o modifica a dati/demo/backup reali.

## AUD-32 — Bozze Windows conservate sul save fallito — 7 ottobre 2026

Riprodotto nell’EXE con file .ofam isolato aperto senza condivisione della cancellazione: Nuova sede chiudeva la bozza sul save fallito. Sette regressioni hanno confermato lo stesso comportamento nei form di sede/piano della mappa e progetto/nuova sede/modifica sede/nuova area/modifica area nel pannello Progetto. I form ora chiudono soltanto quando update termina senza errore; nella mappa l’errore compare anche dentro il dialogo. Nessuna modifica al contratto di DesktopAppState.update, allo schema, al formato o alle dipendenze.

Le sette prove iniziali fallivano tutte per il campo della bozza scomparso (build/reports/aud32-red). Mirata con BusyDialogTest/DesktopIoTest/MasterDetailTest e le sette nuove regressioni: 16 test verdi, BUILD SUCCESSFUL in 16s (aud32-targeted). La verifica completa include anche le assertion successive su stato dirty e un solo elemento di history: errore filesystem reale, copia/progetto/history invariati, bozza conservata, rilascio del file, riprova persistita e undo.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:createDistributable --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 3m 1s: **479 test** nei report (128 core, 91 exchange, 176 Windows, 84 Android), zero fallimenti/errori/saltati. I 176 Windows sono rieseguiti; core/exchange/Android UP-TO-DATE sul codice invariato. EXE rigenerato; XML in build/reports/aud32-full.

EXE finale su Windows 11 Pro (10.0.26300), fixture sintetica da 511 MiB e storage separato: save fallito con bozza «Retried native draft» ancora aperta ed errore leggibile nel dialogo; dopo rilascio del file la stessa bozza salva correttamente. Tre sedi precedenti conservate, quarta sede persistita una sola volta e 16 SHA-256 degli allegati uguali alla fixture sorgente. Catture e JSON in build/reports/res23-20261007. Il vecchio hash del pacchetto rilevato prima della riapertura non era una baseline immediata del guasto finale: nessuna equivalenza byte nativa dedotta da quel confronto; le sette regressioni automatiche la verificano prima/dopo il guasto.

AUD-32 rimosso dal tracker. AUD-33 P2 traccia il pattern nei picker di oggetto/pagina, letto da codice e ancora da riprodurre. RES-13/19 restano aperti senza dispositivi ADB; RES-23 resta parziale per focus/password/recupero/fusione/stampa. Pulizia storica RES-24 non ritentata in attesa della risposta; inventario di 296 file, 1.291.597.555 byte, senza reparse point. Nessun commit/push e nessun processo di altre sessioni terminato.

Checkpoint finale AUD-31/32: app isolata chiusa e nessun processo residuo; i due backup telefono conservano gli SHA-256 iniziali. Il controllo automatico ha respinto la rimozione di build/tmp/res23-20261007 con «blocked by policy», senza motivazione ulteriore. Nessun ritentativo; nuovo scratch incluso in RES-24 con percorso e assenza di reparse point verificati. Evidenze conservate in build/reports.

## AUD-33 — Picker Windows della mappa dopo save fallito — 7 ottobre 2026

Cinque regressioni con .ofam isolato bloccato tramite NOSHARE_DELETE riproducono la perdita del picker: posizione da pressione lunga sulla mappa, inserimento nel rack, seconda pagina PDF, immagine e rimozione dello sfondo. Copia, progetto e history restavano integri ma il dialogo veniva chiuso dal chiamante. FloorHomeSection chiude ora soltanto se update termina senza errore; l’ID dell’allegato appena importato resta disponibile sul rifiuto. MapObjectPicker/ObjectPickerDialog e PlanChooser ricevono un messaggio error facoltativo e lo mostrano dentro il dialogo; callback di salvataggio invariati, default null per gli altri consumatori. Nessun nuovo schema, formato o dipendenza.

Baseline ObjectPickerUiTest/FloorMediaTest: 8 prove verdi, BUILD SUCCESSFUL in 11s (build/reports/aud33-baseline). Corretto il setup della nuova fixture (Rack senza siteId e posizione esplicita prima di aprire il contenitore), poi tutte le cinque regressioni fallivano sulla selezione scomparsa (aud33-red). Mirata finale: 22 prove verdi, BUILD SUCCESSFUL in 29s (aud33-targeted), inclusi AUD-31/32. Le prove verificano bozza/errore/preset/posizione/contenitore/pagina, copia e history invariati, riprova persistita e una sola modifica annullabile.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :pc:app:createDistributable --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 6m: **484 test** nei report (128 core, 91 exchange, 181 Windows, 84 Android), zero fallimenti/errori/saltati; 181 Windows e 84 Android rieseguiti, core/exchange UP-TO-DATE sul codice invariato. APK ed EXE rigenerati; XML in build/reports/aud33-full. Nessuna installazione o prova hardware Android: ADB non rileva dispositivi.

EXE su Windows 11 Pro (10.0.26300), piccolo pacchetto sintetico importato nel precedente storage isolato: picker Switch conserva nome/preset ed errore dopo guasto; dopo rilascio del file Aggiungi persiste un solo switch con 24 porte RJ45 e 4 SFP+. PlanChooser conserva la seconda pagina selezionata e l’errore; la stessa conferma dopo rilascio assegna la pagina corretta di un PDF da tre pagine. Per entrambi i guasti SHA-256 dell’intero .ofam invariato rispetto alla baseline rilevata subito prima; PDF finale uguale alla sorgente. Catture, fixture e verification.json in build/reports/aud33-native. Posizione/contenitore/immagine/rimozione dello sfondo coperti in Compose; la matrice nativa completa resta RES-23.

AUD-33 rimosso dal tracker; AUD-34/35 registrano separatamente i picker Windows fuori mappa e le bozze Android chiuse prima dell’esito asincrono, ancora da riprodurre. Fonti ufficiali [stato e remember in Compose](https://developer.android.com/develop/ui/compose/state) e [Dialog](https://developer.android.com/develop/ui/compose/components/dialog), consultate il 7 ottobre: lo stato remember viene perso quando il composable esce dalla composizione. Gli esiti di persistenza qui indicati derivano dai test del repository.

App isolata chiusa, nessun lock della sessione mantenuto; due backup telefono SHA-256 invariati. Riusato build/tmp/res23-20261007, già incluso in RES-24: pulizie precedentemente respinte non ritentate. Dati/demo/evidenze conservati, nessun processo altrui terminato e nessun commit/push.

## AUD-34 — Picker Windows fuori dalla mappa — 7 ottobre 2026

Completato: inventario, nuovo rack, inserimento in unità rack e pagina PDF da Allegati restano aperti sul salvataggio fallito. L’errore è nel picker, la selezione rack cambia solo dopo il successo e la riprova conserva la bozza. I callback Unit esistenti restano invariati; i soli host interni ricevono un lettore dell’errore corrente. RackUnitPicker inoltra il parametro opzionale di errore, come ObjectPickerDialog.

Baseline delle regressioni precedenti verde (12 test). Tre nuove prove iniziali riproducono la scomparsa della bozza/comando; il selettore della riga rack è stato corretto per usare l’azione accessibile, poiché il clic nel layout ristretto non apriva il picker. Finale: 4 regressioni nuove e 12 precedenti, BUILD SUCCESSFUL in 17s, zero fallimenti/errori/saltati. Guasto reale tramite NOSHARE_DELETE sul pacchetto locale; copia byte per byte, catalogo e history precedenti invariati, riprova dalla stessa schermata, lettura del pacchetto persistito e undo verificati.

Suite Windows completa e baseline Android ProjectCommandTest: BUILD SUCCESSFUL in 3m 32s, 185 test Windows e 10 Android senza fallimenti/errori/saltati. XML in build/reports/aud34-targeted e aud34-full; riproduzione iniziale in aud34-red. Nessun nuovo collaudo EXE: la matrice nativa resta RES-23. AUD-34 rimosso dal tracker; AUD-36 registra gli editor completi e il form allegato letti da codice, non riprodotti.

Fonte ufficiale [stato Compose](https://developer.android.com/develop/ui/compose/state), consultata il 7 ottobre: remember perde lo stato quando il composable esce dalla composizione. Le garanzie di persistenza qui riportate derivano dalle prove del repository.

## AUD-35 — Bozze mappa Android dopo esito asincrono — 7 ottobre 2026

Completato: nuova sede/piano, picker oggetto e scelta PDF/immagine/rimozione dello sfondo attendono l’esito del save. Sul guasto la bozza e la selezione restano composte e l’errore è nello stesso form/picker. La riprova usa i dati conservati. Anche un’immagine appena importata mantiene il riferimento al media fino all’esito dell’assegnazione.

ProjectViewModel.edit conserva la firma precedente e aggiunge un overload interno con onResult(String?): null indica successo, una stringa il guasto. Una modifica senza variazioni conferma il successo senza aggiungere undo. Dopo chiusura/cambio progetto non pubblica esiti o errori della vecchia sessione; ensureActive propaga la cancellazione prima delle pubblicazioni. La UI applica il callback solo se il suo scope Compose è ancora attivo. Nessun cambio di schema Room v2, formato .ofam v1 o dipendenze.

Baseline Android ProjectCommandTest: 10 test verdi prima della correzione. La prova nativa iniziale riproduce la scomparsa di tre bozze; il PDF inizialmente richiedeva scorrimento alla seconda pagina, poi riproduce separatamente la scomparsa del comando di conferma. Durante la verifica sono stati adattati i selettori alla tastiera, alla lista lazy e al menu ⋮ delle finestre strette, senza bypass del save o sostituzione del repository.

Finale mirato: 14 ProjectCommandTest e quattro prove native principali, BUILD SUCCESSFUL in 1m 3s. Ulteriori immagine/rimozione portano a **sei test nativi** su moto g86 Android 16/API 36: BUILD SUCCESSFUL in 59s, zero fallimenti/errori/saltati. Trigger SQLite BEFORE UPDATE su database Room in memoria, media sintetici in una cartella UUID nel cacheDir e renderer PDF Android reale. Integrità del catalogo persistito, nome/tipo/posizione/pagina, errore raggiungibile, riprova, chiusura dopo successo e una sola offerta undo verificati. Tastiera chiusa prima di Salva; campi/errori raggiunti con scorrimento. Il test non verifica la matrice TalkBack/focus completa o SQLCipher/Keystore: RES-13/19 restano aperti. Scratch nativo rimosso in finally; nessun accesso al database della demo. APK principale aggiornato dal runner; EXE non rigenerato.

Suite completa JVM/Compose: **492 test** (128 core, 91 exchange, 185 Windows, 88 Android), zero fallimenti/errori/saltati. Windows eseguito nel passaggio AUD-34, Android rieseguito integralmente in AUD-35, core/exchange UP-TO-DATE. Il comando combinato con le sei prove native era fallito soltanto per due selettori del menu adattivo; dopo la correzione la suite nativa è verde. Conferma finale:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

BUILD SUCCESSFUL in 1s (43 task UP-TO-DATE). Prova nativa eseguita con ANDROID_SERIAL=ZY32LNCB8C e `:mobile:app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.onlyfield.assetmanager.FailedMapSaveNativeTest' --no-parallel --max-workers=1`. XML/log conservati in build/reports/aud35-native, aud35-targeted, aud35-full; riproduzioni iniziali in aud35-native-red e aud35-native-red-pdf. AUD-35 rimosso dal tracker; AUD-37 registra i picker/editor Android fuori mappa letti da codice, non riprodotti. Nessun commit/push o pulizia delle risorse storiche.

Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state), [eventi UI e responsabilità di ViewModel/UI](https://developer.android.com/topic/architecture/ui-layer/events), [cancellazione ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html). Le garanzie specifiche dell’app derivano dalle regressioni del repository.

## Stato telefono dopo collaudo e procedura conservativa — 7 ottobre 2026

Il controllo finale ha trovato com.onlyfield.assetmanager assente dopo connectedDebugAndroidTest. Reinstallando l’APK con -r, files/databases/shared_prefs erano assenti. La sessione non aveva verificato lo stato installato prima del primo test: errore del collaudo, distinto dalle regressioni di codice. L’utente ha confermato che sul moto g86 c’era soltanto Demo Comune. APK reinstallato manualmente, demo ripristinato dalla fixture canonica tramite EncryptedSchemaUpgradeTest#demoPackageIsImportedOnDisk con seedApplicationDemo=true: database vuoto richiesto, import SQLCipher su disco, riapertura e media verificati, **OK (1 test)** in 5,389s. Nessun backup grezzo sovrascritto: entrambi SHA-256 corrispondono a build/reports/res23-backup-before-20261007.json; confronto in build/reports/aud35-native/backup-integrity.json.

Le sei prove AUD-35 sono state rieseguite tramite installazione manuale e am instrument: **OK (6 tests)** in 48,663s; pacchetto principale ancora presente dopo la suite. Rimosso solo com.onlyfield.assetmanager.test (Success), app principale riavviata e presente. Questa procedura evita la disinstallazione automatica del runner Gradle. Non eseguire connectedDebugAndroidTest su un dispositivo che contiene dati da conservare. La sola separazione del database di prova non protegge dalla pulizia dei pacchetti eseguita dal runner.

Comandi verificati sul moto g86; installazioni con -r, senza uninstall del pacchetto principale:

```powershell
adb -s ZY32LNCB8C install -r mobile/app/build/outputs/apk/debug/app-debug.apk
adb -s ZY32LNCB8C install -r mobile/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.FailedMapSaveNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner
```

Verificare prima pacchetto/dati e backup esportabile; controllare il pacchetto principale dopo la prova. Il seedApplicationDemo è stato usato solo per questo recupero concordato e non deve essere incluso nei collaudi ordinari. I backup grezzi cifrati dipendono dal Keystore e non sono garanzia di recupero dopo una disinstallazione. Fonti primarie: [implementazione ufficiale del plugin APK installer UTP](https://android.googlesource.com/platform/tools/base/+/445534e2a5188fca7990ed6455fb83f9aa5bba2a/utp/android-test-plugin-host-apk-installer/src/main/java/com/android/tools/utp/plugins/host/apkinstaller/AndroidTestApkInstallerPlugin.kt), consultata il 7 ottobre: afterAll disinstalla i pacchetti con uninstallAfterTest. Rimozione e ripristino qui descritti sono osservazioni della sessione.

Chiusura documentale AUD-34/35: tracker con sei sole voci aperte/parziali e riferimenti/dipendenze validi; 18 Markdown UTF-8 senza BOM e 143 link locali validi; git diff --check superato. Rimossi quattro import inutilizzati nel solo scope Windows modificato; :pc:app:compileKotlin --no-parallel --max-workers=1, BUILD SUCCESSFUL in 2s. Nessun nuovo test ripetuto per la sola rimozione degli import.

## Passaggio di sessione — 7 ottobre 2026

Su richiesta esplicita dell’utente, salvataggio del lavoro con commit e push sul ramo principale main. Prima del commit, fetch di origin/main riuscito e confronto HEAD...origin/main pari a 0/0; diff e nuovi test rivisti. Il checkpoint comprende AUD-31–35, documentazione e regressioni. Riprendere da AUD-36, poi AUD-37; restano RES-13/19/23/24. I report locali confermano 492 test JVM e sei prove native verdi; nessuna nuova esecuzione richiesta dalle sole modifiche di consegna. Report, APK/EXE, fixture, dati e backup sono ignorati da Git e restano su questa macchina. Controllare presenza delle evidenze prima di usarle in un altro checkout. Preservare Demo Comune ripristinato sul moto g86 e usare la procedura manuale ADB documentata. Nessuna pulizia storica ritentata.

## AUD-36 — Editor Windows e allegati dopo save fallito — 7 ottobre 2026

DeviceDialog e RackDialog (anche Aggiungi e modifica), collocazione, sostituzione, modifica multipla e form allegato si chiudono dopo il successo. Sul guasto rimangono campi, file e selezioni; il nuovo rack viene selezionato dopo la persistenza. Sostituzione e modifica multipla conservano cestino/history e selezione sul fallimento. Nessuna firma, dipendenza, schema Room o formato .ofam cambiato.

Baseline: 11 regressioni preesistenti, BUILD SUCCESSFUL in 52s. Riproduzione iniziale: gli editor apparato scompaiono dopo il guasto filesystem mentre copia e history restano integre. Otto regressioni FailedEditorSaveTest verificano creazione/modifica apparato e rack, collocazione, sostituzione, batch e allegato con file locale aperto NOSHARE_DELETE; riprova, riapertura e un solo undo verdi. Il file viene scelto tramite il vero JFileChooser, con sorgente sintetica in TemporaryFolder.

Comando: .\gradlew.bat :pc:app:test --no-parallel --max-workers=1. **BUILD SUCCESSFUL in 3m 8s; 193 test, zero fallimenti/errori/saltati**. XML in build/reports/aud36-full; baseline e prima verifica isolata in aud36-baseline e aud36-section-targeted.

Limite esplicito: il runner Compose con DesktopApp completo si blocca nel rendering Skiko durante il nuovo rack. Thread dump conservati in build/reports/aud36-renderer-thread-dump.txt e aud36-full-renderer-thread-dump.txt; fermati solo worker avviati da questa sessione. Azione accessibile e pausa del clock non risolvono la matrice completa. I test finali montano le sezioni e i veri form con DesktopAppState/DesktopIo/storage, senza la cornice globale; nessuna asserzione su bozza, errore, integrità o undo esclusa. Questa prova non sostituisce il collaudo EXE/focus/blocco input, ancora in RES-23.

Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state-hoisting) e [cancellazione Kotlin](https://kotlinlang.org/docs/cancellation-and-timeouts.html). L'esito della persistenza deriva dalle prove locali. AUD-36 rimosso dal tracker; AUD-38 registra Modelli/Credenziali Windows, stesso pattern da codice, non riprodotto. Nessun commit/push o pulizia storica.

## AUD-37 — Bozze Android fuori mappa — 7 ottobre 2026

Picker/editor Inventario, Rack e unità rack, collocazione/batch, sede/piano, creazione/modifica/applicazione modello e pagina PDF da Allegati attendono l'esito di edit prima di scartare la bozza. Sul guasto restano campi e selezioni con errore nello stesso form; la riprova salva una sola modifica annullabile. EditSave riusa il callback esistente, limita gli esiti alla composizione/bozza corrente e cancella l'errore quando cambia bozza. Le firme esistenti del ViewModel, Room v2 e .ofam v1 restano invariati; nessuna nuova dipendenza.

Baseline ProjectCommandTest: 14 test, BUILD SUCCESSFUL in 34s. L'editor apparato nativo perde la bozza sul guasto SQLite prima della correzione; nel medesimo tentativo il picker non viene raggiunto per un selettore del FAB. Due nuove regressioni JVM verificano che un editor dismesso non riceva successo/errore tardivi: 16 test mirati verdi, incluso il comportamento preesistente di undo/no-op/sessione/cancellazione.

FailedSectionSaveNativeTest: **OK (17 tests) in 117,658s** su moto g86 API 36, tramite `adb install -r` e `adb shell am instrument -w -e class com.onlyfield.assetmanager.FailedSectionSaveNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner`. Guasto BEFORE UPDATE nella sola Room in memoria; progetto/cache/PDF sintetici isolati. Ogni scenario verifica bozza/selezioni, errore raggiungibile, repository invariato sul fallimento, riprova senza duplicati, riapertura e un solo undo fino al progetto precedente. Tastiera chiusa prima del save; campi/errori raggiunti tramite scorrimento. Primo giro: 10 verdi e 7 errori nei selettori FAB; accesso all'albero semantico non aggregato corretto, 7 mirati verdi, poi intera matrice verde. Nessuna asserzione esclusa e nessun collaudo SQLCipher/Keystore/TalkBack rivendicato.

Verifica complessiva:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

**BUILD SUCCESSFUL in 2m 35s; 502 test nei report, zero fallimenti/errori/saltati** (128 core, 91 exchange, 193 Windows, 90 Android). Android rieseguito integralmente; core/exchange e Windows UP-TO-DATE, quest'ultimo già eseguito in AUD-36. XML in build/reports/aud37-full, log aud37-full-suite.log; baseline/mirati e prove native in aud37-baseline, aud37-targeted e aud37-native (red, primo giro, mirati, finale).

App Android aggiornata con -r; APK test disinstallato (Success), pacchetto principale verificato e app riavviata. Database e WAL originali SHA-256 invariati prima del riavvio, come i due backup storici: confronti in aud37-native. Cache di ogni scenario rimossa in finally. Quattro piccole fixture JUnit Windows lasciate dai worker interrotti (6708 byte totali, soli progetti Editor retry verificati nei ZIP e marker source.txt di questa sessione) conservate: pulizia respinta dal controllo automatico con blocked by policy, senza altra motivazione. Nessun ritentativo; percorsi esatti e inventario in aud36-scratch-inventory.json e RES-24. Recovery senza file; risorse storiche e processi altrui preservati.

Fonti ufficiali consultate il 7 ottobre: [stato e ciclo di vita Compose](https://developer.android.com/develop/ui/compose/state-hoisting), [cancellazione Kotlin](https://kotlinlang.org/docs/cancellation-and-timeouts.html). Le prove locali stabiliscono integrità ed esito del salvataggio. AUD-37 rimosso dal tracker; AUD-39 separa sostituzione/fusione, import/classificazione allegato, credenziali e download cartografico Android, letti da codice e non riprodotti. Restano anche AUD-38 e RES-13/19/23/24. Nessun EXE rigenerato o commit/push.


## AUD-38 — Modelli e Credenziali Windows — 7 ottobre 2026

Creazione/modifica/applicazione modello e creazione/modifica credenziale chiudono la bozza soltanto dopo il save riuscito; l'errore è nel form. DesktopApp inoltra l'errore corrente. Gli overload precedenti preservano i chiamanti, incluse le lambda finali; nessuna nuova dipendenza, schema o formato.

Baseline FailedEditorSaveTest: 8 prove verdi, BUILD SUCCESSFUL in 28s. Dieci regressioni nuove riproducono la bozza scomparsa, con file .ofam sintetico bloccato tramite NOSHARE_DELETE; catalogo, byte e history restano invariati sul guasto. Prima compilazione della fixture corretta con `arrayOf<Any>`; un comando di modifica respinto dal parser PowerShell ha causato un secondo giro sul codice invariato, poi applicate patch puntuali. Il controllo dei consumatori ha rilevato la lambda finale CredentialsSection nel test preesistente: overload compatibile mantenuto.

Finale mirato: 18 prove verdi, BUILD SUCCESSFUL in 41s. Suite ` .\gradlew.bat :pc:app:test --no-parallel --max-workers=1 `: **203 test, zero fallimenti/errori/saltati**, BUILD SUCCESSFUL in 4m 53s. Evidenze in build/reports/aud38-baseline, aud38-red, aud38-targeted e aud38-full. I dieci casi coprono sia FormDialog sia MasterDetailHost, selezioni e campi conservati, segreti esclusivamente sintetici, riprova senza duplicati, lettura del pacchetto persistito e un solo undo.

Correzione AUD-38 completata e rimossa dal tracker; il criterio nativo DesktopApp/EXE resta esplicitamente in RES-23, con il limite del runner già osservato in AUD-36. Nessun nuovo collaudo EXE, commit/push o pulizia delle risorse storiche. Prossimo AUD-39.

Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state) e [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting). Remember perde lo stato quando il form esce dalla composizione; esito e integrità specifici dell'app derivano dalle prove locali.


## AUD-39 — Comandi specializzati e credenziali Android — 7 ottobre 2026

Sostituzione/fusione, import/classificazione allegato, creazione/modifica credenziale e download cartografico conservano il form fino al successo. Sul guasto campi, selezioni e navigazione restano disponibili con errore e riprova. La sostituzione torna indietro dopo il successo. Gli overload precedenti del ViewModel restano compatibili; gli esiti nuovi sono limitati alla sessione, verificano la cancellazione e, per i media, arrivano dopo la pulizia. EditSave.submit riusa lo stesso controllo della composizione degli edit generici. Nessuna nuova dipendenza, schema Room v2 o formato .ofam v1.

Baseline ProjectCommandTest: 16 test verdi, BUILD SUCCESSFUL in 22s. Sei prove native iniziali riproducono bozze/comandi scomparsi e navigazione anticipata, con guasto BEFORE UPDATE nella sola Room in memoria; catalogo e media originali invariati. Red conservato in build/reports/aud39-native/red.txt (6 fallimenti, 46,45s). Il runner am instrument restituisce codice shell zero anche con test falliti: il comando di conferma verifica esplicitamente OK e termina con errore se manca.

FailedSpecializedSaveNativeTest: **OK (8 tests) in 71,349s**, moto g86 5G Android 16/API 36, tramite install -r e am instrument. Otto flussi coprono campi/selezioni conservati, errore raggiungibile, retry senza duplicati, catalogo/cestino/media integri sul guasto, persistenza dopo retry e undo nei flussi che già lo prevedono. Sostituzione/fusione mantengono il cestino transazionale e non ricevono un nuovo undo. Import usa un risultato del picker sintetico; download usa un PNG reale sintetico e una sorgente iniettata dal costruttore interno, provando errore della sorgente e persistenza SQLite senza reti pubbliche. Il costruttore applicativo precedente usa sempre CartographicMapManager reale. Repository, staging e salvataggio dei media restano reali. La prova non copre picker di sistema, server cartografico, SQLCipher/Keystore, TalkBack o matrice UX completa: RES-13/19 restano aperti.

SpecializedCommandTest aggiunge 18 regressioni: callback di guasto dopo cleanup, retry unico, chiusura sessione, cancellazione, editor dismesso, fusione rifiutata e sorgente import illeggibile. Mirata con ProjectCommandTest e MediaAdditionTest: **38 test verdi**, BUILD SUCCESSFUL in 25s. Prima compilazione/APK con i 16 ProjectCommandTest preesistenti: BUILD SUCCESSFUL in 34s. Report baseline/mirati in build/reports/aud39-baseline e aud39-targeted.

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

**BUILD SUCCESSFUL in 2m 41s; 530 test nei report, zero fallimenti/errori/saltati** (128 core, 91 exchange, 203 Windows, 108 Android). Android rieseguito integralmente; Windows già verificato in AUD-38, core/exchange sul codice invariato UP-TO-DATE. XML e conteggi in build/reports/aud39-full; log aud39-full-suite.log. Prove native in aud39-native/first-fixed.txt.

Il telefono si è scollegato prima del controllo finale ed è stato ricollegato dall'utente. SHA-256 di database e WAL identici alla baseline prima della riapertura; due backup storici invariati, confronti in aud39-native. APK test rimosso (Success), app principale verificata e riaperta; nessuna disinstallazione del pacchetto principale. Cache di ogni scenario rimossa in finally e assenza delle cartelle special-save verificata. Nessuna pulizia storica, rigenerazione EXE o commit/push.

AUD-39 completato e rimosso dal tracker. AUD-40 registra separatamente la navigazione anticipata nelle conferme di cancellazione apparato/rack, letta da codice e non riprodotta. Collaudi generali RES-13/19/23/24 conservati. Fonti ufficiali consultate il 7 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state), [eventi UI](https://developer.android.com/topic/architecture/ui-layer/events) e [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html). Le proprietà delle API vengono dalle fonti; esito e integrità dell'app dalle prove locali.


## Passaggio di sessione AUD-36–39 — 7 ottobre 2026

Su richiesta esplicita dell’utente, checkpoint del lavoro corrente con commit e push sul ramo principale main. Prima del checkpoint, fetch origin/main riuscito e confronto HEAD...origin/main pari a 0/0; diff e regressioni rivisti. Il checkpoint comprende AUD-36–39, codice, test e documentazione; il tracker contiene soltanto AUD-40 e RES-13/19/23/24 (quattro P2, un P3). Riprendere da AUD-40: navigazione prima dell’esito di cancellazione apparato/rack Android, evidenza da codice e non riprodotta.

Ultima suite completa: BUILD SUCCESSFUL in 2m 41s, 530 test nei report senza fallimenti/errori/saltati (128 core, 91 exchange, 203 Windows, 108 Android). Otto nuove prove native AUD-39 verdi sul moto g86 API 36; le precedenti 17 prove AUD-37 restano documentate separatamente. Nessuna nuova esecuzione per le sole modifiche di consegna. APK test rimosso e app principale riaperta; database/WAL prima della riapertura e due backup storici SHA-256 invariati.

Report locali, build, APK/EXE, fixture e backup sono ignorati da Git e restano sulla macchina: il push non li trasferisce. Evidenze principali in build/reports/aud38-full, aud39-full, aud39-native e aud39-full-suite.log; conservare anche quelle AUD-36/37. Verificare la loro presenza prima di utilizzarle in un altro checkout. Preservare Demo Comune e dati/backup; collaudi nativi con install -r + am instrument, mai connectedDebugAndroidTest su hardware con dati. Nessuna pulizia storica ritentata; RES-24 conserva gli inventari. I collaudi EXE/focus, matrice UX e SQLCipher/Keystore restano nei rispettivi residui. Alla ripresa verificare ramo, uguaglianza HEAD/origin/main e copia di lavoro.

## AUD-40 — Cancellazione apparato/rack Android — 8 ottobre 2026

Riprodotto su moto g86 API 36 con guasto SQLite isolato: DEVICE/RACK rimangono nel database ma la conferma torna subito alla lista. Due prove native rosse; dieci nuove regressioni JVM riproducono anche l'errore tardivo dopo chiusura/cambio progetto (due rosse, otto verdi). Fixture sincronizzate tramite ObjectHierarchy, non dati applicativi reali.

Rimossi i due back anticipati; il ritorno esistente reagisce all'oggetto assente dopo reload del commit, con controllo di progetto/destinazione. Nessuna nuova firma/callback, dipendenza, schema o formato. moveToTrash rispetta ensureActive prima di pubblicare stato/messaggi e sopprime errori di una sessione chiusa.

Verifica mirata: `.\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.DeletionCommandTest --tests com.onlyfield.assetmanager.ProjectCommandTest --tests com.onlyfield.assetmanager.SpecializedCommandTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 44s, 44 test senza fallimenti/errori/saltati. Baseline dei comandi esistenti verde prima della correzione. Dieci regressioni: guasto/riprova, doppia richiesta con un solo cestino/undo, media e collocazione rack ripristinati, ID assente, chiusura/cambio progetto, altra schermata e cancellazione ViewModel.

Native: `adb -s ZY32LNCB8C install -r` per APK principale/test e `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.FailedDeletionNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → **OK (2 tests), 35,168s**. AppRoot/ConfirmHost/Scaffold/snackbar reali: annullamento conferma, guasto SQLite, scheda e selezioni conservate, errore visualizzato, riprova, un solo ritorno e undo toccato nella UI; database/media verificati. L'undo attende la durata dello snackbar di errore precedente. Cache delete-save-UUID rimossa in finally, database/WAL e due backup storici SHA-256 invariati prima della riapertura. Report `build/reports/aud40-red`, `aud40-targeted`, `aud40-native` e log `aud40-*.log`.

AUD-40 completato e rimosso dal tracker; restano RES-13/19/23/24. Matrici UX/TalkBack, hardware/SQLCipher/Keystore ed EXE/focus/stampa conservano i propri limiti. Fonti primarie consultate l'8 ottobre: [eventi e navigazione UI Android](https://developer.android.com/topic/architecture/ui-layer/events) e [ensureActive Kotlin](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html); esiti e integrità dell'app derivano dalle prove locali.

Suite completa AUD-40: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 2m 43s, 540 report senza fallimenti/errori/saltati** (128 core, 91 exchange, 203 Windows, 118 Android). Android rieseguito integralmente; moduli invariati UP-TO-DATE. XML e conteggi conservati in `build/reports/aud40-full`; log `build/reports/aud40-full-suite.log`.
Perimetro dei collaudi hardware (decisione utente, 8 ottobre 2026): usare la fotocamera integrata Android; lettori USB e scanner esterni sono esclusi definitivamente dalle prove richieste. Le funzionalità applicative restano disponibili; questa è una decisione sul collaudo. RES-13 conserva fotocamera/gesti e recupero SQLCipher/Keystore isolato, con distinzione fra riavvio del processo e del dispositivo.
## RES-13 — Recupero nativo SQLCipher/Keystore parziale — 8 ottobre 2026

Aggiunto EncryptedRecoveryNativeTest: EncryptedDatabase e ProjectRepository reali, con ContextWrapper che separa database, media e file delle chiavi avvolte in cache/native-recovery-UUID. Il provider AndroidKeyStore resta reale; nessuna chiave applicativa viene rimossa o esportata. Le verifiche aprono SQLCipher dopo chiusura del database, controllano fingerprint di progetto/base/verificatore password/cestino, byte media e blob della chiave avvolta. Nessuna modifica al runtime, schema o formato.

| Scenario | Esito verificato su moto g86 API 36 |
| --- | --- |
| Sostituzione media prima del commit | Rollback di catalogo/base/password/cestino e ripristino dei byte precedenti. |
| Sostituzione media dopo il commit | Catalogo/base aggiornati, cestino conservato e byte incoming mantenuti. |
| Cancellazione prima del commit | Progetto/base/verificatore/cestino/media precedenti recuperati. |
| Cancellazione dopo il commit | Progetto/base/media rimossi secondo il commit persistito. |
| File modificato esternamente durante rollback | Recupero bloccato, letture/scritture rifiutate, tutti i file di journal/backup SHA-256 invariati; riprova dopo ripristino del file riuscita. |

Comandi: assembleDebugAndroidTest → BUILD SUCCESSFUL in 4s, install -r del solo APK test, poi `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.EncryptedRecoveryNativeTest -e recoveryPhase prepare -e recoveryRun <UUID> com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` e stessa invocazione con recoveryPhase recover e medesimo UUID. **OK (1 test)** in entrambe le fasi, 20,718s / 13,918s; la seconda verifica esplicitamente un PID diverso. Il test senza argomenti esegue anche il percorso normale roundtrip, **OK (1 test), 35,566s**. Report `build/reports/res13-native/{prepare,recover,roundtrip}.txt`, run.json e prepare-pid.txt; build log res13-recovery-build.log.

Il journal è lasciato pendente intenzionalmente: l'interruzione prima del commit è un'eccezione sintetica nella transazione; dopo il commit manca l'acknowledgement. Il runner termina normalmente e il secondo processo recupera i file. Non è un arresto forzato, riavvio del dispositivo o perdita di alimentazione: queste prove restano non eseguite. Il test ordinario pulisce le proprie fixture; la modalità prepare le conserva soltanto fino alla modalità recover. Database/WAL e blob cifrati delle chiavi applicative confrontati prima della riapertura; backup storici conservati.

RES-13 resta PARTIAL per fotocamera integrata, foto porta/cavo e serie, scansione tramite fotocamera, gesti e riavvio fisico. Lettori USB/scanner esterni esclusi definitivamente dalle prove su indicazione utente; funzionalità applicative conservate. RES-19/23/24 invariati nel perimetro residuo. Fonti primarie consultate l'8 ottobre: [Android Keystore](https://developer.android.com/privacy-and-security/keystore) e [SQLCipher Android con Room](https://github.com/sqlcipher/sqlcipher-android). Le fonti descrivono le API; i cinque esiti provengono dalle prove native.

Controllo finale della sessione: SHA-256 di database/WAL, blob cifrati delle chiavi applicative e due backup storici invariati prima della riapertura. Cache delete-save/native-recovery assente; APK test rimosso con Success e MainActivity riaperta. RES-13 aggiornato a PARTIAL; tracker con sole quattro voci residue (3 P2, 1 P3), AUD-40 rimosso. Nessun EXE rigenerato, pulizia storica ritentata o commit/push.

## AUD-41 — Serie foto Android e risultati tardivi — 8 ottobre 2026

Durante RES-13 emerge dal codice che onPhotoResult richiama onSaved in finally, prima della fine di launchCommand. pendingCommands resta positivo e preparePhoto rifiuta lo scatto successivo. PhotoCommandTest riproduce il guasto e altri due problemi: errore/callback dopo chiusura e accettazione del risultato dopo chiusura/riapertura dello stesso progetto. Baseline MediaAdditionTest verde prima della modifica; nuova suite iniziale cinque test, tre fallimenti, XML/log in build/reports/aud41-red e aud41-red.log.

La continuazione attende Job.join: soltanto dopo rilascio del comando può preparare lo scatto successivo. PendingPhoto conserva progetto/allegato/file/sessione; errori e callback sono vincolati alla sessione ancora valida. Cancellazione rilanciata e file non committati rimossi; PhotoCapture ignora la continuazione dopo uscita dalla composizione. Nessuna firma pubblica, dipendenza, schema o formato modificato.

Mirata: `.\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.PhotoCommandTest --tests com.onlyfield.assetmanager.MediaAdditionTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 17s, nove test**, zero fallimenti/errori/saltati. Verificati prima continuazione, cleanup su errore, undo, esiti dopo chiusura/cancellazione e risultato appartenente a un'altra apertura dello stesso progetto.

Native: assembleDebug/assembleDebugAndroidTest → BUILD SUCCESSFUL in 14s; install -r e `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.PhotoSeriesNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → **OK (5 tests), 31,371s**, moto g86 API 36. Tre serie DEVICE/PORT/CABLE con due JPEG sintetici e terzo risultato annullato; guasto SQLite, arresto della serie e riprova; host smontato durante il save senza nuovo lancio. Byte/target, persistenza, export/import AES-GCM e undo verificati. Camera e risultato del permesso sono simulati solo nel registry di test, senza aprire la fotocamera; Compose/helper/launcher/FileProvider/repository e media sono reali, host minimo. Non è collaudo del sensore, di rotazione reale o delle schede rapide porta/cavo complete. Fixture cache/object_photos/native-series-UUID rimosse in finally.

Scatti reali rinviati su risposta esplicita dell'utente. RES-13 mantiene la checklist fisica; RES-19/23 mantengono matrici UX/TalkBack e Windows, RES-24 le pulizie storiche respinte. Fonti primarie consultate l'8 ottobre: [Job.join](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/join.html), [scope Compose](https://developer.android.com/develop/ui/compose/side-effects) e [TakePicture](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture). Esiti e integrità specifici dell'app derivano dalle prove locali.

Verifica finale: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 2m 45s**. 545 test nei report: 128 core, 91 exchange, 203 Windows, 123 Android; zero fallimenti/errori/saltati. Android rieseguito, moduli invariati UP-TO-DATE; APK test finale compilato. XML e conteggi conservati in `build/reports/aud41-full`, log `build/reports/aud41-full-suite.log`.

Database/WAL, chiavi avvolte applicative e due backup storici SHA-256 invariati prima della riapertura. Nessuna cache native-series residua; APK test rimosso con Success e MainActivity riaperta, evidenza `build/reports/aud41-native/final-integrity.txt`. AUD-41 completato e rimosso dal tracker. Restano quattro residui RES-13/19/23/24 (3 P2, 1 P3); prossima attività RES-19, scatti reali rimandati. Nessun EXE rigenerato, pulizia storica ritentata o commit/push.

Controllo documentale finale: 18 Markdown UTF-8 senza BOM, 143 link locali validi; quattro ID residui univoci, riferimenti/dipendenze validi e git diff --check senza errori.

## AUD-42 / RES-19 — Footer e matrice nativa del configuratore — 8 ottobre 2026

Baseline `:pc:app:test --tests com.onlyfield.assetmanager.pc.VisualConfiguratorUiTest --no-parallel --max-workers=1` verde, quattro test. ConfiguratorMatrixNativeTest sul moto g86 API 36 riproduce Chiudi sovrapposto a Scollega senza Dettagli: regressione geometrica rossa (`360dp-light-1.0: quick.disconnect overlaps ux.close`), più prova funzionale in cui il tocco non apre la conferma. I chiamanti completi attuali forniscono Dettagli: il guasto è riprodotto nell'opzione supportata senza callback, su host minimo, non su AppRoot/Demo Comune. Il primo controllo con Dettagli è verde; l'errore iniziale del selettore SW-PEER è un difetto della nuova prova e viene corretto prima della verifica geometrica.

PortQuickDialog dispone azioni, Chiudi e Dettagli in un unico gruppo verticale del footer, evitando l'interazione tra due blocchi di altezza diversa negli slot di AlertDialog. Salva/Indietro della configurazione restano fissi. Nessuna firma, schema, formato o dipendenza cambiati.

`assembleDebug/assembleDebugAndroidTest` e quattro prove Desktop mirate: BUILD SUCCESSFUL in 18s. App e APK test installati con `adb -s ZY32LNCB8C install -r`. `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.ConfiguratorMatrixNativeTest -e matrixEvidence true com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → **OK (3 tests), 51,805s**. Otto combinazioni 360/412 dp × chiaro/scuro × testo 1,0/1,3: footer con/senza Dettagli (16), griglia 48 porte/disposizione via tocco/PoE/scarto (8), percorso/foto come callback/collegamento a P48/scollegamento confermato o annullato (8). 56 coppie PNG/albero semantico correnti in build/reports/res19-native/final-evidence; screenshot del dialogo con CaptureToImage, campione controllato visivamente. Log iniziali, red e fixed conservati; nessuna rimozione delle evidenze.

Host Compose isolato, progetti in memoria e LocalDensity; nessuna impostazione del dispositivo modificata. Nessun salvataggio persistente, sensore, TalkBack audio, tastiera/rotazione, trascinamento, topologia, mappa densa o intera AppRoot collaudati. Foto reali ancora rinviate. La presenza e la geometria dei controlli non dimostrano annunci TalkBack corretti.

Suite completa `:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 6m**, 545 test nei report: 128 core, 91 exchange, 203 Windows, 123 Android, nessun fallimento/errore/saltato. Windows/Android rieseguiti, core/exchange UP-TO-DATE. XML e counts.json in build/reports/aud42-full, log aud42-full-suite.log. Un thread dump durante la suite documenta elaborazione filesystem ordinaria; nessun worker interrotto.

AUD-42 chiuso e rimosso dal tracker; restano RES-13/19/23/24. Fonti primarie consultate l'8 ottobre: [dialoghi Compose](https://developer.android.com/develop/ui/compose/components/dialog), [semantica](https://developer.android.com/develop/ui/compose/accessibility/semantics), [verifica accessibilità](https://developer.android.com/develop/ui/compose/accessibility/testing) e [ADB](https://developer.android.com/tools/adb). Gli esiti specifici derivano dalle prove locali.

## RES-19 — UX-04 sul componente Android — 8 ottobre 2026

Dopo AUD-42, due prove native coprono otto combinazioni ciascuna. Ricerca fra 12 modelli con filtro 12: altri modelli esclusi, campo/scelta raggiungibili, bozza invariata fino alla selezione. Gruppo RJ45 ridotto da due porte a una, con P2 collegata: errore prima dell'approvazione, unico nodo Checkbox e ruolo corretto, conferma esplicita, ID P1/cavo conservati, capo rimosso aperto, apparato remoto invariato. L'approvazione può rimuovere il controllo: corretto l'assert iniziale della nuova prova sul nodo ormai assente, senza cambiare produzione.

Build APK test: BUILD SUCCESSFUL in 3s. Install -r, poi intera ConfiguratorMatrixNativeTest con matrixEvidence true → OK (5 tests), 87,536s, log build/reports/res19-native/complete.txt. CaptureToImage sul popup con isPopup, dialoghi con isDialog e form con root/inset safeDrawing; alberi completi di ogni root. Tastiera chiusa; non sono audio TalkBack, tastiera aperta, rotazione, persistenza né editor/AppRoot completi. I tentativi iniziali con albero ridotto o cattura dello sfondo sono cronologia, non evidenza finale.

80 coppie finali PNG/albero completo generate sul telefono: copia e pulizia pendenti perché ADB non rileva più il dispositivo. Il pull restituisce device 'ZY32LNCB8C' not found. Chiesta riconnessione USB; nessun ritentativo distruttivo. APK test ancora installato e cartella esterna files/configurator-matrix-evidence da rimuovere solo dopo copia e verifica. Non ancora verificati hash finali di database/WAL/chiavi/backup né riaperta l'app principale. Residuo aggiunto a RES-24, senza confonderlo con le pulizie storiche respinte.

UX-04 componente completato; RES-19 conserva gli altri scenari. Nessun nuovo difetto applicativo o modifica al runtime dopo AUD-42; suite JVM con 545 test già verde, non ripetuta per sole modifiche del test e raccolta evidenze.

## RES-23 — Nuovo rack nell'EXE e riprova — 8 ottobre 2026

`:pc:app:createDistributable --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 4s. App/EXE copiati nel solo build/tmp/res23-20261007 già censito in RES-24, runtime/dati preservati. Prima dell'avvio: nessun processo sul percorso o reparse point, due cataloghi sintetici expanded-511 / AUD-33 native verificati. UI con skill computer-use, finestra 1348×854, tema chiaro.

AUD-33 native senza rack: Nuovo rack, nome RES23-RACK, Aggiungi e modifica apre EditorFrame completo senza il blocco del runner. FileStream Open/Read/FileShare.Read sul solo pacchetto c9fff614-a484-5d8b-8d3a-fc6ccec6c2ec.ofam forza il guasto della sostituzione atomica. Banner leggibile, editor/bozza conservati, nome raggiunto con scroll e rifocalizzato; catalogo su disco a zero rack. Il helper rilascia l'handle con Enter e termina con LOCK_RELEASED. Ctrl+S dalla stessa schermata salva un solo rack. Ctrl+W, riapertura e Ctrl+2: RES23-RACK presente. Alt+F4 chiude l'EXE; list_windows e controllo processo senza residui della sessione.

20 SHA-256 invariati: 17 media e tre pacchetti fuori dalla modifica (altro progetto/base sync). Hash del pacchetto modificato diverso dalla baseline precedente all'avvio: non è prova dell'invariabilità byte sul guasto e non viene rivendicata. Catalogo dopo errore/successo letto da project.json nel ZIP. Report build/reports/res23-20261008: data-before.json, integrity.json, window.json, rack-retry-success.png, rack-reopened.png; errore/focus osservati nella sessione. Fonte primaria [FileShare Microsoft](https://learn.microsoft.com/en-us/dotnet/api/system.io.fileshare?view=net-10.0), consultata l'8 ottobre; esiti dalle prove locali.

Prova rack completata, nessun nuovo runtime/difetto e nessuna suite ripetuta. RES-23 resta parziale per altri editor, temi/dimensioni, focus completo/blocco input sui salvataggi lunghi, protezione/recupero, checkbox fusione e stampa. Scratch storico riutilizzato, nessuna nuova copia runtime; pulizie respinte non ritentate. Telefono ancora assente: copia evidenze, hash finali, APK test e riapertura Android pendenti in RES-24.

Controllo documentale finale della sessione: 18 Markdown UTF-8 senza BOM, 144 link locali validi; quattro ID aperti univoci, riferimenti/dipendenze validi e git diff --check senza errori. Nessun task completato rimasto nel tracker. Telefono ancora non rilevato: sola finalizzazione Android pendente nella voce RES-24; app Windows/helper di questa sessione chiusi. Nessun commit/push.

## Seed demo ampliato e rigenerato — 8 ottobre 2026

Richiesta utente completata: DemoSeed mantiene la rete comunale e aggiunge un laboratorio con dieci apparati, rack 12U, quattro stati di connessione, rilievi verificati/da verificare/conflitto/non rilevato, layout e override PoE, AOC, contenitori annidati, VLAN/subnet/interfacce/access/trunk/LAG, NVR e alimentazioni. Dodici modelli switch ricercabili più rack/cavo; sette PNG sintetici incorporati per tutti i sei target e le tre classificazioni. Le anomalie sono avvisi documentali intenzionali; nessun errore strutturale. Guida dei casi aggiornata in docs/05-testing-and-benchmarks.md e README.

Generatore normale :shared:exchange:demoPackage: BUILD SUCCESSFUL in 6s, validazione e reimport prima della scrittura. Pacchetto fixtures/demo/onlyfield-demo.ofam: 366 apparati, 8 rack, 1007 cavi, 14 modelli, 7 allegati, 489823 byte; SHA-256 fabda66be2d7545a1357b8f3a2af14b2742b9e35b9fd6c557eacbe56eeb4dc33. Baseline 13 prove demo verdi in 28s. Un errore di compilazione del nuovo seed, nome locale com che nascondeva l'import qualificato CableForm, corretto con import normale; nessun difetto runtime emerso.

Verifica finale mirata: :shared:exchange:test --tests "com.onlyfield.assetmanager.exchange.Demo*Test" :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1 → BUILD SUCCESSFUL in 45s, 18 prove demo senza fallimenti/errori/saltati; :pc:app:test --tests "com.onlyfield.assetmanager.pc.DeliveryPdfTest" --no-parallel --max-workers=1 → BUILD SUCCESSFUL in 12s, una prova PDF verde. Scambio semplice/cifrato e checksum dei sette payload, decodifica PNG del file generato, filtri/classificazioni, applicazione modello con ID porte/cavi conservati, percorsi/topologia/XLSX verificati. Due anteprime sintetiche ispezionate. Test SQLCipher aggiornato per confrontare il numero importato anziché il vecchio 356 e chiudere ProjectPackage; APK test ricompilato con nuova fixture, esecuzione nativa non effettuata. Nessuna suite completa ripetuta per dati/test del seed.

Evidenze locali in build/reports/demo-seed-20261008: XML delle 18 prove demo e della prova PDF, conteggi, riepilogo/hash pacchetto e anteprime. Fonti primarie Java ImageIO e Gradle JVM testing consultate l'8 ottobre e collegate nella guida. Tracker aggiornato senza aggiungere attività già completate: RES-13/19/23/24 restano aperti e gli esiti nativi precedenti si riferiscono al seed allora installato. Demo/app installata, dati, media, chiavi, backup ed evidenze precedenti conservati; finalizzazione Android della sessione precedente ancora in attesa USB. Nessun commit/push.

Revisione finale del seed: escluso il rack laboratorio da 12U selezionando i rack operativi da 42U nel controllo dei patch panel; resta indipendente l'asserzione sulle 48 porte. Singola prova corretta rieseguita, BUILD SUCCESSFUL in 5s, un test verde; XML separato targeted-panel-check.xml, report precedenti delle 18 prove conservati. SHA-256 della fixture incorporata nell'APK test identico al pacchetto generato. Controllo documentale: 18 Markdown UTF-8 senza BOM, 146 link locali validi, quattro ID aperti univoci con riferimenti/dipendenze validi; git diff --check senza errori.

## RES-23 — Editor apparato nell’EXE, 8 ottobre 2026

EXE nello storage sintetico esistente build/tmp/res23-20261007, finestra 1348×854 e tema chiaro. Modifica dell’apparato con bozza RES23-DEVICE-RETRY: handle FileStream Open/Read/FileShare.Read sul solo pacchetto sintetico provoca il guasto della sostituzione. Editor, bozza e banner restano visibili; Tab passa al tipo e Ctrl+S resta disponibile. SHA-256 del pacchetto identico alla baseline immediata durante il guasto. Dopo rilascio, Ctrl+S salva; chiusura e riapertura confermano un solo apparato, 28 porte e rack RES23-RACK conservato. Screenshot e JSON in build/reports/res23-followup-20261008.

Scenario apparato completato; RES-23 resta PARTIAL per la matrice input durante operazioni lunghe, altri editor, checkbox della fusione, recupero protetto e errore nativo della stampa. Nessun difetto applicativo emerso, nessuna modifica al runtime o suite JVM ripetuta.

## RES-23 — Checkbox fusione nell’EXE, 8 ottobre 2026

Creato AP-01 nel solo progetto sintetico AUD-33 native. Il vero editor Unisci un duplicato mostra tutte le opzioni tramite scorrimento, descrizione e footer fissi. Credenziali risponde a clic sulla riga e Spazio; Tab raggiunge nell’ordine Configurazioni, Alimentazioni e Campi extra, e Spazio cambia ognuna delle selezioni. Escape apre la conferma di scarto; Scarta ritorna all’inventario con entrambi gli apparati conservati. Screenshot merge-options-before.png e merge-keyboard-options.png in build/reports/res23-followup-20261008.

Completata l’interazione dei quattro controlli a 1348×854, tema chiaro e scala standard. Non eseguiti testo ingrandito, applicazione/trasferimento dei dati o rifiuto per ciclo: restano in RES-23. Nessuna modifica al runtime o suite ripetuta.

## RES-23 — Scorciatoie e commit grande nell’EXE, 8 ottobre 2026

Progetto sintetico expanded-511 (535.835.495 byte ZIP espansi, 16 allegati): pannello leggibile durante apertura e salvataggio di RES23-INPUT-MATRIX. Ctrl+N e Ctrl+O non aprono wizard/picker durante l’apertura; Escape non rilascia il pannello. Durante il salvataggio con form Nuova sede ancora aperto, Ctrl+N e Ctrl+W non avviano il wizard né chiudono il progetto: screenshot prima/dopo con pannello e form in build/reports/res23-followup-20261008.

Ctrl+Z è stato ricevuto dopo il commit e ha avviato l’undo; non è evidenza del blocco durante il save. Undo riuscito e persistito: quattro sedi originali, nessuna RES23-INPUT-MATRIX. Ctrl+1 dopo il completamento torna all’inventario. Il clic finale durante apertura e Ctrl+1 non vengono conteggiati come prove di blocco perché hanno coinciso con il rilascio. La matrice mouse/tastiera/focus resta parziale; nessun difetto applicativo dimostrato e nessuna suite ripetuta.

## RES-23 — Annullamento stampa da tastiera, 8 ottobre 2026

EXE corrente, progetto sintetico AUD-33 native: Documenti e stampa → Stampa apre il vero dialogo Windows. Escape lo annulla, il pannello occupato viene rilasciato e lo stato mostra Stampa annullata. Ctrl+1 dalla finestra principale torna all’inventario; due apparati conservati. Tutti i 21 file dello storage (17 media e quattro pacchetti) mantengono lo SHA-256 della baseline immediata e la coda contiene zero lavori. Evidenze print-native-dialog.png, print-cancelled.png, print-integrity.json e print-result.json in build/reports/res23-followup-20261008.

Annullamento da tastiera completato. Non sono errore del motore di stampa o stampa fisica; RES-23 resta parziale. Fonte primaria consultata l’8 ottobre: [PrinterJob Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/print/PrinterJob.html), che distingue esito di printDialog ed esecuzione di print. Gli esiti specifici provengono dalla prova locale.

## Consolidamento tracker — 8 ottobre 2026

Il tracker conserva solo RES-13/19/23/24 (tre P2, un P3), tutti PARTIAL: descrizioni ridotte al lavoro mancante, cronologia conclusa nella roadmap e nelle evidenze. Nessuna voce residua chiusa integralmente da questa sessione; nessun nuovo difetto applicativo emerso. ADB devices -l non rileva dispositivi, quindi matrice Android, copia delle 80 coppie finali e pulizia del telefono restano in attesa USB. Pulizie storiche respinte non ritentate. I collaudi Windows completati sono registrati nelle sezioni precedenti; dettagli della sessione in build/reports/res23-followup-20261008.

Fonti ufficiali consultate l’8 ottobre: [ADB](https://developer.android.com/tools/adb), [verifica accessibilità Compose](https://developer.android.com/develop/ui/compose/accessibility/testing) e [PrinterJob Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/print/PrinterJob.html). Le API documentate non dimostrano il comportamento dell’app: i risultati e i limiti provengono dai collaudi locali.

Controllo finale della sessione: 18 Markdown UTF-8 senza BOM, 146 link locali validi, quattro ID residui univoci e riferimenti/dipendenze validi; git diff --check senza errori. Tutti i 17 media dello scratch conservano gli SHA-256 iniziali; nessun file .recovery/.tmp nello storage e nessun processo EXE della sessione residuo. Report final-integrity-summary.json e document-validation.json. Nessuna modifica ai sorgenti o dipendenze in questa sessione; suite/compilazione non ripetute perché non è emerso un nuovo difetto. Modifiche preesistenti conservate, nessun commit/push.

## RES-23 — Creazione modello nell’EXE, 8 ottobre 2026

DesktopApp/EditorFrame reali, storage sintetico res23-20261007, finestra 1348×854 chiara. Nuovo modello RES23-MODEL-NEW: guasto filesystem tramite handle Read/FileShare.Read sul pacchetto conserva nome, editor, errore interno e banner; SHA-256 immediato del pacchetto invariato. Rilascio e Ctrl+S dalla stessa bozza salvano un solo modello; due apparati e 29 porte conservati. Screenshot/JSON in build/reports/res23-editors-20261008. Nessun nuovo difetto o modifica al runtime; matrice restante RES-23 aperta.

### RES-23 — Modifica modello nell’EXE, 8 ottobre 2026

Verificata la modifica di `RES23-MODEL-NEW` in `RES23-MODEL-EDIT`: il blocco Windows del pacchetto conserva errore e bozza, senza alterare il file. Rilasciato il blocco, Ctrl+S salva una sola voce con lo stesso ID. Evidenze in `build/reports/res23-editors-20261008` (`edit-model-*`). Gli altri scenari RES-23 restano aperti.

### RES-23 — Applicazione modello nell’EXE, 8 ottobre 2026

Verificata l’applicazione del modello sintetico senza template porte ad AP-01, rinominato `RES23-MODEL-APPLIED`. Un blocco reale del pacchetto conserva bozza ed errore con SHA-256 invariato; la riprova salva sul medesimo ID, senza duplicare apparati. Come previsto dal modello, il destinatario passa da una porta a zero; lo switch e le sue 28 porte restano identici. Evidenze `build/reports/res23-editors-20261008/apply-model-*`.

L’applicazione è stata inoltre annullata: AP-01 e la sua porta vengono ripristinati. Ripetuta poi la sequenza guasto/riprova sullo stesso AP-01, confermando nel pacchetto `technicalName`, ID apparato, ID modello, zero porte e confronto JSON dello switch invariato (`apply-model-undo-baseline.json`, `apply-model-repeat-*`, `apply-model-persisted.json`). Il primo probe usava erroneamente `name` ed è stato sostituito; non era un difetto del prodotto.

### RES-23 — Creazione credenziale nell’EXE, 8 ottobre 2026

Verificata la nuova credenziale sintetica collegata ad AP-01 e a un gruppo di prova. Sul blocco reale del pacchetto restano utente, segnaposto mascherato, tipo, apparato e gruppo; SHA-256 invariato. Dopo rilascio e Ctrl+S, una sola credenziale persiste con tutti i valori attesi. Nessun segreto reale utilizzato o registrato. Evidenze `build/reports/res23-editors-20261008/new-credential-*`.

### RES-23 — Modifica credenziale nell’EXE, 8 ottobre 2026

Verificata la rinomina dell’utente sintetico: guasto reale, bozza ed errore conservati, SHA-256 invariato, rilascio e riprova Ctrl+S. Persiste una sola credenziale con stesso ID e con segnaposto, tipo, apparato, gruppo e note invariati. Evidenze `build/reports/res23-editors-20261008/edit-credential-*`. Nessun difetto del prodotto emerso nei cinque flussi Modelli/Credenziali; RES-23 conserva le verifiche del frame ancora aperte.

### RES-23 — Preferenze EXE, errore/riprova, 8 ottobre 2026

Creato il tema scuro nel solo profilo sintetico; il blocco Windows di settings.properties impedisce il cambio al chiaro. Il banner mostra Preferenze non salvate, tema e hash di preferenze/pacchetto restano invariati. Ctrl+1 raggiunge Inventario dopo errore. Rilasciato il blocco e ripetuto il comando, il tema chiaro persiste e il banner scompare senza modifiche al pacchetto. Evidenze `build/reports/res23-editors-20261008/preferences-*`. Non esteso a lingua o recupero/password.

### AUD-43 — Focus dei selettori Windows, chiuso l’8 ottobre 2026

Riprodotto nel vero EXE: Escape annulla apertura/esportazione, ma Ctrl+3/Ctrl+1 e Alt+F4 non reagiscono fino a un clic nel frame. Tutti e quattro i JFileChooser usavano parent null e appartenevano al frame Swing nascosto. Baseline: DesktopToolchainTest verde (tre test), NativePickerOwnershipTest rosso (quattro casi); BUILD FAILED in 47s. XML conservati in `build/reports/aud43-red`.

DesktopStorageHelper usa ora la finestra AWT attiva come proprietario per cartella, file singolo, file multipli e salvataggio. Nessuna firma, dipendenza, schema o formato modificato. Le quattro regressioni verificano dialogo realmente aperto, proprietario, posizione dentro il frame, annullamento e ritorno al campo prima focalizzato. Mirata: `.\gradlew.bat :pc:app:test --tests com.onlyfield.assetmanager.pc.DesktopToolchainTest --tests com.onlyfield.assetmanager.pc.NativePickerOwnershipTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 6s, sette test verdi, report `build/reports/aud43-targeted`.

Suite Windows e EXE: `.\gradlew.bat :pc:app:test :pc:app:createDistributable --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 3m 18s, 207 test senza fallimenti/errori/saltati. XML e conteggi in `build/reports/aud43-windows`; log e screenshot in `build/reports/res23-editors-20261008`. Nel nuovo runtime generato, copia minima sintetica di AUD-33: dialoghi centrati; Escape → Ctrl+3, Escape → Ctrl+1, apertura → Escape → Ctrl+E e esportazione → Escape → Alt+F4 funzionano senza clic intermedi. Quattro file invariati rispetto alla baseline dopo apertura (`aud43-native-after-open-before.json`, `aud43-native-final-integrity.json`). La prima baseline precedente all’apertura comprendeva la riscrittura prevista da DesktopAppState.open e non prova l’invarianza dei selettori. Entrambi gli EXE/helper chiusi; nessun job di stampa.

AUD-43 completato e rimosso dal tracker. RES-23 conserva matrice occupato, altre superfici, lingua/password/recupero, fusioni/ripristino per ciclo, testo ingrandito e errore motore stampa. Fonte primaria: [JFileChooser Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/JFileChooser.html) e [KeyboardFocusManager](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/KeyboardFocusManager.html), consultate l’8 ottobre.

RES-24: controllo automatico respinge la rimozione della sola nuova fixture `pc/app/build/compose/binaries/main/app/OnlyFieldAssetManager/data` e del probe invalido `build/reports/res23-editors-20261008/apply-model-entities-before.json`, con blocked by policy e senza altra motivazione. Non ritentata. Fixture creata vuota in questa sessione e popolata solo con due pacchetti, un PDF e settings sintetici; inventario/hash in `cleanup-blocked-inventory.json`. Il probe invalido contiene target/other null e non va usato come prova; baseline corretta `apply-model-undo-baseline.json`. Le pulizie storiche restano rinviate. ADB ancora senza dispositivi; scatti reali già rinviati, evidenze Android non copiate. La verifica password locale richiede il passaggio manuale finale previsto da computer-use/confirmations.md, non effettuato. Nessun commit/push.

### RES-23 — Lingua EXE con guasto/riprova, 8 ottobre 2026

Nel nuovo runtime sintetico AUD-43, cambio Sistema/italiano → English bloccato da handle reale su settings.properties: banner italiano leggibile, lingua precedente e SHA-256 conservati. Rilascio e ripetizione salvano language=en, traducono menu/pagina iniziale e rimuovono errore; nome/testi utente conservati. Ripristinato Italiano dal menu View. Evidenze `build/reports/res23-editors-20261008/language-*`. Nessun difetto o nuova suite; password/recupero e altri scenari RES-23 restano aperti.

### RES-23 — Fusione effettiva e undo nell’EXE, 8 ottobre 2026

Copia sintetica nel runtime AUD-43: Unisci AP-01 in RES23-DEVICE-RETRY, tutte le opzioni di trasferimento selezionate. Sul blocco reale del pacchetto, pannello/scelte/duplicato restano presenti, banner leggibile e SHA-256 invariato. Rilascio e Ctrl+S salvano un apparato con 28 porte e trasferiscono la credenziale mantenendo ID e contenuto; duplicato nel cestino. La pagina Credenziali conferma il nuovo destinatario. Ctrl+Z ripristina il progetto JSON integrale precedente, due apparati, collegamento originale e cestino vuoto. Evidenze `build/reports/res23-editors-20261008/merge-*`.

Scenario trasferimento credenziale/errore/riprova/undo completato. La fixture non contiene configurazioni/alimentazioni/campi extra da trasferire e AP-01 ha zero porte: nessuna estensione dell’esito a questi trasferimenti, al rifiuto per ciclo, al ripristino dal cestino o al testo ingrandito. Nessun difetto applicativo emerso, nessuna nuova suite necessaria.

### AUD-44 — Editor Alimentazioni Windows, riprodotto l’8 ottobre 2026

Preparando la fixture di ciclo RES-23, PowerBadgeSection chiude la nuova alimentazione prima del save. Nel vero EXE AUD-43: AP-01, nome RES23-CYCLE-FEED, sorgente switch; handle reale sul pacchetto e Ctrl+S. Il form scompare, banner di errore, lista vuota e SHA-256 invariato: bozza persa. Stesso schema nel codice PoE e Badge, ancora da riprodurre con regressioni. Report `build/reports/res23-editors-20261008/power-editor-red*`. Helper rilasciato ed EXE chiuso; ciclo non ancora collaudato. AUD-44 P2 aggiunto al tracker.

### AUD-44 — Alimentazioni/PoE/Badge Windows, chiuso l’8 ottobre 2026

Guasto reale della sostituzione del pacchetto: creazione/modifica dei tre editor chiudevano prima dell’esito, perdendo la bozza. Dodici regressioni, dialogo e pannello, riproducono la perdita dopo aver verificato file, progetto, cestino e history invariati. Fixture iniziale corretta con UUID valido prima della baseline rossa; nessun errore della fixture attribuito al prodotto. XML rosso: build/reports/res23-editors-20261008/aud44-red.xml.

PowerBadgeSection chiude ora solo dopo save riuscito, mostra errore interno e protegge Nuovo/Modifica tramite il guard esistente. Selezioni, ID e campi nascosti conservati; firma precedente disponibile, nessuna dipendenza/schema/formato modificato. Mirata FailedPowerSaveTest e FailedModelCredentialSaveTest: BUILD SUCCESSFUL in 34s, 22 test verdi. Suite Windows e createDistributable con init-script build/tmp/aud44-20261008/native-output.init.gradle, --no-parallel --max-workers=1: BUILD SUCCESSFUL in 3m 50s, 219 test, zero fallimenti/errori/saltati. XML in build/reports/aud44-windows, log in res23-editors-20261008.

Nel vero EXE isolato, nuova alimentazione AP-01 da switch: guasto mantiene nome/sorgente/errore e SHA-256 invariato; rilascio e Ctrl+S salvano una sola alimentazione; Ctrl+Z ripristina il progetto JSON integrale. Screenshot/JSON aud44-feed-*; EXE/helper chiusi. Le altre undici varianti sono verificate dalle regressioni, senza estendere il collaudo EXE a tutte. AUD-44 rimosso dal tracker; RES-23 resta parziale. Runtime build/tmp/aud44-native-20261008 e fixture sintetica conservati e inventariati per RES-24, senza alterare la fixture AUD-43 già respinta.

Fonti primarie consultate l’8 ottobre: [stato Compose](https://developer.android.com/develop/ui/compose/state) e [distribuzioni native Compose](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html). Risultati applicativi da prove locali; outputBaseDir usato solo nello script di collaudo. Nessun commit/push.

### RES-19/24 — Evidenze Android recuperate e pulizia telefono, 8 ottobre 2026

Moto g86 API 36 nuovamente autorizzato via ADB. Copiati 176 file (11.234.842 byte) in build/reports/res19-native/complete-evidence-20261008, verificando ogni SHA-256 contro il telefono: 80 coppie finali PNG/albero completo e otto coppie footer precedenti, distinte nel manifest complete-evidence-integrity-20261008.json. Ogni nome finale e intestazione API/modello verificati; due campioni finali controllati visivamente. Non ripetuta la suite già verde; questi screenshot documentano host Compose isolati, non AppRoot, tastiera/TalkBack o persistenza completa.

Database, WAL, db_key.bin e recovery_key.bin coincidono con data-before.txt; entrambi i backup storici SHA-256 invariati. Riverificati percorso fisico e 176 hash immediatamente prima della rimozione: eliminati solo i file della cartella configurator-matrix-evidence e la directory vuota; disinstallato esclusivamente com.onlyfield.assetmanager.test (Success). App principale conservata. Hash dati/chiavi nuovamente invariati prima della riapertura; MainActivity riaperta con Status: ok. Report data-before-cleanup-20261008.txt e data-after-cleanup-20261008.txt.

Recupero evidenze e pulizia del telefono completati e rimossi dalle descrizioni aperte. RES-19 conserva la matrice UX non eseguita; RES-24 conserva solo le risorse Windows storiche respinte e gli eventuali scratch nuovi. Fonte ufficiale [ADB](https://developer.android.com/tools/adb), consultata l’8 ottobre. Nessun commit/push.

### RES-23 — Fusione rifiutata per ciclo e riprova EXE, 8 ottobre 2026

Fixture sintetica con AP-01 alimentato dallo switch. Unire AP-01 nello switch trasferendo Alimentazioni creerebbe un arco verso se stesso: il vero EXE mostra il messaggio localizzato, mantiene pannello/duplicato/scelte e SHA-256 del pacchetto invariato. Tutte le scelte di trasferimento restano raggiungibili e selezionate.

Dalla stessa schermata, escluso il trasferimento Alimentazioni, Ctrl+S salva un solo switch con 28 porte e trasferisce la credenziale; duplicato nel cestino, alimentazione esclusa rimossa insieme al duplicato. Ctrl+Z ripristina il progetto JSON integrale precedente, inclusi due apparati, alimentazione e collegamento credenziale. Non è trasferimento di un’alimentazione valida né correzione dei collegamenti: sono ancora scenari distinti. Report/screenshot merge-cycle-* in build/reports/res23-editors-20261008. Nessun difetto, modifica al runtime o suite ripetuta. Scala standard, tema chiaro 1348 × 854; testo ingrandito e ripristino dal cestino per ciclo ancora aperti.

### RES-23 — Password impostata manualmente nell’EXE, 8 ottobre 2026

Utente ha completato il salvataggio nel progetto sintetico AUD-33 del runtime AUD-44; frame mostra Protetto da password e Password del progetto impostata. Verifica filesystem: pacchetto progetto e base sync cifrati, project.json.enc presente e project.json assente; allegati del pacchetto cifrati. Nessun PDF in chiaro o file temporaneo/recovery nello storage. Password non acquisita o registrata. Inventario/prova password-native-* in build/reports/res23-editors-20261008.

Ctrl+W chiude il progetto e Continua apre il dialogo Pacchetto protetto. Richiesta all’utente prova di password errata, poi riprova corretta; ancora pendente, non conteggiata come successo. EXE lasciato aperto per il collaudo guidato. Il passaggio finale di impostazione è manuale secondo computer-use/confirmations.md; nessun segreto chiesto in chat.

### RES-23 — Riapertura corretta e PDF cifrato, 8 ottobre 2026

L’utente ha inserito la password corretta nel dialogo, senza comunicarla. DesktopApp riapre AUD-33 protetto: due apparati, modello e una alimentazione con AP-01/sorgente switch conservati nella UI. L’unico PDF allegato viene decifrato e renderizzato come planimetria dopo il caricamento asincrono; nessun PDF in chiaro o file tmp/recovery nello storage. Screenshot password-native-reopened-correct, password-native-pdf-rendered e password-native-power-retained.

Impostazione e riapertura corretta native completate; password errata/riprova, guasto del cambio password e recupero bloccato non verificati in questo flusso. Nessun confronto JSON integrale del pacchetto cifrato rivendicato, nessuna password acquisita. EXE chiuso, fixture protetta conservata per RES-24; serve la password dell’utente per riaprirla. Nessun difetto e nessuna suite ripetuta.

### RES-13 — Recovery dopo riavvio fisico, preparazione 8 ottobre 2026

Reinstallato con adb install -r il solo APK test; EncryptedRecoveryNativeTest con recoveryPhase=prepare e recoveryRun=b91d2730-9698-4936-a271-3460d7f0c5cc → OK (1 test), 20,781s. Cinque scenari SQLCipher/Keystore isolati: rollback/commit di update e delete, più rollback bloccato da modifica esterna. Journal cifrati pendenti e chiavi wrapped nel solo cache/native-recovery-b91d2730-9698-4936-a271-3460d7f0c5cc; 35 file inventariati. Database/WAL/chiavi principali SHA-256 invariati contro la nuova baseline dopo apertura app.

Preparazione completata; richiesto all’utente riavvio dal menu del moto g86 e sblocco. Boot ID/PID prima del riavvio in build/reports/res13-reboot-20261008. La fase recover con stesso UUID va eseguita soltanto dopo aver verificato un boot ID diverso; non ancora eseguita né conteggiata come superata. Preservare journal/chiavi/fixture e APK test finché il recupero non si conclude; successivamente il test rimuove il proprio root. Riavvio normale con journal pendenti, senza rivendicare perdita improvvisa di alimentazione durante una scrittura o UX AppRoot/TalkBack. Fonte primaria [Android Keystore](https://developer.android.com/privacy-and-security/keystore), consultata l’8 ottobre; esiti specifici da test nativo.

### RES-13 — Recovery dopo riavvio fisico, completato l’8 ottobre 2026

Sul moto g86 API 36, l’utente ha riavviato dal menu e sbloccato il telefono. Boot ID diverso verificato prima di recoveryPhase=recover, stesso recoveryRun=b91d2730-9698-4936-a271-3460d7f0c5cc: OK (1 test), 14,046s. Cinque scenari SQLCipher/Keystore isolati verificati dopo il riavvio: rollback/commit di update e delete, più rollback bloccato da modifica esterna e riprova; chiavi wrapped conservate, journal recuperati secondo l’esito Room. Preparazione precedente: OK (1 test), 20,781s.

Root recovery assente dopo il finally del test; database/WAL/chiavi principali e due backup storici SHA-256 invariati. Rimosso solo com.onlyfield.assetmanager.test (Success); hash principali ancora invariati prima della riapertura di MainActivity (Status: ok). Report boot-before/after, prepare/recover, data-after-recover/cleanup e run.json in build/reports/res13-reboot-20261008.

Riavvio normale con journal pendenti completato e rimosso dalle attività aperte. Restano arresto forzato/perdita improvvisa di alimentazione durante scrittura, fotocamera e gesti; questa prova non verifica UX AppRoot o TalkBack. Fonte primaria [Android Keystore](https://developer.android.com/privacy-and-security/keystore), consultata l’8 ottobre; esiti specifici dal test nativo.

## Consegna sessione — 8 ottobre 2026

Cambio sessione richiesto dall’utente con salvataggio, commit e push sul ramo principale verificato: main, origin/main; fetch conferma divergenza 0/0 prima del commit. Il salvataggio comprende tutte le modifiche di lavoro presenti, inclusi demo rigenerata e test delle sessioni precedenti. Nessuna nuova modifica applicativa in questa consegna.

Stato: AUD-40–44 completati e rimossi dal tracker; restano soltanto RES-13/19/23/24 parziali. Ultima suite generale AUD-41: 545 test verdi; successive verifiche del seed: 18 prove demo e una PDF verdi, singola asserzione pannelli rieseguita verde. Ultima suite Windows AUD-44 e distribuzione nativa: 219 test verdi. Conteggi XML della suite generale e Windows ricontrollati in questa consegna, zero fallimenti/errori/saltati; non ripetuti i test per aggiornamenti documentali. Test SQLCipher sul seed ampliato ricompilato ma non rieseguito nativamente, come già registrato.

Recovery fisico moto g86 API 36: prepare/recover OK (1 test) per fase, cinque scenari isolati con boot ID diverso; database/WAL/chiavi e backup invariati. Fixture e APK test rimossi, app principale riaperta. Ultima ConfiguratorMatrixNativeTest: cinque prove verdi, 80 coppie finali di evidenze esportate/verificate; host isolati, non intera AppRoot/TalkBack. Tutti gli EXE/helper Windows della sessione chiusi.

Ripresa: chiedere quale progetto/piano di prova è aperto in Android > Mappa (domanda precedente senza risposta), guidare pinch/panoramica TOUCH-01, poi TalkBack e matrice UX. Non conteggiare la domanda come prova superata. Fotografie reali rinviate; lettori USB e scanner esterni esclusi. Riavvio normale non chiude perdita improvvisa/arresto forzato. Windows: conservare fixture AUD-44 protetta dalla password dell’utente, mai acquisita; errata/riprova, guasto cambio password, focus/ripristino/fusione e guasto stampa restano tracciati.

Risorse locali non versionate sotto build/: report res13-reboot-20261008, res19-native/complete-evidence-20261008, res23-editors-20261008 e aud44-windows; backup telefono conservati. RES-24 elenca inventari e pulizie storiche/nuove respinte dal controllo automatico; non ritentate. Non eliminare app/demo/media/chiavi/backup per ottenere una working tree pulita. Il risultato del commit/push è verificabile nella cronologia Git della consegna.

## Revisione UI/UX mappa — 8 ottobre 2026

Implementati UX-01/02/03: navigazione adattiva stabile, contesto mappa di sessione, riepilogo espandibile e pannello laterale, componenti/spaziature condivisi, form testuali entro 640 dp, azioni secondarie e icone accessibili. Database, formato `.ofam`, regole di salvataggio e dipendenze invariati. Eliminati i controlli e lo stato locale sostituiti.

Verifica: 589 test JVM/Compose verdi (128 core, 96 exchange, 242 PC, 123 Android), seguiti dalle regressioni mirate dei ritocchi finali. Build APK debug/test riuscita. Nove scenari nativi distinti verdi su moto g86 API 36: mappa adattiva, tastiera, cinque flussi configuratore e due cancellazioni con errore/riprova; dimensioni simulate tramite LocalDensity. Il footer fuori finestra è stato riprodotto e corretto usando i limiti reali della finestra Android; la sola gestione degli inset non bastava con Compose 1.7.5.

Evidenze, fonti e limiti: [audit UI/UX](docs/ui-ux-audit-2026-10-08.md). Rapporti ignorati in `build/reports/ux-native-20261008`, screenshot PC in `pc/app/build/reports/ux` e `ux-map`. Nessuna chiusura implicita di RES-19/23: restano TalkBack, rotazione reale, tablet fisici e collaudo integrale AppRoot/EXE. Commit/push su main richiesti dall’utente per la consegna.

Chiusura del collaudo UI/UX: ultima regressione footer `OK (1 test)`, numero porta separato dal simbolo a scala 1,3; screenshot verificato. Solo APK test disinstallato (`Success`), MainActivity riaperta (`Status: ok`). Database/WAL identici alle precedenti evidenze hardware; indice SQLite e marcatore profilo non trattati come dati immutabili. Report in `build/reports/ux-native-20261008`.

## Passaggio di consegne UI/UX — 8 ottobre 2026

Tracker aggiornato per cambio sessione: nessuna implementazione UI/UX aperta, residui RES-13/19/23/24 conservati. Ripartire dall’audit UI/UX e dai criteri del tracker, senza ripetere i test già verdi salvo nuovi problemi. Report locali ignorati da Git in `build/reports/ux-native-20261008` e screenshot in `pc/app/build/reports/ux` e `ux-map`; conservarli insieme alle fixture precedenti. APK test rimosso e app principale riaperta. L’utente ha richiesto commit e push su main; SHA ed esito remoto verificabili in Git. Questa richiesta non autorizza commit/push delle attività della prossima sessione.

## Restyling tecnico elegante — 9 ottobre 2026

Completati tema condiviso chiaro/scuro, tipografia semibold, controlli e pannelli arrotondati, gerarchia di progetti/navigazione/editor e icona Android vettoriale con segnaposto e tre nodi. Identita visiva coerente fra Android e Windows; icona EXE, dati e contratti invariati. Corretto il suggerimento di ricerca che poteva espandere la barra; alleggerita la barra progetti Android spostando Importa sotto Continua.

Build iniziale e finale riuscite. Otto scenari Desktop (dimensioni, temi, testo) e cinque scenari Android nativi distinti passati; matrice AppRoot ripetuta dopo aver isolato le densita sintetiche dell'host di test. Controllate 46 coppie di contrasto, minimo testo 5,17:1. Icona verificata in zona sicura, maschere e monocromatico; immagini ispezionate. Dettagli, comandi, fonti e limiti nel [resoconto del restyling](docs/ui-restyling-2026-10-09.md).

Evidenze conservate in `build/reports/restyling-20261009` e `pc/app/build/reports/restyling`. APK Android aggiornato senza disinstallare l'app principale; APK test rimosso, MainActivity riaperta. Unica differenza nei quattro file privati confrontati: `files/profileInstalled`; altri file invariati. Nessun EXE avviato. RES-13/19/23/24 conservati: non dedurre TalkBack, rotazione reale, tablet fisici o copertura completa degli editor da questi risultati. Nessun commit o push.

## Passaggio di consegne restyling — 9 ottobre 2026

Tracker aggiornato per cambio sessione. L’utente ha richiesto commit e push su main dell’intero restyling; SHA ed esito remoto da verificare nella cronologia Git. Ripartire dal resoconto UI e dai residui RES-13/19/23/24, senza ripetere verifiche gia verdi in assenza di nuovi problemi. I report e le anteprime ignorati da Git restano locali nei percorsi documentati: conservarli insieme a dati, backup e fixture storiche. Nessuna autorizzazione a commit/push delle attivita della sessione successiva.

## Aggiornamento dati demo — 9 ottobre 2026

Rigenerato `fixtures/demo/onlyfield-demo.ofam` con le API correnti: 366 apparati, 8 rack, 1007 cavi, 14 modelli e 7 allegati. Gli ultimi cambiamenti UI non richiedono nuovi campi o modifiche al formato. Descrizione e guida incorporata aggiornate con sei percorsi per mappa densa, dettagli Espandi/Riduci, contesto conservato, mappe interne, configuratore 8/48 porte, percorsi e moduli. PNG sintetici allineati alla palette blu/turchese; data fissa di revisione 2026-10-09T00:00:00Z condivisa da progetto, rilievi ed export.

Baseline: 18 prove demo verdi, `BUILD SUCCESSFUL in 47s`. Generazione: `:shared:exchange:demoPackage --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 7s`; validazione e reimportazione in memoria prima della scrittura. Finale: `:shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1`, `BUILD SUCCESSFUL in 39s`, 18 prove senza fallimenti/errori/saltati. Guida estratta e ispezionata, senza testi tagliati. Pacchetto 529.332 byte, SHA-256 `c1ee0a6b1d7f2f2eabb9ddd106d619fd53f81c31f839e44577f295b245dc603a`. Evidenze in `build/reports/demo-seed-20261009`.

Aggiornati documentazione di verifica e tracker; nessuna attività implementativa aperta. Questa revisione aggiorna la fixture del repository; copie installate e dati locali conservati. Importazione nativa della revisione non eseguita, residui RES-13/19/23/24 invariati. Nessun commit o push.

## Audit completo del repository — 9 ottobre 2026

Inventariati 382 file versionati, 309 sorgenti Kotlin di cui 161 applicativi. Revisione trasversale di codice, chiamanti, test/fixture/demo, documentazione, tracker e build/CI/rilascio; nessun segnaposto applicativo TODO/FIXME/HACK/NotImplemented individuato. [Report corrente](docs/repo-residuals-2026-10-09.md) con evidenze, fonti primarie, limiti, legacy da conservare e criteri di chiusura.

Registrati sette nuovi rilievi AUD-45–51: P1 assemblaggio portable può sovrascrivere dati destinatari dalla sorgente; P2 bozze ancora perse in editor esclusi dalle correzioni, decimali non finiti, acquisizione cartografica Android, selezione pagine/layout stampa Android, alimentazioni selezionate dal nome A/B nei documenti ed escaping Markdown. Totale tracker: 11 attività aperte/parziali, 1 P1 / 9 P2 / 1 P3; RES-13/19/23/24 conservati e deduplicati. Dipendenze aggiunte alla chiusura finale RES-19/23; casi indipendenti possono proseguire. Nessun rilievo corretto durante questo audit.

Probe Gradle Sync isolato: `BUILD SUCCESSFUL in 4s`, file omonimo sovrascritto e file destinatario aggiuntivo preservato. VLAN Windows in dialogo/pannello con guasto reale della sostituzione del pacchetto: `BUILD SUCCESSFUL in 5s`, 2 casi confermano perdita della bozza a progetto/file/history invariati. Comando finale exchange/Android con probe e prove esistenti: `BUILD SUCCESSFUL in 3s`, 10 casi. Totale JUnit mirato 12 casi (7 probe dei difetti, 5 prove esistenti), zero fallimenti/errori/saltati. Gli esiti verdi dei probe confermano i difetti, non la correzione. Harness temporanei corretti dopo firma MasterDetailHost errata e source set Android senza test; dettagli nel report.

Evidenze archiviate in `build/reports/repo-audit-20261009`: XML, sorgenti/init script dei probe, inventario e controlli documentali. Nessuna suite generale o qualificazione nativa nuova; nessun packaging su dist, modifica di dati utente, installazione, commit o push. Documenti di dominio, flussi, export, mappe, rilascio e verifica allineati; report precedente mantenuto come storico. Conservati demo/AGENTS e aggiornamenti locali già presenti.

Il controllo automatico ha respinto la rimozione delle tre directory scratch `build/tmp/repo-audit-20261009-portable`, `repo-audit-20261009-network` e `repo-audit-20261009-data`, motivo `blocked by policy`, prima dell’esecuzione. Nessun ritentativo; percorsi/inventario aggiunti a RES-24. Copie dei probe ed evidenze conservate nel report; rimozione da completare quando consentita o manualmente senza toccare le risorse storiche.

Verifica documentale: `build/reports/repo-audit-20261009/validate-audit.ps1` completato con esito zero; 21 Markdown, 181 collegamenti locali esistenti, UTF-8 senza BOM, 11 ID unici, priorità 1/9/1, riferimenti presenti e dipendenze acicliche. XML archiviate ricontrollate: 12 casi, zero fallimenti/errori/saltati. `git diff --check` superato; descrizioni dei quattro residui precedenti preservate, SHA-256 della fixture demo invariato rispetto all’aggiornamento di questa sessione.

## Passaggio di consegne demo e audit — 9 ottobre 2026

Utente ha richiesto commit e push su main per cambio sessione. Consegna comprende seed e pacchetto Demo Comune aggiornati, AGENTS.md consolidato, audit AUD-45–51 e documentazione/tracker allineati. Il branch main è stato verificato allineato a origin/main prima del commit. SHA ed esito del push della consegna sono verificabili in Git; questa richiesta non autorizza commit/push del lavoro della prossima sessione.

Ripartire dal report del 9 ottobre e da AUD-45 (protezione dati durante packaging), poi seguire l’ordine del tracker. Restano 11 attività, 1 P1 / 9 P2 / 1 P3; nessun difetto dell’audit corretto. Non ripetere test già verdi senza nuovi problemi; validazione documentale rieseguita per la consegna. Nessuna nuova build, installazione o prova nativa necessaria per questo passaggio documentale.

Evidenze locali ignorate da Git conservate in `build/reports/demo-seed-20261009` e `build/reports/repo-audit-20261009`, oltre ai report storici: non fanno parte del commit. Le tre directory scratch dell’audit restano inventariate in RES-24 dopo rifiuto automatico della pulizia, senza ritentativi. Conservare dati, chiavi, backup, app/demo e fixture dei collaudi precedenti.

## AUD-45 — Corretto il 9 ottobre 2026

La sorgente del Sync esclude data/** e la destinazione mantiene preserve(data/**); progetti, media e preferenze omonimi non vengono sovrascritti. ZIP privo di dati. AUD-45 rimosso dal tracker.

Baseline isolata: hash delle preferenze destinatario modificato (errore atteso). Dopo la correzione: powershell -NoProfile -File tools/testing/portable-data.ps1 → PASS; fixture eliminata dal finally. .\gradlew.bat :pc:app:packagePortable --no-parallel --max-workers=1 → BUILD SUCCESSFUL. Hash dei dati in dist invariati; ZIP effettivo senza data/. La rigenerazione ha eliminato la vecchia fixture sintetica nel runtime sorgente: quattro file ripristinati con SHA-256 identico, un pacchetto di 43.584 byte senza copia identica reperibile. Utente informato: recupero non richiesto, tutto materiale di test. Report portable-source-restoration.json. Aggiunto controllo preventivo su createDistributable: un runtime con data/ blocca la rigenerazione prima della rimozione. Manifest in build/reports/tracker-remediation-20261009/portable-data-before.json. Fonte primaria: [Gradle Sync](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Sync.html), consultata il 9 ottobre.

AUD-45: controllo preventivo nativo verificato con .\gradlew.bat :pc:app:createDistributable --no-parallel --max-workers=1: rifiuto atteso Runtime data must be backed up and moved before regenerating the app; i quattro file ripristinati restano invariati. Regressione portable ripetuta dopo il controllo: PASS. Il primo tentativo di configurazione tasks.named precedeva la registrazione Compose; corretto con matching.configureEach, nessun bypass.

## AUD-46 — Corretto il 9 ottobre 2026

Rete logica e cablaggio Windows chiudono gli editor soltanto dopo save riuscito e mostrano il guasto nella bozza. Alimentazioni/PoE/Badge, rete e cablaggio Android usano rememberEditSave: errori e campi conservati, chiusura sulla callback positiva, risultati ignorati dopo uscita dalla composizione. ID e campi nascosti delle modifiche preservati. Corretto anche il tipo sintetico non valido nella modifica dei cavi senza objectTypeId.

32 regressioni Compose Windows verdi: sei editor rete e due cablaggio, creazione/modifica e dialogo/pannello, guasto reale NOSHARE_DELETE, progetto/file/trash/history invariati, riprova singola, round-trip e undo completo. Baseline documentata: 30 casi riprodotti e due fixture picker errate, corrette prima della prova finale. Il cavo senza tipo introduceva il tipo non UUID cable e rendeva il pacchetto non importabile; fallback legacy corretto, round-trip verificato. Comando .\gradlew.bat :pc:app:test --tests *FailedNetworkSaveTest* :mobile:app:testDebugUnitTest --tests *SpecializedCommandTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL. Le regressioni helper Android verificano errore/riprova e callback dismesse; compilazione delle schermate Android riuscita. Nessun nuovo collaudo nativo/AppRoot Android o EXE completo: criteri trasferiti esplicitamente a RES-19/23. Fonte primaria [stato Compose](https://developer.android.com/develop/ui/compose/state), consultata il 9 ottobre.

## AUD-49 — Corretto il 9 ottobre 2026

Numeri non finiti e overflow rifiutati dai form e dal modello per carichi W/VA, potenze PoE, budget hardware apparati/modelli, lunghezze, coordinate di posizionamenti/annotazioni/tratte. Errori it/en/es prima della scrittura: guard nei salvataggi Room, storage Windows ed export .ofam; validazioni strutturali indipendenti mantengono i rilievi documentali ammessi. JSON continua a rifiutare i numeri speciali. Il campo budget PoE usa il parser decimale condiviso e finito.

Baseline due regressioni rosse; prove finali NonFiniteValidationTest (due test, matrici NaN/Infinity/-Infinity/overflow e nove superfici numeriche), FinitePackageTest, ProjectRepositoryTest.nonFiniteUpdateLeavesRoomProjectUnchanged e NonFiniteSaveTest: cinque test verdi. Valori finiti con punto/virgola e pacchetto .ofam in round-trip; stream export vuoto, progetto Room e file/history Windows invariati al rifiuto. Comandi Gradle mirati con --no-parallel --max-workers=1: BUILD SUCCESSFUL. Fonti [Kotlin isFinite](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/is-finite.html), consultata il 9 ottobre. Residui UI nativi delle bozze gia in RES-19/23.

## AUD-48 — Corretto il 9 ottobre 2026

Tile Android limitati a 2 MiB durante la lettura, connessione disconnessa in finally. Dimensioni 256x256 verificate prima del decode; bitmap individuali e risultato sempre riciclati, anche a guasto parziale. La griglia usa wrap X e clamp Y ai bordi; input geografici non finiti o fuori range rifiutati. Rimossa la costante italiana legacy NO_NETWORK_MESSAGE dal modulo Android; messaggi it/en/es conservati.

Baseline HTTP locale: risposta chunked oltre 2 MiB accettata. .\gradlew.bat :mobile:app:testDebugUnitTest --tests *Cartograph* --no-parallel --max-workers=1: BUILD SUCCESSFUL, sei test. Soglia esatta ammessa, oltre soglia rifiutato con/senza Content-Length; immagini 512x256, non immagini e payload sovradimensionati rifiutati; errore al quinto tile interrompe il risultato; griglie zoom 1/17 ai quattro bordi e attribuzione PNG 768x768 verificate con Robolectric. Server locale chiuso dal finally, nessun download pubblico o collaudo OOM rivendicato. Fonti [BitmapFactory.Options](https://developer.android.com/reference/android/graphics/BitmapFactory.Options#inJustDecodeBounds), [HttpURLConnection](https://developer.android.com/reference/java/net/HttpURLConnection#disconnect()), consultate il 9 ottobre.

## AUD-47 — Corretto il 9 ottobre 2026

La stampa Android calcola geometria e paginazione dagli attributi di sistema: formato, orientamento e margini. Il layout viene misurato senza conservare pagine PDF; la scrittura genera soltanto le pagine richieste, preservando numerazione originale e restituendo intervalli effettivi ordinati e uniti. Export PDF ordinario A4 conservato. Layout e scrittura lavorano fuori Main, callback singola su Main; cancellazione controllata durante la generazione e destinazione chiusa anche prima del lavoro.

Baseline: onLayout dichiara PAGE_COUNT_UNKNOWN (regressione riprodotta). Gradle :mobile:app:testDebugUnitTest --tests "*PrintAdapterTest*" e assembleDebug/assembleDebugAndroidTest: BUILD SUCCESSFUL; 3 regressioni JVM. adb install -r (senza disinstallazione) e am instrument -e class com.onlyfield.assetmanager.PrintPdfNativeTest: OK (1 test), Moto g86. PDF reali A4 verticale e A5 orizzontale, margini asimmetrici, 160 apparati e scheda rack: dimensioni, bounding box, testo completo, pagina singola/intervallo/intervalli disgiunti e sovrapposti, conteggio e intervalli restituiti coerenti. File persistenti Android invariati per SHA-256; fixture cache rimossa. Evidenze: build/reports/tracker-remediation-20261009/print-native.txt. Fonti primarie: [PrintDocumentAdapter](https://developer.android.com/reference/android/print/PrintDocumentAdapter) e [PrintedPdfDocument](https://developer.android.com/reference/android/print/pdf/PrintedPdfDocument), consultate il 9 ottobre.

## AUD-50 — Corretto il 9 ottobre 2026

Markdown e foglio XLSX Alimentazione adottano una riga per ogni record, senza selezione dal nome A/B: apparato, nome/circuito, tipo, sorgente, presa/uscita, tensione, carico VA/W, autonomia osservata, fonte/data del rilievo e note. XLSX conserva celle numeriche per tensione/carichi/autonomia; PoE e badge restano distinti dai record, compresi badge su target non apparato. PDF Android/Windows enumerano gli stessi dettagli. I filtri selezionano le alimentazioni del consumatore; si risolve solo il nome della sorgente esterna come contesto, senza esportarne inventario o altri record. Nessuna autonomia calcolata.

Baseline PowerDocumentTest: 2 test falliti per circuiti personalizzati omessi. Dopo: test mirati exchange (PowerDocumentTest/DocumentExportTest/DocumentSelectionTest), report Windows e PrintAdapterTest: BUILD SUCCESSFUL; nuova regressione PDF Windows PowerReportTest verde. Android build debug/test verde; am instrument SurveyPdfTest,PrintPdfNativeTest → OK (4 tests): PDF reali con tipi PRIMARY_A, SECONDARY_B, UPS, PDU, diretta, OTHER, UNKNOWN, sorgente esterna al filtro e campi completi, tre lingue. Database/preferenze Android invariati per SHA-256 (3 file); cambiato il solo marcatore runtime profileInstalled dopo install -r. Evidenze power-and-print-native.txt e android-after-power.json nella cartella remediation. Fonti: contratto PowerFeed corrente, selezione DocumentSelection verificata dalle regressioni; [GFM tabelle](https://github.github.com/gfm/#tables-extension-), consultato il 9 ottobre.

## AUD-51 — Corretto il 9 ottobre 2026

Escaping Markdown applicato a testo utente in titoli, metadati, inventario, hardware, rack, cablaggio, VLAN, alimentazioni, badge, attribuzioni, note e avvisi. CR/LF normalizzati in spazi soltanto nel documento; punteggiatura Markdown/HTML e backslash resi letterali. Gli span usano delimitatori adeguati ai backtick del contenuto; le pipe sono esterne agli span per mantenere tabelle e backslash anche nei renderer GFM. Testo del progetto invariato. Rimossi i quattro messaggi legacy delle colonne A/B non più usati.

Baseline MarkdownEscapingTest: intestazione CR/LF introdotta dal testo utente (regressione riprodotta). Gradle exchange: 12 regressioni documenti/filtri/lingue verdi prima delle prove ai bordi; MarkdownEscapingTest ora 2 test, BUILD SUCCESSFUL. tools/testing/markdown-content.ps1 → PASS it/en/es, 7 tabelle ciascuna: parser Markdig di ConvertFrom-Markdown verifica intestazioni, liste, markup, righe/colonne e testo letterale rispetto al controllo; PASS anche 7 casi span (pipe, backslash adiacenti, delimitatori backtick, spazi e CR/LF). Fixture/evidenze in shared/exchange/build/reports/markdown-*.md; nessuna dipendenza aggiunta. Fonte primaria [GFM](https://github.github.com/gfm/) e sezioni tabelle/escape/code span, consultate il 9 ottobre.

## Verifica finale della remediation — 9 ottobre 2026

AUD-45–51 completati, tracker ridotto ai soli RES-13/19/23/24 (3 P2, 1 P3). Criteri nativi non eseguiti trasferiti ai residui, senza dedurre la matrice AppRoot/EXE dai test.

`.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 7m 44s: **643 test**, 130 core + 101 exchange + 282 Windows + 130 Android JVM, zero fallimenti/errori/saltati. Conteggi conservati in build/reports/tracker-remediation-20261009/final-suite.json; i report Gradle dei successivi comandi mirati descrivono il relativo sottoinsieme.

Revisione finale: export PDF ordinario mantiene la chiusura dello stream; PrintAdapterTest e MarkdownEscapingTest rieseguiti, BUILD SUCCESSFUL. APK principale/test ricompilati; install -r e am instrument SurveyPdfTest,PrintPdfNativeTest → **OK (4 tests), 4,638s**, moto g86 5G Android API 36. Verificati anche gli stream chiusi. Destinazioni/fixture PDF pulite nel finally; database/preferenze principali (3 file) SHA-256 invariati prima della riapertura. Solo profileInstalled cambia con la nuova APK, non è un dato utente. APK test della sessione rimosso (Success), MainActivity riaperta (Status: ok). Evidenze final-native.txt/final-native-state.json.

`pwsh -NoProfile -File tools/testing/markdown-content.ps1` → PASS nelle tre lingue e nei sette casi ai bordi degli span. Usa ConvertFrom-Markdown già presente, senza dipendenze nuove. Per rigenerare le fixture eseguire prima `.\gradlew.bat :shared:exchange:test --tests '*MarkdownEscapingTest*' --no-parallel --max-workers=1`. `powershell -NoProfile -File tools/testing/portable-data.ps1` già verificato dopo il controllo preventivo del runtime, PASS; nessun packaging ripetuto sulla sorgente popolata.

Documentazione di dominio aggiornata ad ogni chiusura, riepiloghi README/plan/audit riallineati. Stato storico spostato fuori dalle descrizioni operative del tracker. Nessun commit/push o pubblicazione; nessuna pulizia storica respinta ritentata.

Pulizia finale della remediation: il controllo automatico ha respinto la rimozione dei soli build/tmp/remediation_docs.py, build/tmp/__pycache__/remediation_docs.cpython-313.pyc e della directory cache se vuota, con motivo blocked by policy. Nessuna rimozione eseguita o ritentata. Due file inventariati con dimensioni/SHA-256 in build/reports/tracker-remediation-20261009/cleanup-blocked-inventory.json; aggiunti a RES-24. I report restano conservati.

Controlli documentali finali: 21 Markdown renderizzati, 182 link locali validi; 55 file modificati/nuovi UTF-8 senza BOM. Tracker con quattro ID unici, riferimenti/dipendenze validi e nessun AUD completato; git diff --check superato. Diff finale rivisto, nessun lockfile/generated/vendor modificato. La sola pulizia respinta resta in RES-24.

## Passaggio di sessione della remediation — 9 ottobre 2026

Commit e push su main richiesti dall’utente. Consegna delle correzioni AUD-45–51, regressioni, script di verifica e documentazione aggiornata. Prima della consegna, fetch origin/main riuscito e main allineato al remoto (0 commit avanti/indietro); controllo whitespace superato. SHA del commit ed esito del push verificabili in Git.

Tracker aggiornato: nessun task runtime aperto, soli RES-13/19/23/24 (3 P2, 1 P3), prossimo lavoro RES-19. Le verifiche già concluse restano 643 test automatici e quattro prove PDF native, con build APK riuscite; nessuna suite ripetuta per il solo passaggio documentale. Validati nuovamente JSON, riferimenti del tracker e diff prima del commit.

Evidenze ignorate da Git conservate in build/reports/tracker-remediation-20261009 e nelle directory storiche: disponibili nel checkout, escluse dal commit. Preservare dati, backup, media, chiavi e app/demo. Nessuna pulizia respinta ritentata; RES-24 comprende anche helper e bytecode della remediation. La fixture sintetica mancante non richiede recupero, come confermato dall’utente. Questa autorizzazione al commit/push riguarda la sola consegna corrente.

## Secondo audit del repository e aggiornamento tracker — 9 ottobre 2026

Inventariati 398 file versionati con `git ls-files` e ricerca dei punti incompleti/consumatori con `rg`; revisione di dominio, persistenza, scambio, media, UI, documenti, test, build e rilascio. Registrati **AUD-52–62 OPEN**: 2 P1, 8 P2 e 1 P3. Conservati senza modifiche RES-13/19/23/24: totale **15 attività, 2 P1 / 11 P2 / 2 P3**. `activeTask` impostato a `null`; prossime priorità AUD-52 e AUD-53. Nessuna dipendenza obbligatoria individuata. Dettaglio ed esiti nel [secondo audit](docs/repo-residuals-2026-10-09.md#secondo-audit-completo--9-ottobre-2026).

Il P1 AUD-52 è riprodotto: cancellazione del piano con annotazione consentita, export/import rifiutato con `INVALID_ANNOTATION_AREA`. Il P1 AUD-53 è da codice: salvataggio del nuovo progetto Android distinto dall’applicazione della password e ID nuovo alla riprova; nessun guasto nativo provocato. Le evidenze degli altri rilievi distinguono casi riprodotti, rischio da codice e verifica parziale. Fonti primarie Microsoft, W3C e Oracle consultate il 9 ottobre e collegate nel report.

Comando mirato eseguito durante l’analisi:

```powershell
.\gradlew.bat :shared:exchange:test --tests '*PackageSerializerTest*' --tests '*ContractVersionTest*' --tests '*PasswordHasherTest*' :mobile:app:testDebugUnitTest --tests '*ProjectCommandTest*' --tests '*ProjectRepositoryTest*' --no-parallel --max-workers=1
```

**BUILD SUCCESSFUL in 1m 21s**: **46 test**, 12 exchange e 34 Android JVM, zero fallimenti/errori/saltati. Probe sintetici in memoria sui casi riprodotti; esiti essenziali conservati nel report versionato. Nessuna modifica/import di dati utente. I 643 test e le quattro prove PDF native della precedente remediation restano evidenze storiche distinte; le prove verdi non chiudono i nuovi rilievi.

Aggiornati tracker, report, README, piano prodotto e guida di verifica. Conservato lo storico AUD-45–51. Corretto il riepilogo RES-13 del piano: riavvio fisico normale completato l’8 ottobre con boot ID diverso e cinque scenari SQLCipher/Keystore verificati; arresto forzato e perdita improvvisa di alimentazione restano da collaudare. Compatibilità e fixture utili mantenute. Nessuna nuova suite generale per sole modifiche documentali; nessuna correzione runtime, prova nativa, pulizia, commit o push in questo intervento.

Verifica documentale finale dell’aggiornamento: parsing JSON riuscito, **15 ID univoci**, priorità **2/11/2**, stati/evidenze coerenti, riferimenti esistenti e dipendenze valide senza cicli. RES-13/19/23/24 confrontati con HEAD e invariati. **21 Markdown, 221 link locali validi**, UTF-8 senza BOM; diff completo rivisto e `git diff --check` superato. Modificati soltanto sei file di tracker/documentazione, senza file temporanei nuovi, sorgenti, fixture o dipendenze modificati. Report XML dell’analisi ricontrollati: 12 test exchange e 34 Android JVM, zero fallimenti/errori/saltati; nessuna suite rieseguita.

## AUD-52 completato — 9 ottobre 2026

La cancellazione condivisa rifiuta anche i piani referenziati da annotazioni, senza mutare il progetto. PackageSerializerTest verifica rifiuto, conservazione della nota, cancellazione del piano vuoto e riapertura .ofam semplice/protetta. Baseline verde; finale .\gradlew.bat :shared:exchange:test --tests *PackageSerializerTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 5s, nessun fallimento. Prima compilazione della nuova fixture corretta per usare label e coordinate obbligatorie. Fonte ufficiale per verifica mirata: [Gradle JVM testing](https://docs.gradle.org/current/userguide/java_testing.html), consultata il 9 ottobre. Nessun collaudo nativo richiesto per il controllo condiviso.

Rimosso dal tracker dopo verifica; 14 attività aperte: 1 P1 / 11 P2 / 2 P3.

## AUD-53 completato — 9 ottobre 2026

ProjectRepository.createProject prepara il verificatore e salva progetto/inventario/protezione nella stessa transazione Room. Il wizard conserva ID alla riprova e blocca invii simultanei; un ID già esistente viene rifiutato. Baseline ImportedProtectionTest: BUILD SUCCESSFUL in 37s. Finale .\gradlew.bat :mobile:app:testDebugUnitTest --tests *ProjectCreationTest* --tests *ImportedProtectionTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 21s, 9 test, zero fallimenti/errori/saltati. Coperti creazione semplice/protetta, password errata, guasto dopo inserimento progetto, rollback verificatore/inventario, annullamento prima della transazione e riprova senza duplicati. Una prima fixture riutilizzava un ID sede tra progetti ed è stata corretta: il rifiuto ownership esistente resta intatto. Fonte: [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt), consultata il 9 ottobre. Prova Room JVM distinta da SQLCipher nativo; messaggi/focus del wizard protetto restano nella matrice RES-19.

Rimosso dal tracker dopo verifica; 13 attività aperte: 0 P1 / 11 P2 / 2 P3.

## AUD-54 completato — 9 ottobre 2026

Il wizard Windows conserva lo stato in DesktopAppState durante la rimozione temporanea del dialogo per I/O; chiude e azzera i campi solo dopo commit riuscito. Rinomina Android usa EditSave: errore nel dialogo, campi conservati e callback ignorato dopo uscita dalla composizione. Baseline mirata verde; finale .\gradlew.bat :pc:app:test --tests *ProjectWizardSaveTest* :mobile:app:testDebugUnitTest --tests *ProjectCommandTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 26s. Prova Compose con storage inaccessibile e riprova protetta singola, prove Room di guasto/riprova e risultato tardivo. La compilazione Android della baseline includeva già il callback di rinomina appena aggiornato; la baseline Desktop era precedente. Collaudo EXE/focus e dialogo nativo Android restano in RES-23/19.

Rimosso dal tracker dopo verifica; 12 attività aperte: 0 P1 / 10 P2 / 2 P3.

## AUD-55 completato — 9 ottobre 2026

Import rifiuta duplicati manifest/progetto/media, ID manifest discordante, payload plain/encrypted conflittuali, metadati crittografici in pacchetti semplici e progetto protetto in ZIP semplice. Ogni rifiuto è strutturale e chiude staging. Conservata compatibilità utilizzata: cifratura richiesta esplicitamente su progetto non protetto e vecchi allegati non cifrati dentro progetto cifrato restano ammessi. Baseline PackageSerializerTest/StagedPayloadTest verde; finale .\gradlew.bat :shared:exchange:test --tests *PackageMetadataTest* --tests *PackageSerializerTest* --tests *StagedPayloadTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 6s, zero fallimenti/errori/saltati. Nuove fixture includono duplicati reali, staging già presente, controlli validi semplici/protetti; nessun accesso a storage utente.

Rimosso dal tracker dopo verifica; 11 attività aperte: 0 P1 / 9 P2 / 2 P3.

## AUD-56 completato — 9 ottobre 2026

Rimossi i fallback dei cinque mapper: JSON VLAN/LAG illeggibile ed enum sconosciuti interrompono la lettura, senza costruire valori vuoti/predefiniti. Il comando edit segnala il guasto e non salva; righe e stato visibile precedente restano intatti. Baseline 37 prove verdi; finale .\gradlew.bat :mobile:app:testDebugUnitTest --tests *CorruptStoredDataTest* --tests *ProjectCommandTest* --tests *ProjectRepositoryTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 25s, 39 test, zero fallimenti/errori/saltati. Coperti i due JSON, tutti i 29 campi enum interessati, valori validi, riprova dopo correzione ed errore del comando. La prima fixture verificava la colonna legacy del modello mentre configurationJson corrente prevale; corretta per esercitare il formato legacy effettivamente letto. Fonte: [Kotlin enumValueOf](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/enum-value-of.html), consultata il 9 ottobre. Nessuna migrazione schema; collaudo messaggi/focus Android resta RES-19.

Rimosso dal tracker dopo verifica; 10 attività aperte: 0 P1 / 8 P2 / 2 P3.

## AUD-57 completato — 9 ottobre 2026

ModelValidator riusa il controllo CIDR IPv4 dei form e controlla subnet.vlanId e scope VLAN/subnet per progetto/sede/apparato. Riferimenti inesistenti sono strutturali; destinazione SITE/DEVICE non rilevata resta documentale, con messaggi it/en/es. Baseline ModelValidatorTest/EntityFormsTest verde; verifica con NetworkValidationTest e DemoSeedTest: BUILD SUCCESSFUL in 34s; regressioni scope ampliate: BUILD SUCCESSFUL in 3s. Conservati limiti /0 e /32, incompletezza ammessa e demo valido; import semplici/protetti invalidi non restituiscono pacchetti. Fonte [RFC 4632](https://www.rfc-editor.org/rfc/rfc4632), consultata il 9 ottobre. Emersi e tracciati AUD-63 (VLAN referenziate: blocco richiesto dall’utente) e AUD-64 (scope nelle operazioni sede/apparato), da completare separatamente.

Rimosso dal tracker dopo verifica; 11 attività aperte: 0 P1 / 9 P2 / 2 P3.

## AUD-63 completato — 9 ottobre 2026

Applicata la decisione utente: ProjectEdits.deleteVlan rifiuta la cancellazione quando una subnet usa la VLAN, con messaggio it/en/es. Windows visualizza il rifiuto senza chiamare il salvataggio; Android lo riceve nel comando edit senza mutazione. Baseline Desktop ProjectEdits/FailedNetworkSave verde (28s); finale shared/exchange NetworkValidationTest + pc VlanDeletionUiTest + compileDebugKotlin: BUILD SUCCESSFUL in 13s. Android ProjectCommandTest: BUILD SUCCESSFUL in 20s. Verificati dati invariati, errore visibile, riprova dopo scollegamento esplicito della subnet, salvataggio singolo e .ofam semplice/protetto valido. Nessuna modifica automatica delle reti. Rimane distinto il collaudo visivo nativo RES-19/23.

Rimosso dal tracker dopo verifica; 10 attività aperte: 0 P1 / 8 P2 / 2 P3.

## AUD-58 completato — 9 ottobre 2026

XLSX applica ST_Xstring al testo delle celle: protegge underscore iniziali delle sequenze letterali, codifica controlli/XML non validi e CR, conserva LF/tab, Unicode e spazi tramite xml:space=preserve. Originali e tipi numerici invariati. Baseline documentale verde; finale XlsxTextTest/LocalizedExportsTest/DocumentSelectionTest/DemoXlsxPathsTest: BUILD SUCCESSFUL in 6s, 7 test, zero fallimenti/errori/saltati. Tutte le parti XML parsate nelle tre lingue e testo ricostruito in un solo passaggio. Due tentativi iniziali della fixture cercavano erroneamente altezza rack nei fogli; dopo segnalazione e lettura del generatore, la prova numerica usa L2 (numero porte). Fonti consultate il 9 ottobre: [Microsoft ST_Xstring](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-oi29500/d34ae755-c53f-4a44-a363-c6dd3ee018a4) e [W3C XML 1.0](https://www.w3.org/TR/xml/#charsets). Apertura Excel nativa non eseguita.

Rimosso dal tracker dopo verifica; 9 attività aperte: 0 P1 / 7 P2 / 2 P3.

## AUD-59 completato — 9 ottobre 2026

Le tile Windows verificano 256x256 con ImageReader.getWidth/getHeight prima di read(0), con stream in memoria e reader.dispose in finally. Conservato limite 2 MiB anche per fetch iniettato. Baseline DesktopDocumentAndCartographyTest verde; finale TileDecodeTest + suite precedente: BUILD SUCCESSFUL in 5s, 6 test, zero fallimenti/errori/saltati. Sorgente HTTP locale della sessione chiusa al termine: tile valida, PNG 4096x4096 sotto il limite compresso, malformato e risposta oltre 2 MiB. Un PNG con solo header sovradimensionato verifica il rifiuto dimensioni prima dei pixel. Fonte [Oracle ImageReader](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageReader.html), consultata il 9 ottobre. Nessun download dal servizio pubblico né OOM provocato.

Rimosso dal tracker dopo verifica; 8 attività aperte: 0 P1 / 6 P2 / 2 P3.

## AUD-60 completato — 9 ottobre 2026

Planimetrie richieste ma illeggibili o senza payload interrompono PDF/stampa con errore; rimosse le due conversioni silenziose in sfondo assente. Documento/stream chiusi anche al fallimento. Sfondo assente, escluso dal filtro o sezione disattivata restano validi. Baseline DeliveryPdfTest/ReportPdfTest/FloorMediaTest verde; regressioni FloorPlanPdfFailureTest includono immagini/PDF corrotti, payload mancante dopo svuotamento del cache isolato, immagine e PDF validi, filtri e assenza. Dieci test, zero errori/fallimenti/skips: `:pc:app:test --tests '*FloorPlanPdfFailureTest*' --tests '*DeliveryPdfTest*' --tests '*ReportPdfTest*' --tests '*FloorMediaTest*' --no-parallel --max-workers=1`, BUILD SUCCESSFUL in 16s. Corretti un errore di compilazione della fixture e una prima simulazione che manteneva il payload nel cache. Fonti PDFBox 3 consultate il 9 ottobre. Interazione nativa stampa/focus resta RES-23.

Rimosso dal tracker dopo verifica; 7 attività aperte: 0 P1 / 5 P2 / 2 P3.

## AUD-61 completato — 9 ottobre 2026

Rimossa l’euristica LEGACY_DEFAULT e la costante orfana: ogni punto intermedio salvato è una piega. Default a due punti ancora rettilineo. Baseline MapSceneTest verde; dopo modifica MapSceneTest e PackageSerializerTest verdi, DesktopStorageTest verde alla riprova con stato di protezione esplicito, ProjectRepositoryTest verde dopo correzione import della fixture (BUILD SUCCESSFUL in 21s). Verificati coordinate .2/.5/.8, estremi spostati e risalvataggio, working copy semplice/protetta, Room e .ofam semplice/protetto; stesso contenuto e ID. Nessuna migrazione Room/.ofam. Documentazione mappa aggiornata.

Rimosso dal tracker dopo verifica; 6 attività aperte: 0 P1 / 4 P2 / 2 P3.

## AUD-64 completato — 9 ottobre 2026

Policy conservativa confermata dall’utente: guard prima di cancellazione sede, cestino/sostituzione apparato e fusione di entrambi gli apparati quando referenziati da ambito VLAN/subnet. Nessuna rete modificata automaticamente, nessun record di cestino creato al rifiuto. Errori it/en/es; gestione rifiuto in inventario/mappa Windows e dialoghi sostituzione/fusione. NetworkScopeRetentionTest verifica entrambe le sorgenti VLAN/subnet, entrambi i dispositivi, sede vuota referenziata, target estraneo eliminabile, rimozione esplicita, ripristino e .ofam semplice/protetto. ProjectCommandTest verifica errori, Room/inventario/cestino invariati e riprova. Gradle mirato: BUILD SUCCESSFUL in 29s. NetworkScopeUiTest: messaggio Compose di cancellazione, zero callback di save/trash, riprova; fusione Windows semplice/protetta preserva file e undo/cestino, retry/undo riusciti: BUILD SUCCESSFUL in 7s. Baseline ha rilevato vecchia aspettativa di cancellazione VLAN referenziata in ProjectEditsTest; sostituita con rifiuto e disconnessione esplicita conformi ad AUD-63. Documentazione dominio aggiornata. Focus/matrice nativa resta RES-19/23.

Rimosso dal tracker dopo verifica; 5 attività aperte: 0 P1 / 3 P2 / 2 P3.

## AUD-62 completato — 9 ottobre 2026

Ricerca completa dei consumatori conferma che DeviceModelSerializer era usato soltanto dal proprio test: entrambi eliminati. Modelli e fixture utili restano nel flusso del progetto. Baseline PackageSerializerTest/DeviceModelSerializerTest verde (BUILD SUCCESSFUL in 3s); dopo rimozione PackageSerializerTest e tutte le prove Demo verdi (BUILD SUCCESSFUL in 45s). Regressione .ofam semplice/protetto confronta l’intero progetto con modello, ID, metadati, template porte, PoE, hardware/layout/override e campi extra. Nessun consumer applicativo residuo, contratto/schema invariato. Documentazione dominio aggiornata; fonti dello scambio ZIP già consultate.

Rimosso dal tracker dopo verifica; 4 attività aperte: 0 P1 / 3 P2 / 1 P3.

## Chiusura della remediation AUD-52–64 — 9 ottobre 2026

Completati e rimossi tutti i 13 task del secondo audit, inclusi AUD-63/64 emersi dalla validazione rete. Tracker con `remainingTasks=[]`; restano RES-13/19/23/24, **4 residui (0 P1 / 3 P2 / 1 P3)**. Documentazione dominio, workflow, export, mappa e linee guida allineata; fonti primarie nelle sezioni pertinenti.

Verifica completa: **679 test, zero fallimenti/errori/saltati**: core 130, exchange 115, Windows 292, Android JVM 142. Core/exchange verdi nella prima esecuzione completa. Questa aveva quattro fallimenti Windows: tre casi della stessa fixture interop con numero VLAN al posto dell’UUID, e un selettore Compose che trovava due messaggi ora visibili. Fixture corretta con `vlan.id`, assert del messaggio nel dialogo specifico, diagnostica spostata negli assert; nessuna validazione indebolita. Riprova mirata verde in 33s, incluso wizard protetto Android con guasto, invii rapidi e riprova senza duplicati.

Finale `.\gradlew.bat :pc:app:test :mobile:app:testDebugUnitTest :pc:app:assemble :mobile:app:assembleDebug --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 7m 2s**. Build Desktop e APK debug Android riuscite; nessuna installazione, rigenerazione portable o qualificazione hardware. Report XML iniziali falliti e finali conservati in `build/reports/tracker-followup-20261009`, riepilogo `verification.json` con hash APK.

RES-19/23 includono esplicitamente i controlli nativi di wizard, rinomina, rifiuti di riferimenti rete e sfondi PDF. RES-13 mantiene gli scatti rinviati e i limiti hardware; RES-24 conserva tutte le pulizie storiche respinte, senza ritentativi. Nessun commit/push richiesto o eseguito.
Controlli conclusivi: 21 Markdown e 221 collegamenti locali validi; 54 file modificati/nuovi verificati UTF-8 senza BOM. JSON, ID, riferimenti e dipendenze del tracker validi; `git diff --check` superato. Rimosso soltanto l’helper creato per questa sessione `build/tmp/task_updates_20261009.py`; nessuna pulizia storica ritentata. Diff completo rivisto, nessun lockfile/vendor/migrazione applicata modificato. Evidenza `build/reports/tracker-followup-20261009/document-validation.json`.

## Passaggio di sessione su main — 9 ottobre 2026

L’utente ha richiesto successivamente salvataggio, commit e push per cambiare sessione. Consegna dei 13 task AUD-52–64, regressioni e documentazione aggiornata su `main`; prima del commit, `git fetch origin main` e confronto con `origin/main` confermano 0 commit di divergenza dalla base `ff11653`. SHA della consegna ed esito del push sono verificabili nel log Git; riscontro locale in `build/reports/tracker-followup-20261009/handoff.json`.

Ripartenza dai soli RES-13/19/23/24: nessun task attivo o task di audit ancora aperto. Restano validi i 679 test e le build già registrati sopra; il passaggio di sessione modifica soltanto la documentazione e non richiede una nuova esecuzione runtime. Preservati report, dati, backup e chiavi; nessuna installazione, rigenerazione portable o pulizia storica. Il collaudo nativo e hardware rimane esplicitamente aperto nei residui.

## RES-23 — Wizard Windows semplice, errore e riprova verificati il 9 ottobre 2026

EXE compilato dal codice corrente in output isolato con init script: `:pc:app:createDistributable -I build/tmp/res23-native-20261009/native-output.init.gradle --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 20s**. Build e distribuzione entrambe nello scratch nuovo; nessuna rigenerazione del runtime storico con dati o di `dist`.

Wizard “Nuovo sito”, tema chiaro, finestra 1348×854, password vuota: guasto reale del percorso `data/projects` sul solo storage sintetico conserva il dialogo e mostra l’errore completo. Shift+Tab e Invio tornano al piano precedente; sede e piano conservati. Ripristinata la cartella sintetica, riprova dalla stessa bozza riuscita: un solo pacchetto .ofam, nome/cliente/sede/piano verificati nel JSON salvato. Variante protetta, altri temi/dimensioni e restante matrice EXE non verificati. Nessun difetto emerso, modifica runtime o suite ripetuta.

Evidenze PNG, JSON, risultato e log in `build/reports/res23-native-20261009`; init script copiato nel report. Scenario semplice completato e rimosso dal lavoro aperto di RES-23; residuo ancora parziale. EXE chiuso con Alt+F4. Pulizia del solo nuovo scratch respinta dal controllo automatico prima dell’esecuzione, motivo `blocked by policy`; nessun ritentativo. 1058 file con dimensioni/SHA-256 in `cleanup-blocked-inventory.json`; nuova risorsa tracciata in RES-24.

## RES-19 — Avvio guidato Android, 9 ottobre 2026

Moto g86 API 36 collegato e autorizzato ADB. Configurazione letta: 1220×2712 px, densità fisica 450 dpi, testo 1,0, tema scuro. L’utente sceglie di proseguire su Android e dichiara TalkBack non disponibile; lettura ADB `enabled_accessibility_services` restituisce `null`. Nessun annuncio/audio/focus TalkBack verificato. Le prove manuali con il servizio restano esplicite in RES-19, secondo la [guida Android ufficiale ai test di accessibilità](https://developer.android.com/guide/topics/ui/accessibility/testing), consultata il 9 ottobre. Nessuna nuova suite automatica necessaria in assenza di difetti.

Preparazione Android guidata: prima di `adb install -r` conservati APK installato e snapshot dei dati applicativi, esclusi cache/code_cache, in `build/reports/res19-guided-20261009`. Backup per file, senza garanzia transazionale o power-loss. Installazione **Success**; SHA-256 del nuovo APK installato `5274a48533318f266239492f6660c0a6726fe5cee267de1948f227722034e1f1`, uguale alla build debug già verificata. Avvio MainActivity **Status: ok**; Demo Comune ancora visibile dopo il caricamento. Aperta solo una bozza non salvata `RES19 Rotation Draft`: titolo, nome e Avanti visibili in verticale con tastiera. Rotazione fisica richiesta all’utente, ancora in attesa; questo preparativo non chiude la matrice RES-19. Fonte operativa [ADB ufficiale](https://developer.android.com/tools/adb), consultata il 9 ottobre.


## RES-23 — Creazione VLAN nel vero EXE, 9 ottobre 2026

Telefono non disponibile ora, dichiarato dall’utente: RES-19 e RES-13 restano pendenti. Ripreso il runtime Windows sintetico già compilato in `build/tmp/res23-native-20261009`, finestra 1348×854, tema chiaro, progetto semplice. Backup iniziale conservato nel nuovo report; nessun nuovo build o dato reale coinvolto.

Creazione VLAN 20: blocco reale del solo pacchetto mediante FileStream leggibile senza condivisione Delete, errore visibile, bozza conservata e nessuna VLAN prima del commit. Shift+Tab/Tab raggiungono Annulla/Salva; Invio ripete il guasto e, dopo il rilascio dell’handle della sessione, salva una sola VLAN con nome/ID/ambito verificati nel JSON. SHA-256 identico prima/dopo la ripetizione controllata del guasto. La prima apertura riscrive il contenitore ZIP per il flusso `DesktopAppState.open`; contenuto del progetto confrontato e identico al backup.

Evidenze: `build/reports/res23-network-native-20261009/vlan-creation-result.json`, pacchetti e screenshot. Creazione VLAN a questa configurazione completata; matrice RES-23 ancora parziale, senza dedurre altri temi/dimensioni/editor o protezione. Nessun problema emerso, nessuna suite automatica ripetuta. Fonte operativa [FileShare](https://learn.microsoft.com/en-us/dotnet/api/system.io.fileshare?view=net-10.0), consultata il 9 ottobre.


### RES-23 — Modifica VLAN, guasto/riprova/undo verificati

Sul medesimo EXE semplice/chiaro 1348×854, nome della VLAN modificato con mouse/tastiera. Guasto reale senza condivisione Delete: errore nel pannello, nuova bozza conservata, pacchetto SHA-256 invariato. Dopo rilascio, Invio salva il nuovo nome mantenendo ID e ambito. Annulla nella barra progetto ripristina l’intero progetto precedente, confrontato nel JSON. Evidenze `vlan-edit-result.json`, pacchetti e screenshot in `build/reports/res23-network-native-20261009`. Creazione/modifica VLAN con guasto/riprova e undo della modifica completati per questa configurazione; altri editor, temi/dimensioni e protezione restano in RES-23. Nessun difetto emerso.


### RES-23 — Creazione subnet e picker VLAN verificati

Medesimo EXE semplice/chiaro 1348×854: creata subnet 192.0.2.0/24 associata alla VLAN dal picker reale. Al guasto la bozza e la scelta restano conservate, il picker è raggiungibile con lo scorrimento del pannello e SHA-256 del pacchetto resta identico. Invio dopo rilascio del blocco salva una sola subnet con CIDR e riferimento all’UUID VLAN corretti nel JSON. Evidenze `subnet-creation-result.json`, pacchetti e screenshot nello stesso report. Scenario completato per questa configurazione; modifica/undo subnet e altri casi restano aperti.


### RES-23 — Modifica subnet, guasto/riprova/undo verificati

Nome subnet modificato nella medesima configurazione. Guasto reale: errore leggibile, bozza e scelta VLAN conservate/raggiungibili con scorrimento, SHA-256 del pacchetto invariato. Dopo rilascio, Invio salva conservando UUID subnet/VLAN; Annulla ripristina l’intero progetto precedente verificato nel JSON. Evidenze `subnet-edit-result.json`, pacchetti e screenshot. Creazione/modifica subnet con guasto/riprova e undo della modifica completati per semplice/chiaro 1348×854. Altri editor e configurazioni restano in RES-23; nessun difetto emerso.


### RES-23 — Creazione WAN verificata

Medesimo EXE semplice/chiaro 1348×854: connessione WAN senza apparati locali/remoti, esplicitamente Non nel progetto. Al guasto nome/tipo conservati, errore leggibile e pacchetto SHA-256 invariato rispetto alla baseline dopo undo subnet. Invio dopo rilascio salva una sola connessione. Screenshot e pacchetto `wan-created.ofam` nello stesso report; configurazioni con apparati e VPN restano aperte.


### RES-23 — Modifica WAN/VPN e undo visivi

Picker reale cambia WAN in VPN; guasto conserva scelta/nome e SHA-256 del pacchetto. Invio alla riprova mostra VPN; Annulla ripristina WAN. Confronto completo del progetto prima/dopo undo superato. JSON finale conferma type=VPN e ID conservato; evidenza wan-result.json. La prima verifica puntuale usava connectionType invece di type ed è stata corretta dopo lettura del JSON; risultato finale superato. Scenario completato nella stessa configurazione; varianti con apparati e altre configurazioni restano aperte.


### RES-23 — Creazione campo extra verificata

Medesimo EXE semplice/chiaro 1348×854: chiave e valore sintetici, target Progetto e classificazione Condivisibile. Guasto reale conserva bozza/target e SHA-256 del pacchetto; Invio dopo rilascio salva una sola voce. Screenshot e `extra-created.ofam` nello stesso report. Scenario completato per questa configurazione; modifica/undo e altri target/tipi restano aperti.


### RES-23 — Modifica campo extra e undo verificati

Valore modificato, guasto reale senza mutazione del pacchetto e bozza conservata. Invio dopo rilascio salva il nuovo valore mantenendo ID/target/chiave/tipo/classificazione; Annulla ripristina l’intero progetto precedente confrontato nel JSON. Evidenza extra-result.json e screenshot nello stesso report. Scenario completato per campo Progetto/Testo/Condivisibile nella configurazione corrente; altri target/tipi e configurazioni restano aperti.


### RES-23 — Creazione cavo verificata

Cavo rame senza estremità, medesimo EXE semplice/chiaro 1348×854: guasto reale conserva nome/tipo e SHA-256 del pacchetto. Invio dopo rilascio esegue Aggiungi dalla stessa bozza, singolo cavo visibile. Evidenza `cable-created.ofam` e screenshot nello stesso report. Modifica/undo, cablaggio con estremità e altre configurazioni restano aperti.


### RES-23 — Modifica cavo e undo verificati

Nome cavo modificato, guasto reale conserva bozza e SHA-256 del pacchetto. Invio dopo rilascio salva mantenendo ID/tipo/mezzo; Annulla ripristina l’intero progetto precedente confrontato nel JSON. Evidenza cable-result.json e screenshot. Scenario completato per cavo rame senza estremità nella configurazione corrente; collegamenti alle porte, altre varianti e configurazioni restano aperti.


### RES-23 — Creazione interfaccia logica verificata

Switch SW-01 con 28 porte creato tramite preset nativo nel solo progetto sintetico; backup switch-baseline.ofam. Interfaccia scelta dal picker apparato reale, nome compilato, nessun IP/VLAN assegnato. Guasto reale conserva apparato/nome e SHA-256 del pacchetto; Invio dopo rilascio salva una sola interfaccia sullo switch. Screenshot e interface-created.ofam nello stesso report. Configurazione corrente semplice/chiaro 1348×854; modifica/undo e altre varianti restano aperti.


### RES-23 — Modifica interfaccia e undo verificati

Checkbox L3 disattivata, guasto reale conserva bozza/checkbox e SHA-256 del pacchetto. Campi inferiori, checkbox e note raggiungibili con scorrimento. Invio dopo rilascio salva L2 mantenendo ID interfaccia/apparato; Annulla ripristina l’intero progetto precedente confrontato nel JSON. Evidenza interface-result.json e screenshot. Scenario completato nella configurazione corrente; indirizzi, VLAN e altre varianti/configurazioni restano aperti.


### RES-23 — Creazione configurazione apparato verificata

Altri moduli rende raggiungibile Configurazioni apparati. Picker SW-01, titolo e testo sintetici compilati. Guasto reale conserva apparato/titolo/testo e SHA-256 del pacchetto; Invio dopo rilascio salva una sola configurazione. Screenshot e config-created.ofam nello stesso report. Scenario completato per semplice/chiaro 1348×854; modifica/undo e altre configurazioni restano aperti.


### RES-23 — Modifica configurazione e undo verificati

Testo modificato, guasto conserva bozza e SHA-256 del pacchetto. Invio alla riprova salva conservando tutti gli altri campi, inclusi ID/apparato/titolo/data cattura; Annulla ripristina l’intero progetto precedente nel JSON. Evidenza config-result.json e screenshot. Scenario completato nella configurazione corrente, altre varianti/configurazioni restano aperte.


### RES-23 — Creazione permutazione verificata

Picker porta reale, SW-01/P1 e passaggio sconosciuto senza porta B. Guasto conserva scelta/checkbox e SHA-256 del pacchetto; Invio dopo rilascio salva una sola permutazione. Screenshot e mapping-created.ofam nello stesso report. Scenario completato per semplice/chiaro 1348×854; modifica/undo, due porte/patch panel e altre configurazioni restano aperti.


### RES-23 — Modifica permutazione e undo verificati

Checkbox passaggio sconosciuto disattivata; guasto conserva bozza, scelta porta e SHA-256 del pacchetto. Invio alla riprova salva conservando ID permutazione/porta; Annulla ripristina l’intero progetto precedente nel JSON. Evidenza mapping-result.json e screenshot. Scenario completato per porta P1 singola nella configurazione corrente; due porte/patch panel e altre configurazioni restano aperti.

### RES-23 · alimentazione semplice, creazione nativa (9 ottobre)

Nel runtime EXE isolato chiaro 1348×854: picker SW-01, linea RES23 Native Power, Primaria (A), 230 V. Scrittura realmente bloccata: errore visibile, bozza e hash del pacchetto conservati. Rilasciato il solo helper della sessione, Invio salva una sola linea con UUID apparato corretto. Evidenze: build/reports/res23-network-native-20261009/power-created.ofam e schermate power-*. Varianti UPS/PDU e altre configurazioni restano aperte.

### RES-23 · alimentazione semplice, modifica e undo nativi (9 ottobre)

Picker Primaria (A)→Secondaria (B), errore reale con bozza/hash invariati, riprova da Invio: UUID linea/apparato conservati. Undo della modifica ripristina integralmente project.json del pacchetto creato. PASS in build/reports/res23-network-native-20261009/power-result.json; varianti UPS/PDU e altre configurazioni aperte.

### RES-23 · PoE semplice, creazione nativa (9 ottobre)

SW-01/P1 selezionata nel picker reale, Eroga (PSE)/802.3at senza potenza assegnata: errore reale mantiene bozza e hash, riprova da Invio salva una sola mappatura. Evidenze poe-created.ofam e poe-create-failed-* in build/reports/res23-network-native-20261009. Budget/override hardware e varianti restano aperti.

### RES-23 · PoE semplice, modifica e undo nativi (9 ottobre)

Standard 802.3at→802.3af, guasto reale con bozza/hash invariati, riprova da Invio, UUID mappatura/porta preservati. Undo ripristina integralmente project.json. PASS in build/reports/res23-network-native-20261009/poe-result.json. Varianti budget/override hardware e configurazioni diverse aperte.

### RES-23 · badge semplice, creazione nativa (9 ottobre)

Badge di progetto RES23 Native Badge/Etichetta libera: errore reale conserva bozza e hash, riprova da Invio salva un solo badge e target UUID corretto. Evidenze badge-created.ofam e badge-create-failed-* nella cartella rete. Target diversi, badge derivati e resa nei documenti non coperti.

### RES-23 · badge semplice, modifica e undo nativi (9 ottobre)

Categoria Etichetta libera→Problema aperto, guasto reale con bozza/hash invariati e riprova da Invio; UUID badge/target preservati. Undo ripristina integralmente project.json. PASS in build/reports/res23-network-native-20261009/badge-result.json. Target diversi/derivati/resa documentale e configurazioni diverse aperti.

## Consolidamento della matrice nativa Windows · 9 ottobre 2026

Completati e rimossi dal lavoro aperto di RES-23 i casi base degli 11 editor elencati nella [matrice Windows](docs/testing/windows-native-matrix.md): 22 scenari guasto/riprova e 11 undo della modifica. La matrice distingue ogni perimetro dalle varianti ancora pendenti. Rianalisi dei 33 pacchetti creato/modificato/undo: singolo record, unica proprietà attesa modificata, altri campi del record conservati e intero progetto dopo undo identico; 11 PASS in build/reports/res23-network-native-20261009/matrix-result.json. Nessun difetto applicativo, codice modificato o suite generale ripetuta.

EXE della sola sessione chiuso con Alt+F4, assenza finestra confermata; tutti gli helper di blocco rilasciati. Un controllo del processo helper usava erroneamente un pattern wildcard con parentesi quadre: corretto con confronto letterale prima di chiudere il solo helper verificato della sessione. Nessuna pulizia respinta ritentata. Inventario runtime aggiornato: 1058 file con dimensioni/SHA-256 in cleanup-preserved-inventory.json, entrambi i report nativi e backup conservati in RES-24.

Tracker mantiene 4 residui parziali, 0 P1 / 3 P2 / 1 P3, senza task completati. Android: telefono non disponibile per dichiarazione utente, rotazione fisica non eseguita e TalkBack ancora pendente. Disponibilità per protezione Windows/stampa fisica chiesta, senza risposta; nessun esito dedotto. Prossimi casi nella tabella aperta della matrice. Nessun commit/push.

Controlli documentali finali: 4 file UTF-8 senza BOM, 40 link locali validi, ID/riferimenti/dipendenze e activeTask del tracker coerenti, zero completati nel tracker, git diff --check superato. Markdown elaborato con ConvertFrom-Markdown; evidenza build/reports/res23-network-native-20261009/document-validation.json. Diff rivisto; modifiche solo a tracker/documentazione e nuova matrice, evidenze locali conservate.

## Consegna della matrice nativa · 9 ottobre 2026

L’utente richiede salvataggio, commit e push su main per cambiare sessione. Tracker preparato per la ripresa: activeTask nullo, remainingTasks vuoto, RES-13/19/23/24 ancora parziali. Ripartire dalla tabella aperta in [matrice Windows](docs/testing/windows-native-matrix.md), senza ripetere i casi base conclusi in assenza di nuovi problemi. Telefono non disponibile; TalkBack, rotazione fisica, protezione guidata e stampa fisica non dedotti dalle prove concluse.

Consegna di tracker, documentazione di verifica, roadmap e matrice; nessuna modifica runtime. Baseline Git prima del commit: main = origin/main = 7a2fe1f56cb910896954b579b451d97dcfd5d1ce, divergenza 0/0 dopo git fetch origin main. Restano validi gli esiti nativi 22 guasto/riprova e 11 undo e i controlli documentali; nessuna nuova suite necessaria per il solo passaggio di consegne.

Report, screenshot, pacchetti e backup in build/reports restano conservati localmente e ignorati da Git: non disponibili in un checkout nuovo. Runtime della sessione chiuso, helper rilasciati e inventario aggiornato conservato; nessuna pulizia storica respinta ritentata. SHA della consegna, push e allineamento remoto saranno verificati nel log Git; riscontro locale in build/reports/res23-network-native-20261009/handoff.json.

## Aggiornamento demo e installazione Android · 10 ottobre 2026

Rigenerata la fixture dal seed corrente (revisione sintetica del 9 ottobre): 366 apparati, 8 rack, 1007 cavi, 14 modelli, 7 allegati. Generazione e compilazione `.\gradlew.bat :shared:exchange:demoPackage :mobile:app:assembleDebug --no-parallel --max-workers=1`: **BUILD SUCCESSFUL in 39s**. La generazione ha validato la struttura e reimportato il pacchetto in memoria. Nuova fixture 529.281 byte, SHA-256 `da5c5a98c1ec78321968893ceb7f591044da08e7063208764ec6da21bd3aef71`; ID progetto e data fissi, altre entità rigenerate secondo il seed.

Moto g86 5G collegato, Android 16. Conservati APK precedente e snapshot dei dati privati (files/databases/shared_prefs/no_backup, cache escluse; nessuna garanzia transazionale) in `build/reports/demo-device-20261010`. Installazione con `adb install -r`: **Success**; MainActivity **Status: ok**. SHA-256 APK installato uguale alla build `623aaf28f63f4cac7c3e5af40e72e181f60fa6b823dc9a734d33e3491f4f730c`. Pacchetto copiato in Download come `OnlyField-demo-20261010.ofam`, checksum verificato.

Importazione dal normale flusso Android. La copia locale risultava più recente: sostituzione esplicitamente autorizzata dall'utente dopo l'avviso, con backup precedente conservato. Demo aperta e riaperta dal catalogo; descrizione della nuova revisione presente, controllo **0 errori strutturali / 11 avvisi documentali intenzionali**. I checksum dei **7/7 allegati** installati corrispondono alla fixture. App lasciata aperta sulla demo. Nessun problema emerso e nessuna suite aggiuntiva eseguita; i quattro residui hardware/UI/pulizia restano aperti. Nessun commit/push. Fonte operativa: [ADB ufficiale](https://developer.android.com/tools/adb), consultata il 10 ottobre.

## Consegna della demo e passaggio di sessione · 10 ottobre 2026

L'utente richiede salvataggio, commit e push sul ramo principale. Verificato `main` come ramo corrente e default di `origin`; dopo `git fetch origin main`, baseline `HEAD = origin/main = 1b1b21fe496e18c7b80dd1680d29d67b4bb15dff`, divergenza **0/0**. Consegna della fixture rigenerata, documentazione di verifica, roadmap e tracker. Rimangono **RES-13/19/23/24**, tutti parziali (**0 P1 / 3 P2 / 1 P3**); `activeTask` nullo e `remainingTasks` vuoto.

Ripartire dalla tabella aperta della [matrice Windows](docs/testing/windows-native-matrix.md). Demo e APK aggiornati sul moto g86: gli esiti di compilazione, installazione, riapertura e integrità dei sette allegati sono registrati sopra. Rotazione, TalkBack e altri collaudi restano pendenti; dopo la sostituzione non presumere che la vecchia bozza `RES19 Rotation Draft` sia ancora presente. App Android lasciata aperta; nessun nuovo runtime Windows o helper avviato. Backup ed evidenze in `build/reports/demo-device-20261010` conservati localmente e ignorati da Git, quindi assenti in un nuovo checkout. Nessuna pulizia storica respinta ritentata.

Verifica finale documentale e della fixture prima del commit; nessuna suite ripetuta in assenza di nuovi problemi. SHA della consegna, esito push e allineamento remoto verificabili nel log Git e nel report locale `build/reports/demo-device-20261010/handoff.json`.
