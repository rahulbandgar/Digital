'use strict';
// Ad-hoc chat server: zero dependencies, no persistence.
// Messages are relayed to connected clients and never written anywhere.
// For reliable delivery each event is numbered and held in memory per user
// until that user's browser confirms it arrived (a phone in the background can
// leave a dead connection that silently swallows messages). Confirmed events
// are dropped immediately; everything is discarded when the user logs out.

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.env.PORT) || 3000;
const PASSWORD = process.env.CHAT_PASSWORD || 'digitalasset';
const SESSION_MS = Number(process.env.SESSION_MINUTES || 60) * 60_000; // login lasts 1 hour
const AWAY_GRACE_MS = 2 * 60_000; // announce "left" after 2 min without a connection
const MAX_USERS = Number(process.env.MAX_USERS) || 2; // people allowed in the chat at once
const MAX_OUTBOX = 500; // max unconfirmed events held per user
const MAX_MSG = 2000;
const MAX_NAME = 32;
// Behind a hosting proxy (Render, Fly, Railway...), trust X-Forwarded-For for client IP.
const TRUST_PROXY = process.env.TRUST_PROXY === '1' || !!process.env.RENDER || !!process.env.FLY_APP_NAME;

function clientIp(req) {
  if (TRUST_PROXY) {
    const xff = req.headers['x-forwarded-for'];
    if (xff) return String(xff).split(',')[0].trim();
  }
  return req.socket.remoteAddress;
}

const INDEX = fs.readFileSync(path.join(__dirname, 'public', 'index.html'));
const ADMIN = fs.readFileSync(path.join(__dirname, 'public', 'admin.html'));
// Admin password (override with the ADMIN_PASSWORD env var).
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'lalgun';
const ADMIN_SESSION_MS = 60 * 60_000;
// admin token -> expiresAt
const adminTokens = new Map();

function isAdmin(token) {
  const exp = adminTokens.get(token);
  if (!exp) return false;
  if (exp < Date.now()) { adminTokens.delete(token); return false; }
  return true;
}

function servePage(res, body) {
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'no-referrer',
  });
  res.end(body);
}

// token -> { name, expiresAt, res, outbox, present, expireTimer, awayTimer }
let seq = 0; // global event number
const sessions = new Map();
// ip -> { count, resetAt } for basic login throttling
const attempts = new Map();

function safeEqual(a, b) {
  const ha = crypto.createHash('sha256').update(String(a)).digest();
  const hb = crypto.createHash('sha256').update(String(b)).digest();
  return crypto.timingSafeEqual(ha, hb);
}

function send(res, status, obj) {
  res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(obj));
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
  return [...sessions.values()].filter((s) => s.present).map((s) => s.name);
}

// A seat is held by anyone in the room, or who just logged in and is still connecting.
// Someone gone for longer than the away grace period gives up their seat.
function seatsTaken(except) {
  const now = Date.now();
  let n = 0;
  for (const s of sessions.values()) {
    if (s === except) continue;
    if (s.present || (!s.connectedOnce && s.pendingUntil > now)) n += 1;
  }
  return n;
}

function deliver(s, event) {
  if (event.id === undefined) { // presence updates are sent live only
    if (s.res) s.res.write(`data: ${JSON.stringify(event)}\n\n`);
    return;
  }
  const frame = `id: ${event.id}\ndata: ${JSON.stringify(event)}\n\n`;
  s.outbox.push({ id: event.id, frame });
  if (s.outbox.length > MAX_OUTBOX) s.outbox.shift();
  if (s.res) s.res.write(frame);
}

function broadcast(event) {
  if (event.type === 'msg' || event.type === 'system') event.id = ++seq;
  for (const s of sessions.values()) deliver(s, event);
}

// Browser confirmed it has everything up to `upTo`: forget those events.
function ack(s, upTo) {
  const n = Number(upTo);
  if (!Number.isFinite(n) || n <= 0) return;
  let i = 0;
  while (i < s.outbox.length && s.outbox[i].id <= n) i += 1;
  if (i) s.outbox.splice(0, i);
}

function announceUsers() {
  broadcast({ type: 'users', users: online() });
}

function endSession(token, reason) {
  const s = sessions.get(token);
  if (!s) return;
  clearTimeout(s.expireTimer);
  clearTimeout(s.awayTimer);
  sessions.delete(token);
  if (s.res) {
    if (reason) s.res.write(`data: ${JSON.stringify({ type: 'ended', reason })}\n\n`);
    s.res.end();
  }
  if (s.present) {
    broadcast({ type: 'system', text: `${s.name} left`, ts: Date.now() });
    announceUsers();
  }
}

function throttled(ip) {
  const now = Date.now();
  const a = attempts.get(ip);
  if (!a || a.resetAt < now) { attempts.set(ip, { count: 1, resetAt: now + 60_000 }); return false; }
  a.count += 1;
  return a.count > 10;
}

