package gal.galiciana.app.data

import org.junit.Assert.*
import org.junit.Test

class BackupMergeTest {
    private fun pref(value: Any?) = BackupPreference.fromRaw(BackupJson.encode(value))
    private fun saved(path: String, note: String = "") = mapOf("path" to path, "pos" to 1L, "title" to "Obra $path", "note" to note, "snips" to "extracto $path")
    private fun parsed(preferences: Map<String, BackupPreference>, key: String) = BackupJson.parse(preferences.getValue(key).values.single())

    @Test fun defaultMergeKeepsBothFavoriteListsNotesOrphansFoldersHistoryAndTypedPreferences() {
        val current = mapOf("fav_json" to pref(listOf(saved("1", "local"))), "notes_json" to pref(mapOf("1|1" to "local", "orphan|2" to "huérfana")),
            "fav_folders" to pref(listOf("Familia")), "fav_folder_map" to pref(mapOf("orphan|2" to "Familia")),
            "search_saved_json" to pref(listOf(mapOf("id" to "a", "query" to "Galicia"))), "seen" to BackupPreference.fromRaw(setOf("1|1")))
        val incoming = mapOf("fav_json" to pref(listOf(saved("1", "entrante"), saved("2", "segunda"))),
            "notes_json" to pref(mapOf("1|1" to "entrante", "2|1" to "segunda", "other-orphan|3" to "otra huérfana")),
            "fav_folders" to pref(listOf("Prensa")), "fav_folder_map" to pref(mapOf("orphan|2" to listOf("Prensa"))),
            "search_saved_json" to pref(listOf(mapOf("id" to "b", "query" to "Orense"))), "seen" to BackupPreference.fromRaw(setOf("2|1")))
        val result = BackupMerge.merge(current, incoming, 1000)
        assertEquals(2, (parsed(result.preferences, "fav_json") as List<*>).size)
        val notes = parsed(result.preferences, "notes_json") as Map<*, *>
        assertEquals("local", notes["1|1"]); assertEquals("segunda", notes["2|1"])
        assertEquals("huérfana", notes["orphan|2"]); assertEquals("otra huérfana", notes["other-orphan|3"])
        assertEquals(listOf("Familia", "Prensa"), parsed(result.preferences, "fav_folders"))
        assertEquals(listOf("Familia", "Prensa"), (parsed(result.preferences, "fav_folder_map") as Map<*, *>)["orphan|2"])
        assertEquals(2, (parsed(result.preferences, "search_saved_json") as List<*>).size)
        assertEquals(setOf("1|1", "2|1"), result.preferences.getValue("seen").rawValue())
        val conflict = BackupMerge.conflicts(result.preferences).single { it.kind == "Nota de referencia" }
        assertEquals("local", conflict.localValue); assertEquals("entrante", conflict.incomingValue)
        assertEquals(4, BackupMerge.counts(result.preferences).notes)
    }
    @Test fun explicitIncomingNoteResolutionUpdatesBothRepresentations() {
        val local = mapOf("fav_json" to pref(listOf(saved("1", "old"))), "notes_json" to pref(mapOf("1|1" to "old")))
        val other = mapOf("fav_json" to pref(listOf(saved("1", "new"))), "notes_json" to pref(mapOf("1|1" to "new")))
        val preview = BackupMerge.merge(local, other, 1)
        val conflict = preview.conflicts.single()
        val result = BackupMerge.merge(local, other, 1, mapOf(conflict.id to BackupResolution.TAKE_INCOMING))
        assertEquals("new", (parsed(result.preferences, "notes_json") as Map<*, *>)["1|1"])
        assertEquals("new", ((parsed(result.preferences, "fav_json") as List<*>).single() as Map<*, *>)["note"])
        assertTrue(BackupMerge.conflicts(result.preferences).isEmpty())
    }
    @Test fun draftAndUnknownConfigConflictsStayLocalAndKeepIncomingConsultable() {
        val local = mapOf("search_draft_json" to pref(mapOf("query" to "Cárcamo")), "enabled" to BackupPreference.fromRaw(true))
        val other = mapOf("search_draft_json" to pref(mapOf("query" to "Núñez")), "enabled" to BackupPreference.fromRaw(false))
        val result = BackupMerge.merge(local, other, 1)
        assertEquals(local.getValue("search_draft_json"), result.preferences.getValue("search_draft_json"))
        assertEquals(true, result.preferences.getValue("enabled").rawValue())
        assertEquals(2, BackupMerge.conflicts(result.preferences).size)
    }
    @Test fun aMalformedIncomingNoteMapIsPreservedAsAConflictNotSilentlyDiscarded() {
        val local = mapOf("notes_json" to pref(mapOf("1|1" to "conservar")))
        val other = mapOf("notes_json" to BackupPreference.fromRaw("malformed legacy bytes"))
        val result = BackupMerge.merge(local, other, 1)
        assertEquals("conservar", (parsed(result.preferences, "notes_json") as Map<*, *>)["1|1"])
        assertTrue(BackupMerge.conflicts(result.preferences).any { it.incomingValue == "malformed legacy bytes" })
    }
    @Test fun duplicatePageKeepsBothResearchContextsInsteadOfDroppingIncomingSnippets() {
        val local = mapOf("fav_json" to pref(listOf(saved("1") + ("q" to "Cárcamo"))))
        val otherRecord = saved("1") + mapOf("q" to "Núñez", "snips" to "Otra pista de investigación")
        val other = mapOf("fav_json" to pref(listOf(otherRecord)))
        val preview = BackupMerge.merge(local, other, 1)
        val kept = (parsed(preview.preferences, "fav_json") as List<*>).single() as Map<*, *>
        assertEquals("Cárcamo", kept["q"])
        val conflict = BackupMerge.conflicts(preview.preferences).single { it.kind == "Contexto de página" }
        assertTrue(conflict.incomingValue.contains("Otra pista de investigación"))
        assertTrue(conflict.incomingValue.contains("Núñez"))
        val resolved = BackupMerge.merge(local, other, 1, mapOf(conflict.id to BackupResolution.TAKE_INCOMING))
        assertEquals("Núñez", ((parsed(resolved.preferences, "fav_json") as List<*>).single() as Map<*, *>)["q"])
        assertTrue(resolved.conflicts.isEmpty())
    }

    @Test fun explicitDeletedNoteStaysDeletedWhenCombiningAnOlderBackup() {
        val local = mapOf("fav_json" to pref(listOf(saved("1", ""))), "notes_json" to pref(mapOf("1|1" to "")))
        val older = mapOf("fav_json" to pref(listOf(saved("1", "nota antigua"))), "notes_json" to pref(mapOf("1|1" to "nota antigua")))
        val result = BackupMerge.merge(local, older, 1)
        assertEquals("", (parsed(result.preferences, "notes_json") as Map<*, *>)["1|1"])
        assertEquals("", ((parsed(result.preferences, "fav_json") as List<*>).single() as Map<*, *>)["note"])
        assertEquals(0, BackupMerge.counts(result.preferences).notes)
        assertTrue(BackupMerge.conflicts(result.preferences).any { it.kind == "Nota de referencia" && it.incomingValue == "nota antigua" })
        assertEquals(0, BackupMerge.counts(local + ("fav_json" to pref(listOf(saved("1", "copia desactualizada"))))).notes)
    }

}
