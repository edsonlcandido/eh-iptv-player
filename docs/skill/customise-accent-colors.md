# Skill 10 — Customise the accent (brand) color palette

## Goal

Rebrand the Eh! IPTV player's **brand color** — the blue accent that shows up on the welcome button, top-nav selected pill, settings rail selection, focus ring, link text, badge backgrounds, and the highlights on selected cards — to a new color (e.g. orange) by editing **3 lines** in a single file, with no logic changes and no theme refactor.

The user must continue to see:
- The same dark blue-navy background (it does not change)
- The same white/gray text palette (it does not change)
- The same semantic colors for live (red), success (green), warning (amber), error (red) — those are *informational*, not branding
- The same layout, focus behavior, and animations

Only the **brand-accent family** (`Brand*` in `AppColors`) changes.

## Where the color system lives

The StreamVault theme was refactored during the Eh! IPTV customisation so that **all** brand colors flow from a single `object AppColors` in one file. There is exactly one source of truth and exactly one place to edit.

### File 1 — `app/src/main/java/app/ehtudo/iptv/ui/design/AppColors.kt` (the truth)

```kotlin
package app.ehtudo.iptv.ui.design

import androidx.compose.ui.graphics.Color

object AppColors {
    // Backgrounds (do NOT change for branding — these are the canvas)
    val Canvas          = Color(0xFF07111B)
    val CanvasElevated  = Color(0xFF0B1622)
    val Surface         = Color(0xFF0F1B29)
    val SurfaceElevated = Color(0xFF162338)
    val SurfaceEmphasis = Color(0xFF1D2E46)
    val SurfaceAccent   = Color(0xFF223754)

    // Brand accent (CHANGE THESE for branding)
    val Brand           = Color(0xFF69A8FF)  // primary accent — focus, link, brand text
    val BrandMuted      = Color(0x335FA4FF)  // same with 20% alpha — glows, hovers
    val BrandStrong     = Color(0xFF8BBCFF)  // lighter — selected pill, button fill

    val Focus           = Color(0xFFF4F8FF)  // white-ish focus border

    // Text (do NOT change for branding)
    val TextPrimary     = Color(0xFFF5F7FB)
    val TextSecondary   = Color(0xFFBBC6D8)
    val TextTertiary    = Color(0xFF7F8DA5)
    val TextDisabled    = Color(0xFF566173)

    // Semantic (do NOT change for branding — these are *informational*)
    val Live            = Color(0xFFFF5C61)  // "AO VIVO" badge, recording, errors
    val Success         = Color(0xFF4FD39A)  // "ATIVO", OK states
    val Warning         = Color(0xFFFFC766)  // scheduled recording, caution
    val Info            = Color(0xFF57C9FF)  // "ALCançAR" badge, info chips

    val Divider         = Color(0x1AF4F8FF)
    val Outline         = Color(0x264C6D95)

    val HeroTop         = Color(0xCC07111B)
    val HeroBottom      = Color(0xF207111B)
}
```

### File 2 — `app/src/main/java/app/ehtudo/iptv/ui/theme/Color.kt` (aliases, do not edit)

This file re-exports the `AppColors` constants under shorter names (`Primary`, `BackgroundDeep`, `AccentRed`, `OnPrimary`, etc) so the rest of the codebase can reference them semantically. **Do not change hex values here** — they are aliases. If you need a new color, add it to `AppColors` first, then re-export it from `Color.kt`.

```kotlin
// app/src/main/java/app/ehtudo/iptv/ui/theme/Color.kt
val Primary       = AppColors.Brand
val PrimaryLight  = AppColors.BrandStrong
val PrimaryGlow   = AppColors.BrandMuted

val AccentRed     = AppColors.Live
val AccentGreen   = AppColors.Success
val AccentAmber   = AppColors.Warning
val AccentCyan    = AppColors.Info
// ... etc
```

The `Theme.kt` Material 3 wrapper then composes these into `lightColorScheme(...)` / `darkColorScheme(...)`. **Do not edit `Theme.kt` for branding** — it is correct as-is.

## The 3 lines that change everything

To rebrand the app, edit **only** the `Brand*` family in `AppColors.kt`:

```kotlin
// BEFORE (StreamVault blue):
val Brand       = Color(0xFF69A8FF)
val BrandMuted  = Color(0x335FA4FF)
val BrandStrong = Color(0xFF8BBCFF)

// AFTER (Eh! IPTV orange, example):
val Brand       = Color(0xFFFF6A1A)        // dark orange — brand text, focus border, links
val BrandMuted  = Color(0x33FF6A1A)        // same orange with 20% alpha — hovers, glows
val BrandStrong = Color(0xFFFF8A3D)        // light orange — selected pill, button fill
```

