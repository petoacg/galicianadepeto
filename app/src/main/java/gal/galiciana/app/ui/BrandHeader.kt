package gal.galiciana.app.ui

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
import gal.galiciana.app.ui.theme.GalicianaBlue
import gal.galiciana.app.ui.theme.GalicianaBlueDark

val SectionConsulta = Color(0xFF0B6EBD)
val SectionSucesos = Color(0xFF8B2A36)
val SectionHemeroteca = Color(0xFF0B4F86)
val SectionColecciones = Color(0xFF1B6B4A)
val SectionDatos = Color(0xFF3D4A5C)

@Composable
fun SectionBar(
    title: String,
    color: Color,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() })
        }
    }
}

@Composable
fun GalicianaMark(
    onClick: () -> Unit = {},
    size: androidx.compose.ui.unit.Dp = 36.dp,
    onBar: Boolean = true,
    barColor: Color = GalicianaBlue,
) {
    Box(
        modifier = Modifier
            .size(maxOf(48.dp, size))
            .semantics { contentDescription = "Inicio" }
            .clip(RoundedCornerShape(4.dp))
            .background(barColor)
            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "G",
            modifier = Modifier.clearAndSetSemantics { },
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.52f).sp,
            letterSpacing = (-0.5).sp,
        )
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
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 64.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VieiraButton(selected = true, forcedCeleste = true, onClick = onHome, size = 28.dp, contentLabel = "Inicio")
            Column(Modifier.weight(1f)) {
                Text(androidx.compose.ui.res.stringResource(gal.galiciana.app.R.string.app_name), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (!compact) Text(current, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            onAbout?.let { action ->
                IconButton(onClick = action) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.Info, "Acerca de y aviso legal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        color = Color.White.copy(alpha = if (selected) 1f else 0.88f),
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.heightIn(min = 48.dp).semantics { this.selected = selected }.clickable(role = Role.Tab, onClick = onClick)
    )
}
