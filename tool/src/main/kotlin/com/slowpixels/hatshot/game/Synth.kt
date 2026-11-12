package com.slowpixels.hatshot.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Sound effects, all made in code from an 8-bit sound chip's few waves. */
enum class Fx {
    Shot, Ricochet, Bonk, Burst, Clink, Ting, Oops, Ouch, Click, Reload, Loaded, Creak, Cock, Fuse, Star, Clear, GameOver, Menu,
    Draw, Pause, Crack, Spill, Till, Jingle, Boom, Coin, Water
}

/** One note on one channel, in ticks (48 to a beat). [midi] -1 is a rest. */
class NoteEv(val start: Int, val len: Int, val midi: Int, val vol: Int, val duty: Int, val gate: Int)

/** A drum hit: k kick, s snare, h hat, o open hat, c crash, t tom. */
class DrumEv(val start: Int, val kind: Char)

/**
 * A tune: a written melody, and the band generated from its chords (a bass style for the triangle, a harmony style
 * for the second square, a drum pattern for the noise). Everything here is original to this game.
 */
class Song(
    val name: String,
    val bpm: Int,
    val beats: Int,
    chords: String,
    lead: String,
    val bass: Bass,
    val harmony: Harmony,
    drums: String,
    fill: String = "",
    val loop: Boolean = true,
    val leadDuty: Int = 1,
    counter: String? = null,
    /** The bar it goes back to when it comes round again: past an intro that's only played once. */
    val loopFrom: Int = 0,
) {
    /** STRIDE: a ragtime left hand's low notes, on every beat, root and fifth by turns. */
    enum class Bass { ROOT8, BOUNCE, WALK, OOM, DRIVE, SYNCOP, HALF, WALTZ, STRIDE }
    enum class Harmony { PAD, ARP, STAB, ALBERTI, WALTZ, NONE }

    val bar = beats * 48
    val chords: List<List<IntArray>> = chords.split('|').map { b -> b.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map(::chord) }
    val total = this.chords.size * bar
    val lead: List<NoteEv> = Mml.parse(lead, defaultDuty = leadDuty, defaultVol = 11)
    val second: List<NoteEv> = if (counter != null) Mml.parse(counter, defaultDuty = 2, defaultVol = 6) else harmonyNotes()
    val tri: List<NoteEv> = bassNotes()
    val noise: List<DrumEv> = drumNotes(drums, fill)

    val leadLength = Mml.length(lead)

    private fun harmonyNotes(): List<NoteEv> {
        val out = ArrayList<NoteEv>()
        this.chords.forEachIndexed { b, list ->
            val each = bar / list.size
            list.forEachIndexed { k, ch ->
                val at = b * bar + k * each
                val tones = ch.map { it + 12 }
                when (harmony) {
                    Harmony.PAD -> {
                        val half = each / 2
                        out += NoteEv(at, half, tones[1] + 12, 5, 2, 8)
                        out += NoteEv(at + half, each - half, tones[2] + 12, 5, 2, 8)
                    }
                    Harmony.ARP -> {
                        val seq = listOf(tones[0], tones[1], tones[2], tones[0] + 12, tones[2], tones[1]).map { it + 24 }
                        var t = 0; var i = 0
                        while (t < each) { out += NoteEv(at + t, 12, seq[i % seq.size], 4, 2, 6); t += 12; i++ }
                    }
                    Harmony.STAB -> {
                        var t = 24
                        while (t < each) { out += NoteEv(at + t, 24, tones[1] + 12, 6, 1, 3); t += 48 }
                    }
                    Harmony.ALBERTI -> {
                        val seq = listOf(tones[0], tones[2], tones[1], tones[2]).map { it + 12 }
                        var t = 0; var i = 0
                        while (t < each) { out += NoteEv(at + t, 24, seq[i % 4], 5, 2, 6); t += 24; i++ }
                    }
                    Harmony.WALTZ -> {
                        var t = 48
                        while (t < each) { out += NoteEv(at + t, 48, tones[1] + 12, 5, 1, 4); t += 48 }
                    }
                    Harmony.NONE -> Unit
                }
            }
        }
        return out
    }

    private fun bassNotes(): List<NoteEv> {
        val out = ArrayList<NoteEv>()
        this.chords.forEachIndexed { b, list ->
            val each = bar / list.size
            list.forEachIndexed { k, ch ->
                val at = b * bar + k * each
                val root = ch[0] - 12
                val third = ch[1] - 12
                val fifth = ch[2] - 12
                val nextRoot = (this.chords.getOrNull(b + 1)?.first() ?: this.chords[0].first())[0] - 12
                fun n(t: Int, len: Int, m: Int, gate: Int = 7) { if (t < each) out += NoteEv(at + t, minOf(len, each - t), m, 15, 0, gate) }
                when (bass) {
                    Bass.ROOT8 -> for (t in 0 until each step 24) n(t, 24, root)
                    Bass.BOUNCE -> for ((i, t) in (0 until each step 24).withIndex()) n(t, 24, if (i % 2 == 0) root else root + 12)
                    Bass.WALK -> {
                        val steps = listOf(root, third, fifth, if (nextRoot > root) nextRoot - 1 else nextRoot + 1)
                        for ((i, t) in (0 until each step 48).withIndex()) n(t, 48, steps[i % 4])
                    }
                    Bass.OOM -> { n(0, each / 2, root, 8); n(each / 2, each - each / 2, fifth - 12, 8) }
                    Bass.DRIVE -> {
                        val pat = intArrayOf(0, 0, 12, 0, 0, 12, 7, 12)
                        for ((i, t) in (0 until each step 24).withIndex()) n(t, 24, root + pat[i % 8], 6)
                    }
                    Bass.SYNCOP -> { n(0, 36, root); n(36, 12, fifth); n(48, 36, root + 12); n(84, 12, fifth) ; if (each > 96) { n(96, 36, root); n(132, 12, fifth) } }
                    Bass.HALF -> { n(0, 96, root, 8); n(96, 96, fifth - 12, 8) }
                    Bass.WALTZ -> { n(0, 48, root); n(48, 48, fifth); n(96, 48, fifth) }
                    Bass.STRIDE -> for ((i, t) in (0 until each step 48).withIndex()) n(t, 24, if (i % 2 == 0) root else fifth, 6)
                }
            }
        }
        return out
    }

    private fun drumNotes(pattern: String, fill: String): List<DrumEv> {
        val out = ArrayList<DrumEv>()
        val p = pattern.filter { !it.isWhitespace() }
        val f = fill.filter { !it.isWhitespace() }
        if (p.isEmpty()) return out
        val step = bar / p.length
        for (b in this.chords.indices) {
            val pat = if (f.isNotEmpty() && b % 8 == 7) f else p
            val st = bar / pat.length
            pat.forEachIndexed { i, ch -> if (ch != '.') out += DrumEv(b * bar + i * st, ch) }
        }
        if (step <= 0) return emptyList()
        return out
    }

    companion object {
        private val NAMES = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11)

        /** "Am7", "F#", "Bb", "Gmaj7", "Bdim", "C6", "Dsus4": midi notes from C3 up (root, third, fifth, extra). */
        fun chord(name: String): IntArray {
            var i = 1
            var root = NAMES.getValue(name[0].toString())
            if (name.length > 1 && (name[1] == '#' || name[1] == 'b')) { root += if (name[1] == '#') 1 else -1; i = 2 }
            val q = name.substring(i)
            val base = 48 + (root + 12) % 12
            val iv = when (q) {
                "", "maj" -> intArrayOf(0, 4, 7)
                "m" -> intArrayOf(0, 3, 7)
                "7" -> intArrayOf(0, 4, 7, 10)
                "m7" -> intArrayOf(0, 3, 7, 10)
                "maj7" -> intArrayOf(0, 4, 7, 11)
                "6" -> intArrayOf(0, 4, 7, 9)
                "dim" -> intArrayOf(0, 3, 6)
                "sus4" -> intArrayOf(0, 5, 7)
                "aug" -> intArrayOf(0, 4, 8)
                else -> error("chord $name")
            }
            return IntArray(iv.size) { base + iv[it] }
        }
    }
}

