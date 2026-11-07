package gal.galiciana.app.data

import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.domain.OcrWorkGroup
import gal.galiciana.app.domain.PageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class OcrRepository {
    private suspend fun fetchAlto(url: String): String = suspendCancellableCoroutine { continuation ->
        val call = Http.client.newCall(
            Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/xml,text/xml,*/*")
                .build()
        )
        call.timeout().timeout(15, TimeUnit.SECONDS)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume("")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = runCatching { response.use { if (it.isSuccessful) it.body?.string().orEmpty() else "" } }.getOrDefault("")
                if (continuation.isActive) continuation.resume(body)
            }
        })
    }

    data class SearchResult(
        val hits: List<OcrHit>, val nextUrl: String?,
        val sortUrls: Map<String, String> = emptyMap(),
        val totalWorks: Int? = null,
    ) {
        val incomplete: Boolean get() = nextUrl != null
    }
    suspend fun loadPage(path: String, position: Int, imageId: String?): PageContent =
        withContext(Dispatchers.IO) {
            val text = Http.client.newCall(
                Request.Builder().url(GalicianaUrls.ocr(path, position)).build()
            ).execute().use { response ->
                if (!response.isSuccessful) error("OCR: HTTP ${response.code}")
                response.body?.string().orEmpty().takeUnless {
                    it.trimStart().startsWith("<!DOCTYPE html", true) ||
                        it.trimStart().startsWith("<html", true)
                } ?: error("Galiciana no ha devuelto texto OCR")
            }

            PageContent(
                path = path,
                position = position,
                imageId = imageId,
                imageUrl = imageId?.let { GalicianaUrls.jpeg(it) },
                ocrText = text,
            )
        }

    data class AltoHit(val left: Float, val top: Float, val width: Float, val height: Float)

    enum class AltoStatus { AVAILABLE, EMPTY, UNAVAILABLE, INVALID }
    data class AltoMatch(val boxes: List<AltoHit>)
    data class AltoSearchResult(
        val pageSize: Pair<Float, Float>,
        val matches: List<AltoMatch>,
        val status: AltoStatus,
    ) {
        val hits: List<AltoHit> get() = matches.flatMap { it.boxes }
    }

    suspend fun loadAltoHits(path: String, position: Int, query: String, imageId: String? = null): Pair<Pair<Float, Float>, List<AltoHit>> {
        val result = loadAltoSearch(path, position, query, imageId)
        return result.pageSize to result.hits
    }

    suspend fun loadAltoSearch(path: String, position: Int, query: String, imageId: String? = null): AltoSearchResult =
        withContext(Dispatchers.IO) {
            // The canonical page identity has priority. Faster image-only fallbacks
            // must not replace a valid response for path + position.
            val urls = buildList {
                add(GalicianaUrls.alto(path, position))
                if (!imageId.isNullOrBlank()) {
                    add("${GalicianaUrls.BASE}/es/catalogo_imagenes/descargarAlto.do?path=$path&posicion=$position&idImagen=$imageId")
                    add("${GalicianaUrls.BASE}/es/catalogo_imagenes/descargarAlto.do?idImagen=$imageId")
                }
            }
            coroutineScope {
                val requests = urls.map { url -> async { fetchAlto(url) } }
                var invalidResponse = false
                var emptyPage: AltoSearchResult? = null
                for (request in requests) {
                    val body = request.await()
                    if (body.isNotBlank()) {
                        val parsed = parseAltoSearch(body, query)
                        when (parsed.status) {
                            AltoStatus.AVAILABLE -> {
                                requests.forEach { it.cancel() }
                                return@coroutineScope parsed
                            }
                            AltoStatus.EMPTY -> { if (emptyPage == null) emptyPage = parsed }
                            else -> invalidResponse = true
                        }
                    }
                }
                emptyPage ?: AltoSearchResult(0f to 0f, emptyList(),
                    if (invalidResponse) AltoStatus.INVALID else AltoStatus.UNAVAILABLE)
            }
        }

    /** Pure parser: XML quoting/namespaces and search semantics are testable without Android or HTTP. */
    fun parseAltoSearch(xml: String, query: String): AltoSearchResult {
        fun invalid() = AltoSearchResult(0f to 0f, emptyList(), AltoStatus.INVALID)
        val document = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())
        fun org.jsoup.nodes.Element.localName() = tagName().substringAfter(':')
        fun org.jsoup.nodes.Element.readAltoAttribute(name: String): String =
            attributes().firstOrNull { it.key.equals(name, ignoreCase = true) }?.value.orEmpty()
        fun org.jsoup.nodes.Element.number(name: String): Float? =
            readAltoAttribute(name).toFloatOrNull()?.takeIf { it.isFinite() }
        if (document.getAllElements().none { it.localName().equals("alto", true) }) return invalid()
        val page = document.getAllElements().firstOrNull { it.localName().equals("Page", true) }
            ?: return invalid()
        val pageW = page.number("WIDTH")?.takeIf { it > 1f } ?: return invalid()
        val pageH = page.number("HEIGHT")?.takeIf { it > 1f } ?: return invalid()
        val tags = page.getAllElements().filter { it.localName().equals("String", true) }
        if (tags.isEmpty()) return AltoSearchResult(pageW to pageH, emptyList(), AltoStatus.EMPTY)
        data class Word(
            val content: String,
            val parts: List<AltoHit>,
            val block: org.jsoup.nodes.Element,
            val substitutionType: String = "",
            val substitution: String = "",
            val source: org.jsoup.nodes.Element,
        )
        var invalidCoordinates = false
        val words = tags.mapNotNull { tag ->
            val content = tag.readAltoAttribute("CONTENT").ifBlank {
                tag.readAltoAttribute("SUBS_CONTENT").ifBlank { tag.text() }
            }
            val left = tag.number("HPOS")
            val top = tag.number("VPOS")
            val width = tag.number("WIDTH")
            val height = tag.number("HEIGHT")
            if (left == null || top == null || width == null || height == null ||
                left < 0f || top < 0f || width <= 0f || height <= 0f ||
                left + width > pageW + 1f || top + height > pageH + 1f) {
                invalidCoordinates = true
                null
            } else {
                val block = tag.parents().firstOrNull { it.localName().equals("TextBlock", true) } ?: page
                // ALTO may store the printed line-end hyphen as a separate HYP element.
                val following = tag.nextElementSibling()
                val hasHyphen = following?.localName()?.equals("HYP", true) == true
                val text = if (hasHyphen && !content.trimEnd().endsWith('-')) "$content-" else content
                Word(text, listOf(AltoHit(left, top, width, height)), block,
                    tag.readAltoAttribute("SUBS_TYPE"), tag.readAltoAttribute("SUBS_CONTENT"), tag)
            }
        }.toMutableList()
        // Partial coordinates would make a zero-match verdict unreliable. Try another endpoint instead.
        if (invalidCoordinates) return invalid()
        // Block segmentation can split a single newspaper column. Link only flows
        // backed by an explicit ALTO substitution or a tightly aligned line-end cut.
        val flowParents = java.util.IdentityHashMap<org.jsoup.nodes.Element, org.jsoup.nodes.Element>()
        fun flow(block: org.jsoup.nodes.Element): org.jsoup.nodes.Element {
            var root = block
            while (flowParents[root] != null) root = flowParents[root]!!
            return root
        }
        fun linkFlows(left: Word, right: Word) {
            val leftRoot = flow(left.block)
            val rightRoot = flow(right.block)
            if (leftRoot !== rightRoot) flowParents[rightRoot] = leftRoot
        }
        data class LineBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)
        fun textLine(word: Word) = word.source.parents().firstOrNull { it.localName().equals("TextLine", true) }
        val lineBoundsCache = java.util.IdentityHashMap<org.jsoup.nodes.Element, LineBounds>()
        fun lineBounds(line: org.jsoup.nodes.Element): LineBounds? {
            lineBoundsCache[line]?.let { return it }
            val left = line.number("HPOS")
            val top = line.number("VPOS")
            val width = line.number("WIDTH")
            val height = line.number("HEIGHT")
            val result = if (left != null && top != null && width != null && height != null && width > 0f && height > 0f)
                LineBounds(left, top, left + width, top + height)
            else {
                val members = line.getAllElements().filter { it.localName().equals("String", true) }
                val bounds = members.mapNotNull { element ->
                    val x = element.number("HPOS") ?: return@mapNotNull null
                    val y = element.number("VPOS") ?: return@mapNotNull null
                    val w = element.number("WIDTH") ?: return@mapNotNull null
                    val h = element.number("HEIGHT") ?: return@mapNotNull null
                    LineBounds(x, y, x + w, y + h)
                }
                if (bounds.isEmpty()) return null
                LineBounds(bounds.minOf { it.left }, bounds.minOf { it.top },
                    bounds.maxOf { it.right }, bounds.maxOf { it.bottom })
            }
            lineBoundsCache[line] = result
            return result
        }
        fun continuesColumn(left: Word, right: Word): Boolean {
            val firstLine = textLine(left) ?: return false
            val nextLine = textLine(right) ?: return false
            if (firstLine === nextLine) return false
            if (firstLine.getAllElements().lastOrNull { it.localName().equals("String", true) } !== left.source ||
                nextLine.getAllElements().firstOrNull { it.localName().equals("String", true) } !== right.source) return false
            val a = lineBounds(firstLine) ?: return false
            val b = lineBounds(nextLine) ?: return false
            val height = maxOf(a.bottom - a.top, b.bottom - b.top)
            val vertical = b.top - a.top
            val overlap = minOf(a.right, b.right) - maxOf(a.left, b.left)
            return height > 0f && vertical >= height * 0.5f && vertical <= height * 3f &&
                overlap >= minOf(a.right - a.left, b.right - b.left) * 0.6f
        }
        fun trailingHyphen(s: String) = s.trim().lastOrNull()?.let { it in "-‐‑–—­¬" } == true
        var wi = 0
        while (wi < words.lastIndex) {
            val current = words[wi].content.trim()
            val next = words[wi + 1].content.trim()
            val leftWord = words[wi]
            val rightWord = words[wi + 1]
            val sameBlock = leftWord.block === rightWord.block
            val substitutedPair = leftWord.substitutionType.equals("HypPart1", true) &&
                rightWord.substitutionType.equals("HypPart2", true) &&
                leftWord.substitution.isNotBlank() &&
                (rightWord.substitution.isBlank() || leftWord.substitution == rightWord.substitution)
            val printedCut = current.length > 1 && trailingHyphen(current) && next.any(Char::isLetter) &&
                (sameBlock || continuesColumn(leftWord, rightWord))
            if (substitutedPair || printedCut) {
                linkFlows(leftWord, rightWord)
                words[wi] = leftWord.copy(
                    content = if (substitutedPair) leftWord.substitution else current.trimEnd('-', '‐', '‑', '–', '—', '­', '¬') + next,
                    parts = leftWord.parts + rightWord.parts,
                    substitutionType = "", substitution = "")
                words.removeAt(wi + 1)
            } else if (sameBlock && current.isNotEmpty() && current.all { it in "-–—­¬" } && wi > 0 &&
                words[wi - 1].block === leftWord.block && next.any(Char::isLetter) &&
                textLine(words[wi - 1]) === textLine(leftWord) && continuesColumn(leftWord, rightWord)) {
                words[wi - 1] = words[wi - 1].copy(content = words[wi - 1].content + next,
                    parts = words[wi - 1].parts + rightWord.parts)
                words.removeAt(wi + 1)
                words.removeAt(wi)
                wi--
            } else wi++
        }
        fun boxes(pack: List<AltoHit>): List<AltoHit> {
            val lines = mutableListOf<MutableList<AltoHit>>()
            pack.forEach { part ->
                val line = lines.lastOrNull()
                val previous = line?.lastOrNull()
                if (line == null || previous == null ||
                    kotlin.math.abs(line.first().top - part.top) > maxOf(line.first().height, part.height) * 0.7f ||
                    part.left < previous.left || part.left - previous.left - previous.width > maxOf(previous.height, part.height) * 4f)
                    lines.add(mutableListOf(part))
                else line.add(part)
            }
            return lines.map { line ->
                val left = line.minOf { it.left }
                val top = line.minOf { it.top }
                AltoHit(left, top, line.maxOf { it.left + it.width } - left,
                    line.maxOf { it.top + it.height } - top)
            }
        }
        val matches = mutableListOf<Pair<Int, AltoMatch>>()
        val phrases = gal.galiciana.app.ui.quotedPhrases(query)
        if (phrases.isNotEmpty()) {
            // Normalize each positioned word once. Each token keeps its source Word
            // so punctuation/split words can share boxes without inventing geometry.
            data class PositionedToken(val text: String, val sourceWord: Int)
            val normalized = words.mapIndexed { index, word ->
                gal.galiciana.app.ui.normalizedOcrWords(word.content).map { PositionedToken(it, index) }
            }
            val expectedPhrases = phrases.map { phrase ->
                gal.galiciana.app.ui.queryTokens(phrase).map { gal.galiciana.app.ui.lettersOnly(it) }
                    .filter { it.isNotEmpty() }
            }
            var flowStart = 0
            while (flowStart < words.size) {
                var flowEnd = flowStart + 1
                val currentFlow = flow(words[flowStart].block)
                while (flowEnd < words.size && flow(words[flowEnd].block) === currentFlow) flowEnd++
                val positioned = normalized.subList(flowStart, flowEnd).flatten()
                val texts = positioned.map { it.text }
                expectedPhrases.forEach { expected ->
                    gal.galiciana.app.ui.phraseMatchRanges(texts, expected).forEach { range ->
                        val start = positioned[range.first].sourceWord
                        val end = positioned[range.last].sourceWord
                        matches += start to AltoMatch(boxes(words.subList(start, end + 1).flatMap { it.parts }))
                    }
                }
                flowStart = flowEnd
            }
        }
        val tokens = gal.galiciana.app.ui.extraTokens(query)
        fun wordMatch(content: String, token: String): Boolean {
            val slack = gal.galiciana.app.ui.slackForQuery(tokens, token)
            if (gal.galiciana.app.ui.tokenMatches(content, token, slack)) return true
            // A single ALTO String may include punctuation (e.g. d'Artagnan).
            return Regex("""[\p{L}\p{N}�□■#*?]+(?:[-–—­][\p{L}\p{N}]+)*""")
                .findAll(content).any { gal.galiciana.app.ui.tokenMatches(it.value, token, slack) }
        }
        var i = 0
        while (i < words.size && tokens.isNotEmpty()) {
            val direct = tokens.any { token ->
                val target = gal.galiciana.app.ui.lettersOnly(token)
                if (target.length == 1) gal.galiciana.app.ui.lettersOnly(words[i].content) == target
                else wordMatch(words[i].content, token)
            }
            val joined = !direct && i + 1 < words.size && flow(words[i].block) === flow(words[i + 1].block) && tokens.any { token ->
                val first = gal.galiciana.app.ui.lettersOnly(words[i].content)
                val second = gal.galiciana.app.ui.lettersOnly(words[i + 1].content)
                first.length in 2..5 && second.length >= 2 &&
                    first.length + second.length <= gal.galiciana.app.ui.lettersOnly(token).length + 1 &&
                    gal.galiciana.app.ui.tokenMatches(words[i].content + words[i + 1].content, token, 0)
            }
            if (direct || joined) {
                matches += i to AltoMatch(boxes(words[i].parts + if (joined) words[i + 1].parts else emptyList()))
                i += if (joined) 2 else 1
            } else i++
        }
        return AltoSearchResult(pageW to pageH, matches.sortedBy { it.first }.map { it.second }.distinct(), AltoStatus.AVAILABLE)
    }

    suspend fun searchAllPagesDetailed(
        query: String,
        title: String = "",
        author: String = "",
        place: String = "",
        yearFrom: String = "",
        yearTo: String = "",
        fullText: Boolean = true,
        maxPages: Int = 8,
    ): SearchResult = withContext(Dispatchers.IO) {
        val pageResults = fullText && query.isNotBlank()
        val builder = FormBody.Builder()
            .add("busq_general", query)
            .add("busq_titulo", title)
            .add("busq_autor", author)
            .add("busq_lugaredicion", place)
            .add("busq_rango0_fechainicial__fechafinal", yearFrom)
            .add("busq_rango1_fechainicial__fechafinal", yearTo)
            .add("busq_objetosmultimedia", "true")
        if (pageResults) builder.add("general_ocr", "on")
        val url = GalicianaUrls.SEARCH_OCR
        val all = mutableListOf<OcrHit>()
        val visited = mutableSetOf<String>()
        var request = Request.Builder().url(url).post(builder.build()).build()
        var next: String? = null
        var sortUrls = emptyMap<String, String>()
        var totalWorks: Int? = null
        for (index in 0 until maxPages.coerceAtLeast(1)) {
            val html = try {
                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Búsqueda: HTTP ${response.code}")
                    response.body?.string()?.takeIf { it.isNotBlank() }
                        ?: error("Respuesta vacía de Galiciana")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (index == 0) throw e
                break // Keep the pages already read; the next URL remains available.
            }
            val pageHits = try {
                parseSearchResponse(html)
            } catch (e: IllegalStateException) {
                if (index == 0) throw e
                break // Keep validated pages and the pending URL available for retry.
            }
            all += pageHits
            if (index == 0) {
                sortUrls = resultSortUrls(html, request.url.toString())
                totalWorks = resultTotal(html)
            }
            visited += request.url.toString()
            val nextUrl = nextResultsUrl(html, request.url.toString())
            if (nextUrl == null || nextUrl in visited) {
                next = null
                break
            }
            next = nextUrl
            request = Request.Builder().url(nextUrl).get().build()
        }
        SearchResult(mergePageHitsByIdentity(all), next, sortUrls, totalWorks)
    }

    suspend fun searchNextPage(url: String): SearchResult = withContext(Dispatchers.IO) {
        require(url.startsWith("${GalicianaUrls.BASE}/es/consulta/resultados_ocr.do?") ||
            url.startsWith("${GalicianaUrls.BASE}/es/consulta/resultados.do?") ||
            url.startsWith("${GalicianaUrls.BASE}/es/consulta/resultados_busqueda.do?"))
        val html = Http.client.newCall(Request.Builder().url(url).get().build())
            .execute().use { response ->
                if (!response.isSuccessful) error("Búsqueda: HTTP ${response.code}")
                response.body?.string() ?: error("Respuesta vacía de Galiciana")
            }
        SearchResult(parseSearchResponse(html), nextResultsUrl(html, url)?.takeUnless { it == url },
            resultSortUrls(html, url), resultTotal(html))
    }

    suspend fun searchAllPages(
        query: String,
        title: String = "",
        author: String = "",
        place: String = "",
        yearFrom: String = "",
        yearTo: String = "",
        fullText: Boolean = true,
        maxPages: Int = 8,
    ): List<OcrHit> {
        return searchAllPagesDetailed(
            query, title, author, place, yearFrom, yearTo, fullText, maxPages
        ).hits
    }

    private fun nextResultsUrl(html: String, base: String): String? {
        val doc = Jsoup.parse(html, base)
        val link = doc.selectFirst("#boton_siguiente[href]")
            ?: doc.select("a[href*='resultados_ocr.do'], a[href*='resultados.do'], a[href*='resultados_busqueda.do']")
            .firstOrNull { a ->
                val label = (a.text() + " " + a.attr("title") + " " + a.attr("aria-label")).lowercase()
                label.contains("siguiente") || label.contains("next") ||
                    a.text().trim() in listOf("›", "»", ">") ||
                    a.attr("rel").contains("next", true) ||
                    a.hasClass("next") || a.parent()?.hasClass("next") == true
            }
        return link?.absUrl("href")?.takeIf { it.startsWith(GalicianaUrls.BASE) }
    }

    private fun resultSortUrls(html: String, base: String): Map<String, String> {
        val doc = Jsoup.parse(html, base)
        // The sorting controls have appeared under several containers in Galiciana.
        // A pagination URL carries the same query parameters when controls are absent.
        val source = doc.select("a[href]").asSequence()
            .mapNotNull { it.absUrl("href").toHttpUrlOrNull() }
            .firstOrNull { it.queryParameter("campoOrden") != null &&
                it.host == "biblioteca.galiciana.gal" && it.encodedPath in setOf(
                    "/es/consulta/resultados_ocr.do", "/es/consulta/resultados.do", "/es/consulta/resultados_busqueda.do") }
            ?: nextResultsUrl(html, base)?.toHttpUrlOrNull()
                ?.takeIf { it.queryParameter("campoOrden") != null }
            ?: return emptyMap()
        if (source.host != "biblioteca.galiciana.gal") return emptyMap()
        val fields = mapOf("Titulo" to "tituloorden", "Autor" to "autororden",
            "Fecha" to "fechapublicacionorden", "Relevancia" to "score")
        return buildMap {
            fields.forEach { (key, field) ->
                listOf("N", "S").forEach { direction ->
                    put("$key:$direction", source.newBuilder()
                        .setQueryParameter("campoOrden", field)
                        .setQueryParameter("ordenDesc", direction)
                        .setQueryParameter("posicion", "1").build().toString())
                }
            }
        }
    }

    private fun resultTotal(html: String): Int? {
        val nav = Jsoup.parse(html).selectFirst(".navegacion_resultados")?.text().orEmpty()
        return Regex("""\b\d+\s+al\s+\d+\s+de\s+([\d.]+)\b""", RegexOption.IGNORE_CASE)
            .find(nav)?.groupValues?.get(1)?.replace(".", "")?.toIntOrNull()
    }

    fun groupByWork(hits: List<OcrHit>): List<OcrWorkGroup> =
        hits.groupBy { it.path ?: it.title }
            .map { (_, pages) ->
                val first = pages.first()
                fun weak(t: String) =
                    t.isBlank() || t.equals("Documento", true) || t.startsWith("Pág") || t.startsWith("Página")
                val parent = pages.mapNotNull { it.parentTitle?.trim() }.firstOrNull { it.isNotBlank() && !weak(it) }
                val title = pages.map { it.title }.firstOrNull { !weak(it) && it.length > 8 }
                    ?: parent
                    ?: first.title.ifBlank { "Título desconocido" }
                OcrWorkGroup(
                    title = title,
                    parentTitle = parent,
                    pages = pages,
                )
            }

    /** An unexpected HTML page must never be presented as a successful empty search. */
    internal fun parseSearchResponse(html: String): List<OcrHit> {
        val hits = parseHits(html)
        if (hits.isNotEmpty()) return hits
        val doc = Jsoup.parse(html)
        val emptyMessage = Regex(
            "sin resultados|no hay resultados|no se (?:han? |ha )?(?:encontrado|encontraron|hallado|hallaron) (?:resultados|documentos|registros)|" +
                "no (?:ha |han )?(?:producido|obtenido|devuelto) resultados|ning[uú]n (?:resultado|documento|registro)",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(doc.body().text())
        check(emptyMessage || doc.select(".navegacion_resultados, .nav_registros").isNotEmpty()) {
            "Galiciana no ha devuelto una página de resultados reconocible. Vuelve a intentarlo."
        }
        return emptyList()
    }

    fun parseHits(html: String): List<OcrHit> {
        val doc = Jsoup.parse(html, GalicianaUrls.BASE)
        // The results page mixes OCR page hits with bibliographic matches. A
        // page with some unidad_textual entries still has other valid works.
        val cards = doc.select(".nav_registros .nav_registro")
        if (cards.isNotEmpty()) return mergePageHitsByIdentity(cards.flatMap { card ->
            val pages = card.select(".unidad_textual").mapNotNull { parseUnit(it.outerHtml()) }
            if (pages.isNotEmpty()) return@flatMap pages
            val anchor = card.selectFirst("a[href*='grupo.do?path=']") ?: return@flatMap emptyList()
            val href = anchor.absUrl("href")
            val path = Regex("[?&]path=(\\d+)").find(href)?.groupValues?.get(1) ?: return@flatMap emptyList()
            val img = Regex("[?&]idImagen=(\\d+)").find(href)?.groupValues?.get(1)
            val pos = Regex("[?&]posicion=(\\d+)").find(href)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val title = card.selectFirst(".titulo a, .titulo")?.text()?.trim()
                ?.takeIf { it.isNotBlank() } ?: anchor.attr("data-analytics-recordtitle").ifBlank { "Obra $path" }
            listOf(OcrHit(title = title, snippets = emptyList(), path = path, imageId = img, position = pos))
        })
        return mergePageHitsByIdentity(doc.getElementsByClass("unidad_textual").mapNotNull { parseUnit(it.outerHtml()) })
    }

    private fun parseUnit(block: String): OcrHit? {
        val link = Jsoup.parse(block).select("a[href*='grupo.do']").firstOrNull {
            it.attr("href").contains("path=")
        } ?: return null
        val href = link.attr("href").replace("&amp;", "&")
        val path = Regex("""[?&]path=(\d+)""").find(href)?.groupValues?.get(1) ?: return null
        val imageId = Regex("""[?&]idImagen=(\d+)""").find(href)?.groupValues?.get(1)
        val pageLabel = Regex("""[?&]posicion=(\d+)""", RegexOption.IGNORE_CASE)
            .find(href)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""P[áa]g(?:ina|\.)?\s+(\d+)""", RegexOption.IGNORE_CASE)
            .find(block)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""p[áa]g\.?\s*(\d+)\s*/""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""posici[oó]n=(\d+)""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""initialPage["']?\s*[:=]\s*["']?(\d+)""").find(block)?.groupValues?.get(1)?.toIntOrNull()

        val parent = Regex("""En:\s*([^<]+)""")
            .find(block)?.groupValues?.get(1)?.let(::decodeEntities)?.trim()

        fun pickTitle(raw: String?): String? {
            val t = raw?.let(::decodeEntities)?.trim() ?: return null
            if (t.isBlank()) return null
            if (t.startsWith("Pág", true) || t.startsWith("Página", true)) return null
            if (t.equals("Documento", true) || t.length < 6) return null
            return t
        }
        val title = pickTitle(Regex("""data-analytics-recordtitle=['\"]([^'\"]+)['\"]""").find(block)?.groupValues?.get(1))
            ?: pickTitle(Regex("""(?is)class="[^"]*titulo[^"]*"[^>]*>([^<]{6,200})""").find(block)?.groupValues?.get(1))
            ?: pickTitle(Regex("""title=['\"]([^'\"]+)['\"]""").find(block)?.groupValues?.get(1))
            ?: pickTitle(parent)
            ?: "Documento $path"

        val ocurrencias = Regex(
            """(?is)<ul[^>]*texto_ocurrencias[^>]*>(.*?)</ul>"""
        ).findAll(block).joinToString("\n") { it.groupValues[1] }

        val fromList = Regex("""(?is)<li[^>]*>(.*?)</li>""")
            .findAll(ocurrencias)
            .map { cleanHtml(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .toList()

        fun okSnippet(s: String): Boolean {
            // Short or imperfect OCR is still useful evidence of a match.
            return s.length >= 3 && !s.contains("class=") && s.any(Char::isLetterOrDigit)
        }

        val snippets = fromList.filter(::okSnippet).distinct().take(12)

        return OcrHit(
            title = title,
            parentTitle = parent,
            snippets = snippets,
            path = path,
            imageId = imageId,
            position = pageLabel,
        )
    }

    private fun cleanHtml(raw: String): String =
        Jsoup.parseBodyFragment(raw).body().text().replace(Regex("\\s+"), " ").trim()

    private fun decodeEntities(text: String): String {
        var t = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", " ")
            .replace("&gt;", " ")
            .replace("&oacute;", "ó").replace("&Oacute;", "Ó")
            .replace("&aacute;", "á").replace("&Aacute;", "Á")
            .replace("&eacute;", "é").replace("&Eacute;", "É")
            .replace("&iacute;", "í").replace("&Iacute;", "Í")
            .replace("&uacute;", "ú").replace("&Uacute;", "Ú")
            .replace("&ntilde;", "ñ").replace("&Ntilde;", "Ñ")
            .replace("&uuml;", "ü").replace("&Uuml;", "Ü")
            .replace("&agrave;", "à").replace("&Agrave;", "À")
            .replace("&ccedil;", "ç").replace("&Ccedil;", "Ç")
            .replace("&hellip;", "…").replace("&mdash;", "—")
            .replace("&ndash;", "–")
        t = Regex("""&#(\d+);""").replace(t) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString().orEmpty()
        }
        t = Regex("""&#x([0-9a-fA-F]+);""").replace(t) { m ->
            m.groupValues[1].toIntOrNull(16)?.toChar()?.toString().orEmpty()
        }
        t = Regex("""&[a-zA-Z]+;""").replace(t, " ")
        return t
    }

}