That's it. No XML theme override, no `build.gradle.kts` change, no resource recompile, no Hilt module regen. The KSP cache does not need to be wiped. The 26 composables that import `AppColors.Brand` / `Primary` / `PrimaryLight` will pick up the new values automatically on the next incremental build.

## What changes visually

The `Brand*` family is used in 26 files. When you change it, every one of the following flips to the new color **simultaneously** with no extra work:

### UI surfaces that become the new color

| Surface | What the user sees |
|---|---|
| Welcome → "Salvar" button background | Orange (was blue) |
| Welcome → "Fale conosco pelo WhatsApp" link | Orange (was blue) |
| Settings → "Salvar" button (inline provider form) | Orange |
| Top nav selected pill background (TV ao vivo, Filmes, etc) | Orange |
| Sidebar selected item highlight (Categorias, Provedores) | Orange bar/text |
| Focus ring around the currently focused element | Orange (was blue, was white before that) |
| Selected card border outline in lists (canal, filme, série) | Orange (was blue-cyan) |
| "ATIVO" / "Ativo" badge backgrounds (provider cards) | Orange tint |
| "Diagnóstico do provedor" section title text | Orange |
| "Pendente" / progress chip text | Orange |
| Tab indicator (Material 3 selected tab underline) | Orange |
| Switch thumb when ON | Orange |
| Slider thumb and active track | Orange |
| Selected radio button inner circle | Orange |
| Checkbox tick | Orange |
| Hyperlink text in dialogs and info text | Orange |

### UI surfaces that stay the same color

| Surface | Why it does not change |
|---|---|
| App background (canvas, surfaces, cards) | `Canvas*` and `Surface*` are not part of the `Brand` family |
| Text (primary, secondary, tertiary) | `Text*` is a separate family |
| "AO VIVO" badge | Uses `AccentRed = AppColors.Live` — semantic, not branding |
| "REC" badge (recording) | Uses `AccentRed` |
| Error states, error text | Uses `AccentRed` |
| "ATIVO" / "Parcial" status text in green | Uses `AccentGreen = AppColors.Success` |
| "Scheduled" / caution badges in yellow | Uses `AccentAmber = AppColors.Warning` |
| "ALCançAR" / info chips in cyan | Uses `AccentCyan = AppColors.Info` |
| Filled input field background | Uses `Surface*` |

The boundary is clean: **`Brand*` = visual identity. `Live` / `Success` / `Warning` / `Info` = semantic information.** They never overlap.

## How to pick a new color palette

### The three constraints

A `Brand*` family is **3 hex values that must be**:
1. **Visually related** — they are the same color at three brightness/alpha levels. If you pick three totally different colors the UI will look broken.
2. **Readable on the dark canvas** (`#0B1622`) — test contrast in your head: a light pastel on a dark navy. Mid-saturation works better than fully saturated.
3. **Distinct from the semantic colors** — do not pick a green (collides with `Success`), a yellow (collides with `Warning`), or a red (collides with `Live`). Orange, purple, teal, pink, and mid-blue are all safe.

### Recommended formula for a new color

If you want to rebrand to a color **H**, where H is your hue (e.g. for orange you have a base like `#FF7A1A`):

| Token | How to derive | Example for orange `#FF7A1A` |
|---|---|---|
| `Brand` (dark/primary) | Use the base color or darken by ~10% | `#FF6A1A` |
| `BrandMuted` | Same hex with `0x33` alpha prefix | `#33FF6A1A` |
| `BrandStrong` (light/fill) | Lighten by ~15% (mix toward white) | `#FF8A3D` |

The "darken by 10%" / "lighten by 15%" rule keeps the three values visually consistent. If you want a more aggressive palette, push BrandStrong toward white (`#FFB37A`) for a softer, pastel feel.

### Worked examples for common brand colors

These are battle-tested palettes against the `#0B1622` dark canvas. Pick one as a starting point and tweak:

#### Orange (Eh! IPTV default)

```kotlin
val Brand       = Color(0xFFFF6A1A)
val BrandMuted  = Color(0x33FF6A1A)
val BrandStrong = Color(0xFFFF8A3D)
```

#### Red (YouTube TV-style)

