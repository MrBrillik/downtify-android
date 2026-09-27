# Downtify Android — conventions

Native Android client for the Downtify self-hosted music server. Kotlin, Jetpack Compose + Material 3, Hilt, Media3, Room, DataStore. See README.md for build and setup.

## The server contract

- The contract lives in the server repo: `~/git/downtify/docs/mobile-client-contract.md`, with details in `docs/api-reference.md` ("Server and sign-in", "Mobile API (v1)", "WebSocket") and user-facing behaviour in `docs/features/mobile-apps.md`.
- **Don't invent endpoints.** If the app needs something the server doesn't offer, write it in `docs/server-requirements.md` and work around it (or leave the feature out).
- When the docs and the server code disagree, the code wins; note the mismatch in `docs/server-requirements.md`.
- **Never modify the server repo from here.** Read it only. To test against it, run it with `DOWNLOAD_DIR`/`DATABASE_DIR` pointed at a scratch directory and a spare `--port`; port 8000 may be the user's own instance.
- Every request carries `Authorization: Bearer <token>` (`AuthInterceptor`), even when the server doesn't require sign-in: a 401 is how the app learns it was unpaired. A 401 "revoked", a WebSocket close 4401, or a refused WebSocket handshake confirmed by `GET /api/auth/status` all end the session (`SessionStore.onRevoked`).

## Modules and dependency rules

```
:app ──► :core:designsystem
     ──► :core:player ──► :core:data ──► :core:network ──► :core:model
```

- `:core:model` is pure Kotlin (JVM): no Android imports. Put logic here whenever it can live without Android — it's the cheapest place to test. It runs on Android too, so no JVM APIs newer than API 26 (e.g. `URLEncoder.encode(String, Charset)` is API 33 — use the `"UTF-8"` overload). Android lint does not check this module; be careful.
- `:core:network` knows the wire format (DTOs, Retrofit interface, WebSocket, NSD). DTOs never leave it: map to `:core:model` types.
- `:core:data` owns persistence and the sync/report logic. Repositories expose `Flow`/`StateFlow`; the UI never touches DAOs or the API directly.
- `:core:player` owns Media3. The UI talks to playback only through `PlayerController`.
- Screens live in `:app` under `feature/<name>/` (Screen + ViewModel). There are no feature modules yet: at this size they'd add build config without buying isolation. Split a feature out when it gains its own data layer (e.g. offline downloads).
- Gradle config goes in the convention plugins in `build-logic/`; versions only in `gradle/libs.versions.toml`. Check current stable versions before bumping — don't guess.

## Architecture

- Single activity, Navigation Compose with type-safe `@Serializable` routes (`ui/navigation/Routes.kt`).
- MVVM + unidirectional data flow: a ViewModel exposes one `StateFlow<XUiState>`; the screen calls ViewModel functions for events.
- Split every screen into a `XRoute` (gets the ViewModel, collects state) and a stateless `XScreen(state, callbacks…, modifier)` that previews and tests can call directly.
- **Text fields:** keep the text in synchronous snapshot state (`var x by mutableStateOf("")` in the ViewModel, or a plain `MutableStateFlow`), never in a flow that went through `combine`/`stateIn` — the async hop drops keystrokes. Don't rewrite the typed text (e.g. uppercase) in `onValueChange`; use a `VisualTransformation`.
- Coroutines: `viewModelScope` in ViewModels, the `@ApplicationScope` scope for app-lifetime work. Rethrow `CancellationException` from every broad `catch`.
- Android 17+ (target 37): talking to anything on the LAN needs the runtime `ACCESS_LOCAL_NETWORK` permission. The connect screen asks for it before starting NSD discovery.

## Theming and UI

- Use the real Material 3 components; no look-alikes. Colors come from `MaterialTheme.colorScheme` — never hard-code hex values in screens. Brand tokens live in `core/designsystem/theme/Color.kt` (dark is the default look; light matches `Tokens.dc.html`). Dynamic color is opt-in (Settings).
- Type: Sora (display/headline/titleLarge), DM Sans (everything else), as Google downloadable fonts — `theme/Type.kt`. Shapes in `theme/Shape.kt`, spacing on a 4 dp grid via `Spacing`.
- Now Playing, the album header and the mini player take their surface from the cover (`rememberCoverColors`); the play button stays `primary`.
- Icons: Material Symbols Rounded as vector drawables in `:core:designsystem` (`DowntifyIcons`), outlined at rest and filled when selected.
- Edge-to-edge: pad with `WindowInsets`, don't fight the system bars. 48 dp minimum touch targets. Every screen works in `NavigationBar` (compact) and `NavigationRail` (medium+).
- Every user-visible string goes in `app/src/main/res/values/strings.xml`, ready for translation.
- Each screen has `@PreviewLightDark` previews built from `ui/common/PreviewData`.

## Testing and quality

- `./gradlew build` must pass (compile, unit tests, Android lint). `./gradlew detekt spotlessCheck` must be clean; `spotlessApply` formats.
- Unit tests: JUnit 4 + Turbine + fakes (MockK only where a fake is awkward). Pure logic gets tests in `:core:model`; sync and the listen queue are tested in `:core:data` against in-memory fakes.
- Compose UI tests run on the JVM with Robolectric (`app/src/test`, SDK 34 — newer sandboxes need a Java 21 test JVM). Test the stateless `XScreen`s.
- When a change affects behaviour against the server, verify it for real (emulator + a scratch server instance) and say exactly what you ran and what you didn't.

## Commits

- Don't commit or push; the maintainer commits by hand. When a piece of work is finished, write a suggested message to `commit-message.md` at the repo root (it's git-ignored).
- Conventional Commits (`feat:`, `fix:`, `build:`, `docs:`, `test:`, `refactor:`), imperative subject ≤ 72 characters, body explaining why when it isn't obvious.
