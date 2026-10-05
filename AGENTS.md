# AGENTS.md

## Workflow & Guidelines

- Entry `README.md`; domain `docs/`; decisions `plan.md`; evidence `roadmap.md`.
- Code, identifiers and comments are in English. Documentation is in Italian.
- `PROJECT_STATUS.json`: open work only; completions in `roadmap.md`. Update domain docs per task.
- Core and exchange must never depend on Android UI or Context APIs.
- Room v2, no migrations; only `fallbackToDestructiveMigration(true)` for both directions. Downgrade-only overrides upgrade fallback. Bump version on schema edits.
- Hardware port layouts/PoE overrides use existing hardware JSON; no Room columns change.
- Both apps compile `shared/configurator` Compose sources; hardware/ports/continuity stay in `core.forms` and `core.model.ConnectionGraph`.
- Maps/containment: `core.model.MapScene/ObjectMap/ObjectHierarchy`; shared `configurator.map.MapWorkspace`; presets/ports: `core.forms.DevicePresets/PortLogic`. Rendering: Android PdfRenderer, Desktop PDFBox 3.0.8.
- Shared edits/forms/labels/wizard: `core.edit`, `core.forms`, `core.display`, `core.onboarding`. Forms must `copy()` existing entities to preserve hidden fields.
- `.\gradlew.bat :pc:app:packagePortable` → EXE/ZIP in `dist/OnlyFieldAssetManager`; data next to EXE.
- `core.i18n.Messages`: UTF-8 it/en/es, Italian default; capture at generation start, preserve user text.
- CI: manual artifacts; tags publish APK debug, ZIP without data, SHA-256. `prepare-release.ps1` needs empty output.
- Verification: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1`.

## Application Boundaries & Constraints

- Offline-first Android 14+ APK and Windows 11 x64 portable editor.
- Exchange contract: ZIP packages `.ofam` version 1 (any other version rejected) with optional AES-256-GCM / PBKDF2 encryption.
- Manual exchange; no server, automatic sync or automatic merge.
- Credentials stay in project, never documents. Password also protects local Windows media: encrypted package + memory, no plaintext temporary files.
- `STRUCTURAL_ERROR` blocks import; `DOCUMENTARY_WARNING` does not block saving.
- Package limits: ZIP 256 MiB, plaintext file 32 MiB, expanded 512 MiB, 10,000 entries; KDF max 1,000,000. All document filters use `exchange.DocumentSelection`.
- Android services dispatch I/O; Desktop state uses `DesktopIo` with an AWT secondary loop and busy editing gate to retain synchronous results.
