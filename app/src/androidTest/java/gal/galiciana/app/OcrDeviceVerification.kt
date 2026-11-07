package gal.galiciana.app

import android.app.Instrumentation
import android.os.Bundle
import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.ui.countMatches
import gal.galiciana.app.ui.queryTokens
import kotlinx.coroutines.runBlocking

/** Manual device check of the live OCR service, separate from deterministic unit tests.
 * Run with adb shell am instrument -w -e path ... -e position ... -e query ...
 * gal.galiciana2.app.test/gal.galiciana.app.OcrDeviceVerification
 */
class OcrDeviceVerification : Instrumentation() {
    private var parameters = Bundle()
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        parameters = arguments ?: Bundle()
        start()
    }

    override fun onStart() {
        val result = Bundle()
        try {
            if (parameters.getString("mode") == "viewer-pinch") {
                ViewerPinchVerification.run(this, parameters, result)
                finish(android.app.Activity.RESULT_OK, result)
                return
            }
            runBlocking {
                if (parameters.getString("mode") == "search-matrix") {
                    val repo = OcrRepository()
                    val rows = mutableListOf<String>()
                    for (query in listOf("carcamo", "pedro carcamo", "pesro carcamo", "\"pedro carcamo\"", "Pedro Antonio Carcamo Alvarez")) {
                        try {
                            val found = repo.searchAllPagesDetailed(query, maxPages = 1)
                            rows += "$query: cargadas=${found.hits.size}, obrasAnunciadas=${found.totalWorks}, más=${found.nextUrl != null}, objetivo=${found.hits.any { it.path == "1510028" && it.position == 3 }}"
                        } catch (e: Exception) { rows += "$query: ERROR ${e.message}" }
                    }
                    result.putString("stream", rows.joinToString("\n"))
                    return@runBlocking
                }
                if (parameters.getString("mode") == "carcamo-identity") {
                    val document = gal.galiciana.app.data.MetsRepository().load("1510028")
                    sendStatus(0, Bundle().apply { putString("stream", "\n1510028: ${document.title} · ${document.pages.size} páginas\n") })
                    val repo = OcrRepository()
                    val query = "Pedro Antonio Carcamo Alvarez"
                    val page = repo.searchAllPagesDetailed(query, maxPages = 1)
                    val ordered = gal.galiciana.app.ui.search.sortGroups(repo.groupByWork(page.hits), gal.galiciana.app.ui.search.SearchViewModel.SortBy.Relevancia, false, query)
                    val target = ordered.flatMap { it.pages }.firstOrNull { it.path == "1510028" && it.position == 3 }
                    result.putString("stream", "\nConsulta sin tildes: target=${target != null}; título=${target?.title}; posición entre obras=${ordered.indexOfFirst { g -> g.pages.any { it.path == "1510028" && it.position == 3 } } + 1}; extractos=${target?.snippets?.sortedByDescending { gal.galiciana.app.ui.search.pageRelevance(target.copy(snippets=listOf(it)), query) }?.take(2)}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "carcamo-page") {
                    val repo = OcrRepository()
                    val text = repo.loadPage("1510028", 3, null).ocrText
                    val contexts = Regex("(?i)pedro|c.rcamo|lician").findAll(text).map { text.substring((it.range.first-70).coerceAtLeast(0), (it.range.last+130).coerceAtMost(text.length)) }.toList()
                    val q = "Pedro Antonio Cárcamo Alvarez"
                    val report = StringBuilder("OCR ${text.length} caracteres\n${contexts.joinToString("\n")}\n")
                    for (query in listOf(q, "Pedro Antonio Cárcamo", "Cárcamo Alvarez")) {
                        val page = repo.searchAllPagesDetailed(query, maxPages = 1)
                        report.append("$query: ${page.hits.size} resultados, target=${page.hits.any { it.path == "1510028" }}, más=${page.nextUrl != null}\n")
                        val ranked = gal.galiciana.app.ui.search.sortGroups(repo.groupByWork(page.hits),
                            gal.galiciana.app.ui.search.SearchViewModel.SortBy.Relevancia, false, query).flatMap { it.pages }
                        report.append("Página 1510028/3: orden servidor=${page.hits.indexOfFirst { it.path == "1510028" && it.position == 3 } + 1}; orden proximidad=${ranked.indexOfFirst { it.path == "1510028" && it.position == 3 } + 1}\n")
                    }
                    result.putString("stream", report.toString())
                    return@runBlocking
                }
                if (parameters.getString("mode") == "backup-storage") {
                    result.putString("stream", "\n${verifyBackupStorage(targetContext)}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "pdf-storage") {
                    val bitmap = android.graphics.Bitmap.createBitmap(240, 320, android.graphics.Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    val image = java.io.ByteArrayOutputStream().also { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)) }.toByteArray()
                    bitmap.recycle()
                    val pdf = gal.galiciana.app.data.PageDownloads.CombinedPdf(targetContext).use {
                        it.add(image, "Primera línea con Núñez\nSegunda línea de investigación\nÚltima línea")
                        it.bytes()
                    }
                    check(pdf.copyOfRange(0, 5).toString(Charsets.US_ASCII) == "%PDF-")
                    val file = java.io.File.createTempFile("pdf-verification-", ".pdf", targetContext.cacheDir)
                    try {
                        file.writeBytes(pdf)
                        android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                            android.graphics.pdf.PdfRenderer(descriptor).use { renderer ->
                                check(renderer.pageCount == 1)
                                renderer.openPage(0).use { page ->
                                    check(page.width == 240 && page.height == 320)
                                    val target = android.graphics.Bitmap.createBitmap(240, 320, android.graphics.Bitmap.Config.ARGB_8888)
                                    try { page.render(target, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        check(android.graphics.Color.red(target.getPixel(120, 160)) > 240)
                                    } finally { target.recycle() }
                                }
                            }
                        }
                    } finally { file.delete() }
                    result.putString("stream", "\nPDF: imagen generada, OCR multilínea, PDF válido y renderizado nativo: OK. No se utilizaron documentos ni datos reales.\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "notes-storage") {
                    val backing = targetContext.getSharedPreferences("codex_notes_probe", android.content.Context.MODE_PRIVATE)
                    backing.edit().clear().commit()
                    var failWrites = false
                    val commits = mutableListOf<Set<String>>()
                    val preferences = object : android.content.SharedPreferences by backing {
                        override fun edit(): android.content.SharedPreferences.Editor {
                            val delegate = backing.edit()
                            val keys = mutableSetOf<String>()
                            return object : android.content.SharedPreferences.Editor by delegate {
                                override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor {
                                    keys += key.orEmpty()
                                    delegate.putString(key, value)
                                    return this
                                }
                                override fun commit(): Boolean {
                                    commits += keys.toSet()
                                    return if (failWrites) false else delegate.commit()
                                }
                                override fun apply() { if (!failWrites) delegate.apply() }
                            }
                        }
                    }
                    val isolated = object : android.content.ContextWrapper(targetContext) {
                        override fun getSharedPreferences(name: String?, mode: Int): android.content.SharedPreferences = preferences
                    }
                    val store = gal.galiciana.app.data.LocalStore(isolated)
                    val hit = gal.galiciana.app.domain.OcrHit(title = "Prueba temporal", path = "probe", position = 1, imageId = null)
                    try {
                        check(store.toggleHit(hit))
                        val legacy = "Nota de referencia existente"
                        check(store.setNote("probe", 1, legacy))
                        check(gal.galiciana.app.data.LocalStore(isolated).noteOf("probe", 1) == legacy)
                        val longNote = "Investigación archivística.\n".repeat(200).take(4000)
                        commits.clear()
                        check(store.setNote("probe", 1, longNote))
                        check(commits.single() == setOf("notes_json", "fav_json", "fav_json_previous", "fav_stash"))
                        check(store.noteOf("probe", 1) == longNote)
                        check(store.favHits().single().note == longNote)
                        failWrites = true
                        check(!store.setNote("probe", 1, "No debe reemplazar la nota persistida"))
                        check(!store.setNote("probe", 1, ""))
                        check(store.noteOf("probe", 1) == longNote)
                        check(store.favHits().single().note == longNote)
                        check(!store.toggleHit(hit.copy(path = "failed-add")))
                        check(!store.isFav("failed-add", 1))
                        check(store.toggleHit(hit)) // A failed removal stays selected.
                        check(store.isFav("probe", 1))
                        failWrites = false
                        check(store.setNote("probe", 1, ""))
                        check(store.noteOf("probe", 1).isBlank() && store.isFav("probe", 1))
                        val noteToRemove = "Nota que no debe reaparecer"
                        check(store.setNote("probe", 1, noteToRemove))
                        val stale = hit.copy(note = noteToRemove)
                        check(!store.toggleHit(hit))
                        check(store.setNote("probe", 1, ""))
                        check(store.noteOf("probe", 1).isBlank())
                        check(store.toggleHit(stale))
                        check(store.favHits().single().note.isBlank())
                        check(!store.toggleHit(hit))
                        check(store.setNote("probe", 1, "Nueva nota de referencia"))
                        check(store.toggleHit(stale))
                        check(store.noteOf("probe", 1) == "Nueva nota de referencia")
                        check(store.favHits().single().note == "Nueva nota de referencia")
                        result.putString("stream", "\nNotas: texto existente, 4000 caracteres, escritura atómica, fallos de guardado/eliminación, favorito y eliminación: OK. Datos reales intactos.\n")
                    } finally { targetContext.deleteSharedPreferences("codex_notes_probe") }
                    return@runBlocking
                }
                if (parameters.getString("mode") == "catalogue-html-probe") {
                    val base = gal.galiciana.app.data.GalicianaUrls.BASE
                    fun get(url: String): String = gal.galiciana.app.data.Http.client.newCall(okhttp3.Request.Builder().url(url).build())
                        .execute().use { check(it.isSuccessful); it.body!!.string() }
                    val listUrl = "$base/es/consulta/resultados_busqueda.do?busq_secbib=CARTOGRAF&presentacion=mosaico"
                    val listing = get(listUrl)
                    val parsed = org.jsoup.Jsoup.parse(listing, listUrl)
                    val record = parsed.select("a[href*=registro.do]").firstOrNull()?.absUrl("href")
                        ?: Regex("<!--\\s*Id:\\s*(\\d+)\\s*-->").find(listing)?.groupValues?.get(1)?.let { "$base/es/consulta/registro.do?id=$it" }
                        ?: error("No se encontró una ficha: ${listing.length} bytes")
                    val html = get(record)
                    targetContext.openFileOutput("catalogue-probe.html", 0).use { it.write(html.toByteArray()) }
                    result.putString("stream", "Ficha: $record · ${html.length} bytes\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "hemeroteca-diagnostic") {
                    val endpoint = "${gal.galiciana.app.data.GalicianaUrls.BASE}/es/publicaciones/listar_cabeceras.do"
                    val summaries = mutableListOf<org.json.JSONObject>()
                    for ((label, url) in listOf("default" to endpoint, "explicit-a" to "$endpoint?letra=A&posicion=1")) {
                        val summary = org.json.JSONObject().put("request", label)
                        try {
                            val call = gal.galiciana.app.data.Http.client.newCall(okhttp3.Request.Builder().url(url).build())
                            call.timeout().timeout(20, java.util.concurrent.TimeUnit.SECONDS)
                            call.execute().use { response ->
                                val body = response.body?.string().orEmpty()
                                targetContext.openFileOutput("hemeroteca-$label.html", 0).use { it.write(body.toByteArray()) }
                                val finalUrl = response.request.url.toString()
                                val doc = org.jsoup.Jsoup.parse(body, finalUrl)
                                summary.put("http", response.code).put("finalUrl", finalUrl)
                                    .put("bodyLength", body.length).put("title", doc.title())
                                    .put("publicationLinks", doc.select("a[href*=idPublicacion]").size)
                                    .put("indexRows", doc.select("tr.nav_registro").size)
                                    .put("currentLabels", doc.select(".pagina_actual").text())
                                try {
                                    val parsed = gal.galiciana.app.data.HemerotecaRepository().parseIndice(body, finalUrl)
                                    summary.put("parsedPublications", parsed.cabeceras.size)
                                        .put("parsedLetters", parsed.letras.size)
                                        .put("parsedPage", parsed.pageNumber)
                                } catch (e: Exception) {
                                    summary.put("parseException", "${e.javaClass.simpleName}: ${e.message.orEmpty()}")
                                }
                            }
                        } catch (e: Exception) {
                            summary.put("networkException", "${e.javaClass.simpleName}: ${e.message.orEmpty()}")
                        }
                        summaries += summary
                    }
                    val repository = org.json.JSONObject().put("request", "repository-default")
                    try {
                        val parsed = gal.galiciana.app.data.HemerotecaRepository().indice()
                        repository.put("publications", parsed.cabeceras.size).put("letters", parsed.letras.size)
                            .put("page", parsed.pageNumber).put("sourceUrl", parsed.sourceUrl)
                    } catch (e: Exception) {
                        repository.put("exception", "${e.javaClass.simpleName}: ${e.message.orEmpty()}")
                    }
                    summaries += repository
                    val output = org.json.JSONArray(summaries).toString(2)
                    targetContext.openFileOutput("hemeroteca-diagnostic.json", 0).use { it.write(output.toByteArray()) }
                    result.putString("stream", "\n$output\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "editions") {
                    val repo = gal.galiciana.app.data.HemerotecaRepository()
                    val headers = repo.searchIndice("El pueblo gallego").cabeceras
                    val lines = mutableListOf<String>()
                    for (header in headers) {
                        val issues = repo.loadNumeros(header.id, "1975")
                        val sameDay = issues.filter { it.date == java.time.LocalDate.of(1975, 7, 23) }
                        lines += "${header.id} ${header.title}: ${sameDay.map { it.path to it.label }}"
                    }
                    result.putString("stream", "\n${lines.joinToString("\n")}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "viewer-links") {
                    val url = gal.galiciana.app.data.GalicianaUrls.viewer("1264554", 15)
                    val call = gal.galiciana.app.data.Http.client.newCall(okhttp3.Request.Builder().url(url)
                        .header("User-Agent", "Mozilla/5.0").build())
                    call.timeout().timeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    val html = call.execute().use { check(it.isSuccessful); it.body?.string().orEmpty() }
                    targetContext.openFileOutput("reference-viewer.html", 0).use { it.write(html.toByteArray()) }
                    val doc = org.jsoup.Jsoup.parse(html, url)
                    val links = doc.select("a[href]").filter {
                        (it.attr("href") + it.text()).contains(Regex("(?i)ocr|alto|texto|descarga"))
                    }.map { it.text() + " " + it.absUrl("href") }
                    doc.selectFirst("img[src*='imagen_id.do']")?.absUrl("src")?.let { imageUrl ->
                        val imageCall = gal.galiciana.app.data.Http.client.newCall(okhttp3.Request.Builder().url(imageUrl)
                            .header("User-Agent", "Mozilla/5.0").header("Referer", url).build())
                        imageCall.timeout().timeout(20, java.util.concurrent.TimeUnit.SECONDS)
                        val bytes = imageCall.execute().use { check(it.isSuccessful); it.body!!.bytes() }
                        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        check(bounds.outWidth > 0)
                        targetContext.openFileOutput("reference-preview.jpg", 0).use { it.write(bytes) }
                        links.plus("Image ${bounds.outWidth}x${bounds.outHeight}")
                    }
                    result.putString("stream", "\ntitle=${doc.title()} bytes=${html.length}\n${links.joinToString("\n")}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "open-reference") {
                    val referencePath = parameters.getString("path") ?: "1264535"
                    val document = gal.galiciana.app.data.MetsRepository().load(referencePath)
                    val position = parameters.getString("position")?.toIntOrNull() ?: 15
                    val page = document.pages.first { it.position == position }
                    val reading = org.json.JSONObject().put("path", referencePath).put("pos", position)
                        .put("title", "El Pueblo Gallego · 01/07/1975").put("imageId", page.imageId)
                        .put("q", parameters.getString("query") ?: "\"Federación Gallega\"")
                    check(targetContext.getSharedPreferences("galiciana_fans", 0).edit()
                        .putString("last_reading", reading.toString()).commit())
                    result.putString("stream", "\nPrepared position $position image ${page.imageId}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "image") {
                    val call = gal.galiciana.app.data.Http.client.newCall(okhttp3.Request.Builder()
                        .url(gal.galiciana.app.data.GalicianaUrls.jpeg(parameters.getString("imageId") ?: "12589450") + "&formato=jpg&registrardownload=0")
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", gal.galiciana.app.data.GalicianaUrls.viewer("1264554", 15)).build())
                    call.timeout().timeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    val bytes = call.execute().use { check(it.isSuccessful); it.body!!.bytes() }
                    val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    check(options.outWidth > 0) { "Respuesta sin imagen" }
                    targetContext.openFileOutput("reference-preview.jpg", 0).use { it.write(bytes) }
                    result.putString("stream", "\nImage ${options.outWidth}x${options.outHeight}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "reference-search") {
                    val repo = OcrRepository()
                    val rows = org.json.JSONArray()
                    for (query in listOf("Federación Gallega", "Elisardo Núñez Seabra", "Manuel Fernández", "Amador",
                        "Regimiento de Infantería Zamora", "Xinzo de Limia", "Javier Vázquez")) {
                        val row = org.json.JSONObject().put("query", query)
                        try {
                            var page = repo.searchAllPagesDetailed("\"$query\"", title = "pueblo gallego",
                                yearFrom = "1975", yearTo = "1975", maxPages = 1)
                            var loaded = page.hits.size
                            var pages = 1
                            var found = page.hits.any { it.path == "1264535" && it.position == 15 }
                            val visited = mutableSetOf<String>()
                            while (!found && pages < 20) {
                                val next = page.nextUrl ?: break
                                if (!visited.add(next)) break
                                page = repo.searchNextPage(next)
                                pages++
                                loaded += page.hits.size
                                found = page.hits.any { it.path == "1264535" && it.position == 15 }
                                sendStatus(0, Bundle().apply { putString("stream", "\n$query: $pages páginas, $loaded resultados\n") })
                            }
                            row.put("loadedHits", loaded).put("pages", pages).put("hasMore", page.nextUrl != null)
                                .put("referenceFound", found)
                        } catch (failure: Exception) { row.put("error", failure.message) }
                        rows.put(row)
                        sendStatus(0, Bundle().apply { putString("stream", "\n$row\n") })
                    }
                    targetContext.openFileOutput("reference-search-report.json", 0).use { it.write(rows.toString(2).toByteArray()) }
                    result.putString("stream", "\n${rows.toString(2)}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "reference") {
                    result.putString("stream", verifyUserReferencePage(targetContext,
                        parameters.getString("path"), parameters.getString("position")?.toIntOrNull()) { message ->
                        sendStatus(0, Bundle().apply { putString("stream", "\n$message\n") })
                    })
                    return@runBlocking
                }
                if (parameters.getString("mode") == "detail") {
                    val repo = OcrRepository()
                    val path = "1504641"
                    val text = repo.loadPage(path, 3, "14293667").ocrText
                    val call = gal.galiciana.app.data.Http.client.newCall(okhttp3.Request.Builder()
                        .url(gal.galiciana.app.data.GalicianaUrls.alto(path, 3)).build())
                    call.timeout().timeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    val xml = call.execute().use { it.body?.string().orEmpty() }
                    val doc = org.jsoup.Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())
                    val words = doc.getAllElements().filter { it.tagName().substringAfter(':').equals("String", true) }
                    val rows = words.withIndex().filter { indexed ->
                        val value = indexed.value.attr("CONTENT")
                        gal.galiciana.app.ui.foldForSearch(value).let { it.contains("fer") || it.contains("nandez") }
                    }.flatMap { indexed ->
                        words.subList((indexed.index - 1).coerceAtLeast(0), (indexed.index + 3).coerceAtMost(words.size))
                    }.distinct().take(50).map { element ->
                        "${element.outerHtml()} block=${element.parents().firstOrNull { it.tagName().substringAfter(':') == "TextBlock" }?.id()}"
                    }
                    val contexts = gal.galiciana.app.ui.highlightRegex("\"Fernández\"")!!.findAll(text)
                        .map { text.substring((it.range.first - 40).coerceAtLeast(0), (it.range.last + 41).coerceAtMost(text.length)) }.toList()
                    val strict = repo.parseAltoSearch(xml, "\"Fernández\"")
                    val loose = repo.parseAltoSearch(xml, "Manuel Fernández")
                    result.putString("stream", "\ntext contexts=$contexts\nALTO strict=${strict.matches.size} plain=${loose.matches.size}\n${rows.joinToString("\n")}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "examples") {
                    val report = verifyHyphenExamples(parameters.getString("case"), parameters.getString("year")) { message ->
                        sendStatus(0, Bundle().apply { putString("stream", "\n$message\n") })
                    }
                    val file = if (parameters.getString("case") == null) "hyphen-examples.json" else "hyphen-retry.json"
                    targetContext.openFileOutput(file, 0).use { it.write(report.toByteArray()) }
                    result.putString("stream", "\n$report\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "open-example") {
                    val array = org.json.JSONArray(targetContext.openFileInput("hyphen-examples.json")
                        .bufferedReader().use { it.readText() })
                    val wanted = parameters.getString("case") ?: "Federación"
                    val item = (0 until array.length()).map { array.getJSONObject(it) }
                        .first { it.optBoolean("found") && it.getString("query").contains(wanted, true) }
                    gal.galiciana.app.data.LocalStore(targetContext).rememberReading(
                        gal.galiciana.app.domain.OcrHit(title = item.getString("title"),
                            path = item.getString("path"), position = item.getInt("position"),
                            imageId = item.optString("imageId").ifBlank { null },
                            savedQuery = "\"${item.getString("query")}\""))
                    result.putString("stream", "\nPrepared ${item.getString("title")} page ${item.getInt("position")}\n")
                    return@runBlocking
                }
                if (parameters.getString("mode") == "search") {
                    val client = gal.galiciana.app.data.Http.client.newBuilder()
                        .callTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build()
                    val base = gal.galiciana.app.data.GalicianaUrls.BASE
                    val reports = mutableListOf<String>()
                    fun probe(label: String, request: okhttp3.Request) {
                        try {
                            client.newCall(request).execute().use { response ->
                                val html = response.body?.string().orEmpty()
                                val doc = org.jsoup.Jsoup.parse(html)
                                reports += "$label status=${response.code} url=${response.request.url} " +
                                    "redirect=${response.priorResponse?.code} title=${doc.title()} bytes=${html.length} " +
                                    "hits=${OcrRepository().parseHits(html).size} text=${doc.body().text().take(700)} " +
                                    "forms=${doc.select("form").map { it.attr("action") }} " +
                                    "fields=${doc.select("input[name]").map { it.attr("name") }.distinct()}"
                            }
                        } catch (failure: Exception) { reports += "$label failed=${failure.message}" }
                    }
                    fun request(full: Boolean) = okhttp3.Request.Builder()
                        .url("$base/es/consulta/resultados_ocr.do")
                        .post(okhttp3.FormBody.Builder().apply {
                            add("general_ocr", "on"); add("busq_general", "Antonio Carcamo Alvarez")
                            if (full) {
                                listOf("busq_titulo", "busq_autor", "busq_lugaredicion",
                                    "busq_rango0_fechainicial__fechafinal", "busq_rango1_fechainicial__fechafinal")
                                    .forEach { add(it, "") }
                                add("busq_objetosmultimedia", "true")
                            }
                        }.build()).build()
                    probe("minimal", request(false))
                    probe("full", request(true))
                    probe("form", okhttp3.Request.Builder().url("$base/es/consulta/busqueda.do").build())
                    probe("after-form", request(false).newBuilder().header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "$base/es/consulta/busqueda.do").build())
                    try {
                        val first = OcrRepository().searchAllPagesDetailed("Antonio Carcamo Alvarez", maxPages = 1)
                        reports += "repository hits=${first.hits.size} sortKeys=${first.sortUrls.keys}"
                        first.sortUrls["Relevancia:S"]?.let { url ->
                            probe("sort-relevance", okhttp3.Request.Builder().url(url).build())
                        }
                    } catch (failure: Exception) { reports += "repository failed=${failure.message}" }
                    listOf("Pedro Antonio Carcamo Alvarez", "Pedro Carcamo Alvarez").forEach { term ->
                        probe(term, okhttp3.Request.Builder().url("$base/es/consulta/resultados_ocr.do")
                            .post(okhttp3.FormBody.Builder().add("general_ocr", "on").add("busq_general", term).build())
                            .build())
                    }
                    result.putString("stream", "\n" + reports.joinToString("\n") + "\n")
                    return@runBlocking
                }
                val repo = OcrRepository()
                val path = parameters.getString("path") ?: "1472957"
                val position = parameters.getString("position")?.toIntOrNull() ?: 1
                val image = parameters.getString("imageId")
                val text = repo.loadPage(path, position, image).ocrText
                check(text.isNotBlank()) { "The page returned no OCR text." }
                val presentWord = queryTokens(text).firstOrNull { word ->
                    word.length in 6..12 && word.all(Char::isLetter)
                } ?: error("No readable test word in this page.")
                val split = Regex("""([\p{L}]{2,})[-‐‑–—­]\s+([\p{L}]{2,})""").find(text)
                val splitQuery = split?.let { "\"${it.groupValues[1]}${it.groupValues[2]}\"" }
                val queries = (if (parameters.getString("mode") == "federation") listOf(
                    "\"Federación Gallega\"", "\"Federación Ga-llega\"", "\"Gallega\"")
                    else if (parameters.getString("mode") == "names") listOfNotNull(
                    "Pedro Antonio Carcamo Alvarez", "Antonio Carcamo Alvarez", "Pedro Carcamo Alvarez",
                    splitQuery) else listOfNotNull(parameters.getString("query"),
                    "\"$presentWord\"", "zzqvxzzqvxzz")).distinct()
                val reports = queries.map { query ->
                    val positions = repo.loadAltoSearch(path, position, query, image)
                    val textCount = countMatches(text, query)
                    if (query == "zzqvxzzqvxzz") check(textCount == 0 && positions.matches.isEmpty())
                    "query=$query textCount=$textCount imageMatches=${positions.matches.size} " +
                        "boxes=${positions.hits.size} status=${positions.status} page=${positions.pageSize}"
                }
                result.putString("stream", "\npath=$path position=$position splitWordFound=${split != null}\n" + reports.joinToString("\n") + "\n")
                result.putString("presentWord", presentWord)
            }
            finish(android.app.Activity.RESULT_OK, result)
        } catch (failure: Throwable) {
            result.putString("stream", "\nOCR live verification failed: ${failure.message}\n")
            finish(android.app.Activity.RESULT_CANCELED, result)
        }
    }
}
