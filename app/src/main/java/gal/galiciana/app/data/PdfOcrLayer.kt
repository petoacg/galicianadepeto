package gal.galiciana.app.data

import java.nio.charset.Charset
import java.util.Locale

/** Plain OCR text layer, fitted within the page. This is not ALTO-aligned word geometry. */
internal data class PdfOcrLine(val text: String, val x: Double, val baseline: Double, val fontSize: Double)

internal fun layoutPdfOcr(text: String, width: Int, height: Int): List<PdfOcrLine> {
    require(width > 0 && height > 0)
    require(text.length <= 2 * 1024 * 1024) { "El OCR es demasiado extenso para incluirlo completo en este PDF." }
    val lines = text.replace('\u0000', ' ').lineSequence().flatMap { it.chunked(120).asSequence() }
        .filter { it.isNotBlank() }.toList()
    require(lines.size <= 20000) { "El OCR contiene demasiadas líneas para incluirlo completo en este PDF." }
    if (lines.isEmpty()) return emptyList()
    val padding = minOf(24.0, width * 0.02, height * 0.02)
    val availableWidth = width - padding * 2
    val availableHeight = height - padding * 2
    val encoding = Charset.forName("windows-1252")
    val longest = lines.maxOf { it.toByteArray(encoding).size }.coerceAtLeast(1)
    // A conservative Helvetica advance bound keeps even wide glyphs inside the page.
    val font = minOf(10.0, availableWidth / (longest * 1.2), availableHeight / (lines.size * 1.4 + 0.25))
    val leading = font * 1.4
    return lines.mapIndexed { index, line -> PdfOcrLine(line, padding, height - padding - font - index * leading, font) }
}

internal fun pdfOcrCommands(text: String, width: Int, height: Int): String {
    val lines = layoutPdfOcr(text, width, height)
    if (lines.isEmpty()) return ""
    fun number(value: Double) = String.format(Locale.US, "%.6f", value)
    fun encoded(value: String): String = buildString {
        for (byte in value.toByteArray(Charset.forName("windows-1252"))) {
            val n = byte.toInt() and 255
            if (n in 32..126 && n != 40 && n != 41 && n != 92) append(n.toChar())
            else append(String.format(Locale.US, "\\%03o", n))
        }
    }
    return buildString {
        append("BT /F1 ${number(lines.first().fontSize)} Tf 3 Tr\n")
        lines.forEach { line ->
            append("1 0 0 1 ${number(line.x)} ${number(line.baseline)} Tm\n")
            append("(${encoded(line.text)}) Tj\n")
        }
        append("ET\n")
    }
}
