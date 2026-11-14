package gal.galiciana.app.ui

import gal.galiciana.app.i18n.tr
import gal.galiciana.app.data.BackupConflict
import gal.galiciana.app.data.BackupJson

internal data class BackupConflictPresentation(
    val title: String,
    val context: String?,
    val localSummary: String,
    val incomingSummary: String,
)

/** Interpret known archive records for people; the original payload remains available separately. */
internal fun presentBackupConflict(conflict: BackupConflict, knownTitle: String? = null): BackupConflictPresentation {
    fun record(raw: String): Map<*, *>? = runCatching { BackupJson.parse(raw) as? Map<*, *> }.getOrNull()
    fun Map<*, *>.text(key: String) = (get(key) as? String)?.takeIf { it.isNotBlank() }
    val local = record(conflict.localValue)
    val incoming = record(conflict.incomingValue)
    val title = local?.text("title") ?: incoming?.text("title") ?: knownTitle?.takeIf { it.isNotBlank() }
    val page = (local?.get("pos") as? Number)?.toInt() ?: (incoming?.get("pos") as? Number)?.toInt()
        ?: Regex("^\\d+\\|(\\d+)$").matchEntire(conflict.key)?.groupValues?.get(1)?.toIntOrNull()
    val isNote = conflict.kind == gal.galiciana.app.data.ConflictKind.NOTE
    val isSearch = conflict.kind == gal.galiciana.app.data.ConflictKind.SEARCH
    fun summary(raw: String, fields: Map<*, *>?): String {
        if (isNote) return raw.ifBlank { tr("Nota de referencia eliminada.", "Nota de referencia eliminada.") }
        if (fields != null) {
            val lines = mutableListOf<String>()
            if (isSearch) {
                fields.text("query")?.let { lines += tr("Consulta: $it", "Consulta: $it") }
                if (fields["advanced"] == true) {
                    listOf("title" to tr("Publicación", "Publicación"), "author" to tr("Autor", "Autor"), "place" to tr("Lugar", "Lugar")).forEach { (key, label) ->
                        fields.text(key)?.let { lines += "$label: $it" }
                    }
                    val from = fields.text("yearFrom"); val to = fields.text("yearTo")
                    if (from != null || to != null) lines += tr("Período: ${from ?: "sen inicio"} – ${to ?: "sen final"}", "Periodo: ${from ?: "sin inicio"} – ${to ?: "sin final"}")
                    fields.text("section")?.let { lines += "Tema orientativo: $it" }
                }
                (fields["fullText"] as? Boolean)?.let { lines += if (it) tr("Buscar no texto completo", "Buscar en texto completo") else tr("Buscar no catálogo", "Buscar en el catálogo") }
            } else {
                fields.text("title")?.let { lines += it }
                fields.text("parentTitle")?.let { lines += "Publicación: $it" }
                fields.text("savedQuery")?.let { lines += tr("Busca: $it", "Búsqueda: $it") }
                (fields["snippets"] as? List<*>)?.filterIsInstance<String>()?.takeIf { it.isNotEmpty() }
                    ?.let { lines += "Extractos: ${it.joinToString(" · ")}" }
                (fields["values"] as? List<*>)?.let { lines += it.joinToString(" · ") }
            }
            if (lines.isNotEmpty()) return lines.joinToString("\n")
        }
        if (raw.trimStart().startsWith("{") || raw.trimStart().startsWith("["))
            return tr("Esta versión contén datos do arquivo. Podes consultar o seu contido completo.", "Esta versión contiene datos del archivo. Puedes consultar su contenido completo.")
        return raw.ifBlank { tr("Sen contido nesta versión.", "Sin contenido en esta versión.") }
    }
    val headline = when {
        isNote -> "Nota de referencia"
        isSearch -> tr("Busca: ${local?.text("query") ?: incoming?.text("query") ?: "consulta con filtros"}", "Búsqueda: ${local?.text("query") ?: incoming?.text("query") ?: "consulta con filtros"}")
        conflict.kind == gal.galiciana.app.data.ConflictKind.CONTEXT -> tr("Datos da páxina", "Datos de la página")
        conflict.kind == gal.galiciana.app.data.ConflictKind.PAGE_FOLDERS -> tr("Cartafoles da páxina", "Carpetas de la página")
        conflict.kind == gal.galiciana.app.data.ConflictKind.PREFERENCE -> tr("Preferencia de investigación", "Preferencia de investigación")
        conflict.kind == gal.galiciana.app.data.ConflictKind.NOTES_FILE -> tr("Ficheiro de notas non interpretable", "Archivo de notas no interpretable")
        else -> conflict.kind
    }
    val context = listOfNotNull(title?.let { publicationIdentity(it).shortTitle }, page?.let { tr("Páxina $it", "Página $it") })
        .joinToString(" · ").takeIf { it.isNotBlank() }
    return BackupConflictPresentation(headline, context, summary(conflict.localValue, local), summary(conflict.incomingValue, incoming))
}
