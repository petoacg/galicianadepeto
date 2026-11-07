package gal.galiciana.app.data

import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** The backup deliberately contains preferences only: no scans, OCR, documents or caches. */
enum class BackupPreferenceType { STRING, BOOLEAN, INT, LONG, FLOAT, STRING_SET }
data class BackupPreference(val type: BackupPreferenceType, val values: List<String>) {
    fun rawValue(): Any {
        require(type == BackupPreferenceType.STRING_SET || values.size == 1) { "Valor de preferencia inválido" }
        return when (type) {
            BackupPreferenceType.STRING -> values.single()
            BackupPreferenceType.BOOLEAN -> values.single().let { require(it == "true" || it == "false"); it.toBoolean() }
            BackupPreferenceType.INT -> values.single().toInt()
            BackupPreferenceType.LONG -> values.single().toLong()
            BackupPreferenceType.FLOAT -> values.single().toFloat()
            BackupPreferenceType.STRING_SET -> values.toSet().also { require(it.size == values.size) }
        }
    }
    companion object {
        fun fromRaw(value: Any): BackupPreference = when (value) {
            is String -> BackupPreference(BackupPreferenceType.STRING, listOf(value))
            is Boolean -> BackupPreference(BackupPreferenceType.BOOLEAN, listOf(value.toString()))
            is Int -> BackupPreference(BackupPreferenceType.INT, listOf(value.toString()))
            is Long -> BackupPreference(BackupPreferenceType.LONG, listOf(value.toString()))
            is Float -> BackupPreference(BackupPreferenceType.FLOAT, listOf(value.toString()))
            is Set<*> -> BackupPreference(BackupPreferenceType.STRING_SET, value.map { require(it is String); it }.sorted())
            else -> error("Tipo de preferencia no compatible")
        }
    }
}
data class BackupPayload(val preferences: Map<String, BackupPreference>, val createdAt: Long)

object BackupFormat {
    const val VERSION = 1
    const val MAX_BYTES = 8 * 1024 * 1024
    const val MAX_PREFERENCES = 20000
    private const val ENTRY = "manifest.json"
    fun fingerprint(preferences: Map<String, BackupPreference>): String = sha256(BackupJson.encode(preferencesJson(preferences)).toByteArray(Charsets.UTF_8))
    internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun preferencesJson(preferences: Map<String, BackupPreference>): Map<String, Any> = preferences.mapValues { (_, value) ->
        value.rawValue()
        mapOf("type" to value.type.name, "values" to value.values)
    }
    private fun data(payload: BackupPayload): Map<String, Any> = mapOf(
        "format" to "galiciana-peto-preferences", "version" to VERSION.toLong(), "createdAt" to payload.createdAt,
        "preferences" to preferencesJson(payload.preferences))
    fun write(output: OutputStream, payload: BackupPayload) {
        require(payload.preferences.size <= MAX_PREFERENCES)
        val data = data(payload)
        val checksum = sha256(BackupJson.encode(data).toByteArray(Charsets.UTF_8))
        val bytes = BackupJson.encode(data + ("sha256" to checksum)).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "La copia supera el límite de 8 MiB" }
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY)); zip.write(bytes); zip.closeEntry()
        }
    }
    fun read(input: InputStream): BackupPayload {
        val bytes = ZipInputStream(input).use { zip ->
            val entry = zip.nextEntry ?: error("El archivo no contiene una copia válida")
            require(!entry.isDirectory && entry.name == ENTRY) { "La copia contiene rutas o archivos no permitidos" }
            require(entry.size <= MAX_BYTES || entry.size == -1L) { "La copia supera el tamaño permitido" }
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = zip.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BYTES) { "La copia supera el tamaño permitido" }
                output.write(buffer, 0, count)
            }
            zip.closeEntry()
            require(zip.nextEntry == null) { "La copia contiene archivos adicionales o duplicados" }
            output.toByteArray()
        }
        val root = BackupJson.parse(bytes.toString(Charsets.UTF_8)) as? Map<*, *> ?: error("Manifiesto inválido")
        require(root.keys == setOf("format", "version", "createdAt", "preferences", "sha256")) { "Estructura de copia no compatible" }
        require(root["format"] == "galiciana-peto-preferences" && root["version"] == VERSION.toLong()) { "Versión de copia no compatible" }
        val created = root["createdAt"] as? Long ?: error("Fecha de copia inválida")
        require(created >= 0)
        val checksum = root["sha256"] as? String ?: error("Falta la suma de comprobación")
        require(checksum.matches(Regex("[0-9a-f]{64}")))
        val unsigned = root.filterKeys { it != "sha256" }
        require(sha256(BackupJson.encode(unsigned).toByteArray(Charsets.UTF_8)) == checksum) { "La copia está dañada: no coincide su SHA-256" }
        val entries = root["preferences"] as? Map<*, *> ?: error("Preferencias inválidas")
        require(entries.size <= MAX_PREFERENCES)
        val preferences = entries.map { (key, raw) ->
            require(key is String && key.length <= 4096)
            val item = raw as? Map<*, *> ?: error("Preferencia inválida")
            require(item.keys == setOf("type", "values"))
            val type = BackupPreferenceType.valueOf(item["type"] as? String ?: error("Tipo de preferencia inválido"))
            val values = (item["values"] as? List<*>)?.map { require(it is String); it } ?: error("Valor de preferencia inválido")
            key to BackupPreference(type, values).also { it.rawValue() }
        }.toMap()
        return BackupPayload(preferences, created)
    }
}

