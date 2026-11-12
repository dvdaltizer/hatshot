package com.slowpixels.hatshot.game

const val BLACK = 0xFF000000.toInt()
const val WHITE = 0xFFFFFFFF.toInt()

/**
 * The page: black or white, and nothing else is ever drawn but black and white. The town is line work in the page's
 * ink with nothing filled in, so the dark page is mostly black, as LightOS itself is. Snails and props are cut out of
 * paper ([Sprite.cutout]), black ink on white as on the Kompakt, so they're the only solid things in the picture:
 * Light's hollow shapes that fill in when they matter.
 */
class Look(
    val bg: Int,
    /** Everything drawn straight on the page: the town's lines, dust, sparks, words, the bars. */
    val ink: Int,
    val dark: Boolean,
) {
    companion object {
        val DARK = Look(BLACK, WHITE, true)
        val LIGHT = Look(WHITE, BLACK, false)
    }
}

/** A pad coloured once for a [Look], ready to be copied into a frame: its lines in [ink], everything else the page. */
class Page(pad: Pad, look: Look, ink: Int = look.ink) {
    val w = pad.w
    val h = pad.h
    val px = IntArray(w * h) { i ->
        when (pad.fixed[i].toInt()) {
            1 -> WHITE
            2 -> BLACK
            else -> if (pad.touched[i] && pad.ink[i]) ink else look.bg
        }
    }
}

/**
 * One picture of the game in game pixels, drawn from scratch every frame and then blown up to the screen with no
 * smoothing, so every game pixel lands as a crisp square. 216 × 248 is 53,568 numbers: copying the scenery in and
 * drawing a dozen sprites over it takes well under a millisecond.
 */
class Frame(val w: Int, val h: Int) {
    val px = IntArray(w * h)
    var look: Look = Look.DARK

    private var cl = 0
    private var ct = 0
    private var cr = w
    private var cb = h

    /** Draws only inside this box until [unclip]. */
    fun clip(x: Int, y: Int, bw: Int, bh: Int) {
        cl = maxOf(0, x); ct = maxOf(0, y); cr = minOf(w, x + bw); cb = minOf(h, y + bh)
    }

    fun unclip() { cl = 0; ct = 0; cr = w; cb = h }

    inline fun clipped(x: Int, y: Int, bw: Int, bh: Int, draw: () -> Unit) {
        clip(x, y, bw, bh); draw(); unclip()
    }

    fun fill(c: Int = look.bg) = px.fill(c)

    fun page(p: Page) {
        if (p.w == w && p.h == h) System.arraycopy(p.px, 0, px, 0, px.size)
        else for (y in 0 until minOf(h, p.h)) System.arraycopy(p.px, y * p.w, px, y * w, minOf(w, p.w))
    }

    fun dot(x: Int, y: Int, c: Int) {
        if (x in cl until cr && y in ct until cb) px[y * w + x] = c
    }

    fun box(x: Int, y: Int, bw: Int, bh: Int, c: Int) {
        val x0 = maxOf(cl, x); val y0 = maxOf(ct, y); val x1 = minOf(cr, x + bw); val y1 = minOf(cb, y + bh)
        for (j in y0 until y1) { val row = j * w; for (i in x0 until x1) px[row + i] = c }
    }

