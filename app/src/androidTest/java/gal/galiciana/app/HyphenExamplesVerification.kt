package gal.galiciana.app

import gal.galiciana.app.data.OcrRepository
import gal.galiciana.app.domain.OcrHit
import gal.galiciana.app.ui.countMatches
import gal.galiciana.app.ui.highlightRegex
import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject

/** Bounded, on-demand live lookup of the user's supplied newspaper examples. */
internal suspend fun verifyHyphenExamples(only: String?, year: String?, progress: (String) -> Unit): String {
    data class Example(val canonical: String, val split: String, val target: String, val anchor: String)
    val examples = listOf(
        Example("Federación Gallega", "Ga-llega", "Gallega", "Federación"),
        Example("Elisardo Núñez Seabra", "Sea-bra", "Seabra", "Elisardo"),
        Example("Manuel Fernández", "Fer-nández", "Fernández", "Manuel Fer*"),
        Example("Amador", "Ama-dor", "Amador", "Ama dor"),
        Example("Regimiento de Infantería Zamora", "Infante-ria", "Infantería", "Regimiento Zamora"),
        Example("Xinzo de Limia", "Li-mia", "Limia", "Xinzo"),
        Example("Javier Vázquez", "Ja-vier", "Javier", "Vázquez Ja*"),
    ).filter { only == null || it.canonical.contains(only, true) }
    fun fold(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase()
    val repo = OcrRepository()
    val reports = JSONArray()
    for (example in examples) {
        progress("Buscando ${example.canonical}")
        val parts = example.split.split('-')
        val cut = Regex("(?<![\\p{L}])${Regex.escape(fold(parts[0]))}[-‐‑–—­]\\s*${Regex.escape(fold(parts[1]))}(?![\\p{L}])")
        val report = JSONObject().put("query", example.canonical)
        val visited = mutableSetOf<String>()
        var inspected = 0
        var found = false
        val errors = JSONArray()
        val searches = listOf("\"${example.canonical}\"",
            "\"${example.canonical.replace(example.target, example.split, true)}\"", example.anchor).distinct()
        for (query in searches) {
            if (found || inspected >= 12) break
            try {
                val first = repo.searchAllPagesDetailed(query, yearFrom = year.orEmpty(), yearTo = year.orEmpty(), maxPages = 1)
                var ranked = first.sortUrls["Relevancia:S"]?.let {
                    try { repo.searchNextPage(it) } catch (_: Exception) { first }
                } ?: first
                val rankedHits = ranked.hits.toMutableList()
                if (only != null && query.startsWith('"')) repeat(3) {
                    val next = ranked.nextUrl
                    if (next != null) try {
                        ranked = repo.searchNextPage(next)
                        rankedHits += ranked.hits
                    } catch (_: Exception) { }
                }
                val candidates = (rankedHits + first.hits).distinctBy { "${it.path}|${it.position}" }
                    .sortedByDescending { hit ->
                        if (hit.snippets.any { snippet -> highlightRegex("\"${example.canonical}\"")?.findAll(snippet)
                            ?.any { cut.containsMatchIn(fold(it.value)) } == true }) 100
                        else if (hit.snippets.any { cut.containsMatchIn(fold(it)) }) 10
                        else if (hit.snippets.any { fold(it).contains(fold(parts[0]) + "-") }) 5 else 0
                    }
                for (hit in candidates.take(4)) {
                    val path = hit.path ?: continue
                    val pos = hit.position ?: continue
                    if (!visited.add("$path|$pos")) continue
                    if (inspected++ >= 12) break
                    try {
                        val text = repo.loadPage(path, pos, hit.imageId).ocrText
                        val passage = highlightRegex("\"${example.canonical}\"")?.findAll(text)
                            ?.firstOrNull { cut.containsMatchIn(fold(it.value)) } ?: continue
                        val match = cut.find(fold(passage.value)) ?: continue
                        val marked = repo.loadAltoSearch(path, pos, "\"${example.target}\"", hit.imageId)
                        report.put("found", true).put("path", path).put("position", pos)
                            .put("imageId", hit.imageId).put("title", hit.title)
                            .put("retrievalQuery", query).put("splitEvidence", match.value)
                            .put("context", passage.value)
                            .put("fullPhraseMatches", countMatches(text, "\"${example.canonical}\""))
                            .put("textMatches", countMatches(text, "\"${example.target}\""))
                            .put("imageMatches", marked.matches.size).put("boxes", marked.hits.size)
                            .put("altoStatus", marked.status.name)
                        progress("Localizado ${example.canonical}: $path página $pos, texto=${report.getInt("textMatches")}, imagen=${marked.matches.size}, cajas=${marked.hits.size}")
                        found = true
                        break
                    } catch (failure: Exception) { errors.put(failure.message) }
                }
            } catch (failure: Exception) { errors.put(failure.message) }
        }
        report.put("found", found).put("pagesInspected", inspected).put("errors", errors)
        reports.put(report)
        if (!found) progress("${example.canonical}: sin localizar el corte en $inspected páginas inspeccionadas")
    }
    return reports.toString(2)
}
