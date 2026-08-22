# Handoff: MediaVault — Unified Media Hub for Android

## Overview
MediaVault is a single-user (optionally multi-user) Android app that gathers **games, movies, TV shows, music, and documents** into one library, alongside **live content** (Twitch follows, YouTube TV guide) and a **YouTube subscriptions feed**. Local media gets full Plex-style metadata (artwork, seasons, episode lists, cast, collections) via TMDB/TVDB matching. Target device: **Samsung Galaxy Z Fold** — every screen ships in two layouts: folded 5.5″ cover display and unfolded 7.6″ main display.

## About the Design Files
These are **design references built in HTML** — interactive prototypes of intended look and behavior, NOT production code. Recreate them as a native Android app (recommended: **Kotlin + Jetpack Compose + Media3/ExoPlayer**, using `WindowSizeClass` for the fold/unfold layouts). Open the HTML files in a browser to explore every screen and interaction.

- `MediaVault v3.dc.html` — **the design to implement**
- `GameVault v2.dc.html` — the earlier games-only app; its game-detail page (playtime, save states, achievements, controller mapping) is the intended design for game detail inside MediaVault
- `android-frame.jsx` — prototype device bezel, ignore for implementation
- `screenshots/` — one capture per screen (folded + unfolded side by side)

## Fidelity
**High-fidelity** for layout, color, type, spacing, radii, and interaction. Cover art / thumbnails are placeholder gradients with initials — production uses real artwork (TMDB/TVDB for video, IGDB/SteamGridDB for games, service thumbnails for Twitch/YouTube) with gradient+initial as the loading/fallback state.

## Navigation model
**No top header.** Bottom tab bar only: **Home ⌂ · Library ▦ · Live ◉ · Sources ⚙** (frosted `rgba(245,245,247,.88)` + blur, active = accent `#0a7cff`, inactive `#86868b`). Library has a horizontal chip row selecting **Games / Movies / TV / Music / Files** (active chip: `#1d1d1f` bg, white text; inactive: white bg, `#5b5b60`). A profile avatar (34px accent-gradient circle, top-right) appears **only when 2+ users exist** — single-user mode has no profile UI at all.

## Screens

### 1. Home
- **Continue-watching hero**: 16:8.2 backdrop card, 20px radius, dark gradient overlay, "CONTINUE" pill badge, title 19/700 white, meta line, accent progress bar, white ▶ circle. Tap → media detail.
- Horizontal rows (each: 11/700 uppercase gray header + accent "See all" link): **New from your subscriptions** (YouTube, 16:9 cards, opens YouTube), **Live now** (Twitch, pulsing red LIVE badge, viewer counts), **Movies** (2:3 posters), **Jump back in · Games** (3:4 covers). Card widths: 160/104px folded, 190/124px unfolded.

### 2. Library
- Chip row + content per category:
  - **Games / Movies / TV**: poster grid (3-across folded, 5 unfolded; 2:3 for video, 3:4 games), source tag pill bottom-left of each cover, title 11.5/600 + sub 10/gray under.
  - **Music**: white rows (♪ accent-tinted square, track/artist·source, duration) → tapping starts the **mini now-playing bar**: dark pill (`rgba(29,29,31,.95)` blur, 16px radius) docked above the tab bar with art square, title, play/pause, close.
  - **Files**: white rows with app-colored doc squares (Excel green `#1d6f42`/`#e7f4ec`, Word blue `#2b579a`/`#e8effc`, PDF red `#c9302c`/`#fdeaea`), name, folder·size·modified, right label "↗ Excel" etc. Tap = open in the file's native app (ACTION_VIEW intent) — toast confirms in prototype.

### 3. Media detail (movies & shows — Plex-style)
- 180px backdrop hero fading to page bg; back circle.
- Title 24/700; source pill (PLEX/LOCAL/NETFLIX…) + meta line "2025 · Thriller · TV-MA · 2 seasons".
- Synopsis 13px/1.55 `#5b5b60`.
- **Play pill** (full-width accent): "▶ Play", "▶ Resume · 1:12 of 2:46", or "Open in Hulu ↗" for external services.
- **Cast row**: 52px initial circles + names (from TMDB).
- "Matched from TMDB · **Fix match**" footer link (manual re-match flow).
- **Shows**: season chips (dark active) + episode rows — number square, title, "49 min · S2 E7", green ✓ when watched, thin accent progress bar when partial. Tap episode → player.
- **Movies**: "In collection" row of related 2:3 posters (TMDB collection data).
- Unfolded: two columns — info/cast left, episodes/collection right.

