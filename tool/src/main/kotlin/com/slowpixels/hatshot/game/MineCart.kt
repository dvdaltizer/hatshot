package com.slowpixels.hatshot.game

import kotlin.math.roundToInt

/**
 * Buzzard Canyon's mine cart, seen head on, on the rails that run out of the tunnel toward you. It waits up the line in
 * the dark; when somebody's due out of [slot], the opening that is the cart, it rolls out of the tunnel with them
 * riding in it, growing as the rails widen. Once they've been dealt with (or have ducked back down) it rolls away up
 * the line again, and comes back with the next one.
 */
class MineCart(private val slot: Int) : Rolling {
    private enum class At { HERE, GOING, AWAY, READY, COMING }

    private var at = At.HERE
    private var ms = 0
    /** Somebody has been in it since it pulled up. */
    private var used = false
    private var rider: Target? = null
    private var waited = 0

    override fun tick(ms: Int) { waited += ms }
    override fun wave(n: Int) = Unit

    override fun see(round: Round) {
        val step = waited
        waited = 0
        ms += step
        rider = round.live.firstOrNull { it.slotIndex == slot && it.phase != Phase.GONE }
        val busy = rider != null
        when (at) {
            At.HERE -> {
                if (busy) { used = true; ms = 0 }
                // Off again a moment after its passenger's gone; or, if nobody ever got in, after a good wait.
                else if ((used && ms >= LINGER_MS) || ms >= IDLE_MS) go(At.GOING)
            }
            At.GOING -> if (ms >= ROLL_MS) go(At.AWAY)
            At.AWAY -> if (ms >= AWAY_MS) go(At.READY)
            // Somebody's got in, up in the tunnel: out it comes.
            At.READY -> if (busy) go(At.COMING)
            At.COMING -> {
                // The ride doesn't come out of the time they'll stay: that starts when the cart pulls up.
                rider?.let { if (it.phase == Phase.OUT) { it.ms = 0; it.seen = 0 } }
                if (ms >= ROLL_MS || !busy) { go(At.HERE); used = busy }
            }
        }
        val open = at == At.READY || at == At.COMING || (at == At.HERE && (busy || !used))
        round.shut = if (open) emptySet() else setOf(slot)
    }

    private fun go(to: At) { at = to; ms = 0 }

    /** How far down the line it is: 0 up in the tunnel, 1 pulled up in front of you. */
    private val near: Float get() = when (at) {
        At.HERE -> 1f
        At.AWAY, At.READY -> 0f
        At.GOING -> 1f - ease(ms.toFloat() / ROLL_MS)
        At.COMING -> ease(ms.toFloat() / ROLL_MS)
    }

    private fun ease(t: Float): Float { val u = t.coerceIn(0f, 1f); return u * u * (3 - 2 * u) }

    /** While it's rolling, whoever's in it is drawn with it, to its size; pulled up, they're drawn as anyone is. */
    override fun draws(slot: Int) = slot == this.slot && at != At.HERE

    override fun under(f: Frame) {
        if (at == At.AWAY || at == At.READY) return
        val foot = FAR_FOOT + (FOOT - FAR_FOOT) * near
        // As big as the rails are wide where its wheels are: they close in toward the tunnel.
        val size = gauge(foot) / gauge(FOOT.toFloat())
        val rim = foot - (CART.h - 1) * size
        f.clipped(0, G.TOP, G.W, G.BAR - G.TOP) {
            val t = rider
            if (t != null && at == At.COMING && t.rise > 0f) {
                // The rider, as they'd stand in the cart pulled up, brought to the cart's size about the middle of its rim.
                val c = Western.look(t.spec, t.pose, t.frame).cutout
                val x = MIDDLE + (t.x - Sprite.EDGE - MIDDLE) * size
                val y = rim + (t.y - Sprite.EDGE - RIM) * size
                blit(f, c, x, y, size, BLACK, WHITE, below = rim.roundToInt())
            }
            blit(f, CART, MIDDLE - CART.w / 2f * size, rim, size, f.look.ink, f.look.bg)
        }
    }

    override fun over(f: Frame) = Unit

    /** [s] drawn [size] times its own size with its corner at ([x], [y]), nearest pixel for pixel; nothing at or under [below]. */
    private fun blit(f: Frame, s: Sprite, x: Float, y: Float, size: Float, ink: Int, paper: Int, below: Int = Int.MAX_VALUE) {
        val w = maxOf(1, (s.w * size).roundToInt()); val h = maxOf(1, (s.h * size).roundToInt())
        val x0 = x.roundToInt(); val y0 = y.roundToInt()
        for (j in 0 until h) {
            if (y0 + j >= below) break
            for (i in 0 until w) when (s.px[(j * s.h / h) * s.w + i * s.w / w]) {
                INK -> f.dot(x0 + i, y0 + j, ink)
                PAPER -> f.dot(x0 + i, y0 + j, paper)
                PAGE -> f.dot(x0 + i, y0 + j, f.look.bg)
            }
        }
    }

    companion object {
        /** The rails' middle; the cart's rim and where its wheels rest when it's pulled up; and its wheels' rest up in the tunnel mouth. */
        const val MIDDLE = 108
        const val RIM = 192
        const val FOOT = 215
        const val FAR_FOOT = 151f
        const val ROLL_MS = 1100
        const val AWAY_MS = 2600
        const val LINGER_MS = 450
        const val IDLE_MS = 9000

        /** Half the distance between the rails at height [y]: they run from the tunnel's foot out to the bottom of the scene. */
        fun gauge(y: Float) = 8f + 26f * (y - 149f) / 68f

        /**
         * The cart, head on: a rim, a body narrowing toward its floor with its boards and rivets, a lamp on the front,
         * and a wheel under each side, on the rails.
         */
        val CART: Sprite = Pad(60, 24).apply {
            card(0, 13, 6, 11); card(54, 13, 6, 11)
            box(2, 15, 2, 7); box(56, 15, 2, 7)
            poly(3, 3, 56, 3, 50, 19, 9, 19)
            card(13, 19, 34, 3)
            line(20, 4, 22, 18); line(39, 4, 37, 18)
            for ((x, y) in listOf(7 to 6, 52 to 6, 12 to 16, 47 to 16, 24 to 6, 35 to 6)) dot(x, y)
            ring(30, 12, 4); disc(30, 12, 2); disc(30, 12, 1, false)
            card(0, 0, 60, 4)
        }.sprite()
    }
}
