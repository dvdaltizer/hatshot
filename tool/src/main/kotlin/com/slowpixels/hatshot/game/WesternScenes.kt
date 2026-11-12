package com.slowpixels.hatshot.game

import kotlin.math.roundToInt

/**
 * The still pictures behind each stage and the first page, drawn once. Thin lines in the page's ink with nothing
 * filled in, and no more of them than the eye needs (boards and stonework are only hinted at), so the snails, cut out
 * of paper, are what stands out.
 *
 * Every stage is laid out again for the Light Phone's screen: 216 game pixels across and 194 down from the top bar to
 * the bullets, where the Kompakt's was 160 across and 226 down. Wider and shorter, so the buildings have an extra
 * window or two and lose a little height. The snails are the same size as ever.
 */
object WesternScenes {
    val CLOUD = art("""
        .......#####........
        .....##ooooo##......
        ...##oooooooo#####..
        ..#ooooooooooooooo#.
        .#oooooooooooooooo#.
        ..################..
    """)

    val BARREL = art("""
        ...############...
        ..#oooooooooooo#..
        .################.
        .#oo#oooooo#ooo##.
        #ooo#oooooo#oooo##
        ##################
        #ooo#oooooo#oooo##
        #ooo#oooooo#oooo##
        #ooo#oooooo#oooo##
        ##################
        #ooo#oooooo#oooo##
        .#oo#oooooo#ooo##.
        .################.
        ..#oooooooooooo#..
        ...############...
    """)

    val CACTUS = art("""
        .......###........
        ......#ooo#.......
        ......#o#o#.......
        ......#o#o#.......
        ..###.#o#o#.......
        .#ooo##o#o#.###...
        .#o#o##o#o##ooo#..
        .#o#o##o#o##o#o#..
        .#o#oo#o#o##o#o#..
        .#ooooo#oo#oo#o#..
        ..#####o#oooooo#..
        ......#o#o#####...
        ......#o#o#.......
        ......#o#o#.......
        ......#o#o#.......
        ......#o#o#.......
        ......#o#o#.......
        ......#o#o#.......
        ....#########.....
    """)

    /**
     * Clapboard, hinted at: a stroke in from each end of every board and a loose one somewhere along it, the rest left
     * to the eye. Ruled right across, the boards were the brightest thing in the picture, ahead of the snails.
     */
    fun Pad.planks(x: Int, y: Int, w: Int, h: Int, step: Int = 7) {
        var row = 0
        var j = y + step - 1
        while (j < y + h) {
            val left = minOf(w, 7 + (row * 13) % 15)
            val right = minOf(w, 5 + (row * 7) % 17)
            box(x, j, left, 1)
            box(x + w - right, j, right, 1)
            if (w > 70) box(x + 26 + (row * 41) % (w - 62), j, 9 + (row * 5) % 9, 1)
            row++
            j += step
        }
    }

    /** Dust and pebbles on the ground: a dot here and there, never a grey fill. */
    fun Pad.dust(x: Int, y: Int, w: Int, h: Int, seed: Int = 7) {
        val r = kotlin.random.Random(seed)
        repeat(w * h / 70) { dot(x + r.nextInt(w), y + r.nextInt(h)) }
    }

    private fun Pad.window(x: Int, y: Int, w: Int, h: Int) {
        box(x - 2, y - 3, w + 4, h + 6, false)
        outline(x - 2, y - 3, w + 4, h + 6)
        outline(x - 1, y - 1, w + 2, h + 2)
        // The sill.
        box(x - 4, y + h + 2, w + 8, 3, false); outline(x - 4, y + h + 2, w + 8, 3)
        // A faint room behind the glass.
        shade(x, y, w, h) { i, j -> i % 6 == 0 && j % 6 == 0 }
    }

    // ---- Dusty Street. ----

    fun mainStreet(): Pad {
        val p = Pad(G.W, G.H)
        // The false front, stepped up in the middle, with the sign.
        p.card(6, 44, 204, 134)
        p.card(48, 26, 120, 19)
        p.box(49, 44, 118, 1, false)
        p.box(4, 43, 44, 2); p.box(168, 43, 44, 2)
        p.box(46, 25, 124, 2)
        p.card(54, 28, 108, 15); p.outline(56, 30, 104, 11)
        p.west("SODA", West.left("SODA", 108), 31)
        p.planks(7, 46, 202, 48)
        // Upstairs: four windows, and flower boxes under the middle two.
        for (x in UP_XS) p.window(x, 50, 26, 36)
        for (x in listOf(68, 122)) {
            p.card(x - 1, 92, 28, 4)
            for (f in listOf(x + 2, x + 8, x + 14, x + 20)) { p.dot(f, 91); p.dot(f + 1, 90); p.dot(f + 2, 91) }
        }
        // The porch roof: boards running down to a scalloped edge.
        p.card(0, 96, 216, 12)
        var x = 4
        while (x < 220) { p.line(x, 97, x - 3, 106); x += 16 }
        for (i in 0 until 216 step 8) { p.box(i, 108, 8, 1); p.box(i + 1, 109, 6, 1); p.box(i + 2, 110, 4, 1) }
        // Downstairs.
        p.planks(7, 111, 202, 65)
        for (wx in listOf(14, 176)) p.window(wx, 116, 26, 36)
        // The doorway, dark past the doors.
        p.box(84, 108, 48, 68, false)
        p.outline(84, 108, 48, 68)
        p.outline(86, 110, 44, 66)
        p.shade(88, 112, 40, 60) { i, j -> i % 3 == 0 && j % 3 == 0 }
        // Porch posts.
        for (px in listOf(2, 58, 154, 210)) { p.box(px, 110, 4, 66, false); p.outline(px, 110, 4, 66) }
        // The boardwalk and a step down to the street.
        p.card(0, 176, 216, 8)
        var b = 6
        while (b < 216) { p.box(b, 177, 1, 6); b += 11 }
        p.box(0, 184, 216, 1)
        p.dust(0, 186, 216, 32)
        // Barrels, left, stacked two on two.
        p.stamp(BARREL, 2, 188); p.stamp(BARREL, 20, 188)
        p.stamp(BARREL, 2, 203); p.stamp(BARREL, 20, 203)
        // Water trough, right.
        p.card(178, 190, 36, 16)
        p.outline(180, 192, 32, 2)
        p.card(182, 206, 4, 12); p.card(206, 206, 4, 12)
        p.box(182, 197, 28, 1); p.box(182, 201, 28, 1)
        // The fence the bottles sit on.
        p.card(44, 196, 128, 4)
        p.card(44, 208, 128, 4)
        for (fx in listOf(48, 82, 116, 150, 168)) p.card(fx, 192, 4, 26)
        return p
    }

