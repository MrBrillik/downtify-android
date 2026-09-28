# Roadmap

Phase 1 (done): connect & pair, library sync, Home / Library / Album / Artist / Search (local), Settings, streaming with Media3, lyrics, likes, play reports.

Phase 2 (done): offline downloads — see below for what shipped and what's left. Also done alongside it: the server 3.2 contract additions ("Signed in as …" from the pair response and `GET /api/me`, and now-playing reports to `POST /api/activity/playback`).

Phase 3 (done): server search, download requests, previews and the server queue.

Everything from phase 4 on is planned, not built. Each phase uses only what the server contract (`~/git/downtify/docs/mobile-client-contract.md`) already offers unless noted.

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

## Phase 3 — Server search and requests (Search board) — done

Shipped:

- One search field for both: "In your library" (on the phone, instant) and "Not in your library yet" — the server's YouTube Music search (`GET /api/songs/search`, and `GET /api/albums/search` for albums), after a 600 ms pause in the typing. The filter chips apply to both.
- A pasted Spotify or YouTube (Music) link — track, album, playlist or artist — is resolved with `GET /api/url/resolve` instead: a header with the whole thing and "Download all N songs", then its tracks (or an artist's albums).
- Asking the server to download: a song, an album (resolved, then queued) or everything a link points at, all through `POST /api/download/batch` with the song objects sent back exactly as the server gave them. A playlist link keeps its `playlist_url`, so the server files it as that playlist.
- Each result then shows what the server does with it — waiting, a progress ring, ✓, or failed (tap to try again) — and albums show "Downloading 4/9". The song lands in the library by itself (`library_changed`).
- 30-second previews: Spotify's own `preview_url`, else Deezer's through `GET /api/preview`. A small player of its own (not in the media session, the queue or the play counts) pauses the music and resumes it after. The clips come from Spotify's and Deezer's CDNs; the device token is only ever sent to the paired server.
- Downloads › On the server: the server's queue (`GET /api/queue`, then the WebSocket's progress messages), and on "On this phone" the card "The server is downloading 13 songs · Harbor Nights · 4 of 9".

Left for later:

- The queue is read-only here: clearing or cancelling jobs is an admin action on the web page (devices get 403), so the app doesn't offer it.
- Results already in the library aren't hidden from "Not in your library yet" (matching YouTube Music titles to local tags reliably needs more than the ids the server gives).
- Artist links show the artist's albums; their top songs (`/api/artists/top_songs/url`) aren't shown yet.

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
