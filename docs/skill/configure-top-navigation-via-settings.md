# Skill 8 — Customize the top navigation via Settings

## Goal

The user wanted to remove `Downloads`, `Plugins`, and `Home` (Página inicial) from the top menu. The app already has a full UI for this — **do not hardcode `AppTopLevelDestination.defaultOrder` to hide items**. The single source of truth is the user's stored preference in DataStore (`appTopLevelDestinations`).

This skill documents the existing config surface, the default order, and the persistence layer, so future agents know they do **not** need to edit `AppTopLevelDestination.kt`'s `defaultOrder` for "hide this tab" requests.

## Where the config lives in the UI

**`Settings → Navegação superior`** (Top navigation) — implemented in `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/`.

The entry point is the `SettingsBrowsingSection.kt` row that shows a "Top navigation" item. Tapping it opens `TopNavigationDialog.kt`.

`TopNavigationDialog.kt` lists every `AppTopLevelDestination` and provides, for each:
- A visibility toggle (`Visível` / `Oculto`). `SETTINGS` is `isRequired=true` and cannot be hidden.
- `Up` / `Down` buttons to reorder the visible tabs.
- A "Move up / down" pair for reordering the list.

The dialog receives the current persisted list and writes the new list back via `setAppTopLevelDestinations(...)`.

## The data model

`domain/src/main/java/app/ehtudo/domain/model/AppTopLevelDestination.kt`:

```kotlin
enum class AppTopLevelDestination(
    val storageValue: String,
    val landingDestination: AppLandingDestination? = null,
    val isRequired: Boolean = false
) {
    HOME("home", AppLandingDestination.HOME),
    LIVE_TV("live_tv", AppLandingDestination.LIVE_TV),
    MOVIES("movies", AppLandingDestination.MOVIES),
    SERIES("series", AppLandingDestination.SERIES),
    DOWNLOADS("downloads", AppLandingDestination.DOWNLOADS),
    GUIDE("guide", AppLandingDestination.GUIDE),
    SEARCH("search"),
    PLUGINS("plugins", AppLandingDestination.PLUGINS),
    SETTINGS("settings", AppLandingDestination.SETTINGS, isRequired = true);

    companion object {
        val defaultOrder: List<AppTopLevelDestination> = listOf(
            HOME, LIVE_TV, MOVIES, SERIES, DOWNLOADS,
            GUIDE, SEARCH, PLUGINS, SETTINGS
        )
        // ...
    }
}
```

**Important invariants:**

- `SETTINGS` is `isRequired=true`. `normalizeForStorage(...)` always re-appends `SETTINGS` to whatever the user saves, so it can never be hidden.
- `defaultOrder` is the **fallback** when the user has never saved a custom order. After the first save, the persisted list is the truth.
- The full enum has 9 values, and the only "always-on" is `SETTINGS`. Everything else can be hidden via the UI.

## Default state for new users

On a clean install (`pm clear`), no preference has been written to DataStore yet. `appTopLevelDestinations` returns `defaultOrder` (the 9-item list above). The user sees all 9 tabs in the order they appear in that list.

`Home` is the first tab. The "landing destination" preference (`appLandingDestination`) is separate and defaults to `null`, which resolves to the first tab in the visible set — i.e. `Home` by default.

## What the user wants

The user explicitly asked for the top menu to be **TV ao vivo / Filmes / Série / Guia / Pesquisar / Configurações** — i.e. they want:
- Remove `HOME` (Página inicial)
- Remove `DOWNLOADS`
- Remove `PLUGINS`

This is a user preference, not a code change. The right action is to:

1. **Open the app** on the Xiaomi.
2. **Settings → Navegação superior** (the "Top navigation" row in the "Browsing" / "Navegação" section).
3. Toggle `Home` off.
4. Toggle `Downloads` off.
5. Toggle `Plugins` off.
6. Reorder so `TV ao vivo` is first.
7. **Save**.

The change persists immediately and the top menu reflects the new selection on next launch.

## When to also change `appLandingDestination`

`appLandingDestination` controls **where the app lands when launched from a cold start with providers already configured**. After hiding `Home`, the natural landing is `TV ao vivo`.

