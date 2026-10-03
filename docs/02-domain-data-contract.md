# Contratto Dati di Dominio, Pacchetto .ofam v1.11 e Validazione

Data: 3 ottobre 2026

## 1. Struttura del Modello Dati

L'infrastruttura è organizzata secondo la gerarchia principale:
`Project` → `BusinessUnit` (BU) → [`Site` (Sede opzionale)] → `Area`/`Floor` → `Device` (Apparato) → `Port` (Porta).

### Entità di Dominio Principali
- **Project**: Radice del progetto contenente identificativi, metadati e liste di tutte le sottorisorse.
- **BusinessUnit / Site / Area**: Entità di appartenenza organizzativa e localizzazione fisica.
- **Device & Port**: Apparati (nome tecnico, etichetta fisica, alias, IP, MAC, rack, U) e relative porte (nome, lato, stato connessione).
- **Rack & DeviceModel**: Armadi rack con U (es. 42U), numerazione (bottom-to-top / top-to-bottom) e modelli di rack, apparati e cavi riutilizzabili.
- **ObjectContainment**: relazione tipizzata figlio → genitore. Un oggetto ha un solo genitore; rack è sempre contenitore e un apparato lo è soltanto quando la sua tipologia `DEVICE` ha `canContainObjects = true`. Cavi e tipologie `CABLE` non possono contenere. Il piano fisico deriva dalla radice, mentre la BU resta indipendente.
- **Cable & SharedPathSegment**: Cablaggio fisico ( Ethernet, DAC, AOC, fibra, console, orientamento A→B/B→A) e segmenti di percorso condivisi con capacità.
- **PanelMapping**: Permutazioni e passaggi interni sui pannelli di permutazione.
- **Vlan, Subnet, PortVlanMembership, LogicalInterface, LagGroup**: Rete logica, subnet CIDR, appartenenze porta (access/trunk/native), SVI L3 e gruppi LAG/LACP.
- **DeviceConfiguration, WanVpnConnection, VideoSurveillanceMapping, CustomExtraField**: Snapshots di configurazioni, linee WAN/VPN, associazioni videosorveglianza NVR/DVR e campi personalizzati extra tipizzati.
- **PowerFeed & PoeMapping**: Alimentazione A/B, PDU, UPS, autonomia misurata (con fonte/timestamp) e configurazioni PoE (PSE/PD).
- **DocumentBadge**: Badge documentali liberi e derivati automaticamente dal modello.
- **TrashItem**: Cestino locale temporaneo per il ripristino di elementi eliminati (escluso dagli export).

## 2. Formato del Pacchetto di Scambio (`.ofam` / ZIP v1.11)

L'archivio ZIP `.ofam` v1.11 costituisce il formato universale di scambio tra Android e Windows Desktop e contiene:
- `manifest.json`: Metadati del pacchetto, formato (`1.11`), timestamp, checksum SHA-256 e parametri di cifratura KDF.
- `project.json` (o `project.json.enc` se cifrato): L'albero completo del progetto in JSON UTF-8.
- `attachments/<idAllegato>/<nomeFile>`: i file degli allegati (foto, planimetrie, PDF…), con checksum SHA-256 nel manifest. Il percorso è calcolato da `AttachmentFiles.entryName` (`:shared:exchange`) ed è lo stesso su Android e Windows. Un allegato il cui file non è presente sul dispositivo viene esportato solo come metadati, con un avviso all'utente.

### Dati della mappa

- `Project.objectTypes`: tipologie personalizzate con ID, nome, categoria e famiglia; il catalogo generico è fornito dalle app.
- `Device.objectTypeId` e `Cable.objectTypeId`: tipologia del catalogo; assente nei dati precedenti. Rack conserva la propria entità.
- `Project.floorplanPlacements`: posizioni relative degli apparati/rack; `Project.cableRoutes`: ID, cavo, piano e almeno due punti normalizzati finiti.
- `Project.objectContainments`: relazioni di contenimento con `ObjectRef` tipizzato. Importando 1.7–1.9, un `Device.rackId` esistente diventa la relazione equivalente in memoria senza modificare l’archivio sorgente; `rackId` resta sincronizzato con il rack antenato più vicino.
- `Cable.deviceAId/deviceBId`: estremità su apparati quando non è specificata una porta; la porta ha precedenza. Estremità ignote sono ammesse.
- `Attachment.targetType = CABLE`: foto/allegati dei cavi. `Area.floorplanAttachmentId` e `floorplanPageIndex` (base zero) permettono di condividere un PDF tra piani; `Attachment.pageCount` registra il conteggio letto dal file.
- Fusione per ID anche per tipologie, percorsi, posizioni e allegati. Lo sfondo non determina l'esistenza degli oggetti.

