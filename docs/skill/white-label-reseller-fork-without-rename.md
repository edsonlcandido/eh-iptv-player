# Skill 14 — White-label reseller fork without mass package rename

## Goal

Produce the **Eh! IPTV** branded APK (`applicationId = "app.ehtudo.iptv"`) starting from a generic StreamVault base, **without applying the mechanical mass-rename in skill #5**. The Kotlin/Java source packages may stay as `com.streamvault.*` (or whatever the upstream namespace is); only the user-visible identity — package, brand strings, colors, launcher art, Xtream URL, navigation — is customised.

This skill is the **light-weight alternative** to skill #5. It exists because skill #5 touches ~700 files and pollutes the git diff, which is overkill when the goal is just "rebrand and ship one reseller".

## When to use this skill (vs. skill #5)

| Use skill #14 (this) when… | Use skill #5 (rename) when… |
|---|---|
| You ship **one** reseller (e.g. Eh! IPTV) and source hygiene is not a hard requirement | You ship the codebase itself and want `app.ehtudo.iptv` to appear in every Kotlin file |
| You want a small, reviewable diff that survives merge from upstream StreamVault | You want a clean `grep -r streamvault` returning nothing |
| The fork is for **sideload / private distribution** on a known device list | The fork is for **public distribution** (Play Store, where the applicationId becomes a public URL) |
| You are willing to live with `BuildConfig` generated under the upstream namespace (e.g. `com.streamvault.iptv.BuildConfig`) | You want `BuildConfig` to live at `app.ehtudo.iptv.BuildConfig` |
| You may want to merge future upstream StreamVault changes back in | The fork is a one-way fork — upstream merges are not planned |

If you are unsure, **default to skill #14**. The migration to skill #5 is always possible later; the inverse is a 700-file revert.

## Principle: `applicationId` ≠ Kotlin source package

The Android Gradle Plugin separates four distinct concepts that the project (and most Android tutorials) conflate:

1. **`namespace`** (`android { namespace = "..." }` in `app/build.gradle.kts`) — the package that owns `BuildConfig` and `R`. The Kotlin source files DO NOT need to live under this package; they just need to be reachable from the manifest FQCNs.
2. **`applicationId`** (`defaultConfig { applicationId = "..." }`) — the **public identity** of the installed app. What `pm list packages` shows, what shows up in the launcher, what `adb install` keys against.
3. **`package` attribute in `AndroidManifest.xml`** — deprecated; ignored at build time. What matters is `namespace` + the FQCN strings used inside the manifest (`<activity android:name="..."/>`).
4. **Kotlin/Java source `package` declarations** — pure code organisation. The compiler resolves them by the import statements, not by the directory layout.

A working, installable, branded APK only requires (1) and (2) to be set to the reseller identity. (3) is a no-op since AGP 7. (4) is whatever the upstream author chose.

This is the entire trick that lets skill #14 exist.

## Pre-flight

```bash
# Confirm clean state and which base you are on
git status --short
git branch --show-current
git log --oneline -5
```

Two valid starting points:

- **`master`** (the original StreamVault, `com.streamvault.*` packages). You will keep the upstream packages; only `applicationId` and user-facing metadata change.
- **`ehiptv/custom-and-simplify`** (the already-renamed base, `app.ehtudo.*` packages). You are already at `app.ehtudo.iptv`; this skill only matters if you want to fork **another** reseller from this base.

The rest of this skill assumes you are starting from `master` and producing `app.ehtudo.iptv` for the first time. If you are on the already-renamed branch, skip steps 1 and 6.

## Step 1 — `applicationId` and `namespace` in `app/build.gradle.kts`

This is the only Gradle change required. The other modules' `namespace` (`data`, `domain`, `player`) are irrelevant for the user-facing identity — keep them as upstream.

`app/build.gradle.kts`:

```kotlin
android {
    namespace = "app.ehtudo.iptv"             // where BuildConfig and R are generated
    defaultConfig {
        applicationId = "app.ehtudo.iptv"      // PUBLIC identity — what users see
        versionCode = 17
        versionName = "1.0.16"
        buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"app.ehtudo.iptv\"")
        // ... the rest of upstream defaultConfig is unchanged
    }
}
```

Notes:
- The `applicationIdSuffix = ".debug"` on the `debug` build type (line 86) makes the debug install read as `app.ehtudo.iptv.debug`. That is correct — keep it.
- The `beta` suffix is `.beta`, making beta install as `app.ehtudo.iptv.beta`. Also correct — keep it.
- Do **not** touch the `release` `signingConfig` block. The signing material in `local.properties` is what binds the APK to the device fleet.

