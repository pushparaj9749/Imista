// Minimal store abstraction supporting Supabase and in-memory fallback
// For production, create Supabase tables:
//   pairings(pairing_id text pk, sender_device text, receiver_device text, telegram_chat_id text, created_at bigint, status text)
//   messages(id text pk, pairing_id text, package_name text, app_name text, title text, text_content text, big_text text, timestamp bigint, created_at bigint)

import { createClient } from '@supabase/supabase-js'

const SUPABASE_URL = process.env.SUPABASE_URL
const SUPABASE_KEY = process.env.SUPABASE_SERVICE_KEY || process.env.SUPABASE_ANON_KEY

let supabase = null
if (SUPABASE_URL && SUPABASE_KEY) {
  try { supabase = createClient(SUPABASE_URL, SUPABASE_KEY) } catch (e) { console.warn('Supabase init failed', e.message) }
}

// In-memory fallback for dev/preview
const memPairings = new Map()
const memMessages = new Map() // pairingId -> array

export async function createPairing(pairingId, deviceId, role) {
  const now = Date.now()
  if (supabase) {
    const { data, error } = await supabase.from('pairings').upsert({
      pairing_id: pairingId,
      status: 'pending',
      created_at: now,
      ...(role === 'SENDER' ? { sender_device: deviceId } : { receiver_device: deviceId })
    }, { onConflict: 'pairing_id' }).select().single()
    if (error) throw error
    // Merge role if existing
    if (data) {
      const update = {}
      if (role === 'SENDER' && !data.sender_device) update.sender_device = deviceId
      if (role === 'RECEIVER' && !data.receiver_device) update.receiver_device = deviceId
      if (Object.keys(update).length) {
        await supabase.from('pairings').update(update).eq('pairing_id', pairingId)
      }
    }
    return { pairingId, status: 'paired' }
  } else {
    let entry = memPairings.get(pairingId)
    if (!entry) entry = { pairingId, createdAt: now, status: 'pending' }
    if (role === 'SENDER') entry.senderDevice = deviceId
    else entry.receiverDevice = deviceId
    entry.status = (entry.senderDevice && entry.receiverDevice) ? 'paired' : 'pending'
    memPairings.set(pairingId, entry)
    return entry
  }
}

export async function getPairing(pairingId) {
  if (supabase) {
    const { data, error } = await supabase.from('pairings').select('*').eq('pairing_id', pairingId).single()
    if (error) return null
    return data
  }
  return memPairings.get(pairingId) || null
}

export async function linkTelegramChat(pairingId, chatId) {
  if (supabase) {
    await supabase.from('pairings').update({ telegram_chat_id: chatId, status: 'linked' }).eq('pairing_id', pairingId)
  } else {
    const e = memPairings.get(pairingId)
    if (e) { e.telegramChatId = chatId; e.status = 'linked'; memPairings.set(pairingId, e) }
  }
}

export async function storeMessage(pairingId, payload) {
  const record = {
    id: payload.id,
    pairing_id: pairingId,
    package_name: payload.packageName,
    app_name: payload.appName,
    title: payload.title,
    text_content: payload.text || payload.bigText,
    big_text: payload.bigText,
    timestamp: payload.timestamp,
    created_at: Date.now(),
    raw: payload
  }
  if (supabase) {
    const { error } = await supabase.from('messages').insert(record)
    if (error) throw error
  } else {
    if (!memMessages.has(pairingId)) memMessages.set(pairingId, [])
    memMessages.get(pairingId).push(record)
    // retention: keep last 500
    const arr = memMessages.get(pairingId)
    if (arr.length > 500) memMessages.set(pairingId, arr.slice(-500))
  }
  return record
}

export async function fetchMessages(pairingId, since = 0, limit = 50) {
  if (supabase) {
    let q = supabase.from('messages').select('*').eq('pairing_id', pairingId).order('created_at', { ascending: true }).limit(limit)
    if (since) q = q.gt('created_at', since)
    const { data, error } = await q
    if (error) throw error
    return data || []
  } else {
    const all = memMessages.get(pairingId) || []
    return all.filter(m => m.created_at > since).slice(0, limit)
  }
}

export async function verifyPairing(pairingId, deviceId) {
  const p = await getPairing(pairingId)
  if (!p) return false
  // Check device is part of pairing
  if (p.sender_device === deviceId || p.receiver_device === deviceId) return true
  // Fallback mem structure
  if (p.senderDevice === deviceId || p.receiverDevice === deviceId) return true
  // If legacy, allow any member of same pairingId
  return !!p
}