    val UP_XS = listOf(14, 68, 122, 176)

    // ---- The Rusty Spur. ----

    fun saloon(): Pad {
        val p = Pad(G.W, G.H)
        // Ceiling beams.
        p.box(0, 24, 216, 1)
        for (x in 10 until 216 step 34) p.outline(x, 25, 6, 6)
        p.box(0, 31, 216, 1)
        // Upstairs, four rooms, and the rail in front.
        p.box(0, 32, 216, 43, false)
        p.planks(0, 32, 216, 44, 8)
        for (x in ROOM_XS) {
            p.box(x - 3, 31, 34, 45, false); p.outline(x - 3, 31, 34, 45); p.outline(x - 1, 33, 30, 43)
            p.shade(x, 34, 28, 40) { i, j -> i % 6 == 0 && j % 6 == 0 }
        }
        p.card(0, 75, 216, 5)
        var r = 3
        while (r < 216) { p.box(r, 80, 2, 2); r += 8 }
        p.box(0, 82, 216, 1)
        // The shelf behind the bar, with a mirror in the middle.
        p.card(0, 83, 216, 67)
        p.card(4, 106, 208, 3)
        // The mirror in the middle of it, the glasses, and the keg and spittoon on the floor are drawn in play
        // (WesternProps): they can be shot.
        p.card(8, 111, 200, 2)
        p.shade(1, 113, 214, 10) { i, j -> i % 4 == 0 && j % 4 == 0 }
        p.box(0, 123, 216, 1)
        p.planks(0, 123, 216, 27, 8)
        // The bar: a thick top, panels below.
        p.card(0, 150, 216, 6)
        p.box(0, 150, 216, 2)
        p.card(0, 156, 216, 30)
        p.box(1, 157, 214, 28, false)
        for (bx in 4 until 216 step 26) { p.outline(bx, 159, 20, 24); p.outline(bx + 2, 161, 16, 20) }
        // The floor.
        p.box(0, 186, 216, 1)
        p.dust(0, 187, 216, 31, 3)
        // A table, left; crates in the middle; the upright piano and its stool, right.
        tableAndChairs(p, 4, 186)
        p.card(78, 186, 34, 32)
        p.line(78, 186, 111, 217); p.line(111, 186, 78, 217)
        p.outline(80, 188, 30, 28)
        piano(p)
        // The player's stool, at the keys.
        p.card(179, 192, 20, 4)
        p.card(181, 195, 3, 22); p.card(194, 195, 3, 22)
        p.box(184, 205, 10, 1)
        return p
    }

    /** Where the piano's music comes up from: notes float off from about here. */
    const val PIANO_X = 128
    const val PIANO_Y = 158

    /**
     * The saloon's upright piano, turned three-quarters toward its player, as in a photograph of someone at a piano:
     * you see the end of the case, its front, and the keys running along it, so the player can sit at them in profile
     * with his hands on them. Points are given as [a] along the keyboard away from you (4 right and 1 up a unit),
     * [b] out from the front toward the stool (2 right, 1 down) and [h] up, from the nearest bottom corner.
     */
    private fun piano(p: Pad) {
        val nx = 124; val ny = 212
        val long = 14f; val deep = -5f; val top = 43f; val keys = 25f; val out = 4f
        fun x(a: Float, b: Float) = Math.round(nx + 4 * a + 2 * b)
        fun y(a: Float, b: Float, h: Float) = Math.round(ny - a + b - h)
        fun quad(a0: Float, b0: Float, h0: Float, a1: Float, b1: Float, h1: Float, a2: Float, b2: Float, h2: Float, a3: Float, b3: Float, h3: Float) =
            p.poly(x(a0, b0), y(a0, b0, h0), x(a1, b1), y(a1, b1, h1), x(a2, b2), y(a2, b2, h2), x(a3, b3), y(a3, b3, h3))
        fun stroke(a0: Float, b0: Float, h0: Float, a1: Float, b1: Float, h1: Float) = p.line(x(a0, b0), y(a0, b0, h0), x(a1, b1), y(a1, b1, h1))
        // The end of the case, toward you, and the front, toward the player; the lid and its edge.
        quad(0f, deep, 0f, 0f, 0f, 0f, 0f, 0f, top, 0f, deep, top)
        quad(0f, 0f, 0f, long, 0f, 0f, long, 0f, top, 0f, 0f, top)
        quad(0f, deep, top, 0f, 0f, top, long, 0f, top, long, deep, top)
        stroke(0f, deep, top - 3, 0f, 0f, top - 3); stroke(0f, 0f, top - 3, long, 0f, top - 3)
        // A panel on the end.
        quad(0f, deep + 1, 5f, 0f, -1f, 5f, 0f, -1f, top - 7, 0f, deep + 1, top - 7)
        // Sheet music on the stand, over the keys: three lines of it, and a few notes.
        quad(2f, 0f, 28f, 7.5f, 0f, 28f, 7.5f, 0f, 39f, 2f, 0f, 39f)
        for (h in listOf(31f, 34f, 37f)) stroke(2.75f, 0f, h, 6.75f, 0f, h)
        for ((a, h) in listOf(3.25f to 32f, 4.75f to 35f, 6f to 32f)) {
            val x0 = x(a, 0f); val y0 = y(a, 0f, h)
            p.box(x0, y0, 2, 2); p.box(x0 + 1, y0 - 3, 1, 4)
        }
        // The keyboard: a shelf standing out from the front, and its front edge.
        quad(0f, 0f, keys, long, 0f, keys, long, out, keys, 0f, out, keys)
        quad(0f, out, keys, long, out, keys, long, out, keys - 3, 0f, out, keys - 3)
        // The keys along it, each seen at the keyboard's own slant: a line between every two white ones, running from
        // the back of the shelf to its front, and the black ones lying on them at the back, in twos and threes.
        for (k in 1 until long.toInt()) stroke(k.toFloat(), 0f, keys, k.toFloat(), out, keys)
        for (k in 1 until long.toInt()) if (k % 7 in setOf(1, 2, 4, 5, 6)) {
            val x0 = x(k.toFloat(), 0f); val y0 = y(k.toFloat(), 0f, keys)
            val x1 = x(k.toFloat(), out * 0.55f); val y1 = y(k.toFloat(), out * 0.55f, keys)
            p.line(x0, y0, x1, y1); p.line(x0 - 1, y0, x1 - 1, y1); p.line(x0 + 1, y0 + 1, x1 + 1, y1 + 1)
        }
        // The near end of the keyboard, closed in, a leg under each end, and the pedals.
        quad(0f, 0f, keys, 0f, out, keys, 0f, out, keys - 3, 0f, 0f, keys - 3)
        for (a in listOf(0.4f, long - 0.8f)) {
            val lx = x(a, out); val ly = y(a, out, keys - 3); val foot = y(a, out, 0f)
            p.poly(lx, ly, lx + 3, ly, lx + 3, foot, lx, foot)
        }
        stroke(5.2f, 0f, 1f, 5.2f, 2.2f, 1f); stroke(6.2f, 0f, 1f, 6.2f, 2.2f, 1f)
    }

