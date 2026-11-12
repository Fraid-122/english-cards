package com.fraid.englishcards

import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000_000_000_000L
    private val fresh = Card(id = "1", word = "apple", translation = "яблуко")

    @Test
    fun newCardIsDue() {
        assertTrue(Scheduler.isDue(fresh, now))
        assertFalse(Scheduler.isInProgress(fresh, now))
    }

    @Test
    fun correctAnswerMovesCardToInProgress() {
        val card = Scheduler.afterAnswer(fresh, known = true, now = now)
        assertEquals(1, card.level)
        assertEquals(now + day, card.dueAt)
        assertFalse(card.known)
        assertFalse(Scheduler.isDue(card, now))
        assertTrue(Scheduler.isInProgress(card, now))
        assertTrue(Scheduler.isDue(card, now + day))
    }

    @Test
    fun pausesGrowAndCardBecomesLearned() {
        val first = Scheduler.afterAnswer(fresh, true, now)
        val second = Scheduler.afterAnswer(first, true, now)
        assertEquals(now + 3 * day, second.dueAt)
        val third = Scheduler.afterAnswer(second, true, now)
        assertEquals(now + 7 * day, third.dueAt)
        val fourth = Scheduler.afterAnswer(third, true, now)
        assertTrue(fourth.known)
        assertEquals(4, fourth.correctCount)
        assertFalse(Scheduler.isDue(fourth, now + 100 * day))
    }

    @Test
    fun wrongAnswerStartsOver() {
        val progressed = Scheduler.afterAnswer(Scheduler.afterAnswer(fresh, true, now), true, now)
        val missed = Scheduler.afterAnswer(progressed, known = false, now = now)
        assertEquals(0, missed.level)
        assertEquals(1, missed.wrongCount)
        assertTrue(Scheduler.isDue(missed, now))
    }

    @Test
    fun knownSwitchSetsAndResets() {
        val known = Scheduler.markKnown(fresh, true)
        assertTrue(known.known)
        val reset = Scheduler.markKnown(known, false)
        assertFalse(reset.known)
        assertTrue(Scheduler.isDue(reset, now))
    }
}
