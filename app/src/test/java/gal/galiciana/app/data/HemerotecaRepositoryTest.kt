package gal.galiciana.app.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HemerotecaRepositoryTest {
    private val repo = HemerotecaRepository()
    private val index = "${GalicianaUrls.BASE}/es/publicaciones/listar_cabeceras.do"

    @Test fun realIndexUsesTitlesInsteadOfIssueCountsAndExposesActualPagination() {
        val html = javaClass.classLoader!!.getResourceAsStream("hemeroteca/cabeceras_a_page1.html")!!
            .bufferedReader().use { it.readText() }
        val result = repo.parseIndice(html)
        assertEquals(25, result.cabeceras.size)
        assertEquals("1542", result.cabeceras.first().id)
        assertTrue(result.cabeceras.first().title.startsWith("A.C.G : revista mensual ilustrada"))
        assertTrue(result.cabeceras.all { it.title.any(Char::isLetter) })
        assertFalse(result.cabeceras.any { it.title in listOf("44", "13", "2", "3", "4") })
        assertEquals(1, result.pageNumber)
        assertNull(result.previousUrl)
        assertEquals("26", result.nextUrl!!.toHttpUrl().queryParameter("posicion"))
        assertEquals(setOf("26", "51", "76"), result.paginas.map {
            it.second.toHttpUrl().queryParameter("posicion")
        }.toSet())
        assertEquals("A", result.selectedLetter)
        assertTrue(result.letras.any { it.first == "A" })
        assertTrue(result.letras.any { it.first == "Ñ" })
        assertTrue(result.paginas.all { it.second.toHttpUrl().queryParameter("letra") == "A" })
        assertEquals("A", result.sourceUrl!!.toHttpUrl().queryParameter("letra"))
    }

    @Test fun imageOnlyPreviousAndNextLinksKeepSelectedLetterOnMiddlePages() {
        val html = """
            <span class="pagina_actual">Ñ</span><span class="pagina_actual">2</span>
            <a id="boton_anterior" href="listar_cabeceras.do?posicion=1"><img alt="Anterior"/></a>
            <a id="boton_siguiente" href="listar_cabeceras.do?posicion=51"><img alt="Siguiente"/></a>
            <a href="listar_cabeceras.do?letra=B&amp;posicion=1">B</a>
        """.trimIndent()
        val result = repo.parseIndice(html, "$index?posicion=26")
        assertEquals(2, result.pageNumber)
        assertEquals("Ñ", result.selectedLetter)
        assertEquals("1", result.previousUrl!!.toHttpUrl().queryParameter("posicion"))
        assertEquals("51", result.nextUrl!!.toHttpUrl().queryParameter("posicion"))
        assertEquals("Ñ", result.nextUrl.toHttpUrl().queryParameter("letra"))
        assertEquals("Ñ", result.sourceUrl!!.toHttpUrl().queryParameter("letra"))
        assertEquals("B", result.letras.first { it.first == "B" }.second.toHttpUrl().queryParameter("letra"))
    }

    @Test fun titleFilterTravelsWithPaginationButNotWithAnExplicitNewLetter() {
        val html = """
            <form><input name="patron" value="El Pueblo Gallego"/></form>
            <span class="pagina_actual">1</span>
            <a id="boton_siguiente" href="listar_cabeceras.do?posicion=26">Siguiente</a>
            <a href="listar_cabeceras.do?letra=P&amp;posicion=1">P</a>
        """.trimIndent()
        val result = repo.parseIndice(html)
        val next = result.nextUrl!!.toHttpUrl()
        assertEquals("El Pueblo Gallego", next.queryParameter("patron"))
        assertEquals("no", next.queryParameter("letra"))
        assertNull(result.selectedLetter)
        assertNull(result.letras.single().second.toHttpUrl().queryParameter("patron"))
        val restored = repo.parseIndice("<span class='pagina_actual'>2</span>", next.toString())
        assertEquals("El Pueblo Gallego", restored.sourceUrl!!.toHttpUrl().queryParameter("patron"))
    }

    @Test fun paginationNeverUsesAnotherHostOrInventsUnavailablePageNumbers() {
        val html = """
            <table class="nav_registros"><tr><th>Nombre</th></tr></table>
            <a id="boton_siguiente" href="https://example.org/es/publicaciones/listar_cabeceras.do?posicion=26">Siguiente</a>
        """.trimIndent()
        val result = repo.parseIndice(html)
        assertNull(result.nextUrl)
        assertNull(result.pageNumber)
        assertTrue(result.paginas.isEmpty())
    }

    @Test fun titleMatchingIgnoresAccentsAndRepeatedWhitespace() {
        assertTrue(repo.matches("Federación   Gallega", "federacion gallega"))
        assertFalse(repo.matches("A.C.G : revista mensual", "Pueblo Gallego"))
    }

    @Test fun issueRowsKeepTheirOwnDatesAndOnlyDigitalCopiesBecomeNumbers() {
        val html = """
            <table>
            <tr><td>Número 71 · 1 xullo 1975</td><td><a href="grupo.do?path=111">Copia dixital</a></td></tr>
            <tr><td>Número 72 · 02/07/1975</td><td><a href="grupo.do?path=222">Copia digital</a></td></tr>
            <tr><td><a href="grupo.do?path=999">Versión PDF</a></td></tr>
            </table>
        """.trimIndent()
        val issues = repo.parseNumeros(html, "1975")
        assertEquals(listOf("111", "222"), issues.map { it.path })
        assertEquals(listOf(LocalDate.of(1975, 7, 1), LocalDate.of(1975, 7, 2)), issues.map { it.date })
        assertEquals(listOf(7, 7), issues.map { it.month })
    }
    @Test fun unexpectedHtmlCannotBecomeASuccessfulEmptyIndex() {
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            repo.parseIndice("<html><body>Servicio no disponible</body></html>")
        }
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            repo.parseIndice("")
        }
    }

    @Test fun explicitNoMatchesIsALegitimateEmptyTitleSearch() {
        val result = repo.parseIndice("<div id='publicaciones'>No se han encontrado publicaciones con ese título.</div>")
        assertTrue(result.cabeceras.isEmpty())
        assertNull(result.nextUrl)
    }

}
