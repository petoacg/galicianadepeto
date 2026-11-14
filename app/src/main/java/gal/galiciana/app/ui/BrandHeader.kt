package gal.galiciana.app.ui

import gal.galiciana.app.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The one header of the three tab roots (Buscar, Explorar, Gardados): the vieira as the
 * app's mark, then the destination's name, then its actions. Same height, margin and
 * type everywhere, so switching tabs changes the words, not the furniture.
 */
@Composable
fun TabRootBar(
    title: String,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 64.dp).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // 48dp box with a 24dp mark: the shell's edge lines up with the 20dp content margin.
            VieiraButton(selected = true, forcedCeleste = true, onClick = {}, size = 24.dp, interactive = false)
            CappedFontScale {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp).semantics { heading() })
            }
            actions()
        }
    }
}

@Composable
fun SectionBar(
    title: String,
    color: Color,
    onBack: () -> Unit,
    onHome: () -> Unit,
    showBack: Boolean = true,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    if (!showBack) {
        // A tab's own screen is a destination, not a step: the shared root header.
        TabRootBar(title, actions)
        return
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() })
            actions()
        }
    }
}

@Composable
fun BrandHeader(
    current: String = "Inicio",
    onHome: () -> Unit = {},
    onConsulta: () -> Unit = {},
    onHemeroteca: () -> Unit = {},
    onDatos: () -> Unit = {},
    onFavoritos: () -> Unit = {},
    compact: Boolean = false,
    onAbout: (() -> Unit)? = null,
    homeAction: Boolean = true,
) {
    TabRootBar(androidx.compose.ui.res.stringResource(gal.galiciana.app.R.string.app_name)) {
        onAbout?.let { action ->
            IconButton(onClick = action) {
                Icon(androidx.compose.material.icons.Icons.Outlined.Info, tr("Acerca de e aviso legal", "Acerca de y aviso legal"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

