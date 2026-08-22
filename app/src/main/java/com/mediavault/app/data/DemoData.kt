package com.mediavault.app.data

/**
 * The library MediaVault shows before anything real is connected — the exact catalogue
 * from the design prototype. Scanned folders and installed apps merge in on top of it.
 */
object DemoData {

    val games = listOf(
        MediaItem("g-totk", "Tears of the Kingdom", "Yuzu · 63 h", "TK", 0x386641L to 0xA7C957L, "YUZU", "Yuzu", Category.GAMES),
        MediaItem("g-stardew", "Stardew Valley", "Play Store · 88 h", "SV", 0x606C38L to 0xDDA15EL, "PLAY", "Play Store", Category.GAMES),
        MediaItem("g-botw", "Breath of the Wild", "Cemu · 47 h", "Z", 0x1F6F54L to 0x8FD0A0L, "CEMU", "Cemu", Category.GAMES),
        MediaItem("g-hades", "Hades", "Yuzu · 41 h", "H", 0x10002BL to 0xC77DFFL, "YUZU", "Yuzu", Category.GAMES),
        MediaItem("g-balatro", "Balatro", "Play Store · 35 h", "B", 0x240046L to 0xFF5D8FL, "PLAY", "Play Store", Category.GAMES),
        MediaItem("g-elden", "Elden Ring", "Moonlight · 71 h", "ER", 0x582F0EL to 0xE6CCB2L, "STREAM", "Moonlight", Category.GAMES),
    )

    val movies = listOf(
        MediaItem("m-dune2", "Dune: Part Two", "2024 · Plex", "D", 0x7C4A03L to 0xE8B464L, "PLEX", "Plex", Category.MOVIES, "2:46", "1:12", 43),
        MediaItem("m-spirited", "Spirited Away", "2001 · Local", "S", 0x155E63L to 0x76C7C0L, "LOCAL", "Local", Category.MOVIES, "2:05", "0:00", 0),
        MediaItem("m-batman", "The Batman", "2022 · Plex", "B", 0x1A1A1DL to 0xB3122EL, "PLEX", "Plex", Category.MOVIES, "2:56", "0:38", 22),
        MediaItem("m-inter", "Interstellar", "2014 · Local", "I", 0x0B2545L to 0x8DA9C4L, "LOCAL", "Local", Category.MOVIES, "2:49", "0:00", 0),
        MediaItem("m-oppen", "Oppenheimer", "2023 · Netflix", "O", 0x3D0000L to 0xFF7B00L, "NETFLIX", "Netflix", Category.MOVIES, "3:00", "0:00", 0),
        MediaItem("m-past", "Past Lives", "2023 · Plex", "P", 0x31572CL to 0x90A955L, "PLEX", "Plex", Category.MOVIES, "1:46", "0:00", 0),
    )

    val shows = listOf(
        MediaItem("s-sev", "Severance", "S2 E7 · Apple TV+", "S", 0x0F4C5CL to 0x9BD1D4L, "ATV+", "Apple TV+", Category.TV, "0:52", "0:31", 60),
        MediaItem("s-bear", "The Bear", "S3 E2 · Hulu", "B", 0x5C0A0AL to 0xF2C14EL, "HULU", "Hulu", Category.TV, "0:34", "0:00", 0),
        MediaItem("s-andor", "Andor", "S2 E4 · Plex DVR", "A", 0x22333BL to 0xC6AC8FL, "PLEX", "Plex", Category.TV, "0:48", "0:12", 25),
        MediaItem("s-planet", "Planet Earth III", "E5 · Local", "P", 0x1B4332L to 0x95D5B2L, "LOCAL", "Local", Category.TV, "0:58", "0:00", 0),
    )

    val music = listOf(
        Track("t-motion", "Motion", "Ben Howard · Local FLAC", "4:12"),
        Track("t-fishes", "Weird Fishes", "Radiohead · Local FLAC", "5:18", gradient = 0x0B2545L to 0x8DA9C4L),
        Track("t-nights", "Nights", "Frank Ocean · Plex", "5:07", gradient = 0x3D0000L to 0xFF7B00L),
        Track("t-time", "Time", "Hans Zimmer · Local", "4:35", gradient = 0x7C4A03L to 0xE8B464L),
        Track("t-redbone", "Redbone", "Childish Gambino · Plex", "5:26", gradient = 0x240046L to 0xFF5D8FL),
    )

