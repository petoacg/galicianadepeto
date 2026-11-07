package gal.galiciana.app.data

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupFormatTest {
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { output -> entries.forEach { (name, value) -> output.putNextEntry(ZipEntry(name)); output.write(value); output.closeEntry() } }
    }.toByteArray()
    private fun bytes(payload: BackupPayload) = ByteArrayOutputStream().also { BackupFormat.write(it, payload) }.toByteArray()
    private fun payload() = BackupPayload(mapOf("note" to BackupPreference.fromRaw("Núñez & Galicia\n\"Nota\" 📖"),
        "integer" to BackupPreference.fromRaw(4), "long" to BackupPreference.fromRaw(1234567890123L),
        "boolean" to BackupPreference.fromRaw(true), "float" to BackupPreference.fromRaw(1.25f),
        "seen" to BackupPreference.fromRaw(setOf("a|1", "b|2"))), 1234)

    @Test fun versionedZipRoundTripsEveryAndroidPreferenceTypeAndUnicode() {
        val source = payload()
        val result = BackupFormat.read(ByteArrayInputStream(bytes(source)))
        assertEquals(source, result)
        source.preferences.forEach { (key, value) -> assertEquals(value.rawValue(), result.preferences.getValue(key).rawValue()) }
        ZipInputStream(ByteArrayInputStream(bytes(source))).use { zip ->
            assertEquals("manifest.json", zip.nextEntry.name)
            zip.readBytes(); zip.closeEntry(); assertNull(zip.nextEntry)
        }
    }
    @Test fun checksumRejectsChangesToThePreferenceValues() {
        val valid = bytes(payload())
        val manifest = ZipInputStream(ByteArrayInputStream(valid)).use { it.nextEntry; it.readBytes().toString(Charsets.UTF_8) }
        val corrupted = manifest.replace("Núñez", "Otro")
        assertThrows(IllegalArgumentException::class.java) { BackupFormat.read(ByteArrayInputStream(zip("manifest.json" to corrupted.toByteArray()))) }
    }
    @Test fun pathsAdditionalFilesAndUnrecognizedArchivesAreRejectedWithoutExtraction() {
        for (name in listOf("../manifest.json", "/manifest.json", "folder/manifest.json", "C:\\manifest.json"))
            assertThrows(IllegalArgumentException::class.java) { BackupFormat.read(ByteArrayInputStream(zip(name to byteArrayOf(1)))) }
        assertThrows(IllegalArgumentException::class.java) {
            BackupFormat.read(ByteArrayInputStream(zip("manifest.json" to "{}".toByteArray(), "image.jpg" to byteArrayOf(1))))
        }
        assertThrows(IllegalStateException::class.java) { BackupFormat.read(ByteArrayInputStream("not a zip".toByteArray())) }
    }
    @Test fun compressedOversizePayloadAndDeepOrDuplicateJsonAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupFormat.read(ByteArrayInputStream(zip("manifest.json" to ByteArray(BackupFormat.MAX_BYTES + 1) { ' '.code.toByte() })))
        }
        assertThrows(IllegalArgumentException::class.java) { BackupJson.parse("[".repeat(40) + "0" + "]".repeat(40)) }
        assertThrows(IllegalArgumentException::class.java) { BackupJson.parse("{\"id\":1,\"id\":2}") }
    }
    @Test fun unsupportedVersionsAndPreferenceTypeNamesAreNeverSilentlyAccepted() {
        val manifest = ZipInputStream(ByteArrayInputStream(bytes(payload()))).use { it.nextEntry; it.readBytes().toString(Charsets.UTF_8) }
        assertThrows(IllegalArgumentException::class.java) {
            BackupFormat.read(ByteArrayInputStream(zip("manifest.json" to manifest.replace("\"version\":1", "\"version\":99").toByteArray())))
        }
        val altered = BackupJson.parse(manifest) as Map<*, *>
        val unsigned = altered.filterKeys { it != "sha256" }.toMutableMap()
        @Suppress("UNCHECKED_CAST")
        val preferences = (unsigned["preferences"] as Map<String, Any?>).toMutableMap()
        preferences["new"] = mapOf("type" to "UNKNOWN", "values" to listOf("x")); unsigned["preferences"] = preferences
        val withHash = unsigned + ("sha256" to BackupFormat.sha256(BackupJson.encode(unsigned).toByteArray()))
        assertThrows(IllegalArgumentException::class.java) {
            BackupFormat.read(ByteArrayInputStream(zip("manifest.json" to BackupJson.encode(withHash).toByteArray())))
        }
    }
}
