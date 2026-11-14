package gal.galiciana.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FriendlyErrorTest {
    @Test
    fun networkFailuresNeverShowLibraryText() {
        val offline = friendlyError(java.net.UnknownHostException("Unable to resolve host \"biblioteca.galiciana.gal\""))
        assertFalse(offline.contains("Unable"))
        assertEquals("No hay conexión con Galiciana. Comprueba la red y vuelve a intentarlo.", offline)
        assertEquals("Galiciana tarda demasiado en responder. Vuelve a intentarlo.",
            friendlyError(java.net.SocketTimeoutException("timeout")))
    }

    @Test
    fun serverStatusIsExplainedWithoutItsCode() {
        assertEquals("Galiciana no responde ahora. Vuelve a intentarlo en unos minutos.",
            friendlyError(IllegalStateException("METS: HTTP 503")))
    }

    @Test
    fun appMessagesPassThroughAndUnknownErrorsUseFallback() {
        assertEquals("No se pudo crear el PDF", friendlyError(IllegalStateException("No se pudo crear el PDF")))
        assertEquals("Tus datos se conservan.", friendlyError(org.json.JSONException("Unterminated object"), "Tus datos se conservan."))
        assertEquals("Tus datos se conservan.", friendlyError(null, "Tus datos se conservan."))
    }
}
