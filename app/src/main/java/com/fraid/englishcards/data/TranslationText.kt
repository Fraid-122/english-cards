package com.fraid.englishcards.data

/** Small clean-ups of machine translation so it looks right on a card. */
object TranslationText {

    /**
     * Drops the full stop the translator sometimes adds and keeps the first letter
     * lowercase when the user typed a lowercase word.
     */
    fun tidy(source: String, translated: String): String {
        var result = translated.trim()
        val sourceTrimmed = source.trim()
        if (!sourceTrimmed.endsWith('.')) result = result.trimEnd('.').trim()
        val first = sourceTrimmed.firstOrNull()
        if (first != null && first.isLetter() && first.isLowerCase()) {
            // Keep the English pronoun "I" as it is.
            val isPronounI = result == "I" || result.startsWith("I ") || result.startsWith("I'")
            if (!isPronounI) result = result.replaceFirstChar { it.lowercase() }
        }
        return result
    }
}
