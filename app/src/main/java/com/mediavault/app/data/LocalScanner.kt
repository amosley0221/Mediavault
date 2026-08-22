package com.mediavault.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.Locale
import kotlin.math.abs

data class ScanResult(
    val media: List<MediaItem> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val documents: List<DocFile> = emptyList(),
) {
    val total: Int get() = media.size + tracks.size + documents.size

    operator fun plus(other: ScanResult) = ScanResult(
        media + other.media,
        tracks + other.tracks,
        documents + other.documents,
    )
}

/**
 * Walks the folders the user granted through the storage picker and turns filenames into
 * library entries: `Show.S02E07.mkv` becomes a TV episode, `Title (2019).mkv` a movie,
 * audio becomes tracks, everything else becomes a file row that opens in its native app.
 */
object LocalScanner {

    private val VIDEO = setOf("mkv", "mp4", "avi", "mov", "m4v", "webm", "ts", "wmv", "mpg", "mpeg")
    private val AUDIO = setOf("mp3", "flac", "m4a", "wav", "ogg", "aac", "opus", "wma")

    private val EPISODE = Regex("""(?i)^(.*?)[._\s-]+s(\d{1,2})[._\s-]?e(\d{1,3})""")
    private val YEAR = Regex("""^(.*?)[._\s(\[]+((19|20)\d{2})""")

    private const val MAX_DEPTH = 4
    private const val MAX_ITEMS = 1500

    fun scan(context: Context, folderUri: String): ScanResult {
        val root = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) }.getOrNull()
            ?: return ScanResult()
        val media = mutableListOf<MediaItem>()
        val tracks = mutableListOf<Track>()
        val docs = mutableListOf<DocFile>()
        walk(root, 0, media, tracks, docs)
        // One row per show, not per episode.
        val collapsed = media
            .groupBy { it.title.lowercase(Locale.US) to it.category }
            .map { (_, group) -> group.first().copy(sub = subFor(group)) }
        return ScanResult(collapsed, tracks, docs)
    }

    private fun subFor(group: List<MediaItem>): String =
        if (group.size > 1) "${group.size} episodes · Local" else group.first().sub

    private fun walk(
        dir: DocumentFile,
        depth: Int,
        media: MutableList<MediaItem>,
        tracks: MutableList<Track>,
        docs: MutableList<DocFile>,
    ) {
        if (depth > MAX_DEPTH) return
        if (media.size + tracks.size + docs.size > MAX_ITEMS) return
        val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())
        for (child in children) {
            if (child.isDirectory) {
                walk(child, depth + 1, media, tracks, docs)
                continue
            }
            val name = child.name ?: continue
            if (name.startsWith(".")) continue
            val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
            val uri = child.uri.toString()
            when {
                ext in VIDEO -> media += videoItem(name, ext, uri, dir.name.orEmpty())
                ext in AUDIO -> tracks += Track(
                    id = "scan-$uri",
                    title = name.substringBeforeLast('.').replace('_', ' '),
                    sub = "${dir.name ?: "Local"} · ${ext.uppercase(Locale.US)}",
                    duration = "",
                    uri = uri,
                    gradient = gradientFor(name),
                )
                else -> docs += docItem(name, ext, uri, dir.name.orEmpty(), child.length(), child.lastModified(), child.type)
            }
        }
    }

    private fun videoItem(name: String, ext: String, uri: String, folder: String): MediaItem {
        val base = name.substringBeforeLast('.')
        val episode = EPISODE.find(base)
        return if (episode != null) {
            val title = clean(episode.groupValues[1])
            val season = episode.groupValues[2].toIntOrNull() ?: 1
            val number = episode.groupValues[3].toIntOrNull() ?: 1
            MediaItem(
                id = "scan-$uri",
                title = title,
                sub = "S$season E$number · Local",
                letter = initials(title),
                gradient = gradientFor(title),
                tag = "LOCAL",
                source = "Local",
                category = Category.TV,
                length = ext.uppercase(Locale.US),
                uri = uri,
            )
        } else {
            val year = YEAR.find(base)
            val title = clean(year?.groupValues?.get(1) ?: base)
            val sub = year?.groupValues?.get(2)?.let { "$it · Local" } ?: (folder.ifBlank { "Local" } + " · Local")
            MediaItem(
                id = "scan-$uri",
                title = title,
                sub = sub,
                letter = initials(title),
                gradient = gradientFor(title),
                tag = "LOCAL",
                source = "Local",
                category = Category.MOVIES,
                length = ext.uppercase(Locale.US),
                uri = uri,
            )
        }
    }

    private fun docItem(
        name: String,
        ext: String,
        uri: String,
        folder: String,
        size: Long,
        modified: Long,
        mime: String?,
    ): DocFile {
        val (badge, app, bg, fg) = docStyle(ext)
        return DocFile(
            id = "scan-$uri",
            name = name,
            sub = listOfNotNull(
                folder.ifBlank { null },
                readableSize(size),
                readableDate(modified),
            ).joinToString(" · "),
            badge = badge,
            app = app,
            background = bg,
            foreground = fg,
            uri = uri,
            mime = mime,
        )
    }

    private data class DocStyle(val badge: String, val app: String, val background: Long, val foreground: Long)

    private fun docStyle(ext: String): DocStyle = when (ext) {
        "xlsx", "xls", "csv" -> DocStyle("X", "Excel", 0xE7F4ECL, 0x1D6F42L)
        "docx", "doc", "rtf" -> DocStyle("W", "Word", 0xE8EFFCL, 0x2B579AL)
        "pdf" -> DocStyle("P", "PDF viewer", 0xFDEAEAL, 0xC9302CL)
        "pptx", "ppt" -> DocStyle("P", "PowerPoint", 0xFDEEE6L, 0xD24726L)
        "zip", "rar", "7z" -> DocStyle("Z", "Files", 0xF0EDE6L, 0x6B5B3EL)
        "png", "jpg", "jpeg", "gif", "webp" -> DocStyle("I", "Photos", 0xEAF0FBL, 0x3E5C97L)
        else -> DocStyle(ext.take(1).uppercase(Locale.US).ifBlank { "F" }, "Files", 0xEFEFF2L, 0x5B5B60L)
    }

    private fun readableSize(bytes: Long): String? = when {
        bytes <= 0 -> null
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    }

    private fun readableDate(millis: Long): String? {
        if (millis <= 0) return null
        val date = java.text.SimpleDateFormat("MMM d", Locale.US).format(java.util.Date(millis))
        return date
    }

    private fun clean(raw: String): String = raw
        .replace('.', ' ')
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(' ')
        .joinToString(" ") { word ->
            if (word.length <= 2 && word.uppercase(Locale.US) == word) word
            else word.replaceFirstChar { it.titlecase(Locale.US) }
        }

    private fun initials(title: String): String {
        val words = title.split(' ').filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(1).uppercase(Locale.US)
            else -> (words[0].take(1) + words[1].take(1)).uppercase(Locale.US)
        }
    }

    /** Stable pseudo-random cover gradient so a title always looks the same. */
    fun gradientFor(seed: String): Pair<Long, Long> {
        val palette = listOf(
            0x0F4C5CL to 0x9BD1D4L,
            0x5C0A0AL to 0xF2C14EL,
            0x3A0CA3L to 0xB5179EL,
            0x1B4332L to 0x95D5B2L,
            0x7C4A03L to 0xE8B464L,
            0x0B2545L to 0x8DA9C4L,
            0x31572CL to 0x90A955L,
            0x240046L to 0xFF5D8FL,
        )
        return palette[abs(seed.hashCode()) % palette.size]
    }
}
