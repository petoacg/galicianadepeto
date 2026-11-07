package gal.galiciana.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gal.galiciana.app.R
import org.json.JSONArray

@Composable
fun AboutScreen(onBack: () -> Unit, onLegal: () -> Unit, onBackup: () -> Unit = {}) {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() }
    var otherSources by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        SectionBar("Acerca de", MaterialTheme.colorScheme.primary, onBack, onBack)
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Galiciana, de bolsillo.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                version?.let { Text("Versión $it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
            }
            item { Text("Una aplicación independiente para consultar documentos y organizar tus hallazgos. No es una aplicación oficial de la Xunta de Galicia.",
                style = MaterialTheme.typography.bodyLarge) }
            item { Button(onClick = onLegal, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Aviso legal y licencia") } }
            item { OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Copia de tus datos") } }
            item { HorizontalDivider(); Text("Fuentes de los documentos y datos", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp)) }
            item { ExternalSource("Biblioteca Dixital de Galicia", "https://biblioteca.galiciana.gal") }
            item { ExternalSource("Galiciana Patrimonio Dixital", "https://www.galiciana.gal/arpadhub/es/inicio/inicio.do") }
            item { ExternalSource("Datos abiertos de Galiciana", "https://datos-abertos.galiciana.gal/arpad-bibdix-lod/") }
            item { Text("Estos enlaces se abren en tu navegador.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { TextButton(onClick = { otherSources = !otherSources }) { Text(if (otherSources) "Ocultar otros recursos" else "Otros recursos de investigación") } }
            if (otherSources) {
                item { ExternalSource("Galiciana Arquivo Dixital", "https://arquivo.galiciana.gal/arpadweb/es/inicio/inicio.do") }
                item { ExternalSource("Memoria e Concordia", "https://memoriaeconcordia.galiciana.gal/es") }
                item { ExternalSource("Prensa histórica galega", "https://prensahistorica.galiciana.gal/es") }
                item { ExternalSource("Hispana", "https://hispana.mcu.es") }
                item { ExternalSource("Europeana", "https://www.europeana.eu") }
                item { ExternalSource("Digital Public Library of America", "https://dp.la") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val paragraphs = remember { runCatching {
        val data = JSONArray(ctx.assets.open("legal_notice.json").bufferedReader().use { it.readText() })
        (0 until data.length()).map(data::getString)
    }.getOrDefault(emptyList()) }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        TopAppBar(title = { Text("Aviso legal", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (paragraphs.isEmpty()) item { Text("No se pudo cargar el aviso legal.") }
            itemsIndexed(paragraphs) { i, paragraph ->
                val heading = i < 2 || paragraph.matches(Regex("^[1-4]\\. .+"))
                SelectionContainer {
                    Text(paragraph, style = when { i == 0 -> MaterialTheme.typography.headlineSmall
                        heading -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.bodyLarge },
                        fontWeight = if (heading) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            item { ExternalSource("Consultar la licencia CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/deed.es") }
            item { ExternalSource("Aviso legal de Galiciana", "https://biblioteca.galiciana.gal/es/contenido/avisolegal.do") }
        }
    }
}

@Composable
private fun ExternalSource(title: String, url: String) {
    val uriHandler = LocalUriHandler.current
    TextButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        contentPadding = PaddingValues(vertical = 8.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Abrir en el navegador", Modifier.size(20.dp))
    }
}
