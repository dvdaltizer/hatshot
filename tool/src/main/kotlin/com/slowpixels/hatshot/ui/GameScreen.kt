package com.slowpixels.hatshot.ui

import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slowpixels.hatshot.BuildConfig
import com.slowpixels.hatshot.Hub
import com.slowpixels.hatshot.game.Card
import com.slowpixels.hatshot.game.Duel
import com.slowpixels.hatshot.game.Flip
import com.slowpixels.hatshot.game.Frame
import com.slowpixels.hatshot.game.Fx
import com.slowpixels.hatshot.game.Font
import com.slowpixels.hatshot.game.Hud
import com.slowpixels.hatshot.game.Lettering
import com.slowpixels.hatshot.game.G
import com.slowpixels.hatshot.game.Out
import com.slowpixels.hatshot.game.Page
import com.slowpixels.hatshot.game.Phase
import com.slowpixels.hatshot.game.Role
import com.slowpixels.hatshot.game.Play
import com.slowpixels.hatshot.game.Pose
import com.slowpixels.hatshot.game.Round
import com.slowpixels.hatshot.game.Showdown
import com.slowpixels.hatshot.game.Stage
import com.slowpixels.hatshot.game.Story
import com.slowpixels.hatshot.game.West
import com.slowpixels.hatshot.game.WesternArt
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.audio.DefaultLightAudio
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.LocalHapticsEnabled
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameMillis
import kotlin.math.max
import kotlin.math.min

/**
 * A stage, from its story pages to its results, all on one screen: the story, the card before it starts, the play,
 * the pause, a moment for the last hat to land, and the results. Replay and Next stay here, so the kit's screen stack
 * is just home, the stage list and this.
 *
 * The scene is drawn in game pixels and blown up; the top bar (pause, score) and the bottom bar (bullets) are Light's
 * own type and lines laid over it.
 */
class GameScreen(sealed: SealedLightActivity, private val first: Int, private val story: Boolean) : HatScreen(sealed), Out {
    override val tag = "game"

    private enum class Mode { STORY, READY, PLAYING, PAUSED, ENDING, RESULTS }

    private val mode = mutableStateOf(Mode.READY)
    private var index = first
    private val stage: Stage get() = Hub.world.stages[index]
    private var play: Play? = null
    private var duel: Showdown? = null
    private var begun = false

    private val frame = Frame(G.W, G.H)
    private val screen = Screenful(frame)
    private val storyFrame = Frame(Story.W, STORY_FRAME_H)
    private val storyScreen = Screenful(storyFrame)

    // The story.
    private var cards: List<Card> = emptyList()
    private val card = mutableIntStateOf(0)
    private var flip: Flip? = null
    private var flipMs = 0L
    private var shownFrame = -1
    private var afterStory: () -> Unit = {}

    // The stage.
    private var endIn = 0
    private var quietFor = 0
    /** The score as the top bar shows it, counting up to the real one. The bars are drawn in the picture, in pixel letters. */
    private var scoreShown = 0
    /** Counts the stages set up, so the picture is drawn again for a new one even when the mode hasn't changed. */
    private val setUps = mutableIntStateOf(0)
    private val awake = mutableStateOf(false)
    private val result = mutableStateOf<Result?>(null)
    private var resultAt = 0L
    private var readyAt = 0L
    /** The first game opens with how to play. */
    private val teach = mutableStateOf(false)

    private class Result(
        val won: Boolean, val stars: Int, val best: Boolean, val lines: List<Pair<String, String>>, val next: String?,
        val lostBelle: Boolean, val heading: Frame,
    )

    override fun willShow() {
        super.willShow()
        Hub.holder.attach(this, DefaultLightAudio(sealed), lightContext.filesDir)
        Hub.holder.onLost = { if (mode.value == Mode.PLAYING) pause() }
        awake.value = true
        if (!begun) { begun = true; begin(first, story) }
        Hub.look.value.let { play?.look = it; duel?.look = it }
    }

    override fun willHide() {
        awake.value = false
        if (mode.value == Mode.PLAYING) pause()
        Hub.music(null)
    }

    override fun onAppPause() {
        awake.value = false
        if (mode.value == Mode.PLAYING) pause()
        Hub.music(null)
        super.onAppPause()
    }

