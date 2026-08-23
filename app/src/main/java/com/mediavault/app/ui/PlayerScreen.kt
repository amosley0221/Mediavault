package com.mediavault.app.ui

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mediavault.app.util.Launch
import kotlinx.coroutines.delay

/**
 * The full-screen player. A watched-folder file really plays through ExoPlayer; a title
 * from a service the vault only indexes shows the same chrome with a simulated timeline
 * and an "open in …" escape hatch.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(state: AppState, target: PlaybackTarget) {
    val context = LocalContext.current
    var percent by remember(target) { mutableStateOf(target.startPercent) }
    var playing by remember(target) { mutableStateOf(true) }
    var durationMs by remember(target) { mutableStateOf(0L) }

    val exo = remember(target.uri) {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(ExoMediaItem.fromUri(Uri.parse(target.uri)))
                prepare()
                playWhenReady = true
            }
        }.getOrNull()
    }

    DisposableEffect(exo) {
        onDispose { exo?.release() }
    }

    // Tick the scrub bar off the player's real position.
    LaunchedEffect(target, playing) {
        while (true) {
            delay(1000)
            if (!playing) continue
            if (exo != null) {
                val duration = exo.duration
                if (duration > 0) {
                    durationMs = duration
                    percent = ((exo.currentPosition * 100) / duration).toInt().coerceIn(0, 100)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Mv.PlayerBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (exo != null) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).also { exo.setVideoSurfaceView(it) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = "This file could not be opened for playback.\nTry \"Open in another player\" below.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                }

                Text(
                    text = "MediaVault Player",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(18.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0x1FFFFFFF))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )

                CircleGlyphButton(
                    glyph = "✕",
                    background = Color(0x33FFFFFF),
                    foreground = Color.White,
                    fontSize = 14,
                    modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                ) { state.closePlayer(percent) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D0F))
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = target.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(text = target.sourceLabel, color = Color(0xFF86868B), fontSize = 11.5.sp)
                Spacer(Modifier.height(16.dp))

                ProgressBar(
                    percent = percent,
                    track = Color(0x33FFFFFF),
                    height = 4.dp,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = timecode(percent, maxOf(durationMs, target.durationMs)),
                        color = Color(0xFF86868B),
                        fontSize = 11.sp,
                        fontFamily = Mv.Mono,
                    )
                    Text(
                        text = totalLabel(maxOf(durationMs, target.durationMs)),
                        color = Color(0xFF86868B),
                        fontSize = 11.sp,
                        fontFamily = Mv.Mono,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleGlyphButton(
                        glyph = if (playing) "❚❚" else "▶",
                        size = 46.dp,
                        background = Color.White,
                        foreground = Mv.Text,
                        fontSize = 13,
                    ) {
                        playing = !playing
                        exo?.playWhenReady = playing
                    }
                    Spacer(Modifier.size(12.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0x1FFFFFFF))
                            .clickable {
                                if (Launch.openVideoExternally(context, target.uri)) {
                                    state.closePlayer(percent)
                                } else {
                                    state.showToast("No other app on this phone can play this file")
                                }
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Open in another player ↗",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1FFFFFFF))
                            .clickable { state.showToast("CC · no subtitle tracks found") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "CC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun timecode(percent: Int, durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    return format((durationMs * percent / 100) / 1000)
}

private fun totalLabel(durationMs: Long): String =
    if (durationMs > 0) format(durationMs / 1000) else "--:--"

private fun format(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
