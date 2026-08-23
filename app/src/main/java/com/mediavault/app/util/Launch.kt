package com.mediavault.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/** Hands files to the apps that own them, and deep-links out to streaming services. */
object Launch {

    fun web(context: Context, url: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun twitch(context: Context, channel: String): Boolean {
        val app = Intent(Intent.ACTION_VIEW, Uri.parse("twitch://stream/$channel"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, app) || web(context, "https://www.twitch.tv/$channel")
    }

    fun youtube(context: Context, query: String): Boolean =
        web(context, "https://www.youtube.com/results?search_query=${Uri.encode(query)}")

    /** Opens a document in whichever app claims its type. */
    fun openDocument(context: Context, uri: String, mime: String?): Boolean {
        val shareable = shareableUri(context, uri) ?: return false
        val type = mime ?: guessMime(uri) ?: "*/*"
        return viewFile(context, shareable, type)
    }

    /** Escape hatch from the built-in player. */
    fun openVideoExternally(context: Context, uri: String): Boolean {
        val shareable = shareableUri(context, uri) ?: return false
        return viewFile(context, shareable, guessMime(uri) ?: "video/*")
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, intent)
    }

    private fun viewFile(context: Context, uri: Uri, mime: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (start(context, intent)) return true
        // Some viewers only register for the generic type.
        val fallback = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, fallback)
    }

    /**
     * A `file://` uri cannot be handed to another app, so anything found by walking storage
     * is re-issued through this app's FileProvider.
     */
    private fun shareableUri(context: Context, uri: String): Uri? {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
        if (parsed.scheme != "file") return parsed
        val path = parsed.path ?: return null
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))
        }.getOrNull()
    }

    private fun guessMime(uri: String): String? {
        val extension = uri.substringAfterLast('.', "").lowercase(Locale.US).ifBlank { return null }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
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
