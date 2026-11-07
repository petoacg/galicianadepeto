package gal.galiciana.app.ui.search

import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.domain.OcrWorkGroup
import gal.galiciana.app.ui.extraTokens
import gal.galiciana.app.ui.foldForSearch
import gal.galiciana.app.ui.foldedBlob
import gal.galiciana.app.ui.quotedPhrases
import gal.galiciana.app.ui.queryTokens
import gal.galiciana.app.ui.lettersOnly
import gal.galiciana.app.ui.dehyphen
import gal.galiciana.app.ui.containsOcrLiteralPhrase
import gal.galiciana.app.ui.containsStrictQuotedWord

private val Articles = setOf(
    "el", "la", "los", "las", "lo", "un", "una", "unos", "unas",
    "o", "a", "os", "as", "un", "unha", "uns", "unhas",
    "the", "le", "les", "l",
)

fun sortTitleKey(title: String): String {
    var t = title.trim().lowercase()
        .replace(Regex("""^\s*\d+\s*[-.:)]\s*"""), "")
    val words = t.split(Regex("\\s+")).filter { it.isNotBlank() }
    val dropped = words.dropWhile { w ->
        foldForSearch(w.trim('«', '»', '"', '.', ',', ':')) in Articles
    }
    return foldForSearch((dropped.ifEmpty { words }).joinToString(" "))
}

fun yearOfText(vararg parts: String?): Int {
    val blob = parts.filterNotNull().joinToString(" ")
    return Regex("""\b((?:17|18|19|20)\d{2})\b""")
        .findAll(blob)
        .mapNotNull { it.groupValues[1].toIntOrNull() }
        .maxOrNull() ?: 0
}

private val SnippetWordRegex = Regex("[\\p{L}\\p{N}]+(?:[-‐‑–—­][\\p{L}\\p{N}]+)*")
private fun termsOf(query: String) = queryTokens(query).map(::lettersOnly).filter { it.isNotBlank() }.distinct()
private data class PageRank(val score: Int, val seen: Set<String>)

/** Bounded queries for candidate recall; no word is treated as a name or occupation. */
fun unquotedCandidateQueries(query: String): List<String> {
    val words = queryTokens(query)
    if (words.size !in 2..6 || quotedPhrases(query).isNotEmpty()) return emptyList()
    val exact = "\"${words.joinToString(" ")}\""
    val omissions = (1 until words.lastIndex).map { omitted ->
        val rest = words.filterIndexed { index, _ -> index != omitted }
        if (rest.size == 2) "\"${rest.joinToString(" ")}\""
        else "\"${rest.take(2).joinToString(" ")}\" ${rest.drop(2).joinToString(" ")}"
    }
    val adjacent = (0 until words.lastIndex).map { start ->
        words.mapIndexed { index, word ->
            when (index) {
                start -> "\"$word"
                start + 1 -> "$word\""
                else -> word
            }
        }.joinToString(" ")
    }
    return (listOf(exact) + omissions + adjacent).distinct().take(7)
}

/** Bounded proximity windows, computed off the UI thread by the caller. */
private fun pageRank(hit: OcrHit, terms: List<String>, quoted: Boolean): PageRank {
    if (terms.isEmpty()) return PageRank(0, emptySet())
    val indexes = terms.withIndex().associate { it.value to it.index }
    val seen = mutableSetOf<String>()
    var best = 0
    var strongOccurrences = 0
    hit.snippets.forEach { snippet ->
        val words = SnippetWordRegex.findAll(dehyphen(snippet)).map { foldForSearch(it.value) }.toList()
        for (start in words.indices) {
            val first = indexes[words[start]] ?: continue
            val found = BooleanArray(terms.size)
            found[first] = true
            seen += terms[first]
            var covered = 1
            var lastIndex = first
            var ordered = true
            val limit = minOf(words.lastIndex, start + 35)
            for (end in start..limit) {
                if (end > start) {
                    val index = indexes[words[end]] ?: continue
                    seen += terms[index]
                    if (found[index]) continue
                    found[index] = true
                    covered++
                    if (index < lastIndex) ordered = false
                    lastIndex = index
                }
                if (covered < 2) continue
                val gap = end - start - covered + 1
                val all = covered == terms.size
                val skippedMiddle = !quoted && terms.size >= 3 && covered == terms.size - 1 &&
                    found.first() && found.last() && !found.slice(1 until terms.lastIndex).all { it }
                val adjacentPairs = (start until end).count { at ->
                    val index = indexes[words[at]]
                    index != null && indexes[words[at + 1]] == index + 1
                }.coerceAtMost(terms.size - 1)
                val score = covered * 2500 + (6000 - gap * 250).coerceAtLeast(0) +
                    (if (ordered) 800 else 0) + (if (all) 1000 else 0) +
                    (if (skippedMiddle) 1500 else 0) + adjacentPairs * 800 +
                    (if (all && ordered && gap == 0) 1200 else 0) +
                    (if (quoted && all && ordered && gap == 0) 2500 else 0)
                best = maxOf(best, score)
                if (ordered && gap <= 2) strongOccurrences++
            }
        }
    }
    return PageRank(if (best > 0) best + strongOccurrences.coerceAtMost(20) * 15
        else if (seen.size == terms.size && terms.size > 1) 1000 * seen.size
        else seen.size * 200, seen)
}

