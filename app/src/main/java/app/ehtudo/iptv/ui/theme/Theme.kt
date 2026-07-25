package app.ehtudo.iptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import app.ehtudo.iptv.ui.design.AppColors
import app.ehtudo.iptv.ui.design.AppShapes
import app.ehtudo.iptv.ui.design.LocalAppShapes
import app.ehtudo.iptv.ui.design.LocalAppSpacing
import app.ehtudo.iptv.ui.design.rememberAppTypography

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Brand,
    onPrimary = OnPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceElevated,
    onSurfaceVariant = AppColors.TextSecondary,
    background = AppColors.CanvasElevated,
    onBackground = AppColors.TextPrimary,
    error = AppColors.Live,
    onError = OnPrimary
)

@Composable
fun StreamVaultTheme(content: @Composable () -> Unit) {
    val typography = rememberAppTypography()
    CompositionLocalProvider(
        LocalAppSpacing provides app.ehtudo.iptv.ui.design.AppSpacing(),
        LocalAppShapes provides AppShapes()
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = typography,
            content = content
        )
    }
}
