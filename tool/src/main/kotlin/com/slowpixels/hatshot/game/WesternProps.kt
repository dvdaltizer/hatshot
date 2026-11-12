package com.slowpixels.hatshot.game

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Dry Gulch's scenery that answers back when it's shot ([Prop]). The General Store's sacks spill, its eggs roll out,
 * its clock and shelf goods fly off, and its till rings; the Rusty Spur's mirror cracks, its glasses shatter, its keg
 * leaks and its spittoon goes flying; the bank's money bags burst (and come back) and its sign breaks in two; the
 * jailhouse's trough and barrels are stove in and leak. All of it is drawn in play, not in the still picture behind
 * the stage, since it changes.
 */
object WesternProps {
    const val OATS = "oats"
    const val RICE = "rice"
    const val EGGS = "eggs"
    const val TILL = "till"
    const val BAG = "money bag"
    const val TROUGH = "trough"
    const val BARRELS = "barrels"
    const val CLOCK = "clock"
    const val SHELF = "shelf goods"
    const val MIRROR = "mirror"
    const val GLASS = "glass"
    const val KEG = "keg"
    const val SPITTOON = "spittoon"
    const val SIGN = "bank sign"
    const val JAIL_SIGN = "jail sign"

    /** Where the bank's three money bags stand, in a row in front of the strongbox. */
    val BAG_XS = listOf(166, 182, 198)
    const val BAG_Y = 202
    /** A bag that's coming back drops in from above over this long. */
    private const val BAG_DROP_MS = 220

    /** [s] as it stands now; [clock] keeps what's pouring on the move. */
    fun draw(f: Frame, s: PropState, clock: Long) {
        val b = s.prop.box
        when (s.prop.name) {
            OATS, RICE -> sack(f, s, clock)
            EGGS -> eggs(f, s)
            TILL -> till(f, s)
            BAG -> bag(f, s)
            // Shot off the shelf or the wall, and gone for good; the clock leaves its nail.
            SHELF -> if (!s.shot) f.put(goods.getValue(s.prop), b.x, b.y)
            CLOCK -> if (!s.shot) f.put(clockFace, b.x, b.y) else f.box(b.x + 10, b.y + 2, 1, 2, f.look.ink)
            GLASS -> if (!s.shot) f.outline(b.x + 1, b.y + 1, 5, 6, f.look.ink)
            SPITTOON -> if (!s.shot) f.put(spittoon, b.x, b.y)
            MIRROR -> f.put(if (s.shot) mirrorCracked else mirror, b.x, b.y)
            SIGN -> sign(f, s, signWhole, SIGN_BREAK)
            JAIL_SIGN -> sign(f, s, jailSign, JAIL_BREAK)
            KEG -> {
                f.put(keg, b.x, b.y)
                if (s.shot) { hole(f, b.x + 12, b.y + 8); pour(f, b.x + 12, b.y + 10, b.y + b.h - 1, s.since, clock, 7) }
            }
            TROUGH -> {
                f.put(if (s.shot) troughBroken else trough, b.x, b.y)
                if (s.shot) pour(f, b.x + 16, b.y + 14, b.y + b.h - 1, s.since, clock, 12)
            }
            BARRELS -> {
                f.put(if (s.shot) barrelsBroken else barrels, b.x, b.y)
                if (s.shot) {
                    pour(f, b.x + 6, b.y + 24, b.y + b.h - 1, s.since, clock, 5)
                    pour(f, b.x + 27, b.y + 25, b.y + b.h - 1, s.since + 300, clock, 6)
                }
            }
        }
    }

