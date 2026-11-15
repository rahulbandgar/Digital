# Ad-hoc Chat

A password-protected chat for two people that runs in any browser. Only the **last
10 minutes** of messages exist: anything older is deleted continuously, from the server's
memory and from everyone's screen. Nothing is ever written to disk or a database, and a
server restart wipes everything.

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
| `KEEP_MINUTES`   | `10`           | How long messages are kept before being deleted |
| `IDLE_MINUTES`   | `30`           | Log out someone who has had no connection this long |

## Admin page

Open `/admin` (e.g. `http://localhost:3000/admin`) and sign in with the admin password **`lalgun`**.
It lists everyone (online, away, or not connected) and lets you remove a person or
everyone. Removed people see "You were removed" and return to the login screen.

## How it works

- `server.js` — plain Node `http` server. Passwords are checked server-side; a successful
  login gets an in-memory session token (kept in that browser tab's `sessionStorage`
  so a reloaded tab can resume).
- Live updates use Server-Sent Events (`/api/stream`); messages are sent via `/api/send`.
- Every message is numbered and kept in memory for 10 minutes. When someone joins,
  reloads, or reconnects, they receive whatever they haven't seen from the last 10 minutes.
  This also covers phones that silently drop the connection while in another app.
- Pages reconnect automatically: always on returning to the app, and whenever the
  20-second heartbeat goes quiet.
- There is no fixed session length. You stay logged in until you click Leave, the admin
  removes you, or you've had no connection for 30 minutes.
- Others see you as "left" after 2 minutes without a connection, which also frees your
  seat. Only 2 people can be in at once; a third sees "Full".
- Login attempts are throttled (10/minute per IP); duplicate names are rejected.
- For use over the internet, put it behind HTTPS (Render does this automatically).
