package com.slowpixels.hatshot.game

import com.slowpixels.hatshot.game.WesternArt.Mood
import com.slowpixels.hatshot.game.WesternScenes.crag
import com.slowpixels.hatshot.game.WesternScenes.dust
import com.slowpixels.hatshot.game.WesternScenes.planks
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The story's pictures, drawn when a page is opened. The snails are the game's own sprites seen close up, two or three
 * pad pixels to each of theirs, in front of scenery drawn the way the stages are, so a page and a stage look like the
 * same game. Each page is a few frames (a blink, a wave of the feelers, a wheel turning).
 *
 * The pages are the Kompakt's, widened for the Light Phone: drawn 270 pad pixels across at four screen pixels each,
 * with the Kompakt's picture in the middle ([X] in from the left) and the scenery carried out to both edges.
 */
object Story {
    const val W = 270
    const val H = 188
    /** Where the Kompakt's 160-wide picture starts. */
    private const val X = 55
    /** Screen pixels to a pad pixel on a story page. */
    const val PX = 4

    private val HEART = art("""
        .##.##.
        #######
        #######
        .#####.
        ..###..
        ...#...
    """)

    /** Belle's bow by itself, close in. */
    private val LOST_BOW = ArtRows(WesternArt.BOW_ALONE.rows.drop(1).map { it.substring(6, 18) })

    /** Hank from the neck up, for his poster. */
    private val MUGSHOT = ArtRows(WesternArt.rows("bandana").rows.take(23))

    private fun page(draw: Pad.() -> Unit) = Pad(W, H).apply(draw)

    private fun Pad.snail(name: String, mood: Mood, x: Int, y: Int, scale: Int = 3, flip: Boolean = false) =
        cutout(Sprite.of(WesternArt.rows(name, mood), WesternArt.isOutlaw(name)), x, y, scale, flip)

    /** What somebody says, in a box with a point toward whoever said it. */
    private fun Pad.says(text: String, x: Int, y: Int, toX: Int, toY: Int) {
        val w = West.cells(text) + 12
        val h = 19
        box(x + 2, y, w - 4, h, false); box(x, y + 2, w, h - 4, false); box(x + 1, y + 1, w - 2, h - 2, false)
        box(x + 2, y, w - 4, 1); box(x + 2, y + h - 1, w - 4, 1); box(x, y + 2, 1, h - 4); box(x + w - 1, y + 2, 1, h - 4)
        dot(x + 1, y + 1); dot(x + w - 2, y + 1); dot(x + 1, y + h - 2); dot(x + w - 2, y + h - 2)
        val down = toY > y
        val from = if (down) y + h - 1 else y
        val cx = toX.coerceIn(x + 8, x + w - 8)
        val n = abs(toY - from).coerceAtLeast(1)
        for (j in 0..n) {
            val l = cx - 4 + (toX - (cx - 4)) * j / n
            val r = cx + 4 + (toX - (cx + 4)) * j / n
            val yy = if (down) from + j else from - j
            box(l, yy, r - l + 1, 1, false)
            dot(l, yy); dot(r, yy)
        }
        west(text, x + 6, y + 5)
    }

    private fun Pad.ray(cx: Int, cy: Int, degrees: Int, from: Int, to: Int) {
        val a = degrees * PI / 180
        line((cx + from * cos(a)).toInt(), (cy + from * sin(a)).toInt(), (cx + to * cos(a)).toInt(), (cy + to * sin(a)).toInt())
    }

    /** A spoked wheel; [turn] is 0 or 1, for the two pictures of it going round. */
    private fun Pad.wheel(cx: Int, cy: Int, r: Int, turn: Int) {
        disc(cx, cy, r, false)
        ring(cx, cy, r); ring(cx, cy, r - 1)
        for (i in 0 until 4) {
            val a = (i * 45 + turn * 22) * PI / 180
            val dx = ((r - 2) * cos(a)).toInt(); val dy = ((r - 2) * sin(a)).toInt()
            line(cx - dx, cy - dy, cx + dx, cy + dy)
        }
        disc(cx, cy, 2)
    }

