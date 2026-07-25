# Skill 2 — Simplify the ProviderSetupScreen

## Goal

Hide every advanced knob from the Xtream branch of `ProviderSetupScreen` so the only visible fields are **Usuário** and **Senha**. Keep the other branches (Stalker, M3U, Jellyfin) functional but not advertised — the welcome flow bypasses this screen anyway, so it is only used for power-users / provider edits.

The end user must never see:
- "Server URL" / "Portal URL" text field
- "Playlist Name" text field
- `AdvancedProviderOptionsSection` (User-Agent, custom HTTP headers, EPG source policy, VOD classification, sync mode toggles, MAC address, etc.)
- The `SourceType` selector sidebar/tabs (Xtream / Stalker / M3U / Jellyfin cards)

## Files touched

- `app/src/main/java/app/ehtudo/iptv/ui/screens/provider/ProviderSetupScreen.kt` — the only file that needs changes. The repository, validator, sync manager, and DAO layers are untouched.

## The two required constants (add at top of file)

```kotlin
private const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
private const val DEFAULT_PROVIDER_NAME = "Eh! IPTV"
```

These must be byte-identical to the same constants in `WelcomeScreen.kt` (skill 1). If you change the URL, change it in both files.

## Changes to apply (all inside `ProviderFormContent`)

### 1. Hide the "Playlist Name" field for Xtream

Find the unconditional `ProviderTextField` for the playlist name (around line 925 in the original file) and wrap it:

```kotlin
// Playlist name — always shown (Xtream simplifies onboarding, so it is hidden there)
if (sourceType != SourceType.XTREAM) {
    ProviderTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = androidx.compose.ui.res.stringResource(R.string.setup_name_hint)
    )
}
```

### 2. Hide the "Server URL" field inside the Xtream branch

In the `when (sourceType) { SourceType.XTREAM -> { ... } }` block (around line 942 in the original file), **remove** the first `ProviderTextField` that renders `serverUrl` (the placeholder is `R.string.setup_server_hint`). Leave the username and password fields untouched.

### 3. Remove the entire `AdvancedProviderOptionsSection` for Xtream

The Xtream branch currently calls:

```kotlin
AdvancedProviderOptionsSection(
    sourceType = sourceType,
    uiState = uiState,
    httpUserAgent = httpUserAgent,
    onHttpUserAgentChange = onHttpUserAgentChange,
    ...
    stalkerRequestRules = stalkerRequestRules,
    ...
)
```

For Xtream, **delete this whole call** (it accepts ~60 parameters). The `AdvancedProviderOptionsSection` composable itself can stay defined — other branches (Stalker, M3U) still call it and may rely on it.

### 4. Force the Xtream callbacks to pass the hardcoded URL and name

There are **two** `ProviderFormContent(...)` call sites in this file (one in the wide `Row` layout, one in the narrow `Column` layout). In each, the `onLoginXtream` parameter is:

```kotlin
onLoginXtream = { viewModel.loginXtream(serverUrl, username, password, name, httpUserAgent, httpHeaders) },
```

Replace with:

```kotlin
onLoginXtream = { viewModel.loginXtream(HARDCODED_XTREAM_URL, username, password, DEFAULT_PROVIDER_NAME, httpUserAgent, httpHeaders) },
```

This way, no matter what stale state is in the local form (e.g. an `httpUserAgent` the user typed weeks ago), the save action **always** persists the hardcoded URL and the canonical "Eh! IPTV" name.

## What you do NOT change

- The local `var serverUrl by rememberSaveable { mutableStateOf("") }` and the `onServerUrlChange` callback. They can stay in the composable signature for other branches; they just stop being *used* in the Xtream rendering. Removing them requires changing the `ProviderFormContent` signature, which ripples into both layouts and into Stalker/M3U branches — not worth the risk.
- The `ProviderTextField` composable itself (it remains useful for Stalker and M3U and for the password field, where the eye icon is still appropriate).
- `SourceTypeSelectorPanel` / `SourceTypeTabRow` — leave them. They are still used when an editor opens an existing non-Xtream provider. If you want, gate them with `if (sourceType != SourceType.XTREAM)` but the welcome flow already routes to Home on success so the user will never see them.

## Verification

1. `./gradlew :app:assembleDebug --no-daemon` → BUILD SUCCESSFUL.
2. Install.
3. **Welcome happy path:** type creds → Salvar → lands on Home. (Tests the new constants end-to-end.)
4. **Provider edit path:** Settings → Provedores → tap the existing "Eh! IPTV" provider card. You should see the Xtream branch with **only** the username + password fields and a "Entrar" / "Salvar" button. No URL, no name, no advanced panel.
5. **Add provider path (manual, if reachable):** Dashboard empty state → "Adicionar provedor" → Xtream tab. Same as above.

## Anti-patterns (do not)

- Do **not** delete the `serverUrl` local state — it is bound to the Stalker and M3U branches. Only stop rendering it.
- Do **not** delete `AdvancedProviderOptionsSection` — it is still used by Stalker.
- Do **not** change the `validateXtream` validator — its `serverUrl.isBlank()` check still passes because we always pass `HARDCODED_XTREAM_URL`.
- Do **not** remove the `name` parameter from the `XtreamProviderSetupCommand` even though we always pass a constant — the domain `Provider` `init` block enforces `name.isNotBlank()` and several downstream mappers read it. Keep the chain intact, just hardcode the value.
