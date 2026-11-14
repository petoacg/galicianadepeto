package gal.galiciana.app.i18n

import android.content.Context

/** Interface languages. Galician is the app's default; Spanish is selectable in Acerca de. */
enum class Lang(val code: String, val label: String) {
    GL("gl", "Galego"),
    ES("es", "Español");

    companion object {
        private const val PREFS = "ui_language"
        private const val KEY = "lang"

        /**
         * Language used by [tr]. It stays Spanish until [load] runs, so plain unit tests
         * (which never load preferences) keep reading the original Spanish strings.
         */
        @Volatile var current: Lang = ES
            private set

        fun load(context: Context) {
            val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            current = entries.firstOrNull { it.code == saved } ?: GL
        }

        /**
         * Saves the choice and rebuilds the screen in the new language. The activity is
         * recreated, not the process, so nothing being saved can be cut off.
         */
        fun choose(context: Context, lang: Lang) {
            if (lang == current) return
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang.code).commit()
            current = lang
            var c: Context? = context
            while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
            (c as? android.app.Activity)?.recreate()
        }
    }
}

/** Picks the text for the current interface language. */
fun tr(gl: String, es: String): String = if (Lang.current == Lang.GL) gl else es

/**
 * "Archivador general" is the stored name of the default folder and must not change in
 * saved data or backups; only its label is translated.
 */
fun folderLabel(name: String): String =
    if (name.equals("Archivador general", ignoreCase = true)) tr("Arquivador xeral", "Archivador general") else name
