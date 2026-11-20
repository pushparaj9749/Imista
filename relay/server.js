// Single Node entrypoint for Vercel (framework preset: node).
// Routes /api/* to the shared handlers so the relay works without a build step.
import http from 'node:http'
import { health, register, forward, poll } from './lib/handlers.js'

const ROUTES = {
  '/api/health': health,
  '/api/register': register,
  '/api/forward': forward,
  '/api/poll': poll
}

function makeRes(res) {
  const api = {
    statusCode: 200,
    headers: { 'Access-Control-Allow-Origin': '*', 'Content-Type': 'application/json' },
    status(code) {
      api.statusCode = code
      return api
    },
    setHeader(name, value) {
      api.headers[name] = value
    },
    json(obj) {
      res.writeHead(api.statusCode, api.headers)
      res.end(JSON.stringify(obj))
      return api
    },
    end(data) {
      res.writeHead(api.statusCode, api.headers)
      res.end(data)
      return api
    }
  }
  return api
}

function readBody(req) {
  return new Promise((resolve) => {
    let raw = ''
    req.on('data', (chunk) => {
      raw += chunk
      if (raw.length > 1_000_000) req.destroy()
    })
    req.on('end', () => {
      if (!raw) return resolve({})
      try {
        resolve(JSON.parse(raw))
      } catch {
        resolve({})
      }
    })
    req.on('error', () => resolve({}))
  })
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`)
  const path = url.pathname.replace(/\/+$/, '') || '/'
  const handler = ROUTES[path]

  if (req.method === 'OPTIONS') {
    res.writeHead(200, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Pairing-Id, X-Device-Id'
    })
    return res.end()
  }

  if (!handler) {
    if (path === '/' || path === '') {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      return res.end(JSON.stringify({ ok: true, service: 'imista-relay', endpoints: Object.keys(ROUTES) }))
    }
    res.writeHead(404, { 'Content-Type': 'application/json' })
    return res.end(JSON.stringify({ ok: false, error: 'Not found' }))
  }

  const apiRes = makeRes(res)
  const body = req.method === 'POST' ? await readBody(req) : {}
  try {
    await handler({ req, res: apiRes, query: Object.fromEntries(url.searchParams), body })
  } catch (e) {
    apiRes.status(500).json({ ok: false, error: e.message })
  }
})

const port = process.env.PORT || 3000
server.listen(port, '0.0.0.0', () => {
  console.log(`imista-relay listening on ${port}`)
})

export default server