    private fun Pad.picket(x: Int, top: Int, bottom: Int) {
        box(x + 1, top + 3, 5, bottom - top - 3, false)
        box(x + 2, top + 2, 3, 1, false); dot(x + 3, top + 1, false)
        box(x, top + 3, 1, bottom - top - 3); box(x + 6, top + 3, 1, bottom - top - 3)
        line(x, top + 3, x + 3, top); line(x + 3, top, x + 6, top + 3)
    }

    /** A swing door: a frame with slats across it. */
    private fun Pad.door(x: Int, y: Int, w: Int, h: Int) {
        card(x, y, w, h)
        var j = y + 6
        while (j < y + h - 3) { box(x + 2, j, w - 4, 1); j += 6 }
    }

    /**
     * The trail a snail leaves: a pale band with dotted edges, wandering a little and closing up as it goes away,
     * with a glint on it here and there. [glint] picks which glints are lit.
     */
    private fun Pad.trail(x0: Int, y0: Int, x1: Int, y1: Int, wide: Int, glint: Int) {
        val n = abs(y1 - y0)
        fun at(i: Int): Triple<Int, Int, Int> {
            val t = i.toFloat() / n
            return Triple(x0 + ((x1 - x0) * t).toInt() + (3 * sin(t * 9.0)).toInt(), y0 + ((y1 - y0) * t).toInt(), (wide * (1 - t * 0.6f)).toInt())
        }
        for (i in 0..n) {
            val (x, y, half) = at(i)
            box(x - half + 1, y, half * 2 - 1, 1, false)
            if (i % 3 != 2) { dot(x - half, y); dot(x + half, y) }
        }
        for (i in 0..n) if (i % 14 == 4 + glint * 7) {
            val (x, y, _) = at(i)
            box(x - 2, y, 5, 1); box(x, y - 2, 1, 5)
        }
    }

    // ---- The pages. ----

    /** A fine morning in Dry Gulch: Mosey, the new deputy, and Belle by her fence. */
    private fun meet(belle: String = "belle", her: Mood = Mood.PLAIN, mosey: Mood = Mood.PLAIN) = page {
        ring(X + 80, 24, 11)
        for (a in 0 until 12) ray(X + 80, 24, a * 30, 15, 20)
        stamp(WesternScenes.CLOUD, 14, 34); stamp(WesternScenes.CLOUD, X + 126, 14); stamp(WesternScenes.CLOUD, 238, 40)
        box(0, 132, W, 1); box(0, 133, W, 2, false); box(0, 135, W, 1)
        box(0, 154, W, 1); box(0, 155, W, 2, false); box(0, 157, W, 1)
        var x = 3
        while (x < W) { picket(x, 118, 176); x += 12 }
        box(0, 182, W, 1)
        dust(0, 183, W, 5, 5)
        snail("mosey", mosey, X + 2, 62)
        snail(belle, her, X + 86, 62, flip = true)
    }

    val MEET get() = Flip(intArrayOf(0, 0, 0, 1, 0, 1, 0, 0, 2, 0, 0, 0, 3, 0),
        listOf(meet(), meet("belle waving"), meet(her = Mood.BLINK), meet(mosey = Mood.BLINK)))

    /** Hank and the back of his wagon, with Belle in it. He's a good deal bigger than she is. */
    private fun grabbed(belle: String) = page {
        box(0, 22, W, 1)
        planks(0, 23, W, 128, 8)
        box(0, 150, W, 1)
        dust(0, 152, W, 36, 9)
        snail(belle, Mood.SCARED, X + 106, 74, scale = 2)
        card(X + 92, 124, 96, 42)
        for (y in listOf(132, 140, 148, 156)) box(X + 93, y, 94, 1)
        for ((x, y) in listOf(112 to 125, 142 to 133, 104 to 141, 150 to 149, 126 to 157, 170 to 141, 180 to 125)) box(X + x, y, 1, 7)
        wheel(X + 130, 174, 13, 0); wheel(X + 176, 174, 13, 0)
        says("HELP!", X + 100, 48, X + 128, 73)
        snail("bandana", Mood.MEAN, X + 2, 64)
    }

    val GRABBED get() = Flip(intArrayOf(0, 1), listOf(grabbed("belle"), grabbed("belle waving")))

