package gal.galiciana.app.ui.colecciones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import gal.galiciana.app.data.LocalStore
import gal.galiciana.app.ui.PublicationYearSelector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import gal.galiciana.app.data.GalicianaUrls
import gal.galiciana.app.data.Http
import gal.galiciana.app.data.HemerotecaRepository
import gal.galiciana.app.data.Numero
import gal.galiciana.app.ui.SectionBar
import gal.galiciana.app.ui.SectionColecciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

internal data class Seccion(
    val title: String,
    val imageId: String,
    val code: String,
    val filterCode: String = code,
    val children: List<Seccion> = emptyList(),
)

private val Secciones = listOf(
    Seccion("Libros y monografías", "13124554", "MONOGRAFIA"),
    Seccion("Mapas y planos", "14098722", "CARTOGRAF"),
    Seccion("Manuscritos", "13948376", "MANUSCRITO"),
    Seccion("Imágenes y documentos gráficos", "13787413", "ILUSFOT", children = listOf(
        Seccion("Carteles", "13124554", "CARTEIS", "ILUSFOT_CARTEIS"),
        Seccion("Tarjetas postales", "13787413", "POSTAIS", "ILUSFOT_POSTAIS"),
        Seccion("Impresos efímeros", "14185891", "EPHEMERA", "ILUSFOT_EPHEMERA"),
        Seccion("Imágenes del portal conjunto", "14153014", "FOTOS", "ILUSFOT_FOTOS"),
    )),
    Seccion("Archivos personales", "14185891", "ARQUIVOS", children = listOf(
        Seccion("Camilo Díaz Baliño", "14185891", "ARQCDB", "ARQUIVOS_ARQCDB"),
        Seccion("Carlos Casares", "14185891", "ARQCCM", "ARQUIVOS_ARQCCM"),
    )),
    Seccion("Grabaciones sonoras", "13872855", "SONS"),
    Seccion("Vídeos y audiovisuales", "14098722", "AUDIOVISUA"),
    Seccion("Partituras y música impresa", "13948376", "MUSICA"),
)

internal data class CollectionWork(
    val title: String,
    val path: String?,
    val recordUrl: String?,
    val thumbnailUrl: String?,
    val creator: String? = null,
    val date: String? = null,
)

internal data class CollectionPage(val works: List<CollectionWork>, val nextUrl: String?)
internal data class CollectionDetail(
    val publicationId: String?, val path: String?, val description: String,
    val imageUrl: String? = null, val sourceUrl: String? = null,
    val metadata: List<CatalogueField> = emptyList(),
)

internal data class CatalogueField(val label: String, val value: String)

/** Preserve each source field instead of flattening the record into one paragraph. */
internal fun parseCatalogueMetadata(html: String): List<CatalogueField> {
    val document = Jsoup.parse(html)
    val record = document.selectFirst("#panel_ficha_dc, #ficha, .ficha") ?: return emptyList()
    val fields = mutableListOf<CatalogueField>()
    // Dolmen's current public records use label/value spans, not table rows.
    record.select(".ficha_valor").forEach { row ->
        val label = row.selectFirst(".etiqueta")?.text()?.trim()?.trimEnd(':')?.trim().orEmpty()
        val value = row.selectFirst(".valor")?.clone()
        value?.select(".related-search, i, script, style")?.remove()
        val pieces = value?.select(".field_value")?.filter { field ->
            field.parents().none { parent -> parent !== value && parent.hasClass("field_value") }
        }.orEmpty()
        val text = if (pieces.isNotEmpty()) pieces.joinToString("\n") { it.wholeText().trim() }
            else value?.wholeText()?.trim().orEmpty()
        if (label.isNotBlank() && text.isNotBlank()) fields += CatalogueField(label, text)
    }
    record.select("tr").forEach { row ->
        val cells = row.children().filter { it.tagName() == "th" || it.tagName() == "td" }
        if (cells.size >= 2) {
            val label = cells.first().text().trim().trimEnd(':').trim()
            val value = cells.drop(1).joinToString("\n") { it.wholeText().trim() }.trim()
            if (label.isNotBlank() && value.isNotBlank()) fields += CatalogueField(label, value)
        }
    }
    record.select("dt").forEach { term ->
        val values = mutableListOf<String>()
        var next = term.nextElementSibling()
        while (next != null && next.tagName() != "dt") {
            if (next.tagName() == "dd") next.wholeText().trim().takeIf { it.isNotBlank() }?.let(values::add)
            next = next.nextElementSibling()
        }
        val label = term.text().trim().trimEnd(':').trim()
        if (label.isNotBlank() && values.isNotEmpty()) fields += CatalogueField(label, values.joinToString("\n"))
    }
    return fields.distinct()
}

