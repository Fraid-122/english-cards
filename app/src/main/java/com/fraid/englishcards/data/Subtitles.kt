package com.fraid.englishcards.data

/** Turns separate subtitle lines into one flowing text, so a selection can cross lines. */
object Subtitles {

    /**
     * [text] is all lines joined with spaces; line `i` starts at character [starts]`[i]`
     * and is spoken at [times]`[i]` milliseconds.
     */
    class Joined(val text: String, val starts: List<Int>, val times: List<Long>) {

        /** The line being spoken at [positionMs], or -1 before the first one. */
        fun indexAt(positionMs: Long): Int = times.indexOfLast { it <= positionMs }

        /** Where line [index] sits in [text]. */
        fun spanOf(index: Int): TextSpan? {
            if (index !in starts.indices) return null
            val end = if (index + 1 < starts.size) starts[index + 1] - 1 else text.length
            return TextSpan(starts[index], end)
        }
    }

    private val soundTag = Regex("\\[[^\\]]*\\]")
    private val spaces = Regex("\\s+")

    /** Removes speaker marks (">>") and sound tags ("[music]") that auto-subtitles contain. */
    fun clean(line: String): String =
        line.replace(">>", " ").replace(soundTag, " ").replace(spaces, " ").trim()

    fun join(lines: List<VideoLine>): Joined {
        val text = StringBuilder()
        val starts = ArrayList<Int>()
        val times = ArrayList<Long>()
        for (line in lines) {
            val cleaned = clean(line.text)
            if (cleaned.isEmpty()) continue
            if (text.isNotEmpty()) text.append(' ')
            starts.add(text.length)
            times.add(line.startMs)
            text.append(cleaned)
        }
        return Joined(text.toString(), starts, times)
    }
}
