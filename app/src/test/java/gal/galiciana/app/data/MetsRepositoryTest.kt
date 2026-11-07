package gal.galiciana.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetsRepositoryTest {
    @Test fun pagesFollowPhysicalOrderAndOcrIsPerPage() {
        val xml = """
            <mets:mets xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/1999/xlink">
              <mets:fileSec><mets:fileGrp>
                <mets:file ID="i2"><mets:FLocat xlink:href="imagen_id.do?idImagen=22"/></mets:file>
                <mets:file ID="i1"><mets:FLocat xlink:href="imagen_id.do?idImagen=11"/></mets:file>
                <mets:file ID="o1"><mets:FLocat xlink:href="descargarAlto.do?posicion=1"/></mets:file>
              </mets:fileGrp></mets:fileSec>
              <mets:structMap><mets:div>
                <mets:div ORDER="2"><mets:fptr FILEID="i2"/></mets:div>
                <mets:div ORDER="1"><mets:fptr FILEID="i1"/><mets:fptr FILEID="o1"/></mets:div>
              </mets:div></mets:structMap>
            </mets:mets>
        """.trimIndent()
        val pages = MetsRepository().parse("123", xml).pages
        assertEquals(listOf("11", "22"), pages.map { it.imageId })
        assertEquals(listOf(1, 2), pages.map { it.position })
        assertTrue(pages[0].hasOcr)
        assertFalse(pages[1].hasOcr)
    }
}
