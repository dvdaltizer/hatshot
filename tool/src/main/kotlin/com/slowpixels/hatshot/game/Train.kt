package com.slowpixels.hatshot.game

import com.slowpixels.hatshot.game.WesternScenes.crag
import com.slowpixels.hatshot.game.WesternScenes.planks
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The train, one long strip of cars from caboose to engine, and the country going by. On the Kompakt everything moved
 * in big steps a few times a second; here the mesas, the telegraph poles and the ground slide past smoothly, each at
 * its own speed, the far ones slowest, so it looks deep.
 */
object TrainArt {
    const val ROOF = 84
    const val RAIL = 186
    /** How tall the country behind the train is, from the top of the scene. */
    const val BACK_H = ROOF - G.TOP
    const val CARS = 5
    /** A car is as wide as the screen. */
    const val CW = G.W

    /** The openings all the cars share, as in the stage's slots. */
    val PANES = listOf(12, 64, 124, 176)
    private const val PANE_Y = 98
    private const val PANE_W = 28
    private const val PANE_H = 34

    /** The sun stays where it is; everything else goes past it. */
    fun sky(p: Pad) {
        p.ring(196, 36, 7)
    }

    /** Far off: mesas on the skyline, hardly moving. */
    val far: Sprite by lazy {
        val p = Pad(320, BACK_H)
        p.box(0, BACK_H - 4, 320, 1)
        p.crag(16, BACK_H - 20, 66, 16, 3)
        p.crag(126, BACK_H - 16, 40, 12, 5)
        p.crag(206, BACK_H - 26, 80, 22, 7)
        p.sprite()
    }

    /** Nearer: telegraph poles, coming up one after another. */
    val poles: Sprite by lazy {
        val p = Pad(120, BACK_H)
        p.box(59, 6, 2, BACK_H - 6)
        p.box(51, 10, 18, 2)
        for (x in listOf(51, 68)) p.box(x, 8, 1, 2)
        for (i in 0 until 120) {
            val sag = (5 * sin(PI * ((i + 60) % 120) / 120.0)).toInt()
            p.dot(i, 8 + sag)
        }
        p.sprite()
    }

    /** Smoke from the engine, streaming back along the whole train: each puff one soft outline. */
    val smoke: Sprite by lazy {
        val p = Pad(140, 26)
        for (puff in listOf(listOf(Triple(14, 14, 6), Triple(24, 11, 8), Triple(35, 15, 5)), listOf(Triple(94, 12, 5), Triple(103, 15, 6)))) {
            for ((x, y, r) in puff) p.disc(x, y, r)
            for ((x, y, r) in puff) p.disc(x, y, r - 1, false)
        }
        p.sprite()
    }

    /** Close to: ties going under, stones and scrub racing by. */
    val ground: Sprite by lazy {
        val h = G.BAR - RAIL
        val p = Pad(240, h)
        p.box(0, 0, 240, 1)
        var x = 0
        while (x < 240) { p.box(x, 1, 7, 2); x += 12 }
        val r = kotlin.random.Random(9)
        repeat(70) { p.dot(r.nextInt(240), 6 + r.nextInt(h - 6)) }
        for ((bx, by) in listOf(30 to 16, 110 to 24, 176 to 12)) {
            p.box(bx - 5, by, 11, 1)
            p.line(bx - 5, by, bx - 2, by - 3); p.line(bx - 2, by - 3, bx + 3, by - 3); p.line(bx + 3, by - 3, bx + 5, by)
        }
        p.stamp(WesternScenes.CACTUS, 68, h - 19)
        p.stamp(WesternScenes.CACTUS, 206, h - 19)
        p.sprite()
    }

    /** A wheel of [r] at spoke position [turn] of four. */
    private fun wheel(r: Int, turn: Int): Sprite {
        val size = r * 2 + 3
        val c = r + 1
        val p = Pad(size, size)
        p.disc(c, c, r, false)
        p.ring(c, c, r)
        for (i in 0 until 4) {
            val a = (i + turn * 0.25) * PI / 4
            val dx = ((r - 1) * cos(a)).roundToInt(); val dy = ((r - 1) * sin(a)).roundToInt()
            p.line(c - dx, c - dy, c + dx, c + dy)
        }
        p.disc(c, c, 2)
        return p.sprite()
    }

    val small = (0 until 4).map { wheel(10, it) }
    val big = (0 until 4).map { wheel(14, it) }
    val pilot = (0 until 4).map { wheel(7, it) }

