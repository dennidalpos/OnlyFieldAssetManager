# Collaudo EXE Windows — RES-23

Usare un profilo e un progetto sintetici isolati; non rigenerare sopra `data/` personale. Registrare build, dimensione finestra, tema, scala testo, input/focus, screenshot ed esito. Criteri completi nel [tracker](../../PROJECT_STATUS.json).

## Già verificato, con perimetro limitato

Il 9 ottobre 2026 un EXE con progetto semplice, tema chiaro e testo standard ha superato creazione/modifica con guasto/riprova e undo in 11 editor: VLAN, subnet, WAN/VPN, campi extra, cavi, interfacce logiche, configurazioni, permutazioni, alimentazioni, PoE e badge. Sono 22 salvataggi guasto/riprova e 11 undo; `matrix-result.json` e PNG furono registrati in `build/reports/res23-network-native-20261009`. Le varianti seguenti non erano coperte.

L'11 ottobre un EXE isolato ha importato la Demo Comune e visualizzato schema sedi, Logica, Apparati, scheda porta e Home Progetto. Il difetto dei campi VLAN affiancati (AUD-71) è stato corretto nel codice e verificato in Compose; la resa nell'EXE aggiornato resta da controllare.

## Matrice aperta

| Area | Verifica nativa richiesta |
| --- | --- |
| Layout e focus | Temi, finestre larghe/strette, testo ingrandito; editor, dialoghi e campi affiancati AUD-71, percorso tastiera completo. |
| Rete e cablaggio | Ambiti sede/apparato, IP/CIDR/VLAN, WAN/VPN con apparati, estremità/mezzi dei cavi, permutazioni complete, fronte/retro patch panel. |
| Moduli | Configurazioni con allegati, tipi/target dei campi extra, UPS/PDU, PoE con budget/override, badge e documenti. |
| Operazioni lunghe | Mouse, tastiera e focus durante salvataggi; input bloccato, bozza e risultato coerenti. |
| Protezione | Wizard protetto dopo errore, password errata/riprova, cambio password fallito e recupero con focus corretto; richiede interazione dell'utente senza acquisire segreti. |
| Fusione/rifiuti | Trasferimento dati associati, testo ingrandito, rifiuto per ciclo e riferimenti di rete, correzione/riprova senza retarget. |
| PDF/stampa | Errore per sfondo PDF illeggibile, guasto reale dello spooler, annullamento e stampa fisica. |

Le prove JVM/Compose non completano RES-23. Conservare i report già acquisiti e distinguere gli scenari nuovi dai casi base conclusi. Fonte per il guasto di file usato nel collaudo storico: [Microsoft FileShare](https://learn.microsoft.com/en-us/dotnet/api/system.io.fileshare).
