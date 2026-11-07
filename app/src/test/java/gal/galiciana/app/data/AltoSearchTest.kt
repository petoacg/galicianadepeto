package gal.galiciana.app.data

import gal.galiciana.app.ui.countMatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AltoSearchTest {
    private val repo = OcrRepository()
    private fun alto(vararg contents: String): String = """
        <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#"><Layout><Page WIDTH="1000" HEIGHT="2000">
        <PrintSpace><TextBlock><TextLine>${contents.mapIndexed { i, text ->
            "<String CONTENT=\"$text\" HPOS=\"${10 + i * 100}\" VPOS=\"20\" WIDTH=\"90\" HEIGHT=\"20\"/>"
        }.joinToString("")}</TextLine></TextBlock></PrintSpace></Page></Layout></alto>
    """.trimIndent()

    @Test fun namespacedXmlAndBothAttributeQuotesPreserveApostrophesAndEntities() {
        val xml = """
            <a:alto xmlns:a="http://www.loc.gov/standards/alto/ns-v4#"><a:Layout>
            <a:Page WIDTH='1000' HEIGHT='2000'><a:TextLine>
            <a:String CONTENT="d'Artagnan" HPOS='10' VPOS='20' WIDTH='90' HEIGHT='20'/>
            <a:String CONTENT='Mar&#237;a' HPOS="110" VPOS="20" WIDTH="90" HEIGHT="20"/>
            </a:TextLine></a:Page></a:Layout></a:alto>
        """.trimIndent()
        assertEquals(1, repo.parseAltoSearch(xml, "Artagnan").matches.size)
        assertEquals(1, repo.parseAltoSearch(xml, "María").matches.size)
        assertEquals(1000f to 2000f, repo.parseAltoSearch(xml, "María").pageSize)
    }

    @Test fun quotedSingleWordStaysLiteralRatherThanFuzzy() {
        assertEquals(0, repo.parseAltoSearch(alto("Ulanga"), "\"uranga\"").matches.size)
        assertEquals(1, repo.parseAltoSearch(alto("Cárcamo"), "\"carcamo\"").matches.size)
    }

    @Test fun phraseUsesTheSharedWholePhraseOcrErrorBudget() {
        val oneError = "Luciano Cárcamo Ulanga"
        val twoErrors = "Luciano Cárcomo Ulanga"
        assertEquals(countMatches(oneError, "\"luciano cárcamo uranga\""),
            repo.parseAltoSearch(alto("Luciano", "Cárcamo", "Ulanga"), "\"luciano cárcamo uranga\"").matches.size)
        assertEquals(0, countMatches(twoErrors, "\"luciano cárcamo uranga\""))
        assertEquals(0, repo.parseAltoSearch(alto("Luciano", "Cárcomo", "Ulanga"), "\"luciano cárcamo uranga\"").matches.size)
    }

    @Test fun phraseAcrossLinesIsOneOccurrenceWithTwoSmallBoxes() {
        val xml = alto("María", "de", "la", "Cruz")
            .replace("CONTENT=\"la\" HPOS=\"210\" VPOS=\"20\"", "CONTENT=\"la\" HPOS=\"10\" VPOS=\"50\"")
            .replace("CONTENT=\"Cruz\" HPOS=\"310\" VPOS=\"20\"", "CONTENT=\"Cruz\" HPOS=\"110\" VPOS=\"50\"")
        val result = repo.parseAltoSearch(xml, "\"María de la Cruz\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
        assertTrue(result.hits.all { it.height == 20f && it.width < 300f })
    }

    @Test fun hyphenatedWordAcrossLinesRetainsBothPartsAsOneMatch() {
        val xml = alto("Cár-", "camo")
            .replace("CONTENT=\"camo\" HPOS=\"110\" VPOS=\"20\"", "CONTENT=\"camo\" HPOS=\"10\" VPOS=\"50\"")
        val result = repo.parseAltoSearch(xml, "\"cárcamo\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
    }

    @Test fun multipleOccurrencesDontIncludePrecedingUnmatchedWords() {
        val result = repo.parseAltoSearch(alto("ayer", "María", "de", "la", "María", "de", "la"), "\"María de la\"")
        assertEquals(2, result.matches.size)
        assertEquals(110f, result.matches.first().boxes.first().left)
    }

    @Test fun invalidCoordinatesAreNotInventedFromMatchedWords() {
        assertEquals(OcrRepository.AltoStatus.INVALID,
            repo.parseAltoSearch(alto("María").replace(" WIDTH=\"1000\"", ""), "María").status)
        assertEquals(OcrRepository.AltoStatus.INVALID,
            repo.parseAltoSearch(alto("María").replace("HPOS=\"10\"", "HPOS=\"NaN\""), "María").status)
        assertEquals(OcrRepository.AltoStatus.INVALID,
            repo.parseAltoSearch(alto("María").replace("HPOS=\"10\"", "HPOS=\"9999\""), "María").status)
    }

    @Test fun validZeroMatchesEmptyAltoAndUnexpectedHtmlHaveDifferentStatuses() {
        val noMatch = repo.parseAltoSearch(alto("María"), "Salceda")
        assertEquals(OcrRepository.AltoStatus.AVAILABLE, noMatch.status)
        assertTrue(noMatch.matches.isEmpty())
        assertEquals(OcrRepository.AltoStatus.EMPTY, repo.parseAltoSearch(alto(), "Salceda").status)
        assertEquals(OcrRepository.AltoStatus.INVALID,
            repo.parseAltoSearch("<html><body>Servicio no disponible</body></html>", "Salceda").status)
    }

    @Test fun unquotedSingleLetterAndSplitWordFollowTextMatcherBoundaries() {
        assertEquals(1, repo.parseAltoSearch(alto("a", "gato"), "a").matches.size)
        assertEquals(1, repo.parseAltoSearch(alto("Car", "camo"), "Cárcamo").matches.size)
        assertEquals(0, repo.parseAltoSearch(alto("gato"), "pato").matches.size)
    }

    @Test fun mixedQueriesNavigateInAltoReadingOrderNotQueryOrder() {
        val result = repo.parseAltoSearch(alto("Salceda", "María", "Cruz", "Salceda"),
            "\"María Cruz\" Salceda")
        assertEquals(listOf(10f, 110f, 310f), result.matches.map { it.boxes.first().left })
    }

    @Test fun phraseCanBeStoredInOneStringOrSeveralStrings() {
        val query = "\"María de la Cruz\""
        assertEquals(1, repo.parseAltoSearch(alto("María de la Cruz"), query).matches.size)
        assertEquals(1, repo.parseAltoSearch(alto("María", "de", "la", "Cruz"), query).matches.size)
        val repeatedQuery = repo.parseAltoSearch(alto("María de la Cruz"), "$query $query")
        assertEquals(1, repeatedQuery.matches.size)
    }

    @Test fun distantColumnsNeverProduceOneGiantRectangle() {
        val xml = alto("María", "Cruz").replace("HPOS=\"110\"", "HPOS=\"800\"")
        val result = repo.parseAltoSearch(xml, "\"María Cruz\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
    }
    @Test fun unrelatedTextBlocksDoNotJoinHyphensOrPhrases() {
        val xml = alto("Cár-", "camo")
            .replace("<String CONTENT=\"camo\"", "</TextLine></TextBlock><TextBlock><TextLine><String CONTENT=\"camo\"")
        assertEquals(0, repo.parseAltoSearch(xml, "\"Cárcamo\"").matches.size)
        assertEquals(0, repo.parseAltoSearch(xml, "\"Cár camo\"").matches.size)
        val split = xml.replace("Cár-", "Car")
        assertEquals(0, repo.parseAltoSearch(split, "Cárcamo").matches.size)
    }

    @Test fun explicitHyphenSubstitutionsRetainOriginalBoxes() {
        val xml = alto("Cár", "camo")
            .replace("CONTENT=\"Cár\"", "CONTENT=\"Cár\" SUBS_TYPE=\"HypPart1\" SUBS_CONTENT=\"Cárcamo\"")
            .replace("CONTENT=\"camo\"", "CONTENT=\"camo\" SUBS_TYPE=\"HypPart2\" SUBS_CONTENT=\"Cárcamo\"")
            .replace("HPOS=\"110\" VPOS=\"20\"", "HPOS=\"10\" VPOS=\"50\"")
        val result = repo.parseAltoSearch(xml, "\"cárcamo\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
    }

    @Test fun separateHypElementJoinsAcrossTextLinesWithinOneBlock() {
        val xml = alto("Cár", "camo")
            .replace("<String CONTENT=\"camo\"", "<HYP CONTENT=\"-\"/></TextLine><TextLine><String CONTENT=\"camo\"")
            .replace("HPOS=\"110\" VPOS=\"20\"", "HPOS=\"10\" VPOS=\"50\"")
        val result = repo.parseAltoSearch(xml, "\"Cárcamo\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
    }

    @Test fun unquotedTermsRemainIndependentAcrossColumns() {
        val xml = alto("María", "Salceda")
            .replace("<String CONTENT=\"Salceda\"", "</TextLine></TextBlock><TextBlock><TextLine><String CONTENT=\"Salceda\"")
        assertEquals(2, repo.parseAltoSearch(xml, "María Salceda").matches.size)
        assertEquals(0, repo.parseAltoSearch(xml, "\"María Salceda\"").matches.size)
    }

    /** Observed word coordinates in BOPOurense, 8 July 1961, group 1504641 p3. */
    private fun fernandezAcrossBlocks(nextLeft: Int = 606, nextTop: Int = 402): String = """
        <alto><Layout><Page WIDTH="2000" HEIGHT="3000"><PrintSpace>
        <TextBlock ID="block11"><TextLine HPOS="606" VPOS="355" WIDTH="462" HEIGHT="28">
        <String CONTENT="Manuel" HPOS="886" VPOS="356" WIDTH="102" HEIGHT="27"/>
        <String CONTENT="Fer-" HPOS="1013" VPOS="355" WIDTH="54" HEIGHT="26"/>
        </TextLine></TextBlock>
        <TextBlock ID="block12"><TextLine HPOS="$nextLeft" VPOS="$nextTop" WIDTH="462" HEIGHT="28">
        <String CONTENT="nández" HPOS="$nextLeft" VPOS="$nextTop" WIDTH="99" HEIGHT="27"/>
        <String CONTENT="Arce" HPOS="${nextLeft + 110}" VPOS="$nextTop" WIDTH="70" HEIGHT="27"/>
        </TextLine></TextBlock>
        </PrintSpace></Page></Layout></alto>
    """.trimIndent()

    @Test fun observedFernandezCutBridgesAlignedTextBlocksAndKeepsWholePhrase() {
        val xml = fernandezAcrossBlocks()
        val surname = repo.parseAltoSearch(xml, "\"Fernández\"")
        assertEquals(1, surname.matches.size)
        assertEquals(2, surname.matches.single().boxes.size)
        assertEquals(listOf(1013f, 606f), surname.hits.map { it.left })
        val phrase = repo.parseAltoSearch(xml, "\"Manuel Fernández\"")
        assertEquals(1, phrase.matches.size)
        assertEquals(2, phrase.matches.single().boxes.size)
        assertEquals(listOf(886f, 606f), phrase.hits.map { it.left })
        assertEquals(listOf(355f, 402f), phrase.hits.map { it.top })
        assertTrue(phrase.hits.all { it.width < 200f && it.height <= 28f })
    }

    @Test fun crossBlockCutDoesNotJumpToAnotherColumnOrDistantLine() {
        assertEquals(0, repo.parseAltoSearch(fernandezAcrossBlocks(nextLeft = 1200), "\"Fernández\"").matches.size)
        assertEquals(0, repo.parseAltoSearch(fernandezAcrossBlocks(nextTop = 1200), "\"Fernández\"").matches.size)
        assertEquals(0, repo.parseAltoSearch(fernandezAcrossBlocks().replace("Fer-", "Fer"),
            "\"Manuel Fernández\"").matches.size)
    }

    @Test fun explicitAltoHyphenPairCanBridgeBlocksWithoutInferringGeometry() {
        val xml = fernandezAcrossBlocks(nextLeft = 1200, nextTop = 1200)
            .replace("CONTENT=\"Fer-\"", "CONTENT=\"Fer\" SUBS_TYPE=\"HypPart1\" SUBS_CONTENT=\"Fernández\"")
            .replace("CONTENT=\"nández\"", "CONTENT=\"nández\" SUBS_TYPE=\"HypPart2\" SUBS_CONTENT=\"Fernández\"")
        val result = repo.parseAltoSearch(xml, "\"Manuel Fernández\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
        assertTrue(result.hits.all { it.width < 200f })
    }

    @Test fun realReferencePageCutsRecoverAllThreePreviouslyMissingSearches() {
        val xml = javaClass.classLoader!!.getResourceAsStream("alto/reference_1264535_p15_cuts.xml")!!
            .bufferedReader().use { it.readText() }
        val expected = listOf("Amador" to 1, "Regimiento de Infantería Zamora" to 1, "Xinzo de Limia" to 3)
        for ((query, count) in expected) {
            val result = repo.parseAltoSearch(xml, "\"$query\"")
            assertEquals(query, OcrRepository.AltoStatus.AVAILABLE, result.status)
            assertEquals(query, count, result.matches.size)
            assertTrue(query, result.matches.all { it.boxes.size == 2 })
        }
    }

    @Test fun emptySecondSubstitutionContentIsAValidAltoHyphenPairAcrossBlocks() {
        val xml = fernandezAcrossBlocks(nextLeft = 1200, nextTop = 1200)
            .replace("CONTENT=\"Fer-\"", "CONTENT=\"Fer¬\" SUBS_TYPE=\"HypPart1\" SUBS_CONTENT=\"Fernández\"")
            .replace("CONTENT=\"nández\"", "CONTENT=\"nández\" SUBS_TYPE=\"HypPart2\" SUBS_CONTENT=\"\"")
        val result = repo.parseAltoSearch(xml, "\"Manuel Fernández\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
    }

    @Test fun standaloneInlineDashDoesNotSwallowTheFollowingPlaceName() {
        val xml = alto("Moreiras", "-", "Xinzo", "de", "Limia")
        val result = repo.parseAltoSearch(xml, "\"Xinzo de Limia\"")
        assertEquals(1, result.matches.size)
        assertEquals(210f, result.hits.first().left)
    }

    @Test fun standalonePrintedLineEndHyphenStillJoinsTheContinuation() {
        val xml = alto("Car", "-", "camo")
            .replace("<String CONTENT=\"camo\"", "</TextLine><TextLine><String CONTENT=\"camo\"")
            .replace("HPOS=\"210\" VPOS=\"20\"", "HPOS=\"10\" VPOS=\"50\"")
        val result = repo.parseAltoSearch(xml, "\"Cárcamo\"")
        assertEquals(1, result.matches.size)
        assertEquals(2, result.matches.single().boxes.size)
    }

}
