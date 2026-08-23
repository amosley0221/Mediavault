package com.mediavault.app.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.util.Locale
import kotlin.math.abs

data class ScanResult(
    val media: List<MediaItem> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val documents: List<DocFile> = emptyList(),
    /** Every folder holding videos — including ones currently filtered out of the library. */
    val videoFolders: List<VideoFolder> = emptyList(),
) {
    val total: Int get() = media.size + tracks.size + documents.size

    operator fun plus(other: ScanResult) = ScanResult(
        media + other.media,
        tracks + other.tracks,
        documents + other.documents,
        (videoFolders + other.videoFolders)
            .groupBy { it.path }
            .map { (path, group) -> VideoFolder(path, group.first().name, group.sumOf { it.count }) },
    )

    /** Same file found by two scanners (MediaStore and a watched folder) shows up once. */
    fun deduped() = ScanResult(
        media.distinctBy { it.uri ?: it.id },
        tracks.distinctBy { it.uri },
        documents.distinctBy { it.uri },
        videoFolders.sortedByDescending { it.count },
    )
}

/**
 * Reads what is actually on the phone through MediaStore: every video, every track, and —
 * when the user grants all-files access — the documents too. No catalogue, no placeholders.
 */
/**
 * Which folders count as Movies and which count as TV. An empty list for a category means
 * "anywhere on the phone"; once a folder is named, nothing outside it qualifies.
 */
data class VideoFilters(
    val movies: List<String> = emptyList(),
    val tv: List<String> = emptyList(),
    /** Folder path → [FolderRule] name, set by hand in Sources. Beats everything else. */
    val rules: Map<String, String> = emptyMap(),
) {
    /** The rule for a folder, inherited by anything nested inside it. */
    fun ruleFor(path: String): FolderRule {
        val normalised = path.lowercase(Locale.US).trim('/')
        val match = rules.keys
            .filter { key ->
                val target = key.lowercase(Locale.US).trim('/')
                target.isNotBlank() && (normalised == target || normalised.startsWith("$target/"))
            }
            .maxByOrNull { it.length }
            ?: return FolderRule.AUTO
        return runCatching { FolderRule.valueOf(rules.getValue(match)) }.getOrDefault(FolderRule.AUTO)
    }

    private fun matches(folders: List<String>, path: String): Boolean {
        val normalised = path.lowercase(Locale.US).trim('/')
        return folders.any { folder ->
            val target = folder.lowercase(Locale.US).trim('/')
            target.isNotBlank() && (normalised == target || normalised.startsWith("$target/") ||
                normalised.contains("/$target/") || normalised.endsWith("/$target"))
        }
    }

    /** The category this path is pinned to, or null when it is in neither folder. */
    fun categoryFor(path: String): Category? = when {
        matches(tv, path) -> Category.TV
        matches(movies, path) -> Category.MOVIES
        else -> null
    }

    fun hasFilterFor(category: Category): Boolean = when (category) {
        Category.TV -> tv.isNotEmpty()
        Category.MOVIES -> movies.isNotEmpty()
        else -> false
    }
}

object DeviceMedia {

    private val EPISODE = Regex("""(?i)^(.*?)[._\s-]+s(\d{1,2})[._\s-]?e(\d{1,3})""")
    private val YEAR = Regex("""^(.*?)[._\s(\[]+((19|20)\d{2})""")

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

