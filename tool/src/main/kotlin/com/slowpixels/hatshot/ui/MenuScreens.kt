package com.slowpixels.hatshot.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.slowpixels.hatshot.Hub
import com.slowpixels.hatshot.Version
import com.slowpixels.hatshot.game.Frame
import com.slowpixels.hatshot.game.Pace
import com.slowpixels.hatshot.game.Pose
import com.slowpixels.hatshot.game.Sprite
import com.slowpixels.hatshot.game.WesternArt
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.gridUnitsAsDp

/** Sound, music, buzz, pace, the page's colours, and targets only. */
class SettingsScreen(sealed: SealedLightActivity) : HatScreen(sealed) {
    override val tag = "settings"
    private val changed = mutableIntStateOf(0)

    override fun willShow() {
        super.willShow()
        changed.intValue++
    }

    @Composable
    override fun Body() {
        @Suppress("UNUSED_VARIABLE") val n = changed.intValue
        val s = Hub.save
        fun flip(set: () -> Unit) { set(); changed.intValue++ }
        ListPage("Settings", back = { goBack() }) {
            Item("Sound", onOff(s.effects)) { flip { s.effects = !s.effects } }
            Item("Music", onOff(s.music)) { flip { s.music = !s.music; Hub.sound?.poke() } }
            Item("Buzz", onOff(s.vibrate)) { flip { s.vibrate = !s.vibrate; if (s.vibrate) Hub.buzz(false) } }
            Item("Pace", s.pace.label) { navigateTo(::PaceScreen) }
            Item("Page", if (s.dark) "Dark" else "Light") { navigateTo(::PageScreen) }
            Item("Targets only", onOff(s.targetsOnly)) { flip { s.targetsOnly = !s.targetsOnly } }
            Note("Targets only is for younger players: bullseyes come out in place of snails, and nobody shoots back.", lighten = true)
            Item("How to play") { navigateTo(::HelpScreen) }
            Item("About") { navigateTo(::AboutScreen) }
        }
    }
}

/** How long targets stay out. */
class PaceScreen(sealed: SealedLightActivity) : HatScreen(sealed) {
    @Composable
    override fun Body() {
        val s = Hub.save
        ListPage("Pace", back = { goBack() }) {
            for (p in Pace.entries) Choice(p.label, selected = s.pace == p, detail = PACE_DETAIL[p]) { s.pace = p; goBack() }
        }
    }

    private companion object {
        val PACE_DETAIL = mapOf(
            Pace.CALM to "Targets stay out longer",
            Pace.NORMAL to "As the game was made",
            Pace.QUICK to "For showing off",
        )
    }
}

/** The dark page, as LightOS starts, or the light one, the Kompakt's own look. */
class PageScreen(sealed: SealedLightActivity) : HatScreen(sealed) {
    @Composable
    override fun Body() {
        val s = Hub.save
        ListPage("Page", back = { goBack() }) {
            Choice("Dark", selected = s.dark, detail = "A black page") { s.dark = true; Hub.page(); goBack() }
            Choice("Light", selected = !s.dark, detail = "A white page") { s.dark = false; Hub.page(); goBack() }
        }
    }
}

/** The rules, each beside a picture of what it's about. */
class HelpScreen(sealed: SealedLightActivity) : HatScreen(sealed) {
    @Composable
    override fun Body() {
        val outlaw = remember { WesternArt.snail("mustache", Pose.STAND) }
        val friend = remember { WesternArt.snail("sheriff", Pose.STAND) }
        ListPage("How to play", back = { goBack() }) {
            Rule(outlaw.cut(0, 0, 24, 24), "Outlaws wear black. Tap one to shoot its hat off, before it shoots you. Hit its shell for double points.")
            Rule(friend.cut(0, 0, 24, 24), "Good folks wear white. Leave them be.")
            Rule(WesternArt.balloon, "Balloons, bottles, cans and bullseyes are for points. A star balloon gives back a hat.")
            Rule(WesternArt.BULLET, icon = true, text = "Six bullets. Tap the bar at the foot of the screen, or press the camera button, to reload.")
            Rule(WesternArt.LIFE, icon = true, text = "You have five hats. Lose them all and it's game over.")
            Rule(WesternArt.snail("belle", Pose.STAND).cut(0, 0, 24, 24), "Belle is who you came for. Whatever you do, don't hit her.")
            Note("At High Noon, wait for DRAW, then tap, or press the camera button. Too soon is a foul, and so is firing at a word that only looks like DRAW.", lighten = true)
        }
    }
}

/** One rule: a picture in game pixels at the left, the words beside it. */
@Composable
private fun Rule(picture: Sprite, text: String, icon: Boolean = false) {
    val frame = remember(picture) {
        val look = Hub.look.value
        Frame(28, 28).apply {
            this.look = look; fill()
            val x = (28 - picture.w) / 2; val y = (28 - picture.h) / 2
            // Somebody or something from the scene is cut out of paper, as in play; a sign from the bars is in the page's ink, as there.
            if (icon) put(picture, x, y) else cut(picture, x, y)
        }
    }
    val screen = remember(frame) { Screenful(frame).also { it.show() } }
    val density = LocalDensity.current
    Row(Modifier.fillMaxWidth().padding(vertical = 0.4f.gridUnitsAsDp()), verticalAlignment = Alignment.CenterVertically) {
        // Four screen pixels to a game pixel, so the pictures sit small beside the words.
        Box(Modifier.size(with(density) { (28 * 4).toDp() })) { Pixels(screen) }
        Spacer(Modifier.width(0.8f.gridUnitsAsDp()))
        Box(Modifier.weight(1f)) { Note(text) }
    }
}

/** Who made it, and the version. */
class AboutScreen(sealed: SealedLightActivity) : HatScreen(sealed) {
    @Composable
    override fun Body() {
        ListPage("About", back = { goBack() }) {
            Item("Hatshot", Version.NAME)
            Note("A tap-to-shoot gallery in 8-bit, and everyone in it is a snail. Black Shell Hank's gang has carried off Belle, " +
                "and you, Mosey, Dry Gulch's new deputy, go after them. Nothing gory: a snail whose hat is hit is inside its shell " +
                "before the hat comes down.")
            Note("All the pictures, music and sounds are original to this game, except the Rusty Spur's tune: Scott " +
                "Joplin's \"The Entertainer\" (1902), arranged for an 8-bit sound chip. Made with love by David Altizer, " +
                "especially for the Light Phone III: drawn in black and white line work for its screen, and over when " +
                "you put it down.", lighten = true)
            Note("Slow Pixels, 2026. Luke 11:9.", lighten = true)
        }
    }
}
