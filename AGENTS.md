# AGENTS.md

Facts and execution constraints for AI agents.

## Workflow & Guidelines

- `README.md` is the project entry point. `docs/` contains domain-specific documentation. The root `plan.md` outlines step specifications and `roadmap.md` tracks progress.
- Code, identifiers and comments are in English. Documentation is in Italian.
- `PROJECT_STATUS.json` holds only open work and residuals; remove completed entries. Keep completion evidence in `roadmap.md`, and update domain docs after each task.
- The repository is organized into Gradle modules: `:mobile:app`, `:pc:app`, `:shared:core`, `:shared:exchange`. `:shared:core` and `:shared:exchange` must never depend on Android UI or Context APIs.
- Android Room v13: additive migration 12→13; EncryptedDatabase checkpoints and backs up the encrypted database before upgrading.
- Map geometry/catalog and containment live in core.model.ObjectMap/ObjectHierarchy; page rendering uses Android PdfRenderer and desktop PDFBox 3.0.8 off the UI thread.
- Project mutations, form state and Italian enum labels are shared: `core.edit.ProjectEdits`, `core.forms`, `core.display`, `core.onboarding` ("Nuovo sito" wizard). Both UIs use them; edit forms must `copy()` the existing entity so hidden fields are preserved.
- Windows portable build: `.\gradlew.bat :pc:app:packagePortable` → `dist/OnlyFieldAssetManager/OnlyFieldAssetManager.exe` (+ ZIP). Data lives in `data/` next to the exe.
- Run one step at a time, verify baseline unit tests, review diffs, and keep documentation clean and synchronized.

## Application Boundaries & Constraints

- Offline-first Android 14+ APK and Windows 11 x64 portable editor.
- Exchange contract: ZIP packages `.ofam` v1.10 (1.7–1.9 readable; newer versions rejected) with optional AES-256-GCM / PBKDF2 encryption.
- Direct manual export/import. No server required, no automatic cloud sync, no automatic merge.
- Credentials integrated into project. Optional project password. No secret leakage in exports/documents.
- Strict separation of structural errors (`STRUCTURAL_ERROR`) vs documentary warnings (`DOCUMENTARY_WARNING`). Domain warnings do not block saving.
