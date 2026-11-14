package gal.galiciana.app.ui.hemeroteca

import gal.galiciana.app.i18n.tr
import gal.galiciana.app.ui.liveStatus
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gal.galiciana.app.data.*
import gal.galiciana.app.ui.ClearableField
import gal.galiciana.app.ui.publicationIdentity
import gal.galiciana.app.ui.SectionBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HemerotecaScreen(onHome: () -> Unit, onConsulta: () -> Unit, onDatos: () -> Unit,
    onOpenPath: (String) -> Unit, onFavoritos: () -> Unit = {}) {
    val repo = remember { HemerotecaRepository() }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember(ctx) { LocalStore(ctx) }
    val scope = rememberCoroutineScope()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var index by remember { mutableStateOf(IndiceCabeceras(emptyList(), emptyList(), emptyList())) }
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedTitle by rememberSaveable { mutableStateOf("") }
    var fullTitleExpanded by rememberSaveable(openedId) { mutableStateOf(false) }
    var years by remember { mutableStateOf(emptyList<String>()) }
    var year by rememberSaveable { mutableStateOf<String?>(null) }
    var issues by remember { mutableStateOf(emptyList<Numero>()) }
    var month by rememberSaveable { mutableStateOf<Int?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    var sourceUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLetter by rememberSaveable { mutableStateOf<String?>(null) }
    var retryIndexUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var task by remember { mutableStateOf<Job?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var chooseYear by rememberSaveable { mutableStateOf(false) }
    var chooseLetter by rememberSaveable { mutableStateOf(false) }
    var yearQuery by rememberSaveable { mutableStateOf("") }
    val indexList = androidx.compose.foundation.lazy.rememberLazyListState()
    val issueList = androidx.compose.foundation.lazy.rememberLazyListState()

    fun loadIndex(url: String? = null, searching: Boolean = false, resetScroll: Boolean = true) {
        val request = ++generation
        task?.cancel()
        retryIndexUrl = url
        if (searching) { keyboard?.hide(); submittedQuery = query.trim(); sourceUrl = null; selectedLetter = null
            index = IndiceCabeceras(emptyList(), emptyList(), index.letras) }
        task = scope.launch {
            loading = true; error = null
            try {
                val result = if (submittedQuery.isNotBlank() && (searching || url == null || !url.contains("posicion="))) repo.searchIndice(submittedQuery)
                    else if (url != null) repo.indice(url) else repo.indice()
                if (generation == request) {
                    index = if (submittedQuery.isBlank()) result else result.copy(
                        cabeceras = result.cabeceras.filter { repo.matches(it.title, submittedQuery) })
                    sourceUrl = result.sourceUrl
                    selectedLetter = result.selectedLetter
                    if (resetScroll) indexList.scrollToItem(0)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == request) error = tr("Non se puideron cargar as publicacións. Comproba a conexión e téntao de novo.", "No se pudieron cargar las publicaciones. Comprueba la conexión y reintenta.") }
            finally { if (generation == request) loading = false }
        }
    }
    fun loadIssues(id: String, restore: Boolean = false, requestedYear: String? = null) {
        val request = ++generation
        task?.cancel()
        task = scope.launch {
            loading = true; error = null; issues = emptyList()
            try {
                val available = if (years.isEmpty()) repo.yearsOf(id) else years
                val selected = requestedYear ?: year?.takeIf { restore && it in available } ?: available.firstOrNull()
                if (generation == request) { years = available; year = selected }
                val numbers = repo.loadNumeros(id, selected)
                if (generation == request) {
                    issues = numbers
                    if (month != null && numbers.none { it.month == month }) month = null
                    if (!restore) issueList.scrollToItem(0)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == request) error = tr("Non se puideron cargar os números desta publicación. Téntao de novo.", "No se pudieron cargar los números de esta publicación. Vuelve a intentarlo.") }
            finally { if (generation == request) loading = false }
        }
    }
    fun open(c: Cabecera) {
        openedId = c.id; openedTitle = c.title; years = emptyList(); year = null; month = null
        loadIssues(c.id)
    }
    fun back() {
        if (openedId != null) {
            generation++; task?.cancel(); openedId = null; loading = false; error = null
            if (index.cabeceras.isEmpty()) loadIndex(sourceUrl, resetScroll = false)
        } else onHome()
    }
    BackHandler(enabled = openedId != null) { back() }
    LaunchedEffect(Unit) {
        val id = openedId
        if (id != null) loadIssues(id, restore = true)
        else loadIndex(sourceUrl, resetScroll = false)
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        SectionBar("Hemeroteca", MaterialTheme.colorScheme.primary, ::back, onHome)
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), state = if (openedId == null) indexList else issueList,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
            item {
                if (openedId == null) {
                    Text(tr("Xornais e revistas", "Periódicos y revistas"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(tr("Busca unha publicación; despois elixe o ano e o número.", "Busca una publicación; después elige el año y el número."),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClearableField(query, { query = it }, tr("Título da publicación", "Título de la publicación"), Modifier.weight(1f), onSearch = { loadIndex(searching = true) })
                        androidx.compose.material3.Button(onClick = { loadIndex(searching = true) }, enabled = !loading,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Buscar") }
                    }
                    // The alphabet is the index itself: every letter in view, one tap away.
                    if (submittedQuery.isBlank() && index.letras.isNotEmpty()) {
                        val letterState = rememberLazyListState()
                        val current = selectedLetter ?: index.letras.first().first
                        LaunchedEffect(index.letras, current) {
                            index.letras.indexOfFirst { it.first == current }.takeIf { it > 2 }?.let { letterState.scrollToItem(it - 2) }
                        }
                        LazyRow(state = letterState, modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(index.letras) { (letter, url) ->
                                val selected = letter == current
                                Box(Modifier.size(48.dp)
                                    .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .clickable(enabled = !loading, role = androidx.compose.ui.semantics.Role.Button) {
                                        if (!selected) { query = ""; submittedQuery = ""; selectedLetter = letter; loadIndex(url) }
                                    }
                                    .semantics {
                                        contentDescription = if (letter == "#") tr("Números e símbolos", "Números y símbolos") else tr("Letra $letter", "Letra $letter")
                                        this.selected = selected
                                    },
                                    contentAlignment = Alignment.Center) {
                                    Text(letter, style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    if (submittedQuery.isNotBlank()) Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Título: $submittedQuery", "Título: $submittedQuery"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
                            query = ""; submittedQuery = ""; sourceUrl = null; loadIndex()
                        }) { Text(tr("Limpar título", "Limpiar título")) }
                    }
                    if (!loading && error == null) Text(tr("${index.cabeceras.size} publicacións", "${index.cabeceras.size} publicaciones"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp))
                } else {
                    val identity = publicationIdentity(openedTitle)
                    Text(identity.shortTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        maxLines = if (fullTitleExpanded) Int.MAX_VALUE else 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (identity.shortTitle != openedTitle || openedTitle.length > 70) {
                        TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { fullTitleExpanded = !fullTitleExpanded }) {
                            Text(if (fullTitleExpanded) "Ocultar título completo" else "Ver título completo")
                        }
                        if (fullTitleExpanded && identity.shortTitle != openedTitle) Text(openedTitle,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { chooseYear = true; yearQuery = "" }, enabled = years.isNotEmpty() && !loading,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text(tr("Ano ${year ?: "…"} · Cambiar", "Año ${year ?: "…"} · Cambiar"))
                        }
                        Text(tr("${issues.size} números dispoñibles", "${issues.size} números disponibles"), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                    val months = issues.mapNotNull { it.month }.distinct().sorted()
                    if (months.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { gal.galiciana.app.ui.SelectChip(selected = month == null, onClick = { month = null }, label = { Text(tr("Todo o ano", "Todo el año")) }) }
                        items(months) { m -> gal.galiciana.app.ui.SelectChip(selected = month == m, onClick = { month = m }, label = { Text(repo.monthName(m)) }) }
                    }
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 12.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp).liveStatus())
                    androidx.compose.material3.OutlinedButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { openedId?.let { loadIssues(it, restore = true) } ?: loadIndex(retryIndexUrl) }) { Text("Reintentar") } }
            }
            if (openedId == null) {
                items(index.cabeceras, key = { it.id }) { c ->
                    ListItem(headlineContent = { Text(c.title, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text(tr("Ver anos e números", "Ver años y números")) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        modifier = Modifier.clickable(enabled = !loading, role = androidx.compose.ui.semantics.Role.Button) { open(c) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (!loading && error == null && index.cabeceras.isEmpty()) item {
                    Text(tr("Non hai publicacións con ese título nesta páxina. Cambia a consulta ou segue a navegar polo índice.", "No hay publicaciones con ese título en esta página. Cambia la consulta o sigue navegando por el índice."), modifier = Modifier.padding(vertical = 20.dp))
                }
                if (index.previousUrl != null || index.nextUrl != null) item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { index.previousUrl?.let { loadIndex(it) } }, enabled = !loading && index.previousUrl != null,
                            modifier = Modifier.weight(1f)) { Text("Anterior") }
                        Text(index.pageNumber?.let { tr("Páxina $it do índice", "Página $it del índice") } ?: tr("Páxinas do índice", "Páginas del índice"), style = MaterialTheme.typography.labelMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                        TextButton(onClick = { index.nextUrl?.let { loadIndex(it) } }, enabled = !loading && index.nextUrl != null,
                            modifier = Modifier.weight(1f)) { Text(tr("Seguinte", "Siguiente")) }
                    }
                }
            } else {
                issues.filter { month == null || it.month == month }.groupBy { it.month }.forEach { (m, numbers) ->
                    item(key = "month-${m ?: 0}") { Text(m?.let(repo::monthName) ?: tr("Sen mes identificado", "Sin mes identificado"),
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) }
                    items(numbers, key = { it.path }) { n ->
                        ListItem(headlineContent = { Text(n.label) }, trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                            modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                                store.rememberWork(n.path, "$openedTitle · ${n.label}", openedTitle)
                                onOpenPath(n.path)
                            })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                if (!loading && error == null && issues.isEmpty()) item { Text(tr("Non hai números dixitalizados para este ano.", "No hay números digitalizados para este año."), Modifier.padding(vertical = 24.dp)) }
            }
        }
    }
    if (chooseYear || chooseLetter) ModalBottomSheet(onDismissRequest = { chooseYear = false; chooseLetter = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(if (chooseYear) tr("Elixir ano", "Elegir año") else tr("Elixir letra", "Elegir letra"), style = MaterialTheme.typography.headlineSmall)
            if (chooseYear) ClearableField(yearQuery, { yearQuery = it }, tr("Escribe un ano", "Escribe un año"), modifier = Modifier.padding(vertical = 16.dp),
                imeSearch = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 440.dp)) {
                if (chooseYear && years.none { it.contains(yearQuery.trim()) }) item {
                    Text(tr("Non hai anos dispoñibles que coincidan.", "No hay años disponibles que coincidan."), modifier = Modifier.padding(vertical = 20.dp))
                }
                if (chooseYear) items(years.filter { it.contains(yearQuery.trim()) }) { value ->
                    ListItem(headlineContent = { Text(value) }, trailingContent = { if (year == value) Text("Seleccionado", color = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { keyboard?.hide(); chooseYear = false; month = null; openedId?.let { loadIssues(it, requestedYear = value) } })
                } else items(index.letras) { (letter, url) ->
                    ListItem(headlineContent = { Text(if (letter == "#") tr("Números e símbolos", "Números y símbolos") else letter) },
                        modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                            chooseLetter = false; query = ""; submittedQuery = ""; selectedLetter = letter; loadIndex(url)
                        })
                }
            }
        }
    }
}