    /** What flies when [s] is shot at ([x], [y]), and the sound it makes. */
    fun shot(s: PropState, x: Int, y: Int, juice: Juice): Fx {
        val b = s.prop.box
        val cx = b.x + b.w / 2; val cy = b.y + b.h / 2
        return when (s.prop.name) {
            OATS, RICE -> {
                juice.sparks(holeX(s), y, n = 7, force = 0.06f)
                Fx.Spill
            }
            EGGS -> {
                // The corner that's stove in goes off in splinters.
                juice.shatter(crateWhole.cut(0, 13, 16, 15), b.x, b.y + 13, cell = 4, force = 0.07f, lift = 0.07f)
                juice.sparks(b.x + 6, b.y + b.h - 8, n = 6, force = 0.08f)
                Fx.Crack
            }
            TILL -> {
                juice.money(TILL_X + 13, TILL_Y + 19, coins = 4, bills = 1, force = 0.11f)
                Fx.Till
            }
            BAG -> {
                juice.shatter(WesternArt.moneyBag, b.x, b.y, cell = 4, force = 0.1f, lift = 0.09f)
                juice.ring(cx, cy, to = 15)
                juice.money(cx, cy, coins = 7, bills = 3, force = 0.14f)
                Fx.Jingle
            }
            // Knocked flying whole, turning over, away from the middle of the room.
            SHELF -> { juice.hop(goods.getValue(s.prop), b.x, b.y, toward = G.W - cx); Fx.Clink }
            CLOCK -> { juice.hop(clockFace, b.x, b.y, toward = x); juice.sparks(cx, cy, n = 8, force = 0.1f); Fx.Ting }
            SPITTOON -> { juice.hop(spittoon, b.x, b.y, toward = G.W - cx); Fx.Ting }
            GLASS -> {
                juice.shatter(glass, b.x + 1, b.y + 1, cell = 2, force = 0.08f, lift = 0.07f)
                juice.sparks(cx, cy, n = 5, force = 0.1f)
                Fx.Clink
            }
            MIRROR -> {
                juice.sparks(x, y, n = 12, force = 0.12f)
                juice.shatter(mirror.cut(8, 6, 8, 8), b.x + 8, b.y + 6, cell = 2, force = 0.09f, lift = 0.05f)
                Fx.Crack
            }
            SIGN -> {
                juice.shatter(signWhole.cut(SIGN_BREAK - 2, 0, 8, 12), b.x + SIGN_BREAK - 2, b.y, cell = 3, force = 0.09f, lift = 0.06f)
                juice.sparks(b.x + SIGN_BREAK + 2, cy, n = 8, force = 0.1f)
                Fx.Crack
            }
            JAIL_SIGN -> {
                juice.shatter(jailSign.cut(JAIL_BREAK - 2, 0, 8, 15), b.x + JAIL_BREAK - 2, b.y, cell = 3, force = 0.09f, lift = 0.07f)
                juice.sparks(b.x + JAIL_BREAK + 2, cy, n = 8, force = 0.1f)
                Fx.Crack
            }
            KEG -> { juice.sparks(b.x + 12, b.y + 8, n = 7, force = 0.08f); Fx.Water }
            TROUGH -> {
                juice.shatter(trough.cut(13, 0, 8, 14), b.x + 13, b.y, cell = 3, force = 0.09f, lift = 0.08f)
                juice.sparks(b.x + 16, b.y + 6, n = 10, force = 0.1f)
                Fx.Water
            }
            else -> {
                juice.shatter(barrels.cut(3, 20, 8, 8), b.x + 3, b.y + 20, cell = 3, force = 0.09f, lift = 0.08f)
                juice.shatter(barrels.cut(24, 21, 8, 8), b.x + 24, b.y + 21, cell = 3, force = 0.09f, lift = 0.08f)
                juice.sparks(cx, cy, n = 8, force = 0.1f)
                Fx.Water
            }
        }
    }

    // ---- Things that leak: a hole, what runs out of it for a while, then a drip, and the puddle that stays.

    private const val LEAK_MS = 3200

    private fun hole(f: Frame, x: Int, y: Int) {
        f.box(x - 1, y - 1, 4, 3, f.look.ink); f.box(x, y, 2, 1, f.look.bg)
    }

    /** Water (or beer) from ([x], [y]) down to the [floor], [since] it began, and a puddle up to [spread] each way. */
    private fun pour(f: Frame, x: Int, y: Int, floor: Int, since: Int, clock: Long, spread: Int) {
        val ink = f.look.ink; val bg = f.look.bg
        if (since < 0) return
        if (since < LEAK_MS) {
            val step = (clock / 30).toInt()
            f.box(x - 1, y, 4, floor - y, bg)
            for (yy in y until floor) for (xx in x..x + 1) if (Math.floorMod(yy - step + (xx - x), 3) != 0) f.dot(xx, yy, ink)
        } else {
            // A drip now and then, for as long as anyone's looking.
            val t = (clock % 1100).toInt()
            if (t < 400) f.dot(x, y + (floor - y) * t / 400, ink)
        }
        val half = (spread * min(1f, since / LEAK_MS.toFloat())).roundToInt()
        f.box(x - half, floor, half * 2 + 2, 1, ink)
    }

