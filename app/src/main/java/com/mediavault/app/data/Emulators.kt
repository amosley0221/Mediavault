package com.mediavault.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

/**
 * A console MediaVault can recognise from a ROM's file extension, plus the emulators known
 * to run it. [knownPackages] is ordered best-first and only used to spot what is installed;
 * anything else on the phone that claims the file type shows up as a candidate too.
 */
data class GameSystem(
    val id: String,
    val label: String,
    val tag: String,
    val extensions: Set<String>,
    val folderAliases: Set<String> = emptySet(),
    val knownPackages: List<String> = emptyList(),
    /** libretro core basename, for launching through RetroArch. */
    val retroCore: String? = null,
    val gradient: Pair<Long, Long> = 0x1B3A4BL to 0x7AD9F5L,
)

object Emulators {

    val RETRO_ARCH_PACKAGES = listOf(
        "com.retroarch.aarch64",
        "com.retroarch",
        "com.retroarch.ra32",
    )

    val systems = listOf(
        GameSystem(
            id = "nes", label = "NES", tag = "NES",
            extensions = setOf("nes", "fds", "unf", "unif"),
            folderAliases = setOf("nes", "famicom", "fc"),
            knownPackages = listOf("com.explusalpha.NesEmu", "nostalgia.nes", "com.johnemulators.johnnes"),
            retroCore = "nestopia",
            gradient = 0x5C0A0AL to 0xF2C14EL,
        ),
        GameSystem(
            id = "snes", label = "Super Nintendo", tag = "SNES",
            extensions = setOf("smc", "sfc", "swc", "fig"),
            folderAliases = setOf("snes", "sfc", "superfamicom", "super nintendo"),
            knownPackages = listOf("com.explusalpha.Snes9xPlus", "com.snes9x.ex", "com.johnemulators.johnsnes"),
            retroCore = "snes9x",
            gradient = 0x3A0CA3L to 0xB5179EL,
        ),
        GameSystem(
            id = "gb", label = "Game Boy", tag = "GB",
            extensions = setOf("gb", "gbc"),
            folderAliases = setOf("gb", "gbc", "gameboy", "game boy"),
            knownPackages = listOf("com.fastemulator.gbc", "com.explusalpha.GbcEmu", "nostalgia.gbc"),
            retroCore = "gambatte",
            gradient = 0x1B4332L to 0x95D5B2L,
        ),
        GameSystem(
            id = "gba", label = "Game Boy Advance", tag = "GBA",
            extensions = setOf("gba", "agb"),
            folderAliases = setOf("gba", "gameboyadvance", "game boy advance"),
            knownPackages = listOf(
                "com.fastemulator.gba", "com.fastemulator.gbafree",
                "com.explusalpha.GbaEmu", "com.johnemulators.johngbac",
            ),
            retroCore = "mgba",
            gradient = 0x3A0CA3L to 0x7AB8FFL,
        ),
        GameSystem(
            id = "n64", label = "Nintendo 64", tag = "N64",
            extensions = setOf("n64", "z64", "v64"),
            folderAliases = setOf("n64", "nintendo64", "nintendo 64"),
            knownPackages = listOf(
                "org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.fzurita.pro",
                "org.mupen64plusae.v3.alpha",
            ),
            retroCore = "mupen64plus_next",
            gradient = 0x0B2545L to 0x8DA9C4L,
        ),
        GameSystem(
            id = "nds", label = "Nintendo DS", tag = "NDS",
            extensions = setOf("nds", "dsi"),
            folderAliases = setOf("nds", "ds", "nintendods"),
            knownPackages = listOf("com.dsemu.drastic", "me.magnum.melonds"),
            retroCore = "desmume",
            gradient = 0x22333BL to 0xC6AC8FL,
        ),
        GameSystem(
            id = "3ds", label = "Nintendo 3DS", tag = "3DS",
            extensions = setOf("3ds", "cia", "cxi", "app"),
            folderAliases = setOf("3ds", "nintendo3ds"),
            knownPackages = listOf("io.github.lime3ds.android", "org.citra.citra_emu", "org.citra.emu"),
            gradient = 0x7C4A03L to 0xE8B464L,
        ),
        GameSystem(
            id = "switch", label = "Nintendo Switch", tag = "SWITCH",
            extensions = setOf("nsp", "xci"),
            folderAliases = setOf("switch", "nx"),
            knownPackages = listOf("org.yuzu.yuzu_emu", "dev.suyu.suyu_emu", "org.skyline.emu"),
            gradient = 0x8D0801L to 0xE5383BL,
        ),
        GameSystem(
            id = "gamecube", label = "GameCube / Wii", tag = "GC/WII",
            extensions = setOf("gcm", "gcz", "rvz", "wbfs", "wad", "nkit"),
            folderAliases = setOf("gamecube", "gc", "wii", "dolphin"),
            knownPackages = listOf("org.dolphinemu.dolphinemu"),
            gradient = 0x3C096CL to 0x9D4EDDL,
        ),
        GameSystem(
            id = "psp", label = "PSP", tag = "PSP",
            extensions = setOf("cso", "pbp", "chd"),
            folderAliases = setOf("psp", "playstationportable"),
            knownPackages = listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold"),
            retroCore = "ppsspp",
            gradient = 0x1A1A1DL to 0x6C757DL,
        ),
        GameSystem(
            id = "ps1", label = "PlayStation", tag = "PS1",
            extensions = setOf("cue", "img", "mdf", "ecm"),
            folderAliases = setOf("ps1", "psx", "playstation"),
            knownPackages = listOf("com.epsxe.ePSXe", "com.emulator.fpse", "com.emulator.fpse64"),
            retroCore = "pcsx_rearmed",
            gradient = 0x1D1D1FL to 0x6C757DL,
        ),
        GameSystem(
            id = "ps2", label = "PlayStation 2", tag = "PS2",
            extensions = setOf("gz"),
            folderAliases = setOf("ps2", "playstation2"),
            knownPackages = listOf("net.aethersx2.android", "xyz.aethersx2.android"),
            gradient = 0x14213DL to 0x4361EEL,
        ),
        GameSystem(
            id = "genesis", label = "Genesis / Mega Drive", tag = "GENESIS",
            extensions = setOf("md", "gen", "smd", "32x"),
            folderAliases = setOf("genesis", "megadrive", "mega drive", "md", "sega"),
            knownPackages = listOf("com.explusalpha.MdEmu"),
            retroCore = "genesis_plus_gx",
            gradient = 0x0B090AL to 0x5C677DL,
        ),
        GameSystem(
            id = "mastersystem", label = "Master System / Game Gear", tag = "SMS/GG",
            extensions = setOf("sms", "gg"),
            folderAliases = setOf("mastersystem", "sms", "gamegear", "gg"),
            knownPackages = listOf("com.explusalpha.MdEmu"),
            retroCore = "genesis_plus_gx",
            gradient = 0x2D3142L to 0x9C9EB9L,
        ),
        GameSystem(
            id = "dreamcast", label = "Dreamcast", tag = "DC",
            extensions = setOf("gdi", "cdi"),
            folderAliases = setOf("dreamcast", "dc"),
            knownPackages = listOf("com.reicast.emulator", "com.flycast.emulator"),
            retroCore = "flycast",
            gradient = 0x14213DL to 0x00B4D8L,
        ),
        GameSystem(
            id = "pce", label = "PC Engine", tag = "PCE",
            extensions = setOf("pce", "sgx"),
            folderAliases = setOf("pce", "pcengine", "turbografx"),
            retroCore = "mednafen_pce",
            gradient = 0x4A0E0EL to 0xE07A5FL,
        ),
        GameSystem(
            id = "atari", label = "Atari 2600", tag = "A2600",
            extensions = setOf("a26", "a78", "lnx"),
            folderAliases = setOf("atari", "a2600", "2600"),
            retroCore = "stella",
            gradient = 0x582F0EL to 0xE6CCB2L,
        ),
        GameSystem(
            id = "arcade", label = "Arcade", tag = "ARCADE",
            extensions = setOf("zip", "7z"),
            folderAliases = setOf("arcade", "mame", "fbneo", "fba", "neogeo"),
            retroCore = "fbneo",
            gradient = 0x240046L to 0xFF5D8FL,
        ),
    )

