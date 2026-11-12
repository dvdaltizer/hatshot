package com.slowpixels.hatshot.game

/*
 * The pictures, as plain numbers: no Android here, so tests can draw every screen into a PNG.
 *
 * Sprites and pads hold three values, ink, paper and see-through, as the Kompakt game did. On the Light Phone the
 * town is line work in the page's ink ([Page]) and the snails are cut out of paper, black ink on white ([Sprite.cutout]).
 * No checker fills: the screen shows black and white only.
 */

const val INK = 0xFF000000.toInt()
const val PAPER = 0xFFFFFFFF.toInt()
const val CLEAR = 0
/** A thin line that's white over black and black over white, whatever it crosses: bars across a window. */
const val FLIP = 1
/** The page's own colour, whichever page is up: the gap a cutout keeps between itself and the town's lines. */
const val PAGE = 2
/** The page's ink, whichever page is up: line work that's part of a picture, such as bars where nobody's behind them. */
const val LINE = 3

/** Text pictures: `#` ink, `o` paper, `.` see-through, `T` and `+` the cloth colour (an outlaw's hat, beard), `:` paper. */
class ArtRows(val rows: List<String>) {
    val w = rows.maxOf { it.length }
    val h = rows.size
    fun at(x: Int, y: Int): Char = rows.getOrNull(y)?.getOrNull(x) ?: '.'

    /** This picture with [top] laid over it at ([x], [y]); see-through parts of [top] leave this one showing. */
    fun with(top: ArtRows?, x: Int = 0, y: Int = 0): ArtRows {
        if (top == null) return this
        val width = maxOf(w, x + top.w); val height = maxOf(h, y + top.h)
        return ArtRows(List(height) { j ->
            String(CharArray(width) { i -> top.at(i - x, j - y).takeIf { it != '.' } ?: at(i, j) })
        })
    }

    /** Moves everything from row [from] down by [by], leaving see-through rows behind. */
    fun drop(from: Int, by: Int) = ArtRows(List(h) { j ->
        when {
            j < from -> rows[j]
            j < from + by -> ""
            else -> rows[j - by]
        }
    })

    fun swap(a: Char, b: Char) = ArtRows(rows.map { it.replace(a, b) })
    fun mirrored() = ArtRows(rows.map { it.padEnd(w, '.').reversed() })

    /** A quarter turn clockwise. */
    fun turned() = ArtRows(List(w) { y -> String(CharArray(h) { x -> at(y, h - 1 - x) }) })
}

fun art(text: String) = ArtRows(text.trimIndent().lines().filter { it.isNotBlank() })

/** A picture ready to draw: its pixels are [INK], [PAPER] or [CLEAR]. */
class Sprite(val w: Int, val h: Int, val px: IntArray) {
    val mirrored: Sprite by lazy { Sprite(w, h, IntArray(px.size) { i -> px[(i / w) * w + (w - 1 - i % w)] }) }

    /** The same picture a quarter turn clockwise, [n] times: how a tumbling hat turns in 8-bit. */
    fun turned(n: Int): Sprite {
        var s = this
        repeat(((n % 4) + 4) % 4) { s = s.quarter() }
        return s
    }

    private val quarterTurn: Sprite by lazy {
        val out = IntArray(w * h)
        // New width is h, new height is w: (x, y) -> (h - 1 - y, x).
        for (y in 0 until h) for (x in 0 until w) out[x * h + (h - 1 - y)] = px[y * w + x]
        Sprite(h, w, out)
    }

    private fun quarter() = quarterTurn

    /** A part of this picture. */
    fun cut(x0: Int, y0: Int, cw: Int, ch: Int): Sprite {
        val out = IntArray(cw * ch)
        for (j in 0 until ch) for (i in 0 until cw) {
            val x = x0 + i; val y = y0 + j
            out[j * cw + i] = if (x in 0 until w && y in 0 until h) px[y * w + x] else CLEAR
        }
        return Sprite(cw, ch, out)
    }

    /** Whether any of it would show. */
    val blank: Boolean by lazy { px.all { it == CLEAR } }

