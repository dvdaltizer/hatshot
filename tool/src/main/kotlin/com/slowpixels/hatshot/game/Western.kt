package com.slowpixels.hatshot.game


/**
 * The first world: Dry Gulch, a town of snails with an outlaw problem. Black Shell Hank's gang has taken Belle, and
 * you, Mosey, go after them: the street, the General Store, the saloon, the bank, the canyon, the train, the
 * jailhouse, then high noon.
 */
object Western : World {
    override val id = "Wild West"
    override val name = "Dry Gulch"

    // Where a snail's shell is in its picture, and how far the pieces go when an outlaw's is shot to bits.
    private val SHELL = Box(0, 25, 16, 12)
    private val BLAST = Box(WesternArt.SHARDS_AT.first, WesternArt.SHARDS_AT.second, 40, 30)

    // Who's who. The sizes are their pictures' sizes.
    private val MUSTACHE = Spec("mustache", Role.FOE, 24, 40, 100, shell = SHELL, blast = BLAST)
    private val BANDANA = Spec("bandana", Role.FOE, 24, 40, 100, shell = SHELL, blast = BLAST)
    private val BEARD = Spec("beard", Role.FOE, 24, 40, 100, shell = SHELL, blast = BLAST)
    private val SCOWL = Spec("scowl", Role.FOE, 24, 40, 100, shell = SHELL, blast = BLAST)
    private val SHERIFF = Spec("sheriff", Role.FRIEND, 24, 40)
    private val GRANNY = Spec("granny", Role.FRIEND, 24, 40)
    private val KID = Spec("kid", Role.FRIEND, 24, 40)
    private val BARKEEP = Spec("barkeep", Role.FRIEND, 24, 40)
    private val BANKER = Spec("banker", Role.FRIEND, 24, 40)
    private val BONNET = Spec("bonnet", Role.FRIEND, 24, 40)
    private val BABY = Spec("baby", Role.FRIEND, 24, 27)
    private val PIANIST = Spec("pianist", Role.FRIEND, 42, 38, frames = 2, plays = true)
    private val BELLE = Spec("belle", Role.FRIEND, 24, 40, frames = 2, dear = true)
    /** Belle at the jailhouse window, behind bars: always there, waving, and not to be hit. */
    private val BELLE_CELL = Spec("belle in the cell", Role.FRIEND, WesternArt.CELL_W, WesternArt.CELL_H, frames = 2, dear = true, plain = true)

    private val BALLOON = Spec("balloon", Role.FLOAT, 18, 20, 50)
    private val STAR = Spec("star", Role.STAR, 18, 20, 100)
    private val BOTTLE = Spec("bottle", Role.SIT, 6, 12, 30)
    private val CAN = Spec("can", Role.SIT, 8, 9, 30)
    private val BULLSEYE = Spec("bullseye", Role.SIGN, 24, 32, 50)
    private val WEED = Spec("weed", Role.RUN, 16, 16, 200)
    private val MUG = Spec("mug", Role.RUN, 16, 9, 150)
    private val BUZZARD = Spec("buzzard", Role.RUN, 26, 10, 200)
    private val DYNAMITE = Spec("dynamite", Role.LOB, 8, 18, 150, sound = Fx.Boom)
    private val WHITE_HAT = Spec("white hat", Role.STAR, 24, 10, 100)
    private val WHEEL = Spec("wheel", Role.RUN, 16, 16, 150)
    private val COIN = Spec("coin", Role.RUN, 16, 16, 200, sound = Fx.Coin)
    private val WANTED = Spec("poster", Role.SIT, 12, 15, 30)

    private const val SHUTTERS = 1
    private const val BATWINGS = 2
    private const val CURTAIN = 3
    private const val VAULT = 4
    private const val BLIND = 5
    private const val BARS = 6
    private const val DOOR = 7

    private val allFoes = listOf(MUSTACHE, BANDANA, BEARD, SCOWL)

