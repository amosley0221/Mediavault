package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.Category
import com.mediavault.app.util.Launch

@Composable
fun LibraryScreen(state: AppState, unfolded: Boolean, contentPadding: PaddingValues) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(contentPadding.calculateTopPadding()))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Mv.Gutter, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Category.entries.toList()) { category ->
                Chip(
                    text = category.label,
                    selected = state.category == category,
                ) { state.category = category }
            }
        }
        val listPadding = PaddingValues(
            start = Mv.Gutter,
            end = Mv.Gutter,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding(),
        )
        when (state.category) {
            Category.GAMES, Category.MOVIES, Category.TV -> PosterGrid(state, unfolded, listPadding)
            Category.MUSIC -> MusicList(state, listPadding)
            Category.FILES -> FilesList(state, listPadding)
        }
    }
}

@Composable
private fun PosterGrid(state: AppState, unfolded: Boolean, padding: PaddingValues) {
    val context = LocalContext.current
    val items = state.itemsFor(state.category)
    val aspect = if (state.category == Category.GAMES) 3f / 4f else 2f / 3f
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (unfolded) 5 else 3),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items, key = { it.id }) { item ->
            PosterCard(
                title = item.title,
                subtitle = item.sub,
                letter = item.letter,
                gradient = item.gradient,
                aspect = aspect,
                tag = item.tag,
                progress = state.progressOf(item),
            ) {
                if (item.category == Category.GAMES) launchGame(state, context, item)
                else state.openDetail(item)
            }
        }
    }
}

@Composable
private fun MusicList(state: AppState, padding: PaddingValues) {
    val accent = LocalAccent.current
    LazyColumn(
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.tracks, key = { it.id }) { track ->
            ListRow(
                title = track.title,
                subtitle = track.sub,
                right = track.duration.ifBlank { "▶" },
                onClick = { state.playTrack(track) },
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "♪", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilesList(state: AppState, padding: PaddingValues) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.documents, key = { it.id }) { file ->
            ListRow(
                title = file.name,
                subtitle = file.sub,
                right = "↗ ${file.app}",
                onClick = {
                    val uri = file.uri
                    val opened = uri != null && Launch.openDocument(context, uri, file.mime)
                    state.showToast(
                        when {
                            opened -> "↗ Opening “${file.name}” in ${file.app}…"
                            uri == null -> "↗ Opening “${file.name}” in ${file.app}…"
                            else -> "No app on this phone can open ${file.name}"
                        }
                    )
                },
            ) {
                IconSquare(
                    letter = file.badge,
                    background = Color(file.background or 0xFF000000L),
                    foreground = Color(file.foreground or 0xFF000000L),
                )
            }
        }
    }
}