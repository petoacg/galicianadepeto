package gal.galiciana.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import gal.galiciana.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun BackupScreen(onBack: () -> Unit, onRestored: () -> Unit, onBusyChanged: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val archive = remember { ArchiveBackup(context) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var report by remember { mutableStateOf<ImportReport?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var replace by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    var showDifferences by remember { mutableStateOf(false) }
    var resolutions by remember { mutableStateOf<Map<String, BackupResolution>>(emptyMap()) }
    var existingConflicts by remember { mutableStateOf(archive.conflicts()) }
    DisposableEffect(busy) { onBusyChanged(busy); onDispose { onBusyChanged(false) } }
    BackHandler(busy || report != null) { if (!busy) onRestored() }
    fun failure(e: Exception) { error = e.message ?: "No se pudo completar la operación. Tus datos actuales se conservan." }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            busy = true; error = null; message = null
            try {
                val result = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { archive.export(it) }
                        ?: kotlin.error("No se pudo abrir el archivo de destino.")
                }
                message = "Copia guardada: ${result.counts.favorites} páginas marcadas, ${result.counts.notes} notas, ${result.counts.folders} carpetas."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure(e) }
            finally { busy = false }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true; error = null; message = null; preview = null; resolutions = emptyMap(); replace = false
            try {
                preview = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { archive.preview(it) }
                        ?: kotlin.error("No se pudo abrir la copia.")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure(e) }
            finally { busy = false }
        }
    }
    fun restore(replacing: Boolean) {
        val selected = preview ?: return
        scope.launch {
            busy = true; error = null; message = null
            try {
                report = archive.importBackup(selected, ImportOptions(
                    mode = if (replacing) ImportMode.REPLACE else ImportMode.MERGE,
                    resolutions = resolutions, confirmReplace = replacing))
                preview = null
                existingConflicts = archive.conflicts()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure(e); preview = null }
            finally { busy = false }
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0,0,0,0), topBar = {
        SectionBar("Copia de tus datos", MaterialTheme.colorScheme.primary,
            { if (!busy) { if (report != null) onRestored() else onBack() } }, onBack)
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Tu investigación, contigo", style = MaterialTheme.typography.headlineSmall)
                Text("Guarda una copia para recuperar tus páginas marcadas, carpetas, Notas de referencia, búsquedas y preferencias en este u otro teléfono.",
                    modifier = Modifier.padding(top = 12.dp))
                Text("Sin cuenta ni inicio de sesión. La copia no incluye imágenes ni PDF. La consulta de documentos sigue siendo online; las descargas habituales se mantienen.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp))
                if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp)); Text("Preparando tus datos…") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                message?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
            }
            if (report != null) item {
                val result = report!!
                Text(if (result.replaced) "Copia restaurada" else "Datos combinados", style = MaterialTheme.typography.titleLarge)
                Counts(result.counts)
                Text(if (result.conflicts.isEmpty()) "La operación se ha completado."
                    else "Se conservaron ${result.conflicts.size} diferencias para que puedas revisar ambas versiones.")
                Button(onClick = onRestored, modifier = Modifier.fillMaxWidth()) { Text("Volver al inicio") }
            } else {
                item {
                    Button(onClick = { exporter.launch("galiciana-de-peto-${java.time.LocalDate.now()}.zip") }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Guardar copia de datos") }
                    OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)) { Text("Abrir una copia para importar") }
                }
                preview?.let { selected ->
                    item {
                        HorizontalDivider(); Text("Vista previa de la copia", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp))
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(selected.createdAt)),
                            style = MaterialTheme.typography.bodySmall)
                        Counts(selected.counts)
                        Text("En este teléfono: ${selected.currentCounts.favorites} páginas, ${selected.currentCounts.notes} notas y ${selected.currentCounts.folders} carpetas.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(replace, { replace = it }, enabled = !busy)
                            Text("Reemplazar los datos de este teléfono")
                        }
                        Text(if (replace) "Se restaurará el contenido de la copia en lugar de los datos actuales."
                            else "Se añadirán los datos que falten. Si hay diferencias, se conservan ambas versiones salvo que elijas otra opción.",
                            style = MaterialTheme.typography.bodySmall)
                        if (selected.conflicts.isNotEmpty()) TextButton(onClick = { showDifferences = !showDifferences }) {
                            Text("${if (showDifferences) "Ocultar" else "Revisar"} ${selected.conflicts.size} diferencias")
                        }
                    }
                    if (showDifferences && !replace) items(selected.conflicts, key = { it.id }) { conflict ->
                        Column {
                            ConflictComparison(conflict)
                            listOf(BackupResolution.KEEP_BOTH to "Conservar ambas", BackupResolution.KEEP_LOCAL to "Conservar la actual",
                                BackupResolution.TAKE_INCOMING to "Usar la de la copia").forEach { (value, label) ->
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    RadioButton((resolutions[conflict.id] ?: BackupResolution.KEEP_BOTH) == value,
                                        onClick = { resolutions = resolutions + (conflict.id to value) })
                                    Text(label)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    item {
                        Button(onClick = { if (replace) confirmReplace = true else restore(false) }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (replace) "Restaurar esta copia" else "Combinar con mis datos") }
                        TextButton(onClick = { preview = null }, enabled = !busy) { Text("Cancelar importación") }
                    }
                }
            }
            if (existingConflicts.isNotEmpty()) {
                item { HorizontalDivider(); Text("Diferencias conservadas", style = MaterialTheme.typography.titleMedium)
                    Text("Puedes copiar el contenido de versiones que no sustituyeron a tus datos actuales.", style = MaterialTheme.typography.bodySmall) }
                items(existingConflicts, key = { "kept-${it.id}" }) { conflict ->
                    Column {
                        ConflictComparison(conflict, incomingOnly = true)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(conflict.incomingValue)) }) { Text("Copiar versión conservada") }
                    }
                }
            }
        }
    }
    if (confirmReplace) AlertDialog(onDismissRequest = { confirmReplace = false }, title = { Text("Reemplazar tus datos actuales") },
        text = { Text("Esta opción sustituye los datos de este teléfono por los de la copia. Si quieres conservar los dos conjuntos, utiliza Combinar. Guarda antes una copia de los datos actuales si quieres recuperarlos después.") },
        confirmButton = { TextButton(onClick = { confirmReplace = false; restore(true) }) { Text("Reemplazar con la copia") } },
        dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancelar") } })
}

