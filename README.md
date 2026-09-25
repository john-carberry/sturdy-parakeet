# DIY Brick

An Android app that "bricks" distracting apps until you tap an old NFC card or
scan a printed QR code. It's a do-it-yourself take on the Brick focus device.
See [PLAN.md](PLAN.md) for the full design and milestones.

## Project layout

| Module | What it holds |
|---|---|
| `app/` | Android app (Kotlin, Jetpack Compose) |
| `core/model/` | Plain Kotlin data model and default settings |
| `core/security/` | Key hashing (salted SHA-256, constant-time compare) |

## Build

Requires JDK 17+ and the Android SDK (API 35).

```sh
./gradlew assembleDebug   # APK in app/build/outputs/apk/debug/
./gradlew test            # unit tests
```

CI (GitHub Actions) builds the debug APK, runs the unit tests and lint on every
push, and uploads the APK as a build artifact.

## Status

- [x] M0 — project skeleton, CI
- [ ] M1 — key pairing (NFC card + QR code)
- [ ] M2 — brick state machine and background service
- [ ] M3 — app blocking
- [ ] M4 — hardening and emergency unbricks
- [ ] M5 — polish and first release
