import { verifyBot } from '../lib/telegram.js'

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  if (req.method === 'OPTIONS') return res.status(200).end()
  try {
    const hasToken = !!process.env.TELEGRAM_BOT_KEY
    let botOk = false
    let botInfo = null
    if (hasToken) {
      try { const r = await verifyBot(); botOk = r.ok; botInfo = r.result?.username } catch (e) {}
    }
    return res.status(200).json({
      ok: true,
      service: 'imista-relay',
      timestamp: Date.now(),
      telegramConfigured: hasToken,
      botOk,
      botUsername: botInfo,
      supabaseConfigured: !!(process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_KEY),
      version: '1.0.0'
    })
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message })
  }
}
