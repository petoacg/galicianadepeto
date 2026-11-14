package gal.galiciana.app.data

import gal.galiciana.app.i18n.tr
import gal.galiciana.app.domain.Work
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

class OaiRepository {
    var nextToken: String? = null
        private set

    suspend fun firstPage(): List<Work> = fetch(
        GalicianaUrls.OAI.toHttpUrl().newBuilder()
            .addQueryParameter("verb", "ListRecords")
            .addQueryParameter("metadataPrefix", "oai_dc").build().toString()
    )

    suspend fun nextPage(): List<Work> {
        val token = nextToken ?: return emptyList()
        return fetch(GalicianaUrls.OAI.toHttpUrl().newBuilder()
            .addQueryParameter("verb", "ListRecords")
            .addQueryParameter("resumptionToken", token).build().toString())
    }

    private suspend fun fetch(url: String): List<Work> = withContext(Dispatchers.IO) {
        val body = Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            check(response.isSuccessful) { tr("Non se puido consultar o repositorio. Comproba a conexión e téntao de novo.", "No se pudo consultar el repositorio. Comprueba la conexión y reintenta.") }
            response.body?.string()?.takeIf { it.isNotBlank() }
                ?: error(tr("O repositorio devolveu unha resposta baleira. Téntao de novo.", "El repositorio ha devuelto una respuesta vacía. Vuelve a intentarlo."))
        }
        acceptPage(body)
    }

    /** Pagination changes only after every record and the response envelope validate. */
    internal fun acceptPage(xml: String): List<Work> {
        val parsed = parsePage(xml)
        nextToken = parsed.nextToken
        return parsed.records
    }

    internal data class OaiPage(val records: List<Work>, val nextToken: String?)

    fun parseRecords(xml: String): List<Work> = parsePage(xml).records

    internal fun parsePage(xml: String): OaiPage {
        // Jsoup intentionally repairs incomplete XML. A truncated OAI page must
        // not be accepted as the end of a valid pagination session.
        check(!xml.contains("<!DOCTYPE", ignoreCase = true)) {
            tr("O repositorio devolveu un formato XML non permitido.", "El repositorio ha devuelto un formato XML no permitido.")
        }
        try {
            val reader = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
                .newSAXParser().xmlReader
            val handler = object : DefaultHandler() {
                override fun error(e: SAXParseException) { throw e }
                override fun fatalError(e: SAXParseException) { throw e }
            }
            reader.contentHandler = handler
            reader.errorHandler = handler
            reader.entityResolver = org.xml.sax.EntityResolver { _, _ -> InputSource(StringReader("")) }
            reader.parse(InputSource(StringReader(xml)))
        } catch (e: Exception) {
            throw IllegalStateException(tr("O repositorio devolveu XML incompleto ou non válido. Téntao de novo.", "El repositorio ha devuelto XML incompleto o inválido. Vuelve a intentarlo."), e)
        }
        val document = Jsoup.parse(xml, "", Parser.xmlParser())
        fun Element.localName() = tagName().substringAfter(':')
        fun Element.child(name: String): Element? = children().firstOrNull { it.localName() == name }
        val root = document.children().singleOrNull()?.takeIf { it.localName() == "OAI-PMH" }
            ?: error(tr("O repositorio non devolveu unha resposta OAI válida. Téntao de novo.", "El repositorio no ha devuelto una respuesta OAI válida. Vuelve a intentarlo."))
        val namespace = root.attr(if (root.tagName().contains(':')) "xmlns:${root.tagName().substringBefore(':')}" else "xmlns")
        check(namespace.isBlank() || namespace == "http://www.openarchives.org/OAI/2.0/") {
            tr("O repositorio devolveu un formato de resposta descoñecido.", "El repositorio ha devuelto un formato de respuesta desconocido.")
        }
        val errors = root.children().filter { it.localName() == "error" }
        if (errors.isNotEmpty()) {
            if (errors.all { it.attr("code") == "noRecordsMatch" }) return OaiPage(emptyList(), null)
            error(if (errors.any { it.attr("code") == "badResumptionToken" })
                tr("A sesión de rexistros caducou. Carga de novo o catálogo.", "La sesión de registros ha caducado. Vuelve a cargar el catálogo.")
            else tr("O repositorio non puido completar a consulta. Téntao de novo.", "El repositorio no pudo completar la consulta. Vuelve a intentarlo."))
        }
        val records = root.child("ListRecords")
            ?: error(tr("O repositorio non devolveu a lista de rexistros solicitada. Téntao de novo.", "El repositorio no ha devuelto la lista de registros solicitada. Vuelve a intentarlo."))
        val works = records.children().filter { it.localName() == "record" }.mapNotNull { record ->
            val header = record.child("header") ?: error(tr("O repositorio devolveu un rexistro incompleto.", "El repositorio ha devuelto un registro incompleto."))
            if (header.attr("status").equals("deleted", true)) return@mapNotNull null
            val oaiId = header.child("identifier")?.text()?.takeIf { it.isNotBlank() }
                ?: error(tr("O repositorio devolveu un rexistro sen identificador.", "El repositorio ha devuelto un registro sin identificador."))
            val metadata = record.child("metadata") ?: error(tr("O repositorio devolveu un rexistro sen metadatos.", "El repositorio ha devuelto un registro sin metadatos."))
            fun values(name: String): List<String> = metadata.getAllElements()
                .filter { it !== metadata && it.localName() == name }
                .map { it.text().trim() }.filter { it.isNotBlank() }
            val identifiers = values("identifier")
            val recordUrl = identifiers.firstOrNull { value ->
                value.toHttpUrlOrNull()?.let { it.encodedPath.endsWith("/registro.do") && it.queryParameter("id") != null } == true
            }
            val viewerUrl = identifiers.firstOrNull { value ->
                value.toHttpUrlOrNull()?.let { it.encodedPath.endsWith("/grupo.do") && it.queryParameter("path") != null } == true
            }
            Work(
                oaiId = oaiId,
                catalogId = recordUrl?.toHttpUrlOrNull()?.queryParameter("id"),
                path = viewerUrl?.toHttpUrlOrNull()?.queryParameter("path"),
                title = values("title").firstOrNull() ?: tr("(sen título)", "(sin título)"),
                creators = values("creator"),
                date = values("date").firstOrNull(),
                rights = values("rights").firstOrNull(),
                recordUrl = recordUrl,
                viewerUrl = viewerUrl,
            )
        }
        val token = records.child("resumptionToken")?.wholeText()?.trim()?.takeIf { it.isNotBlank() }
        return OaiPage(works, token)
    }
}
