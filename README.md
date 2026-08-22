# MediaVault

An Android app that gathers **games, movies, TV, music and documents** into one library,
alongside live content and your subscriptions feed — built from the MediaVault v3 design
handoff (Kotlin + Jetpack Compose).

## Install it on your phone

1. Open the [**Releases**](../../releases) page of this repo **on your Android phone**.
2. Download the newest `MediaVault-1.0.x.apk`.
3. Tap the downloaded file. Android will ask you to allow installs from your browser or
   file manager — allow it, then tap **Install**.
4. Open **MediaVault** from your app drawer.

Every push to this repo builds a new APK through GitHub Actions and publishes it as a
release, so the Releases page always has an installable build.

> The APK is signed with the project keystore in `keystore/` so that every build installs
> as an upgrade over the previous one. That key is public — anyone with this repo can sign
> a build with it — which is fine for a personal side-loaded app, and not fine for a Play
> Store listing. To use your own key, set the repository secrets `MV_KEYSTORE_PASSWORD`,
> `MV_KEY_ALIAS`, `MV_KEY_PASSWORD` and point `MV_KEYSTORE_FILE` at your own keystore.

## What the app does today

| Screen | Behaviour |
| --- | --- |
| **Home** | Continue-watching hero with real progress, subscription/live/movie/game rows |
| **Library** | Games · Movies · TV · Music · Files, chip-switched; poster grids 3-across folded, 5-across unfolded |
| **Media detail** | Backdrop hero, synopsis, play/resume pill, cast circles, season chips + episode list, collection row, two-column on the unfolded display |
| **Player** | Full-screen player — local files play through ExoPlayer with a live scrub bar, plus an "Open in Plex/VLC ↗" escape hatch |
| **Live** | Twitch grid and YouTube TV "on now" rows that deep-link into their apps |
| **Sources** | Add watched folders (Android storage picker), users, connected-service toggles, accent picker |

Things that really work on the device:

- **Watched folders** — pick any folder; MediaVault reads it with the Storage Access
  Framework and parses filenames (`Show.S02E07.mkv` → episode, `Title (2019).mkv` → movie,
  audio → tracks, everything else → file rows). Permission is persisted across launches.
- **Playback** — video files play in the built-in ExoPlayer screen; audio files play
  through the mini now-playing bar.
- **Files** — tapping a document hands it to whichever app owns that type.
- **Games** — installed games on the phone are listed alongside the catalogue and launch
  on tap.
- **Deep links** — Twitch, YouTube, YouTube TV, Netflix, Hulu, Apple TV+ open in their apps.
- **Persistence** — folders, scan results, users, service toggles, accent and watch
  progress are stored locally (SharedPreferences). Nothing leaves the phone.

The catalogue from the design (Severance, Dune, the Twitch and YouTube TV rows) ships as
seed content so every screen is populated before anything is connected. Scanned media and
installed games merge in on top of it.

### Not built yet

Server integrations from the handoff need credentials and are stubbed at the UI layer:
Plex API auth, TMDB/TVDB metadata matching ("Fix match" is a placeholder), Twitch Helix
and YouTube Data API feeds, and playtime via `UsageStatsManager`. The **Connect** buttons
record the connection state locally and the corresponding rows deep-link out rather than
pulling live data.

## Build it yourself

```bash
./gradlew assembleRelease      # → app/build/outputs/apk/release/
./gradlew installDebug         # straight onto a connected phone
```

Requires JDK 17 and the Android SDK (compileSdk 35, minSdk 26).

## Layout

```
app/src/main/java/com/mediavault/app/
├── MainActivity.kt
├── data/        # models, seed catalogue, folder scanner, installed games, persistence
├── media/       # audio playback for the mini player
├── ui/          # design tokens, shared components, one file per screen, AppState
└── util/        # deep links and file-open intents
.github/workflows/android.yml   # builds + publishes the APK on every push
```
