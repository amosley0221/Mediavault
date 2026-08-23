package com.mediavault.app.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.GameSort
import com.mediavault.app.data.MediaItem
import com.mediavault.app.data.Playtime

/**
 * The games shelf: a carousel of covers with the centred game's details underneath, then
 * every game in a grid. Long-press any cover for its box art.
 */
@Composable
fun GamesSection(
    state: AppState,
    unfolded: Boolean,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val games = state.games
    val carousel = games.take(20)
    val listState = rememberLazyListState()
    val centred by remember(carousel) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            carousel.getOrNull(index)
        }
    }

    if (games.isEmpty()) {
        Column(modifier = Modifier.padding(horizontal = Mv.Gutter).padding(top = 8.dp)) {
            EmptyCard(
                title = if (state.scanning) "Scanning…" else "No games yet",
                message = "MediaVault lists apps Android reports as games, plus any ROMs it finds. " +
                    "If something is missing, add it by hand.",
                primaryLabel = "Add a game",
                onPrimary = { state.showAddGame = true },
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (unfolded) 5 else 3),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 60.dp),
                ) {
                    itemsIndexed(carousel, key = { _, item -> item.id }) { index, item ->
                        val focused = index == listState.firstVisibleItemIndex
                        val scale by animateFloatAsState(if (focused) 1f else 0.86f, label = "cover")
                        CarouselCover(
                            item = item,
                            modifier = Modifier
                                .width(if (unfolded) 210.dp else 170.dp)
                                .scale(scale),
                            onClick = { launchGame(state, context, item) },
                            onLongClick = if (item.systemId != null) {
                                { state.askForArt(item) }
                            } else null,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                val hero = centred ?: carousel.first()
                Text(
                    text = hero.title,
                    color = Mv.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = heroMeta(hero),
                    color = Mv.Secondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(LocalAccent.current)
                            .clickable { launchGame(state, context, hero) }
                            .padding(horizontal = 30.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayGlyph(size = 13.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Play",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    CircleGlyphButton(
                        glyph = if (state.isFavorite(hero)) "★" else "☆",
                        size = 44.dp,
                        background = Mv.Card,
                        foreground = if (state.isFavorite(hero)) LocalAccent.current else Mv.Secondary,
                        fontSize = 16,
                    ) { state.toggleFavorite(hero) }
                    Spacer(Modifier.width(10.dp))
                    CircleGlyphButton(
                        glyph = "ⓘ",
                        size = 44.dp,
                        background = Mv.Card,
                        foreground = Mv.Secondary,
                        fontSize = 16,
                    ) { state.showGameStats = true }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "ALL GAMES · ${games.size}",
                        color = Mv.Secondary,
                        fontSize = Mv.SectionHeader,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GameSort.entries.toList()) { sort ->
                        Chip(
                            text = sort.label,
                            selected = state.gameSort == sort,
                        ) { state.gameSort = sort }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        items(games, key = { it.id }) { item ->
            Box {
                PosterCard(
                    title = item.title,
                    subtitle = item.sub,
                    letter = item.letter,
                    gradient = item.gradient,
                    aspect = 1f,
                    tag = item.tag,
                    artUri = item.artUri,
                    onLongClick = { state.askForArt(item) },
                ) { launchGame(state, context, item) }
                if (state.isFavorite(item)) {
                    Text(
                        text = "★",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color(0x991D1D1F))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun heroMeta(item: MediaItem): String {
    val pieces = buildList {
        add(item.source)
        if (item.playtimeMs > 0) add("${Playtime.format(item.playtimeMs)} played")
        Playtime.formatLastPlayed(item.lastPlayed)?.let { add("last $it") }
    }
    return pieces.joinToString(" · ")
}

@Composable
private fun CarouselCover(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    PosterCard(
        title = item.title,
        subtitle = item.sub,
        letter = item.letter,
        gradient = item.gradient,
        aspect = 3f / 4f,
        tag = item.tag,
        artUri = item.artUri,
        modifier = modifier,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