/**
 * A small music macro language, as the old trackers wrote it: notes `c d e f g a b` with `+`/`#` sharp and `-` flat,
 * a length (1, 2, 4, 8, 16, 32) and dots; `r` a rest; `^4` ties more on; `o4` octave, `<` `>` down and up; `l8`
 * default length; `v10` volume; `@2` duty (12.5, 25, 50, 75%); `q6` gate in eighths; `[ ... ]3` repeats.
 */
object Mml {
    private val NOTES = mapOf('c' to 0, 'd' to 2, 'e' to 4, 'f' to 5, 'g' to 7, 'a' to 9, 'b' to 11)

    /** Writes out `[ ... ]n` repeats (nested is fine; no count means twice). */
    fun expand(text: String): String {
        val sb = StringBuilder()
        var k = 0
        while (k < text.length) {
            val ch = text[k]
            if (ch == '[') {
                var depth = 1
                var j = k + 1
                while (depth > 0) { if (text[j] == '[') depth++; if (text[j] == ']') depth--; j++ }
                val inner = expand(text.substring(k + 1, j - 1))
                var e = j
                while (e < text.length && text[e].isDigit()) e++
                repeat(if (e > j) text.substring(j, e).toInt() else 2) { sb.append(inner) }
                k = e
            } else { sb.append(ch); k++ }
        }
        return sb.toString()
    }

