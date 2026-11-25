package gal.galiciana.app

import gal.galiciana.app.domain.*
import gal.galiciana.app.ui.search.*
import org.junit.Assert.*
import org.junit.Test

class SearchProximityRankingTest {
    private val query = "Pedro Antonio Bárcena Álvarez"
    private fun hit(text: String, pos: Int = 1) = OcrHit("Boletín", snippets = listOf(text), path = "1510028", imageId = null, position = pos)
    private fun rank(text: String) = pageRelevance(hit(text), query)
    @Test fun completeNearbyNamesLeadCompactPartialAndDispersedWords() {
        val literal = rank("Pedro Antonio Bárcena Álvarez")
        val suppliedPage = rank("Pedro Antonio Lician Bárcena Al-\nvarez")
        val partial = rank("Pedro Lician Bárcena Álvarez")
        val dispersed = rank("Pedro " + "noticia ".repeat(9) + "Antonio " + "otra ".repeat(9) + "Bárcena " + "texto ".repeat(9) + "Álvarez")
        assertTrue(literal > suppliedPage)
        assertTrue(suppliedPage > partial)
        assertTrue(partial > dispersed)
    }
    @Test fun relevantExcerptLeadsIncidentalNamesOnTheSamePage() {
        val passages = listOf("Veneranda Cerdeira, Felisa Otero, Antonio Otero", "Pedro Antonio Lician Bárcena Al-varez", "Ramona y Luis Vázquez, Antonio Cerdeira")
        val result = passages.sortedByDescending { pageRelevance(hit(it), query) }
        assertEquals(passages[1], result.first())
    }
    @Test fun adjacentQueryPairWinsAgainstEquallyCoveredButSeparatedWords() {
        assertTrue(rank("Pedro Lician Bárcena Álvarez") > rank("Pedro Lician Bárcena otro Álvarez"))
    }
    @Test fun ranksBothWorksAndPagesButDoesNotCombineSeparateSnippetsIntoOnePhrase() {
        val near = hit("Pedro Antonio Lician Bárcena Alvarez", 3)
        val far = hit("Pedro " + "texto ".repeat(30) + "Antonio Bárcena Alvarez", 2)
        val split = near.copy(snippets = listOf("Pedro Antonio", "Bárcena Alvarez"), position = 4)
        val groups = listOf(OcrWorkGroup("Primero", null, listOf(far, near, split)))
        val sorted = sortGroups(groups, SearchViewModel.SortBy.Relevancia, false, query)
        assertEquals(3, sorted.single().pages.first().position)
        assertFalse(hitMatchesQuotedConstraints(near, "\"Pedro Antonio Bárcena Alvarez\""))
        assertTrue(hitMatchesQuotedConstraints(near, query))
        assertEquals(setOf(2,3,4), sorted.single().pages.map { it.position }.toSet())
    }
}
