# Android hardware qualification — RES-13 and RES-19

[Retained Italian source](hardware-checklist.it.md)

Use synthetic projects without real credentials. Record device, API, APK revision, text scale, theme, dimensions and outcome. Not executed is not passed. Full acceptance belongs to the declared tracker.

## RES-13 — hardware and recovery

| Area | Required checks |
| --- | --- |
| Photos | Capture/reopen offline, cancel/denied permission, series, rotation, encrypted export/import and correct port/cable targets |
| QR/barcode | Integrated camera, unknown/multiple codes, low light, denied permission and leaving the flow |
| Touch | Pinch/pan, selection/drag, save/reopen |
| Recovery | Forced process/power interruption during writing using isolated SQLCipher/Keystore data; retain backups |

External USB readers/scanners are outside the requested qualification. Normal physical reboot with pending journals was recorded on 8 October 2026; it does not replace interruption during a write. Synthetic camera input does not qualify real photographs.

## RES-19 — native UI

Exercise UX-01–06 at 360/412 dp, light/dark, text 1.0/1.3, keyboard open and real rotation. Check reachable errors/fields, discard confirmation, template/port search, targets, TalkBack announcements/focus and a physical tablet where available.

Include Map levels/back navigation, Devices/port cards, dense configurator, project home, Network/Power/Cabling/Media forms, full-screen dialogs, Documents after navigation, protected wizard, asynchronous saves, warned imports, merge/rejections and recovery.

Earlier moto g86 API 36 observations covered sites and Devices views. Simulated Compose density and selected AppRoot checks are bounded evidence. Physical rotation, TalkBack audio/focus, tablet and the complete workflows remain incomplete. Do not infer these results from JVM tests.