    fun parse(src: String, defaultDuty: Int = 1, defaultVol: Int = 10): List<NoteEv> {
        val text = expand(src.filter { !it.isWhitespace() && it != '|' })
        val out = ArrayList<NoteEv>()
        var t = 0
        var octave = 5
        var length = 24
        var vol = defaultVol
        var duty = defaultDuty
        var gate = 7
        var i = 0
        fun num(): Int? { val st = i; while (i < text.length && text[i].isDigit()) i++; return if (i > st) text.substring(st, i).toInt() else null }
        fun ln(): Int {
            val n = num()
            var l = if (n != null) 192 / n else length
            var add = l / 2
            while (i < text.length && text[i] == '.') { l += add; add /= 2; i++ }
            return l
        }
        while (i < text.length) {
            when (val ch = text[i++]) {
                in NOTES.keys -> {
                    var m = NOTES.getValue(ch)
                    while (i < text.length && (text[i] == '+' || text[i] == '#' || text[i] == '-')) { m += if (text[i] == '-') -1 else 1; i++ }
                    val l = ln()
                    out += NoteEv(t, l, 12 * (octave + 1) + m, vol, duty, gate)
                    t += l
                }
                'r' -> t += ln()
                '^' -> {
                    val l = ln()
                    out.lastOrNull()?.let { last -> out[out.size - 1] = NoteEv(last.start, last.len + l, last.midi, last.vol, last.duty, last.gate) }
                    t += l
                }
                'o' -> octave = num() ?: octave
                '<' -> octave--
                '>' -> octave++
                'l' -> { length = ln() }
                'v' -> vol = num() ?: vol
                '@' -> duty = num() ?: duty
                'q' -> gate = num() ?: gate
            }
        }
        return out
    }

    /** How long [src] lasts in ticks, rests at the end included. */
    fun length(src: String) = parse("$src c").last().start
}

/**
 * Plays a [Song] in real time: two square waves, a stepped triangle and a noise drum kit, mixed to 16-bit mono.
 * [render] fills a buffer and keeps its place between calls, so the tune can change speed or stop at any moment.
 */
class Tracker(private val rate: Int) {
    var song: Song? = null
        private set
    private var tick = 0.0
    var fast = false
    var done = false
        private set
    private val idx = IntArray(4)
    private val phase = DoubleArray(3)
    private var lfsr = 1
    private val noiseRnd = Random(7)

    /** The melody plays (true) or rests while the rest of the band keeps going: the piano player's been hit. */
    @Volatile var melody = true

    fun play(s: Song?, fast: Boolean = false) {
        if (s !== song) { song = s; tick = 0.0; idx.fill(0); done = false }
        this.fast = fast
    }

    private fun freq(m: Int) = 440.0 * 2.0.pow((m - 69) / 12.0)

    fun render(out: ShortArray, n: Int) {
        val s = song
        if (s == null || done) { out.fill(0, 0, n); return }
        val bpm = s.bpm * (if (fast) 1.25 else 1.0)
        val ticksPerSample = bpm * 48.0 / 60.0 / rate
        for (k in 0 until n) {
            if (tick >= s.total) {
                if (!s.loop) { done = true; for (j in k until n) out[j] = 0; return }
                tick = tick - s.total + s.loopFrom * s.bar; idx.fill(0)
            }
            var mix = 0.0
            // The melody keeps its place while it rests, so it comes back in where the tune has got to.
            val lead = pulse(s.lead, 0, 0.11)
            if (melody) mix += lead
            mix += pulse(s.second, 1, 0.08)
            mix += triangle(s.tri)
            mix += drums(s.noise)
            out[k] = (softClip(mix) * 32000).toInt().toShort()
            tick += ticksPerSample
        }
    }

