package com.mediavault.app.ui

import android.app.Activity
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mediavault.app.data.MediaItem
import com.mediavault.app.data.Playtime
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Big Picture: the library at arm's length. One row of large covers, the centred one lit
 * and named, everything else dimmed — driven by swipe, by tap, or by a controller's
 * D-pad. System bars are hidden while it is open and restored on the way out.
 */
@Composable
fun BigPictureScreen(
    items: List<MediaItem>,
    state: AppState,
    onLaunch: (MediaItem) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Immersive while open; the bars come back however the screen is left.
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.let {
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    BackHandler { onExit() }

    val focused by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - centre) }
                ?.index
                ?: 0
        }
    }
    val hero = items.getOrNull(focused)

    fun moveTo(index: Int) {
        val target = index.coerceIn(0, items.lastIndex)
        scope.launch { listState.animateScrollToItem(target) }
    }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Mv.PlayerBg)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key.nativeKeyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { moveTo(focused - 1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { moveTo(focused + 1); true }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_BUTTON_A -> {
                        hero?.let(onLaunch); true
                    }
                    KeyEvent.KEYCODE_BUTTON_B -> { onExit(); true }
                    else -> false
                }
            },
    ) {
        val coverWidth = minOf(maxWidth * 0.42f, 300.dp)
        val sidePadding = (maxWidth - coverWidth) / 2

        // The focused cover's own colours, washed across the screen behind it.
        hero?.let { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush(item.gradient)),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color(0xF2000000), Color(0xFF000000))
                    )
                ),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            LazyRow(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState),
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    val isFocused = index == focused
                    val scale by animateFloatAsState(
                        targetValue = if (isFocused) 1f else 0.78f,
                        animationSpec = tween(220),
                        label = "cover",
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (isFocused) 1f else 0.45f,
                        animationSpec = tween(220),
                        label = "dim",
                    )
                    Box(
                        modifier = Modifier
                            .width(coverWidth)
                            .scale(scale)
                            .clickable {
                                if (isFocused) onLaunch(item) else moveTo(index)
                            },
                    ) {
                        ArtworkBox(
                            letter = item.letter,
                            gradient = item.gradient,
                            artUri = item.artUri,
                            letterSize = 54,
                            thumbWidth = 480,
                            thumbHeight = 640,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (item.systemId != null) 3f / 4f else 1f)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 1f - alpha)),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            if (hero != null) {
                Text(
                    text = hero.title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = bigPictureMeta(hero),
                    color = Color(0xFFB9B9C0),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(LocalAccent.current)
                            .clickable { onLaunch(hero) }
                            .padding(horizontal = 44.dp, vertical = 16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayGlyph(size = 15.dp, color = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Play",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    CircleGlyphButton(
                        glyph = if (state.isFavorite(hero)) "★" else "☆",
                        size = 52.dp,
                        background = Color(0x1FFFFFFF),
                        foreground = if (state.isFavorite(hero)) LocalAccent.current else Color.White,
                        fontSize = 18,
                    ) { state.toggleFavorite(hero) }
                }
            }
        }

        Text(
            text = "${focused + 1} / ${items.size}",
            color = Color(0x99FFFFFF),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
        )

        CircleGlyphButton(
            glyph = "✕",
            size = 44.dp,
            background = Color(0x33FFFFFF),
            foreground = Color.White,
            fontSize = 15,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
        ) { onExit() }

        Text(
            text = "BIG PICTURE",
            color = Color(0x66FFFFFF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(26.dp),
        )
    }
}

private fun bigPictureMeta(item: MediaItem): String {
    val pieces = buildList {
        add(item.source)
        if (item.playtimeMs > 0) add("${Playtime.format(item.playtimeMs)} played")
        Playtime.formatLastPlayed(item.lastPlayed)?.let { add("last $it") }
    }
    return pieces.joinToString("  ·  ")
}