    val ROOM_XS = listOf(12, 66, 122, 176)

    private fun tableAndChairs(p: Pad, x: Int, y: Int) {
        p.card(x, y, 40, 4)
        p.box(x + 18, y + 4, 4, 28)
        p.box(x + 12, y + 30, 16, 2)
    }

    // ---- Buzzard Canyon. ----

    /**
     * The gang's hideout: an old mine at the head of a box canyon. Everything here is close by and built to a person's
     * size (timber doorways, a tunnel, rails and a mine cart), so the people look the right size against it.
     */
    fun canyon(): Pad {
        val p = Pad(G.W, G.H)
        p.ring(176, 38, 7)
        p.stamp(CLOUD, 92, 28)
        // The back of the canyon, then a wall coming forward on each side.
        p.crag(40, 60, 136, 90, 12)
        p.crag(-8, 34, 66, 116, 3)
        p.crag(158, 42, 66, 108, 5)
        p.stamp(CACTUS, 16, 16)
        // Two old workings up on ledges, each with a timber door frame and a plank walk.
        timber(p, 6, 46, 36, 46)
        ledge(p, 0, 92, 50)
        timber(p, 174, 54, 36, 46)
        ledge(p, 166, 100, 50)
        // The canyon floor.
        p.box(0, 148, 216, 1)
        p.box(24, 138, 168, 10, false)
        p.box(24, 143, 168, 1)
        p.dust(0, 150, 216, 68, 11)
        // The mine tunnel, a person and a bit tall, with the rails coming out of it.
        timber(p, 86, 100, 44, 48)
        p.box(90, 104, 36, 44, false)
        p.shade(90, 104, 36, 44) { i, j -> (i + j) % 3 == 0 }
        for (y in listOf(150, 155, 161, 168, 177, 188, 202)) {
            val half = 10 + (y - 148) * 34 / 70
            p.card(108 - half - 3, y, half * 2 + 6, 2 + (y - 148) / 26)
        }
        p.line(100, 149, 74, 217); p.line(101, 149, 75, 217)
        p.line(116, 149, 142, 217); p.line(115, 149, 141, 217)
        // Crates of blasting powder to hide behind, left; a heap of rocks, right.
        // The crate of TNT (4, 146) is drawn in play: it can be shot. These two below it stay.
        crate(p, 2, 166, 20, 20, "")
        crate(p, 22, 166, 20, 20, "")
        p.boulder(197, 176, 19, 18)
        p.boulder(172, 189, 12, 10)
        // The mine cart rolls in and out along the rails, so it's drawn in play (MineCart).
        // A barrel, the same barrel as in town, for scale.
        p.stamp(BARREL, 12, 200)
        return p
    }

    /** A doorway held up with timber: two posts, a beam, a dark inside. */
    private fun timber(p: Pad, x: Int, y: Int, w: Int, h: Int) {
        p.box(x, y, w, h, false)
        p.shade(x + 4, y + 5, w - 8, h - 5) { i, j -> i % 3 == 0 && j % 3 == 0 }
        p.card(x, y + 3, 4, h - 3)
        p.card(x + w - 4, y + 3, 4, h - 3)
        p.card(x - 2, y, w + 4, 5)
        p.box(x + 8, y + 2, 1, 1)
        p.box(x + w - 9, y + 2, 1, 1)
    }

    /** A plank walk along the rock, propped from under. */
    private fun ledge(p: Pad, x: Int, y: Int, w: Int) {
        p.card(x, y, w, 4)
        var i = x + 7
        while (i < x + w) { p.box(i, y + 1, 1, 2); i += 9 }
        p.line(x + 12, y + 4, x + 3, y + 14); p.line(x + w - 12, y + 4, x + w - 3, y + 14)
    }

