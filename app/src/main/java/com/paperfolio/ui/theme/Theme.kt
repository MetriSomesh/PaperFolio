package com.paperfolio.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// VELTO-style LIGHT color scheme: off-white background, white cards,
// dark-green hero / primary, lime CTA. There is no dark scheme —
// VELTO's app screens are light; the dark green lives only in the
// hero card, the active nav pill, and selected states.
private val LightColorScheme = lightColorScheme(
    primary = VeltoGreen,
    onPrimary = HeroText,
    primaryContainer = ProfitGreenBg,
    onPrimaryContainer = VeltoGreen,
    secondary = VeltoGreen,
    onSecondary = HeroText,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    error = LossRed,
    onError = Surface
)

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun PaperFolioTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = AppBackground.toArgb()
                window.navigationBarColor = AppBackground.toArgb()
                // Light bars: dark icons on off-white.
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
