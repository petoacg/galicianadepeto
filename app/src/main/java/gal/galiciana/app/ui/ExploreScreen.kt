package gal.galiciana.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ExploreScreen(onHemeroteca: () -> Unit, onCollections: () -> Unit, onData: () -> Unit, onAbout: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)) {
            item {
                Text("Explorar", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                Text("Otra forma de llegar a tus documentos.", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
            }
            item { ExploreDestination(Icons.Outlined.Newspaper, "Hemeroteca", "Elige una cabecera y recorre sus años y números.", onHemeroteca) }
            item { ExploreDestination(Icons.Outlined.CollectionsBookmark, "Colecciones", "Explora los fondos digitalizados de Galiciana.", onCollections) }
            item { ExploreDestination(Icons.Outlined.DataObject, "Materias y datos del catálogo", "Busca personas, lugares y materias; consulta registros y fuentes.", onData) }
            item { ExploreDestination(Icons.Outlined.Info, "Acerca de y aviso legal", "Galiciana de Peto, créditos y condiciones de uso.", onAbout) }
        }
    }
}

@Composable
private fun ExploreDestination(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
