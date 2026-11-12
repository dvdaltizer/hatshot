package com.slowpixels.hatshot

import com.slowpixels.hatshot.game.Frame
import com.slowpixels.hatshot.game.Fx
import com.slowpixels.hatshot.game.G
import com.slowpixels.hatshot.game.Look
import com.slowpixels.hatshot.game.Out
import com.slowpixels.hatshot.game.Pace
import com.slowpixels.hatshot.game.Page
import com.slowpixels.hatshot.game.Phase
import com.slowpixels.hatshot.game.Play
import com.slowpixels.hatshot.game.Role
import com.slowpixels.hatshot.game.Showdown
import com.slowpixels.hatshot.game.Story
import com.slowpixels.hatshot.game.Target
import com.slowpixels.hatshot.game.Western
import com.slowpixels.hatshot.game.WesternScenes
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import org.junit.Test

/** Draws every stage, story page and the duel into build/shots, at the phone's own size, for looking at. */
class ShotsTest {
    private val out = File(System.getenv("SHOTS") ?: "build/shots").apply { mkdirs() }

    private object Quiet : Out {
        override fun fx(fx: Fx) = Unit
        override fun buzz(hard: Boolean) = Unit
        override fun silence(ms: Int) = Unit
        override fun finished() = Unit
    }

    private fun save(f: Frame, scale: Int, name: String) = File(out, "$name.png").writeBytes(png(f, scale))

