# Imista Relay — Minimal Secure Relay

Keeps `TELEGRAM_BOT_KEY` server-side, never in APK.

## Live deployments
- Production alias: https://imista-relay.vercel.app/api/health
- Project: `imista-relay` (Vercel project `prj_TxAYoAwObIZgsJgGVt0ijlsFCOkm`, root directory `relay`, framework preset `node`)
- The APK only ever knows `RELAY_URL` — no bot token is embedded.

## Endpoints
- `POST /api/register` {pairingId, deviceId, deviceName, role} → creates/links pairing
- `GET /api/register?pairingId=` → checks status, auto-discovers chatId via bot polling for `/start pairingId`
- `POST /api/forward` headers `X-Pairing-Id`, `X-Device-Id` body {payload} → stores + forwards to Telegram (if chat linked)
- `GET /api/poll?pairingId=&deviceId=&since=&limit=` → receiver polling (no single-consumer conflict, relay is single source)
- `GET /api/health` → checks token & supabase

## Env (Vercel)
- `TELEGRAM_BOT_KEY` (from GitHub Secret, never in repo)
- `TELEGRAM_CHAT_ID` (optional static chat override, or auto-link via bot poll)
- `SUPABASE_URL` + `SUPABASE_SERVICE_KEY` (optional, fallback in-memory)
- `RELAY_SHARED_SECRET` (optional HMAC)

## Deploy to Vercel
```bash
vercel --prod --env TELEGRAM_BOT_KEY=$TELEGRAM_BOT_KEY
# Or via Vercel Dashboard → Settings → Environment Variables → add TELEGRAM_BOT_KEY
```

## Supabase Tables (if using persistence)
```sql
create table pairings (
  pairing_id text primary key,
  sender_device text,
  receiver_device text,
  telegram_chat_id text,
  created_at bigint,
  status text
);
create table messages (
  id text primary key,
  pairing_id text references pairings(pairing_id),
  package_name text,
  app_name text,
  title text,
  text_content text,
  big_text text,
  timestamp bigint,
  created_at bigint,
  raw jsonb
);
```

## Security
- Token never in client, only server env.
- Pairing verification per request, no guessable codes (UUID).
- HTTPS only, CORS restricted in prod.
- No notification content in logs.

## Local Dev
```bash
cd relay
npm install
TELEGRAM_BOT_KEY=dummy SUPABASE_URL='' vercel dev
```

## Health check
`GET /api/health` returns `{ok:true,...}` for both the Android apps and CI smoke tests.
