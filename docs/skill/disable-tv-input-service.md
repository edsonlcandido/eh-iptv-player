# Skill 12 — Disable the TV Input Service (install like a phone)

## Goal

Strip the `StreamVaultTvInputService` so the app no longer offers itself as a "Live TV channel" inside the Android TV's input/source picker. The user wanted this because the reseller's server doesn't expose a true TV input feed and the modal that the TV launches ("select your provider source") led to an unexpected flow. After this skill:

- The app **does not** appear in the TV's `Inputs / Sources` menu as "StreamVault Live Channels"
- The TV-side modal that asked which provider to use is gone
- The app still appears in the Android TV launcher (`LEANBACK_LAUNCHER`) and launches normally as before
- Watch Next + Launcher Recommendations stay intact (they're separate from the TV input feature)

This is a structural change — it touches the manifest, removes a whole package, and prunes references in 6 Kotlin files. Run the build at the end to make sure nothing else still references the removed classes.

## When to apply

- The reseller wants the app to behave on Android TV like a phone app (no input/source registration)
- The backend doesn't expose a feed the TV input framework can index (most Xtream servers don't)
- The "StreamVault Live Channels" entry in the TV's source picker is causing confusion (different label than the app launcher, offers a setup flow that doesn't match the welcome flow)

Do **not** apply if you actually need the app to integrate with the Android TV Live TV row (cable-box style EPG) — that's a real feature, not bloat. Most resellers don't need it.

## Files to delete

Delete the entire `tvinput` package and its companion XML:

```
app/src/main/java/app/ehtudo/iptv/tvinput/StreamVaultTvInputService.kt
app/src/main/java/app/ehtudo/iptv/tvinput/TvInputSetupActivity.kt
app/src/main/java/app/ehtudo/iptv/tvinput/TvInputChannelSyncManager.kt
app/src/main/res/xml/tv_input_service.xml
```

`StreamVaultTvInputService` is the actual `TvInputService` (the source registration). `TvInputSetupActivity` is the modal the TV launches to let the user pick a provider. `TvInputChannelSyncManager` pushes the channel list into the TV's EPG. The XML declares the input service metadata. None of these are needed once the feature is gone.

## AndroidManifest.xml — what to remove

In `app/src/main/AndroidManifest.xml`, remove the three blocks below.

### 1. The three TV-related permissions (top of `<manifest>`)

```xml
<uses-permission android:name="com.android.providers.tv.permission.READ_EPG_DATA" />
<uses-permission android:name="com.android.providers.tv.permission.WRITE_EPG_DATA" />
<uses-permission android:name="android.permission.BIND_TV_INPUT" />
```

The `BIND_TV_INPUT` permission is what tells the system "this app wants to bind a TV input". `READ_EPG_DATA` / `WRITE_EPG_DATA` are for the channel sync to write EPG entries. All three are dead once the service is gone.

### 2. The `<activity>` for the setup modal

```xml
<activity
    android:name=".tvinput.TvInputSetupActivity"
    android:exported="true"
    android:excludeFromRecents="true"
    android:launchMode="singleTask"
    android:theme="@style/Theme.StreamVault" />
```

This is the modal the TV pops up with "select your provider". Without the service, the TV never launches it anyway — but the activity stays installed and could be triggered by an external intent. Remove it to be safe.

### 3. The `<service>` for the TV input

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

This is the actual registration. The `android:label="StreamVault Live Channels"` is the label the TV shows in the source picker. Note: change it to `"Eh! IPTV Live Channels"` if you're keeping the feature but rebranding — only delete if you're removing the feature entirely.

## Code references to prune

After deleting the package, six Kotlin files will have dangling references. Search for `tvInputChannelSyncManager`, `TvInputChannelSyncManager`, and `tvinput.TvInput` to find them all. The pattern is always the same: an `@Inject` field of type `TvInputChannelSyncManager` and one or more calls to `tvInputChannelSyncManager.refreshTvInputCatalog()`.

