# Skill 9 — All customisation surfaces and their defaults

## Goal

A single reference for every user-facing option in the Eh! IPTV app: compile-time constants (cannot be changed at runtime), DataStore-backed preferences (changed at runtime via Settings UI), and the Settings screen sections that expose them. Use this when the user asks "can I change X?" or "what is the default for Y?" — answer from this table without re-reading the source.

The app has three layers of configuration, in increasing order of how easily the user can change them:

1. **Compile-time constants** — `const val` literals in Kotlin or `buildConfigField` in Gradle. Require a rebuild. Used for things that must be the same across the entire fleet (the Xtream server URL, the brand name, the package name).
2. **DataStore preferences** — typed `Flow<…>` accessors in `data/src/main/java/app/ehtudo/data/preferences/PreferencesRepository.kt`. Defaults are applied when the user has never written that key. The Settings UI (and the ViewModel) is the canonical writer.
3. **Settings UI surfaces** — `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/*` (~69 files). Each section is a `LazyListScope.*` extension that the Settings screen calls in order. The navigation rail on the left has 8 sections: Provedores, Reprodução, Navegação, Privacidade, Gravação, Backup, EPG Sources, Sobre.

---

## 1. Compile-time constants

### App identity (Gradle, `app/build.gradle.kts`)

| Key | Default | Where it shows up |
|---|---|---|
| `namespace` | `app.ehtudo.iptv` | Generated `BuildConfig` package, all internal resources. |
| `applicationId` | `app.ehtudo.iptv` | Play Store ID, `pm list packages`. With build-type suffix, the `debug` build becomes `app.ehtudo.iptv.debug`. |
| `versionCode` | `17` | Internal upgrade key. Must be bumped for every release. |
| `versionName` | `1.0.16-3` | User-visible "About" label. The `-N` suffix is the per-build iteration counter; bump on every release that doesn't also bump `versionCode`. |
| `OFFICIAL_APPLICATION_ID` | `app.ehtudo.iptv` | `BuildConfig.OFFICIAL_APPLICATION_ID` — used by the update checker to verify the install is an official build. |
| `OFFICIAL_SIGNING_CERT_SHA256` | (filled by `keystore.properties` if present) | `BuildConfig.OFFICIAL_SIGNING_CERT_SHA256` — used to validate the install against the official signing cert. |
| `APP_UPDATE_CHANNEL` | `stable` (debug) / `beta` (beta build type) | `BuildConfig.APP_UPDATE_CHANNEL` — used to pick which release stream the auto-updater looks at. |
| `BUILD_TIMESTAMP_UTC` | `0L` (debug) / `System.currentTimeMillis()` (beta/release) | `BuildConfig.BUILD_TIMESTAMP_UTC`. |
| `XTREAM_DEV_*` | empty in release; from `local.properties` in debug | `BuildConfig.XTREAM_DEV_SERVER`, `…_USERNAME`, `…_PASSWORD`, `…_NAME`. Read by `WelcomeViewModel.maybeSeedDevProvider()`. In production builds always empty. |
| `M3U_DEV_URL`, `M3U_DEV_NAME` | empty in release; from `local.properties` in debug | Same dev-seeding path. |

### Brand constants (Kotlin `private const val`)

These must be byte-identical in the two files that hold them. They are the single source of truth for the Xtream server.

| File | Symbol | Default |
|---|---|---|
| `app/src/main/java/app/ehtudo/iptv/ui/screens/welcome/WelcomeScreen.kt:80` | `HARDCODED_XTREAM_URL` | `"http://dnstv.top/"` |
| same file:81 | `DEFAULT_PROVIDER_NAME` | `"Eh! IPTV"` |
| `app/src/main/java/app/ehtudo/iptv/ui/screens/provider/ProviderSetupScreen.kt:118` | `HARDCODED_XTREAM_URL` | `"http://dnstv.top/"` |
| same file:119 | `DEFAULT_PROVIDER_NAME` | `"StreamVault"` (legacy, only used in the power-user edit flow — does **not** show up in the welcome path) |

The Welcome flow always sends `DEFAULT_PROVIDER_NAME = "Eh! IPTV"`. The `ProviderSetupScreen` power-user flow is the only path that still has the older `"StreamVault"` literal — leave it alone unless you also flip the brand elsewhere.

### Time / locale