    /** Where the wheels go along the strip: x of the middle, and which wheel. */
    val wheels: List<Pair<Int, List<Sprite>>> by lazy {
        val out = ArrayList<Pair<Int, List<Sprite>>>()
        for (car in 0 until CARS - 1) for (x in listOf(26, 50, CW - 50, CW - 26)) out += (car * CW + x) to small
        val e = (CARS - 1) * CW
        out += (e + 20) to small
        out += (e + 46) to small
        out += (e + 112) to big
        out += (e + 150) to big
        out += (e + 190) to pilot
        out
    }

    val horse: Sprite by lazy { Sprite.of(WesternArt.HORSE) }

    /** Mosey's six-shooter, held barrel up: a little revolver, cut out of paper like everything he has. */
    val gun: Sprite by lazy { Sprite.of(art("""
        ..#....
        ..#....
        ..#....
        .###...
        .#o##..
        .#oo##.
        .####..
        ..#o#..
        ..#o#..
        ..###..
    """)) }
    /** Where the gun's barrel ends, across from its picture's left. */
    const val MUZZLE_X = 2

    /** The middle of the engine's chimney along the strip. */
    const val STACK_X = (CARS - 1) * CW + 188

    /** Every car, side by side, the caboose first: the train runs to the right, so you come up on it from behind. */
    val strip: Sprite by lazy {
        val p = Pad(CARS * CW, RAIL + 1)
        caboose(p, 0)
        passenger(p, CW)
        express(p, 2 * CW)
        stock(p, 3 * CW)
        engine(p, 4 * CW)
        p.sprite()
    }

    private fun body(p: Pad, x: Int) {
        p.card(x + 6, ROOF + 6, CW - 12, 66)
        p.card(x + 2, ROOF, CW - 4, 6)
        p.box(x, 152, 6, 3); p.box(x + CW - 6, 152, 6, 3)
    }

    private fun frame(p: Pad, x: Int) {
        p.box(x + 6, 156, CW - 12, 2)
        p.card(x + 3, 158, CW - 6, 4)
        for (b in listOf(38, CW - 38)) p.card(x + b - 21, 162, 42, 3)
    }

    private fun pane(p: Pad, x: Int) {
        p.box(x - 3, PANE_Y - 3, PANE_W + 6, PANE_H + 6, false)
        p.outline(x - 3, PANE_Y - 3, PANE_W + 6, PANE_H + 6)
        p.outline(x - 1, PANE_Y - 1, PANE_W + 2, PANE_H + 2)
        p.shade(x, PANE_Y, PANE_W, PANE_H) { i, j -> i % 6 == 0 && j % 6 == 0 }
    }

    private fun line(p: Pad, x: Int, text: String) {
        val w = Font.width(text)
        p.box(x + CW / 2 - w / 2 - 4, 136, w + 8, 13, false)
        p.text(text, x + CW / 2 - w / 2, 139)
    }

    private fun caboose(p: Pad, x: Int) {
        // The lookout on the roof, between the outlaws' spots, and the stove pipe.
        p.card(x + 98, 68, 20, 17)
        p.outline(x + 101, 72, 14, 8)
        p.card(x + 95, 65, 26, 4)
        p.card(x + 159, 72, 5, 13)
        body(p, x)
        p.planks(x + 7, ROOF + 7, CW - 14, 64, 6)
        for (px in PANES) pane(p, x + px)
        line(p, x, "DRY GULCH LINE")
        frame(p, x)
    }

    private fun passenger(p: Pad, x: Int) {
        body(p, x)
        var i = x + 10
        // Upright boards, hinted at: a tick at the head and foot of each.
        while (i < x + CW - 8) { p.box(i, ROOF + 7, 1, 5); p.box(i, ROOF + 66, 1, 5); i += 6 }
        for (px in PANES) pane(p, x + px)
        line(p, x, "PASSENGERS")
        frame(p, x)
    }

    private fun express(p: Pad, x: Int) {
        body(p, x)
        p.planks(x + 7, ROOF + 7, CW - 14, 64, 9)
        // The sliding door's rail, and its door pushed open between the middle windows.
        p.box(x + 94, ROOF + 8, 28, 2)
        p.card(x + 97, ROOF + 10, 24, 58)
        p.line(x + 98, ROOF + 11, x + 119, ROOF + 66); p.line(x + 119, ROOF + 11, x + 98, ROOF + 66)
        for (px in PANES) pane(p, x + px)
        line(p, x, "EXPRESS")
        frame(p, x)
    }

