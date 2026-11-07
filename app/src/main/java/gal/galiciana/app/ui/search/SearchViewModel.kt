package gal.galiciana.app.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.data.mergePageHitsByIdentity
import gal.galiciana.app.data.selectPageCandidates
import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.domain.OcrWorkGroup
import gal.galiciana.app.ui.containsOcrLiteralPhrase
import gal.galiciana.app.ui.quotedPhrases
import gal.galiciana.app.ui.extraTokens
import gal.galiciana.app.ui.queryTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/** Treat keyboard spaces, Unicode spaces and invisible separators identically. */
fun normalizeSearchInput(raw: String): String = raw.map { ch ->
    if (Character.isWhitespace(ch) || Character.isSpaceChar(ch) ||
        ch == '\u200B' || ch == '\uFEFF' || ch == '\u2060') ' ' else ch
}.joinToString("").replace(Regex(" +"), " ").trim()
    .replace(Regex("[“”«»„‟]"), "\"")

/** Filters belonging to the displayed result set, independent of the editable form. */
internal data class SubmittedSearchFilters(
    val title: String = "",
    val author: String = "",
    val place: String = "",
    val yearFrom: String = "",
    val yearTo: String = "",
) {
    companion object {
        fun fromForm(advanced: Boolean, title: String, author: String, place: String,
                     yearFrom: String, yearTo: String): SubmittedSearchFilters =
            if (advanced) SubmittedSearchFilters(normalizeSearchInput(title),
                normalizeSearchInput(author), normalizeSearchInput(place),
                sanitizeYear(yearFrom), sanitizeYear(yearTo))
            else SubmittedSearchFilters()
    }
}

/** Preserve pending branches while preventing a server pagination cycle. */
internal fun nextPendingSearchPages(
    pending: List<String>, loaded: List<String>, next: List<String>, visited: MutableSet<String>,
): List<String> {
    visited.addAll(loaded)
    return (pending.filterNot { it in loaded } + next).distinct().filterNot { it in visited }
}

