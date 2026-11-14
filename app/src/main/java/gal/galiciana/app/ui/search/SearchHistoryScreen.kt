package gal.galiciana.app.ui.search

import gal.galiciana.app.i18n.tr
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
fun SearchHistoryScreen(onBack: () -> Unit, onOpen: (SearchPreset) -> Unit, onForm: () -> Unit = onBack) {
    val context = LocalContext.current
    val history = remember { SearchHistory(context) }
    var savedTab by rememberSaveable { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var clear by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val values = remember(savedTab, tick) { if (savedTab) history.saved() else history.recent() }
    Scaffold(contentWindowInsets = WindowInsets(0,0,0,0), topBar = {
        SectionBar(tr("As túas buscas", "Tus búsquedas"), MaterialTheme.colorScheme.primary, onBack, onBack)
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)) {
            item {
                Text(tr("Retoma unha liña de investigación coa súa consulta e os seus filtros.", "Retoma una línea de investigación con su consulta y sus filtros."), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    gal.galiciana.app.ui.SelectChip(savedTab, { savedTab = true }, label = { Text(tr("Gardadas", "Guardadas")) })
                    gal.galiciana.app.ui.SelectChip(!savedTab, { savedTab = false }, label = { Text(tr("Recentes", "Recientes")) })
                }
                Text(tr("Gárdanse neste teléfono e inclúense na túa copia de datos. As consultas a Galiciana precisan conexión.", "Se guardan en este teléfono y se incluyen en tu copia de datos. Las consultas a Galiciana necesitan conexión."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!savedTab && values.isNotEmpty()) TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { clear = true }) { Text(tr("Borrar recentes", "Borrar recientes")) }
                message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
                if (values.isEmpty()) Text(if (savedTab) tr("Aínda non gardaches buscas. Podes facelo desde o formulario de busca.", "Todavía no has guardado búsquedas. Puedes hacerlo desde el formulario de búsqueda.")
                    else tr("Aquí aparecerán as túas últimas 40 consultas.", "Aquí aparecerán tus últimas 40 consultas."), modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                if (values.isEmpty() && savedTab) androidx.compose.material3.OutlinedButton(onClick = onForm,
                    modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("Ir ao formulario", "Ir al formulario")) }
            }
            items(values, key = { it.id }) { preset ->
                ListItem(headlineContent = { Text(preset.label()) }, supportingContent = { Text(preset.summary()) },
                    trailingContent = {
                        Row {
                        IconButton(onClick = {
                            val ok = if (savedTab) history.removeSaved(preset.id) else history.save(preset)
                            message = if (!ok) tr("Non se puido gardar o cambio. Téntao de novo.", "No se pudo guardar el cambio. Vuelve a intentarlo.")
                                else if (savedTab) tr("Busca quitada de Gardadas.", "Búsqueda quitada de Guardadas.") else tr("Busca gardada.", "Búsqueda guardada.")
                            tick++
                        }) { Icon(if (savedTab) Icons.Outlined.DeleteOutline else Icons.Outlined.BookmarkAdd,
                            if (savedTab) tr("Quitar busca gardada", "Quitar búsqueda guardada") else tr("Gardar busca", "Guardar búsqueda")) }
                        if (!savedTab) IconButton(onClick = {
                            message = if (history.removeRecent(preset.id)) tr("Consulta quitada de Recentes.", "Consulta quitada de Recientes.") else tr("Non se puido quitar a consulta.", "No se pudo quitar la consulta.")
                            tick++
                        }) { Icon(Icons.Outlined.DeleteOutline, tr("Quitar busca recente", "Quitar búsqueda reciente")) }
                        }
                    }, modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { onOpen(preset) })
                HorizontalDivider()
            }
        }
    }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text(tr("Borrar buscas recentes", "Borrar búsquedas recientes")) },
        text = { Text(tr("As buscas gardadas consérvanse.", "Las búsquedas guardadas se conservan.")) }, confirmButton = {
            TextButton(onClick = { if (!history.clearRecent()) message = tr("Non se puideron borrar as consultas.", "No se pudieron borrar las consultas."); tick++; clear = false }) { Text(tr("Borrar recentes", "Borrar recientes")) }
        }, dismissButton = { TextButton(onClick = { clear = false }) { Text("Cancelar") } })
}
