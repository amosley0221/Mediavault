package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.DemoData
import com.mediavault.app.data.TwitchChannel
import com.mediavault.app.util.Launch

@Composable
fun LiveScreen(state: AppState, unfolded: Boolean, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val columns = if (unfolded) 3 else 2

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SectionHeader(
                title = "Live on Twitch",
                action = "Twitch ↗",
                onAction = { Launch.web(context, "https://www.twitch.tv/directory/following") },
                modifier = Modifier.padding(horizontal = Mv.Gutter),
            )
        }

        items(DemoData.twitch.chunked(columns)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Mv.Gutter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { channel ->
                    TwitchCard(
                        channel = channel,
                        modifier = Modifier.weight(1f),
                    ) {
                        state.showToast("↗ Opening ${channel.channel} in Twitch…")
                        Launch.twitch(context, channel.channel)
                    }
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHeader(
                title = "YouTube TV · On now",
                action = "Guide ↗",
                onAction = { Launch.youtubeTv(context) },
                modifier = Modifier.padding(horizontal = Mv.Gutter),
            )
        }

        items(DemoData.youtubeTv) { program ->
            CardSurface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Mv.Gutter),
                onClick = {
                    state.showToast("↗ Tuning to ${program.channel} in YouTube TV…")
                    Launch.youtubeTv(context)
                },
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(gradientBrush(program.gradient)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = program.abbr,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = program.program,
                                color = Mv.Text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${program.channel} · ${program.time}",
                                color = Mv.Secondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    ProgressBar(
                        percent = program.progress,
                        color = Mv.Live,
                        track = Color(0x14000000),
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TwitchCard(
    channel: TwitchChannel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Mv.CoverRadius))
                .background(gradientBrush(channel.gradient)),
        ) {
            Text(
                text = channel.letter,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            LiveBadge(modifier = Modifier.align(Alignment.TopStart).padding(7.dp))
            Text(
                text = "${channel.viewers} watching",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x991D1D1F))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = channel.channel,
            color = Mv.Text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = channel.game,
            color = Mv.Secondary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
