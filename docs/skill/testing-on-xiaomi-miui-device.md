# Skill 7 — Testing on the Xiaomi MIUI device

## Goal

Sideload and exercise a debug APK on the Xiaomi POCO M2012K11AG (`d1d1b8f3`) that ships with the MIUI/HyperOS "Install via USB" gate. This gate is independent of normal `adb install` permissions — it sits at the MIUI Security Center layer and *will* silently drop your install attempt.

The skills in `docs/skill/` all assume a working ADB + Xiaomi loop. This skill is the test rig.

## The Xiaomi device facts

| Property | Value |
|---|---|
| ADB serial | `d1d1b8f3` |
| Model | `M2012K11AG` (POCO F3 / Redmi K40 "alioth") |
| Brand | POCO |
| Android | 13 (SDK 33) |
| ABI | `arm64-v8a` |
| Skin | MIUI / HyperOS |
| Physical size | 1080x2400 (portrait native) |
| App orientation | `screenOrientation="landscape"` → `cur=2400x1080` |
| Real rotation | portrait 0°; app frame rotated 90° (landscape) for the IPTV player |
| Default ADB mode | `adb -s d1d1b8f3` |

## The other ADB device

A Realme TV stick `adb-IN211005662105A18615-DqcAKG._adb-tls-connect._tcp` is also usually connected. **Always target the Xiaomi explicitly** with `adb -s d1d1b8f3`. The `-s` flag prevents `adb` from sending the install to the TV by mistake.

If you ever lose the Xiaomi, run `adb devices` and check the IP/serial mapping.

## MIUI's "Install via USB" gate

MIUI Security Center intercepts every `adb install` and re-prompts the user. The error you see when the user has not authorized the install is **always**:

```
adb: failed to install ... : Failure [INSTALL_FAILED_USER_RESTRICTED: Install canceled by user]
```

The dialog is dismissed silently — you must look at the device, not the host. There is no `adb` command to flip this.

**To unlock it (one-time per session, repeated when the applicationId changes):**

1. Settings → Additional settings → Developer options.
2. Enable "USB debugging" if not already on.
3. Enable "Install via USB" (a.k.a. "USB debugging (Security settings)" on older MIUI). On first activation, MIUI demands you sign in to a Mi account.
4. After the user taps "OK" in the dialog, future `adb install` works until the applicationId changes again or MIUI re-locks.

**Heuristic for the agent:** when an `adb install` returns `INSTALL_FAILED_USER_RESTRICTED`, do not retry automatically in a loop. Ask the user to confirm on the device, then run a single `adb install`. The dialog will appear in the notification shade and on the screen.

## Build & install loop

```bash
# 1. Build
cd /home/edson/StudioProjects/eh-iptv-player
./gradlew :app:assembleDebug --no-daemon   # cold ~10 min, warm ~2 min

# 2. Verify the APK is fresh
ls -la app/build/outputs/apk/debug/app-debug.apk
date

# 3. Uninstall (only if the user wants a true clean state)
adb -s d1d1b8f3 uninstall app.ehtudo.iptv.debug

# 4. Install
adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk
# Expect: "Performing Streamed Install" then "Success".
# Failure modes:
#   INSTALL_FAILED_USER_RESTRICTED → user hasn't approved; ask them to flip the toggle.
#   INSTALL_FAILED_VERSION_DOWNGRADE → use `adb uninstall` first or pass `-d`.
#   INSTALL_FAILED_INSUFFICIENT_STORAGE → free space; check `adb shell df -h /data`.

# 5. Clear app data only when intentionally testing from a clean slate
adb -s d1d1b8f3 shell pm clear app.ehtudo.iptv.debug

# 6. Launch the main activity
adb -s d1d1b8f3 shell am start -n app.ehtudo.iptv.debug/app.ehtudo.iptv.MainActivity

# 7. Pull a screenshot
adb -s d1d1b8f3 exec-out screencap -p > /tmp/screen.png
```

## Driving the TV UI via adb

The app is built for Android TV — almost all interactions are remote/D-pad style. The Xiaomi is a phone, so its `input` subsystem also accepts touch taps, but Compose's TV `ClickableSurface` and focus traversal expect key events.

For tap-based interactions:
```bash
adb -s d1d1b8f3 shell input tap <x> <y>
```

For D-pad navigation (preferred for the welcome flow where the focus ring must move):
```bash
adb -s d1d1b8f3 shell input keyevent KEYCODE_DPAD_UP
adb -s d1d1b8f3 shell input keyevent KEYCODE_DPAD_DOWN
adb -s d1d1b8f3 shell input keyevent KEYCODE_DPAD_LEFT
adb -s d1d1b8f3 shell input keyevent KEYCODE_DPAD_RIGHT
adb -s d1d1b8f3 shell input keyevent KEYCODE_DPAD_CENTER
```

For typing into a focused `OutlinedTextField`:
```bash
adb -s d1d1b8f3 shell input text "<plain text, no spaces, no special chars>"
# Or for safe special chars: %s for space, then escape.
# Do NOT use `input keyboard` — the soft keyboard on the Xiaomi is in a different
# process and the focus chain breaks.
```