    /** The canyon's crate of TNT, on its own, to be drawn where it stands until it's shot. */
    val tntCrate: Sprite by lazy { Pad(32, 20).apply { crate(this, 0, 0, 32, 20, "TNT") }.sprite() }

    fun crate(p: Pad, x: Int, y: Int, w: Int, h: Int, label: String) {
        p.card(x, y, w, h)
        p.outline(x + 2, y + 2, w - 4, h - 4)
        if (label.isEmpty()) { p.line(x + 2, y + 2, x + w - 3, y + h - 3); p.line(x + w - 3, y + 2, x + 2, y + h - 3) }
        else p.text(label, x + (w - label.length * 6 + 1) / 2, y + (h - 7) / 2)
    }

    fun Pad.crag(x: Int, y: Int, w: Int, h: Int, seed: Int) {
        val r = kotlin.random.Random(seed)
        var j = 0
        var l = 0
        var e = 0
        while (j < h) {
            val band = minOf(5 + r.nextInt(7), h - j)
            val nl = r.nextInt(4); val ne = r.nextInt(4)
            box(x + nl, y + j, w - nl - ne, band, false)
            box(x + nl, y + j, 1, band)
            box(x + w - ne - 1, y + j, 1, band)
            if (j == 0) box(x + nl, y, w - nl - ne, 1)
            else { box(x + minOf(l, nl), y + j, kotlin.math.abs(l - nl) + 1, 1); box(x + w - 1 - maxOf(e, ne), y + j, kotlin.math.abs(e - ne) + 1, 1) }
            l = nl; e = ne
            // A band of rock, broken here and there.
            var i = x + 5 + r.nextInt(6)
            while (i < x + w - 8) { box(i, y + j + band / 2, 3 + r.nextInt(7), 1); i += 11 + r.nextInt(12) }
            j += band
        }
    }

    /** A rounded rock, centred at ([cx], [cy]). */
    fun Pad.boulder(cx: Int, cy: Int, rx: Int, ry: Int) {
        for (j in -ry..ry) {
            val half = (rx * kotlin.math.sqrt(1.0 - (j.toDouble() / ry) * (j.toDouble() / ry))).toInt()
            box(cx - half, cy + j, half * 2 + 1, 1, false)
            dot(cx - half, cy + j); dot(cx + half, cy + j)
            if (j == -ry || j == ry) box(cx - half, cy + j, half * 2 + 1, 1)
        }
        for (i in -rx..rx) {
            val half = (ry * kotlin.math.sqrt(1.0 - (i.toDouble() / rx) * (i.toDouble() / rx))).toInt()
            dot(cx + i, cy - half); dot(cx + i, cy + half)
        }
        line(cx - rx / 2, cy - ry / 3, cx - rx / 5, cy - ry / 2)
        line(cx + rx / 6, cy + ry / 4, cx + rx / 2, cy + ry / 8)
    }

    // ---- The General Store. ----

    /** A jar with a lid and a label. */
    fun jar(p: Pad, x: Int, bottom: Int, w: Int, h: Int) {
        p.card(x, bottom - h + 2, w, h - 2)
        p.box(x + 1, bottom - h, w - 2, 2)
        p.box(x + 2, bottom - h / 2, w - 4, 1)
    }

    /** A carton with a cross tied round it. */
    fun carton(p: Pad, x: Int, bottom: Int, w: Int, h: Int) {
        p.card(x, bottom - h, w, h)
        p.box(x + w / 2, bottom - h + 1, 1, h - 2)
        p.box(x + 1, bottom - h / 2, w - 2, 1)
    }

    /** A sack, tied at the neck with its corners sticking up, and a word on it if there's room. */
    fun sack(p: Pad, x: Int, bottom: Int, w: Int, h: Int, label: String = "") {
        val top = bottom - h
        val mid = x + w / 2
        // The body, its corners rounded off.
        p.card(x, top + 6, w, h - 6)
        p.dot(x, top + 6, false); p.dot(x + w - 1, top + 6, false); p.dot(x + 1, top + 7); p.dot(x + w - 2, top + 7)
        p.dot(x, bottom - 1, false); p.dot(x + w - 1, bottom - 1, false); p.dot(x + 1, bottom - 2); p.dot(x + w - 2, bottom - 2)
        // The neck gathered up, the tie, and the two ears of cloth above it.
        p.card(mid - 3, top + 2, 6, 5)
        p.box(mid - 4, top + 4, 8, 1)
        p.line(mid - 3, top + 2, mid - 6, top); p.line(mid + 2, top + 2, mid + 5, top)
        if (label.isNotEmpty() && Font.width(label) <= w - 2) p.text(label, x + (w - Font.width(label)) / 2, top + 13)
    }

    /**
     * Shelves against the back wall. What's on them is drawn in play (WesternProps.shelfGoods): it can be shot off.
     * The third board is where the cans to shoot go.
     */
    private fun shelves(p: Pad, x: Int, y: Int, w: Int, h: Int) {
        p.card(x, y, w, h)
        p.box(x + 1, y + 1, 1, h - 2); p.box(x + w - 2, y + 1, 1, h - 2)
        val boards = listOf(y + 20, y + 40, y + 60, y + h - 4)
        for (b in boards) p.card(x, b, w, 3)
    }

