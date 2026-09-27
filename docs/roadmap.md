# Roadmap

Phase 1 (done): connect & pair, library sync, Home / Library / Album / Artist / Search (local), Settings, streaming with Media3, lyrics, likes, play reports. Everything below is planned, not built. Each phase uses only what the server contract (`~/git/downtify/docs/mobile-client-contract.md`) already offers unless noted.

## Phase 2 — Offline downloads (Downloads board)

- Keep albums, playlists and Liked songs offline; per-item toggle on Album/Playlist headers and a "Keep offline" switch on Liked songs.
- A WorkManager job per collection, downloading `GET /api/v1/tracks/{id}/stream` (original, or a chosen quality) with **Range** requests so interrupted files resume. Constraints: Wi-Fi only (default on), not low on storage.
- Storage: an app-private directory, a user-set limit, least-recently-played eviction; the Room offline index (track id → file, size, quality, etag/mtime) is the source of truth.
- Playback prefers the local file (a `DataSource` that checks the offline index before the network); the library works fully offline.
- Library sync removes offline files for tracks deleted on the server.
- Split `:feature:downloads` (with its own data layer) out of `:app` at this point.

## Phase 3 — Server search and requests (Search board)

- A second Search tab that queries the server (`/api/songs/search`, `/api/url/resolve`) and requests downloads (`POST /api/download/url|batch|album`).
- 30-second previews via `/api/preview` in a lightweight second player, ducking the main one.
- The server queue card on Downloads: `GET /api/queue` kept fresh by the WebSocket progress messages ("The server is downloading N songs").

## Phase 4 — Discover and podcasts

- Discover sections from the server's discover endpoints.
- Podcasts: subscriptions, episode lists, streaming and downloading episodes, **resume position** per episode, played state.

## Phase 5 — Cast

- Google Cast sender (Media3 `CastPlayer`) behind the output sheet from the System board.
- The receiver can't send the device token, so each stream URL is signed with `POST /api/v1/sign` (short-lived signed URLs); re-sign when they expire.

## Phase 6 — Widgets, Android Auto, large screens

- Glance widgets (small: now playing + play/pause; large: cover-tinted with queue peek), per the System board.
- Android Auto: a browsable `MediaLibraryService` tree (Recent, Albums, Artists, Playlists, Liked songs) with search.
- Tablet and foldable layouts: list-detail for Library/Album, a two-pane Now Playing with the queue beside it.

## Ongoing

- Translations (the web UI ships 8 languages; strings are already in `strings.xml`).
- Screenshot tests for the main screens, dark and light.
- Baseline profile for startup and scrolling.
