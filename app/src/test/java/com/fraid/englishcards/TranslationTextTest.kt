package com.fraid.englishcards

import com.fraid.englishcards.data.TranslationText
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationTextTest {

    @Test
    fun lowercasesFirstLetterForLowercaseSource() {
        assertEquals("apple", TranslationText.tidy("яблуко", "Apple"))
        assertEquals("яблуко", TranslationText.tidy("apple", "Яблуко"))
    }

    @Test
    fun keepsCapitalWhenSourceIsCapitalized() {
        assertEquals("Ukraine", TranslationText.tidy("Україна", "Ukraine"))
    }

    @Test
    fun dropsAddedFullStop() {
        assertEquals("power outage", TranslationText.tidy("відключення світла", "Power outage."))
        assertEquals("It works.", TranslationText.tidy("Це працює.", "It works."))
    }

    @Test
    fun keepsPronounI() {
        assertEquals("I know", TranslationText.tidy("я знаю", "I know"))
    }
}
