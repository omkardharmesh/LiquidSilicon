#!/bin/zsh
# Usage: ANDROID_SERIAL=<device> scripts/bench.sh [label]
set -e
A=${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}
PKG=com.liquidglass.demo
$A shell am force-stop $PKG
$A shell am start -n $PKG/.MainActivity ${=EXTRAS} >/dev/null
sleep 5
$A shell dumpsys gfxinfo $PKG reset >/dev/null
for i in 1 2 3 4 5 6; do
  $A shell input swipe 540 1700 540 900 250
  sleep 0.6
  $A shell input swipe 540 900 540 1700 250
  sleep 0.6
done
echo "== ${1:-run}"
$A shell dumpsys gfxinfo $PKG framestats > /tmp/lg_fs.txt
python3 - <<'PY'
import re
hdr=None; g=[]; t=[]
for l in open('/tmp/lg_fs.txt').read().splitlines():
    if l.startswith('Flags,'): hdr=l.strip().rstrip(',').split(',')
    elif hdr and re.match(r'^0,',l):
        r=dict(zip(hdr,map(int,l.strip().rstrip(',').split(',')[:len(hdr)])))
        if r['GpuCompleted'] and r['IssueDrawCommandsStart']: g.append((r['GpuCompleted']-r['IssueDrawCommandsStart'])/1e6)
        if r['FrameCompleted'] and r['IntendedVsync']: t.append((r['FrameCompleted']-r['IntendedVsync'])/1e6)
def pct(a,p): a=sorted(a); return a[min(len(a)-1,int(len(a)*p))] if a else -1
print('frames=%d  gpu p50=%.1fms p90=%.1fms  total p50=%.1fms p90=%.1fms' % (len(g),pct(g,.5),pct(g,.9),pct(t,.5),pct(t,.9)))
PY