    override fun onScreenDestroy() {
        Hub.holder.onLost = null
        Hub.music(null)
        Hub.holder.release(this)
    }

    // ---- Moving between parts. ----

    private fun begin(i: Int, withStory: Boolean) {
        index = i
        val cards = stage.story
        if (withStory && cards.isNotEmpty()) tell(cards) { ready() } else ready()
    }

    private fun tell(cards: List<Card>, then: () -> Unit) {
        this.cards = cards
        afterStory = then
        card.intValue = 0
        openCard()
        Hub.music(null)
        mode.value = Mode.STORY
    }

    private fun openCard() {
        flip = cards[card.intValue].picture()
        flipMs = 0
        shownFrame = -1
    }

    /** The page's words under its picture, in the game's letters, centred; "TAP TO GO ON" at the foot. */
    private fun storyWords(f: Frame) {
        val c = cards.getOrNull(card.intValue) ?: return
        val cx = Story.W / 2
        var y = STORY_TOP + Story.H + 10
        c.head?.let { y = Lettering.heading(f, it, cx, y, Story.W - 24) + 8 }
        Lettering.paragraph(f, c.text, cx, y, Story.W - 32)
        f.westCenter("TAP TO GO ON", cx, STORY_FRAME_H - 24)
    }

    private fun nextCard() {
        if (card.intValue + 1 < cards.size) { card.intValue++; openCard(); Hub.fx(Fx.Menu) } else afterStory()
    }

    private fun ready() {
        val save = Hub.save
        val look = Hub.look.value
        play = null; duel = null
        if (stage.duel) duel = Showdown(save.pace, save.targetsOnly, look, this)
        else play = Play(Hub.world, index, save.pace, save.targetsOnly, look, this)
        scoreShown = 0
        quietFor = 0
        Hub.melody(true)
        result.value = null
        setUps.intValue++
        readyAt = System.currentTimeMillis()
        if (BuildConfig.DEBUG) Log.d("Hatshot", "ready stage=${index + 1} ${stage.title}")
        mode.value = Mode.READY
        Hub.music(null)
        if (!save.taught) { save.taught = true; teach.value = true }
    }

    /** The card before a stage has been up long enough to have been seen: a second tap meant for the story doesn't start it. */
    private fun settled() = System.currentTimeMillis() - readyAt >= READY_MS

    private fun start() {
        duel?.waiting = false
        play?.waiting = false
        mode.value = Mode.PLAYING
        Hub.music(stage.song)
    }

    private fun pause() {
        if (mode.value != Mode.PLAYING) return
        // The score stops counting up where it is, so show what it's counting to.
        scoreShown = play?.round?.score ?: duel?.duel?.score ?: scoreShown
        mode.value = Mode.PAUSED
        Hub.music(null)
        Hub.fx(Fx.Pause)
    }

    private fun resume() {
        mode.value = Mode.PLAYING
        Hub.music(stage.song)
    }

