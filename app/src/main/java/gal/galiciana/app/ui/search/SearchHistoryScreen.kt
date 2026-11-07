package gal.galiciana.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gal.galiciana.app.data.SearchHistory
import gal.galiciana.app.data.SearchPreset
import gal.galiciana.app.ui.SectionBar

@Composable
fun SearchHistoryScreen(onBack: () -> Unit, onOpen: (SearchPreset) -> Unit) {
    val context = LocalContext.current
    val history = remember { SearchHistory(context) }
    var savedTab by rememberSaveable { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var clear by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val values = remember(savedTab, tick) { if (savedTab) history.saved() else history.recent() }
    Scaffold(contentWindowInsets = WindowInsets(0,0,0,0), topBar = {
        SectionBar("Tus búsquedas", MaterialTheme.colorScheme.primary, onBack, onBack)
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Text("Retoma una línea de investigación con su consulta y sus filtros.", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    FilterChip(savedTab, { savedTab = true }, label = { Text("Guardadas") })
                    FilterChip(!savedTab, { savedTab = false }, label = { Text("Recientes") })
                }
                Text("Se guardan en este teléfono y se incluyen en tu copia de datos. Las consultas a Galiciana necesitan conexión.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!savedTab && values.isNotEmpty()) TextButton(onClick = { clear = true }) { Text("Borrar recientes") }
                message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
                if (values.isEmpty()) Text(if (savedTab) "Todavía no has guardado búsquedas. Puedes hacerlo desde el formulario de búsqueda."
                    else "Aquí aparecerán tus últimas 40 consultas.", modifier = Modifier.padding(vertical = 24.dp))
            }
            items(values, key = { it.id }) { preset ->
                ListItem(headlineContent = { Text(preset.label()) }, supportingContent = { Text(preset.summary()) },
                    trailingContent = {
                        Row {
                        IconButton(onClick = {
                            val ok = if (savedTab) history.removeSaved(preset.id) else history.save(preset)
                            message = if (!ok) "No se pudo guardar el cambio. Vuelve a intentarlo."
                                else if (savedTab) "Búsqueda quitada de Guardadas." else "Búsqueda guardada."
                            tick++
                        }) { Icon(if (savedTab) Icons.Outlined.DeleteOutline else Icons.Outlined.BookmarkAdd,
                            if (savedTab) "Quitar búsqueda guardada" else "Guardar búsqueda") }
                        if (!savedTab) IconButton(onClick = {
                            message = if (history.removeRecent(preset.id)) "Consulta quitada de Recientes." else "No se pudo quitar la consulta."
                            tick++
                        }) { Icon(Icons.Outlined.DeleteOutline, "Quitar búsqueda reciente") }
                        }
                    }, modifier = Modifier.clickable { onOpen(preset) })
                HorizontalDivider()
            }
        }
    }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("Borrar búsquedas recientes") },
        text = { Text("Las búsquedas guardadas se conservan.") }, confirmButton = {
            TextButton(onClick = { if (!history.clearRecent()) message = "No se pudieron borrar las consultas."; tick++; clear = false }) { Text("Borrar recientes") }
        }, dismissButton = { TextButton(onClick = { clear = false }) { Text("Cancelar") } })
}
