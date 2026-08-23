package com.mediavault.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.core.graphics.drawable.toBitmap

/**
 * Real artwork for library covers: a video's own frame, an album's art. Falls back to null
 * so the caller can draw the gradient placeholder instead.
 */
object Thumbnails {

    const val APP_ICON_SCHEME = "appicon://"

    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val misses = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun cached(uri: String?): Bitmap? = uri?.let { cache.get(it) }

    fun isKnownMiss(uri: String?): Boolean = uri != null && misses.contains(uri)

    fun load(context: Context, uri: String, width: Int, height: Int): Bitmap? {
        cache.get(uri)?.let { return it }
        if (misses.contains(uri)) return null

        if (uri.startsWith(APP_ICON_SCHEME)) {
            val icon = appIcon(context, uri.removePrefix(APP_ICON_SCHEME))
            if (icon == null) misses.add(uri) else cache.put(uri, icon)
            return icon
        }

        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
        val bitmap = decodeImageFile(parsed, width)
            ?: loadThumbnail(context, parsed, width, height)
            ?: loadAlbumArt(context, parsed)
            ?: extractFrame(context, parsed)

        if (bitmap == null) misses.add(uri) else cache.put(uri, bitmap)
        return bitmap
    }

    private fun appIcon(context: Context, packageName: String): Bitmap? = runCatching {
        context.packageManager.getApplicationIcon(packageName).toBitmap(192, 192)
    }.getOrNull()

    /** Box art sitting next to a ROM is a plain image file, not a media-store entry. */
    private fun decodeImageFile(uri: Uri, targetWidth: Int): Bitmap? {
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        if (path.substringAfterLast('.', "").lowercase(java.util.Locale.US) !in IMAGE_EXTENSIONS) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > targetWidth * 2 && sample < 16) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

    private fun loadThumbnail(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (uri.scheme != "content") return null
        return runCatching {
            context.contentResolver.loadThumbnail(uri, Size(width, height), null)
        }.getOrNull()
    }

    /** Pre-Q album art lives behind content://media/external/audio/albumart/<id>. */
    private fun loadAlbumArt(context: Context, uri: Uri): Bitmap? {
        if (!uri.toString().contains("albumart")) return null
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 2 })
            }
        }.getOrNull()
    }

    private fun extractFrame(context: Context, uri: Uri): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } ?: retriever.getFrameAtTime(2_000_000)
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()
}
