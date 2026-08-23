package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.mediavault.app.data.DeviceMedia
import com.mediavault.app.data.EpisodeFile
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
    val percent = state.progressOf(item)
    val isShow = item.category == Category.TV && item.episodes.isNotEmpty()
    val seasons = item.episodes.map { it.season }.distinct().sorted()
    val activeSeason = seasons.getOrNull(state.season) ?: seasons.firstOrNull() ?: 1
    val episodes = item.episodes.filter { it.season == activeSeason }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            ArtworkBox(
                letter = item.letter,
                gradient = item.gradient,
                artUri = item.artUri,
                thumbWidth = 720,
                thumbHeight = 400,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color(0x33000000), Mv.Page))),
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
                    Text(text = metaLine(item), color = Mv.Secondary, fontSize = 11.5.sp)
                }

                Spacer(Modifier.height(16.dp))
                AccentButton(
                    text = if (percent in 1..99) "▶ Resume · $percent%" else "▶ Play",
                    modifier = Modifier.fillMaxWidth(),
                ) { state.play(item) }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFEFEFF2))
                        .clickable {
                            val uri = item.episodes.firstOrNull()?.uri ?: item.uri
                            if (uri == null || !Launch.openVideoExternally(context, uri)) {
                                state.showToast("No other app can open this file")
                            }
                        }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Open in another player ↗",
                        color = Mv.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(22.dp))
                SectionHeader(title = "File")
                Spacer(Modifier.height(10.dp))
                FileFacts(item)
            }
        }

        val side: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier = modifier.padding(horizontal = Mv.Gutter)) {
                if (isShow) {
                    SectionHeader(title = "Episodes")
                    Spacer(Modifier.height(10.dp))
                    if (seasons.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { season ->
                                Chip(
                                    text = "Season $season",
                                    selected = season == activeSeason,
                                ) { state.season = seasons.indexOf(season) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    episodes.forEach { episode ->
                        EpisodeRow(episode) {
                            state.play(item, item.episodes.indexOf(episode))
                        }
                        Spacer(Modifier.height(8.dp))
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

private fun metaLine(item: MediaItem): String {
    val pieces = buildList {
        if (item.year.isNotBlank()) add(item.year)
        if (item.episodes.isNotEmpty()) {
            add("${item.episodes.size} episodes")
            add("${item.episodes.map { it.season }.distinct().size} seasons")
        }
        DeviceMedia.formatDuration(item.durationMs).takeIf { it.isNotBlank() }?.let { add(it) }
        if (item.folder.isNotBlank()) add(item.folder)
    }
    return pieces.joinToString(" · ").ifBlank { item.source }
}

@Composable
private fun FileFacts(item: MediaItem) {
    val facts = buildList {
        if (item.folder.isNotBlank()) add("Folder" to item.folder)
        DeviceMedia.formatDuration(item.durationMs).takeIf { it.isNotBlank() }?.let { add("Length" to it) }
        DeviceMedia.formatSize(item.sizeBytes)?.let { add("Size" to it) }
        item.mime?.let { add("Type" to it) }
        DeviceMedia.formatDate(item.addedAt)?.let { add("Modified" to it) }
        add("Source" to item.source)
    }
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            facts.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = label, color = Mv.Secondary, fontSize = 11.5.sp, modifier = Modifier.width(78.dp))
                    Text(
                        text = value,
                        color = Mv.Text,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: EpisodeFile, onClick: () -> Unit) {
    CardSurface(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
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
                Text(
                    text = listOfNotNull(
                        "S${episode.season} E${episode.number}",
                        DeviceMedia.formatDuration(episode.durationMs).ifBlank { null },
                        DeviceMedia.formatSize(episode.sizeBytes),
                    ).joinToString(" · "),
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                )
            }
            PlayGlyph(size = 12.dp, color = Mv.Secondary)
        }
    }
}
