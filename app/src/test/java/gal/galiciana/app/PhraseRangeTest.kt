package gal.galiciana.app

import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.ui.lettersOnly
import gal.galiciana.app.ui.normalizedOcrWords
import gal.galiciana.app.ui.phraseMatchRanges
import gal.galiciana.app.ui.queryTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class PhraseRangeTest {
    private fun ranges(text: String, phrase: String) = phraseMatchRanges(
        normalizedOcrWords(text), queryTokens(phrase).map(::lettersOnly))

    @Test fun singleQuotedWordRemainsLiteralAndUsesWholeTokens() {
        assertEquals(listOf(1..1), ranges("Ulanga Uranga Urangas", "Uranga"))
        assertEquals(listOf(0..0), ranges("Cár-\ncamo", "Cárcamo"))
    }

    @Test fun sharedRangesIncludeEveryTokenOfAnUnmarkedOcrSplit() {
        assertEquals(listOf(0..3), ranges("Luciano Car camo Uranga", "Luciano Cárcamo Uranga"))
        assertEquals(listOf(1..3), ranges("ayer Luciano Cárcamo Uranga", "Luciano Cárcamo Uranga"))
    }

    @Test fun phraseStillHasOneGlobalOcrErrorBudgetAndCannotSkipWords() {
        assertEquals(listOf(0..2), ranges("Luciano Cárcamo Ulanga", "Luciano Cárcamo Uranga"))
        assertEquals(emptyList<IntRange>(), ranges("Luciano Cárcomo Ulanga", "Luciano Cárcamo Uranga"))
        assertEquals(emptyList<IntRange>(), ranges("Luciano llegó Cárcamo Uranga", "Luciano Cárcamo Uranga"))
        assertEquals(listOf(0..2), ranges("Luciano Cár#camo Uranga", "Luciano Cárcamo Uranga"))
    }

    @Test fun repeatedAndOverlappingOccurrencesKeepTheirTokenRanges() {
        assertEquals(listOf(0..1, 1..2), ranges("María María María", "María María"))
        assertEquals(listOf(0..1, 3..4), ranges("Manuel Fernández ayer Manuel Fernández", "Manuel Fernández"))
    }

    @Test fun largeNewspaperAltoFindsSparsePhrasesWithoutChangingBoxesOrOrdering() {
        val strings = (0 until 6000).joinToString("") { index ->
            val content = when (index) {
                1000, 4000 -> "Manuel"
                1001, 4001 -> "Fernández"
                else -> "noticia"
            }
            """<String CONTENT="$content" HPOS="${10 + index % 10 * 90}" VPOS="${20 + index / 10 * 24}" WIDTH="80" HEIGHT="20"/>"""
        }
        val xml = """<alto><Layout><Page WIDTH="1000" HEIGHT="20000"><TextBlock><TextLine>$strings</TextLine></TextBlock></Page></Layout></alto>"""
        val result = OcrRepository().parseAltoSearch(xml, "\"Manuel Fernández\"")
        assertEquals(OcrRepository.AltoStatus.AVAILABLE, result.status)
        assertEquals(2, result.matches.size)
        assertEquals(listOf(2420f, 9620f), result.matches.map { it.boxes.single().top })
        assertEquals(listOf(170f, 170f), result.hits.map { it.width })
    }
}