/** Small strict JSON reader/writer shared by the manifest and preference merges. */
internal object BackupJson {
    fun encode(value: Any?): String = when (value) {
        null -> "null"
        is String -> buildString {
            append('"'); value.forEach { char -> when (char) {
                '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
                else -> if (char.code < 32) append("\\u%04x".format(char.code)) else append(char)
            } }; append('"')
        }
        is Boolean, is Number -> value.toString()
        is Map<*, *> -> value.entries.sortedBy { it.key as String }.joinToString(",", "{", "}") { encode(it.key as String) + ":" + encode(it.value) }
        is List<*> -> value.joinToString(",", "[", "]") { encode(it) }
        else -> error("Valor JSON no compatible")
    }
    fun parse(source: String): Any? {
        require(source.length <= BackupFormat.MAX_BYTES)
        var at = 0
        fun whitespace() { while (at < source.length && source[at] in " \n\r\t") at++ }
        fun string(): String {
            require(at < source.length && source[at++] == '"')
            val result = StringBuilder()
            while (at < source.length) {
                val char = source[at++]
                if (char == '"') return result.toString()
                require(char.code >= 32)
                if (char != '\\') result.append(char)
                else {
                    require(at < source.length)
                    when (val escape = source[at++]) {
                        '"', '\\', '/' -> result.append(escape)
                        'b' -> result.append('\b'); 'f' -> result.append('\u000C')
                        'n' -> result.append('\n'); 'r' -> result.append('\r'); 't' -> result.append('\t')
                        'u' -> { require(at + 4 <= source.length); result.append(source.substring(at, at + 4).toInt(16).toChar()); at += 4 }
                        else -> error("Escape JSON inválido")
                    }
                }
            }
            error("Cadena JSON incompleta")
        }
        fun value(depth: Int): Any? {
            require(depth <= 32) { "JSON demasiado anidado" }; whitespace(); require(at < source.length)
            return when (source[at]) {
                '"' -> string()
                '{' -> {
                    at++; whitespace(); val map = linkedMapOf<String, Any?>()
                    if (at < source.length && source[at] == '}') { at++; map }
                    else {
                        while (true) {
                            whitespace(); val key = string(); require(!map.containsKey(key)) { "Clave JSON duplicada" }
                            whitespace(); require(at < source.length && source[at++] == ':'); map[key] = value(depth + 1)
                            whitespace(); require(at < source.length)
                            val end = source[at++]; if (end == '}') break; require(end == ',')
                        }; map
                    }
                }
                '[' -> {
                    at++; whitespace(); val list = mutableListOf<Any?>()
                    if (at < source.length && source[at] == ']') { at++; list }
                    else {
                        while (true) {
                            list += value(depth + 1); whitespace(); require(at < source.length)
                            val end = source[at++]; if (end == ']') break; require(end == ',')
                        }; list
                    }
                }
                't' -> { require(source.startsWith("true", at)); at += 4; true }
                'f' -> { require(source.startsWith("false", at)); at += 5; false }
                'n' -> { require(source.startsWith("null", at)); at += 4; null }
                else -> {
                    val start = at
                    while (at < source.length && source[at] in "0123456789-+.eE") at++
                    val number = source.substring(start, at)
                    require(number.matches(Regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?"))) { "Número JSON inválido" }
                    number.toLongOrNull() ?: number.toDouble().also { require(it.isFinite()) }
                }
            }
        }
        val result = value(0); whitespace(); require(at == source.length) { "Contenido adicional tras JSON" }; return result
    }
}
