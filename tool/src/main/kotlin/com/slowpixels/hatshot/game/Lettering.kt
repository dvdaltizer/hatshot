package com.slowpixels.hatshot.game

/**
 * Words inside the game, in the game's own pixel letters, laid out one way everywhere: centred, headings in the
 * sign-painter's capitals ([West]), sentences in the small letters ([Font]), and nothing in grey. The rule (from a
 * design review of the screens): what happens in Dry Gulch speaks in pixels (the story, the card before a stage, the
 * pause, the results); LightOS's own lists (stages, settings, how to play, about) keep Light's type, so they sit beside
 * Light's own tools.
 */
object Lettering {
    /** A line of small letters is this many game pixels below the one before it, at one dot a pixel. */
    const val LINE = Font.CAP + 4

    /** Breaks [text] into lines no wider than [maxW] game pixels in the small letters at [dot]. */
    fun lines(text: String, maxW: Int, dot: Int = 1): List<String> {
        val out = ArrayList<String>()
        var line = ""
        for (word in text.split(' ').filter { it.isNotEmpty() }) {
            val tryLine = if (line.isEmpty()) word else "$line $word"
            if (Font.width(tryLine, dot) > maxW && line.isNotEmpty()) { out += line; line = word } else line = tryLine
        }
        if (line.isNotEmpty()) out += line
        return out
    }

    /** [text] in the small letters, wrapped to [maxW] and centred on [cx], from [y] down; returns where it ended. */
    fun paragraph(f: Frame, text: String, cx: Int, y: Int, maxW: Int, dot: Int = 1, c: Int = f.look.ink): Int {
        var yy = y
        for (l in lines(text, maxW, dot)) {
            f.font(l, cx - Font.width(l, dot) / 2, yy, c, dot)
            yy += LINE * dot
        }
        return yy - (LINE - Font.CAP) * dot
    }

    /** How tall [text] comes out as a [paragraph]. */
    fun height(text: String, maxW: Int, dot: Int = 1) = lines(text, maxW, dot).size * LINE * dot - (LINE - Font.CAP) * dot

    /** A heading in the sign-painter's capitals, as big as fits in [maxW] up to [most] dots a pixel; returns its foot. */
    fun heading(f: Frame, text: String, cx: Int, y: Int, maxW: Int, most: Int = 2, c: Int = f.look.ink): Int {
        var dot = most
        while (dot > 1 && West.cells(text) * dot > maxW) dot--
        f.westCenter(text, cx, y, c, dot)
        return y + West.H * dot
    }

    /** A word to tap, in the sign-painter's capitals, underlined as LightOS marks what can be tapped. */
    fun button(f: Frame, text: String, cx: Int, y: Int, dot: Int = 1, c: Int = f.look.ink) {
        val x = West.left(text, cx, dot)
        f.west(text, x, y, c, dot)
        f.box(x, y + (West.H + 2) * dot, West.cells(text) * dot, dot, c)
    }

    /** A card: the page, a line round it, and a second line just inside, as the signs in town are drawn. */
    fun panel(f: Frame, x: Int, y: Int, w: Int, h: Int) {
        f.box(x, y, w, h, f.look.bg)
        f.outline(x, y, w, h, f.look.ink)
        f.outline(x + 2, y + 2, w - 4, h - 4, f.look.ink)
        // Corners cut, so it isn't a hard box.
        for ((cx, cy) in listOf(x to y, x + w - 1 to y, x to y + h - 1, x + w - 1 to y + h - 1)) f.dot(cx, cy, f.look.bg)
    }

    /** A small star, filled when [on]: for the stars a stage has won. */
    val starOn = Sprite.of(art("""
        ...#...
        ..###..
        #######
        .#####.
        ..###..
        .##.##.
        .#...#.
    """))
    val starOff = Sprite.of(art("""
        ...#...
        ..#.#..
        ###.###
        .#...#.
        ..#.#..
        .#.#.#.
        .#...#.
    """))

    /** [n] of three stars, in a row centred on [cx]. */
    fun stars(f: Frame, n: Int, cx: Int, y: Int, gap: Int = 3) {
        val w = starOn.w * 3 + gap * 2
        for (i in 0 until 3) f.put(if (i < n) starOn else starOff, cx - w / 2 + i * (starOn.w + gap), y)
    }

    /** A row of a results list: what on the left, how much on the right, in the sign-painter's capitals. */
    fun row(f: Frame, what: String, value: String, y: Int, side: Int = 10) {
        f.west(what.uppercase(), side, y)
        f.west(value.uppercase(), G.W - side - West.cells(value.uppercase()), y)
    }
}
