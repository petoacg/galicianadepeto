package gal.galiciana.app.data

data class DossierEntry(val title: String, val path: String, val position: Int,
    val note: String, val query: String, val folders: List<String>)

object ResearchDossier {
    fun markdown(entries: List<DossierEntry>): String = buildString {
        append("# Galiciana de Peto · Dossier de investigación\n\n")
        append("Referencias y Notas de referencia exportadas. Los documentos se consultan en Galiciana; este archivo no incluye imágenes.\n\n")
        entries.forEachIndexed { index, entry ->
            require(entry.path.matches(Regex("\\d+")) && entry.position > 0) { "Referencia de página no válida" }
            append("## ${index + 1}. ${entry.title.replace('\n', ' ')}\n\n")
            append("Página ${entry.position}\n\n")
            append("Fuente: ${GalicianaUrls.viewer(entry.path, entry.position)}\n\n")
            if (entry.folders.isNotEmpty()) append("Carpetas: ${entry.folders.joinToString(", ")}\n\n")
            if (entry.query.isNotBlank()) append("Consulta: ${entry.query}\n\n")
            if (entry.note.isNotBlank()) append("### Nota de referencia\n\n${entry.note}\n\n")
        }
        append("---\n\nAntes de reutilizar imágenes o documentos, consulta las condiciones de derechos indicadas en cada ficha original.\n")
    }
}
