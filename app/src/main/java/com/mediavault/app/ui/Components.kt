package com.mediavault.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the real artwork for a file — a video frame, an album cover — off the main thread.
 * Null while it loads, and for anything the system has no thumbnail for.
 */
@Composable
fun rememberThumbnail(uri: String?, width: Int = 320, height: Int = 480): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf(Thumbnails.cached(uri)?.asImageBitmap()) }
    LaunchedEffect(uri) {
        if (uri != null && bitmap == null && !Thumbnails.isKnownMiss(uri)) {
            bitmap = withContext(Dispatchers.IO) {
                Thumbnails.load(context, uri, width, height)?.asImageBitmap()
            }
        }
    }
    return bitmap
}

/** Artwork if we have it, the gradient-and-initials placeholder if we don't. */
@Composable
fun ArtworkBox(
    letter: String,
    gradient: Pair<Long, Long>,
    artUri: String?,
    modifier: Modifier = Modifier,
    letterSize: Int = 30,
    thumbWidth: Int = 320,
    thumbHeight: Int = 480,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val art = rememberThumbnail(artUri, thumbWidth, thumbHeight)
    Box(modifier = modifier.background(gradientBrush(gradient))) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = letter,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = letterSize.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        overlay()
    }
}

fun gradientBrush(gradient: Pair<Long, Long>): Brush = Brush.linearGradient(
    listOf(Color(gradient.first or 0xFF000000L), Color(gradient.second or 0xFF000000L))
)

@Composable
fun CoverArt(
    letter: String,
    gradient: Pair<Long, Long>,
    modifier: Modifier = Modifier,
    radius: Dp = Mv.CoverRadius,
    letterSize: Int = 30,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(gradientBrush(gradient)),
    ) {
        Text(
            text = letter,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = letterSize.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        overlay()
    }
}

@Composable
fun TagPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0xCC1D1D1F))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title.uppercase(),
            color = Mv.Secondary,
            fontSize = Mv.SectionHeader,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = LocalAccent.current,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onAction() }.padding(start = 12.dp),
            )
        }
    }
}

@Composable
fun ProgressBar(
    percent: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalAccent.current,
    track: Color = Color(0x33FFFFFF),
    height: Dp = 3.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .clip(RoundedCornerShape(99.dp))
                .background(color),
        )
    }
}

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    radius: Dp = Mv.RowRadius,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Mv.Card)
            .border(1.dp, Mv.Hairline, RoundedCornerShape(radius))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        content()
    }
}

@Composable
fun ListRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    right: String? = null,
    rightColor: Color = Mv.Secondary,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    CardSurface(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Mv.Text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (right != null) {
                Spacer(Modifier.width(10.dp))
                Text(text = right, color = rightColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun IconSquare(
    letter: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = letter, color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccentButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (enabled) accent else Color(0xFFE6E6EA))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Mv.Secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SmallPillButton(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (active) Color(0xFFEFEFF2) else accent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = if (active) Mv.Secondary else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Mv.Text else Mv.Card)
            .border(1.dp, if (selected) Color.Transparent else Mv.Hairline, RoundedCornerShape(99.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Mv.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    title: String,
    subtitle: String,
    letter: String,
    gradient: Pair<Long, Long>,
    aspect: Float,
    modifier: Modifier = Modifier,
    tag: String? = null,
    progress: Int = 0,
    artUri: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val tapModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onLongClick = onLongClick, onClick = onClick)
    } else {
        Modifier.clickable { onClick() }
    }
    Column(modifier = modifier.then(tapModifier)) {
        ArtworkBox(
            letter = letter,
            gradient = gradient,
            artUri = artUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(Mv.CoverRadius)),
        ) {
            if (tag != null) {
                TagPill(
                    text = tag,
                    modifier = Modifier.align(Alignment.BottomStart).padding(7.dp),
                )
            }
            if (progress in 1..99) {
                ProgressBar(
                    percent = progress,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = Mv.Text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = Mv.Secondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun WideCard(
    title: String,
    subtitle: String,
    letter: String,
    gradient: Pair<Long, Long>,
    width: Dp,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    liveLabel: String? = null,
    artUri: String? = null,
    onClick: () -> Unit,
) {
    Column(modifier = modifier.width(width).clickable { onClick() }) {
        ArtworkBox(
            letter = letter,
            gradient = gradient,
            artUri = artUri,
            thumbWidth = 480,
            thumbHeight = 270,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Mv.CoverRadius)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                PlayGlyph(size = 12.dp, color = Color.White)
            }
            if (live) {
                LiveBadge(modifier = Modifier.align(Alignment.TopStart).padding(7.dp))
            }
            if (liveLabel != null) {
                Text(
                    text = liveLabel,
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
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = Mv.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = Mv.Secondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Mv.Live)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        Spacer(Modifier.width(4.dp))
        Text(text = "LIVE", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

/** ▶ drawn as text so no icon dependency is needed. */
@Composable
fun PlayGlyph(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = "▶",
        color = color,
        fontSize = size.value.sp,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
fun CircleGlyphButton(
    glyph: String,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    background: Color = Color(0xE6FFFFFF),
    foreground: Color = Mv.Text,
    fontSize: Int = 15,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = foreground, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}
