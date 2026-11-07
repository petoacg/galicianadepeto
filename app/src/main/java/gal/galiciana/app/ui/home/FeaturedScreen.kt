package gal.galiciana.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gal.galiciana.app.data.*
import gal.galiciana.app.ui.*
import gal.galiciana.app.ui.colecciones.CatalogueFields
import gal.galiciana.app.ui.colecciones.CatalogueProvenance
import gal.galiciana.app.ui.colecciones.CollectionDetail
import gal.galiciana.app.ui.colecciones.CollectionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun FeaturedScreen(recordId: String, onBack: () -> Unit, onHome: () -> Unit,
    onOpenPath: (String) -> Unit, seedTitle: String = "") {
    val catalogue = remember { CollectionRepository() }
    val hemeroteca = remember { HemerotecaRepository() }
    val ctx = LocalContext.current
    val store = remember { LocalStore(ctx) }
    var detail by remember(recordId) { mutableStateOf<CollectionDetail?>(null) }
    var years by remember(recordId) { mutableStateOf(emptyList<String>()) }
    var year by rememberSaveable(recordId) { mutableStateOf<String?>(null) }
    var issues by remember(recordId) { mutableStateOf(emptyList<Numero>()) }
    var loading by remember(recordId) { mutableStateOf(true) }
    var error by remember(recordId) { mutableStateOf<String?>(null) }
    var retry by remember(recordId) { mutableIntStateOf(0) }
    var detailsExpanded by rememberSaveable(recordId) { mutableStateOf(false) }
    val title = seedTitle.ifBlank {
        detail?.metadata?.firstOrNull { it.label.equals("Título", true) || it.label.equals("Titulo", true) }?.value
            ?: "Ficha de la publicación"
    }
    fun open(path: String, issue: String? = null) {
        store.rememberWork(path, issue?.let { "$title · $it" } ?: title, title)
        onOpenPath(path)
    }
    LaunchedEffect(recordId, retry) {
        loading = true; error = null
        try {
            val item = catalogue.detail(GalicianaUrls.record(recordId)); detail = item
            if (item.publicationId != null) {
                years = hemeroteca.yearsOf(item.publicationId)
                year = year?.takeIf { it in years } ?: years.firstOrNull()
                issues = hemeroteca.loadNumeros(item.publicationId, year)
            }
            // A catalogue record stays a record. Opening its document is explicit.
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "No se pudo cargar esta ficha. Comprueba la conexión y reintenta." }
        finally { loading = false }
    }
    val scope = rememberCoroutineScope()
    var issueTask by remember { mutableStateOf<Job?>(null) }
    fun selectYear(value: String) {
        val id = detail?.publicationId ?: return
        issueTask?.cancel(); year = value; issues = emptyList()
        issueTask = scope.launch {
            loading = true; error = null
            try { issues = hemeroteca.loadNumeros(id, value) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "No se pudieron cargar los números de ese año. Vuelve a intentarlo." }
            finally { loading = false }
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        SectionBar("Ficha de la publicación", MaterialTheme.colorScheme.primary, onBack, onHome)
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 16.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp))
                    TextButton(onClick = { retry++ }) { Text("Reintentar") } }
                detail?.let { CatalogueProvenance(it, Modifier.padding(vertical = 12.dp)) }
                if (years.isNotEmpty()) PublicationYearSelector(years, year, !loading) { value ->
                    selectYear(value)
                }
                if (issues.isNotEmpty()) Text("${issues.size} números disponibles · ${year.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp))
                if (detail?.publicationId == null && detail?.path != null) Button(onClick = { detail?.path?.let { open(it) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) { Text("Abrir documento") }
            }
            items(issues, key = { it.path }) { number ->
                ListItem(headlineContent = { Text(number.label) }, modifier = Modifier.clickable { open(number.path, number.label) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            detail?.let { data -> item { CatalogueFields(data, Modifier.padding(vertical = 20.dp)) } }
            if (!loading && error == null && detail?.path == null && issues.isEmpty()) item {
                Text("Esta ficha no ofrece una copia digital consultable desde la app.", modifier = Modifier.padding(vertical = 20.dp))
            }
        }
    }
}
