# Testing: Dispositivi, Volumi e Soglie di Prestazione (Aggiornato Pilota A13)

Data: 2 ottobre 2026

## Dispositivi Disponibili e Piattaforme

- **Emulatore Android Studio:**
  - Dispositivo: Pixel 9 (API 34/35, Android 14+)
  - Stato: Verificato con esecuzione test di integrazione, persistenza Room e rendering UI.
- **Ambiente di Test e Runtime:**
  - JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.

## Campioni, Volumi di Riferimento e Risultati Effettivi (Pilota A13)

### Volume Collaudato (PilotBenchmarkTest)
- 1 Business Unit con 100 Apparati di rete e 2400 Porte
- 5 Armadi Rack (42U)
- 20 VLAN
- 5 Gruppi Credenziali
- 50 Cavi di collegamento
- Sfondo cartografico offline con attribuzione

### Risultati delle Prestazioni Misurate
- **Tempo di Avvio / Apertura Progetto:** < 150 ms (Soglia: < 2000 ms)
- **Ricerca (Nome, IP, Etichetta):** ~15 ms (Soglia: < 500 ms)
- **Validazione Strutturale Modello:** ~18 ms (Soglia: < 500 ms)
- **Export Pacchetto Cifrato (.ofam v1.7):** ~120 ms (Soglia: < 2000 ms)
- **Import e Decifratura Pacchetto:** ~110 ms (Soglia: < 2000 ms)
- **Uso Memoria Heap:** Stabile, < 45 MB per il progetto pilota completo.
- **Leggibilità Report PDF/XLSX/Markdown:** Nessun testo sovrapposto o segreto esposto; t="inlineStr" per prevenire formula injection in Excel.