    private val mainStreet = Stage(
        key = 0,
        story = listOf(
            Card({ Story.MEET }, "You're Mosey, the new deputy. This is Belle, the sweetest snail in the West."),
            Card({ Story.GRABBED }, "Black Shell Hank and his gang slid into town and took her. Only you can save her!"),
        ),
        title = "Dusty Street",
        line = "Outlaws wear black. Folks in white are friends.",
        slots = listOf(
            Slot(10, 24, 24, 20, reach = 19),                          // 0 roof, left
            Slot(182, 24, 24, 20, reach = 19),                         // 1 roof, right
            Slot(14, 50, 26, 36, cover = SHUTTERS),                    // 2 upstairs
            Slot(68, 50, 26, 36, cover = SHUTTERS),                    // 3
            Slot(122, 50, 26, 36, cover = SHUTTERS),                   // 4
            Slot(176, 50, 26, 36, cover = SHUTTERS),                   // 5
            Slot(14, 116, 26, 36, cover = SHUTTERS),                   // 6 downstairs
            Slot(176, 116, 26, 36, cover = SHUTTERS),                  // 7
            Slot(88, 112, 40, 60, from = Side.LEFT, reach = 32, cover = BATWINGS), // 8 the doors
            Slot(4, 157, 30, 31, reach = 30, signs = true),            // 9 behind the barrels
            Slot(182, 159, 30, 31, reach = 30, signs = true),          // 10 behind the trough
        ),
        lineups = listOf(listOf(2, 3, 4), listOf(3, 4, 5), listOf(6, 8, 7), listOf(0, 3, 1), listOf(0, 4, 1)),
        foes = allFoes,
        friends = listOf(SHERIFF, GRANNY, KID, BONNET, BABY),
        balloon = BALLOON, star = STAR, sign = BULLSEYE,
        spots = listOf(
            Triple(BOTTLE, 56, 184), Triple(CAN, 70, 187), Triple(BOTTLE, 92, 184), Triple(CAN, 104, 187), Triple(BOTTLE, 126, 184),
            Triple(CAN, 138, 187), Triple(BOTTLE, 158, 184),
        ),
        waves = listOf(
            Wave("FFbFb", gap = 1900, stay = 2900, most = 2),
            Wave("FfFbFs", gap = 1700, stay = 2600, most = 2, bottles = true),
            Wave("L", gap = 1400, stay = 2600),
            Wave("FfFBFfbF*", gap = 1400, stay = 2300),
            Wave("LL", gap = 2800, stay = 2300),
            Wave("FfFsFBfF", gap = 1200, stay = 2100),
        ),
        song = Songs.TRAIL,
    )

