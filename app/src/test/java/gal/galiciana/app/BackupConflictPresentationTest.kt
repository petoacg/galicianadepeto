package gal.galiciana.app

import gal.galiciana.app.data.BackupConflict
import gal.galiciana.app.ui.presentBackupConflict
import org.junit.Assert.*
import org.junit.Test

class BackupConflictPresentationTest {
    @Test fun noteIdentifiesPublicationAndPageAndKeepsBothTextsExact() {
        val conflict = BackupConflict("example", "1264535|15", "Nota de referencia", "Anotación local", "Otra anotación", 1L)
        val shown = presentBackupConflict(conflict, "El pueblo gallego : rotativo de la mañana")
        assertEquals("Nota de referencia", shown.title)
        assertEquals("El pueblo gallego · Página 15", shown.context)
        assertEquals(conflict.localValue, shown.localSummary)
        assertEquals(conflict.incomingValue, shown.incomingSummary)
    }
    @Test fun savedSearchShowsItsActualCriteriaRatherThanSerializedKeys() {
        val raw = """{"query":"Federación Gallega","advanced":true,"title":"El Progreso","yearFrom":"1975","yearTo":"1976","fullText":true}"""
        val conflict = BackupConflict("example", "internal-query-id", "Búsqueda", raw, raw.replace("1976", "1977"), 1L)
        val shown = presentBackupConflict(conflict)
        assertEquals("Búsqueda: Federación Gallega", shown.title)
        assertTrue(shown.localSummary.contains("Publicación: El Progreso"))
        assertTrue(shown.localSummary.contains("1975 – 1976"))
        assertTrue(shown.incomingSummary.contains("1975 – 1977"))
        assertFalse(shown.localSummary.contains("advanced"))
    }
    @Test fun unknownStructuredDataDoesNotPretendToBeAnUnderstoodRecord() {
        val conflict = BackupConflict("example", "technical_key", "Preferencia", "{\"unfamiliar\":7}", "{\"unfamiliar\":9}", 1L)
        val shown = presentBackupConflict(conflict)
        assertEquals("Preferencia de investigación", shown.title)
        assertNull(shown.context)
        assertTrue(shown.localSummary.contains("contenido completo"))
        assertEquals("{\"unfamiliar\":7}", conflict.localValue)
    }
    @Test fun deletedNoteHasAnExplicitHumanMeaning() {
        val conflict = BackupConflict("example", "1264535|15", "Nota de referencia", "", "Nota antigua", 1L)
        assertEquals("Nota de referencia eliminada.", presentBackupConflict(conflict).localSummary)
    }

}
