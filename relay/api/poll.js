import { poll } from '../lib/handlers.js'

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Pairing-Id, X-Device-Id')
  if (req.method === 'OPTIONS') return res.status(200).end()
  return poll({ req, res, query: req.query || {} })
}
