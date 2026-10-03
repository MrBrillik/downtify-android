# Downtify for Android

The official Android client for [Downtify](https://github.com/henriquesebastiao/downtify), the self-hosted music server. It finds your server on the home network (or takes an address you type), pairs with it, syncs the library and streams it — original quality on Wi-Fi, transcoded on mobile data — with the system media notification, lock screen and Bluetooth controls.

Status: **phase 4** (connect & pair, library sync, browsing, streaming, play reports, offline downloads, server search with previews and download requests, Discover, podcasts). Casting, widgets and Android Auto come later — see [docs/roadmap.md](docs/roadmap.md).

> [!IMPORTANT]
> This app is currently in the development and testing phase; it will contain bugs. Integration with Downtify is also still being developed and tested.

## Screenshots

<img width="30%" vspace="20" src="https://github.com/user-attachments/assets/916b7bbb-2cbc-4c6f-98b8-e9d7ce0547f0" />
<img width="30%" vspace="20" src="https://github.com/user-attachments/assets/06935b7b-0b31-42a6-8d41-bc3276463d02" />
<img width="30%" vspace="20" src="https://github.com/user-attachments/assets/d408af5c-c019-470a-9f2e-5af7279ac33d" />
<img width="30%" vspace="20" src="https://github.com/user-attachments/assets/6d4c2580-0a71-4f42-b30e-5a3ddb5d8b93" />
<img width="30%" vspace="20" src="https://github.com/user-attachments/assets/df3ad441-3286-4bd1-b5a4-c950f6d1bd42" />
<img width="30%" vspace="20" src="https://github.com/user-attachments/assets/2538bf27-99be-4687-a13f-fbc4f28ceefa" />

## Requirements

- JDK 17 or newer (the build targets Java 17).
- Android SDK with platform 37 (`compileSdk`/`targetSdk` 37, `minSdk` 26). Gradle installs missing platforms on first build when `sdk.dir` in `local.properties` points at a writable SDK.
- A Downtify server **3.1.0 or newer** (mobile API v1, pairing).

## Build

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # onto a connected device or emulator
./gradlew build                  # everything: compile, unit tests, Android lint
./gradlew detekt spotlessCheck   # static analysis and formatting
./gradlew spotlessApply          # fix formatting
```

The debug build installs as `com.henriquesebastiao.downtify.debug`, so it sits next to a release build. `assembleRelease` is signed with the debug key (there's no release keystore in the repo) — replace the signing config before shipping.

## Pointing it at a server

1. Run Downtify (`make run` or Docker). Note the address you open it at in a browser, e.g. `http://192.168.1.20:8000`.
2. Open the app. Allow **nearby devices / local network** access when asked (Android 17+ needs it to reach anything on your home network).
3. Pick the server under **Found on this network**, or type its address. `192.168.1.20:8000` is enough; `http://` is added for you. HTTPS addresses work too, including behind a reverse proxy with a private CA installed on the phone.
4. On the web page, signed in as the account the phone should belong to, open **Settings → Apps → Pair a phone**. Scan the QR code, or type the 8-character code.

Notes:

- **Docker:** the default bridge network doesn't carry mDNS, so the server won't show up under "Found on this network". Type the address, or run the container with `network_mode: host`.
- **Emulator:** the host machine is `10.0.2.2` (`http://10.0.2.2:8000`).
- **Plain http** is allowed for any address, because self-hosted servers are usually `http://` on the LAN and Android's network security config can't express "private addresses only". Over http the device token travels unencrypted: fine at home, not across the internet. The connect screen warns when you type a plain-http public address. See `app/src/main/res/xml/network_security_config.xml`.
- **Unpairing** from the web (Settings → Apps → the phone → Unpair) stops playback and sends the app back to the connect screen.

## Project layout

| Module | What's in it |
|---|---|
| `:app` | Activity, navigation, all screens (`feature/*`), strings |
| `:core:model` | Pure Kotlin: domain types and logic (address normalising, pairing URI, stream policy, grouping, listen counter, LRC parser, search) |
| `:core:network` | OkHttp/Retrofit client for the contract, NSD discovery, WebSocket, Keystore-encrypted session store |
| `:core:data` | Room (library, playlists, likes, recents, listen queue, offline index), DataStore settings, sync, offline downloads, the listen flusher (WorkManager), server search and the server's download queue |
| `:core:player` | Media3 `MediaSessionService`, stream URL resolving, cache, listen and activity tracking, UI-side `PlayerController`, the preview player |
| `:core:designsystem` | Material 3 theme (brand tokens, dark/light, dynamic color), fonts, icons, logo, shared components |
| `build-logic` | Gradle convention plugins |

Conventions for working on the code are in [CLAUDE.md](CLAUDE.md). The server contract the app implements lives in the server repository: `docs/mobile-client-contract.md`.

## License

GPL-3.0 — see [LICENSE](LICENSE).
