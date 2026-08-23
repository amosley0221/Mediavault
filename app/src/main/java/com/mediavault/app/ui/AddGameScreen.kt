package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.InstalledApp
import com.mediavault.app.data.Thumbnails
import kotlinx.coroutines.launch

/**
 * Everything installed, searchable — for the games Android never flagged as games. Adding
 * one pins it into the library from then on.
 */
@Composable
fun AddGameScreen(state: AppState, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val apps = remember(context, state.manualGames) { state.installedApps() }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Mv.Page)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Mv.Gutter,
                    end = Mv.Gutter,
                    top = contentPadding.calculateTopPadding(),
                    bottom = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleGlyphButton(glyph = "‹", fontSize = 20) { state.showAddGame = false }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(text = "Add a game", color = Mv.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "For anything the scanner missed",
                    color = Mv.Secondary,
                    fontSize = 12.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = Mv.Gutter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Mv.Card)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            if (query.isEmpty()) {
                Text(text = "Search everything installed…", color = Mv.Secondary, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Mv.Text, fontSize = 14.sp),
                cursorBrush = SolidColor(LocalAccent.current),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn(
            contentPadding = PaddingValues(
                start = Mv.Gutter,
                end = Mv.Gutter,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    added = state.isGameAdded(app.packageName),
                ) { scope.launch { state.addManualGame(app.packageName, app.label) } }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, added: Boolean, onAdd: () -> Unit) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkBox(
                letter = app.label.take(1).uppercase(),
                gradient = com.mediavault.app.data.DeviceMedia.gradientFor(app.packageName),
                artUri = Thumbnails.APP_ICON_SCHEME + app.packageName,
                letterSize = 15,
                thumbWidth = 96,
                thumbHeight = 96,
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    color = Mv.Text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${app.sourceLabel} · ${app.packageName}",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            SmallPillButton(
                text = if (added) "✓ Added" else "+ Add",
                active = added,
                onClick = { if (!added) onAdd() },
            )
        }
    }
}
