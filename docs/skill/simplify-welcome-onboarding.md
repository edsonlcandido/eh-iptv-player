# Skill 1 — Simplify the welcome onboarding

## Goal

Replace the multi-step Welcome flow (tagline + "Setup Provider" + "Set up later" buttons, then a full ProviderSetupScreen with URL/Username/Password/Name and tabs for Xtream / Stalker / M3U / Jellyfin) with a **single 2-field form** that directly logs into the hardcoded Xtream server.

The end user should:
1. See the brand title **"Eh! IPTV"**.
2. Type only `Usuário` and `Senha` (both rendered as plain text — no `PasswordVisualTransformation`, no eye icon).
3. Tap `Salvar`.
4. Be taken straight to Home with the catalog loading in the background.

## Files touched

| File | Why |
|---|---|
| `app/src/main/java/app/ehtudo/iptv/ui/screens/welcome/WelcomeScreen.kt` | Replace the `WelcomeViewModel` body and the `WelcomeStartCard` composable. Add `loginXtream()` ViewModel function and 2 `StateFlow<String>` for inputs. |
| `app/src/main/res/values/strings.xml` | Add `welcome_brand_title`, `welcome_username_hint`, `welcome_password_hint`, `welcome_save`, `welcome_username_required`, `welcome_password_required`. |

Do **not** touch `WelcomeLoadingCard` — keep it for the loading state during sync.

## The two required constants

Put these at the top of `WelcomeScreen.kt`, in a private file-level `// ??? Hardcoded Xtream defaults ???` block. Keep them identical to the same constants in `ProviderSetupScreen.kt` (skill 2 + skill 3):

```kotlin
private const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
private const val DEFAULT_PROVIDER_NAME = "Eh! IPTV"
```

## ViewModel shape (Hilt)

`WelcomeViewModel` already has a `ProviderRepository` and `ValidateAndAddProvider` injected. Add:

```kotlin
private val _username = MutableStateFlow("")
private val _password = MutableStateFlow("")
val username: StateFlow<String> = _username.asStateFlow()
val password: StateFlow<String> = _password.asStateFlow()

private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

private val _error = MutableStateFlow<String?>(null)
val error: StateFlow<String?> = _error.asStateFlow()

fun setUsername(value: String) {
    _username.value = value
    if (_error.value != null) _error.value = null
}
fun setPassword(value: String) {
    _password.value = value
    if (_error.value != null) _error.value = null
}

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
        val result = validateAndAddProvider.loginXtream(
            XtreamProviderSetupCommand(
                serverUrl    = HARDCODED_XTREAM_URL,
                username     = username,
                password     = password,
                name         = DEFAULT_PROVIDER_NAME,
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
```

Behavior contract:
- `isLoading = true` is set only after the blank checks pass.
- `isLoading = false` runs **after** the suspending call in **all** branches the implementation actually produces (Success, SavedWithWarning, ValidationError, Error).
- The coroutine runs in `viewModelScope` — no manual `try/catch` needed; `ValidateAndAddProvider.loginXtream` already wraps downstream errors in `Result.error`.

## Composable contract

The `WelcomeScreen` composable observes `viewModel.isLoading`, `hasProviders`, `syncProgress` to render **three** states (in this priority order):

1. `isLoading == true` → `WelcomeLoadingCard(syncProgress)` — shows the progress chip ("Sincronizando…") and items-indexed counter.
2. `hasProviders == false` → `WelcomeStartCard(...)` — the brand form below.
3. `hasProviders == true && !isLoading` → still `WelcomeLoadingCard` so the navigation `LaunchedEffect` fires.

`WelcomeStartCard` must use **plain Material 3 `OutlinedTextField`** (not the TV Material 3 `ProviderTextField` and **not** any `VisualTransformation = PasswordVisualTransformation`). Use `imeAction = ImeAction.Next` on the username field and `ImeAction.Done` on the password field. Apply `AutoCorrectEnabled = false` and `KeyboardCapitalization.None` on both.

The card needs `verticalScroll(rememberScrollState())` because the contents (title 38sp + subtitle + 2 fields + error + button + hint) can exceed 1080px in landscape on a phone.

## Strings to add

In `app/src/main/res/values/strings.xml`, inside the existing welcome block:

```xml
<!-- Simplified onboarding for end users -->
<string name="welcome_brand_title">Eh! IPTV</string>
<string name="welcome_username_hint">Usuário</string>
<string name="welcome_password_hint">Senha</string>
<string name="welcome_save">Salvar</string>
<string name="welcome_username_required">Digite seu usuário</string>
<string name="welcome_password_required">Digite sua senha</string>
```

## Why "plain text" for the password

The user explicitly wants the password visible (no `PasswordVisualTransformation`, no eye icon). Reasons:
- The form is single-tenant (one reseller). Customers receive their creds from the reseller, not by typing them by memory.
- It removes TV-remote-only navigation friction (the eye-toggle on `ProviderTextField` requires focus juggling, which is painful on a TV stick).
- Avoids IME-action ambiguity (next field vs reveal toggle).

The credential is encrypted at rest in Room via `CredentialCrypto` regardless of how it was typed.

## Verification

1. `./gradlew :app:assembleDebug --no-daemon` → BUILD SUCCESSFUL.
2. Install, launch from a clean state (`adb shell pm clear app.ehtudo.iptv.debug`).
3. **Empty fields → Salvar:** red error text "Digite seu usuário" appears under the field. Loading indicator is **not** shown.
4. **Username only, no password → Salvar:** red error "Digite sua senha".
5. **Both fields filled → Salvar:** spinner appears, request hits `http://dnstv.top/player_api.php?username=...&password=...`, on success the user lands on Home with `hasProviders=true`.

## Anti-patterns (do not)

- Do **not** import `androidx.compose.ui.text.input.PasswordVisualTransformation` in the welcome file.
- Do **not** add a visibility toggle (eye icon) on the password field.
- Do **not** add IME-options that change the form layout (e.g. `ImeAction.Search`).
- Do **not** call `providerRepository.loginXtream` directly from the composable — go through the use case so validation + error wrapping are consistent.
- Do **not** rely on `LaunchedEffect(key1 = hasProviders)` to *submit* the form — it should only *navigate* once the ViewModel flips the flag.
