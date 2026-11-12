package com.fraid.englishcards.data

/**
 * Spaced repetition: a card becomes learned after several correct answers
 * on different days, with longer and longer pauses between them.
 */
object Scheduler {

    const val LEARNED_LEVEL = 4
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Days to wait after reaching level 1, 2 and 3. */
    private val pauseDays = listOf(1, 3, 7)

    /** Ready to be studied now. */
    fun isDue(card: Card, now: Long): Boolean = !card.known && card.dueAt <= now

    /** Answered correctly before and waiting for its next repetition. */
    fun isInProgress(card: Card, now: Long): Boolean =
        !card.known && card.level > 0 && card.dueAt > now

    fun afterAnswer(card: Card, known: Boolean, now: Long): Card {
        if (!known) {
            return card.copy(known = false, level = 0, dueAt = now, wrongCount = card.wrongCount + 1)
        }
        val level = card.level + 1
        return if (level >= LEARNED_LEVEL) {
            card.copy(known = true, level = LEARNED_LEVEL, dueAt = 0L, correctCount = card.correctCount + 1)
        } else {
            card.copy(
                known = false,
                level = level,
                dueAt = now + pauseDays[level - 1] * DAY_MILLIS,
                correctCount = card.correctCount + 1,
            )
        }
    }

    /** Used by the "I know this card" switch in the editor. */
    fun markKnown(card: Card, known: Boolean): Card =
        if (known) card.copy(known = true, level = LEARNED_LEVEL, dueAt = 0L)
        else card.copy(known = false, level = 0, dueAt = 0L)
}