    /** A plain PNG, written by hand: the tests run without Java's image classes. */
    private fun png(f: Frame, scale: Int): ByteArray {
        val w = f.w * scale; val h = f.h * scale
        val raw = ByteArrayOutputStream()
        DeflaterOutputStream(raw).use { z ->
            val row = ByteArray(1 + w * 3)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val c = f.px[(y / scale) * f.w + x / scale]
                    row[1 + x * 3] = (c shr 16).toByte(); row[2 + x * 3] = (c shr 8).toByte(); row[3 + x * 3] = c.toByte()
                }
                z.write(row)
            }
        }
        val o = ByteArrayOutputStream()
        val d = DataOutputStream(o)
        d.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
        fun chunk(type: String, body: ByteArray) {
            d.writeInt(body.size)
            val t = type.toByteArray()
            d.write(t); d.write(body)
            val crc = CRC32(); crc.update(t); crc.update(body)
            d.writeInt(crc.value.toInt())
        }
        val head = ByteArrayOutputStream().also { DataOutputStream(it).apply { writeInt(w); writeInt(h); write(byteArrayOf(8, 2, 0, 0, 0)) } }
        chunk("IHDR", head.toByteArray())
        chunk("IDAT", raw.toByteArray())
        chunk("IEND", ByteArray(0))
        return o.toByteArray()
    }

    @Test fun stagesFull() {
        for (look in listOf(Look.DARK, Look.LIGHT)) {
            Western.stages.forEachIndexed { i, stage ->
                if (stage.duel) return@forEachIndexed
                val play = Play(Western, i, Pace.NORMAL, false, look, Quiet, seed = 3)
                val r = play.round
                // Everyone out at once, outlaws and friends by turns, to see every opening filled.
                stage.slots.forEachIndexed { n, s ->
                    val spec = if (n % 3 == 1 && stage.friends.isNotEmpty()) stage.friends[n % stage.friends.size] else stage.foes[n % stage.foes.size]
                    val t = Target(spec, s, n, 99_999)
                    t.phase = Phase.OUT; t.rise = 1f; t.cover = 1f
                    r.place(t)
                    r.live += t
                }
                play.tick(1)
                val f = Frame(G.W, G.H)
                play.draw(f)
                save(f, G.PX, "stage-$i-full-${if (look.dark) "dark" else "light"}")
                // And the empty scene.
                val e = Frame(G.W, G.H); e.look = look
                e.page(Page(Western.background(stage), look))
                save(e, G.PX, "stage-$i-empty-${if (look.dark) "dark" else "light"}")
            }
        }
    }

    /** A stage played by a bot that shoots every outlaw a moment after it's out, with pictures as hats fly. */
    @Test fun stagesPlayed() {
        Western.stages.forEachIndexed { i, stage ->
            if (stage.duel) return@forEachIndexed
            val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 11)
            val r = play.round
            var shotAt = -1L
            var saved = 0
            var clock = 0L
            while (clock < 40_000 && saved < 3) {
                play.tick(16); clock += 16
                if (r.bullets == 0) r.reload()
                val t = r.live.firstOrNull { it.spec.role == Role.FOE && it.phase == Phase.OUT && it.seen > 300 }
                if (t != null && shotAt < 0) {
                    // Every other one in the shell.
                    val shell = t.spec.shell
                    if (saved % 2 == 1 && shell != null) play.tap(t.x + shell.x + 8, t.y + shell.y + 6) else play.tap(t.x + 12, t.y + 4)
                    shotAt = clock
                }
                if (shotAt >= 0 && clock - shotAt >= 300) {
                    val f = Frame(G.W, G.H)
                    play.draw(f)
                    save(f, G.PX, "stage-$i-played-$saved")
                    saved++
                    shotAt = -1
                    repeat(60) { play.tick(16); clock += 16 }
                }
            }
        }
    }

    /** Pictures 80 ms apart of a hat shot off, and of a shell shot to bits: the Light Phone's animations, to look at. */
    @Test fun filmstrips() {
        for ((name, onShell) in listOf("hat" to false, "shell" to true)) {
            val play = Play(Western, 2, Pace.NORMAL, false, Look.DARK, Quiet, seed = 6)
            val r = play.round
            var clock = 0
            var t: Target? = null
            while (t == null && clock < 60_000) {
                play.tick(16); clock += 16
                t = r.live.firstOrNull { it.spec.role == Role.FOE && it.phase == Phase.OUT && it.slotIndex in 0..3 && it.seen > 400 }
            }
            t!!
            val box = t.spec.shell!!
            if (onShell) play.tap(t.x + box.x + box.w / 2, t.y + box.y + box.h / 2) else play.tap(t.x + 12, t.y + 4)
            for (n in 0 until 8) {
                val f = Frame(G.W, G.H)
                play.draw(f)
                save(f, G.PX, "film-$name-$n")
                repeat(5) { play.tick(16) }
            }
        }
    }

    /** The train's five cars, each with every opening that car uses filled, to see nobody stands on thin air. */
    @Test fun trainCars() {
        val i = Western.stages.indexOfFirst { it.title == "The Iron Horse" }
        val stage = Western.stages[i]
        for (car in 0 until 5) {
            val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 3)
            play.wave(car)
            repeat(260) { play.idle(16) }
            val only = stage.waves[car].only
            stage.slots.forEachIndexed { n, s ->
                if (only != null && n !in only) return@forEachIndexed
                val t = Target(stage.foes[n % stage.foes.size], s, n, 99_999)
                t.phase = Phase.OUT; t.rise = 1f; t.cover = 1f
                play.round.place(t)
                play.round.live += t
            }
            val f = Frame(G.W, G.H)
            play.draw(f)
            save(f, G.PX, "train-car-$car")
        }
    }

    @Test fun storyPages() {
        val all = Western.stages.flatMap { it.story } + Western.ending
        all.forEachIndexed { n, card ->
            val flip = card.picture()
            val f = Frame(Story.W, 310); f.look = Look.DARK; f.fill()
            val page = Page(flip.frames[0], Look.DARK, Look.DARK.ink)
            for (y in 0 until Story.H) System.arraycopy(page.px, y * Story.W, f.px, (y + 30) * Story.W, Story.W)
            save(f, Story.PX, "story-$n")
        }
    }

    /**
     * One busy moment from each stage, on both pages, for the mockups: about half the openings filled, outlaws and
     * good folk by turns, and one outlaw's hat just shot off.
     */
    @Test fun moments() {
        val looks = listOf(Look.DARK to "dark", Look.LIGHT to "light")
        for ((look, page) in looks) {
            Western.stages.forEachIndexed { i, stage ->
                if (stage.duel) {
                    val s = Showdown(Pace.NORMAL, false, look, Quiet, seed = 5)
                    s.waiting = false
                    s.shownScore = 21_900
                    var t = 0
                    while (s.duel.phase.name == "STEADY" && t < 10_000) { s.tick(16); t += 16 }
                    s.tick(60)
                    val d = Frame(G.W, G.H); s.draw(d); save(d, G.PX, "moment-$i-$page")
                    return@forEachIndexed
                }
                val play = Play(Western, i, Pace.NORMAL, false, look, Quiet, seed = 5)
                play.waiting = false
                play.shownScore = 1250 + i * 2450
                val r = play.round
                var k = 0
                stage.slots.forEachIndexed { n, slot ->
                    if ((n + i) % 2 == 1) return@forEachIndexed
                    val spec = if (k % 3 == 1 && stage.friends.isNotEmpty()) stage.friends[k % stage.friends.size] else stage.foes[k % stage.foes.size]
                    val t = Target(spec, slot, n, 99_999)
                    t.phase = Phase.OUT; t.rise = 1f; t.cover = 1f
                    r.place(t)
                    r.live += t
                    k++
                }
                // They've been out a while: the shot is worth plain points, not a quick draw.
                for (t in r.live) t.seen = 900
                play.tick(1)
                (r.live.firstOrNull { it.spec.role == Role.FOE && it.slot?.dark == true }
                    ?: r.live.firstOrNull { it.spec.role == Role.FOE && it.slot != null })?.let { foe ->
                    play.tap(foe.x + 12, maxOf(foe.y, foe.slot!!.y) + 5)
                }
                repeat(14) { play.tick(16) }
                val f = Frame(G.W, G.H)
                play.draw(f)
                save(f, G.PX, "moment-$i-$page")
                // And the same stage with the gun empty: the bar says RELOAD.
                if (i == 0) {
                    while (r.bullets > 0) { play.tap(G.W / 2, G.TOP + 1); play.tick(16) }
                    val e = Frame(G.W, G.H)
                    play.draw(e)
                    save(e, G.PX, "moment-reload-$page")
                }
            }
        }
    }

    /** The whole cast, big, cut out as in play, on the dark page and the light one: outlaws above, good folk below. */
    @Test fun cast() {
        val outlaws = listOf("mustache", "beard", "scowl", "bandana")
        val folk = listOf("sheriff", "granny", "kid", "barkeep", "bonnet", "banker", "belle", "mosey")
        for (look in listOf(Look.DARK, Look.LIGHT)) {
            val f = Frame(8 * 30 + 6, 2 * 46 + 6); f.look = look; f.fill()
            outlaws.forEachIndexed { k, n -> f.cut(com.slowpixels.hatshot.game.WesternArt.snail(n, com.slowpixels.hatshot.game.Pose.STAND), 6 + k * 30, 6) }
            // And one taking aim, at the end of the row.
            f.cut(com.slowpixels.hatshot.game.WesternArt.snail("mustache", com.slowpixels.hatshot.game.Pose.AIM), 6 + 5 * 30, 6)
            folk.forEachIndexed { k, n -> f.cut(com.slowpixels.hatshot.game.WesternArt.snail(n, com.slowpixels.hatshot.game.Pose.STAND), 6 + k * 30, 52) }
            save(f, 8, "cast-${if (look.dark) "dark" else "light"}")
        }
    }

    /** The saloon's ragtime, played through the music player into a WAV, to listen to; and it never clips. */
    @Test fun ragtime() {
        val rate = 32000
        val tr = com.slowpixels.hatshot.game.Tracker(rate)
        tr.play(com.slowpixels.hatshot.game.Songs.RAG)
        val n = rate * 75
        val pcm = ShortArray(n)
        val block = ShortArray(1024)
        var at = 0
        while (at < n) { val k = minOf(1024, n - at); tr.render(block, k); System.arraycopy(block, 0, pcm, at, k); at += k }
        val peak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
        val rms = kotlin.math.sqrt(pcm.sumOf { it.toDouble() * it } / n)
        // As loud as the other stages' tunes, near enough: Dusty Street's, the same way.
        val other = com.slowpixels.hatshot.game.Tracker(rate).apply { play(com.slowpixels.hatshot.game.Songs.TRAIL) }
        val o = ShortArray(rate * 20); var j = 0
        while (j < o.size) { val k = minOf(1024, o.size - j); other.render(block, k); System.arraycopy(block, 0, o, j, k); j += k }
        val orms = kotlin.math.sqrt(o.sumOf { it.toDouble() * it } / o.size)
        println("ragtime peak=$peak rms=${rms.toInt()}; Dusty Street rms=${orms.toInt()}")
        org.junit.Assert.assertTrue("about as loud as the other tunes", rms / orms in 0.7..1.3)
        org.junit.Assert.assertTrue("and not squashed flat against the top", pcm.count { kotlin.math.abs(it.toInt()) > 31000 } < n / 200)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply {
            fun le32(v: Int) { write(v and 255); write((v shr 8) and 255); write((v shr 16) and 255); write((v shr 24) and 255) }
            fun le16(v: Int) { write(v and 255); write((v shr 8) and 255) }
            writeBytes("RIFF"); le32(36 + n * 2); writeBytes("WAVEfmt "); le32(16); le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            writeBytes("data"); le32(n * 2); for (v in pcm) le16(v.toInt())
        }
        File(out, "rusty-spur-entertainer.wav").writeBytes(bytes.toByteArray())
        // And with the piano player hit: 12 seconds of the tune, the melody resting from 4 s to 8 s, the band playing on.
        val hit = com.slowpixels.hatshot.game.Tracker(rate).apply { play(com.slowpixels.hatshot.game.Songs.RAG) }
        val h = ShortArray(rate * 12); var q = 0
        while (q < h.size) {
            hit.melody = q < rate * 4 || q >= rate * 8
            val k = minOf(1024, h.size - q); hit.render(block, k); System.arraycopy(block, 0, h, q, k); q += k
        }
        val quiet = kotlin.math.sqrt((rate * 4 until rate * 8).sumOf { h[it].toDouble() * h[it] } / (rate * 4))
        println("band alone rms=${quiet.toInt()}")
        org.junit.Assert.assertTrue("the band keeps playing while he's out", quiet > 1000)
        val hb = ByteArrayOutputStream()
        DataOutputStream(hb).apply {
            fun le32(v: Int) { write(v and 255); write((v shr 8) and 255); write((v shr 16) and 255); write((v shr 24) and 255) }
            fun le16(v: Int) { write(v and 255); write((v shr 8) and 255) }
            writeBytes("RIFF"); le32(36 + h.size * 2); writeBytes("WAVEfmt "); le32(16); le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            writeBytes("data"); le32(h.size * 2); for (v in h) le16(v.toInt())
        }
        File(out, "rusty-spur-pianist-hit.wav").writeBytes(hb.toByteArray())
    }

    /** The Rusty Spur's piano player: playing (both hands), shot off his stool, back on it and dazed, playing again. */
    @Test fun pianist() {
        val i = Western.stages.indexOfFirst { it.title == "The Rusty Spur" }
        val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 2)
        play.waiting = false
        val r = play.round
        fun shot(name: String) { val f = Frame(G.W, G.H); play.draw(f); save(f, G.PX, name) }
        repeat(20) { play.tick(16) }
        shot("pianist-0-playing")
        repeat(19) { play.tick(16) }
        shot("pianist-1-playing")
        val him = r.live.first { it.spec.plays }
        play.tap(him.x + 20, him.y + 14)
        repeat(12) { play.tick(16) }
        shot("pianist-2-shot")
        repeat(70) { play.tick(16) }
        shot("pianist-3-down")
        repeat(70) { play.tick(16) }
        shot("pianist-4-back")
        repeat(50) { play.tick(16) }
        shot("pianist-5-playing")
    }

    /** The Iron Horse: Mosey's gun going off at an outlaw in a window. */
    @Test fun ridingShot() {
        val i = Western.stages.indexOfFirst { it.title == "The Iron Horse" }
        val stage = Western.stages[i]
        val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 2)
        play.waiting = false
        val r = play.round
        val t = Target(stage.foes[0], stage.slots[5], 5, 99_999)
        t.phase = Phase.OUT; t.rise = 1f; t.cover = 1f
        r.place(t); r.live += t
        play.tick(1)
        play.tap(t.x + 12, t.y + 6)
        play.tick(16)
        val f = Frame(G.W, G.H); play.draw(f); save(f, G.PX, "train-shot")
    }

    /** Buzzard Canyon's TNT: the moment it goes up, then the new crate on its way in. */
    @Test fun tnt() {
        val i = Western.stages.indexOfFirst { it.tnt != null }
        val stage = Western.stages[i]
        val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 2)
        play.waiting = false
        val r = play.round
        val t = Target(stage.foes[1], stage.slots[stage.tntSlot], stage.tntSlot, 99_999)
        t.phase = Phase.OUT; t.rise = 1f; t.cover = 1f
        r.place(t); r.live += t
        play.tick(1)
        val box = stage.tnt!!
        play.tap(box.x + box.w / 2, box.y + box.h / 2)
        for ((n, wait) in listOf(60, 180, 260).withIndex()) {
            repeat(wait / 16) { play.tick(16) }
            val f = Frame(G.W, G.H); play.draw(f); save(f, G.PX, "tnt-blast-$n")
        }
        while (r.tntPushing == 0) play.tick(16)
        repeat(40) { play.tick(16) }
        val f = Frame(G.W, G.H); play.draw(f); save(f, G.PX, "tnt-back")
    }

    /**
     * The scenery that answers back: the General Store's sacks and eggs shot open and its till rung, and the bank's
     * money bags going off and coming back, on both pages.
     */
    @Test fun props() {
        for ((look, page) in listOf(Look.DARK to "dark", Look.LIGHT to "light")) {
            fun shot(p: Play, name: String) { val f = Frame(G.W, G.H); p.draw(f); save(f, G.PX, "$name-$page") }
            val store = Play(Western, Western.stages.indexOfFirst { it.title == "General Store" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            store.waiting = false
            store.tick(16)
            shot(store, "store-0-whole")
            store.tap(12, 203); repeat(22) { store.tick(16) }
            store.tap(44, 200); repeat(6) { store.tick(16) }
            store.tap(190, 204); repeat(26) { store.tick(16) }
            shot(store, "store-1-spilling")
            store.tap(121, 130); repeat(7) { store.tick(16) }
            shot(store, "store-2-till")
            repeat(130) { store.tick(16) }
            store.round.reload(); repeat(40) { store.tick(16) }
            store.tap(121, 130); repeat(4) { store.tick(16) }
            shot(store, "store-3-spilt")

            val jail = Play(Western, Western.stages.indexOfFirst { it.title == "The Jailhouse" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            jail.waiting = false
            jail.tick(16)
            shot(jail, "jail-0-whole")
            jail.tap(20, 205); repeat(4) { jail.tick(16) }
            jail.tap(190, 210); repeat(30) { jail.tick(16) }
            shot(jail, "jail-1-shot")
            repeat(260) { jail.tick(16) }
            shot(jail, "jail-2-after")

            // The store's clock and shelves, the saloon's breakables, the bank's sign.
            val shop = Play(Western, Western.stages.indexOfFirst { it.title == "General Store" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            shop.waiting = false; shop.tick(16)
            for ((x, y) in listOf(108 to 44, 10 to 45, 26 to 64, 195 to 45, 187 to 100, 12 to 100)) { shop.tap(x, y); repeat(5) { shop.tick(16) } }
            shot(shop, "store-4-shelves")
            shop.round.reload(); repeat(120) { shop.tick(16) }
            shot(shop, "store-5-shelves-after")
            val spur = Play(Western, Western.stages.indexOfFirst { it.title == "The Rusty Spur" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            spur.waiting = false; spur.tick(16)
            shot(spur, "spur-0-whole")
            for ((x, y) in listOf(108 to 95, 28 to 103, 138 to 103, 56 to 210, 72 to 214)) { spur.tap(x, y); repeat(6) { spur.tick(16) } }
            shot(spur, "spur-1-shot")
            repeat(250) { spur.tick(16) }
            shot(spur, "spur-2-after")
            val sign = Play(Western, Western.stages.indexOfFirst { it.title == "Dry Gulch Bank" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            sign.waiting = false; sign.tick(16)
            sign.tap(110, 42); repeat(8) { sign.tick(16) }
            shot(sign, "bank-5-sign")
            repeat(60) { sign.tick(16) }
            shot(sign, "bank-6-sign-after")
            // The mine cart on its way up the line, and the engine smoking.
            val mine = Play(Western, Western.stages.indexOfFirst { it.title == "Buzzard Canyon" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            mine.waiting = false
            var n = 0
            // Up the line and away; then out again with the next one riding in it.
            while (6 !in mine.round.shut && n < 4000) { mine.tick(16); n++ }
            while (6 in mine.round.shut && n < 8000) { mine.tick(16); n++ }
            mine.round.live.removeAll { it.slot != null }
            val canyon = Western.stages.first { it.title == "Buzzard Canyon" }
            val rider = Target(canyon.foes[1], canyon.slots[6], 6, 99_999)
            rider.phase = Phase.OUT; rider.rise = 1f; mine.round.place(rider); mine.round.live += rider
            for (k in 0 until 5) { repeat(if (k == 0) 14 else 13) { mine.tick(16) }; shot(mine, "canyon-cart-$k") }
            jail.tap(108, 35); repeat(30) { jail.tick(16) }
            shot(jail, "jail-3-sign")
            val ride = Play(Western, Western.stages.indexOfFirst { it.title == "The Iron Horse" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            ride.waiting = false
            ride.wave(4); repeat(300) { ride.idle(16) }
            shot(ride, "train-smoke")
            // Hank, hit once and twice.
            val noon = Showdown(Pace.NORMAL, false, look, Quiet, seed = 7)
            noon.waiting = false
            var hits = 0; var t = 0
            while (noon.duel.phase.name != "DONE" && t < 120_000) {
                noon.tick(16); t += 16
                if (noon.duel.phase.name == "DRAW") {
                    noon.shoot()
                    if (noon.duel.wins == 2 && noon.duel.bossHits in 1..3) {
                        repeat(16) { noon.tick(16) }
                        val d = Frame(G.W, G.H); noon.draw(d); save(d, G.PX, "hank-${noon.duel.bossHits}-$page"); hits++
                    } else if (noon.duel.wins == 3) {
                        repeat(16) { noon.tick(16) }
                        val d = Frame(G.W, G.H); noon.draw(d); save(d, G.PX, "hank-3-$page")
                    }
                }
            }

            val bank = Play(Western, Western.stages.indexOfFirst { it.title == "Dry Gulch Bank" }, Pace.NORMAL, false, look, Quiet, seed = 2)
            bank.waiting = false
            bank.tick(16)
            shot(bank, "bank-0-whole")
            bank.tap(190, 210); repeat(9) { bank.tick(16) }
            shot(bank, "bank-1-burst")
            repeat(14) { bank.tick(16) }
            bank.tap(172, 210); repeat(12) { bank.tick(16) }
            shot(bank, "bank-2-flying")
            repeat(60) { bank.tick(16) }
            shot(bank, "bank-3-gone")
            while (bank.round.props.any { it.shot }) bank.tick(16)
            shot(bank, "bank-4-back")
        }
    }

    /** High Noon with a fake word up: it slams in like DRAW, but the outlaw's gun stays down. */
    @Test fun duelFake() {
        for (seed in 1L..30L) {
            val s = Showdown(Pace.NORMAL, false, Look.DARK, Quiet, seed = seed)
            s.waiting = false
            s.shownScore = 1_450
            var t = 0
            // The second leader, who always tries at least one.
            while (s.duel.wins < 1 && t < 60_000) { s.tick(16); t += 16; if (s.duel.phase.name == "DRAW") s.shoot() }
            while (!s.duel.faking && t < 120_000 && s.duel.phase.name != "DONE") { s.tick(16); t += 16 }
            if (!s.duel.faking) continue
            repeat(6) { s.tick(16) }
            val d = Frame(G.W, G.H); s.draw(d); save(d, G.PX, "duel-fake")
            return
        }
    }

    @Test fun titleAndDuel() {
        val f = Frame(G.W, G.H); f.look = Look.DARK
        f.page(Page(WesternScenes.title(), Look.DARK, Look.DARK.ink))
        save(f, G.PX, "title")
        val s = Showdown(Pace.NORMAL, false, Look.DARK, Quiet, seed = 5)
        s.tick(16)
        val d = Frame(G.W, G.H); s.draw(d); save(d, G.PX, "duel-steady")
        var t = 0
        while (s.duel.phase.name == "STEADY" && t < 10_000) { s.tick(16); t += 16 }
        s.tick(40)
        val d2 = Frame(G.W, G.H); s.draw(d2); save(d2, G.PX, "duel-draw")
        s.shoot(); repeat(18) { s.tick(16) }
        val d3 = Frame(G.W, G.H); s.draw(d3); save(d3, G.PX, "duel-won")
    }
}
