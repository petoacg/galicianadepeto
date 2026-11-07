package gal.galiciana.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gal.galiciana.app.data.LocalStore
import gal.galiciana.app.domain.OcrHit

val NoteOchre = Color(0xFF8A651F)
val PaperNote = Color(0xFFF8F5EF)
private const val NOTE_LIMIT = 4000

@Composable
fun NoteMark(filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    IconButton(onClick = onClick, modifier = modifier.size(maxOf(48.dp, size))) {
        Icon(Icons.AutoMirrored.Outlined.Notes,
            contentDescription = if (filled) "Editar nota de referencia" else "Añadir nota de referencia",
            tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFlow(
    store: LocalStore,
    hit: OcrHit,
    isFav: Boolean,
    size: Dp = 22.dp,
    onChanged: () -> Unit = {},
    label: String? = null,
) {
    var open by rememberSaveable(hit.path, hit.position) { mutableStateOf(false) }
    var draft by rememberSaveable(hit.path, hit.position) { mutableStateOf("") }
    var original by rememberSaveable(hit.path, hit.position) { mutableStateOf("") }
    var confirmDrop by rememberSaveable(hit.path, hit.position) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(hit.path, hit.position) { mutableStateOf(false) }
    var savedNote by remember(hit.path, hit.position, isFav) { mutableStateOf(store.noteOf(hit.path, hit.position)) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val title = hit.title.ifBlank { store.workTitle(hit.path.orEmpty()) ?: "Documento de Galiciana" }
    val reference = remember(title, hit.path, hit.position) {
        "$title. Página ${hit.position ?: 1}.\nhttps://biblioteca.galiciana.gal/gl/catalogo_imagenes/grupo.do?path=${hit.path.orEmpty()}&posicion=${hit.position ?: 1}&presentacion=pagina"
    }
    fun launchEditor() {
        original = store.noteOf(hit.path, hit.position)
        draft = original
        error = null
        copied = false
        open = true
    }
    fun closeEditor() {
        if (draft != original) confirmDrop = true else open = false
    }
    if (label == null) NoteMark(savedNote.isNotBlank(), ::launchEditor, size = size)
    else Surface(onClick = ::launchEditor, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.AutoMirrored.Outlined.Notes, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(if (savedNote.isBlank()) "Añade contexto a esta página" else "Consultar y editar tu anotación",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (open) Dialog(onDismissRequest = ::closeEditor,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MaterialTheme {
            Scaffold(containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(title = { Text("Nota de referencia", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = { IconButton(onClick = ::closeEditor) { Icon(Icons.Outlined.Close, "Cerrar nota de referencia") } },
                        actions = {
                            TextButton(enabled = draft != original || (original.isBlank() && draft.isNotBlank()), onClick = {
                                if (!store.isFav(hit.path, hit.position) && draft.isNotBlank() && !store.toggleHit(hit, useOpenedResult = true)) {
                                    error = "No se pudo guardar esta página. La nota sigue aquí; vuelve a intentarlo."
                                } else if (store.setNote(hit.path, hit.position, draft)) {
                                    savedNote = draft
                                    original = draft
                                    open = false
                                    onChanged()
                                } else error = "No se pudo guardar la nota. Conservamos el texto para que puedas reintentarlo."
                            }) { Text("Guardar", fontWeight = FontWeight.SemiBold) }
                        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
                }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text("Página ${hit.position ?: 1}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            if (hit.savedQuery.isNotBlank()) Text("Búsqueda: ${hit.savedQuery}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { clipboard.setText(AnnotatedString(reference)); copied = true },
                                contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (copied) "Referencia copiada" else "Copiar referencia")
                            }
                        }
                    }
                    if (!store.isFav(hit.path, hit.position)) Text(
                        "Al guardar la nota, esta página se añadirá a Guardados.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = draft, onValueChange = { if (it.length <= NOTE_LIMIT) draft = it },
                        label = { Text("Tu nota de referencia") },
                        placeholder = { Text("Describe el hallazgo, relaciona nombres o deja una pista para continuar la investigación…") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp)
                            .semantics { contentDescription = "Texto de la nota de referencia" },
                        minLines = 8,
                        supportingText = { Text("${draft.length} / $NOTE_LIMIT", Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End) },
                        shape = MaterialTheme.shapes.medium)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Text("Se guarda en este dispositivo, vinculada a la página original.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (original.isNotBlank()) TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar nota de referencia", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (confirmDrop) AlertDialog(onDismissRequest = { confirmDrop = false },
        title = { Text("¿Descartar los cambios?") },
        text = { Text("La nota de referencia guardada se conservará. Los cambios sin guardar se perderán.") },
        confirmButton = { TextButton(onClick = { draft = original; confirmDrop = false; open = false }) { Text("Descartar cambios") } },
        dismissButton = { TextButton(onClick = { confirmDrop = false }) { Text("Seguir editando") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("¿Eliminar la nota de referencia?") }, text = { Text("La página seguirá en Guardados.") },
        confirmButton = { TextButton(onClick = {
            if (store.setNote(hit.path, hit.position, "")) {
                draft = ""; original = ""; savedNote = ""; confirmDelete = false; open = false; onChanged()
            } else { confirmDelete = false; error = "No se pudo eliminar la nota. Vuelve a intentarlo." }
        }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Conservar nota") } })
}
