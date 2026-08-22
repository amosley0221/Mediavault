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

/**
 * Every poster-shaped thing in the library: a movie, a show, or a game.
 *
 * [uri] is non-null for anything scanned out of a watched folder — those play in the
 * built-in player. Items sourced from an external service deep-link out instead.
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
    val length: String = "",
    val at: String = "",
    val progress: Int = 0,
    val uri: String? = null,
    val packageName: String? = null,
) {
    val isLocal: Boolean get() = uri != null || source.equals("local", true)
    val playsInApp: Boolean get() = uri != null || source.equals("plex", true) || source.equals("local", true)
}

data class Episode(val number: Int, val title: String, val length: String, val progress: Int)

data class MediaMeta(
    val year: Int,
    val genre: String,
    val rating: String,
    val seasons: Int = 1,
    val synopsis: String,
    val cast: List<String> = emptyList(),
    val collection: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList(),
)

data class Track(
    val id: String,
    val title: String,
    val sub: String,
    val duration: String,
    val uri: String? = null,
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
    val uri: String? = null,
    val mime: String? = null,
)

data class TwitchChannel(
    val channel: String,
    val game: String,
    val viewers: String,
    val letter: String,
    val gradient: Pair<Long, Long>,
)

data class TvProgram(
    val channel: String,
    val program: String,
    val time: String,
    val progress: Int,
    val abbr: String,
    val gradient: Pair<Long, Long>,
)

data class YoutubeVideo(
    val title: String,
    val sub: String,
    val gradient: Pair<Long, Long>,
    val query: String,
)

data class ServiceDef(
    val name: String,
    val description: String,
    val abbr: String,
    val gradient: Pair<Long, Long>,
    val launchUri: String,
)

data class WatchedFolder(val uri: String, val path: String, val meta: String)

data class VaultUser(val name: String, val role: String)