    private fun results() {
        val world = Hub.world
        val p = play; val d = duel
        val lostBelle = p?.round?.lostDear == true
        val wonIt = p?.round?.let { it.state == Round.State.WON } ?: (d?.duel?.beatBoss == true)
        val top = if (lostBelle) 104 else if (wonIt) 32 else 70
        val heading = Frame(G.W, RESULTS_H).apply { look = Hub.look.value; fill() }
        val r = if (p != null) {
            val r = p.round
            val won = r.state == Round.State.WON
            val best = won && Hub.save.record(world.id, stage.key, r.score, r.stars)
            Result(won, r.stars, best, listOf(
                "Score" to Hud.commas(r.score),
                (if (r.gentle) "Targets" else "Outlaws") to "${r.foesHit} of ${r.foesTotal}",
                "Accuracy" to "${r.accuracy}%",
                "Best streak" to "${r.bestStreak}",
            ), r.nextStar, r.lostDear, heading)
        } else {
            val duel = d!!.duel
            val won = duel.beatBoss
            val best = won && Hub.save.record(world.id, stage.key, duel.score, duel.stars)
            Result(won, duel.stars, best, listOf(
                "Score" to Hud.commas(duel.score),
                (if (d.gentle) "Hits" else "Duels won") to "${duel.wins} of ${Duel.NEEDED}",
                "Fastest" to if (duel.bestDraw > 0) "${duel.bestDraw} ms" else "-",
            ), duel.nextStar, false, heading)
        }
        // The heading in the sign-painter's letters, with a picture when it went wrong.
        when {
            r.lostBelle -> {
                heading.westCenter("OH NO!", G.W / 2, 2, dot = 3)
                heading.cutBig(WesternArt.moseySorry, 46, 30, 2)
                heading.cutBig(WesternArt.belleHiding, 122, 30, 2, flip = true)
            }
            r.won -> heading.westCenter(if (stage.duel) "SHOWDOWN WON!" else "YEE-HAW!", G.W / 2, 2, dot = if (stage.duel) 2 else 3)
            else -> {
                // The two words one over the other, and Mosey gone into his shell beside them.
                heading.west("GAME", 22, 4, dot = 3)
                heading.west("OVER", 22, 37, dot = 3)
                heading.cutBig(WesternArt.moseyOut, 142, 3, 2)
            }
        }
        // Under the heading: the stars and rank, the tally, what the next star needs; all centred, in the game's letters.
        var y = top + 8
        val cx = G.W / 2
        if (r.lostBelle) {
            y = Lettering.paragraph(heading, "You hit Belle! She got such a fright, she's hiding in her shell.", cx, y, G.W - 30) + 8
            Lettering.paragraph(heading, "Look before you shoot.", cx, y, G.W - 30)
        } else {
            if (r.won) {
                for (i in 0 until 3) heading.putBig(if (i < r.stars) Lettering.starOn else Lettering.starOff, cx - 33 + i * 24, y, 2)
                y += 20
                heading.westCenter(Round.RANKS[r.stars].uppercase() + if (r.best) " - NEW BEST" else "", cx, y)
                y += West.H + 10
            }
            for ((k, v) in r.lines) { Lettering.row(heading, k, v, y); y += West.H + 7 }
            r.next?.let { Lettering.paragraph(heading, it, cx, y + 5, G.W - 30) }
        }
        result.value = r
        // For the simulator's checks: a debug build says how each stage went.
        if (BuildConfig.DEBUG) Log.d("Hatshot", "results stage=${index + 1} won=${r.won} stars=${r.stars} best=${r.best} belle=${r.lostBelle} ${r.lines}")
        resultAt = System.currentTimeMillis()
        mode.value = Mode.RESULTS
    }

    // ---- What the stage says. ----

    override fun fx(fx: Fx) = Hub.fx(fx)
    override fun buzz(hard: Boolean) = Hub.buzz(hard)
    /** The piano player's been hit: his melody stops, the band plays on, until he's back at the keys. */
    override fun silence(ms: Int) { quietFor = ms; Hub.melody(false) }

    override fun finished() {
        mode.value = Mode.ENDING
        endIn = END_MS
        Hub.music(null)
        val won = play?.round?.state == Round.State.WON || duel?.duel?.beatBoss == true
        Hub.fx(if (won) Fx.Clear else Fx.GameOver)
    }

    // ---- Time and taps. ----

    private fun step(dt: Int) {
        val look = Hub.look.value
        when (mode.value) {
            Mode.STORY -> {
                val f = flip ?: return
                flipMs += dt
                val n = f.loop[((flipMs / f.beat) % f.loop.size).toInt()]
                if (n != shownFrame) {
                    shownFrame = n
                    storyFrame.look = look
                    storyFrame.fill()
                    val page = Page(f.frames[n], look, look.ink)
                    for (y in 0 until Story.H) System.arraycopy(page.px, y * Story.W, storyFrame.px, (y + STORY_TOP) * Story.W, Story.W)
                    storyWords(storyFrame)
                    storyScreen.show()
                }
            }
            Mode.READY -> { play?.idle(dt); draw(look) { readyCard(it) } }
            Mode.PLAYING -> {
                if (Hub.auto != null) bot()
                play?.tick(dt)
                duel?.tick(dt)
                if (quietFor > 0) {
                    quietFor -= dt
                    if (quietFor <= 0) Hub.melody(true)
                }
                draw(look)
            }
            Mode.PAUSED -> draw(look, hidden = true) { pauseMenu(it) }
            Mode.ENDING -> {
                play?.tick(dt)
                duel?.tick(dt)
                draw(look)
                endIn -= dt
                if (endIn <= 0) results()
            }
            Mode.RESULTS -> Unit
        }
        val target = play?.round?.score ?: duel?.duel?.score ?: 0
        scoreShown = if (scoreShown < target) min(target, scoreShown + max(1, (target - scoreShown) / 5)) else target
    }