    fun outline(x: Int, y: Int, bw: Int, bh: Int, c: Int) {
        box(x, y, bw, 1, c); box(x, y + bh - 1, bw, 1, c); box(x, y, 1, bh, c); box(x + bw - 1, y, 1, bh, c)
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        var x = x0; var y = y0
        val dx = kotlin.math.abs(x1 - x0); val dy = -kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            dot(x, y, c)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    /** Draws [s] with its top-left at ([x], [y]): its ink in [ink], its paper in [paper]. */
    fun put(s: Sprite, x: Int, y: Int, flip: Boolean = false, ink: Int = look.ink, paper: Int = look.bg) {
        val src = s.px
        val x0 = maxOf(cl, x); val y0 = maxOf(ct, y); val x1 = minOf(cr, x + s.w); val y1 = minOf(cb, y + s.h)
        for (j in y0 until y1) {
            val srow = (j - y) * s.w
            val drow = j * w
            for (i in x0 until x1) {
                val sx = if (flip) s.w - 1 - (i - x) else i - x
                when (src[srow + sx]) {
                    INK -> px[drow + i] = ink
                    PAPER -> px[drow + i] = paper
                    FLIP -> px[drow + i] = if (px[drow + i] == BLACK) WHITE else BLACK
                    PAGE -> px[drow + i] = look.bg
                    LINE -> px[drow + i] = look.ink
                }
            }
        }
    }

    /**
     * [s] cut out of paper: black ink on white, with a thin white edge so a black hat shows on a black window, and the
     * page's colour round that, so the town's lines stop short of it. How every snail and prop is drawn.
     */
    fun cut(s: Sprite, x: Int, y: Int, flip: Boolean = false) =
        put(s.cutout, x - Sprite.EDGE, y - Sprite.EDGE, flip, BLACK, WHITE)

    /** [s] cut out of paper, each of its pixels [scale] game pixels across: a close-up of somebody. */
    fun cutBig(s: Sprite, x: Int, y: Int, scale: Int, flip: Boolean = false) =
        putBig(s.cutout, x - Sprite.EDGE * scale, y - Sprite.EDGE * scale, scale, flip, BLACK, WHITE)

    /** Black to white or white to black at ([x], [y]): a thin bar that shows over anything. */
    fun flip(x: Int, y: Int) {
        if (x in cl until cr && y in ct until cb) { val k = y * w + x; px[k] = if (px[k] == BLACK) WHITE else BLACK }
    }

    /** [s] with each of its pixels [scale] game pixels across, for close-ups. */
    fun putBig(s: Sprite, x: Int, y: Int, scale: Int, flip: Boolean = false, ink: Int = look.ink, paper: Int = look.bg) {
        for (j in 0 until s.h) for (i in 0 until s.w) {
            val c = s.px[j * s.w + (if (flip) s.w - 1 - i else i)]
            if (c == INK) box(x + i * scale, y + j * scale, scale, scale, ink)
            else if (c == PAPER) box(x + i * scale, y + j * scale, scale, scale, paper)
        }
    }

    /** Only [s]'s ink, in [c]: a silhouette, for a flash. */
    fun putInk(s: Sprite, x: Int, y: Int, c: Int, flip: Boolean = false) {
        val x0 = maxOf(cl, x); val y0 = maxOf(ct, y); val x1 = minOf(cr, x + s.w); val y1 = minOf(cb, y + s.h)
        for (j in y0 until y1) for (i in x0 until x1) {
            val sx = if (flip) s.w - 1 - (i - x) else i - x
            if (s.px[(j - y) * s.w + sx] != CLEAR) px[j * w + i] = c
        }
    }

    /** Sign-painter's capitals with their top-left at ([x], [y]). */
    fun west(text: String, x: Int, y: Int, c: Int = look.ink, dot: Int = 1) {
        var cx = x
        for (ch in text) {
            if (ch == ' ') { cx += West.SPACE * dot; continue }
            West.rows(ch)?.forEachIndexed { j, row ->
                row.forEachIndexed { i, k -> if (k == '#') box(cx + i * dot, y + j * dot, dot, dot, c) }
            }
            cx += West.ADV * dot
        }
    }

    fun westCenter(text: String, cx: Int, y: Int, c: Int = look.ink, dot: Int = 1) = west(text, West.left(text, cx, dot), y, c, dot)

    /** The small 5 × 7 letters. */
    fun font(text: String, x: Int, y: Int, c: Int = look.ink, dot: Int = 1) {
        text.forEachIndexed { n, ch ->
            Font.rowsOf(ch)?.forEachIndexed { j, row ->
                row.forEachIndexed { i, k -> if (k == '#') box(x + (n * Font.ADV + i) * dot, y + j * dot, dot, dot, c) }
            }
        }
    }

    /** Moves the whole picture by ([dx], [dy]), filling what's uncovered with the page: a shake. */
    fun shift(dx: Int, dy: Int, top: Int = 0, bottom: Int = h) {
        if (dx == 0 && dy == 0) return
        val copy = px.copyOfRange(top * w, bottom * w)
        for (y in top until bottom) for (x in 0 until w) {
            val sx = x - dx; val sy = y - dy
            px[y * w + x] = if (sx in 0 until w && sy in top until bottom) copy[(sy - top) * w + sx] else look.bg
        }
    }
}
