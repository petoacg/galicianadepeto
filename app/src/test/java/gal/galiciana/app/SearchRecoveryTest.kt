package gal.galiciana.app

import gal.galiciana.app.ui.search.recoveryCandidateQueries
import org.junit.Assert.*
import org.junit.Test

class SearchRecoveryTest {
    @Test fun fullNameRecoveryIncludesBothRealMissingNameCombinations() {
        val query = "Pedro Antonio Bárcena Álvarez"
        val candidates = recoveryCandidateQueries(query)
        assertTrue("Antonio Bárcena Álvarez" in candidates)
        assertTrue("Pedro Bárcena Álvarez" in candidates)
        listOf("Antonio Bár-\ncena Álvarez", "Pedro Bár-\ncena Álvarez").forEach { text ->
            assertFalse(gal.galiciana.app.ui.textContainsQuery(text, query))
            assertEquals(3, gal.galiciana.app.ui.countMatches(text, query))
        }
    }

    @Test fun missingTwoTermsCanBeRecoveredWithoutManualRewriting() {
        val queries = recoveryCandidateQueries("Pedro Antonio Bárcena agrónomo")
        assertTrue("Pedro Bárcena" in queries)
        assertTrue("Bárcena agrónomo" in queries)
        assertEquals("Pedro Antonio Bárcena agrónomo", queries.first())
        assertFalse("Pedro" in queries)
    }

    @Test fun longQueriesDoNotCreateUnboundedRequests() {
        val queries = recoveryCandidateQueries((1..50).joinToString(" ") { "palabra$it" })
        assertTrue(queries.size <= 12)
        assertEquals(queries.distinct(), queries)
        assertTrue(recoveryCandidateQueries("de").isEmpty())
        assertTrue(recoveryCandidateQueries("Bárcena", limit = 0).isEmpty())
    }
}
