package com.mediavault.app.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.util.concurrent.TimeUnit

data class PlayStat(val totalMs: Long, val weekMs: Long, val lastUsed: Long)

/**
 * How long each game has been on screen, read from Android's usage stats.
 *
 * This needs the special "usage access" grant, which only the user can give from system
 * settings — without it every figure is simply zero and the app carries on.
 */
object Playtime {

    fun hasAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = runCatching {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }.getOrDefault(AppOpsManager.MODE_ERRORED)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Package name → time on screen, for everything the system still has records for. */
    fun collect(context: Context): Map<String, PlayStat> {
        if (!hasAccess(context)) return emptyMap()
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val now = System.currentTimeMillis()
        val yearAgo = now - TimeUnit.DAYS.toMillis(365)
        val weekAgo = now - TimeUnit.DAYS.toMillis(7)

        val total = runCatching {
            usage.queryAndAggregateUsageStats(yearAgo, now)
        }.getOrDefault(emptyMap())
        val week = runCatching {
            usage.queryAndAggregateUsageStats(weekAgo, now)
        }.getOrDefault(emptyMap())

        return total.mapValues { (packageName, stats) ->
            PlayStat(
                totalMs = stats.totalTimeInForeground,
                weekMs = week[packageName]?.totalTimeInForeground ?: 0L,
                lastUsed = stats.lastTimeUsed,
            )
        }
    }

    /** "118 h", "9.5 h", "42 m" — the shape the stats screen wants. */
    fun format(ms: Long): String = when {
        ms <= 0 -> "0 m"
        ms < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(ms)} m"
        ms < TimeUnit.HOURS.toMillis(10) -> {
            val hours = ms.toDouble() / TimeUnit.HOURS.toMillis(1)
            String.format(java.util.Locale.US, "%.1f h", hours)
        }
        else -> "${TimeUnit.MILLISECONDS.toHours(ms)} h"
    }

    fun formatLastPlayed(millis: Long): String? {
        if (millis <= 0) return null
        return java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            .format(java.util.Date(millis))
    }
}
