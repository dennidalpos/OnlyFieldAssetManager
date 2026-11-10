# Localization

[Retained Italian source](09-localization.it.md)

The UI offers System, Italiano, English and Español. Default/fallback is Italian. Android stores the choice in preferences; Windows uses data/settings.properties. core.i18n.Messages loads UTF-8 catalogs for three languages.

Validation, wizard, UI and report generators receive the operation language. Labels/warnings are translated; user text, codes, passwords and protocol fields remain unchanged. Document default author uses a localized key.

Keys must exist in it/en/es. Messages.plural chooses singular for 1 and the supported base form otherwise. Enum labels use core.display formatters rather than Kotlin symbol names.

Verify localized content and layout through [tests](05-testing-and-benchmarks.md). Real TalkBack announcements/focus remain [native Android](testing/hardware-checklist.md) qualification; translated strings alone do not prove accessibility.
