package gal.galiciana.app

import gal.galiciana.app.data.SearchPreset
import gal.galiciana.app.ui.cleanSourceTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SourceTextTest {
    @Test fun sourceEllipsisBecomesOne() {
        assertEquals("\"Alemania por encima de todo\"…", cleanSourceTitle("\"Alemania por encima de todo\" : ..."))
        assertEquals("Lugo del dr…", cleanSourceTitle("Lugo del dr...."))
        assertEquals("El Pueblo Gallego", cleanSourceTitle("El Pueblo Gallego"))
    }

    @Test fun hiddenFiltersDoNotDuplicateRecentSearches() {
        val plain = SearchPreset("Porrino")
        val leftover = SearchPreset("porrino ", title = "Faro", advanced = false)
        assertEquals(plain.criteriaKey(), leftover.criteriaKey())
        assertNotEquals(plain.criteriaKey(), leftover.copy(advanced = true).criteriaKey())
    }
}