@Composable private fun Counts(counts: BackupCounts) {
    Text("${counts.favorites} páginas marcadas\n${counts.notes} Notas de referencia\n${counts.folders} carpetas\n${counts.searches} búsquedas",
        modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun ConflictComparison(conflict: BackupConflict, incomingOnly: Boolean = false) {
    val context = LocalContext.current
    val store = remember(context) { LocalStore(context) }
    val path = Regex("^([0-9]+)[|][0-9]+$").matchEntire(conflict.key)?.groupValues?.get(1)
    val display = remember(conflict) { presentBackupConflict(conflict, path?.let(store::workTitle)) }
    var expanded by rememberSaveable(conflict.id, incomingOnly) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(display.title, style = MaterialTheme.typography.titleMedium)
        display.context?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        if (!incomingOnly) {
            Text("En este teléfono", style = MaterialTheme.typography.labelLarge)
            SelectionContainer { Text(display.localSummary, maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) }
        }
        Text(if (incomingOnly) "Versión conservada de la copia" else "En la copia", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(display.incomingSummary, maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) }
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar contenido completo" else "Contenido completo y detalles") }
        if (expanded) {
            Text("Identificador original: ${conflict.key}", style = MaterialTheme.typography.bodySmall)
            if (!incomingOnly) SelectionContainer { Text("En este teléfono:\n${conflict.localValue}", style = MaterialTheme.typography.bodySmall) }
            SelectionContainer { Text("En la copia:\n${conflict.incomingValue}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
