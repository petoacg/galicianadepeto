package gal.galiciana.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Fixed-height chrome (navigation labels, app-bar titles) cannot grow without clipping.
 * Inside this scope text follows the system font size only up to [max]; body content
 * outside it keeps scaling freely.
 */
@Composable
fun CappedFontScale(max: Float = 1.3f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(max)),
        content = content,
    )
}
