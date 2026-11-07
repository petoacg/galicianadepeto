package gal.galiciana.app

import gal.galiciana.app.ui.search.recoveryCandidateQueries
import org.junit.Assert.*
import org.junit.Test

class SearchRecoveryTest {
    @Test fun fullNameRecoveryIncludesBothRealMissingNameCombinations() {
        val query = "Pedro Antonio Cárcamo Álvarez"
        val candidates = recoveryCandidateQueries(query)
        assertTrue("Antonio Cárcamo Álvarez" in candidates)
        assertTrue("Pedro Cárcamo Álvarez" in candidates)
        listOf("Antonio Cár-\ncamo Álvarez", "Pedro Cár-\ncamo Álvarez").forEach { text ->
            assertFalse(gal.galiciana.app.ui.textContainsQuery(text, query))
            assertEquals(3, gal.galiciana.app.ui.countMatches(text, query))
        }
    }

    @Test fun missingTwoTermsCanBeRecoveredWithoutManualRewriting() {
        val queries = recoveryCandidateQueries("Pedro Antonio Cárcamo agrónomo")
        assertTrue("Pedro Cárcamo" in queries)
        assertTrue("Cárcamo agrónomo" in queries)
        assertEquals("Pedro Antonio Cárcamo agrónomo", queries.first())
        assertFalse("Pedro" in queries)
    }

    @Test fun longQueriesDoNotCreateUnboundedRequests() {
        val queries = recoveryCandidateQueries((1..50).joinToString(" ") { "palabra$it" })
        assertTrue(queries.size <= 12)
        assertEquals(queries.distinct(), queries)
        assertTrue(recoveryCandidateQueries("de").isEmpty())
        assertTrue(recoveryCandidateQueries("Cárcamo", limit = 0).isEmpty())
    }
}
