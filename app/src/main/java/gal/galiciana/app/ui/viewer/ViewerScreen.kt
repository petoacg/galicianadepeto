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
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.traversalIndex
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
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.offset
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
    // 0 until the reader asks to read the rest of the document; never automatic.
    var scanRequest by remember(path) { mutableIntStateOf(0) }
    var landOnLastMatch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var metadataRetry by remember(path) { mutableIntStateOf(0) }
    LaunchedEffect(path, metadataRetry) {
        loading = true
        error = null
        // Galiciana sometimes fails the first request of a session; without this list the
        // viewer cannot know there are other pages, so it tries again before giving up.
        var attempt = 0
        var result: Result<gal.galiciana.app.domain.MetsDocument>
        while (true) {
            result = runCatching { withContext(Dispatchers.IO) { metsRepo.load(path) } }
            if (result.isSuccess || attempt >= 2) break
            attempt++
            delay(700L * attempt)
        }
        result
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
    // The page asked for opens on its own, with nothing else downloading beside it. Only
    // once it is on screen, and after a short pause for its marks to load, are its two
    // neighbours read ahead, one at a time: first the one in the reading direction.
    var shownPage by remember(path) { mutableIntStateOf(0) }
    var readingDirection by remember(path) { mutableIntStateOf(1) }
    LaunchedEffect(currentPos) {
        if (shownPage != 0 && currentPos != shownPage)
            readingDirection = if (currentPos > shownPage) 1 else -1
    }
    LaunchedEffect(shownPage) {
        if (shownPage == 0) return@LaunchedEffect
        delay(800)
        listOf(shownPage + readingDirection, shownPage - readingDirection)
            .filter { it in 1..pageCount }
            .forEach { pos ->
                val id = pages.firstOrNull { it.position == pos }?.imageId ?: return@forEach
                try { readAheadPage(path, pos, id) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { }
            }
    }
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
                // Counting runs off the screen's thread: on a long page it takes a quarter of
                // a second, which would stall the page turn that has just started.
                text to withContext(Dispatchers.Default) { countMatches(text, q) }
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

    // Pages with matches come first from the search results, which already named them:
    // entering the viewer downloads nothing but the open page. Reading the rest of the
    // document is the reader's choice (scanRequest), done two pages at a time and with
    // the text matching off the main thread, so the page being read stays smooth.
    LaunchedEffect(path, committedQuery) {
        scanRequest = 0
        pagesChecked = 0
        pagesToCheck = 0
        pagesFailed = 0
        matchPages = SearchPageHints.pages(path, committedQuery).associateWith { 0 }
    }
    LaunchedEffect(scanRequest) {
        if (scanRequest == 0) return@LaunchedEffect
        pagesChecked = 0
        pagesFailed = 0
        val q = highlightTerms(committedQuery)
        if (q.isBlank() || pageCount == 0) return@LaunchedEffect
        val order = issueScanOrder(currentPos, pageCount)
        pagesToCheck = order.size
        val gate = Semaphore(2)
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
                        if (text != null) ocrCache[key] = text
                        val count = text?.let { withContext(Dispatchers.Default) { countMatches(it, q) } }
                        // A new query resets the list before this pass is cancelled: a page
                        // read for the old query must not be counted under the new one.
                        if (highlightTerms(committedQuery) != q) return@withPermit
                        if (count == null) pagesFailed++
                        else if (count > 0) matchPages = matchPages + (pos to count)
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

    // In full screen, Back first brings the reading controls back; it never leaves the page.
    BackHandler(enabled = !showChrome) { showChrome = true }
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

    // Full screen never moves the page: it always fills the screen, and the reading
    // controls fade in and out over its margins instead of pushing it up and down.
    val chromeMotionContext = LocalContext.current
    val chromeReduceMotion = remember { gal.galiciana.app.ui.theme.motionReduced(chromeMotionContext) }
    val chromeIn = if (chromeReduceMotion) androidx.compose.animation.EnterTransition.None
        else androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(160))
    val chromeOut = if (chromeReduceMotion) androidx.compose.animation.ExitTransition.None
        else androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
    var bottomBarHeight by rememberSaveable { mutableIntStateOf(0) }
    // The page's frame is measured with the controls in view and kept while they are hidden,
    // and across a restore, so full screen only adds margin around the page. Only the title
    // bar counts at the top: an error notice or the open search form are passing panels
    // over the page, not part of its frame.
    var topBarsHeight by rememberSaveable { mutableIntStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // The snackbar sits above the page bar when it shows, above the system buttons otherwise.
        snackbarHost = { SnackbarHost(snackbar, if (showChrome && !searchExpanded)
            Modifier.padding(bottom = with(density) { bottomBarHeight.toDp() })
            else Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { _ ->
        // The controls lie over the page, so TalkBack is told the reading order outright:
        // title bar, page, then the search and page bar.
        Box(Modifier.fillMaxSize().semantics { isTraversalGroup = true }) {
            Box(Modifier.fillMaxSize().clipToBounds()) {
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
                    // Centred on the screen, as a full-screen page, and kept clear of the title
                    // bar. The search rows below may lie over the page's foot while shown; a
                    // tap hides them, and the page stays where it is.
                    frameTop = topBarsHeight.toFloat(),
                    frameBottom = topBarsHeight.toFloat(),
                    canTurn = { step -> (currentPos + step) in 1..pageCount },
                    onPageShown = { shownPage = it },
                    neighbourImageId = { step -> pages.firstOrNull { it.position == currentPos + step }?.imageId },
                )
                androidx.compose.animation.AnimatedVisibility(!showChrome, enter = chromeIn, exit = chromeOut,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars).padding(12.dp)) {
                    IconButton(onClick = { showChrome = true }, modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))) {
                        Icon(Icons.Filled.FullscreenExit, tr("Mostrar controis de lectura", "Mostrar controles de lectura"))
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(showChrome, enter = chromeIn, exit = chromeOut,
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().semantics { traversalIndex = -1f }) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))) {
                    TopAppBar(
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
                        modifier = Modifier.onSizeChanged { topBarsHeight = it.height },
                    )
                    error?.takeIf { imageId != null }?.let {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            TextButton(onClick = { metadataRetry++ }) { Text("Reintentar") }
                        }
                    }
                    if (searchExpanded) {
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
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            if (!showChrome && loading) LinearProgressIndicator(Modifier.fillMaxWidth()
                .align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars))
            androidx.compose.animation.AnimatedVisibility(showChrome && !searchExpanded, enter = chromeIn, exit = chromeOut,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().semantics { traversalIndex = 1f }) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .onSizeChanged { bottomBarHeight = it.height }) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // The search and its results sit with the page bar, below the page, so the
                    // top keeps only the title and the page rests high on the screen.
                    if (committedQuery.isNotBlank()) {
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
                        MatchPagesRow(
                            pages = matchPages,
                            current = currentPos,
                            checked = pagesChecked,
                            toCheck = pagesToCheck,
                            failed = pagesFailed,
                            pageCount = pageCount,
                            scanned = scanRequest > 0,
                            onOpen = { currentPos = it },
                            onScanAll = { scanRequest++ },
                        )
                        searchStatus?.let { message ->
                            Text(message, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
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
    frameTop: Float = 0f,
    frameBottom: Float = 0f,
    canTurn: (Int) -> Boolean = { true },
    onPageShown: (Int) -> Unit = {},
    neighbourImageId: (Int) -> String? = { null },
) {
    val canTurnNow = androidx.compose.runtime.rememberUpdatedState(canTurn)
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
    // A screen-sized copy of the page, ready at once when the page was read ahead or seen
    // a moment ago: the turn then brings the page itself, and the sharp image replaces it
    // in place, unnoticed. Without one, a blank sheet stands in.
    val previewBmp = remember(imageUrl) { PagePreviews.get(imageId.orEmpty()) }
    val drawnBmp = pageBmp ?: previewBmp
    var pageFail by remember(imageUrl) { mutableStateOf(false) }

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
        offsetY = offsetY * ratio + (centroid.y - pageFrameCenter(vh, frameTop, frameBottom)) * (1f - ratio) + pan.y
        val bounded = constrainPageTransform(vw, vh,
            drawnBmp?.width?.toFloat() ?: vw, drawnBmp?.height?.toFloat() ?: vh,
            next, offsetX, offsetY, frameTop, frameBottom)
        offsetX = bounded.x
        offsetY = bounded.y
        scale = bounded.scale
    }

    // Turning a page is one continuous movement, like sliding sheets across a desk: the
    // page follows the finger, and on release it carries on out while the next sheet comes
    // in attached to it. Until the new image arrives that sheet is a blank page of the same
    // shape; the image then fades onto it in place. Nothing jumps and nothing goes black.
    // With animations off the page simply changes.
    val motionContext = LocalContext.current
    val reduceMotion = remember { gal.galiciana.app.ui.theme.motionReduced(motionContext) }
    val turnScope = rememberCoroutineScope()
    var dragX by remember { mutableFloatStateOf(0f) }
    var shownPosition by remember { mutableIntStateOf(position) }
    // The page on show; on a turn it is drawn leaving for the length of the slide only.
    var shownBmp by remember { mutableStateOf<ImageBitmap?>(null) }
    var outgoing by remember { mutableStateOf<ImageBitmap?>(null) }
    var outgoingScale by remember { mutableFloatStateOf(1f) }
    var outgoingX by remember { mutableFloatStateOf(0f) }
    var outgoingY by remember { mutableFloatStateOf(0f) }
    var turnDir by remember { mutableIntStateOf(0) }
    var turnStartX by remember { mutableFloatStateOf(0f) }
    val turn = remember { androidx.compose.animation.core.Animatable(1f) }
    val reveal = remember { androidx.compose.animation.core.Animatable(1f) }
    var revealPending by remember { mutableStateOf(false) }
    var awaitingPage by remember { mutableStateOf(false) }
    // True only while the image fades onto its blank sheet; read in composition, unlike
    // the fade itself, so the page is not recomposed on every frame of it.
    var revealing by remember { mutableStateOf(false) }
    var pageAspect by remember { mutableFloatStateOf(0.7f) }
    fun leavingX(width: Float): Float =
        if (turnDir == 0) 0f else turnStartX + (-turnDir * width - turnStartX) * turn.value
    fun arrivingX(width: Float): Float =
        if (turnDir == 0) dragX else leavingX(width) + turnDir * width
    // Declared before the reset below so it still reads the old page's zoom.
    LaunchedEffect(position) {
        if (position == shownPosition) return@LaunchedEffect
        val direction = if (position > shownPosition) 1 else -1
        shownPosition = position
        // The page on show stays the one on show until the next can take its place.
        val leaving = shownBmp
        if (reduceMotion || leaving == null || box.width == 0) { dragX = 0f; return@LaunchedEffect }
        outgoing = leaving
        outgoingScale = scale; outgoingX = offsetX; outgoingY = offsetY
        turnStartX = dragX
        dragX = 0f
        turnDir = direction
        revealPending = false
        try {
            turn.snapTo(0f)
            // Not ready yet: the page settles back, a loading bar says why, and the turn
            // waits for the new page rather than sliding in an empty one.
            if (previewBmp == null && pageBmp == null && !pageFail) {
                awaitingPage = true
                val settle = launch {
                    androidx.compose.animation.core.animate(turnStartX, 0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { v, _ -> turnStartX = v }
                }
                androidx.compose.runtime.snapshotFlow { pageBmp != null || pageFail }
                    .first { it }
                settle.cancel()
                awaitingPage = false
            }
            turn.animateTo(1f, androidx.compose.animation.core.tween(300,
                easing = androidx.compose.animation.core.FastOutSlowInEasing))
        } finally { outgoing = null; turnDir = 0; awaitingPage = false }
    }

    LaunchedEffect(path, position, imageId) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val pageContext = LocalContext.current
    val budget = remember { pageBudget(pageContext) }
    var preview by remember(imageUrl) { mutableStateOf(false) }
    var imageError by remember(imageUrl) { mutableStateOf<String?>(null) }
    var retry by remember(imageUrl) { mutableIntStateOf(0) }
    LaunchedEffect(imageUrl, retry) {
        pageBmp = null
        pageFail = false
        imageError = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val page = PageBytes.getOrAwait(imageId.orEmpty())?.let { PageImage(it, false) }
                    ?: fetchGalicianaPage(imageUrl, path, position, imageId.orEmpty())
                        .also { if (!it.preview) PageBytes.put(imageId.orEmpty(), it.bytes) }
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

    LaunchedEffect(previewBmp) {
        val copy = previewBmp ?: return@LaunchedEffect
        if (pageBmp == null) shownBmp = copy
        pageAspect = copy.width.toFloat() / copy.height.coerceAtLeast(1)
        revealPending = false
    }
    LaunchedEffect(pageBmp) {
        val bmp = pageBmp ?: return@LaunchedEffect
        shownBmp = bmp
        pageAspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
        onPageShown(position)
        // Keep a screen-sized copy, so coming back to this page is instant too.
        imageId?.takeIf { PagePreviews.get(it) == null }?.let { id ->
            launch(Dispatchers.Default) { PagePreviews.put(id, screenCopy(bmp.asAndroidBitmap())) }
        }
        if (revealPending && !reduceMotion) {
            revealPending = false
            revealing = true
            try {
                reveal.snapTo(0f)
                reveal.animateTo(1f, androidx.compose.animation.core.tween(220))
            } finally { revealing = false }
        } else { revealPending = false; reveal.snapTo(1f) }
    }
    // A wait shorter than this needs no spinner.
    var slowLoad by remember(imageUrl) { mutableStateOf(false) }
    LaunchedEffect(imageUrl) { delay(450); slowLoad = true }

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
        val bitmap = drawnBmp ?: return@LaunchedEffect
        val target = matches.getOrNull(currentMatch)?.boxes?.firstOrNull() ?: return@LaunchedEffect
        if (box.width == 0 || box.height == 0 || pageW <= 1f || pageH <= 1f) return@LaunchedEffect
        val focused = focusPageMatch(box.width.toFloat(), box.height.toFloat(),
            bitmap.width.toFloat(), bitmap.height.toFloat(), pageW, pageH, target, scale, frameTop, frameBottom)
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
                    var dragging = false
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
                        // The whole page follows the finger; past the first or last page it
                        // only gives a little.
                        if (multi) { if (dragging) { dragging = false; dragX = 0f } }
                        else if (!reduceMotion && turnDir == 0 && startScale <= 1.02f && scale <= 1.02f &&
                            (dragging || (kotlin.math.abs(dx) > viewConfiguration.touchSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy)))) {
                            dragging = true
                            dragX = if (canTurnNow.value(if (dx < 0) 1 else -1)) dx else dx * 0.25f
                        }
                    }
                    // Whole page shown, or zoomed in but already at that side's edge: turn the page.
                    val atEdge = scale > 1.02f && kotlin.math.abs(offsetX - startX) < 2f
                    val turns = !multi && kotlin.math.abs(dx) > size.width * 0.22f && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f &&
                        ((startScale <= 1.02f && scale <= 1.02f) || atEdge)
                    val step = if (dx < 0) 1 else -1
                    if (turns && canTurnNow.value(step)) swipeHandler.value(step)
                    else if (dragX != 0f) turnScope.launch {
                        androidx.compose.animation.core.animate(dragX, 0f,
                            animationSpec = androidx.compose.animation.core.spring(
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { v, _ -> dragX = v }
                    }
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
            drawnBmp == null && !pageFail -> BlankSheet(pageAspect, frameTop, frameBottom, slowLoad) { arrivingX(box.width.toFloat()) }
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
                val bmp = drawnBmp
                if (revealing) BlankSheet(pageAspect, frameTop, frameBottom, false) { arrivingX(box.width.toFloat()) }
                Canvas(Modifier.fillMaxSize().graphicsLayer {
                    translationX = arrivingX(size.width)
                    alpha = reveal.value
                }) {
                    if (bmp == null) return@Canvas
                    val iw = bmp.width.toFloat().coerceAtLeast(1f)
                    val ih = bmp.height.toFloat().coerceAtLeast(1f)
                    val fit = pageFit(size.width, size.height, iw, ih, frameTop, frameBottom)
                    val dw = iw * fit * scale
                    val dh = ih * fit * scale
                    val ox = (size.width - dw) / 2f + offsetX
                    val oy = pageFrameCenter(size.height, frameTop, frameBottom) - dh / 2f + offsetY
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
                                color = gal.galiciana.app.ui.MatchAmberCurrent.copy(alpha = 0.85f * (1f - p)),
                                radius = base + p * 44.dp.toPx(), center = Offset(cx, cy),
                                style = Stroke(width = 3.dp.toPx()),
                            )
                            if (scale < 1.6f) drawCircle(
                                color = gal.galiciana.app.ui.MatchAmberCurrent.copy(alpha = 0.7f),
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
                                    color = if (active) gal.galiciana.app.ui.MatchAmberCurrent.copy(alpha = 0.4f) else gal.galiciana.app.ui.MatchAmber.copy(alpha = 0.25f),
                                    topLeft = Offset(left, top), size = Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                                )
                                drawRoundRect(
                                    color = if (active) gal.galiciana.app.ui.MatchOutlineCurrent else gal.galiciana.app.ui.MatchOutline,
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
        if (awaitingPage) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter)
            .offset { IntOffset(0, (frameTop - 4.dp.toPx()).roundToInt().coerceAtLeast(0)) })
        // While the finger drags, the neighbouring page shows at the edge, attached.
        Canvas(Modifier.fillMaxSize()) {
            val drag = dragX
            if (turnDir != 0 || drag == 0f) return@Canvas
            val side = if (drag < 0f) 1 else -1
            if (!canTurnNow.value(side)) return@Canvas
            val left = drag + side * size.width
            val copy = neighbourImageId(side)?.let(PagePreviews::get)
            val iw = copy?.width?.toFloat() ?: (pageAspect * 1000f)
            val ih = copy?.height?.toFloat() ?: 1000f
            val fit = pageFit(size.width, size.height, iw, ih, frameTop, frameBottom)
            val dw = iw * fit
            val dh = ih * fit
            val top = pageFrameCenter(size.height, frameTop, frameBottom) - dh / 2f
            val x = left + (size.width - dw) / 2f
            if (copy != null) drawImage(copy, dstOffset = IntOffset(x.roundToInt(), top.roundToInt()),
                dstSize = IntSize(dw.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.Low)
            else drawRect(BlankSheetColor, topLeft = Offset(x, top), size = Size(dw, dh))
        }
        outgoing?.let { leavingPage ->
            Canvas(Modifier.fillMaxSize().graphicsLayer { translationX = leavingX(size.width) }) {
                val iw = leavingPage.width.toFloat().coerceAtLeast(1f)
                val ih = leavingPage.height.toFloat().coerceAtLeast(1f)
                val fit = pageFit(size.width, size.height, iw, ih, frameTop, frameBottom)
                val dw = iw * fit * outgoingScale
                val dh = ih * fit * outgoingScale
                drawImage(leavingPage,
                    dstOffset = IntOffset(((size.width - dw) / 2f + outgoingX).roundToInt(),
                        (pageFrameCenter(size.height, frameTop, frameBottom) - dh / 2f + outgoingY).roundToInt()),
                    dstSize = IntSize(dw.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)),
                    filterQuality = FilterQuality.Low)
            }
        }
    }
}

private data class PageImage(val bytes: ByteArray, val preview: Boolean)

// Keep bulk downloads on the same full-image path as the viewer's download button.
internal suspend fun fetchFullGalicianaPage(path: String, position: Int, imageId: String): ByteArray =
    fetchGalicianaPage(GalicianaUrls.jpeg(imageId), path, position, imageId, allowPreview = false).bytes

/** A page-shaped blank sheet in the page's frame, standing in until the image arrives. */
@Composable
private fun BlankSheet(aspect: Float, frameTop: Float, frameBottom: Float, showProgress: Boolean, offsetX: () -> Float) {
    Box(Modifier.fillMaxSize().graphicsLayer { translationX = offsetX() }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val fit = pageFit(size.width, size.height, aspect * 1000f, 1000f, frameTop, frameBottom)
            val w = aspect * 1000f * fit
            val h = 1000f * fit
            drawRect(BlankSheetColor, topLeft = Offset((size.width - w) / 2f,
                pageFrameCenter(size.height, frameTop, frameBottom) - h / 2f), size = Size(w, h))
        }
        if (showProgress) LinearProgressIndicator(Modifier.fillMaxWidth(0.4f))
    }
}

private val BlankSheetColor = Color(0xFF2E2B27)

/**
 * The last few pages' files, as downloaded. Going back is then instant, and pages read
 * ahead are ready when the reader gets there. Files, not decoded images: a few MB each.
 */
internal object PageBytes {
    private val files = object : LinkedHashMap<String, ByteArray>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?) = size > 5
    }
    @Synchronized fun get(imageId: String): ByteArray? = if (imageId.isBlank()) null else files[imageId]
    @Synchronized fun put(imageId: String, bytes: ByteArray) { if (imageId.isNotBlank()) files[imageId] = bytes }
    private val reading = mutableMapOf<String, kotlinx.coroutines.CompletableDeferred<ByteArray?>>()
    @Synchronized fun startReading(imageId: String): kotlinx.coroutines.CompletableDeferred<ByteArray?>? =
        if (imageId in reading || imageId in files) null
        else kotlinx.coroutines.CompletableDeferred<ByteArray?>().also { reading[imageId] = it }
    @Synchronized fun finishReading(imageId: String, bytes: ByteArray?) {
        if (bytes != null) files[imageId] = bytes
        reading.remove(imageId)?.complete(bytes)
    }
    /** The file, if it is at hand or already on its way (then it is awaited, not asked for twice). */
    suspend fun getOrAwait(imageId: String): ByteArray? {
        if (imageId.isBlank()) return null
        val pending = synchronized(this) { files[imageId]?.let { return it }; reading[imageId] }
        return pending?.await()
    }
}