function openStream(req, res, token, s, since) {
  if (!s.present && seatsTaken(s) >= MAX_USERS) {
    res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-store' });
    res.end(`data: ${JSON.stringify({ type: 'ended', reason: 'full' })}\n\n`);
    return endSession(token);
  }
  // A newer connection replaces any stale one for the same session.
  if (s.res) { const old = s.res; s.res = null; old.end(); }
  clearTimeout(s.awayTimer);

  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-store',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no',
  });
  res.write('retry: 3000\n\n');
  s.res = res;
  s.connectedOnce = true;

  // Re-send everything this browser hasn't confirmed yet.
  ack(s, since);
  for (const e of s.outbox) res.write(e.frame);

  if (!s.present) {
    s.present = true;
    broadcast({ type: 'system', text: `${s.name} joined`, ts: Date.now() });
  }
  announceUsers();

  const ping = setInterval(() => res.write('data: {"type":"ping"}\n\n'), 20_000);
  req.on('close', () => {
    clearInterval(ping);
    if (s.res !== res) return; // already replaced by a newer connection
    s.res = null;
    if (!sessions.has(token)) return;
    s.awayTimer = setTimeout(() => {
      if (s.res || !sessions.has(token)) return;
      s.present = false;
      broadcast({ type: 'system', text: `${s.name} left`, ts: Date.now() });
      announceUsers();
    }, AWAY_GRACE_MS);
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const ip = clientIp(req);

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

    if (req.method === 'GET' && (url.pathname === '/admin' || url.pathname === '/admin/')) {
      return servePage(res, ADMIN);
    }

    if (req.method === 'POST' && url.pathname === '/api/admin/login') {
      if (!ADMIN_PASSWORD) return send(res, 503, { error: 'Admin is not set up' });
      if (throttled(ip)) return send(res, 429, { error: 'Too many attempts. Wait a minute.' });
      const { password } = await readJson(req);
      if (!safeEqual(password || '', ADMIN_PASSWORD)) return send(res, 401, { error: 'Wrong password' });
      const token = crypto.randomBytes(24).toString('hex');
      adminTokens.set(token, Date.now() + ADMIN_SESSION_MS);
      return send(res, 200, { token });
    }

    if (req.method === 'POST' && url.pathname === '/api/admin/users') {
      const { token } = await readJson(req);
      if (!isAdmin(token)) return send(res, 401, { error: 'Not signed in' });
      const users = [...sessions.values()].map((s) => ({
        id: s.id, name: s.name, joinedAt: s.joinedAt, expiresAt: s.expiresAt,
        status: s.res ? 'online' : s.present ? 'away' : 'offline',
      }));
      return send(res, 200, { users, maxUsers: MAX_USERS });
    }

    if (req.method === 'POST' && url.pathname === '/api/admin/remove') {
      const { token, id } = await readJson(req);
      if (!isAdmin(token)) return send(res, 401, { error: 'Not signed in' });
      const ids = id === '*' ? null : new Set([id]);
      for (const [t, s] of [...sessions]) if (!ids || ids.has(s.id)) endSession(t, 'removed');
      return send(res, 200, { ok: true });
    }

    if (req.method === 'POST' && url.pathname === '/api/admin/logout') {
      const { token } = await readJson(req);
      adminTokens.delete(token);
      return send(res, 200, { ok: true });
    }

    if (req.method === 'GET' && url.pathname === '/healthz') {
      return send(res, 200, { ok: true });
    }

    if (req.method === 'POST' && url.pathname === '/api/login') {
      if (throttled(ip)) return send(res, 429, { error: 'Too many attempts. Wait a minute.' });
      const { password, name } = await readJson(req);
      if (!safeEqual(password || '', PASSWORD)) return send(res, 401, { error: 'Wrong password' });
      const clean = String(name || '').replace(/\s+/g, ' ').trim().slice(0, MAX_NAME);
      if (!clean) return send(res, 400, { error: 'Enter a display name' });
      for (const [t, s] of sessions) {
        if (s.name.toLowerCase() !== clean.toLowerCase()) continue;
        if (s.present) return send(res, 409, { error: 'That name is already in the room' });
        endSession(t); // abandoned session with the same name
      }
      if (seatsTaken() >= MAX_USERS) return send(res, 403, { error: 'Full' });
      const token = crypto.randomBytes(24).toString('hex');
      const expiresAt = Date.now() + SESSION_MS;
      const s = { id: crypto.randomBytes(8).toString('hex'), joinedAt: Date.now(), name: clean, expiresAt, res: null, outbox: [], present: false, awayTimer: null,
        connectedOnce: false, pendingUntil: Date.now() + 30_000 };
      s.expireTimer = setTimeout(() => endSession(token, 'expired'), SESSION_MS);
      sessions.set(token, s);
      return send(res, 200, { token, name: clean, expiresAt });
    }

    if (req.method === 'POST' && url.pathname === '/api/session') {
      const { token } = await readJson(req);
      const s = sessions.get(token);
      if (!s) return send(res, 401, { error: 'Session ended' });
      return send(res, 200, { name: s.name, expiresAt: s.expiresAt });
    }

    if (req.method === 'GET' && url.pathname === '/api/stream') {
      const token = url.searchParams.get('token');
      const s = sessions.get(token);
      if (!s) return send(res, 401, { error: 'Session ended' });
      return openStream(req, res, token, s, url.searchParams.get('since'));
    }

    if (req.method === 'POST' && url.pathname === '/api/send') {
      const { token, text, ack: upTo } = await readJson(req);
      const s = sessions.get(token);
      if (!s) return send(res, 401, { error: 'Session ended' });
      ack(s, upTo);
      const msg = String(text || '').trim().slice(0, MAX_MSG);
      if (!msg) return send(res, 400, { error: 'Empty message' });
      broadcast({ type: 'msg', from: s.name, text: msg, ts: Date.now() });
      return send(res, 200, { ok: true });
    }

    if (req.method === 'POST' && url.pathname === '/api/ack') {
      const { token, seq: upTo } = await readJson(req);
      const s = sessions.get(token);
      if (!s) return send(res, 401, { error: 'Session ended' });
      ack(s, upTo);
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

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Ad-hoc chat running on http://localhost:${PORT}`);
});
