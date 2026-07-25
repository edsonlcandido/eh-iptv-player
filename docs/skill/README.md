# Skills — Eh! IPTV Player

Skills for programming agents simplifying **StreamVault** into a custom-branded IPTV player for the **Eh! IPTV** reseller.

## North Star

Turn a generic, multi-provider IPTV player into a **zero-config, single-provider, single-URL** IPTV client where the end user types only **usuário + senha** and the app instantly loads the reseller's catalog (live channels, movies, series) from the Eh! IPTV Xtream server.

The user must **never see**:
- Server URLs
- Playlist names
- Provider type selectors (Xtream / Stalker / M3U / Jellyfin)
- Advanced HTTP options (User-Agent, headers, EPG source policies, VOD classification, sync profiles, MAC addresses, etc.)

The user must **only see**:
- Brand title "Eh! IPTV"
- Two text fields: `Usuário` and `Senha` (both plain text, no password mask)
- A `Salvar` button

## Skill index

| # | Skill | Purpose |
|---|---|---|
| 1 | [simplify-welcome-onboarding.md](./simplify-welcome-onboarding.md) | Replace the multi-step Welcome flow with a single 2-field login form |
| 2 | [simplify-provider-setup-screen.md](./simplify-provider-setup-screen.md) | Hide URL, playlist name, and AdvancedProviderOptionsSection for the Xtream branch |
| 3 | [hardcode-xtream-server-defaults.md](./hardcode-xtream-server-defaults.md) | Where and how to hardcode the Xtream server URL and default provider name |
| 4 | [best-effort-provider-activation.md](./best-effort-provider-activation.md) | Activate the provider immediately after auth so the app is usable in seconds, not minutes |
| 5 | [package-rename-streamvault-to-ehtudo.md](./package-rename-streamvault-to-ehtudo.md) | Mechanical rename of `com.streamvault.*` → `app.ehtudo.iptv` (single source-of-truth namespace) |
| 6 | [iptv-reseller-simplification-checklist.md](./iptv-reseller-simplification-checklist.md) | Master checklist to apply all simplifications in order without regressions |
| 7 | [testing-on-xiaomi-miui-device.md](./testing-on-xiaomi-miui-device.md) | How to sideload and test on the Xiaomi POCO M2012K11AG with the MIUI "Install via USB" gate |

## Project facts

- **Module map:** `:app` (UI + Compose), `:domain` (pure Kotlin models/use-cases), `:data` (Room, repositories, sync), `:player` (Media3 wrapper + FFmpeg decoder)
- **Production app installed on the Xiaomi:** `app.ehtudo.iptv` (no `.debug` suffix)
- **Current dev branch name pattern:** `ehiptv/<short-name>` (e.g. `ehiptv/custom-and-simplify`)
- **Compile environment:** Linux, JDK 17, Gradle 8.12, Android SDK at `/home/edson/Android/Sdk` (declared in `local.properties`)
- **Primary test device:** `adb -s d1d1b8f3` → Xiaomi POCO M2012K11AG (Android 13, arm64-v8a). Physical rotation is portrait (1080x2400) but app runs landscape (`cur=2400x1080`).

## Build / install / test loop

```bash
# Build (cold ~10min, incremental much faster)
./gradlew :app:assembleDebug --no-daemon

# Install (requires user to enable "Instalar via USB" in Developer Options first)
adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb -s d1d1b8f3 shell am start -n app.ehtudo.iptv.debug/app.ehtudo.iptv.MainActivity

# Inspect DB
adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status FROM providers;"
sqlite3 /tmp/db.sqlite "SELECT type, COUNT(*) FROM categories WHERE provider_id=1 GROUP BY type;"
sqlite3 /tmp/db.sqlite "SELECT COUNT(*) FROM channels WHERE provider_id=1;"
```

## Conventions

- The package domain `com.streamvault.*` was renamed to `app.ehtudo.*` — use the new names everywhere. See skill #5.
- Display strings (titles, placeholders, errors) live in `app/src/main/res/values/strings.xml`. The welcome-screen strings have prefix `welcome_brand_title`, `welcome_save`, etc.
- The Xtream server URL is a `private const val` in two places that must stay in sync:
  - `app/src/main/java/app/ehtudo/iptv/ui/screens/welcome/WelcomeScreen.kt:80` — `HARDCODED_XTREAM_URL`
  - `app/src/main/java/app/ehtudo/iptv/ui/screens/provider/ProviderSetupScreen.kt:111` — `HARDCODED_XTREAM_URL`
- The default provider name is `DEFAULT_PROVIDER_NAME = "Eh! IPTV"` in the same two files.

## Order of operations for new agents

1. Read this README.
2. Run [iptv-reseller-simplification-checklist.md](./iptv-reseller-simplification-checklist.md) end-to-end to understand the full state.
3. Apply only the skills that match the user's current ask. Never combine skills 1–4 with skill 5 (rename) in the same commit — the rename pollutes git diffs and is hard to review.
4. Verify with `testing-on-xiaomi-miui-device.md` after each change.