    val files = listOf(
        DocFile("f-budget", "Budget 2026.xlsx", "Documents · 214 KB · edited Tue", "X", "Excel", 0xE7F4ECL, 0x1D6F42L),
        DocFile("f-lease", "Lease agreement.docx", "Downloads · 88 KB · Aug 12", "W", "Word", 0xE8EFFCL, 0x2B579AL),
        DocFile("f-receipt", "Fold8 receipt.pdf", "Downloads · 1.2 MB · Aug 3", "P", "PDF viewer", 0xFDEAEAL, 0xC9302CL),
        DocFile("f-backlog", "Game backlog.xlsx", "Documents · 96 KB · Jul 28", "X", "Excel", 0xE7F4ECL, 0x1D6F42L),
        DocFile("f-trip", "Trip itinerary.docx", "Documents · 132 KB · Jul 20", "W", "Word", 0xE8EFFCL, 0x2B579AL),
    )

    val twitch = listOf(
        TwitchChannel("CohhCarnage", "Elden Ring: Nightreign", "18.2K", "C", 0x3C096CL to 0x9D4EDDL),
        TwitchChannel("Northernlion", "Balatro", "9.4K", "N", 0x240046L to 0xFF5D8FL),
        TwitchChannel("DougDoug", "Just Chatting", "22.1K", "D", 0x014F86L to 0x89C2D9L),
        TwitchChannel("LilyPichu", "Stardew Valley", "6.8K", "L", 0x606C38L to 0xDDA15EL),
    )

    val youtubeTv = listOf(
        TvProgram("ESPN", "College Football: Michigan vs Texas", "7:00–10:30 PM", 45, "ES", 0x8D0801L to 0xE5383BL),
        TvProgram("Food Network", "Diners, Drive-Ins and Dives", "8:00–8:30 PM", 70, "FN", 0xBC6C25L to 0xFFC971L),
        TvProgram("Discovery", "Expedition Unknown", "8:00–9:00 PM", 35, "DS", 0x023E8AL to 0x48CAE4L),
        TvProgram("Comedy Central", "The Office marathon", "6:00–11:00 PM", 55, "CC", 0x3A0CA3L to 0xB5179EL),
    )

    val youtube = listOf(
        YoutubeVideo("I beat Elden Ring with a dance pad", "MissMikkaa · 2 h ago · 24:11", 0x582F0EL to 0xE6CCB2L, "MissMikkaa Elden Ring dance pad"),
        YoutubeVideo("Fold 8 long-term review", "MKBHD · 5 h ago · 14:02", 0x1D1D1FL to 0x6C757DL, "MKBHD Fold 8 review"),
        YoutubeVideo("The history of GameCube emulation", "MVG · 1 day ago · 19:47", 0x5A189AL to 0x48BFE3L, "MVG GameCube emulation history"),
        YoutubeVideo("Balatro but every joker is legendary", "Northernlion · 1 day ago · 31:20", 0x240046L to 0xFF5D8FL, "Northernlion Balatro legendary jokers"),
    )

    val services = listOf(
        ServiceDef("Plex", "Movies, TV & music from your server", "PX", 0x3C3703L to 0xE5A00DL, "https://app.plex.tv"),
        ServiceDef("Twitch", "Live channels you follow", "TW", 0x4B0082L to 0x9146FFL, "https://www.twitch.tv/directory/following"),
        ServiceDef("YouTube", "New videos from subscriptions", "YT", 0x7A0000L to 0xFF0000L, "https://www.youtube.com/feed/subscriptions"),
        ServiceDef("YouTube TV", "Live guide · what's on now", "TV", 0x7A0000L to 0xFF4D4DL, "https://tv.youtube.com/live"),
        ServiceDef("Netflix", "Deep-link titles into the app", "NF", 0x3D0000L to 0xE50914L, "https://www.netflix.com"),
        ServiceDef("Moonlight", "Stream games from your PC", "ML", 0x1B3A4BL to 0x7AD9F5L, "https://moonlight-stream.org"),
    )

