package gal.galiciana.app.data

import android.content.Context
import gal.galiciana.app.domain.OcrHit
import org.json.JSONArray
import org.json.JSONObject

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("galiciana_fans", Context.MODE_PRIVATE)

    private fun validFavorites(raw: String?): JSONArray? = raw?.let {
        runCatching {
            JSONArray(it).also { array ->
                // Do not silently discard malformed entries and later save a shortened list.
                for (i in 0 until array.length()) array.getJSONObject(i)
            }
        }.getOrNull()
    }

    private fun readFavoriteArray(): JSONArray? {
        val raw = prefs.getString("fav_json", "[]")
        validFavorites(raw)?.let { return it }
        val backup = prefs.getString("fav_json_previous", null)
        val recovered = validFavorites(backup) ?: return null
        // Preserve the damaged bytes for diagnosis; never overwrite the good backup.
        val restored = prefs.edit().putString("fav_json_unreadable", raw)
            .putString("fav_json", backup).putBoolean("fav_recovered", true).commit()
        return if (restored) recovered else null
    }

    fun favoritesStorageNotice(): String? = when {
        readFavoriteArray() == null -> "No se puede leer el archivo de favoritos. Se han conservado los datos y bloqueado los cambios para evitar pérdidas."
        prefs.getBoolean("fav_recovered", false) -> "Se recuperó la última copia legible de favoritos tras un error de lectura. Puede faltar el cambio más reciente."
        else -> null
    }

    fun favoritesBrowse(): JSONObject = runCatching {
        JSONObject(prefs.getString("favorites_browse", "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    fun saveFavoritesBrowse(state: JSONObject) {
        prefs.edit().putString("favorites_browse", state.toString()).apply()
    }

    fun rememberReading(hit: OcrHit) {
        if (hit.path.isNullOrBlank()) return
        prefs.edit().putString("last_reading", JSONObject().apply {
            put("path", hit.path)
            put("pos", hit.position ?: 1)
            put("title", hit.title)
            put("imageId", hit.imageId.orEmpty())
            put("q", hit.savedQuery)
        }.toString()).apply()
    }

    fun lastReading(): OcrHit? = runCatching {
        val raw = prefs.getString("last_reading", null) ?: return@runCatching null
        val o = JSONObject(raw)
        val path = o.optString("path").takeIf { it.isNotBlank() } ?: return@runCatching null
        OcrHit(title = o.optString("title").ifBlank { workTitle(path) ?: "Documento" },
            path = path, position = o.optInt("pos", 1).coerceAtLeast(1),
            imageId = o.optString("imageId").ifBlank { null }, savedQuery = o.optString("q"))
    }.getOrNull()

    // Preserve the actual result opened by the user, before the viewer loads OCR.
    fun rememberOpenedHit(hit: OcrHit) {
        prefs.edit().putString("opened_hit", JSONObject().apply {
            put("path", hit.path)
            put("pos", hit.position ?: 1)
            put("title", hit.title)
            put("parent", hit.parentTitle.orEmpty())
            put("imageId", hit.imageId.orEmpty())
            put("q", hit.savedQuery)
            put("snips", JSONArray(hit.snippets))
        }.toString()).apply()
    }

    private fun openedHitFor(hit: OcrHit): OcrHit? = runCatching {
        val o = JSONObject(prefs.getString("opened_hit", "{}") ?: "{}")
        if (o.optString("path") != hit.path || o.optInt("pos") != (hit.position ?: 1) ||
            o.optString("q") != hit.savedQuery) return@runCatching null
        val snips = o.optJSONArray("snips") ?: JSONArray()
        hit.copy(title = o.optString("title"), parentTitle = o.optString("parent").ifBlank { null },
            imageId = o.optString("imageId").ifBlank { hit.imageId },
            snippets = (0 until snips.length()).map { snips.optString(it) }.filter { it.isNotBlank() })
    }.getOrNull()

    fun favHits(): List<OcrHit> {
        val arr = readFavoriteArray() ?: return emptyList()
        return runCatching {
            val notes = notesMap()
            (0 until arr.length()).mapNotNull { i -> runCatching {
                val o = arr.getJSONObject(i)
                val path = o.optString("path").ifBlank { null }
                val pos = o.optInt("pos", 1)
                val titleRaw = o.optString("title")
                OcrHit(
                    title = titleRaw.takeIf { !weakTitle(it) } ?: path?.let { workTitle(it) } ?: titleRaw,
                    parentTitle = o.optString("parent").ifBlank { null } ?: path?.let { workParent(it) },
                    snippets = o.optString("snips").split("\n\n").filter { it.isNotBlank() },
                    path = path,
                    imageId = o.optString("imageId").ifBlank { null },
                    position = pos,
                    savedQuery = o.optString("q"),
                    note = path?.let { notes[noteKey(it, pos)] } ?: o.optString("note"),
                )
            }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    fun isFav(path: String?, pos: Int?): Boolean {
        val p = path ?: return false
        return favHits().any { it.path == p && it.position == (pos ?: 1) }
    }

    fun fillMissingSnippet(path: String, position: Int, excerpt: String): Boolean {
        if (readFavoriteArray() == null) return false
        if (excerpt.isBlank()) return false
        val list = favHits().toMutableList()
        val index = list.indexOfFirst { it.path == path && it.position == position }
        if (index < 0 || list[index].snippets.isNotEmpty()) return false
        list[index] = list[index].copy(snippets = listOf(excerpt.take(400)))
        return writeFavs(list)
    }

    private fun weakTitle(t: String) =
        t.isBlank() || t.equals("Documento", true) || t.startsWith("Pág") || t.startsWith("Página") || t == "Página"

    fun toggleHit(hit: OcrHit, useOpenedResult: Boolean = false): Boolean {
        if (readFavoriteArray() == null) return false
        val list = favHits().toMutableList()
        val idx = list.indexOfFirst { it.path == hit.path && it.position == (hit.position ?: 1) }
        val key = "${hit.path}|${hit.position ?: 1}"
        val on = if (idx >= 0) {
            stashRemoved(list[idx])
            list.removeAt(idx)
            false
        } else {
            val kept = stashOf(key)
            val original = if (useOpenedResult) openedHitFor(hit)?.takeIf { it.snippets.isNotEmpty() } ?: hit else hit
            val title = original.title.takeIf { !weakTitle(it) }
                ?: kept?.title?.takeIf { !weakTitle(it) }
                ?: workTitle(hit.path.orEmpty())
                ?: hit.title
            val snippets = original.snippets.filter { it.isNotBlank() }.ifEmpty {
                kept?.takeIf { hit.savedQuery.isBlank() || it.savedQuery == hit.savedQuery }?.snippets.orEmpty()
            }
            val cleaned = hit.copy(
                title = title,
                position = hit.position ?: 1,
                imageId = original.imageId ?: kept?.imageId,
                parentTitle = original.parentTitle ?: kept?.parentTitle ?: workParent(hit.path.orEmpty()),
                snippets = snippets,
                savedQuery = hit.savedQuery.ifBlank { kept?.savedQuery.orEmpty() },
                note = notesMap()[key] ?: hit.note.ifBlank { kept?.note.orEmpty() },
            )
            list.add(0, cleaned)
            true
        }
        // Callers display the persisted selection state, including failed removals.
        return if (writeFavs(list)) on else idx >= 0
    }

    private fun stashKey(path: String?, pos: Int?) = "${path}|${pos ?: 1}"

    private fun stashRemoved(hit: OcrHit) {
        val o = JSONObject(prefs.getString("fav_stash", "{}") ?: "{}")
        o.put(stashKey(hit.path, hit.position), JSONObject().apply {
            put("title", hit.title)
            put("parent", hit.parentTitle ?: "")
            put("snips", hit.snippets.joinToString("\n\n"))
            put("q", hit.savedQuery)
            put("note", hit.note)
            put("imageId", hit.imageId ?: "")
        })
        prefs.edit().putString("fav_stash", o.toString()).apply()
    }

    private fun stashOf(key: String): OcrHit? {
        val o = JSONObject(prefs.getString("fav_stash", "{}") ?: "{}").optJSONObject(key) ?: return null
        return OcrHit(
            title = o.optString("title"),
            parentTitle = o.optString("parent").ifBlank { null },
            snippets = o.optString("snips").split("\n\n").filter { it.isNotBlank() },
            path = key.substringBefore("|").ifBlank { null },
            imageId = o.optString("imageId").ifBlank { null },
            position = key.substringAfter("|").toIntOrNull() ?: 1,
            savedQuery = o.optString("q"),
            note = o.optString("note"),
        )
    }

    private fun rawFavMap(): Map<String, JSONObject> {
        val arr = readFavoriteArray() ?: return emptyMap()
        val out = mutableMapOf<String, JSONObject>()
        runCatching {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out["${o.optString("path")}|${o.optInt("pos", 1)}"] = o
            }
        }
        return out
    }

    private fun serializeFavorites(list: List<OcrHit>): String {
        val prev = rawFavMap()
        val arr = JSONArray()
        list.forEach { h ->
            val old = prev["${h.path}|${h.position ?: 1}"]
            val title = old?.optString("title")?.takeIf { !weakTitle(it) }
                ?: h.title.takeIf { !weakTitle(it) }
                ?: h.title
            val parent = h.parentTitle?.takeIf { it.isNotBlank() && !weakTitle(it) }
                ?: old?.optString("parent")?.takeIf { it.isNotBlank() }
                ?: ""
            val snips = old?.optString("snips").orEmpty().ifBlank { h.snippets.joinToString("\n\n") }
            val q = old?.optString("q").orEmpty().ifBlank { h.savedQuery }
            val note = h.note
            val imageId = h.imageId?.ifBlank { null } ?: old?.optString("imageId")?.ifBlank { null }
            arr.put(JSONObject().apply {
                put("title", title)
                put("parent", parent)
                put("snips", snips)
                put("path", h.path ?: "")
                put("imageId", imageId ?: "")
                put("pos", h.position ?: 1)
                put("q", q)
                put("note", note)
            })
        }
        return arr.toString()
    }

    private fun writeFavs(list: List<OcrHit>): Boolean {
        if (readFavoriteArray() == null) return false
        val serialized = serializeFavorites(list)
        val previous = prefs.getString("fav_json", "[]") ?: "[]"
        return prefs.edit().putString("fav_json_previous", previous).putString("fav_json", serialized).commit()
    }

    private fun noteKey(path: String, pos: Int?) = "$path|${pos ?: 1}"

    private fun notesMap(): MutableMap<String, String> {
        val raw = prefs.getString("notes_json", "{}") ?: "{}"
        val out = mutableMapOf<String, String>()
        runCatching {
            val o = JSONObject(raw)
            o.keys().forEach { k -> out[k] = o.optString(k) }
        }
        return out
    }

    fun setNote(path: String?, pos: Int?, text: String): Boolean {
        val p = path ?: return false
        // Preserve the existing recovery/blocking rules before preparing a write.
        if (readFavoriteArray() == null) return false
        val list = favHits().toMutableList()
        val map = notesMap()
        val key = noteKey(p, pos)
        val note = text.take(4000).takeUnless { it.isBlank() }.orEmpty()
        // An empty canonical value records explicit deletion, even if a stale UI
        // hit or a removed-page context still carries an earlier note.
        map[key] = note
        val notes = JSONObject()
        map.forEach { (k, v) -> notes.put(k, v) }
        val index = list.indexOfFirst { it.path == p && it.position == (pos ?: 1) }
        // A desmarked page can still have a stored research context. Update its
        // note too, otherwise re-adding it would resurrect an edited/deleted note.
        val stash = runCatching { JSONObject(prefs.getString("fav_stash", "{}") ?: "{}") }.getOrNull()
            ?: return false
        stash.optJSONObject(key)?.put("note", note)
        val editor = prefs.edit().putString("notes_json", notes.toString())
            .putString("fav_stash", stash.toString())
        if (index >= 0) {
            list[index] = list[index].copy(note = note)
            // Both representations and the legacy backup change in one transaction.
            val serialized = serializeFavorites(list)
            val previous = prefs.getString("fav_json", "[]") ?: "[]"
            editor.putString("fav_json_previous", previous).putString("fav_json", serialized)
        }
        return editor.commit()
    }

    fun noteOf(path: String?, pos: Int?): String {
        val p = path ?: return ""
        notesMap()[noteKey(p, pos)]?.let { return it }
        return favHits().firstOrNull { it.path == p && it.position == (pos ?: 1) }?.note.orEmpty()
    }

    /** Notes remain research data even after their page is unmarked. */
    fun annotatedPages(): List<OcrHit> {
        val favorites = favHits().associateBy { "${it.path}|${it.position ?: 1}" }
        val annotated = favorites.values.filter { it.note.isNotBlank() }.associateBy { "${it.path}|${it.position ?: 1}" }.toMutableMap()
        notesMap().filterValues { it.isNotBlank() }.forEach { (key, note) ->
            val path = key.substringBefore('|')
            val pos = key.substringAfter('|', "1").toIntOrNull() ?: return@forEach
            if (!path.matches(Regex("\\d+")) || pos < 1) return@forEach
            val original = favorites[key] ?: stashOf(key)
            annotated[key] = (original ?: OcrHit(title = workTitle(path) ?: "Documento $path", path = path,
                imageId = null, position = pos, parentTitle = workParent(path))).copy(note = note)
        }
        return annotated.values.toList()
    }

    fun removeHits(keys: Set<String>) {
        writeFavs(favHits().filterNot { "${it.path}|${it.position}" in keys })
    }

    fun visited(): Set<String> = prefs.getStringSet("seen", emptySet()) ?: emptySet()

    fun markVisited(key: String) {
        prefs.edit().putStringSet("seen", visited().toMutableSet().apply { add(key) }).apply()
    }

    fun rememberWork(path: String, title: String, parent: String? = null) {
        if (path.isBlank() || weakTitle(title)) return
        prefs.edit()
            .putString("work_$path", title)
            .putString("workp_$path", parent.orEmpty())
            .apply()
    }

    fun workTitle(path: String): String? =
        prefs.getString("work_$path", null)?.takeIf { !weakTitle(it) }

    fun workParent(path: String): String? =
        prefs.getString("workp_$path", null)?.takeIf { it.isNotBlank() && !weakTitle(it) }

    fun folders(): List<String> {
        val raw = prefs.getString("fav_folders", "[]") ?: "[]"
        return runCatching {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun addFolder(name: String) {
        val n = name.trim()
        if (n.isBlank()) return
        val list = folders().toMutableList()
        if (list.none { it.equals(n, true) }) list += n
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString("fav_folders", arr.toString()).apply()
    }

    fun removeFolder(name: String) {
        val n = name.trim()
        if (n.isBlank() || n.equals("Archivador general", true)) return
        val list = folders().filterNot { it.equals(n, true) }
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        val raw = prefs.getString("fav_folder_map", "{}") ?: "{}"
        val o = org.json.JSONObject(raw)
        o.keys().asSequence().toList().forEach { key ->
            val keep = parseFolderList(o.optString(key)).filterNot { it.equals(n, true) }
            if (keep.isEmpty()) o.remove(key)
            else {
                val a = org.json.JSONArray()
                keep.forEach { a.put(it) }
                o.put(key, a)
            }
        }
        prefs.edit()
            .putString("fav_folders", arr.toString())
            .putString("fav_folder_map", o.toString())
            .apply()
    }

    private fun parseFolderList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        if (raw.startsWith("[")) {
            return runCatching {
                val arr = org.json.JSONArray(raw)
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
        }
        return listOf(raw)
    }

    fun foldersOf(path: String?, pos: Int?): List<String> {
        val p = path ?: return emptyList()
        val raw = prefs.getString("fav_folder_map", "{}") ?: "{}"
        return runCatching {
            val o = org.json.JSONObject(raw)
            parseFolderList(o.optString("$p|${pos ?: 1}"))
        }.getOrDefault(emptyList())
    }

    fun folderOf(path: String?, pos: Int?): String = foldersOf(path, pos).firstOrNull().orEmpty()

    fun setFolders(path: String?, pos: Int?, names: List<String>) {
        val p = path ?: return
        val raw = prefs.getString("fav_folder_map", "{}") ?: "{}"
        val o = org.json.JSONObject(raw)
        val key = "$p|${pos ?: 1}"
        val clean = names.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (clean.isEmpty()) o.remove(key)
        else {
            val arr = org.json.JSONArray()
            clean.forEach { arr.put(it) }
            o.put(key, arr)
        }
        prefs.edit().putString("fav_folder_map", o.toString()).apply()
    }

    fun setFolder(path: String?, pos: Int?, name: String) {
        setFolders(path, pos, if (name.isBlank()) emptyList() else listOf(name))
    }

    fun hitsInFolder(name: String): List<OcrHit> {
        if (name.isBlank()) return favHits().filter { foldersOf(it.path, it.position).isEmpty() }
        return favHits().filter { foldersOf(it.path, it.position).any { f -> f.equals(name, true) } }
    }
}

fun favKey(path: String, pos: Int, title: String, imageId: String? = null) =
    "$path|$pos|$title|${imageId.orEmpty()}"
