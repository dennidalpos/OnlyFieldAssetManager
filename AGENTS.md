# AGENTS.md

Facts and execution constraints for AI agents.

## Workflow & Guidelines

- `README.md` is the project entry point. `docs/` contains domain-specific documentation. `plan.md` outlines step specifications and `roadmap.md` tracks progress.
- Code, identifiers and comments are in English. Documentation is in Italian.
- Android A00–A13, Windows W00–W05 e rework UX U01–U02 sono completati e verificati (94 unit test passati su tutti e 4 i moduli).
- Stato del progetto: 100% Completato (20 step su 20 completati). Collaudo di interoperabilità bidirezionale Android ↔ Windows verificato e pacchettizzazione portable x64 eseguita.
- The repository is organized into Gradle modules: `:mobile:app`, `:pc:app`, `:shared:core`, `:shared:exchange`. `:shared:core` and `:shared:exchange` must never depend on Android UI or Context APIs.
- Project mutations, form state and Italian enum labels are shared: `core.edit.ProjectEdits`, `core.forms`, `core.display`. Both UIs use them; edit forms must `copy()` the existing entity so hidden fields are preserved.
- Windows portable build: `.\gradlew.bat :pc:app:packagePortable` → `dist/OnlyFieldAssetManager/OnlyFieldAssetManager.exe` (+ ZIP). Data lives in `data/` next to the exe.
- Run one step at a time, verify baseline unit tests, review diffs, and keep documentation clean and synchronized.

## Application Boundaries & Constraints

- Offline-first Android 14+ APK and Windows 11 x64 portable editor.
- Exchange contract: ZIP packages `.ofam` v1.7 with optional AES-256-GCM / PBKDF2 encryption.
- Direct manual export/import. No server required, no automatic cloud sync, no automatic merge.
- Credentials integrated into project. Optional project password. No secret leakage in exports/documents.
- Strict separation of structural errors (`STRUCTURAL_ERROR`) vs documentary warnings (`DOCUMENTARY_WARNING`). Domain warnings do not block saving.
