package com.fraid.englishcards.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Keeps cards as files: one `<id>.json` per card inside [dir]
 * (the app's private storage on the phone).
 */
class CardRepository(private val dir: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    @Synchronized
    fun load() {
        dir.mkdirs()
        val seededMarker = File(dir, SEEDED_MARKER)
        val files = dir.listFiles { file -> file.extension == "json" }.orEmpty()
        if (files.isEmpty() && !seededMarker.exists()) {
            StarterCards.create().forEach { writeFile(it) }
            seededMarker.writeText("1")
        }
        val now = System.currentTimeMillis()
        _cards.value = dir.listFiles { file -> file.extension == "json" }.orEmpty()
            .mapNotNull { file ->
                runCatching { json.decodeFromString(Card.serializer(), file.readText()) }.getOrNull()
            }
            .map { card ->
                // Cards from earlier versions are converted to the current schedule once.
                val migrated = Scheduler.migrate(card, now)
                if (migrated != card) writeFile(migrated)
                migrated
            }
            .sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun save(card: Card) {
        writeFile(card)
        _cards.value = (_cards.value.filterNot { it.id == card.id } + card)
            .sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun delete(id: String) {
        File(dir, "$id.json").delete()
        _cards.value = _cards.value.filterNot { it.id == id }
    }

    private fun writeFile(card: Card) {
        dir.mkdirs()
        // Write to a temp file first so a power cut can't leave a half-written card.
        val tmp = File(dir, "${card.id}.tmp")
        tmp.writeText(json.encodeToString(Card.serializer(), card))
        val target = File(dir, "${card.id}.json")
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private companion object {
        const val SEEDED_MARKER = ".seeded"
    }
}
