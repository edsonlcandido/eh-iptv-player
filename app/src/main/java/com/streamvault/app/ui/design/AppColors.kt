package com.streamvault.app.ui.design

import androidx.compose.ui.graphics.Color
import com.streamvault.app.BuildConfig

object AppColors {
    val Canvas = Color(0xFF07111B)
    val CanvasElevated = Color(0xFF0B1622)
    val Surface = Color(0xFF0F1B29)
    val SurfaceElevated = Color(0xFF162338)
    val SurfaceEmphasis = Color(0xFF1D2E46)
    val SurfaceAccent = Color(0xFF223754)

    val Brand       = parseHexColor(BuildConfig.BRAND_PRIMARY_COLOR)
    val BrandMuted  = parseHexColor(BuildConfig.BRAND_DIM_COLOR)
    val BrandStrong = parseHexColor(BuildConfig.BRAND_SECONDARY_COLOR)
    val Focus = Color(0xFFF4F8FF)

    val TextPrimary = Color(0xFFF5F7FB)
    val TextSecondary = Color(0xFFBBC6D8)
    val TextTertiary = Color(0xFF7F8DA5)
    val TextDisabled = Color(0xFF566173)

    val Live = Color(0xFFFF5C61)
    val Success = Color(0xFF4FD39A)
    val Warning = Color(0xFFFFC766)
    val Info = Color(0xFF57C9FF)

    val Divider = Color(0x1AF4F8FF)
    val Outline = Color(0x264C6D95)

    val HeroTop = Color(0xCC07111B)
    val HeroBottom = Color(0xF207111B)
}

internal fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#").removePrefix("0x")
    val long = cleaned.toLong(16)
    return if (cleaned.length == 8) Color(long) else Color(0xFF000000L or long)
}
