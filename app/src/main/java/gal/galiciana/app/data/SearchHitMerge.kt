package gal.galiciana.app.data

import gal.galiciana.app.domain.OcrHit

/** Keep server page order while retaining every different excerpt of duplicate page hits. */
fun mergePageHitsByIdentity(items: Iterable<OcrHit>): List<OcrHit> {
    val merged = linkedMapOf<Triple<String?, String?, Int?>, OcrHit>()
    for (hit in items) {
        val key = Triple(hit.path, hit.imageId, hit.position)
        val old = merged[key]
        merged[key] = if (old == null) hit.copy(snippets = hit.snippets.filter(String::isNotBlank).distinct())
        else old.copy(
            title = old.title.ifBlank { hit.title },
            parentTitle = old.parentTitle?.takeIf(String::isNotBlank) ?: hit.parentTitle,
            snippets = (old.snippets + hit.snippets).filter(String::isNotBlank).distinct(),
            savedQuery = old.savedQuery.ifBlank { hit.savedQuery },
            note = old.note.ifBlank { hit.note },
        )
    }
    return merged.values.toList()
}

internal data class BoundedPageCandidates(val hits: List<OcrHit>, val truncated: Boolean)

/** Candidate admission depends on page identity, never on silently excluding an exact phrase. */
internal fun selectPageCandidates(items: Iterable<OcrHit>, excluded: List<OcrHit>, limit: Int): BoundedPageCandidates {
    require(limit >= 0)
    val excludedKeys = excluded.mapTo(mutableSetOf()) { Triple(it.path, it.imageId, it.position) }
    val available = mergePageHitsByIdentity(items).filterNot { Triple(it.path, it.imageId, it.position) in excludedKeys }
    return BoundedPageCandidates(available.take(limit), available.size > limit)
}
