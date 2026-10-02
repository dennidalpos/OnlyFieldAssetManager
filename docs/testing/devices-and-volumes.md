# Testing: Dispositivi, Volumi e Soglie di Prestazione (Step A00)

Data: 2 ottobre 2026

## Dispositivi Disponibili

- **Emulatore Android Studio:**
  - Dispositivo: Pixel 9
  - Sistema Operativo: Android 14+ (API 34/35)
  - Stato: Configurato e disponibile via ADB (`Pixel_9`)
- **Dispositivi Fisici:**
  - Da collaudare e registrare negli step successivi e durante il pilota A13.

## Campioni e Volumi di Riferimento

- **Volume Ordinario per Business Unit (BU):**
  - ~5 Rack
  - ~100 Apparati per BU
  - Connessioni e cavi di rete associati
  - Allegati e planimetrie per area
- **Volume Impegnativo:**
  - Moltiplicatore 3x-5x per test di carico e memoria.

## Soglie Iniziali di Prestazione e Leggibilità

- **Tempo di Avvio / Apertura Progetto:** < 2 secondi per volume ordinario.
- **Ricerca (Nome, IP, Etichetta):** < 500 ms per risposta visibile.
- **Salvataggio Transazionale / Export / Import:** Nessun blocco UI, operazione atomica con feedback visivo.
- **Memoria:** Nessun Memory Leak nell'avvicendamento di viste dense (Rack, Planimetrie).
- **Leggibilità UI / PDF:** Testo visibile e leggibile senza sovrapposizioni a qualsiasi livello di zoom e su schede/PDF generati.