    /**
     * The General Store, where the gang stopped for supplies. Shelves each side of the storeroom's curtain, outlaws
     * looking round them, the counter, a pickle barrel to pop out of, cans on the shelves and a wagon wheel rolling loose.
     */
    private val store = Stage(
        key = 5,
        story = listOf(Card({ Story.STORE }, "The gang stopped at the General Store for supplies on the way out of town. Stop them!")),
        title = "General Store",
        line = "Shoot the cans on the shelves, and the rolling wheel.",
        slots = listOf(
            Slot(90, 66, 36, 47, reach = 46, cover = CURTAIN),         // 0 the storeroom
            Slot(40, 72, 21, 40, from = Side.LEFT, reach = 20),       // 1 round the left shelves
            Slot(155, 72, 21, 40, from = Side.RIGHT, reach = 20),     // 2 round the right shelves
            Slot(8, 114, 26, 26, reach = 24),                          // 3 behind the counter
            Slot(44, 114, 26, 26, reach = 24),                         // 4
            Slot(78, 114, 26, 26, reach = 24),                         // 5
            Slot(97, 156, 24, 30, reach = 28),                         // 6 in the pickle barrel
            Slot(12, 154, 30, 32, reach = 30, signs = true),           // 7 behind the sacks
            Slot(174, 158, 30, 32, reach = 30, signs = true),          // 8 behind the eggs
        ),
        lineups = listOf(listOf(1, 0, 2), listOf(3, 4, 5), listOf(7, 6, 8), listOf(3, 0, 5)),
        foes = allFoes,
        friends = listOf(GRANNY, KID, BONNET, BABY, BARKEEP),
        balloon = BALLOON, star = STAR, sign = BULLSEYE,
        spots = listOf(
            Triple(CAN, 7, 83), Triple(CAN, 24, 83), Triple(CAN, 136, 131), Triple(CAN, 183, 83), Triple(CAN, 200, 83),
        ),
        lanes = mapOf('w' to Lane(WHEEL, 201, 9, 120)),
        // The sacks and the eggs spill, once, for a few points; the till rings every time, for the fun of it.
        props = listOf(
            Prop(WesternProps.OATS, Box(0, 186, WesternProps.SACK_W, WesternProps.SACK_H), points = 20),
            Prop(WesternProps.RICE, Box(27, 186, WesternProps.SACK_W, WesternProps.SACK_H), points = 20),
            Prop(WesternProps.EGGS, Box(166, 190, 46, 28), points = 20),
            Prop(WesternProps.TILL, Box(WesternProps.TILL_X - 2, WesternProps.TILL_Y, 34, 26), again = true),
            // The clock comes off the wall and the goods off the shelves, and none of it comes back.
            Prop(WesternProps.CLOCK, WesternProps.CLOCK_BOX, points = 20),
        ) + WesternProps.shelfGoods(points = 10),
        waves = listOf(
            Wave("FFwFb", gap = 1800, stay = 2800, most = 2),
            Wave("FfFwFs", gap = 1600, stay = 2500, most = 2, bottles = true),
            Wave("L", gap = 1400, stay = 2500),
            Wave("FfFwFBfF*", gap = 1350, stay = 2200),
            Wave("LL", gap = 2700, stay = 2200),
            Wave("FfFsFwFbfF", gap = 1150, stay = 2000),
        ),
        song = Songs.STORE,
    )

    private val saloon = Stage(
        key = 1,
        story = listOf(Card({ Story.BOW }, "Belle's bow, and a shiny trail going into the Rusty Spur. The gang is inside. Go get them!")),
        title = "The Rusty Spur",
        line = "Don't shoot the piano player.",
        slots = listOf(
            Slot(12, 34, 28, 40, cover = SHUTTERS),                    // 0 upstairs rooms
            Slot(66, 34, 28, 40, cover = SHUTTERS),                    // 1
            Slot(122, 34, 28, 40, cover = SHUTTERS),                   // 2
            Slot(176, 34, 28, 40, cover = SHUTTERS),                   // 3
            Slot(10, 116, 28, 34, reach = 32),                         // 4 behind the bar
            Slot(58, 116, 28, 34, reach = 32),                         // 5
            Slot(106, 116, 28, 34, reach = 32),                        // 6
            Slot(154, 116, 28, 34, reach = 32),                        // 7
            Slot(9, 154, 30, 32, reach = 30, signs = true),            // 8 behind the table
            Slot(80, 154, 30, 32, reach = 30, signs = true),           // 9 behind the crates
        ),
        lineups = listOf(listOf(0, 1, 2), listOf(1, 2, 3), listOf(4, 5, 6), listOf(5, 6, 7)),
        foes = allFoes,
        friends = listOf(BARKEEP, BONNET, GRANNY, SHERIFF),
        balloon = BALLOON, star = STAR, sign = BULLSEYE,
        spots = listOf(
            Triple(BOTTLE, 14, 94), Triple(BOTTLE, 40, 94), Triple(BOTTLE, 70, 94), Triple(BOTTLE, 146, 94), Triple(BOTTLE, 170, 94),
            Triple(BOTTLE, 196, 94),
        ),
        lanes = mapOf('m' to Lane(MUG, 141, 9, 120, bothWays = false)),
        resident = Triple(PIANIST, 161, 154),
        // Things to break for a few points: none of them mends.
        props = listOf(
            Prop(WesternProps.MIRROR, WesternProps.MIRROR_BOX, points = 20),
            Prop(WesternProps.KEG, WesternProps.KEG_BOX, points = 20),
            Prop(WesternProps.SPITTOON, WesternProps.SPITTOON_BOX, points = 10),
        ) + WesternProps.GLASS_XS.map { Prop(WesternProps.GLASS, Box(it - 1, 99, 7, 8), points = 10) },
        waves = listOf(
            Wave("FFmFb", gap = 1600, stay = 2500),
            Wave("FfFmFfF", gap = 1400, stay = 2300, bottles = true),
            Wave("L", gap = 1400, stay = 2300),
            Wave("FfFsFmBF*", gap = 1200, stay = 2100),
            Wave("LL", gap = 2400, stay = 2100),
            Wave("FfFmFsFfFF", gap = 1050, stay = 1900),
        ),
        song = Songs.RAG,
    )

