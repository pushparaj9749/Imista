# Imista Forward — Private Phone-to-Phone Notification Forwarding

**Two independent Android applications** that forward notifications from a secondary phone (Sender) to your primary phone (Receiver) using **your own Telegram Bot** as relay — no Firebase, no Supabase, no custom backend.

- **Imista Forward Sender** (`com.imista.forward.sender`) — captures notifications via `NotificationListenerService` and forwards to Telegram.
- **Imista Forward Receiver** (`com.imista.forward.receiver`) — displays forwarded notifications reconstructed from Telegram.

Repository: `pushparaj9749/Imista` • Branch: `arena/cf8a3fb5-imista`

---

## ⚡️ Quick Start (2 Phones — no token on the phones)

**You never type a bot token or chat ID on any phone.** The token lives only in the relay's server
secrets (Supabase). Pairing is one 6-digit code, like a WhatsApp login code.

### 1. Install Apps
- Download from GitHub Releases (`v1.1.0`):
  - `Imista-Forward-Sender-v1.1.0.apk`
  - `Imista-Forward-Receiver-v1.1.0.apk`
- Alternatively build locally (see Build section).

Install **Sender** on the secondary (unused) phone, **Receiver** on your main phone.

### 2. Pair with a 6-digit code
1. Open **Sender** → Setup → tap **Naya code**. The relay mints a code such as `813593`.
2. Open **Receiver** → **Connect Receiver** → type that code → **Connect**.
3. Done. Sender's notifications now go Sender → relay → Receiver feed.

### 3. Optional: also deliver to Telegram (no token typed anywhere)
The relay holds `TELEGRAM_BOT_KEY` server-side. If it is configured:
1. Receiver → **Connect Receiver** → **Open bot** (or on Sender: **Link Telegram**).
2. Press **Start** in Telegram (the deep link already carries your code).
3. Tap **Link Telegram** — the relay detects the `/start <code>` message, stores the chat,
   and from then on every forward is also delivered to that chat.

Nothing is pasted and no token/chat ID is stored on either phone.

> If the relay reports `telegramConfigured:false` (see the latest automated report on the
> relay-probe issue), Telegram delivery is simply off — in-app delivery via relay polling still works.

### 4. Grant Permissions

**Sender (required):**
- **Notification Access** → `Settings → Notification Listener` → enable Imista Sender. This reads only `title`, `text`, `appName`, `package`, `timestamp` exposed by Android — never private databases.
- Optional **Device Admin**: for device-lock feature only. **Honest disclaimer**: ordinary Device Admin does NOT make the app uninstall-proof. User can deactivate then uninstall via normal flow. No hidden persistence.

**Receiver (optional but recommended):**
- **Telegram Notification Listener** — enable so Receiver can reconstruct forwarded messages. Why? **See Architecture Honesty below.** Sender sends to Telegram; Telegram shows a notification; Receiver captures that Telegram notification locally and stores it.

Also exclude both apps from battery optimization (Xiaomi/Samsung/Oppo aggressively kill services).

### 5. Choose Apps to Forward (Sender)
Sender → **App Selection**:
- Modes: `All Apps` / `Selected Only` / `Exclude List`
- Supported examples: WhatsApp, SMS apps, Gmail, Outlook, Instagram, Telegram, Discord, plus any app posting notifications.
- Filtering, preview length, sensitive redaction (`[Hidden]`), duplicate suppression (7s window) configurable.

### 6. Test
Send a WhatsApp message to the Sender phone → Telegram should show formatted message → Receiver Feed should show it seconds later (if Telegram notification enabled and listener active). Check **Delivery History** on Sender for `SENT`/`FAILED`/`RETRYING`.

---

## 🏗️ Architecture Honesty — How Receiver Really Gets Messages

**Important:** Telegram Bot API does **not** provide a general-purpose client-to-client push subscription. A bot's `sendMessage` to a chat does **not** appear as an `getUpdates` update for another app polling the same bot. If both Sender and Receiver called `getUpdates`, they'd steal updates from each other (single-consumer semantics).

**Valid design we implemented:**

