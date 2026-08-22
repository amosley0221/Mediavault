package com.mediavault.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

/** Deep links out to the apps that own the content MediaVault only indexes. */
object Launch {

    fun web(context: Context, url: String): Boolean = view(context, Uri.parse(url), null)

    fun search(context: Context, service: String, title: String): Boolean {
        val encoded = Uri.encode(title)
        val url = when (service.lowercase(Locale.US)) {
            "netflix" -> "https://www.netflix.com/search?q=$encoded"
            "hulu" -> "https://www.hulu.com/search?q=$encoded"
            "apple tv+", "apple tv" -> "https://tv.apple.com/search?term=$encoded"
            "youtube" -> "https://www.youtube.com/results?search_query=$encoded"
            "youtube tv" -> "https://tv.youtube.com/live"
            "plex" -> "https://app.plex.tv/desktop"
            "moonlight" -> "https://moonlight-stream.org"
            else -> "https://www.google.com/search?q=$encoded"
        }
        return web(context, url)
    }

    fun twitch(context: Context, channel: String): Boolean {
        val app = Intent(Intent.ACTION_VIEW, Uri.parse("twitch://stream/$channel"))
        return start(context, app) || web(context, "https://www.twitch.tv/$channel")
    }

    fun youtube(context: Context, query: String): Boolean =
        web(context, "https://www.youtube.com/results?search_query=${Uri.encode(query)}")

    fun youtubeTv(context: Context): Boolean = web(context, "https://tv.youtube.com/live")

    /** Hands a scanned document to whichever app owns that file type. */
    fun openDocument(context: Context, uri: String, mime: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(uri), mime ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, intent)
    }

    /** Escape hatch from the in-app player. */
    fun openVideoExternally(context: Context, uri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(uri), "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, intent)
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, intent)
    }

    private fun view(context: Context, uri: Uri, mime: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (mime != null) type = mime
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