/** Screen-sized copies of the pages around the one being read: about 10 MB each. */
internal object PagePreviews {
    private val copies = object : LinkedHashMap<String, ImageBitmap>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 3
    }
    @Synchronized fun get(imageId: String): ImageBitmap? = if (imageId.isBlank()) null else copies[imageId]
    @Synchronized fun put(imageId: String, copy: ImageBitmap) { if (imageId.isNotBlank()) copies[imageId] = copy }
}

private const val PREVIEW_LONG_SIDE = 2200

private fun screenCopy(source: android.graphics.Bitmap): ImageBitmap {
    val ratio = minOf(1f, PREVIEW_LONG_SIDE.toFloat() / maxOf(source.width, source.height))
    return android.graphics.Bitmap.createScaledBitmap(source,
        (source.width * ratio).roundToInt().coerceAtLeast(1),
        (source.height * ratio).roundToInt().coerceAtLeast(1), true).asImageBitmap()
}

/** Decodes a downloaded page straight to its screen-sized copy, never at full size. */
private fun decodePreview(bytes: ByteArray): ImageBitmap? {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val longSide = maxOf(bounds.outWidth, bounds.outHeight)
    if (longSide <= 0) return null
    var sample = 1
    while (longSide / (sample * 2) >= PREVIEW_LONG_SIDE) sample *= 2
    val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    return if (maxOf(decoded.width, decoded.height) > PREVIEW_LONG_SIDE * 1.2f) screenCopy(decoded).also { decoded.recycle() }
        else decoded.asImageBitmap()
}

/** Downloads a page in the background if it is not already at hand, with its screen copy. */
internal suspend fun readAheadPage(path: String, position: Int, imageId: String) {
    if (imageId.isBlank()) return
    if (PagePreviews.get(imageId) == null) PageBytes.get(imageId)?.let { bytes ->
        withContext(Dispatchers.Default) { decodePreview(bytes) }?.let { PagePreviews.put(imageId, it) }
    }
    PageBytes.startReading(imageId) ?: return
    var bytes: ByteArray? = null
    try {
        bytes = withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            runCatching { fetchGalicianaPage(GalicianaUrls.jpeg(imageId), path, position, imageId, allowPreview = false).bytes }.getOrNull()
        }
    } finally { PageBytes.finishReading(imageId, bytes) }
    bytes?.let { withContext(Dispatchers.Default) { decodePreview(it) } }?.let { PagePreviews.put(imageId, it) }
}

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

