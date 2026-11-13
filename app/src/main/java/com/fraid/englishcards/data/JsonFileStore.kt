package com.fraid.englishcards.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Keeps items as files: one `<id>.json` per item inside [dir]. */
class JsonFileStore<T>(
    private val dir: File,
    private val serializer: KSerializer<T>,
    private val idOf: (T) -> String,
    private val createdAt: (T) -> Long,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    /** Reads everything; on the very first launch writes the [seed] items. */
    @Synchronized
    fun load(seed: () -> List<T> = { emptyList() }) {
        dir.mkdirs()
        val marker = File(dir, ".seeded")
        if (!marker.exists()) {
            seed().forEach { write(it) }
            marker.writeText("1")
        }
        _items.value = dir.listFiles { file -> file.extension == "json" }.orEmpty()
            .mapNotNull { file ->
                runCatching { json.decodeFromString(serializer, file.readText()) }.getOrNull()
            }
            .sortedByDescending(createdAt)
    }

    @Synchronized
    fun save(item: T) {
        write(item)
        val id = idOf(item)
        _items.value = (_items.value.filterNot { idOf(it) == id } + item).sortedByDescending(createdAt)
    }

    @Synchronized
    fun delete(id: String) {
        File(dir, "$id.json").delete()
        _items.value = _items.value.filterNot { idOf(it) == id }
    }

    private fun write(item: T) {
        dir.mkdirs()
        val id = idOf(item)
        val tmp = File(dir, "$id.tmp")
        tmp.writeText(json.encodeToString(serializer, item))
        val target = File(dir, "$id.json")
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
