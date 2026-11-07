package gal.galiciana.app.data

/** Deterministic, pure merge: no preferences or files are written during preview. */
internal object BackupMerge {
    private const val CONFLICTS = "backup_conflicts_json"
    data class Result(val preferences: Map<String, BackupPreference>, val conflicts: List<BackupConflict>)
    private fun text(value: String) = BackupPreference(BackupPreferenceType.STRING, listOf(value))
    private fun json(value: BackupPreference?): Any? {
        require(value?.type == BackupPreferenceType.STRING)
        return BackupJson.parse(value.values.single())
    }
    private fun objectValue(value: BackupPreference?): Map<String, Any?> {
        val map = json(value) as? Map<*, *> ?: error("Objeto inválido")
        return map.entries.associate { require(it.key is String); it.key as String to it.value }
    }
    private fun noteMap(value: BackupPreference?): Map<String, Any?> = objectValue(value).also {
        require(it.values.all { note -> note is String }) { "Mapa de notas inválido" }
    }
    private fun arrayValue(value: BackupPreference?): List<Any?> = json(value) as? List<*> ?: error("Lista inválida")
    private fun stringify(value: Any?): String = if (value is String) value else BackupJson.encode(value)
    private fun conflictMap(conflict: BackupConflict): Map<String, Any> = mapOf("id" to conflict.id,
        "key" to conflict.key, "kind" to conflict.kind, "localValue" to conflict.localValue,
        "incomingValue" to conflict.incomingValue, "createdAt" to conflict.createdAt)
    fun conflicts(preferences: Map<String, BackupPreference>): List<BackupConflict> = runCatching {
        arrayValue(preferences[CONFLICTS]).map { raw ->
            val map = raw as Map<*, *>
            BackupConflict(map["id"] as String, map["key"] as String, map["kind"] as String,
                map["localValue"] as String, map["incomingValue"] as String, map["createdAt"] as Long)
        }
    }.getOrDefault(emptyList())

    fun counts(preferences: Map<String, BackupPreference>): BackupCounts {
        val favorites = runCatching { arrayValue(preferences["fav_json"]).size }.getOrDefault(0)
        val noteKeys = linkedSetOf<String>()
        val canonicalNotes = runCatching { noteMap(preferences["notes_json"]) }.getOrDefault(emptyMap())
        canonicalNotes.filterValues { it is String && it.isNotBlank() }.keys.let(noteKeys::addAll)
        runCatching { arrayValue(preferences["fav_json"]) }.getOrNull()?.forEach { raw ->
            val item = raw as? Map<*, *> ?: return@forEach
            val key = "${item["path"]}|${item["pos"] ?: 1}"
            if (!canonicalNotes.containsKey(key) && (item["note"] as? String).orEmpty().isNotBlank()) noteKeys += key
        }
        val folders = runCatching { arrayValue(preferences["fav_folders"]).size }.getOrDefault(0)
        val searches = listOf("search_history_json", "search_saved_json").sumOf { key -> runCatching { arrayValue(preferences[key]).size }.getOrDefault(0) }
        return BackupCounts(preferences.size, favorites, noteKeys.size, folders, searches)
    }