    /**
     * The Dry Gulch Bank, next door to the saloon. A teller's window each side with its blind, the vault in the middle,
     * columns to hide behind, the clerk's desk, a strongbox with money bags in front of it, and a gold coin rolling
     * across the floor. They brought dynamite.
     */
    private val bank = Stage(
        key = 6,
        story = listOf(Card({ Story.BANK }, "Out the back of the Rusty Spur and into the bank next door. The gang is after the gold!")),
        title = "Dry Gulch Bank",
        line = "Outlaws in the vault! Don't hit the bank clerk.",
        slots = listOf(
            Slot(9, 58, 28, 44, cover = BLIND, reach = 40),           // 0 the tellers' windows, left
            Slot(47, 58, 28, 44, cover = BLIND, reach = 40),          // 1
            Slot(86, 56, 44, 44, cover = VAULT, reach = 42),           // 2 the vault
            Slot(141, 58, 28, 44, cover = BLIND, reach = 40),         // 3 the tellers' windows, right
            Slot(179, 58, 28, 44, cover = BLIND, reach = 40),         // 4
            Slot(80, 150, 22, 40, from = Side.LEFT, reach = 20),      // 5 round the left column
            Slot(114, 150, 22, 40, from = Side.RIGHT, reach = 20),    // 6 round the right column
            Slot(10, 158, 30, 32, reach = 30, signs = true),           // 7 behind the desk
            Slot(176, 156, 30, 32, reach = 30, signs = true),          // 8 behind the strongbox
        ),
        lineups = listOf(listOf(0, 2, 4), listOf(1, 2, 3), listOf(5, 2, 6), listOf(7, 2, 8)),
        // The money bags burst, and are back a few seconds later.
        props = WesternProps.BAG_XS.map { Prop(WesternProps.BAG, Box(it, WesternProps.BAG_Y, 16, 16), points = 20, back = 4000) } +
            // The sign over the vault breaks in two, and stays broken.
            Prop(WesternProps.SIGN, WesternProps.SIGN_BOX, points = 20),
        foes = allFoes,
        friends = listOf(BANKER, GRANNY, BONNET, SHERIFF),
        balloon = BALLOON, star = STAR, sign = BULLSEYE, dynamite = DYNAMITE,
        lanes = mapOf('g' to Lane(COIN, 201, 10, 115)),
        waves = listOf(
            Wave("FFgFb", gap = 1500, stay = 2400),
            Wave("FfdFgFf", gap = 1350, stay = 2250),
            Wave("L", gap = 1350, stay = 2250),
            Wave("FfdFsFgBF*", gap = 1150, stay = 2050),
            Wave("LL", gap = 2300, stay = 2050),
            Wave("FfFgFsdFfFF", gap = 1000, stay = 1850),
        ),
        song = Songs.BANK,
    )

