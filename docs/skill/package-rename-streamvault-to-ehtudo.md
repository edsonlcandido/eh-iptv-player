> **âš ï¸ DEPRECATED â€” Do not use for new work**
>
> The Android Gradle Plugin separates `applicationId` (the public identity) from `namespace` (the source package). With the `productFlavors` architecture in `app/build.gradle.kts`, you can ship `app.ehtudo.iptv` (or any other reseller ID) **without** renaming a single Kotlin file. The mass-rename below is no longer the recommended path.
>
> **Superseded by:** [skill #14](./white-label-reseller-fork-without-rename.md) (white-label reseller fork without rename) + the `productFlavors` block in `app/build.gradle.kts`. Reading this skill for historical context is fine; do not apply it to new resellers.
>
> **When this skill is still relevant:** if you must also rename the source packages for non-Android reasons (e.g. the codebase is being sold/transferred and a `grep -r streamvault` returning zero is a contract term), this skill is the only path. Otherwise, prefer skill #14.

---

# Skill 5 â€” Package rename: `com.streamvault.*` â†’ `app.ehtudo.iptv`

## Goal

Mechanically rename the entire package domain so the app's `applicationId`, `namespace`, and Kotlin/Java package declarations all read `app.ehtudo.iptv` (for the `:app` module) and `app.ehtudo.{data,domain,player}` (for the other modules). The old `com.streamvault.app`, `com.streamvault.data`, `com.streamvault.domain`, `com.streamvault.player` packages cease to exist.

This is required to ship as a distinct APK alongside the production `app.ehtudo.iptv` (which is already on the Xiaomi) **only if** you also need the source packages to read `app.ehtudo.iptv`. For the typical "ship one reseller" goal, skill #14 does the same job without the 700-file diff.

## Scope (do not deviate)

- **In:** Kotlin packages, Java packages, `package` declarations, `import` statements, gradle `namespace` + `applicationId` + `BuildConfig.OFFICIAL_APPLICATION_ID` strings, `AndroidManifest.xml` FQCN references, `app/proguard-rules.pro`, `docs/*.md`, `tools/cast-validation.ps1`, `AGENTS.md` runtime references.
- **Out:** `settings.gradle.kts` `rootProject.name = "StreamVault"` (cosmetic only; renaming it has no effect on Android), `README.md` (the user-facing project name is still StreamVault in marketing), the launcher icon and `app_name` string (these are rebranding concerns, not package concerns).

## Pre-flight checks

```bash
# Confirm the current state
git status --short
git branch --show-current
```

You should be on a clean `ehiptv/<something>` branch with no uncommitted changes. If not, commit or stash first.

```bash
# Count files that need touching (should be > 650 for `:app`, plus a few hundred for the other modules)
find . -type f \( -name "*.kt" -o -name "*.java" -o -name "*.gradle.kts" -o -name "*.toml" -o -name "*.pro" -o -name "*.md" -o -name "*.json" -o -name "*.ps1" -o -name "*.xml" \) 2>/dev/null | grep -v build | grep -v ".gradle/" | xargs grep -l "com\.streamvault" 2>/dev/null | wc -l
```

## Step 1 â€” Update Gradle build files

Edit three files manually so the rename is explicit and reviewable.

`app/build.gradle.kts` (around line 53):
```kotlin
namespace = "app.ehtudo.iptv"
applicationId = "app.ehtudo.iptv"
buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"app.ehtudo.iptv\"")
```

`data/build.gradle.kts` (around line 12):
```kotlin
namespace = "app.ehtudo.data"
```

`player/build.gradle.kts` (around line 12):
```kotlin
namespace = "app.ehtudo.player"
```

The `:domain` module has no `build.gradle.kts` namespace to change (it is pure Kotlin), but its `package com.streamvault.domain.*` files will be rewritten in step 2.

## Step 2 â€” Replace content in every tracked source / config file

```bash
# Run from repo root.
git ls-files '*.kt' '*.java' '*.xml' '*.gradle.kts' '*.toml' '*.pro' \
  '*.md' '*.json' '*.ps1' 2>/dev/null \
  | while read -r f; do
      if grep -q "com\.streamvault" "$f" 2>/dev/null; then
        sed -i 's/com\.streamvault/app.ehtudo/g' "$f"
      fi
    done
```

`sed -i` rewrites the `com.streamvault` prefix to `app.ehtudo` everywhere it appears â€” declarations, imports, FQCN strings, docs, scripts. The `:app` module is now `app.ehtudo.iptv` (because the `applicationId` is `app.ehtudo.iptv` and the source root is `com/streamvault/app` which becomes `app/ehtudo/app`).

`app.ehtudo.iptv` is then produced by the directory move in step 3 (renaming the final `app` segment to `iptv`).

Verify zero residual matches:
```bash
git ls-files '*.kt' '*.java' '*.xml' '*.gradle.kts' '*.toml' '*.pro' '*.md' '*.json' '*.ps1' 2>/dev/null \
  | xargs grep -l "com\.streamvault" 2>/dev/null | wc -l
# Expected output: 0
```

## Step 3 â€” Move physical directories via `git mv`

This preserves git history. Run from repo root:

```bash
# Rename the inner-most directories first.
for src in $(find app/src -type d -path "*/app/ehtudo/app" 2>/dev/null); do
    new_path=$(echo "$src" | sed 's|/app/ehtudo/app$|/app/ehtudo/iptv|')
    echo "  $src -> $new_path"
    git mv "$src" "$new_path"
done

# Remove the now-empty `app/ehtudo` parent directories so they don't shadow the `app` parent.
for d in $(find app/src -type d -path "*/app/ehtudo" 2>/dev/null | grep -v "/app/ehtudo/iptv"); do
    if [ -z "$(ls -A "$d" 2>/dev/null)" ]; then
        rmdir "$d"
        echo "  removed empty $d"
    fi
done
```

The other modules (`data`, `domain`, `player`) are also affected by step 2: their `com/streamvault/{data,domain,player}` directories got rewritten to `app/ehtudo/{data,domain,player}`. Verify:

```bash
find . -type d -path "*/com/streamvault*" 2>/dev/null | grep -v build | grep -v ".gradle/"
# Expected: empty
```

The `app/src/main/java/com.streamvault.app.zip` snapshot file (a reference archive tracked in git) should **not** be renamed â€” it predates the project. Move it back if step 3's logic accidentally relocates it.

## Step 4 â€” AndroidManifest, proguard, docs

Step 2 already rewrote:

- `app/src/main/AndroidManifest.xml` â€” `app.ehtudo.plugin.API`, the cast provider FQCN, etc.
- `app/proguard-rules.pro` â€” any FQCN references.
- `docs/DEV_SEEDING.md`, `docs/GOOGLE_DRIVE_SETUP.md`, `docs/PLUGIN_API.md` â€” `pm clear com.streamvault.app`, plugin API namespace, etc.
- `tools/cast-validation.ps1` â€” `$PackageName = "com.streamvault.app"`.
- `AGENTS.md` â€” runtime `package=com.streamvault.app` references in adb commands.

If you find anything still mentioning `com.streamvault` in any of those, repeat the targeted `sed` for the file in question.

## Step 5 â€” Wipe stale KSP / Hilt caches

Hilt and KSP cache class references to the old FQCN. Without a clean, the next build dies with `Could not find class file for 'app.ehtudo.app.StreamVaultApp'`.

```bash
rm -rf app/build/kspCaches
rm -rf app/build/tmp
rm -rf data/build/kspCaches player/build/kspCaches domain/build/kspCaches
```

## Step 6 â€” Build

```bash
./gradlew clean --no-daemon
./gradlew :app:assembleDebug --no-daemon
```

First build is ~10 min from cold. Expect only pre-existing warnings (KT-73255, unchecked casts in the ViewModels â€” see `app/src/main/java/app/ehtudo/iptv/ui/screens/movies/MoviesViewModel.kt` etc.). Any `Unresolved reference` error points to a file missed by step 2 or a stale cache from step 5.

## Step 7 â€” Install and launch

```bash
# Uninstall the old package (if it was previously installed under com.streamvault.app.debug)
adb -s d1d1b8f3 uninstall com.streamvault.app.debug 2>/dev/null

# Install the new APK
adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk

# Launch (note the new FQCN)
adb -s d1d1b8f3 shell am start -n app.ehtudo.iptv.debug/app.ehtudo.iptv.MainActivity
```

## Why we don't try to do this in one pass with the other simplifications

- The package rename pollutes `git diff` heavily (~700 files, mostly one-line `package` and `import` changes). Reviewing a 1500-line diff alongside logic changes is unreliable.
- KSP/Hilt caches become inconsistent and produce spooky build errors that hide other bugs.
- The user has the option to roll back a pure rename (it's reversible with `git mv` + `git checkout`) but cannot easily roll back a rename mixed with the best-effort activation change.

Treat it as a standalone commit on its own branch (`ehiptv/rename-packages`), merge it once, then build features on top of it.

## Anti-patterns (do not)

- Do **not** use a Python or Java rename script. `sed` + `git mv` covers every case and the diff is reviewable per file.
- Do **not** rename the `:app` module's last segment to anything other than `iptv` if the goal is to match the production `app.ehtudo.iptv`. Different suffixes (e.g. `app.ehtudo.player`, `app.ehtudo.viewer`) cause confusion and Play Store rejection.
- Do **not** rewrite the old `app/src/main/java/com.streamvault.app.zip` reference archive. It is intentionally a frozen snapshot.
- Do **not** rename `settings.gradle.kts` `rootProject.name` â€” this is the *Gradle* project name (used for IDE display), not the applicationId. They can diverge.
- Do **not** skip step 5 (cache wipe). The build *will* fail with stale KSP generated code referencing the old FQCN.
