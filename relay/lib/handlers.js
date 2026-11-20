// Shared route handlers used by both the Vercel serverless entrypoints (api/*.js)
// and the single-file Node server entrypoint (server.js).
import { createPairing, getPairing, linkTelegramChat, verifyPairing, storeMessage, fetchMessages } from './store.js'
import { pollUpdates, sendToTelegram, verifyBot } from './telegram.js'

export async function health({ res }) {
  try {
    const hasToken = !!process.env.TELEGRAM_BOT_KEY
    let botOk = false
    let botUsername = null
    if (hasToken) {
      try {
        const r = await verifyBot()
        botOk = !!r.ok
        botUsername = r.result?.username || null
      } catch (e) {}
    }
    return res.status(200).json({
      ok: true,
      service: 'imista-relay',
      timestamp: Date.now(),
      telegramConfigured: hasToken,
      botOk,
      botUsername,
      supabaseConfigured: !!(process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_KEY),
      version: '1.0.0'
    })
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message })
  }
}

export async function register({ req, res, query, body }) {
  if (req.method === 'POST') {
    try {
      const { pairingId, deviceId, role } = body || {}
      if (!pairingId || !deviceId || !role) {
        return res.status(400).json({ ok: false, error: 'pairingId, deviceId, role required' })
      }
      const pairing = await createPairing(pairingId, deviceId, role)
      return res.status(200).json({ ok: true, pairing })
    } catch (e) {
      return res.status(500).json({ ok: false, error: e.message })
    }
  }

  if (req.method === 'GET') {
    const pairingId = query.pairingId
    if (!pairingId) return res.status(400).json({ ok: false, error: 'pairingId required' })
    const pairing = await getPairing(pairingId)
    if (!pairing) return res.status(404).json({ ok: false, error: 'pairing not found' })

    const chatField = pairing.telegram_chat_id || pairing.telegramChatId
    if (!chatField) {
      try {
        const updates = await pollUpdates(undefined, 20)
        if (updates.ok && updates.result) {
          for (const u of updates.result) {
            const txt = u.message?.text || ''
            if (txt.includes(pairingId)) {
              const chatId = u.message.chat.id.toString()
              await linkTelegramChat(pairingId, chatId)
              return res.status(200).json({ ok: true, pairing: await getPairing(pairingId), linked: true, chatId })
            }
          }
        }
      } catch (e) {}
    }
    return res.status(200).json({ ok: true, pairing, linked: !!chatField })
  }

  return res.status(405).json({ ok: false, error: 'Method not allowed' })
}

export async function forward({ req, res, body }) {
  if (req.method !== 'POST') return res.status(405).json({ ok: false, error: 'Method not allowed' })
  try {
    const headers = req.headers || {}
    const pairingId = headers['x-pairing-id'] || body?.pairingId
    const deviceId = headers['x-device-id'] || body?.deviceId
    const payload = body?.payload || body

    if (!pairingId || !deviceId || !payload) {
      return res.status(400).json({ ok: false, error: 'pairingId, deviceId, payload required' })
    }
    const okPairing = await verifyPairing(pairingId, deviceId)
    if (!okPairing) return res.status(403).json({ ok: false, error: 'Unauthorized pairing' })

    await storeMessage(pairingId, payload)

    let telegramResult = null
    try {
      const pairing = await getPairing(pairingId)
      const chatId = pairing?.telegram_chat_id || pairing?.telegramChatId || process.env.TELEGRAM_CHAT_ID
      if (chatId) {
        telegramResult = await sendToTelegram(chatId, payload)
      } else {
        telegramResult = { ok: false, error: 'Telegram chat not linked yet. Receiver polling will still work.' }
      }
    } catch (e) {
      telegramResult = { ok: false, error: e.message }
    }

    return res.status(200).json({ ok: true, stored: true, telegram: telegramResult })
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message })
  }
}

export async function poll({ req, res, query }) {
  if (req.method !== 'GET') return res.status(405).json({ ok: false, error: 'Method not allowed' })
  try {
    const headers = req.headers || {}
    const pairingId = headers['x-pairing-id'] || query.pairingId
    const deviceId = headers['x-device-id'] || query.deviceId
    const since = parseInt(query.since || '0', 10)
    const limit = Math.min(parseInt(query.limit || '50', 10), 100)

    if (!pairingId || !deviceId) return res.status(400).json({ ok: false, error: 'pairingId, deviceId required' })
    const okPairing = await verifyPairing(pairingId, deviceId)
    if (!okPairing) return res.status(403).json({ ok: false, error: 'Unauthorized pairing' })

    const messages = await fetchMessages(pairingId, since, limit)
    return res.status(200).json({ ok: true, messages, count: messages.length })
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message })
  }
}