internal class CollectionRepository {
    private val base = GalicianaUrls.BASE
    private val catalogue = "$base/es/consulta/resultados_busqueda.do"
    private val photoBase = "https://www.galiciana.gal/arpadhub"
    private fun photoUrl(term: String? = null): String {
        val url = okhttp3.HttpUrl.Builder().scheme("https").host("www.galiciana.gal")
            .addPathSegments("arpadhub/es/consulta/resultados_busqueda.do")
            .addQueryParameter("busq_tipodoc", "Ilustraciones y Fotos")
            .addQueryParameter("descrip_tipodoc", "Ilustraciones y Fotos")
        term?.takeIf { it.isNotBlank() }?.let { url.addQueryParameter("busq_general", it) }
        return url.build().toString()
    }

    suspend fun browse(section: Seccion): CollectionPage = withContext(Dispatchers.IO) {
        if (section.code == "FOTOS") {
            val url = photoUrl()
            return@withContext fetch(Request.Builder().url(url).build(), url)
        }
        val title = URLEncoder.encode(section.title, "UTF-8")
        val url = "$catalogue?busq_secbib=${section.code}&descrip_secbib=$title&presentacion=mosaico"
        fetch(Request.Builder().url(url).build(), url)
    }

    suspend fun search(section: Seccion, term: String): CollectionPage = withContext(Dispatchers.IO) {
        if (section.code == "FOTOS") {
            val url = photoUrl(term)
            return@withContext fetch(Request.Builder().url(url).build(), url)
        }
        val body = FormBody.Builder()
            .add("busq_general", term)
            .add("secc_${section.filterCode}", "on")
            .add("busq_objetosmultimedia", "true")
            .add("tipoResultados", "BIB")
            .build()
        // The advanced catalogue form submits to resultados_ocr.do even when
        // full-text searching is unchecked. This combines the query with the
        // section filter on the server, beyond the currently loaded cards.
        val url = "$base/es/consulta/resultados_ocr.do"
        fetch(Request.Builder().url(url).post(body).build(), url)
    }

    suspend fun next(url: String): CollectionPage = withContext(Dispatchers.IO) {
        require(url.startsWith("$base/es/consulta/") ||
            url.startsWith("$photoBase/es/consulta/resultados_busqueda.do?"))
        fetch(Request.Builder().url(url).build(), url)
    }

