package com.slowpixels.hatshot

import com.slowpixels.hatshot.game.Duel
import com.slowpixels.hatshot.game.DuelEvents
import com.slowpixels.hatshot.game.Events
import com.slowpixels.hatshot.game.Frame
import com.slowpixels.hatshot.game.Fx
import com.slowpixels.hatshot.game.G
import com.slowpixels.hatshot.game.Look
import com.slowpixels.hatshot.game.NoEvents
import com.slowpixels.hatshot.game.Out
import com.slowpixels.hatshot.game.Pace
import com.slowpixels.hatshot.game.Phase
import com.slowpixels.hatshot.game.Play
import com.slowpixels.hatshot.game.Pose
import com.slowpixels.hatshot.game.Role
import com.slowpixels.hatshot.game.Round
import com.slowpixels.hatshot.game.Side
import com.slowpixels.hatshot.game.Stage
import com.slowpixels.hatshot.game.Target
import com.slowpixels.hatshot.game.Western
import com.slowpixels.hatshot.game.WesternArt
import com.slowpixels.hatshot.game.WesternProps
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plays every stage with bots of different skill, and checks the rules and the layouts. */
class RoundTest {
    private val stages = Western.stages.filter { !it.duel }

    /** A player who sees a target [reaction] ms after it's out and taps its middle, reloading when low. */
    private fun play(stage: Stage, pace: Pace, reaction: Int, seed: Long, shootsFriends: Boolean = false, ev: Events = NoEvents,
                     atShells: Boolean = false): Round {
        val r = Round(stage, pace, ev, seed)
        val seen = HashMap<Target, Int>()
        var clock = 0
        var nextShot = 0
        while (r.state == Round.State.PLAYING && clock < 300_000) {
            r.tick(16)
            clock += 16
            for (t in r.live) seen[t] = (seen[t] ?: 0) + 16
            if (r.bullets == 0) { r.reload(); continue }
            if (clock < nextShot) continue
            val mark = r.live.firstOrNull { t ->
                val wanted = t.spec.role != Role.FRIEND || shootsFriends
                val ready = (seen[t] ?: 0) >= reaction
                wanted && ready && t.hittable(t.x + t.spec.w / 2, aimY(t))
            }
            if (mark != null) {
                // Going for the shell, when it's in view and nothing else is in the way; the hat otherwise.
                val box = mark.spec.shell
                val sx = mark.x + (box?.x ?: 0) + (box?.w ?: 0) / 2; val sy = mark.y + (box?.y ?: 0) + 2
                if (atShells && box != null && r.live.all { (it === mark) == it.hittable(sx, sy) }) r.shoot(sx, sy)
                else r.shoot(mark.x + mark.spec.w / 2, aimY(mark))
                nextShot = clock + 220
            }
        }
        return r
    }

    private fun aimY(t: Target): Int {
        val s = t.slot ?: return t.y + t.spec.h / 2
        return maxOf(t.y, s.y) + 8
    }

    @Test fun aGoodPlayerClearsEveryStageAtEveryPace() {
        for (s in stages) for (p in Pace.entries) for (seed in 1L..8L) {
            val r = play(s, p, 450, seed)
            assertEquals("${s.title} ${p.label} seed $seed", Round.State.WON, r.state)
            assertTrue("${s.title} ${p.label} seed $seed leaves a good player ${r.hearts} hats", r.hearts >= 3)
        }
    }

    @Test fun aSlowPlayerStillClearsTheFirstStageOnCalm() {
        for (seed in 1L..8L) assertEquals(Round.State.WON, play(stages[0], Pace.CALM, 1100, seed).state)
    }

    @Test fun nobodyPlayingLosesInTheEnd() {
        for (s in stages) {
            val r = Round(s, Pace.NORMAL, NoEvents, 3)
            var clock = 0
            while (r.state == Round.State.PLAYING && clock < 300_000) { r.tick(16); clock += 16 }
            assertEquals(s.title, Round.State.LOST, r.state)
        }
    }

