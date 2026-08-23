package com.mediavault.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.mediavault.app.data.BoxArt
import com.mediavault.app.data.Category
import com.mediavault.app.data.DeviceMedia
import com.mediavault.app.data.Emulators
import com.mediavault.app.data.GameSystem
import com.mediavault.app.data.DocFile
import com.mediavault.app.data.FolderRule
import com.mediavault.app.data.InstalledApps
import com.mediavault.app.data.LibraryFolder
import com.mediavault.app.data.LocalScanner
import com.mediavault.app.data.MediaAccess
import com.mediavault.app.data.MediaItem
import com.mediavault.app.data.ScanResult
import com.mediavault.app.data.Track
import com.mediavault.app.data.VaultStore
import com.mediavault.app.data.VaultUser
import com.mediavault.app.data.VideoFilters
import com.mediavault.app.data.MediaFolder
import com.mediavault.app.data.WatchedFolder
import com.mediavault.app.media.AudioController
import com.mediavault.app.util.RomLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Tab { HOME, LIBRARY, LIVE, SOURCES }

/** A ROM waiting on the user to say which emulator should run it. */
data class EmulatorPrompt(val rom: MediaItem, val system: GameSystem)

/** What the player was handed — always a real file on this device. */
data class PlaybackTarget(
    val title: String,
    val sourceLabel: String,
    val uri: String,
    val durationMs: Long,
    val startPercent: Int,
    val itemId: String?,
)

/** Single source of truth: the library is whatever is on the phone right now. */
class AppState(private val context: Context) {

    private val store = VaultStore(context)
    private val audio = AudioController(context)
    private val boxArt = BoxArt(context)

    var tab by mutableStateOf(Tab.HOME)
    var category by mutableStateOf(Category.MOVIES)
    var detail by mutableStateOf<MediaItem?>(null)
    var season by mutableStateOf(0)
    var player by mutableStateOf<PlaybackTarget?>(null)
    var nowPlaying by mutableStateOf<Track?>(null)
    var trackPlaying by mutableStateOf(true)
    var toast by mutableStateOf<String?>(null)
    var scanning by mutableStateOf(false)
    var scannedOnce by mutableStateOf(false)

    var hasMediaAccess by mutableStateOf(MediaAccess.hasMediaAccess(context))
        private set
    var hasAllFilesAccess by mutableStateOf(MediaAccess.hasAllFilesAccess())
        private set

    val folders = mutableStateListOf<WatchedFolder>()
    /** Folders that define what Movies and TV mean on this phone. */
    val libraryFolders = mutableStateListOf<LibraryFolder>()
    /** Folder path → rule, for folders sorted by hand in Sources. */
    var folderRules by mutableStateOf(emptyMap<String, String>())
        private set
    val users = mutableStateListOf<VaultUser>()
    var connected by mutableStateOf(emptySet<String>())
    var accentIndex by mutableStateOf(0)
    var library by mutableStateOf(ScanResult())
    var installedGames by mutableStateOf(emptyList<MediaItem>())
    var emulators by mutableStateOf(emptyMap<String, String>())
        private set
    /** Set when a ROM is tapped and more than one emulator could run it. */
    var emulatorPrompt by mutableStateOf<EmulatorPrompt?>(null)
    /** Set when the user asks to give a ROM its own cover. */
    var artTarget by mutableStateOf<MediaItem?>(null)
    var fetchingArt by mutableStateOf(false)
    private var artOverrides by mutableStateOf(emptyMap<String, String>())
    private var progress by mutableStateOf(emptyMap<String, Int>())

    val accent: Color get() = Mv.AccentChoices[accentIndex.coerceIn(0, Mv.AccentChoices.lastIndex)]

    val multiUser: Boolean get() = users.size > 1

    init {
        folders.addAll(store.loadFolders())
        users.addAll(store.loadUsers())
        connected = store.loadConnected()
        accentIndex = store.loadAccentIndex()
        progress = store.loadProgress()
        emulators = store.loadEmulators()
        artOverrides = boxArt.overrides()
        libraryFolders.addAll(store.loadLibraryFolders())
        folderRules = store.loadFolderRules()
    }

    // ---- the library -----------------------------------------------------