    fun merge(current: Map<String, BackupPreference>, incoming: Map<String, BackupPreference>, createdAt: Long,
        resolutions: Map<String, BackupResolution> = emptyMap()): Result {
        val result = current.toMutableMap()
        val pending = linkedMapOf<String, BackupConflict>()
        (conflicts(current) + conflicts(incoming)).forEach { pending[it.id] = it }
        val detected = linkedMapOf<String, BackupConflict>()
        fun choose(key: String, kind: String, local: Any?, other: Any?): Any? {
            if (local == other) return local
            val localString = stringify(local)
            val otherString = stringify(other)
            val id = "$kind:$key:" + BackupFormat.sha256((localString + "\u0000" + otherString).toByteArray()).take(24)
            val conflict = BackupConflict(id, key, kind, localString, otherString, createdAt)
            detected[id] = conflict
            return when (resolutions[id] ?: BackupResolution.KEEP_BOTH) {
                BackupResolution.TAKE_INCOMING -> { pending.remove(id); other }
                BackupResolution.KEEP_LOCAL -> { pending.remove(id); local }
                BackupResolution.KEEP_BOTH -> { pending[id] = conflict; local }
            }
        }
        fun notesOf(source: Map<String, BackupPreference>): Map<String, String> {
            val notes = linkedMapOf<String, String>()
            runCatching { arrayValue(source["fav_json"]) }.getOrNull()?.forEach { raw ->
                val item = raw as? Map<*, *> ?: return@forEach
                val note = item["note"] as? String ?: return@forEach
                if (note.isNotBlank()) notes["${item["path"]}|${item["pos"] ?: 1}"] = note
            }
            runCatching { noteMap(source["notes_json"]) }.getOrNull()?.forEach { (key, value) ->
                if (value is String) notes[key] = value
            }
            return notes
        }
        val mergedNotes = notesOf(current).toMutableMap()
        notesOf(incoming).forEach { (key, value) ->
            val old = mergedNotes[key]
            mergedNotes[key] = if (old == null) value else choose(key, "Nota de referencia", old, value) as String
        }
        fun recordKey(item: Map<*, *>, favorites: Boolean): String = if (favorites && item["path"] is String)
            "${item["path"]}|${item["pos"] ?: 1}" else item["id"] as? String ?: BackupJson.encode(item)
        fun mergeRecords(local: List<Any?>, other: List<Any?>, favorites: Boolean): List<Any?> {
            val rows = linkedMapOf<String, Map<String, Any?>>()
            fun record(raw: Any?): Map<String, Any?> {
                val map = raw as? Map<*, *> ?: error("Registro inválido")
                return map.entries.associate { require(it.key is String); it.key as String to it.value }
            }
            local.forEach { raw -> val item = record(raw); rows[recordKey(item, favorites)] = item }
            other.forEach { raw ->
                val item = record(raw); val key = recordKey(item, favorites); val previous = rows[key]
                if (previous == null) rows[key] = item
                else if (favorites) {
                    // Keep existing bibliographic/snippet provenance; fill only absent values.
                    val combined = item.toMutableMap()
                    previous.forEach { (field, value) ->
                        if (value != null && value != "" && value != emptyList<Any>()) combined[field] = value
                    }
                    val localContext = previous.filterKeys { it != "note" }
                    val incomingContext = item.filterKeys { it != "note" }
                    val conflictingContext = incomingContext.any { (field, value) ->
                        val oldValue = localContext[field]
                        value != null && value != "" && oldValue != null && oldValue != "" && oldValue != value
                    }
                    if (conflictingContext && choose(key, "Contexto de página", localContext, incomingContext) == incomingContext) {
                        incomingContext.forEach { (field, value) -> if (value != null && value != "") combined[field] = value }
                    }
                    rows[key] = combined
                } else if (previous != item) {
                    @Suppress("UNCHECKED_CAST")
                    rows[key] = choose(key, "Búsqueda", previous, item) as Map<String, Any?>
                }
            }
            return rows.map { (key, row) -> if (favorites && mergedNotes.containsKey(key)) row + ("note" to mergedNotes.getValue(key)) else row }
        }
        fun folders(value: Any?): List<String> = when (value) {
            is List<*> -> value.map { require(it is String); it }
            is String -> if (value.trim().startsWith("[")) folders(BackupJson.parse(value)) else listOf(value).filter(String::isNotBlank)
            else -> error("Carpetas inválidas")
        }
        incoming.forEach { (key, value) ->
            if (key == CONFLICTS || key == "notes_json") return@forEach
            val old = current[key]
            if (old == null) { result[key] = value; return@forEach }
            if (old == value) return@forEach
            val combined = runCatching {
                when {
                    old.type == BackupPreferenceType.STRING_SET && value.type == old.type ->
                        BackupPreference(BackupPreferenceType.STRING_SET, (old.values + value.values).distinct().sorted())
                    key in listOf("fav_json", "search_history_json", "search_saved_json") ->
                        text(BackupJson.encode(mergeRecords(arrayValue(old), arrayValue(value), key == "fav_json")))
                    key == "fav_folders" -> text(BackupJson.encode((folders(json(old)) + folders(json(value))).distinctBy { it.lowercase() }))
                    key == "fav_folder_map" -> {
                        val map = objectValue(old).toMutableMap()
                        objectValue(value).forEach { (page, names) ->
                            map[page] = if (map.containsKey(page)) (folders(map[page]) + folders(names)).distinctBy { it.lowercase() } else folders(names)
                        }
                        text(BackupJson.encode(map))
                    }
                    key == "fav_stash" -> {
                        val map = objectValue(old).toMutableMap()
                        objectValue(value).forEach { (page, raw) ->
                            val previous = map[page]
                            if (previous == null) map[page] = raw
                            else if (previous != raw) map[page] = choose(page, "Página archivada", previous, raw)
                        }
                        text(BackupJson.encode(map))
                    }
                    else -> null
                }
            }.getOrNull()
            if (combined != null) result[key] = combined
            else {
                val selected = choose(key, "Preferencia", mapOf("type" to old.type.name, "values" to old.values),
                    mapOf("type" to value.type.name, "values" to value.values)) as Map<*, *>
                result[key] = BackupPreference(BackupPreferenceType.valueOf(selected["type"] as String),
                    (selected["values"] as List<*>).map { it as String })
            }
        }
        // Preserve orphan notes as well as notes embedded in saved pages. Both representations agree.
        if (mergedNotes.isNotEmpty() || current.containsKey("notes_json") || incoming.containsKey("notes_json"))
            result["notes_json"] = text(BackupJson.encode(mergedNotes))
        if (result.containsKey("fav_json")) {
            runCatching {
                val normalized = arrayValue(result["fav_json"]).map { raw ->
                    val row = raw as Map<*, *>
                    val key = "${row["path"]}|${row["pos"] ?: 1}"
                    if (mergedNotes.containsKey(key)) row.entries.associate { it.key as String to it.value } + ("note" to mergedNotes.getValue(key)) else row
                }
                result["fav_json"] = text(BackupJson.encode(normalized))
            }
        }
        // A malformed note map is still preserved explicitly instead of disappearing in a merge.
        listOf(current, incoming).forEachIndexed { i, source ->
            source["notes_json"]?.let { value -> if (runCatching { noteMap(value) }.isFailure) {
                choose("notes_json:$i", "Archivo de notas no interpretable", "", value.values.joinToString("\n"))
            } }
        }
        if (pending.isNotEmpty()) result[CONFLICTS] = text(BackupJson.encode(pending.values.map(::conflictMap)))
        return Result(result.toMap(), detected.values.filter { it.id in pending })
    }
}