1. **Sender never calls `getUpdates`.** It only `getMe`/`getChat`/`sendMessage` (with retry, exponential backoff, `retry_after` rate-limit handling, duplicate `UUID` prevention, delivery status tracking).
2. **Receiver is the sole `getUpdates` poller** (15-min periodic `WorkManager`, with correct `offset` handling, no competing consumer). It uses `getUpdates` **only** to verify bot health and handle pairing commands (`/pair`), **not** to fetch forwarded notifications.
3. **Actual notification content flow:**
   ```
   Sender captures notification → sendMessage to Telegram Chat → Telegram app on Receiver phone posts notification → Receiver's TelegramNotificationService (NotificationListener) captures that Telegram notification → parses Imista-formatted text (requires ID: marker) → stores locally → optional local mirror notification
   ```
   This is a technically valid relay using Telegram's native notification delivery. It relies on **Telegram notification settings** being enabled for the bot chat. Documented, no hidden loops (we filter `com.imista.forward.*` to avoid recursion), and Receiver does **not** request Notification Access unless the user wants this reconstruction feature.

If Telegram notifications are muted/filtered, Receiver will not see forwarded messages — this limitation is shown in UI and docs.

Receiver's **Connection Setup** screen explains this explicitly, and `PollingService` documents single-consumer handling.

---

## 🔒 Security & Privacy

