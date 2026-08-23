package com.mediavault.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The small amount of state that outlives a scan: watched folders, users, connected
 * services, accent choice and watch progress. The library itself is re-read from the
 * device each launch, so nothing about your files is cached here.
 */
class VaultStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("mediavault", Context.MODE_PRIVATE)

    fun loadFolders(): List<WatchedFolder> = readArray(KEY_FOLDERS).map {
        WatchedFolder(it.optString("uri"), it.optString("path"), it.optString("meta"))
    }

    fun saveFolders(folders: List<WatchedFolder>) = writeArray(KEY_FOLDERS, folders) {
        JSONObject().put("uri", it.uri).put("path", it.path).put("meta", it.meta)
    }

    fun loadUsers(): List<VaultUser> {
        val stored = readArray(KEY_USERS).map { VaultUser(it.optString("name"), it.optString("role")) }
        return stored.ifEmpty { listOf(VaultUser("You", "Owner")) }
    }

    fun saveUsers(users: List<VaultUser>) = writeArray(KEY_USERS, users) {
        JSONObject().put("name", it.name).put("role", it.role)
    }

    fun loadConnected(): Set<String> {
        val raw = prefs.getString(KEY_CONNECTED, null) ?: return emptySet()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun saveConnected(connected: Set<String>) {
        prefs.edit().putString(KEY_CONNECTED, JSONArray(connected.toList()).toString()).apply()
    }

    /** Which emulator package runs each console, keyed by [GameSystem.id]. */
    fun loadEmulators(): Map<String, String> {
        val raw = prefs.getString(KEY_EMULATORS, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        }.getOrDefault(emptyMap())
    }

    fun saveEmulators(mapping: Map<String, String>) {
        val obj = JSONObject()
        mapping.forEach { (system, packageName) -> obj.put(system, packageName) }
        prefs.edit().putString(KEY_EMULATORS, obj.toString()).apply()
    }

    /** Folders that define what counts as Movies and what counts as TV. */
    fun loadLibraryFolders(): List<LibraryFolder> = readArray(KEY_LIBRARY_FOLDERS).mapNotNull {
        val category = runCatching { Category.valueOf(it.optString("category")) }.getOrNull()
            ?: return@mapNotNull null
        LibraryFolder(it.optString("uri"), it.optString("path"), category)
    }

    fun saveLibraryFolders(folders: List<LibraryFolder>) = writeArray(KEY_LIBRARY_FOLDERS, folders) {
        JSONObject().put("uri", it.uri).put("path", it.path).put("category", it.category.name)
    }

    /** Folder path → [FolderRule] name, for folders the user sorted by hand. */
    fun loadFolderRules(): Map<String, String> {
        val raw = prefs.getString(KEY_FOLDER_RULES, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        }.getOrDefault(emptyMap())
    }

    fun saveFolderRules(rules: Map<String, String>) {
        val obj = JSONObject()
        rules.forEach { (path, rule) -> obj.put(path, rule) }
        prefs.edit().putString(KEY_FOLDER_RULES, obj.toString()).apply()
    }

    /** Games starred by the user, by item id. */
    fun loadFavorites(): Set<String> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        }.getOrDefault(emptySet())
    }

    fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putString(KEY_FAVORITES, JSONArray(favorites.toList()).toString()).apply()
    }

    /** Apps the user added by hand because the scanner did not call them games. */
    fun loadManualGames(): Set<String> {
        val raw = prefs.getString(KEY_MANUAL_GAMES, null) ?: return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        }.getOrDefault(emptySet())
    }

    fun saveManualGames(packages: Set<String>) {
        prefs.edit().putString(KEY_MANUAL_GAMES, JSONArray(packages.toList()).toString()).apply()
    }

    /** Game sources the user switched off, by source id. */
    fun loadDisabledSources(): Set<String> {
        val raw = prefs.getString(KEY_DISABLED_SOURCES, null) ?: return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        }.getOrDefault(emptySet())
    }

    fun saveDisabledSources(sources: Set<String>) {
        prefs.edit().putString(KEY_DISABLED_SOURCES, JSONArray(sources.toList()).toString()).apply()
    }

    /** romId → absolute path of an imported or downloaded cover. */
    fun loadBoxArt(): Map<String, String> {
        val raw = prefs.getString(KEY_BOXART, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        }.getOrDefault(emptyMap())
    }

    fun saveBoxArt(art: Map<String, String>) {
        val obj = JSONObject()
        art.forEach { (romId, path) -> obj.put(romId, path) }
        prefs.edit().putString(KEY_BOXART, obj.toString()).apply()
    }

    fun loadAccentIndex(): Int = prefs.getInt(KEY_ACCENT, 0)

    fun saveAccentIndex(index: Int) = prefs.edit().putInt(KEY_ACCENT, index).apply()

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
        const val KEY_USERS = "users"
        const val KEY_CONNECTED = "connected"
        const val KEY_EMULATORS = "emulators"
        const val KEY_BOXART = "boxart"
        const val KEY_LIBRARY_FOLDERS = "library_folders"
        const val KEY_FOLDER_RULES = "folder_rules"
        const val KEY_FAVORITES = "favorites"
        const val KEY_MANUAL_GAMES = "manual_games"
        const val KEY_DISABLED_SOURCES = "disabled_sources"
        const val KEY_ACCENT = "accent"
        const val KEY_PROGRESS = "progress"
    }
}