    private fun current(list: List<NoteEv>, ch: Int): NoteEv? {
        var i = idx[ch]
        while (i < list.size && list[i].start + list[i].len <= tick) i++
        idx[ch] = i
        val e = list.getOrNull(i) ?: return null
        return if (tick >= e.start) e else null
    }

    private fun secondsIn(e: NoteEv, bpm: Double) = (tick - e.start) * 60.0 / (bpm * 48.0)

    private fun pulse(list: List<NoteEv>, ch: Int, level: Double): Double {
        val e = current(list, ch) ?: return 0.0
        if (e.midi < 0 || tick - e.start >= e.len * e.gate / 8.0) return 0.0
        val s = song!!
        val t = secondsIn(e, s.bpm.toDouble())
        val vib = if (t > 0.18) 1 + 0.0045 * sin(2 * PI * 5.5 * t) else 1.0
        phase[ch] = (phase[ch] + freq(e.midi) * vib / rate) % 1.0
        val duty = when (e.duty) { 0 -> 0.125; 1 -> 0.25; 2 -> 0.5; else -> 0.75 }
        val env = 0.55 + 0.45 * exp(-t / 0.12)
        val release = ((e.len * e.gate / 8.0 - (tick - e.start)) / 2.0).coerceAtMost(1.0)
        return (if (phase[ch] < duty) 1.0 else -1.0) * level * e.vol / 15.0 * env * release
    }

    private fun triangle(list: List<NoteEv>): Double {
        val e = current(list, 2) ?: return 0.0
        if (e.midi < 0 || tick - e.start >= e.len * e.gate / 8.0) return 0.0
        phase[2] = (phase[2] + freq(e.midi) / rate) % 1.0
        val tri = 1.0 - 4.0 * abs(phase[2] - 0.5)
        return (Math.round((tri + 1) * 7.5) / 7.5 - 1.0) * 0.2 * e.vol / 15.0
    }

    private fun drums(list: List<DrumEv>): Double {
        var i = idx[3]
        while (i + 1 < list.size && list[i + 1].start <= tick) i++
        idx[3] = i
        val e = list.getOrNull(i) ?: return 0.0
        if (tick < e.start) return 0.0
        val t = secondsIn(NoteEv(e.start, 0, 0, 0, 0, 0), song!!.bpm * (if (fast) 1.25 else 1.0))
        return Drums.sample(e.kind, t, this::noise)
    }

    private fun noise(): Double {
        val bit = (lfsr xor (lfsr shr 1)) and 1
        lfsr = (lfsr shr 1) or (bit shl 14)
        return if (lfsr and 1 == 0) 1.0 else -1.0
    }

    private fun softClip(x: Double) = if (x > 1) 1.0 else if (x < -1) -1.0 else x * (1.5 - 0.5 * x * x)
}

/** The drum kit, a sample at a time: [t] seconds since the hit. */
object Drums {
    fun sample(kind: Char, t: Double, noise: () -> Double): Double = when (kind) {
        'k' -> if (t > 0.12) 0.0 else sin(2 * PI * (110 * t - 220 * t * t)) * 0.32 * exp(-t / 0.05) + (if (t < 0.004) noise() * 0.08 else 0.0)
        's' -> if (t > 0.16) 0.0 else noise() * 0.16 * exp(-t / 0.05) + sin(2 * PI * 185 * t) * 0.08 * exp(-t / 0.03)
        'h' -> if (t > 0.03) 0.0 else noise() * 0.07 * exp(-t / 0.012)
        'o' -> if (t > 0.15) 0.0 else noise() * 0.06 * exp(-t / 0.05)
        'c' -> if (t > 0.6) 0.0 else noise() * 0.09 * exp(-t / 0.2)
        't' -> if (t > 0.18) 0.0 else sin(2 * PI * (180 * t - 160 * t * t)) * 0.25 * exp(-t / 0.07)
        'g' -> if (t > 1.6) 0.0 else (sin(2 * PI * 98 * t) * 0.6 + sin(2 * PI * 147 * t) * 0.3 + sin(2 * PI * 233 * t) * 0.2) * 0.25 * exp(-t / 0.6)
        else -> 0.0
    }
}

