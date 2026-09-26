# DIY Brick

An Android app that "bricks" distracting apps until you tap an old NFC card or
scan a printed QR code. It's a do-it-yourself take on the Brick focus device.
See [PLAN.md](PLAN.md) for the full design and milestones.

## Project layout

| Module | What it holds |
|---|---|
| `app/` | Android app (Kotlin, Jetpack Compose) |
| `core/model/` | Plain Kotlin data model and default settings |
| `core/security/` | Key hashing, QR key format, NFC two-tap pairing rules |
| `core/data/` | Room database of paired keys (hashes only) |
| `feature/nfc/` | NFC reader mode and tag routing to screens |
| `feature/qr/` | QR generation and printing (ZXing), scanning (CameraX + ML Kit) |

## Build

Requires JDK 17+ and the Android SDK (API 35).

```sh
./gradlew assembleDebug   # APK in app/build/outputs/apk/debug/
./gradlew test            # unit tests
```

CI (GitHub Actions) builds the debug APK, runs the unit tests and lint on every
push, and uploads the APK as a build artifact.

## Using it (so far)

1. Open **Manage keys** and add an NFC card (tap it twice) or a QR code (print it, then save).
2. Back on the home screen, tap a card or scan the QR code to check it's recognised.

Bricking itself arrives in M2 and M3.

## Status

- [x] M0 — project skeleton, CI
- [x] M1 — key pairing (NFC card + QR code)
- [ ] M2 — brick state machine and background service
- [ ] M3 — app blocking
- [ ] M4 — hardening and emergency unbricks
- [ ] M5 — polish and first release
