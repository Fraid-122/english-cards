package com.fraid.englishcards.data

/** Compares what the user types with the card's English word. */
object AnswerChecker {

    private val spaces = Regex("\\s+")

    fun normalize(text: String): String =
        text.trim()
            .lowercase()
            .replace('’', '\'')
            .replace('`', '\'')
            .replace(spaces, " ")

    /** True when the typed text is exactly the word (ignoring case and extra spaces). */
    fun isCorrect(input: String, word: String): Boolean {
        val typed = normalize(input)
        return typed.isNotEmpty() && typed == normalize(word)
    }

    /** True while every letter typed so far matches the beginning of the word. */
    fun isPrefixOk(input: String, word: String): Boolean =
        normalize(word).startsWith(normalize(input))
}
