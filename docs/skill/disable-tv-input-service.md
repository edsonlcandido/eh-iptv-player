# Skill 12 â€” Disable the TV Input Service (install like a phone)

## Goal

Stop the app from offering itself as a "Live TV channel" inside the Android TV's input/source picker. The user wanted this because the reseller's server doesn't expose a true TV input feed and the modal that the TV launches ("select your provider source") led to an unexpected flow. After this skill:

- The app **does not** appear in the TV's `Inputs / Sources` menu as "StreamVault Live Channels" (or any rebranded label)
- The TV-side modal that asked which provider to use is gone
- The app still appears in the Android TV launcher (`LEANBACK_LAUNCHER`) and launches normally as before
- Watch Next + Launcher Recommendations stay intact (they're separate from the TV input feature)

There are **two** ways to reach this goal. **Use Approach A** â€” it is the canonical path on the `productFlavors` architecture (skill #14) and does not require touching the Kotlin source.

## When to apply

- The reseller wants the app to behave on Android TV like a phone app (no input/source registration)
- The backend doesn't expose a feed the TV input framework can index (most Xtream servers don't)
- The "StreamVault Live Channels" entry in the TV's source picker is causing confusion (different label than the app launcher, offers a setup flow that doesn't match the welcome flow)

Do **not** apply if you actually need the app to integrate with the Android TV Live TV row (cable-box style EPG) â€” that's a real feature, not bloat. Most resellers don't need it. To keep the feature but rebrand the label, just edit the `<service android:label>` in the manifest and the `BRAND_NAME` `buildConfigField` â€” no other change needed.

---

## Approach A â€” Set the `ENABLE_TV_INPUT_SERVICE` BuildConfig flag (recommended)

