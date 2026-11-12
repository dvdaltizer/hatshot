package com.slowpixels.hatshot.game

import com.slowpixels.hatshot.game.Song.Bass
import com.slowpixels.hatshot.game.Song.Harmony

/** The music: original chiptunes, a written melody over chords, with the band generated in a style that suits ([Song]). */
object Songs {
    /** Main Street: a trotting horse in A minor, in no hurry and in good spirits. */
    val TRAIL = Song("Dusty Trail", 132, 4,
        chords = "Am | Am | C | G | F | C | E | Am | Am | Dm | G | C | F | Dm | E | Am",
        lead = """o5 q6
            e8. e16 a4 g8 e8 c4 | d8 e8 c8 <a8 >e4 r4 | g8. g16 >c4 <b8 g8 e4 | d8 e8 g8 b8 a4 g4 |
            a8. a16 >c4 <a8 f8 c4 | e8 g8 >c8 <g8 e4 r4 | g+8 g+8 b4 g+8 e8 b4 | a2. r4 |
            >e8. d16 c4 <b8 a8 g4 | a8 f8 d8 f8 a4 r4 | b8. a16 g4 f8 g8 b4 | >c8 <g8 e8 g8 >c4 r4 |
            c8. <b16 a4 f8 a8 >c4 | d8 c8 <a8 f8 d4 r4 | e8 g+8 b8 >d8 c8 <b8 g+8 b8 | a2 e4 r4""",
        bass = Bass.OOM, harmony = Harmony.STAB, drums = "k.h.t.h.k.h.t.hh", fill = "k.h.t.h.k.t.tttt", leadDuty = 2)

    /**
     * The Saloon: Scott Joplin's "The Entertainer" (1902, long in the public domain), arranged for the sound chip as the
     * house piano's party piece. The melody is from the 1902 edition (Mutopia Project's typesetting): the intro once, the
     * first strain twice, the second strain, round again from the first. Its first figure sits an octave up, so the
     * tune stays in the chip's bright middle. Under it, a ragtime two-step: a low note on each beat, a short chord off
     * it, kick and snare, and the hat on the off-beats. The melody is the piano player's: hit him and it stops while
     * the band keeps the beat going (Tracker.melody).
     */
    val RAG = Song("The Entertainer", 92, 2,
        chords = """G | G | G | G7 |
            C | C | C G7 | C | C | C | D7 | G | C | C | C G7 | C | C | C | C G7 | C |
            C | C | C G7 | C | C | C | D7 | G | C | C | C G7 | C | C | C | C G7 | C |
            C | C | G7 | G7 | C | C C7 | G D7 | G G7 | C | C | G7 | C | F | C | D7 G7 | C""",
        lead = """q6 v15
            o6 d16 e16 c16 o5 a16 ^16 b16 g8 | o5 d16 e16 c16 o4 a16 ^16 b16 g8 |
            o4 d16 e16 c16 o3 a16 ^16 b16 a16 a-16 | o3 g8 r8 o5 g8 d16 d+16 |
            o5 e16 o6 c8 o5 e16 o6 c8 o5 e16 o6 c16 | ^4 ^16 c16 d16 d+16 |
             e16 c16 d16 e16 ^16 o5 b16 o6 d8 | c4 ^8 o5 d16 d+16 |
             o5 e16 o6 c8 o5 e16 o6 c8 o5 e16 o6 c16 | ^4 ^8 o5 a16 g16 |
             f+16 a16 o6 c16 e16 ^16 d16 c16 o5 a16 | o6 d4 ^8 o5 d16 d+16 |
             o5 e16 o6 c8 o5 e16 o6 c8 o5 e16 o6 c16 | ^4 ^16 c16 d16 d+16 |
             e16 c16 d16 e16 ^16 o5 b16 o6 d8 | c4 ^8 c16 d16 |
             e16 c16 d16 e16 ^16 c16 d16 c16 | e16 c16 d16 e16 ^16 c16 d16 c16 |
             e16 c16 d16 e16 ^16 o5 b16 o6 d8 |
            c4 ^8 o5 d16 d+16 |
            o5 e16 o6 c8 o5 e16 o6 c8 o5 e16 o6 c16 | ^4 ^16 c16 d16 d+16 |
            e16 c16 d16 e16 ^16 o5 b16 o6 d8 | c4 ^8 o5 d16 d+16 |
            o5 e16 o6 c8 o5 e16 o6 c8 o5 e16 o6 c16 | ^4 ^8 o5 a16 g16 |
            f+16 a16 o6 c16 e16 ^16 d16 c16 o5 a16 | o6 d4 ^8 o5 d16 d+16 |
            o5 e16 o6 c8 o5 e16 o6 c8 o5 e16 o6 c16 | ^4 ^16 c16 d16 d+16 |
            e16 c16 d16 e16 ^16 o5 b16 o6 d8 | c4 ^8 c16 d16 |
            e16 c16 d16 e16 ^16 c16 d16 c16 | e16 c16 d16 e16 ^16 c16 d16 c16 |
            e16 c16 d16 e16 ^16 o5 b16 o6 d8 |
            o6 c4 ^16 o5 e16 f16 f+16 |
            o5 g8 a16 g16 ^16 e16 f16 f+16 | g8 a16 g16 ^16 e16 c16 o4 g16 |
            a16 b16 o5 c16 d16 e16 d16 c16 d16 | o4 g16 o5 e16 f16 g16 a16 g16 e16 f16 |
            g8 a16 g16 ^16 e16 f16 f+16 | g8 a16 g16 ^16 g16 a16 a+16 |
            b16 b8 b16 ^16 a16 f+16 d16 | g4 ^16 e16 f16 f+16 |
            g8 a16 g16 ^16 e16 f16 f+16 | g8 a16 g16 ^16 e16 c16 o4 g16 |
            a16 b16 o5 c16 d16 e16 d16 c16 d16 | c4 ^16 o4 g16 f+16 g16 |
            o5 c8 o4 a16 o5 c16 ^16 o4 a16 o5 c16 o4 a16 | g16 o5 c16 e16 g16 ^16 e16 c16 o4 g16 |
            a8 o5 c8 e16 d8 c16 | ^4 ^8 d16 d+16""",
        bass = Bass.STRIDE, harmony = Harmony.STAB, drums = "k.h.s.h.", fill = "k.h.s.ss", leadDuty = 1, loopFrom = 4)