/** The effects, rendered once to PCM at the phone's output rate (so they can take Android's fast audio path). */
object Sfx {
    private var rate = 44100

    private fun n(ms: Int) = rate * ms / 1000
    private fun f(m: Int) = 440.0 * 2.0.pow((m - 69) / 12.0)

    private fun square(out: FloatArray, at: Int, len: Int, f0: Double, f1: Double = f0, duty: Double = 0.5, vol: Float, end: Float = 0.2f) {
        var ph = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / len
            ph = (ph + (f0 + (f1 - f0) * t) / rate) % 1.0
            val k = at + i
            if (k in out.indices) out[k] += (if (ph < duty) 1f else -1f) * vol * (1f - (1f - end) * t.toFloat()) * edge(i, len)
        }
    }

    private fun tri(out: FloatArray, at: Int, len: Int, f0: Double, f1: Double = f0, vol: Float) {
        var ph = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / len
            ph = (ph + (f0 + (f1 - f0) * t) / rate) % 1.0
            val v = (Math.round((1.0 - 4.0 * abs(ph - 0.5) + 1) * 7.5) / 7.5 - 1.0).toFloat()
            val k = at + i
            if (k in out.indices) out[k] += v * vol * (1f - t.toFloat() * 0.5f) * edge(i, len)
        }
    }

    private fun noise(out: FloatArray, at: Int, len: Int, vol: Float, hold: Int = 1, seed: Int = 1) {
        val r = Random(seed)
        var v = 0f
        for (i in 0 until len) {
            if (i % hold == 0) v = if (r.nextBoolean()) 1f else -1f
            val k = at + i
            val e = 1f - i.toFloat() / len
            if (k in out.indices) out[k] += v * vol * e * e
        }
    }

    private fun edge(i: Int, len: Int): Float {
        val ramp = n(2).coerceAtMost(len / 2).coerceAtLeast(1)
        return when { i < ramp -> i.toFloat() / ramp; i > len - ramp -> (len - i).toFloat() / ramp; else -> 1f }
    }

    private fun seq(out: FloatArray, at: Int, notes: List<Int>, ms: Int, vol: Float, duty: Double = 0.5): Int {
        var p = at
        for (m in notes) { if (m >= 0) square(out, p, n(ms), f(m), duty = duty, vol = vol, end = 0.5f); p += n(ms) }
        return p
    }

    fun render(fx: Fx, sampleRate: Int): ShortArray {
        rate = sampleRate
        val out: FloatArray
        when (fx) {
            // A six-shooter on a sound chip: a burst of coarse noise over a falling thump.
            Fx.Shot -> { out = FloatArray(n(170)); noise(out, 0, n(150), 0.34f, 9, 2); tri(out, 0, n(110), 210.0, 55.0, 0.42f) }
            // The one that missed, singing off a rock.
            Fx.Ricochet -> { out = FloatArray(n(260)); square(out, n(40), n(210), 2600.0, 1100.0, 0.25, 0.05f, 0.1f) }
            Fx.Bonk -> { out = FloatArray(n(190)); tri(out, 0, n(120), 420.0, 150.0, 0.5f); square(out, n(40), n(120), 1000.0, 620.0, 0.5, 0.1f) }
            Fx.Burst -> { out = FloatArray(n(120)); noise(out, 0, n(70), 0.32f, 2, 7); square(out, 0, n(50), 1500.0, 500.0, 0.125, 0.12f) }
            // A shell going off like a dropped flowerpot: the crack, then two of the bits landing.
            Fx.Crack -> {
                out = FloatArray(n(380))
                noise(out, 0, n(95), 0.3f, 3, 11); tri(out, 0, n(130), 330.0, 85.0, 0.42f)
                square(out, n(170), n(32), f(98), duty = 0.125, vol = 0.07f); square(out, n(250), n(28), f(103), duty = 0.125, vol = 0.06f)
                noise(out, n(170), n(22), 0.07f, 1, 5); noise(out, n(250), n(18), 0.05f, 1, 7)
            }
            Fx.Clink -> { out = FloatArray(n(240)); square(out, 0, n(60), f(96), duty = 0.25, vol = 0.1f); square(out, n(45), n(160), f(100), duty = 0.125, vol = 0.09f); noise(out, 0, n(50), 0.1f, 1, 3) }
            Fx.Ting -> { out = FloatArray(n(320)); square(out, 0, out.size, f(91), duty = 0.125, vol = 0.14f, end = 0.05f); tri(out, 0, n(70), 300.0, 200.0, 0.3f) }
            Fx.Oops -> { out = FloatArray(n(420)); seq(out, 0, listOf(70, 66, 62), 120, 0.14f, 0.25) ; tri(out, 0, n(360), 180.0, 90.0, 0.3f) }
            Fx.Ouch -> { out = FloatArray(n(300)); noise(out, 0, n(90), 0.26f, 5, 4); square(out, 0, n(260), 500.0, 120.0, 0.5, 0.16f) }
            Fx.Click -> { out = FloatArray(n(45)); square(out, 0, n(18), 1800.0, 1500.0, 0.5, 0.1f); noise(out, 0, n(12), 0.1f, 1, 8) }
            // The cylinder going round.
            Fx.Reload -> { out = FloatArray(n(320)); var p = 0; repeat(6) { square(out, p, n(14), 1300.0, 1000.0, 0.5, 0.07f); noise(out, p, n(10), 0.06f, 1, it); p += n(52) } }
            // The cylinder snapping shut.
            Fx.Loaded -> { out = FloatArray(n(70)); square(out, 0, n(24), 900.0, 500.0, 0.5, 0.13f); noise(out, 0, n(16), 0.1f, 2, 5) }
            // A shutter or door about to show someone: the ear gets the news before the panel does.
            Fx.Creak -> { out = FloatArray(n(130)); square(out, 0, out.size, 240.0, 330.0, 0.125, 0.055f, 0.5f) }
            Fx.Cock -> { out = FloatArray(n(120)); square(out, 0, n(30), 1500.0, 1200.0, 0.5, 0.11f); square(out, n(70), n(30), 1000.0, 800.0, 0.5, 0.11f) }
            Fx.Fuse -> { out = FloatArray(n(460)); noise(out, 0, out.size, 0.1f, 1, 6); square(out, 0, n(240), 1400.0, 700.0, 0.125, 0.05f) }
            Fx.Star -> { out = FloatArray(n(520)); seq(out, 0, listOf(79, 84, 88, 91, 96), 80, 0.12f, 0.25) }
            Fx.Clear -> {
                out = FloatArray(n(2100))
                var p = 0
                for ((m, ms) in listOf(67 to 120, 72 to 120, 76 to 120, 79 to 240, 76 to 120, 79 to 700)) {
                    square(out, p, n(ms), f(m), duty = 0.5, vol = 0.14f, end = 0.6f); tri(out, p, n(ms), f(m - 24), vol = 0.3f); p += n(ms + 10)
                }
            }
            Fx.GameOver -> {
                out = FloatArray(n(2200))
                var p = 0
                for ((m, ms) in listOf(69 to 220, 68 to 220, 67 to 220, 66 to 420, 62 to 800)) {
                    square(out, p, n(ms), f(m), duty = 0.25, vol = 0.12f, end = 0.5f); tri(out, p, n(ms), f(m - 24), vol = 0.25f); p += n(ms)
                }
            }
            Fx.Menu -> { out = FloatArray(n(60)); square(out, 0, out.size, f(81), duty = 0.25, vol = 0.1f, end = 0.4f) }
            // The showdown's bell: one clear stroke, so you can draw by ear before the words arrive.
            Fx.Draw -> {
                out = FloatArray(n(600))
                for (i in out.indices) { val t = i.toDouble() / rate; out[i] = ((sin(2 * PI * 880 * t) * 0.6 + sin(2 * PI * 1320 * t) * 0.3 + sin(2 * PI * 2217 * t) * 0.15) * 0.3 * exp(-t / 0.18)).toFloat() }
            }
            Fx.Pause -> { out = FloatArray(n(160)); seq(out, 0, listOf(83, 79), 70, 0.12f) }
            // Dynamite or a crate of TNT going up: a hard crack on top of a long, low, falling rumble.
            Fx.Boom -> {
                out = FloatArray(n(760))
                noise(out, 0, n(110), 0.34f, 4, 5); noise(out, 0, n(520), 0.4f, 16, 3); noise(out, n(60), n(640), 0.2f, 28, 9)
                tri(out, 0, n(560), 150.0, 32.0, 0.55f)
            }
            // A coin: two bright notes, the second ringing on.
            Fx.Coin -> {
                out = FloatArray(n(380))
                square(out, 0, n(60), f(95), duty = 0.5, vol = 0.11f, end = 0.7f)
                square(out, n(60), n(320), f(100), duty = 0.5, vol = 0.11f, end = 0.02f)
            }
            // A sack holed: the thud of it, then the grain sifting out for as long as it pours, in little rushes.
            Fx.Spill -> {
                out = FloatArray(n(1500))
                tri(out, 0, n(70), 170.0, 70.0, 0.34f)
                val r = Random(9)
                var rush = 1f
                for (i in n(30) until out.size) {
                    val t = (i - n(30)).toFloat() / (out.size - n(30))
                    if (i % n(7) == 0) rush = 0.45f + r.nextFloat() * 0.55f
                    out[i] += (if (r.nextBoolean()) 1f else -1f) * 0.12f * rush * (1f - t) * minOf(1f, t * 30f)
                }
            }
            // Something holed that holds water: the knock, then it running out, glugging as it goes.
            Fx.Water -> {
                out = FloatArray(n(1900))
                tri(out, 0, n(90), 260.0, 90.0, 0.36f); noise(out, 0, n(60), 0.2f, 3, 4)
                val r = Random(21)
                // The run of it: soft, coarse noise that dies away.
                var v = 0f
                for (i in n(40) until out.size) {
                    val t = (i - n(40)).toFloat() / (out.size - n(40))
                    if (i % 5 == 0) v = if (r.nextBoolean()) 1f else -1f
                    out[i] += v * 0.06f * (1f - t)
                }
                // The glugs: short notes that slide up, close together at first, then fewer.
                var at = n(70)
                while (at < out.size - n(80)) {
                    val t = at.toFloat() / out.size
                    val len = n(28 + r.nextInt(30))
                    val f0 = 240.0 + r.nextInt(360)
                    var ph = 0.0
                    for (i in 0 until len) {
                        val u = i.toDouble() / len
                        ph += (f0 * (1.0 + 0.9 * u)) / rate
                        out[at + i] += (sin(2 * PI * ph) * 0.2 * (1f - t) * sin(PI * u)).toFloat()
                    }
                    at += len + n(20 + r.nextInt(60) + (t * 120).toInt())
                }
            }
            // Cha-ching: the till's drawer shooting out on its catch, twice, then its bell.
            Fx.Till -> {
                out = FloatArray(n(640))
                noise(out, 0, n(26), 0.2f, 3, 4); square(out, 0, n(30), 520.0, 380.0, 0.5, 0.1f)
                noise(out, n(75), n(24), 0.17f, 2, 6); square(out, n(75), n(28), 700.0, 520.0, 0.5, 0.09f)
                val at = n(150)
                for (i in at until out.size) {
                    val t = (i - at).toDouble() / rate
                    out[i] += ((sin(2 * PI * 2637 * t) * 0.55 + sin(2 * PI * 3951 * t) * 0.3 + sin(2 * PI * 5274 * t) * 0.15) * 0.3 * exp(-t / 0.15)).toFloat()
                }
            }
            // A money bag going off: the pop, and coins ringing as they scatter.
            Fx.Jingle -> {
                out = FloatArray(n(520))
                noise(out, 0, n(70), 0.3f, 2, 7); tri(out, 0, n(90), 300.0, 90.0, 0.3f)
                var p = n(60)
                for ((k, m) in listOf(100, 104, 98, 105, 101, 107, 103).withIndex()) {
                    square(out, p, n(46), f(m), duty = 0.125, vol = 0.075f - k * 0.006f, end = 0.1f); p += n(52 + k * 4)
                }
            }
        }
        return ShortArray(out.size) { (out[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
    }

    fun wav(pcm: ShortArray, sampleRate: Int): ByteArray {
        val data = pcm.size * 2
        val b = java.nio.ByteBuffer.allocate(44 + data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(data)
        for (s in pcm) b.putShort(s)
        return b.array()
    }
}
