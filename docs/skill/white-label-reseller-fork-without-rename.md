# Skill 14 â€” White-label reseller fork without mass package rename

## Goal

Produce the **Eh! IPTV** branded APK (`applicationId = "app.ehtudo.iptv"`) starting from a generic StreamVault base, **without applying the mechanical mass-rename in skill #5**. The Kotlin/Java source packages may stay as `com.streamvault.*` (or whatever the upstream namespace is); only the user-visible identity â€” package, brand strings, colors, launcher art, Xtream URL, navigation â€” is customised.

This skill is the **light-weight alternative** to skill #5. It exists because skill #5 touches ~700 files and pollutes the git diff, which is overkill when the goal is just "rebrand and ship one reseller".

## When to use this skill (vs. skill #5)

| Use skill #14 (this) whenâ€¦ | Use skill #5 (rename) whenâ€¦ |
|---|---|
| You ship **one** reseller (e.g. Eh! IPTV) and source hygiene is not a hard requirement | You ship the codebase itself and want `app.ehtudo.iptv` to appear in every Kotlin file |
| You want a small, reviewable diff that survives merge from upstream StreamVault | You want a clean `grep -r streamvault` returning nothing |
| The fork is for **sideload / private distribution** on a known device list | The fork is for **public distribution** (Play Store, where the applicationId becomes a public URL) |
| You are willing to live with `BuildConfig` generated under the upstream namespace (e.g. `com.streamvault.app.BuildConfig`) | You want `BuildConfig` to live at `app.ehtudo.iptv.BuildConfig` |
| You may want to merge future upstream StreamVault changes back in | The fork is a one-way fork â€” upstream merges are not planned |

If you are unsure, **default to skill #14**. The migration to skill #5 is always possible later; the inverse is a 700-file revert.

## Principle: `applicationId` â‰  Kotlin source package

The Android Gradle Plugin separates four distinct concepts that the project (and most Android tutorials) conflate:

1. **`namespace`** (`android { namespace = "..." }` in `app/build.gradle.kts`) â€” the package that owns `BuildConfig` and `R`. The Kotlin source files DO NOT need to live under this package; they just need to be reachable from the manifest FQCNs.
2. **`applicationId`** (`defaultConfig { applicationId = "..." }`) â€” the **public identity** of the installed app. What `pm list packages` shows, what shows up in the launcher, what `adb install` keys against.
3. **`package` attribute in `AndroidManifest.xml`** â€” deprecated; ignored at build time. What matters is `namespace` + the FQCN strings used inside the manifest (`<activity android:name="..."/>`).
4. **Kotlin/Java source `package` declarations** â€” pure code organisation. The compiler resolves them by the import statements, not by the directory layout.

A working, installable, branded APK only requires (1) and (2) to be set to the reseller identity. (3) is a no-op since AGP 7. (4) is whatever the upstream author chose.

This is the entire trick that lets skill #14 exist.

## Scalable architecture: `productFlavors`

For a **single** reseller, the steps below in "Step 1 / Step 2 / â€¦" are enough. For **multiple** resellers (or the expectation of a second one), move the per-reseller fields into a `productFlavors` block in `app/build.gradle.kts` and consume them via `BuildConfig`. This is the canonical path going forward.

### What becomes a flavor field

| Field | Reseller (Eh! IPTV) value | Notes |
|---|---|---|
| `applicationId` | `app.ehtudo.iptv` | One APK per reseller |
| `versionName` / `versionCode` | `1.0.16` / `17` | Per-flavor version is allowed; typically inherited from `defaultConfig` |
| `BRAND_NAME` | `"Eh! IPTV"` | Surfaces in `welcome_brand_title`, About, etc. |
| `XTREAM_DEFAULT_URL` | `"http://dnstv.top/"` | Consumed by `WelcomeScreen` / `ProviderSetupScreen` |
| `XTREAM_DEFAULT_PROVIDER_NAME` | `"Eh! IPTV"` | Same |
| `WHATSAPP_URL` | `"http://wa.me/+5511932055173"` | The `wa.me` link rendered below "Salvar" |
| `BRAND_PRIMARY_COLOR` | `"#FF6A1A"` | Hex string; parsed at runtime in `AppColors.kt` |
| `BRAND_SECONDARY_COLOR` | `"#FF8A3D"` | Same |
| `BRAND_DIM_COLOR` | `"#33FF6A1A"` | Same (with alpha) |
| `REMOTE_CONFIG_URL` | `"https://ehtudo.app/iptv-config.json"` | Operator-hosted; the URL skill #11 fetches |
| `SHOW_ADVANCED_OPTIONS` | `false` | `BuildConfig` boolean; `ProviderSetupScreen` reads to hide `AdvancedProviderOptionsSection` for Xtream |
| `ENABLE_TV_INPUT_SERVICE` | `false` | `BuildConfig` boolean; manifest `android:enabled` placeholder, removes the `<service>` from the merged manifest |

