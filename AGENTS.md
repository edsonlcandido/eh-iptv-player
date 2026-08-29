## graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)

## Live TV playback validation

For live TV playback bugs, do not validate with a single screenshot, a short
visual check, build success, install success, or launch success. Use frequent
screenshots and log evidence from the emulator.

Use a 2-second screenshot cadence for live TV stuckness checks. Capture long
enough to pass the historical stuck window: at least 45 screenshots for roughly
90 seconds, and prefer 61 screenshots for roughly 2 minutes when validating a
fix that previously failed around the one-minute mark.

Example:

```bash
mkdir -p /private/tmp/streamvault_live_validation
for i in $(seq -w 0 60); do
  adb exec-out screencap -p > /private/tmp/streamvault_live_validation/freq_${i}.png
  stat -f "freq_${i} %z" /private/tmp/streamvault_live_validation/freq_${i}.png
  sleep 2
done
```

After capture, confirm frame progression with hashes:

```bash
shasum -a 256 /private/tmp/streamvault_live_validation/freq_*.png | awk '{print $1}' | sort | uniq | wc -l
```

Then confirm the player is still healthy:

```bash
adb shell dumpsys media_session | awk '/package=com.streamvault.app/{seen=1} seen && /metadata:/{print; getline; print; getline; print} seen && /state=PlaybackState/{print; exit}'
adb logcat -d -v time > /private/tmp/streamvault_live_validation.log
rg -n "fatal-error|live-recovery selected|live-recovery no-candidate|prepare resolvedStreamType=MPEG_TS_LIVE|source-malformed live-ts-fallback|Player stuck|state=ERROR" /private/tmp/streamvault_live_validation.log
rg -n "retry category=|first-frame-success|prepare resolvedStreamType=HLS|read-progress streamType=HLS" /private/tmp/streamvault_live_validation.log | tail -80
```

A passing validation needs:
- screenshots that keep changing through the full capture window
- media session still in `PLAYING` with `error=null`
- no fatal player error, no stuck-player timeout, and no unintended MPEG-TS
  fallback
- sanitized log evidence showing HLS prepare/read/first-frame or recovery
  behavior

## N-resellers architecture (productFlavors)

The canonical base for any reseller fork is **`master`** (Davidona/develop `f86d4aee` from upstream StreamVault). **Always branch from `master`** — never from `ehiptv/custom-and-simplify` or any other renamed/simplified fork. Those are parallel pre-existing directions that this `productFlavors` architecture replaces; the `productFlavors` + `BuildConfig` approach supersedes the `private const val HARDCODED_XTREAM_URL` constant-injection pattern.

The rebrand lives in `app/build.gradle.kts` as one `flavorDimensions += "brand"` + `productFlavors` block with a `create("<brand>") { ... }` per reseller. The first flavor is **`ehtudo`** for Eh! IPTV (`applicationId = "app.ehtudo.iptv"`); future resellers add another `create("otherReseller") { ... }` block in the same dimension. Each flavor declares ~11 `buildConfigField` constants (`BRAND_NAME`, `WHATSAPP_URL`, `XTREAM_DEFAULT_URL`, `XTREAM_DEFAULT_PROVIDER_NAME`, three `BRAND_*_COLOR`, `REMOTE_CONFIG_URL`, `SHOW_ADVANCED_OPTIONS`, `ENABLE_TV_INPUT_SERVICE`) — the single source of truth for brand identity.

Kotlin sources stay at `com.streamvault.app.*` (no package rename). `applicationId` is independent of the source namespace, so each reseller ships as its own APK with **zero code changes per new reseller** (just a new `create(...)` block + skill #13 launcher-art regeneration + remote-config publish). Full pattern and worked example in `docs/skill/white-label-reseller-fork-without-rename.md`; working plan at `docs/plans/20260829-plan-N-resellers.md`.

Validate more than one live channel when the bug is reported as affecting live
TV generally. Record the channel names, screenshot count, interval, unique hash
count, media-session result, and log findings in the final report.
