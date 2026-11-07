package gal.galiciana.app.ui.fav

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextField
import gal.galiciana.app.ui.MatteShape
import gal.galiciana.app.ui.matteFieldColors
import gal.galiciana.app.ui.theme.MatteFill
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gal.galiciana.app.data.LocalStore
import gal.galiciana.app.data.MetsRepository
import gal.galiciana.app.data.PageDownloads
import gal.galiciana.app.data.PageFormat
import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.domain.OcrWorkGroup
import gal.galiciana.app.ui.SectionBar
import gal.galiciana.app.ui.search.ResultHitRow
import gal.galiciana.app.ui.search.SearchViewModel
import gal.galiciana.app.ui.search.hitMatchesQuery
import gal.galiciana.app.ui.search.sortGroups
import gal.galiciana.app.ui.theme.GalicianaBlue
import gal.galiciana.app.ui.viewer.fetchFullGalicianaPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect

class FavoritesBrowseState(val saved: org.json.JSONObject = org.json.JSONObject()) {
    val query = mutableStateOf(saved.optString("query"))
    val notesOnly = mutableStateOf(saved.optBoolean("notesOnly", false))
    val sortBy = mutableStateOf(runCatching { SearchViewModel.SortBy.valueOf(saved.optString("sort")) }
        .getOrDefault(SearchViewModel.SortBy.Titulo))
    val sortAsc = mutableStateOf(saved.optBoolean("ascending", true))
    val openFolder = mutableStateOf(saved.optString("folder").ifBlank { null })
    val sessionSeen = mutableStateOf(setOf<String>())
}

