package com.mediavault.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.Locale

/**
 * Extra folders the user picked by hand — a USB drive, an SD card path, anything MediaStore
 * does not index. Same output shape as [DeviceMedia] so the two merge.
 */
object LocalScanner {

    private val VIDEO = setOf("mkv", "mp4", "avi", "mov", "m4v", "webm", "ts", "wmv", "mpg", "mpeg")
    private val AUDIO = setOf("mp3", "flac", "m4a", "wav", "ogg", "aac", "opus", "wma")

    private val EPISODE = Regex("""(?i)^(.*?)[._\s-]+s(\d{1,2})[._\s-]?e(\d{1,3})""")
    private val YEAR = Regex("""^(.*?)[._\s(\[]+((19|20)\d{2})""")

    private const val MAX_DEPTH = 5
    private const val MAX_ITEMS = 1500

    fun scan(context: Context, folderUri: String): ScanResult {
        val root = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) }.getOrNull()
            ?: return ScanResult()
        val videos = mutableListOf<MediaItem>()
        val episodes = mutableMapOf<String, MutableList<EpisodeFile>>()
        val tracks = mutableListOf<Track>()
        val docs = mutableListOf<DocFile>()
        walk(root, 0, videos, episodes, tracks, docs)

        val shows = episodes.map { (title, files) ->
            val sorted = files.sortedWith(compareBy({ it.season }, { it.number }))
            MediaItem(
                id = "folder-show-${title.lowercase(Locale.US)}",
                title = title,
                sub = "${sorted.size} episode${if (sorted.size == 1) "" else "s"} · folder",
                letter = DeviceMedia.initials(title),
                gradient = DeviceMedia.gradientFor(title),
                tag = "SERIES",
                source = "Watched folder",
                category = Category.TV,
                uri = sorted.first().uri,
                durationMs = sorted.sumOf { it.durationMs },
                sizeBytes = sorted.sumOf { it.sizeBytes },
                episodes = sorted,
                artUri = sorted.first().uri,
            )
        }
        return ScanResult(videos + shows, tracks, docs)
    }

    private fun walk(
        dir: DocumentFile,
        depth: Int,
        videos: MutableList<MediaItem>,
        episodes: MutableMap<String, MutableList<EpisodeFile>>,
        tracks: MutableList<Track>,
        docs: MutableList<DocFile>,
    ) {
        if (depth > MAX_DEPTH) return
        if (videos.size + tracks.size + docs.size > MAX_ITEMS) return
        val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())
        for (child in children) {
            if (child.isDirectory) {
                walk(child, depth + 1, videos, episodes, tracks, docs)
                continue
            }
            val name = child.name ?: continue
            if (name.startsWith(".")) continue
            val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
            val uri = child.uri.toString()
            val folder = dir.name.orEmpty()
            when {
                ext in VIDEO -> addVideo(name, uri, folder, child.length(), child.lastModified(), videos, episodes)
                ext in AUDIO -> tracks += Track(
                    id = "folder-audio-$uri",
                    title = DeviceMedia.clean(name.substringBeforeLast('.')),
                    artist = folder,
                    album = "",
                    durationMs = 0,
                    uri = uri,
                    artUri = uri,
                    folder = folder,
                    gradient = DeviceMedia.gradientFor(name),
                )
                else -> docs += DocFile(
                    id = "folder-doc-$uri",
                    name = name,
                    sub = listOfNotNull(
                        folder.ifBlank { null },
                        DeviceMedia.formatSize(child.length()),
                        DeviceMedia.formatDate(child.lastModified()),
                    ).joinToString(" · "),
                    badge = ext.take(1).uppercase(Locale.US).ifBlank { "F" },
                    app = "Files",
                    background = 0xEFEFF2L,
                    foreground = 0x5B5B60L,
                    uri = uri,
                    mime = child.type,
                    modifiedAt = child.lastModified(),
                )
            }
        }
    }

    private fun addVideo(
        name: String,
        uri: String,
        folder: String,
        size: Long,
        modified: Long,
        videos: MutableList<MediaItem>,
        episodes: MutableMap<String, MutableList<EpisodeFile>>,
    ) {
        val base = name.substringBeforeLast('.')
        val match = EPISODE.find(base)
        if (match != null) {
            val show = DeviceMedia.clean(match.groupValues[1])
            val number = match.groupValues[3].toIntOrNull() ?: 1
            episodes.getOrPut(show) { mutableListOf() } += EpisodeFile(
                season = match.groupValues[2].toIntOrNull() ?: 1,
                number = number,
                title = DeviceMedia.clean(base.removeRange(match.range).trim(' ', '.', '-', '_'))
                    .ifBlank { "Episode $number" },
                uri = uri,
                durationMs = 0,
                sizeBytes = size,
            )
            return
        }
        val year = YEAR.find(base)?.groupValues?.get(2).orEmpty()
        val title = DeviceMedia.clean(YEAR.find(base)?.groupValues?.get(1) ?: base)
        videos += MediaItem(
            id = "folder-video-$uri",
            title = title,
            sub = listOfNotNull(year.ifBlank { null }, folder.ifBlank { null }).joinToString(" · "),
            letter = DeviceMedia.initials(title),
            gradient = DeviceMedia.gradientFor(title),
            tag = "FOLDER",
            source = "Watched folder",
            category = Category.MOVIES,
            uri = uri,
            sizeBytes = size,
            folder = folder,
            year = year,
            addedAt = modified,
        )
    }
}
