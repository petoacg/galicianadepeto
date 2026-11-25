package gal.galiciana.app

import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.ui.containsStrictQuotedWord
import gal.galiciana.app.ui.countMatches
import gal.galiciana.app.ui.highlightPossibleTerms
import gal.galiciana.app.ui.highlightRegex
import gal.galiciana.app.ui.textContainsQuery
import gal.galiciana.app.ui.tokenMatches
import gal.galiciana.app.ui.search.hitMatchesQuery
import gal.galiciana.app.ui.search.pageRelevance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchReconstructionTest {
    private fun page(vararg text: String) = OcrHit(
        title = "Periódico", snippets = text.toList(), path = "123", imageId = "45", position = 1)

    @Test fun lineHyphenDoesNotRemoveAQueryTermFromSavedResultCoverageOrRanking() {
        val query = "carlos bárcena agrónomo"
        val intact = page("Carlos Bárcena agrónomo")
        for (split in listOf("Carlos Bár-\ncena agrónomo", "Carlos Bár- cena agrónomo",
            "Carlos Bár‐\r\ncena agrónomo", "Carlos Bár‑\ncena agrónomo")) {
            assertTrue(split, hitMatchesQuery(page(split), query))
            assertEquals(split, pageRelevance(intact, query), pageRelevance(page(split), query))
            assertTrue(split, containsStrictQuotedWord(split, "Bárcena"))
        }
    }

    @Test fun approximateHighlightsRetainTheOriginalSplitWordCoordinates() {
        val text = "Fue Bár-\ncena quien llegó"
        val marked = highlightPossibleTerms(text, "bárcena agrónomo")
        assertEquals(text, marked.text)
        assertEquals(listOf("Bár-\ncena"), marked.spanStyles.map {
            marked.text.substring(it.start, it.end)
        })
    }

    @Test fun separateExcerptsAreNotJoinedAsIfTheyWereAdjacentColumns() {
        assertFalse(hitMatchesQuery(page("Carlos Bár-", "cena agrónomo"), "\"Carlos Bárcena\""))
        assertFalse(hitMatchesQuery(page("Bar-", "cena agrónomo"), "bárcena agrónomo"))
    }

    @Test fun removingOneTermIsOnlyTheExistingSavedResultApproximation() {
        val text = page("Carlos Javier era agrónomo")
        assertTrue(hitMatchesQuery(text, "Carlos Javier Bárcena agrónomo"))
        assertFalse(hitMatchesQuery(text, "Carlos Javier Bárcena agrónomo médico"))
        assertFalse(hitMatchesQuery(page("Carlos llegó"), "Carlos Bárcena"))
        assertFalse(textContainsQuery("Carlos Javier era agrónomo", "Carlos Javier Bárcena agrónomo"))
        assertFalse(hitMatchesQuery(text, "\"Carlos Javier Bárcena\" agrónomo"))
    }

    @Test fun damagedWordCannotMatchOnlyAPrefixOfALongerSearchWord() {
        assertFalse(tokenMatches("Bar#", "bárcena", 0))
        assertFalse(tokenMatches("adminis#", "administración", 0))
        assertTrue(tokenMatches("adminis#ración", "administración", 0))
        assertTrue(tokenMatches("Bár#cena", "bárcena", 0))
    }

    @Test fun highlightsUseFullUnicodeWordAndNumericBoundaries() {
        assertEquals(listOf("1900"), highlightRegex("1900")!!.findAll("11900 1900 19001")
            .map { it.value.trim() }.toList())
        assertEquals(listOf("ana"), highlightRegex("ana")!!.findAll("αana ana banana")
            .map { it.value.trim() }.toList())
    }

    @Test fun quotedPhraseReconstructsLineCutsButNeverSkipsMissingWords() {
        assertEquals(1, countMatches("Carlos Bár-\ncena Ubeira", "\"Carlos Bárcena Ubeira\""))
        assertEquals(0, countMatches("Carlos Ubeira", "\"Carlos Bárcena Ubeira\""))
    }
}