    /**
     * Dry Gulch's General Store, from inside: shelves either side of the storeroom's curtain, the counter with its till,
     * the stove, and a barrel, sacks and a crate out front to hide behind.
     */
    fun store(): Pad {
        val p = Pad(G.W, G.H)
        // The ceiling and its beams.
        p.box(0, 24, 216, 1)
        p.card(0, 25, 216, 6)
        for (x in 12 until 216 step 34) p.outline(x, 25, 6, 6)
        // The back wall, boards down to the counter.
        p.box(0, 31, 216, 109, false)
        p.planks(0, 31, 216, 82, 8)
        p.box(0, 113, 216, 1)
        // The clock over the storeroom is drawn in play (WesternProps): it can be shot off the wall.
        // The storeroom door, dark past the curtain, and its rod.
        p.card(86, 58, 44, 56)
        p.box(89, 61, 38, 53, false)
        p.shade(89, 61, 38, 53) { i, j -> i % 3 == 0 && j % 3 == 0 }
        p.outline(88, 60, 40, 54)
        p.box(86, 65, 44, 1)
        // The shelves, left and right of it.
        shelves(p, 2, 32, 36, 80)
        shelves(p, 178, 32, 36, 80)
        // Two lamps hung from the beams.
        for (lx in listOf(62, 152)) {
            p.box(lx, 31, 1, 12)
            p.card(lx - 4, 43, 9, 3); p.card(lx - 3, 46, 7, 7); p.box(lx - 4, 53, 9, 1)
        }
        // The stove, its pipe going into the wall.
        p.card(156, 113, 6, 10)
        p.box(156, 113, 18, 1)
        p.card(150, 122, 24, 4)
        for (j in -18..18) {
            val half = (12 * kotlin.math.sqrt(1.0 - (j / 19.0) * (j / 19.0))).toInt()
            p.box(162 - half, 144 + j, half * 2 + 1, 1, false)
            p.dot(162 - half, 144 + j); p.dot(162 + half, 144 + j)
        }
        p.box(156, 126, 13, 1); p.box(156, 162, 13, 1)
        p.card(156, 140, 12, 9)
        for (x in 158 until 167 step 2) p.box(x, 142, 1, 5)
        p.card(150, 162, 24, 5)
        p.box(152, 167, 3, 5); p.box(169, 167, 3, 5)
        // The counter, and the till on it.
        p.card(0, 140, 146, 5)
        p.card(2, 145, 142, 26)
        for (x in listOf(6, 42, 78, 114)) { p.outline(x, 149, 28, 18); p.outline(x + 2, 151, 24, 14) }
        // The till's case, display and keys; its number, lever and drawer are drawn in play (WesternProps): it rings.
        p.card(108, 126, 26, 14)
        p.card(111, 115, 20, 11)
        for (y in 129 until 137 step 3) for (x in 111 until 131 step 4) p.box(x, y, 2, 1)
        // The floor.
        p.box(0, 171, 216, 1)
        for ((y, step) in listOf(178 to 23, 187 to 29, 199 to 37, 214 to 47)) {
            p.box(0, y, 216, 1)
            var x = (y * 7) % step
            while (x < 216) { p.box(x, y - (if (y == 178) 6 else 9), 1, if (y == 178) 6 else 9); x += step }
        }
        // Out front: the pickle barrel. The two sacks and the crate of eggs each side of it are drawn in play
        // (WesternProps): they can be shot.
        for (j in 0 until 32) {
            val bulge = (3 * kotlin.math.sin(Math.PI * j / 31)).toInt()
            val l = 96 - bulge; val r = 121 + bulge
            p.box(l, 186 + j, r - l + 1, 1, false)
            p.dot(l, 186 + j); p.dot(r, 186 + j)
        }
        p.box(96, 186, 26, 2); p.box(94, 192, 30, 1); p.box(94, 210, 30, 1)
        return p
    }

    // ---- The bank. ----

    /** A column, from its top to its foot. */
    private fun column(p: Pad, x: Int, top: Int, bottom: Int) {
        p.card(x, top, 18, 5)
        p.card(x + 3, top + 5, 12, bottom - top - 10)
        for (f in listOf(6, 9, 12)) p.box(x + f, top + 7, 1, bottom - top - 14)
        p.card(x, bottom - 5, 18, 5)
    }

    val TELLER_XS = listOf(4, 42, 136, 174)

    /**
     * The Dry Gulch Bank: its name over the vault, two tellers' windows each side with bars, the counter, two columns
     * in the hall, the clerk's desk and the strongbox.
     */
    fun bank(): Pad {
        val p = Pad(G.W, G.H)
        // A heavy cornice with teeth under it.
        p.box(0, 24, 216, 1)
        p.card(0, 25, 216, 6)
        var d = 2
        while (d < 216) { p.box(d, 31, 3, 3); d += 6 }
        p.box(0, 34, 216, 1)
        p.box(0, 35, 216, 69, false)
        // The name over the vault is drawn in play (WesternProps): the sign can be shot in two.
        // The vault: a steel frame with rivets, and the round way in, dark, with gold stacked inside.
        p.card(82, 50, 52, 54)
        p.outline(84, 52, 48, 50)
        for ((x, y) in listOf(86 to 54, 128 to 54, 86 to 98, 128 to 98)) p.box(x, y, 2, 2)
        p.disc(108, 78, 21, false)
        p.shade(87, 57, 43, 43) { i, j -> (i - 108) * (i - 108) + (j - 78) * (j - 78) < 19 * 19 && i % 3 == 0 && j % 3 == 0 }
        p.ring(108, 78, 21)
        // The tellers' windows: a frame, and dark behind it.
        for (x in TELLER_XS) {
            p.card(x, 50, 38, 54)
            p.outline(x + 2, 52, 34, 50)
            p.box(x + 4, 56, 30, 46, false)
            p.shade(x + 4, 56, 30, 46) { i, j -> i % 4 == 0 && j % 4 == 0 }
            p.outline(x + 3, 55, 32, 48)
        }
        // The counter the tellers stand behind.
        p.card(0, 104, 216, 6)
        p.card(0, 110, 216, 14)
        for (x in 4 until 216 step 26) p.outline(x, 112, 20, 10)
        // The hall's floor, in tiles running away from you.
        p.box(0, 124, 216, 1)
        for (y in listOf(132, 143, 157, 174, 194)) p.box(0, y, 216, 1)
        for (i in -8..8) p.line(108 + i * 12, 125, 108 + i * 30, 217)
        // Two columns.
        column(p, 62, 124, 190)
        column(p, 136, 124, 190)
        // The clerk's desk, with the ink, left; the money bags, right.
        p.card(0, 190, 52, 4)
        p.card(2, 194, 48, 24)
        p.outline(6, 198, 40, 9); p.box(24, 201, 4, 2)
        p.card(41, 183, 6, 7); p.line(44, 183, 48, 175)
        // The strongbox, right, to hide behind: iron bands, a lid and a lock. The money bags in front of it are drawn
        // in play (WesternProps): they can be shot.
        p.card(170, 188, 44, 5)
        p.card(172, 192, 40, 20)
        for (x in listOf(177, 204)) { p.box(x, 188, 3, 24, false); p.outline(x, 188, 3, 24) }
        p.card(188, 193, 8, 8); p.box(191, 195, 2, 2); p.box(191, 197, 2, 2, false); p.box(192, 197, 1, 2)
        for (x in listOf(174, 184, 199, 209)) p.dot(x, 190)
        return p
    }

