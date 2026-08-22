package com.mediavault.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything the vault remembers between launches: watched folders, the scan cache,
 * users, connected services, accent choice and watch progress. Plain SharedPreferences
 * with JSON payloads — no server, nothing leaves the device.
 */
class VaultStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("mediavault", Context.MODE_PRIVATE)

    // ---- watched folders -------------------------------------------------

    fun loadFolders(): List<WatchedFolder> = readArray(KEY_FOLDERS).map {
        WatchedFolder(it.optString("uri"), it.optString("path"), it.optString("meta"))
    }

    fun saveFolders(folders: List<WatchedFolder>) = writeArray(KEY_FOLDERS, folders) {
        JSONObject().put("uri", it.uri).put("path", it.path).put("meta", it.meta)
    }

    // ---- scan cache ------------------------------------------------------

    fun loadScanned(): ScanResult {
        val media = readArray(KEY_SCAN_MEDIA).mapNotNull { o ->
            val category = runCatching { Category.valueOf(o.optString("cat")) }.getOrNull() ?: return@mapNotNull null
            MediaItem(
                id = o.optString("id"),
                title = o.optString("title"),
                sub = o.optString("sub"),
                letter = o.optString("letter"),
                gradient = o.optLong("g1") to o.optLong("g2"),
                tag = o.optString("tag").ifBlank { null },
                source = o.optString("source"),
                category = category,
                length = o.optString("length"),
                progress = o.optInt("progress"),
                uri = o.optString("uri").ifBlank { null },
            )
        }
        val tracks = readArray(KEY_SCAN_TRACKS).map { o ->
            Track(o.optString("id"), o.optString("title"), o.optString("sub"), o.optString("duration"), o.optString("uri").ifBlank { null })
        }
        val docs = readArray(KEY_SCAN_DOCS).map { o ->
            DocFile(
                id = o.optString("id"),
                name = o.optString("name"),
                sub = o.optString("sub"),
                badge = o.optString("badge"),
                app = o.optString("app"),
                background = o.optLong("bg"),
                foreground = o.optLong("fg"),
                uri = o.optString("uri").ifBlank { null },
                mime = o.optString("mime").ifBlank { null },
            )
        }
        return ScanResult(media, tracks, docs)
    }

    fun saveScanned(result: ScanResult) {
        writeArray(KEY_SCAN_MEDIA, result.media) {
            JSONObject()
                .put("id", it.id).put("title", it.title).put("sub", it.sub).put("letter", it.letter)
                .put("g1", it.gradient.first).put("g2", it.gradient.second)
                .put("tag", it.tag ?: "").put("source", it.source).put("cat", it.category.name)
                .put("length", it.length).put("progress", it.progress).put("uri", it.uri ?: "")
        }
        writeArray(KEY_SCAN_TRACKS, result.tracks) {
            JSONObject().put("id", it.id).put("title", it.title).put("sub", it.sub)
                .put("duration", it.duration).put("uri", it.uri ?: "")
        }
        writeArray(KEY_SCAN_DOCS, result.documents) {
            JSONObject().put("id", it.id).put("name", it.name).put("sub", it.sub).put("badge", it.badge)
                .put("app", it.app).put("bg", it.background).put("fg", it.foreground)
                .put("uri", it.uri ?: "").put("mime", it.mime ?: "")
        }
    }

    // ---- users -----------------------------------------------------------

    fun loadUsers(): List<VaultUser> {
        val stored = readArray(KEY_USERS).map { VaultUser(it.optString("name"), it.optString("role")) }
        return stored.ifEmpty { listOf(VaultUser("You", "Owner")) }
    }

    fun saveUsers(users: List<VaultUser>) = writeArray(KEY_USERS, users) {
        JSONObject().put("name", it.name).put("role", it.role)
    }

    // ---- connected services ---------------------------------------------

    fun loadConnected(): Set<String> {
        val raw = prefs.getString(KEY_CONNECTED, null) ?: return setOf("Plex", "Twitch", "YouTube")
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun saveConnected(connected: Set<String>) {
        prefs.edit().putString(KEY_CONNECTED, JSONArray(connected.toList()).toString()).apply()
    }

    // ---- accent ----------------------------------------------------------

    fun loadAccentIndex(): Int = prefs.getInt(KEY_ACCENT, 0)

    fun saveAccentIndex(index: Int) = prefs.edit().putInt(KEY_ACCENT, index).apply()

    // ---- watch progress --------------------------------------------------

    fun loadProgress(): Map<String, Int> {
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return emptyMap()
        val obj = JSONObject(raw)
        return obj.keys().asSequence().associateWith { obj.optInt(it) }
    }

    fun saveProgress(progress: Map<String, Int>) {
        val obj = JSONObject()
        progress.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_PROGRESS, obj.toString()).apply()
    }

    // ---- helpers ---------------------------------------------------------

    private fun readArray(key: String): List<JSONObject> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        }.getOrDefault(emptyList())
    }

    private fun <T> writeArray(key: String, items: List<T>, encode: (T) -> JSONObject) {
        val arr = JSONArray()
        items.forEach { arr.put(encode(it)) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private companion object {
        const val KEY_FOLDERS = "folders"
        const val KEY_SCAN_MEDIA = "scan_media"
        const val KEY_SCAN_TRACKS = "scan_tracks"
        const val KEY_SCAN_DOCS = "scan_docs"
        const val KEY_USERS = "users"
        const val KEY_CONNECTED = "connected"
        const val KEY_ACCENT = "accent"
        const val KEY_PROGRESS = "progress"
    }
}
