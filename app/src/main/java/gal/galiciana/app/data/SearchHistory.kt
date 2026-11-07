package gal.galiciana.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SearchPreset(
    val query: String, val title: String = "", val author: String = "", val place: String = "",
    val yearFrom: String = "", val yearTo: String = "", val section: String = "",
    val advanced: Boolean = false, val fullText: Boolean = true,
    val id: String = UUID.randomUUID().toString(), val createdAt: Long = System.currentTimeMillis(),
) {
    fun json(): JSONObject = JSONObject().put("id", id).put("createdAt", createdAt)
        .put("query", query).put("title", title).put("author", author).put("place", place)
        .put("yearFrom", yearFrom).put("yearTo", yearTo).put("section", section)
        .put("advanced", advanced).put("fullText", fullText)
    fun criteriaKey(): String = listOf(query, title, author, place, yearFrom, yearTo, section, advanced, fullText).joinToString("\u0000")
    fun label(): String = query.ifBlank { title.ifBlank { author.ifBlank { place.ifBlank { "Consulta del catálogo" } } } }
    fun summary(): String = buildList {
        add(if (fullText) "Texto completo" else "Catálogo")
        if (advanced) {
            if (title.isNotBlank()) add("Título: $title")
            if (author.isNotBlank()) add("Autor: $author")
            if (place.isNotBlank()) add("Lugar: $place")
            if (yearFrom.isNotBlank() || yearTo.isNotBlank()) add("${yearFrom.ifBlank { "…" }}–${yearTo.ifBlank { "…" }}")
            if (section.isNotBlank()) add("Tema: $section")
        }
    }.joinToString(" · ")
    companion object {
        fun fromJson(o: JSONObject): SearchPreset = SearchPreset(o.getString("query"), o.optString("title"),
            o.optString("author"), o.optString("place"), o.optString("yearFrom"), o.optString("yearTo"),
            o.optString("section"), o.optBoolean("advanced"), o.optBoolean("fullText", true),
            o.optString("id").ifBlank { UUID.randomUUID().toString() }, o.optLong("createdAt"))
    }
}

class SearchHistory(context: Context) {
    private val prefs = context.getSharedPreferences("galiciana_fans", Context.MODE_PRIVATE)
    private fun read(key: String): List<SearchPreset> = runCatching {
        val a = JSONArray(prefs.getString(key, "[]"))
        (0 until a.length()).map { SearchPreset.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())
    private fun write(key: String, values: List<SearchPreset>): Boolean = prefs.edit()
        .putString(key, JSONArray().apply { values.forEach { put(it.json()) } }.toString()).commit()
    fun draft(): SearchPreset? = runCatching {
        SearchPreset.fromJson(JSONObject(prefs.getString("search_draft_json", "") ?: ""))
    }.getOrNull()
    fun saveDraft(preset: SearchPreset) { prefs.edit().putString("search_draft_json", preset.json().toString()).apply() }
    fun recent(): List<SearchPreset> = read("search_history_json").distinctBy { it.criteriaKey() }.take(40)
    fun saved(): List<SearchPreset> = read("search_saved_json").distinctBy { it.criteriaKey() }
    fun remember(preset: SearchPreset): Boolean = write("search_history_json",
        (listOf(preset) + recent().filterNot { it.criteriaKey() == preset.criteriaKey() }).take(40))
    fun save(preset: SearchPreset): Boolean = write("search_saved_json",
        listOf(preset) + saved().filterNot { it.criteriaKey() == preset.criteriaKey() })
    fun removeSaved(id: String): Boolean = write("search_saved_json", read("search_saved_json").filterNot { it.id == id })
    fun removeRecent(id: String): Boolean = write("search_history_json", read("search_history_json").filterNot { it.id == id })
    fun clearRecent(): Boolean = write("search_history_json", emptyList())
}
