package gal.galiciana.app.ui.viewer

import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import gal.galiciana.app.i18n.tr
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
import androidx.compose.ui.graphics.asAndroidBitmap
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
    var ocr by remember { mutableStateOf("") }  // empty until the page text has arrived
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
    var matchPages by remember(path) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var pagesChecked by remember(path) { mutableIntStateOf(0) }
    var pagesToCheck by remember(path) { mutableIntStateOf(0) }
    var pagesFailed by remember(path) { mutableIntStateOf(0) }
    var landOnLastMatch by remember { mutableStateOf(false) }
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
            .onFailure { error = tr("Non se puido cargar a información desta publicación.", "No se pudo cargar la información de esta publicación.") }
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
        if (landOnLastMatch) {
            hitAt = altoMatches.lastIndex.coerceAtLeast(0)
            landOnLastMatch = false
        }
        finding = false
    }

    // The query is read on the other pages too, so the reader can go from match to
    // match across the document instead of paging blind.
    LaunchedEffect(path, committedQuery, pageCount, searchRequest) {
        matchPages = emptyMap()
        pagesChecked = 0
        pagesFailed = 0
        val q = highlightTerms(committedQuery)
        if (q.isBlank() || pageCount == 0) {
            pagesToCheck = 0
            return@LaunchedEffect
        }
        val order = issueScanOrder(currentPos, pageCount)
        pagesToCheck = order.size
        val gate = Semaphore(4)
        coroutineScope {
            order.forEach { pos ->
                launch {
                    gate.withPermit {
                        val img = pages.firstOrNull { it.position == pos }?.imageId
                        val key = "$pos|${img.orEmpty()}"
                        val text = ocrCache[key] ?: try {
                            withContext(Dispatchers.IO) { ocrRepo.loadPage(path, pos, img).ocrText }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                        if (text == null) pagesFailed++
                        else {
                            ocrCache[key] = text
                            val count = countMatches(text, q)
                            if (count > 0) matchPages = matchPages + (pos to count)
                        }
                        pagesChecked++
                    }
                }
            }
        }
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
            readerError = tr("Non se puido cargar o texto desta páxina. Comproba a conexión e téntao de novo.", "No se pudo cargar el texto de esta página. Comprueba la conexión e inténtalo de nuevo.")
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
    var goToPage by remember { mutableStateOf(false) }
    var goToText by remember { mutableStateOf("") }
    var noteTick by remember { mutableIntStateOf(0) }
    val hasNote = remember(path, currentPos, fav, noteTick) { storeBar.noteOf(path, currentPos).isNotBlank() }
    var chooseDownloadFormat by remember { mutableStateOf(false) }
    var inFolders by remember { mutableStateOf(storeBar.foldersOf(path, currentPos).isNotEmpty()) }
    LaunchedEffect(path, currentPos, fav, pickFolder) {
        inFolders = storeBar.foldersOf(path, currentPos).isNotEmpty()
    }

    val snackbar = remember { SnackbarHostState() }
    fun doDownload(format: PageFormat) {
        val id = imageId
        if (format == PageFormat.JPEG && id == null) return
        val page = currentPos
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val progress = launch { snackbar.showSnackbar(tr("Gardando a páxina $page…", "Guardando página $page…"), duration = SnackbarDuration.Indefinite) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = if (format == PageFormat.PDF) PageDownloads.pdf(ctxBar, path, page, id)
                        else fetchFullGalicianaPage(path, page, requireNotNull(id))
                    PageDownloads.save(ctxBar, path, page, bytes, format)
                }
            }
            snackbar.currentSnackbarData?.dismiss()
            progress.cancel()
            if (result.isSuccess) {
                snackbar.showSnackbar(
                    tr("Páxina $page gardada en ${gal.galiciana.app.data.downloadLocation(format, android.os.Build.VERSION.SDK_INT)}", "Página $page guardada en ${gal.galiciana.app.data.downloadLocation(format, android.os.Build.VERSION.SDK_INT)}"),
                    withDismissAction = true, duration = SnackbarDuration.Long)
            } else {
                val retry = snackbar.showSnackbar(
                    tr("Non se puido descargar a páxina. ${gal.galiciana.app.ui.friendlyError(result.exceptionOrNull())}", "No se pudo descargar la página. ${gal.galiciana.app.ui.friendlyError(result.exceptionOrNull())}"),
                    actionLabel = "Reintentar", withDismissAction = true, duration = SnackbarDuration.Long)
                if (retry == SnackbarResult.ActionPerformed && currentPos == page) doDownload(format)
            }
        }
    }

    if (goToPage) {
        val wanted = goToText.toIntOrNull()
        val valid = wanted != null && wanted in 1..pageCount
        AlertDialog(
            onDismissRequest = { goToPage = false },
            title = { Text(tr("Ir á páxina", "Ir a la página")) },
            text = {
                OutlinedTextField(value = goToText, onValueChange = { goToText = it.filter(Char::isDigit).take(5) },
                    singleLine = true, label = { Text(tr("Número de páxina (1–$pageCount)", "Número de página (1–$pageCount)")) },
                    isError = goToText.isNotEmpty() && !valid,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = {
                        if (valid) { currentPos = wanted!!; goToPage = false }
                    }))
            },
            confirmButton = { TextButton(enabled = valid, onClick = { currentPos = wanted!!; goToPage = false }) { Text("Ir") } },
            dismissButton = { TextButton(onClick = { goToPage = false }) { Text("Cancelar") } },
        )
    }

    if (chooseDownloadFormat) {
        AlertDialog(
            onDismissRequest = { chooseDownloadFormat = false },
            title = { Text(tr("Descargar a páxina $currentPos", "Descargar página $currentPos")) },
            text = { Text(tr("Elixe JPEG ou PDF para esta páxina. O PDF inclúe o texto OCR dispoñible para buscar ou copiar; a súa selección non segue a posición das palabras na imaxe.", "Elige JPEG o PDF para esta página. El PDF incluye el texto OCR disponible para buscar o copiar; su selección no sigue la posición de las palabras en la imagen.")) },
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
        if (altoMatches.isNotEmpty() && hitAt < altoMatches.lastIndex) {
            hitAt += 1
            return
        }
        val next = adjacentMatchPage(matchPages.keys, currentPos, forward = true)
        when {
            next != null -> currentPos = next
            altoMatches.isNotEmpty() -> hitAt = 0
        }
    }

    fun goPrevHit() {
        showChrome = true
        if (altoMatches.isNotEmpty() && hitAt > 0) {
            hitAt -= 1
            return
        }
        val previous = adjacentMatchPage(matchPages.keys, currentPos, forward = false)
        when {
            previous != null -> {
                landOnLastMatch = true
                currentPos = previous
            }
            altoMatches.isNotEmpty() -> hitAt = altoMatches.lastIndex
        }
    }

    val otherMatchPages = matchPages.keys.any { it != currentPos }
    val hitLabel = when {
        altoMatches.isNotEmpty() -> tr("${hitAt + 1} de ${altoMatches.size} nesta páxina", "${hitAt + 1} de ${altoMatches.size} en esta página")
        locating -> "Localizando…"
        finding -> tr("Consultando o texto…", "Consultando texto…")
        ocrMatchCount > 0 -> tr("$ocrMatchCount no texto", "$ocrMatchCount en el texto")
        otherMatchPages -> tr("Non está nesta páxina", "No está en esta página")
        else -> tr("Sen marcas", "Sin marcas")
    }
    val searchStatus = when {
        committedQuery.isBlank() -> null
        altoMatches.isNotEmpty() -> if (!finding && ocrFailed)
            tr("As marcas están dispoñibles; non se puido cargar o texto para lelo.", "Las marcas están disponibles; no se pudo cargar el texto para leerlo.") else null
        locating || finding -> null
        altoStatus == AltoStatus.UNAVAILABLE ->
            tr("Non se puideron consultar as posicións das palabras. Podes tentalo de novo ou ler o texto.", "No se pudieron consultar las posiciones de las palabras. Puedes reintentar o leer el texto.")
        altoStatus == AltoStatus.INVALID ->
            tr("As posicións desta páxina non permiten un marcado fiable. Podes consultar o texto.", "Las posiciones de esta página no permiten un marcado fiable. Puedes consultar el texto.")
        ocrFailed -> tr("Non se puido consultar o texto desta páxina. Podes tentalo de novo.", "No se pudo consultar el texto de esta página. Puedes reintentar.")
        ocrMatchCount > 0 ->
            tr("Hai coincidencias no texto, pero non se localizaron sobre a imaxe. O texto e as súas posicións poden diferir.", "Hay coincidencias en el texto, pero no se han localizado sobre la imagen. El texto y sus posiciones pueden diferir.")
        altoStatus == AltoStatus.EMPTY -> tr("Esta páxina non ten palabras con posicións dispoñibles.", "Esta página no tiene palabras con posiciones disponibles.")
        otherMatchPages -> tr("Toca unha páxina ou usa as frechas.", "Toca una página o usa las flechas.")
        else -> tr("Non se atopou esa expresión nesta páxina. Proba outra palabra ou revisa o texto.", "No se encontró esa expresión en esta página. Prueba otra palabra o revisa el texto.")
    }

    BackHandler(enabled = searchExpanded && showChrome) {
        keyboard?.hide()
        searchExpanded = false
    }
    val publicationTitle = storeBar.workTitle(path) ?: doc?.title.orEmpty()
    fun toggleFavorite(openFolders: Boolean = true) {
        val lines = if (ocrPosition != currentPos || ocr.isBlank()) emptyList()
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
        if (adding && fav && openFolders) scope.launch {
            // Saving is one act; filing stays one tap away instead of interrupting.
            showPageActions = false
            snackbar.currentSnackbarData?.dismiss()
            val organize = snackbar.showSnackbar(tr("Páxina gardada", "Página guardada"), actionLabel = "Organizar",
                duration = SnackbarDuration.Short)
            if (organize == SnackbarResult.ActionPerformed) pickFolder = true
        }
        if (!adding && !fav) scope.launch {
            // Removal is one tap, so it stays reversible: the page comes back with its
            // excerpt, folders and Nota de referencia.
            snackbar.currentSnackbarData?.dismiss()
            val undo = snackbar.showSnackbar(tr("Páxina quitada de Gardados", "Página quitada de Guardados"), actionLabel = tr("Desfacer", "Deshacer"),
                duration = SnackbarDuration.Long)
            if (undo == SnackbarResult.ActionPerformed && !storeBar.isFav(path, currentPos)) toggleFavorite(openFolders = false)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // The page bar already clears the system buttons; without it the snackbar must.
        snackbarHost = { SnackbarHost(snackbar, if (showChrome && !searchExpanded) Modifier
            else Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
        topBar = {
            if (showChrome) TopAppBar(
                title = {
                    // The bar has a fixed height; two lines at 200 % would clip the page line.
                    gal.galiciana.app.ui.CappedFontScale { Column {
                        Text(publicationTitle.ifBlank { "Documento" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(
                                if (pageCount > 0) tr("Páxina $currentPos de $pageCount", "Página $currentPos de $pageCount") else tr("Páxina $currentPos", "Página $currentPos"),
                                if (fav && hasNote) tr("Gardada con nota", "Guardada con nota") else if (fav) tr("Gardada", "Guardada") else null,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    gal.galiciana.app.ui.VieiraButton(selected = fav, size = 24.dp, onClick = { toggleFavorite() })
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Filled.Search, if (searchExpanded) tr("Pregar a busca", "Plegar la búsqueda") else tr("Buscar no documento", "Buscar en el documento"))
                    }
                    IconButton(onClick = { showPageActions = true }) {
                        Icon(Icons.Filled.MoreVert, tr("Accións desta páxina", "Acciones de esta página"))
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
                    TextButton(onClick = { goToText = ""; goToPage = true }, enabled = pageCount > 1,
                        modifier = Modifier.semantics {
                            contentDescription = tr("Páxina $currentPos de ${if (pageCount > 0) pageCount.toString() else "total pendente"}", "Página $currentPos de ${if (pageCount > 0) pageCount.toString() else "total pendiente"}")
                        }, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface)) {
                        Text(if (pageCount > 0) "$currentPos / $pageCount" else "$currentPos / …",
                            style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = { if (currentPos < pageCount) currentPos++ },
                        modifier = Modifier.weight(1f), enabled = !loading && pageCount > 0 && currentPos < pageCount) {
                        Text(tr("Seguinte", "Siguiente"))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars.only(
                if (showChrome && searchExpanded) WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                else WindowInsetsSides.Horizontal))
            .imePadding()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.takeIf { imageId != null }?.let {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    TextButton(onClick = { metadataRetry++ }) { Text("Reintentar") }
                }
            }
            if (showChrome && searchExpanded) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = { findQuery = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(tr("Buscar no documento", "Buscar en el documento")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (findQuery.isNotEmpty()) IconButton(onClick = { findQuery = "" }) {
                                    Icon(Icons.Filled.Close, tr("Borrar o texto de busca", "Borrar texto de búsqueda"))
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { startScan(findQuery) }),
                        )
                        androidx.compose.material3.Button(onClick = { startScan(findQuery) },
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
                    }) { Text(tr("Quitar busca e marcas", "Quitar búsqueda y marcas")) }
                }
            }
            if (showChrome && !searchExpanded && committedQuery.isNotBlank()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button,
                        onClickLabel = tr("Editar busca", "Editar búsqueda"), onClick = { searchExpanded = true })
                        .padding(vertical = 6.dp)) {
                        Text(committedQuery, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(hitLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (altoMatches.isNotEmpty() || otherMatchPages) {
                        IconButton(onClick = { goPrevHit() }, enabled = !locating) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Coincidencia anterior", "Coincidencia anterior"))
                        }
                        IconButton(onClick = { goNextHit() }, enabled = !locating) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, tr("Seguinte coincidencia", "Siguiente coincidencia"))
                        }
                    } else if (!finding && (altoStatus == AltoStatus.UNAVAILABLE || ocrFailed)) {
                        TextButton(onClick = { startScan(committedQuery) }) { Text("Reintentar") }
                    }
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Filled.Edit, tr("Editar a busca", "Editar la búsqueda"))
                    }
                }
                if (pagesToCheck > 0) MatchPagesRow(
                    pages = matchPages,
                    current = currentPos,
                    checked = pagesChecked,
                    toCheck = pagesToCheck,
                    failed = pagesFailed,
                    pageCount = pageCount,
                    onOpen = { currentPos = it },
                    onRetry = { searchRequest++ },
                )
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
                        Text(tr("Non se puido obter a imaxe desta páxina.", "No se pudo obtener la imagen de esta página."),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(tr("Comproba a conexión e téntao de novo.", "Comprueba la conexión y vuelve a intentarlo."),
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
                    onSwipePage = { step -> (currentPos + step).takeIf { it in 1..pageCount }?.let { currentPos = it } },
                )
                if (!showChrome) Box(Modifier.align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars).padding(12.dp)) {
                    IconButton(onClick = { showChrome = true }, modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))) {
                        Icon(Icons.Filled.FullscreenExit, tr("Mostrar controis de lectura", "Mostrar controles de lectura"))
                    }
                }
            }
        }
    }
    if (showPageActions) {
        val pageActionBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ModalBottomSheet(onDismissRequest = { showPageActions = false },
            modifier = Modifier.statusBarsPadding(),
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth()
                .padding(bottom = pageActionBottomInset).clipToBounds()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text(tr("Páxina $currentPos", "Página $currentPos"), style = MaterialTheme.typography.headlineSmall)
                Text(publicationTitle.ifBlank { "Documento" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
                Text(tr("Gardar e organizar", "Guardar y organizar"), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .clickable(role = Role.Button, onClick = { toggleFavorite() }),
                    verticalAlignment = Alignment.CenterVertically) {
                    VieiraButton(selected = fav, size = 24.dp, onClick = { toggleFavorite() },
                        modifier = Modifier.clearAndSetSemantics { })
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(if (fav) tr("Páxina gardada", "Página guardada") else tr("Gardar páxina", "Guardar página"), style = MaterialTheme.typography.bodyLarge)
                        Text(if (fav) tr("Toca para quitala de Gardados", "Toca para quitarla de Guardados") else tr("Engadir a Gardados", "Añadir a Guardados"),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                NoteFlow(store = storeBar,
                    hit = gal.galiciana.app.domain.OcrHit(title = publicationTitle, path = path,
                        imageId = imageId, position = currentPos, savedQuery = committedQuery),
                    isFav = fav, size = 24.dp, label = "Nota de referencia",
                    onChanged = { fav = storeBar.isFav(path, currentPos); noteTick++ },
                    // Leaving the editor returns to the page being read, not to this sheet.
                    onEditorClosed = { showPageActions = false })
                ViewerActionRow(Icons.Outlined.Folder, tr("Organizar en cartafoles", "Organizar en carpetas"),
                    if (inFolders) tr("Esta páxina xa está nos teus cartafoles", "Esta página ya está en tus carpetas") else tr("Elixe onde gardar esta páxina", "Elige dónde guardar esta página"),
                    onClick = {
                        // Opening the sheet changes nothing; the page is saved only on "Listo".
                        showPageActions = false
                        pickFolder = true
                    })
                ViewerActionRow(Icons.Filled.Download, tr("Descargar páxina", "Descargar página"), tr("JPEG orixinal ou PDF", "JPEG original o PDF"), onClick = {
                    showPageActions = false
                    chooseDownloadFormat = true
                })
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("Lectura", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ViewerActionRow(Icons.Filled.TextSnippet, tr("Ler e copiar texto", "Leer y copiar texto"), tr("Texto recoñecido desta páxina", "Texto reconocido de esta página"), onClick = {
                    showPageActions = false
                    showOcr = true
                })
                if (altoMatches.isNotEmpty()) ViewerActionRow(
                    if (showHighlights) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    if (showHighlights) "Ocultar marcas" else "Mostrar marcas",
                    tr("Conserva a busca mentres les o orixinal", "Conserva la búsqueda mientras lees el original"), onClick = {
                        showHighlights = !showHighlights
                        showPageActions = false
                    })
                ViewerActionRow(Icons.Filled.Info, tr("Datos da publicación", "Datos de la publicación"),
                    if (showBibliography) "Ocultar información" else tr("Título, páxina e fonte", "Título, página y fuente"), onClick = {
                        showBibliography = !showBibliography
                    })
                if (showBibliography) SelectionContainer {
                    Column(Modifier.fillMaxWidth().padding(start = 56.dp, bottom = 16.dp)) {
                        Text(publicationTitle.ifBlank { tr("Título non dispoñible", "Título no disponible") }, style = MaterialTheme.typography.bodyMedium)
                        // The METS label repeats the page number when the source has no real one.
                        val sourceLabel = page?.label?.takeUnless { Regex("""^P[áa][gx]\.?\s*\d+$""").matches(it.trim()) }
                        Text(tr("Páxina $currentPos${sourceLabel?.let { " · $it" }.orEmpty()}", "Página $currentPos${sourceLabel?.let { " · $it" }.orEmpty()}"),
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        Text(tr("Fonte: Galiciana · Biblioteca Dixital de Galicia", "Fuente: Galiciana · Biblioteca Dixital de Galicia"),
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                ViewerActionRow(Icons.AutoMirrored.Filled.OpenInNew, tr("Abrir orixinal en Galiciana", "Abrir original en Galiciana"),
                    tr("Ábrese no navegador", "Se abre en el navegador"), onClick = {
                        ctxBar.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(GalicianaUrls.viewer(path, currentPos))))
                    })
                ViewerActionRow(Icons.Filled.Home, tr("Volver ao inicio", "Volver al inicio"), onClick = {
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
                        Text(tr("Texto da páxina $currentPos", "Texto de la página $currentPos"), style = MaterialTheme.typography.titleLarge)
                        Text(publicationTitle.ifBlank { "Documento" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showOcr = false }) { Icon(Icons.Filled.Close, tr("Pechar texto", "Cerrar texto")) }
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                when {
                    readerLoading -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(tr("Cargando o texto…", "Cargando texto…"), modifier = Modifier.padding(top = 12.dp))
                    }
                    readerError != null -> {
                        Text(readerError.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { readerRetry++ }) { Text("Reintentar") }
                    }
                    else -> SelectionContainer(Modifier.weight(1f)) {
                        Text(readerText.ifBlank { tr("Esta páxina non ten texto recoñecido dispoñible.", "Esta página no tiene texto reconocido disponible.") },
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
            onConfirmed = { if (!fav) toggleFavorite(openFolders = false) },
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
    onSwipePage: (Int) -> Unit = {},
) {
    val swipeHandler = androidx.compose.runtime.rememberUpdatedState(onSwipePage)
    if (imageUrl == null) {
        Text(
            tr("Galiciana non indica a imaxe dixitalizada desta páxina. Podes consultala na súa web:\n$fallback", "Galiciana no indica la imagen digitalizada de esta página. Puedes consultarla en su web:\n$fallback"),
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

    // Turning a page: a screen-sized copy of the old page slides out toward the gesture while
    // the new one slides in from the other side once its image is ready. The copy, never the
    // full page, is kept, and only for the 200 ms of the exit. Off when the system says not
    // to animate; then the page simply changes.
    val motionContext = LocalContext.current
    val reduceMotion = remember { gal.galiciana.app.ui.theme.motionReduced(motionContext) }
    val enter = remember { androidx.compose.animation.core.Animatable(0f) }
    var shownPosition by remember { mutableIntStateOf(position) }
    var pendingDirection by remember { mutableIntStateOf(0) }
    // The page on show. While reading it is the same object as pageBmp, so it costs nothing;
    // on a turn it outlives pageBmp just long enough to make the small exit copy.
    var shownBmp by remember { mutableStateOf<ImageBitmap?>(null) }
    var exitCopy by remember { mutableStateOf<ImageBitmap?>(null) }
    var exitDirection by remember { mutableIntStateOf(0) }
    val exit = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(position) {
        if (position != shownPosition) {
            val direction = if (position > shownPosition) 1 else -1
            pendingDirection = direction
            shownPosition = position
            val outgoing = shownBmp
            shownBmp = null
            val copy = if (reduceMotion || outgoing == null || box.width == 0 || box.height == 0) null
                else withContext(Dispatchers.Default) {
                    val source = outgoing.asAndroidBitmap()
                    val fit = min(box.width.toFloat() / source.width, box.height.toFloat() / source.height).coerceAtMost(1f)
                    android.graphics.Bitmap.createScaledBitmap(source,
                        (source.width * fit).roundToInt().coerceAtLeast(1),
                        (source.height * fit).roundToInt().coerceAtLeast(1), true).asImageBitmap()
                }
            if (copy != null) {
                exitCopy = copy
                exitDirection = direction
                try {
                    exit.snapTo(0f)
                    exit.animateTo(1f, androidx.compose.animation.core.tween(200,
                        easing = androidx.compose.animation.core.FastOutLinearInEasing))
                } finally { exitCopy = null }
            }
        }
    }

    val pageContext = LocalContext.current
    val budget = remember { pageBudget(pageContext) }
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
                val bitmap = decodeGalicianaPage(page.bytes, budget) ?: error(UNREADABLE_PAGE)
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

    LaunchedEffect(pageBmp) { pageBmp?.let { shownBmp = it } }
    LaunchedEffect(pageBmp) {
        val direction = pendingDirection
        if (pageBmp == null || direction == 0) return@LaunchedEffect
        pendingDirection = 0
        if (reduceMotion) { enter.snapTo(0f); return@LaunchedEffect }
        // Whatever interrupts the slide, the page ends where it belongs.
        try {
            enter.snapTo(direction * 0.35f)
            enter.animateTo(0f, androidx.compose.animation.core.tween(200,
                easing = androidx.compose.animation.core.FastOutSlowInEasing))
        } finally { enter.snapTo(0f) }
    }

    // Locator: the page never zooms on its own, so the active match announces itself
    // with an amber halo that pulses a few times when the matches arrive or the reader
    // moves to another one, then stays as a faint ring while the page is seen whole.
    val locatorPulse = remember { androidx.compose.animation.core.Animatable(1f) }
    val activeKey = matches.getOrNull(currentMatch)?.boxes?.firstOrNull()
    LaunchedEffect(activeKey, showHighlights) {
        if (activeKey == null || !showHighlights || reduceMotion) { locatorPulse.snapTo(1f); return@LaunchedEffect }
        try {
            repeat(3) {
                locatorPulse.snapTo(0f)
                locatorPulse.animateTo(1f, androidx.compose.animation.core.tween(700,
                    easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            }
        } finally { locatorPulse.snapTo(1f) }
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
                contentDescription = tr("Imaxe dixitalizada da páxina $position", "Imagen digitalizada de la página $position")
                customActions = listOf(
                    CustomAccessibilityAction(tr("Ampliar imaxe", "Ampliar imagen")) {
                        applyZoom(scale, scale * 1.5f, Offset(box.width / 2f, box.height / 2f), Offset.Zero)
                        true
                    },
                    CustomAccessibilityAction(tr("Reducir imaxe", "Reducir imagen")) {
                        applyZoom(scale, scale / 1.5f, Offset(box.width / 2f, box.height / 2f), Offset.Zero)
                        true
                    },
                    CustomAccessibilityAction(tr("Ver a imaxe completa", "Ver imagen completa")) {
                        applyZoom(scale, 1f, Offset.Zero, Offset.Zero)
                        true
                    },
                    CustomAccessibilityAction(tr("Desprazar cara abaixo", "Desplazar hacia abajo")) {
                        applyZoom(scale, scale, Offset.Zero, Offset(0f, -box.height / 3f))
                        true
                    },
                    CustomAccessibilityAction(tr("Desprazar cara arriba", "Desplazar hacia arriba")) {
                        applyZoom(scale, scale, Offset.Zero, Offset(0f, box.height / 3f))
                        true
                    },
                    CustomAccessibilityAction(tr("Desprazar cara á dereita", "Desplazar hacia la derecha")) {
                        applyZoom(scale, scale, Offset.Zero, Offset(-box.width / 3f, 0f))
                        true
                    },
                    CustomAccessibilityAction(tr("Desprazar cara á esquerda", "Desplazar hacia la izquierda")) {
                        applyZoom(scale, scale, Offset.Zero, Offset(box.width / 3f, 0f))
                        true
                    },
                    CustomAccessibilityAction(tr("Mostrar ou ocultar controis de lectura", "Mostrar u ocultar controles de lectura")) { onToggleChrome(); true },
                )
            }
            .pointerInput(path, position) {
                // A one-finger horizontal swipe on the whole page turns it. Observed without
                // consuming, so pinch, pan and taps keep working; zoomed pages pan instead.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false,
                        pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val startScale = scale
                    val startX = offsetX
                    var multi = false
                    var last = down.position
                    var dx = 0f
                    var dy = 0f
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } > 1) multi = true
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        dx += change.position.x - last.x
                        dy += change.position.y - last.y
                        last = change.position
                    }
                    // Whole page shown, or zoomed in but already at that side's edge: turn the page.
                    val atEdge = scale > 1.02f && kotlin.math.abs(offsetX - startX) < 2f
                    val turns = !multi && kotlin.math.abs(dx) > size.width * 0.22f && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f &&
                        ((startScale <= 1.02f && scale <= 1.02f) || atEdge)
                    if (turns) swipeHandler.value(if (dx < 0) 1 else -1)
                }
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
                Text(tr("Non se puido abrir a páxina", "No se pudo abrir la página"), color = Color.White)
                Text(if (imageError == UNREADABLE_PAGE) tr("O ficheiro recibido non é unha imaxe válida.", "El archivo recibido no es una imagen válida.")
                    else tr("Comproba a conexión e téntao de novo.", "Comprueba la conexión y vuelve a intentarlo."),
                    color = Color.LightGray, style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                OutlinedButton(onClick = { retry++ }) { Text("Reintentar") }
            }
            else -> {
                val bmp = pageBmp
                Canvas(Modifier.fillMaxSize().graphicsLayer {
                    translationX = enter.value * size.width
                    alpha = 1f - kotlin.math.abs(enter.value) * 1.5f
                }) {
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
                        matches.getOrNull(currentMatch)?.boxes?.firstOrNull()?.let { hit ->
                            val cx = ox + (hit.left + hit.width / 2f) * sx
                            val cy = oy + (hit.top + hit.height / 2f) * sy
                            val base = maxOf(hit.width * sx, hit.height * sy) / 2f + 10.dp.toPx()
                            val p = locatorPulse.value
                            // The pulse widens and fades; the resting ring only matters while the page is small.
                            if (p < 1f) drawCircle(
                                color = Color(0xFFFFC54D).copy(alpha = 0.85f * (1f - p)),
                                radius = base + p * 44.dp.toPx(), center = Offset(cx, cy),
                                style = Stroke(width = 3.dp.toPx()),
                            )
                            if (scale < 1.6f) drawCircle(
                                color = Color(0xFFFFC54D).copy(alpha = 0.7f),
                                radius = base, center = Offset(cx, cy),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
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
                        .clickable(role = Role.Button, onClickLabel = tr("Cargar a imaxe en alta resolución", "Cargar la imagen en alta resolución")) { retry++ }
                        .heightIn(min = 48.dp)
                        .padding(12.dp),
                )
            }
        }
        exitCopy?.let { copy ->
            Canvas(Modifier.fillMaxSize().graphicsLayer {
                translationX = -exitDirection * exit.value * size.width
                alpha = 1f - exit.value
            }) {
                val w = copy.width.toFloat()
                val h = copy.height.toFloat()
                drawImage(copy, dstOffset = IntOffset(((size.width - w) / 2f).roundToInt(), ((size.height - h) / 2f).roundToInt()),
                    dstSize = IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Low)
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
            val body = response.body ?: error(tr("Resposta sen imaxe", "Respuesta sin imagen"))
            val input = body.byteStream()
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (output.size() + n > 32 * 1024 * 1024) error(tr("Imaxe maior de 32 MB", "Imagen mayor de 32 MB"))
                output.write(buffer, 0, n)
            }
            output.toByteArray().also { bytes ->
                if (bytes.size < 100 ||
                    !((bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) ||
                      (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()))) {
                    error(tr("O servidor devolveu contido que non é unha imaxe", "El servidor devolvió contenido que no es una imagen"))
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
    error(last?.message ?: tr("Imaxe non dispoñible", "Imagen no disponible"))
}

private const val UNREADABLE_PAGE = "Formato de imagen no reconocido"

/** How large a decoded page may be on this device. */
internal data class PageBudget(val longSide: Int, val pixels: Double)

/**
 * Phones with 6 GB or more keep the full 7000 px page for legible zoom. Smaller or
 * low-RAM devices get a lighter page so the system does not close the app mid-reading.
 */
internal fun pageBudget(totalRamBytes: Long, lowRam: Boolean): PageBudget = when {
    lowRam || totalRamBytes < 3_500_000_000L -> PageBudget(4000, 8_000_000.0)
    totalRamBytes < 5_500_000_000L -> PageBudget(5200, 13_000_000.0)
    else -> PageBudget(7000, 22_000_000.0)
}

private fun pageBudget(context: android.content.Context): PageBudget {
    val manager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val info = android.app.ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
    return pageBudget(info.totalMem, manager.isLowRamDevice)
}

private fun decodeGalicianaPage(bytes: ByteArray, budget: PageBudget): android.graphics.Bitmap? {
    if (bytes.size < 100) return null
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    // Keep the long side within the device budget (7000 px on capable phones for legible
    // zoom), never above ~88 MB of ARGB pixels: Android refuses to draw bitmaps over
    // 100 MB and the app would close.
    val longSide = maxOf(bounds.outWidth, bounds.outHeight)
    val pixels = bounds.outWidth.toDouble() * bounds.outHeight
    val ratio = minOf(1.0, budget.longSide.toDouble() / longSide, kotlin.math.sqrt(budget.pixels / pixels))
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
    while (longSide / sample > budget.longSide || pixels / (sample.toDouble() * sample) > budget.pixels) sample *= 2
    val options = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

