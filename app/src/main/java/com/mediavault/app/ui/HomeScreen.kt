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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.Category
import com.mediavault.app.data.DemoData
import com.mediavault.app.data.MediaItem
import com.mediavault.app.util.Launch

@Composable
fun HomeScreen(state: AppState, unfolded: Boolean, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val wide = if (unfolded) 190.dp else 160.dp
    val poster = if (unfolded) 124.dp else 104.dp
    val hero = state.continueWatching

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            ContinueHero(
                item = hero,
                percent = state.progressOf(hero),
                modifier = Modifier.padding(horizontal = Mv.Gutter),
                onOpen = { state.openDetail(hero) },
            )
        }

        item {
            Column {
                SectionHeader(
                    title = "New from your subscriptions",
                    action = "YouTube ↗",
                    onAction = {
                        if (!Launch.youtube(context, "subscriptions")) state.showToast("No app can open YouTube")
                    },
                    modifier = Modifier.padding(horizontal = Mv.Gutter),
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Mv.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(DemoData.youtube) { video ->
                        WideCard(
                            title = video.title,
                            subtitle = video.sub,
                            letter = "▶",
                            gradient = video.gradient,
                            width = wide,
                        ) {
                            state.showToast("↗ Opening “${video.title}” in YouTube…")
                            Launch.youtube(context, video.query)
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(
                    title = "Live now",
                    action = "See all",
                    onAction = { state.tab = Tab.LIVE },
                    modifier = Modifier.padding(horizontal = Mv.Gutter),
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Mv.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(DemoData.twitch) { channel ->
                        WideCard(
                            title = channel.channel,
                            subtitle = "${channel.game} · ${channel.viewers} watching",
                            letter = channel.letter,
                            gradient = channel.gradient,
                            width = wide,
                            live = true,
                            liveLabel = "${channel.viewers} watching",
                        ) {
                            state.showToast("↗ Opening ${channel.channel} in Twitch…")
                            Launch.twitch(context, channel.channel)
                        }
                    }
                }
            }
        }

        item {
            PosterRow(
                title = "Movies",
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

        item {
            PosterRow(
                title = "Jump back in · Games",
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
}

@Composable
private fun PosterRow(
    title: String,
    items: List<MediaItem>,
    width: androidx.compose.ui.unit.Dp,
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
            items(items.take(12)) { item ->
                PosterCard(
                    title = item.title,
                    subtitle = item.sub,
                    letter = item.letter,
                    gradient = item.gradient,
                    aspect = aspect,
                    tag = item.tag,
                    progress = state.progressOf(item),
                    modifier = Modifier.width(width),
                ) {
                    if (item.category == Category.GAMES) launchGame(state, context, item)
                    else state.openDetail(item)
                }
            }
        }
    }
}

internal fun launchGame(state: AppState, context: android.content.Context, item: MediaItem) {
    val packageName = item.packageName
    when {
        packageName == null -> state.showToast("▶ Launching ${item.title} via ${item.source}…")
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
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 8.2f)
            .clip(RoundedCornerShape(Mv.HeroRadius))
            .background(gradientBrush(item.gradient))
            .clickable { onOpen() },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x1A000000), Color(0xB3000000))
                    )
                ),
        )
        Text(
            text = "CONTINUE",
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
                text = "Continue watching · ${item.sub} · ${item.at} of ${item.length}",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 60.dp),
            )
            Spacer(Modifier.height(10.dp))
            ProgressBar(percent = percent)
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onOpen() },
            contentAlignment = Alignment.Center,
        ) {
            PlayGlyph(size = 16.dp, color = Mv.Text)
        }
    }
}