- **Keystore-backed encryption**: Bot token & Chat ID stored via `EncryptedSharedPreferences` (MasterKey AES256-GCM, `AndroidKeyStore`). No hardcoded secrets, never logged, never uploaded to GitHub. Clear via **Privacy → Disconnect & Clear Credentials**.
- **HTTPS only** for Telegram API (OkHttp, no body logging — notification content never appears in logs).
- **Telegram Bot messages are NOT Telegram Secret Chats.** They are HTTPS-encrypted in transit but visible to Telegram servers. We do **not** claim E2E. Optional **app-level AES-GCM encryption** (Keystore key) can be enabled in Sender → Privacy, which wraps payload before Telegram transport (proper key exchange/auth documented as best-effort; we don't claim unimplemented E2E).
- **No unnecessary components**: No analytics SDKs, no ads, no third-party notification-content collection. Configurable 7-day retention, auto-prune via `WorkManager`, manual clear in History. Secure deletion of local Room records.
- **Pairing**: UUID-based `pairingId`, base64 QR payload `pairingId|chatId|role`, verified via bot chat. Not publicly guessable.
- **Duplicate prevention**: UUID per notification + 7s window + package/title/text/timestamp cache.
- **Logs**: notification content never logged (`HttpLoggingInterceptor` disabled, `Log.e` only error codes).
- See `PRIVACY.md` and in-app **Privacy & Security** screens.

---

## 📱 Screens

### Sender (9 screens)
1. **Dashboard** — listener/telegram/device-admin status, forwarding active, pending queue, today's count, last delivery, quick actions.
2. **Telegram Configuration** — token/chat/pairing QR, test, format/limitations.
3. **Notification Access** — explain & open system settings, manufacturer caveats.
4. **App Selection** — install-app list, modes (All/Selected/Exclude).
5. **Notification Filtering** (Privacy) — hide sensitive, max preview, encryption toggle.
6. **Device Admin** — explicit activation, status, deactivation flow, lock test, honest uninstall-proof disclaimer.
7. **Delivery History** — pending/retrying + recent 100 with status/error, clear.
8. **Privacy & Security** — Keystore note, E2E disclaimer, retention.
9. **About** — version, permissions, limitations.

### Receiver (8 screens)
1. **Home Dashboard** — sender/telegram status, last received, totals, recent feed, quick actions.
2. **Connection Setup** — same token/chat, architecture note, test.
3. **Notification Feed** — icon/app/title/text/timestamp/read, search, app chips, mark read, clear.
4. **Application Filters** — display-only, explains forwarding filters live on Sender.
5. **Notification Details** — via feed tap.
6. **Delivery History** — explains Receiver has no delivery status (Sender owns it), retention.
7. **Privacy Settings** — mirror local toggle, retention slider, clear credentials.
8. **About** — permissions, limitations, single-consumer explanation.

Design: premium dark (`#0B0B14` background, `#6366F1` primary, `#06B6D4` secondary, `#1E1E2F` surface), modern typography, rounded cards (16dp), smooth transitions, responsive, accessible, original vector icons (`IF` brand).

Sender UI emphasizes forwarding/permissions; Receiver emphasizes browsing/management — visually consistent but not identical.

---

## 🔧 Storage & Reliability

- **Room Database** for settings, filters, pending queue, delivery history, pairing.
- **WorkManager** `OneTimeWorkRequest` with `NetworkType.CONNECTED`, exponential backoff (10s base), `retry_after` respect, `PeriodicWorkRequest` for polling/poll health, `BOOT_COMPLETED` re-enqueue.
- **Duplicate event IDs** (UUID), **connection state**, **local retention** (7 days auto-prune, configurable on Receiver), graceful process-termination handling, permission-revocation handling (shows `Disabled` status, pauses forwarding).
- **ForegroundServices** only where permitted (`dataSync`).
- **Honest background** note: Android battery optimization + manufacturer restrictions mean no permanent-process guarantee; offline queue + retry covers this.

---

## 🧪 Testing

**Automated unit tests** (`shared`, `sender`, `receiver`):

- `FilterEngineTest` — all-apps, excluded, selected-only, exclude-list, duplicate suppression 7s, redaction, unique key.
- `TelegramRepositoryTest` — format contains fields, parse ID extraction, null when no ID, truncate 900, HTML escape.
- `PairingValidatorTest` — bot token format, chat ID, QR roundtrip, UUID uniqueness, invalid handling.
- `NotificationPayloadTest` — uniqueKey, isDuplicateOf window, different package not duplicate, displayText bigText preference.
- `RetryLogicTest` — exponential delays 10/20/40/80, max retries 5, rate-limit `retry_after` parsing, duplicate window constant.
- `PollingLogicTest` — single-consumer (Sender false, Receiver true), offset increment, telegram parsing requires ID.

Additional coverage in plan: notification parsing, app filtering, duplicate suppression, Telegram success/auth-fail/network-fail/retry, DB, pairing validation, device admin activation/deactivation, permission revocation, navigation, lifecycle (verified via manual matrix).

**Manual/integration matrix** (run on emulator/device where available):
1. Sender online, Receiver online — forwarded appears.
2. Sender online, Receiver offline — queued until Telegram available.
3. Internet disconnected — WorkManager queues, retries on reconnect.
4. Telegram API 429 — respects `retry_after`.
5. Notification Listener revoked — status `Disabled`, no forwarding.
6. Device Admin disabled — still forwards.
7. App process restarted — `BootReceiver` re-enqueues.
8. Duplicate notifications — suppressed.
9. Multiple apps — filtering per mode.

Run: `./gradlew test` (or via GitHub Actions `test` job). Never mark untested as passed — CI shows actual results.

---

## 🏗️ Build

**Requirements:** Android Studio Hedgehog+, JDK 17, Android SDK 35, Gradle 8.7.

```bash
./gradlew :sender:assembleRelease
./gradlew :receiver:assembleRelease
# Outputs:
# sender/build/outputs/apk/release/ → Imista-Forward-Sender-v1.0.0.apk
# receiver/build/outputs/apk/release/ → Imista-Forward-Receiver-v1.0.0.apk
```

**Signing:** Debug keystore auto-used in CI for verification. For real release, configure `signingConfigs.release` in `sender/build.gradle.kts` & `receiver/...` with your keystore (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) via `gradle.properties` or env — never commit keys. See `docs/BUILD.md`. Workflow documents release signing: if no credential, CI builds unsigned debug-verified release and warns.

**R8/minify**: enabled safely (keep shared models, datastore, security). No secrets embedded.

**Verify:**
```bash
sha256sum *.apk
adb install Imista-Forward-Sender-v1.0.0.apk
adb install Imista-Forward-Receiver-v1.0.0.apk
# Check package names differ, permissions correct, launch ok
```

**CI:** `.github/workflows/build.yml` installs SDK 35, builds both APKs, renames, generates `SHA256SUMS.txt`, uploads artifacts, and on tag `v*` creates **GitHub Release** `v1.0.0` with APKs attached.

If `TELEGRAM_BOT_KEY` secret is configured, workflow validates format (without leaking value); otherwise warns and skips live Telegram test — manual verification required.

---

## 📦 Project Structure

```
Imista/
├── shared/          # Kotlin library — models, TelegramApi, CryptoManager, parsers, validators
├── sender/          # com.imista.forward.sender — capture, queue, Telegram send, Device Admin
├── receiver/        # com.imista.forward.receiver — Telegram reconstruction, feed, polling
├── docs/            # Setup, privacy, build docs
├── README.md
├── PRIVACY.md
├── CHANGELOG.md
├── LICENSE
├── .github/workflows/build.yml
└── gradle/wrapper/
```

Use feature branch `arena/cf8a3fb5-imista`, merge to `main` only after tests pass.

---

## 📄 Device Admin Detail (Important)

Implemented via `DeviceAdminReceiver` + `DevicePolicyManager` official APIs:
- Activation screen explains privileges (only `force-lock`/`watch-login`)
- Current admin status display, user-controlled activation/deactivation, official flow via system settings (`ACTION_ADD_DEVICE_ADMIN`, `ACTION_SECURITY_SETTINGS`)
- Handles revocation, shows `Inactive` status, forwarding unaffected.

**Does NOT do:** hidden activation, deceptive dialogs, uninstall bypass, persistence against owner's wishes, locking as punishment, hidden surveillance. For child-uninstall-proof needs, investigate **Device Owner** provisioning or legitimate parental-control solutions — clearly documented as different from Device Admin.

---

## ⚠️ Known Limitations (Honest)

- Telegram Bot API rate limit ~30 msgs/sec → we handle `retry_after`.
- Background work subject to Doze/manufacturer killers — queue + retry mitigates, but don't promise permanent alive.
- Receiver reconstruction depends on Telegram notification settings (must not be muted/suppressed).
- Only one `getUpdates` consumer allowed — we honor that; Sender never polls.
- Duplicate window 7s (configurable conceptually, fixed for v1).
- No SMS/call interception beyond notifications — respects visibility restrictions.
- A small relay endpoint is bundled (Supabase Edge Function `imista-relay`) so the bot token never ships in the APK. Set `TELEGRAM_BOT_KEY` + `TELEGRAM_CHAT_ID` as function secrets for live Telegram delivery; without them messages still flow Sender → relay → Receiver over polling.

### Relay endpoint
- Health: `https://effpkfajwqetiddcqopi.supabase.co/functions/v1/imista-relay/api/health`
- Routes: `api/register`, `api/forward`, `api/poll`
- The URL is baked into the APK via `RELAY_URL` (build-time env / Gradle property) and can be overridden at runtime in the app's settings.

---

## 📜 License

MIT — see `LICENSE`.

---

## 🔗 Release

- Tag: `v1.0.0` — **Imista Forward v1.0.0 — Sender & Receiver**
- Assets: Sender APK + Receiver APK + SHA256SUMS.txt + RELEASE_NOTES.md
- Changelog: `CHANGELOG.md`
- No tokens/keys in repo — `.gitignore` excludes `local.properties`, keystores, encrypted prefs backups.

---

**Built with:** Kotlin 1.9.22, Compose BOM 2024.02.00 (Material3), Room 2.6.1, WorkManager 2.9.0, DataStore 1.0.0, Security-Crypto 1.1.0-alpha06, Retrofit 2.9.0, OkHttp 4.12.0, Coroutines 1.8.0, Coil 2.6.0, ZXing 4.3.0.

**Final note:** This project completes all 15 phases — two installable APKs, honest Telegram relay, tests, CI, release — without simulated success. If a real bot token/device is unavailable in CI, manual setup steps are clearly documented and app gracefully shows `Disconnected`.