    private fun stock(p: Pad, x: Int) {
        body(p, x)
        var y = ROOF + 10
        while (y < 154) { p.box(x + 7, y, CW - 14, 2); y += 7 }
        for (b in listOf(48, 108, 162)) p.card(x + b, ROOF + 7, 6, 64)
        p.line(x + 8, 154, x + 46, ROOF + 8); p.line(x + 168, 154, x + 208, ROOF + 8)
        for (px in PANES) pane(p, x + px)
        frame(p, x)
    }

    private fun engine(p: Pad, x: Int) {
        // The tender, heaped with coal.
        p.card(x + 6, ROOF + 16, 52, 56)
        p.box(x + 7, ROOF + 17, 50, 2)
        for (i in 0 until 7) { p.disc(x + 13 + i * 6, ROOF + 10 + (i % 3) * 2, 5, false); p.ring(x + 13 + i * 6, ROOF + 10 + (i % 3) * 2, 5) }
        p.box(x + 8, ROOF + 1, 44, 8, false)
        for (i in 0 until 6) p.ring(x + 14 + i * 7, ROOF + 5 + (i % 2) * 2, 4)
        p.box(x + 8, ROOF, 46, 1)
        p.text("DGL", x + 23, 128)
        p.card(x + 58, 150, 8, 4)
        // The cab.
        p.card(x + 60, ROOF + 4, 44, 68)
        p.card(x + 56, ROOF, 52, 5)
        pane(p, x + 64)
        // The boiler, fat and riveted, with the dome, the chimney and the lamp. Its top is level with the roofs, so
        // whoever stands on it stands on it.
        p.card(x + 104, ROOF, 88, 64)
        for (bx in listOf(124, 146, 168)) { p.box(x + bx, ROOF + 1, 2, 62); var r = ROOF + 5; while (r < ROOF + 62) { p.box(x + bx + 4, r, 1, 1); r += 7 } }
        p.card(x + 192, ROOF + 10, 8, 52)
        p.card(x + 110, ROOF - 12, 12, 13); p.card(x + 108, ROOF - 14, 16, 3)
        p.card(x + 184, ROOF - 24, 9, 25); p.card(x + 181, ROOF - 30, 15, 7)
        p.card(x + 194, ROOF + 12, 9, 9)
        // Under it all, and the cowcatcher.
        p.card(x + 60, 150, 140, 8)
        p.line(x + 196, 158, x + 213, 182); p.line(x + 190, 158, x + 205, 184); p.line(x + 202, 158, x + 214, 172)
    }
}

/** What's going on outside the window of a train you're galloping beside. */
class TrainRide : Rolling {
    private var ms = 0L
    private var camera = 0f
    private var aim = 0f

    override fun tick(ms: Int) {
        this.ms += ms
        if (camera != aim) camera = min(camera + SLIDE * ms, aim)
    }

    override fun wave(n: Int) {
        aim = (min(n, TrainArt.CARS - 1) * G.W).toFloat()
    }

    private fun Frame.tile(s: Sprite, offset: Int, y: Int) {
        var x = -(offset % s.w)
        while (x < G.W) { put(s, x, y); x += s.w }
    }

    override fun under(f: Frame) {
        f.clip(0, G.TOP, G.W, G.BAR - G.TOP)
        f.tile(TrainArt.far, (ms * FAR).toInt(), G.TOP)
        val cam = camera.roundToInt()
        // The smoke starts at the chimney once you can see it, and comes from up ahead until then.
        f.clip(0, G.TOP, min(G.W, TrainArt.STACK_X - cam), G.BAR - G.TOP)
        f.tile(TrainArt.smoke, (ms * SMOKE).toInt(), 26)
        f.clip(0, G.TOP, G.W, G.BAR - G.TOP)
        f.tile(TrainArt.poles, (ms * POLES).toInt(), G.TOP)
        f.put(TrainArt.strip, -cam, 0)
        puffs(f, TrainArt.STACK_X - cam)
        val turn = ((ms / WHEEL_MS) % 4).toInt()
        for ((x, w) in TrainArt.wheels) {
            val s = w[turn]
            val sx = x - cam - s.w / 2
            if (sx + s.w >= 0 && sx < G.W) f.put(s, sx, TrainArt.RAIL - s.h + 1)
        }
        f.tile(TrainArt.ground, (ms * GROUND).toInt(), TrainArt.RAIL)
        f.unclip()
    }

