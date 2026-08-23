package com.mediavault.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.ServiceDef
import com.mediavault.app.data.Services
import com.mediavault.app.util.Launch

/**
 * Live content belongs to the streaming apps. Until their APIs are wired up MediaVault is
 * honest about that: it launches them rather than inventing a guide.
 */
@Composable
fun LiveScreen(state: AppState, unfolded: Boolean, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val installed = remember(context) { Services.all.filter { isInstalled(context, it) } }
    val rest = Services.all - installed.toSet()

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Live & streaming")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "MediaVault indexes what is on this phone. Live channels and streaming " +
                        "catalogues live in their own apps — tap to jump straight there.",
                    color = Mv.Secondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }

        if (installed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Installed on this phone",
                    modifier = Modifier.padding(horizontal = Mv.Gutter),
                )
            }
            items(installed, key = { it.name }) { service ->
                ServiceRow(service, installed = true, modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                    open(state, context, service)
                }
            }
        }

        item {
            SectionHeader(
                title = "Other services",
                modifier = Modifier.padding(horizontal = Mv.Gutter),
            )
        }
        items(rest, key = { it.name }) { service ->
            ServiceRow(service, installed = false, modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                open(state, context, service)
            }
        }
    }
}

private fun open(state: AppState, context: android.content.Context, service: ServiceDef) {
    val packageName = service.packageHint
    val launched = packageName != null && Launch.launchApp(context, packageName)
    if (!launched && !Launch.web(context, service.launchUri)) {
        state.showToast("Nothing on this phone can open ${service.name}")
    }
}

private fun isInstalled(context: android.content.Context, service: ServiceDef): Boolean {
    val packageName = service.packageHint ?: return false
    return context.packageManager.getLaunchIntentForPackage(packageName) != null
}

@Composable
private fun ServiceRow(
    service: ServiceDef,
    installed: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CardSurface(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gradientBrush(service.gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = service.abbr, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    color = Mv.Text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = service.description,
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (installed) "Open ↗" else "Web ↗",
                color = LocalAccent.current,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