    // ---- The jailhouse: the water trough and the barrels. Shot, they're stove in and leaking, and stay that way.

    private fun troughPad() = Pad(36, 22).apply {
        card(0, 0, 36, 14); outline(2, 2, 32, 2)
        card(4, 14, 4, 8); card(28, 14, 4, 8)
        box(4, 7, 28, 1); box(4, 10, 28, 1)
    }
    private val trough: Sprite by lazy { troughPad().sprite() }
    private val troughBroken: Sprite by lazy {
        troughPad().apply {
            // Split down the middle: a ragged gap, the boards each side knocked out of line, one hanging off.
            box(14, 1, 6, 12, false)
            for ((x0, y0, x1, y1) in listOf(listOf(14, 0, 12, 4), listOf(12, 4, 15, 8), listOf(15, 8, 13, 13),
                listOf(20, 0, 21, 5), listOf(21, 5, 19, 9), listOf(19, 9, 21, 13))) line(x0, y0, x1, y1)
            box(22, 8, 10, 1, false); box(22, 9, 10, 1); box(4, 10, 9, 1, false); box(4, 11, 9, 1)
            line(15, 14, 11, 20); line(16, 14, 12, 20)
        }.sprite()
    }
    private fun barrelsPad() = Pad(36, 30).apply {
        stamp(WesternScenes.BARREL, 0, 15); stamp(WesternScenes.BARREL, 18, 15); stamp(WesternScenes.BARREL, 9, 0)
    }
    private val barrels: Sprite by lazy { barrelsPad().sprite() }
    private val barrelsBroken: Sprite by lazy {
        barrelsPad().apply {
            // A stave stove in on each of the two below, and the one on top cracked across.
            for ((x, y) in listOf(4 to 20, 25 to 21)) {
                box(x, y, 6, 5, false)
                line(x, y, x + 2, y + 2); line(x + 2, y + 2, x, y + 4); line(x + 5, y, x + 3, y + 2); line(x + 3, y + 2, x + 5, y + 4)
            }
            line(12, 3, 16, 7); line(16, 7, 14, 10); line(14, 10, 19, 13)
        }.sprite()
    }

    // ---- The General Store's clock and the goods on its shelves: each can be shot off, once, and doesn't come back.

    private val clockFace: Sprite by lazy {
        Pad(21, 21).apply {
            disc(10, 10, 9, false); ring(10, 10, 9); ring(10, 10, 8)
            box(10, 4, 1, 6); box(10, 10, 5, 1)
        }.sprite()
    }
    val CLOCK_BOX = Box(98, 34, 21, 21)

    private val goods = HashMap<Prop, Sprite>()

    /** Everything on the store's shelves, as props worth [points] each: jars, cartons and little sacks. */
    fun shelfGoods(points: Int): List<Prop> {
        val out = ArrayList<Prop>()
        fun add(x: Int, bottom: Int, w: Int, h: Int, draw: Pad.() -> Unit) {
            val prop = Prop(SHELF, Box(x, bottom - h, w, h), points = points)
            goods[prop] = Pad(w, h).apply(draw).sprite()
            out += prop
        }
        fun jar(x: Int, bottom: Int, w: Int, h: Int) = add(x, bottom, w, h) { WesternScenes.jar(this, 0, h, w, h) }
        fun carton(x: Int, bottom: Int, w: Int, h: Int) = add(x, bottom, w, h) { WesternScenes.carton(this, 0, h, w, h) }
        fun sack(x: Int, bottom: Int, w: Int, h: Int) = add(x, bottom, w, h) { WesternScenes.sack(this, 0, h, w, h) }
        // The left shelves, board by board; then the right. The third board each side is left for the cans.
        for (i in 0 until 3) jar(7 + i * 9, 52, 7, 13)
        carton(6, 72, 12, 11); carton(20, 72, 12, 14)
        sack(5, 108, 14, 16); sack(19, 108, 14, 16)
        // Two cartons, one on the other: they go together.
        add(182, 52, 26, 18) { WesternScenes.carton(this, 0, 18, 26, 9); WesternScenes.carton(this, 5, 9, 16, 9) }
        for (i in 0 until 3) jar(183 + i * 9, 72, 7, 15)
        jar(182, 108, 12, 16); carton(196, 108, 12, 13)
        return out
    }

