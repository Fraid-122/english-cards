package com.fraid.englishcards.data

/**
 * Decides when each card is shown again, from how well the user knows it.
 *
 * Every card sits on a ladder of pauses: minutes for words that do not stick,
 * then days, then months. A right answer moves the card up the ladder, a wrong
 * answer sends it back to the first step.
 */
object Scheduler {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /** 1, 2, 3, 4, 5, 10, 30, 60 minutes; 1, 2, 3, 4, 5, 7, 14, 24 days; 1, 2, 3 months. */
    val steps: List<Long> = listOf(
        1 * MINUTE, 2 * MINUTE, 3 * MINUTE, 4 * MINUTE, 5 * MINUTE,
        10 * MINUTE, 30 * MINUTE, 60 * MINUTE,
        1 * DAY, 2 * DAY, 3 * DAY, 4 * DAY, 5 * DAY, 7 * DAY, 14 * DAY, 24 * DAY,
        30 * DAY, 60 * DAY, 90 * DAY,
    )

    /** A word known at first sight skips the minutes and comes back in a day. */
    const val FIRST_DAY_STEP = 8

    /** From the one-month pause on, the card counts as learned. */
    const val LEARNED_STEP = 16

    /** Version of this scheduling scheme, stored in each card. */
    const val SCHEME = 2

    /** Never shown again: passed the last step. */
    private const val RETIRED = Long.MAX_VALUE

    /** Time to study the card: new, failed a moment ago, or its pause is over. */
    fun isDue(card: Card, now: Long): Boolean = card.dueAt <= now

    /** Waiting for its next repetition and not learned yet. */
    fun isInProgress(card: Card, now: Long): Boolean = !isDue(card, now) && !card.known

    /** Waiting and already on the month-long pauses (or finished). */
    fun isLearned(card: Card, now: Long): Boolean = !isDue(card, now) && card.known

    fun afterAnswer(card: Card, known: Boolean, now: Long): Card {
        if (!known) {
            return card.copy(
                known = false,
                level = 0,
                streak = 0,
                dueAt = now + steps[0],
                wrongCount = card.wrongCount + 1,
                scheme = SCHEME,
            )
        }
        val streak = card.streak + 1
        val firstSight = card.correctCount == 0 && card.wrongCount == 0
        val next = when {
            firstSight -> FIRST_DAY_STEP
            // Three right answers in a row: the word sticks, climb faster.
            streak >= 3 -> card.level + 2
            else -> card.level + 1
        }
        val answered = card.copy(
            streak = streak,
            correctCount = card.correctCount + 1,
            scheme = SCHEME,
        )
        return if (next > steps.lastIndex) {
            answered.copy(known = true, level = steps.size, dueAt = RETIRED)
        } else {
            answered.copy(known = next >= LEARNED_STEP, level = next, dueAt = now + steps[next])
        }
    }

    /** Used by the "I know this card" switch in the editor. */
    fun markKnown(card: Card, known: Boolean, now: Long = System.currentTimeMillis()): Card =
        if (known) {
            card.copy(known = true, level = LEARNED_STEP, dueAt = now + steps[LEARNED_STEP], scheme = SCHEME)
        } else {
            card.copy(known = false, level = 0, streak = 0, dueAt = 0L, scheme = SCHEME)
        }

    /** Converts cards saved by earlier versions of the app to this scheme. */
    fun migrate(card: Card, now: Long): Card = when {
        card.scheme >= SCHEME -> card
        card.known -> card.copy(level = LEARNED_STEP, dueAt = now + steps[LEARNED_STEP], scheme = SCHEME)
        card.level > 0 -> card.copy(level = FIRST_DAY_STEP, scheme = SCHEME)
        else -> card
    }

    /** Short Ukrainian text for a waiting time, e.g. "5 хв", "2 год", "3 дн". */
    fun formatWait(millis: Long): String = when {
        millis < MINUTE -> "менше хвилини"
        millis < HOUR -> "${(millis + MINUTE - 1) / MINUTE} хв"
        millis < DAY -> "${(millis + HOUR - 1) / HOUR} год"
        else -> "${(millis + DAY - 1) / DAY} дн"
    }
}
