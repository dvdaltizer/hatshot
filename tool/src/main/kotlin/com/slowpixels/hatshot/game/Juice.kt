package com.slowpixels.hatshot.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * What the Light Phone's screen can do that the Kompakt's couldn't: things flying. A hat shot off hangs a moment, then
 * goes up turning over and falls away; a shell goes off in pieces that arc and drop; bottles shatter, cans hop off the
 * shelf, balloons pop in a ring; the screen shakes when you're hit. Everything still moves a whole game pixel at a
 * time and turns a quarter at a time, so it stays 8-bit.
 *
 * Positions are game pixels, times milliseconds, speeds game pixels a millisecond.
 */
class Juice(seed: Long = System.nanoTime()) {
    private val rnd = Random(seed)

    /** A piece of something, flying. */
    class Bit(
        val sprite: Sprite, var x: Float, var y: Float, var vx: Float, var vy: Float,
        /** Quarter turns a second, either way; 0 keeps it upright. */
        val spin: Float, var life: Int,
        val gravity: Float = GRAVITY,
        /** Held still this long before it starts to move. */
        var hold: Int = 0,
        /** A spark off a miss: it goes out sooner. */
        val faint: Boolean = false,
        /** A whole thing knocked flying (a hat, a can): drawn cut out of paper, as it was before it flew. */
        val whole: Boolean = false,
        /** Drawn in the page's own ink, as lines out in the open are: a note of music in the air. */
        val inked: Boolean = false,
        /** Drawn over the top bar too: a hat leaving the top bar. */
        val anywhere: Boolean = false,
        val flip: Boolean = false,
        /** Cloth drawn in ink, as an outlaw's hat is. */
        var age: Int = 0,
    )

    /** A ring opening out: a balloon popping, a bullseye's middle. */
    class Ring(val x: Int, val y: Int, val to: Int, var ms: Int, val total: Int = ms)

    val bits = ArrayList<Bit>()
    val rings = ArrayList<Ring>()

    /** While above zero, the scene shakes, by [shakeBy] game pixels at most. */
    var shaking = 0
        private set
    private var shakeBy = 0
    /** While above zero, the edge of the scene flashes: you've been hit. */
    var flashing = 0
        private set

    fun tick(ms: Int) {
        for (i in bits.indices.reversed()) {
            val b = bits[i]
            if (b.hold > 0) { b.hold -= ms; continue }
            b.age += ms
            b.vy += b.gravity * ms
            b.x += b.vx * ms
            b.y += b.vy * ms
            b.life -= ms
            if (b.life <= 0 || b.y > G.H + 20) bits.removeAt(i)
        }
        for (i in rings.indices.reversed()) {
            rings[i].ms -= ms
            if (rings[i].ms <= 0) rings.removeAt(i)
        }
        if (shaking > 0) shaking -= ms
        if (flashing > 0) flashing -= ms
    }

    /** How far the scene is pushed this frame. */
    fun shake(): Pair<Int, Int> {
        if (shaking <= 0) return 0 to 0
        val k = (shakeBy * shaking / SHAKE_MS.toFloat()).roundToInt().coerceAtLeast(1)
        return rnd.nextInt(-k, k + 1) to rnd.nextInt(-k, k + 1)
    }

    // ---- What happens. ----

    /**
     * A hat shot off: it hangs where it was for a moment (its owner is already inside the shell, as in a cartoon),
     * then goes up turning over and falls.
     */
    fun hatOff(hat: Sprite, x: Int, y: Int, fromLeft: Boolean) {
        val dir = if (fromLeft) 1 else -1
        // Over the bars too: a hat from an upstairs window goes up past the score and comes down again.
        bits += Bit(hat, x.toFloat(), y.toFloat(), dir * (0.02f + rnd.nextFloat() * 0.03f), -0.19f,
            spin = dir * 7f, life = 2600, hold = HANG_MS, anywhere = true, whole = true)
    }

