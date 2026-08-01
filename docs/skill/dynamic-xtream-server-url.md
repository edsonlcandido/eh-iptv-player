# Skill 11 — Dynamic Xtream server URL via remote config

## Goal

The Eh! IPTV reseller runs a single Xtream server (today: `http://dnstv.top/`). Skill #3 hardcodes the URL as a `private const val` in exactly two files. That is the right default for a single-tenant build — the URL is part of the build, every code path references the same constant, and there is no runtime indirection.

**This skill exists for the case where the hardcoded URL is not enough.** Specifically: when the reseller's domain is blocked by an ISP, gets seized, or the operator needs to migrate to a new domain (CDN change, new datacenter, mirror swap), and we cannot ask every installed user to sideload a new APK.

This skill introduces a **remote config file** (a tiny JSON hosted on the reseller's own HTTPS domain) that overrides the hardcoded URL at runtime, with three layers of fallback so the app never breaks if the remote is unreachable.

The fallback order is:

1. **Remote config** (`https://ehtudo.app/iptv-config.json`) — the operator can update this in seconds to point at a new mirror
2. **DataStore cache** — the last URL that successfully worked is persisted on-device, so the app keeps working even if `ehtudo.app` is also down
3. **Compile-time hardcoded fallback** — the original `HARDCODED_XTREAM_URL` from skill #3, used only if both the remote and the cache fail

The user-facing form (Welcome screen with 2 fields and a Salvar button) is unchanged. The user still types only `Usuário` and `Senha`. The only thing that moves is which Xtream server the credentials are sent to.

## When to use this skill

Apply this skill **only if the user explicitly asks for it**. Skill #3's compile-time approach is still the right default for a single-tenant reseller build where the server is stable.

Triggers that justify moving from skill #3 to skill #11:

- The reseller reports the Xtream domain is being blocked by Brazilian ISPs (a common pattern — Vivo, Claro, and Oi routinely block IPTV domains).
- The reseller is operating **multiple mirrors** and needs to switch between them without rebuilding the APK.
- The reseller wants to migrate to a new domain (e.g. from `dnstv.top` to `dnstv.net`) without forcing a Play Store update.
- The reseller is **rotating the server** as a countermeasure against takedown notices.

If none of these apply, stay on skill #3. The indirection is small but it is still indirection, and skill #3 is correct.

## How it works (end-to-end)

The flow is: on every Salvar (or every app launch with a cached provider), the app asks "where is the Xtream server right now?" by checking, in order:

```
┌──────────────────────────────────────┐
│ 1. Remote config (HTTPS GET)         │  ← the operator can update this at any time
│    https://ehtudo.app/iptv-config.   │
│    json                              │
│    { "xtreamServer": "http://.../" } │
└──────────────────┬───────────────────┘
                   │  (timeout 3s, on failure → step 2)
                   ▼
┌──────────────────────────────────────┐
│ 2. DataStore cache                   │  ← last URL that successfully worked
│    preferences[XTREAM_SERVER_URL]    │     on this device
└──────────────────┬───────────────────┘
                   │  (null or stale → step 3)
                   ▼
┌──────────────────────────────────────┐
│ 3. Compile-time hardcoded fallback   │  ← HARDCODED_XTREAM_URL from skill #3
│    private const val in WelcomeScreen│
└──────────────────────────────────────┘
```

The successful URL is **cached to DataStore** so the next launch does not need to call the remote config again. The cache is invalidated when:

- The remote config returns a different value (operator pushed a new URL)
- The user signs out and back in (manual cache flush — handled by the same `welcome_*` key namespace)

If the remote config is unreachable, the app uses the cache silently — no error to the user, no broken state.

## The remote config file

### Where to host it

Any HTTPS endpoint the operator controls. Recommended (in order of cost/operational simplicity):

1. **Cloudflare Pages** — free, global CDN, custom domain, instant updates. Drop a single `.json` file in a git-tracked folder, push, done.
2. **GitHub Pages** — free, but slower propagation and GitHub's CDN has had regional issues in Brazil before.
3. **Vercel / Netlify** — also free, similar to Cloudflare Pages.
4. **The reseller's own web server** (if they already have `ehtudo.app` hosted somewhere) — drop the file at `https://ehtudo.app/iptv-config.json`.

The endpoint **must be HTTPS** (the app's `network_security_config.xml` only allows cleartext for the Xtream host itself; all other traffic goes through the system HTTPS stack).

### The file

`https://ehtudo.app/iptv-config.json`:

```json
{
  "version": 1,
  "xtreamServer": "http://dnstv.top/",
  "notes": "Point at a mirror if dnstv.top is blocked. Update version on every change."
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `version` | int | yes | Bump on every change. The app can compare to the cached version to detect a push. |
| `xtreamServer` | string | yes | Full URL with scheme, lowercase, trailing slash. e.g. `http://dnstv.net/`. |
| `notes` | string | no | Free-form, ignored by the app. Useful for human readers editing the file. |

The file is intentionally **tiny** (under 200 bytes). The operator can update it from a phone, from a git commit, or from any text editor.

### How to update the server (operator flow)

When the operator needs to switch mirrors:

```bash
# 1. Edit the file
vim iptv-config.json
# {"version": 2, "xtreamServer": "http://dnstv.net/"}

# 2. Push / deploy
git add iptv-config.json && git commit -m "switch mirror to dnstv.net" && git push
# Cloudflare Pages deploys in ~30s

# 3. The next Salvar on every installed app picks up the new URL.
#    Existing cached URLs in DataStore are bumped because version changed.
```

The operator does **not** need to:
- Build a new APK
- Push a Play Store update
- Ask the user to do anything

## File-by-file implementation

### Step 1 — Add the config DTO

**New file:** `data/src/main/java/app/ehtudo/data/config/XtreamRemoteConfig.kt`

```kotlin
package app.ehtudo.data.config

import kotlinx.serialization.Serializable

@Serializable
data class XtreamRemoteConfig(
    val version: Long = 0L,
    val xtreamServer: String = "",
    val notes: String = ""
)
```

(Requires `kotlinx.serialization` — already in the project, used elsewhere. If the `:data` module does not have it yet, add to `data/build.gradle.kts`.)

### Step 2 — Add the repository

**New file:** `data/src/main/java/app/ehtudo/data/config/XtreamConfigRepository.kt`

```kotlin
package app.ehtudo.data.config

import android.util.Log
import app.ehtudo.data.preferences.PreferencesRepository
import app.ehtudo.data.preferences.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class XtreamConfigRepository(
    private val preferencesRepository: PreferencesRepository,
    private val remoteConfigUrl: String = DEFAULT_REMOTE_CONFIG_URL,
    private val hardcodedFallback: String,
    private val httpTimeoutMs: Int = 3_000
) {
    suspend fun resolveXtreamServerUrl(): String {
        // 1. Try remote
        val remote = fetchRemote()
        if (remote != null && remote.xtreamServer.isNotBlank()) {
            val cachedVersion = preferencesRepository
                .xtreamRemoteConfigVersion.first()
            if (remote.version > cachedVersion) {
                preferencesRepository.setXtreamServerUrl(remote.xtreamServer)
                preferencesRepository.setXtreamRemoteConfigVersion(remote.version)
                Log.i(TAG, "remote config updated: v${remote.version} → ${remote.xtreamServer}")
            }
            return remote.xtreamServer
        }

        // 2. Fall back to DataStore cache
        val cached = preferencesRepository.xtreamServerUrl.first()
        if (!cached.isNullOrBlank()) {
            Log.w(TAG, "remote config unreachable, using cached URL: $cached")
            return cached
        }

        // 3. Compile-time hardcoded fallback
        Log.w(TAG, "no remote or cached URL, using hardcoded fallback: $hardcodedFallback")
        return hardcodedFallback
    }

    private fun fetchRemote(): XtreamRemoteConfig? = runCatching {
        val conn = (URL(remoteConfigUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = httpTimeoutMs
            readTimeout = httpTimeoutMs
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode in 200..299) {
                Json.decodeFromString(
                    XtreamRemoteConfig.serializer(),
                    conn.inputStream.bufferedReader().use { it.readText() }
                )
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }.getOrElse { e ->
        Log.w(TAG, "remote config fetch failed: ${e.javaClass.simpleName}")
        null
    }

    companion object {
        private const val TAG = "XtreamConfigRepository"

        // The operator's own HTTPS endpoint. Update this constant when moving
        // the config to a new host. Must be HTTPS — cleartext is blocked by
        // the network_security_config.xml for any host other than the Xtream
        // server itself.
        const val DEFAULT_REMOTE_CONFIG_URL = "https://ehtudo.app/iptv-config.json"
    }
}
```

The `HttpURLConnection` choice (instead of OkHttp / Retrofit / Ktor) is intentional: this is a single, infrequent call and the project already has `HttpURLConnection` patterns in similar small fetchers. If the project already has a shared OkHttp client via Hilt, swap to it — the public API of `fetchRemote()` is the same.

### Step 3 — Add DataStore preferences

**Edit:** `data/src/main/java/app/ehtudo/data/preferences/PreferencesRepository.kt`

Add three new entries to the `PreferencesKeys` object:

```kotlin
val XTREAM_SERVER_URL = stringPreferencesKey("xtream_server_url")
val XTREAM_REMOTE_CONFIG_VERSION = longPreferencesKey("xtream_remote_config_version")
val XTREAM_REMOTE_CONFIG_LAST_FETCH = longPreferencesKey("xtream_remote_config_last_fetch_ms")
```

And three new accessors on the class:

```kotlin
val xtreamServerUrl: Flow<String?> = dataStore.data.map { it[PreferencesKeys.XTREAM_SERVER_URL] }

val xtreamRemoteConfigVersion: Flow<Long> = dataStore.data.map {
    it[PreferencesKeys.XTREAM_REMOTE_CONFIG_VERSION] ?: 0L
}

suspend fun setXtreamServerUrl(value: String) {
    dataStore.edit { it[PreferencesKeys.XTREAM_SERVER_URL] = value }
}

suspend fun setXtreamRemoteConfigVersion(value: Long) {
    dataStore.edit { it[PreferencesKeys.XTREAM_REMOTE_CONFIG_VERSION] = value }
}
```

### Step 4 — Inject into WelcomeViewModel

**Edit:** `app/src/main/java/app/ehtudo/iptv/ui/screens/welcome/WelcomeScreen.kt`

Add the new dependency to the `@HiltViewModel` constructor (or whatever the project's DI pattern is — most of StreamVault uses `@Inject constructor`):

```kotlin
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val validateAndAddProvider: ValidateAndAddProvider,
    private val xtreamConfigRepository: XtreamConfigRepository,
    // ... existing dependencies
) : ViewModel() {

    // ... existing StateFlows ...

    fun loginXtream() {
        val username = _username.value.trim()
        val password = _password.value
        when {
            username.isBlank() -> { _error.value = "Digite seu usuário"; return }
            password.isBlank() -> { _error.value = "Digite sua senha"; return }
        }
        _error.value = null
        _isLoading.value = true
        viewModelScope.launch {
            val serverUrl = xtreamConfigRepository.resolveXtreamServerUrl()
            val result = validateAndAddProvider.loginXtream(
                XtreamProviderSetupCommand(
                    serverUrl    = serverUrl,    // ← no longer hardcoded
                    username     = username,
                    password     = password,
                    name         = DEFAULT_PROVIDER_NAME,   // still hardcoded
                    xtreamFastSyncEnabled = true
                )
            )
            _isLoading.value = false
            _error.value = when (result) {
                is ValidateAndAddProviderResult.Success          -> null
                is ValidateAndAddProviderResult.SavedWithWarning -> null
                is ValidateAndAddProviderResult.ValidationError  -> result.message
                is ValidateAndAddProviderResult.Error            -> result.message
            }
        }
    }
}
```

The `HARDCODED_XTREAM_URL` constant **stays** in this file (as a fallback for `XtreamConfigRepository`), but it is no longer the only path. The skill #3 invariant "the URL is compile-time" relaxes to "the URL has a compile-time default".

### Step 5 — Wire the Hilt module

**Edit:** `app/src/main/java/app/ehtudo/iptv/di/` (whichever module declares `@Provides` for the data layer — likely `DataModule.kt` or similar)

Add a `@Provides` for `XtreamConfigRepository`:

```kotlin
@Provides
@Singleton
fun provideXtreamConfigRepository(
    preferencesRepository: PreferencesRepository
): XtreamConfigRepository = XtreamConfigRepository(
    preferencesRepository = preferencesRepository,
    hardcodedFallback = "http://dnstv.top/"  // keep in sync with HARDCODED_XTREAM_URL
)
```

If the project has a `BuildConfig` field for the remote config URL (recommended for testing):

```kotlin
buildConfigField("String", "XTREAM_REMOTE_CONFIG_URL", "\"https://ehtudo.app/iptv-config.json\"")
```

then pass `BuildConfig.XTREAM_REMOTE_CONFIG_URL` instead of the hardcoded string.

### Step 6 — Optional: surface the resolved URL in the provider edit path

If the operator changed the URL via remote config, and the user opens **Settings → Provedores → Editar** on the existing provider, the `ProviderSetupScreen` will still show the new URL as a read-only field (today it is hidden, per skill #2). No change needed — the field stays hidden and the user does not see the URL.

If you want the support team to be able to see "the app is currently using this server" in some diagnostic screen, you can add a `viewModel.currentResolvedServerUrl: StateFlow<String>` and show it in the `Eh! IPTV` provider card's diagnostic chip. Not required for the skill.

## What you do NOT change

- **The user-facing form** in `WelcomeScreen.kt` is unchanged. Still 2 fields, still a Salvar button. The dynamic URL is invisible to the user.
- **`HARDCODED_XTREAM_URL`** stays in both `WelcomeScreen.kt` and `ProviderSetupScreen.kt` as the final fallback. The skill #3 invariant "two files, byte-identical" still holds for the fallback value.
- **`DEFAULT_PROVIDER_NAME = "Eh! IPTV"`** stays hardcoded. The provider name is brand identity, not infrastructure.
- **The `network_security_config.xml`** does not need to change. The remote config endpoint must be HTTPS, which the system stack handles by default.
- **`ProviderRepositoryImpl.loginXtream`** does not change. It already accepts `serverUrl` as a parameter to `XtreamProviderSetupCommand`; the call from `WelcomeViewModel.loginXtream()` is the only thing that gets a different value.

## What to verify after the change

1. `./gradlew :app:assembleDebug --no-daemon` → BUILD SUCCESSFUL.
2. `pm clear app.ehtudo.iptv.debug` to start from a clean state.
3. **Happy path with remote up:**
   - `iptv-config.json` is reachable at the configured URL.
   - Type creds → Salvar.
   - In `adb logcat`, see `XtreamConfigRepository: remote config updated: v1 → http://dnstv.top/`.
   - Provider saves, sync starts, channels appear.
4. **Fallback to cache:**
   - `iptv-config.json` is unreachable (turn off Wi-Fi, or block `ehtudo.app`).
   - Salvar.
   - In `adb logcat`, see `XtreamConfigRepository: remote config unreachable, using cached URL: ...`.
   - Provider saves against the cached URL. App works.
5. **Fallback to hardcoded:**
   - `pm clear` (clears DataStore cache).
   - Salvar with remote down.
   - In `adb logcat`, see `XtreamConfigRepository: no remote or cached URL, using hardcoded fallback: http://dnstv.top/`.
   - Provider saves against the hardcoded URL. App works.
6. **Operator change:**
   - Edit `iptv-config.json` to point at a different mirror, bump `version` to 2.
   - On the device, sign out and back in (or `pm clear` for a clean test).
   - Salvar.
   - In `adb logcat`, see `XtreamConfigRepository: remote config updated: v2 → http://dnstv.net/`.
   - Provider saves against the new mirror. App works.
7. **DB check:**
   ```bash
   adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
   sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status, server_url FROM providers;"
   ```
   The `server_url` column reflects whichever URL the resolve call returned.

## Anti-patterns (do not)

- Do **not** add a UI to let the user edit the server URL. The whole point of this skill is that the user **never** sees the URL. The form is still 2 fields and a Salvar button.
- Do **not** store the URL in SharedPreferences (the legacy API). Use the same DataStore that the rest of the project uses.
- Do **not** put the remote config URL in `local.properties`. The whole point of the remote config is that it can be updated without rebuilding — if it is compile-time, you defeat the purpose.
- Do **not** make the remote config call on the main thread. The implementation in step 2 uses `viewModelScope.launch`, which is fine.
- Do **not** fall through silently if the remote config returns a **200 OK with malformed JSON**. Wrap the JSON parse in `runCatching` and treat parse failure as "remote unreachable" (fall through to cache). The implementation already does this.
- Do **not** cache the URL forever. The cache is invalidated when the remote `version` increases. If the operator forgets to bump `version`, the cache will not refresh — that is the trade-off for avoiding the "always trust remote" pattern that breaks when the remote is down.
- Do **not** introduce a `RemoteConfigRepository` superclass. This is the only remote config in the app. If a second one appears later, refactor then.
- Do **not** make the remote config call synchronous. The `XtreamConfigRepository.resolveXtreamServerUrl()` is a `suspend fun` and the implementation in step 4 awaits it inside `viewModelScope.launch`. Never block.
- Do **not** roll back to skill #3 by removing this code. The remote config layer is additive — removing it brings back the original fragility (one blocked domain = one broken app). The hardcoded fallback inside `XtreamConfigRepository` makes the system more robust than the skill #3 baseline, not less.
- Do **not** commit the `iptv-config.json` file with credentials. The file is public (anyone can fetch it). It contains the Xtream server URL only — never the username or password. The credentials come from the user's input on the Welcome form, as before.
