package gal.galiciana.app.ui

import gal.galiciana.app.i18n.tr
import gal.galiciana.app.data.BackupImportException

private val HttpStatus = Regex("""HTTP (\d{3})""")

/**
 * Explains a failure in the user's terms. Network and server errors never reach the
 * screen as library text ("Unable to resolve host…", "METS: HTTP 503"); messages the
 * app writes itself with error()/require() are already in Spanish and pass through.
 */
fun friendlyError(
    e: Throwable?,
    fallback: String = tr("Produciuse un erro inesperado. Téntao de novo.", "Se produjo un error inesperado. Vuelve a intentarlo."),
): String {
    val message = e?.message.orEmpty()
    HttpStatus.find(message)?.let { _ ->
        return tr("Galiciana non responde agora. Téntao de novo nuns minutos.", "Galiciana no responde ahora. Vuelve a intentarlo en unos minutos.")
    }
    return when (e) {
        is java.net.SocketTimeoutException -> tr("Galiciana tarda demasiado en responder. Téntao de novo.", "Galiciana tarda demasiado en responder. Vuelve a intentarlo.")
        is java.io.IOException -> tr("Non hai conexión con Galiciana. Comproba a rede e téntao de novo.", "No hay conexión con Galiciana. Comprueba la red y vuelve a intentarlo.")
        is BackupImportException -> message.ifBlank { fallback }
        is IllegalStateException, is IllegalArgumentException -> message.ifBlank { fallback }
        else -> fallback
    }
}
