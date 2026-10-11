---
name: onlyfield-maintenance
description: Implement, review, or document repository work in OnlyFieldAssetManager while preserving its offline Android/Windows architecture, exchange safety, tracker evidence, and platform-specific verification boundaries. Use for feature fixes, UI work, releases, audits, and tracker updates in this repository; not for general Kotlin or Compose questions.
---

# OnlyFieldAssetManager maintenance

Use this skill with the repository's `AGENTS.md`; it supplies the project-specific operating context and does not replace its safety or scope rules.

## Start from current evidence

Before changing repository state, read the applicable sections of:

- `PROJECT_STATUS.json` for active work and residuals;
- `plan.md` for accepted product decisions and open questions;
- `roadmap.md` for completed work and exact verification evidence;
- the relevant document under `docs/` and nearby production and test callers.

Treat tracker counts, handoffs, installed builds, and manual-test results as historical until confirmed in those current files. Do not treat an observation marked "da valutare con l'utente" as an approved change. Avoid reopening completed tracker work or rerunning deferred native tests unless the request or a code change makes it relevant.

For a multi-step task, state the intended scope and verification before editing. Keep implementation, tracker updates, and evidence aligned: remove a completed item from `PROJECT_STATUS.json`, record the outcome and actual commands in `roadmap.md`, and update the affected Italian domain documentation when behavior or workflow changes. Keep code and identifiers in English.

## Preserve the architecture

- `shared:core` and `shared:exchange` must remain independent of Android UI and `Context`.
- Put shared configurator UI in `shared/configurator`; represent hardware layouts and PoE overrides in hardware JSON, not Room columns. Use `core.forms` and `core.model.ConnectionGraph` for the corresponding domain behavior.
- Room schema changes require the documented version bump. Keep foreign-ID rejection before `ProjectStore` deletion and use only the documented bidirectional destructive-migration configuration.
- `MapWorkspace` owns no durable map state: use its host `MapUiState` so camera, selection, and containers survive view and size changes. Expose each map level through `MapViewBar` and `MapView.forLevel`; do not hide a map view in a menu or dialog.
- Reuse `OnlyFieldTheme`, `AppSpacing`, `SectionCard`, `ListRow`, `FormRow`, and `ContentDialog`. Text forms are at most 640 dp; `wide` is for lists and technical canvases. Keep confirmations in `AlertDialog`, use window/IME bounds, and use full-screen dialogs on compact windows.
- Preserve hidden form fields with `copy()`, close editors after a successful save, and never retarget a network after a scope refusal.
- Keep i18n messages UTF-8 in Italian, English, and Spanish. Italian is the default; capture the locale at generation start and preserve user-authored text.

## Protect local projects and exchanges

- The app is offline-only: Android 14+ and Windows 11 x64 portable, with manual `.ofam` v1 exchange. Do not introduce server sync, automatic merge, or credentials in project documents.
- Maintain the import boundary: `STRUCTURAL_ERROR` blocks import, while `DOCUMENTARY_WARNING` can be saved; use `exchange.DocumentSelection` for filtering and close every `ProjectPackage`.
- Preserve all resource ceilings: 256 MiB ZIP, 32 MiB file, 512 MiB expanded payload, 10,000 ZIP entries, and PBKDF2 at most 1,000,000 iterations.
- For protected media on Windows, use bounded RAM plus encrypted staging and never leave plaintext temporary files. Preserve encrypted recovery journals and pending backups; Android recovery keys remain Keystore-wrapped.
- Keep Android reads and commands worker-based, ordered, and transactional. Desktop I/O uses the worker, AWT secondary loop, and busy-editing gate.

## Verify proportionally and report boundaries

Read `docs/05-testing-and-benchmarks.md` before selecting the native verification path. Start with the smallest native Gradle test scope that covers the edited module, then expand only for cross-module or high-risk changes. Never claim that Compose/JVM tests verify physical rotation, TalkBack, tablets, real camera/scanner hardware, a native EXE, or power-loss recovery.

Connected Android Gradle tests uninstall app data. Do not run them, replace an installed app, or manipulate device data unless the request explicitly covers that destructive device action. Native instrumentation evidence is complete only when it ends with `OK`.

For demos, generate with:

```powershell
.\gradlew.bat :shared:exchange:demoPackage --no-parallel --max-workers=1
```

Then run the focused demo checks:

```powershell
.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1
```

For a portable Windows package, use `:pc:app:packagePortable`; output is under `dist/OnlyFieldAssetManager` with data beside the EXE. Do not regenerate over existing runtime data, and require an empty output directory before `prepare-release.ps1`.

## Finish cleanly

Review the full diff for unrelated changes, generated files, temporary artifacts, and documentation drift. Report the files changed, commands actually run and their outcome, plus any platform/manual verification deliberately left pending. Do not commit or push unless explicitly requested.
