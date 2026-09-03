package com.mediavault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.Category
import com.mediavault.app.data.MediaAccess
import com.mediavault.app.util.Launch

@Composable
fun LibraryScreen(
    state: AppState,
    unfolded: Boolean,
    contentPadding: PaddingValues,
    onRequestAccess: () -> Unit,
    onRequestAllFiles: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(contentPadding.calculateTopPadding()))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Mv.Gutter, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Category.entries.toList()) { category ->
                    val count = state.countFor(category)
                    Chip(
                        text = if (count > 0) "${category.label}  $count" else category.label,
                        selected = state.category == category,
                    ) { state.category = category }
                }
            }
            if (state.category == Category.GAMES) {
                CircleGlyphButton(
                    glyph = "+",
                    size = 36.dp,
                    background = Mv.Card,
                    foreground = LocalAccent.current,
                    fontSize = 18,
                    modifier = Modifier.padding(end = 8.dp),
                ) { state.showAddGame = true }
                CircleGlyphButton(
                    glyph = "◔",
                    size = 36.dp,
                    background = Mv.Card,
                    foreground = LocalAccent.current,
                    fontSize = 15,
                    modifier = Modifier.padding(end = 8.dp),
                ) { state.showGameStats = true }
                CircleGlyphButton(
                    glyph = "⛶",
                    size = 36.dp,
                    background = Mv.Card,
                    foreground = LocalAccent.current,
                    fontSize = 15,
                    modifier = Modifier.padding(end = Mv.Gutter),
                ) { state.bigPicture = true }
            }
        }
        val listPadding = PaddingValues(
            start = Mv.Gutter,
            end = Mv.Gutter,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding(),
        )

        if (!state.hasMediaAccess) {
            Column(modifier = Modifier.padding(Mv.Gutter)) {
                EmptyCard(
                    title = "Media access is off",
                    message = "MediaVault needs permission to read the videos, music and files on " +
                        "this phone before it can show a library.",
                    primaryLabel = "Allow access",
                    onPrimary = onRequestAccess,
                )
            }
        } else {
            when (state.category) {
                Category.GAMES -> GamesSection(state, unfolded, listPadding)
                Category.MOVIES, Category.TV -> PosterGrid(state, unfolded, listPadding)
                Category.MUSIC -> MusicList(state, listPadding)
                Category.FILES -> FilesList(state, listPadding, onRequestAllFiles)
            }
        }
    }
}

@Composable
private fun PosterGrid(state: AppState, unfolded: Boolean, padding: PaddingValues) {
    val context = LocalContext.current
    val items = state.itemsFor(state.category)
    val aspect = if (state.category == Category.GAMES) 3f / 4f else 2f / 3f

    if (items.isEmpty()) {
        EmptyForCategory(state, padding)
        return
    }

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
                artUri = item.artUri,
                onLongClick = if (item.systemId != null) {
                    { state.askForArt(item) }
                } else null,
            ) {
                if (item.category == Category.GAMES) launchGame(state, context, item)
                else state.openDetail(item)
            }
        }
    }
}

@Composable
private fun EmptyForCategory(state: AppState, padding: PaddingValues) {
    val needsFolder = state.category in setOf(Category.MOVIES, Category.TV) &&
        !state.hasSourceFor(state.category)
    val (title, message) = if (needsFolder) {
        "No ${state.category.label.lowercase()} folder chosen" to
            "MediaVault only lists what you point it at. Open Sources, find the folder your " +
                "${state.category.label.lowercase()} live in, and assign it — everything else on " +
                "the phone stays out."
    } else when (state.category) {
        Category.GAMES -> "No games or ROMs found" to
            "This tab lists ROM files on the phone alongside installed games. ROMs are matched by " +
                "extension — .gba, .smc, .nes and friends — so keep them in a folder named for the " +
                "console, and turn on all-files access under Sources so they can be read."
        Category.TV -> "No series in that folder" to
            "Episodes group into a series when the filename carries a season and episode " +
                "number, like \"Show.S02E07.mkv\"."
        else -> "No videos in that folder" to
            "The folder you assigned holds no video MediaVault can read."
    }
    Column(modifier = Modifier.padding(horizontal = Mv.Gutter).padding(top = 8.dp)) {
        EmptyCard(
            title = if (state.scanning) "Scanning…" else title,
            message = message,
            primaryLabel = "Open Sources",
            onPrimary = { state.tab = Tab.SOURCES },
        )
    }
}

@Composable
private fun MusicList(state: AppState, padding: PaddingValues) {
    if (state.tracks.isEmpty()) {
        Column(modifier = Modifier.padding(horizontal = Mv.Gutter).padding(top = 8.dp)) {
            EmptyCard(
                title = when {
                    state.scanning -> "Scanning…"
                    !state.hasSourceFor(Category.MUSIC) -> "No music folder chosen"
                    else -> "No music in that folder"
                },
                message = if (state.hasSourceFor(Category.MUSIC))
                    "The folder you assigned holds no audio MediaVault can read."
                else
                    "Open Sources and assign the folder your music lives in. Nothing is listed " +
                        "here until you do.",
                primaryLabel = "Open Sources",
                onPrimary = { state.tab = Tab.SOURCES },
            )
        }
        return
    }
    LazyColumn(
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.tracks, key = { it.id }) { track ->
            TrackRow(track = track, onClick = { state.playTrack(track) })
        }
    }
}

@Composable
private fun FilesList(state: AppState, padding: PaddingValues, onRequestAllFiles: () -> Unit) {
    val context = LocalContext.current

    if (state.documents.isEmpty()) {
        Column(modifier = Modifier.padding(horizontal = Mv.Gutter).padding(top = 8.dp)) {
            if (MediaAccess.needsAllFilesForDocuments()) {
                EmptyCard(
                    title = "Documents need all-files access",
                    message = "Android only shares photos, video and music through the media " +
                        "permission. To list PDFs, spreadsheets and documents, MediaVault needs " +
                        "the all-files access toggle.",
                    primaryLabel = "Turn on all-files access",
                    onPrimary = onRequestAllFiles,
                )
            } else {
                EmptyCard(
                    title = if (state.scanning) "Scanning…" else "No documents found",
                    message = "Nothing with a document extension turned up in shared storage.",
                    primaryLabel = "Open Sources",
                    onPrimary = { state.tab = Tab.SOURCES },
                )
            }
        }
        return
    }

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
        }
        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${state.documents.size} files",
                color = Mv.Secondary,
                fontSize = 11.sp,
            )
        }
    }
}
