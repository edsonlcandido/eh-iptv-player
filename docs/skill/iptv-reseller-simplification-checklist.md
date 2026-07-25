# Skill 6 — IPTV reseller simplification checklist

## Goal

Apply every "make the reseller app simple" change in the right order, with the right verification at each step. Used at the start of a new agent session to recover the current state and decide what's still pending.

## When to use this skill

- At the **start of any new agent session** that touches the welcome, provider-setup, or activation flow.
- After pulling a new branch — confirm the work matches the contract below.
- Before declaring "done" on a customisation pass.

## Phase 0 — Snapshot the current state

```bash
# Confirm the active branch and what is uncommitted
git status --short
git log --oneline -10

# Confirm the installed package on the Xiaomi
adb -s d1d1b8f3 shell pm list packages | grep ehtudo
# Expected: package:app.ehtudo.iptv.debug

# Dump the current provider row
adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status, server_url, username FROM providers;"
```

The expected state of an "applied" build:
- `server_url = http://dnstv.top/` (or your reseller URL)
- `is_active = 1`, `status = ACTIVE` after first login
- `name = Eh! IPTV` (or your brand)

## Phase 1 — Code contracts that must hold

After applying the simplification skills, the following must be true. **Any deviation is a regression.**

### Welcome screen (`WelcomeScreen.kt`)

- [ ] Title text = `R.string.welcome_brand_title` ("Eh! IPTV").
- [ ] Only two `OutlinedTextField`s, `Usuário` and `Senha`, both with `VisualTransformation = None` (i.e. plain text).
- [ ] No eye-icon, no `PasswordVisualTransformation`, no `keyboardType = KeyboardType.Password`.
- [ ] The `WelcomeViewModel.loginXtream()` function:
  - validates non-blank inputs and sets `_error` accordingly
  - builds `XtreamProviderSetupCommand(serverUrl = HARDCODED_XTREAM_URL, name = DEFAULT_PROVIDER_NAME, xtreamFastSyncEnabled = true)`
  - on success, returns `Result.success(providerData)` without waiting for sync to complete
- [ ] `HARDCODED_XTREAM_URL` and `DEFAULT_PROVIDER_NAME` are `private const val` at the top of the file.

### Provider setup screen (`ProviderSetupScreen.kt`)

- [ ] Same two `private const val` constants present, byte-identical to the welcome file.
- [ ] `SourceType.XTREAM -> { ... }` branch contains exactly two `ProviderTextField`s (username + password) and a single `ActionButton`. No server URL field, no playlist name field, no `AdvancedProviderOptionsSection(...)` call.
- [ ] Both `onLoginXtream` lambdas (wide and narrow layouts) pass `HARDCODED_XTREAM_URL` and `DEFAULT_PROVIDER_NAME` instead of the local form state.
- [ ] The `ProviderTextField` for `name` is wrapped in `if (sourceType != SourceType.XTREAM) { ... }`.

### Activation flow (`ProviderRepositoryImpl.loginXtream`)

- [ ] The insert path sets `isActive = true, status = ACTIVE` (not `false` / `PARTIAL`).
- [ ] The edit path sets `isActive = true, status = ACTIVE` (not `false` / `PARTIAL`).
- [ ] After the insert/update, the function calls `syncManager.scheduleProviderSyncResume(providerData.id)` and `maybeScheduleBackgroundEpgSync(providerData.id)` instead of running the full sync inline.
- [ ] The function returns `Result.success(providerData)` immediately after the background dispatch.
- [ ] `handleInitialOnboardingSync` is still defined and still called from `loginM3u`, `loginJellyfin`, `loginStalker` (those paths are unchanged).

### Strings (`strings.xml`)

- [ ] `welcome_brand_title` = "Eh! IPTV"
- [ ] `welcome_username_hint` = "Usuário"
- [ ] `welcome_password_hint` = "Senha"
- [ ] `welcome_save` = "Salvar"
- [ ] `welcome_username_required` and `welcome_password_required` exist.

### Build / install / runtime

- [ ] `./gradlew :app:assembleDebug --no-daemon` → `BUILD SUCCESSFUL`
- [ ] APK installs on Xiaomi (`d1d1b8f3`) without `INSTALL_FAILED_USER_RESTRICTED`
- [ ] First launch: shows the brand title + two plain-text inputs.
- [ ] Type creds → tap Salvar → land on Home **in under 2 seconds** (no `Sync required` banners).
- [ ] TV ao vivo shows > 0 channels within the first minute of background sync.

## Phase 2 — End-to-end verification (run after every change)

