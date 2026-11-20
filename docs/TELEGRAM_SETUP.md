# Telegram Bot Setup — Imista Forward

## Create Bot
1. Telegram → search `@BotFather` → Start
2. `/newbot` → choose name e.g., `Imista Forward Bot` → username e.g., `imista_forward_123_bot` (must end with `bot`)
3. Copy token: `123456789:AAH...` (format `digits:alphanumeric_-`)

**You must create your own bot. Never hardcode token in APK.**

## Get Chat ID
1. On **both phones**, search your bot username → Start → send `Hello`.
2. Option A: Message `@userinfobot` → it replies with your chat ID (e.g., `123456789`).
3. Option B: In browser, open `https://api.telegram.org/bot<TOKEN>/getUpdates` (replace `<TOKEN>` with real token) → look for `"chat":{"id":123456789}`.

The Chat ID for private chats is your user ID (positive, ~9 digits). For groups/channels it's negative (not needed here — use private).

## Test via Browser (optional)
```bash
curl -X POST https://api.telegram.org/bot<TOKEN>/sendMessage \
  -H "Content-Type: application/json" \
  -d '{"chat_id":"<CHAT_ID>","text":"Hello from cURL"}'
```
You should see message in Telegram.

## Configure Apps
- Sender → Telegram Configuration → paste Token + Chat ID → Test & Send Verification.
- Receiver → Connection Setup → same Token + Chat ID → Test.

Both use **same** bot and **same** chat ID (your private chat). Sender sends to that chat; Receiver reconstructs via Telegram notifications for that chat.

## Limitations Explained
- **Not E2E**: Bot messages use HTTPS, not Secret Chats. Don't claim E2E.
- **Not generic push**: Bot API doesn't push sent messages to another app's `getUpdates`. Hence Receiver uses notification listening.
- **Rate limit**: ~30 msgs/sec globally; we handle `retry_after`.
- **Single consumer**: Only Receiver polls `getUpdates`; Sender never does.

## Security
- Store token via Keystore (EncryptedSharedPreferences).
- Never commit token, don't log content, exclude from backups.
- If token leaks, revoke via `@BotFather` → `/revoke`.

## TELEGRAM_BOT_KEY Secret (CI)
If you set GitHub secret `TELEGRAM_BOT_KEY` with your token, CI workflow validates format without exposing value. It does NOT embed token in APK. Leave empty if not testing live.
