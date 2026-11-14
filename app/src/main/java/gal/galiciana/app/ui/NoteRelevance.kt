package gal.galiciana.app.ui

import gal.galiciana.app.i18n.tr
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gal.galiciana.app.data.LocalStore
import gal.galiciana.app.domain.OcrHit

private const val NOTE_LIMIT = 4000

@Composable
fun NoteMark(filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    IconButton(onClick = onClick, modifier = modifier.size(maxOf(48.dp, size))) {
        Icon(Icons.AutoMirrored.Outlined.Notes,
            contentDescription = if (filled) "Editar nota de referencia" else tr("Engadir nota de referencia", "Añadir nota de referencia"),
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
    onEditorClosed: () -> Unit = {},
) {
    var open by rememberSaveable(hit.path, hit.position) { mutableStateOf(false) }
    var draft by rememberSaveable(hit.path, hit.position) { mutableStateOf("") }
    var original by rememberSaveable(hit.path, hit.position) { mutableStateOf("") }
    var confirmDrop by rememberSaveable(hit.path, hit.position) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(hit.path, hit.position) { mutableStateOf(false) }
    var savedNote by remember(hit.path, hit.position, isFav) { mutableStateOf(store.noteOf(hit.path, hit.position)) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var truncated by remember { mutableStateOf(false) }
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
        truncated = false
        open = true
    }
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) wasOpen = true else if (wasOpen) { wasOpen = false; onEditorClosed() }
    }
    fun closeEditor() {
        if (draft != original) confirmDrop = true else open = false
    }
    if (label == null) NoteMark(savedNote.isNotBlank(), ::launchEditor, size = size)
    else Surface(onClick = ::launchEditor, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        // Same geometry as the other rows of the page sheet: 48dp icon box, text 8dp after it.
        Row(Modifier.heightIn(min = 64.dp).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(if (savedNote.isBlank()) tr("Engade contexto a esta páxina", "Añade contexto a esta página") else tr("Consultar e editar a túa anotación", "Consultar y editar tu anotación"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val hostView = androidx.compose.ui.platform.LocalView.current
    if (open) Dialog(onDismissRequest = ::closeEditor,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // In landscape the dialog window started after the camera cutout yet kept the
        // full screen width, pushing Guardar off the right edge. Let the window cover the
        // whole display; the Scaffold's safeDrawing insets keep content clear of it.
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent
            as? androidx.compose.ui.window.DialogWindowProvider)?.window
        val editorBackground = MaterialTheme.colorScheme.surface.toArgb()
        SideEffect {
            dialogWindow?.let { w ->
                // Paint the whole window: otherwise the sheet behind shows through the status bar strip.
                w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(editorBackground))
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
                @Suppress("DEPRECATION")
                run { w.statusBarColor = android.graphics.Color.TRANSPARENT; w.navigationBarColor = android.graphics.Color.TRANSPARENT }
                w.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                if (android.os.Build.VERSION.SDK_INT >= 28) w.attributes = w.attributes.apply {
                    layoutInDisplayCutoutMode = if (android.os.Build.VERSION.SDK_INT >= 30)
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    else android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        // The dialog window does not always receive the side navigation bar and cutout
        // insets in landscape; borrow the app window's and keep the larger of each side.
        val host = androidx.core.view.ViewCompat.getRootWindowInsets(hostView)?.getInsets(
            androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                androidx.core.view.WindowInsetsCompat.Type.displayCutout())
        val frame = WindowInsets.safeDrawing.union(WindowInsets(left = host?.left ?: 0, right = host?.right ?: 0, bottom = host?.bottom ?: 0))
        MaterialTheme {
            Scaffold(containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = frame,
                topBar = {
                    TopAppBar(windowInsets = frame.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                        title = { CappedFontScale { Text("Nota de referencia", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } },
                        navigationIcon = { IconButton(onClick = ::closeEditor) { Icon(Icons.Outlined.Close, tr("Pechar nota de referencia", "Cerrar nota de referencia")) } },
                        actions = {
                            TextButton(enabled = draft != original || (original.isBlank() && draft.isNotBlank()), onClick = {
                                if (!store.isFav(hit.path, hit.position) && draft.isNotBlank() && !store.toggleHit(hit, useOpenedResult = true)) {
                                    error = tr("Non se puido gardar esta páxina. A nota segue aquí; téntao de novo.", "No se pudo guardar esta página. La nota sigue aquí; vuelve a intentarlo.")
                                } else if (store.setNote(hit.path, hit.position, draft)) {
                                    savedNote = draft
                                    original = draft
                                    open = false
                                    onChanged()
                                } else error = tr("Non se puido gardar a nota. Conservamos o texto para que poidas tentalo de novo.", "No se pudo guardar la nota. Conservamos el texto para que puedas reintentarlo.")
                            }) { Text(tr("Gardar", "Guardar"), fontWeight = FontWeight.SemiBold) }
                        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
                }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text(tr("Páxina ${hit.position ?: 1}", "Página ${hit.position ?: 1}"), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            if (hit.savedQuery.isNotBlank()) Text(tr("Busca: ${hit.savedQuery}", "Búsqueda: ${hit.savedQuery}"),
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
                        tr("Ao gardar a nota, esta páxina engadirase a Gardados.", "Al guardar la nota, esta página se añadirá a Guardados."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = draft, onValueChange = {
                            // A long paste is kept up to the limit instead of being ignored.
                            truncated = it.length > NOTE_LIMIT
                            draft = it.take(NOTE_LIMIT)
                        },
                        label = { Text(tr("A túa nota de referencia", "Tu nota de referencia")) },
                        placeholder = { Text(tr("Describe o achado, relaciona nomes ou deixa unha pista para continuar a investigación…", "Describe el hallazgo, relaciona nombres o deja una pista para continuar la investigación…")) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                        minLines = 8,
                        isError = truncated,
                        supportingText = { Text(
                            if (truncated) tr("Recortouse o texto: o máximo son $NOTE_LIMIT caracteres.", "Se ha recortado el texto: el máximo son $NOTE_LIMIT caracteres.")
                            else "${draft.length} / $NOTE_LIMIT", Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End) },
                        shape = MaterialTheme.shapes.medium)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.liveStatus()) }
                    Text(tr("Gárdase neste dispositivo, vinculada á páxina orixinal.", "Se guarda en este dispositivo, vinculada a la página original."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (original.isNotBlank()) TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar nota de referencia", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (confirmDrop) AlertDialog(onDismissRequest = { confirmDrop = false },
        title = { Text(tr("Descartar os cambios?", "¿Descartar los cambios?")) },
        text = { Text(if (original.isBlank()) tr("Esta nota aínda non se gardou. Perderase o que escribiches.", "Esta nota todavía no se ha guardado. Se perderá lo que has escrito.")
            else tr("A nota de referencia gardada conservarase. Os cambios sen gardar perderanse.", "La nota de referencia guardada se conservará. Los cambios sin guardar se perderán.")) },
        confirmButton = { TextButton(onClick = { draft = original; confirmDrop = false; open = false }) { Text("Descartar cambios") } },
        dismissButton = { TextButton(onClick = { confirmDrop = false }) { Text("Seguir editando") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text(tr("Eliminar a nota de referencia?", "¿Eliminar la nota de referencia?")) }, text = { Text(tr("A páxina seguirá en Gardados.", "La página seguirá en Guardados.")) },
        confirmButton = { TextButton(onClick = {
            if (store.setNote(hit.path, hit.position, "")) {
                draft = ""; original = ""; savedNote = ""; confirmDelete = false; open = false; onChanged()
            } else { confirmDelete = false; error = tr("Non se puido eliminar a nota. Téntao de novo.", "No se pudo eliminar la nota. Vuelve a intentarlo.") }
        }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Conservar nota") } })
}
