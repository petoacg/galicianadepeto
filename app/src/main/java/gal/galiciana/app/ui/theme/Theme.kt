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
import androidx.compose.ui.unit.sp

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
    onSecondary = Color(0xFF15222D),
    tertiaryContainer = Color(0xFF1E4A5E),
    onTertiaryContainer = Color(0xFFD2EEF9),
    // Material fills unset container roles from its violet baseline; dialogs, menus
    // and sheets must step up from the night-blue canvas instead.
    surfaceDim = Color(0xFF101A23),
    surfaceContainerLowest = Color(0xFF0C141B),
    surfaceContainerLow = Color(0xFF15222D),
    surfaceContainer = Color(0xFF1A2834),
    surfaceContainerHigh = Color(0xFF1E2E3B),
    surfaceContainerHighest = Color(0xFF253746),
    surfaceBright = Color(0xFF2B3D4C),
    inverseSurface = Color(0xFFE5EDF3),
    inverseOnSurface = Color(0xFF15222D),
    inversePrimary = Color(0xFF12476D),
)

/**
 * System sans, as DESIGN.md fixes, with an archive's habits: titles of destinations and
 * publications carry weight, and every figure is tabular so dates, issue numbers and
 * page counts line up like a catalogue's columns.
 */
private val GalicianaTypography = androidx.compose.material3.Typography().let { base ->
    fun androidx.compose.ui.text.TextStyle.archive(
        weight: androidx.compose.ui.text.font.FontWeight? = null,
        tracking: androidx.compose.ui.unit.TextUnit? = null,
    ) = copy(
        fontWeight = weight ?: fontWeight,
        letterSpacing = tracking ?: letterSpacing,
        fontFeatureSettings = "tnum",
    )
    val bold = androidx.compose.ui.text.font.FontWeight.Bold
    val semi = androidx.compose.ui.text.font.FontWeight.SemiBold
    val medium = androidx.compose.ui.text.font.FontWeight.Medium
    base.copy(
        displayLarge = base.displayLarge.archive(bold, (-0.5).sp),
        displayMedium = base.displayMedium.archive(bold, (-0.5).sp),
        displaySmall = base.displaySmall.archive(bold, (-0.25).sp),
        headlineLarge = base.headlineLarge.archive(bold, (-0.5).sp),
        headlineMedium = base.headlineMedium.archive(bold, (-0.25).sp),
        headlineSmall = base.headlineSmall.archive(semi, (-0.1).sp),
        titleLarge = base.titleLarge.archive(semi),
        titleMedium = base.titleMedium.archive(semi, 0.sp),
        titleSmall = base.titleSmall.archive(medium),
        bodyLarge = base.bodyLarge.archive(),
        bodyMedium = base.bodyMedium.archive(),
        bodySmall = base.bodySmall.archive(),
        labelLarge = base.labelLarge.archive(),
        labelMedium = base.labelMedium.archive(),
        labelSmall = base.labelSmall.archive(),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GalicianaTheme(content: @Composable () -> Unit) {
    val colors = DarkColors
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = colors.primary.copy(alpha = 0.16f)),
    ) {
        MaterialTheme(colorScheme = colors,
            typography = GalicianaTypography,
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

/** Folders are archive objects, not actions: one amber everywhere a folder is drawn. */
val FolderAmber = Color(0xFFE6C27A)

/** True when the user turned off animations in the system settings. */
fun motionReduced(context: android.content.Context): Boolean =
    android.provider.Settings.Global.getFloat(context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

