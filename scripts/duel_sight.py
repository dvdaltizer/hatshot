"""Plays High Noon by sight: watches real screenshots for the big word DRAW! and taps the screen when it's up."""
import io, subprocess, sys, time
from PIL import Image
ADB = ["adb", "-s", "emulator-5604"]
def shot():
    return Image.open(io.BytesIO(subprocess.run(ADB + ["exec-out", "screencap", "-p"], capture_output=True).stdout)).convert("L")
def drawn(img):
    # DRAW! is drawn bigger than any other word, so only it has white in the rows above where the others start.
    band = img.crop((300, 142, 780, 160))
    return sum(1 for p in band.tobytes() if p >= 250) > 60
taps = 0; start = time.time(); last = 0
while time.time() - start < 70:
    if "results stage" in subprocess.run(ADB + ["logcat", "-d", "-s", "Hatshot:D"], capture_output=True, text=True).stdout: break
    if drawn(shot()) and time.time() - last > 1.5:
        subprocess.run(ADB + ["shell", "input", "tap", "540", "800"]); taps += 1; last = time.time()
print(f"tapped on DRAW {taps} times in {time.time() - start:.0f} s")
