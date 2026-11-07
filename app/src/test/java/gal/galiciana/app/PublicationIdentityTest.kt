package gal.galiciana.app

import gal.galiciana.app.ui.publicationIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicationIdentityTest {
    @Test fun newspaperKeepsOriginalAndExtractsOnlySuppliedIdentity() {
        val original = "El pueblo gallego : rotativo de la mañana: Ano 51 Número 18760 - 1975 xullo 1"
        val identity = publicationIdentity(original)
        assertEquals(original, identity.fullTitle)
        assertEquals("El pueblo gallego", identity.shortTitle)
        assertEquals("1975 xullo 1", identity.date)
        assertEquals("Número 18760", identity.issue)
    }

    @Test fun issueSuffixAndNumericDateLeavePublicationReadable() {
        val identity = publicationIdentity("El Progreso: Año LXVIII Número 21127 - 1975 julio 13")
        assertEquals("El Progreso", identity.shortTitle)
        assertEquals("1975 julio 13", identity.date)
        assertEquals("Número 21127", identity.issue)
        assertEquals("20/03/1917", publicationIdentity("El correo de Galicia: Num. 4902 (20/03/1917)").date)
    }

    @Test fun descriptiveBookSubtitleIsNotDiscardedAndMissingDataIsNotInvented() {
        val book = "Galicia: mapas y territorios"
        val identity = publicationIdentity(book)
        assertEquals(book, identity.shortTitle)
        assertNull(identity.date)
        assertNull(identity.issue)
    }

    @Test fun fullHeadingKeepsItsArticleEvenIfParentWasNormalizedForGrouping() {
        assertEquals("El pueblo gallego", publicationIdentity("El pueblo gallego : rotativo de la mañana: Ano 50 Número 1 - 1974 xullo 11", "pueblo gallego").shortTitle)
    }

    @Test fun knownParentNamesPublicationWithoutLosingFullIssueTitle() {
        val identity = publicationIdentity("Número 42 - 1917-03-20", "El correo de Galicia")
        assertEquals("El correo de Galicia", identity.shortTitle)
        assertEquals("1917-03-20", identity.date)
        assertEquals("Número 42 - 1917-03-20", identity.fullTitle)
    }
}
