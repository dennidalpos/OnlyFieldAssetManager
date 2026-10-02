# Contratto Dati di Dominio, Pacchetto .ofam v1.7 e Validazione

Data: 2 ottobre 2026

## 1. Struttura del Modello Dati

L'infrastruttura è organizzata secondo la gerarchia principale:
`Project` → `BusinessUnit` (BU) → [`Site` (Sede opzionale)] → `Area`/`Floor` → `Device` (Apparato) → `Port` (Porta).

### Entità di Dominio Principali
- **Project**: Radice del progetto contenente identificativi, metadati e liste di tutte le sottorisorse.
- **BusinessUnit / Site / Area**: Entità di appartenenza organizzativa e localizzazione fisica.
- **Device & Port**: Apparati (nome tecnico, etichetta fisica, alias, IP, MAC, rack, U) e relative porte (nome, lato, stato connessione).
- **Rack & DeviceModel**: Armadi rack con U (es. 42U), numerazione (bottom-to-top / top-to-bottom) e modelli di apparati riutilizzabili.
- **Cable & SharedPathSegment**: Cablaggio fisico ( Ethernet, DAC, AOC, fibra, console, orientamento A→B/B→A) e segmenti di percorso condivisi con capacità.
- **PanelMapping**: Permutazioni e passaggi interni sui pannelli di permutazione.
- **Vlan, Subnet, PortVlanMembership, LogicalInterface, LagGroup**: Rete logica, subnet CIDR, appartenenze porta (access/trunk/native), SVI L3 e gruppi LAG/LACP.
- **DeviceConfiguration, WanVpnConnection, VideoSurveillanceMapping, CustomExtraField**: Snapshots di configurazioni, linee WAN/VPN, associazioni videosorveglianza NVR/DVR e campi personalizzati extra tipizzati.
- **PowerFeed & PoeMapping**: Alimentazione A/B, PDU, UPS, autonomia misurata (con fonte/timestamp) e configurazioni PoE (PSE/PD).
- **DocumentBadge**: Badge documentali liberi e derivati automaticamente dal modello.
- **TrashItem**: Cestino locale temporaneo per il ripristino di elementi eliminati (escluso dagli export).

## 2. Formato del Pacchetto di Scambio (`.ofam` / ZIP v1.7)

L'archivio ZIP `.ofam` v1.7 costituisce il formato universale di scambio tra Android e Windows Desktop e contiene:
- `manifest.json`: Metadati del pacchetto, formato (`1.7`), timestamp, checksum SHA-256 e parametri di cifratura KDF.
- `project.json` (o `project.json.enc` se cifrato): L'albero completo del progetto in JSON UTF-8.
- `attachments/`: Risorse binarie (foto, planimetrie, sfondi cartografici, schemi PDF) referenziate con SHA-256.

### Parametri Crittografici del Pacchetto Cifrato
- **Derivazione Chiave (KDF):** `PBKDF2WithHmacSHA256`, 100.000 iterazioni, salt casuale da 16 byte.
- **Cifratura Simmetrica:** AES-256 in modalità `AES/GCM/NoPadding` con IV casuale da 12 byte (96 bit) e tag a 128 bit.

## 3. Motore di Validazione (`ModelValidator`)

Il motore di validazione separa rigorosamente la gravità dei riscontri:

### Errori Strutturali (`STRUCTURAL_ERROR`)
Impediscono l'importazione o il salvataggio poiché corrompono l'integrità dei dati:
- Formato UUID invalido o identificativo duplicato.
- Ciclo nella catena di alimentazione di apparati (`POWER_FEED_CYCLE_DETECTED`).
- Riferimenti ad apparati, sorgenti alimentazione o porte inesistenti.
- Numero VLAN fuori dal range 1..4094 o CIDR subnet malformato.
- Pacchetto ZIP corrotti, checksum SHA-256 non corrispondenti, password errata o assente.

### Avvisi Documentali (`DOCUMENTARY_WARNING`)
Evidenziati all'utente senza bloccare il salvataggio o l'esportazione:
- Autonomia calcolata/espressa senza fonte e data della misurazione.
- Apparato principale con alimentazione singola/parziale.
- VLAN con ID duplicato nello stesso ambito/scope.
- Cavo con estremità scollegata o da verificare (`DETACHED_CABLE_ENDPOINT`).
- Apparato senza posizione assegnata o sovrapposizione slot U in un rack.
