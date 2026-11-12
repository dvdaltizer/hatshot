package com.slowpixels.hatshot

import com.slowpixels.hatshot.game.Duel
import com.slowpixels.hatshot.game.Frame
import com.slowpixels.hatshot.game.Fx
import com.slowpixels.hatshot.game.G
import com.slowpixels.hatshot.game.Look
import com.slowpixels.hatshot.game.Out
import com.slowpixels.hatshot.game.Pace
import com.slowpixels.hatshot.game.Phase
import com.slowpixels.hatshot.game.Play
import com.slowpixels.hatshot.game.Role
import com.slowpixels.hatshot.game.Round
import com.slowpixels.hatshot.game.Showdown
import com.slowpixels.hatshot.game.Western
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bug hunting by brute force: every stage played by a thumb with no sense at all (taps anywhere, any time, reloads at
 * random, frames of every length), drawn every frame, on both pages and in both modes. Nothing may throw, and the
 * numbers must stay possible.
 */
class HammerTest {
    private class Count : Out {
        var finished = 0
        var sounds = 0
        override fun fx(fx: Fx) { sounds++ }
        override fun buzz(hard: Boolean) = Unit
        override fun silence(ms: Int) = Unit
        override fun finished() { finished++ }
    }

    @Test fun aSenselessThumbBreaksNothing() {
        val frame = Frame(G.W, G.H)
        var games = 0
        Western.stages.forEachIndexed { i, stage ->
            if (stage.duel) return@forEachIndexed
            for (seed in 1L..12L) {
                val rnd = Random(seed * 31 + i)
                val gentle = seed % 4 == 0L
                val out = Count()
                val play = Play(Western, i, Pace.entries[(seed % 3).toInt()], gentle, if (seed % 2 == 0L) Look.DARK else Look.LIGHT, out, seed)
                val r = play.round
                var clock = 0
                var lastScore = 0
                while (r.state == Round.State.PLAYING && clock < 400_000) {
                    // Frames of every length a phone might give: a smooth 16, a stutter, a stall.
                    val dt = when (rnd.nextInt(20)) { 0 -> 80; 1 -> 50; 2 -> 1; else -> 16 }
                    // Some thumbs aim; this one mostly doesn't.
                    repeat(rnd.nextInt(3)) {
                        when (rnd.nextInt(10)) {
                            0 -> play.tap(rnd.nextInt(-5, G.W + 5), rnd.nextInt(G.BAR, G.H + 5))          // the bottom bar
                            1 -> play.tap(rnd.nextInt(G.W), rnd.nextInt(0, G.TOP))                        // the top bar
                            2, 3, 4 -> r.live.randomOrNull(rnd)?.let { t -> play.tap(t.x + rnd.nextInt(t.spec.w), t.y + rnd.nextInt(t.spec.h)) }
                            else -> play.tap(rnd.nextInt(G.W), rnd.nextInt(G.TOP, G.BAR))
                        }
                    }
                    play.tick(dt); clock += dt
                    play.draw(frame)
                    assertTrue("hats ${r.hearts}", r.hearts in 0..Round.START_HEARTS)
                    assertTrue("bullets ${r.bullets}", r.bullets in 0..Round.BULLETS)
                    assertTrue("loaded ${r.loaded}", r.loaded in 0f..Round.BULLETS.toFloat())
                    assertTrue("score ${r.score}", r.score >= 0)
                    assertTrue("left ${r.foesLeft}", r.foesLeft in 0..r.foesTotal)
                    assertTrue("multiplier ${r.multiplier}", r.multiplier in 1..Round.MAX_MULT)
                    assertTrue("hits ${r.hits} of ${r.shots}", r.hits <= r.shots)
                    for (t in r.live) {
                        assertTrue("${stage.title}: ${t.spec.name} rise ${t.rise}", t.rise in 0f..1f)
                        assertTrue("${stage.title}: ${t.spec.name} cover ${t.cover}", t.cover in 0f..1f)
                        // In its opening, or out in the open somewhere near the screen.
                        assertTrue("${stage.title}: ${t.spec.name} at ${t.x},${t.y}", t.x in -80..G.W + 80 && t.y in -80..G.H + 80)
                    }
                    assertTrue("${stage.title}: ${play.juice.bits.size} things in the air", play.juice.bits.size < 600)
                    for (c in frame.px) if (c ushr 24 != 0xFF) throw AssertionError("a see-through pixel in the picture")
                    lastScore = r.score
                }
                assertTrue("${stage.title} seed $seed ends", r.state != Round.State.PLAYING)
                assertEquals("${stage.title} seed $seed says it's over once", 1, out.finished)
                if (r.state == Round.State.WON) assertEquals("${stage.title} seed $seed: nobody left", 0, r.foesLeft)
                // Targets only: nobody shoots back. (Someone who's always there, the piano player or Belle in her cell,
                // can still be hit, as on the Kompakt.)
                if (gentle && stage.resident == null) assertEquals("${stage.title} seed $seed: targets only takes no hats", Round.START_HEARTS, r.hearts)
                // What's in the air comes down, and the picture goes on drawing after the end.
                repeat(400) { play.tick(16); play.draw(frame) }
                assertTrue("${stage.title}: everything has landed", play.juice.bits.isEmpty())
                assertEquals("${stage.title}: still over once", 1, out.finished)
                games++
            }
        }
        println("$games senseless games")
    }