class SearchViewModel(
    private val repo: OcrRepository = OcrRepository(),
) : ViewModel() {
    private var sortJob: Job? = null
    private var variantJob: Job? = null
    private var recoveryJob: Job? = null
    private var submittedFilters = SubmittedSearchFilters()
    private val visitedPageUrls = mutableSetOf<String>()
    private var sortUrls: Map<String, String> = emptyMap()
    private var unverifiedQuotedHits: List<OcrHit> = emptyList()
    private var unverifiedQuotedTruncated = false
    var totalWorks by mutableStateOf<Int?>(null)
        private set
    var globalSortAvailable by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
    var showingResults by mutableStateOf(false)
    var searchGeneration by mutableStateOf(0)
        private set
    var completedSearchGeneration by mutableStateOf(0)
        private set
    var fullText by mutableStateOf(true)
    var title by mutableStateOf("")
    var author by mutableStateOf("")
    var place by mutableStateOf("")
    var yearFrom by mutableStateOf("")
    var yearTo by mutableStateOf("")
    var section by mutableStateOf("")
    var advanced by mutableStateOf(false)
    var submittedQuery by mutableStateOf("")
        private set
    var submittedSection by mutableStateOf("")
        private set
    var variantHits by mutableStateOf<List<OcrHit>>(emptyList())
        private set
    var variantGroups by mutableStateOf<List<OcrWorkGroup>>(emptyList())
        private set
    var variantLoading by mutableStateOf(false)
        private set
    var variantScanIncomplete by mutableStateOf(false)
        private set
    var recoveryHits by mutableStateOf<List<OcrHit>>(emptyList())
        private set
    var recoveryGroups by mutableStateOf<List<OcrWorkGroup>>(emptyList())
        private set
    var recoveryLoading by mutableStateOf(false)
        private set
    var recoverySearched by mutableStateOf(false)
        private set
    var recoveryIncomplete by mutableStateOf(false)
        private set
    var recoveryLimit by mutableStateOf(50)
        private set

    companion object {
        val SECTIONS = listOf(
            "Sucesos y sociedad",
            "Sucesos",
            "Sociedad",
            "Política",
            "Local",
            "Deportes",
            "Cultura",
            "Esquelas",
            "Anuncios",
        )
    }
    var hits by mutableStateOf<List<OcrHit>>(emptyList())
        private set
    var groups by mutableStateOf<List<OcrWorkGroup>>(emptyList())
        private set
    var sortBy by mutableStateOf(SortBy.Relevancia)
        private set
    var sortAsc by mutableStateOf(false)
        private set

    enum class SortBy { Titulo, Autor, Fecha, Relevancia }
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var incomplete by mutableStateOf(false)
        private set
    var pendingUrls by mutableStateOf<List<String>>(emptyList())
        private set

    val yearError: String?
        get() {
            if (!advanced) return null
            if ((yearFrom.isNotBlank() && sanitizeYear(yearFrom).isBlank()) ||
                (yearTo.isNotBlank() && sanitizeYear(yearTo).isBlank()))
                return "Introduce años de cuatro cifras entre 1000 y 2100."
            if (yearFrom.isNotBlank() && yearTo.isNotBlank() &&
                yearFrom.trim().toInt() > yearTo.trim().toInt())
                return "El año inicial no puede ser posterior al final."
            return null
        }

    val canSearch: Boolean
        get() = yearError == null && (normalizeSearchInput(query).isNotBlank() ||
            (advanced && (title.isNotBlank() || author.isNotBlank() || place.isNotBlank() ||
                yearFrom.isNotBlank() || yearTo.isNotBlank())))

    fun search() {
        if (!canSearch || loading) return
        val requestedQuery = normalizeSearchInput(query)
        sortJob?.cancel()
        variantJob?.cancel()
        recoveryJob?.cancel()
        searchGeneration++
        viewModelScope.launch {
            loading = true
            error = null
            incomplete = false
            variantHits = emptyList()
            variantGroups = emptyList()
            variantLoading = false
            variantScanIncomplete = false
            recoveryHits = emptyList()
            recoveryGroups = emptyList()
            recoveryLoading = false
            recoverySearched = false
            recoveryIncomplete = false
            unverifiedQuotedHits = emptyList()
            unverifiedQuotedTruncated = false
            pendingUrls = emptyList()
            visitedPageUrls.clear()
            sortUrls = emptyMap()
            totalWorks = null
            globalSortAvailable = false
            showingResults = true
            hits = emptyList()
            groups = emptyList()
            val normalizedQ = requestedQuery
            if (query != normalizedQ) query = normalizedQ
            val apiQ = if (normalizedQ.count { it == '"' } % 2 == 0) normalizedQ
                else normalizedQ.replace("\"", "")
            submittedFilters = SubmittedSearchFilters.fromForm(advanced, title, author, place, yearFrom, yearTo)
            val effectiveTitle = submittedFilters.title
            val effectiveAuthor = submittedFilters.author
            val effectivePlace = submittedFilters.place
            val from = submittedFilters.yearFrom
            val to = submittedFilters.yearTo
            val selectedSection = if (advanced) section else ""
            val requestedFullText = fullText
            // Only a full-text query belongs in the viewer's page search.
            submittedQuery = if (requestedFullText) normalizedQ else ""
            submittedSection = selectedSection
            val sections = when {
                selectedSection.equals("Sucesos y Sociedad", true) ->
                    listOf("Sucesos", "Sociedad")
                selectedSection.isBlank() -> emptyList()
                else -> listOf(selectedSection)
            }
            val literalPhrase = quotedPhrases(apiQ).singleOrNull()
                ?.takeIf { extraTokens(apiQ).isEmpty() && requestedFullText && sections.isEmpty() }
            try {
                suspend fun find(term: String): List<OcrRepository.SearchResult> {
                    val terms = if (sections.isEmpty()) listOf(term) else sections.map { "$term $it".trim() }
                    return terms.map { text ->
                        repo.searchAllPagesDetailed(text, effectiveTitle, effectiveAuthor, effectivePlace,
                            from, to, requestedFullText, maxPages = 1)
                    }
                }
                // Preserve the server query. Auxiliary OCR candidates remain separate.
                var results = find(apiQ)
                // The website orders the full result set. Always request its first page
                // in the selected order before displaying any locally loaded subset.
                val key = "${sortBy.name}:${if (sortAsc) "N" else "S"}"
                results = results.map { result ->
                    result.sortUrls[key]?.takeIf { sortBy != SortBy.Relevancia }?.let {
                        val sorted = repo.searchNextPage(it)
                        visitedPageUrls += it
                        sorted
                    } ?: result
                }
                sortUrls = results.singleOrNull()?.sortUrls.orEmpty()
                globalSortAvailable = sortUrls.isNotEmpty() && results.size == 1 &&
                    sortBy != SortBy.Relevancia
                totalWorks = results.singleOrNull()?.totalWorks
                incomplete = results.any { it.incomplete }
                pendingUrls = results.mapNotNull { it.nextUrl }.distinct().filterNot { it in visitedPageUrls }
                val shown = strictQuotedHits(mergePageHitsByIdentity(results.flatMap { it.hits }))
                hits = shown
                groups = withContext(Dispatchers.Default) { rankedGroups(shown) }
                completedSearchGeneration = searchGeneration
                if (shown.isEmpty()) error = if (pendingUrls.isNotEmpty())
                    "Sin coincidencias verificadas en este lote; carga más resultados."
                else "Sin resultados con esos criterios"
                if (literalPhrase != null) {
                    val generation = searchGeneration
                    variantLoading = true
                    variantJob = viewModelScope.launch {
                        try {
                            // This is an indexed candidate search, not the fuzzy recovery.
                            // Verify the quoted words and their order against each OCR excerpt.
                            val words = queryTokens(literalPhrase)
                            val candidateQueries = (listOf(words.joinToString(" ")) +
                                if (words.size > 1) words.indices.map { omitted ->
                                    words.filterIndexed { index, _ -> index != omitted }.joinToString(" ")
                                } else emptyList()).distinct()
                            val candidatePages = candidateQueries.map { candidateQuery ->
                                repo.searchAllPagesDetailed(candidateQuery, effectiveTitle,
                                    effectiveAuthor, effectivePlace, from, to, true,
                                    maxPages = if (words.size == 1) 4 else 2)
                            }
                            if (generation == searchGeneration) {
                                val verifiedCandidates = mergePageHitsByIdentity(candidatePages.flatMap { it.hits })
                                    .filter { it.snippets.any { excerpt -> containsOcrLiteralPhrase(excerpt, literalPhrase) } }
                                val selection = selectPageCandidates(verifiedCandidates, hits, 200)
                                variantScanIncomplete = candidatePages.any { it.incomplete } || selection.truncated
                                variantHits = selection.hits
                                refreshVariantGroups()
                                val verified = variantHits.mapTo(mutableSetOf()) { hitKey(it) }
                                recoveryHits = recoveryHits.filter { hitKey(it) !in verified }
                                refreshRecoveryGroups()
                                if (variantHits.isNotEmpty() && hits.isEmpty()) error = null
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            // The original exact search remains usable if the candidate scan fails.
                        } finally {
                            if (generation == searchGeneration) variantLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No se pudo completar la búsqueda: ${e.message ?: "error de conexión"}"
                incomplete = true
            }
            loading = false
        }
    }

    fun loadMore() {
        if (loading || pendingUrls.isEmpty()) return
        val urls = pendingUrls.take(2)
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val pages = urls.map { repo.searchNextPage(it) }
                val newHits = pages.flatMap { it.hits }
                val combined = strictQuotedHits(mergePageHitsByIdentity(hits + newHits))
                hits = combined
                val exactKeys = combined.mapTo(mutableSetOf()) { hitKey(it) }
                variantHits = variantHits.filter { hitKey(it) !in exactKeys }
                refreshVariantGroups()
                recoveryHits = recoveryHits.filter { hitKey(it) !in exactKeys }
                refreshRecoveryGroups()
                groups = withContext(Dispatchers.Default) { rankedGroups(combined) }
                pendingUrls = nextPendingSearchPages(pendingUrls, urls, pages.mapNotNull { it.nextUrl }, visitedPageUrls)
                if (pages.size == 1) sortUrls = pages.single().sortUrls
                globalSortAvailable = sortUrls.isNotEmpty() && pages.size == 1 &&
                    sortBy != SortBy.Relevancia
                incomplete = pendingUrls.isNotEmpty()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No se pudieron cargar más resultados: ${e.message ?: "error de conexión"}"
            } finally {
                loading = false
            }
        }
    }

    fun setSort(next: SortBy) {
        if (loading) return
        if (next == SortBy.Relevancia) {
            sortJob?.cancel()
            sortBy = SortBy.Relevancia
            sortAsc = false
            error = null
            globalSortAvailable = false
            sortJob = viewModelScope.launch {
                groups = withContext(Dispatchers.Default) { rankedGroups(hits) }
                refreshVariantGroups()
                refreshRecoveryGroups()
            }
            return
        }
        val previousSort = sortBy
        val previousAsc = sortAsc
        if (sortBy == next && next != SortBy.Relevancia) sortAsc = !sortAsc
        else {
            sortBy = next
            sortAsc = next != SortBy.Fecha && next != SortBy.Relevancia
        }
        val key = "${sortBy.name}:${if (sortAsc) "N" else "S"}"
        val url = sortUrls[key]
        if (url == null) {
            sortBy = previousSort
            sortAsc = previousAsc
            error = "Galiciana no ofrece este orden para la búsqueda."
            return
        }
        sortJob?.cancel()
        sortJob = viewModelScope.launch {
            loading = true
            error = null
            try {
                val result = repo.searchNextPage(url)
                sortUrls = result.sortUrls
                totalWorks = result.totalWorks
                hits = strictQuotedHits(result.hits)
                val exactKeys = hits.mapTo(mutableSetOf()) { hitKey(it) }
                variantHits = variantHits.filter { hitKey(it) !in exactKeys }
                refreshVariantGroups()
                recoveryHits = recoveryHits.filter { hitKey(it) !in exactKeys }
                refreshRecoveryGroups()
                visitedPageUrls.clear()
                visitedPageUrls += url
                pendingUrls = listOfNotNull(result.nextUrl).filterNot { it in visitedPageUrls }
                incomplete = result.incomplete
                groups = withContext(Dispatchers.Default) { rankedGroups(hits) }
                globalSortAvailable = sortUrls.isNotEmpty() && sortBy != SortBy.Relevancia
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                sortBy = previousSort
                sortAsc = previousAsc
                error = "No se pudo cambiar el orden. Se conserva el anterior. Comprueba la conexión y vuelve a intentarlo."
            } finally {
                loading = false
            }
        }
    }

    /** An explicit, bounded second search. It never replaces precise results. */
    fun loadRecovery() {
        if (loading || recoveryLoading || submittedQuery.isBlank()) return
        val generation = searchGeneration
        val filters = submittedFilters
        val words = queryTokens(submittedQuery)
        if (words.isEmpty()) return
        val queries = recoveryCandidateQueries(submittedQuery)
        if (queries.isEmpty()) return
        recoveryLimit = if ((totalWorks ?: hits.size) >= 25)
            ((totalWorks ?: hits.size) * 2).coerceAtMost(200) else 50
        recoveryJob = viewModelScope.launch {
            recoveryLoading = true
            recoverySearched = false
            try {
                val pages = queries.map { term ->
                    repo.searchAllPagesDetailed(term, filters.title, filters.author, filters.place,
                        filters.yearFrom, filters.yearTo,
                        true, maxPages = if (words.size == 1) 1 else 2)
                }
                if (generation == searchGeneration) {
                    recoverySearched = true
                    val selection = selectPageCandidates(unverifiedQuotedHits + pages.flatMap { it.hits },
                        hits + variantHits, recoveryLimit)
                    recoveryIncomplete = pages.any { it.incomplete } || selection.truncated || unverifiedQuotedTruncated
                    recoveryHits = selection.hits
                    refreshRecoveryGroups()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (generation == searchGeneration) error = "No se pudo cargar la repesca: ${e.message ?: "error de conexión"}"
            } finally {
                if (generation == searchGeneration) recoveryLoading = false
            }
        }
    }

    // Pagination, sorting and auxiliary scans may finish in different orders.
    // A suspended grouping must never overwrite groups for a newer hit list.
    private suspend fun refreshVariantGroups() {
        val snapshot = variantHits
        val grouped = withContext(Dispatchers.Default) { rankedGroups(snapshot) }
        if (variantHits === snapshot) variantGroups = grouped
    }

    private suspend fun refreshRecoveryGroups() {
        val snapshot = recoveryHits
        val grouped = withContext(Dispatchers.Default) { rankedGroups(snapshot) }
        if (recoveryHits === snapshot) recoveryGroups = grouped
    }

    /** Re-rank only relevance, within each existing admission category and loaded subset. */
    private fun rankedGroups(items: List<OcrHit>): List<OcrWorkGroup> {
        val grouped = repo.groupByWork(items)
        return if (sortBy == SortBy.Relevancia) sortGroups(grouped, sortBy, sortAsc, submittedQuery)
            else grouped
    }

    private fun hitKey(hit: OcrHit) = "${hit.path}|${hit.imageId}|${hit.position}"

    private fun strictQuotedHits(items: List<OcrHit>): List<OcrHit> {
        val quoted = quotedPhrases(submittedQuery)
        if (quoted.isEmpty()) return items
        val (verified, unverified) = items.partition { hitMatchesQuotedConstraints(it, submittedQuery) }
        // Excerpts are truncated. A missing quoted word is not proof that the
        // complete OCR lacks it, so retain these pages for the optional recovery.
        val retained = mergePageHitsByIdentity(unverifiedQuotedHits + unverified)
        unverifiedQuotedTruncated = unverifiedQuotedTruncated || retained.size > 200
        unverifiedQuotedHits = retained.take(200)
        return verified
    }
}
