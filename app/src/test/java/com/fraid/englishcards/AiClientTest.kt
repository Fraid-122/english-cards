package com.fraid.englishcards

import com.fraid.englishcards.data.AiClient
import com.fraid.englishcards.data.AiException
import com.fraid.englishcards.data.Difficulty
import com.fraid.englishcards.data.LanguageDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientTest {

    @Test
    fun detectsUkrainianAndEnglish() {
        assertTrue(LanguageDetector.isUkrainian("яблуко"))
        assertTrue(LanguageDetector.isUkrainian("їжа"))
        assertFalse(LanguageDetector.isUkrainian("apple"))
        assertFalse(LanguageDetector.isUkrainian("to figure out"))
    }

    @Test
    fun promptStatesTheDirection() {
        assertTrue(AiClient.buildPrompt("apple", emptyList()).contains("language: English"))
        assertTrue(AiClient.buildPrompt("яблуко", listOf("Їжа")).contains("language: Ukrainian"))
        assertTrue(AiClient.buildPrompt("яблуко", listOf("Їжа")).contains("[Їжа]"))
    }

    @Test
    fun parsesSuggestion() {
        val suggestion = AiClient.parseSuggestion(
            """{"word":"apple","translation":"яблуко","example":"I eat an apple.","topic":"Їжа","difficulty":"easy","emoji":"🍎"}"""
        )
        assertEquals("apple", suggestion.word)
        assertEquals("яблуко", suggestion.translation)
        assertEquals("Їжа", suggestion.topic)
        assertEquals(Difficulty.EASY, suggestion.difficulty)
        assertEquals("🍎", suggestion.emoji)
    }

    @Test
    fun parsesSuggestionInsideCodeFence() {
        val suggestion = AiClient.parseSuggestion(
            "```json\n{\"word\":\"journey\",\"translation\":\"подорож\",\"difficulty\":\"MEDIUM\"}\n```"
        )
        assertEquals("journey", suggestion.word)
        assertEquals(Difficulty.MEDIUM, suggestion.difficulty)
        assertEquals("", suggestion.example)
    }

    @Test
    fun parsesApiEnvelopeAndSkipsThoughts() {
        val response = """
            {"candidates":[{"content":{"parts":[
              {"text":"thinking...","thought":true},
              {"text":"{\"word\":\"bread\",\"translation\":\"хліб\",\"difficulty\":\"HARD\"}"}
            ]}}]}
        """.trimIndent()
        val suggestion = AiClient.parseResponse(response)
        assertEquals("bread", suggestion.word)
        assertEquals("хліб", suggestion.translation)
        assertEquals(Difficulty.HARD, suggestion.difficulty)
    }

    @Test
    fun brokenAnswersGiveReadableError() {
        assertThrows(AiException::class.java) { AiClient.parseSuggestion("sorry, no json") }
        assertThrows(AiException::class.java) { AiClient.parseResponse("{\"candidates\":[]}") }
        assertThrows(AiException::class.java) { AiClient.parseResponse("not json at all") }
    }
}