The Kotlin source files are still under `app/src/main/java/com/streamvault/iptv/...` (if starting from `master`). They keep working unchanged because nothing in the code references `applicationId` directly — the only reference is `BuildConfig.APPLICATION_ID`, which the compiler resolves through `namespace`.

## Step 2 — Brand strings in `res/values/strings.xml`

`app/src/main/res/values/strings.xml`:

```xml
<string name="app_name">Eh! IPTV</string>
<string name="welcome_brand_title">Eh! IPTV</string>
<string name="welcome_save">Salvar</string>
<!-- ... all other strings stay as upstream; only the user-visible brand name is changed -->
```

Reference the strings by resource (`R.string.welcome_brand_title`) from composables — never hardcode the brand name in Kotlin. This is the same convention skill #1 documents.

If you want per-locale variants of the brand name (e.g. `values-pt-rBR/strings.xml`), drop a `strings.xml` override in the locale folder.

## Step 3 — Xtream server URL

Pick **one** of the following two patterns. They are not compatible — pick the pattern that matches the operational model.

### Pattern A — Compile-time default (skill #3)

`app/src/main/java/com/streamvault/iptv/ui/screens/welcome/WelcomeScreen.kt` — there is a constant near the top of the file:

```kotlin
internal const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
```

`app/src/main/java/com/streamvault/iptv/ui/screens/provider/ProviderSetupScreen.kt` — same constant, must stay in sync (skill #3 documents the constraint):

```kotlin
internal const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
```

And the default provider name in both files:

```kotlin
private const val DEFAULT_PROVIDER_NAME = "Eh! IPTV"
```

This pattern is fine if the reseller URL never changes after release. An APK rebuild is required to change the URL.

### Pattern B — Remote config with cache and fallback (skill #11) — RECOMMENDED

Apply skill #11 (`dynamic-xtream-server-url.md`) on top of the upstream. The URL is then resolved at runtime in this order:

1. `https://ehtudo.app/iptv-config.json` (HTTPS GET, 3 s timeout). Operator-hosted. The `version` field bumps when mirrors swap.
2. DataStore cache (last accepted URL, invalidated when remote `version` increases).
3. The `HARDCODED_XTREAM_URL` from pattern A above, kept as a cold fallback.

Operator changes the URL by editing the JSON and bumping `version`. **No APK rebuild required.**

Skill #11 also adds the `BuildConfig.XTREAM_REMOTE_CONFIG_URL` field, with a `debug`-only override via `xtream.dev.remoteConfigUrl` in `local.properties`. Keep this override pattern — it lets the dev sideload a different config without touching release.

## Step 4 — Visual brand: accent colors + launcher art

### Accent colors (skill #10)

`app/src/main/java/com/streamvault/iptv/ui/theme/AppColors.kt` — the `Brand*` color family drives focus rings, links, selected pills, card borders, and badges. Three lines change:

```kotlin
val BrandStrong = Color(0xFF1E88E5)   // primary button background, top-bar focus
val Brand       = Color(0xFF42A5F5)   // link text, WhatsApp link, secondary highlights
val BrandDim    = Color(0xFF1565C0)   // pressed/hover state
```

Skill #10 documents the exact line numbers and provides worked palette examples (orange, red, purple, teal, pink). Do **not** change the semantic colors (success/warn/danger/info) — only `Brand*`.

### Launcher art and welcome background (skill #13)

The launcher icon, TV banner, and welcome background are regenerated from one canonical PNG. Skill #13 ships a Python script:

```bash
# from repo root, with Python + Pillow installed
python tools/regen_launcher_art.py --source assets/ic_launcher_vault_art.png \
    --out app/src/main/res
```

This regenerates the Android adaptive icon layers, the TV banner (`banner.xml` / `ic_tv_banner.png`), the welcome background, and the Play Store icon at 512×512. No code changes.

## Step 5 — Top navigation (skill #8)

The top-nav rail tabs (Home, Downloads, Plugins, …) are **runtime-configurable** via the in-app dialog **Settings → Navegação superior**. The order and visibility are persisted in DataStore and applied by `SettingsViewModel`. There is no hardcoded `defaultOrder` list to edit.

To change the default set shipped to new users, edit `SettingsRepository.kt` / `SettingsViewModel.kt` to seed the DataStore with the desired tab order on first launch. Skill #8 documents the exact call-sites and provides a worked example.

## Step 6 — Verify and install

```bash
# Wipe any pre-existing installs under the OLD applicationId (if you previously
# had StreamVault installed as com.streamvault.iptv.*)
adb -s d1d1b8f3 uninstall com.streamvault.iptv        # release
adb -s d1d1b8f3 uninstall com.streamvault.iptv.debug  # debug
adb -s d1d1b8f3 uninstall com.streamvault.iptv.beta   # beta

# Build
./gradlew :app:assembleDebug --no-daemon

# Install — note this is a CLEAN install because the applicationId changed
adb -s d1d1b8f3 install app/build/outputs/apk/debug/app-debug.apk

# Launch
adb -s d1d1b8f3 shell am start -n app.ehtudo.iptv.debug/com.streamvault.iptv.MainActivity
```

**Critical:** the launch command's component is `app.ehtudo.iptv.debug` (the new applicationId) but the activity class is still at `com.streamvault.iptv.MainActivity` (the upstream source package). This asymmetry is the whole point of the skill — it works because Android resolves components by their FQCN, and the FQCN is declared in the manifest against the namespace, not the applicationId.

If the activity fails to launch with `ClassNotFoundException`, the manifest's `<activity android:name>` is still pointing at the old FQCN with a typo. Fix the manifest string — do **not** rename the source package.

## What this skill does NOT cover (must combine with others)

The metadata-only path handles the **visible identity**. It does not handle the simplification logic. To produce the full "2-field welcome / no Advanced / best-effort activation" reseller experience, you also need:

| Concern | Skill |
|---|---|
| Welcome screen 2-field form, no password mask | #1 `simplify-welcome-onboarding` |
| Hide URL, playlist name, `AdvancedProviderOptionsSection` for Xtream | #2 `simplify-provider-setup-screen` |
| Best-effort provider activation (don't block entry on full sync) | #4 `best-effort-provider-activation` |
| Disable `StreamVaultTvInputService` (no Live Channels TV-input registration) | #12 `disable-tv-input-service` |
| Master checklist to apply all simplifications in order | #6 `iptv-reseller-simplification-checklist` |
| How to install on the Xiaomi POCO (MIUI "Install via USB" gate) | #7 `testing-on-xiaomi-miui-device` |

Each of these still touches code. The "metadata-only" framing is honest about which user-visible knobs are 100% config and which still require small code changes.

## When to upgrade to skill #5

You started with skill #14. The following signals mean you should bite the bullet and apply skill #5 on top:

- You are publishing to **Google Play** and want the applicationId to match the visible brand. Play Store URLs use the applicationId, and reviewers grep the source.
- A **new contributor** is joining and will be confused by `com.streamvault.iptv` packages shipping as `app.ehtudo.iptv`.
- You are forking a **second reseller** and want both to share an internal namespace so the merge is sane.
- The reseller's brand is going to be marketed under the `app.ehtudo.iptv` name in their own marketing — having `StreamVault` show up in a stack trace is a brand risk.

The migration is: apply skill #5 in its own commit on its own branch (`ehiptv/rename-packages`), then rebase the working branch. The reverse is not possible (skill #5 cannot be cleanly reverted once commits land on top).

## Anti-patterns (do not)

- **Do not** rename the `:app` module's `applicationId` to anything other than `app.ehtudo.iptv` if the goal is to match the production Xiaomi install. Different suffixes cause `adb install -r` failures and Play Store rejection.
- **Do not** also rename the `namespace` to the upstream package (e.g. `namespace = "com.streamvault.iptv"` while `applicationId = "app.ehtudo.iptv"`). It works, but it puts `BuildConfig` at the upstream path while the app is branded differently — needless cognitive load.
- **Do not** edit the upstream `com.streamvault.iptv.*` Kotlin packages to add `app.ehtudo.iptv` aliases. Either keep them as upstream (this skill) or do the full rename (skill #5). Mixing is the worst of both worlds.
- **Do not** add a `providers` whitelist in the Xtream `iptv-config.json` that restricts the welcome screen to a single user account. The two-field form is meant to be universal; the Xtream server itself enforces account validity.
- **Do not** skip the `uninstall` step in Step 6. Android treats the old `com.streamvault.iptv.debug` install as a different app and will refuse to upgrade it; `install -r` returns `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- **Do not** commit the new icon PNGs from skill #13 without running an `aapt dump badging` check — the adaptive icon foreground/background layers must be present in all densities, or the launcher shows a blank square on some devices.
