package com.slowpixels.hatshot.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * One go at a stage: what's out, what got hit, the score. No drawing and no Android here, so tests can play it.
 *
 * The rules are the Kompakt game's, unchanged. What's new is how things move: the Kompakt's E Ink panel could only show
 * a target in two or three separate pictures, while the Light Phone's screen can slide it out smoothly, so every
 * opening, rising, sinking and drifting here runs on time, a game pixel at a step.
 *
 * Positions are game pixels ([G]), times are milliseconds.
 */
class Round(
    val stage: Stage, val pace: Pace, private val ev: Events, seed: Long = System.nanoTime(),
    /** Targets only: bullseyes come out in place of people, so nobody is shot at and nobody shoots back. */
    val gentle: Boolean = false,
) {
    private val rnd = Random(seed)
    private val k = pace.factor

    var state = State.PLAYING
        private set
    var hearts = START_HEARTS
        private set
    var bullets = BULLETS
        private set
    /** Above zero while the bullets are going in. */
    var reloading = 0
        private set
    var score = 0
        private set
    var streak = 0
        private set
    var bestStreak = 0
        private set
    var shots = 0
        private set
    var hits = 0
        private set
    var foesHit = 0
        private set
    var friendsHit = 0
        private set
    /** Belle was hit, so the stage is over whatever's left. */
    var lostDear = false
        private set
    /** Outlaws shot in the shell, which blows it to bits and scores double. */
    var shellsPopped = 0
        private set
    /** Outlaws still to come or still out: the number on the wanted poster. */
    var foesLeft = stage.waves.sumOf { w -> w.events.sumOf { foesIn(it) + if (gentle && it == 's') 1 else 0 } }
        private set
    val foesTotal = foesLeft
    /** Which wave is on, from 0; -1 before the first. */
    var wave = -1
        private set
    /** Milliseconds since the round began, for anything that sways or bobs. */
    var clock = 0L
        private set

    val live = ArrayList<Target>()
    val flashes = ArrayList<Flash>()

    private val bag = ArrayDeque<Char>()
    private var nextIn = 700
    private var lastSlot = -1
    private var lastFoe: Spec? = null
    private var lastFriend: Spec? = null
    private var bottlesLaid = 0
    private var bottlesHit = 0

    init {
        stage.resident?.let { (spec, x, y) ->
            live += Target(spec, null, -1, 0).apply { this.x = x; this.y = y; phase = Phase.OUT; rise = 1f; forever = true }
        }
    }

    val multiplier get() = 1 + min(streak / STREAK_STEP, MAX_MULT - 1)
    val accuracy get() = if (shots == 0) 0 else hits * 100 / shots
    /** Stars for this go: one for getting through, two for a steady hand, three for a clean one. */
    val stars get() = when {
        state != State.WON -> 0
        accuracy >= THREE_STAR_AIM && friendsHit == 0 && hearts >= THREE_STAR_HATS -> 3
        accuracy >= TWO_STAR_AIM && hearts >= TWO_STAR_HATS -> 2
        else -> 1
    }

    /** What the next star would have taken, in a few plain words, or null when there's no star left to get. */
    val nextStar: String? get() = when (stars) {
        0 -> if (lostDear) null else "Finish the stage for a star."
        1 -> needs(listOfNotNull(
            "$TWO_STAR_AIM% accuracy".takeIf { accuracy < TWO_STAR_AIM },
            "$TWO_STAR_HATS hats left".takeIf { hearts < TWO_STAR_HATS }))
        2 -> needs(listOfNotNull(
            "$THREE_STAR_AIM% accuracy".takeIf { accuracy < THREE_STAR_AIM },
            "no friends hit".takeIf { friendsHit > 0 },
            "$THREE_STAR_HATS hats left".takeIf { hearts < THREE_STAR_HATS }))
        else -> null
    }

    /** How far the bullets have gone back in, 0 to 6, while reloading. */
    val loaded: Float get() = if (reloading <= 0) bullets.toFloat() else BULLETS * (1f - reloading.toFloat() / reloadMs)
    private val reloadMs get() = (RELOAD_MS * min(k, 1f)).toInt()

    /** The stage's crate of TNT is there to be shot. */
    var tntReady = stage.tnt != null
        private set
    /** While above zero the crate's gone, and this long until somebody brings another. */
    private var tntGone = 0
    /** While above zero the new crate is being pushed in: how much longer that takes. */
    var tntPushing = 0
        private set

    /** Openings nobody can come out of just now: what they'd stand in or behind is away (a mine cart, gone up the line). */
    var shut: Set<Int> = emptySet()

    /** The stage's scenery that can be shot, and how each piece stands. */
    val props = stage.props.map { PropState(it) }

    // ---- Time. ----

    fun tick(ms: Int) {
        clock += ms
        if (tntGone > 0) { tntGone -= ms; if (tntGone <= 0) { tntPushing = TNT_PUSH_MS; ev.fx(Fx.Creak) } }
        else if (tntPushing > 0) { tntPushing -= ms; if (tntPushing <= 0) { tntPushing = 0; tntReady = true } }
        for (p in props) {
            if (p.times > 0 && p.since < FOREVER) p.since += ms
            if (p.gone > 0) { p.gone -= ms; if (p.gone <= 0) { p.gone = 0; p.shot = false; ev.mended(p) } }
        }
        if (reloading > 0) {
            reloading -= ms
            if (reloading <= 0) { reloading = 0; bullets = BULLETS; ev.fx(Fx.Loaded) }
        }
        for (i in flashes.indices.reversed()) {
            val f = flashes[i]
            f.ms -= ms
            if (f.ms <= 0) flashes.removeAt(i)
        }
        for (i in live.indices.reversed()) if (i < live.size) step(live[i], ms)
        live.removeAll { it.phase == Phase.GONE }
        if (state == State.PLAYING) direct(ms)
    }

    private fun direct(ms: Int) {
        nextIn -= ms
        if (nextIn > 0) return
        if (bag.isEmpty()) {
            if (live.any { it.busy }) { nextIn = 100; return }
            val next = stage.waves.getOrNull(wave + 1)
            if (next == null) { state = State.WON; ev.finished(); return }
            wave++
            val events = next.events.toList()
            bag.addAll(if (next.shuffle) events.shuffled(rnd) else events)
            if (next.bottles) layBottles()
            nextIn = (WAVE_REST * k).toInt()
            ev.wave(wave)
            return
        }
        val w = stage.waves[wave]
        if (spawn(bag.first(), w)) {
            bag.removeFirst()
            nextIn = (w.gap * k).toInt()
        } else nextIn = 120
    }

    private fun spawn(e: Char, w: Wave): Boolean {
        val out = live.count { it.slot != null }
        val stay = (w.stay * k).toInt()
        return when (e) {
            'F', 'd' -> {
                if (out >= w.most) return false
                val i = freeSlot(w) ?: return false
                if (gentle) pop(stage.sign ?: return true, i, stay) else pop(foe(), i, stay, throws = e == 'd')
                true
            }
            'f' -> {
                if (gentle) return true
                if (out >= w.most) return false
                val i = freeSlot(w) ?: return false
                pop(friend(), i, stay); true
            }
            's' -> {
                val sign = stage.sign ?: return true
                if (out >= w.most) return false
                val i = freeSlot(w, signsToo = true) ?: return false
                pop(sign, i, stay); true
            }
            'L' -> {
                if (stage.lineups.isEmpty()) return true
                val free = stage.lineups.filter { l ->
                    l.all { i -> (w.only == null || i in w.only) && live.none { it.slotIndex == i } && (i != stage.tntSlot || tntReady) && i !in shut }
                }
                if (free.isEmpty()) return false
                val l = free.random(rnd)
                val odd = l.random(rnd)
                for (i in l) when {
                    gentle -> if (i != odd) stage.sign?.let { pop(it, i, stay) }
                    i == odd -> pop(friend(), i, stay)
                    else -> pop(foe(), i, stay)
                }
                ev.fx(Fx.Creak)
                true
            }
            'b', 'B', '*' -> {
                val spec = if (e == '*') stage.star else stage.balloon
                if (spec != null) {
                    float(spec, stage.floatXs.random(rnd))
                    if (e == 'B') float(spec, stage.floatXs.random(rnd).let { x -> if (x < G.W / 2) x + 64 else x - 64 })
                }
                true
            }
            else -> {
                stage.lanes[e]?.let { run(it) }
                true
            }
        }
    }

    private fun foe(): Spec {
        val s = stage.foes.filter { it !== lastFoe }.ifEmpty { stage.foes }.random(rnd)
        lastFoe = s
        return s
    }

    private fun friend(): Spec {
        val s = stage.friends.filter { it !== lastFriend }.ifEmpty { stage.friends }.random(rnd)
        lastFriend = s
        return s
    }

    private fun freeSlot(w: Wave, signsToo: Boolean = false): Int? {
        val all = stage.slots.indices.filter { i ->
            live.none { it.slotIndex == i } && (!signsToo || stage.slots[i].signs) && (w.only == null || i in w.only) &&
                (i != stage.tntSlot || tntReady) && i !in shut
        }
        // Not the same place twice running, unless it's the only place left.
        val i = all.filter { it != lastSlot }.ifEmpty { all }.randomOrNull(rnd) ?: return null
        lastSlot = i
        return i
    }

    private fun pop(spec: Spec, index: Int, stay: Int, throws: Boolean = false) {
        val slot = stage.slots[index]
        val t = Target(spec, slot, index, stay)
        t.throws = throws
        t.aim = if (spec.role == Role.FOE) max(MIN_AIM, stay * 35 / 100) else 0
        t.phase = if (slot.opens) Phase.OPENING else Phase.RISING
        t.cover = if (slot.opens) 0f else 1f
        t.rise = 0f
        place(t)
        live += t
        if (slot.opens) ev.fx(Fx.Creak)
    }

    private fun float(spec: Spec, x: Int) {
        val t = Target(spec, null, -1, 0)
        t.x = x; t.y = G.BAR - 2; t.ox = x; t.oy = t.y
        t.fx = x.toFloat(); t.fy = t.y.toFloat()
        t.phase = Phase.FLOATING
        t.speed = FLOAT_STEP / (FLOAT_MS * k)
        live += t
    }

    private fun run(lane: Lane) {
        val t = Target(lane.spec, null, -1, 0)
        val fromLeft = if (lane.bothWays) rnd.nextBoolean() else !lane.fromRight
        t.flip = !fromLeft
        t.x = if (fromLeft) -lane.spec.w else G.W
        t.y = lane.y; t.ox = t.x; t.oy = t.y
        t.fx = t.x.toFloat(); t.fy = t.y.toFloat()
        t.dx = if (fromLeft) lane.step else -lane.step
        t.speed = lane.step / (lane.ms * k)
        t.hop = lane.hop
        t.phase = Phase.RUNNING
        t.stepMs = (lane.ms * k).toInt()
        live += t
    }

    private fun layBottles() {
        live.removeAll { it.spec.role == Role.SIT && it.phase == Phase.OUT }
        bottlesLaid = stage.spots.size
        bottlesHit = 0
        stage.spots.forEachIndexed { i, (spec, x, y) ->
            live += Target(spec, null, -1, 0).apply {
                this.x = x; this.y = y; ox = x; oy = y; phase = Phase.OUT; rise = 1f; forever = true; frame = i
            }
        }
        ev.fx(Fx.Clink)
    }

    private fun step(t: Target, ms: Int) {
        t.ms += ms
        t.seen += ms
        when (t.phase) {
            Phase.OPENING -> {
                t.cover = min(1f, t.ms.toFloat() / OPEN_MS)
                if (t.ms >= OPEN_MS) { t.ms = 0; t.cover = 1f; t.phase = Phase.RISING }
            }
            Phase.RISING -> {
                t.rise = min(1f, t.ms.toFloat() / RISE_MS)
                place(t)
                if (t.ms >= RISE_MS) { t.ms = 0; t.rise = 1f; t.phase = Phase.OUT; t.seen = 0; place(t) }
            }
            Phase.OUT -> {
                if (t.spec.frames > 1) {
                    t.beat += ms
                    if (t.beat >= WAVE_MS) { t.beat = 0; t.frame++ }
                }
                if (t.forever) {
                    if (t.spec.plays) {
                        // The piano player's knocked off his stool for as long as his melody rests, less a moment
                        // sitting back on it; then he plays again.
                        if (t.pose == Pose.HIT && t.ms >= SILENCE_MS - BACK_UP_MS) t.pose = Pose.BARE
                        else if (t.pose == Pose.BARE && t.ms >= SILENCE_MS) t.pose = Pose.STAND
                    } else if (t.pose == Pose.HIT && t.ms >= HIT_MS * 2) t.pose = Pose.STAND
                    return
                }
                if (t.ms >= t.stay - t.aim) {
                    t.ms = 0
                    when {
                        t.throws -> { throwDynamite(t); t.pose = Pose.AIM; t.phase = Phase.FIRED }
                        t.spec.role == Role.FOE -> { t.phase = Phase.AIMING; t.pose = Pose.AIM; ev.fx(Fx.Cock) }
                        else -> sink(t)
                    }
                }
            }
            Phase.AIMING -> if (t.ms >= t.aim) {
                t.ms = 0
                t.phase = Phase.FIRED
                t.pose = Pose.FIRE
                ev.fired(t)
                hurt(t.x + t.spec.w / 2, t.y + 18, "BANG!")
            }
            Phase.FIRED -> if (t.ms >= FIRED_MS) sink(t)
            Phase.STUNNED -> if (t.ms >= HIT_MS) sink(t)
            Phase.SINKING -> {
                t.rise = max(0f, t.sinkFrom * (1f - t.ms.toFloat() / SINK_MS))
                place(t)
                if (t.ms >= SINK_MS) {
                    t.rise = 0f
                    if (t.slot?.opens == true) { t.ms = 0; t.phase = Phase.CLOSING } else leave(t)
                }
            }
            Phase.CLOSING -> {
                t.cover = max(0f, 1f - t.ms.toFloat() / OPEN_MS)
                if (t.ms >= OPEN_MS) leave(t)
            }
            Phase.FLOATING -> {
                t.age += ms
                t.fy -= t.speed * ms
                t.fx = t.ox + SWAY * sin(t.age * 2 * PI / SWAY_MS).toFloat()
                t.x = t.fx.roundToInt(); t.y = t.fy.roundToInt()
                if (t.y + t.spec.h < G.TOP) gone(t)
            }
            Phase.RUNNING -> {
                t.age += ms
                t.fx += (if (t.dx > 0) t.speed else -t.speed) * ms
                t.frame = (abs(t.fx - t.ox) / max(1, abs(t.dx))).toInt()
                val bounce = if (t.hop > 0) abs(sin(t.age * PI / HOP_MS)).toFloat() * t.hop else 0f
                t.x = t.fx.roundToInt(); t.y = (t.fy - bounce).roundToInt()
                if (t.x > G.W || t.x + t.spec.w < 0) gone(t)
            }
            Phase.LOBBED -> {
                t.age += ms
                val p = min(1f, t.age.toFloat() / (LOB_STEPS * LOB_MS * k))
                t.frame = (t.age / LOB_MS).toInt()
                t.x = t.ox + ((t.tx - t.ox) * p).roundToInt()
                t.y = t.oy + ((t.ty - t.oy) * p).roundToInt() - (LOB_ARC * sin(PI * p)).roundToInt()
                if (p >= 1f) {
                    gone(t)
                    ev.smashed(t)
                    ev.fx(Fx.Boom)
                    hurt(t.x + t.spec.w / 2, t.y, "KABOOM!")
                }
            }
            Phase.GONE -> Unit
        }
    }

    private fun gone(t: Target) { t.phase = Phase.GONE }

    private fun leave(t: Target) {
        t.phase = Phase.GONE
        if (t.spec.role == Role.FOE || gentle && t.spec.role == Role.SIGN) foesLeft = max(0, foesLeft - 1)
    }

    private fun sink(t: Target) {
        t.ms = 0
        t.phase = Phase.SINKING
        t.seen = 0
        // From wherever it is: one hit on its way out, or on its way back in, doesn't pop up to full height first.
        t.sinkFrom = t.rise
    }

    /** Puts a target where its slot shows it, [Target.rise] of the way out: a quick start, easing in at the end. */
    internal fun place(t: Target) {
        val s = t.slot ?: return
        val across = s.from == Side.LEFT || s.from == Side.RIGHT
        val r = t.rise
        val eased = 1f - (1f - r) * (1f - r)
        val shown = ((if (across) s.reach else min(t.spec.h, s.reach)) * eased).roundToInt()
        when (s.from) {
            Side.UP -> { t.x = s.x + (s.w - t.spec.w) / 2; t.y = s.y + s.h - shown }
            Side.DOWN -> { t.x = s.x + (s.w - t.spec.w) / 2; t.y = s.y - t.spec.h + shown }
            Side.LEFT -> { t.x = s.x - t.spec.w + shown; t.y = s.y + s.h - t.spec.h }
            Side.RIGHT -> { t.x = s.x + s.w - shown; t.y = s.y + s.h - t.spec.h }
        }
    }

    private fun throwDynamite(t: Target) {
        val spec = stage.dynamite ?: return
        val d = Target(spec, null, -1, 0)
        d.x = t.x + t.spec.w / 2; d.y = t.y + 12; d.ox = d.x; d.oy = d.y
        d.tx = G.W / 2 + rnd.nextInt(-50, 50); d.ty = G.BAR - spec.h - 4
        d.phase = Phase.LOBBED
        live += d
        ev.fx(Fx.Fuse)
    }

    private fun hurt(x: Int, y: Int, word: String) {
        if (state != State.PLAYING) return
        hearts--
        streak = 0
        burst(x, y)
        say(x, y - 16, word)
        ev.fx(Fx.Ouch)
        ev.buzz()
        ev.hurt(x, y)
        if (hearts <= 0) { state = State.LOST; ev.finished() }
    }

    // ---- The player. ----

    fun shoot(x: Int, y: Int) {
        if (state != State.PLAYING || reloading > 0) return
        if (bullets == 0) { ev.fx(Fx.Click); ev.empty(); return }
        bullets--
        shots++
        ev.fx(Fx.Shot)
        ev.shot(bullets)
        val box = stage.tnt
        if (box != null && tntReady && x in box.x until box.x + box.w && y in box.y until box.y + box.h) { blowTnt(box); return }
        val t = pick(x, y)
        if (t != null) { struck(t, x, y); return }
        // Nobody there: perhaps something that was standing behind them.
        val p = props.firstOrNull { it.takes(x, y) }
        if (p != null) { strike(p, x, y); return }
        streak = 0
        puff(x, y)
        ev.missed(x, y)
        ev.fx(Fx.Ricochet)
    }

    /**
     * A piece of the scenery is shot. One worth points is a hit like any other. One that's only for fun (the till)
     * takes the bullet and nothing else: it isn't held against anyone's aim, and a streak goes on as it was.
     */
    private fun strike(p: PropState, x: Int, y: Int) {
        val prop = p.prop
        p.times++
        p.since = 0
        p.hitX = x; p.hitY = y
        p.rolled = p.roll
        p.roll = (p.roll * 31 + clock.toInt() * 7 + x * 13 + y) and 0xFFFFFF
        if (!prop.again) { p.shot = true; p.gone = prop.back }
        burst(x, y)
        val cx = prop.box.x + prop.box.w / 2; val top = prop.box.y - WORD_H - 2
        if (prop.points > 0) say(cx, top, "${prop.word} +${award(prop.points)}".trim())
        else {
            shots--
            if (prop.word.isNotEmpty()) say(cx, top, prop.word)
        }
        ev.prop(p, x, y)
    }

    fun reload() {
        if (state != State.PLAYING || reloading > 0 || bullets == BULLETS) return
        reloading = reloadMs
        ev.fx(Fx.Reload)
    }

    /**
     * The crate goes up: the outlaw hiding behind it (or else whichever is out nearest) is knocked into the shell by the
     * blast and counts as shot, for a bonus; the crate's gone a while, then somebody pushes another in.
     */
    private fun blowTnt(box: Box) {
        tntReady = false
        tntGone = TNT_BACK_MS
        val cx = box.x + box.w / 2; val cy = box.y + box.h / 2
        ev.blew(box)
        ev.fx(Fx.Boom)
        ev.buzz()
        val victim = live.filter {
            (it.spec.role == Role.FOE || gentle && it.spec.role == Role.SIGN) && it.pose != Pose.HIT && it.pose != Pose.BARE &&
                it.phase in BLASTABLE
        }.minByOrNull { if (it.slotIndex == stage.tntSlot) -1 else abs(it.x + it.spec.w / 2 - cx) + abs(it.y + it.spec.h / 2 - cy) }
        if (victim == null) { streak = 0; say(cx, box.y - WORD_UP, "BOOM!"); return }
        foesHit++
        val got = award(victim.spec.points + TNT_BONUS)
        victim.pose = Pose.HIT
        victim.phase = Phase.STUNNED
        victim.ms = 0
        ev.knocked(victim, victim.x + victim.spec.w / 2, victim.y + 4, false)
        say(cx, box.y - WORD_UP, "KA-BOOM! +$got")
    }

    /** The top target under a finger. */
    private fun pick(x: Int, y: Int): Target? {
        var best: Target? = null
        var bestLayer = -1
        var bestDist = Int.MAX_VALUE
        for (t in live) {
            if (!t.hittable(x, y)) continue
            val d = abs(t.x + t.spec.w / 2 - x) + abs(t.y + t.spec.h / 2 - y)
            if (t.layer > bestLayer || (t.layer == bestLayer && d < bestDist)) { best = t; bestLayer = t.layer; bestDist = d }
        }
        return best
    }

    /**
     * Whether this hit lands on what the target carries (an outlaw's shell) and blows it to bits, which scores double.
     * Only a shot on the shell does it, and only when enough of the shell is in view to be aimed at and seen going.
     */
    private fun pops(t: Target, x: Int, y: Int): Boolean {
        val box = t.spec.shell ?: return false
        val s = t.slot ?: return false
        val inView = when (s.from) {
            Side.UP -> min(t.spec.h, s.reach) >= box.y + POP_PEEK
            Side.DOWN -> s.reach >= t.spec.h
            else -> true
        }
        return inView && x >= t.x + box.x - 1 && x <= t.x + box.x + box.w && y >= t.y + box.y - 1 && y <= t.y + box.y + box.h
    }

    private fun struck(t: Target, x: Int, y: Int) {
        val spec = t.spec
        val pop = spec.role == Role.FOE && pops(t, x, y)
        val shell = spec.shell; val slot = t.slot
        if (pop && shell != null && slot != null) burst(t.x + shell.x + shell.w / 2, min(t.y + shell.y + shell.h / 2, slot.y + slot.h - 3))
        else burst(x, y)
        when (spec.role) {
            Role.FRIEND -> if (spec.dear) {
                // Belle: she ducks into her shell with such a fright that it's all over.
                friendsHit++
                streak = 0
                t.pose = Pose.HIT
                t.ms = 0
                if (!t.forever) t.phase = Phase.STUNNED
                ev.knocked(t, x, y, false)
                say(x, y - WORD_UP, "OH NO!")
                ev.fx(Fx.Oops)
                ev.buzz()
                lostDear = true
                state = State.LOST
                ev.finished()
                return
            } else {
                hearts--
                friendsHit++
                streak = 0
                score = max(0, score - FRIEND_FINE)
                t.pose = Pose.HIT
                t.ms = 0
                if (!t.forever) t.phase = Phase.STUNNED else if (spec.plays) ev.silence(SILENCE_MS)
                ev.knocked(t, x, y, false)
                say(x, y - WORD_UP, FRIEND_WORDS.random(rnd))
                ev.fx(Fx.Oops)
                ev.buzz()
                if (hearts <= 0) { state = State.LOST; ev.finished() }
                return
            }
            Role.FOE -> {
                foesHit++
                val quick = t.phase == Phase.RISING || (t.phase == Phase.OUT && t.seen < QUICK_MS)
                val got = award((if (pop) spec.points * SHELL_TIMES else spec.points) + if (quick) QUICK_BONUS else 0)
                t.pose = if (pop) Pose.BARE else Pose.HIT
                t.phase = Phase.STUNNED
                t.ms = 0
                if (pop) shellsPopped++
                ev.knocked(t, x, y, pop)
                // The word comes off the stage's own dice either way, so a shell going doesn't change what follows.
                val word = if (quick) "QUICK DRAW!" else FOE_WORDS.random(rnd).let { if (pop) POP_WORD else it }
                // Over the hat, not over the face of someone who has just lost a shell.
                if (pop) say(t.x + spec.w / 2, max(t.y, t.slot?.y ?: t.y) - WORD_H - 1, "$word +$got")
                else say(x, y - WORD_UP, "$word +$got")
                ev.fx(if (pop) Fx.Crack else Fx.Bonk)
            }
            Role.SIGN -> {
                if (gentle) foesHit++
                val bull = abs(x - (t.x + spec.w / 2)) <= BULL && abs(y - (t.y + BULL_Y)) <= BULL
                val got = award(if (bull) spec.points * 3 else spec.points)
                t.pose = Pose.HIT
                t.phase = Phase.STUNNED
                t.ms = 0
                ev.knocked(t, x, y, bull)
                say(x, y - WORD_UP, if (bull) "BULLSEYE! +$got" else "+$got")
                ev.fx(Fx.Ting)
            }
            Role.SIT -> {
                val got = award(spec.points)
                t.phase = Phase.GONE
                ev.smashed(t)
                ev.fx(Fx.Clink)
                bottlesHit++
                if (bottlesHit == bottlesLaid) {
                    score += BOTTLE_BONUS
                    say(G.W / 2, t.y - 14, "CLEAN SWEEP! +${got + BOTTLE_BONUS}")
                    ev.fx(Fx.Star)
                } else say(t.x + spec.w / 2, t.y - 14, "+$got")
            }
            Role.STAR -> {
                val got = award(spec.points)
                t.phase = Phase.GONE
                ev.smashed(t)
                if (hearts < START_HEARTS) { hearts++; ev.gotHat() }
                say(x, y - WORD_UP, "EXTRA HAT! +$got")
                ev.fx(Fx.Star)
            }
            else -> {
                val got = award(spec.points)
                t.phase = Phase.GONE
                ev.smashed(t)
                say(t.x + spec.w / 2, t.y - 14, "+$got")
                ev.fx(spec.sound ?: when (spec.role) { Role.FLOAT -> Fx.Burst; Role.RUN -> Fx.Clink; else -> Fx.Bonk })
            }
        }
    }

    /** Scores a hit and says what it was worth, which the caller shows with its word, in the one box. */
    private fun award(points: Int): Int {
        hits++
        streak++
        bestStreak = max(bestStreak, streak)
        val got = points * multiplier
        score += got
        return got
    }

    private fun burst(x: Int, y: Int) = flash(Flash.Kind.BURST, x - 12, y - 12, 24, 24, BURST_MS)
    private fun puff(x: Int, y: Int) = flash(Flash.Kind.PUFF, x - 5, y - 5, 11, 11, PUFF_MS)

    /**
     * Puts [word] in a box centred on [x], as near height [y] as it can go without covering another word. If two hits
     * come so close there's nowhere left, the older word gives way.
     */
    private fun say(x: Int, y: Int, word: String) {
        val w = West.cells(word) + WORD_PAD
        val l = (x - w / 2).coerceIn(0, G.W - w)
        fun clash(top: Int) = flashes.filter {
            it.kind == Flash.Kind.WORD && l < it.x + it.w && it.x < l + w && top < it.y + it.h && it.y < top + WORD_H
        }
        val low = G.TOP; val high = G.BAR - WORD_H
        val want = y.coerceIn(low, high)
        val free = (0..6).flatMap { n -> listOf(want - n * (WORD_H + 1), want + n * (WORD_H + 1)) }
            .firstOrNull { it in low..high && clash(it).isEmpty() }
        val top = free ?: want
        for (f in clash(top)) flashes.remove(f)
        flash(Flash.Kind.WORD, l, top, w, WORD_H, WORD_MS, word)
    }

    private fun flash(kind: Flash.Kind, x: Int, y: Int, w: Int, h: Int, ms: Int, word: String = "") {
        val fx = x.coerceIn(0, G.W - w)
        val fy = y.coerceIn(G.TOP, G.BAR - h)
        flashes += Flash(kind, fx, fy, w, h, ms, word)
    }

    enum class State { PLAYING, WON, LOST }

    companion object {
        const val START_HEARTS = 5
        /** What three stars, two and one make you, for the results page. */
        val RANKS = listOf("", "Deputy", "Sheriff", "Marshal")
        const val BULLETS = 6
        /** How long a blown crate of TNT is gone before somebody brings another, and how long the pushing takes. */
        const val TNT_BACK_MS = 9000
        const val TNT_PUSH_MS = 1600
        /** Out where the blast can reach: coming up, out, taking aim or having just fired. */
        private val BLASTABLE = setOf(Phase.RISING, Phase.OUT, Phase.AIMING, Phase.FIRED)
        /** On top of the outlaw's own points, for getting him with the TNT. */
        const val TNT_BONUS = 200
        /** Long enough that nothing is still waiting on it. */
        private const val FOREVER = 1_000_000
        const val STREAK_STEP = 5
        const val MAX_MULT = 4

        private const val WAVE_REST = 900
        /** A shutter or door takes this long to swing open, and a target this long to come all the way out. */
        const val OPEN_MS = 140
        const val RISE_MS = 190
        const val SINK_MS = 200
        const val HIT_MS = 560
        private const val FIRED_MS = 480
        private const val MIN_AIM = 520
        private const val RELOAD_MS = 420
        private const val QUICK_MS = 420
        private const val QUICK_BONUS = 50
        private const val FRIEND_FINE = 150
        private const val BOTTLE_BONUS = 300
        private const val SILENCE_MS = 3000
        /** The last of that, he's back on his stool, not yet playing. */
        private const val BACK_UP_MS = 800
        private const val BULL = 4
        private const val BULL_Y = 11

        /** Balloons rise this far in this long, swaying [SWAY] each side once every [SWAY_MS]. */
        private const val FLOAT_MS = 120f
        private const val FLOAT_STEP = 7f
        private const val SWAY = 2f
        private const val SWAY_MS = 960.0
        /** A tumbleweed's bounce takes this long. */
        private const val HOP_MS = 260.0
        private const val LOB_MS = 140
        private const val LOB_STEPS = 10
        private const val LOB_ARC = 46

        private const val WAVE_MS = 300
        /** A shot on an outlaw's shell scores this many times a shot on the hat. */
        const val SHELL_TIMES = 2
        /** What each star asks for: accuracy in percent, and hats left at the end. */
        const val TWO_STAR_AIM = 60
        const val TWO_STAR_HATS = 2
        const val THREE_STAR_AIM = 80
        const val THREE_STAR_HATS = 4
        /** How many rows of a shell have to be in view for it to go off. */
        private const val POP_PEEK = 5
        const val POP_WORD = "MY SHELL!"
        const val BURST_MS = 170
        const val PUFF_MS = 260
        const val WORD_MS = 900
        /** A word's box is this tall, and this much wider than its letters. */
        const val WORD_H = 13
        const val WORD_PAD = 5
        /** How far above the shot a word goes. */
        private const val WORD_UP = 22

        val FOE_WORDS = listOf("YOWCH!", "OOF!", "MY HAT!", "SHUCKS!", "YIPES!", "WHOA NELLY!", "NOT SO FAST!")
        val FRIEND_WORDS = listOf("HEY!", "MERCY!", "NOT ME!", "SAKES ALIVE!", "WATCH IT!", "SLOW DOWN!")

        fun foesIn(event: Char) = when (event) { 'F', 'd' -> 1; 'L' -> 2; else -> 0 }

        /** "Next star: 80% accuracy, no friends hit." */
        fun needs(what: List<String>): String? = if (what.isEmpty()) null else "Next star: " + what.joinToString(", ") + "."
    }
}

