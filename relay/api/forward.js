import { verifyPairing, storeMessage, getPairing } from '../lib/store.js'
import { sendToTelegram } from '../lib/telegram.js'

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Pairing-Id, X-Device-Id')
  if (req.method === 'OPTIONS') return res.status(200).end()
  if (req.method !== 'POST') return res.status(405).json({ ok: false, error: 'Method not allowed' })

  try {
    const pairingId = req.headers['x-pairing-id'] || req.body?.pairingId
    const deviceId = req.headers['x-device-id'] || req.body?.deviceId
    const payload = req.body?.payload || req.body

    if (!pairingId || !deviceId || !payload) return res.status(400).json({ ok: false, error: 'pairingId, deviceId, payload required' })

    const ok = await verifyPairing(pairingId, deviceId)
    if (!ok) return res.status(403).json({ ok: false, error: 'Unauthorized pairing' })

    // Store for receiver polling
    await storeMessage(pairingId, payload)

    // Also forward to Telegram if chat linked (best-effort, don't fail on Telegram error)
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
    console.error(e)
    return res.status(500).json({ ok: false, error: e.message })
  }
}
