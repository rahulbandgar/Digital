# Ad-hoc Chat

A password-protected group chat that runs in any browser. **No history is stored** —
messages are relayed live to whoever is connected and then forgotten. Nothing is written
to disk, a database, or browser storage. Refresh or leave, and the conversation is gone.

## Run

Requires Node.js 18+. No dependencies to install.

```bash
cd adhoc-chat
npm start            # or: node server.js
```

Open http://localhost:3000, enter a display name and the password **`digitalasset`**.
Share the URL (e.g. `http://<your-ip>:3000`) with others on your network to chat together.

## Options

| Env var         | Default        | Purpose               |
|-----------------|----------------|-----------------------|
| `PORT`          | `3000`         | HTTP port             |
| `CHAT_PASSWORD` | `digitalasset` | Password to join chat |

## How it works

- `server.js` — plain Node `http` server. Password is checked server-side; a successful
  login gets a one-time in-memory session token.
- Live updates use Server-Sent Events (`/api/stream`); messages are sent via `/api/send`
  and broadcast to everyone connected. Only the list of online users is kept in memory.
- Login attempts are throttled (10/minute per IP); duplicate names are rejected.
- For use over the internet, put it behind HTTPS (e.g. a reverse proxy) so the password
  and messages are encrypted in transit.