- `appLanguage` (DataStore, see below) overrides the system language.
- `appTimeFormat` (DataStore) controls 12h/24h, locale, timezone formatting.

### Network security

- `app/src/main/res/xml/network_security_config.xml` permits cleartext traffic globally. Required for the `http://dnstv.top/` Xtream server. If you move the server to `https://`, you can scope the file to allow cleartext only for the IPTV host.

---

## 2. DataStore preferences (the bulk of the settings)

All preferences live in `data/src/main/java/app/ehtudo/data/preferences/PreferencesRepository.kt`. The `PreferencesKeys` object defines the key strings; the `Flow<…>` accessors apply the default when no value has been written.

### 2.1 Onboarding & top-level layout

| Preference | Type | Default | UI in Settings |
|---|---|---|---|
| `appLanguage` | String | (system default) | Settings → Navegação → Idioma do app |
| `appLandingDestination` | String (enum storage) | `null` (resolves to first visible tab) | Settings → Navegação → Tela inicial |
| `appTopLevelDestinations` | String (newline-separated) | `defaultOrder` (9 tabs) | Settings → Navegação → Navegação superior |
| `appHomeDashboardShelves` | String | `defaultOrder` (6 enabled shelves) | Settings → Navegação → Personalizar Home |

`AppTopLevelDestination.defaultOrder` (the 9 tabs, in order):
1. `HOME` ("Página inicial")
2. `LIVE_TV` ("TV ao vivo")
3. `MOVIES` ("Filmes")
4. `SERIES` ("Série")
5. `DOWNLOADS` ("Downloads")
6. `GUIDE` ("Guia")
7. `SEARCH` ("Pesquisar")
8. `PLUGINS` ("Plugins")
9. `SETTINGS` ("Configurações") — `isRequired = true`, **cannot be hidden**

`AppHomeDashboardShelf.defaultOrder` (only the `defaultEnabled = true` ones, in order):
1. `FAVORITE_CHANNELS`
2. `RECENT_CHANNELS`
3. `LIVE_SHORTCUTS`
4. `CONTINUE_WATCHING`
5. `RECENT_MOVIES`
6. `RECENT_SERIES`

The remaining 6 shelves (`FAVORITE_MOVIES`, `FAVORITE_SERIES`, `CONTINUE_WATCHING_MOVIES`, `CONTINUE_WATCHING_SERIES`, `TOP_RATED_MOVIES`, `RECOMMENDED_MOVIES`) are off by default; the user can enable them in Settings.

### 2.2 Live TV

| Preference | Type | Default | UI |
|---|---|---|---|
| `liveTvChannelMode` | String (enum) | `COMFORTABLE` | Settings → Navegação → Modo de canais de TV ao vivo |
| `showLiveSourceSwitcher` | Boolean | `false` (default OFF — only shown if multiple live sources are configured) | Settings → Navegação → "Show live source switcher" toggle |
| `showFavoritesCategory` | Boolean | `true` | Settings → Navegação → "Show favorites category" toggle |
| `showAllChannelsCategory` | Boolean | `true` | Settings → Navegação → "Show all channels category" toggle |
| `showRecentChannelsCategory` | Boolean | `true` | Settings → Navegação → "Show recent channels category" toggle |
| `liveTvCategoryFilters` | String (newline-separated) | empty | Settings → Navegação → Quick filters |
| `liveTvQuickFilterVisibility` | String (enum) | `ALL` (show both) | Settings → Navegação → "Quick filter visibility" |
| `hideDecorativeLiveRows` | Boolean | `false` | Settings → Navegação → "Hide decorative live rows" toggle |
| `liveChannelNumberingMode` | String (enum) | `NUMBER` (continuous) | Settings → Navegação → "Live channel numbering" |
| `liveChannelGroupingMode` | String (enum) | `FLAT` (no grouping) | Settings → Navegação → "Live channel grouping" |
| `groupedChannelLabelMode` | String (enum) | `ALL` | Settings → Navegação → "Grouped channel label" (sub-option of grouping) |
| `liveVariantPreferenceMode` | String (enum) | `AUTO` | Settings → Navegação → "Live variant preference" |
| `liveVariantSelections` | String (JSON) | empty | per-channel user pin |
| `liveVariantObservations` | String (JSON) | empty | per-channel last-viewed |
| `categorySortModes` (per type) | String (enum) | `DEFAULT` (server order) | Settings → Navegação → Sort by type (Live/Movies/Series) |
| `guideDefaultCategoryId` | Long | `-1L` (all categories) | Settings → Navegação → "Guide default category" |
| `guideScheduledOnly` | Int | `0` | (internal) |
| `guideAnchorTime` | Long | `-1L` | (internal) |
| `guideFavoritesOnly` | Int | `0` | (internal) |
| `guideDensity` | String (enum) | `COMFORTABLE` | (in Guide settings) |
| `guideChannelMode` | String (enum) | `ALL` | (in Guide settings) |
| `epgTimeShiftByProvider` | String (JSON) | empty | (auto-managed) |
| `promotedLiveGroupIds` | String | empty | (auto-managed) |

