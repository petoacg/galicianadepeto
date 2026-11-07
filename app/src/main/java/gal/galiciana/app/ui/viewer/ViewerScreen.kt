package gal.galiciana.app.ui.viewer

import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import gal.galiciana.app.data.OcrRepository.AltoStatus
import gal.galiciana.app.data.OcrRepository.AltoMatch
import gal.galiciana.app.data.OcrRepository.AltoSearchResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import gal.galiciana.app.ui.textContainsQuery
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Folder
import gal.galiciana.app.ui.NoteFlow
import gal.galiciana.app.ui.VieiraButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.roundToInt
import kotlin.math.min
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import gal.galiciana.app.data.GalicianaUrls
import gal.galiciana.app.data.Http
import gal.galiciana.app.data.MetsRepository
import gal.galiciana.app.data.PageDownloads
import gal.galiciana.app.data.PageFormat
import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.domain.MetsDocument
import gal.galiciana.app.ui.countMatches
import gal.galiciana.app.ui.highlightTerms
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewerScreen(
    path: String,
    position: Int,
    initialQuery: String = "",
    seedImageId: String? = null,
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onGo: (Int) -> Unit,
) {
    val metsRepo = remember { MetsRepository() }
    val ocrRepo = remember { OcrRepository() }
    var doc by remember { mutableStateOf<MetsDocument?>(null) }
    var currentPos by rememberSaveable(path) { mutableIntStateOf(position.coerceAtLeast(1)) }
    var ocr by remember { mutableStateOf("Cargando OCR…") }
    var ocrPosition by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // OCR se carga debajo para marcar y contar; ya no hay pestaña de texto.
    var findQuery by rememberSaveable(path) { mutableStateOf(highlightTerms(initialQuery).let { if (it == "-") "" else it }) }
    var committedQuery by rememberSaveable(path) { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    var ocrFailed by remember { mutableStateOf(false) }
    var altoStatus by remember { mutableStateOf<AltoStatus?>(null) }
    var locating by remember { mutableStateOf(false) }
    var showHighlights by rememberSaveable(path) { mutableStateOf(true) }
    var showChrome by rememberSaveable(path) { mutableStateOf(true) }
    var searchExpanded by rememberSaveable(path) { mutableStateOf(false) }
    var showPageActions by rememberSaveable(path) { mutableStateOf(false) }
    var showBibliography by rememberSaveable(path) { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var searchRequest by remember { mutableIntStateOf(0) }
    var hitAt by remember { mutableIntStateOf(0) }
    var altoPage by remember { mutableStateOf(1f to 1f) }
    var altoMatches by remember { mutableStateOf<List<AltoMatch>>(emptyList()) }
    var ocrMatchCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var metadataRetry by remember(path) { mutableIntStateOf(0) }
    LaunchedEffect(path, metadataRetry) {
        loading = true
        error = null
        runCatching { withContext(Dispatchers.IO) { metsRepo.load(path) } }
            .onSuccess { loaded ->
                doc = loaded
                val wanted = seedImageId
                if (!wanted.isNullOrBlank() && currentPos == position.coerceAtLeast(1)) {
                    loaded.pages.firstOrNull { it.imageId == wanted }?.let { currentPos = it.position }
                }
            }
            .onFailure { error = "No se pudo cargar la información de esta publicación. Vuelve atrás e inténtalo de nuevo." }
        loading = false
    }

    val pages = doc?.pages.orEmpty()
    val page = pages.firstOrNull { it.position == currentPos }
        ?: pages.getOrNull(currentPos - 1)
    val imageId = page?.imageId ?: seedImageId
    val imageUrl = imageId?.let { GalicianaUrls.jpeg(it) }
    val pageCount = pages.size
    val ocrCache = remember(path) { mutableMapOf<String, String>() }
    val altoCache = remember(path) {
        mutableMapOf<String, AltoSearchResult>()
    }

    fun startScan(qRaw: String) {
        val q = highlightTerms(qRaw)
        if (q.isBlank()) return
        keyboard?.hide()
        searchExpanded = false
        showChrome = true
        committedQuery = q
        showHighlights = true
        finding = true
        searchRequest++
    }

    LaunchedEffect(path, initialQuery) {
        if (committedQuery.isBlank()) {
            val incoming = highlightTerms(initialQuery)
            if (incoming.isNotBlank() && incoming != "-") {
                findQuery = incoming
                startScan(incoming)
            }
        }
    }

    LaunchedEffect(path, currentPos, imageId, committedQuery, searchRequest) {
        val q = highlightTerms(committedQuery)
        altoMatches = emptyList()
        altoStatus = null
        hitAt = 0
        ocrMatchCount = 0
        ocrFailed = false
        ocrPosition = 0
        if (q.isBlank()) {
            ocr = ""
            finding = false
            locating = false
            return@LaunchedEffect
        }
        finding = true
        locating = true
        val cacheKey = "$currentPos|${imageId.orEmpty()}"
        val altoKey = "$cacheKey|$q"
        ocr = ocrCache[cacheKey].orEmpty()
        val positionsJob = async {
            altoCache[altoKey] ?: ocrRepo.loadAltoSearch(path, currentPos, q, imageId)
        }
        val textJob = async {
            try {
                val text = ocrCache[cacheKey] ?: ocrRepo.loadPage(path, currentPos, imageId).ocrText
                text to countMatches(text, q)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null to 0
            }
        }
        val positions = positionsJob.await()
        // A temporary download failure is retryable, never a cached empty match list.
        if (positions.status == AltoStatus.AVAILABLE || positions.status == AltoStatus.EMPTY)
            altoCache[altoKey] = positions
        altoStatus = positions.status
        altoPage = positions.pageSize
        altoMatches = positions.matches
        locating = false
        val (text, count) = textJob.await()
        ocrFailed = text == null
        ocr = text.orEmpty()
        if (text != null) ocrCache[cacheKey] = text
        ocrPosition = currentPos
        ocrMatchCount = count
        finding = false
    }

    val ctxBar = androidx.compose.ui.platform.LocalContext.current
    val storeBar = remember { gal.galiciana.app.data.LocalStore(ctxBar) }
    var fav by remember(path, currentPos) { mutableStateOf(storeBar.isFav(path, currentPos)) }
    var showOcr by rememberSaveable(path) { mutableStateOf(false) }
    var readerText by remember(path, currentPos, imageId) { mutableStateOf("") }
    var readerLoading by remember(path, currentPos, imageId) { mutableStateOf(false) }
    var readerError by remember(path, currentPos, imageId) { mutableStateOf<String?>(null) }
    var readerRetry by remember(path, currentPos, imageId) { mutableIntStateOf(0) }
    LaunchedEffect(path, currentPos, imageId, showOcr, readerRetry) {
        if (!showOcr) return@LaunchedEffect
        readerLoading = true
        readerError = null
        val cacheKey = "$currentPos|${imageId.orEmpty()}"
        try {
            val text = ocrCache[cacheKey] ?: withContext(Dispatchers.IO) {
                ocrRepo.loadPage(path, currentPos, imageId).ocrText
            }
            readerText = text
            ocrCache[cacheKey] = text
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            readerError = "No se pudo cargar el texto de esta página. Comprueba la conexión e inténtalo de nuevo."
        } finally {
            readerLoading = false
        }
    }
    LaunchedEffect(path, currentPos, imageId, doc?.title, committedQuery) {
        if (doc != null && page != null) storeBar.rememberReading(
            gal.galiciana.app.domain.OcrHit(
                title = storeBar.workTitle(path) ?: doc?.title.orEmpty(),
                path = path, position = currentPos, imageId = imageId,
                savedQuery = committedQuery,
            )
        )
    }
    var pickFolder by remember { mutableStateOf(false) }
    var chooseDownloadFormat by remember { mutableStateOf(false) }
    var inFolders by remember { mutableStateOf(storeBar.foldersOf(path, currentPos).isNotEmpty()) }
    LaunchedEffect(path, currentPos, fav, pickFolder) {
        inFolders = storeBar.foldersOf(path, currentPos).isNotEmpty()
    }

    fun doDownload(format: PageFormat) {
        val id = imageId
        if (format == PageFormat.JPEG && id == null) return
        android.widget.Toast.makeText(ctxBar, "Guardando página…", android.widget.Toast.LENGTH_SHORT).show()
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val bytes = if (format == PageFormat.PDF) PageDownloads.pdf(ctxBar, path, currentPos, id)
                    else fetchFullGalicianaPage(path, currentPos, requireNotNull(id))
                PageDownloads.save(ctxBar, path, currentPos, bytes, format)
            }
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    ctxBar,
                    if (result.isSuccess) "Guardada en ${gal.galiciana.app.data.downloadLocation(format, android.os.Build.VERSION.SDK_INT)}"
                    else result.exceptionOrNull()?.message ?: "No se pudo descargar la página",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    if (chooseDownloadFormat) {
        AlertDialog(
            onDismissRequest = { chooseDownloadFormat = false },
            title = { Text("Descargar página $currentPos") },
            text = { Text("Elige JPEG o PDF para esta página. El PDF incluye el texto OCR disponible para buscar o copiar; su selección no sigue la posición de las palabras en la imagen.") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        chooseDownloadFormat = false
                        doDownload(PageFormat.JPEG)
                    }, enabled = imageId != null) { Text("JPEG") }
                    TextButton(onClick = {
                        chooseDownloadFormat = false
                        doDownload(PageFormat.PDF)
                    }) { Text("PDF") }
                }
            },
            dismissButton = {
                TextButton(onClick = { chooseDownloadFormat = false }) { Text("Cancelar") }
            },
        )
    }

    fun goNextHit() {
        showChrome = true
        val q = findQuery.trim()
        if (q.isNotBlank() && highlightTerms(committedQuery) != highlightTerms(q)) {
            startScan(q)
            return
        }
        when {
            altoMatches.isNotEmpty() && hitAt < altoMatches.lastIndex -> hitAt += 1
            altoMatches.isNotEmpty() -> hitAt = (hitAt + 1) % altoMatches.size
        }
    }

    fun goPrevHit() {
        showChrome = true
        when {
            altoMatches.size > 1 && hitAt > 0 -> hitAt -= 1
            altoMatches.isNotEmpty() -> hitAt = (hitAt - 1 + altoMatches.size) % altoMatches.size
        }
    }

    val hitLabel = when {
        altoMatches.isNotEmpty() -> "${hitAt + 1} de ${altoMatches.size}"
        locating -> "Localizando…"
        finding -> "Consultando texto…"
        ocrMatchCount > 0 -> "$ocrMatchCount en el texto"
        else -> "Sin marcas"
    }
    val searchStatus = when {
        committedQuery.isBlank() -> null
        altoMatches.isNotEmpty() -> if (!finding && ocrFailed)
            "Las marcas están disponibles; no se pudo cargar el texto para leerlo." else null
        locating || finding -> null
        altoStatus == AltoStatus.UNAVAILABLE ->
            "No se pudieron consultar las posiciones de las palabras. Puedes reintentar o leer el texto."
        altoStatus == AltoStatus.INVALID ->
            "Las posiciones de esta página no permiten un marcado fiable. Puedes consultar el texto."
        ocrFailed -> "No se pudo consultar el texto de esta página. Puedes reintentar."
        ocrMatchCount > 0 ->
            "Hay coincidencias en el texto, pero no se han localizado sobre la imagen. El texto y sus posiciones pueden diferir."
        altoStatus == AltoStatus.EMPTY -> "Esta página no tiene palabras con posiciones disponibles."
        else -> "No se encontró esa expresión en esta página. Prueba otra palabra o revisa el texto."
    }

    BackHandler(enabled = searchExpanded && showChrome) {
        keyboard?.hide()
        searchExpanded = false
    }
    val publicationTitle = storeBar.workTitle(path) ?: doc?.title.orEmpty()
    fun toggleFavorite() {
        val lines = if (ocrPosition != currentPos || ocr.startsWith("Cargando") || ocr.startsWith("No se")) emptyList()
        else {
            val all = ocr.lineSequence().map { it.trim() }.filter { it.length > 18 }.toList()
            if (committedQuery.isBlank()) all.take(3)
            else all.filter { textContainsQuery(it, committedQuery) }.take(3)
        }
        val adding = !fav
        fav = storeBar.toggleHit(
            gal.galiciana.app.domain.OcrHit(
                title = publicationTitle.ifBlank { "Documento" },
                parentTitle = storeBar.workParent(path) ?: doc?.title,
                snippets = lines,
                path = path, imageId = imageId, position = currentPos,
                savedQuery = if (committedQuery == highlightTerms(initialQuery)) initialQuery else committedQuery,
            ),
            useOpenedResult = true,
        )
        if (adding && fav) {
            showPageActions = false
            pickFolder = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showChrome) TopAppBar(
                title = {
                    Column {
                        Text(publicationTitle.ifBlank { "Documento" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (pageCount > 0) "Página $currentPos de $pageCount" else "Página $currentPos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Filled.Search, if (searchExpanded) "Plegar búsqueda en la página" else "Buscar en la página")
                    }
                    IconButton(onClick = { showPageActions = true }) {
                        Icon(Icons.Filled.MoreVert, "Acciones de esta página")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (showChrome && !searchExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).heightIn(min = 56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { if (currentPos > 1) currentPos-- },
                        modifier = Modifier.weight(1f), enabled = !loading && currentPos > 1) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                        Text("Anterior")
                    }
                    Text(if (pageCount > 0) "$currentPos / $pageCount" else "$currentPos / …",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.semantics { contentDescription = "Página $currentPos de ${if (pageCount > 0) pageCount.toString() else "total pendiente"}" })
                    TextButton(onClick = { if (currentPos < pageCount) currentPos++ },
                        modifier = Modifier.weight(1f), enabled = !loading && pageCount > 0 && currentPos < pageCount) {
                        Text("Siguiente")
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.takeIf { imageId != null }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (showChrome && searchExpanded) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = { findQuery = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Buscar en esta página") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (findQuery.isNotEmpty()) IconButton(onClick = { findQuery = "" }) {
                                    Icon(Icons.Filled.Close, "Borrar texto de búsqueda")
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { startScan(findQuery) }),
                        )
                        FilledTonalButton(onClick = { startScan(findQuery) },
                            enabled = findQuery.isNotBlank() && !finding,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Buscar") }
                    }
                    if (committedQuery.isNotBlank()) TextButton(onClick = {
                        committedQuery = ""
                        findQuery = ""
                        altoMatches = emptyList()
                        hitAt = 0
                        ocrMatchCount = 0
                        finding = false
                        searchExpanded = false
                    }) { Text("Quitar búsqueda y marcas") }
                }
            }
            if (showChrome && !searchExpanded && committedQuery.isNotBlank()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button,
                        onClickLabel = "Editar búsqueda", onClick = { searchExpanded = true })
                        .padding(vertical = 6.dp)) {
                        Text(committedQuery, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(hitLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (altoMatches.isNotEmpty()) {
                        IconButton(onClick = { goPrevHit() }, enabled = !locating) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Coincidencia anterior")
                        }
                        IconButton(onClick = { goNextHit() }, enabled = !locating) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Siguiente coincidencia")
                        }
                    } else if (!finding && (altoStatus == AltoStatus.UNAVAILABLE || ocrFailed)) {
                        TextButton(onClick = { startScan(committedQuery) }) { Text("Reintentar") }
                    }
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Filled.Edit, "Editar búsqueda en la página")
                    }
                }
                searchStatus?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                if (imageId == null) Column(Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (loading) androidx.compose.material3.CircularProgressIndicator()
                    else {
                        Text("No se pudo obtener la imagen de esta página.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("Comprueba la conexión y vuelve a intentarlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { metadataRetry++ }) { Text("Reintentar") }
                    }
                } else ZoomablePage(
                    path = path, position = currentPos, imageId = imageId,
                    imageUrl = imageUrl, fallback = GalicianaUrls.viewer(path, currentPos),
                    pageW = altoPage.first, pageH = altoPage.second,
                    matches = altoMatches, currentMatch = hitAt,
                    showHighlights = showHighlights,
                    onToggleChrome = { showChrome = !showChrome },
                    onHideChrome = { showChrome = false },
                )
                if (!showChrome) Box(Modifier.align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars).padding(12.dp)) {
                    IconButton(onClick = { showChrome = true }, modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))) {
                        Icon(Icons.Filled.MoreVert, "Mostrar controles de lectura")
                    }
                }
            }
        }
    }
    if (showPageActions) {
        val pageActionBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ModalBottomSheet(onDismissRequest = { showPageActions = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth()
                .padding(bottom = pageActionBottomInset).clipToBounds()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("Página $currentPos", style = MaterialTheme.typography.headlineSmall)
                Text(publicationTitle.ifBlank { "Documento" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
                Text("Guardar y organizar", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .clickable(role = Role.Button, onClick = { toggleFavorite() }),
                    verticalAlignment = Alignment.CenterVertically) {
                    VieiraButton(selected = fav, size = 24.dp, onClick = { toggleFavorite() },
                        modifier = Modifier.clearAndSetSemantics { })
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(if (fav) "Página guardada" else "Guardar página", style = MaterialTheme.typography.bodyLarge)
                        Text(if (fav) "Toca para quitarla de favoritos" else "Añadir a tu archivo de favoritos",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                NoteFlow(store = storeBar,
                    hit = gal.galiciana.app.domain.OcrHit(title = publicationTitle, path = path,
                        imageId = imageId, position = currentPos, savedQuery = committedQuery),
                    isFav = fav, size = 24.dp, label = "Nota de referencia",
                    onChanged = { fav = storeBar.isFav(path, currentPos) })
                ViewerActionRow(Icons.Outlined.Folder, "Organizar en carpetas",
                    if (inFolders) "Esta página ya está en tu archivo" else "Elige dónde guardar esta página",
                    onClick = {
                        if (!fav) toggleFavorite()
                        showPageActions = false
                        pickFolder = true
                    })
                ViewerActionRow(Icons.Filled.Download, "Descargar página", "JPEG original o PDF", onClick = {
                    showPageActions = false
                    chooseDownloadFormat = true
                })
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("Lectura", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ViewerActionRow(Icons.Filled.TextSnippet, "Leer y copiar texto", "Texto reconocido de esta página", onClick = {
                    showPageActions = false
                    showOcr = true
                })
                if (altoMatches.isNotEmpty()) ViewerActionRow(
                    if (showHighlights) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    if (showHighlights) "Ocultar marcas" else "Mostrar marcas",
                    "Conserva la búsqueda mientras lees el original", onClick = {
                        showHighlights = !showHighlights
                        showPageActions = false
                    })
                ViewerActionRow(Icons.Filled.Info, "Datos de la publicación",
                    if (showBibliography) "Ocultar información" else "Título, página y fuente", onClick = {
                        showBibliography = !showBibliography
                    })
                if (showBibliography) SelectionContainer {
                    Column(Modifier.fillMaxWidth().padding(start = 56.dp, bottom = 16.dp)) {
                        Text(publicationTitle.ifBlank { "Título no disponible" }, style = MaterialTheme.typography.bodyMedium)
                        Text("Página $currentPos${page?.label?.let { " · $it" }.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        Text("Fuente: Galiciana · Biblioteca Dixital de Galicia",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                TextButton(onClick = {
                    ctxBar.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(GalicianaUrls.viewer(path, currentPos))))
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Abrir original en Galiciana") }
                ViewerActionRow(Icons.Filled.Home, "Volver al inicio", onClick = {
                    showPageActions = false
                    onHome()
                })
            }
        }
    }
    if (showOcr) {
        ModalBottomSheet(onDismissRequest = { showOcr = false },
            containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Texto de la página $currentPos", style = MaterialTheme.typography.titleLarge)
                        Text(publicationTitle.ifBlank { "Documento" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showOcr = false }) { Icon(Icons.Filled.Close, "Cerrar texto") }
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                when {
                    readerLoading -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Cargando texto…", modifier = Modifier.padding(top = 12.dp))
                    }
                    readerError != null -> {
                        Text(readerError.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { readerRetry++ }) { Text("Reintentar") }
                    }
                    else -> SelectionContainer(Modifier.weight(1f)) {
                        Text(readerText.ifBlank { "Esta página no tiene texto reconocido disponible." },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()))
                    }
                }
            }
        }
    }
    if (pickFolder) {
        gal.galiciana.app.ui.FolderPickSheet(
            store = storeBar,
            path = path,
            position = currentPos,
            onDismiss = { pickFolder = false },
        )
    }
}

@Composable
private fun ViewerActionRow(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick)
        .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ZoomablePage(
    path: String,
    position: Int,
    imageId: String?,
    imageUrl: String?,
    fallback: String,
    pageW: Float,
    pageH: Float,
    matches: List<AltoMatch>,
    currentMatch: Int = 0,
    showHighlights: Boolean = true,
    onToggleChrome: () -> Unit = {},
    onHideChrome: () -> Unit = {},
) {
    if (imageUrl == null) {
        Text(
            "Este METS no trajo idImagen.\n$fallback",
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var pageBmp by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }

    fun applyZoom(old: Float, nextRaw: Float, centroid: Offset, pan: Offset) {
        val next = nextRaw.coerceIn(1f, 8f)
        if (next <= 1.02f) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            return
        }
        val vw = box.width.toFloat().coerceAtLeast(1f)
        val vh = box.height.toFloat().coerceAtLeast(1f)
        val ratio = next / old.coerceAtLeast(0.01f)
        offsetX = offsetX * ratio + (centroid.x - vw / 2f) * (1f - ratio) + pan.x
        offsetY = offsetY * ratio + (centroid.y - vh / 2f) * (1f - ratio) + pan.y
        val bounded = constrainPageTransform(vw, vh,
            pageBmp?.width?.toFloat() ?: vw, pageBmp?.height?.toFloat() ?: vh,
            next, offsetX, offsetY)
        offsetX = bounded.x
        offsetY = bounded.y
        scale = bounded.scale
    }

    LaunchedEffect(path, position, imageId) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    var pageFail by remember(imageUrl) { mutableStateOf(false) }
    var preview by remember(imageUrl) { mutableStateOf(false) }
    var imageError by remember(imageUrl) { mutableStateOf<String?>(null) }
    var retry by remember(imageUrl) { mutableIntStateOf(0) }
    LaunchedEffect(imageUrl, retry) {
        pageBmp = null
        pageFail = false
        imageError = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val page = fetchGalicianaPage(imageUrl, path, position, imageId.orEmpty())
                val bitmap = decodeGalicianaPage(page.bytes) ?: error("Formato de imagen no reconocido")
                bitmap.asImageBitmap() to page.preview
            }
        }
        result.onSuccess {
            pageBmp = it.first
            preview = it.second
        }.onFailure {
            imageError = it.message
            pageFail = true
        }
    }

    // Only explicit previous/next-match navigation may focus a match.
    // OCR arrival, page entry and viewport changes never zoom automatically.
    var previousMatch by remember(path, position) { mutableIntStateOf(currentMatch) }
    LaunchedEffect(currentMatch) {
        val changed = previousMatch != currentMatch
        previousMatch = currentMatch
        if (!changed || !showHighlights) return@LaunchedEffect
        val bitmap = pageBmp ?: return@LaunchedEffect
        val target = matches.getOrNull(currentMatch)?.boxes?.firstOrNull() ?: return@LaunchedEffect
        if (box.width == 0 || box.height == 0 || pageW <= 1f || pageH <= 1f) return@LaunchedEffect
        val focused = focusPageMatch(box.width.toFloat(), box.height.toFloat(),
            bitmap.width.toFloat(), bitmap.height.toFloat(), pageW, pageH, target, scale)
        scale = focused.scale
        offsetX = focused.x
        offsetY = focused.y
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF1C1A17))
            .onSizeChanged { box = it }
            .semantics {
                contentDescription = "Imagen digitalizada de la página $position"
                customActions = listOf(
                    CustomAccessibilityAction("Ampliar imagen") {
                        applyZoom(scale, scale * 1.5f, Offset(box.width / 2f, box.height / 2f), Offset.Zero)
                        true
                    },
                    CustomAccessibilityAction("Reducir imagen") {
                        applyZoom(scale, scale / 1.5f, Offset(box.width / 2f, box.height / 2f), Offset.Zero)
                        true
                    },
                    CustomAccessibilityAction("Ver imagen completa") {
                        applyZoom(scale, 1f, Offset.Zero, Offset.Zero)
                        true
                    },
                    CustomAccessibilityAction("Desplazar hacia abajo") {
                        applyZoom(scale, scale, Offset.Zero, Offset(0f, -box.height / 3f))
                        true
                    },
                    CustomAccessibilityAction("Desplazar hacia arriba") {
                        applyZoom(scale, scale, Offset.Zero, Offset(0f, box.height / 3f))
                        true
                    },
                    CustomAccessibilityAction("Desplazar hacia la derecha") {
                        applyZoom(scale, scale, Offset.Zero, Offset(-box.width / 3f, 0f))
                        true
                    },
                    CustomAccessibilityAction("Desplazar hacia la izquierda") {
                        applyZoom(scale, scale, Offset.Zero, Offset(box.width / 3f, 0f))
                        true
                    },
                    CustomAccessibilityAction("Mostrar u ocultar controles de lectura") { onToggleChrome(); true },
                )
            }
            .pointerInput(path, position) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (zoom != 1f || pan != Offset.Zero) onHideChrome()
                    applyZoom(scale, scale * zoom, centroid, pan)
                }
            }
            .pointerInput(path, position) {
                detectTapGestures(
                    onDoubleTap = { pos ->
                        onHideChrome()
                        if (scale > 1.15f) applyZoom(scale, 1f, pos, Offset.Zero)
                        else applyZoom(1f, 2.6f, pos, Offset.Zero)
                    },
                    onTap = { onToggleChrome() },
                    onLongPress = { onHideChrome() },
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            pageBmp == null && !pageFail -> LinearProgressIndicator(Modifier.fillMaxWidth(0.4f))
            pageFail -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No se pudo abrir la página", color = Color.White)
                Text(imageError.orEmpty().take(100), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { retry++ }) { Text("Reintentar") }
            }
            else -> {
                val bmp = pageBmp
                Canvas(Modifier.fillMaxSize()) {
                    if (bmp == null) return@Canvas
                    val iw = bmp.width.toFloat().coerceAtLeast(1f)
                    val ih = bmp.height.toFloat().coerceAtLeast(1f)
                    val fit = min(size.width / iw, size.height / ih)
                    val dw = iw * fit * scale
                    val dh = ih * fit * scale
                    val ox = (size.width - dw) / 2f + offsetX
                    val oy = (size.height - dh) / 2f + offsetY
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()),
                        dstSize = IntSize(dw.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.High,
                    )
                    if (showHighlights && matches.isNotEmpty() && pageW > 1f && pageH > 1f) {
                        val sx = dw / pageW
                        val sy = dh / pageH
                        // Draw the current occurrence last so its outline remains visible.
                        val ordered = matches.indices.filter { it != currentMatch } +
                            listOfNotNull(currentMatch.takeIf { it in matches.indices })
                        ordered.forEach { index ->
                            val active = index == currentMatch
                            matches[index].boxes.forEach { hit ->
                                val left = ox + hit.left * sx
                                val top = oy + hit.top * sy
                                val w = (hit.width * sx).coerceAtLeast(2f)
                                val h = (hit.height * sy).coerceAtLeast(2f)
                                drawRoundRect(
                                    color = if (active) Color(0x66FFC54D) else Color(0x40FFE36D),
                                    topLeft = Offset(left, top), size = Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                                )
                                drawRoundRect(
                                    color = if (active) Color(0xFF075A9C) else Color(0xFF996700),
                                    topLeft = Offset(left, top), size = Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                                    style = Stroke(width = if (active) 2.5.dp.toPx() else 1.dp.toPx()),
                                )
                            }
                        }
                    }
                }

                if (preview) Text(
                    "Vista previa de menor resolución · Reintentar",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .background(Color(0xCC111111))
                        .clickable { retry++ }
                        .padding(12.dp),
                )
            }
        }
    }
}

