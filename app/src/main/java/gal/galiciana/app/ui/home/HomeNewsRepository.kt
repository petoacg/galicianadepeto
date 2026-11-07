package gal.galiciana.app.ui.home

import gal.galiciana.app.data.GalicianaUrls
import gal.galiciana.app.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

internal data class HomeNews(val title: String, val description: String, val url: String)

internal class HomeNewsRepository {
    private val client = Http.client.newBuilder().callTimeout(18, TimeUnit.SECONDS).build()
    companion object {
        const val NEWS_URL = "https://biblioteca.galiciana.gal/es/cms/coleccion.do?id=novedades%2Fnoticias"
    }

    suspend fun latest(): List<HomeNews> = withContext(Dispatchers.IO) {
        val html = fetch(NEWS_URL)
        val items = parse(html)
        if (items.isEmpty()) error("Galiciana no ha devuelto noticias")
        items.take(3).map { item ->
            if (item.description.isNotBlank()) item
            else item.copy(description = runCatching { articleDescription(item.url) }.getOrDefault(""))
        }
    }

    internal fun parse(html: String): List<HomeNews> {
        val doc = Jsoup.parse(html, NEWS_URL)
        return doc.select("#contenido a[href], #pagina_contenido a[href]")
            .mapNotNull { anchor ->
                val url = anchor.absUrl("href")
                if (!url.startsWith("${GalicianaUrls.BASE}/es/cms/elemento.do?")) return@mapNotNull null
                val decoded = URLDecoder.decode(url, "UTF-8")
                if (!decoded.contains("id=novedades/noticias/")) return@mapNotNull null
                val card = anchor.parents().firstOrNull { parent ->
                    parent.tagName() in setOf("article", "li") ||
                        parent.classNames().any { it.contains("noticia", true) || it.contains("news", true) }
                } ?: anchor.parent() ?: return@mapNotNull null
                val heading = card.selectFirst("h2, h3, h4")?.text()?.trim().orEmpty()
                val title = heading.ifBlank { anchor.text().trim() }
                if (title.length < 8 || title.equals("Ver más", true)) return@mapNotNull null
                val description = card.select(".resumen, .descripcion, p")
                    .map { it.text().trim() }
                    .firstOrNull { it.length > 20 && it != title }.orEmpty()
                HomeNews(title, description, url)
            }.distinctBy { item ->
                // The CMS can link the same article through several URL variants.
                // A repeated visible headline is one news item for this list.
                item.title.lowercase().replace(Regex("\\s+"), " ").trim()
            }
    }

    private fun articleDescription(url: String): String {
        val doc = Jsoup.parse(fetch(url), url)
        return doc.select("#contenido p, #pagina_contenido p")
            .map { it.text().trim() }
            .firstOrNull { it.length > 35 && it.length < 1500 }.orEmpty()
    }

    private fun fetch(url: String): String = client.newCall(Request.Builder().url(url).build())
        .execute().use { response ->
            if (!response.isSuccessful) error("Galiciana: HTTP ${response.code}")
            response.body?.string() ?: error("Respuesta vacía de Galiciana")
        }
}