/** What a [Round] tells the screen around it. */
interface Events {
    fun fx(fx: Fx)
    fun buzz()
    /** A tap with no bullets left. */
    fun empty()
    fun finished()
    /** The piano player was hit: the music stops a while. */
    fun silence(ms: Int)
    /** Wave [n] is about to start. */
    fun wave(n: Int) {}
    /** A shot went off; [left] bullets are left. */
    fun shot(left: Int) {}
    /** A shot hit nothing at ([x], [y]). */
    fun missed(x: Int, y: Int) {}
    /** [t] was hit at ([x], [y]) and is now in its hit pose; [special] when its shell went, or a bullseye's middle. */
    fun knocked(t: Target, x: Int, y: Int, special: Boolean) {}
    /** [t] was shot to bits or burst, and is gone. */
    fun smashed(t: Target) {}
    /** An outlaw has fired. */
    fun fired(t: Target) {}
    /** You were hit, by a shot or dynamite, from ([x], [y]). */
    fun hurt(x: Int, y: Int) {}
    /** A star balloon gave a hat back. */
    fun gotHat() {}
    /** The crate of TNT, at [box], has gone up. */
    fun blew(box: Box) {}
    /** A piece of the scenery was shot, at ([x], [y]). */
    fun prop(p: PropState, x: Int, y: Int) {}
    /** A piece of the scenery that was shot is whole again. */
    fun mended(p: PropState) {}
}