| File | What to remove |
|---|---|
| `app/src/main/java/app/ehtudo/iptv/MainActivity.kt` | The `@Inject lateinit var tvInputChannelSyncManager` field; the `tvInputChannelSyncManager.refreshTvInputCatalog()` line inside the `isTelevisionDevice()` block; the `import app.ehtudo.iptv.tvinput.TvInputChannelSyncManager` |
| `app/src/main/java/app/ehtudo/iptv/plugins/StreamVaultPluginManager.kt` | The constructor parameter `private val tvInputChannelSyncManager: TvInputChannelSyncManager`; the two `refreshTvInputCatalogInBackground()` call sites; the private `refreshTvInputCatalogInBackground()` function; the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsViewModel.kt` | The constructor parameter `private val tvInputChannelSyncManager: TvInputChannelSyncManager`; the corresponding `tvInputChannelSyncManager = tvInputChannelSyncManager` argument at the construction site; the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsSyncActions.kt` | The constructor parameter; the entire `if (completed.any { it == ... settings_sync_option_tv ... }) { tvInputChannelSyncManager.refreshTvInputCatalog() }` block (it's dead once the service is gone — the `settings_sync_option_tv` string can stay in `strings.xml` as a no-op label, or you can clean it up too); the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderActions.kt` | The constructor parameter; the three `tvInputChannelSyncManager.refreshTvInputCatalog()` call sites (in `setActiveProvider`, in the sync-complete branch, in the `Result.Error` branch); the import |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/home/HomeViewModel.kt` | The constructor parameter; the one `tvInputChannelSyncManager.refreshTvInputCatalog()` call in the provider-refresh helper; the import |

In each file, the constructor argument list in the `ClassName(` block also needs the `tvInputChannelSyncManager = tvInputChannelSyncManager,` line removed — otherwise the build fails with "no parameter named 'tvInputChannelSyncManager' found".

## Test files to clean up

Two unit tests reference the removed manager:

- **Delete entirely:** `app/src/test/java/app/ehtudo/iptv/tvinput/TvInputChannelSyncManagerTest.kt`
- **Edit:** `app/src/test/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderActionsTest.kt` — remove the `import`, the `private val tvInputChannelSyncManager = mock()` field, the `tvInputChannelSyncManager = tvInputChannelSyncManager,` constructor argument, and the three `verify(tvInputChannelSyncManager).refreshTvInputCatalog()` assertions
- **Edit:** `app/src/test/java/app/ehtudo/iptv/ui/screens/home/HomeViewModelTest.kt` — remove the `import`, the mock field, and the constructor argument

`assembleDebug` does not compile tests, so you can defer this cleanup. But clean it up before the next `testDebugUnitTest` run.

## Translations to clean up (optional)

The setup flow used 15 strings with the `tv_input_setup_*` prefix across all 27 locales (`values/`, `values-pt/`, `values-es/`, `values-fr/`, …). At minimum, remove them from `values/strings.xml`. The other locale files can stay — they're inert dead weight, not a build issue, and cleaning 27 files is a separate PR.

## Verification

1. **Grep for stragglers** before building:

   ```bash
   rg "tvinput|TvInput|TV_INPUT|READ_EPG_DATA|WRITE_EPG_DATA|BIND_TV_INPUT" app/src/main
   ```

   Anything that shows up after the cleanup is a missed reference. Should be empty (except the locale files if you deferred the optional cleanup).

2. **Build:**

   ```bash
   ./gradlew :app:assembleDebug --no-da-daemon
   ```

3. **Install on the TV** (always uninstall first — the old install leaves a stale TV input registration in the system's input table):

   ```bash
   adb -s <ID_TV> uninstall app.ehtudo.iptv   # or .debug
   adb -s <ID_TV> install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Smoke check on the TV:** open the TV's `Inputs / Sources` menu. The "Eh! IPTV" entry should not be listed. Open the regular app from the launcher — it should launch into the welcome flow (or the dashboard if already configured) without any "select your provider" modal.

## Anti-patterns (do not)

- Do **not** just hide the `<service>` behind a `tools:node="remove"` or a `BuildConfig.DEBUG` flag. The TV input system caches service registrations aggressively; once the system has indexed the input, uninstalling the app and reinstalling with a different manifest is the only way to fully remove the entry. Strip the code, not the manifest.
- Do **not** keep `StreamVaultTvInputService.kt` as a stub class because you think you might want it back later. It pulls in `androidx.tvprovider`, `TvContract`, and a chunk of `media.tv` APIs that will rot. Delete it.
- Do **not** remove `LEANBACK_LAUNCHER` from the MainActivity's intent filter as part of this skill. The app still belongs in the Android TV launcher — it just no longer registers as a TV input. `LEANBACK_LAUNCHER` and `TvInputService` are independent.
- Do **not** skip the test cleanup thinking the production build doesn't need it. The next person who runs `testDebugUnitTest` will hit a wall of compile errors.