```kotlin
val Brand       = Color(0xFFFF3D3D)
val BrandMuted  = Color(0x33FF3D3D)
val BrandStrong = Color(0xFFFF6B6B)
```

**Warning:** red-on-dark looks great but visually overlaps with `Live` badge. Use only if you do not mind the "AO VIVO" badge blending with the focus ring.

#### Purple (Twitch-style)

```kotlin
val Brand       = Color(0xFF9146FF)
val BrandMuted  = Color(0x339146FF)
val BrandStrong = Color(0xFFB280FF)
```

#### Teal (HBO Max-style)

```kotlin
val Brand       = Color(0xFF00D4D4)
val BrandMuted  = Color(0x3300D4D4)
val BrandStrong = Color(0xFF66E5E5)
```

#### Pink (Globo Play-style)

```kotlin
val Brand       = Color(0xFFFF4D8D)
val BrandMuted  = Color(0x33FF4D8D)
val BrandStrong = Color(0xFFFF8AB3)
```

## Optional: making the "AO VIVO" badge match the new brand

If you want the brand color to be even more dominant (and the "AO VIVO" badge to feel part of the brand instead of "alert red"), edit the `Live` semantic color too:

```kotlin
// BEFORE (default StreamVault red):
val Live        = Color(0xFFFF5C61)

// AFTER (warm orange to match brand):
val Live        = Color(0xFFFF6A1A)
```

**Trade-off:** the "AO VIVO" badge stops being a "red alert" and becomes a brand color. This is fine for single-tenant reseller builds (where the badge is a brand element, not a warning), but breaks the universal "red = live" convention that users expect from YouTube, Netflix, Globoplay, etc. **Only do this if the user explicitly opts in.**

Similarly, `Info` (used for "ALCançAR" badge) can be retinted if you want the "has archive" indicator to match the brand instead of looking like a separate semantic color:

```kotlin
// BEFORE (cyan):
val Info = Color(0xFF57C9FF)

// AFTER (light orange to match brand):
val Info = Color(0xFFFFB37A)
```

## Anti-patterns (do not)

- Do **not** create a second `object AppColors` or a `BrandColors` parallel hierarchy. The whole point of the refactor was one source of truth.
- Do **not** inline `Color(0xFF...)` literals in composables. Always reference `AppColors.X` or the alias in `Color.kt`. If you find yourself wanting to inline a color, it probably belongs in `AppColors`.
- Do **not** put the brand hex in `build.gradle.kts` as a `buildConfigField`. The color is a Compose runtime value, not a build-time constant.
- Do **not** create a "theme switcher" / dark vs light theme override for the brand. The app is dark-only.
- Do **not** add a new `R.color.brand_*` in `colors.xml`. The XML color system is not used for Compose — Compose reads from `AppColors.kt` directly.
- Do **not** edit `Color.kt` to add a new alias without first adding the underlying constant in `AppColors.kt`. The alias file is a pure re-export.
- Do **not** add a fourth color to the `Brand*` family (e.g. `BrandStronger`, `BrandLighter`). The 3-value scale is what makes the UI feel consistent. If you need a 4th value, you are solving a different problem (probably a new semantic color, not a brand variant).
- Do **not** swap the `Live` color from red to orange on a multi-tenant / reseller build where "AO VIVO" is meant to read as urgent. Reserve that change for the single-tenant Eh! IPTV build.

## Verification after the change

1. Build:
   ```bash
   ./gradlew :app:assembleDebug --no-daemon
   ```
2. Install (preserves the user's login):
   ```bash
   adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Take a screenshot and compare against a pre-change reference:
   ```bash
   adb -s d1d1b8f3 exec-out screencap -p > /tmp/brand_after.png
   ```
4. Walk through the four screens that exercise every brand surface:
   - **Welcome screen** → confirm "Salvar" button is the new color
   - **Top nav + TV ao vivo** → confirm the selected pill is the new color
   - **Settings → Provedores** → confirm the sidebar selection, "Salvar" button, and "ATIVO" badges are the new color
   - **Any list with a selected card** (Live TV channel list, Movies, Series) → confirm the focus border around the selected card is the new color
5. Verify the semantic colors did **not** change:
   - "AO VIVO" badge in TV list → still red
   - Green "ATIVO" / "Parcial" status text → still green
   - Yellow scheduled / warning badges → still yellow
6. If you changed `Live` to match the brand (optional), re-verify the "AO VIVO" badge is now the brand color.
7. Confirm the dark canvas and text colors did not change — they should be identical to the pre-change screenshot.
