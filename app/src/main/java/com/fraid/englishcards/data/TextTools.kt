package com.fraid.englishcards.data

/** A selected piece of a text: from [start] up to, but not including, [end]. */
data class TextSpan(val start: Int, val end: Int)

/** Small text helpers: picking a word from a text, making exercises, reading links. */
object TextTools {

    private const val SENTENCE_END = ".!?\n"

    private fun isWordChar(c: Char): Boolean = c.isLetter() || c == '\'' || c == '’' || c == '-'

    /** Where the word under the character at [offset] starts and ends, or null for a space or symbol. */
    fun wordSpanAt(text: String, offset: Int): TextSpan? {
        if (text.isEmpty()) return null
        var i = offset.coerceIn(0, text.length - 1)
        if (!isWordChar(text[i])) {
            if (i > 0 && isWordChar(text[i - 1])) i -= 1 else return null
        }
        var start = i
        while (start > 0 && isWordChar(text[start - 1])) start--
        var end = i
        while (end < text.length - 1 && isWordChar(text[end + 1])) end++
        // Drop quotes and dashes around the word.
        while (start <= end && !text[start].isLetter()) start++
        while (end >= start && !text[end].isLetter()) end--
        return if (start > end) null else TextSpan(start, end + 1)
    }

    /** The word under the character at [offset], or null when it is a space or a symbol. */
    fun wordAt(text: String, offset: Int): String? =
        wordSpanAt(text, offset)?.let { text.substring(it.start, it.end) }

    /** The start of the word before [start] on the same line, or [start] when there is none. */
    fun expandLeft(text: String, start: Int): Int {
        var i = start.coerceIn(0, text.length) - 1
        while (i >= 0 && !text[i].isLetter()) {
            if (text[i] == '\n') return start
            i--
        }
        if (i < 0) return start
        return wordSpanAt(text, i)?.start ?: start
    }

    /** The end of the word after [end] on the same line, or [end] when there is none. */
    fun expandRight(text: String, end: Int): Int {
        var i = end.coerceIn(0, text.length)
        while (i < text.length && !text[i].isLetter()) {
            if (text[i] == '\n') return end
            i++
        }
        if (i >= text.length) return end
        return wordSpanAt(text, i)?.end ?: end
    }

    /** Where the sentence around the character at [offset] starts and ends. */
    fun sentenceSpanAt(text: String, offset: Int): TextSpan {
        if (text.isEmpty()) return TextSpan(0, 0)
        val i = offset.coerceIn(0, text.length - 1)
        var start = i
        while (start > 0 && text[start - 1] !in SENTENCE_END) start--
        var end = i
        while (end < text.length - 1 && text[end] !in SENTENCE_END) end++
        while (start < end && text[start].isWhitespace()) start++
        var endExclusive = end + 1
        while (endExclusive > start && text[endExclusive - 1].isWhitespace()) endExclusive--
        return TextSpan(start, endExclusive)
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
