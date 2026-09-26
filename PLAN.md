# Live Free — Project Plan

A do-it-yourself phone lock for focus. Instead of buying a dedicated NFC
gadget, you lock and unlock your phone by tapping a **card you already
own** (NFC) or by scanning a **QR code** you printed and put somewhere out of
reach. Blocking and settings are handled by an Android app.

---

## 1. How it works (user's view)

1. Install the app and grant the permissions it needs (setup walks through them).
2. **Pair a key.** Choose one of these:
   - **NFC card**: tap an old NFC card on the back of the phone. This can be a
     hotel key, transit card, gym or office badge, Amiibo, or a cheap NTAG sticker.
   - **QR code**: the app generates a one-time QR code. You print it, stick it
     somewhere inconvenient (fridge, garage, another room), and the app deletes
     its own copy.
3. **Pick a mode.** A mode is a named list of blocked apps (or allowed apps).
   Examples: "Work", "Sleep", "Weekend".
4. **Lock.** Tap the card or scan the QR code. The chosen apps are now blocked.
5. **Unlock.** Tap the same card or scan the same QR code again. Without the key
   you have to use one of a small number of **emergency unlocks**, which can
   come with a waiting period.

---

## 2. Key choice: NFC card or QR code

| | NFC ("read an old card") | QR code |
|---|---|---|
| Hardware needed | Phone with NFC + any 13.56 MHz card | Just a printer or a pen |
| Speed | Instant tap | ~1–2 s to open the camera and scan |
| Cheat resistance | Good if the card is physically elsewhere | Weaker: you could photograph the code (see mitigations) |
| Works on every phone | No (needs NFC) | Yes |

**Recommendation:** support both, and let the user pair several keys, e.g. a
card at the office and a QR code at home.

### 2.1 NFC card details and gotchas
- Use `NfcAdapter.enableReaderMode()` while the app is in the foreground. For a
  tap from anywhere, also register an intent filter for
  `TAG_DISCOVERED`/`TECH_DISCOVERED`.
- **Identity = tag UID.** Store only `SHA-256(UID + per-install salt)`, never
  the raw UID.
- **Cards that WON'T work reliably:**
  - **Contactless bank cards and phone/watch wallets.** They show a *random* UID
    on every tap. The app should detect this (the UID changes between two
    pairing taps) and refuse the card with a clear message. Don't read EMV data;
    it's a privacy and security liability.
  - **Some MIFARE Classic cards** (older transit and hotel cards) can't be read
    on phones without an NXP NFC chip. They still expose a UID on most phones,
    and the UID is all we need.
- **Stronger option for writable tags (NTAG213/215/216 stickers, about $0.30):**
  write an NDEF record containing a random 128-bit secret and store its hash. A
  cloned UID alone can't unlock it.
- Pairing flow: tap twice → check the UID is stable → save → show "Card paired ✓".

### 2.2 QR code details
- At setup, generate a random 256-bit secret. Encode it as
  `livefree://key/v1/<base64url-secret>` and store only its hash.
- Render the QR code (ZXing `QRCodeWriter`) once, for print or share, then
  **delete it**. Offer "Save as PDF to print" and block screenshots on that
  screen with `FLAG_SECURE`.