@Composable
fun FavScreen(
    onBackup: () -> Unit = {},
    onHome: () -> Unit,
    onConsulta: () -> Unit,
    onHemeroteca: () -> Unit,
    onDatos: () -> Unit,
    onOpen: (String, Int, String) -> Unit,
    browseState: FavoritesBrowseState,
    listState: LazyListState,
    folderListState: LazyListState,
) {
    val ctx = LocalContext.current
    val store = remember(ctx) { LocalStore(ctx) }
    var hits by remember { mutableStateOf(store.favHits()) }
    var q by browseState.query
    var notesOnly by browseState.notesOnly
    var sortBy by browseState.sortBy
    var sortAsc by browseState.sortAsc
    var showFolders by rememberSaveable { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    var askBulk by remember { mutableStateOf(false) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0 to 0) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    var chooseDownloadFormat by remember { mutableStateOf(false) }
    var sessionSeen by browseState.sessionSeen
    var openFolder by browseState.openFolder
    var newFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderTick by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val folderNames = remember(hits, folderTick) { store.folders() }
    val scope = rememberCoroutineScope()
    val storageNotice = remember(hits) { store.favoritesStorageNotice() }

    fun saveBrowsePosition() {
        store.saveFavoritesBrowse(org.json.JSONObject().apply {
            put("folder", openFolder.orEmpty())
            put("query", q)
            put("notesOnly", notesOnly)
            put("sort", sortBy.name)
            put("ascending", sortAsc)
            put("index", listState.firstVisibleItemIndex)
            put("offset", listState.firstVisibleItemScrollOffset)
            put("folderIndex", folderListState.firstVisibleItemIndex)
            put("folderOffset", folderListState.firstVisibleItemScrollOffset)
        })
    }
    DisposableEffect(Unit) { onDispose { saveBrowsePosition() } }
    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(openFolder.orEmpty(), q, notesOnly.toString(), sortBy.name, sortAsc.toString(),
                listState.firstVisibleItemIndex.toString(), listState.firstVisibleItemScrollOffset.toString(),
                folderListState.firstVisibleItemIndex.toString(), folderListState.firstVisibleItemScrollOffset.toString(),
                listState.isScrollInProgress.toString(), folderListState.isScrollInProgress.toString())
        }.collect { if (!listState.isScrollInProgress && !folderListState.isScrollInProgress) saveBrowsePosition() }
    }

    fun downloadPicked(format: PageFormat, combined: Boolean = false) {
        if (downloading) return
        val selected = (if (notesOnly) store.annotatedPages() else hits).filter { "${it.path}|${it.position ?: 1}" in picked }
            .distinctBy { "${it.path}|${it.position ?: 1}" }
            .filter { !it.path.isNullOrBlank() }
        if (selected.isEmpty()) return
        downloading = true
        downloadProgress = 0 to selected.size
        downloadMessage = null
        downloadJob = scope.launch {
            var saved = 0
            var failed = 0
            var withOcr = 0
            var lastFailure: String? = null
            val pagesByPath = mutableMapOf<String, Map<Int, String?>>()
            try {
                val document = if (combined) PageDownloads.CombinedPdf(ctx) else null
                try {
                selected.forEachIndexed { index, hit ->
                    val path = hit.path ?: return@forEachIndexed
                    val position = hit.position ?: 1
                    try {
                        withContext(Dispatchers.IO) {
                            val bytes = if (format == PageFormat.PDF && !combined) PageDownloads.pdf(ctx, path, position, hit.imageId)
                            else {
                                val imageId = hit.imageId ?: pagesByPath.getOrPut(path) {
                                    MetsRepository().load(path).pages.associate { it.position to it.imageId }
                                }[position] ?: error("Imagen no identificada")
                                fetchFullGalicianaPage(path, position, imageId)
                            }
                            if (document != null) {
                                val text = PageDownloads.ocr(path, position)
                                document.add(bytes, text)
                                if (text.isNotBlank()) withOcr++
                            }
                            else PageDownloads.save(ctx, path, position, bytes, format)
                        }
                        saved++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                        lastFailure = e.message
                    }
                    downloadProgress = index + 1 to selected.size
                }
                if (document != null && saved > 0) withContext(Dispatchers.IO) { document.save(ctx) }
                } finally { document?.close() }
                downloadMessage = (if (combined && saved == 0) "No se pudo crear el PDF: ninguna página descargada"
                    else if (combined) "$saved páginas en un PDF guardado en ${gal.galiciana.app.data.downloadLocation(PageFormat.PDF, android.os.Build.VERSION.SDK_INT)}"
                    else "$saved páginas guardadas en ${gal.galiciana.app.data.downloadLocation(format, android.os.Build.VERSION.SDK_INT)}") +
                    (if (combined && withOcr < saved) " · OCR disponible en $withOcr de $saved páginas" else "") +
                    (if (failed > 0) " · $failed sin descargar${lastFailure?.let { ": $it" }.orEmpty()}" else "")
            } catch (_: CancellationException) {
                downloadMessage = if (combined) "Descarga detenida: no se guardó el PDF único"
                    else "Descarga detenida: $saved guardadas, $failed fallidas"
            } catch (e: Exception) {
                downloadMessage = "No se pudo guardar el PDF único: ${e.message.orEmpty()}"
            } finally {
                downloading = false
                downloadJob = null
            }
        }
    }

    var dossierEntries by remember { mutableStateOf(emptyList<gal.galiciana.app.data.DossierEntry>()) }
    val dossierExport = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) scope.launch {
            try {
                val entries = dossierEntries
                withContext(Dispatchers.IO) {
                    val text = gal.galiciana.app.data.ResearchDossier.markdown(entries)
                    ctx.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                        ?: error("No se pudo escribir el dossier.")
                }
                downloadMessage = "Dossier guardado con ${entries.size} referencias."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { downloadMessage = "No se pudo exportar el dossier: ${e.message.orEmpty()}" }
        }
    }

    val filteredAll = remember(hits, q, notesOnly, folderTick) {
        (if (notesOnly) store.annotatedPages() else hits).filter { hit ->
            (!notesOnly || hit.note.isNotBlank()) &&
                (q.isBlank() || hitMatchesQuery(hit.copy(snippets = hit.snippets + hit.note), q))
        }
    }
    val filtered = remember(filteredAll, openFolder, hits, folderTick, notesOnly) {
        val tag = openFolder
        if (tag.isNullOrBlank() && notesOnly) filteredAll
        else if (tag.isNullOrBlank()) {
            filteredAll.filter {
                val fs = store.foldersOf(it.path, it.position)
                fs.isEmpty() || fs.any { f -> f.equals("Archivador general", true) }
            }
        } else filteredAll.filter { store.foldersOf(it.path, it.position).any { f -> f.equals(tag, true) } }
    }
    val groups = remember(filtered, sortBy, sortAsc, q) {
        val raw = runCatching { OcrRepository().groupByWork(filtered) }.getOrDefault(emptyList())
        sortGroups(raw, sortBy, sortAsc, q)
    }
    val visibleKeys = remember(filtered) { filtered.map { "${it.path}|${it.position ?: 1}" }.toSet() }
    LaunchedEffect(visibleKeys) {
        // Hidden pages must not remain selected for a later bulk deletion.
        picked = picked.intersect(visibleKeys)
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SectionBar("Guardados", GalicianaBlue, onBack = onHome, onHome = onHome)
            if (chooseDownloadFormat) {
                AlertDialog(
                    onDismissRequest = { chooseDownloadFormat = false },
                    title = { Text("Descargar ${picked.size} páginas") },
                    text = { Text("Elige JPEG por página, PDF por página o un PDF único con las páginas seleccionadas. El PDF incluye el OCR disponible para buscar o copiar, sin selección alineada con la imagen.") },
                    confirmButton = {
                        Column {
                            TextButton(onClick = {
                                chooseDownloadFormat = false
                                downloadPicked(PageFormat.JPEG)
                            }) { Text("JPEG") }
                            TextButton(onClick = {
                                chooseDownloadFormat = false
                                downloadPicked(PageFormat.PDF)
                            }) { Text("PDF separados") }
                            TextButton(onClick = {
                                chooseDownloadFormat = false
                                downloadPicked(PageFormat.PDF, combined = true)
                            }) { Text("PDF único") }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { chooseDownloadFormat = false }) { Text("Cancelar") }
                    },
                )
            }
            LazyColumn(Modifier.weight(1f), state = listState) {
            item(key = "archive-controls") {
            storageNotice?.let { notice ->
                Text(notice, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    TextButton(onClick = onBackup) { Text("Copia de tus datos") }
                    TextButton(onClick = {
                        val selected = if (picked.isEmpty()) filtered else filtered.filter { "${it.path}|${it.position ?: 1}" in picked }
                        dossierEntries = selected.filter { !it.path.isNullOrBlank() }.map { hit ->
                            gal.galiciana.app.data.DossierEntry(hit.title, hit.path!!, hit.position ?: 1,
                                store.noteOf(hit.path, hit.position), hit.savedQuery, store.foldersOf(hit.path, hit.position))
                        }
                        dossierExport.launch("galiciana-de-peto-referencias.md")
                    }, enabled = filtered.isNotEmpty()) { Text("Exportar referencias") }
                }
                gal.galiciana.app.ui.ClearableField(
                    value = q,
                    onValueChange = {
                        if (q != it) {
                            q = it
                            scope.launch { listState.scrollToItem(0) }
                        }
                    },
                    placeholder = "Buscar páginas y notas",
                    imeSearch = true,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !notesOnly, onClick = { notesOnly = false }, label = { Text("Páginas") })
                    FilterChip(selected = notesOnly, onClick = { notesOnly = true; openFolder = null },
                        label = { Text("Notas de referencia") })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showFolders = !showFolders }, modifier = Modifier.weight(1f)) {
                        Text("Carpetas: ${openFolder ?: if (notesOnly) "Todas las notas" else "Archivador general"}",
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (showFolders) TextButton(onClick = { newFolder = true }) { Text("Nueva") }
                }
                if (showFolders) LazyRow(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), state = folderListState) {
                    item {
                        FolderChip(
                            name = if (notesOnly) "Todas las notas" else "Archivador general",
                            selected = openFolder == null,
                            onClick = {
                                if (openFolder != null) {
                                    openFolder = null
                                    scope.launch { listState.scrollToItem(0) }
                                }
                            },
                        )
                    }
                    items(folderNames.filterNot { it.equals("Archivador general", true) }) { name ->
                        FolderChip(
                            name = name,
                            selected = openFolder == name,
                            onClick = {
                                if (openFolder != name) {
                                    openFolder = name
                                    scope.launch { listState.scrollToItem(0) }
                                }
                            },
                            onLongClick = { pendingDelete = name },
                        )
                    }
                    item {
                        FolderChip(
                            name = "Nueva",
                            selected = false,
                            onClick = { newFolder = true },
                            plus = true,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${filtered.size} ${if (filtered.size == 1) "página" else "páginas"}",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { sortMenuOpen = true }) {
                            Text(when (sortBy) {
                                SearchViewModel.SortBy.Titulo -> "Título"
                                SearchViewModel.SortBy.Autor -> "Autor"
                                SearchViewModel.SortBy.Fecha -> "Fecha"
                                SearchViewModel.SortBy.Relevancia -> "Relevancia"
                            } + if (sortAsc) " ↑" else " ↓")
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            listOf(SearchViewModel.SortBy.Titulo to "Título", SearchViewModel.SortBy.Autor to "Autor",
                                SearchViewModel.SortBy.Fecha to "Fecha", SearchViewModel.SortBy.Relevancia to "Relevancia").forEach { (key, label) ->
                                DropdownMenuItem(text = { Text(label + if (sortBy == key)
                                    (if (sortAsc) " · ascendente" else " · descendente") else "") }, onClick = {
                                    if (sortBy == key) sortAsc = !sortAsc
                                    else { sortBy = key; sortAsc = key != SearchViewModel.SortBy.Fecha && key != SearchViewModel.SortBy.Relevancia }
                                    sortMenuOpen = false
                                    scope.launch { listState.scrollToItem(0) }
                                })
                            }
                        }
                    }
                    TextButton(enabled = !downloading && (selecting || filtered.isNotEmpty()), onClick = {
                        selecting = !selecting
                        if (!selecting) picked = emptySet()
                    }) { Text(if (selecting) "Cancelar" else "Seleccionar") }
                }
                if (filtered.isEmpty() && storageNotice == null) {
                    Text(
                        when {
                            notesOnly -> "Todavía no hay notas de referencia que coincidan. Puedes añadir una desde cualquier página del visor."
                            hits.isEmpty() -> "Todavía no has guardado páginas. Pulsa la vieira de un resultado para añadirlo al archivo."
                            q.isNotBlank() -> "No hay coincidencias en esta carpeta. Borra el filtro o consulta otra carpeta."
                            else -> "Esta carpeta no contiene páginas. Usa el icono de carpeta de un favorito para organizarlo aquí."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            if (selecting) {
                TextButton(enabled = !downloading && filtered.isNotEmpty(), onClick = {
                    picked = filtered.map { "${it.path}|${it.position ?: 1}" }.toSet()
                }, modifier = Modifier.padding(horizontal = 20.dp)) { Text("Seleccionar todos (${filtered.size})") }
            }
            if (selecting) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Button(onClick = { chooseDownloadFormat = true },
                        enabled = picked.isNotEmpty() && !downloading,
                        modifier = Modifier.weight(1f)) { Text("Descargar (${picked.size})") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { askBulk = true },
                        enabled = picked.isNotEmpty() && !downloading && !notesOnly) { Text("Borrar") }
                }
            }
            if (downloading) {
                val (done, total) = downloadProgress
                LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Descargando $done/$total páginas", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { downloadJob?.cancel() }) { Text("Cancelar") }
                }
            }
            downloadMessage?.let { message ->
                Text(message, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
            }
                groups.forEach { group ->
                    item(key = "h-${group.title}-${group.pages.firstOrNull()?.path}") {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                        gal.galiciana.app.ui.search.WorkHead(
                            title = group.title,
                            parent = group.parentTitle,
                            pages = group.pages.size,
                            hits = group.occurrenceCount,
                            visited = sessionSeen.contains("${group.pages.firstOrNull()?.path}|${group.pages.firstOrNull()?.position ?: 1}"),
                            onOpen = {
                                val first = group.pages.minByOrNull { it.position ?: 999 }
                                first?.path?.let { p ->
                                    saveBrowsePosition()
                                    store.rememberOpenedHit(first)
                                    store.rememberWork(p, group.title, group.parentTitle)
                                    onOpen(p, first.position ?: 1, q.ifBlank { first.savedQuery })
                                }
                            },
                        )
                        }
                    }
                    items(group.pages, key = { "${it.path}-${it.imageId}-${it.position}" }) { hit ->
                        Box(Modifier.padding(horizontal = 20.dp)) {
                        ResultHitRow(
                            hit = hit,
                            query = q.ifBlank { hit.savedQuery },
                            visited = sessionSeen.contains("${hit.path}|${hit.position ?: 1}"),
                            onClick = {
                                val key = "${hit.path}|${hit.position ?: 1}"
                                if (selecting) {
                                    picked = if (key in picked) picked - key else picked + key
                                } else {
                                    sessionSeen = sessionSeen + key
                                    hit.path?.let { p ->
                                        saveBrowsePosition()
                                        store.rememberOpenedHit(hit)
                                        store.rememberWork(p, hit.title, hit.parentTitle)
                                        onOpen(p, hit.position ?: 1, q.ifBlank { hit.savedQuery })
                                    }
                                }
                            },
                            onFavChanged = {
                                hits = store.favHits()
                                folderTick++
                            },
                            confirmUnfav = true,
                            selecting = selecting,
                            selected = "${hit.path}|${hit.position ?: 1}" in picked,
                            onSelectToggle = {
                                val key = "${hit.path}|${hit.position ?: 1}"
                                picked = if (key in picked) picked - key else picked + key
                            },
                            showNoteText = true,
                            recoverMissingOcr = false,
                            showSavedSearch = true,
                        )
                        }
                    }
                }
            }
            if (newFolder) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { newFolder = false },
                    title = { Text("Nueva carpeta") },
                    text = {
                        TextField(value = newFolderName, onValueChange = { newFolderName = it }, singleLine = true, placeholder = { Text("Nombre") })
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            store.addFolder(newFolderName)
                            openFolder = store.folders().firstOrNull { it.equals(newFolderName.trim(), true) }
                            newFolderName = ""
                            newFolder = false
                            folderTick++
                            hits = store.favHits()
                            scope.launch { listState.scrollToItem(0) }
                        }, enabled = newFolderName.isNotBlank()) { Text("Crear") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { newFolder = false }) { Text("Cancelar") }
                    },
                )
            }
            pendingDelete?.let { target ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text("Borrar carpeta") },
                    text = { Text("¿Borrar «$target»? Las páginas se conservan en sus otras carpetas; las que no tengan ninguna pasarán al archivador general.") },
                    confirmButton = {
                        TextButton(onClick = {
                            store.removeFolder(target)
                            if (openFolder == target) openFolder = null
                            folderTick++
                            hits = store.favHits()
                            pendingDelete = null
                        }) { Text("Borrar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
                    },
                )
            }
            if (askBulk) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { askBulk = false },
                    title = { Text("Borrar seleccionados") },
                    text = { Text("Vas a quitar ${picked.size} páginas de Favoritos. No se pueden deshacer.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            store.removeHits(picked)
                            hits = store.favHits()
                            picked = emptySet()
                            selecting = false
                            askBulk = false
                        }) { Text("Borrar") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { askBulk = false }) { Text("Cancelar") }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    showIcon: Boolean = true,
    plus: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val isSelected = selected
    Row(
        Modifier
            .padding(end = 8.dp)
            .widthIn(max = 240.dp)
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = name
                this.selected = isSelected
            }
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) GalicianaBlue else Color(0xFFB8C0C6),
                shape = RoundedCornerShape(6.dp),
            )
            .then(
                if (onLongClick != null) Modifier.combinedClickable(role = Role.Button,
                    onClick = onClick, onLongClickLabel = "Borrar carpeta", onLongClick = onLongClick)
                else Modifier.clickable(role = Role.Button, onClick = onClick)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (plus) {
            Text("+", color = GalicianaBlue, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(4.dp))
        }
        if (showIcon) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = Color(0xFFE6C27A),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

