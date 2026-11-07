package gal.galiciana.app.ui.colecciones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogueMetadataTest {
    @Test fun tableRowsKeepLabelsAndMultilineValuesWithoutCombiningUnrelatedFields() {
        val fields = parseCatalogueMetadata("""
            <div class="ficha"><table>
            <tr><th>Título:</th><td>Mapa de Galicia</td></tr>
            <tr><th>Notas</th><td>Primera nota<br>Segunda nota</td><td>Otra parte</td></tr>
            <tr><th>Derechos</th><td>CC BY 4.0</td></tr>
            <tr><td>Encabezado sin valor</td></tr>
            </table></div>
        """.trimIndent())
        assertEquals(listOf("Título", "Notas", "Derechos"), fields.map { it.label })
        assertEquals("Mapa de Galicia", fields[0].value)
        assertTrue(fields[1].value.contains("Primera nota\nSegunda nota"))
        assertTrue(fields[1].value.endsWith("Otra parte"))
        assertEquals("CC BY 4.0", fields.last().value)
    }

    @Test fun definitionListsPreserveEveryValueAndEntityFromTheSource() {
        val fields = parseCatalogueMetadata("""
            <div id="panel_ficha_dc"><dl>
            <dt>Autor:</dt><dd>María &amp; Juan</dd><dd>Otro autor</dd>
            <dt>Dereitos:</dt><dd>Consultar a institución</dd>
            </dl></div>
        """.trimIndent())
        assertEquals(2, fields.size)
        assertEquals(CatalogueField("Autor", "María & Juan\nOtro autor"), fields.first())
        assertEquals("Consultar a institución", fields.last().value)
    }

    @Test fun realMapRecordKeepsSeparateFieldsAndEveryNote() {
        val html = javaClass.getResource("/catalogue/map_record.html")!!.readText()
        val fields = parseCatalogueMetadata(html)
        assertTrue(fields.any { it.label == "Autor" && it.value.contains("Tofiño") })
        assertTrue(fields.any { it.label.contains("Descripción física") && it.value.contains("38 x 54") })
        assertTrue(fields.any { it.label == "Derechos" && it.value.contains("dominio público") })
        val notes = fields.first { it.label == "Notas" }.value
        assertTrue(notes.contains("2009\n"))
        assertTrue(notes.contains("Cada parte orientada"))
        assertTrue(!notes.contains("2009Cada"))
    }

    @Test(expected = IllegalStateException::class) fun portalBlockPageIsAnErrorRatherThanZeroWorks() {
        CollectionRepository().parse("<html><head><script>function redireccionar(){}</script></head><body class='block'></body></html>",
            "https://biblioteca.galiciana.gal/es/consulta/resultados_busqueda.do?busq_secbib=CARTOGRAF")
    }

    @Test fun unstructuredRecordDoesNotInventFieldBoundaries() {
        assertTrue(parseCatalogueMetadata("<div class='ficha'>Sin etiquetas: un texto original</div>").isEmpty())
    }
}
