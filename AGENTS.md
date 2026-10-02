# AGENTS.md

Updated: 2026-10-02. Repository facts and execution constraints.

## Workflow

- plan.md contains the development instructions; roadmap.md tracks step evidence; README.md is the entry point. Documentation is Italian; code, identifiers and comments are English.
- Implement Android A00-A13 first in Android Studio with Gemini. Start Windows W00-W05 with desktop AI tools only after A13 passes.
- This revision changes documentation only. No app code, Gradle wrapper, test runner or Git metadata is present; .idea exists. Application checks are not verified.
- A00 must generate mobile/, pc/, shared/, docs/ and fixtures/. These folders do not exist yet. Reserve pc/ without desktop code until W00.
- The repository root is the future Gradle project. Mobile code belongs in mobile/app/; Windows code in pc/app/. shared/core/ and shared/exchange/ must not depend on Android APIs or UI.
- Run one step at a time, verify the baseline and exit criteria, review the full diff and update roadmap.md with actual results. Do not claim future Gradle commands are verified.

## Application boundaries

- Offline Android 14+ APK first; Windows 11 x64 portable second. Desktop must preserve the Android exchange contract and meaningful data, with hardware-adapted interaction.
- One working copy transferred by full manual export/import. No internal project history, automatic backup, copy merge or simultaneous collaboration.
- Credentials are integrated; project password is optional. Without it credentials are readable. Password changes/removal require the current password; no recovery.
- Complete exports include credentials and preserve protection. Document exports exclude secret fields; free text/media require sharing review.
- Location, physical cabling, logical network and power are distinct. Unknown/conflicting observations remain explicit; domain warnings do not block saving.
- Deleted ports/devices leave detached endpoints marked for verification. Replacement creates a new device without inherited data or references.
- Original media are preserved; no preset attachment count cap. Exclude internal components, individual fibers/splices, CSV/SVG/audio, discovery and monitoring.

## Verified command

PowerShell, repository root, 2026-10-02:

`rg -n '^(#|##|###) ' README.md plan.md roadmap.md`

This verifies document structure only.