    @Test fun stagesRunAboutAMinute() {
        for (s in stages) {
            val r = Round(s, Pace.NORMAL, NoEvents, 5)
            val seen = HashMap<Target, Int>()
            var clock = 0
            while (r.state == Round.State.PLAYING && clock < 300_000) {
                r.tick(16); clock += 16
                for (t in r.live) seen[t] = (seen[t] ?: 0) + 16
                if (r.bullets == 0) r.reload()
                r.live.firstOrNull { it.spec.role != Role.FRIEND && (seen[it] ?: 0) > 500 && it.hittable(it.x + it.spec.w / 2, aimY(it)) }
                    ?.let { r.shoot(it.x + it.spec.w / 2, aimY(it)) }
            }
            assertTrue("${s.title} took ${clock / 1000} s", clock in 35_000..100_000)
            println("${s.title}: ${clock / 1000} s, ${r.foesTotal} outlaws, score ${r.score}, stars ${r.stars}")
        }
    }

    // ---- Layouts: everything on the Light Phone's screen, and where a finger can find it. ----

    @Test fun slotsDontOverlap() {
        for (s in stages) s.slots.forEachIndexed { i, a -> s.slots.forEachIndexed { j, b ->
            if (i < j) assertTrue("${s.title} $i and $j",
                a.x + a.w <= b.x || b.x + b.w <= a.x || a.y + a.h <= b.y || b.y + b.h <= a.y)
        } }
    }

    @Test fun everyOpeningIsInsideTheScene() {
        for (s in stages) s.slots.forEachIndexed { i, a ->
            assertTrue("${s.title} slot $i across", a.x >= 0 && a.x + a.w <= G.W)
            assertTrue("${s.title} slot $i down", a.y >= G.TOP && a.y + a.h <= G.BAR)
        }
    }

    /** A target must be at least a fingertip wide: 16 game pixels is 80 screen pixels, about 5 mm. */
    @Test fun peepingTargetsAreBigEnoughToTap() {
        for (s in stages) for (slot in s.slots) assertTrue(s.title, minOf(slot.w, slot.reach) >= 16)
    }

    /** A snail in an opening it rises into shows its whole hat and face: nobody is cut off at the eyes. */
    @Test fun aSnailShowsItsFaceInEveryOpening() {
        for (s in stages) s.slots.forEachIndexed { i, slot ->
            if (slot.from == Side.UP) assertTrue("${s.title} slot $i shows ${slot.reach} rows", slot.reach >= 17)
            assertTrue("${s.title} slot $i is ${slot.w} wide", slot.w >= 18)
        }
    }

    @Test fun wavesOnlyAskForWhatAStageHas() {
        for (s in stages) {
            for (l in s.lineups) for (i in l) assertTrue("${s.title} line-up $l", i in s.slots.indices)
            for (w in s.waves) {
                w.only?.forEach { assertTrue("${s.title} only $it", it in s.slots.indices) }
                for (e in w.events) when (e) {
                    'F', 'f', 'L', 'b', 'B' -> Unit
                    's' -> assertNotNull("${s.title} has a bullseye", s.sign)
                    '*' -> assertNotNull("${s.title} has a star", s.star)
                    'd' -> assertNotNull("${s.title} has dynamite", s.dynamite)
                    else -> assertTrue("${s.title} has a lane '$e'", e in s.lanes)
                }
                if ('s' in w.events) assertTrue("${s.title} has room for a bullseye", s.slots.indices.any { s.slots[it].signs && (w.only == null || it in w.only) })
                if ('L' in w.events) assertTrue("${s.title} has a line-up it may use", s.lineups.any { l -> l.all { w.only == null || it in w.only } })
            }
            for ((spec, x, y) in s.spots) assertTrue("${s.title} ${spec.name} at $x,$y", x >= 0 && x + spec.w <= G.W && y >= G.TOP && y + spec.h <= G.BAR)
            s.resident?.let { (spec, x, y) -> assertTrue("${s.title} ${spec.name}", x >= 0 && x + spec.w <= G.W && y >= G.TOP && y + spec.h <= G.BAR) }
            for (x in s.floatXs) assertTrue("${s.title} balloon at $x", x >= 0 && x + 18 <= G.W)
            for (l in s.lanes.values) assertTrue("${s.title} ${l.spec.name} lane", l.y - l.hop >= G.TOP && l.y + l.spec.h <= G.BAR)
        }
    }

    // ---- Rules. ----

    @Test fun aMissEndsAStreakAndUsesABullet() {
        val r = Round(stages[0], Pace.NORMAL, NoEvents, 1)
        r.shoot(2, 100)
        assertEquals(Round.BULLETS - 1, r.bullets)
        assertEquals(0, r.streak)
        assertEquals(1, r.shots)
        assertEquals(0, r.hits)
    }

