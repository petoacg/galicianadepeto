package gal.galiciana.app.ui

import gal.galiciana.app.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sell
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
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        // Same bar as Gardados: a tab root has a title and no back arrow.
        SectionBar("Explorar", MaterialTheme.colorScheme.primary, onBack = {}, onHome = {}, showBack = false)
        LazyColumn(Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
            item {
                Text(tr("Percorre os fondos de Galiciana por publicación, colección ou materia.", "Recorre los fondos de Galiciana por publicación, colección o materia."), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
            }
            item { ExploreDestination(Icons.Outlined.Newspaper, "Hemeroteca", tr("Elixe unha cabeceira e percorre os seus anos e números.", "Elige una cabecera y recorre sus años y números."), onHemeroteca) }
            item { ExploreDestination(Icons.Outlined.CollectionsBookmark, tr("Coleccións", "Colecciones"), tr("Explora os fondos dixitalizados de Galiciana.", "Explora los fondos digitalizados de Galiciana."), onCollections) }
            item { ExploreDestination(Icons.Outlined.Sell, tr("Materias e datos do catálogo", "Materias y datos del catálogo"), tr("Busca persoas, lugares e materias; consulta rexistros e fontes.", "Busca personas, lugares y materias; consulta registros y fuentes."), onData) }
            item { ExploreDestination(Icons.Outlined.Info, tr("Acerca de e aviso legal", "Acerca de y aviso legal"), tr("Galiciana de Peto, créditos e condicións de uso.", "Galiciana de Peto, créditos y condiciones de uso."), onAbout) }
        }
        }
    }
}

@Composable
private fun ExploreDestination(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(vertical = 24.dp),
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
