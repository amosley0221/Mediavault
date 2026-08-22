package com.mediavault.app.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/**
 * Drives the mini now-playing bar. Scanned tracks really play; the demo catalogue has no
 * file behind it, so those just hold the bar in a playing state.
 */
class AudioController(private val context: Context) {

    private var player: MediaPlayer? = null

    fun play(uri: String?): Boolean {
        release()
        val source = uri ?: return false
        return runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, Uri.parse(source))
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
            true
        }.getOrElse {
            release()
            false
        }
    }

    fun setPlaying(playing: Boolean) {
        val p = player ?: return
        runCatching { if (playing) p.start() else p.pause() }
    }

    fun release() {
        runCatching { player?.release() }
        player = null
    }
}