    /** The simulator's stand-in for a thumb ([Hub.auto]): hats mostly, a shell now and then, and a fair draw. */
    private fun bot() {
        val how = Hub.auto
        if (how == "idle") return
        play?.let { p ->
            val r = p.round
            if (r.bullets == 0) { r.reload(); return }
            // "belle" goes for Belle when she shows; "friends" for any of the good folk.
            if (how == "belle" || how == "friends") {
                val f = r.live.firstOrNull { it.spec.role == Role.FRIEND && (how == "friends" || it.spec.dear) && it.phase == Phase.OUT && (it.forever || it.seen > 300) }
                if (f != null && f.pose != Pose.HIT) { p.tap(f.x + 12, max(f.y, f.slot?.y ?: f.y) + 6); return }
            }
            val t = r.live.firstOrNull { it.spec.role == Role.FOE && it.phase == Phase.OUT && it.seen > 380 } ?: return
            val shell = t.spec.shell
            val sx = t.x + (shell?.x ?: 0) + 8; val sy = t.y + (shell?.y ?: 0) + 6
            if (shell != null && r.foesHit % 3 == 2 && t.hittable(sx, sy) && sy < (t.slot?.let { it.y + it.h } ?: G.BAR)) p.tap(sx, sy)
            else p.tap(t.x + 12, max(t.y, t.slot?.y ?: t.y) + 5)
        }
        duel?.let { if (it.duel.phase == Duel.Phase.DRAW && it.duel.drawnFor > 260) it.shoot() }
    }

    /**
     * The scene as it stands. No fading behind a card or a menu: the screen shows black and white only. The card before
     * a stage is solid; the pause menu is words on the page, so while it's up the scene is [hidden] and only the bars
     * stay (which also means a pause is no way to study who's at the windows).
     */
    private fun draw(look: com.slowpixels.hatshot.game.Look, hidden: Boolean = false, over: ((Frame) -> Unit)? = null) {
        play?.let { it.look = look; it.shownScore = scoreShown; it.draw(frame) }
        duel?.let { it.look = look; it.shownScore = scoreShown; it.draw(frame) }
        if (hidden) frame.box(0, G.TOP, G.W, G.BAR - G.TOP, look.bg)
        over?.invoke(frame)
        screen.show()
    }

    /** The card before a stage, in the game's letters: which stage, its name, what to do, your best; TAP TO START below. */
    private fun readyCard(f: Frame) {
        val world = Hub.world
        val save = Hub.save
        val best = save.best(world.id, stage.key)
        val stars = save.stars(world.id, stage.key)
        val line = when {
            save.targetsOnly && stage.duel -> "Wait for DRAW, then tap the target."
            // Someone who's always there (the piano player, Belle in her cell) is there in targets only too, and still
            // mustn't be hit: the stage's own line says so.
            save.targetsOnly && stage.resident != null -> "Shoot the targets and balloons. ${stage.line}"
            save.targetsOnly -> "Shoot the targets and balloons."
            else -> stage.line
        }
        val hint = if (stage.duel) "The camera button fires too." else "Tap the bar at the foot, or press the camera button, to reload."
        val x = 12; val w = G.W - 2 * x; val cx = G.W / 2; val inner = w - 22
        val h = 14 + Font.CAP + 8 + West.H + 9 + Lettering.height(line, inner) + 7 + Lettering.height(hint, inner) + 10 + 9 + 12
        val y0 = G.TOP + (G.BAR - G.TOP - h) / 2
        Lettering.panel(f, x, y0, w, h)
        var y = y0 + 14
        val which = "STAGE ${index + 1} OF ${world.stages.size}"
        f.font(which, cx - Font.width(which) / 2, y); y += Font.CAP + 8
        y = Lettering.heading(f, stage.title.uppercase(), cx, y, inner, most = 1) + 9
        y = Lettering.paragraph(f, line, cx, y, inner) + 7
        y = Lettering.paragraph(f, hint, cx, y, inner) + 10
        if (best > 0) {
            val b = "BEST ${Hud.commas(best)}"
            val bw = Font.width(b) + 6 + 3 * 7 + 6
            f.font(b, cx - bw / 2, y + 1)
            Lettering.stars(f, stars, cx - bw / 2 + Font.width(b) + 6 + (3 * 7 + 6) / 2, y)
        } else {
            f.font("NOT PLAYED YET", cx - Font.width("NOT PLAYED YET") / 2, y + 1)
        }
        Lettering.button(f, "TAP TO START", cx, G.BAR + (G.H - G.BAR - West.H) / 2 - 1)
    }

