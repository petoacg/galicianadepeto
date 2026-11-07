package gal.galiciana.app.domain

data class Work(
    val oaiId: String,
    val catalogId: String?,
    val path: String?,
    val title: String,
    val creators: List<String>,
    val date: String?,
    val rights: String?,
    val recordUrl: String?,
    val viewerUrl: String?,
)

data class MetsPage(
    val position: Int,
    val imageId: String?,
    val label: String?,
    val hasOcr: Boolean,
)

data class MetsDocument(
    val path: String,
    val title: String?,
    val pages: List<MetsPage>,
)

data class PageContent(
    val path: String,
    val position: Int,
    val imageId: String?,
    val imageUrl: String?,
    val ocrText: String,
)

data class OcrHit(
    val title: String,
    val parentTitle: String? = null,
    val snippets: List<String> = emptyList(),
    val path: String?,
    val imageId: String?,
    val position: Int?,
    val savedQuery: String = "",
    val note: String = "",
) {
    val snippet: String get() = snippets.joinToString("\n\n")
    val thumbUrl: String?
        get() = imageId?.let { "https://biblioteca.galiciana.gal/es/media/object-miniature.do?id=$it" }
}

data class OcrWorkGroup(
    val title: String,
    val parentTitle: String?,
    val pages: List<OcrHit>,
) {
    val occurrenceCount: Int get() = pages.sumOf { it.snippets.size }
}