### 4. In-app player
- Full-black screen: backdrop area with ▶ circle, × close, "MediaVault Player" badge; bottom sheet `#0d0d0f` with title, "Playing from Plex/Local file", monospace timecodes + accent scrub bar, **"Open in Plex ↗ / Open in VLC ↗"** escape-hatch button, CC button.
- Routing rule: Plex + local files play in-app (ExoPlayer; VLC fallback for exotic codecs). External services (Netflix, Hulu, Apple TV+, Twitch, YouTube, YouTube TV) deep-link out — prototype shows a toast "↗ Opening … in <app>".

### 5. Live
- **Twitch grid** (2-across folded, 3 unfolded): 16:9 cards, pulsing LIVE badge, viewer-count pill, channel + game. Tap → Twitch deep link.
- **YouTube TV · On now**: white rows — channel logo square, program title, channel + time slot, red on-now progress bar. Tap → YouTube TV deep link.

### 6. Sources
- **Watched folders**: white rows (📁 accent square, monospace path, "38 items · movies", × remove) + dashed "+ Add a folder to watch" (SAF folder picker in production).
- **Users**: card listing users (avatar circle, name, Owner/Member, × remove for non-owners) + "+ Add a user". Helper text: with one user profiles are hidden; adding a second enables the header avatar/profile switcher.
- **Connected services**: Plex, Twitch, YouTube, YouTube TV, Netflix, Moonlight — brand-gradient logo square, description, accent **Connect** button ↔ gray "✓ Connected".
- Unfolded: two columns (folders/users left, services right).

## Interactions & Behavior
- Screen transitions fade 250ms; list/grid items pop in (8px rise, 300ms). Toast: dark pill bottom-center, auto-dismiss 2.4–2.6s.
- Tap flows: poster → detail → play (in-app or deep link); file → native app; music → mini player; game → launch (see GameVault v2 for game detail design).
- Source toggles/folder removal immediately filter the library.

## Data & Integrations
- **Local scan**: SAF-permission folders → filename parsing (`Title (Year)`, `Show.S02E07`) → TMDB/TVDB lookup → cached metadata DB (Room): posters, backdrops, synopsis, cast, episode data, collections; "Fix match" manual search for misses.
- **Plex**: PIN-link auth → HTTP API for libraries, artwork, progress, stream URLs (in-app playback).
- **Twitch**: Helix API + OAuth → followed live channels, viewers, thumbnails; deep link to watch.
- **YouTube**: Data API + Google sign-in → subscriptions feed; embedded official player or deep link.
- **YouTube TV**: no public API — third-party EPG for the guide, deep link to tune.
- **Games**: PackageManager (installed), emulator ROM folders, Moonlight; playtime via UsageStatsManager. See `GameVault v2.dc.html` + its README concepts for the full game-detail spec.
- Watch progress, users, connected services, watched folders: Room/DataStore, all local.

## Design Tokens
- **Colors**: page `#f5f5f7`; card `#ffffff`; text `#1d1d1f`; secondary `#86868b`; hairline `rgba(0,0,0,.07)`; accent `#0a7cff` (themeable: `#7ab8ff`, `#ff6b3d`, `#a06bff`); live red `#e0245e`; watched green `#34c759`; player bg `#0d0d0f`; toast `#1d1d1f`.
- **Type**: system sans (Roboto/SF). Section headers 11/700 uppercase +1.4 tracking; card titles 11.5–13.5/600; detail title 24/700 −0.5; synopsis 13/1.55.
- **Spacing**: 22px gutters; 8px row gaps; 12–16px grid gaps; card padding 10–16px.
- **Radii**: cards/rows 14px, hero 20px, covers 12px, pills 99px, icon squares 9–10px.
- **Shadows**: row `0 1px 4px rgba(0,0,0,.05)`; cover `0 4px 12px rgba(0,0,0,.10)`; hero `0 10px 28px rgba(0,0,0,.14)`; accent button `0 6px 18px accent@30%`.

## Assets
- No bundled imagery; all art fetched at runtime (TMDB/TVDB/IGDB/service APIs). Unicode glyphs (⌂ ▦ ◉ ⚙ ♪ ▶) are stand-ins — use Material Symbols.

## Files
- `MediaVault v3.dc.html` — primary design, all screens, both fold states, fully interactive
- `GameVault v2.dc.html` — game detail & launcher spec (companion)
- `screenshots/01-home.png` … `08-sources.png`