    /** The General Store from the street, and its storekeeper pointing you in. [swing] tips the OPEN sign. */
    private fun store(swing: Int) = page {
        stamp(WesternScenes.CLOUD, 4, 2); stamp(WesternScenes.CLOUD, 244, 6)
        // The false front and its sign.
        card(X + 2, 10, 156, 140)
        card(X + 10, 14, 140, 18); outline(X + 12, 16, 136, 14)
        west("GENERAL STORE", X + 30, 19)
        planks(X + 3, 32, 154, 92, 7)
        // The buildings either side.
        card(0, 40, X, 110); planks(0, 41, X - 1, 83, 8); card(X + 158, 34, W - X - 158, 116); planks(X + 159, 35, W - X - 160, 89, 8)
        // The big window, with goods in it.
        card(X + 8, 42, 62, 56); outline(X + 10, 44, 58, 52)
        box(X + 12, 46, 54, 48, false)
        text("SALE", X + 27, 49)
        box(X + 12, 79, 54, 2)
        for (i in 0 until 4) { card(X + 15 + i * 12, 64, 8, 15); box(X + 16 + i * 12, 62, 6, 2) }
        disc(X + 38, 88, 6, false); ring(X + 38, 88, 6); line(X + 38, 88, X + 43, 84); line(X + 38, 88, X + 44, 88)
        // The door, with its glass and the sign hung in it.
        card(X + 94, 46, 40, 78); outline(X + 96, 48, 36, 76)
        box(X + 99, 51, 30, 32, false)
        shade(X + 99, 51, 30, 32) { i, j -> i % 4 == 0 && j % 4 == 0 }
        line(X + 108, 54, X + 114, 50); line(X + 120, 54, X + 114, 50)
        val tip = if (swing == 0) 0 else 1
        card(X + 103, 54 + tip, 22, 10 - tip * 2)
        text("OPEN", X + 104, 56)
        box(X + 126, 88, 3, 3)
        // The boardwalk.
        box(0, 124, W, 2)
        var x = 6
        while (x < W + 30) { line(x, 126, x - 12, 150); x += 18 }
        box(0, 150, W, 1)
        dust(0, 152, W, 36, 3)
        snail("barkeep", Mood.SCARED, X + 2, 66)
        says("IN THERE!", X + 64, 130, X + 52, 112)
    }

    val STORE get() = Flip(intArrayOf(0, 0, 1, 1), listOf(store(0), store(1)))

    /** The bank: columns, its name up top, the door open, a money bag on the step, and the clerk in a state. */
    private fun bank(roll: Int) = page {
        // The roof, and the name in it.
        line(X - 26, 30, X + 80, 4); line(X + 186, 30, X + 80, 4); box(X - 26, 30, 213, 1)
        card(X - 26, 31, 213, 6)
        west("BANK", X + 64, 15)
        // The front, between columns.
        box(X - 26, 37, 213, 1)
        for (cx in listOf(-22, 8, 44, 98, 134, 164)) {
            card(X + cx, 37, 18, 4); card(X + cx + 3, 41, 12, 89); card(X + cx, 130, 18, 4)
            for (f in listOf(6, 9, 12)) box(X + cx + f, 43, 1, 85)
        }
        // The door, open, and dark inside.
        card(X + 62, 52, 36, 82); box(X + 65, 55, 30, 79, false)
        shade(X + 65, 55, 30, 79) { i, j -> i % 3 == 0 && j % 3 == 0 }
        outline(X + 64, 54, 32, 80)
        // Steps.
        card(0, 134, W, 6); card(0, 140, W, 6); card(0, 146, W, 6)
        dust(0, 154, W, 34, 17)
        // A money bag dropped on the step, and a coin rolling off it.
        stamp(WesternArt.MONEY_BAG, X + 116, 119, false, 1)
        val cx = X + 140 + roll * 8; val cy = 130 + roll * 6
        disc(cx, cy, 4, false); ring(cx, cy, 4); box(cx - 1, cy - 1, 3, 3)
        snail("banker", Mood.SCARED, X + 2, 66)
        says("THE GOLD!", X + 58, 140, X + 52, 116)
    }

    val BANK get() = Flip(intArrayOf(0, 1), listOf(bank(0), bank(1)))