    /** ROMs first — they are the games; installed game apps come after. */
    val roms: List<MediaItem>
        get() = library.media
            .filter { it.category == Category.GAMES }
            .map { rom -> artOverrides[rom.id]?.let { rom.copy(artUri = it) } ?: rom }
    val games: List<MediaItem> get() = roms + installedGames

    val romSystems: List<GameSystem>
        get() = Emulators.systems.filter { system -> roms.any { it.systemId == system.id } }

    fun romsFor(system: GameSystem): List<MediaItem> = roms.filter { it.systemId == system.id }
    val movies: List<MediaItem>
        get() = library.media.filter { it.category == Category.MOVIES }.sortedByDescending { it.addedAt }
    val shows: List<MediaItem>
        get() = library.media.filter { it.category == Category.TV }.sortedByDescending { it.addedAt }
    val tracks: List<Track> get() = library.tracks
    val documents: List<DocFile> get() = library.documents

    fun itemsFor(category: Category): List<MediaItem> = when (category) {
        Category.GAMES -> games
        Category.MOVIES -> movies
        Category.TV -> shows
        else -> emptyList()
    }

    fun countFor(category: Category): Int = when (category) {
        Category.MUSIC -> tracks.size
        Category.FILES -> documents.size
        else -> itemsFor(category).size
    }

    val isEmpty: Boolean get() = library.total == 0 && installedGames.isEmpty()

    fun progressOf(item: MediaItem): Int = progress[item.id] ?: 0

    fun setProgress(id: String, percent: Int) {
        progress = progress + (id to percent.coerceIn(0, 100))
        store.saveProgress(progress)
    }

    /** Anything left part-way through, newest first; otherwise the newest video on the phone. */
    val continueWatching: MediaItem?
        get() = (movies + shows).firstOrNull { progressOf(it) in 1..99 }
            ?: (movies + shows).maxByOrNull { it.addedAt }

    // ---- navigation ------------------------------------------------------

    fun openDetail(item: MediaItem) {
        detail = item
        season = 0
    }

    fun closeDetail() {
        detail = null
    }

    fun play(item: MediaItem, episodeIndex: Int = 0) {
        val episode = item.episodes.getOrNull(episodeIndex)
        val uri = episode?.uri ?: item.uri ?: run {
            showToast("No playable file for ${item.title}")
            return
        }
        player = PlaybackTarget(
            title = if (episode != null) "${item.title} · S${episode.season} E${episode.number}" else item.title,
            sourceLabel = if (item.source == "Watched folder") "Playing from a watched folder"
            else "Playing from ${item.folder.ifBlank { "this phone" }}",
            uri = uri,
            durationMs = episode?.durationMs ?: item.durationMs,
            startPercent = progressOf(item),
            itemId = item.id,
        )
    }

    /** Hands a ROM to its emulator, asking which one only when the choice is genuinely open. */
    fun launchRom(context: Context, rom: MediaItem) {
        when (val result = RomLauncher.launch(context, rom, emulators[rom.systemId])) {
            is RomLauncher.Result.Launched -> showToast("▶ ${rom.title}")
            is RomLauncher.Result.NeedsChoice -> emulatorPrompt = EmulatorPrompt(rom, result.system)
            is RomLauncher.Result.Failed -> showToast(result.reason)
        }
    }

    fun chooseEmulator(context: Context, system: GameSystem, packageName: String, remember: Boolean) {
        if (remember) setEmulator(system.id, packageName)
        val rom = emulatorPrompt?.rom
        emulatorPrompt = null
        if (rom != null && !RomLauncher.launchWith(context, rom, system, packageName)) {
            showToast("Couldn't hand ${rom.title} to that emulator")
        }
    }

    fun setEmulator(systemId: String, packageName: String?) {
        emulators = if (packageName == null) emulators - systemId else emulators + (systemId to packageName)
        store.saveEmulators(emulators)
    }

    fun dismissEmulatorPrompt() {
        emulatorPrompt = null
    }

    // ---- box art ---------------------------------------------------------

    val romsMissingArt: List<MediaItem> get() = roms.filter { it.artUri == null }

    fun askForArt(rom: MediaItem) {
        artTarget = rom
    }

    fun dismissArtTarget() {
        artTarget = null
    }