### The `app/build.gradle.kts` shape

```kotlin
android {
    namespace = "com.streamvault.app"     // upstream; do not rename
    compileSdk = 36

    defaultConfig {
        applicationId = "app.ehtudo.iptv"  // gets overridden per flavor
        minSdk = 25
        targetSdk = 36
        // buildConfigField defaults â€” overridden per flavor below
    }

    flavorDimensions += "brand"
    productFlavors {
        create("ehtudo") {
            dimension = "brand"
            applicationId = "app.ehtudo.iptv"
            versionName   = "1.0.16"
            versionCode   = 17
            buildConfigField("String",  "BRAND_NAME",                   "\"Eh! IPTV\"")
            buildConfigField("String",  "XTREAM_DEFAULT_URL",            "\"http://dnstv.top/\"")
            buildConfigField("String",  "XTREAM_DEFAULT_PROVIDER_NAME",  "\"Eh! IPTV\"")
            buildConfigField("String",  "WHATSAPP_URL",                  "\"http://wa.me/+5511932055173\"")
            buildConfigField("String",  "BRAND_PRIMARY_COLOR",           "\"#FF6A1A\"")
            buildConfigField("String",  "BRAND_SECONDARY_COLOR",         "\"#FF8A3D\"")
            buildConfigField("String",  "BRAND_DIM_COLOR",               "\"#33FF6A1A\"")
            buildConfigField("String",  "REMOTE_CONFIG_URL",             "\"https://ehtudo.app/iptv-config.json\"")
            buildConfigField("boolean", "SHOW_ADVANCED_OPTIONS",         "false")
            buildConfigField("boolean", "ENABLE_TV_INPUT_SERVICE",       "false")
        }
        // future reseller: add another create("otherBrand") { ... } block. Zero code changes.
    }
}
```

### How the code consumes the fields

The one-time refactor of the 7 files below reads from `BuildConfig` instead of the local constants / hardcoded literals. Each file changes **once**, not per reseller.

| File | Old value | New value |
|---|---|---|
| `WelcomeScreen.kt:80` | `private const val HARDCODED_XTREAM_URL` | `BuildConfig.XTREAM_DEFAULT_URL` |
| `WelcomeScreen.kt:81` | `private const val DEFAULT_PROVIDER_NAME` | `BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME` |
| `WelcomeScreen.kt` | `private const val EH_IPTV_WHATSAPP_URL` | `BuildConfig.WHATSAPP_URL` |
| `ProviderSetupScreen.kt:111` | same as WelcomeScreen | same |
| `AppColors.kt` | `val Brand = Color(0xFF69A8FF)` etc. | `val Brand = parseHexColor(BuildConfig.BRAND_PRIMARY_COLOR)` etc. |
| `ProviderSetupScreen.kt` | unconditional `AdvancedProviderOptionsSection(...)` call | `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { ... }` |
| `app/src/main/AndroidManifest.xml` | `<service ... />` for TV input | `<service ... android:enabled="${ENABLE_TV_INPUT_SERVICE}" />` |

### Per-reseller effort (after the refactor)

| Action | Effort |
|---|---|
| Add `create("newReseller") { ... }` block in `build.gradle.kts` | ~15 lines of Kotlin DSL |
| Create `app/src/newreseller/res/values/strings.xml` with `app_name` (optional â€” if you want the resource to win) | 3 lines XML |
| Run skill #13 to regenerate launcher art from `ic_launcher_<brand>.png` | 1 command |
| Publish `iptv-config.json` at the brand's domain | 1 PUT |
| **Total per new reseller** | ~20 lines + 1 image, **zero code** |

The `assembleEhtudoDebug` / `installEhtudoDebug` task names fall out of the flavor block automatically (Android Gradle Plugin convention: `<flavor><BuildType>`).

### When to use the simple path (this skill, steps 1â€“6 below) vs. the flavor path

