#!/bin/zsh
# Wins all eight stages back to back with the simulator's auto-player, taking pictures and watching for trouble.
# The simulator is muted while it runs. When it ends, the game is closed, the auto-player switched off and the sound
# put back, so it's ready to be played by hand.
# Needs a debug build on the Hatshot simulator (run-hatshot.sh). Usage: soak.sh <folder for the pictures>
export ANDROID_SERIAL=emulator-5604
S="$1"
w() { perl -e "select(undef,undef,undef,$1)"; }
shot() { adb exec-out screencap -p > "$S/$1.png"; }
mem() { adb shell dumpsys meminfo com.slowpixels.hatshot | grep -E "TOTAL PSS|TOTAL:" | head -1 | tr -s ' '; }
results() { adb logcat -d -s Hatshot:D | grep -c "results stage"; }
# Quiet while it plays itself (it's heard on the Mac): the simulator's sound goes off for the run. When the run ends,
# however it ends, the game is closed, the auto-player switched off and the sound put back.
muted() { adb shell dumpsys audio | grep -A1 "^- STREAM_MUSIC" | grep -q "Muted: true"; }
HUSHED=0; if ! muted; then adb shell input keyevent 164; HUSHED=1; fi
tidy() {
  adb shell am force-stop com.slowpixels.hatshot
  adb shell "run-as com.slowpixels.hatshot rm -f files/autoplay"
  if [ "$HUSHED" = 1 ] && muted; then adb shell input keyevent 164; fi
}
trap tidy EXIT
adb shell "run-as com.slowpixels.hatshot sh -c 'echo win > files/autoplay'"
# Not the first game: the How to play page that opens before a first game is checked by itself.
adb shell "run-as com.slowpixels.hatshot sh -c 'grep -q taught files/hatshot.properties 2>/dev/null || echo taught=true >> files/hatshot.properties'"
adb shell am force-stop com.slowpixels.hatshot
adb logcat -c
adb shell monkey -p com.slowpixels.hatshot -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
w 4
echo "start: $(mem)"
adb shell input tap 540 515; w 1.5
adb shell input tap 300 216; w 1.5
for n in 1 2 3 4 5 6 7 8; do
  shot story-$n
  adb shell input tap 975 60; w 1.5
  shot card-$n
  adb shell input tap 540 700
  w 12; shot play-$n-a
  w 14; shot play-$n-b
  # Wait for this stage's results line, two minutes at most.
  i=0; while [ "$(results)" -lt $n ] && [ $i -lt 60 ]; do w 2; i=$((i+1)); done
  w 1.5
  shot result-$n
  echo "stage $n: waited $((26 + i*2)) s; $(mem)"
  adb shell input tap 540 1160; w 2
done
shot ending-1; adb shell input tap 540 700; w 1.5
shot ending-2; adb shell input tap 540 700; w 2.5
shot home-after
echo "--- what the game said:"
adb logcat -d -s Hatshot:D | grep -E "results|ready" | sed 's/^.*Hatshot *: //'
echo "--- crashes:"
adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: |ANR in" | head -20
echo "--- end: $(mem)"
