# Ad-hoc Chat

A password-protected chat for two people that runs in any browser. Messages are kept
for **8 hours**, then deleted from the server's memory and from everyone's screen, and
each login lasts 8 hours. Nothing is ever written to disk or a database, and a server
restart wipes everything.

## Run

Requires Node.js 18+. No dependencies to install.

```bash
cd adhocda
npm start            # or: node server.js
```

Open http://localhost:3000, enter a display name and the password **`digitalasset`**.
Share the URL (e.g. `http://<your-ip>:3000`) with the other person.

## Options

| Env var          | Default        | Purpose |
|------------------|----------------|---------|
| `PORT`           | `3000`         | HTTP port |
| `CHAT_PASSWORD`  | `digitalasset` | Password to join |
| `ADMIN_PASSWORD` | `lalgun`       | Password for the `/admin` page |
| `MAX_USERS`      | `2`            | People allowed in at once |
| `SESSION_HOURS`  | `8`            | How long a login lasts and how long messages are kept |

## Admin page

Open `/admin` (e.g. `http://localhost:3000/admin`) and sign in with the admin password **`lalgun`**.
It lists everyone (online, away, or not connected) and lets you remove a person or
everyone. Removed people see "You were removed" and return to the login screen.

## How it works

- `server.js` — plain Node `http` server. Passwords are checked server-side; a successful
  login gets an in-memory session token (kept in that browser tab's `sessionStorage`
  so a reloaded tab can resume).
- Live updates use Server-Sent Events (`/api/stream`); messages are sent via `/api/send`.
- Every message is numbered and kept in memory for 8 hours. When someone joins,
  reloads, or reconnects, they receive whatever they haven't seen from the last 8 hours.
  This also covers phones that silently drop the connection while in another app.
- Reply to a specific message like WhatsApp: swipe it right on a phone, or hover and click
  the reply arrow on a computer. The reply shows a quote of the original; tapping the quote
  jumps to it. The server attaches the quote from its in-memory history.
- While someone types, the other person sees "Name is typing…" above the message box
  (sent live via `/api/typing`, never stored). It clears when they send, empty the box,
  or stop typing for 3 seconds.
- Pages reconnect automatically: always on returning to the app, and whenever the
  20-second heartbeat goes quiet.
- A login lasts 8 hours, then you're returned to the login screen. Switching apps or a
  dropped connection doesn't log you out. Leave or admin removal ends it sooner.
- Others see you as "left" after 2 minutes without a connection, which also frees your
  seat. Only 2 people can be in at once; a third sees "Full".
- Login attempts are throttled (10/minute per IP); duplicate names are rejected.
- For use over the internet, put it behind HTTPS (Render does this automatically).