    /**
     * This picture cut out of paper: whatever its lines shut in is paper too, and round it a white edge and then a gap
     * in the page's colour, [EDGE] pixels in all, so a black hat shows on a black window and the town's lines stop
     * short of whoever stands in front of them.
     */
    val cutout: Sprite by lazy {
        val cw = w + 2 * EDGE; val ch = h + 2 * EDGE
        val out = IntArray(cw * ch)
        for (y in 0 until h) for (x in 0 until w) out[(y + EDGE) * cw + x + EDGE] = px[y * w + x]
        // Clear reached from outside stays clear; clear shut inside the lines is paper.
        val outside = BooleanArray(cw * ch)
        val stack = ArrayDeque<Int>().apply { add(0) }
        while (stack.isNotEmpty()) {
            val k = stack.removeLast()
            if (outside[k] || out[k] != CLEAR) continue
            outside[k] = true
            val x = k % cw; val y = k / cw
            if (x > 0) stack.add(k - 1); if (x < cw - 1) stack.add(k + 1)
            if (y > 0) stack.add(k - cw); if (y < ch - 1) stack.add(k + cw)
        }
        for (k in out.indices) if (out[k] == CLEAR && !outside[k]) out[k] = PAPER
        for (ring in intArrayOf(PAPER, PAGE)) {
            val was = out.copyOf()
            for (y in 0 until ch) for (x in 0 until cw) {
                if (was[y * cw + x] != CLEAR) continue
                var near = false
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx; val ny = y + dy
                    if (nx in 0 until cw && ny in 0 until ch && was[ny * cw + nx] != CLEAR) near = true
                }
                if (near) out[y * cw + x] = ring
            }
        }
        Sprite(cw, ch, out)
    }

    companion object {
        /** How far [cutout] reaches past a picture's own edge. */
        const val EDGE = 2

        fun of(rows: ArtRows, cloth: Boolean = true): Sprite {
            val out = IntArray(rows.w * rows.h)
            for (y in 0 until rows.h) for (x in 0 until rows.w) {
                out[y * rows.w + x] = when (rows.at(x, y)) {
                    '#' -> INK
                    'o' -> PAPER
                    'T', '+' -> if (cloth) INK else PAPER
                    ':' -> PAPER
                    else -> CLEAR
                }
            }
            return Sprite(rows.w, rows.h, out)
        }
    }
}

/**
 * A drawing pad in game pixels, for the still pictures behind each stage and the story pages: drawn once, then copied
 * into a [Frame] every frame.
 */
class Pad(val w: Int, val h: Int) {
    val ink = BooleanArray(w * h)
    val touched = BooleanArray(w * h)
    /** Cells that are white (1) or black (2) whichever page is up: a snail cut out of paper, in a story picture. */
    val fixed = ByteArray(w * h)
    // Drawing only lands inside this box.
    private var left = 0
    private var top = 0
    private var right = w
    private var bottom = h

    fun dot(x: Int, y: Int, on: Boolean = true) {
        if (x in left until right && y in top until bottom) { ink[y * w + x] = on; touched[y * w + x] = true; fixed[y * w + x] = 0 }
    }

    /** Draws with everything outside the box left alone, as through a window. */
    fun within(x: Int, y: Int, bw: Int, bh: Int, draw: Pad.() -> Unit) {
        val was = intArrayOf(left, top, right, bottom)
        left = maxOf(left, x); top = maxOf(top, y); right = minOf(right, x + bw); bottom = minOf(bottom, y + bh)
        draw()
        left = was[0]; top = was[1]; right = was[2]; bottom = was[3]
    }
    fun get(x: Int, y: Int) = x in 0 until w && y in 0 until h && ink[y * w + x]

    fun box(x: Int, y: Int, bw: Int, bh: Int, on: Boolean = true) {
        for (j in y until y + bh) for (i in x until x + bw) dot(i, j, on)
    }

    fun outline(x: Int, y: Int, bw: Int, bh: Int, on: Boolean = true) {
        box(x, y, bw, 1, on); box(x, y + bh - 1, bw, 1, on); box(x, y, 1, bh, on); box(x + bw - 1, y, 1, bh, on)
    }

