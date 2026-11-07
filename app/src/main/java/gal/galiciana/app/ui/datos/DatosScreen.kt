package gal.galiciana.app.ui.datos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import gal.galiciana.app.data.LocalStore


import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import gal.galiciana.app.ui.MatteShape
import gal.galiciana.app.ui.matteFieldColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import gal.galiciana.app.data.GalicianaUrls
import gal.galiciana.app.data.Http
import gal.galiciana.app.data.OaiRepository
import gal.galiciana.app.domain.Work
import gal.galiciana.app.ui.SectionBar
import gal.galiciana.app.ui.SectionDatos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

@Composable
fun DatosScreen(
    onHome: () -> Unit,
    onConsulta: () -> Unit,
    onHemeroteca: () -> Unit,
    onFavoritos: () -> Unit = {},
    onOpenPath: (String) -> Unit = {},
    onBuscarTitle: (String) -> Unit = {},
) {
    val oai = remember { OaiRepository() }
    val ctx = LocalContext.current
    val store = remember { LocalStore(ctx) }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf("explorar") }
    var works by remember { mutableStateOf<List<Work>>(emptyList()) }
    var openedState by rememberSaveable { mutableStateOf("") }
    val opened = remember(openedState) { restoreWork(openedState) }
    var metadataExpanded by rememberSaveable { mutableStateOf(false) }
    var sparqlQ by rememberSaveable { mutableStateOf("") }
    var sparqlOut by remember { mutableStateOf("") }
    var sparqlApplied by rememberSaveable { mutableStateOf("") }
    var subjectApplied by rememberSaveable { mutableStateOf("") }
    var subjectsReady by remember { mutableStateOf(false) }
    var titlesReady by remember { mutableStateOf(false) }
    var recordsReady by remember { mutableStateOf(false) }
    var subjectQuery by rememberSaveable { mutableStateOf("") }
    var subjects by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var facet by rememberSaveable { mutableStateOf("materias") }
    var selectedConcept by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var relatedWorks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detailTask by remember { mutableStateOf<Job?>(null) }
    var detailGeneration by remember { mutableIntStateOf(0) }
    var generalTask by remember { mutableStateOf<Job?>(null) }
    var generalGeneration by remember { mutableIntStateOf(0) }

    fun back() {
        when {
            opened != null -> { openedState = ""; error = null }
            selectedConcept != null -> {
                detailGeneration++
                detailTask?.cancel()
                selectedConcept = null
                loading = false
                error = null
            }
            else -> onHome()
        }
    }
    BackHandler(enabled = opened != null || selectedConcept != null) { back() }

    fun loadOai() {
        val request = ++generalGeneration
        generalTask?.cancel()
        generalTask = scope.launch {
            loading = true
            error = null
            try { works = oai.firstPage(); recordsReady = true }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "No se pudieron cargar los registros del catálogo. Vuelve a intentarlo." }
            finally { if (generalGeneration == request) loading = false }
        }
    }

    fun moreOai() {
        if (loading) return
        val request = ++generalGeneration
        generalTask?.cancel()
        generalTask = scope.launch {
            loading = true; error = null
            try { works = (works + oai.nextPage()).distinctBy { it.oaiId }; recordsReady = true }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "No se pudieron cargar los registros del catálogo. Vuelve a intentarlo." }
            finally { if (generalGeneration == request) loading = false }
        }
    }

    fun runSparql(restoredQuery: String? = null) {
        val needle = (restoredQuery ?: sparqlQ).trim().ifBlank { return }
        sparqlApplied = needle
        sparqlOut = ""; titlesReady = false
        val request = ++generalGeneration
        generalTask?.cancel()
        generalTask = scope.launch {
            loading = true
            error = null
            try { sparqlOut = withContext(Dispatchers.IO) {
                val q = """
                    PREFIX dc: <http://purl.org/dc/elements/1.1/>
                    SELECT ?title WHERE {
                      ?s dc:title ?title .
                      FILTER(CONTAINS(LCASE(STR(?title)), "${needle.lowercase().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")}"))
                    } LIMIT 25
                """.trimIndent()
                val raw = requestSparql(q)
                val rows = JSONObject(raw).getJSONObject("results").getJSONArray("bindings")
                (0 until rows.length()).mapNotNull { index ->
                    rows.getJSONObject(index).optJSONObject("title")?.optString("value")
                        ?.takeIf { it.isNotBlank() }
                }.distinct().joinToString("\n")
            } } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudieron consultar los títulos del catálogo. Vuelve a intentarlo." }
            finally { if (generalGeneration == request) { loading = false; if (error == null) titlesReady = true } }
        }
    }

    fun searchSubjects(restoredQuery: String? = null) {
        val needle = (restoredQuery ?: subjectQuery).trim()
        if (needle.isBlank()) return
        subjectApplied = needle
        subjectsReady = false
        val request = ++detailGeneration
        detailTask?.cancel()
        detailTask = scope.launch {
            loading = true
            error = null
            subjects = emptyList()
            selectedConcept = null
            relatedWorks = emptyList()
            try {
                subjects = withContext(Dispatchers.IO) {
                    val escaped = needle.lowercase().replace("\\", "\\\\")
                        .replace("\"", "\\\"").replace("\n", " ")
                    val relation = when (facet) {
                        "personas" -> "dc:creator"
                        "lugares" -> "dcterms:spatial"
                        else -> "dc:subject"
                    }
                    val query = """
                        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
                        PREFIX dc: <http://purl.org/dc/elements/1.1/>
                        PREFIX dcterms: <http://purl.org/dc/terms/>
                        SELECT DISTINCT ?concept ?label WHERE {
                          ?concept skos:prefLabel ?label .
                          ?work $relation ?concept .
                          FILTER(CONTAINS(LCASE(STR(?label)), "$escaped"))
                        } LIMIT 50
                    """.trimIndent()
                    val raw = requestSparql(query)
                    val rows = JSONObject(raw).getJSONObject("results").getJSONArray("bindings")
                    (0 until rows.length()).mapNotNull { index ->
                        val row = rows.getJSONObject(index)
                        val label = row.optJSONObject("label")?.optString("value").orEmpty()
                        val uri = row.optJSONObject("concept")?.optString("value").orEmpty()
                        if (label.isBlank() || uri.isBlank()) null else label to uri
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo consultar este índice. Vuelve a intentarlo." }
            finally { if (detailGeneration == request) { loading = false; if (error == null) subjectsReady = true } }
        }
    }

    fun openConcept(concept: Pair<String, String>) {
        val uri = concept.second
        if (!uri.matches(Regex("https?://[^<>\\s]+"))) return
        selectedConcept = concept
        relatedWorks = emptyList()
        val request = ++detailGeneration
        detailTask?.cancel()
        detailTask = scope.launch {
            loading = true
            error = null
            try {
                relatedWorks = withContext(Dispatchers.IO) {
                    val relation = when (facet) {
                        "personas" -> "dc:creator"
                        "lugares" -> "dcterms:spatial"
                        else -> "dc:subject"
                    }
                    val query = """
                        PREFIX dc: <http://purl.org/dc/elements/1.1/>
                        PREFIX dcterms: <http://purl.org/dc/terms/>
                        SELECT DISTINCT ?work ?title WHERE {
                          ?work $relation <$uri>; dc:title ?title .
                        } LIMIT 100
                    """.trimIndent()
                    val raw = requestSparql(query)
                    val rows = JSONObject(raw).getJSONObject("results").getJSONArray("bindings")
                    (0 until rows.length()).mapNotNull { index ->
                        val row = rows.getJSONObject(index)
                        val title = row.optJSONObject("title")?.optString("value").orEmpty()
                        val id = row.optJSONObject("work")?.optString("value").orEmpty()
                        if (title.isBlank()) null else title to id
                    }.distinctBy { it.second }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudieron cargar las obras relacionadas. Vuelve a intentarlo." }
            finally { if (detailGeneration == request) loading = false }
        }
    }

    LaunchedEffect(tab, openedState) {
        if (opened == null) when (tab) {
            "oai" -> if (!recordsReady && works.isEmpty()) loadOai()
            "explorar" -> when {
                selectedConcept != null -> openConcept(selectedConcept!!)
                !subjectsReady && subjectApplied.isNotBlank() -> searchSubjects(subjectApplied)
            }
            "obras" -> if (!titlesReady && sparqlApplied.isNotBlank()) runSparql(sparqlApplied)
        }
    }
    fun retryCurrent() {
        if (selectedConcept != null) openConcept(selectedConcept!!)
        else when (tab) { "explorar" -> searchSubjects(subjectApplied.takeIf { it.isNotBlank() }); "obras" -> runSparql(sparqlApplied.takeIf { it.isNotBlank() })
            "oai" -> if (works.isEmpty()) loadOai() else moreOai() }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        SectionBar(if (opened != null) "Ficha del registro" else if (selectedConcept != null) "Obras relacionadas" else "Materias y datos del catálogo",
            MaterialTheme.colorScheme.primary, { back() }, onHome)
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            if (opened == null && selectedConcept == null) Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text("Explora las descripciones de las obras por materia, persona o lugar. El texto de las páginas se busca en Buscar.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                RowTabs(tab) {
                    detailGeneration++; detailTask?.cancel(); generalGeneration++; generalTask?.cancel()
                    loading = false; selectedConcept = null; error = null; tab = it
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { message ->
                Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retryCurrent() }, enabled = !loading) { Text("Reintentar") }
                }
            }
            if (opened != null) {
                val work = opened
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp)) {
                    item {
                        Text(work.title, style = MaterialTheme.typography.headlineSmall)
                        work.creators.forEach { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                        work.date?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
                        work.path?.let { path -> Button(onClick = {
                            store.rememberWork(path, work.title)
                            onOpenPath(path)
                        }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Abrir documento") } }
                        work.recordUrl?.takeIf { it.startsWith("${GalicianaUrls.BASE}/") }?.let { url ->
                            TextButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("Ficha original · abre el navegador")
                            }
                        }
                        work.rights?.let { Text("Derechos: $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 20.dp)) }
                        TextButton(onClick = { metadataExpanded = !metadataExpanded }) { Text(if (metadataExpanded) "Ocultar identificadores" else "Identificadores del registro") }
                        if (metadataExpanded) {
                            Text("OAI: ${work.oaiId}", style = MaterialTheme.typography.bodySmall)
                            work.catalogId?.let { Text("Catálogo: $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            } else when (tab) {
                "explorar" -> {
                    if (selectedConcept == null) {
                        Column(Modifier.padding(horizontal = 24.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("materias" to "Materias", "personas" to "Personas", "lugares" to "Lugares")) { (id, label) ->
                                    FilterChip(selected = facet == id, onClick = {
                                        detailGeneration++; detailTask?.cancel(); loading = false; error = null
                                        facet = id; subjects = emptyList(); subjectApplied = ""; subjectsReady = false
                                    }, label = { Text(label) })
                                }
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                gal.galiciana.app.ui.ClearableField(subjectQuery, { subjectQuery = it }, "Buscar $facet",
                                    Modifier.weight(1f), onSearch = { searchSubjects() })
                                FilledTonalButton(onClick = { searchSubjects() }, enabled = !loading && subjectQuery.isNotBlank()) { Text("Buscar") }
                            }
                            if (subjectApplied.isNotBlank()) Text("Resultados de «$subjectApplied»", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                            if (!loading && subjectsReady && subjects.isEmpty() && error == null && subjectApplied.isNotBlank()) item {
                                Text("No se encontraron entradas en este índice. Prueba otro nombre o término.", Modifier.padding(vertical = 16.dp))
                            }
                            items(subjects, key = { it.second }) { concept ->
                                ListItem(headlineContent = { Text(concept.first) }, supportingContent = { Text("Ver obras relacionadas") },
                                    modifier = Modifier.clickable { openConcept(concept) })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp)) {
                            item {
                                Text(selectedConcept!!.first, style = MaterialTheme.typography.headlineSmall)
                                Text("Estos títulos permiten continuar en la búsqueda general del catálogo.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
                            }
                            items(relatedWorks, key = { it.second.ifBlank { it.first } }) { (title, _) ->
                                ListItem(headlineContent = { Text(title) }, supportingContent = { Text("Buscar este título en el catálogo") },
                                    modifier = Modifier.clickable { onBuscarTitle(title) })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            if (!loading && error == null && relatedWorks.isEmpty()) item { Text("No se encontraron obras relacionadas.") }
                        }
                    }
                }
                "obras" -> {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            gal.galiciana.app.ui.ClearableField(sparqlQ, { sparqlQ = it }, "Palabras del título", Modifier.weight(1f), onSearch = { runSparql() })
                            FilledTonalButton(onClick = { runSparql() }, enabled = !loading && sparqlQ.isNotBlank()) { Text("Buscar") }
                        }
                        if (sparqlApplied.isNotBlank()) Text("Títulos que contienen «$sparqlApplied» · hasta 25 resultados",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 24.dp)) {
                        val titles = sparqlOut.lines().filter { it.isNotBlank() }
                        if (!loading && titlesReady && error == null && sparqlApplied.isNotBlank() && titles.isEmpty()) item { Text("No se encontraron títulos con esas palabras.", Modifier.padding(vertical = 16.dp)) }
                        items(titles) { title ->
                            ListItem(headlineContent = { Text(title) }, supportingContent = { Text("Buscar este título en el catálogo") },
                                modifier = Modifier.clickable { onBuscarTitle(title) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                "fuentes" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text("Fuentes y formatos", style = MaterialTheme.typography.headlineSmall) }
                    item { Text("Los índices enlazan descripciones del catálogo. OAI permite consultar registros y SPARQL consultar sus relaciones. Estas herramientas no buscan el texto OCR de las páginas.") }
                    item { TextButton(onClick = { uriHandler.openUri("${GalicianaUrls.BASE}/es/datos_abiertos/datos_abiertos.do") }) { Text("Datos abiertos de Galiciana · navegador") } }
                    item { TextButton(onClick = { uriHandler.openUri("https://datos-abertos.galiciana.gal/arpad-bibdix-lod/es/sparql") }) { Text("Consulta SPARQL avanzada · navegador") } }
                }
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp)) {
                    item { Text("Registros del catálogo (OAI)", style = MaterialTheme.typography.titleMedium)
                        Text("Consulta técnica de metadatos; no es una lista de resultados OCR.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) }
                    if (!loading && error == null && recordsReady && works.isEmpty()) item { Text("El servicio no devolvió registros para esta consulta.", Modifier.padding(vertical = 16.dp)) }
                    items(works, key = { it.oaiId }) { work ->
                        ListItem(headlineContent = { Text(work.title) }, supportingContent = { Text(listOfNotNull(work.date, work.creators.firstOrNull()).joinToString(" · ")) },
                            modifier = Modifier.clickable { openedState = saveWork(work); metadataExpanded = false; error = null })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (oai.nextToken != null) item { TextButton(onClick = { moreOai() }, enabled = !loading) { Text("Cargar más registros") } }
                }
            }
        }
    }
}

@Composable
private fun RowTabs(current: String, onPick: (String) -> Unit) {
    var more by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        FilterChip(selected = current == "explorar", onClick = { onPick("explorar") }, label = { Text("Índices") })
        FilterChip(selected = current == "obras", onClick = { onPick("obras") }, label = { Text("Por título") })
        Box {
            TextButton(onClick = { more = true }) { Text(when (current) { "oai" -> "Registros OAI"; "fuentes" -> "Fuentes"; else -> "Más opciones" }) }
            DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                DropdownMenuItem(text = { Text("Registros del catálogo (OAI)") }, onClick = { more = false; onPick("oai") })
                DropdownMenuItem(text = { Text("Fuentes y formatos") }, onClick = { more = false; onPick("fuentes") })
            }
        }
    }
}

private fun saveWork(work: Work): String = JSONObject().apply {
    put("oaiId", work.oaiId); put("catalogId", work.catalogId); put("path", work.path); put("title", work.title)
    put("creators", org.json.JSONArray(work.creators)); put("date", work.date); put("rights", work.rights)
    put("recordUrl", work.recordUrl); put("viewerUrl", work.viewerUrl)
}.toString()

private fun restoreWork(raw: String): Work? = runCatching {
    if (raw.isBlank()) return@runCatching null
    val o = JSONObject(raw)
    fun field(name: String) = o.optString(name).takeIf { it.isNotBlank() && it != "null" }
    val creators = o.optJSONArray("creators") ?: org.json.JSONArray()
    Work(o.getString("oaiId"), field("catalogId"), field("path"), o.getString("title"),
        (0 until creators.length()).map(creators::getString), field("date"), field("rights"), field("recordUrl"), field("viewerUrl"))
}.getOrNull()

private fun requestSparql(query: String): String {
    val url = "https://datos-abertos.galiciana.gal/arpad-bibdix-lod/es/sparql".toHttpUrl().newBuilder()
        .addQueryParameter("query", query)
        .addQueryParameter("format", "application/sparql-results+json")
        .build()
    val request = Request.Builder().url(url)
        .header("Accept", "application/sparql-results+json, application/json")
        .build()
    return Http.client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) error("Galiciana Datos Abiertos: HTTP ${response.code}")
        val raw = response.body?.string().orEmpty()
        if (!raw.trimStart().startsWith("{"))
            error("Galiciana Datos Abiertos devolvió una página web en lugar de resultados. Inténtalo más tarde.")
        raw
    }
}