### 2.3 Player

| Preference | Type | Default | UI |
|---|---|---|---|
| `playerMuted` | Boolean | `false` | (initial mute state at launch) |
| `playerMediaSessionEnabled` | Boolean | `true` | (system media notification) |
| `playerFastRetryOnTransientFailures` | Boolean | `false` | (auto-recovery on network blips) |
| `playerDecoderMode` (legacy) | String | (deprecated) | — |
| `playerAudioDecoderMode` | String (enum) | `AUTO` | Settings → Reprodução → Decoder de áudio |
| `playerVideoDecoderMode` | String (enum) | `AUTO` | Settings → Reprodução → Decoder de vídeo |
| `playerPlaybackBufferMode` | String (enum) | `DEFAULT` | Settings → Reprodução → Buffer |
| `playerLiveStreamFormatMode` | String (enum) | `AUTO` | (auto) |
| `playerVodHttpProtocolMode` | String (enum) | `COMPATIBILITY_HTTP1` | (auto) |
| `playerAudioOutputPreference` | String (enum) | `AUTO` | (auto) |
| `playerCompatibilityMemoryEnabled` | Boolean | `true` | (auto) |
| `playerSurfaceMode` | String (enum) | `AUTO` | (auto) |
| `playerPlaybackSpeed` | String (Float) | `"1"` (1.0×) | Settings → Reprodução → Velocidade de reprodução |
| `playerExternalPlaybackMode` | String (enum) | `INTERNAL_PLAYER` | (auto) |
| `playerAudioVideoSyncEnabled` | Boolean | `false` | (advanced) |
| `playerAudioVideoOffsetMs` | Int | `0` | (advanced, range −500..+500) |
| `preferredAudioLanguage` | String | `"auto"` | Settings → Reprodução → Idioma do áudio preferido |
| `playerSubtitleTextScale` | String (Float) | `"1"` (1.0×, range 0.75..1.75) | Settings → Legendas → Tamanho |
| `playerSubtitleTextColor` | Int (ARGB) | `0xFFFFFFFF` (white) | Settings → Legendas → Cor do texto |
| `playerSubtitleBackgroundColor` | Int (ARGB) | `0x80000000` (translucent black) | Settings → Legendas → Cor do fundo |
| `playerLiveTranslationEnabled` | Boolean | `false` | Settings → Reprodução → Tradução ao vivo |
| `playerLiveTranslationEndpoint` | String | `"http://10.0.2.2:8765"` (Android emulator localhost) | (advanced) |
| `playerControlsTimeoutSeconds` | Int | `5` (range 2..60) | Settings → Reprodução → Auto-hide controls |
| `playerLiveOverlayTimeoutSeconds` | Int | `4` (range 2..60) | (advanced) |
| `playerNoticeTimeoutSeconds` | Int | `6` (range 2..60) | (advanced) |
| `playerDiagnosticsTimeoutSeconds` | Int | `15` (range 2..60) | (advanced) |
| `playerWifiMaxVideoHeight` | Int | `0` (no cap) | Settings → Qualidade de rede → Wi-Fi |
| `playerEthernetMaxVideoHeight` | Int | `0` (no cap) | Settings → Qualidade de rede → Ethernet |
| `playerTimeshiftEnabled` | Boolean | `false` | (advanced) |
| `playerTimeshiftDepthMinutes` | Int | `30` (15 / 30 / 60) | (advanced) |
| `playerTimeshiftBackend` | String (enum) | `AUTO` | (advanced) |
| `defaultStopPlaybackTimerMinutes` | Int | `0` (off) | Settings → Reprodução → Stop timer |
| `defaultIdleStandbyTimerMinutes` | Int | `0` (off) | Settings → Reprodução → Standby timer |
| `preventStandbyDuringPlayback` | Boolean | `true` | Settings → Reprodução → Keep screen on |
| `autoPlayNextEpisode` | Boolean | `true` | Settings → Reprodução → Auto-play next episode |

