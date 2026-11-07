package gal.galiciana.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

enum class ImportMode { MERGE, REPLACE }
enum class BackupResolution { KEEP_BOTH, KEEP_LOCAL, TAKE_INCOMING }
data class ImportOptions(
    val mode: ImportMode = ImportMode.MERGE,
    val resolutions: Map<String, BackupResolution> = emptyMap(),
    val confirmReplace: Boolean = false,
)
data class BackupCounts(val preferences: Int, val favorites: Int, val notes: Int, val folders: Int, val searches: Int)
data class BackupConflict(val id: String, val key: String, val kind: String,
    val localValue: String, val incomingValue: String, val createdAt: Long)
data class BackupReport(val counts: BackupCounts, val createdAt: Long, val formatVersion: Int = BackupFormat.VERSION)
class BackupPreview internal constructor(
    internal val payload: BackupPayload,
    val prefsFingerprint: String,
    val counts: BackupCounts,
    val currentCounts: BackupCounts,
    val conflicts: List<BackupConflict>,
) {
    val createdAt: Long get() = payload.createdAt
    val formatVersion: Int get() = BackupFormat.VERSION
}
data class ImportReport(val counts: BackupCounts, val conflicts: List<BackupConflict>, val replaced: Boolean)
class BackupImportException(message: String, val rollbackSucceeded: Boolean, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/** Preferences-only backup. Stream ownership is transferred to export/preview. */
class ArchiveBackup(private val context: Context) {
    private val prefs: SharedPreferences get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun export(output: OutputStream): BackupReport = withContext(Dispatchers.IO) {
        synchronized(lock) {
            check(recoverPendingImport(context)) { "Hay una recuperación pendiente; no se puede crear otra copia todavía." }
            val payload = BackupPayload(snapshot(prefs), System.currentTimeMillis())
            BackupFormat.write(output, payload)
            BackupReport(BackupMerge.counts(payload.preferences), payload.createdAt)
        }
    }
    suspend fun preview(input: InputStream): BackupPreview = withContext(Dispatchers.IO) {
        val payload = BackupFormat.read(input)
        synchronized(lock) {
            check(recoverPendingImport(context)) { "No se pudo recuperar una importación anterior." }
            val current = snapshot(prefs)
            val merge = BackupMerge.merge(current, payload.preferences, payload.createdAt)
            BackupPreview(payload, BackupFormat.fingerprint(current), BackupMerge.counts(payload.preferences),
                BackupMerge.counts(current), merge.conflicts)
        }
    }
    suspend fun importBackup(preview: BackupPreview, options: ImportOptions = ImportOptions()): ImportReport =
        withContext(Dispatchers.IO + NonCancellable) {
            synchronized(lock) {
                check(recoverPendingImport(context)) { "No se pudo recuperar una importación anterior." }
                val current = snapshot(prefs)
                check(BackupFormat.fingerprint(current) == preview.prefsFingerprint) {
                    "Los datos cambiaron desde la vista previa. Reabre la copia antes de importarla."
                }
                require(options.mode != ImportMode.REPLACE || options.confirmReplace) { "Reemplazar requiere confirmación explícita." }
                val merged = if (options.mode == ImportMode.REPLACE) BackupMerge.Result(preview.payload.preferences, emptyList())
                    else BackupMerge.merge(current, preview.payload.preferences, preview.createdAt, options.resolutions)
                val journal = journal(context)
                check(!journal.exists()) { "Ya existe una recuperación pendiente." }
                val temporary = File(journal.parentFile, "pending.tmp")
                val committed = File(journal.parentFile, "committed")
                val committedTemporary = File(journal.parentFile, "committed.tmp")
                check(journal.parentFile!!.mkdirs() || journal.parentFile!!.isDirectory)
                try {
                    FileOutputStream(temporary).use { file ->
                        // ZipOutputStream closes its stream; flush+fsync before that close.
                        val durable = object : java.io.FilterOutputStream(file) {
                            override fun close() { flush(); file.fd.sync() }
                        }
                        BackupFormat.write(durable, BackupPayload(current, System.currentTimeMillis()))
                    }
                    check(temporary.renameTo(journal)) { "No se pudo preparar la recuperación de seguridad." }
                    if (!replace(prefs, merged.preferences)) {
                        val restored = replace(prefs, current)
                        if (restored) journal.delete()
                        throw BackupImportException(if (restored) "No se pudo importar. Se han restaurado los datos anteriores."
                            else "No se pudo importar ni completar la recuperación. Conservamos una copia interna para reintentar al abrir la app.", restored)
                    }
                    // Publish the marker atomically; a partial marker must never certify a commit.
                    FileOutputStream(committedTemporary).use { it.write(byteArrayOf(1)); it.fd.sync() }
                    check(committedTemporary.renameTo(committed)) { "No se pudo confirmar el cierre de la importación." }
                    // If cleanup fails, retain the marker so startup preserves this successful import.
                    if (journal.delete()) committed.delete()
                    ImportReport(BackupMerge.counts(merged.preferences), merged.conflicts, options.mode == ImportMode.REPLACE)
                } catch (e: BackupImportException) { throw e }
                catch (e: Exception) {
                    committed.delete()
                    val restored = if (journal.exists()) replace(prefs, current) else true
                    if (restored) journal.delete()
                    throw BackupImportException(if (restored) "No se pudo completar la importación; se han conservado los datos anteriores."
                        else "La recuperación sigue pendiente. Conservamos una copia interna de tus datos.", restored, e)
                } finally { temporary.delete(); committedTemporary.delete() }
            }
        }

    fun conflicts(): List<BackupConflict> = BackupMerge.conflicts(snapshot(prefs))

    companion object {
        private const val PREFS = "galiciana_fans"
        private val lock = Any()
        private fun journal(context: Context) = File(context.filesDir, "backup_recovery/pending.zip")
        private fun snapshot(prefs: SharedPreferences): Map<String, BackupPreference> = prefs.all.mapValues { (_, value) ->
            BackupPreference.fromRaw(requireNotNull(value))
        }
        private fun replace(prefs: SharedPreferences, values: Map<String, BackupPreference>): Boolean {
            val raw = values.mapValues { it.value.rawValue() }
            val editor = prefs.edit().clear()
            raw.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.map { it as String }.toSet())
            } }
            return editor.commit()
        }
        /** Call before creating the UI/LocalStore. No work occurs unless a journal exists. */
        fun recoverPendingImport(context: Context): Boolean = synchronized(lock) {
            val pending = journal(context)
            val committed = File(pending.parentFile, "committed")
            if (committed.exists()) {
                val removed = !pending.exists() || pending.delete()
                if (removed) committed.delete()
                return@synchronized removed
            }
            if (!pending.exists()) return@synchronized true
            try {
                val previous = pending.inputStream().use { BackupFormat.read(it) }
                if (!replace(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE), previous.preferences)) false
                else pending.delete()
            } catch (_: Exception) { false }
        }
    }
}