private data class PageImage(val bytes: ByteArray, val preview: Boolean)

// Keep bulk downloads on the same full-image path as the viewer's download button.
internal suspend fun fetchFullGalicianaPage(path: String, position: Int, imageId: String): ByteArray =
    fetchGalicianaPage(GalicianaUrls.jpeg(imageId), path, position, imageId, allowPreview = false).bytes

private suspend fun fetchGalicianaPage(
    url: String, path: String, position: Int, imageId: String, allowPreview: Boolean = true,
): PageImage {
    val referer = GalicianaUrls.viewer(path, position)
    suspend fun pull(candidate: String): ByteArray {
        val request = okhttp3.Request.Builder().url(candidate)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .header("Accept", "image/jpeg,image/png,image/*;q=0.9,*/*;q=0.1")
            .header("Referer", referer)
            .build()
        return Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Respuesta sin imagen")
            val input = body.byteStream()
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (output.size() + n > 32 * 1024 * 1024) error("Imagen mayor de 32 MB")
                output.write(buffer, 0, n)
            }
            output.toByteArray().also { bytes ->
                if (bytes.size < 100 ||
                    !((bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) ||
                      (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()))) {
                    error("El servidor devolvió contenido que no es una imagen")
                }
            }
        }
    }
    var last: Throwable? = null
    val preferred = imageId.takeIf { it.isNotBlank() }?.let(GalicianaUrls::jpegHi) ?: url
    for (attempt in 0..1) {
        try { return PageImage(pull(preferred), false) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { last = e }
        if (attempt == 0) {
            // The viewer can establish the session expected by its image endpoint.
            runCatching {
                Http.client.newCall(okhttp3.Request.Builder().url(referer).build())
                    .execute().close()
            }
            delay(600)
        }
    }
    if (imageId.isNotBlank()) {
        try { return PageImage(pull(url), false) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { last = e }
        try { return PageImage(pull(GalicianaUrls.jpegDownload(path, position, imageId)), false) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { last = e }
        if (allowPreview) {
            try { return PageImage(pull(GalicianaUrls.thumb(imageId)), true) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { last = e }
        }
    }
    error(last?.message ?: "Imagen no disponible")
}

private fun decodeGalicianaPage(bytes: ByteArray): android.graphics.Bitmap? {
    if (bytes.size < 100) return null
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    // Keep the long side near 7000 px for legible zoom, within a roughly 80 MB bitmap.
    val longSide = maxOf(bounds.outWidth, bounds.outHeight)
    val ratio = minOf(1.0, 7000.0 / longSide)
    if (android.os.Build.VERSION.SDK_INT >= 28) {
        try {
            return android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
            ) { decoder, _, _ ->
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setMemorySizePolicy(android.graphics.ImageDecoder.MEMORY_POLICY_LOW_RAM)
                if (ratio < 1.0) decoder.setTargetSize(
                    (bounds.outWidth * ratio).toInt().coerceAtLeast(1),
                    (bounds.outHeight * ratio).toInt().coerceAtLeast(1),
                )
            }
        } catch (_: Exception) {
            // Older/quirky image formats can still be decoded by BitmapFactory below.
        }
    }
    var sample = 1
    while (longSide / sample > 7000) sample *= 2
    val options = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

