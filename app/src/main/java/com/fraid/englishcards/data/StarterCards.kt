package com.fraid.englishcards.data

import java.util.UUID

/** A few example cards so the app is not empty on first launch. */
object StarterCards {

    private data class Seed(
        val word: String,
        val translation: String,
        val example: String,
        val topic: String,
        val difficulty: Difficulty,
        val emoji: String,
    )

    private val seeds = listOf(
        Seed("apple", "яблуко", "I eat an apple every day.", "Їжа", Difficulty.EASY, "🍎"),
        Seed("bread", "хліб", "She bought fresh bread.", "Їжа", Difficulty.EASY, "🍞"),
        Seed("journey", "подорож", "The journey took three hours.", "Подорожі", Difficulty.MEDIUM, "🧭"),
        Seed("luggage", "багаж", "Where is my luggage?", "Подорожі", Difficulty.MEDIUM, "🧳"),
        Seed("to charge", "заряджати", "I need to charge my phone.", "Побут", Difficulty.EASY, "🔌"),
        Seed("power outage", "відключення світла", "We had a power outage last night.", "Побут", Difficulty.HARD, "💡"),
        Seed("to figure out", "з'ясувати, розібратися", "Let's figure out how it works.", "Фразові дієслова", Difficulty.HARD, "🧩"),
        Seed("weather", "погода", "The weather is nice today.", Card.DEFAULT_TOPIC, Difficulty.EASY, "⛅"),
    )

    fun create(now: Long = System.currentTimeMillis()): List<Card> =
        seeds.mapIndexed { index, seed ->
            Card(
                id = UUID.randomUUID().toString(),
                word = seed.word,
                translation = seed.translation,
                example = seed.example,
                topic = seed.topic,
                difficulty = seed.difficulty,
                emoji = seed.emoji,
                createdAt = now - index,
            )
        }
}