    /** The step of the Rusty Spur: Belle's bow, and the trail the gang left going in. */
    private fun bow(glint: Int) = page {
        card(X + 18, 2, 124, 16)
        west("THE RUSTY SPUR", X + 27, 6)
        box(0, 20, W, 1)
        planks(0, 21, X + 46, 119, 8); planks(X + 114, 21, W - X - 114, 119, 8)
        // The doorway, dark inside, and the doors.
        card(X + 46, 20, 68, 121); outline(X + 48, 22, 64, 119)
        shade(X + 50, 24, 60, 116) { i, j -> i % 4 == 0 && j % 4 == 0 }
        door(X + 49, 58, 29, 52); door(X + 82, 58, 29, 52)
        // Hank's poster.
        card(X + 2, 34, 42, 66)
        text("WANTED", X + 6, 37)
        box(X + 5, 46, 36, 1)
        stamp(MUGSHOT, X + 11, 50, true)
        box(X + 7, 80, 32, 1)
        box(X + 10, 86, 26, 1); box(X + 14, 92, 18, 1)
        // A window each side.
        for (wx in listOf(8, 230)) { card(wx, 40, 32, 44); outline(wx + 2, 42, 28, 40); box(wx + 15, 42, 1, 40); box(wx + 2, 62, 28, 1) }
        // The boardwalk.
        box(0, 140, W, 2)
        var x = 8
        while (x < W + 30) { line(x, 142, x - 16, H - 1); x += 20 }
        box(0, 163, W, 1); box(0, 164, W, 2, false); box(0, 166, W, 1)
        trail(X + 26, H - 1, X + 80, 142, 9, glint)
        stamp(LOST_BOW, X + 96, 148, false, 3)
    }

    val BOW get() = Flip(intArrayOf(0, 0, 1, 1), listOf(bow(0), bow(1)))

    /** The way into Buzzard Canyon, and who it's named for. */
    private fun canyon(flap: Int) = page {
        ring(X + 126, 26, 9)
        crag(-6, 30, 70, 101, 3); crag(X + 4, 50, 58, 81, 8); crag(X + 108, 48, 58, 83, 5); crag(X + 160, 34, 60, 97, 9)
        box(0, 130, W, 1)
        dust(0, 132, W, 56, 11)
        trail(X + 112, H - 1, X + 84, 131, 10, flap)
        card(X + 18, 122, 6, 66)
        card(X + 2, 94, 72, 28)
        west("BUZZARD", X + 10, 98); west("CANYON", X + 14, 109)
        stamp(if (flap == 0) WesternArt.BUZZARD_UP else WesternArt.BUZZARD_DOWN, X + 50, 6, true, 2)
    }

    val CANYON get() = Flip(intArrayOf(0, 0, 1, 1), listOf(canyon(0), canyon(1)))

    /** The side of a passenger car going by, and Belle at its window. */
    private fun window(belle: String, turn: Int) = page {
        box(0, 20, W, 3)
        var x = 0
        while (x < W) { box(x, 26, 1, 126); x += 10 }
        box(X + 22, 28, 116, 118, false)
        shade(X + 28, 34, 104, 106) { i, j -> i % 6 == 0 && j % 6 == 0 }
        within(X + 26, 32, 108, 110) { snail(belle, Mood.SCARED, X + 44, 42) }
        outline(X + 22, 28, 116, 118); outline(X + 23, 29, 114, 116); outline(X + 25, 31, 110, 112)
        card(X + 18, 146, 124, 5)
        box(0, 154, W, 2)
        wheel(X + 34, 177, 15, turn); wheel(X + 126, 177, 15, turn); wheel(X - 46, 177, 15, turn); wheel(X + 206, 177, 15, turn)
        for (y in listOf(163, 171, 181)) { box(X + 60 + turn * 8, y, 14, 1); box(X + 86 - turn * 6, y + 3, 9, 1) }
        says("HELP!", X + 100, 2, X + 98, 38)
    }

    val WINDOW get() = Flip(intArrayOf(0, 1), listOf(window("belle", 0), window("belle waving", 1)))