### 2.4 VOD / Movies / Series

| Preference | Type | Default | UI |
|---|---|---|---|
| `vodViewMode` | String (enum) | `MODERN` | Settings → Navegação → Modo de visualização de filmes |
| `vodInfiniteScroll` | Boolean | `true` | Settings → Navegação → Rolagem infinita (sub-option of modern mode) |
| `vodDuplicateHandlingMode` | String (enum) | `GROUP_BY_NAME` | Settings → Navegação → Duplicatas de filmes |
| `vodVariantPreferenceMode` | String (enum) | `AUTO` | Settings → Navegação → Variantes (sub-option) |
| `vodVariantSelections` | String (JSON) | empty | per-movie pin |
| `vodVariantObservations` | String (JSON) | empty | per-movie last-viewed |

### 2.5 Search

| Preference | Type | Default | UI |
|---|---|---|---|
| `recentSearchQueries` | String (JSON list) | empty | (history drawer) |

### 2.6 Sync / Xtream provider

| Preference | Type | Default | UI |
|---|---|---|---|
| `useXtreamTextClassification` | Boolean | `true` (default ON) | Settings → Provedores → "Use Xtream text classification" |
| `xtreamBase64TextCompatibility` | Boolean | `false` | Settings → Provedores → "Xtream base64 text compatibility" |
| `xtreamTextImportGeneration` | Long | `0L` | (auto-managed) |
| `lastSpeedTestMegabits` | String (Double) | empty | (auto) |
| `lastSpeedTestTimestamp` | Long | `0` | (auto) |
| `lastSpeedTestTransport` | String | empty | (auto) |
| `lastSpeedTestRecommendedHeight` | Int | `0` | (auto) |
| `lastSpeedTestEstimated` | Boolean | `false` | (auto) |

### 2.7 Privacy / Incognito

| Preference | Type | Default | UI |
|---|---|---|---|
| `isIncognitoMode` | Boolean | `false` | (Quick toggle in the top bar) |
| `parentalControlLevel` | Int | `2` (`PRIVATE`) | Settings → Privacidade → Controle parental |
| `parentalPin*` | String (hashed) | empty | (set on first PIN entry) |

### 2.8 Recording

| Preference | Type | Default | UI |
|---|---|---|---|
| `recordingWifiOnly` | Boolean | `false` | Settings → Gravação → Wi-Fi only |
| `recordingPaddingBeforeMinutes` | Int | `0` (range 0..30) | Settings → Gravação → Margem antes |
| `recordingPaddingAfterMinutes` | Int | `0` (range 0..30) | Settings → Gravação → Margem depois |
| `downloadTreeUri` | String | empty (system default) | Settings → Gravação → Pasta de download |
| `maxConcurrentStreams` | Int | `2` (range 1..4) | (advanced) |
| `zapAutoRevert` | Boolean | `true` | (advanced) |

### 2.9 Backup / Drive

| Preference | Type | Default | UI |
|---|---|---|---|
| `backupAuto*` | (multiple) | (see Backup section in `SettingsBackupAboutSections.kt`) | Settings → Backup → Auto-backup |
| `appUpdateDownload*` | String | (auto-managed) | (auto-update cache) |
| `lastMaintenanceSnapshot*` | Long/Int/Bool | (auto-managed) | (diagnostics) |

### 2.10 App updates

| Preference | Type | Default | UI |
|---|---|---|---|
| `autoCheckAppUpdates` | Boolean | `true` | Settings → Sobre → "Auto-check for updates" |
| `autoDownloadAppUpdates` | Boolean | `false` | Settings → Sobre → "Auto-download updates" |
| `lastAppUpdateCheckTimestamp` | Long | `0` | (auto) |

### 2.11 Multiview

| Preference | Type | Default | UI |
|---|---|---|---|
| `multiviewPreset1` / `preset2` / `preset3` | String (JSON) | empty | (in multiview panel) |
| `multiviewPerformanceMode` | String (enum) | `AUTO` | (advanced) |
| `multiviewCenterTwoSlotLayout` | Boolean | `false` | (advanced) |
| `multiviewRespectProviderConnectionLimit` | Boolean | `true` | (advanced) |

