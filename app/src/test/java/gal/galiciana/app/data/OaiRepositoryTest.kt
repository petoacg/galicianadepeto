package gal.galiciana.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OaiRepositoryTest {
    private fun fixture() = javaClass.classLoader!!.getResourceAsStream("oai/list_records_dc.xml")!!
        .bufferedReader().use { it.readText() }

    private fun rejected(block: () -> Unit) {
        try { block(); fail("The invalid OAI response must be rejected") }
        catch (_: IllegalStateException) { }
    }

    @Test fun namespacedRecordsDecodeEntitiesKeepCreatorsAndParseUrlParameters() {
        val repo = OaiRepository()
        val result = repo.parseRecords(fixture()).single()
        assertEquals("oai:galiciana:615025", result.oaiId)
        assertEquals("Crónica & noticias de <Galicia>", result.title)
        assertEquals(listOf("Núñez, José & Compañía", "Vázquez, Ana"), result.creators)
        assertEquals("1975", result.date)
        assertEquals("Consulta & estudio", result.rights)
        assertEquals("615025", result.catalogId)
        assertEquals("1264535", result.path)
        assertTrue(result.recordUrl!!.contains("&presentacion=ficha"))
        assertTrue(result.viewerUrl!!.contains("&posicion=15"))
        assertFalse(result.viewerUrl.contains("&amp;"))
        assertNull(repo.nextToken) // Pure parsing cannot alter the active cursor.
    }

    @Test fun acceptedPageKeepsTheExactOpaqueResumptionToken() {
        val repo = OaiRepository()
        assertEquals(1, repo.acceptPage(fixture()).size)
        assertEquals("cursor+1&next=dos%2F3", repo.nextToken)
    }

    @Test fun unexpectedHtmlAndWrongProtocolCannotBecomeAnEmptySuccessfulPage() {
        val repo = OaiRepository()
        repo.acceptPage(fixture())
        for (response in listOf("<html><body>Servicio no disponible</body></html>",
            "", fixture().substringBefore("</o:OAI-PMH>"),
            fixture().replace("</o:header>", "</o:broken>"),
            "<OAI-PMH xmlns='urn:other'><ListRecords/></OAI-PMH>",
            "<OAI-PMH><Identify/></OAI-PMH>")) {
            rejected { repo.acceptPage(response) }
            assertEquals("cursor+1&next=dos%2F3", repo.nextToken)
        }
    }

    @Test fun noRecordsMatchIsAValidEmptyResultAndClosesPagination() {
        val repo = OaiRepository()
        repo.acceptPage(fixture())
        assertTrue(repo.acceptPage("""
            <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/">
            <error code="noRecordsMatch">No records match</error></OAI-PMH>
        """.trimIndent()).isEmpty())
        assertNull(repo.nextToken)
    }

    @Test fun protocolErrorsAndIncompleteRecordsPreserveThePreviousCursor() {
        val repo = OaiRepository()
        repo.acceptPage(fixture())
        for (response in listOf("<OAI-PMH><error code='badResumptionToken'>Expired</error></OAI-PMH>",
            "<OAI-PMH><error code='cannotDisseminateFormat'>Unknown format</error></OAI-PMH>",
            "<OAI-PMH><ListRecords><record><metadata/></record><resumptionToken>new</resumptionToken></ListRecords></OAI-PMH>")) {
            rejected { repo.acceptPage(response) }
            assertEquals("cursor+1&next=dos%2F3", repo.nextToken)
        }
    }

    @Test fun deletedRecordsNeedNoMetadataAndEmptyFinalTokenEndsPagination() {
        val repo = OaiRepository()
        repo.acceptPage(fixture())
        val response = """
            <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/"><ListRecords>
            <record><header status="deleted"><identifier>oai:deleted</identifier></header></record>
            <resumptionToken completeListSize="1"/>
            </ListRecords></OAI-PMH>
        """.trimIndent()
        assertTrue(repo.acceptPage(response).isEmpty())
        assertNull(repo.nextToken)
    }
}