### Versioni del Contratto

- **1.11** (configuratore): hardware e gruppi di porte, lato/posizione/connettore/modulo ottico, profondità utile e montaggio rack, modelli delle tre famiglie. Lettura 1.7–1.10 conservata; versioni successive a 1.11 rifiutate. Nessuna inferenza fronte/retro sui vecchi progetti. [Dettagli](10-object-configurator.md).
- **1.10** (MAP03): aggiunge `objectContainments` e `canContainObjects`. Le app di quella versione leggono 1.7–1.10; la versione corrente supporta anche 1.11. Le versioni successive al formato supportato sono rifiutate con `UNSUPPORTED_FORMAT_VERSION`. Le relazioni e le proprietà del contenitore sono nel JSON cifrato e nella fusione; genitori concorrenti generano un conflitto sul figlio.
- **1.9** (MAP02): aggiunge i dati della mappa, le tipologie e le foto dei cavi.
- **1.8** (fase v1.1): aggiunge `Device.serialNumber` (facoltativo). Tutti i campi nuovi hanno default nullo e l'import ignora le chiavi sconosciute, quindi i pacchetti 1.7 si importano senza errori (campo assente = `null`) e le app 1.7 leggono i pacchetti 1.8 ignorando il seriale. Verificato da `ContractVersionTest`.
- **1.7**: versione consegnata con la v1.0 (allegati nei pacchetti, `attachmentsEncrypted`).

### Parametri Crittografici del Pacchetto Cifrato
- **Derivazione Chiave (KDF):** `PBKDF2WithHmacSHA256`, 100.000 iterazioni, salt casuale da 16 byte.
- **Cifratura Simmetrica:** AES-256 in modalità `AES/GCM/NoPadding` con IV casuale da 12 byte (96 bit) e tag a 128 bit.
- **Allegati cifrati:** nei pacchetti protetti anche ogni file in `attachments/` è cifrato con la stessa chiave; ogni voce contiene IV (12 byte) seguito dal testo cifrato. Il manifest lo segnala con `attachmentsEncrypted: true` (campo opzionale, assente = `false`, compatibile con i pacchetti v1.7 precedenti, che non contenevano allegati).

## 3. Motore di Validazione (`ModelValidator`)

Il motore di validazione separa rigorosamente la gravità dei riscontri:

### Errori Strutturali (`STRUCTURAL_ERROR`)
Impediscono l'importazione dei pacchetti; i form applicano le proprie verifiche prima del salvataggio. L'autosave locale conserva il lavoro e mostra i riscontri:
- Formato UUID invalido o identificativo duplicato.
- Percorso di cavo duplicato per piano, riferimenti mancanti o punti non validi.
- Ciclo nella catena di alimentazione di apparati (`POWER_FEED_CYCLE_DETECTED`).
- Riferimenti ad apparati, sorgenti alimentazione o porte inesistenti.
- Riferimenti di contenimento inesistenti, multipli per lo stesso figlio, autoriferimenti, cicli o genitori non contenitori (`INVALID_OBJECT_CONTAINMENT`).
- Numero VLAN fuori dal range 1..4094 o CIDR subnet malformato.
- Pacchetto ZIP corrotti, checksum SHA-256 non corrispondenti, password errata o assente.

### Avvisi Documentali (`DOCUMENTARY_WARNING`)
Evidenziati all'utente senza bloccare il salvataggio o l'esportazione:
- Autonomia calcolata/espressa senza fonte e data della misurazione.
- Apparato principale con alimentazione singola/parziale.
- VLAN con ID duplicato nello stesso ambito/scope.
- Cavo con estremità scollegata o da verificare (`DETACHED_CABLE_ENDPOINT`).
- Apparato senza posizione assegnata o sovrapposizione slot U in un rack.
