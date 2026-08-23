package com.mediavault.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.Locale

/** Real games on the phone: anything launchable that declares itself a game. */
object InstalledApps {

    fun games(context: Context): List<MediaItem> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { pm.queryIntentActivities(launcher, 0) }.getOrDefault(emptyList())
        return resolved.asSequence()
            .mapNotNull { info ->
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                if (!isGame(appInfo)) return@mapNotNull null
                val label = info.loadLabel(pm).toString()
                MediaItem(
                    id = "app-${appInfo.packageName}",
                    title = label,
                    sub = sourceLabel(pm, appInfo.packageName),
                    letter = initials(label),
                    gradient = DeviceMedia.gradientFor(appInfo.packageName),
                    tag = "GAME",
                    source = "Installed app",
                    category = Category.GAMES,
                    packageName = appInfo.packageName,
                    artUri = Thumbnails.APP_ICON_SCHEME + appInfo.packageName,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.title.lowercase(Locale.US) }
            .toList()
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
            "com.android.vending" -> "Play Store"
            null -> "Sideloaded"
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
