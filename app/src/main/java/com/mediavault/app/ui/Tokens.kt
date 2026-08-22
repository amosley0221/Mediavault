package com.mediavault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design tokens from the MediaVault v3 handoff. */
object Mv {
    val Page = Color(0xFFF5F5F7)
    val Card = Color(0xFFFFFFFF)
    val Text = Color(0xFF1D1D1F)
    val Secondary = Color(0xFF86868B)
    val Muted = Color(0xFF5B5B60)
    val Hairline = Color(0x12000000)
    val Live = Color(0xFFE0245E)
    val Watched = Color(0xFF34C759)
    val PlayerBg = Color(0xFF0D0D0F)
    val Toast = Color(0xFF1D1D1F)

    val AccentChoices = listOf(
        Color(0xFF0A7CFF),
        Color(0xFF7AB8FF),
        Color(0xFFFF6B3D),
        Color(0xFFA06BFF),
    )

    val Gutter = 22.dp
    val RowRadius = 14.dp
    val HeroRadius = 20.dp
    val CoverRadius = 12.dp

    val SectionHeader = 11.sp
    val CardTitle = 13.sp
    val Mono = FontFamily.Monospace
}

val LocalAccent = compositionLocalOf { Mv.AccentChoices[0] }

@Composable
fun ProvideAccent(accent: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAccent provides accent, content = content)
}
