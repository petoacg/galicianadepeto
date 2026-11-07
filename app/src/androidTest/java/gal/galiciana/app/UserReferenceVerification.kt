package gal.galiciana.app

import android.content.Context
import gal.galiciana.app.data.*
import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.ui.countMatches
import gal.galiciana.app.ui.highlightRegex
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

internal suspend fun verifyUserReferencePage(context: Context, pathOverride: String?, positionOverride: Int?, progress: (String) -> Unit): String {
    val hemeroteca = HemerotecaRepository()
    progress("Localizando El Pueblo Gallego, 01/07/1975")
    val headers = if (pathOverride == null) hemeroteca.searchIndice("El pueblo gallego").cabeceras else emptyList()
    progress("Cabeceras: ${headers.map { it.title }}")
    var path: String? = pathOverride
    for (header in headers.take(4)) {
        val issue = hemeroteca.loadNumeros(header.id, "1975")
            .firstOrNull { it.date == LocalDate.of(1975, 7, 1) }
        if (issue != null) { path = issue.path; break }
    }
    val selected = path ?: error("No se localizó el número exacto del 01/07/1975")
    val metsCall = Http.client.newCall(Request.Builder().url(GalicianaUrls.mets(selected)).build())
    metsCall.timeout().timeout(20, TimeUnit.SECONDS)
    val mets = metsCall.execute().use { check(it.isSuccessful); it.body?.string().orEmpty() }
    context.openFileOutput("reference-page-mets.xml", 0).use { it.write(mets.toByteArray()) }
    val document = MetsRepository().parse(selected, mets)
    var position = positionOverride ?: 15
    var page = document.pages.first { it.position == position }
    val repo = OcrRepository()
    fun fetchXml(pos: Int): String {
        val request = Http.client.newCall(Request.Builder().url(GalicianaUrls.alto(selected, pos))
            .header("User-Agent", "Mozilla/5.0").header("Accept", "application/xml,text/xml,*/*").build())
        request.timeout().timeout(20, TimeUnit.SECONDS)
        return request.execute().use { check(it.isSuccessful); it.body?.string().orEmpty() }
    }
    fun positionedText(xml: String): String {
        val parsed = org.jsoup.Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())
        check(parsed.getAllElements().any { it.tagName().substringAfter(':').equals("alto", true) })
        return parsed.getAllElements().filter { it.tagName().substringAfter(':').equals("String", true) }
            .joinToString(" ") { word ->
                when (word.attr("SUBS_TYPE")) {
                    "HypPart1" -> word.attr("SUBS_CONTENT").ifBlank { word.attr("CONTENT") }
                    "HypPart2" -> ""
                    else -> word.attr("CONTENT")
                }
            }
    }
    var provisionalXml: String? = null
    var rawTextAvailable = true
    var text = try { repo.loadPage(selected, position, page.imageId).ocrText } catch (_: Exception) {
        progress("Texto plano no disponible; contrastando los titulares del ALTO")
        rawTextAvailable = false
        try {
            provisionalXml = fetchXml(position)
            positionedText(provisionalXml!!)
        } catch (_: Exception) { provisionalXml = null; "" }
    }
    fun matchesImage(value: String) = value.contains("FINALIZARON", true) && value.contains("BILLAR", true)
    if (!matchesImage(text)) {
        for (candidate in document.pages.sortedBy { kotlin.math.abs(it.position - position) }.filter { it.position != position }.take(5)) {
            progress("Contrastando titulares en posición ${candidate.position}")
            val candidateText = try { repo.loadPage(selected, candidate.position, candidate.imageId).ocrText }
                catch (_: Exception) { continue }
            if (matchesImage(candidateText)) {
                page = candidate
                position = candidate.position
                text = candidateText
                provisionalXml = null
                rawTextAvailable = true
                break
            }
        }
    }
    check(matchesImage(text)) { "No se encontró la página de la imagen entre las posiciones próximas" }
    progress("Página de la imagen confirmada por titulares: $selected, posición $position, imagen ${page.imageId}")
    val xml = provisionalXml ?: fetchXml(position)
    context.openFileOutput("reference-page-ocr.txt", 0).use { it.write(text.toByteArray()) }
    context.openFileOutput("reference-page-alto.xml", 0).use { it.write(xml.toByteArray()) }
    val queries = listOf("Federación Gallega", "Elisardo Núñez Seabra", "Manuel Fernández", "Amador",
        "Regimiento de Infantería Zamora", "Xinzo de Limia", "Javier Vázquez")
    val results = JSONArray()
    for (query in queries) {
        val positions = repo.parseAltoSearch(xml, "\"$query\"")
        val spans = highlightRegex("\"$query\"")?.findAll(text)?.map { it.value }?.toList().orEmpty()
        results.put(JSONObject().put("query", query).put("textCount", countMatches(text, "\"$query\""))
            .put("imageMatches", positions.matches.size).put("boxes", positions.hits.size)
            .put("status", positions.status.name).put("textSpans", JSONArray(spans)))
        progress("$query: texto=${countMatches(text, "\"$query\"")}, imagen=${positions.matches.size}, cajas=${positions.hits.size}")
    }
    LocalStore(context).rememberReading(OcrHit(document.title.orEmpty(), path = selected, position = position,
        imageId = page.imageId, savedQuery = "\"Federación Gallega\""))
    val report = JSONObject().put("title", document.title).put("path", selected).put("position", position)
        .put("textSource", if (rawTextAvailable) "plainOCR" else "ALTO-derived")
        .put("imageId", page.imageId).put("results", results).toString(2)
    context.openFileOutput("reference-page-report.json", 0).use { it.write(report.toByteArray()) }
    return "\n$report\n"
}