    /** Extensions that only mean "ROM" inside a system-named folder — .zip is not a console. */
    private val AMBIGUOUS = setOf("zip", "7z", "bin", "iso", "chd", "cue", "img", "gz", "app")

    private val byExtension: Map<String, List<GameSystem>> = buildMap {
        systems.forEach { system ->
            system.extensions.forEach { ext ->
                put(ext, (get(ext) ?: emptyList()) + system)
            }
        }
        // Shared disc/dump extensions: resolved by the folder they sit in.
        put("iso", listOf(byId("psp"), byId("ps1"), byId("gamecube"), byId("ps2")))
        put("bin", listOf(byId("ps1"), byId("genesis")))
        put("chd", listOf(byId("psp"), byId("ps1"), byId("dreamcast")))
    }

    private fun byId(id: String): GameSystem = systems.first { it.id == id }

    fun isRomExtension(ext: String): Boolean = byExtension.containsKey(ext.lowercase(Locale.US))

    /**
     * Picks the console for a file. Unambiguous extensions answer themselves; shared ones
     * (.iso, .bin, .zip) are decided by the folder the file sits in, and are ignored when
     * that gives no hint — otherwise every archive on the phone would look like a ROM.
     */
    fun systemFor(extension: String, folderPath: String): GameSystem? {
        val ext = extension.lowercase(Locale.US)
        val candidates = byExtension[ext] ?: return null
        val folders = folderPath.lowercase(Locale.US).split('/', '\\').filter { it.isNotBlank() }

        val byFolder = candidates.firstOrNull { system ->
            folders.any { folder -> system.folderAliases.any { alias -> folder == alias || folder.contains(alias) } }
        }
        if (byFolder != null) return byFolder
        if (ext in AMBIGUOUS) {
            // Still a ROM if it lives somewhere obviously emulation-shaped.
            val emulationFolder = folders.any { it in EMULATION_FOLDERS }
            return if (emulationFolder && candidates.size == 1) candidates.first() else null
        }
        return candidates.first()
    }

