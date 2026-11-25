package gal.galiciana.app

import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.domain.OcrWorkGroup
import gal.galiciana.app.ui.search.pageRelevance
import gal.galiciana.app.ui.search.unquotedCandidateQueries
import gal.galiciana.app.ui.search.hitMatchesQuery
import gal.galiciana.app.ui.search.plausibleUnquotedHit
import gal.galiciana.app.ui.search.sortGroups
import gal.galiciana.app.ui.search.SearchViewModel
import gal.galiciana.app.ui.search.hitMatchesQuotedConstraints
import gal.galiciana.app.ui.search.normalizeSearchInput
import gal.galiciana.app.ui.containsPhrase
import gal.galiciana.app.ui.containsOcrLiteralPhrase
import gal.galiciana.app.ui.containsStrictQuotedWord
import gal.galiciana.app.ui.countMatches
import gal.galiciana.app.ui.highlightTerms
import gal.galiciana.app.ui.highlightPossibleTerms
import gal.galiciana.app.ui.textContainsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSemanticsTest {
    @Test fun supportedWholeQueryQuoteStylesDoNotDuplicatePhraseAsExtraWords() {
        listOf("'María de la Cruz'", "‘María de la Cruz’", "„María de la Cruz”")
            .forEach { query ->
                assertEquals("\"María de la Cruz\"", highlightTerms(query))
                assertEquals(1, countMatches("María de la Cruz", query))
            }
    }

    @Test fun unexpectedHtmlIsAnErrorRatherThanASuccessfulEmptySearch() {
        val repo = OcrRepository()
        listOf("", "<html><body>Servicio temporalmente no disponible</body></html>",
            "<html><head><script>window.location='/'</script></head><body class='block'></body></html>")
            .forEach { html ->
                assertTrue(runCatching { repo.parseSearchResponse(html) }.exceptionOrNull()
                    is IllegalStateException)
            }
    }

    @Test fun recognizedEmptySearchAndRealResultsRemainValid() {
        val repo = OcrRepository()
        assertTrue(repo.parseSearchResponse("<p>No se han encontrado resultados</p>").isEmpty())
        assertTrue(repo.parseSearchResponse("<ol class='nav_registros'></ol>").isEmpty())
        val html = """
            <div class="unidad_textual"><a href="/es/catalogo_imagenes/grupo.do?path=123&amp;posicion=2"
                data-analytics-recordtitle="Una publicación">Página 2</a>
                <ul class="texto_ocurrencias"><li>Salceda aparece aquí</li></ul></div>
        """.trimIndent()
        assertEquals("123", repo.parseSearchResponse(html).single().path)
    }

    @Test fun recoveryFiltersAreAnImmutableNormalizedSubmittedSnapshot() {
        var titleDraft = "  La\u00a0Región  "
        val submitted = gal.galiciana.app.ui.search.SubmittedSearchFilters.fromForm(
            true, titleDraft, "  autor ", " Vigo ", " 1900 ", "1910")
        titleDraft = "Otra publicación"
        assertEquals("La Región", submitted.title)
        assertEquals("autor", submitted.author)
        assertEquals("Vigo", submitted.place)
        assertEquals("1900", submitted.yearFrom)
        assertEquals("1910", submitted.yearTo)
        assertFalse(titleDraft == submitted.title)
        assertEquals(gal.galiciana.app.ui.search.SubmittedSearchFilters(),
            gal.galiciana.app.ui.search.SubmittedSearchFilters.fromForm(
                false, titleDraft, "autor", "Vigo", "1900", "1910"))
    }

    @Test fun paginationStopsCyclesAndKeepsIndependentPendingBranches() {
        val visited = mutableSetOf("first")
        val next = gal.galiciana.app.ui.search.nextPendingSearchPages(
            listOf("second", "other"), listOf("second"),
            listOf("first", "third", "third"), visited)
        assertEquals(listOf("other", "third"), next)
        assertEquals(setOf("first", "second"), visited)
        assertEquals(emptyList<String>(), gal.galiciana.app.ui.search.nextPendingSearchPages(
            listOf("third"), listOf("third"), listOf("second"), visited))
    }

    @Test fun keyboardWhitespaceDoesNotChangeTheSubmittedQuery() {
        val expected = "\"barcena ubeira\""
        assertEquals(expected, normalizeSearchInput("$expected "))
        assertEquals(expected, normalizeSearchInput("\u200B  “barcena\u00A0ubeira”\u200B "))
        assertEquals("barcena ubeira", normalizeSearchInput("barcena\n  ubeira  "))
    }
    @Test fun quotedPhraseRequiresAdjacentWordsButAcceptsLineHyphen() {
        assertTrue(containsPhrase("Ma-\nria de la Cruz", "\"María de la\""))
        assertFalse(containsPhrase("María llegó ayer de la Cruz", "\"María de la\""))
        assertEquals(1, countMatches("Ma-\nria de la Cruz", "\"María de la\""))
    }

    @Test fun oneQuotedWordIsLiteralButAThreeWordPhraseToleratesOneOcrLetter() {
        assertTrue(containsOcrLiteralPhrase("Anselmo Bár-\ncena Ubeira", "anselmo bárcena ubeira"))
        assertTrue(containsOcrLiteralPhrase("Anselmo Bárcena Ubeila", "anselmo bárcena ubeira"))
        assertTrue(containsOcrLiteralPhrase("Anselmo Bár#cena Ubeira", "anselmo bárcena ubeira"))
        assertFalse(containsOcrLiteralPhrase("Ubeila", "ubeira"))
        assertFalse(textContainsQuery("Ubeila", "\"ubeira\""))
        assertTrue(textContainsQuery("Anselmo Bárcena Ubeila", "\"anselmo bárcena ubeira\""))
        assertFalse(containsOcrLiteralPhrase("Anselmo Bárcena Lubeila", "anselmo bárcena ubeira"))
        assertFalse(containsOcrLiteralPhrase("Anselmo pintó Bárcena Ubeira", "anselmo bárcena ubeira"))
        assertFalse(containsPhrase("Anselmo Bárcana Ubeila", "\"anselmo bárcena ubeira\""))
        assertEquals(0, countMatches("Anselmo Bárcana Ubeila", "\"anselmo bárcena ubeira\""))
    }

    @Test fun mixedQueryDoesNotConfuseTheSurnameWithCanales() {
        val query = "ingeniero \"canelas\""
        val wrong = OcrHit(title = "Periódico", snippets = listOf(
            "Ingeniero de caminos, canales y puertos"), path = "1", imageId = "2", position = 1)
        val right = wrong.copy(snippets = listOf("El ingeniero Canelas dirigió las obras"))
        assertFalse(containsStrictQuotedWord("Ingeniero de canales", "canelas"))
        assertTrue(containsStrictQuotedWord("El ingeniero Ca-\nnelas", "canelas"))
        assertFalse(hitMatchesQuotedConstraints(wrong, query))
        assertTrue(hitMatchesQuotedConstraints(right, query))
    }

    @Test fun mixedQueryKeepsEveryTermAndOpenQuoteBehavesAsWords() {
        assertTrue(textContainsQuery("La Región publicó los Sucesos", "\"La Región\" Sucesos"))
        assertFalse(textContainsQuery("La Región publicó avisos", "\"La Región\" Sucesos"))
        assertEquals("María Sucesos", highlightTerms("\"María Sucesos"))
        assertTrue(textContainsQuery("Ma-ría Sucesos", "\"María Sucesos"))
    }

    @Test fun damagedGlyphAndLikelyOcrConfusionDoNotMatchUnrelatedShortWords() {
        assertTrue(textContainsQuery("En la adminis#ración local", "administración"))
        assertTrue(textContainsQuery("La rnúsica se escuchaba", "música"))
        assertTrue(textContainsQuery("La adminisrración local", "administración local"))
        assertFalse(textContainsQuery("El gato duerme", "pato"))
        assertFalse(textContainsQuery("La administración local", "administrado local"))
    }

    @Test fun broadenedPhraseMarksSeparateWordsInTheShownExcerpt() {
        val marked = highlightPossibleTerms("Jorge habló con Bárcena", "\"jorge barcena\"")
        val underlined = marked.spanStyles.map { marked.text.substring(it.start, it.end) }
        assertEquals(listOf("Jorge", "Bárcena"), underlined)
    }

    @Test fun relevancePrefersNearbyWordsOverScatteredOcrMentions() {
        fun page(vararg snippets: String) = OcrHit(
            title = "Periódico", snippets = snippets.toList(), path = "123", imageId = "45", position = 1,
        )
        val nearby = page("Jorge Bárcena asistió al acto")
        val separated = page("Jorge fue nombrado gobernador y habló después con Bárcena")
        val split = page("Jorge habló en el acto", "Bárcena estuvo presente")
        val single = page("Jorge fue nombrado gobernador")
        assertTrue(pageRelevance(nearby, "Jorge Bárcena") > pageRelevance(separated, "Jorge Bárcena"))
        assertTrue(pageRelevance(separated, "Jorge Bárcena") > pageRelevance(split, "Jorge Bárcena"))
        assertTrue(pageRelevance(split, "Jorge Bárcena") > pageRelevance(single, "Jorge Bárcena"))
    }

    @Test fun relevanceAcceptsUnknownMiddleNameButPrefersOrderedProximity() {
        fun page(text: String) = OcrHit(
            title = "Periódico", snippets = listOf(text), path = "123", imageId = "45", position = 1,
        )
        val nearby = page("Pedro Antonio Anselmo Bárcena Álvarez")
        val shortened = page("Pedro Bárcena presentó el documento")
        val scattered = page("Pedro declaró. Antonio habló con otros vecinos y Bárcena llegó después")
        val reversed = page("Bárcena se reunió con Antonio y Pedro")
        assertTrue(pageRelevance(nearby, "pedro antonio barcena") >
            pageRelevance(scattered, "pedro antonio barcena"))
        assertTrue(pageRelevance(nearby, "pedro antonio barcena") >
            pageRelevance(reversed, "pedro antonio barcena"))
        // Complete compact evidence leads a partial match, even with an inserted word.
        assertTrue(pageRelevance(nearby, "pedro antonio barcena") >
            pageRelevance(shortened, "pedro antonio barcena"))
        assertFalse(hitMatchesQuotedConstraints(nearby, "\"pedro antonio barcena\""))
        assertTrue(hitMatchesQuotedConstraints(nearby, "pedro antonio barcena"))
    }

    @Test fun unquotedTwoWordSearchRanksEveryAdjacentOccurrenceAboveScatteredWords() {
        fun page(text: String) = OcrHit(
            title = "Periódico", snippets = listOf(text), path = "123", imageId = "45", position = 1,
        )
        val adjacent = page("Carlos Javier figura en la relación")
        val repeated = page("Carlos Javier firmó. Carlos Javier volvió a firmar")
        val scattered = page("Carlos figura en una relación extensa; Javier en otra")
        assertTrue(pageRelevance(adjacent, "carlos Javier") >
            pageRelevance(scattered, "carlos Javier"))
        assertTrue(pageRelevance(repeated, "carlos Javier") >
            pageRelevance(adjacent, "carlos Javier"))
    }

    @Test fun unquotedSearchCombinesCoverageWithProximityForAnyWords() {
        fun page(text: String) = OcrHit(
            title = "Periódico", snippets = listOf(text), path = "123", imageId = "45", position = 1,
        )
        val query = "carlos javier bárcena agrónomo"
        val closeWithoutOne = page("Carlos Javier era agrónomo en la comarca")
        val allScattered = page("Carlos habló; Javier llegó mucho después. Bárcena publicó una nota. El agrónomo intervino al final")
        val allTogether = page("Carlos Javier Bárcena, agrónomo")
        assertTrue(pageRelevance(allTogether, query) > pageRelevance(closeWithoutOne, query))
        assertTrue(pageRelevance(closeWithoutOne, query) > pageRelevance(allScattered, query))
        assertTrue(unquotedCandidateQueries(query).any {
            it.contains("\"carlos javier\"") && it.contains("agrónomo") && !it.contains("bárcena")
        })
        assertTrue(unquotedCandidateQueries("\"carlos javier\"").isEmpty())
    }

    @Test fun mainCandidateAndSavedSearchShareQuotedAndUnquotedBoundaries() {
        fun page(text: String) = OcrHit(
            title = "Periódico", snippets = listOf(text), path = "123", imageId = "45", position = 1,
        )
        val near = page("Carlos Javier era agrónomo")
        val tooBroad = page("Carlos Javier saludó")
        val canals = page("El ingeniero de canales tomó posesión")
        assertTrue(plausibleUnquotedHit(near, "carlos javier bárcena agrónomo"))
        assertFalse(plausibleUnquotedHit(tooBroad, "carlos javier bárcena agrónomo"))
        assertTrue(hitMatchesQuery(near, "carlos javier bárcena agrónomo"))
        assertFalse(hitMatchesQuery(canals, "ingeniero \"canelas\""))
    }

    @Test fun relevanceWithoutOcrTermsPreservesServerOrder() {
        val first = OcrWorkGroup("Primero 1900", null, emptyList())
        val second = OcrWorkGroup("Segundo 2000", null, emptyList())
        assertEquals(listOf(first, second), sortGroups(listOf(first, second),
            SearchViewModel.SortBy.Relevancia, false, ""))
    }

    @Test fun groupingKeepsGalicianaPageOrder() {
        val laterPage = OcrHit(title = "Obra", snippets = listOf("carlos javier"),
            path = "123", imageId = "22", position = 22)
        val earlierPage = laterPage.copy(imageId = "3", position = 3)
        assertEquals(listOf(22, 3), OcrRepository().groupByWork(
            listOf(laterPage, earlierPage)).single().pages.map { it.position })
    }

    @Test fun catalogueAndOcrResultsPreserveTheirDifferentShapes() {
        val catalogue = """
            <div class="nav_registros"><div class="nav_registro">
              <div class="titulo"><a>Un libro digitalizado</a></div>
              <a href="/es/catalogo_imagenes/grupo.do?path=123&amp;idImagen=45">Copia dixital</a>
            </div></div>
        """.trimIndent()
        val work = OcrRepository().parseHits(catalogue).single()
        assertEquals("123", work.path)
        assertEquals("Un libro digitalizado", work.title)

        val ocr = """
            <div class="unidad_textual"><a href="/es/catalogo_imagenes/grupo.do?path=123&amp;idImagen=45&amp;posicion=7"
                data-analytics-recordtitle="Un libro digitalizado">Página</a>
              <ul class="texto_ocurrencias"><li>Ma- ria llegó en 1907</li></ul>
            </div>
        """.trimIndent()
        val page = OcrRepository().parseHits(ocr).single()
        assertEquals(7, page.position ?: 0)
        assertEquals("Ma- ria llegó en 1907", page.snippets.single())

        val withoutImageId = ocr.replace("&amp;idImagen=45", "")
        assertEquals("123", OcrRepository().parseHits(withoutImageId).single().path)
    }

    @Test fun webResultsIncludeBothOcrPagesAndOtherDigitizedWorks() {
        val html = """
            <ol class="nav_registros">
              <li class="nav_registro"><span class="titulo"><a>Libro con OCR</a></span>
                <ul><li class="unidad_textual"><a href="../catalogo_imagenes/grupo.do?path=123&amp;idImagen=45"
                    data-analytics-recordtitle="Libro con OCR">Página 11</a>
                  <ul class="texto_ocurrencias"><li>La palabra buscada</li></ul></li></ul></li>
              <li class="nav_registro"><span class="titulo"><a>Otra obra</a></span>
                <a href="../catalogo_imagenes/grupo.do?path=456">Copia dixital</a></li>
            </ol>
        """.trimIndent()
        val hits = OcrRepository().parseHits(html)
        assertEquals(setOf("123", "456"), hits.mapNotNull { it.path }.toSet())
    }
}