object NoEvents : Events {
    override fun fx(fx: Fx) = Unit
    override fun buzz() = Unit
    override fun empty() = Unit
    override fun finished() = Unit
    override fun silence(ms: Int) = Unit
}

enum class Phase { OPENING, RISING, OUT, AIMING, FIRED, STUNNED, SINKING, CLOSING, FLOATING, RUNNING, LOBBED, GONE }

/** Something that can be shot, or shouldn't be. */
class Target(val spec: Spec, val slot: Slot?, val slotIndex: Int, val stay: Int) {
    var phase = Phase.OUT
    var pose = Pose.STAND
    var ms = 0
    /** How long since it was fully out, or since it started going down. */
    var seen = 0
    var aim = 0
    /** How far out of its slot: 0 hidden, 1 all the way. */
    var rise = 0f
    /** How far out it was when it started back in. */
    var sinkFrom = 1f
    /** Its slot's shutter or door: 0 shut, 1 open. */
    var cover = 1f
    var x = 0
    var y = 0
    var ox = 0
    var oy = 0
    var fx = 0f
    var fy = 0f
    var tx = 0
    var ty = 0
    var dx = 0
    /** Game pixels a millisecond, for anything that drifts or runs. */
    var speed = 0f
    var hop = 0
    var flip = false
    var frame = 0
    var age = 0L
    var stepMs = 100
    var beat = 0
    var throws = false
    var forever = false

