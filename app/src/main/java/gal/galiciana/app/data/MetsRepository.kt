package gal.galiciana.app.data

import gal.galiciana.app.domain.MetsDocument
import gal.galiciana.app.domain.MetsPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.nodes.Element

class MetsRepository {
    suspend fun load(path: String): MetsDocument = withContext(Dispatchers.IO) {
        Http.client.newCall(Request.Builder().url(GalicianaUrls.mets(path)).build())
            .execute().use { response ->
                if (!response.isSuccessful) error("METS: HTTP ${response.code}")
                parse(path, response.body?.string().orEmpty())
            }
    }

    fun parse(path: String, xml: String): MetsDocument {
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        fun Element.tagIs(name: String) = tagName().substringAfterLast(':').equals(name, true)
        val title = doc.getAllElements().firstOrNull { it.tagIs("title") }?.text()
        val files = doc.getAllElements().filter { it.tagIs("file") }.associate { file ->
            val href = file.getAllElements().firstOrNull { it.tagIs("FLocat") }?.let {
                it.attr("xlink:href").ifBlank { it.attr("href") }
            }.orEmpty()
            file.attr("ID") to href
        }
        fun imageId(href: String): String? =
            Regex("""(?:imagen_id\.do\?[^"\s]*?idImagen=|object-miniature\.do\?id=)(\d+)""",
                RegexOption.IGNORE_CASE).find(href.replace("&amp;", "&"))?.groupValues?.get(1)

        val divisions = doc.getAllElements().filter { div ->
            div.tagIs("div") && div.getAllElements().any { it.tagIs("fptr") } &&
                div.getAllElements().none { it !== div && it.tagIs("div") }
        }
        val structured = divisions.mapNotNull { div ->
            val refs = div.getAllElements().filter { it.tagIs("fptr") }.map { it.attr("FILEID") }
            val refsHref = refs.mapNotNull { files[it] }
            val id = refsHref.firstNotNullOfOrNull(::imageId) ?: return@mapNotNull null
            Triple(div.attr("ORDER").toIntOrNull(), id, refsHref.any {
                it.contains("alto", true) || it.contains("ocr", true)
            })
        }
        val ordered = structured.sortedWith(compareBy<Triple<Int?, String, Boolean>> { it.first ?: Int.MAX_VALUE })
            .distinctBy { it.second }
        val pages = if (ordered.isNotEmpty()) ordered.mapIndexed { index, (_, id, ocr) ->
            MetsPage(index + 1, id, "Pág. ${index + 1}", ocr)
        } else {
            files.values.mapNotNull(::imageId).distinct().mapIndexed { index, id ->
                MetsPage(index + 1, id, "Pág. ${index + 1}", false)
            }
        }
        if (pages.isEmpty()) error("El METS no contiene páginas de imagen reconocibles")
        return MetsDocument(path, title, pages)
    }
}
