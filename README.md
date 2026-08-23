# MediaVault

An Android app that gathers **your** videos, series, music, documents and games into one
library — built from the MediaVault v3 design handoff (Kotlin + Jetpack Compose).

Everything on screen comes off the phone. There is no demo catalogue.

## Install it on your phone

1. Open the [**Releases**](../../releases) page of this repo **on your Android phone**.
2. Download the newest `MediaVault-1.0.x.apk`.
3. Tap the downloaded file. Android will ask you to allow installs from your browser or
   file manager — allow it, then tap **Install**.
4. Open **MediaVault** and allow media access when it asks. The library fills itself in.

Every push builds a new APK through GitHub Actions and publishes it as a release, so the
Releases page always has an installable build.

> The APK is signed with the project keystore in `keystore/` so each build installs as an
> upgrade over the last. That key is public — fine for a personal side-loaded app, not for
> a Play Store listing. To use your own, set the repository secrets `MV_KEYSTORE_PASSWORD`,
> `MV_KEY_ALIAS`, `MV_KEY_PASSWORD` and point `MV_KEYSTORE_FILE` at your keystore.

## Where the library comes from

| Source | What it gives you |
| --- | --- |
| **MediaStore video** | Every video on the phone. Filenames with `SxxExx` group into a series with a real episode list; the rest are films, with the year parsed out of `Title (2019)`. |
| **MediaStore audio** | Every music track, with artist, album, duration and album art. |
| **Shared storage** | PDFs, spreadsheets, documents, archives — needs the optional all-files access toggle on Android 11+, because Android only shares media through the media permission. |
| **Installed apps** | Apps the system reports as games, with their real icons; tapping launches them. |
| **Watched folders** | Anything the system does not index — an SD card, a USB drive — added by hand under Sources. |

Artwork is real: video frames and album art through `loadThumbnail` (with a
`MediaMetadataRetriever` fallback on older releases) and app icons for games, cached in
memory. The gradient-and-initials cover from the design is only the fallback for files the
system has no thumbnail for.

Permissions are asked for on first launch and are also rows under **Sources**, showing
granted/not-granted state, so nothing is hidden behind a settings hunt. Returning from the
system permission screen triggers a rescan.

## The screens

| Screen | Behaviour |
| --- | --- |
| **Home** | Continue-watching hero (whatever you left part-way, else the newest video), recently added, videos, series, music, games, recent files |
| **Library** | Games · Movies · TV · Music · Files with live counts on each chip; 3-across folded, 5-across unfolded |
| **Detail** | Backdrop, play/resume, "open in another player", real file facts (folder, length, size, type, modified) and the actual episode list per season |
| **Player** | Full-screen ExoPlayer with a live scrub bar, and a hand-off to any other player on the phone |
| **Live** | Launches the streaming apps it detects — MediaVault indexes the phone, those apps own their catalogues |
| **Sources** | Access toggles, library counts, watched folders, users, accent |

Watch progress, watched folders, users and accent are stored locally
(SharedPreferences). Nothing leaves the phone — there is no network call in the app beyond
opening a link you tapped.

### Not built yet

The handoff's server integrations need credentials and are not wired up: Plex API auth,
TMDB/TVDB metadata matching (so titles come from filenames, not a metadata service),
Twitch Helix and YouTube Data feeds, and playtime via `UsageStatsManager`. The Live tab is
deliberately a launcher rather than a fabricated guide.

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
├── data/        # MediaStore scanner, folder scanner, thumbnails, installed apps, permissions, persistence
├── media/       # audio playback for the mini player
├── ui/          # design tokens, shared components, one file per screen, AppState
└── util/        # file opening and app deep links
.github/workflows/android.yml   # builds + publishes the APK on every push
design/                          # the original v3 design handoff, for reference
```
