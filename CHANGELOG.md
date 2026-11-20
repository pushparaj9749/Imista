# Changelog

All notable changes to Imista Forward are documented here.

## [1.0.0] — 2026-11-20

### Added
- **Two independent APKs**: `com.imista.forward.sender` & `com.imista.forward.receiver` (API 26+, target 35, Kotlin, Compose, MVVM, Room, WorkManager).
- **Telegram Bot relay**: user-provided token, chat ID pairing, connection test, `sendMessage` with retry/exponential backoff, rate-limit `retry_after`, duplicate prevention (UUID + 7s window), delivery tracking.
- **Sender**: NotificationListenerService (WhatsApp, SMS, Gmail, Outlook, Instagram, Telegram, Discord, any app posting notifications), filtering (All/Selected/Exclude), sensitive redaction, preview length, pending queue, delivery history, last delivery, pending count.
- **Device Admin** (Sender optional): official `DeviceAdminReceiver`/`DevicePolicyManager`, explicit activation screen, status, deactivation flow, lock test, honest uninstall-proof disclaimer.
- **Receiver**: Honest architecture — sole `getUpdates` poller (offset handling, 15-min WorkManager, no competing consumer), Telegram notification reconstruction (`TelegramNotificationService`) for forwarded messages, local feed (search, app chips, read/unread), privacy controls, clear history, connection status.
- **Security**: Keystore-backed EncryptedSharedPreferences, HTTPS only, no hardcoded secrets, no content in logs, no analytics/ads, auto-prune 7 days, optional AES-GCM app encryption, .gitignore for credentials.
- **UI**: Premium dark theme (#0B0B14, #6366F1, #06B6D4), 9 Sender + 8 Receiver screens, dashboard metrics, smooth Compose navigation, original IF logos.
- **Reliability**: Room + WorkManager queue, process-restart recovery, boot receiver, permission revocation handling, manufacturer caveats documented.
- **Tests**: FilterEngine, Telegram formatting/parsing, pairing validator, payload duplicate, retry/rate-limit, polling single-consumer, etc.
- **CI/CD**: GitHub Actions test + build + release workflow, handling `TELEGRAM_BOT_KEY` secret without leak, building both APKs, checksums, release notes.
- **Docs**: README (setup, pairing, honesty, permissions, limitations), PRIVACY.md (Keystore, retention, Device Admin truth), BUILD.md, Telegram setup, permissions.

### Known Limitations
- Depends on Telegram notifications enabled for Receiver reconstruction.
- Single `getUpdates` consumer respected.
- Background work not permanently guaranteed on Android.
- Rate limits ~30 msg/sec.

### Security Notes
- Bot messages not Secret-Chat E2E; app-level encryption optional.