- Scan with **CameraX + ML Kit Barcode Scanning** (bundled model, fully offline).
- **Anti-cheat mitigations** (there's no perfect fix, since you can always photograph paper):
  - `FLAG_SECURE` on the generation screen.
  - Optional "rotate key" that regenerates the code and makes old printouts
    useless.
  - Optional **liveness hint**: reject a scan where the code fills the frame
    perfectly flat with a screen moiré pattern. This is best-effort and off by
    default.
  - Honest framing: a QR key is a speed bump for your own willpower, not a vault.

---

## 3. How apps actually get blocked on Android

This is the hardest part technically. There are three tiers; ship tier A first.

| Tier | Mechanism | Strength | Setup effort |
|---|---|---|---|
| **A (MVP)** | `AccessibilityService` detects the foreground app → shows a full-screen "This app is locked" overlay and sends the user Home | Good | Toggle one setting |
| B (fallback) | `UsageStatsManager` polling + `SYSTEM_ALERT_WINDOW` overlay | Medium (slight lag) | Two permission screens |
| **C (strict mode)** | **Device Owner** + `DevicePolicyManager.setPackagesSuspended()`. The OS itself greys out the apps | Very strong | One-time `adb` command on a freshly reset or no-account phone |

Extra hardening while locked (tier A):
- Block the Accessibility settings page, the app's own App Info page, and the
  uninstall dialog. Otherwise disabling the service is a two-tap cheat.
- Register as a **Device Admin** so the app can't be uninstalled without
  deactivating it first. That deactivation screen is blocked too.
- A foreground service with a persistent notification ("Locked since 9:14 • Work")
  keeps the process alive. Re-arm on `BOOT_COMPLETED`.
- Optional: a local `VpnService` DNS filter to block the *websites* of locked
  apps (e.g. instagram.com), since browsers can otherwise bypass app blocking.

> Distribution note: Google Play restricts AccessibilityService use. Plan on
> sideloading (GitHub Releases / F-Droid) first. A Play release would need the
> accessibility use declared and justified.

---

## 4. Android app

**Stack:** Kotlin, Jetpack Compose (Material 3), minSdk 26 (Android 8), target
the latest SDK. Hilt for DI, Room for data, DataStore for settings. CameraX +
ML Kit for scanning, ZXing for QR generation.

### 4.1 Screens
1. **Onboarding / permissions**: step-by-step checklist (Accessibility, Device
   Admin, Notifications, Overlay, Camera, NFC on). Each item shows a live ✓.
2. **Home**: big status card (🔒 *Locked* / 🔓 *Unlocked*), current mode, time
   locked, and buttons "Tap card" / "Scan QR". An NFC tap works from anywhere
   in the app.
3. **Keys**: list of paired keys (name, type, date added). Add, rename,
   remove, and rotate. You can only remove keys while unlocked.
4. **Modes**: create or edit a mode. Choose between a block list and an allow
   list, using a searchable app picker with icons.
5. **Schedules** (v2): e.g. "Auto-lock 22:00–07:00; tap key to end early".
6. **Blocked-app overlay**: shown when you open a locked app. Includes a
   friendly message, time remaining or elapsed, "Go Home", and a small
   "Emergency unlock" link.
7. **Emergency unlock**: shows how many you have left (e.g. 3/5). Optionally
   makes you wait 10 minutes or type a long phrase first.
8. **Stats** (v2): time locked per day, blocked-open attempts, streaks.
9. **Settings**: strictness, emergency count, website blocking, export/backup
   (keys are excluded), about.

### 4.2 Module layout
```
app/                  # Compose UI, navigation, DI wiring
core/model/           # Mode, Key, LockSession, AppRule
core/data/            # Room DB, DataStore, repositories
core/security/        # hashing, salt, key validation, tamper checks
feature/nfc/          # reader mode, pairing, UID-stability check, NDEF write
feature/qr/           # generator (ZXing), scanner (CameraX + ML Kit)
feature/blocker/      # AccessibilityService, overlay, UsageStats fallback,
                      # DeviceOwner/suspend impl behind one Blocker interface
feature/service/      # foreground service, boot receiver, schedule alarms
```

### 4.3 Core state machine
```
             tap/scan valid key                 tap/scan valid key
 UNLOCKED ───────────────────────▶ LOCKED ───────────────────────▶ UNLOCKED
                                     │  emergency unlock (wait, then count--)
                                     └───────────────────────────▶ UNLOCKED
```
- State is persisted (Room) so it survives reboots and process kills.
- Every transition is logged (for stats and debugging).
- Key validation: `constantTimeEquals(hash(scanned), storedHash)`.

### 4.4 Data model (Room)
- `Key(id, type[NFC_UID|NFC_NDEF|QR], label, secretHash, createdAt)`
- `Mode(id, name, listType[BLOCK|ALLOW], packages: List<String>)`
- `Session(id, modeId, startedAt, endedAt?, endReason[KEY|EMERGENCY|SCHEDULE])`
- `Settings` (DataStore): `emergencyRemaining`, `strictMode`, `salt`, …

---

## 5. Optional physical build (a lock key of your own)

If you'd rather have a dedicated object than a random card:
- **Easiest:** an NTAG215 sticker or coin tag stuck under a 3D-printed or wooden
  block, or glued inside an old card holder. Cost is under $2.
- **Recycle:** any old hotel key, transit card, or badge. Mount it on the wall
  by the door with a command strip.
- **QR key:** laminate the printed QR code and fix it somewhere you have to
  walk to.
- No electronics, batteries, or firmware needed. The phone does all the work.

---

## 6. Milestones

| # | Milestone | Deliverable |
|---|---|---|
| M0 | Project skeleton | Gradle multi-module, Compose app, CI (build + unit tests + lint) |
| M1 | Key pairing | NFC UID pairing with random-UID detection; QR generate, print, and scan; hashed storage |
| M2 | Lock state machine | Persisted UNLOCKED/LOCKED state, foreground service, boot re-arm |
| M3 | Blocking (tier A) | AccessibilityService + overlay + mode app picker |
| M4 | Hardening | Device Admin, settings/uninstall page blocking, emergency unlocks |
| M5 | Polish + release | Onboarding checklist, stats, signed APK on GitHub Releases |
| M6 (v2) | Extras | Schedules, strict Device Owner mode, website blocking via VPN, NDEF secrets |

---

## 7. Testing plan
- **Unit:** state machine, key hashing/validation, UID-stability logic,
  emergency counter.
- **Instrumented:** Room migrations, the accessibility service against a test
  app, overlay display.
- **Manual device matrix:** Pixel (stock), Samsung (One UI kills background
  services aggressively), Xiaomi/MIUI (needs "autostart" permission guidance).
- **Cards to test:** NTAG213/215, MIFARE Classic 1K, MIFARE Ultralight
  (transit), DESFire (badges), a bank card (must be *rejected*), a phone wallet
  (must be *rejected*).
- **Cheat tests:** reboot, force-stop, disable accessibility, uninstall, change
  the clock, and airplane mode. Each should leave the phone locked or be blocked.

---

## 8. Risks and open questions
- **OEM battery killers** (Samsung, Xiaomi, Huawei) may stop the service. We
  mitigate with a foreground service, battery-optimization exemption prompts,
  and per-OEM help screens.
- **Accessibility can always be turned off from Safe Mode.** Only Device Owner
  (tier C) fully prevents this. Document it honestly.
- **Play Store policy** on accessibility means sideloading first.
- **Decisions (v1 defaults):**
  1. One key unlocks every mode. Keys are not tied to specific modes.
  2. 5 emergency unlocks, which never refill.
  3. Website blocking (VPN) is out of scope for v1.
  4. Strict Device Owner mode is deferred to v2.
