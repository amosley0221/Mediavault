package com.mediavault.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * What the app shows instead of a fake catalogue: why a shelf is empty, and the one tap
 * that fills it.
 */
@Composable
fun EmptyCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    CardSurface(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                color = Mv.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                color = Mv.Muted,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
            if (primaryLabel != null && onPrimary != null) {
                Spacer(Modifier.height(14.dp))
                AccentButton(text = primaryLabel, modifier = Modifier.fillMaxWidth(), onClick = onPrimary)
            }
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = secondaryLabel,
                        color = LocalAccent.current,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onSecondary() }
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}