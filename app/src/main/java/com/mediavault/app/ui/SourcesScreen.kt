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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.mediavault.app.data.Category
import com.mediavault.app.data.FolderRule
import com.mediavault.app.data.LibraryFolder
import com.mediavault.app.data.MediaFolder
import com.mediavault.app.data.Emulators
import com.mediavault.app.data.GameSystem
import com.mediavault.app.data.MediaAccess
import com.mediavault.app.data.Thumbnails
import com.mediavault.app.data.VaultUser
import com.mediavault.app.data.WatchedFolder
import kotlinx.coroutines.launch

@Composable
fun SourcesScreen(
    state: AppState,
    unfolded: Boolean,
    contentPadding: PaddingValues,
    onRequestAccess: () -> Unit,
    onRequestAllFiles: () -> Unit,
    onRequestPlaytime: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val accent = LocalAccent.current
    val context = LocalContext.current
    var editingSystem by remember { mutableStateOf<GameSystem?>(null) }
    var pickingFor by remember { mutableStateOf<Category?>(null) }
    var sortingFolder by remember { mutableStateOf<Pair<MediaFolder, Boolean>?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { state.addFolder(uri) }
    }

    val libraryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val category = pickingFor
        pickingFor = null
        if (uri != null && category != null) scope.launch { state.addLibraryFolder(uri, category) }
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(
                    title = "This phone",
                    action = if (state.scanning) "Scanning…" else "Rescan",
                    onAction = { scope.launch { state.rescan() } },
                )
                Spacer(Modifier.height(10.dp))

                AccessRow(
                    title = "Media access",
                    subtitle = if (state.hasMediaAccess)
                        "Videos, music and photos are readable"
                    else
                        "Needed before anything shows up in the library",
                    granted = state.hasMediaAccess,
                    onGrant = onRequestAccess,
                )
                Spacer(Modifier.height(8.dp))
                AccessRow(
                    title = "All files access",
                    subtitle = if (state.hasAllFilesAccess)
                        "Documents and downloads are readable"
                    else
                        "Optional — required for PDFs, spreadsheets and other documents",
                    granted = state.hasAllFilesAccess,
                    onGrant = onRequestAllFiles,
                )

                Spacer(Modifier.height(8.dp))
                AccessRow(
                    title = "Usage access",
                    subtitle = if (state.hasPlaytimeAccess)
                        "Granted — playtime is being tracked"
                    else
                        "Optional — needed to show how long each game has been played",
                    granted = state.hasPlaytimeAccess,
                    onGrant = onRequestPlaytime,
                )

                Spacer(Modifier.height(12.dp))
                LibraryCounts(state)
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Video folders")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Nothing is in the library until you put it there. Assign a folder to " +
                        "Movies or TV and its videos appear; leave it alone and they never do.",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(10.dp))
                if (state.videoFolders.isEmpty()) {
                    Text(
                        text = if (state.scanning) "Scanning…" else "No video folders found yet.",
                        color = Mv.Secondary,
                        fontSize = 11.sp,
                    )
                } else {
                    state.videoFolders.forEach { folder ->
                        MediaFolderRow(
                            folder = folder,
                            rule = state.ruleFor(folder),
                            unit = "video",
                        ) { sortingFolder = folder to true }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Music folders")
                Spacer(Modifier.height(10.dp))
                if (state.audioFolders.isEmpty()) {
                    Text(
                        text = if (state.scanning) "Scanning…" else "No music folders found yet.",
                        color = Mv.Secondary,
                        fontSize = 11.sp,
                    )
                } else {
                    state.audioFolders.forEach { folder ->
                        MediaFolderRow(
                            folder = folder,
                            rule = state.ruleFor(folder),
                            unit = "track",
                        ) { sortingFolder = folder to false }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Folders elsewhere")
                Spacer(Modifier.height(10.dp))
                LibraryFolderGroup(
                    state = state,
                    category = Category.MOVIES,
                    onAdd = { pickingFor = Category.MOVIES; libraryPicker.launch(null) },
                    onRemove = { folder -> scope.launch { state.removeLibraryFolder(folder) } },
                )
                Spacer(Modifier.height(14.dp))
                LibraryFolderGroup(
                    state = state,
                    category = Category.TV,
                    onAdd = { pickingFor = Category.TV; libraryPicker.launch(null) },
                    onRemove = { folder -> scope.launch { state.removeLibraryFolder(folder) } },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Point a category at a folder the system does not index — an SD card, a " +
                        "USB drive. Once set, that category shows nothing outside it.",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Watched folders")
                Spacer(Modifier.height(10.dp))
                state.folders.forEach { folder ->
                    FolderRow(folder) { state.removeFolder(folder) }
                    Spacer(Modifier.height(8.dp))
                }
                DashedAddRow(text = "+ Add a folder to watch") { picker.launch(null) }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Use this for media the system does not index — an SD card, a USB drive, " +
                        "a downloads folder. Everything is read on the device; nothing is uploaded.",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(
                    title = "Game sources",
                    action = "Add a game",
                    onAction = { state.showAddGame = true },
                )
                Spacer(Modifier.height(10.dp))
                if (state.gameSources.isEmpty()) {
                    Text(
                        text = "No games found yet. Anything Android flags as a game is picked up " +
                            "automatically; add the rest by hand.",
                        color = Mv.Secondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                } else {
                    state.gameSources.forEach { (source, count, _) ->
                        val enabled = source !in state.disabledSources
                        CardSurface(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.ifBlank { "Unknown" },
                                        color = if (enabled) Mv.Text else Mv.Secondary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "$count game${if (count == 1) "" else "s"}",
                                        color = Mv.Secondary,
                                        fontSize = 11.sp,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                SmallPillButton(
                                    text = if (enabled) "✓ On" else "Off",
                                    active = enabled,
                                ) { state.toggleSource(source) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Emulators")
                Spacer(Modifier.height(10.dp))
                if (state.romSystems.isEmpty()) {
                    Text(
                        text = "No ROMs found yet. MediaVault recognises them by extension — put " +
                            "them in a folder named for the console (a \"SNES\" or \"GBA\" folder " +
                            "under ROMs works well) and turn on all-files access, or add that " +
                            "folder below.",
                        color = Mv.Secondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                } else {
                    state.romSystems.forEach { system ->
                        val chosen = state.emulators[system.id]
                        val label = chosen?.let { Emulators.labelFor(context, it) } ?: "Ask every time"
                        CardSurface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { editingSystem = system },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(gradientBrush(system.gradient)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = system.tag.take(3),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = system.label,
                                        color = Mv.Text,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${state.romsFor(system).size} ROMs · $label",
                                        color = Mv.Secondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Change",
                                    color = LocalAccent.current,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = "Tapping a ROM opens it in the emulator set here. RetroArch is launched " +
                            "with the matching core; standalone emulators are handed the file.",
                        color = Mv.Secondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Mv.Gutter)) {
                SectionHeader(title = "Box art")
                Spacer(Modifier.height(10.dp))
                val missing = state.romsMissingArt.size
                CardSurface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (state.fetchingArt) null else {
                        { scope.launch { state.downloadMissingArt() } }
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.fetchingArt) "Looking up covers…" else "Download missing covers",
                                color = Mv.Text,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (missing == 0) "Every ROM has a cover"
                                else "$missing without one · searches the libretro archive",
                                color = Mv.Secondary,
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "›",
                            color = Mv.Secondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Long-press any game to set its cover by hand from a picture on this phone. " +
                        "Downloads are the only time MediaVault touches the network.",
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
            }
        }
    }

    sortingFolder?.let { (folder, isVideo) ->
        FolderRuleDialog(
            folder = folder,
            current = state.ruleFor(folder),
            options = if (isVideo) listOf(FolderRule.MOVIES, FolderRule.TV, FolderRule.NONE)
            else listOf(FolderRule.MUSIC, FolderRule.NONE),
            onDismiss = { sortingFolder = null },
            onPick = { rule ->
                sortingFolder = null
                scope.launch { state.setFolderRule(folder, rule) }
            },
        )
    }

    editingSystem?.let { system ->
        EmulatorPickerDialog(
            system = system,
            romTitle = null,
            currentPackage = state.emulators[system.id],
            onDismiss = { editingSystem = null },
            onPick = { packageName, _ ->
                state.setEmulator(system.id, packageName)
                editingSystem = null
            },
        )
    }
}

@Composable
private fun MediaFolderRow(
    folder: MediaFolder,
    rule: FolderRule,
    unit: String,
    onClick: () -> Unit,
) {
    CardSurface(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name.ifBlank { folder.path },
                    color = if (rule == FolderRule.NONE) Mv.Secondary else Mv.Text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${folder.count} $unit${if (folder.count == 1) "" else "s"} · /${folder.path}",
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = rule.label,
                color = if (rule == FolderRule.NONE) Mv.Secondary else LocalAccent.current,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FolderRuleDialog(
    folder: MediaFolder,
    current: FolderRule,
    options: List<FolderRule>,
    onDismiss: () -> Unit,
    onPick: (FolderRule) -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Mv.Card)
                .padding(20.dp),
        ) {
            Text(
                text = folder.name.ifBlank { folder.path },
                color = Mv.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${folder.count} files · /${folder.path}",
                color = Mv.Secondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(16.dp))

            options.forEach { rule ->
                CardSurface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPick(rule) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rule.label,
                                color = Mv.Text,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = when (rule) {
                                    FolderRule.NONE -> "Keep these out of the library"
                                    FolderRule.MOVIES -> "List these under Movies"
                                    FolderRule.TV -> "List these under TV, grouped into series"
                                    FolderRule.MUSIC -> "List these under Music"
                                },
                                color = Mv.Secondary,
                                fontSize = 11.sp,
                            )
                        }
                        if (rule == current) {
                            Text(
                                text = "✓",
                                color = Mv.Watched,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Cancel",
                    color = LocalAccent.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onDismiss() }.padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryFolderGroup(
    state: AppState,
    category: Category,
    onAdd: () -> Unit,
    onRemove: (LibraryFolder) -> Unit,
) {
    val folders = state.libraryFoldersFor(category)
    Text(
        text = category.label,
        color = Mv.Text,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))
    if (folders.isEmpty()) {
        Text(
            text = "Every video folder on the phone",
            color = Mv.Secondary,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(6.dp))
    } else {
        folders.forEach { folder ->
            CardSurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "/" + folder.path,
                        color = Mv.Text,
                        fontSize = 12.5.sp,
                        fontFamily = Mv.Mono,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "✕",
                        color = Mv.Secondary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onRemove(folder) }.padding(6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    DashedAddRow(text = "+ Set a ${category.label.lowercase()} folder", onClick = onAdd)
}

@Composable
private fun LibraryCounts(state: AppState) {
    val counts = listOf(
        "Videos" to state.countFor(Category.MOVIES),
        "Series" to state.countFor(Category.TV),
        "Tracks" to state.countFor(Category.MUSIC),
        "Files" to state.countFor(Category.FILES),
        "Games" to state.countFor(Category.GAMES),
    )
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            counts.forEach { (label, count) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = count.toString(),
                        color = Mv.Text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = label, color = Mv.Secondary, fontSize = 10.5.sp)
                }
            }
        }
    }
}

@Composable
private fun AccessRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (granted) Mv.Watched.copy(alpha = 0.14f) else Color(0xFFFDEAEA)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (granted) "✓" else "!",
                    color = if (granted) Mv.Watched else Color(0xFFC9302C),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Mv.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    color = Mv.Secondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            SmallPillButton(text = if (granted) "✓ On" else "Turn on", active = granted) { onGrant() }
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
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f)))),
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
