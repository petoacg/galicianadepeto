package gal.galiciana.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.Normalizer
import java.time.LocalDate
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class Cabecera(
    val id: String,
    val title: String,
)

data class Numero(
    val label: String,
    val path: String,
    val date: LocalDate? = null,
    val month: Int? = date?.monthValue,
)

data class IndiceCabeceras(
    val cabeceras: List<Cabecera>,
    val paginas: List<Pair<String, String>>,
    val letras: List<Pair<String, String>>,
    val pageNumber: Int? = null,
    val previousUrl: String? = null,
    val nextUrl: String? = null,
    val selectedLetter: String? = null,
    val sourceUrl: String? = null,
)

class HemerotecaRepository {
    private val index = "${GalicianaUrls.BASE}/es/publicaciones/listar_cabeceras.do"

    suspend fun indice(url: String = "$index?letra=A&posicion=1"): IndiceCabeceras = withContext(Dispatchers.IO) {
        require(url == index || url.startsWith("$index?"))
        val html = Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Galiciana: HTTP ${response.code}")
            response.body?.string() ?: error("Respuesta vacía de Galiciana")
        }
        parseIndice(html, url)
    }

    fun parseIndice(html: String, url: String = index): IndiceCabeceras {
        val doc = Jsoup.parse(html, url)
        val publicationId = Regex("[?&]idPublicacion=(\\d+)")
        fun isTitle(text: String): Boolean {
            val value = text.trim()
            return value.any(Char::isLetter) && value.length > 1 &&
                !value.equals("Copia dixital", true) &&
                !value.equals("Ver números", true) &&
                !value.equals("Filtrar", true)
        }
        // The same table links the publication title and numeric columns (issue
        // count, first/last issue) with idPublicacion. Counts are not headings.
        val byRow = doc.select("tr").mapNotNull { row ->
            val links = row.select("a[href*='idPublicacion=']")
            val id = links.firstNotNullOfOrNull { link ->
                publicationId.find(link.attr("href"))?.groupValues?.get(1)
            } ?: return@mapNotNull null
            val title = links.firstOrNull { isTitle(it.text()) && it.text().trim().length > 2 }?.text()?.trim()
                ?: row.selectFirst("td")?.select("a")?.firstOrNull { isTitle(it.text()) }?.text()?.trim()
                ?: return@mapNotNull null
            Cabecera(id, title)
        }
        val byLink = doc.select("a[href*='idPublicacion=']").mapNotNull { anchor ->
            val id = publicationId.find(anchor.attr("href"))?.groupValues?.get(1)
                ?: return@mapNotNull null
            val title = anchor.text().trim().takeIf(::isTitle) ?: return@mapNotNull null
            Cabecera(id, title)
        }
        val cabeceras = (byRow + byLink).distinctBy { it.id }
        val hasNavigation = doc.select(".pagina_actual").isNotEmpty()
        val emptyTable = doc.select("table.nav_registros, table#nav_registros").any { table ->
            table.select("th").isNotEmpty() && table.select("tr:has(td)").isEmpty()
        }
        val emptyMessage = Regex(
            "sin resultados|no hay (?:publicaciones|registros|resultados)|" +
                "no se (?:han? )?(?:encontrado|localizado|hallado|recuperado) (?:publicaciones|registros|resultados)|" +
                "ninguna publicaci[oó]n|ning[uú]n registro",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(doc.body().text())
        check(cabeceras.isNotEmpty() || hasNavigation || emptyTable || emptyMessage) {
            "Galiciana no ha devuelto un índice de publicaciones reconocible. Vuelve a intentarlo."
        }
        val currentLabels = doc.select(".pagina_actual").map { it.text().trim() }
        val currentPage = currentLabels.firstNotNullOfOrNull { it.toIntOrNull() }
        val supplied = url.toHttpUrlOrNull()
        val letter = currentLabels.firstOrNull { it.matches(Regex("[A-Za-zÑñ#]")) }
            ?: supplied?.queryParameter("letra")?.takeIf { it.matches(Regex("[A-Za-zÑñ#]")) }
        val pattern = doc.selectFirst("form input[name=patron]")?.attr("value")?.trim()?.takeIf { it.isNotBlank() }
            ?: supplied?.queryParameter("patron")?.trim().orEmpty()
        fun contextualLink(target: String): String? {
            if (target != index && !target.startsWith("$index?")) return null
            val address = target.toHttpUrlOrNull() ?: return null
            val builder = address.newBuilder()
            val targetLetter = address.queryParameter("letra")
            // The official page links omit session-held filters. Carry only the
            // site's actual form/letter parameters so saved pages restore correctly.
            if (targetLetter == null) {
                if (pattern.isNotBlank()) builder.setQueryParameter("letra", "no")
                else if (letter != null) builder.setQueryParameter("letra", letter)
            }
            if (pattern.isNotBlank() && (targetLetter == null || targetLetter == "no") &&
                address.queryParameter("patron") == null) builder.setQueryParameter("patron", pattern)
            return builder.build().toString()
        }
        val navigation = doc.select("a[href*='listar_cabeceras.do']").mapNotNull { anchor ->
            val target = contextualLink(anchor.absUrl("href")) ?: return@mapNotNull null
            val text = anchor.text().trim()
            if (text.isNotBlank()) text to target else null
        }.distinctBy { it.first to it.second }
        fun safeLink(selector: String): String? = doc.selectFirst(selector)?.absUrl("href")?.let(::contextualLink)
        val pageLinks = navigation.filter { it.first.matches(Regex("\\d+")) }
        val previous = safeLink("#boton_anterior[href]") ?: safeLink(".boton_anterior a[href]")
            ?: currentPage?.let { current -> pageLinks.filter { (it.first.toIntOrNull() ?: current) < current }
                .maxByOrNull { it.first.toInt() }?.second }
        val next = safeLink("#boton_siguiente[href]") ?: safeLink(".boton_siguiente a[href]")
            ?: currentPage?.let { current -> pageLinks.filter { (it.first.toIntOrNull() ?: current) > current }
                .minByOrNull { it.first.toInt() }?.second }
        val letters = navigation.filter { it.first.matches(Regex("[A-Za-zÑñ#]")) }.toMutableList()
        if (letter != null && letters.none { it.first.equals(letter, true) }) {
            val first = index.toHttpUrlOrNull()!!.newBuilder().setQueryParameter("letra", letter)
                .setQueryParameter("posicion", "1").build().toString()
            letters += letter to first
        }
        return IndiceCabeceras(cabeceras, pageLinks, letters.sortedBy { it.first }, currentPage,
            previous, next, letter, contextualLink(url) ?: url)
    }
    /** Submit the title form of the periodicals index, not the general catalogue. */
    suspend fun searchIndice(query: String): IndiceCabeceras = withContext(Dispatchers.IO) {
        val page = Http.client.newCall(Request.Builder().url("$index?letra=A&posicion=1").build()).execute().use { response ->
            if (!response.isSuccessful) error("Galiciana: HTTP ${response.code}")
            response.body?.string() ?: error("Respuesta vacía de Galiciana")
        }
        val doc = Jsoup.parse(page, index)
        val form = doc.select("form").firstOrNull { candidate ->
            candidate.select("input[type=text], input:not([type])").isNotEmpty() &&
                (candidate.text().contains("título", true) ||
                    candidate.text().contains("titulo", true) ||
                    candidate.select("input").any { it.attr("name").contains("titulo", true) ||
                        it.attr("name").contains("title", true) })
        } ?: error("No se encontró el formulario de títulos de Hemeroteca")
        val input = form.select("input[type=text], input:not([type])")
            .firstOrNull { it.attr("name").contains("titulo", true) ||
                it.attr("name").contains("title", true) }
            ?: form.selectFirst("input[type=text], input:not([type])")
            ?: error("El formulario no tiene un campo de búsqueda")
        val action = form.absUrl("action").ifBlank { index }
        require(action.startsWith("${GalicianaUrls.BASE}/es/publicaciones/")) {
            "La búsqueda de Hemeroteca usa una ruta desconocida"
        }
        // HTML submits only the clicked submit button. Sending "Limpiar" as well
        // cancels the title filter on this catalogue.
        val fields = form.select("input[name]").filter { element ->
            element.attr("type").lowercase() in listOf("hidden", "text", "") &&
                element.attr("name").isNotBlank()
        }.associate { it.attr("name") to if (it == input) query else it.attr("value") }
            .toMutableMap()
        form.select("input[type=submit][name], button[type=submit][name]")
            .firstOrNull { !it.text().contains("limpiar", true) &&
                !it.attr("value").contains("limpiar", true) }
            ?.let { fields[it.attr("name")] = it.attr("value") }
        val request = if (form.attr("method").equals("post", true)) {
            val body = FormBody.Builder().apply { fields.forEach { (key, value) -> add(key, value) } }.build()
            Request.Builder().url(action).post(body).build()
        } else {
            val url = action.toHttpUrlOrNull()?.newBuilder()
                ?: error("Dirección de Hemeroteca inválida")
            fields.forEach { (key, value) -> url.setQueryParameter(key, value) }
            Request.Builder().url(url.build()).build()
        }
        val html = Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Galiciana: HTTP ${response.code}")
            response.body?.string() ?: error("Respuesta vacía de Galiciana")
        }
        val result = parseIndice(html, request.url.toString())
        val matching = result.cabeceras.filter { matches(it.title, query) }
        if (matching.isNotEmpty()) return@withContext result.copy(cabeceras = matching)
        // Some deployments ignore the title form and return the ordinary A page.
        // Follow the site's own letter and page links rather than inventing IDs.
        searchAlphabet(query, parseIndice(page, index))
    }

    fun matches(title: String, query: String): Boolean {
        fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase().replace(Regex("\\s+"), " ").trim()
        return normalized(title).contains(normalized(query))
    }

    private suspend fun searchAlphabet(query: String, root: IndiceCabeceras): IndiceCabeceras {
        val words = query.trim().split(Regex("\\s+"))
        val initial = words.firstOrNull()?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        val contentInitial = words.firstOrNull { it.lowercase() !in setOf("el", "la", "los", "las", "a", "o") }
            ?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        val candidates = listOf(initial, contentInitial).filter { it.isNotBlank() }.distinct()
        val found = linkedMapOf<String, Cabecera>()
        for (letter in candidates) {
            val first = root.letras.firstOrNull { it.first.equals(letter, true) }?.second ?: continue
            var url: String? = first
            val visited = mutableSetOf<String>()
            while (url != null && visited.add(url) && visited.size <= 80) {
                val page = indice(url)
                page.cabeceras.filter { matches(it.title, query) }.forEach { found[it.id] = it }
                val currentPosition = Regex("[?&]posicion=(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                url = page.nextUrl?.takeUnless { it in visited } ?: page.paginas.mapNotNull { (_, target) ->
                    val position = Regex("[?&]posicion=(\\d+)").find(target)
                        ?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                    if (position > currentPosition && target !in visited) position to target else null
                }.minByOrNull { it.first }?.second
            }
        }
        return IndiceCabeceras(found.values.toList(), emptyList(), root.letras)
    }

    suspend fun yearsOf(publicationId: String): List<String> = withContext(Dispatchers.IO) {
        val html = fetchIssues(publicationId, null)
        val linked = Regex("""anyo=(\d{4})""").findAll(html).map { it.groupValues[1] }.toList()
        val dated = parseNumeros(html).mapNotNull { number ->
            Regex("""\b(?:18|19|20)\d{2}\b""").find(number.label)?.value
        }
        val headings = Regex("""(?i)a[nñ]o\s+((?:18|19|20)\d{2})""")
            .findAll(Jsoup.parse(html).text()).map { it.groupValues[1] }.toList()
        (linked + dated + headings).distinct().sorted()
    }

    suspend fun loadNumeros(publicationId: String, year: String? = null): List<Numero> =
        withContext(Dispatchers.IO) {
            parseNumeros(fetchIssues(publicationId, year), year)
        }

    private fun fetchIssues(publicationId: String, year: String?): String =
        Http.client.newCall(
            Request.Builder()
                .url(GalicianaUrls.issues(publicationId, year))
                .header("User-Agent", "Mozilla/5.0")
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) error("Galiciana: HTTP ${response.code}")
            response.body?.string()?.takeIf { it.isNotBlank() }
                ?: error("Galiciana no ha devuelto números")
        }

    fun parseNumeros(html: String, selectedYear: String? = null): List<Numero> {
        val doc = Jsoup.parse(html)
        return doc.select("a[href*='grupo.do']").mapNotNull { anchor ->
            if (!anchor.text().trim().matches(Regex("(?i)copia\\s+di(?:x|g)ital.*")))
                return@mapNotNull null
            val path = Regex("[?&]path=(\\d+)").find(anchor.attr("href"))
                ?.groupValues?.get(1) ?: return@mapNotNull null
            val context = issueContext(anchor)
            val date = parseIssueDate(context, selectedYear)
            val number = Regex("(?i)\\b(?:n[uú]mero|n[uú]m\\.)\\s*(?:s\\.n\\.|[0-9]+[A-Za-z0-9./-]*)")
                .find(context)?.value?.trim()
            val displayDate = date?.let { "%02d/%02d/%04d".format(it.dayOfMonth, it.monthValue, it.year) }
            val label = listOfNotNull(number, displayDate).joinToString(" · ")
                .ifBlank { "Ejemplar sin fecha identificada" }
            Numero(label, path, date, date?.monthValue ?: monthIn(context))
        }.distinctBy { it.path }.sortedWith(compareBy<Numero> { it.date ?: LocalDate.MAX }
            .thenBy { it.month ?: 13 })
    }

    private fun issueContext(anchor: Element): String {
        var node: Element? = anchor.parent()
        var best = ""
        repeat(7) {
            val current = node ?: return@repeat
            val copies = current.select("a[href*='grupo.do']").count {
                it.text().contains("Copia", true)
            }
            if (copies > 1) return best
            val text = current.text().trim()
            if (text.length > best.length) best = text
            if (Regex("(?i)\\b(?:n[uú]mero|n[uú]m\\.)\\s*(?:s\\.n\\.|\\d)|\\b(?:18|19|20)\\d{2}\\s+[a-záéíóúñ]+\\s+\\d{1,2}").containsMatchIn(text))
                return text
            node = current.parent()
        }
        return best
    }

    private val months = listOf("xaneiro", "febreiro", "marzo", "abril", "maio", "xuño",
        "xullo", "agosto", "setembro", "outubro", "novembro", "decembro")
    private fun monthIn(text: String): Int? {
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        val monthNames = listOf(
            listOf("xaneiro", "enero", "jan"), listOf("febreiro", "febrero", "feb"),
            listOf("marzo", "mar"), listOf("abril", "abr"), listOf("maio", "mayo", "may"),
            listOf("xuno", "junio", "jun"), listOf("xullo", "julio", "jul"),
            listOf("agosto", "ago"), listOf("setembro", "septiembre", "setiembre", "sep"),
            listOf("outubro", "octubre", "oct"), listOf("novembro", "noviembre", "nov"),
            listOf("decembro", "diciembre", "dic"),
        )
        return monthNames.indexOfFirst { names -> names.any { name ->
            Regex("\\b${Regex.escape(name)}\\b").containsMatchIn(normalized)
        } }.takeIf { it >= 0 }?.plus(1)
    }

    private fun parseIssueDate(text: String, selectedYear: String?): LocalDate? {
        val numeric = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b").find(text)
        if (numeric != null) return runCatching { LocalDate.of(numeric.groupValues[3].toInt(),
            numeric.groupValues[2].toInt(), numeric.groupValues[1].toInt()) }.getOrNull()
        val yearMonthDay = Regex("\\b((?:18|19|20)\\d{2})\\s+([a-záéíóúñ]+)\\s+(\\d{1,2})\\b", RegexOption.IGNORE_CASE).find(text)
        val dayMonthYear = Regex("\\b(\\d{1,2})\\s+([a-záéíóúñ]+)\\s+((?:18|19|20)\\d{2})\\b", RegexOption.IGNORE_CASE).find(text)
        val year = yearMonthDay?.groupValues?.get(1)?.toIntOrNull()
            ?: dayMonthYear?.groupValues?.get(3)?.toIntOrNull()
            ?: selectedYear?.toIntOrNull()
        val day = yearMonthDay?.groupValues?.get(3)?.toIntOrNull()
            ?: dayMonthYear?.groupValues?.get(1)?.toIntOrNull()
        val month = monthIn(yearMonthDay?.groupValues?.get(2)
            ?: dayMonthYear?.groupValues?.get(2) ?: text)
        return if (year != null && month != null && day != null)
            runCatching { LocalDate.of(year, month, day) }.getOrNull() else null
    }

    fun monthName(month: Int): String = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre").getOrNull(month - 1) ?: "Mes $month"

    private fun decode(s: String) = s
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}
