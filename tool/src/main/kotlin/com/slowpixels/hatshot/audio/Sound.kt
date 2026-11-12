package com.slowpixels.hatshot.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import com.slowpixels.hatshot.game.Fx
import com.slowpixels.hatshot.game.Sfx
import com.slowpixels.hatshot.game.Song
import com.slowpixels.hatshot.game.Tracker
import com.slowpixels.hatshot.store.Save
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * The game's ears, carried over from the Kompakt. Music is synthesised live on a thread of its own (so it can stop and
 * start at once); effects are made once into small WAV files at the phone's own rate and played through a sound pool.
 * Nothing here needs Android's Context, which a Light Phone tool can't have.
 */
class Sound(dir: File, private val save: Save, version: Int) {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val musicAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attributes).build()
    private val ids = ConcurrentHashMap<Fx, Int>()
    private val effects = Executors.newSingleThreadExecutor { Thread(it, "effects").apply { isDaemon = true } }
    @Volatile private var released = false
    @Volatile private var paused = false

    private val rate = 32000
    private val tracker = Tracker(rate)
    private val lock = Object()
    @Volatile private var wanted: Song? = null
    @Volatile private var fast = false

    init {
        thread(name = "sfx", isDaemon = true) {
            runCatching {
                // Named by version, so a change to the sounds makes fresh files rather than playing stale ones.
                val sfx = File(dir, "sfx-$version").apply { mkdirs() }
                val native = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf { it > 0 } ?: 48000
                for (fx in Fx.entries) {
                    val f = File(sfx, "${fx.name}-$native.wav")
                    if (!f.exists()) {
                        val part = File(sfx, "${fx.name}.part")
                        part.writeBytes(Sfx.wav(Sfx.render(fx, native), native))
                        part.renameTo(f)
                    }
                    if (released) return@thread
                    ids[fx] = pool.load(f.path, 1)
                }
            }
        }
        thread(name = "music", isDaemon = true) { runCatching { musicLoop() } }
    }

    /** On a thread of its own, so a sound never holds up the frame that shows what made it. */
    fun play(fx: Fx) {
        if (!save.effects || paused) return
        val id = ids[fx] ?: return
        effects.execute { if (!released) pool.play(id, 1f, 1f, 1, 0, 1f) }
    }

    /** What to play now (null for silence). The same song again keeps its place; [fast] is the hurry-up tempo. */
    fun music(song: Song?, fast: Boolean = false) {
        synchronized(lock) {
            wanted = song
            this.fast = fast
            lock.notifyAll()
        }
    }

    /** The melody rests (false) while the beat goes on, or comes back in (true). */
    fun melody(on: Boolean) { tracker.melody = on }

    /** The tool is in the background: silence, without forgetting what was playing. */
    fun pause(p: Boolean) { synchronized(lock) { paused = p; lock.notifyAll() } }

    /** The music setting changed. */
    fun poke() { synchronized(lock) { lock.notifyAll() } }

    private fun musicLoop() {
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(musicAttributes)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(min, 4096) * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val buf = ShortArray(1024)
        var playing = false
        try {
            while (!released) {
                val song: Song?
                synchronized(lock) {
                    while (!released && (wanted == null || paused || !save.music)) {
                        if (playing) { track.pause(); track.flush(); playing = false }
                        lock.wait(500)
                    }
                    song = wanted
                }
                if (released || song == null) break
                tracker.play(song, fast)
                tracker.render(buf, buf.size)
                if (!playing) { track.play(); playing = true }
                track.write(buf, 0, buf.size)
                if (tracker.done) synchronized(lock) { if (wanted === song) wanted = null }
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    fun release() {
        released = true
        synchronized(lock) { lock.notifyAll() }
        effects.shutdown()
        pool.release()
    }
}
