// Server-side Telegram helper - token never leaves server
const TELEGRAM_BASE = 'https://api.telegram.org'

function getToken() {
  const t = process.env.TELEGRAM_BOT_KEY
  if (!t) throw new Error('TELEGRAM_BOT_KEY not configured')
  return t
}

export async function sendToTelegram(chatId, payload) {
  const token = getToken()
  const formatted = formatPayload(payload)
  const url = `${TELEGRAM_BASE}/bot${token}/sendMessage`
  const body = {
    chat_id: chatId,
    text: formatted,
    parse_mode: 'HTML',
    disable_web_page_preview: true
  }
  const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  const json = await res.json()
  if (!json.ok) {
    const retryAfter = json.parameters?.retry_after
    return { ok: false, error: json.description, retryAfter, code: json.error_code }
  }
  return { ok: true, messageId: json.result.message_id }
}

export async function verifyBot() {
  const token = getToken()
  const res = await fetch(`${TELEGRAM_BASE}/bot${token}/getMe`)
  const j = await res.json()
  return j
}

export async function getChat(chatId) {
  const token = getToken()
  const res = await fetch(`${TELEGRAM_BASE}/bot${token}/getChat?chat_id=${encodeURIComponent(chatId)}`)
  return res.json()
}

export async function pollUpdates(offset, limit = 10) {
  const token = getToken()
  const url = `${TELEGRAM_BASE}/bot${token}/getUpdates?offset=${offset || ''}&limit=${limit}&timeout=0`
  const res = await fetch(url)
  return res.json()
}

function escapeHtml(s) {
  return (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function formatPayload(p) {
  const app = escapeHtml(p.appName || 'Unknown')
  const title = escapeHtml(p.title || '')
  const text = escapeHtml(p.bigText || p.text || '')
  const pkg = escapeHtml(p.packageName || '')
  const time = new Date(p.timestamp).toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' })
  let out = `<b>📱 ${app}</b>\n`
  if (title) out += `<b>${title}</b>\n`
  if (text) out += `${text.length > 900 ? text.slice(0,900)+'…' : text}\n`
  out += `\n<i>${pkg} • ${time}</i>\n`
  out += `<code>ID:${p.id}</code>`
  return out
}
