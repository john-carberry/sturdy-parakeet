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
| `core/data/` | Room database: paired keys (hashes only), brick sessions, blocked apps |
| `core/ui/` | Shared Compose theme |
| `feature/nfc/` | NFC reader mode and tag routing to screens |
| `feature/qr/` | QR generation and printing (ZXing), scanning (CameraX + ML Kit) |
| `feature/service/` | "Bricked" notification service and reboot/update receiver |
| `feature/blocker/` | Accessibility service that blocks apps, and the "app is bricked" screen |

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
   Then **Choose apps to block**, and turn on app blocking when the home screen asks.
2. Back on the home screen, tap a paired card or scan your QR code to brick the phone.
   Do it again to unbrick. A "Bricked" notification with a timer shows meanwhile.
3. Stuck without your key? Use one of your 5 emergency unbricks (they don't refill).

While bricked, opening a blocked app sends you Home and shows "This app is bricked".
Calls, the keyboard, the launcher and Settings always work. Keys and blocked apps
can't be changed while bricked.

## Status

- [x] M0 — project skeleton, CI
- [x] M1 — key pairing (NFC card + QR code)
- [x] M2 — brick state machine and background service
- [x] M3 — app blocking
- [ ] M4 — hardening and emergency unbricks
- [ ] M5 — polish and first release
