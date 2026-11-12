package com.fraid.englishcards

import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {

    private val minute = 60_000L
    private val day = 24 * 60 * minute
    private val now = 1_000_000_000_000L
    private val fresh = Card(id = "1", word = "apple", translation = "яблуко")

    @Test
    fun ladderMatchesTheSpec() {
        val minutes = Scheduler.steps.take(8).map { it / minute }
        val days = Scheduler.steps.drop(8).map { it / day }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 10L, 30L, 60L), minutes)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 7L, 14L, 24L, 30L, 60L, 90L), days)
    }

    @Test
    fun newCardIsDue() {
        assertTrue(Scheduler.isDue(fresh, now))
        assertFalse(Scheduler.isInProgress(fresh, now))
        assertFalse(Scheduler.isLearned(fresh, now))
    }

    @Test
    fun wordKnownAtFirstSightComesBackInADay() {
        val card = Scheduler.afterAnswer(fresh, known = true, now = now)
        assertEquals(now + day, card.dueAt)
        assertTrue(Scheduler.isInProgress(card, now))
        assertTrue(Scheduler.isDue(card, now + day))
    }

    @Test
    fun wrongAnswerComesBackInAMinute() {
        val card = Scheduler.afterAnswer(fresh, known = false, now = now)
        assertEquals(now + minute, card.dueAt)
        assertEquals(0, card.level)
        assertEquals(1, card.wrongCount)
        assertFalse(card.known)
    }

    @Test
    fun hardWordClimbsTheMinutesThenSpeedsUp() {
        var card = Scheduler.afterAnswer(fresh, false, now)
        card = Scheduler.afterAnswer(card, true, now)
        assertEquals(now + 2 * minute, card.dueAt)
        card = Scheduler.afterAnswer(card, true, now)
        assertEquals(now + 3 * minute, card.dueAt)
        // Third right answer in a row jumps two steps: 3 min -> 5 min.
        card = Scheduler.afterAnswer(card, true, now)
        assertEquals(now + 5 * minute, card.dueAt)
        card = Scheduler.afterAnswer(card, true, now)
        assertEquals(now + 30 * minute, card.dueAt)
    }

    @Test
    fun wrongAnswerOnOldCardStartsOver() {
        var card = Scheduler.afterAnswer(fresh, true, now)
        card = Scheduler.afterAnswer(card, true, now)
        assertEquals(now + 2 * day, card.dueAt)
        card = Scheduler.afterAnswer(card, false, now)
        assertEquals(now + minute, card.dueAt)
        assertEquals(0, card.streak)
    }

    @Test
    fun cardBecomesLearnedOnMonthPausesAndFinallyRetires() {
        var card = fresh
        var learnedAt = -1L
        repeat(30) {
            card = Scheduler.afterAnswer(card, true, now)
            if (card.known && learnedAt < 0) learnedAt = card.dueAt - now
        }
        // The climb speeds up, so the first month-long pause it lands on is two months.
        assertEquals(60 * day, learnedAt)
        assertTrue(card.known)
        assertFalse(Scheduler.isDue(card, now + 10_000 * day))
        assertTrue(Scheduler.isLearned(card, now))
    }

    @Test
    fun knownSwitchSetsAndResets() {
        val known = Scheduler.markKnown(fresh, true, now)
        assertTrue(Scheduler.isLearned(known, now))
        val reset = Scheduler.markKnown(known, false, now)
        assertFalse(reset.known)
        assertTrue(Scheduler.isDue(reset, now))
    }

    @Test
    fun migratesCardsFromEarlierVersions() {
        val oldKnown = fresh.copy(known = true, level = 4)
        assertTrue(Scheduler.isLearned(Scheduler.migrate(oldKnown, now), now))
        val oldProgress = fresh.copy(level = 2, dueAt = now + 3 * day)
        val migrated = Scheduler.migrate(oldProgress, now)
        assertEquals(Scheduler.FIRST_DAY_STEP, migrated.level)
        assertEquals(now + 3 * day, migrated.dueAt)
        assertEquals(fresh, Scheduler.migrate(fresh, now))
        val current = Scheduler.afterAnswer(fresh, false, now)
        assertEquals(current, Scheduler.migrate(current, now))
    }

    @Test
    fun formatsWaitingTime() {
        assertEquals("5 хв", Scheduler.formatWait(5 * minute))
        assertEquals("2 год", Scheduler.formatWait(90 * minute))
        assertEquals("3 дн", Scheduler.formatWait(3 * day))
    }
}
