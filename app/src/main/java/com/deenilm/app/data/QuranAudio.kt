package com.deenilm.app.data

import android.media.MediaPlayer

/**
 * Single shared player for Quran audio (tilawat + Urdu translation).
 * Uses the platform MediaPlayer — no extra dependencies.
 * Always stops/releases the previous stream before starting a new one.
 */
object QuranAudio {
    private var player: MediaPlayer? = null
    private var currentUrl: String? = null

    /** True while [url] is the loaded stream and actually playing. */
    fun isPlaying(url: String): Boolean =
        currentUrl == url && player?.isPlaying == true

    fun isPlayingAny(): Boolean = player?.isPlaying == true

    /**
     * Plays [url]. [onPrepared] fires when playback actually starts,
     * [onDone] when the stream completes, [onError] with a user-facing message.
     * Callbacks run on the thread that created the player (call from the main thread).
     */
    fun play(
        url: String,
        onPrepared: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        stop()
        try {
            val mp = MediaPlayer()
            player = mp
            currentUrl = url
            mp.setDataSource(url)
            mp.setOnPreparedListener { it.start(); onPrepared() }
            mp.setOnCompletionListener { stop(); onDone() }
            mp.setOnErrorListener { _, _, _ ->
                stop()
                onError("Audio unavailable — check your connection and try again.")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stop()
            onError("Audio unavailable — check your connection and try again.")
        }
    }

    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        currentUrl = null
    }
}