    private val canyon = Stage(
        key = 2,
        story = listOf(Card({ Story.CANYON }, "The gang's trail goes out of town, to the old mine in Buzzard Canyon. Go after them!")),
        title = "Buzzard Canyon",
        line = "Shoot the dynamite before it lands. Or shoot the TNT!",
        slots = listOf(
            Slot(10, 52, 28, 40, reach = 38, dark = true),             // 0 the left working, on the ledge
            Slot(178, 60, 28, 40, reach = 38, dark = true),            // 1 the right working
            Slot(90, 104, 18, 44, from = Side.LEFT, reach = 18, dark = true),   // 2 the tunnel, left side
            Slot(108, 104, 18, 44, from = Side.RIGHT, reach = 18, dark = true), // 3 the tunnel, right side
            Slot(6, 112, 28, 34, reach = 30),                          // 4 behind the crates
            Slot(182, 124, 28, 34, reach = 30),                        // 5 behind the rocks
            Slot(92, 158, 32, 34, reach = 32, signs = true),           // 6 in the mine cart
        ),
        lineups = listOf(listOf(0, 2, 1), listOf(4, 6, 5), listOf(0, 3, 1)),
        foes = allFoes,
        friends = listOf(SHERIFF, KID, BABY, BONNET),
        balloon = BALLOON, star = STAR, sign = BULLSEYE, dynamite = DYNAMITE,
        tnt = Box(4, 146, 32, 20), tntSlot = 4,
        lanes = mapOf(
            't' to Lane(WEED, 200, 9, 115, hop = 6),
            'v' to Lane(BUZZARD, 28, 10, 120),
        ),
        waves = listOf(
            Wave("FFtFv", gap = 1400, stay = 2200),
            Wave("FfdFtF", gap = 1300, stay = 2100),
            Wave("L", gap = 1400, stay = 2100),
            Wave("FfdFvFbtF*", gap = 1100, stay = 1900),
            Wave("LL", gap = 2200, stay = 1900),
            Wave("FfdFtFdFvfFF", gap = 950, stay = 1750),
        ),
        song = Songs.MESA,
    )

    /**
     * The chase. You ride beside the train from its back end to its front: each wave is the next car coming alongside.
     * The cars all share the same openings, so the rules don't change; only the engine has fewer.
     */
    private val train = Stage(
        key = 4,
        story = listOf(Card({ Story.WINDOW }, "Too late! Hank has Belle on the train. Get on your horse and catch it!")),
        title = "The Iron Horse",
        line = "Shoot the outlaws. Don't hit Belle, or it's over!",
        slots = listOf(
            Slot(12, 46, 28, 38, reach = 36),                          // 0 on the roof, back end
            Slot(64, 46, 28, 38, reach = 36),                          // 1 on the roof
            Slot(124, 46, 28, 38, reach = 36),                         // 2 on the roof
            Slot(176, 46, 28, 38, reach = 36),                         // 3 on the roof, front end
            Slot(12, 98, 28, 34, reach = 32, dark = true),             // 4 the windows
            Slot(64, 98, 28, 34, reach = 32, dark = true),             // 5
            Slot(124, 98, 28, 34, reach = 32, dark = true),            // 6
            Slot(176, 98, 28, 34, reach = 32, dark = true),            // 7
        ),
        lineups = listOf(listOf(4, 5, 6), listOf(5, 6, 7), listOf(0, 1, 2), listOf(1, 2, 3), listOf(0, 5, 3)),
        foes = allFoes,
        friends = listOf(BELLE, GRANNY, BONNET),
        star = WHITE_HAT, sign = BULLSEYE, dynamite = DYNAMITE,
        lanes = mapOf(
            't' to Lane(WEED, 194, 13, 110, bothWays = false, fromRight = true, hop = 5),
            'v' to Lane(BUZZARD, 28, 12, 120, bothWays = false, fromRight = true),
        ),
        waves = listOf(
            Wave("FFtFvF", gap = 1350, stay = 2200),                   // the caboose
            Wave("FfFdFfFt", gap = 1250, stay = 2100),                 // the passenger car
            Wave("LFdFv*F", gap = 1250, stay = 2000),                  // the express car
            Wave("FfdFtLFf", gap = 1100, stay = 1900),                 // the stock car
            Wave("FdFfdFvFLF", gap = 1000, stay = 1800, only = listOf(0, 1, 2, 5)), // the engine
        ),
        song = Songs.IRON,
    )

