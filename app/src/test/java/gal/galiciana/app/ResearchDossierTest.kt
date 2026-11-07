package gal.galiciana.app

import gal.galiciana.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ResearchDossierTest {
    @Test fun preservesResearchTextAndStablePageReferences() {
        val note = "Cárcamo: ‘familia’\nSegunda línea con comillas \"y\" guiones."
        val result = ResearchDossier.markdown(listOf(DossierEntry("El pueblo gallego · 1975", "1264535", 15,
            note, "Federación Gallega", listOf("Familia", "Prensa"))))
        assertTrue(result.contains(note)); assertTrue(result.contains("path=1264535"))
        assertTrue(result.contains("posicion=15")); assertTrue(result.contains("Carpetas: Familia, Prensa"))
        assertTrue(result.contains("Nota de referencia")); assertTrue(result.contains("Consulta: Federación Gallega"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonDocumentReferences() {
        ResearchDossier.markdown(listOf(DossierEntry("Título", "../file", 1, "", "", emptyList())))
    }
}