### 2.12 Live source selection

| Preference | Type | Default | UI |
|---|---|---|---|
| `lastActiveProviderId` | Long | `null` | (auto-managed) |
| `activeLiveSourceType` | String | `null` | (auto-managed) |
| `activeLiveSourceId` | Long | `null` | (auto-managed) |
| `defaultViewMode` | String | `null` | (auto-managed) |
| `defaultCategoryId` | Long | `-1L` | (auto-managed) |

### 2.13 Database maintenance (auto-managed diagnostics)

| Preference | Type | Default |
|---|---|---|
| `lastMaintenanceAt` | Long | `0` |
| `lastMaintenanceDeletedPrograms` | Int | `0` |
| `lastMaintenanceDeletedExternalProgrammes` | Int | `0` |
| `lastMaintenanceDeletedOrphanEpisodes` | Int | `0` |
| `lastMaintenanceDeletedStaleFavorites` | Int | `0` |
| `lastMaintenanceVacuumRan` | Boolean | `false` |
| `lastMaintenanceMainDbBytes` | Long | `0` |
| `lastMaintenanceWalBytes` | Long | `0` |
| `lastMaintenanceReclaimableBytes` | Long | `0` |
| `lastMaintenanceChannelRows` | Long | `0` |
| `lastMaintenanceMovieRows` | Long | `0` |
| `lastMaintenanceSeriesRows` | Long | `0` |
| `lastMaintenanceEpisodeRows` | Long | `0` |
| `lastMaintenanceProgramRows` | Long | `0` |
| `lastMaintenanceEpgProgrammeRows` | Long | `0` |
| `lastMaintenancePlaybackHistoryRows` | Long | `0` |
| `lastMaintenanceFavoriteRows` | Long | `0` |

---

## 3. Settings UI surfaces (the 8 sections)

Implemented in `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/`. The left rail (`SettingsNavigationRail.kt`) lists 8 entries. Each one delegates to a `settingsXxxSection(...)` extension on `LazyListScope`.

### 3.1 Provedores (`SettingsProviderSection.kt`)

- Add provider card (the standard Xtream/M3U flow — hidden in the welcome-first build, but the screen stays as a power-user affordance).
- Provider cards with: Ativar/Desativar, Editar, Sincronizar, Excluir.
- "Dev seeding" controls (debug only).
- "Use Xtream text classification" toggle.
- "Xtream base64 text compatibility" toggle.
- "Xtream text import generation" — bumps the regeneration counter.

### 3.2 Reprodução (`SettingsPlaybackSection.kt`)

- Decoder de áudio (AUTO / HW / SW).
- Decoder de vídeo (AUTO / HW / SW).
- Buffer (DEFAULT / FAST / COMPAT).
- Velocidade de reprodução (slider 0.5×..2×).
- Sincronização A/V (toggle + offset em ms).
- Stop timer + Idle standby timer (minute pickers).
- Idioma do áudio preferido (combobox: auto, en, pt, es, fr, de, it, ja, ko, zh, ru).
- Manter tela ligada durante reprodução (toggle).
- Auto-play next episode (toggle).
- Auto-download app updates (toggle).
- Auto-check for app updates (toggle).

### 3.3 Navegação (`SettingsBrowsingSection.kt`) — the biggest section

- **Modo de canais de TV ao vivo** — COMFORTABLE / COMPACT / PRO.
- **Navegação superior** (configures the 9 tabs and their order) — see skill 8.
- **Personalizar Home** (configures the 12 home dashboard shelves, 6 enabled by default).
- **Tela inicial** (default landing destination after a fresh launch with providers configured).
- **Show live source switcher** (toggle).
- **Show favorites category** / **Show all channels category** / **Show recent channels category** (toggles).
- **Quick filters** (multi-select list of category name patterns).
- **Quick filter visibility** (ALL / ONLY_PINNED / HIDE_ALL).
- **Hide decorative live rows** (toggle).
- **Live channel numbering** (NONE / NUMBER / LEAVE_GAP).
- **Live channel grouping** (FLAT / GROUPED / BY_CATEGORY).
- **Grouped channel label** (sub-option of grouping).
- **Live variant preference** (AUTO / PREFER_HD / PREFER_SD / etc.).
- **Guide default category** (combobox).
- **Time format** (12h/24h, locale, timezone).
- **VOD view mode** (MODERN / CLASSIC).
- **VOD infinite scroll** (sub-option of modern).
- **VOD duplicate handling** (GROUP_BY_NAME / KEEP_ALL / SHOW_ALL / etc.).
- **VOD variant preference** (sub-option).
- **Sort by type** — separate pickers for Live / Movies / Series category sort.
- **Idioma do app**.
- **Remote shortcuts panel** — bind the four colored remote buttons to actions per profile (Global / TV / Media / etc.).

