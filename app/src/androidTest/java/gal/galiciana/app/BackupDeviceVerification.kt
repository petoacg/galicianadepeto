package gal.galiciana.app

import android.content.Context
import android.content.ContextWrapper
import gal.galiciana.app.data.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** A real Android preferences round-trip; never reads or replaces user preferences. */
internal suspend fun verifyBackupStorage(target: Context): String {
    val root = File(target.cacheDir, "backup-verification-${System.nanoTime()}").apply { mkdirs() }
    val prefs = target.getSharedPreferences("codex_backup_probe", Context.MODE_PRIVATE)
    val isolated = object : ContextWrapper(target) {
        override fun getSharedPreferences(name: String?, mode: Int) = prefs
        override fun getFilesDir(): File = root
    }
    prefs.edit().clear().commit()
    try {
        val store = LocalStore(isolated)
        val favorite = gal.galiciana.app.domain.OcrHit(title = "El pueblo gallego · 1975", path = "1264535", imageId = "12588994", position = 15)
        check(store.toggleHit(favorite))
        store.rememberWork("42", "Investigación sin favorito")
        check(store.setNote("1264535", 15, "Nota de referencia local con ñ\nSegunda línea"))
        check(store.setNote("42", 3, "Nota sin página marcada"))
        store.addFolder("Familia Cárcamo")
        store.setFolders("1264535", 15, listOf("Familia Cárcamo"))
        store.markVisited("42|3"); store.rememberReading(favorite)
        val history = SearchHistory(isolated)
        val query = SearchPreset("Federación Gallega", title = "El pueblo", yearFrom = "1975", advanced = true)
        check(history.remember(query)); check(history.save(query)); history.saveDraft(query.copy(query = "Borrador no ejecutado"))
        prefs.edit().putBoolean("probe_boolean", true).putInt("probe_int", 7).putLong("probe_long", 9000000000L)
            .putFloat("probe_float", 1.25f).putStringSet("probe_set", setOf("ñ", "Galicia")).commit()
        val initial = prefs.all.toMap()
        val archive = ArchiveBackup(isolated)
        val bytes = ByteArrayOutputStream().also { archive.export(it) }.toByteArray()
        check(bytes.size < 100_000)
        check(store.setNote("1264535", 15, "Versión actual distinta"))
        store.addFolder("Nueva línea")
        val preview = archive.preview(ByteArrayInputStream(bytes))
        check(preview.counts.favorites == 1 && preview.counts.notes == 2)
        val merged = archive.importBackup(preview)
        check(store.noteOf("1264535", 15) == "Versión actual distinta")
        check(store.noteOf("42", 3) == "Nota sin página marcada")
        check(store.folders().containsAll(listOf("Familia Cárcamo", "Nueva línea")))
        check(merged.conflicts.isNotEmpty() && archive.conflicts().isNotEmpty())
        check(store.annotatedPages().any { it.path == "42" && it.position == 3 })
        check(!store.isFav("42", 3))
        val oldPreview = archive.preview(ByteArrayInputStream(bytes))
        prefs.edit().putString("changed_after_preview", "No borrar").commit()
        check(runCatching { archive.importBackup(oldPreview) }.isFailure)
        check(prefs.getString("changed_after_preview", "") == "No borrar")
        val replacement = archive.preview(ByteArrayInputStream(bytes))
        check(runCatching { archive.importBackup(replacement, ImportOptions(mode = ImportMode.REPLACE)) }.isFailure)
        archive.importBackup(replacement, ImportOptions(mode = ImportMode.REPLACE, confirmReplace = true))
        check(prefs.all == initial) { "La restauración no conserva todos los tipos y valores originales" }
        check(history.saved().single().criteriaKey() == query.criteriaKey())
        check(history.draft()?.query == "Borrador no ejecutado")
        check(store.annotatedPages().size == 2)
        var failNext = false
        val flaky = object : android.content.SharedPreferences by prefs {
            override fun edit(): android.content.SharedPreferences.Editor {
                val delegate = prefs.edit()
                return object : android.content.SharedPreferences.Editor by delegate {
                    override fun clear(): android.content.SharedPreferences.Editor { delegate.clear(); return this }
                    override fun commit(): Boolean {
                        val actual = delegate.commit()
                        return if (failNext) { failNext = false; false } else actual
                    }
                }
            }
        }
        val flakyContext = object : ContextWrapper(isolated) {
            override fun getSharedPreferences(name: String?, mode: Int) = flaky
            override fun getFilesDir(): File = root
        }
        val flakyArchive = ArchiveBackup(flakyContext)
        val beforeFailedImport = prefs.all.toMap()
        val failPreview = flakyArchive.preview(ByteArrayInputStream(bytes))
        failNext = true
        val failed = runCatching { flakyArchive.importBackup(failPreview) }.exceptionOrNull()
        check(failed is BackupImportException && failed.rollbackSucceeded)
        check(prefs.all == beforeFailedImport)
        val pending = File(root, "backup_recovery/pending.zip")
        pending.parentFile!!.mkdirs(); pending.writeBytes(bytes)
        prefs.edit().putString("interrupted_change", "Debe desaparecer al recuperar").commit()
        check(ArchiveBackup.recoverPendingImport(isolated))
        check(prefs.all == initial)
        pending.writeBytes(bytes)
        File(pending.parentFile, "committed").writeBytes(byteArrayOf(1))
        prefs.edit().putString("committed_change", "Debe conservarse").commit()
        check(ArchiveBackup.recoverPendingImport(isolated))
        check(prefs.getString("committed_change", "") == "Debe conservarse")
        return "Copia ZIP ligera ${bytes.size} bytes: favoritos, notas huérfanas, carpetas, búsquedas, borrador, preferencias tipadas; combinar conserva ambas versiones; vista previa obsoleta rechazada; reemplazo confirmado restaura exactamente; fallo de escritura revierte; recuperación tras interrupción y marcador de éxito comprobados. Datos reales intactos."
    } finally {
        prefs.edit().clear().commit()
        target.deleteSharedPreferences("codex_backup_probe")
        root.deleteRecursively()
    }
}
