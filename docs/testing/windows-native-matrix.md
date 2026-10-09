# Matrice nativa Windows · RES-23

Aggiornata il 9 ottobre 2026. Il [tracker](../../PROJECT_STATUS.json) contiene solo il lavoro aperto; la [roadmap](../../roadmap.md) conserva gli esiti per singolo task.

## Perimetro verificato

EXE corrente, runtime sintetico isolato `build/tmp/res23-native-20261009`, progetto semplice, tema chiaro, finestra 1348×854, testo standard e pannelli laterali. Nessun dato reale modificato. Ogni riga include creazione e modifica con guasto reale di scrittura, bozza conservata, pacchetto SHA-256 invariato al guasto e riprova da Invio senza duplicati. Undo riguarda la modifica: confronto integrale di `project.json` con il pacchetto creato.

| Editor | Creazione verificata | Modifica verificata | Esito | Report JSON |
| --- | --- | --- | --- | --- |
| VLAN | 20, ambito Progetto | Nome | PASS | vlan-creation-result / vlan-edit-result |
| Subnet | 192.0.2.0/24, picker VLAN | Nome | PASS | subnet-creation-result / subnet-edit-result |
| WAN/VPN | WAN senza apparati | Tipo WAN→VPN | PASS | wan-result |
| Campo extra | Progetto, Testo, Condivisibile | Valore | PASS | extra-result |
| Cavo | Rame/Ethernet, senza estremità | Etichetta | PASS | cable-result |
| Interfaccia logica | Picker SW-01, senza IP/CIDR/VLAN | Checkbox L3→L2 | PASS | interface-result |
| Configurazione | SW-01, titolo e testo, Altri moduli | Testo | PASS | config-result |
| Permutazione | SW-01/P1, passaggio sconosciuto, seconda porta assente | Checkbox passaggio | PASS | mapping-result |
| Alimentazione | SW-01, Primaria (A), 230 V, sorgente assente | Tipo Secondaria (B) | PASS | power-result |
| PoE | SW-01/P1, PSE, 802.3at, potenza non assegnata | Standard 802.3af | PASS | poe-result |
| Badge | Progetto, Etichetta libera | Categoria Problema aperto | PASS | badge-result |

**22 scenari di salvataggio con guasto/riprova e 11 undo della modifica**. `matrix-result.json` verifica nuovamente singolo record, campo modificato, tutti gli altri campi del record invariati e uguaglianza integrale del progetto dopo undo. Catture PNG, risultati e pacchetti prima/dopo sono conservati in `build/reports/res23-network-native-20261009`. La baseline VLAN precedente all’apertura ha ZIP differente per riserializzazione, ma progetto integralmente identico: l’invarianza al guasto usa la baseline rilevata dopo l’apertura.

I picker reali sono stati usati quando indicato; scorrimento dopo errore verificato per subnet/interfaccia. Shift+Tab/Tab/Invio dopo guasto verificati nella creazione VLAN. Non si deduce una sequenza completa di focus per tutti i controlli. Switch sintetico SW-01 creato dalla UI, 24 RJ45 + 4 SFP+; questo preparativo non estende il collaudo degli apparati. WAN/cavo incompleti producono avvisi documentali attesi, zero errori strutturali visibili.

Il guasto usa un handle di lettura sul solo pacchetto sintetico con `FileShare.ReadWrite`, senza `Delete`, per impedire il replace atomico. Fonte primaria consultata il 9 ottobre: [Microsoft FileShare](https://learn.microsoft.com/en-us/dotnet/api/system.io.fileshare?view=net-10.0). Tutti gli helper della sessione rilasciati e EXE chiuso con Alt+F4, assenza della finestra confermata. Nessuna nuova modifica al codice o suite JVM ripetuta: nessun difetto applicativo emerso.

## Lavoro ancora aperto

| Priorità | Casistica | Prossima verifica ed evidenza richiesta |
| --- | --- | --- |
| P2 | Varianti editor AUD-46 | Ripetere creazione/modifica/guasto/riprova/undo su temi, dimensioni e testo ingrandito previsti; pannelli/dialoghi e percorso completo del focus. Progetto protetto con intervento utente. |
| P2 | Rete e cablaggio completi | Ambiti sede/apparato, IP/CIDR/VLAN interfaccia, WAN/VPN con apparati, cavi con estremità e mezzi diversi, permutazioni a due porte e patch panel fronte/retro. |
| P2 | Altri moduli completi | Configurazioni con allegati, campi extra con altri target/tipi/classificazioni, sorgenti UPS/PDU, PoE con potenza/budget/override hardware, badge con altri target/derivati e resa documentale. |
| P2 | Operazioni lunghe | Matrice EXE mouse/tastiera/focus e blocco input durante tutti i salvataggi ancora non verificati. Conservare esiti nativi precedenti della roadmap. |
| P2 | Protezione e recupero | Wizard protetto dopo errore, password errata/riprova, guasto cambio password, focus dopo recupero bloccato/riprova: interazione utente, nessun segreto acquisito. Domanda di disponibilità ancora senza risposta. |
| P2 | Fusione e rifiuti | Testo ingrandito, trasferimento porte/configurazioni/alimentazioni/campi extra, ripristino rifiutato per ciclo e riprova; rifiuti VLAN/sedi/apparati/fusione senza retarget. |
| P2 | PDF e stampa | Messaggio per sfondo PDF illeggibile, errore reale del motore di stampa e stampa fisica; hardware/intervento utente quando necessari. |

Queste verifiche restano in RES-23. La matrice base conclusa non chiude il residuo complessivo e non verifica Android. L’utente ha dichiarato il telefono non disponibile: rotazione fisica della bozza `RES19 Rotation Draft` e RES-13/19 restano pendenti.

## Risorse conservate

Nessuna pulizia storica respinta ritentata. Il runtime già tracciato in RES-24 conserva 1058 file; inventario aggiornato con dimensioni/SHA-256 in `build/reports/res23-network-native-20261009/cleanup-preserved-inventory.json`. Il vecchio inventario resta disponibile. Prima di eventuale rimozione manuale/consentita riverificare quello aggiornato; preservare entrambi i report, tutti i pacchetti e i backup.
