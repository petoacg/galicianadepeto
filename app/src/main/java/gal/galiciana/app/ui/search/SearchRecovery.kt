package gal.galiciana.app.ui.search

import gal.galiciana.app.ui.queryTokens

/** Explicit approximate search: bounded candidates, never a replacement for the main AND query. */
internal fun recoveryCandidateQueries(query: String, limit: Int = 12): List<String> {
    val words = queryTokens(query).distinctBy { it.lowercase() }
    if (words.isEmpty() || limit <= 0) return emptyList()
    if (words.size == 1) {
        val word = words.single()
        if (word.length < 5) return emptyList()
        return word.indices.take(limit).map { word.replaceRange(it, it + 1, "?") }
    }
    val candidates = linkedSetOf(words.joinToString(" "))
    words.indices.forEach { omitted ->
        if (candidates.size < limit)
            candidates += words.filterIndexed { index, _ -> index != omitted }.joinToString(" ")
    }
    // If OCR lost two of four terms, omitting only one could never retrieve the page.
    // Try pairs without issuing one request for every possible subset of a long query.
    for (first in words.indices) for (second in first + 1 until words.size) {
        if (candidates.size >= limit) break
        candidates += "${words[first]} ${words[second]}"
    }
    return candidates.take(limit)
}
