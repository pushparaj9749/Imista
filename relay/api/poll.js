import { verifyPairing, fetchMessages } from '../lib/store.js'

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Pairing-Id, X-Device-Id')
  if (req.method === 'OPTIONS') return res.status(200).end()
  if (req.method !== 'GET') return res.status(405).json({ ok: false })

  try {
    const pairingId = req.headers['x-pairing-id'] || req.query.pairingId
    const deviceId = req.headers['x-device-id'] || req.query.deviceId
    const since = parseInt(req.query.since || '0', 10)
    const limit = Math.min(parseInt(req.query.limit || '50', 10), 100)

    if (!pairingId || !deviceId) return res.status(400).json({ ok: false, error: 'pairingId, deviceId required' })
    const ok = await verifyPairing(pairingId, deviceId)
    if (!ok) return res.status(403).json({ ok: false, error: 'Unauthorized pairing' })

    const messages = await fetchMessages(pairingId, since, limit)
    return res.status(200).json({ ok: true, messages, count: messages.length })
  } catch (e) {
    console.error(e)
    return res.status(500).json({ ok: false, error: e.message })
  }
}
