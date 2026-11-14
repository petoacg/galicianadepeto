package gal.galiciana.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every visible text in the interface goes through tr("galego", "español"). A literal
 * outside tr() is accepted only when it is written the same in both languages and is
 * listed in i18n_same_in_both.txt. A new text added without its Galician version fails here.
 */
class TranslationCoverageTest {
    private val visible = Regex("""(?:Text\(|contentDescription = |label = \{ Text\(|placeholder = )"((?:[^"\\]|\\.)*)"""")

    @Test fun everyVisibleTextHasBothLanguages() {
        val allowed = File("src/test/resources/i18n_same_in_both.txt").readLines().filter { it.isNotBlank() }.toSet()
        val missing = File("src/main/java/gal/galiciana/app/ui").walk().filter { it.extension == "kt" }.flatMap { file ->
            visible.findAll(file.readText()).map { it.groupValues[1] }
                .filter { Regex("[A-Za-zÁÉÍÓÚáéíóúñÑ]{2}").containsMatchIn(it.replace(Regex("""\$\{.*?\}|\$\w+"""), "")) }
                .filterNot { it in allowed }
                .map { "${file.name}: \"$it\"" }
        }.toList()
        assertTrue("Textos visibles sen versión galega (usa tr):\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