    val layer get() = when (spec.role) {
        Role.FLOAT, Role.STAR, Role.RUN, Role.LOB -> 2
        Role.SIT -> 0
        else -> 1
    }

    /** Still in the middle of something: the next wave waits for these. */
    val busy get() = !forever

    fun hittable(px: Int, py: Int): Boolean {
        if (phase == Phase.GONE || phase == Phase.OPENING || phase == Phase.CLOSING || phase == Phase.STUNNED) return false
        if (pose == Pose.HIT || pose == Pose.BARE) return false
        if (phase == Phase.SINKING && seen > GRACE) return false
        val s = slot
        var l = x; var t = y; var r = x + spec.w; var b = y + spec.h
        if (s != null) {
            l = max(l, s.x); t = max(t, s.y); r = min(r, s.x + s.w); b = min(b, s.y + s.h)
            if (l >= r || t >= b) return false
        }
        val slack = if (spec.role == Role.LOB || spec.role == Role.SIT) SLACK + 2 else SLACK
        return px >= l - slack && px < r + slack && py >= t - slack && py < b + slack
    }

    companion object {
        const val SLACK = 3
        const val GRACE = 120
    }
}

/** How a piece of the scenery that can be shot ([Prop]) stands. */
class PropState(val prop: Prop) {
    /** Shot, and not whole again. */
    var shot = false
    /** How long since it was last shot, once it has been. */
    var since = 0
    /** While above zero, how long until it's whole again. */
    var gone = 0
    /** How many times it's been shot. */
    var times = 0
    /** Where the last shot landed. */
    var hitX = 0
    var hitY = 0
    /** A number that's different after every shot, and the one before it: for whatever a prop shows by chance. */
    var roll = 0
    var rolled = 0

    /** Whether a shot at ([x], [y]) lands on it and does something. */
    fun takes(x: Int, y: Int): Boolean {
        val b = prop.box
        return (prop.again || !shot) && x >= b.x && x < b.x + b.w && y >= b.y && y < b.y + b.h
    }
}

/** A short-lived mark on the scene. */
class Flash(val kind: Kind, val x: Int, val y: Int, val w: Int, val h: Int, var ms: Int, val word: String) {
    enum class Kind { BURST, PUFF, WORD }
    val total = ms
    /** How far through its life, 0 to 1. */
    val age get() = 1f - ms.toFloat() / total
}