    /** The Canyon: a lonely whistle a long way off, and then not so far off. */
    val MESA = Song("Long Shadow", 104, 4,
        chords = "Dm | Dm | Bb | Bb | C | C | Dm | A | Dm | Dm | F | C | Bb | Gm | A | A",
        lead = """o5 q8 v11
            a4. f8 d2 | f8 g8 a8 >c8 <a2 | b-4. f8 d2 | d8 f8 b-8 >d8 c2 |
            <g4. e8 c2 | e8 g8 >c8 d8 e2 | d4 c8 <a8 f4 a4 | g+8 a8 >c+8 e8 d2 |
            f4. e8 d2 | c8 d8 f8 a8 g2 | a4 >c4 f4 e8 d8 | c2 <g2 |
            b-4 >d4 f4 e8 d8 | <b-8 >d8 g8 f8 d2 | c+4 e4 a4 g8 e8 | c+2 <a2""",
        bass = Bass.HALF, harmony = Harmony.ARP, drums = "k.....h.t.....h.", fill = "k.....h.t.t.tttt", leadDuty = 0)

    /** The train: a gallop in E minor, flat out, with the rhythm of hooves under it. */
    val IRON = Song("Iron Horse", 168, 4,
        chords = "Em | Em | C | D | Em | Em | Am | B7 | G | D | Em | Em | C | D | B7 | Em",
        lead = """o5 q5
            e8 e16 e16 g8 e8 b4 g4 | a8 g8 f+8 e8 f+4 e4 | g8 g16 g16 a8 g8 >c4 <g4 | a8 b8 >d8 <b8 a4 f+4 |
            e8 e16 e16 g8 e8 b4 g4 | b8 >c8 d8 e8 d4 <b4 | a8 >c8 e8 c8 <a4 >c4 | <b8 a8 g8 f+8 d+4 f+4 |
            >d8 d16 d16 <b8 g8 d4 g4 | a8 b8 >c8 d8 <a4 f+4 | g8 f+8 e8 g8 b4 >e4 | d8 <b8 g8 b8 e2 |
            >e8 d8 c8 <g8 e4 g4 | a8 b8 >d8 <b8 a4 f+4 | d+8 f+8 b8 >d+8 f+8 d+8 <b8 a8 | g4 f+4 e4 r4""",
        bass = Bass.DRIVE, harmony = Harmony.STAB, drums = "k.hk.hk.k.hk.hh.", fill = "k.hk.hk.k.t.tttt", leadDuty = 2)

