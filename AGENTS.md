## graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)

## Project overview

Multi-module Android TV IPTV player (Kotlin, Jetpack Compose, Room, Hilt, Media3).

- Modules: `:app` (UI + Compose), `:domain` (pure Kotlin, models, use cases), `:data` (Room, repositories, sync), `:player` (Media3 wrapper, FFmpeg decoder)
- `settings.gradle.kts` root project name is still `StreamVault`; the user-facing brand is migrating to **Eh! IPTV**
- Current package root: `app.ehtudo.*` — `app` module is `app.ehtudo.iptv`, others are `app.ehtudo.{data,domain,player}`. The plugin API contract is `app.ehtudo.plugin.API` (see `docs/PLUGIN_API.md`)
- `applicationId` is `app.ehtudo.iptv`; `debug` build adds `.debug` suffix, `beta` adds `.beta`
- Version: `versionName = "1.0.16"`, `versionCode = 17` — bump in `app/build.gradle.kts`

The Welcome screen is the simplified onboarding: title "Eh! IPTV", only username + password as plain text inputs, hardcoded Xtream URL `http://dnstv.top/`, default provider name "Eh! IPTV". The full `ProviderSetupScreen` is still reachable for advanced/Stalker/M3U/Jellyfin.

## Build and test

Linux build env, JDK 17, Gradle 8.12, Android SDK at `/home/edson/Android/Sdk` (declared in `local.properties`).

Common commands (use `--no-daemon` for stability on memory-pressured boxes):

```bash
# Debug APK (most common during development)
./gradlew :app:assembleDebug --no-daemon

# Beta APK (what CI publishes on pushes to develop)
./gradlew :app:assembleBeta --no-daemon

# Release APK + unit tests + coverage (what CI runs on workflow_dispatch)
./gradlew testDebugUnitTest koverXmlReportCi koverHtmlReportCi :app:assembleRelease --no-daemon

# Single module tests
./gradlew :data:test :domain:test :player:test --no-daemon

# Lint / typecheck — none are wired into the build; KSP + Kotlin compile are the typecheck.
# CI does not run lint; do not rely on it.
```

APK output: `app/build/outputs/apk/{debug,beta,release}/`. Full debug build takes ~10 min from cold (multiple KSP rounds); incremental is much faster.

Tests:
- Unit tests: `app/src/test/`, `data/src/test/`, `domain/src/test/`, `player/src/test/`. Some are Robolectric (e.g. `LiveTranslationClientTest`, `ExternalPlayerLauncherTest`) — they run on the JVM via the Robolectric runner.
- Golden / screenshot tests: `app/src/androidTest/` — `PremiumRouteGoldenTest`, `PlayerOverlayGoldenTest`, `ShellGoldenTest`. Need a connected device or emulator.
- Kover is configured in root `build.gradle.kts`; the `ci` variant produces XML+HTML at `build/reports/kover/`. Excludes are Config/R/Hilt/factory boilerplate.

## Enviar o APK para o celular

Conecte o celular por USB, habilite a **Depuração USB** e confirme a autorização exibida no aparelho. Antes de instalar, confira os dispositivos conectados:

```bash
adb devices
```

Gere e instale o APK de debug no Xiaomi usando o serial explícito para não atingir a TV por engano:

```bash
./gradlew :app:assembleDebug --no-daemon
adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk
```

A opção `-r` atualiza a instalação existente preservando os dados do aplicativo. Para uma instalação limpa:

```bash
adb -s d1d1b8f3 uninstall app.ehtudo.iptv.debug
adb -s d1d1b8f3 install app/build/outputs/apk/debug/app-debug.apk
```

Para instalar outra variante, gere o APK correspondente e use o arquivo em `app/build/outputs/apk/{beta,release}/`. Sempre use `adb -s <id>` quando houver mais de um dispositivo conectado.

