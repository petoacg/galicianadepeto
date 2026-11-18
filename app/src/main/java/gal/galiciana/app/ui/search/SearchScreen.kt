package gal.galiciana.app.ui.search

import androidx.compose.material.icons.outlined.BookmarkAdd
import gal.galiciana.app.i18n.tr
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import gal.galiciana.app.ui.MatteShape
import gal.galiciana.app.ui.matteFieldColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import gal.galiciana.app.data.LocalStore
import gal.galiciana.app.data.favKey
import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.ui.ClearableField
import gal.galiciana.app.ui.NoteFlow
import gal.galiciana.app.ui.VieiraButton
import gal.galiciana.app.ui.highlightQuery
import gal.galiciana.app.ui.highlightPossibleTerms

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onOpenHit: (String, Int, String, String?) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    seedQuery: String = "",
    seedTitle: String = "",
    seedFilters: Boolean = false,
    seedPreset: gal.galiciana.app.data.SearchPreset? = null,
    onHistory: () -> Unit = {},
    listState: LazyListState,
    vm: SearchViewModel = viewModel(),
) {
    val androidContext = LocalContext.current
    val store = remember { LocalStore(androidContext) }
    val history = remember { gal.galiciana.app.data.SearchHistory(androidContext) }
    var searchNotice by remember { mutableStateOf<String?>(null) }
    fun preset() = gal.galiciana.app.data.SearchPreset(vm.query, vm.title, vm.author, vm.place,
        vm.yearFrom, vm.yearTo, vm.section, vm.advanced, vm.fullText)
    fun runSearch() {
        if (vm.canSearch) {
            if (!history.remember(preset())) searchNotice = tr("Non se puido gardar esta consulta en Recentes.", "No se pudo guardar esta consulta en Recientes.")
            vm.search()
        }
    }
    val scope = rememberCoroutineScope()
    var handledSearchGeneration by rememberSaveable { mutableStateOf(vm.searchGeneration) }
    var handledCompletedGeneration by rememberSaveable { mutableStateOf(vm.completedSearchGeneration) }
    var handledRecoveryScroll by rememberSaveable { mutableStateOf(vm.recoverySearched) }
    LaunchedEffect(vm.searchGeneration) {
        if (vm.searchGeneration != handledSearchGeneration) {
            handledSearchGeneration = vm.searchGeneration
            if (vm.searchGeneration > 0) listState.scrollToItem(0)
        }
    }
    LaunchedEffect(vm.completedSearchGeneration) {
        if (vm.completedSearchGeneration != handledCompletedGeneration) {
            handledCompletedGeneration = vm.completedSearchGeneration
            if (vm.completedSearchGeneration > 0) listState.scrollToItem(0)
        }
    }
    LaunchedEffect(vm.recoverySearched) {
        if (vm.recoverySearched && !handledRecoveryScroll) {
            val precise = vm.groups.sumOf { 1 + it.pages.size }
            val more = if (vm.pendingUrls.isNotEmpty()) 1 else 0
            val variants = if (vm.variantGroups.isNotEmpty())
                1 + vm.variantGroups.sumOf { 1 + it.pages.size } +
                    (if (vm.variantScanIncomplete) 1 else 0)
            else 0
            listState.scrollToItem(1 + precise + more + variants)
        }
        handledRecoveryScroll = vm.recoverySearched
    }
    LaunchedEffect(seedQuery, seedTitle, seedFilters, seedPreset) {
        val restored = if (seedPreset == null && seedQuery.isBlank() && seedTitle.isBlank() && vm.searchGeneration == 0) history.draft() else null
        if (restored != null) {
            vm.query = restored.query; vm.title = restored.title; vm.author = restored.author; vm.place = restored.place
            vm.yearFrom = restored.yearFrom; vm.yearTo = restored.yearTo; vm.section = restored.section
            vm.advanced = restored.advanced || seedFilters; vm.fullText = restored.fullText
        }
        if (seedPreset != null && vm.searchGeneration == 0) {
            vm.query = seedPreset.query; vm.title = seedPreset.title; vm.author = seedPreset.author
            vm.place = seedPreset.place; vm.yearFrom = seedPreset.yearFrom; vm.yearTo = seedPreset.yearTo
            vm.section = seedPreset.section; vm.advanced = seedPreset.advanced; vm.fullText = seedPreset.fullText
            runSearch()
        }
        if (seedFilters && vm.searchGeneration == 0) vm.advanced = true
        if (seedQuery.isNotBlank() && vm.query.isBlank()) vm.query = seedQuery
        if (seedTitle.isNotBlank()) {
            if (vm.title.isBlank()) vm.title = seedTitle
            vm.advanced = true
        }
        if (seedQuery.isNotBlank() && vm.searchGeneration == 0) runSearch()
    }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { preset().criteriaKey() }.collect { history.saveDraft(preset()) }
    }
    // A search typed on Inicio opens straight into results: Back returns to Inicio. The
    // form becomes a step of its own only once the user has opened it.
    var formVisited by rememberSaveable { mutableStateOf(seedQuery.isBlank() && seedPreset == null) }
    var resultsShown by remember { mutableStateOf(false) }
    LaunchedEffect(vm.showingResults) {
        if (vm.showingResults) resultsShown = true else if (resultsShown) formVisited = true
    }
    val hints = remember { androidContext.getSharedPreferences("ui_hints", android.content.Context.MODE_PRIVATE) }
    var actionsHintSeen by remember { mutableStateOf(hints.getBoolean("result_actions", false)) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showSearchHelp by rememberSaveable { mutableStateOf(false) }
    var publicationFilters by rememberSaveable { mutableStateOf(seedPreset?.let { it.title.isNotBlank() || it.author.isNotBlank() || it.place.isNotBlank() } ?: (seedTitle.isNotBlank() || history.draft()?.let { it.title.isNotBlank() || it.author.isNotBlank() || it.place.isNotBlank() } == true)) }
    var periodFilters by rememberSaveable { mutableStateOf(seedPreset?.let { it.yearFrom.isNotBlank() || it.yearTo.isNotBlank() } ?: (history.draft()?.let { it.yearFrom.isNotBlank() || it.yearTo.isNotBlank() } == true)) }
    var showResultDetails by rememberSaveable { mutableStateOf(false) }
    // Picking several result pages to save them together, as Gardados already allows for removal.
    var pickingResults by remember { mutableStateOf(false) }
    var pickedResults by remember { mutableStateOf<Map<String, OcrHit>>(emptyMap()) }
    var bulkSavedNotice by remember { mutableStateOf<String?>(null) }
    fun pickKey(hit: OcrHit) = "${hit.path}|${hit.position ?: 1}"
    fun togglePick(hit: OcrHit) {
        val key = pickKey(hit)
        pickedResults = if (key in pickedResults) pickedResults - key else pickedResults + (key to hit)
    }
    // Filtros opened from results is a step on top of them: Back returns to the results.
    var formFromResults by rememberSaveable { mutableStateOf(false) }
    // Undoing Filtros also undoes its mark on the history: the results then go back to
    // wherever they came from, not to the form that was just closed.
    var formVisitedBeforeFilters by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.showingResults) { if (vm.showingResults) formFromResults = false }
    // Back always undoes the latest step: a selection first, then the form or the
    // results the user came from, and only then leaves the screen.
    val hasInnerStep = pickingResults ||
        (vm.showingResults && formVisited) || (!vm.showingResults && formFromResults)
    fun stepBack() {
        // The "saved" notice is a message, not a step: it goes away with whatever Back does.
        bulkSavedNotice = null
        when {
            pickingResults -> { pickingResults = false; pickedResults = emptyMap() }
            !vm.showingResults && formFromResults -> {
                formFromResults = false
                formVisited = formVisitedBeforeFilters
                vm.showingResults = true
            }
            vm.showingResults && formVisited -> vm.showingResults = false
            else -> onBack()
        }
    }
    BackHandler(enabled = hasInnerStep) { stepBack() }
    val qTerm = vm.submittedQuery
    // Opening a page also hands the viewer the other pages of that document that these
    // results already found, so it can list them without downloading the document.
    // Only exact results count: pages found through a spelling variant or the approximate
    // search would be listed as holding the query when the viewer cannot mark it there.
    val openHitWithPages: (String, Int, String, String?) -> Unit = { p, pos, q, img ->
        val known = vm.hits.filter { it.path == p }.mapNotNull { it.position }.toSet() + pos
        gal.galiciana.app.ui.viewer.SearchPageHints.remember(p, q, known)
        onOpenHit(p, pos, q, img)
    }
    val viewerQuery = qTerm
    val barra = MaterialTheme.colorScheme.primary
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            gal.galiciana.app.ui.SectionBar(
                title = when {
                    vm.showingResults -> "Resultados"
                    else -> "Buscar"
                },
                color = barra,
                onBack = { stepBack() },
                onHome = onHome,
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .imePadding()
        ) {
        if (vm.showingResults) {
            LazyColumn(Modifier.weight(1f), state = listState) {
            item(key = "search-controls") {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClearableField(value = vm.query, onValueChange = { vm.query = it },
                        placeholder = tr("Modificar busca", "Modificar búsqueda"), modifier = Modifier.weight(1f), onSearch = { runSearch() })
                    IconButton(onClick = { runSearch() }, enabled = vm.canSearch && !vm.loading) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar con esta consulta",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                // A search in flight or a failed one is not "0 obras": counts appear only
                // once Galiciana has answered.
                val failed = vm.error != null && vm.error != "Sin resultados con esos criterios"
                val status = when {
                    vm.hits.isEmpty() && vm.loading -> "Buscando en Galiciana…"
                    vm.hits.isEmpty() && failed -> null
                    else -> {
                        // "por agora" says more pages exist without exposing how Galiciana pages its results.
                        val more = vm.pendingUrls.isNotEmpty()
                        tr("${vm.groups.size} ${if (vm.groups.size == 1) "obra" else "obras"} · ${vm.hits.size} ${if (vm.hits.size == 1) "páxina" else "páxinas"}${if (more) " por agora" else ""}",
                            "${vm.groups.size} ${if (vm.groups.size == 1) "obra" else "obras"} · ${vm.hits.size} ${if (vm.hits.size == 1) "página" else "páginas"}${if (more) " por ahora" else ""}")
                    }
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }) }
                if (vm.submittedQuery.isNotBlank() && vm.query != vm.submittedQuery) Text(
                    "Consulta aplicada: ${vm.submittedQuery}", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { formVisitedBeforeFilters = formVisited; formFromResults = true; vm.showingResults = false; vm.advanced = true }) {
                        Icon(Icons.Filled.Tune, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Filtros")
                    }
                    Box {
                        TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { sortMenuOpen = true }, enabled = !vm.loading) {
                            val label = when (vm.sortBy) {
                                SearchViewModel.SortBy.Titulo -> "Título"
                                SearchViewModel.SortBy.Autor -> "Autor"
                                SearchViewModel.SortBy.Fecha -> tr("Data", "Fecha")
                                SearchViewModel.SortBy.Relevancia -> "Relevancia"
                            }
                            Text(tr("Orde: $label", "Orden: $label"))
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            listOf(SearchViewModel.SortBy.Relevancia to "Relevancia",
                                SearchViewModel.SortBy.Titulo to "Título", SearchViewModel.SortBy.Autor to "Autor",
                                SearchViewModel.SortBy.Fecha to tr("Data", "Fecha")).forEach { (key, label) ->
                                gal.galiciana.app.ui.SortMenuItem(label, active = vm.sortBy == key,
                                    detail = if (key == SearchViewModel.SortBy.Relevancia) null
                                        else if (vm.sortAsc) "ascendente" else "descendente",
                                    onClick = { sortMenuOpen = false; vm.setSort(key)
                                        scope.launch { listState.scrollToItem(0) } })
                            }
                        }
                    }
                    TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { showResultDetails = !showResultDetails }) {
                        Text(if (showResultDetails) "Menos" else "Detalles")
                    }
                    if (vm.hits.isNotEmpty()) TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
                        pickingResults = !pickingResults
                        pickedResults = emptyMap()
                        bulkSavedNotice = null
                    }) { Text(if (pickingResults) "Cancelar" else "Seleccionar") }
                }
                if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                vm.error?.takeUnless { it == "Sin resultados con esos criterios" && vm.variantLoading }
                    ?.let { message ->
                        val emptyResult = message == "Sin resultados con esos criterios"
                        Text(if (emptyResult) tr("Non atopamos páxinas con eses criterios. Proba outra palabra ou amplía os filtros.", "No encontramos páginas con esos criterios. Prueba otra palabra o amplía los filtros.") else message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (emptyResult) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite })
                        // The query and filters stay as typed; retrying repeats the step that failed.
                        if (!emptyResult && !vm.loading) OutlinedButton(
                            onClick = { when {
                                vm.recoveryFailed -> vm.loadRecovery()
                                vm.hits.isNotEmpty() && vm.pendingUrls.isNotEmpty() -> vm.loadMore()
                                else -> runSearch()
                            } },
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).heightIn(min = 48.dp),
                        ) { Text("Reintentar") }
                    }
                // Scope notes live under Detalles; only a date order over partial results needs saying up front.
                if (vm.hits.isNotEmpty() && vm.sortBy == SearchViewModel.SortBy.Fecha && vm.pendingUrls.isNotEmpty() && !vm.globalSortAvailable)
                    Text(tr("Orde por data só entre as páxinas mostradas; carga máis resultados para completala.",
                        "Orden por fecha solo entre las páginas mostradas; carga más resultados para completarlo."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (vm.submittedSection.isNotBlank()) Text("Tema orientativo: ${vm.submittedSection}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (showResultDetails) {
                    Text(tr("${vm.groups.sumOf { it.occurrenceCount }} extractos", "${vm.groups.sumOf { it.occurrenceCount }} extractos") +
                        (vm.totalWorks?.let { tr(" · $it obras en total en Galiciana", " · $it obras en total en Galiciana") } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (vm.pendingUrls.isNotEmpty()) Text(
                        if (vm.globalSortAvailable) tr("Orde aplicada ao total de Galiciana; móstranse as primeiras páxinas.", "Orden aplicado al total de Galiciana; se muestran las primeras páginas.")
                        else tr("A orde aplícase ás páxinas mostradas.", "El orden se aplica a las páginas mostradas."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (vm.sortBy == SearchViewModel.SortBy.Relevancia) Text(tr("A proximidade calcúlase entre as páxinas mostradas; cargar máis pode cambiar a orde.", "La cercanía se calcula entre las páginas mostradas; cargar más puede cambiar el orden."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tr("As variantes de OCR e as coincidencias máis amplas aparecen en grupos separados.", "Las variantes de OCR y las coincidencias más amplias aparecen en grupos separados."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (vm.error == null && !vm.loading && !vm.variantLoading && vm.groups.isEmpty() && vm.variantGroups.isEmpty() && vm.recoveryGroups.isEmpty())
                    Text(tr("Proba outra palabra ou amplía os filtros.", "Prueba otra palabra o amplía los filtros."), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                // After a real failure Reintentar is the one way forward; a broader search is offered
                // only when Galiciana answered.
                if (vm.submittedQuery.isNotBlank() && !vm.loading && !vm.variantLoading &&
                    vm.completedSearchGeneration == vm.searchGeneration &&
                    (vm.error == null || vm.error == "Sin resultados con esos criterios") &&
                    (showResultDetails || (vm.groups.isEmpty() && vm.variantGroups.isEmpty()))) {
                    TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { vm.loadRecovery() }, enabled = !vm.recoveryLoading) {
                        Text(if (vm.recoveryLoading) "Buscando coincidencias…" else tr("Buscar coincidencias máis amplas", "Buscar coincidencias más amplias"))
                    }
                }
                if (vm.variantLoading) Text(tr("Comprobando outras grafías no OCR…", "Comprobando otras grafías en el OCR…"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            }
                // Name the first tier only when other tiers follow, so the three read as a set.
                if (vm.groups.isNotEmpty() && (vm.variantGroups.isNotEmpty() || vm.recoveryGroups.isNotEmpty())) item(key = "exact-heading") {
                    Text(tr("Coincidencias exactas", "Coincidencias exactas"), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() })
                    Text(tr("Páxinas onde o OCR recoñece a túa busca tal como a escribiches.",
                        "Páginas donde el OCR reconoce tu búsqueda tal como la escribiste."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (vm.hits.isNotEmpty() && !actionsHintSeen) item(key = "actions-hint") {
                    // Shown once: the two row icons carry the app's core actions and have no text.
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                            Text(tr("A vieira garda a páxina en Gardados.", "La vieira guarda la página en Guardados."), style = MaterialTheme.typography.bodyMedium)
                            Text(tr("A icona de nota abre a súa Nota de referencia.", "El icono de nota abre su Nota de referencia."), style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { hints.edit().putBoolean("result_actions", true).apply(); actionsHintSeen = true }) {
                            Text("Entendido")
                        }
                    }
                }
                vm.groups.forEach { group ->
                    item(key = "h-${group.pages.firstOrNull()?.path}-${group.pages.firstOrNull()?.position}-${group.title.hashCode()}") {
                        WorkHead(
                            title = group.title.ifBlank { group.parentTitle.orEmpty() }.ifBlank { group.pages.firstOrNull()?.title.orEmpty() },
                            parent = group.parentTitle,
                            pages = group.pages.size,
                            hits = group.occurrenceCount,
                            visited = store.visited().any { it.startsWith("${group.pages.firstOrNull()?.path}|") },
                            onOpen = {
                                val first = group.pages.firstOrNull()
                                first?.path?.let { p ->
                                    store.rememberOpenedHit(first.copy(savedQuery = viewerQuery))
                                    store.markVisited(favKey(p, first.position ?: 1, group.title))
                                    store.rememberWork(p, group.title, group.parentTitle)
                                    openHitWithPages(p, first.position ?: 1, viewerQuery, first.imageId)
                                }
                            },
                        )
                    }
                    items(group.pages, key = { "p-${it.path}-${it.imageId}-${it.position}-${it.title.hashCode()}" }) { hit ->
                        ResultHitRow(
                            hit = hit,
                            query = qTerm,
                            approximate = false,
                            visited = store.visited().contains(favKey(hit.path.orEmpty(), hit.position ?: 1, hit.title)),
selecting = pickingResults, selected = pickKey(hit) in pickedResults, onSelectToggle = { togglePick(hit) },
                            onClick = { if (pickingResults) togglePick(hit) else {
                                hit.path?.let { p ->
                                    store.markVisited(favKey(p, hit.position ?: 1, hit.title))
                                    store.rememberWork(p, hit.title, hit.parentTitle)
                                    openHitWithPages(p, hit.position ?: 1, viewerQuery, hit.imageId)
                                }
                            } },
                        )
                        /* slot */
                    }
                }
                if (vm.pendingUrls.isNotEmpty()) item(key = "load-more") {
                    Button(
                        onClick = { vm.loadMore() },
                        enabled = !vm.loading,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    ) { Text(tr("Cargar máis resultados", "Cargar más resultados")) }
                }
                if (vm.variantGroups.isNotEmpty()) {
                    item(key = "ocr-variants-heading") {
                        Text(tr("Mesma frase con erros de OCR · ${vm.variantHits.size} páxinas", "Misma frase con errores de OCR · ${vm.variantHits.size} páginas"),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                        Text(tr("A frase aparece completa, pero o OCR leu mal algunha letra. Comproba a palabra na imaxe.",
                            "La frase aparece completa, pero el OCR leyó mal alguna letra. Comprueba la palabra en la imagen."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    vm.variantGroups.forEach { group ->
                        item(key = "v-h-${group.pages.firstOrNull()?.path}-${group.title.hashCode()}") {
                            WorkHead(title = group.title, parent = group.parentTitle,
                                pages = group.pages.size, hits = group.occurrenceCount,
                                visited = store.visited().any { it.startsWith("${group.pages.firstOrNull()?.path}|") },
                                onOpen = {
                                    group.pages.firstOrNull()?.let { hit ->
                                        store.rememberOpenedHit(hit.copy(savedQuery = viewerQuery))
                                        hit.path?.let { openHitWithPages(it, hit.position ?: 1, viewerQuery, hit.imageId) }
                                    }
                                })
                        }
                        items(group.pages, key = { "v-p-${it.path}-${it.imageId}-${it.position}" }) { hit ->
                            ResultHitRow(hit = hit, query = qTerm, approximate = true,
                                visited = store.visited().contains(favKey(hit.path.orEmpty(), hit.position ?: 1, hit.title)),
selecting = pickingResults, selected = pickKey(hit) in pickedResults, onSelectToggle = { togglePick(hit) },
                                onClick = { if (pickingResults) togglePick(hit) else {
                                    hit.path?.let {
                                        store.markVisited(favKey(it, hit.position ?: 1, hit.title))
                                        openHitWithPages(it, hit.position ?: 1, viewerQuery, hit.imageId)
                                    }
                                } })
                        }
                    }
                    if (vm.variantScanIncomplete) item(key = "variant-scan-limit") {
                        Text(tr("Comprobáronse os primeiros candidatos de Galiciana; pode haber máis variantes OCR.", "Se han comprobado los primeros candidatos de Galiciana; puede haber más variantes OCR."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (vm.recoveryGroups.isNotEmpty()) {
                    item(key = "recovery-heading") {
                        Text(tr("Coincidencias máis amplas · ${vm.recoveryHits.size} páxinas (máximo ${vm.recoveryLimit})", "Coincidencias más amplias · ${vm.recoveryHits.size} páginas (máximo ${vm.recoveryLimit})"),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        Text(tr("Candidatos máis flexibles: poden faltar palabras da túa consulta. Comproba o texto de cada páxina. Conservan os filtros de título, autor, lugar e anos; non limitan as palabras de sección.", "Candidatos más flexibles: pueden faltar palabras de tu consulta. Comprueba el texto de cada página. Conservan los filtros de título, autor, lugar y años; no limitan las palabras de sección."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    vm.recoveryGroups.forEach { group ->
                        item(key = "r-h-${group.pages.firstOrNull()?.path}-${group.title.hashCode()}") {
                            WorkHead(title = group.title, parent = group.parentTitle,
                                pages = group.pages.size, hits = group.occurrenceCount,
                                visited = store.visited().any { it.startsWith("${group.pages.firstOrNull()?.path}|") },
                                onOpen = {
                                    group.pages.firstOrNull()?.let { hit ->
                                        store.rememberOpenedHit(hit.copy(savedQuery = viewerQuery))
                                        hit.path?.let { openHitWithPages(it, hit.position ?: 1, viewerQuery, hit.imageId) }
                                    }
                                })
                        }
                        items(group.pages, key = { "r-p-${it.path}-${it.imageId}-${it.position}" }) { hit ->
                            ResultHitRow(hit = hit, query = qTerm, approximate = true,
                                visited = store.visited().contains(favKey(hit.path.orEmpty(), hit.position ?: 1, hit.title)),
selecting = pickingResults, selected = pickKey(hit) in pickedResults, onSelectToggle = { togglePick(hit) },
                                onClick = { if (pickingResults) togglePick(hit) else {
                                    hit.path?.let {
                                        store.markVisited(favKey(it, hit.position ?: 1, hit.title))
                                        openHitWithPages(it, hit.position ?: 1, viewerQuery, hit.imageId)
                                    }
                                } })
                        }
                    }
                    if (vm.recoveryIncomplete) item(key = "recovery-limit") {
                        Text(tr("Hai máis candidatos en Galiciana; a busca ampliada detívose no límite indicado.", "Hay más candidatos en Galiciana; la búsqueda ampliada se ha detenido en el límite indicado."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (vm.recoverySearched && vm.recoveryGroups.isEmpty()) item(key = "recovery-empty") {
                    Text(tr("Non se atoparon candidatos adicionais na busca ampliada.", "No se encontraron candidatos adicionales en la búsqueda ampliada."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            // Action bar for picked results, at the thumb's reach below the list.
            if (pickingResults || bulkSavedNotice != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(bulkSavedNotice ?: if (pickedResults.isEmpty()) tr("Toca as páxinas que queres gardar", "Toca las páginas que quieres guardar")
                        else tr("${pickedResults.size} ${if (pickedResults.size == 1) "páxina seleccionada" else "páxinas seleccionadas"}",
                            "${pickedResults.size} ${if (pickedResults.size == 1) "página seleccionada" else "páginas seleccionadas"}"),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite })
                    if (bulkSavedNotice != null) TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { bulkSavedNotice = null }) { Text("Entendido") }
                    else Button(enabled = pickedResults.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp), onClick = {
                        var added = 0
                        pickedResults.values.forEach { hit ->
                            if (hit.path != null && !store.isFav(hit.path, hit.position)) {
                                store.rememberWork(hit.path, hit.title, hit.parentTitle)
                                if (store.toggleHit(hit.copy(savedQuery = qTerm))) added++
                            }
                        }
                        val already = pickedResults.size - added
                        bulkSavedNotice = tr("$added ${if (added == 1) "páxina gardada" else "páxinas gardadas"} en Gardados", "$added ${if (added == 1) "página guardada" else "páginas guardadas"} en Guardados") +
                            if (already > 0) tr(" · $already xa estaban", " · $already ya estaban") else ""
                        pickingResults = false
                        pickedResults = emptyMap()
                    }) { Text(tr("Gardar", "Guardar")) }
                }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = onHistory) { Text(tr("As túas buscas", "Tus búsquedas"), maxLines = 1) }
                TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
                    searchNotice = if (history.save(preset())) tr("Busca gardada cos seus filtros.", "Búsqueda guardada con sus filtros.") else tr("Non se puido gardar a busca.", "No se pudo guardar la búsqueda.")
                }, enabled = vm.canSearch) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.BookmarkAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Gardar busca", "Guardar búsqueda"), maxLines = 1)
                }
            }
            ClearableField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = tr("Palabras, persoas ou lugares", "Palabras, personas o lugares"),
                label = tr("A túa busca", "Tu búsqueda"),
                imeSearch = true,
                onSearch = { runSearch() },
            )
            // The form is the filtered search itself: reaching it means filters apply,
            // so there is no on/off switch to find first.
            LaunchedEffect(Unit) { vm.advanced = true }
            Spacer(Modifier.height(12.dp))
            Text(tr("Buscar en", "Buscar en"), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                gal.galiciana.app.ui.SelectChip(selected = vm.fullText, onClick = { vm.fullText = true },
                    label = { Text(tr("Texto das páxinas", "Texto de las páginas")) }, modifier = Modifier.heightIn(min = 48.dp))
                gal.galiciana.app.ui.SelectChip(selected = !vm.fullText, onClick = { vm.fullText = false },
                    label = { Text(tr("Catálogo", "Catálogo")) }, modifier = Modifier.heightIn(min = 48.dp))
            }
            Text(if (vm.fullText) tr("Persoas, lugares e noticias dentro dos documentos dixitalizados.", "Personas, lugares y noticias dentro de los documentos digitalizados.")
                else tr("Títulos, autores e datos das fichas; móstranse documentos con páxinas dixitalizadas.", "Títulos, autores y datos de las fichas; se muestran documentos con páginas digitalizadas."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { showSearchHelp = !showSearchHelp }) {
                Text(if (showSearchHelp) tr("Ocultar axuda", "Ocultar ayuda") else tr("Como afinar a busca", "Cómo afinar la búsqueda"))
            }
            if (showSearchHelp) Text(tr("Sen comiñas búscanse todas as palabras, aínda que estean separadas e noutra orde. Con comiñas búscase unha expresión seguida. Se o OCR omite algunha, proba «Buscar coincidencias máis amplas». ? substitúe unha letra e * unha terminación.", "Sin comillas se buscan todas las palabras, aunque estén separadas y en otro orden. Con comillas se busca una expresión seguida. Si el OCR omite alguna, prueba «Buscar coincidencias más amplias». ? sustituye una letra y * una terminación."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (vm.advanced) {
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Tema orientativo", "Tema orientativo"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(tr("Engade palabras á consulta. Non identifica automaticamente as seccións do xornal.", "Añade palabras a la consulta. No identifica automáticamente las secciones del periódico."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SearchViewModel.SECTIONS) { name ->
                        FilterChip(
                            selected = vm.advanced && vm.section == name,
                            onClick = {
                                if (vm.advanced) vm.section = if (vm.section == name) "" else name
                            },
                            enabled = vm.advanced,
                            label = { Text(name) },
                            modifier = Modifier.padding(end = 6.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            shape = RoundedCornerShape(6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (vm.advanced) {
            TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { publicationFilters = !publicationFilters }) {
                Text(if (publicationFilters) tr("Ocultar publicación, autor e lugar", "Ocultar publicación, autor y lugar") else tr("Publicación, autor e lugar", "Publicación, autor y lugar"))
            }
            if (publicationFilters) {
            ClearableField(vm.title, { vm.title = it }, placeholder = "", label = "Título", enabled = vm.advanced, imeSearch = false)
            Spacer(Modifier.height(12.dp))
            ClearableField(vm.author, { vm.author = it }, placeholder = "", label = "Autor", enabled = vm.advanced, imeSearch = false)
            Spacer(Modifier.height(12.dp))
            ClearableField(vm.place, { vm.place = it }, placeholder = "", label = tr("Lugar de edición/impresión", "Lugar edición/impresión"), enabled = vm.advanced, imeSearch = false)
            Spacer(Modifier.height(12.dp))
            }
            TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { periodFilters = !periodFilters }) {
                Text(if (periodFilters) tr("Ocultar período", "Ocultar periodo") else tr("Período de publicación", "Periodo de publicación"))
            }
            if (periodFilters) Row {
                ClearableField(vm.yearFrom, { vm.yearFrom = it }, placeholder = "", label = tr("Ano desde", "Año desde"), modifier = Modifier.weight(1f), enabled = vm.advanced, imeSearch = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                Spacer(Modifier.width(12.dp))
                ClearableField(vm.yearTo, { vm.yearTo = it }, placeholder = "", label = tr("Ano ata", "Año hasta"), modifier = Modifier.weight(1f), enabled = vm.advanced, imeSearch = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            }
            }
            Spacer(Modifier.height(16.dp))
            } // Form scrolls independently from its primary action.
            // Only filters hidden in a closed section need repeating above the button.
            val activeFilters = preset().summary().split(" · ").drop(1)
            if (activeFilters.isNotEmpty()) Text(tr("Filtros activos: ", "Filtros activos: ") + activeFilters.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp))
            searchNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            vm.yearError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { runSearch() }, enabled = vm.canSearch && !vm.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Buscar") }
            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        }
    }
}

@Composable
fun WorkHead(
    title: String,
    parent: String?,
    pages: Int,
    hits: Int,
    visited: Boolean,
    onOpen: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp)
        .clickable(role = Role.Button, onClick = onOpen).padding(vertical = 8.dp)) {
        Text(title.ifBlank { tr("Título descoñecido", "Título desconocido") },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (visited) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() })
        parent?.trim()?.takeIf { it.isNotBlank() && !it.equals(title.trim(), ignoreCase = true) }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        Text(tr("$pages ${if (pages == 1) "páxina" else "páxinas"}", "$pages ${if (pages == 1) "página" else "páginas"}") +
            (if (hits > 0) " · $hits ${if (hits == 1) "extracto" else "extractos"}" else ""),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun ResultHitRow(
    hit: OcrHit,
    query: String,
    approximate: Boolean = false,
    visited: Boolean = false,
    onClick: () -> Unit,
    onFavChanged: () -> Unit = {},
    confirmUnfav: Boolean = false,
    selecting: Boolean = false,
    selected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null,
    showNoteText: Boolean = false,
    recoverMissingOcr: Boolean = false,
    showSavedSearch: Boolean = false,
) {
    val androidContext = LocalContext.current
    val store = remember { LocalStore(androidContext) }
    var fav by remember(hit.path, hit.position, selecting) { mutableStateOf(store.isFav(hit.path, hit.position)) }
    var ask by remember { mutableStateOf(false) }
    var pickFolder by remember { mutableStateOf(false) }
    val markQ = query.ifBlank { hit.savedQuery }
    val orderedSnippets = remember(hit.snippets, markQ) {
        hit.snippets.sortedByDescending { pageRelevance(hit.copy(snippets = listOf(it)), markQ) }
    }
    var excerptsExpanded by rememberSaveable(hit.path, hit.position, markQ) { mutableStateOf(false) }
    var noteExpanded by rememberSaveable(hit.path, hit.position) { mutableStateOf(false) }
    var noteOverflow by remember(hit.path, hit.position) { mutableStateOf(false) }
    var excerptsOverflow by remember(hit.path, hit.position, markQ) { mutableStateOf(false) }
    var recoveringOcr by remember(hit.path, hit.position) { mutableStateOf(false) }
    var attemptedOcr by remember(hit.path, hit.position) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(hit.path, hit.position, recoverMissingOcr, hit.snippets.isEmpty()) {
        val path = hit.path
        val position = hit.position
        val originalQuery = hit.savedQuery
        if (recoverMissingOcr && hit.snippets.isEmpty() && originalQuery.isNotBlank() && !attemptedOcr && path != null && position != null) {
            attemptedOcr = true
            recoveringOcr = true
            try {
                val ocr = gal.galiciana.app.data.PageDownloads.ocr(path, position)
                val normalized = ocr.replace(Regex("\\s+"), " ").trim()
                if (normalized.isNotEmpty()) {
                    val phrase = originalQuery.trim().trim('"').replace(Regex("\\s+"), " ")
                    val at = normalized.indexOf(phrase, ignoreCase = true).takeIf { it >= 0 }
                    if (at != null) {
                        val start = (at - 65).coerceAtLeast(0)
                        if (store.fillMissingSnippet(path, position, normalized.drop(start).take(320))) onFavChanged()
                    }
                }
            } finally {
                recoveringOcr = false
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)
        .clickable(enabled = hit.path != null, role = Role.Button) {
            if (!selecting) store.rememberOpenedHit(if (showSavedSearch) hit else hit.copy(savedQuery = markQ))
            onClick()
        }.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(model = hit.thumbUrl, contentDescription = null,
                modifier = Modifier.size(width = 60.dp, height = 84.dp)
                    .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (showSavedSearch && hit.savedQuery.isNotBlank()) Text(
                    if (hit.savedQuery.isNotBlank()) tr("Busca: ${hit.savedQuery}", "Búsqueda: ${hit.savedQuery}") else tr("Busca orixinal non rexistrada", "Búsqueda original no registrada"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                val noteTxt = store.noteOf(hit.path, hit.position).ifBlank { hit.note }
                if (showNoteText && noteTxt.isNotBlank()) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text("Nota de referencia", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(noteTxt, style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (noteExpanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!noteExpanded && it.hasVisualOverflow) noteOverflow = true },
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                        if (noteOverflow || noteExpanded) TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { noteExpanded = !noteExpanded }) {
                            Text(if (noteExpanded) tr("Mostrar menos", "Mostrar menos") else tr("Ler nota completa", "Leer nota completa"))
                        }
                    }
                }
                // A saved page without its excerpt simply shows its search and page; saying
                // what is missing on every older save read as a fault.
                if (hit.snippets.isEmpty()) {
                    if (recoveringOcr) Text(tr("Recuperando o fragmento orixinal…", "Recuperando el fragmento original…"),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else if (!showSavedSearch) Text(tr("Sen extracto de texto dispoñible", "Sin extracto de texto disponible"),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else {
                    // With a Nota de referencia the user's words lead; the OCR excerpt steps back.
                    val noted = showNoteText && noteTxt.isNotBlank()
                    (if (excerptsExpanded) orderedSnippets else orderedSnippets.take(if (noted) 1 else 2)).forEach { line ->
                        Text(if (approximate) highlightPossibleTerms("“$line”", markQ) else highlightQuery("“$line”", markQ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (excerptsExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!excerptsExpanded && it.hasVisualOverflow) excerptsOverflow = true },
                            color = if (noted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp))
                    }
                    if (hit.snippets.size > (if (noted) 1 else 2) || excerptsOverflow) TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { excerptsExpanded = !excerptsExpanded }) {
                        Text(if (excerptsExpanded) "Mostrar menos" else if (hit.snippets.size > 1)
                            "Ver ${hit.snippets.size} extractos" else tr("Ler extracto completo", "Leer extracto completo"))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text((if (hit.position != null) tr("Páxina ${hit.position}", "Página ${hit.position}") else tr("Abrir páxina", "Abrir página")) + if (visited) tr(" · Xa aberta", " · Ya abierta") else "",
                style = MaterialTheme.typography.labelLarge,
                color = if (visited) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f))
            if (selecting) Checkbox(checked = selected, onCheckedChange = { onSelectToggle?.invoke() })
            else {
                VieiraButton(selected = fav, size = 22.dp, onClick = {
                    if (fav && confirmUnfav) ask = true
                    else {
                        val adding = !fav
                        if (adding) store.rememberWork(hit.path.orEmpty(), hit.title, hit.parentTitle)
                        fav = store.toggleHit(hit.copy(savedQuery = markQ))
                        onFavChanged()
                        // Saving is one act; the folder icon that appears beside the vieira files it.
                    }
                })
                NoteFlow(store, hit, isFav = fav, size = 22.dp, onChanged = {
                    fav = store.isFav(hit.path, hit.position)
                    onFavChanged()
                })
                if (fav) {
                    val inFolders = store.foldersOf(hit.path, hit.position).isNotEmpty()
                    IconButton(onClick = { pickFolder = true }) {
                        Icon(if (inFolders) Icons.Filled.Folder else Icons.Outlined.Folder,
                            contentDescription = tr("Organizar páxina en cartafoles", "Organizar página en carpetas"),
                            tint = gal.galiciana.app.ui.theme.FolderAmber)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    if (ask) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text(tr("Quitar de Gardados", "Quitar de Guardados")) },
            text = { Text(tr("Esta páxina deixará de aparecer en Gardados. A súa Nota de referencia consérvase.", "Esta página dejará de aparecer en Guardados. Su Nota de referencia se conserva.")) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    fav = store.toggleHit(hit)
                    ask = false
                    onFavChanged()
                }) { Text("Quitar") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { ask = false }) { Text("Cancelar") }
            },
        )
    }
    if (pickFolder) {
        gal.galiciana.app.ui.FolderPickSheet(
            store = store,
            path = hit.path,
            position = hit.position,
            onDismiss = { pickFolder = false },
        )
    }
}

