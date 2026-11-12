package com.slowpixels.hatshot.audio

import com.thelightphone.sdk.audio.LightAudio
import com.thelightphone.sdk.audio.LightAudioItem
import com.thelightphone.sdk.audio.LightAudioPlayer
import com.thelightphone.sdk.audio.LightAudioSource
import com.thelightphone.sdk.audio.LightMediaMetadata
import com.thelightphone.sdk.audio.LightPlayerSpec
import java.io.DataOutputStream
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Audio focus for the game's music. A tool can't ask Android for focus, but Light's player takes it while it plays. So
 * while a stage's music plays, an ordinary player plays a file of silence: a podcast on the phone pauses for the game
 * rather than talking over it, and when the phone takes the sound away for a call the player stops by itself and
 * [onLost] pauses the game. With the game's music switched off nothing is taken, and a podcast plays on under the
 * shots. (Carried over from Four Track, where it was proved.)
 */
class Holder(private val scope: CoroutineScope) {
    private var player: LightAudioPlayer? = null
    private var owner: Any? = null
    private var watching: Job? = null
    @Volatile private var want = false
    /** Set while a stop of our own is on its way, so it isn't taken for the phone's. */
    @Volatile private var stopping = false
    /** Whoever is on screen and wants to know the phone took the sound away. */
    @Volatile var onLost: (() -> Unit)? = null

    /**
     * A game screen is up: make the player, with the silence ready in its queue. A player lives with the screen that
     * made it, so another screen's is let go first.
     */
    fun attach(by: Any, audio: LightAudio, filesDir: File) {
        if (player != null && owner === by) return
        release()
        val p = runCatching { audio.newPlayer(LightPlayerSpec()) }.getOrNull() ?: return
        player = p
        owner = by
        watching = scope.launch {
            runCatching {
                if (!p.awaitReady()) return@launch
                val silence = withContext(Dispatchers.IO) { silence(filesDir) }
                p.setMediaQueue(List(QUEUE) { item(silence, it) })
                var was = false
                p.isPlaying.collect { playing ->
                    // Stopped by the phone, not by us: the game pauses too. Started by the phone (a call ended): no.
                    if (!playing && was && want && !stopping) { want = false; onLost?.invoke() }
                    if (!playing) stopping = false
                    if (playing && !want) p.pause()
                    was = playing
                }
            }
        }
    }

    /** The game screen [by] is gone (or, with nobody named, whoever has it). */
    fun release(by: Any? = null) {
        if (by != null && owner !== by) return
        owner = null
        stopping = false
        want = false
        watching?.cancel()
        watching = null
        runCatching { player?.release() }
        player = null
    }

    /** Music is starting: the silence starts over from the top, for up to half an hour. */
    fun take() {
        if (want) return
        want = true
        scope.launch { runCatching { player?.let { stopping = it.isPlaying.value; it.stop(); stopping = false; it.play() } } }
    }

    /** Stopped, not paused: Light's player only lets the focus go when it stops. */
    fun drop() {
        if (!want) return
        want = false
        scope.launch { runCatching { player?.let { stopping = it.isPlaying.value; it.stop() } } }
    }

    companion object {
        private const val RATE = 8_000
        private const val SECONDS = 60
        private const val QUEUE = 30

        /** A minute of 8-bit silence, written once: 480 KB. */
        fun silence(dir: File): File {
            val file = File(dir, "silence.wav")
            val data = RATE * SECONDS
            if (file.length() == 44L + data) return file
            DataOutputStream(file.outputStream().buffered()).use { out ->
                fun text(s: String) = out.writeBytes(s)
                fun int(v: Int) = out.writeInt(Integer.reverseBytes(v))
                fun short(v: Int) = out.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
                text("RIFF"); int(36 + data); text("WAVE")
                text("fmt "); int(16); short(1); short(1); int(RATE); int(RATE); short(1); short(8)
                text("data"); int(data)
                val quiet = ByteArray(RATE) { 0x80.toByte() }
                repeat(SECONDS) { out.write(quiet) }
            }
            return file
        }

        private fun item(file: File, n: Int) = LightAudioItem(
            source = LightAudioSource.FileSource(file),
            metadata = LightMediaMetadata(title = "Hatshot"),
            id = "silence-$n",
        )
    }
}
