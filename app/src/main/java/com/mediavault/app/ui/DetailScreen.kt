package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.Category
import com.mediavault.app.data.DemoData
import com.mediavault.app.data.Episode
import com.mediavault.app.data.MediaItem
import com.mediavault.app.util.Launch

@Composable
fun DetailScreen(
    state: AppState,
    item: MediaItem,
    unfolded: Boolean,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val meta = DemoData.metaFor(item)
    val isShow = item.category == Category.TV
    val percent = state.progressOf(item)
    val seasonCount = meta.seasons.coerceAtLeast(1)
    val episodes = episodesFor(meta, state.season, seasonCount)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(gradientBrush(item.gradient)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(listOf(Color(0x00000000), Mv.Page))
                    ),
            )
            CircleGlyphButton(
                glyph = "‹",
                fontSize = 20,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = Mv.Gutter, top = contentPadding.calculateTopPadding() + 8.dp),
            ) { state.closeDetail() }
        }

        val body: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier = modifier.padding(horizontal = Mv.Gutter)) {
                Text(
                    text = item.title,
                    color = Mv.Text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.tag?.let {
                        TagPill(it)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = buildString {
                            append(meta.year.takeIf { it > 0 }?.toString() ?: "—")
                            append(" · ${meta.genre} · ${meta.rating}")
                            if (isShow) append(" · $seasonCount season${if (seasonCount > 1) "s" else ""}")
                        },
                        color = Mv.Secondary,
                        fontSize = 11.5.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = meta.synopsis,
                    color = Mv.Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(16.dp))
                AccentButton(
                    text = playLabel(item, percent),
                    modifier = Modifier.fillMaxWidth(),
                ) { play(state, context, item, percent) }

                if (meta.cast.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    SectionHeader(title = "Cast")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(meta.cast) { name ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(66.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(gradientBrush(com.mediavault.app.data.LocalScanner.gradientFor(name))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = name.split(' ').mapNotNull { it.firstOrNull() }.take(2)
                                            .joinToString("").uppercase(),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    color = Mv.Secondary,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row {
                    Text(text = "Matched from TMDB · ", color = Mv.Secondary, fontSize = 11.sp)
                    Text(
                        text = "Fix match",
                        color = LocalAccent.current,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            state.showToast("🔎 Manual match for “${item.title}” — connect a metadata source")
                        },
                    )
                }
            }
        }

        val side: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier = modifier.padding(horizontal = Mv.Gutter)) {
                if (isShow) {
                    if (seasonCount > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items((0 until seasonCount).toList()) { index ->
                                Chip(
                                    text = "Season ${index + 1}",
                                    selected = state.season == index,
                                ) { state.season = index }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    episodes.forEach { episode ->
                        EpisodeRow(episode) {
                            state.openPlayer(
                                PlaybackTarget(
                                    title = "${item.title} · S${state.season + 1} E${episode.number}",
                                    sourceLabel = sourceLabel(item),
                                    uri = item.uri,
                                    length = episode.length,
                                    startPercent = episode.progress,
                                    itemId = item.id,
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                } else if (meta.collection.isNotEmpty()) {
                    SectionHeader(title = "In collection")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(meta.collection) { title ->
                            PosterCard(
                                title = title,
                                subtitle = "TMDB collection",
                                letter = title.take(1),
                                gradient = com.mediavault.app.data.LocalScanner.gradientFor(title),
                                aspect = 2f / 3f,
                                modifier = Modifier.width(104.dp),
                            ) { state.showToast("Not in your library yet") }
                        }
                    }
                }
            }
        }

        if (unfolded) {
            Row(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                body(Modifier.weight(1f))
                side(Modifier.weight(1f))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
            ) {
                item { body(Modifier) }
                item { Spacer(Modifier.height(22.dp)) }
                item { side(Modifier) }
            }
        }
    }
}

private fun episodesFor(meta: com.mediavault.app.data.MediaMeta, season: Int, seasonCount: Int): List<Episode> {
    val isLastSeason = season == seasonCount - 1
    if (isLastSeason && meta.episodes.isNotEmpty()) return meta.episodes
    return (1..8).map { Episode(it, "Episode $it", "45 min", if (isLastSeason) 0 else 100) }
}

private fun playLabel(item: MediaItem, percent: Int): String = when {
    !item.playsInApp -> "Open in ${item.source} ↗"
    percent in 1..99 -> "▶ Resume · ${item.at} of ${item.length}"
    else -> "▶ Play"
}

private fun sourceLabel(item: MediaItem): String = when {
    item.uri != null -> "Playing from a watched folder"
    item.source.equals("plex", true) -> "Playing from Plex"
    else -> "Playing from ${item.source}"
}

private fun play(state: AppState, context: android.content.Context, item: MediaItem, percent: Int) {
    if (!item.playsInApp) {
        state.showToast("↗ Opening “${item.title}” in ${item.source}…")
        Launch.search(context, item.source, item.title)
        return
    }
    state.openPlayer(
        PlaybackTarget(
            title = item.title,
            sourceLabel = sourceLabel(item),
            uri = item.uri,
            length = item.length,
            startPercent = percent,
            itemId = item.id,
        )
    )
}

@Composable
private fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    val accent = LocalAccent.current
    CardSurface(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFEFEFF2)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = episode.number.toString(),
                        color = Mv.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        color = Mv.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(text = episode.length, color = Mv.Secondary, fontSize = 11.sp)
                }
                if (episode.progress >= 100) {
                    Text(text = "✓", color = Mv.Watched, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (episode.progress in 1..99) {
                ProgressBar(
                    percent = episode.progress,
                    color = accent,
                    track = Color(0x14000000),
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp),
                )
            }
        }
    }
}
