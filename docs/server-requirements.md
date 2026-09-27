# Server requirements and contract notes

Things the app needs from the server that it doesn't offer yet, and places where the server's docs and code disagree. The server is the source of truth; nothing here is implemented server-side from this repo.

## Doc/code mismatches

### Revoked token on the WebSocket handshake is HTTP 403, not close code 4401

- **Docs** (`mobile-client-contract.md` §3 and "WebSocket"; `api-reference.md` "WebSocket"): an unpaired device's socket "closes with code `4401`".
- **Code** (`downtify/auth.py`, the WebSocket branch of the auth middleware): when the token is already revoked at connect time, the middleware sends `websocket.close` with 4401 *before* accepting, which the ASGI server turns into an **HTTP 403** on the handshake. 4401 only reaches clients that were connected when the device was unpaired (`api.py`, `close_device`).
- **What the app does:** treats a refused handshake as "maybe revoked" and confirms with `GET /api/auth/status` before signing out (a proxy that blocks WebSockets also gives 403, so it can't sign out on 403 alone). Verified against a running server: unpairing the device while the app was connected closed its socket, the app checked `/api/auth/status`, dropped the token, stopped playback and returned to the connect screen.
- **Suggested fix:** document the 403 case, or accept the socket and then close with 4401 so clients see one signal.

## Nice to have

### Recently played across devices

"Jump back in" on Home is tracked locally (Room `recent_contexts`), per the phase-1 brief. A server endpoint listing a device's (or the user's) recent play contexts — album, playlist, artist, liked songs — would let it follow the user between the web page and the phone. The listen reports (`POST /api/discover/listens`) carry track ids only, not the context they were played from.
