package com.slowpixels.hatshot.game

import kotlin.math.max
import kotlin.math.min

/** What a stage being played tells the phone around it: sounds, buzzes, music. */
interface Out {
    fun fx(fx: Fx)
    /** A short buzz, or a longer one when you've been hit. */
    fun buzz(hard: Boolean)
    /** The piano player was hit: the music stops a while. */
    fun silence(ms: Int)
    /** The stage is over, won or lost. */
    fun finished()
}

/**
 * A stage being played: its [Round], everything flying about ([Juice]), and the picture of it all. No Android here,
 * so a test can play it and draw it.
 */
class Play(
    val world: World, val index: Int, pace: Pace, gentle: Boolean, look: Look, private val out: Out,
    seed: Long = System.nanoTime(),
) : Events {
    val stage = world.stages[index]
    val round = Round(stage, pace, this, seed, gentle)
    val juice = Juice(seed)
    private val rolling: Rolling? = world.rolling(stage)
    private val pad = world.background(stage)
    private var page = Page(pad, look)

    var look: Look = look
        set(v) { if (v !== field) { field = v; page = Page(pad, v) } }

    /** The gun is empty: the bottom bar says to reload, from the moment the last bullet goes. */
    var wanted = false
        private set

    /** The card before the stage is still up: no score or bullets in the bars yet. */
    var waiting = true

    /** The score as the top bar shows it, counting up to the real one; the screen keeps it moving. */
    var shownScore = 0

    /** Taps inside the scene shoot; taps on the bottom bar reload. */
    fun tap(x: Int, y: Int) {
        when {
            y >= G.BAR -> round.reload()
            y >= G.TOP -> {
                val before = round.bullets
                round.shoot(x, y)
                if (round.bullets < before) rolling?.shot(x, y)
            }
        }
    }

    /** While above zero the piano player's been hit and the music's stopped: no notes in the air. */
    private var quiet = 0
    private var nextNote = 400

    fun tick(ms: Int) {
        round.tick(ms)
        // Whoever plays the music (the piano player) sends notes up into the air while it's playing.
        if (quiet > 0) quiet -= ms
        round.live.firstOrNull { it.spec.plays }?.let { p ->
            nextNote -= ms
            if (nextNote <= 0 && quiet <= 0 && round.state == Round.State.PLAYING) {
                nextNote = 520 + (p.x * 7 + round.shots * 13) % 260
                val which = if ((nextNote / 7) % 3 == 0) WesternArt.NOTES else WesternArt.NOTE
                juice.note(which, WesternScenes.PIANO_X + 8 + (nextNote * 3) % 30, WesternScenes.PIANO_Y - 8)
            }
        }
        rolling?.tick(ms)
        rolling?.see(round)
        juice.tick(ms)
        if (round.bullets > 0 || round.reloading > 0) wanted = false
    }

    /** Whether the scene moves by itself with nobody playing (the train): the card before the stage shows it rolling. */
    val restless get() = rolling != null

    /** Just the things that keep moving whatever happens (the train, what's flying): for the card before a stage. */
    fun idle(ms: Int) {
        rolling?.tick(ms)
        juice.tick(ms)
    }

    // ---- What the round says. ----

    override fun fx(fx: Fx) = out.fx(fx)
    override fun buzz() = out.buzz(false)
    override fun empty() { wanted = true }
    override fun finished() = out.finished()
    override fun silence(ms: Int) { quiet = ms; out.silence(ms) }
    override fun wave(n: Int) { rolling?.wave(n) }
    override fun shot(left: Int) {
        juice.casing(Hud.bulletX(left), G.BAR + (G.H - G.BAR) / 2 - 4)
        if (left == 0) wanted = true
    }

    override fun missed(x: Int, y: Int) = juice.sparks(x, y, n = 5, force = 0.05f, faint = true)

    override fun knocked(t: Target, x: Int, y: Int, special: Boolean) {
        val spec = t.spec
        // Whoever has ducked into a shell has left a hat (or a bow) behind; one who's lost the shell keeps the hat on.
        if (t.pose == Pose.HIT) WesternArt.hatOf(spec.name)?.let { juice.hatOff(it, t.x, t.y, fromLeft = x < t.x + spec.w / 2) }
        // Hitting one of the good folks costs a hat, and it's seen to go.
        if (spec.role == Role.FRIEND && !spec.dear) juice.hatLost(WesternArt.LIFE, hatX(round.hearts.coerceAtLeast(0)), HAT_Y)
        if (special) when (spec.role) {
            Role.FOE -> {
                // The shell goes off in pieces: the shell itself, cut up.
                val s = spec.shell ?: return
                val shell = WesternArt.snail(spec.name, Pose.STAND).cut(s.x, s.y, 24, 15)
                juice.shatter(shell, t.x + s.x, t.y + s.y, cell = 3, force = 0.12f, lift = 0.11f)
                juice.ring(t.x + s.x + 12, t.y + s.y + 7, to = 18)
            }
            Role.SIGN -> juice.ring(t.x + spec.w / 2, t.y + 11, to = 16)
            else -> Unit
        } else if (spec.role == Role.SIGN) juice.sparks(x, y, n = 6, force = 0.07f)
    }

    override fun smashed(t: Target) {
        val s = world.look(t.spec, t.pose, t.frame)
        val cx = t.x + t.spec.w / 2; val cy = t.y + t.spec.h / 2
        when (t.spec.name) {
            "bottle" -> { juice.shatter(s, t.x, t.y, cell = 2, force = 0.08f, lift = 0.07f); juice.sparks(cx, t.y + 2, n = 5, force = 0.1f) }
            "can", "white hat", "wheel" -> juice.hop(s, t.x, t.y, toward = G.W / 2 + (cx - G.W / 2) * 2)
            "coin" -> { juice.hop(s, t.x, t.y, toward = cx + 1); juice.sparks(cx, cy, n = 6, force = 0.08f) }
            "poster" -> juice.shatter(s, t.x, t.y, cell = 4, force = 0.04f, lift = 0.03f, gravity = 0.00018f, life = 1400)
            "balloon" -> { juice.ring(cx, t.y + 8, to = 13); juice.shatter(s, t.x, t.y, cell = 3, force = 0.06f, lift = 0f, gravity = 0.00022f) }
            "star" -> { juice.ring(cx, t.y + 8, to = 16); juice.sparks(cx, t.y + 8, n = 12, force = 0.1f, gravity = 0.0002f) }
            "dynamite" -> {
                juice.ring(cx, cy, to = 22, ms = 320)
                juice.sparks(cx, cy, n = 14, force = 0.14f)
                juice.shatter(s, t.x, t.y, cell = 3, force = 0.1f)
            }
            "buzzard" -> juice.shatter(s, t.x, t.y, cell = 3, force = 0.06f, lift = 0.02f, gravity = 0.0002f, life = 1500)
            else -> { juice.shatter(s, t.x, t.y, cell = 3, force = 0.09f); juice.sparks(cx, cy, n = 4) }
        }
    }

    override fun fired(t: Target) {
        // The gun's muzzle, at the snail's left, and a flash there.
        juice.ring(t.x + 1, t.y + 24, to = 6, ms = 140)
        juice.sparks(t.x + 1, t.y + 24, n = 6, force = 0.09f, gravity = 0f)
    }

    override fun blew(box: Box) {
        val cx = box.x + box.w / 2; val cy = box.y + box.h / 2
        juice.shatter(WesternScenes.tntCrate, box.x, box.y, cell = 4, force = 0.16f, lift = 0.14f, life = 1300)
        juice.ring(cx, cy, to = 30, ms = 380)
        juice.ring(cx, cy, to = 18, ms = 260)
        juice.sparks(cx, cy, n = 18, force = 0.16f)
        juice.thump()
    }

    override fun prop(p: PropState, x: Int, y: Int) = out.fx(WesternProps.shot(p, x, y, juice))

    override fun hurt(x: Int, y: Int) {
        juice.hit()
        out.buzz(true)
        // The hat you've just lost leaves the top bar.
        val i = round.hearts.coerceAtLeast(0)
        juice.hatLost(WesternArt.LIFE, hatX(i), HAT_Y)
    }

    override fun gotHat() {
        juice.ring(hatX(round.hearts - 1) + WesternArt.LIFE.w / 2, HAT_Y + 3, to = 9)
    }

    // ---- The picture. ----

    fun draw(f: Frame) {
        f.look = look
        f.page(page)
        val r = round
        rolling?.under(f)
        tnt(f)
        for (p in r.props) WesternProps.draw(f, p, r.clock)
        stage.slots.forEachIndexed { i, s ->
            val t = r.live.firstOrNull { it.slotIndex == i }
            f.clipped(s.x, s.y, s.w, s.h) {
                // A window or a door is an empty opening: nothing of the room behind, so whoever's in it stands out.
                if (s.dark) f.box(s.x, s.y, s.w, s.h, look.bg)
                if (t != null && t.rise > 0f && rolling?.draws(i) != true) person(f, t)
                world.cover(s, t?.cover ?: 0f)?.let { f.put(it, s.x, s.y) }
            }
        }
        // Everything out in the open stays inside the scene, so a balloon can't float over the bars.
        f.clipped(0, G.TOP, G.W, G.BAR - G.TOP) {
            for (t in r.live) {
                if (t.slot != null || t.layer != 0) continue
                person(f, t)
            }
            for (t in r.live) {
                if (t.slot != null) continue
                if (t.layer == 2 || t.forever && t.spec.role == Role.FRIEND) person(f, t)
            }
            rolling?.over(f)
            for (fl in r.flashes) when (fl.kind) {
                Flash.Kind.BURST -> if (fl.age < 0.55f) f.put(WesternArt.BURST, fl.x + (fl.w - WesternArt.BURST.w) / 2, fl.y + (fl.h - WesternArt.BURST.h) / 2)
                Flash.Kind.PUFF -> if (fl.age < 0.6f) {
                    val up = (fl.age * 4).toInt()
                    f.put(WesternArt.PUFF, fl.x, fl.y - up)
                }
                Flash.Kind.WORD -> Unit
            }
            juice.draw(f, scene = true)
            // Words last, over everything flying: each rises a little, and blinks before it goes.
            for (fl in r.flashes) if (fl.kind == Flash.Kind.WORD) {
                if (fl.ms < 140 && (fl.ms / 45) % 2 == 0) continue
                // Not up past the top of the scene, where its upper edge would be cut off.
                val y = max(G.TOP, fl.y - (min(1f, fl.age * 3f) * 3).toInt())
                f.box(fl.x, y, fl.w, fl.h, look.bg)
                f.outline(fl.x, y, fl.w, fl.h, look.ink)
                f.westCenter(fl.word, fl.x + fl.w / 2, y + 2)
            }
        }
        val (dx, dy) = juice.shake()
        f.shift(dx, dy, G.TOP, G.BAR)
        juice.drawFlash(f)
        // The bars, in the game's own letters.
        Hud.clear(f)
        Hud.hats(f, r.hearts, Round.START_HEARTS)
        if (!waiting) {
            Hud.score(f, shownScore)
            Hud.bottom(f, r, reload = wanted && r.reloading == 0, left = left)
        }
        juice.draw(f, scene = false)
    }

    /**
     * The crate of TNT, if the stage has one: where it stands while it's there; being pushed back in from the left
     * edge by one of the townsfolk once it's been blown and a while has passed; nothing in between.
     */
    private fun tnt(f: Frame) {
        val box = stage.tnt ?: return
        val r = round
        when {
            r.tntReady -> f.put(WesternScenes.tntCrate, box.x, box.y)
            r.tntPushing > 0 -> {
                val p = 1f - r.tntPushing.toFloat() / Round.TNT_PUSH_MS
                // In for the first two thirds, then the helper ducks back out of sight.
                val inP = (p / 0.66f).coerceAtMost(1f)
                val x = -box.w - 26 + ((box.x + box.w + 26) * inP).toInt()
                f.put(WesternScenes.tntCrate, x, box.y)
                val helperX = if (p < 0.66f) x - 26 else x - 26 - ((p - 0.66f) / 0.34f * 30).toInt()
                f.clipped(0, G.TOP, G.W, G.BAR - G.TOP) {
                    f.cut(WesternArt.snail("kid", Pose.STAND).cut(0, 0, 24, 30), helperX, box.y + box.h - 30, flip = true)
                }
            }
        }
    }

    /** Whoever or whatever [t] is, cut out of paper; or just as drawn, for a picture that brings its own edges. */
    private fun person(f: Frame, t: Target) {
        val s = world.look(t.spec, t.pose, t.frame)
        if (t.spec.plain) f.put(s, t.x, t.y, t.flip, BLACK, WHITE) else f.cut(s, t.x, t.y, t.flip)
    }

    /** What's left to shoot, for the bottom bar. */
    val left get() = max(0, round.foesLeft)

    companion object {
        /** The top bar's hats: five in a row at its right. */
        const val HAT_Y = 9
        fun hatX(i: Int) = G.W - 6 - (Round.START_HEARTS - i) * 15
    }
}