    /** One of your own hats leaves the top bar: you've been hit. */
    fun hatLost(hat: Sprite, x: Int, y: Int) {
        bits += Bit(hat, x.toFloat(), y.toFloat(), -0.03f, -0.12f, spin = -6f, life = 1800, anywhere = true)
    }

    /**
     * Blows [s], drawn at ([x], [y]), into pieces [cell] game pixels square that fly out from its middle, as hard as
     * [force], and fall. [lift] throws them up as well as out.
     */
    fun shatter(s: Sprite, x: Int, y: Int, cell: Int = 4, force: Float = 0.09f, lift: Float = 0.08f,
                gravity: Float = GRAVITY, life: Int = 1100, faint: Boolean = false) {
        val cx = s.w / 2f; val cy = s.h / 2f
        var j = 0
        while (j < s.h) {
            var i = 0
            while (i < s.w) {
                val piece = s.cut(i, j, cell, cell)
                if (!piece.blank) {
                    val dx = i + cell / 2f - cx; val dy = j + cell / 2f - cy
                    val d = maxOf(1f, kotlin.math.sqrt(dx * dx + dy * dy))
                    val f = force * (0.6f + rnd.nextFloat() * 0.8f)
                    bits += Bit(piece, (x + i).toFloat(), (y + j).toFloat(), dx / d * f, dy / d * f - lift * (0.6f + rnd.nextFloat()),
                        spin = (rnd.nextFloat() - 0.5f) * 10f, life = life + rnd.nextInt(300), gravity = gravity, faint = faint)
                }
                i += cell
            }
            j += cell
        }
    }

    /** Something knocked flying whole, turning over: a can off the shelf, a coin. */
    fun hop(s: Sprite, x: Int, y: Int, toward: Int) {
        val dir = if (toward >= x + s.w / 2) -1 else 1
        bits += Bit(s, x.toFloat(), y.toFloat(), dir * (0.04f + rnd.nextFloat() * 0.03f), -0.2f - rnd.nextFloat() * 0.05f,
            spin = dir * 9f, life = 1800, whole = true)
    }

    /** A note of music floating up from ([x], [y]) and away, swaying a little, as a piano plays. */
    fun note(s: Sprite, x: Int, y: Int) {
        bits += Bit(s, x.toFloat(), y.toFloat(), (rnd.nextFloat() - 0.3f) * 0.012f, -0.018f - rnd.nextFloat() * 0.008f,
            spin = 0f, life = 1500 + rnd.nextInt(400), gravity = 0f, inked = true)
    }

    /** A spent case thrown up out of the bottom bar as a shot goes off, turning over as it falls back. */
    fun casing(x: Int, y: Int) {
        bits += Bit(CASE, x.toFloat(), y.toFloat(), 0.025f + rnd.nextFloat() * 0.035f, -0.17f - rnd.nextFloat() * 0.05f,
            spin = 12f, life = 1100, anywhere = true)
    }

    /** A few single pixels thrown out from ([x], [y]): sparks, glints, glass. */
    fun sparks(x: Int, y: Int, n: Int = 8, force: Float = 0.12f, faint: Boolean = false, gravity: Float = GRAVITY) {
        for (k in 0 until n) {
            val a = 2 * PI * k / n + rnd.nextDouble() * 0.5
            val f = force * (0.5f + rnd.nextFloat())
            bits += Bit(DOT, x.toFloat(), y.toFloat(), (cos(a) * f).toFloat(), (sin(a) * f).toFloat() - 0.05f,
                spin = 0f, life = 500 + rnd.nextInt(300), gravity = gravity, faint = faint)
        }
    }

    fun ring(x: Int, y: Int, to: Int = 14, ms: Int = 260) { rings += Ring(x, y, to, ms) }