```bash
# 1. Clean install
adb -s d1d1b8f3 shell pm clear app.ehtudo.iptv.debug
adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Launch
adb -s d1d1b8f3 shell am start -n app.ehtudo.iptv.debug/app.ehtudo.iptv.MainActivity
sleep 4

# 3. Inspect the welcome screen
adb -s d1d1b8f3 exec-out screencap -p > /tmp/welcome.png
# Verify: title "Eh! IPTV", two text fields labeled Usuário / Senha, Salvar button.

# 4. Try empty Salvar (validates the empty-state UX)
# Tap on Salvar without typing → expect red error "Digite seu usuário".

# 5. Try valid creds
# Fill in user/pass → tap Salvar → expect navigation to Home in < 3s.

# 6. Inspect the DB right after Salvar
adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status FROM providers;"
# Expect: is_active=1, status=ACTIVE

# 7. Watch the background Worker
adb -s d1d1b8f3 logcat -d -t 200 | grep -iE "ProviderSync|XtreamIndex|BackgroundEpg"
# Expect: workers start, channel rows get inserted over time.

# 8. Check TV ao vivo populates
sqlite3 /tmp/db.sqlite "SELECT COUNT(*) FROM channels WHERE provider_id=1;"
# Expect: > 0 (eventually reaches 2700+ for the Eh! IPTV Xtream).

# 9. Check the live UI
adb -s d1d1b8f3 shell input keyevent KEYCODE_DPAD_RIGHT
adb -s d1d1b8f3 shell input keyevent KEYCODE_DPAD_CENTER
sleep 2
adb -s d1d1b8f3 exec-out screencap -p > /tmp/livetv.png
# Expect: "All Channels 2737" or similar.
```

## Phase 3 — Known regression patterns to look for

When something breaks, look for these signatures first:

| Symptom | Likely cause | Fix |
|---|---|---|
| `INITIAL_ONBOARDING_PHASE_FAILED` in logcat after Salvar | Auth failure with `http://dnstv.top/` (DNS, 401, `auth=0`) | Curl the URL manually (`curl -v "http://dnstv.top/player_api.php?username=test&password=test"`) to confirm. Then check `XtreamErrorFormatter` for the exact reason. |
| Home shows "Sincronização necessária" forever | The Worker never started, or `is_active` flipped back to 0 | `adb shell dumpsys jobscheduler | grep ProviderSync` to see the job state. Inspect `sync_metadata` table. |
| Salvar does nothing, no error shown | The composable's `onClick` is bound to a stale `viewModel.loginXtream` reference; the ViewModel re-creates the function on each recomposition in some refactors | Re-read the call site in `WelcomeStartCard` and confirm the method-reference. |
| `XtreamErrorFormatter.message` returns the bare (non-translated) string | `R.string` is missing | Check `welcome_username_required` / `welcome_password_required`. |
| `INITIAL_ONBOARDING_PHASE_STARTING` never advances to `COMPLETED` | The `XtreamLiveSyncReason.INITIAL_ONBOARDING` branch is stuck | This was the old bug; with the best-effort activation, the phase is now advisory, not gating. The Worker will still run. |

## Phase 4 — When to revisit

These skills become stale when:
- The Xtream server moves to `https://` (regenerate the curl, update `network_security_config.xml` if you scope per-domain).
- The product wants a multi-tenant mode (more than one Xtream URL). Then `HARDCODED_XTREAM_URL` becomes a per-tenant setting and skills 1, 2, 3 must be reworked.
- The product adds OAuth / magic-link / QR-pairing onboarding. Then `WelcomeViewModel.loginXtream` is replaced by a different entry point; the constants in skill 3 stay.
- The user wants the welcome screen to also accept the playlist name (multi-brand per reseller). Then `DEFAULT_PROVIDER_NAME` becomes a per-reseller default and skill 3 must be reworked.

In any of these cases, **read the affected skill first**, then refactor in its recommended order.

## Anti-patterns (do not)

- Do **not** combine skill 5 (package rename) with any of skills 1–4 in the same commit. The diff is unreadable.
- Do **not** introduce a "Default URL" preference that the user can mutate. The URL is compile-time.
- Do **not** remove `handleInitialOnboardingSync` even after applying the best-effort activation. M3U / Stalker / Jellyfin still use it.
- Do **not** change the `XtreamProvider`/`ValidateAndAddProvider` layer to skip auth. Auth is the contract that gates persistence.
- Do **not** add a "Sync now" button to the welcome screen. The welcome screen's only job is to get the user in; sync is background.