    // ---- The Rusty Spur: the mirror behind the bar, the glasses on the shelf, a keg and a spittoon on the floor.

    private val mirror: Sprite by lazy {
        Pad(24, 20).apply { card(0, 0, 24, 20); line(4, 16, 17, 3); line(9, 16, 19, 6) }.sprite()
    }
    private val mirrorCracked: Sprite by lazy {
        Pad(24, 20).apply {
            card(0, 0, 24, 20)
            for ((x, y) in listOf(1 to 2, 12 to 1, 22 to 4, 22 to 13, 17 to 18, 5 to 18, 1 to 11)) line(12, 10, x, y)
            line(6, 6, 9, 3); line(17, 7, 20, 10); line(8, 14, 11, 16)
        }.sprite()
    }
    private val glass: Sprite by lazy { Pad(5, 6).apply { outline(0, 0, 5, 6) }.sprite() }
    val GLASS_XS = listOf(26, 56, 136, 186)
    val MIRROR_BOX = Box(96, 85, 24, 20)
    val KEG_BOX = Box(48, 203, 18, 15)
    val SPITTOON_BOX = Box(68, 210, 9, 8)
    private val keg: Sprite by lazy { Sprite.of(WesternScenes.BARREL) }
    private val spittoon: Sprite = Sprite.of(art("""
        .#######.
        ..#ooo#..
        ..#ooo#..
        .#ooooo#.
        #ooooooo#
        #ooooooo#
        .#ooooo#.
        ..#####..
    """))

    // ---- The bank's sign: shot, it breaks in two, and the right half hangs down by its far end.

    private const val SIGN_TEXT = "DRY GULCH BANK"
    val SIGN_BOX = Box(52, 36, 112, 12)
    /** How far along it breaks, and how long the loose half takes to drop. */
    private const val SIGN_BREAK = 54
    private const val SIGN_DROP_MS = 260
    private val signWhole: Sprite by lazy {
        Pad(112, 12).apply {
            card(0, 0, 112, 12); outline(2, 2, 108, 8)
            text(SIGN_TEXT, 56 - Font.width(SIGN_TEXT) / 2, 2)
        }.sprite()
    }

    /** The sign on the jailhouse roof, which breaks the same way. */
    val JAIL_SIGN_BOX = Box(86, 28, 44, 15)
    private const val JAIL_BREAK = 20
    private val jailSign: Sprite by lazy {
        Pad(44, 15).apply {
            card(0, 0, 44, 15); outline(2, 2, 40, 11)
            west("JAIL", West.left("JAIL", 22), 3)
        }.sprite()
    }

    /** [whole] where [s] is; or, shot, broken at [at] across, with what's right of the break sagging. */
    private fun sign(f: Frame, s: PropState, whole: Sprite, at: Int) {
        val signWhole = whole
        val SIGN_BREAK = at
        val b = s.prop.box
        if (!s.shot) { f.put(signWhole, b.x, b.y); return }
        val ink = f.look.ink; val bg = f.look.bg
        val p = min(1f, s.since.toFloat() / SIGN_DROP_MS)
        for (i in 0 until signWhole.w) {
            // A ragged gap where it broke; left of it stays up, right of it sags, most at the break.
            val gap = i - SIGN_BREAK
            if (gap in 0..3) continue
            val jag = if (gap == -1 || gap == 4) 3 else if (gap == -2 || gap == 5) 1 else 0
            val dy = if (gap < 0) 0 else (p * (7 - (i - SIGN_BREAK) * 7 / (signWhole.w - SIGN_BREAK))).roundToInt()
            for (j in 0 until signWhole.h) {
                if (jag > 0 && (j + i) % 4 < jag) continue
                val v = signWhole.px[j * signWhole.w + i]
                if (v == INK) f.dot(b.x + i, b.y + j + dy, ink) else if (v == PAPER) f.dot(b.x + i, b.y + j + dy, bg)
            }
        }
    }

    // ---- The sacks: a hole where the shot went in, the grain running out into a heap, the sack sinking as it empties.