    // ---- The jailhouse. ----

    /**
     * The Dry Gulch jail, from the street: a stone front with a flat roof, Belle's cell window and another cell, the
     * sheriff's office windows, the door, wanted posters, and a water trough and barrels in the street.
     */
    fun jailhouse(): Pad {
        val p = Pad(G.W, G.H)
        p.stamp(WesternArt.SUN_ROWS, 56, 25)
        p.stamp(CLOUD, 144, 27)
        // The parapet. The sign on the roof is drawn in play (WesternProps): it can be shot in two.
        p.card(2, 42, 212, 5)
        // The stone front.
        p.card(4, 47, 208, 139)
        // Stonework, hinted at: a few stones picked out here and there rather than every one ruled in.
        var y = 55
        var row = 0
        while (y < 186) {
            var x = 5 + (row % 2) * 9
            var col = 0
            while (x < 211) {
                if (((row / 2) * 7 + (col / 2) * 3 + (row % 2) * 2) % 6 == 0) {
                    val w = minOf(18, 211 - x)
                    p.box(x, y, w, 1); p.box(x, y - 8, w, 1)
                    p.box(x, y - 8, 1, 8); if (x + 18 < 211) p.box(x + 18, y - 8, 1, 8)
                }
                x += 18; col++
            }
            y += 8; row++
        }
        // The two cells, at the ends: a lintel, a sill and the dark inside. Their bars are drawn in play, in front of
        // whoever's in there: Belle's with her (WesternArt.belleInCell), the other's as its cover.
        for (wx in listOf(12, 170)) {
            p.card(wx - 2, 52, 38, 6)
            p.box(wx + 2, 58, 30, 32, false)
            p.outline(wx + 1, 57, 32, 34)
            p.card(wx - 2, 90, 38, 5)
        }
        // The sheriff's office windows, between them.
        p.window(66, 58, 28, 34)
        p.window(122, 58, 28, 34)
        // The door, under its board.
        p.card(86, 112, 44, 74)
        p.card(86, 115, 44, 11)
        p.text("SHERIFF", 108 - Font.width("SHERIFF") / 2, 117)
        p.box(92, 128, 32, 56, false)
        p.shade(92, 128, 32, 56) { i, j -> i % 3 == 0 && j % 3 == 0 }
        p.outline(91, 127, 34, 57)
        p.card(82, 184, 52, 4)
        // The street.
        p.dust(0, 188, 216, 30, 13)
        // The water trough, left, and the barrels, right, are drawn in play (WesternProps): they can be shot.
        return p
    }

    // ---- High noon. ----

    /** Noon: an empty street running away to a clock with both hands up, buildings each side. */
    fun noon(): Pad {
        val p = Pad(G.W, G.H)
        // The town sits low, to leave the sky for the word and the name of who you're facing.
        val horizon = 122
        p.stamp(WesternArt.SUN_ROWS, 190, 28)
        p.box(0, horizon, 216, 1)
        for ((x, w, top) in listOf(Triple(0, 24, 74), Triple(24, 20, 88), Triple(44, 16, 98), Triple(60, 14, 106))) {
            p.card(x, top, w, horizon - top + 1)
            p.card(215 - x - w + 1, top, w, horizon - top + 1)
            p.outline(x + 4, top + 6, w - 8, 10)
            p.outline(215 - x - w + 5, top + 6, w - 8, 10)
        }
        // The clock tower at the far end, reading twelve.
        p.card(98, 84, 20, 39)
        p.ring(108, 96, 7)
        p.box(108, 90, 1, 6)
        p.line(96, 84, 108, 74); p.line(120, 84, 108, 74); p.box(96, 84, 25, 1)
        // The street's edges, running out toward you.
        p.line(80, horizon, 0, 200); p.line(136, horizon, 215, 200)
        p.dust(0, horizon + 2, 216, 104, 5)
        return p
    }

    val SMALL_STAR = art("""
        .......#.......
        ......###......
        ......#o#......
        .....#ooo#.....
        #####ooooo#####
        .#ooooooooooo#.
        ..##ooooooo##..
        ....#ooooo#....
        ...#ooooooo#...
        ...#oo###oo#...
        ..#o##...##o#..
        ..##.......##..
    """)

    /** Dry Gulch's oldest resident. */
    private val HAPPY_CACTUS = art("""
        .......######.......
        .......######.......
        .....##########.....
        ......#oooooo#......
        ......#o#oo#o#......
        ...##.#oooooo#.##...
        ..#oo##o#oo#o##oo#..
        ..#oo##oo##oo##oo#..
        ..#oo##oooooo##oo#..
        ..#oooooooooooooo#..
        ...####oooooo####...
        ......#oooooo#......
        ......#oooooo#......
        ......#oooooo#......
        ......#oooooo#......
        .....##########.....
    """)

