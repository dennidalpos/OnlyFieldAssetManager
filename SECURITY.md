# Security Policy

## Supported versions

Only the latest commit on the `main` branch is supported.

## Reporting a vulnerability

Please do not open a public issue for security problems.

Use GitHub's private vulnerability reporting: open the **Security** tab of this repository and choose **Report a vulnerability**. Include the affected component (Android app, Windows app, `.ofam` exchange), reproduction steps and the impact you observed. Remove real survey data from any evidence you attach.

You can expect an acknowledgement within a few days. Confirmed issues are fixed on `main` and disclosed after a fix is available.

## Scope notes

- Projects are stored locally (SQLCipher on Android, a local `data/` folder on Windows) and exchanged manually as `.ofam` packages.
- Dependencies are monitored with Dependabot and secret scanning with push protection.
