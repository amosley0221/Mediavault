package com.mediavault.app.data

enum class Category { GAMES, MOVIES, TV, MUSIC, FILES;

    val label: String
        get() = when (this) {
            GAMES -> "Games"
            MOVIES -> "Movies"
            TV -> "TV"
            MUSIC -> "Music"
            FILES -> "Files"
        }
}

/** One episode file found on the device, hung off its show. */
data class EpisodeFile(
    val season: Int,
    val number: Int,
    val title: String,
    val uri: String,
    val durationMs: Long,
    val sizeBytes: Long,
)

/**
 * Every poster-shaped thing in the library — a video file, a show built from its episode
 * files, or an installed game. Everything here comes off the phone: [uri] plays,
 * [packageName] launches.
 */
data class MediaItem(
    val id: String,
    val title: String,
    val sub: String,
    val letter: String,
    val gradient: Pair<Long, Long>,
    val tag: String?,
    val source: String,
    val category: Category,
    val uri: String? = null,
    val packageName: String? = null,
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val folder: String = "",
    val mime: String? = null,
    val year: String = "",
    val addedAt: Long = 0,
    val episodes: List<EpisodeFile> = emptyList(),
    /** Absolute path, when the file was found by walking storage — emulators want paths. */
    val filePath: String? = null,
    /** [GameSystem.id] when this item is a ROM. */
    val systemId: String? = null,
    /** The untouched filename — box-art lookups match on it, not the cleaned title. */
    val fileName: String? = null,
    /** Uri whose thumbnail represents this item (an episode's, for a show). */
    val artUri: String? = uri,
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artUri: String? = null,
    val folder: String = "",
    val gradient: Pair<Long, Long> = 0x1F6F54L to 0x8FD0A0L,
)

data class DocFile(
    val id: String,
    val name: String,
    val sub: String,
    val badge: String,
    val app: String,
    val background: Long,
    val foreground: Long,
    val uri: String,
    val mime: String? = null,
    val modifiedAt: Long = 0,
)

data class ServiceDef(
    val name: String,
    val description: String,
    val abbr: String,
    val gradient: Pair<Long, Long>,
    val launchUri: String,
    val packageHint: String? = null,
)

/**
 * A folder the user designated as the home of one category. When any exist for a category,
 * only files inside them are listed there — which is how "Movies" stops meaning "every video
 * on the phone, including WhatsApp clips".
 */
data class LibraryFolder(val uri: String, val path: String, val category: Category)

data class WatchedFolder(val uri: String, val path: String, val meta: String)

data class VaultUser(val name: String, val role: String)

/** Streaming apps MediaVault hands off to — it indexes the phone, they own their content. */
object Services {
    val all = listOf(
        ServiceDef("Plex", "Your server's movies, TV & music", "PX", 0x3C3703L to 0xE5A00DL, "https://app.plex.tv", "com.plexapp.android"),
        ServiceDef("Twitch", "Live channels you follow", "TW", 0x4B0082L to 0x9146FFL, "https://www.twitch.tv/directory/following", "tv.twitch.android.app"),
        ServiceDef("YouTube", "Subscriptions and watch later", "YT", 0x7A0000L to 0xFF0000L, "https://www.youtube.com/feed/subscriptions", "com.google.android.youtube"),
        ServiceDef("YouTube TV", "Live guide · what's on now", "TV", 0x7A0000L to 0xFF4D4DL, "https://tv.youtube.com/live", "com.google.android.apps.youtube.unplugged"),
        ServiceDef("Netflix", "Pick up where you left off", "NF", 0x3D0000L to 0xE50914L, "https://www.netflix.com", "com.netflix.mediaclient"),
        ServiceDef("Moonlight", "Stream games from your PC", "ML", 0x1B3A4BL to 0x7AD9F5L, "https://moonlight-stream.org", "com.limelight"),
    )
}