    private fun fetch(request: Request, url: String): CollectionPage {
        val html = Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Galiciana: HTTP ${response.code}")
            response.body?.string() ?: error("Respuesta vacía de Galiciana")
        }
        return parse(html, url)
    }

    fun parse(html: String, url: String): CollectionPage {
        val doc = Jsoup.parse(html, url)
        check(!(doc.body().hasClass("block") && doc.select(".registro, .nav_registro, a[href*=registro.do], .ficha").isEmpty())) {
            "Galiciana no ha devuelto el catálogo. Vuelve a intentarlo."
        }
        if (url.startsWith(photoBase)) {
            val works = doc.select(".nav_registros .nav_registro").mapNotNull { card ->
                val anchor = card.selectFirst(".titulo a[href*='idBib=']") ?: return@mapNotNull null
                val recordUrl = anchor.absUrl("href")
                    .takeIf { it.startsWith("$photoBase/es/registros/registro.do?") }
                    ?: return@mapNotNull null
                CollectionWork(anchor.text().trim(), null, recordUrl, null)
            }.distinctBy { it.recordUrl }
            val nextUrl = doc.selectFirst("#boton_siguiente")?.absUrl("href")
                ?.takeIf { it.startsWith("$photoBase/es/consulta/resultados_busqueda.do?") }
            return CollectionPage(works, nextUrl)
        }
        val works = doc.select(".nav_registros .nav_registro").mapNotNull { card ->
            val group = card.selectFirst("a[href*='grupo.do?path=']")
            val path = group?.attr("href")?.let {
                Regex("[?&]path=(\\d+)").find(it)?.groupValues?.get(1)
            }
            val record = card.selectFirst(".titulo a[href*='registro.do'], a[href*='registro.do'], .titulo a[href*='forma=ficha'], a[href*='forma=ficha']")
            val title = card.selectFirst(".titulo a, .titulo")?.text()?.trim()?.takeIf { it.isNotBlank() }
                ?: group?.attr("data-analytics-recordtitle")?.trim()?.takeIf { it.isNotBlank() }
                ?: record?.text()?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            // Non-digitised records have no grupo.do link. Their catalogue ID is also
            // embedded in the result card even when the title is not an HTML link.
            val recordId = Regex("<!--\\s*Id:\\s*(\\d+)\\s*-->", RegexOption.IGNORE_CASE)
                .find(card.outerHtml())?.groupValues?.get(1)
            val recordUrl = record?.absUrl("href")
                ?.takeIf { it.startsWith("$base/es/consulta/") &&
                    (it.contains("registro.do?") || it.contains("forma=ficha")) }
                ?: recordId?.let(GalicianaUrls::record)
            val thumbnailUrl = card.selectFirst("img.miniatura")?.absUrl("src")
                ?.takeIf { it.startsWith(base) }
            CollectionWork(title, path, recordUrl, thumbnailUrl,
                card.selectFirst(".autor, .autores, [itemprop=author]")?.text()?.trim()?.takeIf { it.isNotBlank() },
                card.selectFirst(".fecha, [itemprop=datePublished]")?.text()?.trim()?.takeIf { it.isNotBlank() })
        }.distinctBy { it.path ?: it.recordUrl ?: it.title }
        val nextUrl = doc.selectFirst("#boton_siguiente")?.absUrl("href")
            ?.takeIf { it.startsWith("$base/es/consulta/") }
        return CollectionPage(works, nextUrl)
    }

    suspend fun detail(url: String): CollectionDetail = withContext(Dispatchers.IO) {
        val isPhoto = url.startsWith("$photoBase/es/registros/registro.do?")
        require(isPhoto || (url.startsWith("$base/es/consulta/") &&
            (url.contains("registro.do?") || url.contains("forma=ficha"))))
        val html = Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Galiciana: HTTP ${response.code}")
            response.body?.string() ?: error("Respuesta vacía de Galiciana")
        }
        val doc = Jsoup.parse(html, url)
        check(!(doc.body().hasClass("block") && doc.select(".registro, .nav_registro, a[href*=registro.do], .ficha").isEmpty())) {
            "Galiciana no ha devuelto el catálogo. Vuelve a intentarlo."
        }
        if (isPhoto) {
            val image = Regex("https://(?:arquivo\\.galiciana\\.gal/arpadweb|biblioteca\\.galiciana\\.gal)/[^\\s<>\"]+/catalogo_imagenes/imagen_id\\.do\\?idImagen=\\d+")
                .find(html)?.value
            val fields = doc.select("#panel_ficha_dc dt").mapNotNull { term ->
                val value = term.nextElementSibling()?.takeIf { it.tagName() == "dd" }
                    ?.text()?.trim().orEmpty()
                value.takeIf { it.isNotBlank() }?.let { "${term.text().trim()} $it" }
            }.distinct().joinToString("\n")
            return@withContext CollectionDetail(null, null, fields, image, url, parseCatalogueMetadata(html))
        }
        val publicationId = Regex("[?&]idPublicacion=(\\d+)")
            .find(html)?.groupValues?.get(1)
        val path = doc.select("a[href*='grupo.do']").firstNotNullOfOrNull { anchor ->
            Regex("[?&]path=(\\d+)").find(anchor.attr("href"))?.groupValues?.get(1)
        }
        val ficha = doc.selectFirst(".ficha, #ficha")
        val rows = ficha?.select("tr")?.mapNotNull { row ->
            val cells = row.select("th, td").map { it.text().trim() }.filter { it.isNotBlank() }
            cells.takeIf { it.isNotEmpty() }?.joinToString(": ")
        }.orEmpty()
        val description = rows.distinct().joinToString("\n").ifBlank { ficha?.text().orEmpty() }
        CollectionDetail(publicationId, path, description, sourceUrl = url, metadata = parseCatalogueMetadata(html))
    }
}

