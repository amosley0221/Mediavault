package com.mediavault.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.mediavault.app.data.Category
import com.mediavault.app.data.DemoData
import com.mediavault.app.data.DocFile
import com.mediavault.app.data.InstalledApps
import com.mediavault.app.data.LocalScanner
import com.mediavault.app.data.MediaItem
import com.mediavault.app.data.ScanResult
import com.mediavault.app.data.Track
import com.mediavault.app.data.VaultStore
import com.mediavault.app.data.VaultUser
import com.mediavault.app.data.WatchedFolder
import com.mediavault.app.media.AudioController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Tab { HOME, LIBRARY, LIVE, SOURCES }

/** What the in-app player was handed: a real file, or a design-catalogue title. */
data class PlaybackTarget(
    val title: String,
    val sourceLabel: String,
    val uri: String?,
    val length: String,
    val startPercent: Int,
    val itemId: String?,
)

/** Single source of truth for the whole app — screens read it, taps mutate it. */
class AppState(private val context: Context) {

    private val store = VaultStore(context)
    private val audio = AudioController(context)

    var tab by mutableStateOf(Tab.HOME)
    var category by mutableStateOf(Category.MOVIES)
    var detail by mutableStateOf<MediaItem?>(null)
    var season by mutableStateOf(0)
    var player by mutableStateOf<PlaybackTarget?>(null)
    var nowPlaying by mutableStateOf<Track?>(null)
    var trackPlaying by mutableStateOf(true)
    var toast by mutableStateOf<String?>(null)
    var scanning by mutableStateOf(false)

    val folders = mutableStateListOf<WatchedFolder>()
    val users = mutableStateListOf<VaultUser>()
    var connected by mutableStateOf(emptySet<String>())
    var accentIndex by mutableStateOf(0)
    var scanned by mutableStateOf(ScanResult())
    var installedGames by mutableStateOf(emptyList<MediaItem>())
    private var progress by mutableStateOf(emptyMap<String, Int>())

    val accent: Color get() = Mv.AccentChoices[accentIndex.coerceIn(0, Mv.AccentChoices.lastIndex)]

    val multiUser: Boolean get() = users.size > 1

    init {
        folders.addAll(store.loadFolders())
        users.addAll(store.loadUsers())
        connected = store.loadConnected()
        accentIndex = store.loadAccentIndex()
        scanned = store.loadScanned()
        progress = store.loadProgress()
    }

    // ---- library ---------------------------------------------------------

    val games: List<MediaItem> get() = installedGames + DemoData.games
    val movies: List<MediaItem> get() = scanned.media.filter { it.category == Category.MOVIES } + DemoData.movies
    val shows: List<MediaItem> get() = scanned.media.filter { it.category == Category.TV } + DemoData.shows
    val tracks: List<Track> get() = scanned.tracks + DemoData.music
    val documents: List<DocFile> get() = scanned.documents + DemoData.files

    fun itemsFor(category: Category): List<MediaItem> = when (category) {
        Category.GAMES -> games
        Category.MOVIES -> movies
        Category.TV -> shows
        else -> emptyList()
    }

    fun progressOf(item: MediaItem): Int = progress[item.id] ?: item.progress

    fun setProgress(id: String, percent: Int) {
        progress = progress + (id to percent.coerceIn(0, 100))
        store.saveProgress(progress)
    }

    /** The Continue-watching hero: whatever was left part-way through. */
    val continueWatching: MediaItem
        get() = (shows + movies).firstOrNull { progressOf(it) in 1..99 } ?: DemoData.shows.first()

    // ---- navigation ------------------------------------------------------

    fun openDetail(item: MediaItem) {
        detail = item
        season = 0
    }

    fun closeDetail() {
        detail = null
    }

    fun openPlayer(target: PlaybackTarget) {
        player = target
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
        showToast(if (started) "♪ Playing “${track.title}”" else "♪ ${track.title} — queued")
    }

    fun toggleTrack() {
        trackPlaying = !trackPlaying
        audio.setPlaying(trackPlaying)
    }

    fun stopTrack() {
        audio.release()
        nowPlaying = null
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

    /** Re-reads every watched folder plus the installed-games list. */
    suspend fun rescan() {
        scanning = true
        try {
            val snapshot = folders.toList()
            val perFolder = withContext(Dispatchers.IO) {
                snapshot.associate { folder -> folder.uri to LocalScanner.scan(context, folder.uri) }
            }
            val merged = perFolder.values.fold(ScanResult()) { acc, next -> acc + next }
            scanned = merged
            store.saveScanned(merged)

            snapshot.forEachIndexed { index, folder ->
                val result = perFolder[folder.uri] ?: ScanResult()
                folders[index] = folder.copy(meta = "${result.total} items · ${summarize(result)}")
            }
            store.saveFolders(folders.toList())

            installedGames = withContext(Dispatchers.IO) { InstalledApps.games(context) }
        } finally {
            scanning = false
        }
    }

    private fun summarize(result: ScanResult): String {
        val parts = buildList {
            if (result.media.any { it.category == Category.MOVIES }) add("movies")
            if (result.media.any { it.category == Category.TV }) add("tv")
            if (result.tracks.isNotEmpty()) add("music")
            if (result.documents.isNotEmpty()) add("files")
        }
        return if (parts.isEmpty()) "nothing found" else parts.joinToString(", ")
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
        showToast(if (isOn) "✕ Disconnected $name" else "✓ Connected $name")
    }

    fun setAccent(index: Int) {
        accentIndex = index
        store.saveAccentIndex(index)
    }
}
