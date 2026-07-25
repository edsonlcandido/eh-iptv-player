package app.ehtudo.iptv.ui.theme

import app.ehtudo.iptv.ui.design.AppSpacing
import app.ehtudo.iptv.ui.design.LocalAppSpacing

typealias Spacing = AppSpacing

val LocalSpacing = LocalAppSpacing

fun defaultSpacing(): Spacing = AppSpacing()
