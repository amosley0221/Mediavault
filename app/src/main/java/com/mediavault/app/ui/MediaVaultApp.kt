package com.mediavault.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.MediaAccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TAB_BAR_HEIGHT = 62.dp
private val MINI_PLAYER_HEIGHT = 62.dp

@Composable
fun MediaVaultApp(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        state.refreshAccess()
        if (granted.values.any { it }) {
            scope.launch { state.rescan() }
        } else {
            state.showToast("Media access denied — turn it on in Settings to see your library")
        }
    }

    val onRequestAccess: () -> Unit = {
        if (state.hasMediaAccess) scope.launch { state.rescan() }
        else permissionLauncher.launch(MediaAccess.mediaPermissions)
    }

    val onRequestAllFiles: () -> Unit = {
        val intent = MediaAccess.allFilesSettingsIntent(context)
        if (intent != null) {
            runCatching { context.startActivity(intent) }
                .onFailure { runCatching { context.startActivity(MediaAccess.appSettingsIntent(context)) } }
            state.showToast("Allow all files access, then come back")
        } else {
            onRequestAccess()
        }
    }

    LaunchedEffect(Unit) {
        if (!state.hasMediaAccess) permissionLauncher.launch(MediaAccess.mediaPermissions)
    }

    ProvideAccent(state.accent) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Mv.Page)) {
            val unfolded = maxWidth >= 600.dp
            val systemBars = WindowInsets.systemBars.asPaddingValues()
            val detail = state.detail
            val player = state.player

            BackHandler(enabled = player != null || detail != null || state.tab != Tab.HOME) {
                when {
                    player != null -> state.closePlayer(player.startPercent)
                    detail != null -> state.closeDetail()
                    state.tab != Tab.HOME -> state.tab = Tab.HOME
                }
            }

            val bottomInset = systemBars.calculateBottomPadding()
            val contentPadding = PaddingValues(
                top = systemBars.calculateTopPadding() + 12.dp,
                bottom = bottomInset + TAB_BAR_HEIGHT + 18.dp +
                    (if (state.nowPlaying != null) MINI_PLAYER_HEIGHT else 0.dp),
            )

            if (player != null) {
                PlayerScreen(state, player)
            } else {
                Crossfade(
                    targetState = detail?.id ?: state.tab.name,
                    animationSpec = tween(250),
                    label = "screen",
                ) { key ->
                    if (detail != null && key == detail.id) {
                        DetailScreen(state, detail, unfolded, contentPadding)
                    } else {
                        when (state.tab) {
                            Tab.HOME -> HomeScreen(state, unfolded, contentPadding, onRequestAccess)
                            Tab.LIBRARY -> LibraryScreen(
                                state, unfolded, contentPadding, onRequestAccess, onRequestAllFiles,
                            )
                            Tab.LIVE -> LiveScreen(state, unfolded, contentPadding)
                            Tab.SOURCES -> SourcesScreen(
                                state, unfolded, contentPadding, onRequestAccess, onRequestAllFiles,
                            )
                        }
                    }
                }

                if (state.multiUser && detail == null) {
                    ProfileAvatar(
                        initial = state.users.first().name.take(1),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = systemBars.calculateTopPadding() + 10.dp, end = Mv.Gutter),
                    ) { state.showToast("Profiles: ${state.users.joinToString { it.name }}") }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    state.nowPlaying?.let { track ->
                        MiniPlayer(
                            title = track.title,
                            subtitle = listOf(track.artist, track.album)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .ifBlank { "On this phone" },
                            gradient = track.gradient,
                            artUri = track.artUri,
                            playing = state.trackPlaying,
                            onToggle = { state.toggleTrack() },
                            onClose = { state.stopTrack() },
                            modifier = Modifier.padding(horizontal = Mv.Gutter),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    TabBar(
                        selected = state.tab,
                        bottomInset = bottomInset,
                    ) { state.tab = it; state.closeDetail() }
                }
            }

            state.emulatorPrompt?.let { prompt ->
                EmulatorPickerDialog(
                    system = prompt.system,
                    romTitle = prompt.rom.title,
                    currentPackage = state.emulators[prompt.system.id],
                    onDismiss = { state.dismissEmulatorPrompt() },
                    onPick = { packageName, remember ->
                        state.chooseEmulator(context, prompt.system, packageName, remember)
                    },
                )
            }

            state.toast?.let { message ->
                LaunchedEffect(message) {
                    delay(2500)
                    state.clearToast()
                }
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomInset + TAB_BAR_HEIGHT + 26.dp, start = 28.dp, end = 28.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Mv.Toast)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(initial: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.6f))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercase(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MiniPlayer(
    title: String,
    subtitle: String,
    gradient: Pair<Long, Long>,
    artUri: String?,
    playing: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MINI_PLAYER_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF21D1D1F))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(
            letter = "♪",
            gradient = gradient,
            artUri = artUri,
            letterSize = 15,
            thumbWidth = 128,
            thumbHeight = 128,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = Color(0xFF9A9AA0),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        CircleGlyphButton(
            glyph = if (playing) "❚❚" else "▶",
            size = 34.dp,
            background = Color(0x33FFFFFF),
            foreground = Color.White,
            fontSize = 11,
            onClick = onToggle,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "✕",
            color = Color(0xFF9A9AA0),
            fontSize = 14.sp,
            modifier = Modifier.clickable { onClose() }.padding(6.dp),
        )
    }
}

@Composable
private fun TabBar(selected: Tab, bottomInset: androidx.compose.ui.unit.Dp, onSelect: (Tab) -> Unit) {
    val accent = LocalAccent.current
    val tabs = listOf(
        Tab.HOME to ("⌂" to "Home"),
        Tab.LIBRARY to ("▦" to "Library"),
        Tab.LIVE to ("◉" to "Live"),
        Tab.SOURCES to ("⚙" to "Sources"),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE0F5F5F7))
            .padding(top = 8.dp, bottom = bottomInset + 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, labels) ->
            val active = tab == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text(
                    text = labels.first,
                    color = if (active) accent else Mv.Secondary,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = labels.second,
                    color = if (active) accent else Mv.Secondary,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
