# AGENTS.md

## Workflow & Guidelines

- Entry `README.md`; docs `docs/`, decisions `plan.md`, evidence `roadmap.md`.
- Documentation is in Italian.
- `PROJECT_STATUS.json`: open work only; completions in `roadmap.md`. Update domain docs per task.
- Core/exchange have no Android UI/Context dependency.
- Room v2; bump version on schema edits. ProjectStore rejects IDs owned by another project before deletion. Only `fallbackToDestructiveMigration(true)`, both directions; downgrade-only fallback breaks upgrades.
- Hardware layouts/PoE overrides use hardware JSON, not Room columns.
- Apps compile `shared/configurator`; hardware/ports/continuity: `core.forms`, `core.model.ConnectionGraph`.
- Maps: `core.model.MapScene/ObjectMap/ObjectHierarchy`, `configurator.map.MapWorkspace`. Rendering: Android PdfRenderer, Desktop PDFBox 3.0.8.
- Shared edits/forms/labels/wizard: `core.edit`, `core.forms`, `core.display`, `core.onboarding`. Forms `copy()` entities to preserve hidden fields.
- `.\gradlew.bat :pc:app:packagePortable` → EXE/ZIP in `dist/OnlyFieldAssetManager`; data next to EXE.
- `core.i18n.Messages`: UTF-8 it/en/es, Italian default; capture at generation start, preserve user text.
- `prepare-release.ps1` needs empty output.
- Native tests: use `adb install -r` + `adb shell am instrument`; Gradle connected tests uninstall app/data.
- Tests: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`.

## Application Boundaries & Constraints

- Offline-first Android 14+ APK and Windows 11 x64 portable editor.
- Exchange: ZIP packages `.ofam` version 1 with optional AES-256-GCM / PBKDF2 encryption.
- Manual exchange; no server, automatic sync or automatic merge.
- Credentials stay in project, never documents. Password also protects local Windows media: encrypted package, bounded RAM and encrypted staging; no plaintext temporary files. Close imported `ProjectPackage` after use/cancellation.
- `STRUCTURAL_ERROR` blocks import; `DOCUMENTARY_WARNING` does not block saving.
- Package limits: ZIP 256 MiB, plaintext file 32 MiB, expanded 512 MiB, 10,000 entries; KDF max 1,000,000. Document filters use `exchange.DocumentSelection`.
- Recovery: encrypted `.recovery` journals; never discard pending backups. Android recovery key is Keystore-wrapped.
- Android: worker I/O, ordered commands, transactional reads; map drafts await save outcome. `DesktopIo`: worker, AWT secondary loop and busy editing gate.
