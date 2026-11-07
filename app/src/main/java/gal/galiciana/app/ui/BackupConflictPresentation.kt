package gal.galiciana.app.ui

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
    val isNote = conflict.kind == "Nota de referencia"
    val isSearch = conflict.kind == "Búsqueda"
    fun summary(raw: String, fields: Map<*, *>?): String {
        if (isNote) return raw.ifBlank { "Nota de referencia eliminada." }
        if (fields != null) {
            val lines = mutableListOf<String>()
            if (isSearch) {
                fields.text("query")?.let { lines += "Consulta: $it" }
                if (fields["advanced"] == true) {
                    listOf("title" to "Publicación", "author" to "Autor", "place" to "Lugar").forEach { (key, label) ->
                        fields.text(key)?.let { lines += "$label: $it" }
                    }
                    val from = fields.text("yearFrom"); val to = fields.text("yearTo")
                    if (from != null || to != null) lines += "Periodo: ${from ?: "sin inicio"} – ${to ?: "sin final"}"
                    fields.text("section")?.let { lines += "Tema orientativo: $it" }
                }
                (fields["fullText"] as? Boolean)?.let { lines += if (it) "Buscar en texto completo" else "Buscar en el catálogo" }
            } else {
                fields.text("title")?.let { lines += it }
                fields.text("parentTitle")?.let { lines += "Publicación: $it" }
                fields.text("savedQuery")?.let { lines += "Búsqueda: $it" }
                (fields["snippets"] as? List<*>)?.filterIsInstance<String>()?.takeIf { it.isNotEmpty() }
                    ?.let { lines += "Extractos: ${it.joinToString(" · ")}" }
                (fields["values"] as? List<*>)?.let { lines += it.joinToString(" · ") }
            }
            if (lines.isNotEmpty()) return lines.joinToString("\n")
        }
        if (raw.trimStart().startsWith("{") || raw.trimStart().startsWith("["))
            return "Esta versión contiene datos del archivo. Puedes consultar su contenido completo."
        return raw.ifBlank { "Sin contenido en esta versión." }
    }
    val headline = when {
        isNote -> "Nota de referencia"
        isSearch -> "Búsqueda: ${local?.text("query") ?: incoming?.text("query") ?: "consulta con filtros"}"
        conflict.kind == "Contexto de página" -> "Datos de la página"
        conflict.kind == "Página archivada" -> "Carpetas de la página"
        conflict.kind == "Preferencia" -> "Preferencia de investigación"
        else -> conflict.kind
    }
    val context = listOfNotNull(title?.let { publicationIdentity(it).shortTitle }, page?.let { "Página $it" })
        .joinToString(" · ").takeIf { it.isNotBlank() }
    return BackupConflictPresentation(headline, context, summary(conflict.localValue, local), summary(conflict.incomingValue, incoming))
}
