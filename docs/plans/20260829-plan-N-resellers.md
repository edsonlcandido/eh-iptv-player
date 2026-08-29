# Plan — Prepare the Eh! IPTV player codebase for N resellers

> **Status:** Plan mode draft. Do not implement until the user approves via `ExitPlanMode`.
> **Target path (after approval):** `D:\repos\edsonlcandido\eh-iptv-player\docs\plans\20260829-plan-N-resellers.md`
> **Source branch:** `feature/start-custom-and-simplify-1-0-17-1` (already created by the user, based on `f86d4aee` from upstream `Davidona/develop`).

## Base and target (read this first)

- **The base is upstream StreamVault**, not the Eh! IPTV fork. The current `feature/start-custom-and-simplify-1-0-17-1` branch has:
  - `namespace = "com.streamvault.app"`
  - `applicationId = "com.streamvault.app"`
  - `versionName = "1.0.17.1"` / `versionCode = 19`
  - All Kotlin sources under `com.streamvault.app.*` packages
  - `Brand` / `BrandMuted` / `BrandStrong` as `val Color(0xFF...)` literals in `AppColors.kt`
  - The upstream multi-step welcome screen (not the simplified 2-field form)
  - An unconditional `<service>` for the TV input feature in `AndroidManifest.xml`
  - **No** `HARDCODED_XTREAM_URL` / `DEFAULT_PROVIDER_NAME` constants in any Kotlin file
  - **No** `productFlavors` block in `app/build.gradle.kts`
  - **No** `BRAND_*` / `XTREAM_DEFAULT_*` / `WHATSAPP_URL` `buildConfigField` entries
- **The target is the N-resellers architecture** with **Eh! IPTV as the first flavor** (the `ehtudo` block in `productFlavors`). The Eh! IPTV APK emerges from `assembleEhtudoDebug` / `assembleEhtudoRelease`. There is no Eh! IPTV build on the current branch yet — it is created by this plan.
- **The skills in `docs/skill/` are the documentation of the target.** They were written against the `ehiptv/custom-and-simplify` branch (which already has the simplified welcome screen, the renamed packages, and the `HARDCODED_XTREAM_URL` constants). This plan adapts the skill content to the actual state of `feature/start-custom-and-simplify-1-0-17-1`, which is upstream StreamVault.

## Summary