    /**
     * The jailhouse, with Belle locked in it and the gang all round: on the roof, at the windows, in the doorway and
     * behind the trough and the barrels. Belle waves from her cell: don't hit her.
     */
    private val jail = Stage(
        key = 7,
        story = listOf(Card({ Story.JAIL }, "Hank rode back to town and locked Belle in the old jail. His gang is guarding it!")),
        title = "The Jailhouse",
        line = "Belle's in the window. Don't hit her, or it's over!",
        slots = listOf(
            Slot(30, 24, 24, 18, reach = 17),                          // 0 roof, left
            Slot(162, 24, 24, 18, reach = 17),                         // 1 roof, right
            Slot(66, 58, 28, 34, cover = SHUTTERS, reach = 32),        // 2 the sheriff's windows
            Slot(122, 58, 28, 34, cover = SHUTTERS, reach = 32),       // 3
            Slot(172, 58, 28, 32, cover = BARS, opens = false, reach = 30), // 4 the other cell
            Slot(92, 128, 32, 56, cover = DOOR),                       // 5 the door
            Slot(8, 164, 30, 32, reach = 30, signs = true),            // 6 behind the trough
            Slot(176, 156, 30, 32, reach = 30, signs = true),          // 7 behind the barrels
        ),
        lineups = listOf(listOf(0, 2, 1), listOf(0, 3, 1), listOf(6, 5, 7), listOf(4, 5, 2)),
        foes = allFoes,
        friends = listOf(SHERIFF, GRANNY, KID, BONNET, BABY),
        balloon = BALLOON, star = STAR, sign = BULLSEYE,
        spots = listOf(Triple(WANTED, 18, 104), Triple(WANTED, 44, 102), Triple(WANTED, 160, 102), Triple(WANTED, 186, 104)),
        lanes = mapOf('t' to Lane(WEED, 200, 9, 110, hop = 6)),
        resident = Triple(BELLE_CELL, 14, 58),
        // The trough and the barrels are stove in when they're shot, and stand there leaking for the rest of the stage.
        props = listOf(
            Prop(WesternProps.TROUGH, Box(4, 196, 36, 22), points = 20),
            Prop(WesternProps.BARRELS, Box(170, 188, 36, 30), points = 20),
            Prop(WesternProps.JAIL_SIGN, WesternProps.JAIL_SIGN_BOX, points = 20),
        ),
        waves = listOf(
            Wave("FFtFbF", gap = 1300, stay = 2100),
            Wave("FfFtFfFb", gap = 1200, stay = 2000, bottles = true),
            Wave("L", gap = 1250, stay = 1950),
            Wave("FfFsFtBF*F", gap = 1050, stay = 1850),
            Wave("LL", gap = 2100, stay = 1850),
            Wave("FfFtFsFfFFb", gap = 950, stay = 1700),
        ),
        song = Songs.JAIL,
    )

    private val showdown = Stage(
        key = 3,
        story = listOf(Card({ Story.SHOWDOWN }, "It ends here, at high noon: his two best guns, then Hank himself. Wait for DRAW, then tap.")),
        title = "High Noon",
        line = "Wait for DRAW, then tap fast. Read it first: they'll try to fool you!",
        slots = emptyList(), foes = emptyList(), friends = emptyList(), waves = emptyList(),
        song = Songs.NOON, duel = true,
    )