    /** The General Store: a bright little tune in C, a bell over the door and sweets in jars. */
    val STORE = Song("Penny Candy", 126, 4,
        chords = "C | C | F | C | G | G7 | C | G | C | C | F | F | C | G7 | C | C",
        lead = """q6
            o5 c8 e8 g8 e8 c8 e8 g4 | o5 a8 g8 f8 e8 d4 o4 g4 | o5 f8 a8 o6 c8 o5 a8 f8 a8 o6 c4 | o5 b8 a8 g8 f8 e4 c4 |
            o5 d8 g8 b8 g8 d8 g8 b4 | o6 c8 o5 b8 a8 g8 f4 d4 | o5 e8 g8 o6 c8 o5 g8 e8 d8 c4 | o5 d8 e8 f8 d8 g2 |
            o5 c8 e8 g8 e8 c8 e8 g4 | o5 a8 g8 f8 e8 d4 c4 | o5 f8 a8 o6 c8 d8 c8 o5 a8 f4 | o5 a8 b-8 a8 g8 f4 a4 |
            o5 g8 e8 c8 e8 g8 o6 c8 o5 g4 | o5 f8 d8 o4 b8 o5 d8 f8 a8 g4 | o5 e8 g8 o6 c8 o5 g8 e4 d4 | o5 c2 r4 o4 g8 b8""",
        bass = Bass.OOM, harmony = Harmony.STAB, drums = "k.h.s.h.k.h.s.h.", fill = "k.h.s.h.k.s.ssss", leadDuty = 2)

    /** The bank: on tiptoe in G minor, a walking bass and short notes, as if nobody should hear. */
    val BANK = Song("Tiptoe", 112, 4,
        chords = "Gm | Gm | Cm | Gm | D7 | D7 | Gm | D7 | Gm | Gm | Eb | Cm | Gm | D7 | Gm | Gm",
        lead = """q4
            o4 g8 r8 b-8 r8 o5 d8 r8 o4 b-4 | o5 d8 e-8 d8 c8 o4 b-4 g4 | o5 c8 r8 e-8 r8 g8 r8 e-4 | o5 d8 c8 o4 b-8 a8 g2 |
            o4 a8 r8 o5 c8 r8 d8 r8 f+4 | o5 a8 g8 f+8 e-8 d2 | o5 g8 f+8 g8 a8 b-4 g4 | o5 f+8 e-8 d8 c8 o4 a4 f+4 |
            o4 g8 r8 b-8 r8 o5 d8 r8 g4 | o5 f+8 g8 a8 g8 f+4 d4 | o5 e-8 g8 b-8 g8 e-4 g4 | o5 c8 e-8 g8 e-8 c4 e-4 |
            o5 d8 r8 g8 r8 b-8 a8 g4 | o5 a8 f+8 d8 c8 o4 a8 f+8 d4 | o4 g8 b-8 o5 d8 g8 f+8 d8 o4 b-4 | o4 g2 r2""",
        bass = Bass.WALK, harmony = Harmony.STAB, drums = "k...h...s...h..h", fill = "k...h...s.s.ssss", leadDuty = 1)

    /** The jailhouse: a hard gallop in D minor, because this is the last of the gang before high noon. */
    val JAIL = Song("Hold the Fort", 150, 4,
        chords = "Dm | Dm | Bb | C | Dm | Dm | Gm | A | F | C | Dm | Dm | Bb | Gm | A | Dm",
        lead = """q5
            o5 d8 d16 d16 f8 d8 a4 f4 | o5 g8 f8 e8 d8 e4 c+4 | o5 f8 f16 f16 b-8 f8 d4 f4 | o5 g8 a8 g8 f8 e4 c4 |
            o5 d8 d16 d16 f8 d8 a4 o6 d4 | o6 c8 o5 a8 f8 a8 o6 d4 o5 a4 | o5 b-8 a8 g8 b-8 o6 d4 o5 b-4 | o5 a8 g8 f8 e8 c+4 e4 |
            o5 a8 a16 a16 o6 c8 o5 a8 f4 a4 | o5 g8 e8 c8 e8 g4 o6 c4 | o5 f8 e8 d8 f8 a4 o6 d4 | o6 c8 o5 a8 f8 e8 d4 a4 |
            o5 b-8 a8 b-8 o6 d8 f4 d4 | o5 g8 a8 b-8 o6 c8 d4 o5 b-4 | o5 c+8 e8 a8 e8 c+8 e8 a4 | o5 d4 f4 d4 r4""",
        bass = Bass.DRIVE, harmony = Harmony.STAB, drums = "k.hk.hk.k.hk.hh.", fill = "k.hk.hk.k.t.tttt", leadDuty = 2)

    /** Showdown: nearly nothing. A clock, a low note, and everyone holding their breath. */
    val NOON = Song("High Sun", 72, 4,
        chords = "Am | Am | F | E | Am | Am | Dm | E",
        lead = """o4 q8 v8
            a1 | r1 | a2 >c2 | <b1 | a1 | r1 | >d2 c2 | <b1""",
        bass = Bass.HALF, harmony = Harmony.NONE, drums = "h...h...h...h...", fill = "h...h...h...t...", leadDuty = 2)
}