    /** A white box with a black line round it. */
    fun card(x: Int, y: Int, bw: Int, bh: Int) { box(x, y, bw, bh, false); outline(x, y, bw, bh) }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, on: Boolean = true) {
        var x = x0; var y = y0
        val dx = kotlin.math.abs(x1 - x0); val dy = -kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            dot(x, y, on)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    /**
     * A shape with straight sides, cleared inside with a line round it: [pts] are x, y pairs, in order round it. For
     * things seen at an angle, such as the saloon's piano.
     */
    fun poly(vararg pts: Int) {
        val n = pts.size / 2
        val top = (0 until n).minOf { pts[2 * it + 1] }
        val foot = (0 until n).maxOf { pts[2 * it + 1] }
        for (y in top..foot) {
            val xs = ArrayList<Float>()
            for (i in 0 until n) {
                val x0 = pts[2 * i]; val y0 = pts[2 * i + 1]
                val x1 = pts[2 * ((i + 1) % n)]; val y1 = pts[2 * ((i + 1) % n) + 1]
                if ((y0 <= y && y1 > y) || (y1 <= y && y0 > y)) xs += x0 + (y - y0).toFloat() / (y1 - y0) * (x1 - x0)
            }
            xs.sort()
            var k = 0
            while (k + 1 < xs.size) {
                for (x in kotlin.math.ceil(xs[k]).toInt()..kotlin.math.floor(xs[k + 1]).toInt()) dot(x, y, false)
                k += 2
            }
        }
        for (i in 0 until n) line(pts[2 * i], pts[2 * i + 1], pts[2 * ((i + 1) % n)], pts[2 * ((i + 1) % n) + 1])
    }

    /** Fills a box with dots where [on] says, leaving the rest as it was. */
    fun shade(x: Int, y: Int, bw: Int, bh: Int, on: (Int, Int) -> Boolean) {
        for (j in y until y + bh) for (i in x until x + bw) if (on(i, j)) dot(i, j)
    }

    fun disc(cx: Int, cy: Int, r: Int, on: Boolean = true) {
        for (j in -r..r) for (i in -r..r) if (i * i + j * j <= r * r + r / 2) dot(cx + i, cy + j, on)
    }

    fun ring(cx: Int, cy: Int, r: Int) { disc(cx, cy, r); disc(cx, cy, r - 1, false) }

    fun stamp(a: ArtRows, x: Int, y: Int, cloth: Boolean = true) {
        for (j in 0 until a.h) for (i in 0 until a.w) when (a.at(i, j)) {
            '#' -> dot(x + i, y + j)
            'o' -> dot(x + i, y + j, false)
            'T', '+' -> dot(x + i, y + j, cloth)
            ':' -> dot(x + i, y + j, false)
        }
    }

    /**
     * A text picture blown up, [scale] pad pixels to each of its own, as a sprite's close-up; [flip] turns it to face
     * the other way.
     */
    fun stamp(a: ArtRows, x: Int, y: Int, cloth: Boolean, scale: Int, flip: Boolean = false) {
        for (j in 0 until a.h) for (i in 0 until a.w) {
            val on = when (a.at(if (flip) a.w - 1 - i else i, j)) {
                '#' -> true
                'o' -> false
                'T', '+' -> cloth
                ':' -> false
                else -> continue
            }
            box(x + i * scale, y + j * scale, scale, scale, on)
        }
    }

    fun text(s: String, x: Int, y: Int, dot: Int = 1, on: Boolean = true) {
        s.forEachIndexed { n, ch ->
            val rows = Font.rowsOf(ch) ?: return@forEachIndexed
            rows.forEachIndexed { j, row ->
                row.forEachIndexed { i, c -> if (c == '#') box(x + (n * Font.ADV + i) * dot, y + j * dot, dot, dot, on) }
            }
        }
    }

    /** Sign-painter's capitals; [tight] closes the letters up a little where a word has to fit. */
    fun west(s: String, x: Int, y: Int, dot: Int = 1, tight: Boolean = false) {
        var cx = x
        for (ch in s) {
            if (ch == ' ') { cx += 5 * dot; continue }
            West.rows(ch)?.forEachIndexed { j, row ->
                row.forEachIndexed { i, c -> if (c == '#') box(cx + i * dot, y + j * dot, dot, dot) }
            }
            cx += West.ADV * dot - if (tight) 1 else 0
        }
    }

    /**
     * Big painted letters, centred on the page at height [y]: white with a black line round them and a thin shadow to
     * the lower right. Line work, so a big word doesn't turn into a big black block.
     */
    fun hollow(s: String, y: Int, dot: Int) {
        val gap = 2
        val cell = West.W * dot + gap * dot
        val total = s.length * cell - gap * dot
        val x0 = (w - total) / 2
        val letter = BooleanArray(w * h)
        s.forEachIndexed { n, ch ->
            West.rows(ch)?.forEachIndexed { j, row ->
                row.forEachIndexed { i, c ->
                    if (c == '#') for (b in 0 until dot) for (a in 0 until dot) {
                        val px = x0 + n * cell + i * dot + a; val py = y + j * dot + b
                        if (px in 0 until w && py in 0 until h) letter[py * w + px] = true
                    }
                }
            }
        }
        fun near(px: Int, py: Int, r: Int): Boolean {
            for (b in -r..r) for (a in -r..r) {
                val qx = px + a; val qy = py + b
                if (qx in 0 until w && qy in 0 until h && letter[qy * w + qx]) return true
            }
            return false
        }
        for (py in y - 4 until y + 9 * dot + 5) for (px in 0 until w) {
            if (py !in 0 until h) continue
            val inside = letter[py * w + px]
            val line = near(px, py, 1)
            val shadow = !line && (near(px - 2, py - 2, 1) || near(px - 1, py - 1, 1))
            when {
                inside -> dot(px, py, false)
                line || shadow -> dot(px, py)
                near(px, py, 3) -> dot(px, py, false)
            }
        }
    }

    fun sprite() = Sprite(w, h, IntArray(w * h) { i -> if (!touched[i]) CLEAR else if (ink[i]) INK else PAPER })

    /**
     * [s] cut out of paper as it is in play ([Sprite.cutout]), [scale] pad pixels to each of its own: black ink on
     * white whichever page is up, with the gap round it cleared back to the page.
     */
    fun cutout(s: Sprite, x: Int, y: Int, scale: Int = 1, flip: Boolean = false) {
        val c = s.cutout
        for (j in 0 until c.h) for (i in 0 until c.w) {
            val v = c.px[j * c.w + (if (flip) c.w - 1 - i else i)]
            if (v == CLEAR) continue
            for (b in 0 until scale) for (a in 0 until scale) {
                val px = x + (i - Sprite.EDGE) * scale + a; val py = y + (j - Sprite.EDGE) * scale + b
                if (px !in left until right || py !in top until bottom) continue
                val k = py * w + px
                when (v) {
                    INK -> fixed[k] = 2
                    PAPER -> fixed[k] = 1
                    else -> { fixed[k] = 0; touched[k] = false; ink[k] = false }
                }
            }
        }
    }

}