    const val SACK_W = 27
    const val SACK_H = 32
    /** How long a sack takes to run out, and how far it sinks doing it. */
    private const val POUR_MS = 1500
    private const val SLUMP = 5

    private val sacks = HashMap<Pair<String, Int>, Sprite>()

    private fun sackPicture(label: String, drop: Int) = sacks.getOrPut(label to drop) {
        Pad(SACK_W, SACK_H).apply { WesternScenes.sack(this, 0, SACK_H, SACK_W, SACK_H - drop, label) }.sprite()
    }

    /** The hole goes where the shot did, as near as the cloth allows. */
    private fun holeX(s: PropState) = s.hitX.coerceIn(s.prop.box.x + 5, s.prop.box.x + s.prop.box.w - 7)

    private fun sack(f: Frame, s: PropState, clock: Long) {
        val b = s.prop.box
        val label = s.prop.name.uppercase()
        if (!s.shot) { f.put(sackPicture(label, 0), b.x, b.y); return }
        val ink = f.look.ink; val bg = f.look.bg
        val oats = s.prop.name == OATS
        val p = min(1f, s.since.toFloat() / POUR_MS)
        val drop = (SLUMP * p).roundToInt()
        f.put(sackPicture(label, drop), b.x, b.y)
        val floor = b.y + b.h - 1
        // The hole: across from where the shot went in, up above the word on the sack, coming down as the sack does.
        val hx = holeX(s)
        val hy = b.y + HOLE_Y + drop
        for (x in hx..hx + 1) { f.dot(x, hy - 1, ink); f.dot(x, hy + 1, ink); f.dot(x, hy, bg) }
        f.dot(hx - 1, hy, ink); f.dot(hx + 2, hy, ink)

        // The heap in front of the sack, growing as it runs out. Grain is pale: the heap is cut out of paper, as the
        // snails are, so on the dark page it's a solid white mound with a grain picked out here and there.
        val mx = hx.coerceIn(b.x + 11, b.x + b.w - 12)
        val half = (4 + 7 * p).roundToInt()
        val tall = 2 + 5 * p
        fun high(i: Int): Int {
            if (i < -half || i > half) return 0
            val u = i / (half + 0.5)
            return maxOf(1, (tall * sqrt(1.0 - u * u)).roundToInt())
        }
        val edge = if (f.look.dark) WHITE else BLACK
        // The grain still running from the hole down onto the heap, in front of the sack.
        if (p < 1f) {
            val thin = s.since > POUR_MS - 320
            val wide = if (thin) 0 else 1
            val step = (clock / 30).toInt()
            val to = floor - high(hx - mx)
            f.box(hx - 1, hy + 2, wide + 3, to - hy - 1, bg)
            for (x in hx..hx + wide) for (y in hy + 1..to) {
                val on = if (oats) Math.floorMod(y - step + (x - hx) * 2, 4) < 2 else Math.floorMod(y - step + (x - hx), 2) == 0
                if (on) f.dot(x, y, ink)
            }
        }
        for (i in -half..half) {
            val h = high(i)
            val open = min(high(i - 1), high(i + 1))
            for (j in 0 until h) {
                val rim = j == h - 1 || j >= open
                val grain = !rim && j > 0 && (if (oats) Math.floorMod(i * 3 + j * 5, 7) == 0 else Math.floorMod(i * 5 + j * 3, 6) == 0)
                f.dot(mx + i, floor - j, if (rim) edge else if (grain) BLACK else WHITE)
            }
        }
    }

    /** How far down a sack its hole is: above the word on it, so the word can still be read. */
    private const val HOLE_Y = 10

    // ---- The eggs: the crate's near corner stove in, and the eggs rolling out of it over the floor, to stay there.

    private val crateWhole: Sprite by lazy { Pad(46, 28).apply { WesternScenes.crate(this, 0, 0, 46, 28, "EGGS") }.sprite() }

    private val crateBroken: Sprite by lazy {
        Pad(46, 28).apply {
            WesternScenes.crate(this, 0, 0, 46, 28, "EGGS")
            // A ragged bite out of the bottom-left corner: everything this side of a jagged line is gone.
            val jag = listOf(1 to 13, 5 to 16, 4 to 19, 9 to 20, 8 to 23, 13 to 23, 12 to 26, 16 to 27)
            val edge = Pad(46, 28)
            for (k in 0 until jag.size - 1) edge.line(jag[k].first, jag[k].second, jag[k + 1].first, jag[k + 1].second)
            for (y in 13 until 28) {
                val far = (0 until 46).lastOrNull { edge.get(it, y) } ?: continue
                box(0, y, far, 1, false)
            }
            for (y in 0 until 28) for (x in 0 until 46) if (edge.get(x, y)) dot(x, y)
            // A split board hanging off it.
            line(18, 26, 22, 22); line(20, 27, 25, 24)
        }.sprite()
    }

