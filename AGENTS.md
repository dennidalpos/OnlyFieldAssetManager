# AGENTS.md

## Workflow & Guidelines

- Entry: `README.md`; domain docs: `docs/`; decisions: `plan.md`; progress/evidence: `roadmap.md`.
- Code, identifiers and comments are in English. Documentation is in Italian.
- `PROJECT_STATUS.json` holds only open work; completion evidence belongs in `roadmap.md`. Update domain docs after each task.
- Gradle modules: `:mobile:app`, `:pc:app`, `:shared:core`, `:shared:exchange`. `:shared:core` and `:shared:exchange` must never depend on Android UI or Context APIs.
- Android Room v1 (greenfield): no migrations; any other schema version is dropped and recreated (destructive fallback).
- `shared/configurator` is Compose source shared by both app source sets; hardware, port reconciliation and continuity tracing stay in `core.forms` / `core.model.ConnectionGraph`.
- Map and containment: core.model.MapScene/ObjectMap/ObjectHierarchy; shared UI in `shared/configurator` package `configurator.map` (MapWorkspace); presets and port logic in core.forms.DevicePresets/PortLogic. Page rendering uses Android PdfRenderer and desktop PDFBox 3.0.8 off the UI thread.
- Shared edits, forms and labels: `core.edit.ProjectEdits`, `core.forms`, `core.display`, `core.onboarding` ("Nuovo sito" wizard). Both UIs use them; edit forms must `copy()` the existing entity so hidden fields are preserved.
- Windows portable build: `.\gradlew.bat :pc:app:packagePortable` → `dist/OnlyFieldAssetManager/OnlyFieldAssetManager.exe` (+ ZIP). Data lives in `data/` next to the exe.
- `core.i18n.Messages` uses UTF-8 bundles (it/en/es), defaults to Italian; capture it at document generation start. Custom/user text is preserved.
- CI `release.yml`: manual runs only upload artifacts; tag pushes publish APK debug, portable ZIP without data, SHA-256. Local `prepare-release.ps1` requires empty output.
- Run one step at a time, verify baseline unit tests, review diffs, and keep documentation clean and synchronized.

## Application Boundaries & Constraints

- Offline-first Android 14+ APK and Windows 11 x64 portable editor.
- Exchange contract: ZIP packages `.ofam` version 1 (any other version rejected) with optional AES-256-GCM / PBKDF2 encryption.
- Direct manual export/import. No server required, no automatic cloud sync, no automatic merge.
- Credentials integrated into project. Optional project password. No secret leakage in exports/documents.
- Strict separation of structural errors (`STRUCTURAL_ERROR`) vs documentary warnings (`DOCUMENTARY_WARNING`). Domain warnings do not block saving.