> No Xiaomi com MIUI/HyperOS, se aparecer `INSTALL_FAILED_USER_RESTRICTED`, habilite **Configurações → Opções do desenvolvedor → Instalar via USB**. Essa opção pode exigir login em uma conta Mi.

## Gotchas

### Package rename ⇒ wipe KSP caches

After renaming packages (e.g. `com.streamvault.*` → `app.ehtudo.*`, or `app.ehtudo.app` → `app.ehtudo.iptv`), the KSP cache under `app/build/kspCaches/` keeps stale class references and the next build fails with `Could not find class file for '<old.fqcn>'`. Fix:

```bash
rm -rf app/build/kspCaches data/build/kspCaches player/build/kspCaches domain/build/kspCaches
./gradlew :app:assembleDebug --no-daemon
```

### MIUI "Install via USB" gate

On MIUI / HyperOS devices, `adb install` returns `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` until the user has toggled **Configurações → Opções do desenvolvedor → "Instalar via USB"** on the device. First enable requires an Mi-account login. Re-triggered every time the applicationId changes. The dialog is dismissed silently — you must look at the device, not the host. There is no `adb` command to flip this; ask the user.

### Dev seeding

`local.properties` (gitignored) injects `BuildConfig.XTREAM_DEV_*` / `M3U_DEV_*` for the `debug` build only, so `WelcomeViewModel.maybeSeedDevProvider()` can auto-create a provider on first launch. See `docs/DEV_SEEDING.md` and `local.properties.example`. Release builds receive empty strings — never ship credentials.

### Player module FFmpeg AAR

`player/libs/media3-decoder-ffmpeg-1.9.2.aar` is consumed via `implementation(files(...))` in `app/build.gradle.kts`. The `player` module has a `verifyLocalFfmpegArtifact` task that validates the AAR is present; check it before debugging FFmpeg-decoder issues.

## Devices

Two adb devices are usually attached:

- `d1d1b8f3` — Xiaomi POCO M2012K11AG (Android 13, arm64-v8a) — primary development phone
- `adb-IN211005...` — Realme TV stick (Android TV, 4K_Google_TV_Stick) — useful for TV-layout smoke tests

Always target with `adb -s <id>` to avoid hitting the wrong one. The production `app.ehtudo.iptv` build lives on the Xiaomi. `pm clear` is your friend when retesting onboarding.

## StreamVault emulator orientation

When starting the emulator with StreamVault, always ensure the visible device frame
and the app orientation are aligned before playback debugging. The known-good
orientation from the debugging session is the emulator in landscape with the app
upright at Android `ROTATION_270`.

Use:

```bash
adb shell cmd window set-ignore-orientation-request true
adb shell cmd window user-rotation lock 3
```

Verify with:

```bash
adb shell dumpsys window displays | rg "cur=|mRotation=|mUserRotationMode|mUserRotation=|mCurrentRotation|mDisplayRotation|ignoreOrientationRequest"
```

Expected state:
- `cur=2340x1080 app=2340x1080`
- `mDisplayRotation=ROTATION_270`
- `mRotation=3`
- `mUserRotationMode=USER_ROTATION_LOCKED`
- `mUserRotation=ROTATION_270`
- `ignoreOrientationRequest=true`

Do not treat `cur=2340x1080 app=2340x1080` alone as sufficient; the app can
still be sideways or upside down if the emulator frame and Android rotation are
not aligned. If the phone frame is portrait while the app is upright, rotate the
emulator frame with `adb emu rotate`, then reapply the `ROTATION_270` lock above.

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
adb shell dumpsys media_session | awk '/package=app.ehtudo.iptv/{seen=1} seen && /metadata:/{print; getline; print; getline; print} seen && /state=PlaybackState/{print; exit}'
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

Validate more than one live channel when the bug is reported as affecting live
TV generally. Record the channel names, screenshot count, interval, unique hash
count, media-session result, and log findings in the final report.