    /** Uses an image the user picked out of their own storage. */
    fun importArt(rom: MediaItem, source: Uri) {
        if (boxArt.importFrom(rom.id, source)) {
            artOverrides = boxArt.overrides()
            showToast("Cover set for ${rom.title}")
        } else {
            showToast("Couldn't read that image")
        }
        artTarget = null
    }

    fun clearArt(rom: MediaItem) {
        boxArt.clear(rom.id)
        artOverrides = boxArt.overrides()
        artTarget = null
    }

    /** Fetches one cover from the libretro archive. Network, so always user-initiated. */
    suspend fun downloadArt(rom: MediaItem) {
        val system = Emulators.systems.firstOrNull { it.id == rom.systemId } ?: return
        artTarget = null
        fetchingArt = true
        try {
            val found = withContext(Dispatchers.IO) { boxArt.download(rom, system) }
            artOverrides = boxArt.overrides()
            showToast(
                if (found) "Cover found for ${rom.title}"
                else "No cover in the archive for ${rom.title}"
            )
        } finally {
            fetchingArt = false
        }
    }

    /** Fills in every ROM that still has no cover. */
    suspend fun downloadMissingArt() {
        val targets = romsMissingArt
        if (targets.isEmpty()) {
            showToast("Every ROM already has a cover")
            return
        }
        fetchingArt = true
        showToast("Looking up ${targets.size} covers…")
        try {
            val found = withContext(Dispatchers.IO) {
                targets.count { rom ->
                    val system = Emulators.systems.firstOrNull { it.id == rom.systemId }
                    system != null && boxArt.download(rom, system)
                }
            }
            artOverrides = boxArt.overrides()
            showToast("Found $found of ${targets.size} covers")
        } finally {
            fetchingArt = false
        }
    }

    fun closePlayer(atPercent: Int) {
        player?.itemId?.let { setProgress(it, atPercent) }
        player = null
    }

    fun showToast(message: String) {
        toast = message
    }

    fun clearToast() {
        toast = null
    }

    // ---- music -----------------------------------------------------------

    fun playTrack(track: Track) {
        nowPlaying = track
        trackPlaying = true
        val started = audio.play(track.uri)
        if (!started) showToast("Couldn't play ${track.title}")
    }

    fun toggleTrack() {
        trackPlaying = !trackPlaying
        audio.setPlaying(trackPlaying)
    }

    fun stopTrack() {
        audio.release()
        nowPlaying = null
    }

    // ---- access & scanning ----------------------------------------------

    fun refreshAccess() {
        hasMediaAccess = MediaAccess.hasMediaAccess(context)
        hasAllFilesAccess = MediaAccess.hasAllFilesAccess()
    }

    /** Re-reads the phone: MediaStore, then any hand-picked folders, then installed games. */
    suspend fun rescan() {
        refreshAccess()
        scanning = true
        try {
            val folderSnapshot = folders.toList()
            val filters = videoFilters()
            val libraries = libraryFolders.toList()
            val result = withContext(Dispatchers.IO) {
                val device = if (hasMediaAccess) DeviceMedia.scan(context, filters) else ScanResult()
                val perFolder = folderSnapshot.associate { it.uri to LocalScanner.scan(context, it.uri) }
                // Designated folders are scanned directly too, so files MediaStore has not
                // indexed still land in the right category.
                val designated = libraries.map { LocalScanner.scan(context, it.uri, it.category) }
                Triple(device, perFolder, designated)
            }
            val (device, perFolder, designated) = result
            library = (perFolder.values + designated)
                .fold(device) { acc, next -> acc + next }
                .deduped()

            folderSnapshot.forEachIndexed { index, folder ->
                val scan = perFolder[folder.uri] ?: ScanResult()
                if (index < folders.size) {
                    folders[index] = folder.copy(meta = "${scan.total} items · ${summarize(scan)}")
                }
            }
            store.saveFolders(folders.toList())

            installedGames = withContext(Dispatchers.IO) { InstalledApps.games(context) }
        } finally {
            scanning = false
            scannedOnce = true
        }
    }

    private fun videoFilters() = VideoFilters(
        movies = libraryFolders.filter { it.category == Category.MOVIES }.map { it.path },
        tv = libraryFolders.filter { it.category == Category.TV }.map { it.path },
        music = libraryFolders.filter { it.category == Category.MUSIC }.map { it.path },
        rules = folderRules,
    )

