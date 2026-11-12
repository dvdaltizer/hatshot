package com.slowpixels.hatshot.game

import kotlin.random.Random

/**
 * The showdown: three quick-draws against the gang's leaders. Wait for the word, then tap. Each duel gives you less
 * time, and tapping before the word is a foul. With [tricks], while you wait the word now and then slams in as one
 * that only looks like DRAW (DRAG!, DRAT!...): firing at it is a foul too, and each leader tries it more often than
 * the last. The tell, for a careful eye: an outlaw only raises his gun for the real DRAW.
 */
class Duel(val pace: Pace, private val ev: DuelEvents, seed: Long = System.nanoTime(), private val tricks: Boolean = true) {
    private val rnd = Random(seed)
    enum class Phase { STEADY, DRAW, WON, BEATEN, FOUL, DONE }

    var phase = Phase.STEADY
        private set
    var hearts = HEARTS
        private set
    var wins = 0
        private set
    /**
     * How many times the last leader, Black Shell Hank, has been beaten to the draw. It takes [BOSS_TAKES] to finish
     * him: the first costs him his hat, the second his bandana, the third his shell.
     */
    var bossHits = 0
        private set
    private val bossTakes = if (tricks) BOSS_TAKES else 1
    var word = "STEADY..."
        private set
    var score = 0
        private set
    /** Your last draw, in milliseconds. */
    var lastDraw = 0
        private set
    var bestDraw = 0
        private set
    var shots = 0
        private set

    private var ms = 0
    private var wait = 0
    /** The first duel hasn't begun: it opens with a quiet spell, so there's time to look at the street. */
    private var opening = true
    /** When, in this duel's wait, each fake word comes up: they're up for [FAKE_MS] each. */
    private var fakes = IntArray(0)
    private var fakeWords = emptyList<String>()
    /** A fake word is up right now: it looks like DRAW, and firing at it is a foul. */
    var faking = false
        private set
    /** How many fake words you've been shown, and how many fooled you. */
    var fakesShown = 0
        private set
    var fooled = 0
        private set
    /** How long the word has been up, while it is. */
    val drawnFor get() = if (phase == Phase.DRAW) ms else 0
    private val won get() = wins >= NEEDED

    init { setUp() }

    private fun setUp() {
        phase = Phase.STEADY
        word = "STEADY..."
        ms = 0
        faking = false
        // Hank, once he's been hit, is rattled: he's quicker back to it, and tries fewer tricks.
        val again = bossHits > 0
        val n = if (!tricks) 0 else if (again) rnd.nextInt(1, 3) else rnd.nextInt(FAKES_LEAST[wins.coerceAtMost(2)], FAKES_MOST[wins.coerceAtMost(2)] + 1)
        // Nothing at all happens for the first few seconds of the stage: no word, real or fake.
        val lead = if (opening) OPENING_MS else 0
        opening = false
        // Room for every fake, well apart, and a breath between the last of them and the real word.
        wait = lead + (if (again) rnd.nextInt(1200, 3000) else rnd.nextInt(1500, 3600 + wins * 900)) + n * (FAKE_MS + FAKE_GAP)
        fakes = IntArray(n) { k -> lead + 700 + k * (FAKE_MS + FAKE_GAP) + rnd.nextInt(0, FAKE_GAP / 2) }
        fakeWords = List(n) { FAKE_WORDS[rnd.nextInt(FAKE_WORDS.size)] }
    }

    private val window get() = ((WINDOWS.getOrElse(wins) { WINDOWS.last() }) * pace.factor).toInt()

    fun tick(step: Int) {
        ms += step
        when (phase) {
            Phase.STEADY -> if (ms >= wait) {
                ms = 0
                faking = false
                phase = Phase.DRAW
                word = "DRAW!"
                ev.fx(Fx.Draw)
                ev.changed()
            } else {
                // A fake word while it's up, slammed in like the real one; then back to waiting.
                val k = fakes.indexOfFirst { ms >= it && ms < it + FAKE_MS }
                val now = k >= 0
                if (now != faking) {
                    faking = now
                    word = if (now) fakeWords[k] else "STEADY..."
                    if (now) { fakesShown++; ev.fx(Fx.Cock) }
                    ev.changed()
                }
            }
            Phase.DRAW -> if (ms >= window) {
                ms = 0
                phase = Phase.BEATEN
                hearts--
                word = "TOO SLOW!"
                ev.fx(Fx.Ouch)
                ev.buzz()
                ev.changed()
            }
            Phase.WON, Phase.BEATEN, Phase.FOUL -> if (ms >= AFTER_MS) next()
            Phase.DONE -> Unit
        }
    }

    private fun next() {
        if (hearts <= 0 || won) { phase = Phase.DONE; ev.finished(); return }
        setUp()
        ev.changed()
    }

    fun shoot() {
        when (phase) {
            Phase.STEADY -> {
                shots++
                if (faking) fooled++
                word = if (faking) "FOOLED YA!" else "TOO SOON!"
                faking = false
                ms = 0
                phase = Phase.FOUL
                hearts--
                ev.fx(Fx.Oops)
                ev.buzz()
                ev.changed()
            }
            Phase.DRAW -> {
                shots++
                lastDraw = ms
                if (bestDraw == 0 || ms < bestDraw) bestDraw = ms
                // The last leader has to be beaten to the draw more than once; the others, and his last, are a win.
                val finishing = wins < NEEDED - 1 || bossHits + 1 >= bossTakes
                if (wins == NEEDED - 1) bossHits++
                if (finishing) wins++
                score += (if (finishing) 500 else 300) + maxOf(0, (window - ms)) * 2
                phase = Phase.WON
                word = "$ms MS"
                ms = 0
                ev.fx(Fx.Shot)
                ev.fx(Fx.Bonk)
                ev.changed()
            }
            else -> Unit
        }
    }

    val stars get() = when {
        !won -> 0
        hearts == HEARTS -> 3
        hearts == HEARTS - 1 -> 2
        else -> 1
    }
    val beatBoss get() = won

    /** What the next star would have taken, or null when there's none left to get. */
    val nextStar: String? get() = when (stars) {
        0 -> "Win all three duels for a star."
        1 -> "Next star: lose 1 hat at most."
        2 -> "Next star: lose no hats."
        else -> null
    }

    companion object {
        const val HEARTS = 3
        const val NEEDED = 3
        const val BOSS_TAKES = 3
        private const val AFTER_MS = 1700
        /**
         * How long the first duel holds off before anything can come up. With the least wait after it, the first word
         * of the stage, real or fake, is never sooner than [FIRST_WORD_MS].
         */
        private const val OPENING_MS = 2500
        const val FIRST_WORD_MS = OPENING_MS + 700
        /** How long you get to fire once DRAW is up, in each duel: Black Shell Hank, last, is quickest. */
        private val WINDOWS = intArrayOf(1000, 820, 660)
        /** How many fake words each leader tries, at least and at most. */
        private val FAKES_LEAST = intArrayOf(0, 1, 2)
        private val FAKES_MOST = intArrayOf(1, 2, 3)
        /** How long a fake word stays up, and the quiet after it. */
        const val FAKE_MS = 650
        private const val FAKE_GAP = 650
        /** Words that look like DRAW at a glance. */
        val FAKE_WORDS = listOf("DRAG!", "DRAT!", "DROP!", "DRUM!", "DREW!", "DRAWL!")
    }
}

interface DuelEvents {
    fun changed()
    fun fx(fx: Fx)
    fun buzz()
    fun finished()
}