    override val stages = listOf(mainStreet, store, saloon, bank, canyon, train, jail, showdown)

    override val ending = listOf(
        Card({ Story.FREE }, "You did it! Hank's gang is finished, and all of Dry Gulch is cheering."),
        Card({ Story.SUNSET }, "You and Belle moseyed off into the sunset.", head = "THE END"),
    )

    override fun look(spec: Spec, pose: Pose, frame: Int): Sprite = when (spec) {
        BABY -> if (pose == Pose.HIT) WesternArt.babyHit else WesternArt.baby
        // Playing; shot off his stool and seeing stars; back on it, getting his breath.
        PIANIST -> when (pose) {
            Pose.HIT -> WesternArt.pianistDown[frame and 1]
            Pose.BARE -> WesternArt.pianistDazed[frame and 1]
            else -> WesternArt.pianist[frame and 1]
        }
        BALLOON -> WesternArt.balloon
        STAR -> WesternArt.starBalloon
        BOTTLE -> WesternArt.bottle
        CAN -> WesternArt.can
        BULLSEYE -> if (pose == Pose.HIT) WesternArt.bullseyeHit else WesternArt.bullseye
        WEED -> WesternArt.tumbleweed[frame and 3]
        MUG -> WesternArt.mug
        BUZZARD -> WesternArt.buzzard[(frame shr 1) and 1]
        DYNAMITE -> WesternArt.dynamite
        BELLE -> WesternArt.snail(if (frame and 1 == 1) "belle waving" else "belle", pose)
        WHITE_HAT -> WesternArt.whiteHat
        WHEEL -> WesternArt.wheel[frame and 3]
        COIN -> WesternArt.coin[frame and 3]
        WANTED -> WesternArt.wanted
        BELLE_CELL -> WesternArt.belleInCell[if (pose == Pose.HIT) 2 else frame and 1]
        else -> WesternArt.snail(spec.name, pose)
    }

    /** The leader for this duel, twice life size. */
    fun boss(duel: Int, pose: Pose): Sprite = WesternScenes.boss(duel, pose)

    override fun cover(slot: Slot, open: Float): Sprite? = when (slot.cover) {
        SHUTTERS -> WesternScenes.shutters(slot.w, slot.h, open)
        BATWINGS -> WesternScenes.batwings(slot.w, slot.h, open)
        CURTAIN -> WesternScenes.curtain(slot.w, slot.h, open)
        VAULT -> WesternScenes.vault(slot.w, slot.h, open)
        BLIND -> WesternScenes.blind(slot.w, slot.h, open)
        BARS -> WesternScenes.bars(slot.w, slot.h)
        DOOR -> WesternScenes.door(slot.w, slot.h, open)
        else -> null
    }

    override fun badge(stage: Stage): Sprite = when (stage) {
        mainStreet -> WesternArt.LIFE
        store -> WesternArt.candyJar
        saloon -> WesternArt.mug
        bank -> WesternArt.moneyBag
        jail -> WesternArt.key
        canyon -> WesternArt.tumbleweed[0]
        train -> WesternArt.engine
        else -> WesternArt.SUN
    }

    override fun background(stage: Stage): Pad = when (stage) {
        mainStreet -> WesternScenes.mainStreet()
        store -> WesternScenes.store()
        saloon -> WesternScenes.saloon()
        bank -> WesternScenes.bank()
        jail -> WesternScenes.jailhouse()
        canyon -> WesternScenes.canyon()
        train -> Pad(G.W, G.H).also { TrainArt.sky(it) }
        else -> WesternScenes.noon()
    }

    override fun rolling(stage: Stage): Rolling? = when {
        stage === train -> TrainRide()
        stage === canyon -> MineCart(stage.slots.indexOfFirst { it.signs })
        else -> null
    }
}
