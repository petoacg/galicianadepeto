package gal.galiciana.app

import gal.galiciana.app.data.PageFormat
import gal.galiciana.app.data.downloadLocation
import org.junit.Assert.*
import org.junit.Test

class DownloadLocationTest {
    @Test fun legacyDestinationNeverClaimsPublicStorage() {
        for (sdk in listOf(26, 27, 28)) for (format in PageFormat.entries) {
            val label = downloadLocation(format, sdk)
            assertTrue(label.contains("privada"))
            assertTrue(label.contains("desinstalar"))
            assertFalse(label.contains("Descargas/"))
            assertFalse(label.contains("Fotos/"))
        }
        assertEquals("Descargas/Galiciana", downloadLocation(PageFormat.PDF, 29))
        assertEquals("Fotos/Galiciana", downloadLocation(PageFormat.JPEG, 35))
    }
}