    private val EMULATION_FOLDERS = setOf("roms", "rom", "emulation", "emulators", "games", "retroarch")

    /** True when a folder name says "ROMs live here", used to widen an ambiguous match. */
    fun looksLikeRomFolder(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower in EMULATION_FOLDERS || systems.any { system ->
            system.folderAliases.any { it == lower }
        }
    }

    // ---- installed emulators --------------------------------------------

    data class EmulatorApp(val packageName: String, val label: String)

    /** Emulators on the phone that can plausibly open [system]. */
    fun candidatesFor(context: Context, system: GameSystem): List<EmulatorApp> {
        val pm = context.packageManager
        val found = linkedMapOf<String, EmulatorApp>()

        fun add(packageName: String) {
            if (found.containsKey(packageName)) return
            val info = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull() ?: return
            if (pm.getLaunchIntentForPackage(packageName) == null) return
            found[packageName] = EmulatorApp(packageName, pm.getApplicationLabel(info).toString())
        }

        system.knownPackages.forEach(::add)
        if (system.retroCore != null) RETRO_ARCH_PACKAGES.forEach(::add)

        // Anything else that registers for this kind of file.
        val probe = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://example/rom.${system.extensions.first()}"), "*/*")
        }
        runCatching { pm.queryIntentActivities(probe, 0) }.getOrDefault(emptyList()).forEach { resolved ->
            val packageName = resolved.activityInfo?.packageName ?: return@forEach
            if (looksLikeEmulator(packageName, resolved.loadLabel(pm).toString())) add(packageName)
        }
        return found.values.toList()
    }

    /** Every emulator-ish app installed, whichever system it serves. */
    fun installedEmulators(context: Context): List<EmulatorApp> {
        val pm = context.packageManager
        val known = (systems.flatMap { it.knownPackages } + RETRO_ARCH_PACKAGES).toSet()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching { pm.queryIntentActivities(launcher, 0) }.getOrDefault(emptyList())
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm).toString()
                if (packageName in known || looksLikeEmulator(packageName, label)) {
                    EmulatorApp(packageName, label)
                } else null
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.US) }
    }

    fun isEmulator(packageName: String, label: String): Boolean =
        packageName in (systems.flatMap { it.knownPackages } + RETRO_ARCH_PACKAGES).toSet() ||
            looksLikeEmulator(packageName, label)

    private val EMULATOR_WORDS = listOf(
        "emulator", "emulation", "retroarch", "libretro", "dolphin", "ppsspp", "drastic",
        "citra", "yuzu", "epsxe", "fpse", "mupen", "snes9x", "melonds", "flycast", "redream",
        "nostalgia", "myboy", "my boy", "oldboy", "aethersx2", "eden", "duckstation",
    )

    private fun looksLikeEmulator(packageName: String, label: String): Boolean {
        val haystack = (packageName + " " + label).lowercase(Locale.US)
        return EMULATOR_WORDS.any { haystack.contains(it) }
    }

    /** The app's own name for a package, falling back to the package id. */
    fun labelFor(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun isRetroArch(packageName: String): Boolean = packageName in RETRO_ARCH_PACKAGES

    /** Where RetroArch keeps the core for a system, as its intent extras expect it. */
    fun retroCorePath(packageName: String, system: GameSystem): String? {
        val core = system.retroCore ?: return null
        return "/data/data/$packageName/cores/${core}_libretro_android.so"
    }
}