    /** The old jail: a stone wall, one window, and Belle behind its bars. */
    private fun jail(belle: String) = page {
        var y = 24
        var row = 0
        while (y < H) {
            box(0, y, W, 1)
            var x = (row % 2) * 12
            while (x < W) { box(x, y, 1, 10); x += 24 }
            y += 10; row++
        }
        card(X + 44, 4, 72, 17)
        west("JAIL", X + 64, 8)
        box(X + 24, 28, 112, 124, false)
        within(X + 27, 31, 106, 118) { snail(belle, Mood.WORRIED, X + 44, 42) }
        for (x in listOf(40, 58, 100, 118)) box(X + x, 31, 3, 118)
        box(X + 27, 132, 106, 3)
        outline(X + 24, 28, 112, 124); outline(X + 25, 29, 110, 122); outline(X + 26, 30, 108, 120)
        says("HURRY!", X + 92, 158, X + 84, 147)
    }

    val JAIL get() = Flip(intArrayOf(0, 0, 0, 1, 0, 1), listOf(jail("belle"), jail("belle waving")))

    /** High noon: Hank at the end of the street. [weed] is how far the tumbleweed has got; under 0 there's none. */
    private fun showdown(weed: Int) = page {
        stamp(WesternArt.SUN_ROWS, X + 72, 3)
        box(0, 112, W, 1)
        for ((x, w, top) in listOf(Triple(0, 26, 50), Triple(26, 24, 62), Triple(50, 22, 72), Triple(72, 18, 84))) {
            card(x, top, w, 113 - top); card(W - x - w, top, w, 113 - top)
            outline(x + 4, top + 6, w - 8, 10); outline(W - x - w + 4, top + 6, w - 8, 10)
        }
        line(X + 54, 112, 0, H - 30); line(X + 106, 112, W - 1, H - 30)
        dust(0, 114, W, 74, 5)
        snail("bandana", Mood.MEAN, X + 44, 66)
        says("DRAW, PARTNER.", X + 20, 26, X + 80, 62)
        if (weed >= 0) {
            val roll = generateSequence(WesternArt.TUMBLEWEED) { it.turned() }.elementAt(weed % 4)
            stamp(roll, X + 170 - weed * 36, 156, true, 2)
        }
    }

    val SHOWDOWN get() = Flip(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 7, 7, 7, 7), (0..6).map { showdown(it) } + showdown(-1))

    /** Belle out of the jail, and the whole town cheering. */
    private fun free(step: Int) = page {
        for (a in 0 until 24) ray(X + 80, 100, a * 15, 76, 160)
        // Stars going up: where each starts, how fast it climbs and how far before it starts again.
        for ((x, y, speed, rise) in listOf(listOf(X + 72, 150, 9, 34), listOf(X + 4, 44, 5, 34), listOf(X + 142, 46, 6, 18),
                listOf(14, 120, 7, 30), listOf(240, 96, 8, 26))) {
            stamp(WesternScenes.SMALL_STAR, x, y - (step * speed) % rise, true)
        }
        snail("mosey", Mood.GLAD, X + 2, 62)
        snail(if (step % 2 == 0) "belle" else "belle waving", Mood.GLAD, X + 86, 62, flip = true)
        says("THANK YOU!", X + 66, 6, X + 122, 58)
    }

    val FREE get() = Flip(intArrayOf(0, 1, 2, 3), (0..3).map { free(it) })

    /** The two of them and the sun going down. Nobody is in a hurry. */
    private fun sunset(step: Int) = page {
        disc(X + 80, 100, 58); disc(X + 80, 100, 57, false)
        for (a in 0..12) ray(X + 80, 100, 180 + a * 15, 63, 72)
        box(0, 140, W, H - 140, false)
        crag(0, 120, 52, 20, 4); crag(X + 132, 122, 28, 18, 6); crag(220, 116, 50, 24, 7)
        box(0, 140, W, 1)
        dust(0, 143, W, 45, 21)
        snail("mosey", if (step == 2) Mood.BLINK else Mood.PLAIN, X + 28, 60, scale = 2)
        snail(if (step == 1) "belle waving" else "belle", Mood.PLAIN, X + 84, 60, scale = 2, flip = true)
        stamp(HEART, X + 73, if (step == 1) 38 else 40, true, 2)
    }

    val SUNSET get() = Flip(intArrayOf(0, 0, 1, 1, 0, 0, 2, 0), listOf(sunset(0), sunset(1), sunset(2)))
}