    private val EGG_UP = Sprite.of(art("""
        .###.
        #ooo#
        #ooo#
        #ooo#
        #ooo#
        #ooo#
        .###.
    """))
    private val EGG_SIDE = Sprite.of(art("""
        .#####.
        #ooooo#
        #ooooo#
        #ooooo#
        .#####.
    """))
    /** How far left of the crate each egg comes to rest, the nearest first. */
    private val EGG_RESTS = intArrayOf(12, 21, 31, 40)

    private fun eggs(f: Frame, s: PropState) {
        val b = s.prop.box
        if (!s.shot) { f.put(crateWhole, b.x, b.y); return }
        f.put(crateBroken, b.x, b.y)
        val floor = b.y + b.h - 1
        // White eggs: solid on the dark page, lined on the light one.
        val ink = if (f.look.dark) WHITE else BLACK
        val from = b.x + 4
        for (i in EGG_RESTS.indices) {
            val t = s.since - 120 - i * 260
            if (t < 0) continue
            // The first one out goes furthest.
            val far = EGG_RESTS.size - 1 - i
            val rest = b.x - EGG_RESTS[far]
            val p = min(1f, t / (520f + far * 110f))
            val eased = 1f - (1f - p) * (1f - p)
            val x = (from + (rest - from) * eased).roundToInt()
            // End over end as it goes; on its side once it stops.
            val egg = if (p < 1f && ((from - x) / 4) % 2 == 1) EGG_UP else EGG_SIDE
            f.put(egg, x, floor - egg.h + 1, ink = ink, paper = WHITE)
        }
    }

    // ---- The till: every shot rings it up. The drawer shoots out, the lever comes down, and the number changes.

    /** The till's top-left corner on the counter; the still picture draws its case, this draws what moves. */
    const val TILL_X = 108
    const val TILL_Y = 115
    private const val RING_MS = 320

    /** What the till's display reads: 5 to begin with, and something else after every shot. */
    fun tillShows(s: PropState): Int {
        if (s.times == 0) return 5
        val now = 1 + s.roll % 99
        val before = if (s.times == 1) 5 else 1 + s.rolled % 99
        return if (now == before) now % 99 + 1 else now
    }

    private fun till(f: Frame, s: PropState) {
        val ink = f.look.ink; val bg = f.look.bg
        val x = TILL_X; val y = TILL_Y
        val rung = s.times > 0 && s.since < RING_MS
        val n = tillShows(s).toString()
        f.box(x + 4, y + 1, 18, 9, bg)
        f.font(n, x + 13 - Font.width(n) / 2, y + 2, ink)
        if (rung) {
            // The lever down, the drawer out, and the bell still ringing.
            f.line(x + 26, y + 15, x + 31, y + 13, ink); f.box(x + 30, y + 11, 3, 3, ink)
            f.box(x - 2, y + 20, 30, 6, bg); f.outline(x - 2, y + 20, 30, 6, ink); f.box(x + 11, y + 22, 4, 1, ink)
            if (s.since < 220) {
                f.line(x + 7, y - 2, x + 5, y - 5, ink); f.line(x + 13, y - 3, x + 13, y - 6, ink); f.line(x + 19, y - 2, x + 21, y - 5, ink)
            }
        } else {
            f.line(x + 26, y + 15, x + 30, y + 9, ink); f.box(x + 29, y + 7, 3, 3, ink)
        }
    }

    // ---- The money bags: there, or burst and gone, or dropping back in.

    private fun bag(f: Frame, s: PropState) {
        val b = s.prop.box
        when {
            !s.shot -> f.put(WesternArt.moneyBag, b.x, b.y)
            s.gone in 1 until BAG_DROP_MS -> f.put(WesternArt.moneyBag, b.x, b.y - s.gone * 12 / BAG_DROP_MS)
        }
    }
}
