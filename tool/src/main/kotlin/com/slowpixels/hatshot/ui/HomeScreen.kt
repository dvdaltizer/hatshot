package com.slowpixels.hatshot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.slowpixels.hatshot.Hub
import com.slowpixels.hatshot.game.Frame
import com.slowpixels.hatshot.game.G
import com.slowpixels.hatshot.game.Look
import com.slowpixels.hatshot.game.Page
import com.slowpixels.hatshot.game.Pose
import com.slowpixels.hatshot.game.WesternArt
import com.slowpixels.hatshot.game.WesternScenes
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * The first page: the town sign, a signpost with three boards (PLAY, HOW TO PLAY, SETTINGS), Mosey at its foot and
 * an outlaw at the other side. The boards are Light's own line and spaced capitals; the rest is the game's 8-bit
 * pictures. Now and then a tumbleweed rolls by.
 */
@InitialScreen
class HomeScreen(sealed: SealedLightActivity) : HatScreen(sealed) {
    override val tag = "home"
    private val frame = Frame(G.W, G.H)
    private val screen = Screenful(frame)
    private var sign: Page? = null
    private var ground: Page? = null
    private var made: Look? = null

    private fun draw(look: Look, ms: Long) {
        if (made !== look) {
            sign = Page(WesternScenes.title(), look, look.ink)
            ground = Page(WesternScenes.titleGround(), look)
            made = look
        }
        frame.look = look
        frame.page(ground!!)
        frame.clipped(0, 0, G.W, G.H) {
            val s = sign!!
            for (i in s.px.indices) if (s.px[i] != look.bg) frame.px[i] = s.px[i]
        }
        // Mosey blinks now and then; the outlaw just glowers.
        val blink = (ms / 120) % 32 == 0L
        frame.cut(if (blink) WesternArt.moseyBlink else WesternArt.snail("mosey", Pose.STAND), 10, 182)
        frame.cut(WesternArt.snail("beard", Pose.STAND), 182, 182, flip = true)
        // A tumbleweed every so often, bouncing along the ground.
        val t = ms % 9000
        if (t < 4200) {
            val x = G.W - (t * (G.W + 20) / 4200).toInt()
            val hop = (abs(sin(t / 160.0)) * 5).roundToInt()
            frame.put(WesternArt.tumbleweed[((t / 90) % 4).toInt()], x, 206 - hop)
        }
        screen.show()
    }

    @Composable
    override fun Body() {
        val look by Hub.look.collectAsState()
        LaunchedEffect(this, look) {
            // On the screen's own clock, which stops when the tool isn't showing: twenty pictures a second is plenty for
            // a blink and a tumbleweed.
            var start = -1L
            var drawn = -1000L
            while (isActive) {
                withFrameMillis { now ->
                    if (start < 0) start = now
                    if (now - drawn >= 50) { drawn = now; draw(look, now - start) }
                }
            }
        }
        Themed {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val px = with(density) { maxWidth.toPx() } / G.W
                fun gp(n: Int) = with(density) { (n * px).toDp() }
                Pixels(screen)
                val labels = listOf("PLAY", "HOW TO PLAY", "SETTINGS")
                val go = listOf({ navigateTo(::StagesScreen) }, { navigateTo(::HelpScreen) }, { navigateTo(::SettingsScreen) })
                val ink = LightThemeTokens.colors.content
                val paper = LightThemeTokens.colors.background
                WesternScenes.BOARDS.forEachIndexed { i, (x, y, w, h, right) ->
                    Box(
                        Modifier.offset(gp(x), gp(y)).size(gp(w), gp(h))
                            .lightClickable(onClick = go[i])
                            .semantics { role = Role.Button; contentDescription = labels[i] },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val tip = size.height / 2
                            val p = Path().apply {
                                if (right == 1) {
                                    moveTo(0f, 0f); lineTo(size.width - tip, 0f); lineTo(size.width, size.height / 2)
                                    lineTo(size.width - tip, size.height); lineTo(0f, size.height); close()
                                } else {
                                    moveTo(tip, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height)
                                    lineTo(tip, size.height); lineTo(0f, size.height / 2); close()
                                }
                            }
                            drawPath(p, paper)
                            drawPath(p, ink, style = Stroke(width = px * 0.8f))
                        }
                        LightText(labels[i], if (i == 0) LightTextVariant.Subtitle else LightTextVariant.Button,
                            Modifier.offset(x = gp(if (right == 1) -4 else 4)))
                    }
                }
            }
        }
    }
}
