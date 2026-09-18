package com.familiachat.app.data

import android.media.MediaPlayer

/** Toca um áudio a partir de uma URL remota (Firebase Storage), um de cada vez. */
class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var playingUrl: String? = null

    fun isPlaying(url: String): Boolean = playingUrl == url && mediaPlayer?.isPlaying == true

    fun toggle(url: String, onFinished: () -> Unit) {
        if (isPlaying(url)) {
            stop()
            return
        }
        stop()
        val player = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                stop()
                onFinished()
            }
            prepareAsync()
        }
        mediaPlayer = player
        playingUrl = url
    }

    fun stop() {
        mediaPlayer?.apply {
            try { if (isPlaying) stop() } catch (e: IllegalStateException) { /* já parado */ }
            release()
        }
        mediaPlayer = null
        playingUrl = null
    }
}
