package gal.galiciana.app.data

import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.ui.search.hitMatchesQuotedConstraints
import org.junit.Assert.*
import org.junit.Test

class SearchHitMergeTest {
    private fun hit(path: String = "1510028", position: Int = 3, vararg excerpts: String) = OcrHit(
        title = "Boletín de la provincia", path = path, imageId = "image-$path-$position", position = position,
        snippets = excerpts.toList())

    @Test fun duplicatePagesUnionTheirExcerptsWithoutMovingServerOrderOrOtherPages() {
        val first = hit(excerpts = arrayOf("Pedro intervino en otro asunto"))
        val other = hit(path = "other", excerpts = arrayOf("Otra página"))
        val repeated = first.copy(snippets = listOf("Pedro Antonio Cárcamo Álvarez", "Pedro intervino en otro asunto"), parentTitle = "Boletín")
        val result = mergePageHitsByIdentity(listOf(first, other, repeated))
        assertEquals(listOf("1510028", "other"), result.map { it.path })
        assertEquals(listOf("Pedro intervino en otro asunto", "Pedro Antonio Cárcamo Álvarez"), result.first().snippets)
        assertEquals("Boletín", result.first().parentTitle)
    }

    @Test fun separatePositionsAndImagesRemainSeparateIdentities() {
        val first = hit()
        assertEquals(3, mergePageHitsByIdentity(listOf(first, first.copy(position = 4), first.copy(imageId = "another-image"))).size)
    }

    @Test fun htmlDuplicateUnitsRetainQuotedEvidenceFromTheLaterOccurrence() {
        fun unit(excerpt: String) = """
            <div class="unidad_textual"><a href="/es/catalogo_imagenes/grupo.do?path=1510028&amp;posicion=3&amp;idImagen=12"
            data-analytics-recordtitle="Boletín de la provincia">Página 3</a>
            <ul class="texto_ocurrencias"><li>$excerpt</li></ul></div>
        """.trimIndent()
        val units = unit("Pedro intervino en otro asunto") + unit("Pedro Antonio Cárcamo Álvarez")
        for (html in listOf(units, "<div class='nav_registros'><div class='nav_registro'>$units</div></div>")) {
            val page = OcrRepository().parseHits(html).single()
            assertEquals(2, page.snippets.size)
            assertTrue(hitMatchesQuotedConstraints(page, "\"Pedro Antonio Cárcamo Álvarez\""))
        }
    }

    @Test fun exactPhraseFoundDuringRecoveryIsNotSilentlyDropped() {
        val precise = hit(excerpts = arrayOf("Pedro Antonio Cárcamo Álvarez"))
        val broad = hit(path = "other", excerpts = arrayOf("Pedro habló con Cárcamo"))
        val selected = selectPageCandidates(listOf(precise, broad), emptyList(), 50)
        assertEquals(listOf(precise, broad), selected.hits)
        assertFalse(selected.truncated)
        assertEquals(listOf(broad), selectPageCandidates(listOf(precise, broad), listOf(precise), 50).hits)
    }

    @Test fun localLimitFlagCountsUniqueCandidatesAfterExclusion() {
        val first = hit(); val second = hit(path = "second"); val third = hit(path = "third")
        assertFalse(selectPageCandidates(listOf(first, first, second), emptyList(), 2).truncated)
        val limited = selectPageCandidates(listOf(first, first, second, third), emptyList(), 2)
        assertEquals(listOf(first, second), limited.hits)
        assertTrue(limited.truncated)
        assertFalse(selectPageCandidates(listOf(first, second, third), listOf(first), 2).truncated)
    }
}