internal class ColeccionesViewModel : ViewModel() {
    private val repo = CollectionRepository()
    private val issuesRepo = HemerotecaRepository()
    var vista by mutableStateOf("raiz")
    var parent by mutableStateOf<Seccion?>(null)
    var selected by mutableStateOf<Seccion?>(null)
    var works by mutableStateOf<List<CollectionWork>>(emptyList())
    var nextUrl by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var q by mutableStateOf("")
    var appliedQuery by mutableStateOf("")
    var openedWork by mutableStateOf<CollectionWork?>(null)
    var detail by mutableStateOf<CollectionDetail?>(null)
    var years by mutableStateOf<List<String>>(emptyList())
    var year by mutableStateOf<String?>(null)
    var issues by mutableStateOf<List<Numero>>(emptyList())
    private var job: Job? = null
    private var generation = 0
    private var retryOperation: (() -> Unit)? = null
    var browseGeneration by mutableStateOf(0)
        private set
    fun retry() { if (!loading) retryOperation?.invoke() }
    private val previewSemaphore = Semaphore(4)
    private val queuedPreviews = mutableSetOf<String>()
    private val photoDetails = mutableMapOf<String, CollectionDetail>()

    fun preview(work: CollectionWork) {
        if (selected?.code != "FOTOS" || work.thumbnailUrl != null) return
        val url = work.recordUrl ?: return
        if (!queuedPreviews.add(url)) return
        viewModelScope.launch {
            try {
                val item = previewSemaphore.withPermit { repo.detail(url) }
                photoDetails[url] = item
                item.imageUrl?.let { image ->
                    works = works.map { if (it.recordUrl == url) it.copy(thumbnailUrl = image) else it }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { queuedPreviews.remove(url) }
        }
    }

    fun open(section: Seccion) {
        job?.cancel()
        browseGeneration++
        selected = section
        vista = "resultados"
        q = ""
        appliedQuery = ""
        works = emptyList()
        nextUrl = null
        load { repo.browse(section) }
    }

    fun search() {
        val section = selected ?: return
        val term = q.trim()
        if (term.isBlank()) {
            if (appliedQuery.isNotBlank()) open(section)
            return
        }
        job?.cancel()
        browseGeneration++
        appliedQuery = term
        works = emptyList()
        nextUrl = null
        load { repo.search(section, term) }
    }

    fun more() {
        if (loading) return
        val url = nextUrl ?: return
        load(append = true) { repo.next(url) }
    }

    fun openWork(work: CollectionWork) {
        val url = work.recordUrl
        retryOperation = { openWork(work) }
        job?.cancel()
        val request = ++generation
        openedWork = work
        vista = "ficha"
        detail = null
        years = emptyList()
        issues = emptyList()
        job = viewModelScope.launch {
            loading = true
            error = null
            try {
                val item = if (url != null) photoDetails[url] ?: repo.detail(url)
                    else CollectionDetail(null, work.path, "")
                detail = item
                if (item.publicationId != null) {
                    years = issuesRepo.yearsOf(item.publicationId)
                    year = years.firstOrNull()
                    issues = issuesRepo.loadNumeros(item.publicationId, year)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No se pudo cargar la ficha. Comprueba la conexión e inténtalo de nuevo."
            } finally { if (generation == request) loading = false }
        }
    }

    fun selectYear(value: String) {
        val id = detail?.publicationId ?: return
        retryOperation = { selectYear(value) }
        job?.cancel()
        val request = ++generation
        job = viewModelScope.launch {
            loading = true
            error = null
            year = value
            issues = emptyList()
            try { issues = issuesRepo.loadNumeros(id, value) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No se pudieron cargar los números de este año. Puedes volver a intentarlo."
            } finally { if (generation == request) loading = false }
        }
    }

    fun back(): Boolean = when (vista) {
        "ficha" -> {
            generation++
            job?.cancel()
            vista = "resultados"
            openedWork = null
            error = null
            loading = false
            true
        }
        "resultados" -> {
            generation++
            job?.cancel()
            loading = false
            vista = if (parent == null) "raiz" else "hijos"
            true
        }
        "hijos" -> {
            parent = null
            vista = "raiz"
            true
        }
        else -> false
    }

    private fun load(append: Boolean = false, fetch: suspend () -> CollectionPage) {
        retryOperation = { load(append, fetch) }
        val request = ++generation
        job = viewModelScope.launch {
            loading = true
            error = null
            try {
                val page = fetch()
                works = if (append) (works + page.works).distinctBy { it.path ?: it.recordUrl ?: it.title }
                    else page.works
                nextUrl = page.nextUrl
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No se pudieron cargar las obras. Comprueba la conexión e inténtalo de nuevo."
            } finally {
                if (generation == request) loading = false
            }
        }
    }
}

@Composable
fun ColeccionesScreen(
    onHome: () -> Unit,
    onConsulta: () -> Unit,
    onHemeroteca: () -> Unit,
    onDatos: () -> Unit,
    onFavoritos: () -> Unit = {},
    onBuscar: (String) -> Unit = {},
    onOpenPath: (String, Int, String, String?) -> Unit = { _, _, _, _ -> },
) {
    val vm: ColeccionesViewModel = viewModel()
    val context = LocalContext.current
    val store = androidx.compose.runtime.remember(context) { LocalStore(context) }
    val rootGrid = rememberLazyGridState()
    val childrenGrid = rememberLazyGridState()
    val resultsGrid = rememberLazyGridState()
    val detailList = rememberLazyListState()
    LaunchedEffect(vm.browseGeneration) { resultsGrid.scrollToItem(0) }
    LaunchedEffect(vm.parent?.code) { childrenGrid.scrollToItem(0) }
    LaunchedEffect(vm.openedWork?.recordUrl, vm.openedWork?.path) { detailList.scrollToItem(0) }
    var expandedPhoto by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    expandedPhoto?.let { image ->
        Dialog(onDismissRequest = { expandedPhoto = null }) {
            var scale by androidx.compose.runtime.remember(image) { mutableFloatStateOf(1f) }
            val gesture = rememberTransformableState { zoom, _, _ -> scale = (scale * zoom).coerceIn(1f, 6f) }
            Column {
                TextButton(onClick = { expandedPhoto = null }) { Text("Cerrar imagen") }
                AsyncImage(model = image, contentDescription = vm.openedWork?.title ?: "Imagen ampliada",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale).transformable(gesture),
                    contentScale = ContentScale.Fit)
            }
        }
    }
    fun back() { if (!vm.back()) onHome() }
    fun openCopy(path: String, title: String, parent: String? = null) {
        store.rememberWork(path, title, parent)
        onOpenPath(path, 1, "", null)
    }
    BackHandler(enabled = vm.vista != "raiz") { back() }
    val screenTitle = when (vm.vista) {
        "ficha" -> vm.openedWork?.title.orEmpty().ifBlank { "Ficha del catálogo" }
        "hijos" -> vm.parent?.title.orEmpty()
        "resultados" -> vm.selected?.title.orEmpty()
        else -> "Colecciones"
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SectionBar(screenTitle, MaterialTheme.colorScheme.primary, onBack = { back() }, onHome = onHome)
            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            vm.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                TextButton(onClick = { vm.retry() }, enabled = !vm.loading,
                    modifier = Modifier.padding(horizontal = 12.dp)) { Text("Reintentar") }
            }
            when (vm.vista) {
                "ficha" -> LazyColumn(Modifier.weight(1f), state = detailList) {
                    item {
                        Text(vm.openedWork?.title.orEmpty(), style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(20.dp))
                        val copy = vm.detail?.path ?: vm.openedWork?.path
                        if (copy != null && vm.detail?.publicationId == null) {
                            Button(onClick = { openCopy(copy, vm.openedWork?.title.orEmpty()) },
                                modifier = Modifier.padding(horizontal = 20.dp).heightIn(min = 48.dp)) { Text("Abrir copia digital") }
                        }
                        vm.detail?.let { CatalogueProvenance(it, Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                        if (vm.years.isNotEmpty()) Box(Modifier.padding(horizontal = 12.dp)) {
                            PublicationYearSelector(vm.years, vm.year, enabled = !vm.loading, onSelect = vm::selectYear)
                        }
                    }
                    listItems(vm.issues, key = { it.path }) { issue ->
                        Text(issue.label.ifBlank { "Abrir número digitalizado" },
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) {
                                openCopy(issue.path, listOf(vm.openedWork?.title.orEmpty(), issue.label).filter { it.isNotBlank() }.joinToString(" · "), vm.openedWork?.title)
                            }.padding(20.dp))
                    }
                    vm.detail?.imageUrl?.let { image -> item {
                        Text("Imagen digitalizada · toca para ampliar", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        AsyncImage(model = image, contentDescription = vm.openedWork?.title,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(20.dp)
                                .clickable(role = Role.Button) { expandedPhoto = image }, contentScale = ContentScale.Fit)
                    } }
                    vm.detail?.let { data -> item { CatalogueFields(data, Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) } }
                    if (!vm.loading && vm.error == null && vm.detail != null && vm.issues.isEmpty()) item {
                        val hasCopy = vm.detail?.path != null || vm.openedWork?.path != null || vm.detail?.imageUrl != null
                        if (!hasCopy) Text(if (vm.detail?.publicationId != null) "No hay números digitalizados disponibles en este año."
                            else "Esta ficha no tiene copia digital disponible.", modifier = Modifier.padding(20.dp))
                    }
                }
                "hijos" -> LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 156.dp), state = childrenGrid,
                    modifier = Modifier.weight(1f).padding(8.dp)) {
                    vm.parent?.children?.let { children -> items(children) { section -> Tile(section.title, section.imageId) { vm.open(section) } } }
                    item(span = { GridItemSpan(maxLineSpan) }) { vm.parent?.let { parent ->
                        TextButton(onClick = { vm.open(parent) }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Consultar todo: ${parent.title}") }
                    } }
                }
                "resultados" -> {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        gal.galiciana.app.ui.ClearableField(value = vm.q, onValueChange = { vm.q = it },
                            placeholder = "Buscar en esta colección", modifier = Modifier.weight(1f), onSearch = { vm.search() })
                        IconButton(onClick = { vm.search() }, enabled = !vm.loading) { Icon(Icons.Filled.Search, "Buscar en esta colección") }
                    }
                    Text("${vm.works.size} obras cargadas" + if (vm.appliedQuery.isNotBlank()) " · ${vm.appliedQuery}" else "",
                        modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
                    if (vm.selected?.code == "FOTOS") Text("Este conjunto reúne fotografías, ilustraciones y carteles de los portales de Galiciana.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                    if (!vm.loading && vm.error == null && vm.works.isEmpty()) Text("No hay obras con estos criterios. Prueba otra palabra o borra la búsqueda.",
                        modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 156.dp), state = resultsGrid,
                        modifier = Modifier.weight(1f).padding(8.dp)) {
                        items(vm.works, key = { it.path ?: it.recordUrl ?: it.title }) { work ->
                            if (vm.selected?.code == "FOTOS" && work.thumbnailUrl == null) LaunchedEffect(work.recordUrl) { vm.preview(work) }
                            Column(Modifier.padding(8.dp).clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = work.path != null || work.recordUrl != null, role = Role.Button) { vm.openWork(work) }) {
                                Box(Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center) {
                                    if (work.thumbnailUrl != null) AsyncImage(model = work.thumbnailUrl, contentDescription = null,
                                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Sin miniatura", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(work.title, style = MaterialTheme.typography.titleSmall, minLines = 3, maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                                listOfNotNull(work.creator, work.date).takeIf { it.isNotEmpty() }?.let { lines ->
                                    Text(lines.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(if (work.path != null || work.recordUrl != null) "Ver ficha del catálogo" else "Sin ficha disponible",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                            }
                        }
                        if (vm.nextUrl != null) item(span = { GridItemSpan(maxLineSpan) }) {
                            TextButton(onClick = { vm.more() }, enabled = !vm.loading,
                                modifier = Modifier.fillMaxWidth().padding(12.dp)) { Text("Cargar más obras") }
                        }
                    }
                }
                else -> LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 156.dp), state = rootGrid,
                    modifier = Modifier.weight(1f).padding(8.dp)) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Fondos por tipo de documento", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(12.dp))
                    }
                    items(Secciones) { section -> Tile(section.title, section.imageId) {
                        if (section.children.isEmpty()) vm.open(section) else { vm.parent = section; vm.vista = "hijos" }
                    } }
                }
            }
        }
    }
}

