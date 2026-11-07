package gal.galiciana.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gal.galiciana.app.data.GalicianaUrls
import gal.galiciana.app.data.LocalStore
import gal.galiciana.app.ui.BrandHeader
import gal.galiciana.app.ui.ClearableField
import gal.galiciana.app.ui.VieiraButton
import gal.galiciana.app.ui.search.normalizeSearchInput

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onSearchQuery: (String) -> Unit = {},
    onAbout: () -> Unit = {},
    onHistory: () -> Unit = {},
    onFavoritos: () -> Unit = {},
    onOpenSaved: (String, Int, String?, String) -> Unit = { _, _, _, _ -> },
    onOpenFeatured: (String, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    val saved = store.favHits()
    val lastReading = store.lastReading()
    var query by rememberSaveable { mutableStateOf("") }
    var showSources by rememberSaveable { mutableStateOf(false) }
    fun search() {
        normalizeSearchInput(query).takeIf { it.isNotBlank() }?.let(onSearchQuery)
    }
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()
            .verticalScroll(rememberScrollState())) {
            BrandHeader(current = "Buscar", onConsulta = onOpenSearch,
                onFavoritos = onFavoritos, compact = true, onAbout = onAbout)
            Column(Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)
                .fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text("Busca en Galiciana", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(8.dp))
                Text("Personas, lugares e historias entre sus páginas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                ClearableField(value = query, onValueChange = { query = it },
                    placeholder = "Palabras, personas o lugares", label = "Tu búsqueda",
                    onSearch = { search() })
                Spacer(Modifier.height(12.dp))
                Button(onClick = { search() }, enabled = normalizeSearchInput(query).isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onHistory) { Text("Tus búsquedas") }
                    TextButton(onClick = onOpenSearch) { Text("Buscar con filtros") }
                }
                if (lastReading?.path != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Continuar leyendo", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    val identity = gal.galiciana.app.ui.publicationIdentity(lastReading)
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        headlineContent = { Text(identity.shortTitle.ifBlank { "Documento" }, maxLines = 2,
                            overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(listOfNotNull(identity.date, identity.issue, "Página ${lastReading.position ?: 1}").joinToString(" · "))
                        },
                        leadingContent = { lastReading.imageId?.let { id ->
                            AsyncImage(GalicianaUrls.thumb(id), contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop)
                        } },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                            onOpenSaved(lastReading.path, lastReading.position ?: 1, lastReading.imageId, lastReading.savedQuery)
                        },
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    VieiraButton(selected = true, onClick = onFavoritos, size = 28.dp,
                        forcedCeleste = true)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("Guardados", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(if (saved.isEmpty()) "Guarda páginas con la vieira para volver a ellas."
                            else "${saved.size} ${if (saved.size == 1) "página guardada" else "páginas guardadas"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onFavoritos) { Text("Abrir") }
                }
                if (saved.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(saved.take(6), key = { "${it.path}|${it.position}" }) { hit ->
                            val identity = gal.galiciana.app.ui.publicationIdentity(hit)
                            Column(Modifier.width(144.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) {
                                hit.path?.let { onOpenSaved(it, hit.position ?: 1, hit.imageId, hit.savedQuery) }
                            }) {
                                AsyncImage(hit.imageId?.let(GalicianaUrls::thumb), null,
                                    modifier = Modifier.fillMaxWidth().height(80.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop)
                                Text(identity.shortTitle.ifBlank { "Documento" }, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 6.dp))
                                identity.date?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                                Text(listOfNotNull(identity.issue, "Página ${hit.position ?: 1}").joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("Obras destacadas", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(ObrasDestacadas, key = { it.recordId }) { work ->
                        Column(Modifier.width(148.dp).clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenFeatured(work.recordId, work.title) }) {
                            AsyncImage(GalicianaUrls.thumb(work.imageId), null,
                                modifier = Modifier.fillMaxWidth().height(168.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop)
                            Text(work.title, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp))
                            Text(work.year, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                TextButton(onClick = onAbout) { Text("Acerca de · Aviso legal y fuentes") }

            }
        }
    }
}

@Composable
private fun ExploreRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SourceLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}

private data class ObraDestacada(
    val title: String,
    val year: String,
    val imageId: String,
    val recordId: String,
)

private val ObrasDestacadas = listOf(
    ObraDestacada("Grandes fiestas en Porriño", "1960", "13124554", "577655"),
    ObraDestacada("Puebla del Caramiñal: fiestas de septiembre", "1960", "13787413", "583444"),
    ObraDestacada("II Certamen Internacional de Cine de Humor", "1974", "14185891", "615091"),
    ObraDestacada("Fiestas del Berbés", "1963", "13872855", "586681"),
    ObraDestacada("Programa de las fiestas del Patrón", "1888", "13948376", "417147"),
    ObraDestacada("Grandes fiestas tradicionales del Apóstol", "1939", "14098722", "601479"),
    ObraDestacada("La voz del Centro", "1917-1918", "14175789", "615025"),
    ObraDestacada("El agrario", "1918-1926", "14153014", "615029"),
)
