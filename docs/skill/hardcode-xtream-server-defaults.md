# Skill 3 — Hardcode the Xtream server defaults

## Goal

For the Eh! IPTV reseller build, the Xtream server URL and the default provider name are **never** user-editable. They live as `private const val` at the top of exactly two files, and every code path that builds an `XtreamProviderSetupCommand` references them.

The two constants are the single source of truth. Rotating the reseller's server = changing two lines.

## The canonical values

```kotlin
private const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
private const val DEFAULT_PROVIDER_NAME = "Eh! IPTV"
```

URL must be lowercase, end in `/`, scheme `http` (or `https` if you have a cert — the existing `network_security_config.xml` permits cleartext globally so `http://` works out of the box).

Provider name should be the user-facing brand. It shows up:
- As the playlist label in the Dashboard.
- In Settings → Providers list.
- In the diagnostic chip on Settings.
- In `providerDao.getActive()?.name` queried by every screen.

## Where to put them

Exactly these two files, at the top of the file, inside a single private file-level comment block:

### File 1 — `app/src/main/java/app/ehtudo/iptv/ui/screens/welcome/WelcomeScreen.kt`

Add (anywhere in the top-level `// ??? Hardcoded Xtream defaults ???` block, before the `WelcomeViewModel` class):

```kotlin
// ??? Hardcoded Xtream defaults ??????????????????????????????????????????????
// End-user onboarding: the Xtream server is fixed, so the welcome form only
// asks for the playlist username and password. The password field stays a
// normal text input (no VisualTransformation / no password mask) per spec.
private const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
private const val DEFAULT_PROVIDER_NAME = "Eh! IPTV"
```

### File 2 — `app/src/main/java/app/ehtudo/iptv/ui/screens/provider/ProviderSetupScreen.kt`

Same comment block, same constants. This is the "power user" path (settings → edit provider); it must produce an `XtreamProviderSetupCommand` that points at the same server.

## Where the constants are USED

Search for `HARDCODED_XTREAM_URL` and `DEFAULT_PROVIDER_NAME` to find every consumer. Today there are exactly four usages:

| File | Line (approx, may shift) | Purpose |
|---|---|---|
| `WelcomeScreen.kt` | inside `WelcomeViewModel.loginXtream` | Builds the `XtreamProviderSetupCommand` for the welcome form |
| `WelcomeScreen.kt` | inside the `init { ... maybeSeedDevProvider() }` block | Dev seeding (no-op if `local.properties` is empty, but kept in sync) |
| `ProviderSetupScreen.kt` (wide layout) | inside `ProviderFormContent(...)` `onLoginXtream` lambda | The wide-screen save action |
| `ProviderSetupScreen.kt` (narrow layout) | inside the second `ProviderFormContent(...)` `onLoginXtream` lambda | The narrow-screen save action |

If you add a new entry point (e.g. a QR-pairing flow, a settings quick-action button), it must reference the same two constants — do **not** add a new magic string.

## What stays parameterized

These fields remain per-user, even on the simplified form:

- `username` — required, plain text.
- `password` — required, plain text (no mask, per the user's spec).
- `httpUserAgent` — defaults to `""` in the `XtreamProviderSetupCommand`. The user never types it.
- `httpHeaders` — defaults to `""`. The user never types it.
- `epgSyncMode` — defaulted to `BACKGROUND` (or whatever is in the user's `ProviderEpgSyncMode` preferences).
- `xtreamLiveSyncMode` — defaulted to `AUTO`.
- `guideSourcePolicy` — defaulted to `AUTO`.
- `channelLogoSourcePolicy` — defaulted to `SUPPLIER_PREFERRED`.
- `xtreamFastSyncEnabled` — set to `true` in the welcome path so the user sees content fast.

These flow through `ValidateAndAddProvider.loginXtream` and land in the persisted `Provider` row untouched.

## What stays hidden from the user but is read at runtime

- `BuildConfig.XTREAM_DEV_*` fields (in `app/build.gradle.kts` lines 74-79) are read from `local.properties`. They drive the dev-seed path only (see `WelcomeViewModel.maybeSeedDevProvider`). In production builds they are empty strings. If a developer wants to bootstrap a provider on first launch without going through the form, they set:

  ```properties
  # local.properties (gitignored)
  xtream.dev.server=http://test.example.com/
  xtream.dev.username=foo
  xtream.dev.password=bar
  xtream.dev.name=Test Provider
  ```

  This **does not** override `HARDCODED_XTREAM_URL`. Both code paths coexist.

## Changing the reseller server later

When the reseller moves to a new domain (e.g. `http://newserver.com/`):

1. `git grep HARDCODED_XTREAM_URL` should return exactly 4 hits (the two files × two `onLoginXtream` lambda call sites each contribute 1; `maybeSeedDevProvider` in `WelcomeScreen` is the 3rd; the literal definition is the 4th, counted twice for two files).
2. Edit the literal in both files. The two must stay identical.
3. Update `docs/DEV_SEEDING.md` and any onboarding scripts that reference the old host.
4. Bump the cache-busting build version (`versionCode` and `versionName` in `app/build.gradle.kts`) so users get the new URL on the next install — otherwise the old URL is still baked into their already-installed APK.

## Anti-patterns (do not)

- Do **not** add a "Change server" entry in any settings screen. There is no UI affordance to mutate the URL.
- Do **not** read the URL from a `BuildConfig` field, a remote config, a SharedPreferences key, or a DataStore. The URL is compile-time.
- Do **not** introduce a `RemoteConfigRepository` or similar indirection. The user wants simplicity; indirection is the opposite.
- Do **not** move the constants to a shared `Constants.kt` file unless you also write tests for that file. Two grep-able string literals is faster to read and review than an indirection.
