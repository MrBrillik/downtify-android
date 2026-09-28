# Roadmap

Phase 1 (done): connect & pair, library sync, Home / Library / Album / Artist / Search (local), Settings, streaming with Media3, lyrics, likes, play reports.

Phase 2 (done): offline downloads — see below for what shipped and what's left. Also done alongside it: the server 3.2 contract additions ("Signed in as …" from the pair response and `GET /api/me`, and now-playing reports to `POST /api/activity/playback`).

Everything from phase 3 on is planned, not built. Each phase uses only what the server contract (`~/git/downtify/docs/mobile-client-contract.md`) already offers unless noted.

## Phase 2 — Offline downloads (Downloads board) — done

Shipped:

- Keep albums, playlists and Liked songs offline: the "Download / On this phone" chip on Album, Playlist and Liked songs headers, and "Keep Liked songs offline" on Downloads. Downloaded tracks get a check in every track list.
- One WorkManager job (`OfflineSyncWorker`) downloads the originals (`GET /api/v1/tracks/{id}/stream`) one by one into `files/offline/`, resuming an interrupted file with a **Range** request. Constraints: Wi-Fi only (default on, Settings › Downloads) and storage not low.
- Kept collections follow the library: a song added to a kept album on the server downloads, a removed one is deleted; liking a song with Liked songs kept downloads it.
- A storage limit (default 8 GB, Settings › Downloads). Collections fill it oldest first; what doesn't fit is shown as "doesn't fit the storage limit", and lowering the limit deletes the newest collections' songs first. Nothing is evicted silently.
- Playback reads the offline copy first ("Playing from this phone" in Now Playing); the library works fully offline.
- Changing to another server deletes the previous server's offline copies.

Left for later:

- Offline copies are always the original file. A "download quality" setting (a transcoded copy for big FLAC libraries) would use the same URL with `format`/`bitrate`.
- Covers of offline albums rely on Coil's disk cache (256 MB); they aren't pinned with the audio, so a long-unseen album can show a placeholder while offline.
- Downloads run as ordinary WorkManager work, not a foreground service: on a big collection Android may pause them when the app is in the background for long, and they resume on the next run. A progress notification (foreground `dataSync`, or a user-initiated data transfer job) would make them steadier.
- The "On the server" half of the Downloads board (the server's download queue) belongs to phase 3.
- `:feature:downloads` wasn't split into its own module: the data layer lives in `:core:data` (`offline/`), which the player needs too, so the screen alone didn't earn a module.

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
