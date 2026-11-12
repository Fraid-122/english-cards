package com.fraid.englishcards.data

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty(val label: String) {
    EASY("Легка"),
    MEDIUM("Середня"),
    HARD("Складна"),
}

/** One flashcard. Each card is stored as its own JSON file on the phone. */
@Serializable
data class Card(
    val id: String,
    /** English word or phrase. */
    val word: String,
    /** Ukrainian translation. */
    val translation: String,
    val example: String = "",
    val topic: String = DEFAULT_TOPIC,
    val difficulty: Difficulty = Difficulty.EASY,
    /** Simple picture for the card; an emoji for now. */
    val emoji: String = "",
    /** Learned: answered correctly enough times (see Scheduler). */
    val known: Boolean = false,
    /** How many correct answers in a row, on different days. */
    val level: Int = 0,
    /** When the card should be shown again; 0 means right away. */
    val dueAt: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val createdAt: Long = 0L,
    /** Where the card came from, e.g. a YouTube link (used in a later stage). */
    val source: String = "",
) {
    companion object {
        const val DEFAULT_TOPIC = "Загальне"
    }
}