    /** The first page's sign: the name on a board on two ropes, a star, and a hat just shot off. */
    fun title(): Pad {
        val p = Pad(G.W, G.H)
        for (x in listOf(44, 170)) {
            var y = 0
            while (y < 10) { p.box(x + (y / 2) % 2, y, 2, 2); y += 2 }
        }
        p.card(18, 8, 180, 76)
        p.outline(20, 10, 176, 72)
        for ((x, y) in listOf(23 to 13, 191 to 13, 23 to 77, 191 to 77)) p.box(x, y, 2, 2)
        p.hollow("HAT", 15, 3)
        p.hollow("SHOT", 49, 3)
        p.stamp(SMALL_STAR, 31, 22)
        // A hat, just shot off and on its way up.
        p.box(157, 18, 30, 22, false)
        p.stamp(WesternArt.HAT, 160, 20)
        for ((x, y) in listOf(163 to 32, 171 to 33, 179 to 32)) p.box(x, y, 1, 5)
        return p
    }

    /** Behind the first page's sign: the signpost, the mesas far off and the ground coming toward you. */
    fun titleGround(): Pad {
        val p = Pad(G.W, G.H)
        p.card(103, 84, 10, 140)
        p.box(107, 86, 1, 136, true)
        var g = 94
        while (g < 216) { p.box(106, g, 1, 5); p.box(109, g + 6, 1, 7); g += 19 }
        p.crag(0, 206, 50, 16, 4)
        p.crag(160, 210, 56, 12, 6)
        p.box(0, 222, 216, 1)
        p.dust(0, 224, 216, 24, 21)
        p.stamp(HAPPY_CACTUS, 60, 205)
        return p
    }

    /** Where the first page's three boards go, in game pixels: x, y, width, height, and which way each points. */
    val BOARDS = listOf(intArrayOf(30, 88, 158, 30, 1), intArrayOf(24, 128, 128, 22, 0), intArrayOf(76, 160, 116, 22, 1))

    // ---- Shutters and doors, opened [open] of the way (0 shut, 1 open). ----

    private val covers = HashMap<Long, Sprite>()

    private fun key(kind: Int, w: Int, h: Int, step: Int) = kind.toLong() * 1_000_000_000 + w * 1_000_000L + h * 1000L + step