    @Test fun anEmptyGunDoesNothingUntilReloaded() {
        val r = Round(stages[0], Pace.NORMAL, NoEvents, 1)
        repeat(Round.BULLETS) { r.shoot(2, 100) }
        assertEquals(0, r.bullets)
        r.shoot(2, 100)
        assertEquals(Round.BULLETS, r.shots)
        r.reload()
        r.shoot(2, 100)
        assertEquals("no shooting while loading", Round.BULLETS, r.shots)
        repeat(40) { r.tick(16) }
        assertEquals(Round.BULLETS, r.bullets)
    }

    @Test fun shootingAFriendCostsAHat() {
        val r = play(stages[0], Pace.NORMAL, 300, 2, shootsFriends = true)
        assertTrue(r.friendsHit > 0)
        assertTrue(r.hearts < Round.START_HEARTS || r.state == Round.State.LOST)
    }

    @Test fun threeDuelsWinTheShowdown() {
        var clock = 0
        var over = false
        val ev = object : DuelEvents {
            override fun changed() = Unit
            override fun fx(fx: Fx) = Unit
            override fun buzz() = Unit
            override fun finished() { over = true }
        }
        val d = Duel(Pace.NORMAL, ev, 4)
        while (!over && clock < 60_000) {
            d.tick(16); clock += 16
            if (d.phase == Duel.Phase.DRAW) d.shoot()
        }
        assertTrue(d.beatBoss)
        assertEquals(3, d.stars)
    }

    @Test fun theLeadersTryFakeWordsAndFiringAtOneIsAFoul() {
        val ev = object : DuelEvents {
            override fun changed() = Unit
            override fun fx(fx: Fx) = Unit
            override fun buzz() = Unit
            override fun finished() = Unit
        }
        // Over a few showdowns played well, fakes come up, never read DRAW, and the real word still comes.
        var shown = 0
        for (seed in 1L..8L) {
            val d = Duel(Pace.NORMAL, ev, seed)
            var clock = 0
            while (d.phase != Duel.Phase.DONE && clock < 120_000) {
                d.tick(16); clock += 16
                if (d.faking) assertTrue(d.word != "DRAW!" && d.word in Duel.FAKE_WORDS)
                if (d.phase == Duel.Phase.DRAW) d.shoot()
            }
            assertTrue("seed $seed: won by waiting for the real word", d.beatBoss)
            shown += d.fakesShown
        }
        assertTrue("the leaders tried some fakes", shown >= 8)
        // Firing at one costs a hat, and says so.
        for (seed in 1L..20L) {
            val d = Duel(Pace.NORMAL, ev, seed)
            var clock = 0
            while (!d.faking && d.phase != Duel.Phase.DONE && clock < 60_000) {
                d.tick(16); clock += 16
                if (d.phase == Duel.Phase.DRAW) d.shoot()
            }
            if (!d.faking) continue
            d.shoot()
            assertEquals(Duel.Phase.FOUL, d.phase)
            assertEquals("FOOLED YA!", d.word)
            assertEquals(1, d.fooled)
            assertEquals(Duel.HEARTS - 1, d.hearts)
            return
        }
        error("no fake word came up")
    }

    @Test fun targetsOnlyDuelsHaveNoTricks() {
        val ev = object : DuelEvents {
            override fun changed() = Unit
            override fun fx(fx: Fx) = Unit
            override fun buzz() = Unit
            override fun finished() = Unit
        }
        for (seed in 1L..8L) {
            val d = Duel(Pace.NORMAL, ev, seed, tricks = false)
            var clock = 0
            while (d.phase != Duel.Phase.DONE && clock < 120_000) {
                d.tick(16); clock += 16
                assertFalse(d.faking)
                if (d.phase == Duel.Phase.DRAW) d.shoot()
            }
            assertEquals(0, d.fakesShown)
        }
    }

    @Test fun targetsOnlyHasNoPeopleAndNobodyShootsBack() {
        for (s in stages) for (seed in 1L..6L) {
            val r = Round(s, Pace.NORMAL, NoEvents, seed, gentle = true)
            var clock = 0
            while (r.state == Round.State.PLAYING && clock < 300_000) {
                r.tick(16); clock += 16
                for (t in r.live) {
                    assertTrue("${s.title}: ${t.spec.name} is out", t.spec.role != Role.FOE && t.spec.role != Role.LOB)
                    assertTrue("${s.title}: ${t.spec.name} is out", t.spec.role != Role.FRIEND || t.forever)
                }
            }
            assertEquals("${s.title} ends by itself", Round.State.WON, r.state)
            assertEquals("${s.title} takes no hats", Round.START_HEARTS, r.hearts)
            assertEquals("${s.title} counts its targets down", 0, r.foesLeft)
        }
    }