    val meta: Map<String, MediaMeta> = mapOf(
        "Dune: Part Two" to MediaMeta(
            2024, "Sci-fi", "PG-13",
            synopsis = "Paul Atreides unites with the Fremen and seeks revenge against the conspirators who destroyed his family, facing a choice between the love of his life and the fate of the universe.",
            cast = listOf("Timothée Chalamet", "Zendaya", "Rebecca Ferguson", "Austin Butler", "Javier Bardem"),
            collection = listOf("Dune (2021)"),
        ),
        "Spirited Away" to MediaMeta(
            2001, "Animation", "PG",
            synopsis = "A ten-year-old girl wanders into a world of spirits and must work in a bathhouse for the gods to free herself and her parents.",
            cast = listOf("Rumi Hiiragi", "Miyu Irino", "Mari Natsuki"),
            collection = listOf("Princess Mononoke", "Howl's Moving Castle"),
        ),
        "The Batman" to MediaMeta(
            2022, "Crime", "PG-13",
            synopsis = "In his second year fighting crime, Batman uncovers corruption in Gotham while pursuing the Riddler, a serial killer targeting the city's elite.",
            cast = listOf("Robert Pattinson", "Zoë Kravitz", "Paul Dano", "Colin Farrell"),
        ),
        "Interstellar" to MediaMeta(
            2014, "Sci-fi", "PG-13",
            synopsis = "With Earth becoming uninhabitable, a team of explorers travels through a wormhole in search of a new home for humanity.",
            cast = listOf("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"),
        ),
        "Oppenheimer" to MediaMeta(
            2023, "Biography", "R",
            synopsis = "The story of J. Robert Oppenheimer and the development of the atomic bomb.",
            cast = listOf("Cillian Murphy", "Emily Blunt", "Robert Downey Jr."),
        ),
        "Past Lives" to MediaMeta(
            2023, "Drama", "PG-13",
            synopsis = "Two childhood friends reunite in New York for one fateful week, confronting notions of destiny, love, and the choices that make a life.",
            cast = listOf("Greta Lee", "Teo Yoo", "John Magaro"),
        ),
        "Severance" to MediaMeta(
            2025, "Thriller", "TV-MA", seasons = 2,
            synopsis = "Employees at Lumon Industries have their memories surgically divided between work and personal lives — until an innie starts to question everything.",
            cast = listOf("Adam Scott", "Britt Lower", "Zach Cherry", "Patricia Arquette"),
            episodes = listOf(
                Episode(1, "Hello, Ms. Cobel", "49 min", 100),
                Episode(2, "Goodbye, Mrs. Selvig", "44 min", 100),
                Episode(3, "Who Is Alive?", "51 min", 100),
                Episode(4, "Woe's Hollow", "55 min", 100),
                Episode(5, "Trojan's Horse", "46 min", 100),
                Episode(6, "Attila", "48 min", 100),
                Episode(7, "Chikhai Bardo", "52 min", 60),
                Episode(8, "Sweet Vitriol", "43 min", 0),
            ),
        ),
        "The Bear" to MediaMeta(
            2024, "Comedy-drama", "TV-MA", seasons = 3,
            synopsis = "A young chef returns to Chicago to run his family's sandwich shop, wrestling with grief, debt, and a kitchen full of strong personalities.",
            cast = listOf("Jeremy Allen White", "Ayo Edebiri", "Ebon Moss-Bachrach"),
        ),
        "Andor" to MediaMeta(
            2025, "Sci-fi", "TV-14", seasons = 2,
            synopsis = "The story of the rebellion's unlikely beginnings, told through Cassian Andor's journey from cynical survivor to revolutionary.",
            cast = listOf("Diego Luna", "Stellan Skarsgård", "Genevieve O'Reilly"),
        ),
        "Planet Earth III" to MediaMeta(
            2023, "Documentary", "TV-G",
            synopsis = "David Attenborough narrates a journey to the planet's most extraordinary landscapes and the animals that call them home.",
            cast = listOf("David Attenborough"),
        ),
    )

    /** Fallback metadata for anything scanned off the device — no TMDB match yet. */
    fun metaFor(item: MediaItem): MediaMeta = meta[item.title] ?: MediaMeta(
        year = item.sub.take(4).toIntOrNull() ?: 0,
        genre = if (item.category == Category.TV) "Series" else "Film",
        rating = "NR",
        seasons = 1,
        synopsis = "Scanned from a watched folder. Connect a metadata source to fill in the synopsis, cast and artwork for this title.",
        cast = emptyList(),
        episodes = emptyList(),
    )
}
