package gal.galiciana.app.ui.search

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
import gal.galiciana.app.ui.theme.MatteFill
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
            if (!history.remember(preset())) searchNotice = "No se pudo guardar esta consulta en Recientes."
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
    BackHandler(enabled = vm.showingResults) { vm.showingResults = false }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showSearchHelp by rememberSaveable { mutableStateOf(false) }
    var publicationFilters by rememberSaveable { mutableStateOf(seedPreset?.let { it.title.isNotBlank() || it.author.isNotBlank() || it.place.isNotBlank() } ?: (seedTitle.isNotBlank() || history.draft()?.let { it.title.isNotBlank() || it.author.isNotBlank() || it.place.isNotBlank() } == true)) }
    var periodFilters by rememberSaveable { mutableStateOf(seedPreset?.let { it.yearFrom.isNotBlank() || it.yearTo.isNotBlank() } ?: (history.draft()?.let { it.yearFrom.isNotBlank() || it.yearTo.isNotBlank() } == true)) }
    var showResultDetails by rememberSaveable { mutableStateOf(false) }
    val qTerm = vm.submittedQuery
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
                onBack = {
                    if (vm.showingResults) vm.showingResults = false else onBack()
                },
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
                        placeholder = "Modificar búsqueda", modifier = Modifier.weight(1f), onSearch = { runSearch() })
                    IconButton(onClick = { runSearch() }, enabled = vm.canSearch && !vm.loading) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar con esta consulta",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("${vm.groups.size} ${if (vm.groups.size == 1) "obra" else "obras"} · ${vm.hits.size} ${if (vm.hits.size == 1) "página cargada" else "páginas cargadas"}",
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                if (vm.submittedQuery.isNotBlank() && vm.query != vm.submittedQuery) Text(
                    "Consulta aplicada: ${vm.submittedQuery}", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.showingResults = false; vm.advanced = true }) {
                        Icon(Icons.Filled.Tune, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Filtros")
                    }
                    Box {
                        TextButton(onClick = { sortMenuOpen = true }, enabled = !vm.loading) {
                            val label = when (vm.sortBy) {
                                SearchViewModel.SortBy.Titulo -> "Título"
                                SearchViewModel.SortBy.Autor -> "Autor"
                                SearchViewModel.SortBy.Fecha -> "Fecha"
                                SearchViewModel.SortBy.Relevancia -> "Relevancia"
                            }
                            Text("Orden: $label")
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            listOf(SearchViewModel.SortBy.Relevancia to "Relevancia",
                                SearchViewModel.SortBy.Titulo to "Título", SearchViewModel.SortBy.Autor to "Autor",
                                SearchViewModel.SortBy.Fecha to "Fecha").forEach { (key, label) ->
                                DropdownMenuItem(text = { Text(label + if (vm.sortBy == key && key != SearchViewModel.SortBy.Relevancia)
                                    (if (vm.sortAsc) " · ascendente" else " · descendente") else "") },
                                    onClick = { sortMenuOpen = false; vm.setSort(key)
                                        scope.launch { listState.scrollToItem(0) } })
                            }
                        }
                    }
                    TextButton(onClick = { showResultDetails = !showResultDetails }) {
                        Text(if (showResultDetails) "Menos" else "Detalles")
                    }
                }
                if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                vm.error?.takeUnless { it == "Sin resultados con esos criterios" && vm.variantLoading }
                    ?.let { message ->
                        val emptyResult = message == "Sin resultados con esos criterios"
                        Text(if (emptyResult) "No encontramos páginas con esos criterios. Prueba otra palabra o amplía los filtros." else message,
                            color = if (emptyResult) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                val scopeLabel = listOfNotNull(
                    if (vm.pendingUrls.isNotEmpty()) "Resultados parciales" else null,
                    if (vm.sortBy == SearchViewModel.SortBy.Relevancia) "Relevancia sobre páginas cargadas" else null,
                ).joinToString(" · ")
                if (scopeLabel.isNotBlank()) Text(scopeLabel,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (vm.submittedSection.isNotBlank()) Text("Tema orientativo: ${vm.submittedSection}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (showResultDetails) {
                    Text("${vm.groups.sumOf { it.occurrenceCount }} extractos cargados" +
                        (vm.totalWorks?.let { " · $it obras en Galiciana" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (vm.pendingUrls.isNotEmpty()) Text(
                        if (vm.globalSortAvailable) "Orden aplicado al total de Galiciana; se muestran las primeras páginas."
                        else "El orden se aplica a las páginas cargadas de Galiciana.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (vm.sortBy == SearchViewModel.SortBy.Relevancia) Text("La cercanía se calcula entre las páginas cargadas. Cargar más páginas puede cambiar el orden.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Las variantes de OCR y las coincidencias más amplias aparecen en grupos separados.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (vm.error == null && !vm.loading && !vm.variantLoading && vm.groups.isEmpty() && vm.variantGroups.isEmpty() && vm.recoveryGroups.isEmpty())
                    Text("Prueba otra palabra o amplía los filtros.", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (vm.submittedQuery.isNotBlank() && !vm.loading && !vm.variantLoading &&
                    vm.completedSearchGeneration == vm.searchGeneration &&
                    (showResultDetails || (vm.groups.isEmpty() && vm.variantGroups.isEmpty()))) {
                    TextButton(onClick = { vm.loadRecovery() }, enabled = !vm.recoveryLoading) {
                        Text(if (vm.recoveryLoading) "Buscando coincidencias…" else "Buscar coincidencias más amplias")
                    }
                }
                if (vm.variantLoading) Text("Comprobando variantes de OCR…",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    onOpenHit(p, first.position ?: 1, viewerQuery, first.imageId)
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
                            onClick = {
                                hit.path?.let { p ->
                                    store.markVisited(favKey(p, hit.position ?: 1, hit.title))
                                    store.rememberWork(p, hit.title, hit.parentTitle)
                                    onOpenHit(p, hit.position ?: 1, viewerQuery, hit.imageId)
                                }
                            },
                        )
                        /* slot */
                    }
                }
                if (vm.pendingUrls.isNotEmpty()) item(key = "load-more") {
                    Button(
                        onClick = { vm.loadMore() },
                        enabled = !vm.loading,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    ) { Text("CARGAR MÁS RESULTADOS") }
                }
                if (vm.variantGroups.isNotEmpty()) {
                    item(key = "ocr-variants-heading") {
                        Text("Misma frase con errores de OCR · ${vm.variantHits.size} páginas",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }
                    vm.variantGroups.forEach { group ->
                        item(key = "v-h-${group.pages.firstOrNull()?.path}-${group.title.hashCode()}") {
                            WorkHead(title = group.title, parent = group.parentTitle,
                                pages = group.pages.size, hits = group.occurrenceCount,
                                visited = false,
                                onOpen = {
                                    group.pages.firstOrNull()?.let { hit ->
                                        store.rememberOpenedHit(hit.copy(savedQuery = viewerQuery))
                                        hit.path?.let { onOpenHit(it, hit.position ?: 1, viewerQuery, hit.imageId) }
                                    }
                                })
                        }
                        items(group.pages, key = { "v-p-${it.path}-${it.imageId}-${it.position}" }) { hit ->
                            ResultHitRow(hit = hit, query = qTerm, approximate = true,
                                visited = false,
                                onClick = {
                                    hit.path?.let { onOpenHit(it, hit.position ?: 1, viewerQuery, hit.imageId) }
                                })
                        }
                    }
                    if (vm.variantScanIncomplete) item(key = "variant-scan-limit") {
                        Text("Se han comprobado los primeros candidatos de Galiciana; puede haber más variantes OCR.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (vm.recoveryGroups.isNotEmpty()) {
                    item(key = "recovery-heading") {
                        Text("Coincidencias más amplias · ${vm.recoveryHits.size} páginas (máximo ${vm.recoveryLimit})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        Text("Candidatos más flexibles: pueden faltar palabras de tu consulta. Comprueba el texto de cada página. Conservan los filtros de título, autor, lugar y años; no limitan las palabras de sección.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    vm.recoveryGroups.forEach { group ->
                        item(key = "r-h-${group.pages.firstOrNull()?.path}-${group.title.hashCode()}") {
                            WorkHead(title = group.title, parent = group.parentTitle,
                                pages = group.pages.size, hits = group.occurrenceCount,
                                visited = false,
                                onOpen = {
                                    group.pages.firstOrNull()?.let { hit ->
                                        store.rememberOpenedHit(hit.copy(savedQuery = viewerQuery))
                                        hit.path?.let { onOpenHit(it, hit.position ?: 1, viewerQuery, hit.imageId) }
                                    }
                                })
                        }
                        items(group.pages, key = { "r-p-${it.path}-${it.imageId}-${it.position}" }) { hit ->
                            ResultHitRow(hit = hit, query = qTerm, approximate = true,
                                visited = false,
                                onClick = {
                                    hit.path?.let { onOpenHit(it, hit.position ?: 1, viewerQuery, hit.imageId) }
                                })
                        }
                    }
                    if (vm.recoveryIncomplete) item(key = "recovery-limit") {
                        Text("Hay más candidatos en Galiciana; la búsqueda ampliada se ha detenido en el límite indicado.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (vm.recoverySearched && vm.recoveryGroups.isEmpty()) item(key = "recovery-empty") {
                    Text("No se encontraron candidatos adicionales en la búsqueda ampliada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onHistory) { Text("Tus búsquedas") }
                TextButton(onClick = {
                    searchNotice = if (history.save(preset())) "Búsqueda guardada con sus filtros." else "No se pudo guardar la búsqueda."
                }, enabled = vm.canSearch) { Text("Guardar búsqueda") }
            }
            ClearableField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = "Palabras, personas o lugares",
                label = "Tu búsqueda",
                imeSearch = true,
                onSearch = { runSearch() },
            )
            Spacer(Modifier.height(12.dp))
            Text("Encuentra personas, lugares y noticias en los documentos digitalizados.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { showSearchHelp = !showSearchHelp }) {
                Text(if (showSearchHelp) "Ocultar ayuda" else "Cómo afinar la búsqueda")
            }
            if (showSearchHelp) Text("Sin comillas se buscan todas las palabras, aunque estén separadas y en otro orden. Con comillas se busca una expresión seguida. Si el OCR omite alguna, prueba «Buscar coincidencias más amplias». ? sustituye una letra y * una terminación.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(value = vm.advanced, role = Role.Checkbox, onValueChange = { vm.advanced = it }),
                verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = vm.advanced, onCheckedChange = null)
                Text("Mostrar filtros", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(value = vm.fullText, role = Role.Checkbox, onValueChange = { vm.fullText = it }),
                verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = vm.fullText, onCheckedChange = null)
                Text("Buscar en el texto completo", style = MaterialTheme.typography.bodySmall)
            }
            Text("Se muestran documentos con páginas digitalizadas.", style = MaterialTheme.typography.bodySmall)
            if (vm.advanced) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tema orientativo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text("Añade palabras a la consulta. No identifica automáticamente las secciones del periódico.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LazyRow {
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
            TextButton(onClick = { publicationFilters = !publicationFilters }) {
                Text(if (publicationFilters) "Ocultar publicación y autor" else "Publicación, autor y lugar")
            }
            if (publicationFilters) {
            ClearableField(vm.title, { vm.title = it }, placeholder = "", label = "Título", enabled = vm.advanced, imeSearch = false)
            Spacer(Modifier.height(12.dp))
            ClearableField(vm.author, { vm.author = it }, placeholder = "", label = "Autor", enabled = vm.advanced, imeSearch = false)
            Spacer(Modifier.height(12.dp))
            ClearableField(vm.place, { vm.place = it }, placeholder = "", label = "Lugar edición/impresión", enabled = vm.advanced, imeSearch = false)
            Spacer(Modifier.height(12.dp))
            }
            TextButton(onClick = { periodFilters = !periodFilters }) {
                Text(if (periodFilters) "Ocultar periodo" else "Periodo de publicación")
            }
            if (periodFilters) Row {
                ClearableField(vm.yearFrom, { vm.yearFrom = it }, placeholder = "", label = "Año desde", modifier = Modifier.weight(1f), enabled = vm.advanced, imeSearch = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                Spacer(Modifier.width(12.dp))
                ClearableField(vm.yearTo, { vm.yearTo = it }, placeholder = "", label = "Año hasta", modifier = Modifier.weight(1f), enabled = vm.advanced, imeSearch = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            }
            vm.yearError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(16.dp))
            } // Form scrolls independently from its primary action.
            val active = preset().summary()
            if (vm.advanced) Text(active, style = MaterialTheme.typography.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 8.dp))
            searchNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            vm.yearError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { runSearch() }, enabled = vm.canSearch && !vm.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text("Buscar") }
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
        Text(title.ifBlank { "Título desconocido" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (visited) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() })
        parent?.trim()?.takeIf { it.isNotBlank() && !it.equals(title.trim(), ignoreCase = true) }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        Text("$pages ${if (pages == 1) "página" else "páginas"}" +
            if (hits > 0) " · $hits ${if (hits == 1) "extracto" else "extractos"}" else "",
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
    var fav by remember(hit.path, hit.position) { mutableStateOf(store.isFav(hit.path, hit.position)) }
    var ask by remember { mutableStateOf(false) }
    var pickFolder by remember { mutableStateOf(false) }
    val markQ = query.ifBlank { hit.savedQuery }
    val orderedSnippets = remember(hit.snippets, markQ) {
        hit.snippets.sortedByDescending { pageRelevance(hit.copy(snippets = listOf(it)), markQ) }
    }
    var excerptsExpanded by rememberSaveable(hit.path, hit.position, markQ) { mutableStateOf(false) }
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
                    if (hit.savedQuery.isNotBlank()) "Búsqueda: ${hit.savedQuery}" else "Búsqueda original no registrada",
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
                            maxLines = 4, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                if (hit.snippets.isEmpty()) Text(
                    if (recoveringOcr) "Recuperando el fragmento original…"
                    else if (showSavedSearch) "El extracto original no quedó guardado"
                    else "Sin extracto de texto disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else {
                    (if (excerptsExpanded) orderedSnippets else orderedSnippets.take(2)).forEach { line ->
                        Text(if (approximate) highlightPossibleTerms("“$line”", markQ) else highlightQuery("“$line”", markQ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (excerptsExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!excerptsExpanded && it.hasVisualOverflow) excerptsOverflow = true },
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    if (hit.snippets.size > 2 || excerptsOverflow) TextButton(onClick = { excerptsExpanded = !excerptsExpanded }) {
                        Text(if (excerptsExpanded) "Mostrar menos" else if (hit.snippets.size > 1)
                            "Ver ${hit.snippets.size} extractos" else "Leer extracto completo")
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (hit.position != null) "Página ${hit.position}" else "Página",
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
                        if (adding && fav) pickFolder = true
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
                            contentDescription = "Organizar página en carpetas",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    if (ask) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("Quitar del archivo") },
            text = { Text("¿Seguro? Esta página dejará de estar en tu archivo de favoritos.") },
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