    /**
     * The engine's chimney, working hard: a puff leaves it every third of a second, swelling as it climbs and falling
     * behind as the train runs on. Drawn only once the chimney's in sight, at [x].
     */
    private fun puffs(f: Frame, x: Int) {
        if (x < -10 || x > G.W + 80) return
        val ink = f.look.ink; val bg = f.look.bg
        val top = TrainArt.ROOF - 31
        // The oldest first, so the newest, smallest puff sits on the others at the chimney's mouth.
        for (k in PUFFS - 1 downTo 0) {
            val age = ((ms + k * (PUFF_MS / PUFFS)) % PUFF_MS) / PUFF_MS.toFloat()
            val r = (3 + age * 8).roundToInt()
            val cx = x - (age * age * 74).roundToInt() + (sin((ms / 260.0) + k) * 1.5).roundToInt()
            val cy = top - r + 1 - (age * 26).roundToInt()
            for (dy in -r..r) for (dx in -r..r) {
                val d = dx * dx + dy * dy
                if (d > r * r + r / 2) continue
                val rim = d > (r - 1) * (r - 1) + (r - 1) / 2
                // The last of it thins out: only some of its outline is left.
                if (age > 0.8f) { if (rim && (dx + dy + k) % 3 != 0) f.dot(cx + dx, cy + dy, ink) }
                else f.dot(cx + dx, cy + dy, if (rim) ink else bg)
            }
        }
    }

    /** When the last shot went off (on this ride's clock), and where it was aimed. */
    private var shotAt = -1L
    private var shotX = 0
    private var shotY = 0

    override fun shot(x: Int, y: Int) { shotAt = ms; shotX = x; shotY = y }

    override fun over(f: Frame) {
        val gallop = (abs(sin(ms * PI / GALLOP_MS)) * 3).roundToInt()
        f.clipped(0, G.TOP, G.W, G.BAR - G.TOP) {
            // Between the car's two sets of wheels, so they don't tangle with the hat and the horse's ears.
            val rx = (G.W - TrainArt.horse.w) / 2; val ry = HORSE_Y + gallop
            f.cut(TrainArt.horse, rx, ry)
            // Mosey's six-shooter, held up at his right: it kicks as it fires, a flash at the muzzle, and the shot's
            // line runs to where you tapped.
            val since = ms - shotAt
            val firing = shotAt >= 0 && since < FIRE_MS
            val kick = if (shotAt >= 0 && since < KICK_MS) 2 else 0
            val gx = rx + GUN_X; val gy = ry + GUN_Y - kick
            f.cut(TrainArt.gun, gx, gy)
            if (firing) {
                val mx = gx + TrainArt.MUZZLE_X; val my = gy - 2
                tracer(f, mx, my, shotX, shotY)
                for ((dx, dy) in FLASH) f.dot(mx + dx, my + dy, f.look.ink)
            }
        }
    }

    /** The shot's path, a dot every third pixel, from the muzzle to where it was aimed. */
    private fun tracer(f: Frame, x0: Int, y0: Int, x1: Int, y1: Int) {
        val n = maxOf(abs(x1 - x0), abs(y1 - y0)).coerceAtLeast(1)
        for (k in 4..n step 3) f.dot(x0 + (x1 - x0) * k / n, y0 + (y1 - y0) * k / n, f.look.ink)
    }

    private companion object {
        /** How many puffs are in the air over the chimney at once, and how long each lasts. */
        const val PUFFS = 5
        const val PUFF_MS = 1700L
        /** Game pixels a millisecond: the far mesas, the smoke, the poles, the ground. */
        const val FAR = 0.014f
        const val SMOKE = 0.047f
        const val POLES = 0.075f
        const val GROUND = 0.115f
        /** How fast the next car comes alongside between waves. */
        const val SLIDE = 0.36f
        const val WHEEL_MS = 38L
        const val GALLOP_MS = 300.0
        /** The rider's picture is 54 tall: its foot sits on the bottom bar, and dips under it at each stride. */
        const val HORSE_Y = 164
        /** Where Mosey's gun is held, from the rider picture's top-left: up beside his hat, at his right. */
        const val GUN_X = 41
        const val GUN_Y = 6
        const val FIRE_MS = 90L
        const val KICK_MS = 70L
        /** The muzzle flash: a little star of pixels round the end of the barrel. */
        val FLASH = listOf(0 to 0, 0 to -1, 0 to -2, -1 to -1, 1 to -1, -2 to -2, 2 to -2, -2 to 0, 2 to 0, 0 to -3)
    }
}
