package gal.galiciana.app.data
import gal.galiciana.app.i18n.tr

data class DossierEntry(val title: String, val path: String, val position: Int,
    val note: String, val query: String, val folders: List<String>)

object ResearchDossier {
    fun markdown(entries: List<DossierEntry>): String = buildString {
        append("# Galiciana de Peto · Dossier de investigación\n\n")
        append(tr("Referencias e Notas de referencia exportadas. Os documentos consúltanse en Galiciana; este ficheiro non inclúe imaxes.\n\n", "Referencias y Notas de referencia exportadas. Los documentos se consultan en Galiciana; este archivo no incluye imágenes.\n\n"))
        entries.forEachIndexed { index, entry ->
            require(entry.path.matches(Regex("\\d+")) && entry.position > 0) { tr("Referencia de páxina non válida", "Referencia de página no válida") }
            append("## ${index + 1}. ${entry.title.replace('\n', ' ')}\n\n")
            append(tr("Páxina ${entry.position}\n\n", "Página ${entry.position}\n\n"))
            append(tr("Fonte: ${GalicianaUrls.viewer(entry.path, entry.position)}\n\n", "Fuente: ${GalicianaUrls.viewer(entry.path, entry.position)}\n\n"))
            if (entry.folders.isNotEmpty()) append(tr("Cartafoles: ${entry.folders.joinToString(", ")}\n\n", "Carpetas: ${entry.folders.joinToString(", ")}\n\n"))
            if (entry.query.isNotBlank()) append("Consulta: ${entry.query}\n\n")
            if (entry.note.isNotBlank()) append("### Nota de referencia\n\n${entry.note}\n\n")
        }
        append(tr("---\n\nAntes de reutilizar imaxes ou documentos, consulta as condicións de dereitos indicadas en cada ficha orixinal.\n", "---\n\nAntes de reutilizar imágenes o documentos, consulta las condiciones de derechos indicadas en cada ficha original.\n"))
    }
}
