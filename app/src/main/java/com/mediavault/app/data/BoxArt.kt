package com.mediavault.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Cover art for ROMs that have none sitting next to them.
 *
 * Two routes, both explicit: the user picks an image from the phone, or MediaVault fetches
 * one from the public libretro thumbnail archive — the same set RetroArch ships against,
 * matched on the ROM's filename. Downloads only ever happen when asked for.
 */
class BoxArt(context: Context) {

    private val appContext = context.applicationContext
    private val store = VaultStore(appContext)
    private val directory = File(appContext.filesDir, "boxart").apply { mkdirs() }

    /** romId → absolute path of the stored image. */
    private var stored: MutableMap<String, String> = store.loadBoxArt().toMutableMap()

    fun artFor(romId: String): String? {
        val path = stored[romId] ?: return null
        val file = File(path)
        if (!file.exists()) {
            stored.remove(romId)
            store.saveBoxArt(stored)
            return null
        }
        return Uri.fromFile(file).toString()
    }

    fun overrides(): Map<String, String> = stored.mapValues { Uri.fromFile(File(it.value)).toString() }

    fun clear(romId: String) {
        stored.remove(romId)?.let { File(it).delete() }
        store.saveBoxArt(stored)
    }

    /** Copies an image the user picked into the vault's own storage. */
    fun importFrom(romId: String, source: Uri): Boolean = runCatching {
        val target = File(directory, "${romId.hashCode().toUInt()}.img")
        appContext.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: return false
        remember(romId, target)
        true
    }.getOrDefault(false)

    /**
     * Looks the ROM up in the libretro archive. Tries the exact filename first, then the
     * name with its region and dump tags stripped, then the usual regional variants —
     * which is how the archive names things.
     */
    fun download(rom: MediaItem, system: GameSystem): Boolean {
        val folders = libretroFolders(system, rom)
        if (folders.isEmpty()) return false
        val base = (rom.fileName ?: rom.title).substringBeforeLast('.')

        for (folder in folders) {
            for (kind in THUMBNAIL_KINDS) {
                for (candidate in candidates(base)) {
                    val url = buildUrl(folder, kind, candidate)
                    val bytes = fetch(url) ?: continue
                    val target = File(directory, "${rom.id.hashCode().toUInt()}.img")
                    runCatching { FileOutputStream(target).use { it.write(bytes) } }
                        .onFailure { return false }
                    remember(rom.id, target)
                    return true
                }
            }
        }
        return false
    }

    private fun remember(romId: String, file: File) {
        stored[romId] = file.absolutePath
        store.saveBoxArt(stored)
    }

    private fun candidates(base: String): List<String> {
        val stripped = base.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "").trim()
        val regionals = listOf("USA", "World", "Europe", "Japan", "USA, Europe")
        return buildList {
            add(base)
            if (stripped.isNotBlank() && stripped != base) add(stripped)
            val root = stripped.ifBlank { base }
            regionals.forEach { add("$root ($it)") }
        }.distinct()
    }

    /** The archive replaces characters that are awkward in filenames with an underscore. */
    private fun libretroName(name: String): String =
        name.replace(Regex("""[&*/:`<>?\\|"]"""), "_").trim()

    private fun buildUrl(folder: String, kind: String, name: String): String {
        val encodedFolder = Uri.encode(folder)
        val encodedName = Uri.encode(libretroName(name) + ".png")
        return "$BASE/$encodedFolder/$kind/$encodedName"
    }

    private fun fetch(url: String): ByteArray? = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MediaVault")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    /**
     * A few of MediaVault's systems cover two consoles the archive keeps apart, so the
     * ROM's own extension decides which folder to try first.
     */
    private fun libretroFolders(system: GameSystem, rom: MediaItem): List<String> {
        val extension = (rom.fileName ?: rom.uri.orEmpty())
            .substringAfterLast('.', "")
            .lowercase(Locale.US)
        return when (system.id) {
            "nes" -> listOf("Nintendo - Nintendo Entertainment System")
            "snes" -> listOf("Nintendo - Super Nintendo Entertainment System")
            "gb" -> if (extension == "gbc") {
                listOf("Nintendo - Game Boy Color", "Nintendo - Game Boy")
            } else {
                listOf("Nintendo - Game Boy", "Nintendo - Game Boy Color")
            }
            "gba" -> listOf("Nintendo - Game Boy Advance")
            "n64" -> listOf("Nintendo - Nintendo 64")
            "nds" -> listOf("Nintendo - Nintendo DS")
            "3ds" -> listOf("Nintendo - Nintendo 3DS")
            "gamecube" -> if (extension == "wbfs" || extension == "wad") {
                listOf("Nintendo - Wii", "Nintendo - GameCube")
            } else {
                listOf("Nintendo - GameCube", "Nintendo - Wii")
            }
            "psp" -> listOf("Sony - PlayStation Portable")
            "ps1" -> listOf("Sony - PlayStation")
            "ps2" -> listOf("Sony - PlayStation 2")
            "genesis" -> listOf("Sega - Mega Drive - Genesis", "Sega - 32X")
            "mastersystem" -> if (extension == "gg") {
                listOf("Sega - Game Gear", "Sega - Master System - Mark III")
            } else {
                listOf("Sega - Master System - Mark III", "Sega - Game Gear")
            }
            "dreamcast" -> listOf("Sega - Dreamcast")
            "pce" -> listOf("NEC - PC Engine - TurboGrafx 16")
            "atari" -> listOf("Atari - 2600", "Atari - 7800", "Atari - Lynx")
            "arcade" -> listOf("MAME")
            // The archive has no Switch set.
            else -> emptyList()
        }
    }

    private companion object {
        const val BASE = "https://thumbnails.libretro.com"
        val THUMBNAIL_KINDS = listOf("Named_Boxarts", "Named_Titles", "Named_Snaps")
    }
}
