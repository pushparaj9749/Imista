import { createPairing, getPairing, linkTelegramChat } from '../lib/store.js'
import { pollUpdates } from '../lib/telegram.js'

export default async function handler(req, res) {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')
  if (req.method === 'OPTIONS') return res.status(200).end()

  if (req.method === 'POST') {
    try {
      const { pairingId, deviceId, deviceName, role } = req.body || {}
      if (!pairingId || !deviceId || !role) return res.status(400).json({ ok: false, error: 'pairingId, deviceId, role required' })
      // Basic pairingId validation (UUID)
      const pairing = await createPairing(pairingId, deviceId, role)
      return res.status(200).json({ ok: true, pairing })
    } catch (e) {
      console.error(e)
      return res.status(500).json({ ok: false, error: e.message })
    }
  }

  if (req.method === 'GET') {
    // Verify pairing status or auto-link telegram chat by scanning bot updates for /start pairingId
    const { pairingId } = req.query
    if (!pairingId) return res.status(400).json({ ok: false, error: 'pairingId required' })
    const pairing = await getPairing(pairingId)
    if (!pairing) return res.status(404).json({ ok: false, error: 'pairing not found' })

    // If not yet linked, try to discover chatId via bot poll (best-effort)
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
      } catch (e) { /* ignore */ }
    }

    return res.status(200).json({ ok: true, pairing, linked: !!chatField })
  }

  return res.status(405).json({ ok: false, error: 'Method not allowed' })
}