    private fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t.coerceIn(0f, 1f)).roundToInt()

    fun shutters(w: Int, h: Int, open: Float): Sprite {
        val lw = lerp(w / 2, 3, open)
        return covers.getOrPut(key(1, w, h, lw)) {
            val px = IntArray(w * h)
            fun put(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) px[y * w + x] = c }
            /** One leaf: louvres when it faces you, a thin edge as it swings open. */
            fun leaf(x0: Int, lw: Int) {
                for (y in 0 until h) for (x in x0 until x0 + lw) {
                    val edge = x == x0 || x == x0 + lw - 1 || y == 0 || y == h - 1
                    put(x, y, if (edge || (lw > 5 && y % 8 == 4 && y > 2 && y < h - 3)) INK else PAPER)
                }
            }
            leaf(0, lw); leaf(w - lw, lw)
            Sprite(w, h, px)
        }
    }

    fun batwings(w: Int, h: Int, open: Float): Sprite {
        val lw = lerp(w / 2, 4, open)
        return covers.getOrPut(key(2, w, h, lw)) {
            val px = IntArray(w * h)
            fun put(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) px[y * w + x] = c }
            val top = 18; val bottom = h - 10
            fun leaf(x0: Int, lw: Int, left: Boolean) {
                for (y in top until bottom) for (x in x0 until x0 + lw) {
                    val edge = x == x0 || x == x0 + lw - 1 || y == bottom - 1
                    val curve = if (left) (y == top + (x - x0) * 6 / maxOf(lw, 1)) else (y == top + (x0 + lw - 1 - x) * 6 / maxOf(lw, 1))
                    val above = if (left) y < top + (x - x0) * 6 / maxOf(lw, 1) else y < top + (x0 + lw - 1 - x) * 6 / maxOf(lw, 1)
                    if (above) continue
                    val slat = lw > 6 && (y - top) % 8 == 5
                    put(x, y, if (edge || curve || slat) INK else PAPER)
                }
            }
            leaf(0, lw, false); leaf(w - lw, lw, true)
            Sprite(w, h, px)
        }
    }

    /** The storeroom's curtain: two halves drawn across on a rod, then pulled back to the sides. */
    fun curtain(w: Int, h: Int, open: Float): Sprite {
        val half = lerp(w / 2, 3, open)
        return covers.getOrPut(key(3, w, h, half)) {
            val px = IntArray(w * h)
            fun put(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) px[y * w + x] = c }
            for (x in 0 until w) { put(x, 0, INK); put(x, 1, INK) }
            for (side in 0..1) for (y in 2 until h) for (i in 0 until half) {
                val x = if (side == 0) i else w - 1 - i
                // Folds: a line every third pixel, swaying a little, and the hem at the bottom.
                val fold = (i + (y / 6) % 2) % 6 == 0
                put(x, y, if (i == half - 1 || fold || y == h - 1) INK else PAPER)
            }
            Sprite(w, h, px)
        }
    }

    /**
     * The vault's round steel door, as big as its slot: shut with its wheel and bolts, then swinging out on its hinge
     * at the right, narrowing to its edge. Outside the round it's see-through, so the frame drawn behind shows.
     */
    fun vault(w: Int, h: Int, open: Float): Sprite {
        val step = if (open <= 0f) 0 else lerp(1, 8, open)
        return covers.getOrPut(key(4, w, h, step)) {
            val px = IntArray(w * h)
            val cx = w / 2.0; val cy = h / 2.0; val r = minOf(w, h) / 2.0 - 1
            fun put(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) px[y * w + x] = c }
            if (step == 0) {
                for (y in 0 until h) for (x in 0 until w) {
                    val dx = x + 0.5 - cx; val dy = y + 0.5 - cy
                    val d = kotlin.math.sqrt(dx * dx + dy * dy)
                    when {
                        d > r -> Unit
                        d > r - 2 -> put(x, y, INK)
                        d in (r - 7)..(r - 6) -> put(x, y, INK)
                        else -> put(x, y, PAPER)
                    }
                }
                // Bolts round the edge, and the wheel in the middle.
                for (k in 0 until 8) {
                    val a = k * Math.PI / 4
                    val bx = (cx + (r - 4) * kotlin.math.cos(a)).toInt(); val by = (cy + (r - 4) * kotlin.math.sin(a)).toInt()
                    put(bx, by, INK); put(bx - 1, by, INK); put(bx, by - 1, INK); put(bx - 1, by - 1, INK)
                }
                for (k in 0 until 3) {
                    val a = k * Math.PI / 3
                    for (t in -9..9) put((cx + t * kotlin.math.cos(a)).toInt(), (cy + t * kotlin.math.sin(a)).toInt(), INK)
                }
                for (y in -2..1) for (x in -2..1) put(cx.toInt() + x, cy.toInt() + y, INK)
            } else {
                // Swinging out: the round seen more and more edge-on, its middle sliding toward the hinge.
                val t = step / 8.0
                val rx = r * (1 - t) + 2 * t
                val ox = cx + (w - 3 - rx - cx) * t
                for (y in 0 until h) for (x in 0 until w) {
                    val dx = (x + 0.5 - ox) / rx; val dy = (y + 0.5 - cy) / r
                    val d = dx * dx + dy * dy
                    if (d <= 1.0) put(x, y, if (d > 1.0 - 2.4 / rx || x == ox.toInt()) INK else PAPER)
                }
            }
            Sprite(w, h, px)
        }
    }

    /** A teller's window: bars that stay put, and a blind behind them that rolls up when somebody's there. */
    fun blind(w: Int, h: Int, open: Float): Sprite {
        val down = lerp(h, 4, open)
        return covers.getOrPut(key(5, w, h, down)) {
            val px = IntArray(w * h)
            fun put(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) px[y * w + x] = c }
            for (y in 0 until down) for (x in 0 until w) put(x, y, if (y == down - 1 || y % 5 == 4) INK else PAPER)
            if (down == h) { put(w / 2, h - 2, INK); put(w / 2 - 1, h - 3, INK); put(w / 2 + 1, h - 3, INK); put(w / 2, h - 4, INK) }
            for (x in BAR_XS) for (y in 0 until h) put(x, y, if (y < down) INK else FLIP)
            Sprite(w, h, px)
        }
    }

    /** A cell window's bars, which never open: the same iron as Belle's, two pixels thick. */
    fun bars(w: Int, h: Int): Sprite = covers.getOrPut(key(6, w, h, 0)) {
        val px = IntArray(w * h)
        for (b in WesternArt.CELL_BARS) for (x in b..b + 1) if (x < w) for (y in 0 until h) px[y * w + x] = FLIP
        Sprite(w, h, px)
    }

    /** Where bars go across a window, one every six game pixels. */
    private val BAR_XS = intArrayOf(3, 9, 15, 21, 27)

    /** The jailhouse door: planks, two iron bands and a little barred hatch; it swings in on its hinge at the left. */
    fun door(w: Int, h: Int, open: Float): Sprite {
        val dw = lerp(w, 4, open)
        return covers.getOrPut(key(7, w, h, dw)) {
            val px = IntArray(w * h)
            fun put(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) px[y * w + x] = c }
            val shut = dw == w
            for (y in 0 until h) for (x in 0 until dw) {
                val edge = x == 0 || x == dw - 1 || y == 0 || y == h - 1
                val plank = dw > 12 && x % 6 == 5
                val band = dw > 12 && (y in 10..11 || y in h - 13..h - 12)
                put(x, y, if (edge || plank || band) INK else PAPER)
            }
            if (shut) {
                // The hatch, with bars.
                for (y in 17..27) for (x in w / 2 - 6..w / 2 + 5) {
                    val rim = y == 17 || y == 27 || x == w / 2 - 6 || x == w / 2 + 5
                    put(x, y, if (rim || (x - (w / 2 - 6)) % 3 == 0) INK else PAPER)
                }
                // The ring to pull it by.
                for ((x, y) in listOf(w - 7 to 30, w - 6 to 29, w - 5 to 30, w - 7 to 31, w - 5 to 31, w - 6 to 32)) put(x, y, INK)
            }
            Sprite(w, h, px)
        }
    }

    // ---- The gang's three leaders, one for each duel: an outlaw doubled, so the pixels loom. ----

    private val bosses = HashMap<Pair<Int, Pose>, Sprite>()
    private val LEADERS = listOf("beard", "scowl", "bandana")
    val LEADER_NAMES = listOf("Whiskers Malone", "The Dusty Kid", "Black Shell Hank")

    fun leader(duel: Int) = LEADERS[duel.coerceIn(0, 2)]

    fun boss(duel: Int, pose: Pose): Sprite = bosses.getOrPut(duel to pose) { twice(WesternArt.snail(leader(duel), pose)) }

    /** Black Shell Hank after [hits] of the three shots it takes: whole, hatless, then without his bandana either. */
    fun hank(hits: Int, pose: Pose): Sprite {
        val k = hits.coerceIn(0, 2)
        return bosses.getOrPut(10 + k to pose) { twice(WesternArt.snail(HANK[k], pose)) }
    }
    private val HANK = listOf("bandana", "hank hatless", "hank unmasked")

    /** A sprite with every pixel doubled. */
    fun twice(s: Sprite): Sprite {
        val w = s.w * 2; val h = s.h * 2
        return Sprite(w, h, IntArray(w * h) { i -> s.px[(i / w / 2) * s.w + (i % w) / 2] })
    }
}
