package com.fraid.englishcards

import com.fraid.englishcards.data.AnswerChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {

    @Test
    fun exactWordIsCorrect() {
        assertTrue(AnswerChecker.isCorrect("apple", "apple"))
    }

    @Test
    fun caseAndSpacesAreIgnored() {
        assertTrue(AnswerChecker.isCorrect("  Power   Outage ", "power outage"))
    }

    @Test
    fun curlyApostropheMatchesStraight() {
        assertTrue(AnswerChecker.isCorrect("don’t", "don't"))
    }

    @Test
    fun unfinishedWordIsNotCorrect() {
        assertFalse(AnswerChecker.isCorrect("appl", "apple"))
        assertFalse(AnswerChecker.isCorrect("", "apple"))
        assertFalse(AnswerChecker.isCorrect("   ", "apple"))
    }

    @Test
    fun prefixIsOkWhileLettersMatch() {
        assertTrue(AnswerChecker.isPrefixOk("", "apple"))
        assertTrue(AnswerChecker.isPrefixOk("App", "apple"))
        assertTrue(AnswerChecker.isPrefixOk("power ", "power outage"))
    }

    @Test
    fun prefixIsWrongAfterMistake() {
        assertFalse(AnswerChecker.isPrefixOk("apx", "apple"))
        assertFalse(AnswerChecker.isPrefixOk("apples", "apple"))
    }
}
