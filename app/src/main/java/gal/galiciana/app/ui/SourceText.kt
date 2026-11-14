package gal.galiciana.app.ui

import gal.galiciana.app.i18n.Lang

/**
 * Catalogue titles often arrive cut by the source with a trailing " : ..." or "....".
 * Shown with a single ellipsis so it never doubles up with the app's own truncation.
 */
fun cleanSourceTitle(title: String): String =
    title.trim().replace(Regex("""\s*[:;,/]?\s*\.{3,}$"""), "…")

/**
 * A few fixed status phrases come from Galiciana in Galician. In the Spanish interface
 * they are shown in Spanish; every other catalogue value is shown as published.
 */
fun localizeSourcePhrase(value: String): String {
    if (Lang.current != Lang.ES) return value
    return when (value.trim().trimEnd('.').lowercase()) {
        "sen obxecto dixital asociado" -> "Sin objeto digital asociado"
        "sen dixitalizar" -> "Sin digitalizar"
        "dominio público" -> "Dominio público"
        else -> value
    }
}
