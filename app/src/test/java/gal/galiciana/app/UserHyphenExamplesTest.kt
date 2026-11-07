package gal.galiciana.app

import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.ui.countMatches
import gal.galiciana.app.ui.highlightPossibleTerms
import gal.galiciana.app.ui.queryTokens
import gal.galiciana.app.ui.textContainsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Concrete newspaper cuts supplied by the user, exercised without remote-service dependencies. */
class UserHyphenExamplesTest {
    private data class Example(val canonical: String, val printed: String)
    private val examples = listOf(
        Example("Federación Gallega", "Federación Ga-llega"),
        Example("Elisardo Núñez Seabra", "Elisardo Núñez Sea-bra"),
        Example("Manuel Fernández", "Manuel Fer-nández"),
        Example("Amador", "Ama-dor"),
        Example("Regimiento de Infantería Zamora", "Regimiento de Infante-ria Zamora"),
        Example("Xinzo de Limia", "Xinzo de Li-mia"),
        Example("Javier Vázquez", "Ja-vier Vázquez"),
    )
    private val repo = OcrRepository()

    @Test fun allUserExamplesMatchPlainAndQuotedQueriesAcrossPrintedCutVariants() {
        for (example in examples) {
            val variants = listOf(example.canonical, example.printed,
                example.printed.replace("-", "-\n"),
                example.printed.replace("-", "- "),
                example.printed.replace("-", "-\r\n"))
            for (text in variants) {
                val context = "${example.canonical}: $text"
                assertTrue(context, textContainsQuery(text, example.canonical))
                assertTrue(context, textContainsQuery(text, "\"${example.canonical}\""))
                assertEquals(context, 1, countMatches(text, "\"${example.canonical}\""))
                assertEquals(context, queryTokens(example.canonical).size,
                    countMatches(text, example.canonical))
            }
        }
    }

    @Test fun originalLineCutRemainsIntactInApproximateHighlightSpans() {
        for (example in examples) {
            val text = example.printed.replace("-", "-\n")
            val marked = highlightPossibleTerms(text, example.canonical)
            assertEquals(example.canonical, text, marked.text)
            val cutWord = text.substringBefore('-').substringAfterLast(' ') + "-\n" +
                text.substringAfter("-\n").substringBefore(' ')
            val spans = marked.spanStyles.map { marked.text.substring(it.start, it.end) }
            assertTrue("${example.canonical}: $spans", cutWord in spans)
        }
    }

    @Test fun eachUserPhraseIsOneAltoOccurrenceWithTwoLineBoxes() {
        for (example in examples) {
            val result = repo.parseAltoSearch(altoAcrossLines(example), "\"${example.canonical}\"")
            assertEquals(example.canonical, OcrRepository.AltoStatus.AVAILABLE, result.status)
            assertEquals(example.canonical, 1, result.matches.size)
            assertEquals(example.canonical, 2, result.matches.single().boxes.size)
            assertEquals(example.canonical, listOf(20f, 60f), result.hits.map { it.top })
            assertTrue(example.canonical, result.hits.all { it.height == 20f && it.width < 500f })
        }
    }

    @Test fun plainAltoQueriesPreserveEveryTermAndGroupBothHyphenParts() {
        for (example in examples) {
            val result = repo.parseAltoSearch(altoAcrossLines(example), example.canonical)
            assertEquals(example.canonical, queryTokens(example.canonical).size, result.matches.size)
            val splitMatches = result.matches.filter { it.boxes.size == 2 }
            assertEquals(example.canonical, 1, splitMatches.size)
            assertEquals(example.canonical, listOf(20f, 60f), splitMatches.single().boxes.map { it.top })
            assertEquals(example.canonical, queryTokens(example.canonical).size + 1, result.hits.size)
        }
    }

    private fun altoAcrossLines(example: Example): String {
        val pieces = example.printed.split('-', limit = 2)
        val lines = listOf(pieces[0] + "-", pieces[1])
        val textLines = lines.mapIndexed { lineIndex, line ->
            val words = line.split(' ').mapIndexed { wordIndex, word ->
                """<String CONTENT="$word" HPOS="${10 + wordIndex * 90}" VPOS="${20 + lineIndex * 40}" WIDTH="80" HEIGHT="20"/>"""
            }.joinToString("")
            "<TextLine>$words</TextLine>"
        }.joinToString("")
        return """<alto xmlns="http://www.loc.gov/standards/alto/ns-v4#"><Layout>
            <Page WIDTH="1200" HEIGHT="1600"><PrintSpace><TextBlock>$textLines</TextBlock></PrintSpace></Page>
            </Layout></alto>""".trimIndent()
    }
}
