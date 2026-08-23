package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.Category
import com.mediavault.app.data.DeviceMedia
import com.mediavault.app.data.MediaItem
import com.mediavault.app.util.Launch

@Composable
fun HomeScreen(
    state: AppState,
    unfolded: Boolean,
    contentPadding: PaddingValues,
    onRequestAccess: () -> Unit,
) {
    val context = LocalContext.current
    val wide = if (unfolded) 190.dp else 160.dp
    val poster = if (unfolded) 124.dp else 104.dp
    val hero = state.continueWatching

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (!state.hasMediaAccess) {
            item {
                EmptyCard(
                    title = "Let MediaVault see your media",
                    message = "MediaVault builds its library from the videos, music and files already " +
                        "on this phone. Nothing is uploaded — the scan happens on the device.",
                    modifier = Modifier.padding(horizontal = Mv.Gutter),
                    primaryLabel = "Allow access",
                    onPrimary = onRequestAccess,
                )
            }
        } else if (state.hasNoSources && state.scannedOnce) {
            item {
                EmptyCard(
                    title = if (state.scanning) "Scanning…" else "Choose your folders",
                    message = "MediaVault lists only what you point it at. Open Sources and assign " +
                        "the folders your movies, series and music live in — everything else on " +
                        "the phone stays out of the library.",
                    modifier = Modifier.padding(horizontal = Mv.Gutter),
                    primaryLabel = "Open Sources",
                    onPrimary = { state.tab = Tab.SOURCES },
                )
            }
        }

        if (hero != null) {
            item {
                ContinueHero(
                    item = hero,
                    percent = state.progressOf(hero),
                    modifier = Modifier.padding(horizontal = Mv.Gutter),
                    onOpen = { state.openDetail(hero) },
                    onPlay = { state.play(hero) },
                )
            }
        }

        val recent = (state.movies + state.shows).sortedByDescending { it.addedAt }.take(12)
        if (recent.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(
                        title = "Recently added",
                        modifier = Modifier.padding(horizontal = Mv.Gutter),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Mv.Gutter),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recent, key = { it.id }) { item ->
                            WideCard(
                                title = item.title,
                                subtitle = item.sub,
                                letter = item.letter,
                                gradient = item.gradient,
                                width = wide,
                                artUri = item.artUri,
                            ) { state.openDetail(item) }
                        }
                    }
                }
            }
        }

        if (state.movies.isNotEmpty()) {
            item {
                PosterRow(
                    title = "Videos",
                    items = state.movies,
                    width = poster,
                    aspect = 2f / 3f,
                    state = state,
                    onSeeAll = {
                        state.category = Category.MOVIES
                        state.tab = Tab.LIBRARY
                    },
                )
            }
        }

        if (state.shows.isNotEmpty()) {
            item {
                PosterRow(
                    title = "Series on this phone",
                    items = state.shows,
                    width = poster,
                    aspect = 2f / 3f,
                    state = state,
                    onSeeAll = {
                        state.category = Category.TV
                        state.tab = Tab.LIBRARY
                    },
                )
            }
        }

        if (state.tracks.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                    SectionHeader(
                        title = "Music",
                        action = "See all",
                        onAction = {
                            state.category = Category.MUSIC
                            state.tab = Tab.LIBRARY
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    state.tracks.take(4).forEach { track ->
                        TrackRow(track = track, onClick = { state.playTrack(track) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        if (state.games.isNotEmpty()) {
            item {
                PosterRow(
                    title = if (state.roms.isNotEmpty()) "Games & ROMs" else "Games on this phone",
                    items = state.games,
                    width = poster,
                    aspect = 3f / 4f,
                    state = state,
                    onSeeAll = {
                        state.category = Category.GAMES
                        state.tab = Tab.LIBRARY
                    },
                )
            }
        }

        if (state.documents.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                    SectionHeader(
                        title = "Recent files",
                        action = "See all",
                        onAction = {
                            state.category = Category.FILES
                            state.tab = Tab.LIBRARY
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    state.documents.take(3).forEach { file ->
                        ListRow(
                            title = file.name,
                            subtitle = file.sub,
                            right = "↗ ${file.app}",
                            onClick = {
                                if (!Launch.openDocument(context, file.uri, file.mime)) {
                                    state.showToast("No app on this phone can open ${file.name}")
                                }
                            },
                        ) {
                            IconSquare(
                                letter = file.badge,
                                background = Color(file.background or 0xFF000000L),
                                foreground = Color(file.foreground or 0xFF000000L),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrackRow(
    track: com.mediavault.app.data.Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = track.title,
        subtitle = listOf(track.artist, track.album, track.folder)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { "Audio file" },
        right = DeviceMedia.formatDuration(track.durationMs).ifBlank { "▶" },
        onClick = onClick,
        modifier = modifier,
    ) {
        ArtworkBox(
            letter = "♪",
            gradient = track.gradient,
            artUri = track.artUri,
            letterSize = 15,
            thumbWidth = 128,
            thumbHeight = 128,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
        )
    }
}

@Composable
private fun PosterRow(
    title: String,
    items: List<MediaItem>,
    width: Dp,
    aspect: Float,
    state: AppState,
    onSeeAll: () -> Unit,
) {
    val context = LocalContext.current
    Column {
        SectionHeader(
            title = title,
            action = "See all",
            onAction = onSeeAll,
            modifier = Modifier.padding(horizontal = Mv.Gutter),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Mv.Gutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items.take(15), key = { it.id }) { item ->
                PosterCard(
                    title = item.title,
                    subtitle = item.sub,
                    letter = item.letter,
                    gradient = item.gradient,
                    aspect = aspect,
                    tag = item.tag,
                    progress = state.progressOf(item),
                    artUri = item.artUri,
                    onLongClick = if (item.systemId != null) {
                        { state.askForArt(item) }
                    } else null,
                    modifier = Modifier.width(width),
                ) {
                    if (item.category == Category.GAMES) launchGame(state, context, item)
                    else state.openDetail(item)
                }
            }
        }
    }
}

/** A ROM goes to its emulator; an installed game app just launches. */
internal fun launchGame(state: AppState, context: android.content.Context, item: MediaItem) {
    if (item.systemId != null) {
        state.launchRom(context, item)
        return
    }
    val packageName = item.packageName
    when {
        packageName == null -> state.showToast("No launcher for ${item.title}")
        Launch.launchApp(context, packageName) -> state.showToast("▶ Launching ${item.title}…")
        else -> state.showToast("Couldn't launch ${item.title}")
    }
}

@Composable
private fun ContinueHero(
    item: MediaItem,
    percent: Int,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 8.2f)
            .clip(RoundedCornerShape(Mv.HeroRadius))
            .clickable { onOpen() },
    ) {
        ArtworkBox(
            letter = item.letter,
            gradient = item.gradient,
            artUri = item.artUri,
            thumbWidth = 720,
            thumbHeight = 400,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 8.2f),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color(0x1A000000), Color(0xCC000000)))),
        )
        Text(
            text = if (percent in 1..99) "CONTINUE" else "ON THIS PHONE",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 9.dp, vertical = 4.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 60.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = heroMeta(item, percent),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 60.dp),
            )
            if (percent in 1..99) {
                Spacer(Modifier.height(10.dp))
                ProgressBar(percent = percent)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onPlay() },
            contentAlignment = Alignment.Center,
        ) {
            PlayGlyph(size = 16.dp, color = Mv.Text)
        }
    }
}

private fun heroMeta(item: MediaItem, percent: Int): String {
    val duration = DeviceMedia.formatDuration(item.durationMs)
    val pieces = buildList {
        if (percent in 1..99) add("$percent% watched")
        if (item.episodes.isNotEmpty()) add("${item.episodes.size} episodes")
        if (duration.isNotBlank()) add(duration)
        if (item.folder.isNotBlank()) add(item.folder)
        DeviceMedia.formatSize(item.sizeBytes)?.let { add(it) }
    }
    return pieces.joinToString(" · ").ifBlank { "On this phone" }
}
