# Live Free

An Android app that locks distracting apps away until you tap an old NFC card or
scan a printed QR code kept somewhere else. A do-it-yourself focus lock.
See [PLAN.md](PLAN.md) for the full design and milestones.

## Project layout

| Module | What it holds |
|---|---|
| `app/` | Android app (Kotlin, Jetpack Compose) |
| `core/model/` | Plain Kotlin data model and default settings |
| `core/security/` | Key hashing, QR key format, NFC two-tap pairing rules |
| `core/data/` | Room database: paired keys (hashes only), lock sessions, blocked apps |
| `core/ui/` | Shared Compose theme |
| `feature/nfc/` | NFC reader mode and tag routing to screens |
| `feature/qr/` | QR generation and printing (ZXing), scanning (CameraX + ML Kit) |
| `feature/service/` | "Locked" notification service and reboot/update receiver |
| `feature/blocker/` | Accessibility service that blocks apps and guards Live Free's Settings pages, uninstall protection (device admin), and the "app was closed" screen |

## Build

Requires JDK 17+ and the Android SDK (API 35).

```sh
./gradlew assembleDebug   # APKs in app/build/outputs/apk/debug/ (use arm64-v8a for most phones)
./gradlew test            # unit tests
```

CI (GitHub Actions) builds the debug APK, runs the unit tests and lint on every
push, and uploads the APK as a build artifact.

## Using it (so far)

1. Open **Manage keys** and add an NFC card (tap it twice) or a QR code (print it, then save).
   Then **Choose apps to block**, and follow **Protection setup** (app blocking and
   uninstall protection) when the home screen asks.
2. Back on the home screen, tap a paired card or scan your QR code to lock the phone.
   Do it again to unlock. A "Locked" notification with a timer shows meanwhile.
3. Stuck without your key? Start one of your 5 emergency unlocks (they don't refill).
   It unlocks after a 10-minute wait and must then be used within 5 minutes.

While locked, opening a blocked app sends you Home and shows "This app is locked".
Calls, messages, alarms, the keyboard, the launcher and Settings always work. Keys and
blocked apps can't be changed while locked, and Live Free's own Settings pages (its
accessibility switch, App info, device admin, uninstall) are closed while locked.

To remove Live Free: unlock, turn off "Live Free uninstall protection" under
Settings › Security › Device admin apps, then uninstall as usual.

Known limits: Safe Mode disables third-party accessibility services, and the Settings
guard matches English button text.

## Releases

Pushing a tag like `v0.5.0` runs `.github/workflows/release.yml`, which tests, builds
signed APKs and publishes them on GitHub Releases. The signing key is never in the
repository; the workflow reads it from four repository secrets
(Settings › Secrets and variables › Actions):

| Secret | Value |
|---|---|
| `LIVEUNLOCKED_KEYSTORE_BASE64` | the keystore file, base64-encoded |
| `LIVEUNLOCKED_KEYSTORE_PASSWORD` | keystore password |
| `LIVEUNLOCKED_KEY_ALIAS` | key alias |
| `LIVEUNLOCKED_KEY_PASSWORD` | key password |

Builds on any machine use the same key when `LIVEUNLOCKED_KEYSTORE_FILE` and the three
password/alias variables are set. Android only updates an app signed with the same
key, so keep a safe copy: losing it means uninstalling (and re-pairing keys) to update.

## Privacy: where your keys live

- Card IDs and QR secrets never leave your phone. The app stores only a salted
  SHA-256 hash of each one, in its private database, and excludes that data from
  cloud backups and phone-to-phone transfers.
- A QR key's secret exists only on the printout (and briefly on screen, where
  screenshots are blocked). Treat the printout, or a PDF of it, like a house key.
- Nothing in this repository holds a real key. `scripts/check-no-secrets.sh` runs
  in CI and fails if a commit contains an app build, a database, a keystore, a PDF,
  the local-only `app/src/debug/res/` folder, or a QR key secret. Run it before
  every commit with a one-time `git config core.hooksPath .githooks`.

## Status

- [x] M0 — project skeleton, CI
- [x] M1 — key pairing (NFC card + QR code)
- [x] M2 — lock state machine and background service
- [x] M3 — app blocking
- [x] M4 — hardening and emergency unlocks
- [x] M5 — polish and first release (setup checklist, stats, signed GitHub Releases)
