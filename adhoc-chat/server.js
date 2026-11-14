'use strict';
// Ad-hoc chat server: zero dependencies, no persistence.
// Messages are relayed to connected clients and immediately forgotten.

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.env.PORT) || 3000;
const PASSWORD = process.env.CHAT_PASSWORD || 'digitalasset';
const MAX_MSG = 2000;
const MAX_NAME = 32;

const INDEX = fs.readFileSync(path.join(__dirname, 'public', 'index.html'));

// token -> { name, res (SSE response or null), timer }
const sessions = new Map();
// ip -> { count, resetAt } for basic login throttling
const attempts = new Map();

function safeEqual(a, b) {
  const ha = crypto.createHash('sha256').update(String(a)).digest();
  const hb = crypto.createHash('sha256').update(String(b)).digest();
  return crypto.timingSafeEqual(ha, hb);
}

function send(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
  res.end(body);
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    req.on('data', (c) => {
      data += c;
      if (data.length > 16 * 1024) { reject(new Error('too large')); req.destroy(); }
    });
    req.on('end', () => { try { resolve(JSON.parse(data || '{}')); } catch (e) { reject(e); } });
    req.on('error', reject);
  });
}

function online() {
  return [...sessions.values()].filter((s) => s.res).map((s) => s.name);
}

function broadcast(event) {
  const frame = `data: ${JSON.stringify(event)}\n\n`;
  for (const s of sessions.values()) if (s.res) s.res.write(frame);
}

function endSession(token) {
  const s = sessions.get(token);
  if (!s) return;
  clearTimeout(s.timer);
  sessions.delete(token);
  if (s.res) {
    broadcast({ type: 'system', text: `${s.name} left`, ts: Date.now() });
    broadcast({ type: 'users', users: online() });
  }
}

function throttled(ip) {
  const now = Date.now();
  const a = attempts.get(ip);
  if (!a || a.resetAt < now) { attempts.set(ip, { count: 1, resetAt: now + 60_000 }); return false; }
  a.count += 1;
  return a.count > 10;
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const ip = req.socket.remoteAddress;

  try {
    if (req.method === 'GET' && url.pathname === '/') {
      res.writeHead(200, {
        'Content-Type': 'text/html; charset=utf-8',
        'Cache-Control': 'no-store',
        'X-Content-Type-Options': 'nosniff',
        'Referrer-Policy': 'no-referrer',
      });
      return res.end(INDEX);
    }

    if (req.method === 'POST' && url.pathname === '/api/login') {
      if (throttled(ip)) return send(res, 429, { error: 'Too many attempts. Wait a minute.' });
      const { password, name } = await readJson(req);
      if (!safeEqual(password || '', PASSWORD)) return send(res, 401, { error: 'Wrong password' });
      const clean = String(name || '').replace(/\s+/g, ' ').trim().slice(0, MAX_NAME);
      if (!clean) return send(res, 400, { error: 'Enter a display name' });
      if (online().some((n) => n.toLowerCase() === clean.toLowerCase())) {
        return send(res, 409, { error: 'That name is already in the room' });
      }
      const token = crypto.randomBytes(24).toString('hex');
      // Token must open a stream within 30s or it expires.
      const timer = setTimeout(() => endSession(token), 30_000);
      sessions.set(token, { name: clean, res: null, timer });
      return send(res, 200, { token, name: clean });
    }

    if (req.method === 'GET' && url.pathname === '/api/stream') {
      const token = url.searchParams.get('token');
      const s = sessions.get(token);
      if (!s || s.res) return send(res, 401, { error: 'Invalid session' });
      clearTimeout(s.timer);
      res.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-store',
        Connection: 'keep-alive',
        'X-Accel-Buffering': 'no',
      });
      res.write(': connected\n\n');
      s.res = res;
      const ping = setInterval(() => res.write(': ping\n\n'), 25_000);
      req.on('close', () => { clearInterval(ping); endSession(token); });
      broadcast({ type: 'system', text: `${s.name} joined`, ts: Date.now() });
      broadcast({ type: 'users', users: online() });
      return;
    }

    if (req.method === 'POST' && url.pathname === '/api/send') {
      const { token, text } = await readJson(req);
      const s = sessions.get(token);
      if (!s || !s.res) return send(res, 401, { error: 'Session ended' });
      const msg = String(text || '').trim().slice(0, MAX_MSG);
      if (!msg) return send(res, 400, { error: 'Empty message' });
      broadcast({ type: 'msg', from: s.name, text: msg, ts: Date.now() });
      return send(res, 200, { ok: true });
    }

    if (req.method === 'POST' && url.pathname === '/api/logout') {
      const { token } = await readJson(req);
      endSession(token);
      return send(res, 200, { ok: true });
    }

    send(res, 404, { error: 'Not found' });
  } catch (e) {
    send(res, 400, { error: 'Bad request' });
  }
});

server.listen(PORT, () => {
  console.log(`Ad-hoc chat running on http://localhost:${PORT}`);
});