    /** Folders holding video and music, busiest first — what Sources lists for sorting. */
    val videoFolders: List<MediaFolder> get() = library.videoFolders
    val audioFolders: List<MediaFolder> get() = library.audioFolders

    /** True once at least one folder feeds this category. */
    /** No folder feeds Movies, TV or Music yet — the library cannot have anything in it. */
    val hasNoSources: Boolean
        get() = !hasSourceFor(Category.MOVIES) && !hasSourceFor(Category.TV) &&
            !hasSourceFor(Category.MUSIC) && folders.isEmpty()

    fun hasSourceFor(category: Category): Boolean =
        folderRules.values.any { runCatching { FolderRule.valueOf(it) }.getOrNull()?.category == category } ||
            libraryFolders.any { it.category == category }

    fun ruleFor(folder: MediaFolder): FolderRule = videoFilters().ruleFor(folder.path)

    /** Assigns a folder to a category, or takes it back out of the library. */
    suspend fun setFolderRule(folder: MediaFolder, rule: FolderRule) {
        folderRules = if (rule == FolderRule.NONE) folderRules - folder.path
        else folderRules + (folder.path to rule.name)
        store.saveFolderRules(folderRules)
        rescan()
    }

    fun libraryFoldersFor(category: Category): List<LibraryFolder> =
        libraryFolders.filter { it.category == category }

    /** Pins a category to a folder: from now on only files inside it are listed there. */
    suspend fun addLibraryFolder(uri: Uri, category: Category) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        val path = documentPath(uri)
        if (libraryFolders.none { it.uri == uri.toString() && it.category == category }) {
            libraryFolders.add(LibraryFolder(uri.toString(), path, category))
            store.saveLibraryFolders(libraryFolders.toList())
        }
        showToast("${category.label} now comes from /$path")
        rescan()
    }

    suspend fun removeLibraryFolder(folder: LibraryFolder) {
        libraryFolders.remove(folder)
        store.saveLibraryFolders(libraryFolders.toList())
        rescan()
    }

    /** "primary:Movies/Films" from a picked tree uri becomes "Movies/Films". */
    private fun documentPath(uri: Uri): String {
        val docId = runCatching { android.provider.DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: uri.lastPathSegment.orEmpty()
        return docId.substringAfter(':').trim('/')
    }

    private fun summarize(result: ScanResult): String {
        val parts = buildList {
            if (result.media.any { it.category == Category.MOVIES }) add("video")
            if (result.media.any { it.category == Category.TV }) add("tv")
            if (result.tracks.isNotEmpty()) add("music")
            if (result.documents.isNotEmpty()) add("files")
        }
        return if (parts.isEmpty()) "nothing found" else parts.joinToString(", ")
    }

    // ---- sources ---------------------------------------------------------

    suspend fun addFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        val path = readablePath(uri)
        if (folders.none { it.uri == uri.toString() }) {
            folders.add(WatchedFolder(uri.toString(), path, "scanning…"))
            store.saveFolders(folders.toList())
        }
        showToast("📁 Watching $path")
        rescan()
    }

    fun removeFolder(folder: WatchedFolder) {
        folders.remove(folder)
        store.saveFolders(folders.toList())
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(folder.uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        showToast("✕ Stopped watching ${folder.path}")
    }

    private fun readablePath(uri: Uri): String {
        val raw = uri.lastPathSegment ?: uri.toString()
        val tail = raw.substringAfterLast(':')
        return "/" + tail.trim('/')
    }

    // ---- users & services -----------------------------------------------

    fun addUser() {
        val names = listOf("Sam", "Alex", "Kim", "Jordan", "Riley")
        val name = names.firstOrNull { candidate -> users.none { it.name == candidate } } ?: "Guest ${users.size}"
        users.add(VaultUser(name, "Member"))
        store.saveUsers(users.toList())
        showToast("✓ Added $name — profile switcher enabled")
    }

    fun removeUser(user: VaultUser) {
        if (user.role == "Owner") return
        users.remove(user)
        store.saveUsers(users.toList())
        showToast("✕ Removed ${user.name}")
    }

    fun toggleService(name: String) {
        val isOn = name in connected
        connected = if (isOn) connected - name else connected + name
        store.saveConnected(connected)
    }

    fun setAccent(index: Int) {
        accentIndex = index
        store.saveAccentIndex(index)
    }
}