    @Test fun aShotOnTheShellScoresDoubleAndAShotOnTheHatDoesNot() {
        val stage = stages.first { it.title == "The Rusty Spur" }   // its upstairs rooms show a snail whole
        for (onShell in listOf(true, false)) for (seed in 1L..6L) {
            val r = Round(stage, Pace.NORMAL, NoEvents, seed)
            var clock = 0
            var t: Target? = null
            while (t == null && clock < 60_000) {
                r.tick(16); clock += 16
                t = r.live.firstOrNull { it.spec.role == Role.FOE && it.phase == Phase.OUT && it.slotIndex in 0..3 && it.seen > 600 }
            }
            t!!
            val box = t.spec.shell!!
            if (onShell) r.shoot(t.x + box.x + box.w / 2, t.y + box.y + box.h / 2) else r.shoot(t.x + 12, t.y + 4)
            assertEquals("seed $seed", if (onShell) Pose.BARE else Pose.HIT, t.pose)
            assertEquals("seed $seed", if (onShell) 200 else 100, r.score)
            assertEquals(if (onShell) 1 else 0, r.shellsPopped)
        }
    }

    @Test fun hittingBelleEndsTheStage() {
        val train = stages.first { it.title == "The Iron Horse" }
        var done = false
        for (seed in 1L..40L) {
            val r = Round(train, Pace.NORMAL, NoEvents, seed)
            var clock = 0
            while (r.state == Round.State.PLAYING && clock < 120_000 && !done) {
                r.tick(16); clock += 16
                if (r.bullets == 0) r.reload()
                val belle = r.live.firstOrNull { it.spec.dear && it.phase == Phase.OUT }
                if (belle != null) {
                    r.shoot(belle.x + 12, maxOf(belle.y, belle.slot!!.y) + 8)
                    assertEquals(Round.State.LOST, r.state)
                    assertTrue(r.lostDear)
                    done = true
                }
            }
            if (done) break
        }
        assertTrue("Belle came to a window", done)
    }

    // ---- What the Light Phone adds: things fly. ----

    private object Quiet : Out {
        override fun fx(fx: Fx) = Unit
        override fun buzz(hard: Boolean) = Unit
        override fun silence(ms: Int) = Unit
        override fun finished() = Unit
    }

    @Test fun aHatShotOffFliesAndFallsAway() {
        val play = Play(Western, 0, Pace.NORMAL, false, Look.DARK, Quiet, seed = 4)
        var clock = 0
        var t: Target? = null
        while (t == null && clock < 60_000) {
            play.tick(16); clock += 16
            t = play.round.live.firstOrNull { it.spec.role == Role.FOE && it.phase == Phase.OUT && it.seen > 300 }
        }
        t!!
        play.tap(t.x + 12, maxOf(t.y, t.slot!!.y) + 6)
        assertEquals(1, play.round.foesHit)
        val hat = play.juice.bits.single { it.sprite === WesternArt.hatOf(t.spec.name) }
        assertTrue("and a spent case with it", play.juice.bits.any { it.sprite === com.slowpixels.hatshot.game.Juice.CASE })
        val startY = hat.y
        repeat(30) { play.tick(16) }
        assertTrue("it has gone up", hat.y < startY)
        repeat(260) { play.tick(16) }
        assertTrue("and it's gone", hat !in play.juice.bits)
        // Drawing it never throws, wherever the hat is.
        play.draw(Frame(G.W, G.H))
    }

    @Test fun beingShotShakesTheSceneAndTakesAHatFromTheTopBar() {
        val play = Play(Western, 0, Pace.NORMAL, false, Look.DARK, Quiet, seed = 4)
        var clock = 0
        while (play.round.hearts == Round.START_HEARTS && clock < 60_000) { play.tick(16); clock += 16 }
        assertEquals(Round.START_HEARTS - 1, play.round.hearts)
        assertTrue(play.juice.shaking > 0)
        assertTrue("the lost hat leaves the top bar", play.juice.bits.any { it.anywhere && it.sprite === WesternArt.LIFE })
    }

