package com.mediavault.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediavault.app.data.DemoData
import com.mediavault.app.data.VaultUser
import com.mediavault.app.data.WatchedFolder
import kotlinx.coroutines.launch

@Composable
fun SourcesScreen(state: AppState, unfolded: Boolean, contentPadding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val accent = LocalAccent.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { state.addFolder(uri) }
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(
                    title = "Watched folders",
                    action = if (state.scanning) "Scanning…" else "Rescan",
                    onAction = { scope.launch { state.rescan() } },
                )
                Spacer(Modifier.height(10.dp))
                state.folders.forEach { folder ->
                    FolderRow(folder) { state.removeFolder(folder) }
                    Spacer(Modifier.height(8.dp))
                }
                DashedAddRow(text = "+ Add a folder to watch") {
                    picker.launch(null)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Folders you add are scanned on the device — filenames become movies, " +
                        "episodes, tracks and file rows. Nothing is uploaded.",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Users")
                Spacer(Modifier.height(10.dp))
                CardSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        state.users.forEach { user ->
                            UserRow(user, accent) { state.removeUser(user) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                DashedAddRow(text = "+ Add a user") { state.addUser() }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (state.multiUser)
                        "Profile switcher is on — the avatar appears top-right."
                    else
                        "With one user, profiles stay hidden. Add a second to turn on the switcher.",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Connected services")
                Spacer(Modifier.height(10.dp))
                DemoData.services.forEach { service ->
                    val connected = service.name in state.connected
                    CardSurface(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(gradientBrush(service.gradient)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = service.abbr,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
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
                            SmallPillButton(
                                text = if (connected) "✓ Connected" else "Connect",
                                active = connected,
                            ) { state.toggleService(service.name) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Accent")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Mv.AccentChoices.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (state.accentIndex == index) 3.dp else 0.dp,
                                    color = Mv.Text,
                                    shape = CircleShape,
                                )
                                .clickable { state.setAccent(index) },
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "MediaVault · design build",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun FolderRow(folder: WatchedFolder, onRemove: () -> Unit) {
    val accent = LocalAccent.current
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "📁", fontSize = 15.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.path,
                    color = Mv.Text,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Mv.Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = folder.meta, color = Mv.Secondary, fontSize = 11.sp)
            }
            Text(
                text = "✕",
                color = Mv.Secondary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onRemove() }.padding(6.dp),
            )
        }
    }
}

@Composable
private fun UserRow(user: VaultUser, accent: Color, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(accent, accent.copy(alpha = 0.55f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = user.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = user.name,
            color = Mv.Text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(text = user.role, color = Mv.Secondary, fontSize = 11.sp)
        if (user.role != "Owner") {
            Text(
                text = "✕",
                color = Mv.Secondary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onRemove() }.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun DashedAddRow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Mv.RowRadius))
            .border(1.dp, Color(0x33000000), RoundedCornerShape(Mv.RowRadius))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = LocalAccent.current,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