### 3.4 Privacidade (`SettingsPrivacySection.kt`)

- **Modo Incógnito** (toggle — disables history saving).
- **Controle parental** — set level (PUBLIC / PG / PRIVATE / HIDDEN), set PIN, manage groups.
- **Clear search history** (button).
- **Reset playback history** (button).

### 3.5 Gravação (`SettingsRecordingSection.kt`)

- **Wi-Fi only** (toggle).
- **Margem antes/depois** (minute pickers 0..30).
- **Pasta de download** (system SAF file picker).
- **Concorrência** (max concurrent streams, 1..4).
- **Zap auto-revert** (toggle — return to last channel after a configurable idle).
- **Storage usage** (computed at runtime).
- **Recording browser** (list of in-progress and finished recordings).

### 3.6 Backup & Restauração (`SettingsBackupAboutSections.kt`)

- **Auto-backup** (toggle).
- **Wi-Fi only** (toggle for auto-backup).
- **Backup to Drive** (Google Drive integration).
- **Restore from backup / file**.
- **Reset all settings** (with confirmation).

### 3.7 EPG Sources (`SettingsEpgSection.kt`)

- **EPG source list** (per-provider assignment).
- **Time shift** (per-provider UTC offset).
- **External XMLTV** (URL + auto-refresh interval).
- **EPG favorites** (link EPG to favorite channels).

### 3.8 Sobre (`SettingsBackupAboutSections.kt` + `SettingsAppUpdate*`)

- **Versão** (versionName).
- **Build** (versionCode + channel).
- **Certificado oficial** (matches `BuildConfig.OFFICIAL_SIGNING_CERT_SHA256`).
- **Update channel** (stable / beta).
- **Check for updates** (button — fetches GitHub release).
- **Download and install** (when update available).
- **Diagnostic info** (DB size, WAL size, channel/movie/series row counts, last maintenance).
- **Run maintenance** (button).
- **Speed test** (button — runs InternetSpeedTestRunner).
- **Open-source licenses** (button).

---

## 4. How to read this when answering the user

When the user asks a question like "can the user change X?" or "what is the default for Y?", the answer flow is:

1. Search this file for the closest match.
2. If X is a compile-time constant (Section 1), explain that it requires a rebuild and what file to edit.
3. If X is a DataStore preference (Section 2), explain the default and which Settings screen section exposes it.
4. If neither, the question is "can the user configure X?" — and the answer is "no, not currently". Do **not** add a new preference ad-hoc; propose a feature.
5. Always pair the answer with the file paths so the user can verify.

## Anti-patterns (do not)

- Do **not** edit `AppTopLevelDestination.defaultOrder`, `AppHomeDashboardShelf.defaultOrder`, or any enum `defaultOrder` to satisfy a per-user "hide this tab" request. Direct the user to the **Settings → Navegação superior** dialog. See skill 8.
- Do **not** hardcode defaults in multiple places. The single source of truth is the `?:` Elvis operator on the DataStore read (e.g. `preferences[PreferencesKeys.X] ?: true`). If you need a default change, edit the Elvis expression.
- Do **not** add a new compile-time constant that should be user-tunable. Promote it to a DataStore preference with a `BuildConfig` field only if the user truly cannot change it (e.g. branding, server URL, package name).
- Do **not** leak the dev-seeding `BuildConfig.XTREAM_DEV_*` fields into release builds — `app/build.gradle.kts:74-79` defaults them to `""` for release and reads from `local.properties` only for debug. The current build variant is what controls the value.
- Do **not** use the build variant `debug` of a user's package as the production app. If the user wants to ship to their customers, build a `release` (or `beta`) variant with the official signing cert configured in `keystore.properties`.
- Do **not** add settings UI without also adding a `set*` setter on the `SettingsViewModel` and a write to DataStore via `PreferencesRepository`. The UI must always round-trip through DataStore.
