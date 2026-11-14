package gal.galiciana.app.ui

import androidx.compose.material.icons.outlined.CreateNewFolder
import gal.galiciana.app.i18n.tr
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import gal.galiciana.app.data.LocalStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickSheet(
    store: LocalStore,
    path: String?,
    position: Int?,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit = {},
) {
    val archivador = "Archivador general"
    var draft by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var folders by remember { mutableStateOf(listOf(archivador) + store.folders().filterNot { it.equals(archivador, true) }) }
    var selected by remember {
        val have = store.foldersOf(path, position)
        mutableStateOf(if (have.isEmpty()) setOf(archivador) else have.toSet())
    }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(tr("Gardar en", "Guardar en"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                tr("Podes marcar varios á vez.", "Puedes marcar varias a la vez."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                folders.forEach { name ->
                    val on = selected.any { it.equals(name, true) }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                            value = on,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                selected = if (checked) selected + name else selected.filterNot { it.equals(name, true) }.toSet()
                            },
                        ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(checked = on, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(gal.galiciana.app.i18n.folderLabel(name), modifier = Modifier.weight(1f))
                        if (!name.equals(archivador, true)) {
                            IconButton(onClick = { pendingDelete = name }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = tr("Eliminar cartafol $name", "Eliminar carpeta $name"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(androidx.compose.material.icons.Icons.Outlined.CreateNewFolder, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("Novo cartafol", "Nueva carpeta"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focus.clearFocus()
                        keyboard?.hide()
                    }),
                    label = { Text(tr("Nome do cartafol", "Nombre de la carpeta")) },
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val n = draft.trim()
                    if (n.isNotBlank()) {
                        store.addFolder(n)
                        folders = listOf(archivador) + store.folders().filterNot { it.equals(archivador, true) }
                        selected = selected + n
                        draft = ""
                    }
                }, enabled = draft.isNotBlank()) { Text(tr("Engadir", "Añadir")) }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    store.setFolders(path, position, selected.ifEmpty { setOf(archivador) }.toList())
                    onConfirmed()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Feito", "Listo")) }
        }
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr("Eliminar cartafol", "Eliminar carpeta")) },
            text = { Text(tr("Eliminar «$target»? As páxinas consérvanse nos seus outros cartafoles; as que non teñan ningún pasarán ao arquivador xeral.", "¿Eliminar «$target»? Las páginas se conservan en sus otras carpetas; las que no tengan ninguna pasarán al archivador general.")) },
            confirmButton = {
                TextButton(onClick = {
                    store.removeFolder(target)
                    folders = listOf(archivador) + store.folders().filterNot { it.equals(archivador, true) }
                    selected = selected.filterNot { it.equals(target, true) }.ifEmpty { setOf(archivador) }.toSet()
                    pendingDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }
    }
}