    /** What the pause menu offers, top to bottom, and where each word sits. */
    private val pauseChoices = listOf("RESUME", "START OVER", "STAGES")
    private fun pauseY(i: Int) = G.TOP + 82 + i * 36

    /** The pause menu, centred in the middle of the screen: the scene is hidden behind it, the bars stay. */
    private fun pauseMenu(f: Frame) {
        f.westCenter("PAUSED", G.W / 2, G.TOP + 30, dot = 3)
        pauseChoices.forEachIndexed { i, word -> f.westCenter(word, G.W / 2, pauseY(i), dot = 2) }
    }

    private fun down(gx: Int, gy: Int) {
        when (mode.value) {
            Mode.STORY -> nextCard()
            Mode.READY -> if (gy >= G.TOP && settled()) start()
            Mode.PAUSED -> {
                // A word is a band the width of the screen, so a thumb doesn't have to land on the letters.
                val i = pauseChoices.indices.firstOrNull { gy in pauseY(it) - 10 until pauseY(it) + West.H * 2 + 10 }
                when (i) {
                    0 -> resume()
                    1 -> begin(index, false)
                    2 -> goBack()
                }
            }
            Mode.PLAYING -> if (gy >= G.TOP) { play?.tap(gx, gy); duel?.shoot() }
            else -> Unit
        }
    }

