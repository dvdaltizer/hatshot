package com.slowpixels.hatshot.store

import com.slowpixels.hatshot.game.Pace
import java.io.File
import java.util.Properties

/**
 * Settings, best scores and stars, kept in a file on the phone and nowhere else. Written to a new file and moved over
 * the old one, so a full phone keeps the last good copy.
 */
class Save(private val file: File) {
    private val values = Properties().apply { runCatching { file.inputStream().use { load(it) } } }

    private fun bool(key: String, default: Boolean) = values.getProperty(key)?.toBooleanStrictOrNull() ?: default
    private fun int(key: String, default: Int) = values.getProperty(key)?.toIntOrNull() ?: default

    @Synchronized
    private fun put(vararg pairs: Pair<String, Any>) {
        var changed = false
        for ((k, v) in pairs) if (values.getProperty(k) != v.toString()) { values.setProperty(k, v.toString()); changed = true }
        if (!changed) return
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".new")
            tmp.outputStream().use { values.store(it, null) }
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
    }

    var effects: Boolean
        get() = bool("effects", true)
        set(v) = put("effects" to v)
    var music: Boolean
        get() = bool("music", true)
        set(v) = put("music" to v)
    /** A short buzz on a hit and a longer one when you're shot: on, since the Light Phone's buzz is a quiet one. */
    var vibrate: Boolean
        get() = bool("vibrate", true)
        set(v) = put("vibrate" to v)
    /** For younger players: bullseyes in place of people, and nobody shoots back. */
    var targetsOnly: Boolean
        get() = bool("targetsOnly", false)
        set(v) = put("targetsOnly" to v)
    /** The first game opens with the rules. */
    var taught: Boolean
        get() = bool("taught", false)
        set(v) = put("taught" to v)
    /** The dark page (white on black, as LightOS starts) or the light one (the Kompakt's black on white). */
    var dark: Boolean
        get() = bool("dark", true)
        set(v) = put("dark" to v)

    var pace: Pace
        get() = Pace.entries.getOrElse(int("pace", 1)) { Pace.NORMAL }
        set(v) = put("pace" to v.ordinal)

    fun best(world: String, stage: Int) = int(key("best", world, stage), 0)
    fun stars(world: String, stage: Int) = int(key("stars", world, stage), 0)

    /** Keeps a result if it beats what's there; true for a new best score. */
    fun record(world: String, stage: Int, score: Int, stars: Int): Boolean {
        val better = score > best(world, stage)
        val pairs = ArrayList<Pair<String, Any>>()
        if (better) pairs += key("best", world, stage) to score
        if (stars > stars(world, stage)) pairs += key("stars", world, stage) to stars
        put(*pairs.toTypedArray())
        return better
    }

    private fun key(what: String, world: String, stage: Int) = "${what}_${world.replace(' ', '_')}_$stage"
}
