package com.fraid.englishcards.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL

/**
 * Best-effort loading of a video's title and English subtitles.
 *
 * YouTube has no official way to read the subtitles of other people's videos,
 * so this reads the public watch page. It can stop working when YouTube changes
 * the page; the app then still plays the video and lets the user paste the text.
 */
object YouTubeCaptions {

    class Result(val title: String, val lines: List<VideoLine>)

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(videoId: String): Result = withContext(Dispatchers.IO) {
        val html = get("https://www.youtube.com/watch?v=$videoId&hl=en")
        val title = parseTitle(html)
        val trackUrl = captionTrackUrl(html)
        val lines = if (trackUrl == null) emptyList() else {
            runCatching { parseJson3(get("$trackUrl&fmt=json3")) }.getOrDefault(emptyList())
        }
        Result(title, lines)
    }

    /** Finds the English subtitle track on the watch page, preferring hand-made subtitles. */
    fun captionTrackUrl(html: String): String? {
        val key = html.indexOf("\"captionTracks\":")
        if (key < 0) return null
        val start = html.indexOf('[', key)
        if (start < 0) return null
        val end = matchingBracket(html, start) ?: return null
        return try {
            val tracks = json.parseToJsonElement(html.substring(start, end + 1)).jsonArray
                .map { it.jsonObject }
            fun field(track: kotlinx.serialization.json.JsonObject, name: String): String =
                track[name]?.jsonPrimitive?.contentOrNull.orEmpty()

            val english = tracks.filter { field(it, "languageCode").startsWith("en") }
            val best = english.firstOrNull { field(it, "kind") != "asr" }
                ?: english.firstOrNull()
                ?: tracks.firstOrNull()
            best?.let { field(it, "baseUrl") }?.takeIf { it.startsWith("http") }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Reads subtitles in YouTube's json3 format. */
    fun parseJson3(text: String): List<VideoLine> =
        try {
            json.parseToJsonElement(text).jsonObject["events"]?.jsonArray.orEmpty()
                .mapNotNull { event ->
                    val obj = event.jsonObject
                    val start = obj["tStartMs"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    val line = obj["segs"]?.jsonArray.orEmpty()
                        .joinToString("") { it.jsonObject["utf8"]?.jsonPrimitive?.contentOrNull.orEmpty() }
                        .replace('\n', ' ')
                        .trim()
                    if (line.isEmpty()) null else VideoLine(start, line)
                }
        } catch (e: IllegalArgumentException) {
            emptyList()
        }

    fun parseTitle(html: String): String {
        val start = html.indexOf("<title>")
        val end = html.indexOf("</title>")
        if (start < 0 || end <= start) return ""
        return html.substring(start + 7, end)
            .removeSuffix(" - YouTube")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }

    /** Index of the bracket that closes the one at [open], skipping brackets inside strings. */
    private fun matchingBracket(text: String, open: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in open until text.length) {
            val c = text[i]
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '[' -> depth++
                c == ']' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    private fun get(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
            )
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.setRequestProperty("Cookie", "CONSENT=YES+1; SOCS=CAI")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
