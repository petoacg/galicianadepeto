package gal.galiciana.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val GalicianaBlue = Color(0xFF0B6EBD)
val GalicianaBlueDark = Color(0xFF084E88)
val PageBg = Color(0xFFFFFFFF)
val PageTint = Color(0xFFEEF5F8)
val MatteFill = Color(0xFFD4E2EB)

private val DarkColors = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF8BCCFF),
    onPrimary = Color(0xFF003556),
    secondary = Color(0xFFB0D5EF),
    tertiary = Color(0xFF9ECFE3),
    onTertiary = Color(0xFF12384A),
    primaryContainer = Color(0xFF12476D),
    onPrimaryContainer = Color(0xFFD9EEFF),
    secondaryContainer = Color(0xFF253D50),
    onSecondaryContainer = Color(0xFFD9EEFF),
    background = Color(0xFF101A23),
    surface = Color(0xFF15222D),
    surfaceVariant = Color(0xFF20313F),
    onBackground = Color(0xFFE5EDF3),
    onSurface = Color(0xFFE5EDF3),
    onSurfaceVariant = Color(0xFFB2C3D0),
    outline = Color(0xFF8296A5),
    outlineVariant = Color(0xFF354B5C),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GalicianaTheme(content: @Composable () -> Unit) {
    val colors = DarkColors
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = colors.primary.copy(alpha = 0.16f)),
    ) {
        MaterialTheme(colorScheme = colors,
            shapes = androidx.compose.material3.Shapes(
                small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
            content = content)
    }
}

@Composable
fun AppFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
}