- **1 reseller, no plans for a second:** use the simple path. The `applicationId` change + `strings.xml` + `AppColors.kt` is enough.
- **1 reseller now, possible N later:** do the simple path now, but leave a `// TODO: move to productFlavors when 2nd reseller appears` in `build.gradle.kts`. Migration is a clean extract â€” no breakage.
- **N resellers from day one:** skip steps 1â€“6 of the simple path; do the flavor refactor from the start.

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

## Step 1 â€” `applicationId` and `namespace` in `app/build.gradle.kts`

This is the only Gradle change required. The other modules' `namespace` (`data`, `domain`, `player`) are irrelevant for the user-facing identity â€” keep them as upstream.

`app/build.gradle.kts`:

```kotlin
android {
    namespace = "app.ehtudo.iptv"             // where BuildConfig and R are generated
    defaultConfig {
        applicationId = "app.ehtudo.iptv"      // PUBLIC identity â€” what users see
        versionCode = 17
        versionName = "1.0.16"
        buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"app.ehtudo.iptv\"")
        // ... the rest of upstream defaultConfig is unchanged
    }
}
```

Notes:
- The `applicationIdSuffix = ".debug"` on the `debug` build type (line 86) makes the debug install read as `app.ehtudo.iptv.debug`. That is correct â€” keep it.
- The `beta` suffix is `.beta`, making beta install as `app.ehtudo.iptv.beta`. Also correct â€” keep it.
- Do **not** touch the `release` `signingConfig` block. The signing material in `local.properties` is what binds the APK to the device fleet.

The Kotlin source files are still under `app/src/main/java/com/streamvault/app/...` (if starting from `master`). They keep working unchanged because nothing in the code references `applicationId` directly â€” the only reference is `BuildConfig.APPLICATION_ID`, which the compiler resolves through `namespace`.

## Step 2 â€” Brand strings in `res/values/strings.xml`

`app/src/main/res/values/strings.xml`:

```xml
<string name="app_name">Eh! IPTV</string>
<string name="welcome_brand_title">Eh! IPTV</string>
<string name="welcome_save">Salvar</string>
<!-- ... all other strings stay as upstream; only the user-visible brand name is changed -->
```

Reference the strings by resource (`R.string.welcome_brand_title`) from composables â€” never hardcode the brand name in Kotlin. This is the same convention skill #1 documents.

If you want per-locale variants of the brand name (e.g. `values-pt-rBR/strings.xml`), drop a `strings.xml` override in the locale folder.

## Step 3 â€” Xtream server URL

Pick **one** of the following two patterns. They are not compatible â€” pick the pattern that matches the operational model.

### Pattern A â€” Compile-time default (skill #3)

`app/src/main/java/com/streamvault/app/ui/screens/welcome/WelcomeScreen.kt` â€” there is a constant near the top of the file:

```kotlin
internal const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
```

