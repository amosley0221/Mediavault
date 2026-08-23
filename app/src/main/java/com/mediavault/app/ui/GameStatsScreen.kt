package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.Playtime

/** Where the hours went: totals, the most-played list, and a breakdown by source. */
@Composable
fun GameStatsScreen(
    state: AppState,
    unfolded: Boolean,
    contentPadding: PaddingValues,
    onRequestPlaytimeAccess: () -> Unit,
) {
    val mostPlayed = state.games.filter { it.playtimeMs > 0 }.sortedByDescending { it.playtimeMs }.take(8)
    val peak = mostPlayed.firstOrNull()?.playtimeMs ?: 1L

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
            CircleGlyphButton(glyph = "‹", fontSize = 20) { state.showGameStats = false }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(text = "Stats", color = Mv.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Across every source", color = Mv.Secondary, fontSize = 12.sp)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = Mv.Gutter,
                end = Mv.Gutter,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (!state.hasPlaytimeAccess) {
                item {
                    EmptyCard(
                        title = "Playtime needs usage access",
                        message = "Android keeps the record of how long each app has been on " +
                            "screen behind a separate switch. Turn it on and these figures fill in.",
                        primaryLabel = "Turn on usage access",
                        onPrimary = onRequestPlaytimeAccess,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        value = Playtime.format(state.totalPlaytimeMs),
                        label = "TOTAL PLAYTIME",
                        accent = true,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = Playtime.format(state.weekPlaytimeMs),
                        label = "THIS WEEK",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (mostPlayed.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(title = "Most played")
                        Spacer(Modifier.height(10.dp))
                        mostPlayed.forEach { game ->
                            Text(
                                text = game.title,
                                color = Mv.Text,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProgressBar(
                                    percent = ((game.playtimeMs * 100) / peak).toInt(),
                                    track = Mv.Hairline,
                                    height = 6.dp,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = Playtime.format(game.playtimeMs),
                                    color = Mv.Secondary,
                                    fontSize = 11.5.sp,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }

            item {
                Column {
                    SectionHeader(title = "By source")
                    Spacer(Modifier.height(10.dp))
                    state.gameSources.forEach { (source, count, playtime) ->
                        CardSurface(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.ifBlank { "Unknown" },
                                        color = Mv.Text,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "$count game${if (count == 1) "" else "s"}",
                                        color = Mv.Secondary,
                                        fontSize = 11.sp,
                                    )
                                }
                                Text(
                                    text = if (playtime > 0) Playtime.format(playtime) else "–",
                                    color = if (playtime > 0) LocalAccent.current else Mv.Secondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    CardSurface(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text(
                text = value,
                color = if (accent) LocalAccent.current else Mv.Text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = Mv.Secondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            )
        }
    }
}
