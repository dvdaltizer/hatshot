package com.slowpixels.hatshot.game

/**
 * The screen grid. The Light Phone's screen is 1080 × 1240 and every game pixel is a 5 × 5 block on it, so the game is
 * 216 × 248: a game pixel is then about the size it was on the Kompakt, and the pictures read as 8-bit.
 */
object G {
    const val PX = 5
    const val W = 216
    const val H = 248
    /** The scene runs from under the top bar down to the bullets. */
    const val TOP = 24
    const val BAR = 218
}

/** Which way a target comes in from. */
enum class Side { UP, DOWN, LEFT, RIGHT }

/**
 * What a target does, whatever it looks like, so a new world only brings new pictures: a foe shoots back, a friend
 * must be left alone, a float drifts up, a runner crosses, a sitter waits to be picked off, a sign just scores, and a
 * lob has to be shot before it lands.
 */
enum class Role { FOE, FRIEND, FLOAT, STAR, RUN, SIT, SIGN, LOB }

/** How a target is showing: standing, taking aim, firing, hit, or hit so that what it carried is blown clean off. */
enum class Pose { STAND, AIM, FIRE, HIT, BARE }

/** A box inside a target's picture, measured from the picture's top-left corner. */
class Box(val x: Int, val y: Int, val w: Int, val h: Int)

/** A kind of target: how it behaves and how big it is. Its pictures come from the [World]. */
class Spec(
    val name: String, val role: Role, val w: Int, val h: Int, val points: Int = 0,
    /** Pictures it keeps moving between while it's out. */
    val frames: Int = 1,
    /**
     * Something it carries that a shot can blow to bits, such as a snail's shell: where that is in its picture. A hit
     * there always does it, and now and then any hit does. It's for show: the score is the same either way.
     */
    val shell: Box? = null,
    /** The box the bits fly in, which is the size of the picture of them. */
    val blast: Box? = null,
    /** Always there and playing the music: hitting them stops it a while (the piano player). */
    val plays: Boolean = false,
    /** The one you've come to save (Belle): hitting her ends the stage there and then. */
    val dear: Boolean = false,
    /**
     * Their picture is drawn just as it comes, not cut out of paper again: it brings its own edges, and whatever is in
     * front of them (Belle behind her bars).
     */
    val plain: Boolean = false,
    /** The sound it makes when it's shot, when that isn't just the sound of its kind: a coin, a stick of dynamite. */
    val sound: Fx? = null,
)

/**
 * A piece of the scenery that does something when it's shot: a sack that spills, a till that rings, a money bag that
 * bursts. What it looks like, before and after, is the world's business.
 */
class Prop(
    val name: String,
    /** Where it is: a shot in this box lands on it, when nobody's in the way. */
    val box: Box,
    /** What shooting it scores, as a hit. 0 for one that's only for fun: it costs a bullet and nothing else. */
    val points: Int = 0,
    /** How long it stays shot before it's whole again; 0 for one that stays as it's left. */
    val back: Int = 0,
    /** Does its turn every time it's shot (a till), not just the once. */
    val again: Boolean = false,
    /** What's said when it's shot, ahead of any points. */
    val word: String = "",
)

/** A window, door or rock edge a target shows through. Nothing is drawn outside this opening. */
class Slot(
    val x: Int, val y: Int, val w: Int, val h: Int,
    val from: Side = Side.UP,
    /** How far out it comes. */
    val reach: Int = h - 2,
    /** Which shutter or door covers it (0 for none); the world draws them. */
    val cover: Int = 0,
    /** Roomy enough for the bullseye. */
    val signs: Boolean = false,
    /** The cover opens before anyone comes out (shutters, doors); false for one that stays put, such as bars. */
    val opens: Boolean = cover != 0,
    /** A way in to somewhere dark (a window, a door, a mine): drawn empty, so whoever's in it stands out. */
    val dark: Boolean = cover != 0,
)

/** A straight line a runner crosses along, in steps. */
class Lane(
    val spec: Spec, val y: Int, val step: Int, val ms: Int, val bothWays: Boolean = true,
    /** When it only goes one way, it comes in from the right. */
    val fromRight: Boolean = false,
    /** How high it bounces as it goes (a tumbleweed), in game pixels; a buzzard's flap is its own. */
    val hop: Int = 0,
)