    @Test fun aSenselessThumbAtHighNoonBreaksNothing() {
        val frame = Frame(G.W, G.H)
        for (seed in 1L..40L) {
            val rnd = Random(seed)
            val out = Count()
            val s = Showdown(Pace.entries[(seed % 3).toInt()], seed % 5 == 0L, if (seed % 2 == 0L) Look.DARK else Look.LIGHT, out, seed)
            s.waiting = false
            var clock = 0
            while (out.finished == 0 && clock < 200_000) {
                val dt = if (rnd.nextInt(15) == 0) 80 else 16
                // Taps early, on time and late, by turns.
                when (seed % 4) {
                    0L -> if (rnd.nextInt(40) == 0) s.shoot()
                    1L -> if (s.duel.phase == Duel.Phase.DRAW && s.duel.drawnFor > 150) s.shoot()
                    2L -> Unit
                    else -> if (rnd.nextInt(6) == 0) s.shoot()
                }
                s.tick(dt); clock += dt
                s.draw(frame)
                assertTrue(s.duel.hearts in 0..Duel.HEARTS)
                assertTrue(s.duel.wins in 0..Duel.NEEDED)
                assertTrue(s.who in 0 until Duel.NEEDED)
            }
            assertEquals("seed $seed ends once", 1, out.finished)
            assertEquals(Duel.Phase.DONE, s.duel.phase)
            if (seed % 4 == 1L) assertTrue("a fair draw every time wins", s.duel.beatBoss)
            if (seed % 4 == 2L) assertTrue("never drawing loses", !s.duel.beatBoss)
            repeat(200) { s.tick(16); s.draw(frame) }
            assertEquals(1, out.finished)
        }
    }

    /** Hit on the way out or on the way back in, a snail ducks from where it is: it doesn't pop up to full height first. */
    @Test fun aSnailHitHalfWayOutDucksFromThere() {
        var checked = 0
        for (seed in 1L..30L) {
            val play = Play(Western, 0, Pace.NORMAL, false, Look.DARK, Count(), seed)
            val r = play.round
            var clock = 0
            while (clock < 30_000 && r.state == Round.State.PLAYING) {
                play.tick(4); clock += 4
                val t = r.live.firstOrNull { it.spec.role == Role.FOE && it.phase == Phase.RISING && it.rise in 0.3f..0.7f } ?: continue
                val s = t.slot!!
                if (!t.hittable(t.x + 12, maxOf(t.y, s.y) + 3)) continue
                val at = t.rise
                play.tap(t.x + 12, maxOf(t.y, s.y) + 3)
                if (t.phase != Phase.STUNNED) continue
                var top = at
                while (t.phase == Phase.STUNNED || t.phase == Phase.SINKING) { play.tick(4); top = maxOf(top, t.rise) }
                assertTrue("seed $seed: hit at $at, rose to $top", top <= at + 0.001f)
                checked++
                break
            }
        }
        assertTrue("some snails were caught half way out ($checked)", checked >= 5)
    }

    /** Hitting one of the good folks takes a hat, and the hat is seen leaving the top bar. */
    @Test fun aFriendHitTakesAHatFromTheTopBar() {
        for (seed in 1L..20L) {
            val play = Play(Western, 0, Pace.NORMAL, false, Look.DARK, Count(), seed)
            val r = play.round
            var clock = 0
            while (clock < 60_000 && r.state == Round.State.PLAYING) {
                play.tick(16); clock += 16
                val f = r.live.firstOrNull { it.spec.role == Role.FRIEND && it.phase == Phase.OUT && it.seen > 200 } ?: continue
                val before = r.hearts
                play.tap(f.x + 12, maxOf(f.y, f.slot!!.y) + 8)
                if (r.friendsHit == 0) continue
                assertEquals(before - 1, r.hearts)
                assertTrue(play.juice.bits.any { it.anywhere && it.sprite === com.slowpixels.hatshot.game.WesternArt.LIFE })
                return
            }
        }
        throw AssertionError("no friend came out")
    }
}
