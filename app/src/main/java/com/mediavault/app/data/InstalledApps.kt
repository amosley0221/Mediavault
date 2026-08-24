package com.mediavault.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.Locale

/** One launchable app, as offered in the "add a game" picker. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val sourceLabel: String,
)

/**
 * Games installed on the phone. Anything Android flags as a game counts automatically;
 * anything else only counts when the user adds it by hand, because the flag is set by the
 * developer and plenty of games never set it.
 */
object InstalledApps {

    const val SOURCE_PLAY = "Play Store"
    const val SOURCE_SIDELOADED = "Sideloaded"
    const val SOURCE_MANUAL = "Added by hand"

    fun games(
        context: Context,
        manualPackages: Set<String> = emptySet(),
        playtime: Map<String, PlayStat> = emptyMap(),
    ): List<MediaItem> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { pm.queryIntentActivities(launcher, 0) }.getOrDefault(emptyList())

        return resolved.asSequence()
            .mapNotNull { info ->
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                val packageName = appInfo.packageName
                val label = info.loadLabel(pm).toString()
                val manual = packageName in manualPackages
                if (!manual) {
                    if (!isGame(appInfo)) return@mapNotNull null
                    // Emulators are not games — they belong to the ROMs, under Sources.
                    if (Emulators.isEmulator(packageName, label)) return@mapNotNull null
                }
                val stat = playtime[packageName]
                val installedFrom = sourceLabel(pm, packageName)
                MediaItem(
                    id = "app-$packageName",
                    title = label,
                    sub = listOfNotNull(
                        installedFrom,
                        stat?.totalMs?.takeIf { it > 0 }?.let { Playtime.format(it) + " played" },
                    ).joinToString(" · "),
                    letter = initials(label),
                    gradient = DeviceMedia.gradientFor(packageName),
                    // No tag on an installed game — the section it sits in already says so.
                    // ROMs keep theirs, because that pill names the console.
                    tag = null,
                    source = installedFrom,
                    category = Category.GAMES,
                    packageName = packageName,
                    playtimeMs = stat?.totalMs ?: 0L,
                    lastPlayed = stat?.lastUsed ?: 0L,
                    sourceId = if (manual) SOURCE_MANUAL else installedFrom,
                    artUri = Thumbnails.APP_ICON_SCHEME + packageName,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.title.lowercase(Locale.US) }
            .toList()
    }

    /** Everything launchable, for the picker — the scanner's misses live in here. */
    fun allApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching { pm.queryIntentActivities(launcher, 0) }.getOrDefault(emptyList())
            .mapNotNull { info ->
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                InstalledApp(
                    packageName = appInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    sourceLabel = sourceLabel(pm, appInfo.packageName),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.US) }
    }

    private fun isGame(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_GAME ||
            @Suppress("DEPRECATION") (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    private fun sourceLabel(pm: PackageManager, packageName: String): String {
        val installer = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION") pm.getInstallerPackageName(packageName)
            }
        }.getOrNull()
        return when (installer) {
            "com.android.vending" -> SOURCE_PLAY
            null -> SOURCE_SIDELOADED
            else -> installer.substringAfterLast('.').replaceFirstChar { it.titlecase(Locale.US) }
        }
    }

    private fun initials(label: String): String {
        val words = label.split(' ', ':').filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase(Locale.US)
            else -> (words[0].take(1) + words[1].take(1)).uppercase(Locale.US)
        }
    }
}