/**
 * A run of targets. [events] says what comes out, one letter each:
 * F outlaw, f friend, s bullseye, b balloon, B two balloons, * star balloon, d outlaw who throws dynamite,
 * L a line-up (three at once, one of them a friend); any other letter is a lane of the stage.
 */
class Wave(
    val events: String,
    /** Time from one target to the next. */
    val gap: Int,
    /** How long a target stays out; an outlaw's last third is aiming. */
    val stay: Int,
    val most: Int = 3,
    val shuffle: Boolean = true,
    /** Set the bottles up at the start. */
    val bottles: Boolean = false,
    /** Only these slots, for a part of a stage with fewer openings. */
    val only: List<Int>? = null,
)

class Stage(
    /** Saved scores are filed under this, so stages can be put in a new order without mixing them up. */
    val key: Int,
    val title: String,
    val line: String,
    val slots: List<Slot>,
    val foes: List<Spec>,
    val friends: List<Spec>,
    val waves: List<Wave>,
    val song: Song,
    val lineups: List<List<Int>> = emptyList(),
    val spots: List<Triple<Spec, Int, Int>> = emptyList(),
    val floatXs: List<Int> = listOf(22, 66, 110, 150, 178),
    val balloon: Spec? = null,
    val star: Spec? = null,
    val sign: Spec? = null,
    val dynamite: Spec? = null,
    val lanes: Map<Char, Lane> = emptyMap(),
    /**
     * A crate of TNT to shoot (Buzzard Canyon): it goes up and takes an outlaw with it, the one hiding behind it if
     * there is one; a while later somebody pushes another in. The crate's box in game pixels, and the hiding place
     * behind it, which nobody uses while the crate's gone.
     */
    val tnt: Box? = null,
    val tntSlot: Int = -1,
    /** Scenery that can be shot: sacks, a till, money bags. */
    val props: List<Prop> = emptyList(),
    /** Someone who's always there and must be left alone, with where they sit. */
    val resident: Triple<Spec, Int, Int>? = null,
    /** A showdown is played as duels, not targets. */
    val duel: Boolean = false,
    /** The story pages that come before it. */
    val story: List<Card> = emptyList(),
)

/** One page of the story: a picture, and a few words under it. [head] is for a big word instead of small print. */
class Card(val picture: () -> Flip, val text: String, val head: String? = null)

/** A story picture: its frames, drawn at story size ([Story.W] × [Story.H]), and the order they play in. */
class Flip(val loop: IntArray, val frames: List<Pad>, val beat: Int = 340) {
    val count get() = frames.size
}

/** A stage that keeps moving, such as a chase. It paints behind and in front of the targets. */
interface Rolling {
    fun tick(ms: Int)
    /** A new wave is starting. */
    fun wave(n: Int)
    fun under(f: Frame)
    fun over(f: Frame)
    /** A shot went off at ([x], [y]), so whoever's riding along can be seen firing it. */
    fun shot(x: Int, y: Int) {}
    /** Whether this draws whoever's in opening [slot] itself, just now (they're riding something that's moving). */
    fun draws(slot: Int) = false
    /** A look at the round, after each of its ticks: to see who's out, and to say which openings are shut ([Round.shut]). */
    fun see(round: Round) {}
}

/** How long targets stay out. Calm suits a child or a tired thumb; Quick, someone showing off. */
enum class Pace(val label: String, val factor: Float) {
    CALM("Calm", 1.4f), NORMAL("Normal", 1f), QUICK("Quick", 0.72f)
}

/** A place to shoot in: its stages and all its pictures. */
interface World {
    /** Saved scores are filed under this, so it never changes, whatever the world gets called. */
    val id: String
    val name: String
    val stages: List<Stage>
    /** The still picture behind a stage, a whole [G.W] × [G.H] pad. */
    fun background(stage: Stage): Pad
    fun look(spec: Spec, pose: Pose, frame: Int): Sprite
    /** A slot's shutter or door, [open] from 0 (shut) to 1 (open); null when nothing covers it. */
    fun cover(slot: Slot, open: Float): Sprite?
    /** The little picture beside a stage on the list. */
    fun badge(stage: Stage): Sprite
    /** What keeps moving in this stage, if anything does. */
    fun rolling(stage: Stage): Rolling? = null
    /** The pages after the last stage is won. */
    val ending: List<Card> get() = emptyList()
}