    @Test fun theBarSaysReloadTheMomentTheLastBulletGoes() {
        val play = Play(Western, 0, Pace.NORMAL, false, Look.DARK, Quiet, seed = 4)
        play.tick(16)
        // Six shots at the empty sky: no seventh, empty click is needed before the bar asks to reload.
        repeat(Round.BULLETS) {
            assertFalse("not while there are bullets left", play.wanted)
            play.tap(G.W / 2, G.TOP + 2)
            play.tick(16)
        }
        assertEquals(0, play.round.bullets)
        assertTrue("RELOAD shows at once", play.wanted)
        // A tap on the bar reloads, and the word goes as the bullets go back in.
        play.tap(G.W / 2, G.BAR + 4)
        play.tick(16)
        assertTrue(play.round.reloading > 0)
        assertFalse(play.wanted)
        repeat(60) { play.tick(16) }
        assertEquals(Round.BULLETS, play.round.bullets)
        assertFalse(play.wanted)
    }

    @Test fun theTntTakesTheOutlawBehindItAndAnotherCrateComesBack() {
        val i = Western.stages.indexOfFirst { it.tnt != null }
        val stage = Western.stages[i]
        val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 2)
        val r = play.round
        val slot = stage.slots[stage.tntSlot]
        val t = Target(stage.foes[0], slot, stage.tntSlot, 99_999)
        t.phase = Phase.OUT; t.rise = 1f; t.cover = 1f
        r.place(t)
        r.live += t
        play.tick(1)
        assertTrue(r.tntReady)
        val box = stage.tnt!!
        play.tap(box.x + box.w / 2, box.y + box.h / 2)
        assertEquals("the outlaw behind the crate is got", 1, r.foesHit)
        assertEquals(Phase.STUNNED, t.phase)
        assertFalse("and the crate's gone", r.tntReady)
        // Nobody hides behind a crate that isn't there; then somebody pushes another one in.
        var clock = 0
        while (!r.tntReady && clock < Round.TNT_BACK_MS + Round.TNT_PUSH_MS + 1000) {
            play.tick(16); clock += 16
            if (!r.tntReady) assertTrue("no one behind the missing crate", r.live.none { it.slotIndex == stage.tntSlot && it !== t })
        }
        assertTrue(r.tntReady)
        assertTrue(clock >= Round.TNT_BACK_MS)
    }

    @Test fun theTntGetsAnOutlawWhoIsTakingAim() {
        val i = Western.stages.indexOfFirst { it.tnt != null }
        val stage = Western.stages[i]
        val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 2)
        val r = play.round
        // Nobody behind the crate; one in the mine cart, gun up.
        val cart = stage.slots.indexOfFirst { it.signs }
        val t = Target(stage.foes[0], stage.slots[cart], cart, 99_999)
        t.phase = Phase.AIMING; t.rise = 1f; t.cover = 1f
        r.place(t)
        r.live += t
        play.tick(1)
        val box = stage.tnt!!
        play.tap(box.x + box.w / 2, box.y + box.h / 2)
        assertEquals(1, r.foesHit)
        assertEquals(Phase.STUNNED, t.phase)
    }

    // ---- Scenery that answers back. ----

    private fun prop(r: Round, name: String) = r.props.first { it.prop.name == name }

    @Test fun theStoresGoodsSpillOnceForAFewPoints() {
        val store = stages.first { it.title == "General Store" }
        for (name in listOf(WesternProps.OATS, WesternProps.RICE, WesternProps.EGGS)) {
            val r = Round(store, Pace.NORMAL, NoEvents, 3)
            r.tick(16)
            val p = prop(r, name)
            val box = p.prop.box
            r.shoot(box.x + box.w / 2, box.y + box.h / 2)
            assertTrue("$name is shot open", p.shot)
            assertEquals("a few points, as a hit", 20, r.score)
            assertEquals(1, r.hits)
            assertEquals(1, r.streak)
            // It stays as it's left: another shot at it is a miss, and nothing brings it back.
            r.shoot(box.x + box.w / 2, box.y + box.h / 2)
            assertEquals(20, r.score)
            assertEquals("a miss ends the streak", 0, r.streak)
            repeat(1500) { r.tick(16) }
            assertTrue("and it never mends", p.shot)
        }
    }

    @Test fun theTillRingsEveryTimeAndCostsOnlyTheBullet() {
        val store = stages.first { it.title == "General Store" }
        val r = Round(store, Pace.NORMAL, NoEvents, 3)
        r.tick(16)
        // A streak going, to see the till leaves it alone.
        val oats = prop(r, WesternProps.OATS).prop.box
        r.shoot(oats.x + 10, oats.y + 16)
        val till = prop(r, WesternProps.TILL)
        val box = till.prop.box
        assertEquals("it starts at 5", 5, WesternProps.tillShows(till))
        var was = 5
        repeat(4) { n ->
            r.tick(160)
            r.shoot(box.x + box.w / 2, box.y + box.h / 2)
            assertEquals(n + 1, till.times)
            val now = WesternProps.tillShows(till)
            assertTrue("a new number every time: $was then $now", now != was && now in 1..99)
            was = now
        }
        assertEquals("no points for it", 20, r.score)
        assertEquals("and it isn't counted for or against anyone's aim", 1, r.shots)
        assertEquals(1, r.hits)
        assertEquals("the streak's as it was", 1, r.streak)
        assertEquals("but the bullets are gone", Round.BULLETS - 5, r.bullets)
    }

    @Test fun whoeverIsInFrontOfASackTakesTheShot() {
        val store = stages.first { it.title == "General Store" }
        val r = Round(store, Pace.NORMAL, NoEvents, 3)
        r.tick(16)
        // The loose wheel, rolling past the sacks.
        val wheel = Target(store.lanes.getValue('w').spec, null, -1, 0)
        wheel.x = 6; wheel.y = 201; wheel.phase = Phase.RUNNING
        r.live += wheel
        r.shoot(14, 208)
        assertEquals("the wheel is hit", Phase.GONE, wheel.phase)
        assertFalse("and the sack behind it isn't", prop(r, WesternProps.OATS).shot)
    }

    @Test fun theBanksMoneyBagsBurstAndComeBack() {
        val i = Western.stages.indexOfFirst { it.title == "Dry Gulch Bank" }
        val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 2)
        val r = play.round
        play.tick(16)
        val bags = r.props.filter { it.prop.name == WesternProps.BAG }
        assertEquals(3, bags.size)
        val bag = bags[1]
        val box = bag.prop.box
        play.tap(box.x + 8, box.y + 8)
        assertTrue(bag.shot)
        assertEquals(20, r.score)
        assertTrue("money flies", play.juice.bits.count { it.sprite === com.slowpixels.hatshot.game.Juice.COIN } >= 5)
        assertTrue(play.juice.bits.any { it.sprite === com.slowpixels.hatshot.game.Juice.BILL })
        assertTrue("the others are still there", bags[0].takes(bags[0].prop.box.x + 8, box.y + 8) && !bags[0].shot)
        // While it's gone there's nothing there to shoot.
        play.tap(box.x + 8, box.y + 8)
        assertEquals(20, r.score)
        var clock = 0
        while (bag.shot && clock < 10_000) { play.tick(16); clock += 16; play.draw(Frame(G.W, G.H)) }
        assertTrue("back in a few seconds: $clock", clock in 3500..4500)
        if (r.bullets == 0) { r.reload(); repeat(60) { play.tick(16) } }
        val before = r.score
        play.tap(box.x + 8, box.y + 8)
        assertTrue("and it can be burst again", bag.shot && r.score > before)
    }

    /** Shoots every prop called [name] in [title] once, and again: points the first time only, and none of them mends. */
    private fun breaksOnce(title: String, name: String, points: Int) {
        val stage = stages.first { it.title == title }
        val r = Round(stage, Pace.NORMAL, NoEvents, 2)
        r.tick(16)
        val all = r.props.filter { it.prop.name == name }
        assertTrue("$title has a $name", all.isNotEmpty())
        var score = 0
        for (p in all) {
            if (r.bullets == 0) { r.reload(); repeat(60) { r.tick(16) } }
            val box = p.prop.box
            r.shoot(box.x + box.w / 2, box.y + box.h / 2)
            assertTrue("$name is shot", p.shot)
            assertTrue("$name scores a little", r.score > score && r.score - score <= points * Round.MAX_MULT)
            score = r.score
            if (r.bullets == 0) { r.reload(); repeat(60) { r.tick(16) } }
            r.shoot(box.x + box.w / 2, box.y + box.h / 2)
            assertEquals("$name: only the once", score, r.score)
        }
        repeat(1200) { r.tick(16) }
        assertTrue("$name stays as it's left", all.all { it.shot })
    }

    @Test fun sceneryBreaksOnceForAFewPointsAndStaysBroken() {
        breaksOnce("General Store", WesternProps.CLOCK, 20)
        breaksOnce("General Store", WesternProps.SHELF, 10)
        breaksOnce("The Rusty Spur", WesternProps.MIRROR, 20)
        breaksOnce("The Rusty Spur", WesternProps.GLASS, 10)
        breaksOnce("The Rusty Spur", WesternProps.KEG, 20)
        breaksOnce("The Rusty Spur", WesternProps.SPITTOON, 10)
        breaksOnce("Dry Gulch Bank", WesternProps.SIGN, 20)
        breaksOnce("The Jailhouse", WesternProps.TROUGH, 20)
        breaksOnce("The Jailhouse", WesternProps.BARRELS, 20)
        breaksOnce("The Jailhouse", WesternProps.JAIL_SIGN, 20)
        assertEquals("everything on the store's shelves", 13, stages.first { it.title == "General Store" }.props.count { it.name == WesternProps.SHELF })
    }

    @Test fun propsDontCoverEachOtherOrTheOpenings() {
        for (stage in stages) {
            val boxes = stage.props.map { it.box }
            for (a in boxes.indices) for (b in a + 1 until boxes.size) {
                val p = boxes[a]; val q = boxes[b]
                assertFalse("${stage.title}: ${stage.props[a].name} and ${stage.props[b].name}",
                    p.x < q.x + q.w && q.x < p.x + p.w && p.y < q.y + q.h && q.y < p.y + p.h)
            }
            for (p in stage.props) {
                assertTrue("${stage.title}: ${p.name} is in the scene",
                    p.box.x >= 0 && p.box.x + p.box.w <= G.W && p.box.y >= G.TOP && p.box.y + p.box.h <= G.BAR)
                for (s in stage.slots) assertFalse("${stage.title}: ${p.name} is over an opening",
                    p.box.x < s.x + s.w && s.x < p.box.x + p.box.w && p.box.y < s.y + s.h && s.y < p.box.y + p.box.h)
            }
        }
    }

    @Test fun theMineCartRollsAwayAndComesBack() {
        val i = Western.stages.indexOfFirst { it.title == "Buzzard Canyon" }
        val stage = Western.stages[i]
        val cart = stage.slots.indexOfFirst { it.signs }
        val play = Play(Western, i, Pace.NORMAL, false, Look.DARK, Quiet, seed = 3)
        val r = play.round
        var away = 0; var back = 0; var wasShut = false
        var clock = 0
        while (r.state == Round.State.PLAYING && clock < 60_000) {
            play.tick(16); clock += 16
            if (r.bullets == 0) r.reload()
            val shut = cart in r.shut
            if (shut && !wasShut) away++
            if (!shut && wasShut) back++
            wasShut = shut
            // Nobody comes out where the cart isn't, and it never leaves with somebody in it.
            if (shut) assertTrue("at $clock ms", r.live.none { it.slotIndex == cart && it.phase != Phase.GONE })
            r.live.firstOrNull { it.spec.role == Role.FOE && it.phase == Phase.OUT && it.seen > 250 }?.let { play.tap(it.x + 12, maxOf(it.y, it.slot!!.y) + 6) }
            if (clock % 800 == 0) play.draw(Frame(G.W, G.H))
        }
        assertTrue("it went up the line and came back: $away away, $back back", away >= 2 && back >= 2)
    }

    @Test fun blackShellHankTakesThreeShots() {
        val ev = object : DuelEvents {
            override fun changed() = Unit
            override fun fx(fx: Fx) = Unit
            override fun buzz() = Unit
            override fun finished() = Unit
        }
        val d = Duel(Pace.NORMAL, ev, 7)
        var draws = 0
        val seen = ArrayList<Pair<Int, Int>>()
        var clock = 0
        while (d.phase != Duel.Phase.DONE && clock < 120_000) {
            d.tick(16); clock += 16
            if (d.phase == Duel.Phase.DRAW) { d.shoot(); draws++; seen += d.wins to d.bossHits }
        }
        assertEquals("two leaders, then Hank three times", 5, draws)
        assertEquals(listOf(1 to 0, 2 to 0, 2 to 1, 2 to 2, 3 to 3), seen)
        assertTrue(d.beatBoss)
        assertEquals(3, d.stars)
        // Drawn all the way through, with his hat and bandana going.
        val s = com.slowpixels.hatshot.game.Showdown(Pace.NORMAL, false, Look.DARK, Quiet, seed = 7)
        s.waiting = false
        var t = 0
        while (s.duel.phase != Duel.Phase.DONE && t < 120_000) {
            s.tick(16); t += 16
            if (s.duel.phase == Duel.Phase.DRAW) s.shoot()
            if (t % 400 == 0) s.draw(Frame(G.W, G.H))
        }
        assertTrue(s.duel.beatBoss)
    }

    @Test fun belleIsBehindBarsInTheJailAndInTheCage() {
        // Every bar runs unbroken from the top of the opening to the bottom, over her and over the dark.
        fun barred(s: com.slowpixels.hatshot.game.Sprite, from: Int, to: Int) {
            for (b in WesternArt.CELL_BARS) for (x in b..b + 1) for (y in from until to) {
                val v = s.px[y * s.w + x]
                assertTrue("bar at $x, $y", v == com.slowpixels.hatshot.game.INK || v == com.slowpixels.hatshot.game.LINE)
            }
            // And she's there behind them: some of her shows between the bars.
            assertTrue(s.px.any { it == com.slowpixels.hatshot.game.PAPER })
        }
        for (s in WesternArt.belleInCell) barred(s, 0, WesternArt.CELL_H)
        // In the cage, down as far as the rail the padlock hangs from.
        for (s in WesternArt.belleCaged) barred(s, 4, WesternArt.CAGE_HEAD + 21)
        // In the jail she's a whole window's worth, and hitting anywhere in it is hitting her.
        val jail = stages.first { it.title == "The Jailhouse" }
        val r = Round(jail, Pace.NORMAL, NoEvents, 2)
        r.tick(16)
        val belle = r.live.first { it.spec.dear }
        assertTrue(belle.spec.plain)
        r.shoot(belle.x + 15, belle.y + 20)
        assertTrue(r.lostDear)
    }

    @Test fun highNoonOpensWithAFewQuietSeconds() {
        val ev = object : DuelEvents {
            override fun changed() = Unit
            override fun fx(fx: Fx) = Unit
            override fun buzz() = Unit
            override fun finished() = Unit
        }
        for (pace in Pace.entries) for (seed in 1L..60L) {
            val d = Duel(pace, ev, seed)
            var clock = 0
            while (clock < 3000) {
                d.tick(16); clock += 16
                assertEquals("seed $seed at $clock ms: nothing yet", Duel.Phase.STEADY, d.phase)
                assertFalse("seed $seed at $clock ms: no fake word either", d.faking)
                assertEquals("STEADY...", d.word)
            }
            // Then it plays as ever, and the next duel doesn't hold off again.
            while (d.phase == Duel.Phase.STEADY && clock < 20_000) { d.tick(16); clock += 16 }
            assertEquals(Duel.Phase.DRAW, d.phase)
            d.shoot()
            var second = 0
            while (d.phase != Duel.Phase.STEADY) d.tick(16)
            while (d.phase == Duel.Phase.STEADY && !d.faking && second < 20_000) { d.tick(16); second += 16 }
            assertTrue("seed $seed: the second duel's first word came at $second ms", second < 9000)
        }
        assertTrue(Duel.FIRST_WORD_MS >= 3000)
    }

    @Test fun everyoneWithAHatHasOneToLose() {
        for (name in listOf("bandana", "mustache", "beard", "scowl", "sheriff", "kid", "bonnet", "banker", "belle", "mosey")) {
            assertNotNull(name, WesternArt.hatOf(name))
        }
        // Granny's bun and the barkeep's bald head stay on.
        assertEquals(null, WesternArt.hatOf("granny"))
        assertEquals(null, WesternArt.hatOf("barkeep"))
    }
}

class VersionTest {
    @Test fun aboutShowsTheVersionInTheManifest() {
        val toml = File("lighttool.toml").readText()
        val named = Regex("""versionName\s*=\s*"([^"]+)"""").find(toml)!!.groupValues[1]
        val code = Regex("""versionCode\s*=\s*(\d+)""").find(toml)!!.groupValues[1].toInt()
        assertEquals(named, Version.NAME)
        assertEquals(code, Version.CODE)
        assertTrue("the simulator's server never goes out", Regex("""(?m)^serverPackage = "com.lightos"""").containsMatchIn(toml))
    }
}
