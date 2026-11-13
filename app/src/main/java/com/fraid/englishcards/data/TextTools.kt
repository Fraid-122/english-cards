package com.fraid.englishcards.data

/** Small text helpers: picking a word from a text, making exercises, reading links. */
object TextTools {

    private const val SENTENCE_END = ".!?\n"

    private fun isWordChar(c: Char): Boolean = c.isLetter() || c == '\'' || c == '’' || c == '-'

    /** The word under the character at [offset], or null when it is a space or a symbol. */
    fun wordAt(text: String, offset: Int): String? {
        if (text.isEmpty()) return null
        var i = offset.coerceIn(0, text.length - 1)
        if (!isWordChar(text[i])) {
            if (i > 0 && isWordChar(text[i - 1])) i -= 1 else return null
        }
        var start = i
        while (start > 0 && isWordChar(text[start - 1])) start--
        var end = i
        while (end < text.length - 1 && isWordChar(text[end + 1])) end++
        val word = text.substring(start, end + 1).trim('\'', '’', '-')
        return word.takeIf { candidate -> candidate.any { it.isLetter() } }
    }

    /** The sentence around the character at [offset]. */
    fun sentenceAt(text: String, offset: Int): String {
        if (text.isEmpty()) return ""
        val i = offset.coerceIn(0, text.length - 1)
        var start = i
        while (start > 0 && text[start - 1] !in SENTENCE_END) start--
        var end = i
        while (end < text.length - 1 && text[end] !in SENTENCE_END) end++
        return text.substring(start, end + 1).trim().take(240)
    }

    /** The example with the word replaced by a gap, or null when the word is not in it. */
    fun gapSentence(example: String, word: String): String? {
        val target = word.trim()
        if (target.isEmpty()) return null
        val index = example.indexOf(target, ignoreCase = true)
        if (index < 0) return null
        return example.replaceRange(index, index + target.length, "_____")
    }

    private val youtubeLink = Regex("""(?:youtu\.be/|[?&]v=|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})""")
    private val youtubeBareId = Regex("""^[A-Za-z0-9_-]{11}$""")

    /** The video id from a YouTube link (or a bare id), or null. */
    fun youtubeId(input: String): String? {
        val text = input.trim()
        youtubeLink.find(text)?.let { return it.groupValues[1] }
        return text.takeIf { youtubeBareId.matches(it) }
    }
}
