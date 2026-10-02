# Motori di Esportazione Documentale, Stampa e Privacy

Data: 2 ottobre 2026

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
- **Sorgenti Mappa:** Supporto per OpenTopoMap ("© OpenTopoMap contributors") e CARTO Positron ("© CARTO, © OpenStreetMap contributors").
- **Proiezione e Caching:** Conversione Mercatore latitudine/longitudine -> coordinate tile (z/x/y), caching locale e banner di attribuzione d'uso obbligatorio.

## Regole di Sicurezza, Riservatezza e Privacy

1. **Esclusione Tassativa dei Segreti:**
   - I motori documentali **escludono sempre e garantiscono zero leakage** di credenziali, password SSH/SNMP o segreti memorizzati.
2. **Filtro Riservatezza (`CONFIDENTIAL`):**
   - L'utente può scegliere di escludere note ed allegati contrassegnati come riservati tramite il flag `includeConfidential`.
3. **Prompt di Riesame (`REVIEW_REQUIRED`):**
   - Presenza di un prompt di conferma esplicito per la revisione prima dell'esportazione in caso di elementi non classificati.