This is the canonical path on the `productFlavors` architecture (skill #14). The TV input code stays in the source tree but is excluded from the merged manifest for the reseller's flavor.

### Step 1 â€” Set the flag in the flavor block

In `app/build.gradle.kts`, inside the `productFlavors { create("ehtudo") { ... } }` block, set:

```kotlin
buildConfigField("boolean", "ENABLE_TV_INPUT_SERVICE", "false")
```

The merged manifest for the `ehtudo` flavor will then exclude the TV input `<service>`. No code change, no permission removal, no file deletion.

### Step 2 â€” Use the manifest placeholder

In `app/src/main/AndroidManifest.xml`, declare the TV input service with a placeholder:

```xml
<service
    android:name=".tvinput.StreamVaultTvInputService"
    android:enabled="${ENABLE_TV_INPUT_SERVICE}"
    android:exported="true"
    android:label="@string/tv_input_label"
    android:permission="android.permission.BIND_TV_INPUT">

    <intent-filter>
        <action android:name="android.media.tv.TvInputService" />
    </intent-filter>

    <meta-data
        android:name="android.media.tv.input"
        android:resource="@xml/tv_input_service" />
</service>
```

The `android:enabled` placeholder is resolved by the manifest merger against the flavor's `BuildConfig.ENABLE_TV_INPUT_SERVICE`. With `false`, the service is **omitted** from the merged manifest entirely (AGP treats `enabled="false"` on a `<service>` as "do not declare").

### Step 3 â€” Verify (Approach A)

```bash
./gradlew :app:assembleEhtudoDebug --no-daemon

# Confirm the <service> is gone from the merged manifest
# (aapt2 dump works on the APK directly)
unzip -p app/build/outputs/apk/ehtudo/debug/app-ehtudo-debug.apk AndroidManifest.xml \
    | aapt2 dump xmltree --file - 2>/dev/null \
    | grep -i "tvinput\|tv_input" \
    || echo "TV input service correctly omitted from the merged manifest"
```

If the reseller later exposes a real TV input feed, flip the flag to `true` in the same flavor block and rebuild â€” no other change. To rebrand the label without re-enabling, just change `android:label`.

### Why Approach A is the canonical path

- **One line per reseller.** Adding a new reseller that needs TV input = flip the flag in their flavor block. No need to re-add the manifest entry, the permissions, or the Kotlin call-sites.
- **Reversible in seconds.** Going from "TV input off" to "TV input on" for a reseller is a 1-character edit + rebuild, not a multi-file restoration.
- **No code rot.** The TV input code stays compiled, lint-clean, and ready. You don't risk losing the upstream StreamVault's bug fixes when you rebase â€” the file is always there.

---

## Approach B â€” Strip the code entirely (legacy / code-hygiene path)

This is the original approach: physically remove the TV input classes, manifest entries, permissions, and Kotlin call-sites. Use it only if you have non-Android reasons to remove the code (e.g. the unused `androidx.tvprovider` dependency is bloat you want gone, or you want `grep -r tvinput` to return zero for code-hygiene audits).

### Files to delete

Delete the entire `tvinput` package and its companion XML:

```
app/src/main/java/app/ehtudo/iptv/tvinput/StreamVaultTvInputService.kt
app/src/main/java/app/ehtudo/iptv/tvinput/TvInputSetupActivity.kt
app/src/main/java/app/ehtudo/iptv/tvinput/TvInputChannelSyncManager.kt
app/src/main/res/xml/tv_input_service.xml
```

`StreamVaultTvInputService` is the actual `TvInputService` (the source registration). `TvInputSetupActivity` is the modal the TV launches to let the user pick a provider. `TvInputChannelSyncManager` pushes the channel list into the TV's EPG. The XML declares the input service metadata. None of these are needed once the feature is gone.

### AndroidManifest.xml â€” what to remove

In `app/src/main/AndroidManifest.xml`, remove the three blocks below.

#### 1. The three TV-related permissions (top of `<manifest>`)

```xml
<uses-permission android:name="com.android.providers.tv.permission.READ_EPG_DATA" />
<uses-permission android:name="com.android.providers.tv.permission.WRITE_EPG_DATA" />
<uses-permission android:name="android.permission.BIND_TV_INPUT" />
```

The `BIND_TV_INPUT` permission is what tells the system "this app wants to bind a TV input". `READ_EPG_DATA` / `WRITE_EPG_DATA` are for the channel sync to write EPG entries. All three are dead once the service is gone.

#### 2. The `<activity>` for the setup modal

```xml
<activity
    android:name=".tvinput.TvInputSetupActivity"
    android:exported="true"
    android:excludeFromRecents="true"
    android:launchMode="singleTask"
    android:theme="@style/Theme.StreamVault" />
```

This is the modal the TV pops up with "select your provider". Without the service, the TV never launches it anyway â€” but the activity stays installed and could be triggered by an external intent. Remove it to be safe.

#### 3. The `<service>` for the TV input

```xml
<service
    android:name=".tvinput.StreamVaultTvInputService"
    android:exported="true"
    android:label="StreamVault Live Channels"
    android:permission="android.permission.BIND_TV_INPUT">

    <intent-filter>
        <action android:name="android.media.tv.TvInputService" />
    </intent-filter>

    <meta-data
        android:name="android.media.tv.input"
        android:resource="@xml/tv_input_service" />
</service>
```

This is the actual registration. The `android:label="StreamVault Live Channels"` is the label the TV shows in the source picker. Note: change it to `"Eh! IPTV Live Channels"` if you're keeping the feature but rebranding â€” only delete if you're removing the feature entirely.

### Code references to prune

After deleting the package, six Kotlin files will have dangling references. Search for `tvInputChannelSyncManager`, `TvInputChannelSyncManager`, and `tvinput.TvInput` to find them all. The pattern is always the same: an `@Inject` field of type `TvInputChannelSyncManager` and one or more calls to `tvInputChannelSyncManager.refreshTvInputCatalog()`.

| File | What to remove |
|---|---|
| `app/src/main/java/app/ehtudo/iptv/MainActivity.kt` | The `@Inject lateinit var tvInputChannelSyncManager` field; the `tvInputChannelSyncManager.refreshTvInputCatalog()` line inside the `isTelevisionDevice()` block; the `import app.ehtudo.iptv.tvinput.TvInputChannelSyncManager` |
| `app/src/main/java/app/ehtudo/iptv/plugins/StreamVaultPluginManager.kt` | The constructor parameter `private val tvInputChannelSyncManager: TvInputChannelSyncManager`; the two `refreshTvInputCatalogInBackground()` call sites; the private `refreshTvInputCatalogInBackground()` function; the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsViewModel.kt` | The constructor parameter `private val tvInputChannelSyncManager: TvInputChannelSyncManager`; the corresponding `tvInputChannelSyncManager = tvInputChannelSyncManager` argument at the construction site; the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsSyncActions.kt` | The constructor parameter; the entire `if (completed.any { it == ... settings_sync_option_tv ... }) { tvInputChannelSyncManager.refreshTvInputCatalog() }` block (it's dead once the service is gone â€” the `settings_sync_option_tv` string can stay in `strings.xml` as a no-op label, or you can clean it up too); the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderActions.kt` | The constructor parameter; the three `tvInputChannelSyncManager.refreshTvInputCatalog()` call sites (in `setActiveProvider`, in the sync-complete branch, in the `Result.Error` branch); the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/home/HomeViewModel.kt` | The constructor parameter; the one `tvInputChannelSyncManager.refreshTvInputCatalog()` call in the provider-refresh helper; the import |

In each file, the constructor argument list in the `ClassName(` block also needs the `tvInputChannelSyncManager = tvInputChannelSyncManager,` line removed â€” otherwise the build fails with "no parameter named 'tvInputChannelSyncManager' found".

### Test files to clean up

Two unit tests reference the removed manager:

- **Delete entirely:** `app/src/test/java/app/ehtudo/iptv/tvinput/TvInputChannelSyncManagerTest.kt`
- **Edit:** `app/src/test/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderActionsTest.kt` â€” remove the `import`, the `private val tvInputChannelSyncManager = mock()` field, the `tvInputChannelSyncManager = tvInputChannelSyncManager,` constructor argument, and the three `verify(tvInputChannelSyncManager).refreshTvInputCatalog()` assertions
- **Edit:** `app/src/test/java/app/ehtudo/iptv/ui/screens/home/HomeViewModelTest.kt` â€” remove the `import`, the mock field, and the constructor argument

`assembleDebug` does not compile tests, so you can defer this cleanup. But clean it up before the next `testDebugUnitTest` run.

### Translations to clean up (optional)

The setup flow used 15 strings with the `tv_input_setup_*` prefix across all 27 locales (`values/`, `values-pt/`, `values-es/`, `values-fr/`, â€¦). At minimum, remove them from `values/strings.xml`. The other locale files can stay â€” they're inert dead weight, not a build issue, and cleaning 27 files is a separate PR.

### Verify (Approach B)

```bash
# 1. Grep for stragglers before building
rg "tvinput|TvInput|TV_INPUT|READ_EPG_DATA|WRITE_EPG_DATA|BIND_TV_INPUT" app/src/main

# Anything that shows up after the cleanup is a missed reference. Should be empty
# (except the locale files if you deferred the optional cleanup).

# 2. Build
./gradlew :app:assembleDebug --no-daemon

# 3. Install on the TV (always uninstall first â€” the old install leaves a stale
#    TV input registration in the system's input table)
adb -s <ID_TV> uninstall app.ehtudo.iptv   # or .debug
adb -s <ID_TV> install app/build/outputs/apk/debug/app-debug.apk

# 4. Smoke check on the TV: open the TV's Inputs / Sources menu. The
#    "StreamVault Live Channels" entry should not be listed. Open the regular
#    app from the launcher â€” it should launch into the welcome flow (or the
#    dashboard if already configured) without any "select your provider" modal.
```

---

## Anti-patterns (do not)

**Applies to both approaches:**

- Do **not** remove `LEANBACK_LAUNCHER` from the MainActivity's intent filter as part of this skill. The app still belongs in the Android TV launcher â€” it just no longer registers as a TV input. `LEANBACK_LAUNCHER` and `TvInputService` are independent.

**Approach A specific:**

- Do **not** try to set the flag with `tools:node="remove"` in a flavor-specific `AndroidManifest.xml` override. The placeholder approach is cleaner and supported by AGP; the `tools:node` override requires duplicating the entire `<service>` block in the flavor manifest and is fragile to upstream manifest changes.
- Do **not** conditionally inject the `BuildConfig` value with a `manifestPlaceholders` block in `defaultConfig`. The placeholder syntax `${ENABLE_TV_INPUT_SERVICE}` reads from the BuildConfig field automatically when the field exists; do not double-define it.

**Approach B specific:**

- Do **not** just hide the `<service>` behind a `tools:node="remove"` while leaving the Kotlin code in place. The TV input system caches service registrations aggressively; once the system has indexed the input, uninstalling the app and reinstalling with a different manifest is the only way to fully remove the entry. If you go Approach B, strip the code too.
- Do **not** keep `StreamVaultTvInputService.kt` as a stub class because you think you might want it back later. It pulls in `androidx.tvprovider`, `TvContract`, and a chunk of `media.tv` APIs that will rot. Delete it. If you want reversibility, use Approach A.
- Do **not** skip the test cleanup thinking the production build doesn't need it. The next person who runs `testDebugUnitTest` will hit a wall of compile errors.