    private val DOC_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx", "txt", "rtf", "md",
        "epub", "zip", "rar", "7z", "apk", "json", "odt", "ods",
    )

    fun scan(context: Context, filters: VideoFilters = VideoFilters()): ScanResult {
        val storage = otherFiles(context)
        val videos = videos(context, filters)
        return ScanResult(
            media = videos + storage.roms,
            tracks = audio(context),
            documents = storage.documents,
            videoFolders = videoFolderCensus,
        )
    }

    private data class StorageScan(
        val documents: List<DocFile> = emptyList(),
        val roms: List<MediaItem> = emptyList(),
    )

    // ---- video -----------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun videos(context: Context, filters: VideoFilters): List<MediaItem> {
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            projection += MediaStore.Video.Media.RELATIVE_PATH
        }

        val singles = mutableListOf<MediaItem>()
        val folderCounts = mutableMapOf<String, VideoFolder>()
        val shows = mutableMapOf<String, MutableList<EpisodeFile>>()
        val showArt = mutableMapOf<String, String>()
        val showAdded = mutableMapOf<String, Long>()

        query(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection.toTypedArray(),
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC") { cursor ->
            val id = cursor.getLong(MediaStore.Video.Media._ID) ?: return@query
            val name = cursor.getString(MediaStore.Video.Media.DISPLAY_NAME) ?: return@query
            val duration = cursor.getLong(MediaStore.Video.Media.DURATION) ?: 0L
            val size = cursor.getLong(MediaStore.Video.Media.SIZE) ?: 0L
            val modified = (cursor.getLong(MediaStore.Video.Media.DATE_MODIFIED) ?: 0L) * 1000
            val mime = cursor.getString(MediaStore.Video.Media.MIME_TYPE)
            val folder = cursor.getString(MediaStore.Video.Media.BUCKET_DISPLAY_NAME).orEmpty()
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
            val dataPath = cursor.getString(MediaStore.Video.Media.DATA)
            val relativePath = cursor.getString(MediaStore.Video.Media.RELATIVE_PATH)
            val folderPath = relativePath ?: dataPath?.substringBeforeLast('/').orEmpty().ifBlank { folder }

            val normalisedFolder = folderPath.trim('/')
            folderCounts[normalisedFolder.lowercase(Locale.US)] = VideoFolder(
                path = normalisedFolder,
                name = folder.ifBlank { normalisedFolder.substringAfterLast('/') },
                count = (folderCounts[normalisedFolder.lowercase(Locale.US)]?.count ?: 0) + 1,
            )

            val base = name.substringBeforeLast('.')
            val episode = EPISODE.find(base)

            // Order of authority: a rule the user set on the folder, then a folder they
            // designated for a category, then the filename. Anything outside a configured
            // folder is dropped from that category entirely.
            val rule = filters.ruleFor(normalisedFolder)
            if (rule == FolderRule.HIDDEN) return@query
            val pinned = when (rule) {
                FolderRule.MOVIES -> Category.MOVIES
                FolderRule.TV -> Category.TV
                else -> filters.categoryFor(folderPath)
            }
            val natural = if (episode != null) Category.TV else Category.MOVIES
            val category = pinned ?: natural
            if (pinned == null && filters.hasFilterFor(category)) return@query

            if (episode != null && category == Category.TV) {
                val show = clean(episode.groupValues[1])
                val season = episode.groupValues[2].toIntOrNull() ?: 1
                val number = episode.groupValues[3].toIntOrNull() ?: 1
                shows.getOrPut(show) { mutableListOf() } += EpisodeFile(
                    season = season,
                    number = number,
                    title = clean(base.removeRange(episode.range).trim(' ', '.', '-', '_'))
                        .ifBlank { "Episode $number" },
                    uri = uri,
                    durationMs = duration,
                    sizeBytes = size,
                )
                showArt.putIfAbsent(show, uri)
                showAdded[show] = maxOf(showAdded[show] ?: 0L, modified)
            } else {
                val year = YEAR.find(base)?.groupValues?.get(2).orEmpty()
                val title = clean(YEAR.find(base)?.groupValues?.get(1) ?: base)
                singles += MediaItem(
                    id = "video-$id",
                    title = title,
                    sub = listOfNotNull(
                        year.ifBlank { null },
                        folder.ifBlank { null },
                        formatDuration(duration).ifBlank { null },
                    ).joinToString(" · "),
                    letter = initials(title),
                    gradient = gradientFor(title),
                    tag = folder.uppercase(Locale.US).take(10).ifBlank { "VIDEO" },
                    source = "This phone",
                    category = category,
                    uri = uri,
                    durationMs = duration,
                    sizeBytes = size,
                    folder = folder,
                    mime = mime,
                    year = year,
                    addedAt = modified,
                    filePath = dataPath,
                )
            }
        }

        val showItems = shows.map { (title, episodes) ->
            val sorted = episodes.sortedWith(compareBy({ it.season }, { it.number }))
            val seasons = sorted.map { it.season }.distinct().size
            MediaItem(
                id = "show-${title.lowercase(Locale.US)}",
                title = title,
                sub = "${sorted.size} episode${if (sorted.size == 1) "" else "s"} · " +
                    "$seasons season${if (seasons == 1) "" else "s"}",
                letter = initials(title),
                gradient = gradientFor(title),
                tag = "SERIES",
                source = "This phone",
                category = Category.TV,
                uri = sorted.first().uri,
                durationMs = sorted.sumOf { it.durationMs },
                sizeBytes = sorted.sumOf { it.sizeBytes },
                addedAt = showAdded[title] ?: 0L,
                episodes = sorted,
                artUri = showArt[title],
            )
        }

        videoFolderCensus = folderCounts.values.sortedByDescending { it.count }
        return singles + showItems
    }

    /** Filled by the video pass so Sources can list folders even when they are filtered out. */
    private var videoFolderCensus: List<VideoFolder> = emptyList()

    // ---- audio -----------------------------------------------------------

    private fun audio(context: Context): List<Track> {
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
        }

        val tracks = mutableListOf<Track>()
        query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection.toTypedArray(),
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0") { cursor ->
            val id = cursor.getLong(MediaStore.Audio.Media._ID) ?: return@query
            val title = cursor.getString(MediaStore.Audio.Media.TITLE)
                ?: cursor.getString(MediaStore.Audio.Media.DISPLAY_NAME)?.substringBeforeLast('.')
                ?: return@query
            val artist = cursor.getString(MediaStore.Audio.Media.ARTIST)
                ?.takeUnless { it == "<unknown>" }.orEmpty()
            val album = cursor.getString(MediaStore.Audio.Media.ALBUM).orEmpty()
            val albumId = cursor.getLong(MediaStore.Audio.Media.ALBUM_ID)
            val duration = cursor.getLong(MediaStore.Audio.Media.DURATION) ?: 0L
            val folder = cursor.getString(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME).orEmpty()
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

            tracks += Track(
                id = "audio-$id",
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                uri = uri,
                artUri = albumId?.let {
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it).toString()
                },
                folder = folder,
                gradient = gradientFor(album.ifBlank { title }),
            )
        }
        return tracks
    }

    // ---- documents -------------------------------------------------------

    /**
     * Android 13+ only hands out media through MediaStore, so documents come from a direct
     * walk of shared storage when the user has granted all-files access. Older versions can
     * still see them through MediaStore.Files with read permission.
     */
    private fun otherFiles(context: Context): StorageScan =
        if (hasAllFilesAccess()) walkSharedStorage() else mediaStoreFiles(context)

    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    /**
     * One pass over shared storage that picks up documents and ROMs together, and notices
     * box art sitting next to a ROM under the same name.
     */
    private fun walkSharedStorage(): StorageScan {
        val root = Environment.getExternalStorageDirectory() ?: return StorageScan()
        val docs = mutableListOf<DocFile>()
        val roms = mutableListOf<MediaItem>()
        val stack = ArrayDeque<Pair<File, Int>>()
        stack += root to 0

        while (stack.isNotEmpty() && docs.size + roms.size < 3000) {
            val (dir, depth) = stack.removeFirst()
            if (depth > 6) continue
            val children = dir.listFiles() ?: continue

            // Sidecar artwork in this folder, keyed by the ROM's base name.
            val artwork = children.asSequence()
                .filter { it.isFile && it.extension.lowercase(Locale.US) in IMAGE_EXTENSIONS }
                .associateBy { it.nameWithoutExtension.lowercase(Locale.US) }

            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) {
                    if (child.name == "Android") continue
                    stack += child to depth + 1
                    continue
                }
                val ext = child.extension.lowercase(Locale.US)
                val system = if (Emulators.isRomExtension(ext)) {
                    Emulators.systemFor(ext, child.parent.orEmpty())
                } else null

                if (system != null) {
                    roms += rom(child, system, artwork[child.nameWithoutExtension.lowercase(Locale.US)])
                } else if (ext in DOC_EXTENSIONS) {
                    docs += docFile(
                        id = "file-${child.absolutePath}",
                        name = child.name,
                        ext = ext,
                        folder = child.parentFile?.name.orEmpty(),
                        size = child.length(),
                        modified = child.lastModified(),
                        uri = Uri.fromFile(child).toString(),
                        mime = null,
                    )
                }
            }
        }
        return StorageScan(
            documents = docs.sortedByDescending { it.modifiedAt },
            roms = roms.sortedBy { it.title.lowercase(Locale.US) },
        )
    }

    private fun rom(file: File, system: GameSystem, art: File?): MediaItem {
        val title = clean(file.nameWithoutExtension)
        return MediaItem(
            id = "rom-${file.absolutePath}",
            title = title,
            sub = listOfNotNull(system.label, formatSize(file.length())).joinToString(" · "),
            letter = initials(title),
            gradient = system.gradient,
            tag = system.tag,
            source = system.label,
            category = Category.GAMES,
            uri = Uri.fromFile(file).toString(),
            sizeBytes = file.length(),
            folder = file.parentFile?.name.orEmpty(),
            addedAt = file.lastModified(),
            filePath = file.absolutePath,
            systemId = system.id,
            fileName = file.name,
            artUri = art?.let { Uri.fromFile(it).toString() },
        )
    }

    /**
     * Without all-files access the only window onto non-media files is MediaStore.Files,
     * which older Android versions populate for everything on the volume.
     */
    private fun mediaStoreFiles(context: Context): StorageScan {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )
        val collection = MediaStore.Files.getContentUri("external")
        val docs = mutableListOf<DocFile>()
        val roms = mutableListOf<MediaItem>()
        query(context, collection, projection, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC") { cursor ->
            val id = cursor.getLong(MediaStore.Files.FileColumns._ID) ?: return@query
            val name = cursor.getString(MediaStore.Files.FileColumns.DISPLAY_NAME) ?: return@query
            val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
            val uri = ContentUris.withAppendedId(collection, id).toString()
            val size = cursor.getLong(MediaStore.Files.FileColumns.SIZE) ?: 0L
            val modified = (cursor.getLong(MediaStore.Files.FileColumns.DATE_MODIFIED) ?: 0L) * 1000

            val system = if (Emulators.isRomExtension(ext)) Emulators.systemFor(ext, "") else null
            if (system != null) {
                val title = clean(name.substringBeforeLast('.'))
                roms += MediaItem(
                    id = "rom-$id",
                    title = title,
                    sub = listOfNotNull(system.label, formatSize(size)).joinToString(" · "),
                    letter = initials(title),
                    gradient = system.gradient,
                    tag = system.tag,
                    source = system.label,
                    category = Category.GAMES,
                    uri = uri,
                    sizeBytes = size,
                    addedAt = modified,
                    systemId = system.id,
                    fileName = name,
                    artUri = null,
                )
            } else if (ext in DOC_EXTENSIONS) {
                docs += docFile(
                    id = "doc-$id",
                    name = name,
                    ext = ext,
                    folder = "",
                    size = size,
                    modified = modified,
                    uri = uri,
                    mime = cursor.getString(MediaStore.Files.FileColumns.MIME_TYPE),
                )
            }
        }
        return StorageScan(docs, roms.sortedBy { it.title.lowercase(Locale.US) })
    }

    private fun docFile(
        id: String,
        name: String,
        ext: String,
        folder: String,
        size: Long,
        modified: Long,
        uri: String,
        mime: String?,
    ): DocFile {
        val style = docStyle(ext)
        return DocFile(
            id = id,
            name = name,
            sub = listOfNotNull(
                folder.ifBlank { null },
                formatSize(size),
                formatDate(modified),
            ).joinToString(" · "),
            badge = style.badge,
            app = style.app,
            background = style.background,
            foreground = style.foreground,
            uri = uri,
            mime = mime,
            modifiedAt = modified,
        )
    }

    private data class DocStyle(val badge: String, val app: String, val background: Long, val foreground: Long)

    private fun docStyle(ext: String): DocStyle = when (ext) {
        "xlsx", "xls", "csv", "ods" -> DocStyle("X", "Excel", 0xE7F4ECL, 0x1D6F42L)
        "docx", "doc", "rtf", "odt" -> DocStyle("W", "Word", 0xE8EFFCL, 0x2B579AL)
        "pdf" -> DocStyle("P", "PDF viewer", 0xFDEAEAL, 0xC9302CL)
        "pptx", "ppt" -> DocStyle("P", "PowerPoint", 0xFDEEE6L, 0xD24726L)
        "zip", "rar", "7z" -> DocStyle("Z", "Files", 0xF0EDE6L, 0x6B5B3EL)
        "apk" -> DocStyle("A", "Installer", 0xE9F6EAL, 0x3D8B40L)
        "epub" -> DocStyle("E", "Reader", 0xF2ECFBL, 0x6A3FA0L)
        else -> DocStyle(ext.take(1).uppercase(Locale.US).ifBlank { "F" }, "Files", 0xEFEFF2L, 0x5B5B60L)
    }

    // ---- helpers ---------------------------------------------------------

    private fun query(
        context: Context,
        collection: Uri,
        projection: Array<String>,
        sortOrder: String?,
        selection: String? = null,
        onRow: (CursorRow) -> Unit,
    ) {
        runCatching {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val row = CursorRow(cursor)
                while (cursor.moveToNext()) onRow(row)
            }
        }
    }

    /** Column lookups by name so a missing column on an old API is just null. */
    class CursorRow(private val cursor: Cursor) {
        private val indices = mutableMapOf<String, Int>()

        private fun index(column: String): Int =
            indices.getOrPut(column) { cursor.getColumnIndex(column) }

        fun getString(column: String): String? {
            val i = index(column)
            return if (i < 0 || cursor.isNull(i)) null else cursor.getString(i)
        }

        fun getLong(column: String): Long? {
            val i = index(column)
            return if (i < 0 || cursor.isNull(i)) null else cursor.getLong(i)
        }
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return ""
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun formatSize(bytes: Long): String? = when {
        bytes <= 0 -> null
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
        else -> String.format(Locale.US, "%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    }

    fun formatDate(millis: Long): String? {
        if (millis <= 0) return null
        return java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(millis))
    }

    fun clean(raw: String): String = raw
        .replace('.', ' ')
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(' ')
        .joinToString(" ") { word ->
            if (word.length <= 2 && word.uppercase(Locale.US) == word) word
            else word.replaceFirstChar { it.titlecase(Locale.US) }
        }

    fun initials(title: String): String {
        val words = title.split(' ', ':', '-').filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase(Locale.US)
            else -> (words[0].take(1) + words[1].take(1)).uppercase(Locale.US)
        }
    }

    /** Stable fallback cover colour when a file has no thumbnail. */
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
