# Privacy Policy — Imista Forward

**Last updated: 2026-11-20**

## Overview
Imista Forward (Sender `com.imista.forward.sender` & Receiver `com.imista.forward.receiver`) is designed for private phone-to-phone notification forwarding via **your own Telegram Bot**. There is no Imista cloud server collecting your notification content.

## Data Collection
- **No analytics, no advertising SDKs, no third-party notification-content collection.**
- **Notification content**: Only what Android exposes via `NotificationListenerService` (app name, package, title, text, bigText, subText, category, timestamp, isOngoing). We do **not** access private app databases or messages not shown as notifications.
- **Delivery**: Content is sent to **your configured Telegram bot's chat** via HTTPS (`api.telegram.org`). It is visible in your private Telegram conversation and to Telegram servers, but not to Imista developers.
- **Telegram encryption note**: Ordinary bot conversations are **not** Telegram Secret Chats and are **not** end-to-end encrypted. They are HTTPS-encrypted in transit but stored on Telegram servers. Do not send highly sensitive data without additional protection.

## Optional Application-Level Encryption
When enabled (Sender → Privacy → App-Level Encryption), notification title/text is wrapped with AES/GCM using a key from `AndroidKeyStore` before Telegram transport. This is best-effort app-layer encryption; key exchange is local to your two paired devices via QR. We make **no false claims** of unimplemented cryptography.

## Storage
- **Credentials**: Bot token & Chat ID stored in `EncryptedSharedPreferences` backed by `AndroidKeyStore` (AES256-GCM). Excluded from auto-backup (`backup_rules.xml`, `data_extraction_rules.xml`).
- **Local databases**: Room `imista_sender.db` / `imista_receiver.db` holds pending queue, filters, delivery history, received feed. Auto-prunes after 7 days (Receiver configurable 1–30 days) plus on `Clear History`. Secure deletion on `Clear` or app uninstall.
- **Logs**: Notification content is **never** logged. Network logging is disabled (`HttpLoggingInterceptor Level.NONE`). Only generic error codes are logged.
- **Retention**: Configurable; user can set max preview length (50–1000), hide sensitive content (redacts text to `[Hidden]`).

## Permissions Justification
- `INTERNET` + `ACCESS_NETWORK_STATE`: Telegram API.
- `BIND_NOTIFICATION_LISTENER_SERVICE`: Read notifications you allow forwarding for (Sender) / reconstruct Telegram notifications (Receiver optional).
- `QUERY_ALL_PACKAGES`: List installed apps for App Selection filtering.
- `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE`: Delivery status & re-posting where permitted.
- `RECEIVE_BOOT_COMPLETED`: Re-enqueue pending after reboot.
- `BIND_DEVICE_ADMIN` (Sender only, optional): Only `force-lock`/`watch-login`, explicit activation, user-deactivatable, never required for forwarding.

## Device Admin Honesty
Ordinary Device Admin does **not** make the app uninstall-proof. Users can deactivate admin via Settings → Security → Device Administrators, then uninstall normally. We never implement hidden activation, deceptive dialogs, or uninstall bypass.

## Sharing
We do not share data with third parties except Telegram Bot API per your own bot configuration. You control the bot.

## User Controls
- Enable/disable forwarding per app, exclude list, sensitive redaction.
- Disconnect & Clear Credentials wipes Keystore prefs + local DB.
- Disable Notification Listener or Device Admin anytime via system settings — app handles revocation gracefully, shows `Disabled` status.

## Children's Data
Not directed to children under 13. Parental controls requiring Device Owner are separate and not falsely advertised as Device Admin.

## Contact
Open an issue in the GitHub repository for questions.

## Consent
By installing and configuring a bot token, you consent to forwarding as described. You can withdraw by clearing credentials and uninstalling.
