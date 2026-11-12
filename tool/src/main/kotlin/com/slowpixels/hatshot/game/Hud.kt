package com.slowpixels.hatshot.game

/**
 * The bars above and below the scene, in the game's own pixel letters, drawn into the picture with everything else:
 * the score and your hats on top; the streak, the bullets and what's left to shoot at the foot. Only the pause button
 * is LightOS's own, laid over the top-left corner by the screen.
 */
object Hud {
    /** The middle of the score: between the pause button and the hats. */
    const val SCORE_CX = 84
    private const val TOP_Y = 8
    /** Where a line of letters sits in the bottom bar. */
    private val TEXT_Y = G.BAR + (G.H - G.BAR - West.H) / 2
    private const val SIDE = 6

    private const val BULLET_W = 5
    private const val BULLET_H = 13
    private const val PITCH = 12
    private val BULLETS_X = (G.W - (BULLET_W + (Round.BULLETS - 1) * PITCH)) / 2
    private val BULLETS_Y = G.BAR + (G.H - G.BAR - BULLET_H) / 2

    /** A cartridge: solid while it's loaded, hollow once it's fired, as Light's own shapes fill in when they're on. */
    private val LOADED = Sprite.of(art("""
        .###.
        #####
        #####
        #####
        #####
        #####
        #####
        #####
        #####
        #####
        .....
        #####
        #####
    """))
    private val SPENT = Sprite.of(art("""
        .###.
        #ooo#
        #ooo#
        #ooo#
        #ooo#
        #ooo#
        #ooo#
        #ooo#
        #ooo#
        #####
        .....
        #####
        #####
    """))

    private val STAR_WON = Sprite.of(WesternScenes.SMALL_STAR.swap('o', '#'))
    private val STAR_TO_WIN = Sprite.of(WesternScenes.SMALL_STAR)

    /** The middle of bullet [i], counted from the left: where its spent case flies up from. */
    fun bulletX(i: Int) = BULLETS_X + i * PITCH + BULLET_W / 2

    /** Wipes both bars back to the page, over whatever of the scene strayed into them. */
    fun clear(f: Frame) {
        f.box(0, 0, G.W, G.TOP, f.look.bg)
        f.box(0, G.BAR, G.W, G.H - G.BAR, f.look.bg)
    }

    fun score(f: Frame, score: Int) {
        val s = commas(score)
        f.west(s, West.left(s, SCORE_CX), TOP_Y)
    }

    /** Your hats, [left] of [of] still on: a hat lost is left as its outline. */
    fun hats(f: Frame, left: Int, of: Int) {
        for (i in 0 until of) {
            f.put(if (i < left) WesternArt.LIFE else WesternArt.LIFE_GONE, G.W - SIDE - (of - i) * 15, Play.HAT_Y)
        }
    }

    /** The bottom bar while a stage is played: the streak, the six bullets (or RELOAD once they're gone), what's left. */
    fun bottom(f: Frame, round: Round, reload: Boolean, left: Int) {
        if (round.multiplier > 1) f.west("×${round.multiplier}", SIDE, TEXT_Y)
        val word = "$left LEFT"
        f.west(word, G.W - SIDE - West.cells(word), TEXT_Y)
        if (reload) {
            val x = West.left("RELOAD", G.W / 2)
            f.west("RELOAD", x, TEXT_Y)
            // Underlined, as LightOS marks what's there to be tapped.
            f.box(x, TEXT_Y + West.H + 2, West.cells("RELOAD"), 1, f.look.ink)
            return
        }
        val loaded = round.loaded
        for (i in 0 until Round.BULLETS) {
            val full = i < loaded.toInt()
            // The one going in drops into place from above.
            val coming = i == loaded.toInt() && round.reloading > 0
            val drop = if (coming) ((1f - (loaded - i)) * 8).toInt() else 0
            f.put(if (full || coming) LOADED else SPENT, BULLETS_X + i * PITCH, BULLETS_Y - drop)
        }
    }

    /** The bottom bar at High Noon: a star for each of the three duels, filled in as it's won. */
    fun stars(f: Frame, wins: Int, of: Int = 3) {
        val w = STAR_WON.w; val gap = 8
        val x0 = (G.W - (of * w + (of - 1) * gap)) / 2
        val y = G.BAR + (G.H - G.BAR - STAR_WON.h) / 2
        for (i in 0 until of) f.put(if (i < wins) STAR_WON else STAR_TO_WIN, x0 + i * (w + gap), y)
    }

    /** 12345 as 12,345, the same on every phone whatever its language. */
    fun commas(n: Int): String {
        val digits = n.coerceAtLeast(0).toString()
        val out = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) out.append(',')
            out.append(c)
        }
        return out.toString()
    }
}