@Composable
private fun Tile(title: String, imageId: String, onClick: () -> Unit) {
    Column(Modifier.padding(8.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).clickable(onClick = onClick)) {
        AsyncImage(model = GalicianaUrls.thumb(imageId), contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(118.dp), contentScale = ContentScale.Crop)
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp))
    }
}

private fun CatalogueField.isRights(): Boolean = Regex("derech|dereit|rights|licencia", RegexOption.IGNORE_CASE).containsMatchIn(label)

@Composable
internal fun CatalogueProvenance(detail: CollectionDetail, modifier: Modifier = Modifier) {
    val browser = LocalUriHandler.current
    Column(modifier.fillMaxWidth()) {
        val rights = detail.metadata.filter { it.isRights() }
        rights.forEach { field ->
            Text(field.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            SelectionContainer { Text(field.value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }
        }
        if (rights.isEmpty()) Text("Consulta las condiciones de uso en el registro original.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        detail.sourceUrl?.let { url ->
            TextButton(onClick = { browser.openUri(url) }) { Text("Registro original · abre el navegador") }
        }
    }
}

@Composable
internal fun CatalogueFields(detail: CollectionDetail, modifier: Modifier = Modifier) {
    var originalExpanded by rememberSaveable(detail.sourceUrl) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        val fields = detail.metadata.filterNot { it.isRights() }
        if (fields.isNotEmpty()) Text("Datos del catálogo", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp))
        fields.forEach { field ->
            var expanded by rememberSaveable(detail.sourceUrl, field.label, field.value) { mutableStateOf(false) }
            val long = field.value.length > 500 || field.value.count { it == '\n' } > 5
            Text(field.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            SelectionContainer { Text(field.value, style = MaterialTheme.typography.bodyMedium,
                maxLines = if (long && !expanded) 5 else Int.MAX_VALUE,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }
            if (long) TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Mostrar menos" else "Leer campo completo: ${field.label}")
            }
            androidx.compose.material3.HorizontalDivider(Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (detail.description.isNotBlank()) {
            TextButton(onClick = { originalExpanded = !originalExpanded }) {
                Text(if (originalExpanded) "Ocultar texto completo de la ficha" else "Texto completo de la ficha")
            }
            if (originalExpanded) SelectionContainer { Text(detail.description, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
