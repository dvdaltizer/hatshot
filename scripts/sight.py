"""Plays Dusty Street by sight, from outside the game: looks at real screenshots, finds solid hats, taps them.

Unlike the game's own test player, every shot here goes through the touch screen, so it checks that a tap lands where
the picture says the target is. Usage: sight.py <seconds> [stage-row-y]
"""
import io, subprocess, sys, time
from PIL import Image

ADB = ["adb", "-s", "emulator-5604"]
PX = 5

# Where each opening's snail stands when fully out, in game pixels: the top-left of its 24-wide picture.
SPOTS = {
    "roof left": (10, 25), "roof right": (182, 25),
    "up 1": (15, 52), "up 2": (69, 52), "up 3": (123, 52), "up 4": (177, 52),
    "down left": (15, 118), "down right": (177, 118),
    "door": (96, 132), "barrels": (7, 158), "trough": (185, 160),
}


def sh(*args):
    return subprocess.run(ADB + ["shell", *args], capture_output=True, text=True).stdout


def shot():
    png = subprocess.run(ADB + ["exec-out", "screencap", "-p"], capture_output=True).stdout
    return Image.open(io.BytesIO(png)).convert("L")


def solid(img, tx, ty):
    """How much of the hat's crown is filled: near 1 for an outlaw, near 0 for a friend or nobody."""
    box = ((tx + 8) * PX, (ty + 3) * PX, (tx + 16) * PX, (ty + 5) * PX)
    px = list(img.crop(box).getdata())
    return sum(1 for p in px if p >= 250) / len(px)


def face(img, tx, ty):
    """Whether somebody is there at all: any bright white in the head's box."""
    box = (tx * PX, ty * PX, (tx + 24) * PX, (ty + 18) * PX)
    px = list(img.crop(box).getdata())
    return sum(1 for p in px if p >= 250) / len(px)


def main():
    seconds = float(sys.argv[1]) if len(sys.argv) > 1 else 90
    start = time.time()
    shots = seen_outlaws = seen_friends = frames = 0
    since_reload = 0
    last = {}
    while time.time() - start < seconds:
        img = shot()
        frames += 1
        if "results stage" in subprocess.run(ADB + ["logcat", "-d", "-s", "Hatshot:D"], capture_output=True, text=True).stdout:
            break
        for name, (tx, ty) in SPOTS.items():
            s = solid(img, tx, ty)
            if s >= 0.9:
                # Not twice at the same hat within a moment: it takes a beat for the picture to catch up.
                if time.time() - last.get(name, 0) < 1.2:
                    continue
                seen_outlaws += 1
                sh("input", "tap", str((tx + 12) * PX), str((ty + 5) * PX))
                last[name] = time.time()
                shots += 1
                since_reload += 1
            elif face(img, tx, ty) > 0.08:
                seen_friends += 1
        if since_reload >= 4:
            sh("input", "tap", "540", "1175")
            since_reload = 0
    print(f"looked {frames} times in {time.time() - start:.0f} s; saw a solid hat {seen_outlaws} times, someone else {seen_friends} times; tapped {shots} hats")


main()