For dismissing the IME:
```bash
adb -s d1d1b8f3 shell input keyevent KEYCODE_BACK   # hides the IME, does NOT navigate
```

For sending the IME-action ("Done"):
```bash
adb -s d1d1b8f3 shell input keyevent KEYCODE_ENTER
```

## Capturing evidence for a regression

```bash
# 1. Start a logcat tap
adb -s d1d1b8f3 logcat -c
# 2. Trigger the action under test (tap / type / etc.)
# 3. Pull the logcat
adb -s d1d1b8f3 logcat -d > /tmp/lc.log
rg -n "fatal-error|live-recovery selected|prepare resolvedStreamType=HLS|first-frame-success" /tmp/lc.log | tail -80
# 4. Screenshot
adb -s d1d1b8f3 exec-out screencap -p > /tmp/repro.png
```

For live-TV playback bugs (per the AGENTS.md "StreamVault live TV validation" protocol), use a 2-second screenshot cadence for at least 90 seconds, then dedupe by SHA-256 to confirm frames changed.

## Inspecting the Room database

The Xiaomi's Room database lives at `/data/data/app.ehtudo.iptv.debug/databases/streamvault.db` and is private to the app. Two ways to read it:

```bash
# Option A — pull via run-as (only works for debuggable builds)
adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
ls -la /tmp/db.sqlite  # should be > 0 bytes

# Option B — use sqlite3 in the app sandbox (may need root on MIUI)
adb -s d1d1b8f3 shell "run-as app.ehtudo.iptv.debug sqlite3 databases/streamvault.db 'SELECT ...;'" 2>&1
```

Once the DB is on the host, query it:
```bash
sqlite3 /tmp/db.sqlite ".tables"                                # list tables
sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status FROM providers;"
sqlite3 /tmp/db.sqlite "SELECT type, COUNT(*) FROM categories WHERE provider_id=1 GROUP BY type;"
sqlite3 /tmp/db.sqlite "SELECT COUNT(*) FROM channels WHERE provider_id=1;"
```

The most useful invariants to check:
- `providers.is_active` is `1` (the best-effort activation skill is applied)
- `providers.status` is `ACTIVE`
- `categories` has rows for `LIVE`, `MOVIE`, `SERIES` once the worker finishes
- `channels` is non-empty for `LIVE`

## The orientation trap

The phone's physical orientation is portrait, but the app forces `screenOrientation="landscape"`. The screencap output is the *framebuffer* which respects the app's orientation. So screenshots are always 2400×1080 (landscape), even though `wm size` reports the physical 1080×2400.

When scripting tap coordinates, **always use the 2400×1080 framebuffer coordinates**, not the physical 1080×2400. Using the wrong coordinate system is the #1 cause of "the tap does nothing" debugging sessions.

Verify the current orientation before relying on coordinates:
```bash
adb -s d1d1b8f3 shell dumpsys window displays | grep -E "cur=|mRotation=" | head -2
# Expect: cur=2400x1080 (when app is in landscape).
```

## Common pitfalls

| Pitfall | What goes wrong | Fix |
|---|---|---|
| Install blocked with `INSTALL_FAILED_USER_RESTRICTED` | MIUI gate not flipped | Ask the user to toggle "Install via USB" in Developer Options. |
| Install "successful" but launch goes to home | The activity is renamed / package is different | Check `adb shell pm list packages` and `adb shell cmd package resolve-activity` to confirm the FQCN. |
| `am start` says "Activity not found" | Wrong FQCN in the launch command | After a package rename, the activity FQCN changes from `com.streamvault.app.MainActivity` to `app.ehtudo.iptv.MainActivity`. |
| Tap does nothing | Used physical coordinates (1080×2400) instead of framebuffer (2400×1080) | Recompute coordinates based on the screenshot. |
| Soft keyboard steals focus | IME input dispatcher runs in a different process | Use `input keyevent` for navigation; use `input text` only when the field has focus from a prior tap. |
| `screencap` returns a black image | Surface flinger cache miss; try again after 1 s | Re-run the command. If it persists, the activity may be in `Surface.lockCanvas`-only mode. |
| App says "Sincronização necessária" on every tab | `is_active=0` in the DB | The best-effort activation skill is not applied. Check `ProviderRepositoryImpl.loginXtream`. |

## Anti-patterns (do not)

- Do **not** assume `adb install -r` is enough to bypass MIUI's gate. It is not.
- Do **not** loop on `INSTALL_FAILED_USER_RESTRICTED` — the dialog is on the device, not the host.
- Do **not** use `adb root` on the Xiaomi — MIUI blocks it.
- Do **not** use `dumpsys window` to compute tap coordinates — it's the *physical* display, not the framebuffer.
- Do **not** try to test the best-effort activation path by reading the DB *before* the Worker finishes. The provider is *already* `is_active=1, status=ACTIVE` immediately after Salvar. The Worker only populates the catalog tables; it does not change activation.
