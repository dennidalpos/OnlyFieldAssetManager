# Istruzioni del repository: OnlyFieldAssetManager

<!-- secondbrain:repository:begin -->
Direttive comuni applicate: revisione **3.0** — 2026-10-19.
<!-- secondbrain:source global=DC59C089490D9EE3E7E7F389FCC087D199EB12C16ADD2D20EA78076D9BB3BBDB workstation=3090606D3A18890581CDAD79BE8A65348612B40AAF40D5EAEB4E62AE0215D430 -->
Dossier SecondBrain di questo repository: 02-areas/products/OnlyFieldAssetManager, relativo alla radice di SecondBrain indicata nelle istruzioni globali.
- Leggi a inizio task: questo AGENTS.md, 02-areas/products/OnlyFieldAssetManager/context.md, 02-areas/products/OnlyFieldAssetManager/memory.md e PROJECT_STATUS.json (o 02-areas/products/OnlyFieldAssetManager/tracker.md).
- Nei task di scrittura aggiorna solo novità pertinenti: decisioni e gotcha in 02-areas/products/OnlyFieldAssetManager/memory.md; architettura e stato in 02-areas/products/OnlyFieldAssetManager/context.md; attività nel tracker preservandone schema e storico.
- Nei task di scrittura pulisci temporanei del task e residui confermati, preservando dati e lavoro unico; rivedi diff e git status --ignored. Esegui secondbrain.ps1 -Sync dopo modifiche a dossier o direttive, oppure su richiesta. Nei task di sola lettura segnala i residui senza rimuoverli.
<!-- secondbrain:repository:end -->

Revisione specifica del progetto 2.4 — 2026-10-18. Vincoli specifici del progetto; i contenuti comuni sono gestiti separatamente.

## Contesto

- **Scopo**: Editor e sistema di censimento per reti aziendali, apparati, cablaggi, planimetrie e sedi operative. Gestione progetti locale offline-first con scambio dati cross-platform (Android 14+ e Windows 11 x64) tramite pacchetti validati `.ofam` v1.
- **Modalità**: Greenfield (semplificazione aggressiva per architettura minimale; eliminazione shim temporanei e dead code)
- **Target Applicativo**: Cross-platform: Mobile Android 14+ e Desktop Windows 11 x64.
- **Stack & Toolchain**: Kotlin, Jetpack Compose Android e Compose Desktop JVM, Gradle Kotlin DSL; moduli mobile:app, pc:app, shared:core e shared:exchange. Fonte: settings.gradle.kts, pc/app/build.gradle.kts e CI release.yml.

## Vincoli specifici

  * Validazione rigorosa dello schema di scambio pacchetti `.ofam` v1 (schema runtime, prevenzione Zip Slip / Path Traversal).
  * Privacy e archiviazione: progetti e dati di rete rigorosamente locali; nessuna telemetria o trasmissione remota non autorizzata.
  * Solo il download esplicito di cartografia/mappe richiede accesso alla rete; tutte le altre funzioni operano offline.
  * Integrità dei dati: export e import `.ofam` transazionali e verificati con checksum.

## Comandi e verifica

Comandi verificati staticamente contro i file del progetto; questa revisione delle direttive non certifica l'esecuzione di build o test applicativi. Consultare anche i gate della CI, quando presenti.

| Attività | Comando | Ambiente | Evidenza e limiti |
|---|---|---|---|
| **Full checks** | `.\gradlew.bat --no-daemon --warning-mode all :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest` | pwsh / root | Task usati da .github/workflows/release.yml; richiede JDK e Android SDK configurati |

## Riferimenti e collaborazione

- Consultare README e documentazione effettivamente presenti; versioni dai manifest/lockfile, non da ipotesi sullo stack.
- 02-areas/products/OnlyFieldAssetManager/memory.md e 02-areas/products/OnlyFieldAssetManager/context.md (nel dossier SecondBrain) conservano decisioni tecniche durevoli, gotcha e architettura; aggiornarli a fine task.
- PROJECT_STATUS.json è il tracker esistente: contiene lavoro aperto, senza storico dei task conclusi; preservarne lo schema.
- Eseguire i controlli previsti per la modifica prima di un commit richiesto. Nessun commit o push automatico.
- Preservare dati persistenti e configurazioni esterne; non estendere la manutenzione a operazioni distruttive non autorizzate.
