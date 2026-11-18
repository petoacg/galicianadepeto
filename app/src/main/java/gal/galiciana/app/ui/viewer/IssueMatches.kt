package gal.galiciana.app.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import gal.galiciana.app.i18n.tr

/** Pages read per document when looking for the viewer's query on other pages. */
internal const val ISSUE_SCAN_CAP = 120

/**
 * Order in which the other pages are read: nearest to the open page first, so the
 * neighbours a reader is most likely to visit answer before distant ones.
 * Documents longer than [cap] are read only around the open page.
 */
internal fun issueScanOrder(current: Int, pageCount: Int, cap: Int = ISSUE_SCAN_CAP): List<Int> {
    if (pageCount <= 0) return emptyList()
    val start = current.coerceIn(1, pageCount)
    val order = ArrayList<Int>(minOf(pageCount, cap))
    order += start
    var step = 1
    while (order.size < minOf(pageCount, cap) && (start - step >= 1 || start + step <= pageCount)) {
        if (start + step <= pageCount && order.size < cap) order += start + step
        if (start - step >= 1 && order.size < cap) order += start - step
        step++
    }
    return order
}

/**
 * Page to open when the reader moves past the last (or first) match of [current].
 * Wraps around the document; null when no other page has matches.
 */
internal fun adjacentMatchPage(matchPages: Collection<Int>, current: Int, forward: Boolean): Int? {
    val others = matchPages.filter { it != current }.sorted()
    if (others.isEmpty()) return null
    return if (forward) others.firstOrNull { it > current } ?: others.first()
    else others.lastOrNull { it < current } ?: others.last()
}

/**
 * Pages of a document that the last search results named for a query. The viewer
 * lists them at once instead of downloading every page to find them again.
 */
object SearchPageHints {
    private var path: String? = null
    private var query: String = ""
    private var known: Set<Int> = emptySet()

    fun remember(path: String, query: String, pages: Set<Int>) {
        this.path = path
        this.query = gal.galiciana.app.ui.highlightTerms(query)
        known = pages
    }

    fun pages(path: String, query: String): Set<Int> =
        if (path == this.path && gal.galiciana.app.ui.highlightTerms(query) == this.query && this.query.isNotBlank()) known
        else emptySet()
}

/**
 * One line under the viewer's query: the pages of this document whose text contains
 * it, as numbers to jump to, with the open page marked. Until the reader asks to
 * read the rest of the document, the numbers are those the search results named.
 */
@Composable
internal fun MatchPagesRow(
    pages: Map<Int, Int>,
    current: Int,
    checked: Int,
    toCheck: Int,
    failed: Int,
    pageCount: Int,
    scanned: Boolean,
    onOpen: (Int) -> Unit,
    onScanAll: () -> Unit,
) {
    val scanning = scanned && checked < toCheck
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).horizontalScroll(rememberScrollState())
            .heightIn(min = 48.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val onlyHere = pages.keys.all { it == current }
        when {
            pages.isNotEmpty() && onlyHere && !scanning -> Text(
                if (scanned) tr("Só nesta páxina", "Solo en esta página") else tr("Nos resultados, só esta páxina", "En los resultados, solo esta página"),
                style = MaterialTheme.typography.labelMedium, color = muted)
            pages.isEmpty() -> Text(
                when {
                    scanning -> tr("Buscando noutras páxinas… $checked de $toCheck", "Buscando en otras páginas… $checked de $toCheck")
                    !scanned -> tr("Outras páxinas sen revisar", "Otras páginas sin revisar")
                    failed == toCheck -> tr("Non se puideron revisar as outras páxinas", "No se pudieron revisar las otras páginas")
                    else -> tr("Non aparece no texto doutras páxinas", "No aparece en el texto de otras páginas")
                },
                style = MaterialTheme.typography.labelMedium, color = muted,
            )
            else -> {
                Text(tr("Páxinas", "Páginas"), style = MaterialTheme.typography.labelMedium, color = muted)
                pages.keys.sorted().forEach { p ->
                    val count = pages.getValue(p)
                    val here = p == current
                    FilterChip(
                        selected = here,
                        onClick = { if (!here) onOpen(p) },
                        label = { Text("$p", style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = when (count) {
                                0 -> tr("Páxina $p", "Página $p")
                                1 -> tr("Páxina $p, 1 coincidencia", "Página $p, 1 coincidencia")
                                else -> tr("Páxina $p, $count coincidencias", "Página $p, $count coincidencias")
                            }
                            if (here) stateDescription = tr("Aberta", "Abierta")
                        },
                    )
                }
            }
        }
        if (scanning && pages.isNotEmpty()) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = muted)
            Text("$checked / $toCheck", style = MaterialTheme.typography.labelSmall, color = muted)
        }
        if (!scanned && pageCount > 1) TextButton(onClick = onScanAll, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(tr("Revisar todo o documento", "Revisar todo el documento"))
        }
        if (scanned && !scanning && failed > 0) TextButton(onClick = onScanAll, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (failed == 1) tr("1 sen revisar · Reintentar", "1 sin revisar · Reintentar")
                else tr("$failed sen revisar · Reintentar", "$failed sin revisar · Reintentar"))
        }
        if (scanned && !scanning && failed == 0 && toCheck < pageCount) Text(
            tr("· revisadas $toCheck de $pageCount", "· revisadas $toCheck de $pageCount"),
            style = MaterialTheme.typography.labelSmall, color = muted,
        )
    }
}