This preference is set via `Settings → Padrão → Tela inicial` (or wherever the app exposes the "default landing screen" picker). The user can also pick `TV ao vivo` directly.

If the user has a hidden landing destination, `AppTopLevelDestination.resolveLandingDestination(...)` falls back to the first visible destination.

## When to actually edit `defaultOrder`

`defaultOrder` is the **initial value for new users**. Edit it only when:
- The product wants to ship with a pre-curated menu (e.g. the Eh! IPTV reseller build should ship with `LIVE_TV` first and `HOME` hidden by default, because the app is single-tenant and the user is meant to land on TV).

For a per-user toggle, never edit `defaultOrder`. The user already has a UI for it.

If the user requests "ship with this menu order out of the box" or "remove the Home tab by default", then it **is** appropriate to edit `defaultOrder`. Always revert that change if the user later says "actually, the user can configure it themselves" — which is what this skill is for.

## Related preference surfaces (worth knowing about)

The same pattern (DataStore-backed, configured via Settings dialogs) exists for:

| Surface | DataStore key | Settings entry |
|---|---|---|
| Top navigation tabs | `APP_TOP_LEVEL_DESTINATIONS` | Settings → Navegação superior |
| Default landing screen | `APP_LANDING_DESTINATION` | Settings → Padrão → Tela inicial (or similar) |
| Home dashboard shelves | `APP_HOME_DASHBOARD_SHELVES` | Settings → Home (or similar) |
| Time format | `APP_TIME_FORMAT` | Settings → Hora |
| Preferred audio language | `PREFERRED_AUDIO_LANGUAGE` | Settings → Áudio |
| Player playback speed | `PLAYER_PLAYBACK_SPEED` | Settings → Player → Velocidade |
| Player buffer mode | `PLAYER_PLAYBACK_BUFFER_MODE` | Settings → Player → Buffer |
| Audio/video decoder | `*_DECODER_MODE` | Settings → Player → Decoder |
| Subtitle size / color | `SUBTITLE_*` | Settings → Legendas |
| Live translation endpoint | `PLAYER_LIVE_TRANSLATION_ENDPOINT` | Settings → Player → Tradução ao vivo |
| Timeshift depth / backend | `PLAYER_TIMESHIFT_*` | Settings → Player → Timeshift |
| Wifi / ethernet quality cap | `WIFI_MAX_VIDEO_HEIGHT` / `ETHERNET_MAX_VIDEO_HEIGHT` | Settings → Qualidade de rede |
| Backup auto / on wifi only | `BACKUP_AUTO` / `BACKUP_WIFI_ONLY` | Settings → Backup |
| Etc. | (see `PreferencesRepository.kt` for the full list) | |

Before answering a "can the user configure X" question, check `data/src/main/java/app/ehtudo/data/preferences/PreferencesRepository.kt` and `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/` to see if the surface already exists. Most user-facing toggles are already there.

## Anti-patterns (do not)

- Do **not** edit `AppTopLevelDestination.defaultOrder` to satisfy a per-user "hide this tab" request. Direct the user to **Settings → Navegação superior** instead. The UI is one tap away.
- Do **not** add a new `appTopLevelDestinations = ...` hard-coded override in any composable to "force" a different default. The user's saved preference always wins (it is read with `.collectAsStateWithLifecycle(initialValue = AppTopLevelDestination.defaultOrder)` in `AppNavigation` and falls back to the default only when no preference has been written).
- Do **not** delete the `SETTINGS` row from `defaultOrder` or from the enum. It is `isRequired=true` and the `normalizeForStorage` helper depends on it being in the list.
- Do **not** remove the `TopNavigationDialog.kt` file or its callers — it is the canonical UI for this preference.
- Do **not** couple `defaultOrder` to a specific brand. If the Eh! IPTV reseller needs a different default menu, set the default in a per-flavor source set (e.g. a `:flavor-ehtudo` source set) or in the `WelcomeViewModel.maybeSeedDevProvider` after the user has saved a provider. The user-side setting is the user's domain.
