package com.slowpixels.hatshot

import com.slowpixels.hatshot.audio.Holder
import com.slowpixels.hatshot.audio.Sound
import com.slowpixels.hatshot.game.Fx
import com.slowpixels.hatshot.game.Look
import com.slowpixels.hatshot.game.Song
import com.slowpixels.hatshot.game.Western
import com.slowpixels.hatshot.game.World
import com.slowpixels.hatshot.store.Save
import com.thelightphone.sdk.LightVibrator
import com.thelightphone.sdk.ui.LightThemeController
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

/** What lasts as long as the tool's process: the saved settings and scores, the sound, the page's colours. */
object Hub {
    val world: World = Western
    lateinit var save: Save
        private set
    var sound: Sound? = null
        private set
    private var vibrator: LightVibrator? = null
    val look = MutableStateFlow(Look.DARK)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    /** Pauses a podcast or music on the phone while a stage's music plays. */
    val holder = Holder(scope)

    /**
     * For checking on the simulator only: with a file named "autoplay" in the tool's folder, a debug build plays by
     * itself, so the pages after a stage can be looked at. The file's one word says how: nothing or "win" shoots every
     * outlaw, "idle" shoots nobody, "friends" shoots the good folks, "belle" shoots Belle. A build for the phone never
     * looks for the file.
     */
    var auto: String? = null
        private set

    /** A screen further down asks every list above it to close, up to the one named here. */
    var closeUntil: String? = null

    /** Called by every screen as it shows, so whichever opens first sets things up. */
    fun start(files: File, v: LightVibrator) {
        vibrator = v
        if (::save.isInitialized) return
        save = Save(File(files, "hatshot.properties"))
        auto = if (BuildConfig.DEBUG) File(files, "autoplay").takeIf { it.isFile }?.let { f -> runCatching { f.readText().trim() }.getOrDefault("") } else null
        sound = runCatching { Sound(files, save, Version.CODE) }.getOrNull()
        page()
    }

    /** Puts LightOS's theme and the game's colours on the page chosen in Settings. */
    fun page() {
        if (save.dark) LightThemeController.setDarkTheme() else LightThemeController.setLightTheme()
        look.value = if (save.dark) Look.DARK else Look.LIGHT
    }

    fun fx(f: Fx) { sound?.play(f) }
    /** What to play now (null for silence). With the music switched off nothing plays, and nothing else is paused. */
    fun music(s: Song?) {
        sound?.music(s)
        if (s != null && save.music) holder.take() else holder.drop()
    }

    /** The tune's melody rests or comes back, the rest of it playing on: the piano player's been hit, or he's back. */
    fun melody(on: Boolean) { sound?.melody(on) }

    /** LightOS's own switch for haptics, as the game screen last saw it: off there is off here. */
    @Volatile var haptics = true

    fun buzz(hard: Boolean) {
        if (!save.vibrate || !haptics) return
        runCatching { vibrator?.vibrate(if (hard) 70.milliseconds else 22.milliseconds) }
    }
}
