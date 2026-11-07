package gal.galiciana.app.ui

import gal.galiciana.app.domain.OcrHit

/** Reading labels derived only from the supplied catalogue text. The original is never replaced. */
data class PublicationIdentity(
    val fullTitle: String,
    val shortTitle: String,
    val date: String?,
    val issue: String?,
)

fun publicationIdentity(hit: OcrHit): PublicationIdentity = publicationIdentity(hit.title, hit.parentTitle)

fun publicationIdentity(title: String, parentTitle: String? = null): PublicationIdentity {
    val full = title.trim()
    val months = "enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre|xaneiro|febreiro|maio|xuño|xullo|setembro|outubro|novembro|decembro"
    val date = Regex("\\b\\d{4}\\s+(?:$months)\\s+\\d{1,2}\\b", RegexOption.IGNORE_CASE).find(full)?.value
        ?: Regex("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b").find(full)?.value
    val issue = Regex("(?:Número|N[uú]m\\.?|N[º°])\\s*\\d+(?:[-/]\\d+)?", RegexOption.IGNORE_CASE).find(full)?.value
    val descriptor = Regex("\\s*:\\s*(?:rotativo|diario|semanario|periódico|revista|boletín)\\b.*", RegexOption.IGNORE_CASE)
    val issueSuffix = Regex("\\s*[:·-]?\\s*(?:A[nñ]o\\s+[IVXLCDM\\d]+|N[uú]mero\\s+\\d|N[uú]m\\.?\\s+\\d).*$", RegexOption.IGNORE_CASE)
    fun shorten(value: String) = value.replace(descriptor, "").replace(issueSuffix, "").trim().trimEnd(':', '·', '-').trim()
    val short = shorten(full).takeUnless { it.isBlank() || it.matches(Regex("(?i)(?:p[áa]gina|documento)\\s*\\d*")) }
        ?: parentTitle?.trim()?.let(::shorten)?.takeIf { it.isNotBlank() } ?: full
    return PublicationIdentity(full, short, date, issue)
}
