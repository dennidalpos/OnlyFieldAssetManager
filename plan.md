# Decisioni di prodotto

## Obiettivo e confini

OnlyFieldAssetManager serve a un tecnico o integratore per censire progetti con più sedi, apparati, rack, cablaggi, rete logica, alimentazione e documenti. Il rilievo può essere fatto su Android e rifinito su Windows. I progetti restano locali; lo scambio `.ofam` è manuale. Non sono previsti server, sincronizzazione automatica, scoperta LLDP/SNMP, mappa geografica delle sedi o import da fogli/strumenti esterni.

## Decisioni confermate

- Android 14+ e Windows 11 x64 portable condividono regole, formato `.ofam` v1 e configuratore. Lo schema Android corrente è Room v3; cambiare schema richiede un nuovo numero di versione. In assenza di migrazione, il fallback ricrea il database: esportare prima i dati da conservare.
- La gerarchia è progetto → sede → piano; il piano dell'apparato è facoltativo. Stato operativo e stato del rilievo sono distinti. I moduli Credenziali, Configurazioni e Badge restano nel modello anche quando la UI li nasconde se vuoti.
- Il collegamento fisico è porta → cavo → passanti → porta. I collegamenti WAN/VPN appartengono al modello logico. Non inferire un passaggio ambiguo né retarget di rete in caso di cancellazione/rifiuto.
- La mappa apre schema sedi al progetto, topologia alla sede e planimetria al piano; la barra espone anche Logica, Apparati ed Elenco dove applicabili. Gli schemi fra sedi non rappresentano coordinate geografiche.
- I dati di gestione di terzi (fornitore/cliente/sconosciuto) sono facoltativi. Layout delle porte e override PoE restano nel JSON hardware. Modifiche dei modelli applicate alle istanze solo con azione esplicita.
- I documenti non includono credenziali; testo e note dell'utente non sono tradotti. Contenuti riservati o da rivedere richiedono scelta esplicita nell'export.
- `STRUCTURAL_ERROR` blocca import/salvataggio; `DOCUMENTARY_WARNING` non lo blocca. Import, fusione, media e recupero devono preservare la copia precedente quando falliscono. La fusione a tre vie richiede scelte esplicite per i conflitti.
- Limiti: ZIP 256 MiB, file 32 MiB, espanso 512 MiB, 10.000 entry, PBKDF2 massimo 1.000.000 iterazioni. Il budget Android protetto è prudenziale e può rifiutare contenuto vicino a 256 MiB anche se lo ZIP finale entrerebbe nel limite.
- I collaudi hardware richiesti usano la fotocamera integrata; lettori USB e scanner esterni sono esclusi dalla verifica. Gli scatti reali erano stati rinviati dall'utente.

## Stato e riferimenti

Le funzionalità previste risultano implementate nei sorgenti; la chiusura dei collaudi nativi resta distinta. Il [tracker](PROJECT_STATUS.json) contiene i soli residui aperti RES-13, RES-19 e RES-23 (tutti P2). RES-24 e AUD-71 risultano completati nell'[ultimo handoff](roadmap.md). Per comportamento corrente consultare [dominio](docs/02-domain-data-contract.md), [flussi](docs/07-workflows.md), [mappa](docs/08-floor-map.md) e [verifica](docs/05-testing-and-benchmarks.md).
