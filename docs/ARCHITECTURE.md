# Architecture — Imista Forward

## Overview
```
[Sender Phone] --NotificationListener--> [FilterEngine] --Room Queue--> [TelegramRepository.sendMessage] --HTTPS--> [Telegram Bot API]
                                                                                                         |
                                                                                                         v
                                                                          [Telegram App on Receiver Phone posts notification]
                                                                                                         |
[Receiver Phone] --TelegramNotificationService (captures Telegram notif)--> [Parse ID:] --> [Room received_notifications] --> [Feed UI + optional mirror]
                                                                          ^
                                                                          |
                                                               [PollingWorker (15m) — getUpdates offset handling, pairing commands]
```

## Modules
- **shared**: models (NotificationPayload, Telegram models, PairingInfo, Filters), security (CryptoManager + EncryptedSharedPreferences + AES-GCM), network (TelegramApi + TelegramRepository + format/parse), util (NotificationParser, FilterEngine, Constants, PairingValidator).
- **sender**: MVVM, Room (pending, history, filters, settings), SecureStorage, NotificationService, ForwardingService (WorkManager with backoff, rate-limit, deduplication), DeviceAdminReceiver, Compose UI (9 screens).
- **receiver**: MVVM, Room (received, settings), SecureStorage, TelegramNotificationService (reconstruction), PollingService (single-consumer getUpdates), NotificationHelper (mirror), Compose UI (8 screens).

## Key Design Decisions

### Telegram Relay Honesty
- Sender never polls getUpdates; only Receiver polls, avoiding single-consumer theft.
- Receiver polling uses `offset = maxUpdateId + 1`, persists in SecureStorage.
- Forwarded content not fetched via getUpdates (impossible) → reconstructed via Telegram notification listener. Dependency documented.
- No Firebase/Supabase — only Telegram Bot API.

### Security
- Keystore-backed storage, no hardcoded token, no log of content, HTTPS only, no analytics.
- Pairing: UUID, base64 QR `pairingId|chatId|role`, verified via bot chat.
- Optional app-level encryption: Keystore AES/GCM, documented as best-effort, not claimed as Telegram E2E.

### Reliability
- Offline queue + WorkManager exponential backoff + retry_after handling.
- Duplicate suppression: UUID + 7s window + content hash.
- Prune 7-day retention, boot re-enqueue, permission revocation handling.
- ForegroundService only dataSync, no promise of eternal alive.

### Device Admin
- Standard Receiver + PolicyManager, explicit UI, deactivation flow, status, lockNow() test, honest disclaimer.

## Data Flow
1. `onNotificationPosted` → parse → FilterEngine (mode + excluded/selected + duplicate cache + sensitive redaction) → insert Room PENDING → enqueue ForwardWorker.
2. ForwardWorker → get token/chat → repo.sendNotification (formatPayload with HTML + ID) → on success mark SENT + history + lastDelivery, else retry with backoff or fail after 5.
3. RetryAllWorker on boot/network → re-enqueue pendings, prune old.
4. Receiver's TelegramNotificationService filters pkg=telegram.* + ID marker → parse → insert received → update lastReceived → optional mirror (avoid loop via own pkg filter).
5. PollingWorker validates bot/chat, handles /pair commands, updates lastPoll/lastError, persists offset.

## Testing
Unit tests cover parsing, filtering, duplicate, Telegram format/parse, pairing, retry, polling offset. Manual matrix covers online/offline, rate limit, revocation, restart, etc. CI runs `testDebugUnitTest` per module.

## Limits
- Telegram notifications must be enabled for reconstruction.
- Background work subject to OEM killers.
- No E2E without optional encryption.
