package com.fraid.englishcards

import com.fraid.englishcards.data.AiClient
import com.fraid.englishcards.data.BigDecks
import com.fraid.englishcards.data.Decks
import com.fraid.englishcards.data.Streak
import com.fraid.englishcards.data.TextTools
import com.fraid.englishcards.data.VoiceClient
import com.fraid.englishcards.data.YouTubeCaptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentToolsTest {

    private val text = "I wake up at seven. Then I don't eat; I make tea!\nThe end"

    @Test
    fun picksTheWordUnderTheTap() {
        assertEquals("wake", TextTools.wordAt(text, text.indexOf("wake") + 2))
        assertEquals("don't", TextTools.wordAt(text, text.indexOf("don't") + 4))
        // A tap right after a word still picks it.
        assertEquals("seven", TextTools.wordAt(text, text.indexOf("seven") + 5))
        assertNull(TextTools.wordAt("a  b", 2))
        assertNull(TextTools.wordAt("", 0))
    }

    @Test
    fun picksTheSentenceAroundTheTap() {
        assertEquals("I wake up at seven.", TextTools.sentenceAt(text, 3))
        assertEquals("Then I don't eat; I make tea!", TextTools.sentenceAt(text, text.indexOf("tea")))
        assertEquals("The end", TextTools.sentenceAt(text, text.length - 1))
    }

    @Test
    fun selectionGrowsWordByWordAndToSentence() {
        val start = text.indexOf("wake")
        val word = TextTools.wordSpanAt(text, start)!!
        assertEquals("wake", text.substring(word.start, word.end))
        val left = TextTools.expandLeft(text, word.start)
        assertEquals("I wake", text.substring(left, word.end))
        val right = TextTools.expandRight(text, word.end)
        assertEquals("wake up", text.substring(word.start, right))
        // Nothing before the first word; nothing after the last one.
        assertEquals(0, TextTools.expandLeft(text, 0))
        assertEquals(text.length, TextTools.expandRight(text, text.length))
        // Growing stops at a line break.
        val end = text.indexOf("The end")
        assertEquals(end, TextTools.expandLeft(text, end))
        val sentence = TextTools.sentenceSpanAt(text, text.indexOf("tea"))
        assertEquals("Then I don't eat; I make tea!", text.substring(sentence.start, sentence.end))
        // Quotes around a word are not part of it.
        val quoted = "He said 'hello' twice"
        val hello = TextTools.wordSpanAt(quoted, quoted.indexOf("hello"))!!
        assertEquals("hello", quoted.substring(hello.start, hello.end))
    }

    @Test
    fun makesGapSentence() {
        assertEquals("I eat an _____ every day.", TextTools.gapSentence("I eat an apple every day.", "apple"))
        assertEquals("_____ is good.", TextTools.gapSentence("Bread is good.", "bread"))
        assertNull(TextTools.gapSentence("Let's figure it out.", "to figure out"))
    }

    @Test
    fun readsYouTubeLinks() {
        assertEquals("dQw4w9WgXcQ", TextTools.youtubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
        assertEquals("dQw4w9WgXcQ", TextTools.youtubeId("https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", TextTools.youtubeId("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", TextTools.youtubeId(" dQw4w9WgXcQ "))
        assertNull(TextTools.youtubeId("https://example.com/video"))
    }

    @Test
    fun streakGrowsDayByDayAndResets() {
        assertEquals(1, Streak.afterStudy(lastDay = 0, streak = 0, today = 100))
        assertEquals(1, Streak.afterStudy(lastDay = 100, streak = 1, today = 100))
        assertEquals(2, Streak.afterStudy(lastDay = 100, streak = 1, today = 101))
        assertEquals(1, Streak.afterStudy(lastDay = 100, streak = 5, today = 103))
        assertEquals(5, Streak.current(lastDay = 100, streak = 5, today = 101))
        assertEquals(0, Streak.current(lastDay = 100, streak = 5, today = 102))
    }

    @Test
    fun decksAreWellFormed() {
        assertTrue(Decks.all.isNotEmpty())
        Decks.all.forEach { deck ->
            val cards = deck.cards(now = 1000)
            assertEquals(deck.size, cards.size)
            cards.forEach { card ->
                assertTrue(card.word.isNotBlank())
                assertTrue(card.translation.isNotBlank())
                assertTrue(card.example.isNotBlank())
                assertEquals(deck.title, card.topic)
            }
        }
    }

    @Test
    fun bigDecksAreLargeAndUsableInGapExercise() {
        assertEquals(10, BigDecks.all.size)
        assertEquals(Decks.all.size, Decks.all.map { it.id }.toSet().size)
        BigDecks.all.forEach { deck ->
            assertTrue(deck.size >= 50)
            deck.cards(now = 1000).forEach { card ->
                assertTrue(card.word, TextTools.gapSentence(card.example, card.word) != null)
                assertTrue(card.emoji.isNotBlank())
            }
        }
    }

    @Test
    fun findsEnglishCaptionTrack() {
        val html = """<html><title>My &amp; video - YouTube</title><script>var x = {"captions":{"playerCaptionsTracklistRenderer":{"captionTracks":[{"baseUrl":"https://www.youtube.com/api/timedtext?v=1&lang=uk","languageCode":"uk"},{"baseUrl":"https://www.youtube.com/api/timedtext?v=1&lang=en&kind=asr","languageCode":"en","kind":"asr","name":{"simpleText":"English [auto]"}},{"baseUrl":"https://www.youtube.com/api/timedtext?v=1&lang=en","languageCode":"en"}],"audioTracks":[]}}};</script></html>"""
        assertEquals("https://www.youtube.com/api/timedtext?v=1&lang=en", YouTubeCaptions.captionTrackUrl(html))
        assertEquals("My & video", YouTubeCaptions.parseTitle(html))
        assertNull(YouTubeCaptions.captionTrackUrl("<html>no captions</html>"))
    }

    @Test
    fun readsJson3Subtitles() {
        val json3 = """{"events":[{"tStartMs":0,"dDurationMs":1000},{"tStartMs":1200,"segs":[{"utf8":"Hello "},{"utf8":"world"}]},{"tStartMs":2000,"segs":[{"utf8":"\n"}]},{"tStartMs":3000,"segs":[{"utf8":"Second\nline"}]}]}"""
        val lines = YouTubeCaptions.parseJson3(json3)
        assertEquals(2, lines.size)
        assertEquals(1200L, lines[0].startMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals("Second line", lines[1].text)
        assertTrue(YouTubeCaptions.parseJson3("broken").isEmpty())
    }

    @Test
    fun speechCacheNameIsStableAndSpecific() {
        val name = VoiceClient.cacheName("voice", "model", "Apple")
        assertEquals(name, VoiceClient.cacheName("voice", "model", "  apple "))
        assertTrue(name.endsWith(".mp3"))
        assertTrue(name != VoiceClient.cacheName("voice", "model", "apples"))
        assertTrue(name != VoiceClient.cacheName("other", "model", "apple"))
        assertTrue(name != VoiceClient.cacheName("voice", "other", "apple"))
    }

    @Test
    fun explainsElevenLabsErrors() {
        assertTrue(VoiceClient.errorMessage(401, """{"detail":{"status":"quota_exceeded"}}""").contains("ліміт"))
        assertTrue(VoiceClient.errorMessage(401, """{"detail":{"status":"invalid_api_key"}}""").contains("не підходить"))
        assertTrue(VoiceClient.errorMessage(404, "").contains("Голос"))
    }

    @Test
    fun readsXmlSubtitlesAndPlayerAnswer() {
        val classic = """<transcript><text start="1.5" dur="2">Hello &amp;amp; welcome</text><text start="4" dur="1">It&amp;#39;s <i>fine</i></text></transcript>"""
        val lines = YouTubeCaptions.parseXml(classic)
        assertEquals(2, lines.size)
        assertEquals(1500L, lines[0].startMs)
        assertEquals("Hello & welcome", lines[0].text)
        assertEquals("It's fine", lines[1].text)

        val modern = """<timedtext><body><p t="2000" d="900">First line</p><p t="3000" d="500"></p></body></timedtext>"""
        val modernLines = YouTubeCaptions.parseXml(modern)
        assertEquals(1, modernLines.size)
        assertEquals(2000L, modernLines[0].startMs)

        val answer = """{"captions":{"playerCaptionsTracklistRenderer":{"captionTracks":[{"baseUrl":"https://www.youtube.com/api/timedtext?v=1&lang=en","languageCode":"en","kind":"asr"}]}}}"""
        assertEquals("https://www.youtube.com/api/timedtext?v=1&lang=en", YouTubeCaptions.playerResponseTrackUrl(answer))
        assertNull(YouTubeCaptions.playerResponseTrackUrl("""{"playabilityStatus":{"status":"ERROR"}}"""))
    }

    @Test
    fun readsAiTranscript() {
        val lines = AiClient.parseTranscript("""[{"t": 0.5, "text": "Hi there."}, {"t": 3, "text": " Second one "}, {"t": 5, "text": ""}]""")
        assertEquals(2, lines.size)
        assertEquals(500L, lines[0].startMs)
        assertEquals("Second one", lines[1].text)
        assertTrue(AiClient.parseTranscript("no transcript").isEmpty())
    }

    @Test
    fun readsChatReply() {
        val response = """{"candidates":[{"content":{"parts":[{"text":"Hi! How are you?"}]}}]}"""
        assertEquals("Hi! How are you?", AiClient.extractText(response))
        assertTrue(AiClient.chatInstruction(listOf("apple", "bread")).contains("[apple, bread]"))
    }
}
