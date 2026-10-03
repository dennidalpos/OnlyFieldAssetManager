# Motori di Esportazione Documentale, Stampa e Privacy

Data: 3 ottobre 2026

## Motori di Esportazione

L'applicazione include motori nativi per l'esportazione documentale sia su Android (`:mobile:app`) che su Windows Desktop (`:pc:app`).

### 1. Esportatore OpenXML XLSX (`XlsxExportManager`)
- **Implementazione:** Generatore nativo OpenXML `.xlsx` a zero dipendenze esterne in `:shared:exchange`.
- **Fogli prodotti (5):**
  1. `Inventario Apparati`: Dettaglio apparati, categoria, IP, MAC, rack, U, note.
  2. `Porte e Cablaggio`: Porte, collegamenti, cavi, orientamento, mezzo, segmenti condivisi.
  3. `Rete Logica e VLAN`: VLAN, subnet CIDR, interfacce L3, appartenenze access/trunk, gruppi LAG.
  4. `Alimentazione e Badge`: Sorgenti A/B, PDU, UPS, autonomia misurata, PoE, badge.
  5. `Note e Osservazioni`: Allegati, note di osservazione e campi extra personalizzati.
- **Protezione da Formula Injection:** Tutte le celle di testo libero vengono formattate come stringhe esplicite con `t="inlineStr"`, prevenendo l'esecuzione di formule indotte (es. `=SUM`, `=CMD`).
- **Endpoint:** Distinzione chiara tra estremità fuori ambito ("Fuori Ambito") ed estremità ignote/scollegate ("Ignoto / Scollegato").

### 2. Esportatore Markdown (`MarkdownExportManager`)
- **Implementazione:** Generatore nativo in `:shared:exchange` per la produzione di report tecnici `.md`.
- **Sezioni:** Intestazione metadati, tabella KPI inventario, prospetti rack, cablaggio, rete logica, alimentazione e note.

### 3. Esportatore Report PDF Composto e Stampa Nativa Desktop (`DesktopDocumentManager`)
- **Implementazione:** Modulo documentale nativo in `:pc:app` per la generazione di report PDF composti e la stampa su Windows 11.
- **Integrazione Stampa Nativa:** Utilizza `java.awt.print.PrinterJob` per dialogare con le stampanti di sistema di Windows 11 ed inviare processi di stampa formattati.
- **Filtri:** Selezione dinamica delle sezioni via `ReportSelection` e filtri per Business Unit, Sede, Area o categoria.

### 4. Stampa Android (`ProjectPrintDocumentAdapter`)
- Integrazione diretta con `PrintManager` di sistema Android per l'invio alle stampanti o il salvataggio in PDF tramite anteprima nativa.

### 5. Cartografia e Mappe Raster Desktop (`DesktopCartographyManager`)
- **Fonte:** OpenTopoMap, senza chiave API. CARTO rimosso il 3 ottobre 2026; gli allegati già acquisiti conservano immagini e attribuzioni originali.
- **Acquisizione:** «Strumenti › Allegati e cartografia › Cartografia»: latitudine, longitudine, zoom 1–17 e nome; «Scarica e salva mappa» scarica 3 × 3 tessere, mostra avanzamento e anteprima, quindi salva il PNG come allegato del progetto. Gli errori HTTP e l'assenza di rete sono espliciti; nessun download automatico.
- **Uso offline:** l'allegato può diventare lo sfondo di un'area da «Allegati › Usa come planimetria…». I byte e l'attribuzione viaggiano nel `.ofam`, anche cifrato; i documenti conservano l'attribuzione.
- **Attribuzione:** «© OpenStreetMap contributors, SRTM | © OpenTopoMap (CC-BY-SA)» visibile nel PNG e nei metadati. Condizioni verificate il 3 ottobre 2026 nelle [istruzioni ufficiali OpenTopoMap](https://opentopomap.org/about#verwendung): uso anche commerciale con attribuzione e condivisione della mappa alle stesse condizioni; evitare download massivi. L'app acquisisce nove tessere per richiesta.

## Mappa del piano e pacchetto 1.10

La mappa operativa è descritta in [08-floor-map.md](08-floor-map.md). Tipologie, gerarchie, geometrie e foto viaggiano nel `.ofam` anche cifrato; importare/rimuovere uno sfondo conserva le posizioni. I report documentali mantengono le sezioni e i filtri esistenti; il canvas interattivo non costituisce un nuovo formato di report.

## Regole di Sicurezza, Riservatezza e Privacy

1. **Esclusione Tassativa dei Segreti:**
   - I motori documentali **escludono sempre e garantiscono zero leakage** di credenziali, password SSH/SNMP o segreti memorizzati.
2. **Filtro Riservatezza (`CONFIDENTIAL`):**
   - L'utente può scegliere di escludere note ed allegati contrassegnati come riservati tramite il flag `includeConfidential`.
3. **Prompt di Riesame (`REVIEW_REQUIRED`):**
   - Presenza di un prompt di conferma esplicito per la revisione prima dell'esportazione in caso di elementi non classificati.

## Lingua dei documenti

Tutti i generatori e la stampa ricevono la lingua dell'app catturata all'avvio: italiano, inglese o spagnolo. Intestazioni, etichette e nomi dei fogli sono localizzati; testi utente, codici tecnici e attribuzioni restano invariati. Le API senza lingua esplicita producono italiano. Markdown, XLSX e PDF Android comprendono anche i piani diretti della BU. PdfDocument genera PDF reali e propaga gli errori, senza documenti fittizi. Vedi [localizzazione e verifiche](09-localization.md).

## Hardware e porte (contratto 1.11)

XLSX comprende dimensioni, PoE e caratteristiche nell’inventario e una tabella delle porte nel foglio cablaggio. Markdown include schemi tabellari di porte e profondità esterna/utile rack. L’esportazione usa il motore condiviso di continuità; modulo ottico e connettore restano distinti. [Campi e verifiche](10-object-configurator.md).
