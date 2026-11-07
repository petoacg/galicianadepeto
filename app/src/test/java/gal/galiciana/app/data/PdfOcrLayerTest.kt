package gal.galiciana.app.data

import org.junit.Assert.*
import org.junit.Test

class PdfOcrLayerTest {
    @Test fun everyOcrLineIncludingThoseAfter2500StaysInsideThePdfPage() {
        val text = (1..3005).joinToString("\n") { "Línea $it de investigación" }
        val lines = layoutPdfOcr(text, 600, 800)
        assertEquals(3005, lines.size)
        assertEquals("Línea 1 de investigación", lines.first().text)
        assertEquals("Línea 3005 de investigación", lines.last().text)
        lines.forEach {
            assertTrue(it.fontSize > 0)
            assertTrue(it.x >= 0)
            assertTrue(it.baseline - it.fontSize * 0.25 >= 0)
            assertTrue(it.baseline + it.fontSize <= 800)
            assertTrue(it.x + it.text.length * it.fontSize * 1.2 <= 600)
        }
        assertTrue(lines.zipWithNext().all { (a, b) -> a.baseline > b.baseline })
    }

    @Test fun narrowAndShortPagesStillFitWideTextWithoutNegativePositions() {
        val text = "W".repeat(240) + "\nÚltima línea"
        val lines = layoutPdfOcr(text, 40, 30)
        assertEquals(3, lines.size)
        lines.forEach {
            assertTrue(it.baseline >= 0 && it.baseline + it.fontSize <= 30)
            assertTrue(it.x + it.text.length * it.fontSize * 1.2 <= 40)
        }
    }

    @Test fun pdfCommandsUseAbsoluteInPageMatricesAndEscapeLatinText() {
        val commands = pdfOcrCommands("Núñez (nota)\\archivo\nSegunda línea", 240, 320)
        assertTrue(commands.contains("3 Tr"))
        assertEquals(2, Regex(" Tm\\n").findAll(commands).count())
        assertFalse(commands.contains("T*"))
        assertTrue(commands.contains("\\361"))
        assertTrue(commands.contains("\\050nota\\051"))
        assertTrue(commands.contains("\\134archivo"))
        assertTrue(commands.contains("Segunda l"))
    }

    @Test fun emptyOcrDoesNotCreateTextAndExcessiveOcrFailsExplicitlyInsteadOfTruncating() {
        assertEquals("", pdfOcrCommands("\n \n", 240, 320))
        assertThrows(IllegalArgumentException::class.java) { layoutPdfOcr("x\n".repeat(20001), 240, 320) }
    }
}