`app/src/main/java/com/streamvault/app/ui/screens/provider/ProviderSetupScreen.kt` â€” same constant, must stay in sync (skill #3 documents the constraint):

```kotlin
internal const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
```

And the default provider name in both files:

```kotlin
private const val DEFAULT_PROVIDER_NAME = "Eh! IPTV"
```

This pattern is fine if the reseller URL never changes after release. An APK rebuild is required to change the URL.

### Pattern B â€” Remote config with cache and fallback (skill #11) â€” RECOMMENDED

Apply skill #11 (`dynamic-xtream-server-url.md`) on top of the upstream. The URL is then resolved at runtime in this order:

1. `https://ehtudo.app/iptv-config.json` (HTTPS GET, 3 s timeout). Operator-hosted. The `version` field bumps when mirrors swap.
2. DataStore cache (last accepted URL, invalidated when remote `version` increases).
3. The `HARDCODED_XTREAM_URL` from pattern A above, kept as a cold fallback.

Operator changes the URL by editing the JSON and bumping `version`. **No APK rebuild required.**

Skill #11 also adds the `BuildConfig.XTREAM_REMOTE_CONFIG_URL` field, with a `debug`-only override via `xtream.dev.remoteConfigUrl` in `local.properties`. Keep this override pattern â€” it lets the dev sideload a different config without touching release.

## Step 4 â€” Visual brand: accent colors + launcher art

### Accent colors (skill #10)

`app/src/main/java/com/streamvault/app/ui/theme/AppColors.kt` â€” the `Brand*` color family drives focus rings, links, selected pills, card borders, and badges. Three lines change:

```kotlin
val BrandStrong = Color(0xFF1E88E5)   // primary button background, top-bar focus
val Brand       = Color(0xFF42A5F5)   // link text, WhatsApp link, secondary highlights
val BrandDim    = Color(0xFF1565C0)   // pressed/hover state
```

Skill #10 documents the exact line numbers and provides worked palette examples (orange, red, purple, teal, pink). Do **not** change the semantic colors (success/warn/danger/info) â€” only `Brand*`.

### Launcher art and welcome background (skill #13)

The launcher icon, TV banner, and welcome background are regenerated from one canonical PNG. Skill #13 ships a Python script:

```bash
# from repo root, with Python + Pillow installed
python tools/regen_launcher_art.py --source assets/ic_launcher_vault_art.png \
    --out app/src/main/res
```

This regenerates the Android adaptive icon layers, the TV banner (`banner.xml` / `ic_tv_banner.png`), the welcome background, and the Play Store icon at 512Ã—512. No code changes.

## Step 5 â€” Top navigation (skill #8)

The top-nav rail tabs (Home, Downloads, Plugins, â€¦) are **runtime-configurable** via the in-app dialog **Settings â†’ NavegaÃ§Ã£o superior**. The order and visibility are persisted in DataStore and applied by `SettingsViewModel`. There is no hardcoded `defaultOrder` list to edit.

To change the default set shipped to new users, edit `SettingsRepository.kt` / `SettingsViewModel.kt` to seed the DataStore with the desired tab order on first launch. Skill #8 documents the exact call-sites and provides a worked example.

## Step 6 â€” Verify and install

```bash
# Wipe any pre-existing installs under the OLD applicationId (if you previously
# had StreamVault installed as com.streamvault.app.*)
adb -s d1d1b8f3 uninstall com.streamvault.app        # release
adb -s d1d1b8f3 uninstall com.streamvault.app.debug  # debug
adb -s d1d1b8f3 uninstall com.streamvault.app.beta   # beta

# Build
./gradlew :app:assembleDebug --no-daemon

# Install â€” note this is a CLEAN install because the applicationId changed
adb -s d1d1b8f3 install app/build/outputs/apk/debug/app-debug.apk

# Launch
adb -s d1d1b8f3 shell am start -n app.ehtudo.iptv.debug/com.streamvault.app.MainActivity
```

**Critical:** the launch command's component is `app.ehtudo.iptv.debug` (the new applicationId) but the activity class is still at `com.streamvault.app.MainActivity` (the upstream source package). This asymmetry is the whole point of the skill â€” it works because Android resolves components by their FQCN, and the FQCN is declared in the manifest against the namespace, not the applicationId.

If the activity fails to launch with `ClassNotFoundException`, the manifest's `<activity android:name>` is still pointing at the old FQCN with a typo. Fix the manifest string â€” do **not** rename the source package.

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
- A **new contributor** is joining and will be confused by `com.streamvault.app` packages shipping as `app.ehtudo.iptv`.
- You are forking a **second reseller** and want both to share an internal namespace so the merge is sane.
- The reseller's brand is going to be marketed under the `app.ehtudo.iptv` name in their own marketing â€” having `StreamVault` show up in a stack trace is a brand risk.

The migration is: apply skill #5 in its own commit on its own branch (`ehiptv/rename-packages`), then rebase the working branch. The reverse is not possible (skill #5 cannot be cleanly reverted once commits land on top).

## Anti-patterns (do not)

- **Do not** rename the `:app` module's `applicationId` to anything other than `app.ehtudo.iptv` if the goal is to match the production Xiaomi install. Different suffixes cause `adb install -r` failures and Play Store rejection.
- **Do not** also rename the `namespace` to the upstream package (e.g. `namespace = "com.streamvault.app"` while `applicationId = "app.ehtudo.iptv"`). It works, but it puts `BuildConfig` at the upstream path while the app is branded differently â€” needless cognitive load.
- **Do not** edit the upstream `com.streamvault.app.*` Kotlin packages to add `app.ehtudo.iptv` aliases. Either keep them as upstream (this skill) or do the full rename (skill #5). Mixing is the worst of both worlds.
- **Do not** add a `providers` whitelist in the Xtream `iptv-config.json` that restricts the welcome screen to a single user account. The two-field form is meant to be universal; the Xtream server itself enforces account validity.
- **Do not** skip the `uninstall` step in Step 6. Android treats the old `com.streamvault.app.debug` install as a different app and will refuse to upgrade it; `install -r` returns `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- **Do not** commit the new icon PNGs from skill #13 without running an `aapt dump badging` check â€” the adaptive icon foreground/background layers must be present in all densities, or the launcher shows a blank square on some devices.