fun pageRelevance(hit: OcrHit, query: String): Int =
    pageRank(hit, termsOf(query), quotedPhrases(query).isNotEmpty() && extraTokens(query).isEmpty()).score

/** Saved-result filtering may omit one unquoted term; the remote main query remains unchanged. */
fun plausibleUnquotedHit(hit: OcrHit, query: String): Boolean {
    val terms = termsOf(query)
    if (terms.isEmpty()) return false
    val rank = pageRank(hit, terms, quoted = false)
    return rank.seen.size >= if (terms.size == 1) 1 else maxOf(2, terms.size - 1)
}

private fun groupScore(group: OcrWorkGroup, ranks: List<PageRank>, terms: List<String>,
                       titleF: String, authorF: String, placeF: String): Int {
    val titleBlob = foldedBlob(group.title + " " + group.parentTitle.orEmpty())
    val seen = ranks.flatMapTo(mutableSetOf<String>()) { it.seen }
    val best = ranks.maxOfOrNull { it.score } ?: 0
    var score = best + if (terms.size > 1 && seen.size == terms.size && best < 4000) 1800 else 0
    if (terms.isNotEmpty() && terms.all { titleBlob.split(' ').contains(it) }) score += 1200
    if (titleF.isNotBlank() && titleBlob.contains(foldForSearch(titleF))) score += 30
    if (authorF.isNotBlank() && titleBlob.contains(foldForSearch(authorF))) score += 20
    if (placeF.isNotBlank() && titleBlob.contains(foldForSearch(placeF))) score += 15
    return score
}

fun relevanceScore(group: OcrWorkGroup, query: String, titleF: String, authorF: String, placeF: String): Int {
    val terms = termsOf(query)
    val quoted = quotedPhrases(query).isNotEmpty() && extraTokens(query).isEmpty()
    return groupScore(group, group.pages.map { pageRank(it, terms, quoted) }, terms, titleF, authorF, placeF)
}

fun sortGroups(
    list: List<OcrWorkGroup>,
    sortBy: SearchViewModel.SortBy,
    asc: Boolean,
    query: String = "",
    titleF: String = "",
    authorF: String = "",
    placeF: String = "",
): List<OcrWorkGroup> {
    if (sortBy == SearchViewModel.SortBy.Relevancia) {
        val terms = termsOf(query)
        if (terms.isEmpty()) return list
        val quoted = quotedPhrases(query).isNotEmpty() && extraTokens(query).isEmpty()
        return list.map { group ->
            val ranks = group.pages.map { pageRank(it, terms, quoted) }
            val sortedPages = group.pages.indices.sortedByDescending { ranks[it].score }.map { group.pages[it] }
            Triple(group.copy(pages = sortedPages), groupScore(group, ranks, terms, titleF, authorF, placeF),
                yearOfText(group.title, group.parentTitle))
        }.sortedWith(compareByDescending<Triple<OcrWorkGroup, Int, Int>> { it.second }
            .thenByDescending { it.third }).map { it.first }
    }
    val cmp = when (sortBy) {
        SearchViewModel.SortBy.Titulo -> compareBy<OcrWorkGroup> { sortTitleKey(it.title) }
        SearchViewModel.SortBy.Autor -> compareBy { sortTitleKey(it.parentTitle ?: it.title) }
        SearchViewModel.SortBy.Fecha -> compareBy { yearOfText(it.title, it.parentTitle) }
        SearchViewModel.SortBy.Relevancia -> error("Handled above")
    }
    val sorted = if (asc) list.sortedWith(cmp) else list.sortedWith(cmp.reversed())
    return sorted
}

fun hitMatchesQuery(hit: OcrHit, query: String): Boolean {
    if (query.isBlank()) return true
    val fields = hit.snippets + listOf(hit.title, hit.parentTitle.orEmpty())
    val phrases = quotedPhrases(query)
    val extra = extraTokens(query)
    if (phrases.isEmpty()) return plausibleUnquotedHit(hit.copy(snippets = fields), query)
    return hitMatchesQuotedConstraints(hit, query) &&
        extra.all { token -> fields.any { gal.galiciana.app.ui.textContainsQuery(it, token) } }
}

/** Quoted terms are compulsory even if the website's OCR index is permissive. */
fun hitMatchesQuotedConstraints(hit: OcrHit, query: String): Boolean {
    val quoted = quotedPhrases(query)
    if (quoted.isEmpty()) return true
    val fields = hit.snippets + listOf(hit.title, hit.parentTitle.orEmpty())
    return quoted.all { phrase ->
        if (queryTokens(phrase).size == 1)
            fields.any { containsStrictQuotedWord(it, phrase) }
        else fields.any { containsOcrLiteralPhrase(it, phrase) }
    }
}

fun sanitizeYear(raw: String): String = raw.trim().takeIf {
    it.matches(Regex("""\d{4}""")) && (it.toIntOrNull() ?: 0) in 1000..2100
}.orEmpty()
