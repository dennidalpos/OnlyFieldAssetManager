# AGENTS.md

## Workflow & Guidelines

- Italian docs: `README.md`, `docs/`, decisions `plan.md`, evidence `roadmap.md`.
- `PROJECT_STATUS.json`: open work; completions in `roadmap.md`. Update domain docs.
- Core/exchange have no Android UI/Context dependency.
- Room v2: bump on schema edits. ProjectStore rejects foreign IDs before deletion. Use only `fallbackToDestructiveMigration(true)` both directions.
- Hardware layouts/PoE overrides belong in hardware JSON, not Room columns.
- Shared UI: `shared/configurator`; hardware: `core.forms`, `core.model.ConnectionGraph`.
- `MapWorkspace` uses host `MapUiState`: camera/selection/containers survive view/size changes; outside Room/.ofam.
- Reuse `OnlyFieldTheme`, `AppSpacing` and `ContentDialog`; text forms max 640 dp, technical canvases exempt. Dialogs use window/IME bounds.
- Forms `copy()` retain hidden fields; close on successful save. Scope refusals never retarget networks.
- `core.i18n.Messages`: UTF-8 it/en/es, Italian default; capture at generation start, preserve user text.
- `.\gradlew.bat :pc:app:packagePortable` → EXE/ZIP in `dist/OnlyFieldAssetManager`; data beside EXE. Regeneration refuses runtime data/. `prepare-release.ps1` needs an empty output dir.
- Native tests: `adb install -r` + `adb shell am instrument`; connected Gradle tests uninstall app/data. Require instrumentation `OK`.
- Checks: `docs/05-testing-and-benchmarks.md`; simulated widths cannot verify TalkBack/rotation/tablets.
- Demo: `.\gradlew.bat :shared:exchange:demoPackage --no-parallel --max-workers=1`; checks: `.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1`.
- DemoSeed/DemoMedia → `fixtures/demo/onlyfield-demo.ofam`: fixed date/project ID, regenerated entity IDs; separate import into apps.

## Application Boundaries & Constraints

- Offline Android 14+; Windows 11 x64 portable. Manual .ofam v1 ZIP exchange; no server/sync/automatic merge.
- AES-256-GCM/PBKDF2 optional protection. Credentials never enter documents. Protected Windows media: bounded RAM/encrypted staging, no plaintext temps. Close imports (`ProjectPackage`).
- `STRUCTURAL_ERROR` blocks import; `DOCUMENTARY_WARNING` permits saving. Filters: `exchange.DocumentSelection`.
- Limits: ZIP 256 MiB, file 32 MiB, expanded 512 MiB, 10,000 entries; KDF max 1,000,000.
- Recovery: encrypted .recovery journals; preserve pending backups. Android key is Keystore-wrapped.
- Android: worker I/O, ordered commands, transactional reads. `DesktopIo`: worker, AWT secondary loop, busy editing gate.
