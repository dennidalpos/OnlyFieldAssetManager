# Documento di Rilascio, Collaudo e Handover Windows (Step A13)

Data: 2 ottobre 2026  
Stato: Completato ed Evidenziato

## 1. Artefatti di Build Android Rilasciati

- **Progetto Gradle Root:** `OnlyFieldAssetManager`
- **Modulo Mobile:** `:mobile:app`
- **Versione SDK:** `compileSdk 35`, `minSdk 34` (Android 14+)
- **Comandi di Build:**
  ```powershell
  .\gradlew.bat :mobile:app:assembleDebug :mobile:app:testDebugUnitTest :shared:core:test :shared:exchange:test
  ```
- **Percorso Output APK:** `mobile/app/build/outputs/apk/debug/app-debug.apk`

## 2. Esiti del Collaudo e Benchmarking delle Prestazioni (Pilota A13)

### 2.1 Copertura Test e Integrità
- **Test Unitari Totali:** 44 test eseguiti e superati con successo a zero errori (12 `:shared:core`, 12 `:shared:exchange`, 20 `:mobile:app`).
- **Persistenza e Migrazioni Room:** Verificate tutte le migrazioni dal database locale v1 fino alla v9 (`MIGRATION_8_9`).

### 2.2 Volumi di Riferimento e Tempi di Esecuzione (PilotBenchmarkTest)
- **Campione di Carico Testato:**
  - 1 Business Unit con 100 Apparati di rete
  - 5 Armadi Rack (42U)
  - 20 VLAN
  - 5 Gruppi Credenziali
  - 50 Cavi e collegamenti
  - Sfondo cartografico offline con attribuzione
- **Misurazioni Effettive:**
  - **Validazione Strutturale Modello:** ~18 ms (Soglia richiesta: < 500 ms)
  - **Esportazione Pacchetto Cifrato (.ofam v1.7):** ~120 ms (Soglia richiesta: < 2000 ms)
  - **Importazione e Decifratura Pacchetto:** ~110 ms (Soglia richiesta: < 2000 ms)
  - **Reperimento e Ricerca Inventario:** ~15 ms (Soglia richiesta: < 500 ms)

## 3. Specifiche per la Fase Windows (Handover W00–W05)

1. **Contratto Dati Consolidato:**
   - **Versione Contratto:** `1.7` (`docs/contract/v1-contract.md`).
   - **Formato Pacchetto di Scambio:** ZIP `.ofam` con `manifest.json`, `project.json` / `project.json.enc` (PBKDF2/AES-GCM) e cartella `attachments/`.
2. **Riutilizzo Architetturale e Moduli Comuni:**
   - I moduli `shared/core` (modello e regole) e `shared/exchange` (serializzazione ZIP, cifratura AES-GCM, OpenXML XLSX, Markdown, validazione) sono 100% JVM-pure e privi di dipendenze Android UI o Room.
   - Il futuro editor PC in `pc/app/` (Compose Desktop) riutilizzerà direttamente `shared/core` e `shared/exchange`.
