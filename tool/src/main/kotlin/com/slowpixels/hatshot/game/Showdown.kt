package com.slowpixels.hatshot.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High noon: three quick-draws, played as the Kompakt's [Duel] with the Light Phone's screen to show it. The word
 * slams in and the street shakes; a beaten leader's hat flies and lands in the dust, where it stays; Black Shell Hank,
 * last, loses the shell he's named for, in pieces. A tumbleweed goes by while everyone waits.
 */
class Showdown(
    pace: Pace, val gentle: Boolean, look: Look, private val out: Out, seed: Long = System.nanoTime(),
) : DuelEvents {
    val duel = Duel(pace, this, seed, tricks = !gentle)
    val juice = Juice(seed)
    private val pad = WesternScenes.noon()
    private var page = Page(pad, look)
    var look: Look = look
        set(v) { if (v !== field) { field = v; page = Page(pad, v) } }

    private var pose = Pose.STAND
    private var ms = 0L
    /** Before the start, the sky is left empty for the card that's over it. */
    var waiting = true

    /** The score as the top bar shows it, counting up to the real one; the screen keeps it moving. */
    var shownScore = 0
    /** The hats of the leaders already beaten, lying in the street, and one still on its way down. */
    private val fallen = ArrayList<Sprite>()
    private var falling: Sprite? = null
    private var landsIn = 0
    private var weedAt = -1f
    private var weedWait = 2200

    fun shoot() = duel.shoot()

    fun tick(step: Int) {
        ms += step
        duel.tick(step)
        juice.tick(step)
        if (falling != null) {
            landsIn -= step
            if (landsIn <= 0) { fallen += falling!!; falling = null }
        }
        // Now and then, while everyone waits, a tumbleweed.
        if (weedAt < 0) {
            weedWait -= step
            if (weedWait <= 0 && duel.phase == Duel.Phase.STEADY) weedAt = G.W.toFloat()
        } else {
            weedAt -= step * 0.07f
            if (weedAt < -20) { weedAt = -1f; weedWait = 4000 + (ms % 3000).toInt() }
        }
    }

    /** Whose turn it is: once beaten, it's still the one you beat who's standing there, not the next. */
    val who get() = (if (pose == Pose.HIT || pose == Pose.BARE) duel.wins - 1 else duel.wins).coerceIn(0, Duel.NEEDED - 1)
    val name get() = if (gentle) "Target ${who + 1}" else WesternScenes.LEADER_NAMES[who]

    override fun changed() {
        val was = pose
        pose = when (duel.phase) {
            Duel.Phase.DRAW, Duel.Phase.BEATEN -> Pose.AIM
            // The first two lose their hats. Black Shell Hank, last, loses the shell he's named for.
            // And Hank takes three: after the first two he's still standing there, short of a hat, then of a bandana.
            Duel.Phase.WON -> if (duel.beatBoss && !gentle) Pose.BARE else if (stripping) Pose.STAND else Pose.HIT
            else -> Pose.STAND
        }
        when (duel.phase) {
            Duel.Phase.DRAW -> juice.thump()
            // A fake word lands as hard as the real one; only the outlaw's hand stays down.
            Duel.Phase.STEADY -> if (duel.faking) juice.thump()
            Duel.Phase.BEATEN -> {
                juice.ring(BOSS_X + 2, BOSS_Y + 48, to = 10, ms = 160)
                juice.sparks(BOSS_X + 2, BOSS_Y + 48, n = 8, force = 0.12f, gravity = 0f)
                juice.hit(4)
            }
            Duel.Phase.FOUL -> juice.hit(1)
            Duel.Phase.WON -> if (stripping) stripped() else if (was != Pose.HIT && was != Pose.BARE) beaten()
            else -> Unit
        }
    }

    /** Hank has been hit but isn't finished: this was one of the shots that only cost him something he wore. */
    private val stripping get() = !gentle && duel.phase == Duel.Phase.WON && !duel.beatBoss && duel.bossHits > 0

    /** What the shot took off him: his hat first, which lands in the street with the others; then his bandana. */
    private fun stripped() {
        if (duel.bossHits == 1) WesternArt.hatOf(WesternScenes.leader(2))?.let { hat ->
            juice.hatOff(WesternScenes.twice(hat), BOSS_X, BOSS_Y, fromLeft = true)
            falling = hat
            landsIn = LAND_MS
        } else juice.hatOff(WesternScenes.twice(WesternArt.hankBandana), BOSS_X, BOSS_Y, fromLeft = false)
        juice.ring(BOSS_X + 24, BOSS_Y + 24, to = 16)
        out.fx(Fx.Bonk)
    }

    private fun beaten() {
        if (gentle) {
            juice.ring(BOSS_X + 24, BOSS_Y + 22 + 16, to = 24)
            return
        }
        val leader = WesternScenes.leader(duel.wins - 1)
        // The first two duck into their shells and leave their hats, which come down in the street and stay there.
        if (pose == Pose.HIT) WesternArt.hatOf(leader)?.let { hat ->
            juice.hatOff(WesternScenes.twice(hat), BOSS_X, BOSS_Y, fromLeft = true)
            falling = hat
            landsIn = LAND_MS
        }
        if (pose == Pose.BARE) {
            val shell = WesternScenes.twice(WesternArt.snail(leader, Pose.STAND).cut(0, 25, 24, 15))
            juice.shatter(shell, BOSS_X, BOSS_Y + 50, cell = 4, force = 0.14f, lift = 0.12f)
            juice.ring(BOSS_X + 24, BOSS_Y + 64, to = 28, ms = 340)
            out.fx(Fx.Crack)
        }
    }

    override fun fx(fx: Fx) = out.fx(fx)
    override fun buzz() = out.buzz(true)
    override fun finished() = out.finished()

    fun draw(f: Frame) {
        f.look = look
        f.page(page)
        f.clipped(0, G.TOP, G.W, G.BAR - G.TOP) {
            // The beaten leaders' hats, in the dust.
            fallen.forEachIndexed { i, hat -> f.cut(hat, 138 + i * 26, 204) }
            if (weedAt >= 0) {
                val hop = (abs(sin(weedAt * PI / 24)) * 6).roundToInt()
                f.put(WesternArt.tumbleweed[((-weedAt / 6).toInt() % 4 + 4) % 4], weedAt.roundToInt(), 196 - hop)
            }
            // Belle in Hank's wagon cage, sad, behind its bars, a way down the street by the buildings: small because
            // she's far off, where Hank, close up, is big.
            f.put(WesternArt.belleCaged[((ms / 900) % 2).toInt()], BELLE_X - 3, BELLE_Y - WesternArt.CAGE_HEAD, ink = BLACK, paper = WHITE)
            if (gentle) f.putBig(if (pose == Pose.HIT) WesternArt.bullseyeHit else WesternArt.bullseye, BOSS_X, BOSS_Y + 16, 2)
            else f.cut(if (who == Duel.NEEDED - 1) WesternScenes.hank(duel.bossHits, pose) else Western.boss(who, pose), BOSS_X, BOSS_Y)
            juice.draw(f, scene = true)
            // The word, and who you're facing.
            if (!waiting) {
                val big = duel.phase == Duel.Phase.DRAW || duel.faking
                val dot = if (big) 3 else 2
                f.westCenter(duel.word, G.W / 2, if (big) 28 else 33, look.ink, dot)
                val n = name
                f.font(n, G.W / 2 - Font.width(n) / 2, 60, look.ink)
            }
        }
        val (dx, dy) = juice.shake()
        f.shift(dx, dy, G.TOP, G.BAR)
        juice.drawFlash(f)
        Hud.clear(f)
        Hud.hats(f, duel.hearts, Duel.HEARTS)
        if (!waiting) {
            Hud.score(f, shownScore)
            Hud.stars(f, duel.wins)
        }
        juice.draw(f, scene = false)
    }

    companion object {
        const val BOSS_X = 84
        const val BOSS_Y = 128
        /** Belle's cage stands on the street's left edge near the far end, its wheels at about y 150. */
        const val BELLE_X = 40
        const val BELLE_Y = 100
        /** How long a leader's hat is in the air before it's lying in the street. */
        const val LAND_MS = 1300
    }
}
