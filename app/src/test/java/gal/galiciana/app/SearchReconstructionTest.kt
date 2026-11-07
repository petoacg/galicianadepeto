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
        val query = "carlos cárcamo agrónomo"
        val intact = page("Carlos Cárcamo agrónomo")
        for (split in listOf("Carlos Cár-\ncamo agrónomo", "Carlos Cár- camo agrónomo",
            "Carlos Cár‐\r\ncamo agrónomo", "Carlos Cár‑\ncamo agrónomo")) {
            assertTrue(split, hitMatchesQuery(page(split), query))
            assertEquals(split, pageRelevance(intact, query), pageRelevance(page(split), query))
            assertTrue(split, containsStrictQuotedWord(split, "Cárcamo"))
        }
    }

    @Test fun approximateHighlightsRetainTheOriginalSplitWordCoordinates() {
        val text = "Fue Cár-\ncamo quien llegó"
        val marked = highlightPossibleTerms(text, "cárcamo agrónomo")
        assertEquals(text, marked.text)
        assertEquals(listOf("Cár-\ncamo"), marked.spanStyles.map {
            marked.text.substring(it.start, it.end)
        })
    }

    @Test fun separateExcerptsAreNotJoinedAsIfTheyWereAdjacentColumns() {
        assertFalse(hitMatchesQuery(page("Carlos Cár-", "camo agrónomo"), "\"Carlos Cárcamo\""))
        assertFalse(hitMatchesQuery(page("Car-", "camo agrónomo"), "cárcamo agrónomo"))
    }

    @Test fun removingOneTermIsOnlyTheExistingSavedResultApproximation() {
        val text = page("Carlos Javier era agrónomo")
        assertTrue(hitMatchesQuery(text, "Carlos Javier Cárcamo agrónomo"))
        assertFalse(hitMatchesQuery(text, "Carlos Javier Cárcamo agrónomo médico"))
        assertFalse(hitMatchesQuery(page("Carlos llegó"), "Carlos Cárcamo"))
        assertFalse(textContainsQuery("Carlos Javier era agrónomo", "Carlos Javier Cárcamo agrónomo"))
        assertFalse(hitMatchesQuery(text, "\"Carlos Javier Cárcamo\" agrónomo"))
    }

    @Test fun damagedWordCannotMatchOnlyAPrefixOfALongerSearchWord() {
        assertFalse(tokenMatches("Car#", "cárcamo", 0))
        assertFalse(tokenMatches("adminis#", "administración", 0))
        assertTrue(tokenMatches("adminis#ración", "administración", 0))
        assertTrue(tokenMatches("Cár#camo", "cárcamo", 0))
    }

    @Test fun highlightsUseFullUnicodeWordAndNumericBoundaries() {
        assertEquals(listOf("1900"), highlightRegex("1900")!!.findAll("11900 1900 19001")
            .map { it.value.trim() }.toList())
        assertEquals(listOf("ana"), highlightRegex("ana")!!.findAll("αana ana banana")
            .map { it.value.trim() }.toList())
    }

    @Test fun quotedPhraseReconstructsLineCutsButNeverSkipsMissingWords() {
        assertEquals(1, countMatches("Carlos Cár-\ncamo Uranga", "\"Carlos Cárcamo Uranga\""))
        assertEquals(0, countMatches("Carlos Uranga", "\"Carlos Cárcamo Uranga\""))
    }
}