This plan converts the documentation into a concrete refactor on the StreamVault base: add a `productFlavors` block, introduce 11 `buildConfigField` constants, refactor 7 source files to consume them, and add a manifest placeholder for the optional TV input service. The end state is one APK per reseller brand, with **zero code change per new reseller** — only a new `create("...") { ... }` block in `app/build.gradle.kts` and asset regeneration (skill #13). The first APK that emerges is `app.ehtudo.iptv` from the `ehtudo` flavor.

**Key design decisions (recorded for the implementer):**

1. **Do not rename packages.** Stay on `com.streamvault.app.*` Kotlin sources. `applicationId` is independent of `namespace`, so the rebranded APK ships as `app.ehtudo.iptv` (or any other ID) without touching imports. Skill #5 (mass rename) is **deprecated** for this plan.
2. **One `productFlavors` block, one dimension: `brand`.** Future resellers add `create("otherReseller") { ... }` inside the same block. The Android Gradle Plugin generates `assembleOtherResellerDebug` / `installOtherResellerDebug` tasks automatically.
3. **`buildConfigField` is the single source of truth for brand identity.** 11 fields, all typed (`String` or `boolean`), all defined per-flavor. Code reads `BuildConfig.BRAND_NAME` etc. — no `private const val` for brand data in any Kotlin file.
4. **TV input service stays in source, gated by manifest placeholder.** `${ENABLE_TV_INPUT_SERVICE}` resolves from `BuildConfig.ENABLE_TV_INPUT_SERVICE` at manifest-merge time. Default `false` for the `ehtudo` flavor; flip to `true` if a reseller exposes a real feed.
5. **Advanced options hidden by a `BuildConfig` boolean.** `SHOW_ADVANCED_OPTIONS = false` causes `ProviderSetupScreen` to skip the `AdvancedProviderOptionsSection` composable entirely for the Xtream branch. Skill #2's manual UI hiding becomes one boolean per flavor.
6. **One-time refactor: pre-fill + gate, not literal-replacement.** Because the upstream code has form fields (not constants), the refactor uses `ifBlank { BuildConfig.XTREAM_DEFAULT_URL }` fallbacks at the Xtream call sites, and wraps the "Server URL" / "Playlist Name" / `AdvancedProviderOptionsSection` rendering in `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { ... }`. The form still accepts user input on flavors where advanced options are enabled (`SHOW_ADVANCED_OPTIONS = true`); the BuildConfig values are the default when the form is empty. No business logic moves.

## Context

- **Why this plan exists.** The user is preparing to ship `app.ehtudo.iptv` as the Eh! IPTV reseller fork. They want the codebase to support N resellers going forward (skill #14's "Scalable architecture: `productFlavors`" path), with each new reseller requiring only metadata changes (config block + assets), not code changes.
- **What is already in place (from this session).**
  - Skill #14 created and updated at `docs/skill/white-label-reseller-fork-without-rename.md` (19.8 KB). Documents the `productFlavors` pattern, the 11 `buildConfigField` list, the 7 files that consume them, and the per-reseller effort.
  - Skills #3 (`hardcode-xtream-server-defaults.md`) and #5 (`package-rename-streamvault-to-ehtudo.md`) marked **DEPRECATED** with banners pointing to skill #14.
  - Skills #6 (checklist), #9 (customisation surfaces), #10 (colors), #12 (TV input), #14 itself updated to reference the new `BuildConfig.*` fields.
  - `docs/skill/README.md` index updated (rows #3 and #5 struck through, "Order of operations" expanded).
- **What is NOT yet in place (the gap this plan fills).**
  - No `productFlavors` block in `app/build.gradle.kts`.
  - No `BRAND_NAME`, `XTREAM_DEFAULT_URL`, `BRAND_PRIMARY_COLOR`, etc. in `BuildConfig`.
  - No brand data of any kind (no `private const val` literals, no remote config, no DataStore). The `ehiptv/custom-and-simplify` branch introduced such constants, but they are **not present** on `feature/start-custom-and-simplify-1-0-17-1`. The first task is to *introduce* the brand data via `buildConfigField` so consumer code can be wired to read it.
  - `AppColors.Brand` / `BrandMuted` / `BrandStrong` are `val Color(0xFF...)` literals at `app/src/main/java/com/streamvault/app/ui/design/AppColors.kt:13-15` (the upstream StreamVault blue palette).
  - TV input `<service>` at `app/src/main/AndroidManifest.xml:145-158` is unconditional; needs an `android:enabled` placeholder.

## Goals

1. **`app.ehtudo.iptv` ships as a flavor** of the upstream StreamVault codebase, without renaming any Kotlin file. The Eh! IPTV build emerges from the new `ehtudo` flavor; no Eh! IPTV build exists on the current branch before this plan.
2. **Adding a new reseller is a 5-minute job** — copy the `create("ehtudo") { ... }` block, change ~15 values, run skill #13 for the icons, publish `iptv-config.json`. No source code touched.
3. **The Eh! IPTV APK at `versionName 1.0.17-1`** installs on the Xiaomi POCO M2012K11AG with the orange Eh! IPTV palette. The welcome screen still shows the upstream multi-step form (this plan does not apply skill #1), but the "Server URL" and "Playlist Name" fields are hidden by `BuildConfig.SHOW_ADVANCED_OPTIONS = false` and the form auto-fills from `BuildConfig.XTREAM_DEFAULT_URL` / `BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME` when blank.
4. **All skill #3 / skill #5 simplifications that the user might apply later** (welcome screen 2-field form, package rename, etc.) keep working — the new `BuildConfig` reads don't conflict with them; they sit at a lower layer.
5. **The build is reproducible** — `./gradlew :app:assembleEhtudoDebug --no-daemon` produces a working APK; no manual file edits per build.

## Non-goals (explicit "do not" list)

- **Do not rename** any Kotlin package, import, or directory. `com.streamvault.app.*` stays as the source namespace. `applicationId` becomes `app.ehtudo.iptv` (or whatever) via the flavor.
- **Do not refactor business logic.** No ViewModel rewrites, no repository changes, no use-case signature changes. The only Kotlin changes are reading `BuildConfig.*` where the upstream code currently has its own defaults / user input.
- **Do not delete the TV input service code** (skill #12 Approach B is not used). Approach A (manifest placeholder) is the canonical path. Code stays in tree; `<service>` is omitted from the merged manifest when `ENABLE_TV_INPUT_SERVICE = false`.
- **Do not change `compileSdk` / `minSdk` / `targetSdk`** (25 / 36).
- **Do not introduce a new dependency.** All work is in `app/build.gradle.kts`, `AndroidManifest.xml`, and the existing `app/src/main/...` Kotlin files.
- **Do not add a "switch reseller at runtime" feature.** Each reseller is a separate flavor → separate APK. Multi-reseller in one install is out of scope.
- **Do not apply the welcome screen simplification (skill #1).** The upstream multi-step welcome flow stays in place for now. The refactor introduces `BuildConfig.XTREAM_DEFAULT_URL` as a *default* that the welcome screen falls back to (or that pre-fills the URL field), but the form structure is unchanged. Skill #1 is a follow-up that flattens the form to 2 fields and is independent of this plan. The user can apply it in a later commit.
- **Do not migrate the `ehiptv/custom-and-simplify` branch's hardcoded constants forward.** The plan applies cleanly to the *current* `feature/start-custom-and-simplify-1-0-17-1` branch state (no constants yet, upstream StreamVault). If the user later merges `ehiptv/custom-and-simplify` into the new branch, skill #14's migration note applies; that merge is out of scope here.
- **Do not reference, pull from, or merge `ehiptv/custom-and-simplify`.** That branch is a **parallel pre-existing direction** — it has already done a mass package rename (`com.streamvault.*` → `app.ehtudo.*`), introduced a simplified 2-field welcome form, and added hardcoded `HARDCODED_XTREAM_URL` / `DEFAULT_PROVIDER_NAME` / `EH_IPTV_WHATSAPP_URL` `private const val` literals. It is **not** the source of truth for this plan and must be **ignored**. The base for this plan is the **`master`** branch (Davidona/develop `f86d4aee` from upstream StreamVault), which has none of those customizations. The current working branch `feature/start-custom-and-simplify-1-0-17-1` is on top of `master` with only documentation commits — the production code is identical to `master`. **Always branch from `master`**, not from `ehiptv/custom-and-simplify` or any renamed/simplified fork; the `productFlavors` + `BuildConfig` architecture introduced by this plan replaces the constant-injection approach used elsewhere. Do not merge from `ehiptv/custom-and-simplify`, do not import its constants, and do not treat it as a starting point — the `productFlavors` + `BuildConfig` architecture introduced by this plan replaces the constant-injection approach that branch uses.

## Current implementation (what the implementer starts from)

Working tree: `feature/start-custom-and-simplify-1-0-17-1`, based on upstream `f86d4aee` (Davidona/develop). Clean working tree. No `productFlavors`. No `BRAND_*` BuildConfig fields. The branch is **upstream StreamVault** — no Eh! IPTV customization has been applied yet.

### Key file inventory

| Path | Current state | What this plan changes |
|---|---|---|
| `app/build.gradle.kts` | `namespace = "com.streamvault.app"`, `applicationId = "com.streamvault.app"`, `versionName = "1.0.17.1"`, `versionCode = 19`. No `flavorDimensions`, no `productFlavors`. Existing `OFFICIAL_APPLICATION_ID`, `OFFICIAL_SIGNING_CERT_SHA256`, `APP_UPDATE_CHANNEL`, `BUILD_TIMESTAMP_UTC`, `XTREAM_DEV_*`, `M3U_DEV_*` `buildConfigField` entries. | Add `flavorDimensions += "brand"` and `productFlavors { create("ehtudo") { ... } }` block with 11 new `buildConfigField` entries. The default `applicationId` is overridden by the flavor. |
| `app/src/main/java/com/streamvault/app/ui/design/AppColors.kt` | `val Brand = Color(0xFF69A8FF)`, `val BrandMuted = Color(0x335FA4FF)`, `val BrandStrong = Color(0xFF8BBCFF)` (lines 13–15). | Replace the three `val Color(0xFF...)` literals with `parseHexColor(BuildConfig.BRAND_PRIMARY_COLOR)` / `BRAND_SECONDARY_COLOR` / `BRAND_DIM_COLOR`. Add the `parseHexColor` helper. The 11-line surrounding palette (Canvas, Surface, Text, Live, Success, Warning, Info) stays untouched. |
| `app/src/main/java/com/streamvault/app/ui/screens/welcome/WelcomeScreen.kt` | The upstream multi-step welcome flow. Has at least one "Server URL" text field, a "Username" field, a "Password" field, and a button that calls a `loginXtream(serverUrl, username, password, ...)`-style function. The `serverUrl` is currently `var serverUrl by rememberSaveable { mutableStateOf("") }` (empty default) or a hardcoded `""` literal. | **Read the file end-to-end first** (acceptance criterion #1). At the call site that builds the `XtreamProviderSetupCommand`, fall back to `BuildConfig.XTREAM_DEFAULT_URL` when the local `serverUrl` is blank. Same for `name` → `BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME`. If the file has a `EH_IPTV_WHATSAPP_URL` constant, replace it with `BuildConfig.WHATSAPP_URL`; otherwise introduce the BuildConfig read at the `Intent.ACTION_VIEW` URL. |
| `app/src/main/java/com/streamvault/app/ui/screens/provider/ProviderSetupScreen.kt` | The upstream multi-provider screen. Has a `when (sourceType) { ... }` over Xtream / Stalker / M3U / Jellyfin branches. Each branch builds an `XtreamProviderSetupCommand` (or equivalent) with the form's `serverUrl` and `name`. The Xtream branch calls `AdvancedProviderOptionsSection(...)`. | (a) At the Xtream call site, fall back to `BuildConfig.XTREAM_DEFAULT_URL` / `BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME` when the local form fields are blank. (b) Wrap the `AdvancedProviderOptionsSection(...)` call in `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { ... }`. Other branches (Stalker, M3U, Jellyfin) are unchanged. |
| `app/src/main/java/com/streamvault/app/ui/screens/settings/SettingsProviderSection.kt` | The upstream Settings → Provedores section. May or may not have a `EH_IPTV_WHATSAPP_URL` constant — grep the file first. | If the WhatsApp link exists, replace its URL constant with `BuildConfig.WHATSAPP_URL`. If it does not exist on this branch, no change (the welcome screen is the only entry point for the support link on the Eh! IPTV build). |
| `app/src/main/AndroidManifest.xml` | Lines 145–158: unconditional `<service android:name=".tvinput.StreamVaultTvInputService" ...>`. | Add `android:enabled="${ENABLE_TV_INPUT_SERVICE}"` to the `<service>` element. No other manifest change. |
| `app/src/main/java/app/ehtudo/iptv/data/config/RemoteConfigRepository.kt` (skill #11) | Not present on this branch (skill #11 is in the simplification branch). | Out of scope for this plan. When skill #11 lands, it reads `BuildConfig.REMOTE_CONFIG_URL`. |
| `docs/plans/20260829-plan-N-resellers.md` | Does not exist. | Created from the canonical plan file at the end of implementation. |

### Refactor strategy (StreamVault base, not the simplified branch)

This plan does **not** assume the `HARDCODED_XTREAM_URL` / `DEFAULT_PROVIDER_NAME` constants from the simplification branch exist. The current branch has the upstream code, where:

- The welcome screen has a "Server URL" text field. The user can type a URL. The form sends whatever is in the field. The default is `""` (empty), which would fail `validateXtream.serverUrl.isBlank()`.
- The provider setup screen similarly has a "Server URL" field. The Xtream branch sends whatever is in the field.

The refactor changes this so that:

- When the local `serverUrl` field is **empty**, the Xtream command uses `BuildConfig.XTREAM_DEFAULT_URL` instead of `""`. This makes the form work out-of-the-box on the `ehtudo` flavor without forcing the user to type a URL.
- When the local `serverUrl` field is **non-empty**, the user's typed value is sent (advanced users can still override). This is gated behind `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { /* show the URL field */ }` — if `SHOW_ADVANCED_OPTIONS = false` (the default for `ehtudo`), the URL field is hidden and the BuildConfig value is always used.
- Same pattern for `name` → `BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME`.

This is a **pre-fill + hide** pattern, not a "replace literal" pattern. The 7-file table below is reframed accordingly.

### `applicationId` and the debug suffix

- `applicationIdSuffix = ".debug"` in `buildTypes.debug`. The flavor `ehtudo` produces `app.ehtudo.iptv` for release and `app.ehtudo.iptv.debug` for debug. The `beta` build type adds `.beta` similarly.
- **Important:** when an APK with a new `applicationId` is installed, Android treats it as a different app. There is no `install -r` upgrade path from `com.streamvault.app.debug` to `app.ehtudo.iptv.debug` — the user must `adb uninstall com.streamvault.app.debug` first. The verification step in skill #14 documents this. Any saved providers / preferences / parental PIN are lost in the transition; this is acceptable because the old install was StreamVault upstream (a different product) and the new one is Eh! IPTV.

## Proposed architecture

### `app/build.gradle.kts` — `productFlavors` block

```kotlin
android {
    namespace = "com.streamvault.app"     // keep upstream namespace; do not rename
    compileSdk = 36

    defaultConfig {
        applicationId = "app.ehtudo.iptv" // base; can be overridden per-flavor
        minSdk = 25
        targetSdk = 36
        versionCode = 19                  // bumped per release
        versionName = "1.0.17-1"          // reseller version; can differ from upstream

        // Existing OFFICIAL_APPLICATION_ID, OFFICIAL_SIGNING_CERT_SHA256, etc.
        // ... (unchanged)
    }

    flavorDimensions += "brand"
    productFlavors {
        create("ehtudo") {
            dimension = "brand"
            applicationId = "app.ehtudo.iptv"
            versionName   = "1.0.17-1"
            versionCode   = 19

            // Identity
            buildConfigField("String",  "BRAND_NAME",                   "\"Eh! IPTV\"")
            buildConfigField("String",  "WHATSAPP_URL",                 "\"http://wa.me/+5511932055173\"")

            // Xtream defaults
            buildConfigField("String",  "XTREAM_DEFAULT_URL",           "\"http://dnstv.top/\"")
            buildConfigField("String",  "XTREAM_DEFAULT_PROVIDER_NAME", "\"Eh! IPTV\"")

            // Brand colors (hex strings, parsed at runtime — see AppColors.kt below)
            buildConfigField("String",  "BRAND_PRIMARY_COLOR",          "\"#FF6A1A\"")   // AppColors.Brand
            buildConfigField("String",  "BRAND_SECONDARY_COLOR",        "\"#FF8A3D\"")   // AppColors.BrandStrong
            buildConfigField("String",  "BRAND_DIM_COLOR",              "\"#33FF6A1A\"") // AppColors.BrandMuted

            // Remote config (skill #11)
            buildConfigField("String",  "REMOTE_CONFIG_URL",            "\"https://ehtudo.app/iptv-config.json\"")

            // Feature flags
            buildConfigField("boolean", "SHOW_ADVANCED_OPTIONS",        "false")
            buildConfigField("boolean", "ENABLE_TV_INPUT_SERVICE",      "false")
        }

        // Future resellers: create("otherBrand") { dimension = "brand"; applicationId = ...; buildConfigField(...) }
    }

    buildTypes {
        // ... existing debug/beta/release blocks unchanged.
        // Note: applicationIdSuffix = ".debug" in debug and ".beta" in beta
        //       compose with the flavor applicationId, yielding
        //       app.ehtudo.iptv.debug and app.ehtudo.iptv.beta.
    }
}
```

### 11 `BuildConfig` fields — single source of truth

| Field | Type | Eh! IPTV value | Consumer file(s) |
|---|---|---|---|
| `BRAND_NAME` | `String` | `"Eh! IPTV"` | `WelcomeScreen.kt`, `strings.xml` (resource override) |
| `WHATSAPP_URL` | `String` | `"http://wa.me/+5511932055173"` | `WelcomeScreen.kt`, `SettingsProviderSection.kt` |
| `XTREAM_DEFAULT_URL` | `String` | `"http://dnstv.top/"` | `WelcomeScreen.kt`, `ProviderSetupScreen.kt` (Xtream branch, both layouts) |
| `XTREAM_DEFAULT_PROVIDER_NAME` | `String` | `"Eh! IPTV"` | `WelcomeScreen.kt`, `ProviderSetupScreen.kt` |
| `BRAND_PRIMARY_COLOR` | `String` (hex) | `"#FF6A1A"` | `AppColors.kt` → `Brand` |
| `BRAND_SECONDARY_COLOR` | `String` (hex) | `"#FF8A3D"` | `AppColors.kt` → `BrandStrong` |
| `BRAND_DIM_COLOR` | `String` (hex) | `"#33FF6A1A"` | `AppColors.kt` → `BrandMuted` |
| `REMOTE_CONFIG_URL` | `String` | `"https://ehtudo.app/iptv-config.json"` | Skill #11's `RemoteConfigRepository.kt` (when present) |
| `SHOW_ADVANCED_OPTIONS` | `boolean` | `false` | `ProviderSetupScreen.kt` (gates `AdvancedProviderOptionsSection`) |
| `ENABLE_TV_INPUT_SERVICE` | `boolean` | `false` | `AndroidManifest.xml` (manifest placeholder) |

### One-time refactor of 7 files

Each file is touched **once**. After the refactor, no Kotlin file has a hardcoded brand-specific URL, name, or color — all reads go through `BuildConfig.*`. The refactor pattern is **pre-fill + gate**, not literal-replacement, because the upstream code has form fields and runtime defaults, not constants.

| # | File | Upstream behavior (today) | Refactored behavior (after) |
|---|---|---|---|
| 1 | `app/build.gradle.kts` | `namespace = "com.streamvault.app"`, no `productFlavors` | add `flavorDimensions += "brand"` + `productFlavors { create("ehtudo") { ... } }` with 11 `buildConfigField` values. `namespace` stays as upstream. `applicationId` is overridden by the flavor. |
| 2 | `app/src/main/java/com/streamvault/app/ui/design/AppColors.kt` | `val Brand = Color(0xFF69A8FF)` (line 13), `BrandMuted` (line 14), `BrandStrong` (line 15) as `val Color(0xFF...)` literals | `val Brand = parseHexColor(BuildConfig.BRAND_PRIMARY_COLOR)`. Same pattern for `BrandMuted` and `BrandStrong`. Add the `parseHexColor` helper at the bottom of the file. |
| 3 | `app/src/main/java/com/streamvault/app/ui/screens/welcome/WelcomeScreen.kt` | The welcome form has a `var serverUrl by rememberSaveable { mutableStateOf("") }`. The Xtream login sends `serverUrl = state.serverUrl` (or similar). If empty, validation fails. | (a) At the call site that builds `XtreamProviderSetupCommand`, fall back: `serverUrl = state.serverUrl.ifBlank { BuildConfig.XTREAM_DEFAULT_URL }`. Same for `name = state.name.ifBlank { BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME }`. (b) The "Server URL" and "Playlist Name" form fields are wrapped in `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { ... }` — hidden on the `ehtudo` flavor. (c) The WhatsApp link target reads from `BuildConfig.WHATSAPP_URL` (introduce the import). |
| 4 | `app/src/main/java/com/streamvault/app/ui/screens/provider/ProviderSetupScreen.kt` | The Xtream branch of the `when (sourceType)` builds an `XtreamProviderSetupCommand` with the form's `serverUrl` and `name`. `AdvancedProviderOptionsSection(...)` is called unconditionally for the Xtream branch. | (a) Same `ifBlank` fallback as the welcome screen for `serverUrl` / `name`. (b) Wrap `AdvancedProviderOptionsSection(...)` in `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { ... }`. Other branches (Stalker, M3U, Jellyfin) are unchanged. (c) Introduce the BuildConfig import. |
| 5 | `app/src/main/java/com/streamvault/app/ui/screens/settings/SettingsProviderSection.kt` | (Upstream — verify by reading the file. May or may not have a `EH_IPTV_WHATSAPP_URL` constant.) | If the WhatsApp link exists, replace its URL constant with `BuildConfig.WHATSAPP_URL`. If absent, no change on this branch. |
| 6 | `app/src/main/AndroidManifest.xml` | Lines 145–158: unconditional `<service android:name=".tvinput.StreamVaultTvInputService" ...>` | Add `android:enabled="${ENABLE_TV_INPUT_SERVICE}"` to the `<service>` element. No other manifest change. |
| 7 | `app/src/test/java/com/streamvault/app/ui/design/AppColorsParseTest.kt` (new file) | (does not exist) | Unit tests for `parseHexColor` covering: 6-digit hex (`#FF6A1A`), 8-digit hex with alpha (`#33FF6A1A`), missing `#` prefix, malformed input. `parseHexColor` is `internal` (or `public`) for testability. |

#### `parseHexColor` helper in `AppColors.kt`

```kotlin
import com.streamvault.app.BuildConfig  // generated under namespace = "com.streamvault.app"

private fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val long = cleaned.toLong(16)
    return if (cleaned.length == 8) Color(long) else Color(0xFF000000 or long)
}
```

The `if (length == 8)` branch handles the `#33FF6A1A` style (alpha + RGB), the else branch handles the `#FF6A1A` style (RGB only, alpha forced to 0xFF).

### End-to-end flow: adding a new reseller

After the refactor lands, adding `app.outromarket.iptv` is 5 steps:

1. **Add a flavor block** in `app/build.gradle.kts` `productFlavors`:
   ```kotlin
   create("outromarket") {
       dimension = "brand"
       applicationId = "app.outromarket.iptv"
       versionName   = "1.0.0"
       versionCode   = 1
       buildConfigField("String",  "BRAND_NAME",                   "\"Outro Market\"")
       // ... 10 more fields, all different values
   }
   ```
2. **Generate icons** with skill #13: `python tools/regen_launcher_art.py --source assets/ic_launcher_outromarket.png --out app/src/main/res`.
3. **Publish the remote config** at `https://outromarket.app/iptv-config.json` with the `version` field bumped.
4. **Build & install**: `./gradlew :app:assembleOutromarketDebug --no-daemon && adb -s d1d1b8f3 install -r app/build/outputs/apk/outromarket/debug/app-outromarket-debug.apk`.
5. **Uninstall any old install** under a different `applicationId` (Android treats it as a new app). Save the user's credentials elsewhere first; the new install starts with an empty `streamvault.db`.

**Total per new reseller: ~20 lines of Kotlin DSL + 1 image + 1 JSON publish. Zero source code touched.**

## State, ownership, concurrency, failure, and recovery

- **State ownership.** Each `productFlavors` block owns its own 11 `buildConfigField` values. There is no shared mutable state across flavors — flavors are independent build outputs.
- **No runtime state.** The `BuildConfig` fields are compile-time constants baked into the APK. No `SharedPreferences`, no DataStore, no DataStore key. The app cannot mutate them at runtime.
- **Concurrency.** N/A. The plan is a build-time refactor. Runtime behavior is identical to the upstream StreamVault (modulo brand text + colors + TV input).
- **Failure modes.**
  1. **`buildConfigField` typo / unparseable hex.** The Kotlin compiler surfaces the issue at first use of `BuildConfig.BRAND_PRIMARY_COLOR` in `AppColors.kt`. Mitigation: unit test the `parseHexColor` helper with a few inputs.
  2. **Manifest placeholder unresolved.** If `ENABLE_TV_INPUT_SERVICE` is not declared in any flavor, the manifest merge fails with `Manifest placeholder ... did not exist`. Mitigation: every flavor block declares all 11 fields. The plan enforces this by listing them in the template.
  3. **Color mismatch between welcome screen and Settings.** The welcome screen's button background uses `AppColors.BrandStrong`; if the value is wrong the button is unreadable. Mitigation: verify the four brand surfaces (Welcome button, top-nav pill, Settings rail, selected-card border) in the smoke test.
  4. **`applicationId` change breaks data migration.** Switching from `com.streamvault.app.debug` to `app.ehtudo.iptv.debug` makes the new install start with an empty DB. The user must re-enter the Xtream credentials. This is documented in skill #14's Step 6.
- **Recovery.** A bad flavor block is reverted by deleting it from `productFlavors`. The other flavors are unaffected. A bad `BuildConfig` value is fixed by editing the hex string; no source change needed.

## Tests and acceptance criteria

### Build-time checks (must all pass)

1. `./gradlew :app:assembleEhtudoDebug --no-daemon` exits 0.
2. `./gradlew :app:assembleEhtudoRelease --no-daemon` exits 0 (or fails only on missing `RELEASE_STORE_*` env vars, which is acceptable).
3. `./gradlew :app:assembleDebug --no-daemon` (no flavor specified) fails with a clear error: `ProductFlavor names cannot be empty`. This is expected — every build must specify a flavor.

### APK content checks (manual, on the produced APK)

4. `aapt2 dump badging app/build/outputs/apk/ehtudo/debug/app-ehtudo-debug.apk | head` shows `package: name='app.ehtudo.iptv.debug'` and `application-label:'Eh! IPTV'`.
5. The APK contains the merged manifest with the TV input `<service>` having `android:enabled="false"`. Verify with `aapt2 dump xmltree --file AndroidManifest.xml` (extracted from the APK).
6. `BuildConfig.BRAND_NAME == "Eh! IPTV"` is reachable at runtime — verified by the welcome screen rendering "Eh! IPTV" as the title.

### Functional checks (manual, on the Xiaomi POCO)

7. Install the APK, launch, confirm the welcome screen shows "Eh! IPTV" as the title.
8. Type a username + password, tap "Salvar". The Xtream login hits `http://dnstv.top/player_api.php?username=...&password=...` (verify in logcat). On success, lands on Home with channels loading.
9. Open the TV's `Inputs / Sources` menu. The "StreamVault Live Channels" entry is **not** present (gated by `ENABLE_TV_INPUT_SERVICE = false`).
10. Open Settings → Provedores → tap the existing provider card. The Xtream branch shows only the username + password fields, no URL field, no advanced panel (gated by `SHOW_ADVANCED_OPTIONS = false`).
11. Walk through the four screens that exercise every brand surface: Welcome button, Top-nav pill, Settings rail, selected-card border. All four use the orange `Brand*` palette.

### Unit tests (Kotlin, in `app/src/test/java/...`)

12. `parseHexColor("#FF6A1A")` returns `Color(0xFFFF6A1A)`.
13. `parseHexColor("#33FF6A1A")` returns `Color(0x33FF6A1A)`.
14. `parseHexColor("FF6A1A")` (no `#`) returns the same as #12.
15. `parseHexColor("not-a-color")` throws `IllegalArgumentException` (or returns a documented fallback).

### Skill documentation cross-check

16. The deprecation banners on skills #3 and #5 are unchanged. The README index still shows them as struck through.
17. The new `/docs/plans/20260829-plan-N-resellers.md` is committed alongside the code, with a 1-line index entry in the docs README (if such an index exists — verify; if not, add a `## Plans` section).
18. Skills #6, #9, #10, #12, #14 remain accurate after the refactor. (Re-read each and confirm the references to file paths / BuildConfig field names are still correct.)

## Risks and unresolved decisions

1. **Welcome screen stays on the upstream multi-step form.** This plan does **not** apply skill #1 (the 2-field simplified welcome). The upstream welcome has a "Server URL" text field, a "Username" field, a "Password" field, and a "Playlist Name" field. The refactor introduces `BuildConfig.XTREAM_DEFAULT_URL` as a fallback when the URL field is empty, and hides the URL/Name fields behind `BuildConfig.SHOW_ADVANCED_OPTIONS = false`. The user still sees the multi-step form, but on the `ehtudo` flavor, only username + password are visible and the rest auto-fills from `BuildConfig`. If the user wants the 2-field form (no URL, no name, no SourceType selector), that is a follow-up applying skill #1 — independent of this plan and not blocking.

2. **TV input service default for `ehtudo`: `false` (disabled).** Upstream the manifest has the `<service>` declared unconditionally. Setting `ENABLE_TV_INPUT_SERVICE = false` in the `ehtudo` flavor causes the merged manifest to omit the service — the reseller's app no longer appears in the TV's input picker. This matches the reseller framing ("tratar o app.ehtudo.iptv como um resseller" — reseller servers typically don't expose a TV-input feed). If a different reseller needs the feed, they flip the flag to `true` in their own flavor block. The plan's default is `false`. *If the user wants it `true`, override the value in the flavor block before implementation.*

3. **`applicationId` change is destructive for installed users.** Android treats `com.streamvault.app.debug` and `app.ehtudo.iptv.debug` as different apps. The user's first install of the Eh! IPTV build is a clean install. Saved providers, parental PIN, playback history are all reset. *Mitigation:* if the user has data they want to preserve on the upstream install, back up the `streamvault.db` first: `adb -s d1d1b8f3 exec-out run-as com.streamvault.app.debug cat databases/streamvault.db > /tmp/streamvault.db`. There is no automated migration path because the data model differs from upstream (Eh! IPTV is a different product).

4. **Hilt / KSP cache invalidation is not a concern here.** This plan does **not** rename packages — the source stays at `com.streamvault.app.*`. The deprecated skill #5 warned about `Could not find class file for 'app.ehtudo.app.StreamVaultApp'` after a rename; that concern does not apply. *Mitigation (defensive only):* if the build fails with a stale-cache error after the refactor, run `./gradlew --stop` and `rm -rf app/build/kspCaches`. This should not be needed.

5. **Welcome screen state for advanced users on `ehtudo`.** The refactor hides the "Server URL" and "Playlist Name" fields when `SHOW_ADVANCED_OPTIONS = false`, but keeps the form state inside `var serverUrl by rememberSaveable { mutableStateOf("") }`. If the user types a URL on a `SHOW_ADVANCED_OPTIONS = true` build (developer or admin flavor), then upgrades to a `SHOW_ADVANCED_OPTIONS = false` build (ehtudo), the form is empty (good — `ifBlank` triggers the fallback). If they downgrade, the field reappears with the empty value. *No data loss; the typed URL was never persisted server-side.* The Xtream command is what carries the URL, not the form's local state.

6. **The user may want a second reseller next week.** The plan enables that — adding `create("otherReseller") { ... }` is the documented path. No additional work is required in this plan.

7. **Color hex parser edge cases.** `parseHexColor` must handle 6-digit hex (`#FF6A1A`), 8-digit hex with alpha (`#33FF6A1A`), the `0x` prefix, and a missing `#`. It must throw or fallback gracefully on malformed input. *Mitigation:* the unit tests in `AppColorsParseTest.kt` cover these cases. If a malformed hex slips into a flavor block (e.g. a typo), the build still succeeds (it's a string) and the runtime `parseHexColor` throws at first use — caught at smoke-test time, not at build time.

## Open decisions to confirm with the user before implementation

These are the only questions that materially change what the implementer does. All others can be answered from the repository or skill docs.

1. **TV input service default for the `ehtudo` flavor: `true` (keep) or `false` (disable)?** Plan default: `false`, per the reseller framing (reseller servers typically don't expose a TV-input feed). The implementer uses `false` unless the user overrides.
2. **Hex color palette: orange (`#FF6A1A` / `#FF8A3D` / `#33FF6A1A`) or different?** Plan default: the orange palette from skill #10's "Worked examples" section, matching the Eh! IPTV brand identity in the project memory. If the user wants a different palette, swap the three hex strings in the `ehtudo` flavor block.
3. **Welcome screen simplification (skill #1) — in this plan or follow-up?** *Resolved: follow-up.* The user clarified the base is upstream StreamVault. The plan keeps the upstream multi-step form and only adds the BuildConfig fallbacks. Applying skill #1 (2-field welcome) is a follow-up commit on top of this one. The implementer does **not** modify the welcome form structure.

If the user gives no answer to (1), (2) before the implementer starts, the implementer uses the defaults: `ENABLE_TV_INPUT_SERVICE = false`, orange palette.

## File change map (concrete diff the implementer will produce)

### A. `app/build.gradle.kts` — add the flavor block

Insert after `defaultConfig { ... }` and before `signingConfigs { ... }`:

```kotlin
flavorDimensions += "brand"
productFlavors {
    create("ehtudo") {
        dimension = "brand"
        applicationId = "app.ehtudo.iptv"
        versionName   = "1.0.17-1"
        versionCode   = 19
        buildConfigField("String",  "BRAND_NAME",                   "\"Eh! IPTV\"")
        buildConfigField("String",  "WHATSAPP_URL",                 "\"http://wa.me/+5511932055173\"")
        buildConfigField("String",  "XTREAM_DEFAULT_URL",           "\"http://dnstv.top/\"")
        buildConfigField("String",  "XTREAM_DEFAULT_PROVIDER_NAME", "\"Eh! IPTV\"")
        buildConfigField("String",  "BRAND_PRIMARY_COLOR",          "\"#FF6A1A\"")
        buildConfigField("String",  "BRAND_SECONDARY_COLOR",        "\"#FF8A3D\"")
        buildConfigField("String",  "BRAND_DIM_COLOR",              "\"#33FF6A1A\"")
        buildConfigField("String",  "REMOTE_CONFIG_URL",            "\"https://ehtudo.app/iptv-config.json\"")
        buildConfigField("boolean", "SHOW_ADVANCED_OPTIONS",        "false")
        buildConfigField("boolean", "ENABLE_TV_INPUT_SERVICE",      "false")
    }
}
```

No other change to `app/build.gradle.kts` is required for this plan. The existing `OFFICIAL_APPLICATION_ID` / `OFFICIAL_SIGNING_CERT_SHA256` / `APP_UPDATE_CHANNEL` / `BUILD_TIMESTAMP_UTC` / `XTREAM_DEV_*` / `M3U_DEV_*` `buildConfigField` entries stay as-is.

### B. `app/src/main/java/com/streamvault/app/ui/design/AppColors.kt` — color parser

Replace lines 13–15 (`val Brand`, `val BrandMuted`, `val BrandStrong`) with:

```kotlin
val Brand       = parseHexColor(BuildConfig.BRAND_PRIMARY_COLOR)
val BrandMuted  = parseHexColor(BuildConfig.BRAND_DIM_COLOR)
val BrandStrong = parseHexColor(BuildConfig.BRAND_SECONDARY_COLOR)
```

Add at the bottom of the file (private helper):

```kotlin
private fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#").removePrefix("0x")
    val long = cleaned.toLong(16)
    return if (cleaned.length == 8) Color(long) else Color(0xFF000000L or long)
}
```

Add the import at the top: `import com.streamvault.app.BuildConfig`.

### C. `app/src/main/java/com/streamvault/app/ui/screens/welcome/WelcomeScreen.kt` — pre-fill + gate

**Verify first** by reading the file end-to-end. The upstream welcome screen has:
- A `var serverUrl by rememberSaveable { mutableStateOf("") }` (or similar) holding the typed URL
- A `var name by rememberSaveable { mutableStateOf("") }` (or similar) holding the typed playlist name
- A button that calls a `viewModel.loginXtream(serverUrl, username, password, name, ...)` function

Apply these three changes:

1. **At the call site that builds the `XtreamProviderSetupCommand`**, change:
   ```kotlin
   serverUrl = serverUrl,
   name = name,
   ```
   to:
   ```kotlin
   serverUrl = serverUrl.ifBlank { BuildConfig.XTREAM_DEFAULT_URL },
   name = name.ifBlank { BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME },
   ```
   This makes the form work out-of-the-box on the `ehtudo` flavor without forcing the user to type a URL.

2. **Hide the "Server URL" and "Playlist Name" text fields** by wrapping them in:
   ```kotlin
   if (BuildConfig.SHOW_ADVANCED_OPTIONS) {
       // ... the existing ProviderTextField for serverUrl and the one for name ...
   }
   ```
   On the `ehtudo` flavor (where `SHOW_ADVANCED_OPTIONS = false`), the user only sees username + password.

3. **The WhatsApp link** (if present in the file — `grep` for `wa.me` or `EH_IPTV_WHATSAPP_URL`) reads its URL from `BuildConfig.WHATSAPP_URL`:
   ```kotlin
   val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.WHATSAPP_URL))
   ```

Add the import: `import com.streamvault.app.BuildConfig`.

### D. `app/src/main/java/com/streamvault/app/ui/screens/provider/ProviderSetupScreen.kt` — pre-fill + gate

Same pattern as C, plus one extra change:

1. **At the Xtream call site** of the `when (sourceType)` block, apply the same `ifBlank { BuildConfig.XTREAM_DEFAULT_URL }` / `ifBlank { BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME }` fallback to the `serverUrl` and `name` parameters passed to `XtreamProviderSetupCommand`.

2. **Wrap the `AdvancedProviderOptionsSection(...)` call** (the one inside the Xtream branch of the `when (sourceType)`) in:
   ```kotlin
   if (BuildConfig.SHOW_ADVANCED_OPTIONS) {
       AdvancedProviderOptionsSection(
           sourceType = sourceType,
           uiState = uiState,
           httpUserAgent = httpUserAgent,
           onHttpUserAgentChange = onHttpUserAgentChange,
           // ... (all 60 parameters unchanged)
       )
   }
   ```
   On the `ehtudo` flavor, `AdvancedProviderOptionsSection` is not rendered for the Xtream branch. The composable's definition stays — other branches (Stalker, M3U, Jellyfin) still call it.

3. **Hide the "Server URL" text field** in the Xtream branch the same way as in C (wrap in `if (BuildConfig.SHOW_ADVANCED_OPTIONS)`).

Add the import: `import com.streamvault.app.BuildConfig`.

### E. `app/src/main/java/com/streamvault/app/ui/screens/settings/SettingsProviderSection.kt` — WhatsApp URL

Grep the file for `wa.me` / `EH_IPTV_WHATSAPP_URL`. If a WhatsApp link exists:
- Replace its URL constant with `BuildConfig.WHATSAPP_URL`.
- Add `import com.streamvault.app.BuildConfig`.

If no WhatsApp link exists on this branch (likely, since the upstream has no such link), no change here. The welcome screen is the only entry point for support on the `ehtudo` build.

### F. `app/src/main/AndroidManifest.xml` — manifest placeholder

At line 145, add `android:enabled="${ENABLE_TV_INPUT_SERVICE}"` to the `<service>` element:

```xml
<service
    android:name=".tvinput.StreamVaultTvInputService"
    android:enabled="${ENABLE_TV_INPUT_SERVICE}"
    android:exported="true"
    android:label="StreamVault Live Channels"
    android:permission="android.permission.BIND_TV_INPUT">
    ...
</service>
```

No other manifest change.

### G. New file: `app/src/test/java/com/streamvault/app/ui/design/AppColorsParseTest.kt`

Unit tests for `parseHexColor` (the helper is private — make it `internal` or test via the public `Brand` / `BrandMuted` / `BrandStrong` fields with a fixed hex string injected at test time, or expose `parseHexColor` as `internal` and add a KSP test rule).

## Order of implementation

The plan executes in this order. The starting point is the **upstream StreamVault base** (no `productFlavors`, no `BuildConfig.BRAND_*`, no `app.ehtudo.*` packages, no hardcoded constants — the multi-step welcome form is intact). The end state is the **Eh! IPTV build** as the first flavor of the N-resellers architecture.

1. **Read `WelcomeScreen.kt` and `ProviderSetupScreen.kt` end-to-end.** Map the call sites that build `XtreamProviderSetupCommand`. Note the local state for `serverUrl` and `name` (typically `var serverUrl by rememberSaveable { mutableStateOf("") }`). Note where `AdvancedProviderOptionsSection` is called. *No code change yet — this is investigation.*

2. **Add the `productFlavors` block to `app/build.gradle.kts` (file A).** The 11 `buildConfigField` entries are now declared. `./gradlew :app:assembleEhtudoDebug` may still succeed with the upstream `Brand = Color(0xFF69A8FF)` palette because `AppColors.kt` is unchanged. The flavor task exists; the fields are unused.

3. **Add the `parseHexColor` helper to `AppColors.kt` (file B, helper only).** The three `val Brand*` stay as `Color(0xFF...)` for now. Helper is unused but compiles. `parseHexColor` is `internal` (not `private`) so the unit test in step 7 can reach it.

4. **Swap the three `val Brand*` literals in `AppColors.kt` for `parseHexColor(BuildConfig.BRAND_*)` reads (file B, swap).** Rebuild. Verify the welcome screen now shows the orange palette. If the colors are wrong, the `parseHexColor` helper has a bug — fix before continuing.

5. **Refactor `ProviderSetupScreen.kt` (file D).** (a) At the Xtream call site, add the `ifBlank { BuildConfig.XTREAM_DEFAULT_URL }` / `ifBlank { BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME }` fallbacks. (b) Wrap the "Server URL" text field in `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { ... }`. (c) Wrap the `AdvancedProviderOptionsSection(...)` call in the same gate. Other branches (Stalker, M3U, Jellyfin) are untouched.

6. **Refactor `WelcomeScreen.kt` (file C).** (a) At the `loginXtream` call site, add the same `ifBlank` fallbacks. (b) Wrap the "Server URL" and "Playlist Name" fields in `if (BuildConfig.SHOW_ADVANCED_OPTIONS) { ... }`. (c) If the WhatsApp link exists, point it at `BuildConfig.WHATSAPP_URL`.

7. **Refactor `SettingsProviderSection.kt` (file E).** Grep for the WhatsApp link. If present, replace its URL constant with `BuildConfig.WHATSAPP_URL`. If absent (likely on this branch), skip this step.

8. **Add the manifest placeholder (file F).** Rebuild. Verify `aapt2 dump xmltree` on the merged APK shows `android:enabled="false"` on the TV input `<service>`.

9. **Add the unit tests for `parseHexColor` (file G).** `./gradlew :app:testEhtudoDebugUnitTest`. Tests cover: 6-digit hex, 8-digit hex with alpha, missing `#`, malformed input.

10. **Run the full smoke test on the Xiaomi POCO** (acceptance criteria #4–11). The Eh! IPTV APK at `app/build/outputs/apk/ehtudo/debug/app-ehtudo-debug.apk` installs as `app.ehtudo.iptv.debug`. The welcome form shows username + password (URL/Name fields hidden); typing creds and tapping "Salvar" lands on Home with the orange palette.

11. **Save the plan file** at `D:\repos\edsonlcandido\eh-iptv-player\docs\plans\20260829-plan-N-resellers.md` (the user-requested path). Commit the plan + the code together in a single PR. Subsequent resellers (e.g. `app.outromarket.iptv`) are added by creating a new `create("outromarket") { ... }` block in `app/build.gradle.kts` and running skill #13 for the icons.

## Acceptance gates (the implementer stops and reports when all of these are true)

- All 18 acceptance criteria above pass.
- The Xiaomi POCO has the Eh! IPTV build installed and the welcome screen shows the form with the orange `BrandStrong` accent. The "Server URL" and "Playlist Name" fields are hidden. Typing credentials and tapping "Salvar" lands on Home with the Eh! IPTV playlist loaded.
- The TV input service is absent from the TV's input picker (verified via `aapt2 dump xmltree` on the merged manifest).
- `./gradlew :app:assembleEhtudoDebug --no-daemon` succeeds in a clean cache (`rm -rf app/build` first).
- The plan file is committed at `docs/plans/20260829-plan-N-resellers.md` and the user has been shown the final diff.
- A second `assembleOutromarketDebug`-style flavor can be added by copying the `create("ehtudo") { ... }` block in a follow-up commit and changing the values — the user has been shown the diff for the first flavor and the path to the second is documented.

## Out of scope (explicit)

- **Welcome screen simplification (skill #1) — follow-up.** The plan keeps the upstream multi-step welcome form. The "Server URL" and "Playlist Name" fields are hidden behind `BuildConfig.SHOW_ADVANCED_OPTIONS = false`, but the form still has a "SourceType" selector and the multi-step layout. Flattening to a 2-field form (skill #1) is independent of this plan and is the obvious follow-up commit. The implementer does **not** apply skill #1 in this plan.
- **Top navigation visibility (skill #8)** — already runtime-configurable via Settings → Navegação superior, no code change needed for rebranding.
- **Dynamic Xtream URL via remote config (skill #11)** — not on this branch; when it lands, it reads `BuildConfig.REMOTE_CONFIG_URL`.
- **Banner / launcher art regeneration (skill #13)** — operator runs the script *after* the refactor; not part of this plan's commit. The default Eh! IPTV icons are used (skill #13 produces the orange-themed ones in a follow-up).
- **Multi-reseller in one APK (e.g. tenant selector in Settings)** — explicitly out of scope per the design decision in §"Key design decisions".
- **Package rename (`com.streamvault.app.*` → `app.ehtudo.iptv.*`)** — explicitly out of scope; skill #5 is deprecated for this purpose.
