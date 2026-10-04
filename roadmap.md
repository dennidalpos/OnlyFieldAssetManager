# Evidenze e residui

Aggiornato al 4 ottobre 2026 dopo confronto con il codice e il tracker.

## Evidenze disponibili

- I moduli Gradle sono `:shared:core`, `:shared:exchange`, `:mobile:app` e `:pc:app`.
- Il codice implementa `.ofam` 1.11, Room 14, storage desktop atomico, database Android cifrato, import/export, documenti, mappe e configuratore condiviso.
- La suite locale registrata prima di questa revisione copre core, exchange, Android JVM e Desktop; le prove strumentali e manuali hanno limiti espliciti sotto.
- Il workflow GitHub esegue le suite JVM/Compose, genera APK debug e ZIP portable, calcola SHA-256 e pubblica soltanto su tag `v*`.

## Residui aperti

| ID | Stato | Evidenza richiesta |
| --- | --- | --- |
| RES-01 | Parziale | CI corretta (run manuale verde); resta la ripubblicazione su tag con conferma dell'utente. |
| RES-13 | Aperto | Checklist foto, scansione, multitouch e lettore USB su hardware reale. |
| RES-17 | Aperto | Suite UI Android verde su API 37 senza disabilitare controlli. |
| RES-19 | Parziale | Emulatore eseguito (UX-01, 02, 03, 05, 06); restano TalkBack, combinazioni mancanti e moto g86. |

Il dettaglio operativo e il criterio di chiusura sono in [PROJECT_STATUS.json](PROJECT_STATUS.json). La checklist hardware e in [docs/testing/hardware-checklist.md](docs/testing/hardware-checklist.md).

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
