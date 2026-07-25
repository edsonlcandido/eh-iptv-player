# Skill 4 — Best-effort provider activation

## Goal

Today, after a successful Xtream `authenticate()`, the new `Provider` row is inserted with **`is_active = 0, status = PARTIAL`** and the entire sync pipeline (LIVE → VOD → SERIES → EPG) runs **inline** as a suspend call. Only after that single big sync completes does `handleInitialOnboardingSync` flip `is_active = 1`. If anything fails (network blip, 5xx, EPG not supported, user cancelling) the provider stays orphaned and the app shows "Sincronização necessária" on every tab.

For the Eh! IPTV reseller build, the user wants the app **usable in seconds**, even if VOD / SERIES / EPG take 5–10 min to fully index.

The fix: **mark the provider `is_active = 1, status = ACTIVE` immediately after auth**, and run the full sync **in the background** via the existing `SyncManager.scheduleProviderSyncResume(providerId)` WorkManager hook.

## File to edit

`data/src/main/java/app/ehtudo/data/repository/ProviderRepositoryImpl.kt`

The change is local to the `loginXtream` function (lines 231–339 in the current revision). Nothing else needs to move.

## The exact edit

Find the `return when (val authResult = provider.authenticate()) {` block. Inside the `is Result.Success -> { ... }` branch, find the two `authResult.data.copy(...)` calls — one for the **edit** path (when `existingProvider != null`) and one for the **insert** path. In each, change:

```kotlin
xtreamFastSyncEnabled = false,
isActive = false,
status = ProviderStatus.PARTIAL
```

to:

```kotlin
xtreamFastSyncEnabled = false,
isActive = true,
status = ProviderStatus.ACTIVE
```

Then replace the trailing `handleInitialOnboardingSync(syncResult = syncManager.sync(...))` call with two background dispatches:

```kotlin
// Best-effort onboarding: activate the provider immediately so the app is
// usable while LIVE/VOD/SERIES/EPG run in the background via WorkManager.
onProgress?.invoke("Scheduling background sync...")
syncManager.scheduleProviderSyncResume(providerData.id)
maybeScheduleBackgroundEpgSync(providerData.id)
Result.success(providerData)
```

`maybeScheduleBackgroundEpgSync` is already a private member of `ProviderRepositoryImpl` and is a no-op for Xtream providers (it fires for Stalker and Jellyfin). Calling it unconditionally is safe.

## Why this works

- `scheduleProviderSyncResume(providerId)` enqueues a `ProviderSyncWorker` via `WorkManager` with the existing constraints (network, battery, etc.). The app returns to the user immediately.
- The Worker calls `SyncManager.sync(providerId, force = true, ...)` which is the same path that previously ran inline. It re-reads the provider (now `is_active = 1`) and indexes LIVE → VOD → SERIES → EPG.
- The Dashboard observes the active provider and re-emits as soon as `getActive()` returns non-null. Channels/categories become visible as soon as the Worker's first DB commit lands.
- If the Worker is killed mid-sync (low memory, app force-stop), `scheduleProviderSyncResume` re-enqueues it with backoff.

## Why it does not break the edit-provider path

When `existingProvider != null`, the user is editing creds or re-saving. After the change, the row is updated with `is_active = true, status = ACTIVE` instantly, and a re-sync is scheduled. Same UX as add: app stays usable while re-sync runs in background. If the user re-saves to fix a wrong password, the auth call will fail (`Result.Error`) and the row is **not** updated at all (the `is Result.Success -> { ... }` branch is not entered).

## What you must verify afterwards

1. `./gradlew :app:assembleDebug --no-daemon` → BUILD SUCCESSFUL.
2. Install, launch from clean state.
3. **Add provider:** type creds → Salvar → land on Home **in under 2 seconds** (no "Sincronização necessária" anywhere).
4. **Check the DB:**
   ```bash
   adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
   sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status FROM providers;"
   ```
   The new provider is `is_active=1, status='ACTIVE'`.
5. **Background sync still runs.** Watch the work in the WorkManager:
   ```bash
   adb -s d1d1b8f3 logcat | grep -iE "XtreamIndexWorker|BackgroundEpg|ProviderSync"
   ```
   You should see the worker start within seconds of the user being navigated to Home.
6. **Channels appear as they index.** Switch to TV ao vivo while the Worker is still running. You should see channels stream in over time (the Dashboard ViewModel is a `Flow<ProviderEntity>` so it re-emits).
7. **Edit existing provider:** Settings → Provedores → tap "Eh! IPTV" → change username/password → Entrar. Should still work and stay `is_active=1`.

## When the inline sync is still required

The `handleInitialOnboardingSync(syncResult, syncFailurePrefix)` helper is **still** called from non-best-effort paths. It is still used by `loginM3u`, `loginJellyfin`, and `loginStalker` (which have different sync semantics — M3U doesn't need an Xtream-style two-phase flow). Do **not** delete `handleInitialOnboardingSync`; only stop calling it from `loginXtream`.

## Anti-patterns (do not)

- Do **not** mark the provider active *before* `provider.authenticate()` returns. Activation is a side-effect of successful auth. If auth fails (HTTP 401, `auth=0`, network error) the row must not be inserted at all.
- Do **not** delete the `handleInitialOnboardingSync` helper. It is still used by the M3U / Stalker / Jellyfin paths and by `loginStalker` for the device-fingerprint sync.
- Do **not** add a manual `delay(...)` to wait for the Worker. The Worker is fire-and-forget.
- Do **not** call `setActive` again inside the Worker. The provider is already `is_active=1`; the Worker's job is to populate the catalog tables, not to toggle activation.
- Do **not** change the auth path (e.g. bypass `auth=1` validation). Activation is post-auth only.