    /** The camera button and the wheel's press: reload, or fire at high noon. Volume stays with LightOS. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode !in SIDE_KEYS) return false
        if (event.repeatCount > 0) return true
        when (mode.value) {
            Mode.STORY -> nextCard()
            Mode.READY -> if (settled()) start()
            Mode.PLAYING -> { play?.round?.reload(); duel?.shoot() }
            else -> return false
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent) = keyCode in SIDE_KEYS && mode.value != Mode.RESULTS

    // ---- Drawing. ----

    @Composable
    override fun Body() {
        LaunchedEffect(this) {
            var last = -1L
            var still = 0
            while (isActive) {
                // Nothing moves on the results page or behind the pause menu: draw it once, then leave the screen alone.
                val resting = mode.value == Mode.RESULTS || mode.value == Mode.PAUSED ||
                    (mode.value == Mode.READY && play?.restless != true)
                if (resting && still >= 2) {
                    // Until something changes: the mode, or a new stage set up behind the same card.
                    snapshotFlow { mode.value to setUps.intValue }.drop(1).first()
                    last = -1L; still = 0
                    continue
                }
                still = if (resting) still + 1 else 0
                withFrameMillis { now ->
                    val dt = if (last < 0) 16 else (now - last).coerceIn(0, MAX_STEP).toInt()
                    last = now
                    step(dt)
                }
            }
        }
        LaunchedEffect(this, teach.value) {
            if (teach.value) { teach.value = false; navigateTo(::HelpScreen) }
        }
        @Suppress("UNUSED_VARIABLE") val look by Hub.look.collectAsState()
        Hub.haptics = LocalHapticsEnabled.current
        val m = mode.value
        // The screen is held on only while a stage is being played (a duel's long wait for DRAW included): a story page
        // or the card before a stage left lying sleeps like any other screen.
        val keep = awake.value && (m == Mode.PLAYING || m == Mode.ENDING)
        Themed {
            Box(Modifier.fillMaxSize().then(if (keep) Modifier.keepScreenOn() else Modifier)) {
                when (m) {
                    Mode.STORY -> StoryPage()
                    Mode.RESULTS -> result.value?.let { Results(it) }
                    else -> Stage(m)
                }
            }
        }
    }

    @Composable
    private fun StoryPage() {
        val c = cards.getOrNull(card.intValue) ?: return
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val px = with(density) { maxWidth.toPx() } / Story.W
            fun sp(n: Int) = with(density) { (n * px).toDp() }
            @Suppress("UNUSED_EXPRESSION") c
            Pixels(storyScreen, onDown = { _, _ -> nextCard() })
            LightTopBar(
                leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack() }),
                rightButton = LightBarButton.Text("SKIP", onClick = { afterStory() }),
            )
        }
    }

    private val goHome: () -> Unit = { Hub.closeUntil = "home"; goBack() }

    @Composable
    private fun Stage(m: Mode) {
        BoxWithConstraints(Modifier.fillMaxSize().then(
            if (m == Mode.PLAYING) Modifier.systemGestureExclusion { Rect(0f, it.size.height * 0.22f, it.size.width.toFloat(), it.size.height * 0.72f) }
            else Modifier,
        )) {
            val density = LocalDensity.current
            val px = with(density) { maxWidth.toPx() } / G.W
            fun gp(n: Int) = with(density) { (n * px).toDp() }
            Pixels(screen, onDown = ::down)
            // The top bar holds only LightOS's own button: pause (or back, before the start). The score, the hats, the
            // bullets and what's left are in the picture, in the game's pixel letters (Hud).
            LightTopBar(
                leftButton = if (m == Mode.READY) LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack() })
                else LightBarButton.LightIcon(LightIcons.PAUSE, onClick = { pause() }, contentDescription = "Pause"),
            )

        }
    }

    @Composable
    private fun Results(r: Result) {
        val world = Hub.world
        val last = index == world.stages.size - 1
        val heading = remember(r) { Screenful(r.heading).also { it.show() } }
        fun settled(go: () -> Unit): () -> Unit = { if (System.currentTimeMillis() - resultAt >= SETTLE_MS) go() }
        Column(Modifier.fillMaxSize()) {
            LightTopBar(
                leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack() }),
                center = LightTopBarCenter.Text(stage.title),
            )
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val density = LocalDensity.current
                val px = with(density) { maxWidth.toPx() } / G.W
                Box(Modifier.fillMaxWidth().height(with(density) { (r.heading.h * px).toDp() })) { Pixels(heading) }
            }
            LightBottomBar(
                if (r.won) listOf(
                    LightBarButton.LightIcon(LightIcons.REFRESH, onClick = settled { begin(index, false) }, contentDescription = "Replay"),
                    LightBarButton.Text("NEXT", onClick = settled { if (last) tell(world.ending, goHome) else begin(index + 1, true) }),
                    LightBarButton.LightIcon(LightIcons.LIST, onClick = settled { goBack() }, contentDescription = "Stages"),
                ) else listOf(
                    LightBarButton.LightIcon(LightIcons.LIST, onClick = settled { goBack() }, contentDescription = "Stages"),
                    LightBarButton.Text("RETRY", onClick = settled { begin(index, false) }),
                    null,
                ),
            )
        }
    }

    private companion object {
        /** Where a story page's picture starts, and how tall its frame is, in story pixels (4 screen pixels each). */
        const val STORY_TOP = 30
        const val STORY_FRAME_H = 310
        /** How long the last hat has to land before the results. */
        const val END_MS = 1200
        /** A last excited tap from the stage shouldn't press a button nobody has seen yet. */
        const val SETTLE_MS = 700L
        /** And the same for the card before a stage, after the story's last page. */
        const val READY_MS = 350L
        /** The most game time one picture may stand for: a stall slows the game down, it doesn't make it jump. */
        const val MAX_STEP = 80L
        val SIDE_KEYS = setOf(KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_FOCUS, 319)
        /** The results page's picture, in game pixels: the heading, then the tally under it. */
        const val RESULTS_H = 186
    }
}