    /**
     * Money thrown up and out from ([x], [y]): [coins] coins that arc and drop, and [bills] notes that turn over and
     * come down slower, as paper does.
     */
    fun money(x: Int, y: Int, coins: Int, bills: Int, force: Float = 0.1f) {
        for (k in 0 until coins) {
            val a = -PI / 2 + (rnd.nextDouble() - 0.5) * 2.4
            val f = force * (0.6f + rnd.nextFloat() * 0.9f)
            bits += Bit(COIN, x - 2f, y - 2f, (cos(a) * f).toFloat(), (sin(a) * f).toFloat() - 0.05f,
                spin = 0f, life = 1500 + rnd.nextInt(300), inked = true, hold = k * 25)
        }
        for (k in 0 until bills) {
            val dir = if (rnd.nextBoolean()) 1 else -1
            bits += Bit(BILL, x - 4f, y - 3f, dir * (0.015f + rnd.nextFloat() * 0.04f), -0.1f - rnd.nextFloat() * 0.06f,
                spin = dir * 5f, life = 2400 + rnd.nextInt(400), gravity = GRAVITY * 0.3f, inked = true, hold = 40 + k * 60)
        }
    }

    /** You're hit: the scene shakes and its edge flashes. */
    fun hit(strength: Int = 3) {
        shaking = SHAKE_MS; shakeBy = strength
        flashing = FLASH_MS
    }

    /** A smaller shake, for a word that lands: DRAW! */
    fun thump() { shaking = SHAKE_MS / 2; shakeBy = 2 }

    // ---- Drawing. ----

    fun draw(f: Frame, scene: Boolean) {
        for (r in rings) {
            val p = 1f - r.ms.toFloat() / r.total
            val rad = (2 + (r.to - 2) * p).roundToInt()
            if (p >= 0.7f) continue
            val c = f.look.ink
            // A ring drawn a dot at a time, stepped so it stays 8-bit.
            val n = maxOf(8, rad * 6)
            for (k in 0 until n) {
                val a = 2 * PI * k / n
                f.dot(r.x + (cos(a) * rad).roundToInt(), r.y + (sin(a) * rad).roundToInt(), c)
            }
        }
        for (b in bits) {
            if (scene == b.anywhere) continue
            val turns = if (b.spin == 0f) 0 else (b.age * b.spin / 1000f).toInt()
            val s = b.sprite.turned(turns)
            // A turned sprite turns about its middle.
            val ox = (b.sprite.w - s.w) / 2; val oy = (b.sprite.h - s.h) / 2
            val x = b.x.roundToInt() + ox; val y = b.y.roundToInt() + oy
            when {
                b.faint && b.age > 260 -> Unit
                b.whole -> f.cut(s, x, y, b.flip)
                // Pieces of something that was black on white stay black on white; sparks and cases are the page's ink.
                s === DOT || s === CASE || b.inked -> f.put(s, x, y, b.flip)
                else -> f.put(s, x, y, b.flip, ink = BLACK, paper = WHITE)
            }
        }
    }

    /** The edge of the scene, lit while you're being hit. */
    fun drawFlash(f: Frame) {
        if (flashing <= 0) return
        val t = 2
        f.box(0, G.TOP, G.W, t, f.look.ink); f.box(0, G.BAR - t, G.W, t, f.look.ink)
        f.box(0, G.TOP, t, G.BAR - G.TOP, f.look.ink); f.box(G.W - t, G.TOP, t, G.BAR - G.TOP, f.look.ink)
    }

    fun clear() { bits.clear(); rings.clear(); shaking = 0; flashing = 0 }

    companion object {
        /** Game pixels a millisecond, every millisecond: about what a dropped hat would do, at this scale. */
        const val GRAVITY = 0.00055f
        const val HANG_MS = 170
        const val SHAKE_MS = 280
        const val FLASH_MS = 110
        val DOT = Sprite(1, 1, intArrayOf(INK))
        /** A spent case: two game pixels by four. */
        val CASE = Sprite(2, 4, IntArray(8) { INK })
        /** A coin and a banknote, as small as they can be and still be told apart. */
        val COIN = Sprite.of(art("""
            .###.
            #ooo#
            #o#o#
            #ooo#
            .###.
        """))
        val BILL = Sprite.of(art("""
            #########
            #ooooooo#
            #oo###oo#
            #ooooooo#
            #########
        """))
    }
}
