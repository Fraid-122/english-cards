package com.fraid.englishcards.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Best-effort loading of a video's title and English subtitles (hand-made or auto-generated).
 *
 * YouTube has no official way to read the subtitles of other people's videos,
 * so this asks YouTube the way its own apps do. It can stop working when YouTube
 * changes something; the app then still plays the video and offers other ways.
 */
object YouTubeCaptions {

    /** [note] says why there are no lines, for showing to the user. */
    class Result(val title: String, val lines: List<VideoLine>, val note: String = "")

    private val json = Json { ignoreUnknownKeys = true }

    private const val BROWSER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
    private const val APP_VERSION = "20.10.38"
    private const val APP_AGENT = "com.google.android.youtube/$APP_VERSION (Linux; U; Android 14)"

    suspend fun load(videoId: String): Result = withContext(Dispatchers.IO) {
        val html = runCatching { get("https://www.youtube.com/watch?v=$videoId&hl=en", BROWSER_AGENT) }
            .getOrElse { return@withContext Result("", emptyList(), "немає зв'язку з YouTube") }
        val title = parseTitle(html)

        // 1. Ask the player service the way the mobile app does.
        var foundTrack = false
        val apiKey = Regex("\"INNERTUBE_API_KEY\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
        if (apiKey != null) {
            val trackUrl = runCatching { playerTrackUrl(videoId, apiKey) }.getOrNull()
            if (trackUrl != null) {
                foundTrack = true
                val lines = fetchLines(trackUrl)
                if (lines.isNotEmpty()) return@withContext Result(title, lines)
            }
        }

        // 2. Fall back to the track list embedded in the watch page.
        val pageTrackUrl = captionTrackUrl(html)
        if (pageTrackUrl != null) {
            foundTrack = true
            val lines = fetchLines(pageTrackUrl)
            if (lines.isNotEmpty()) return@withContext Result(title, lines)
        }

        Result(
            title,
            emptyList(),
            if (foundTrack) "YouTube не віддав текст субтитрів" else "у цього відео немає субтитрів",
        )
    }

    private fun fetchLines(trackUrl: String): List<VideoLine> {
        val base = trackUrl.replace(Regex("&fmt=[^&]*"), "")
        val asJson = runCatching { parseJson3(get("$base&fmt=json3", BROWSER_AGENT)) }.getOrDefault(emptyList())
        if (asJson.isNotEmpty()) return asJson
        return runCatching { parseXml(get(base, BROWSER_AGENT)) }.getOrDefault(emptyList())
    }

    private fun playerTrackUrl(videoId: String, apiKey: String): String? {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID")
                    put("clientVersion", APP_VERSION)
                }
            }
            put("videoId", videoId)
        }.toString()
        val response = post("https://www.youtube.com/youtubei/v1/player?key=$apiKey", body)
        return playerResponseTrackUrl(response)
    }

    /** Finds the English subtitle track in the player service's answer. */
    fun playerResponseTrackUrl(response: String): String? =
        try {
            val tracks = json.parseToJsonElement(response).jsonObject["captions"]
                ?.jsonObject?.get("playerCaptionsTracklistRenderer")
                ?.jsonObject?.get("captionTracks")
                ?.jsonArray?.map { it.jsonObject }.orEmpty()
            pickTrack(tracks)
        } catch (e: IllegalArgumentException) {
            null
        }

    /** Finds the English subtitle track on the watch page, preferring hand-made subtitles. */
    fun captionTrackUrl(html: String): String? {
        val key = html.indexOf("\"captionTracks\":")
        if (key < 0) return null
        val start = html.indexOf('[', key)
        if (start < 0) return null
        val end = matchingBracket(html, start) ?: return null
        return try {
            pickTrack(json.parseToJsonElement(html.substring(start, end + 1)).jsonArray.map { it.jsonObject })
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun pickTrack(tracks: List<JsonObject>): String? {
        fun field(track: JsonObject, name: String): String =
            track[name]?.jsonPrimitive?.contentOrNull.orEmpty()

        val english = tracks.filter { field(it, "languageCode").startsWith("en") }
        val best = english.firstOrNull { field(it, "kind") != "asr" }
            ?: english.firstOrNull()
            ?: tracks.firstOrNull()
        return best?.let { field(it, "baseUrl") }?.takeIf { it.startsWith("http") }
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

    private val xmlSeconds = Regex("<text start=\"([0-9.]+)\"[^>]*>(.*?)</text>", RegexOption.DOT_MATCHES_ALL)
    private val xmlMillis = Regex("<p t=\"([0-9]+)\"[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
    private val xmlTag = Regex("<[^>]+>")

    /** Reads subtitles in YouTube's XML formats. */
    fun parseXml(text: String): List<VideoLine> {
        val bySeconds = xmlSeconds.findAll(text).mapNotNull { match ->
            val start = match.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
            cleanXml(match.groupValues[2]).takeIf { it.isNotEmpty() }?.let { VideoLine((start * 1000).toLong(), it) }
        }.toList()
        if (bySeconds.isNotEmpty()) return bySeconds
        return xmlMillis.findAll(text).mapNotNull { match ->
            val start = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            cleanXml(match.groupValues[2]).takeIf { it.isNotEmpty() }?.let { VideoLine(start, it) }
        }.toList()
    }

    private fun cleanXml(raw: String): String = unescape(unescape(raw.replace(xmlTag, "")))
        .replace('\n', ' ')
        .trim()

    private fun unescape(text: String): String = text
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

    fun parseTitle(html: String): String {
        val start = html.indexOf("<title>")
        val end = html.indexOf("</title>")
        if (start < 0 || end <= start) return ""
        return unescape(html.substring(start + 7, end).removeSuffix(" - YouTube")).trim()
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

    private fun get(address: String, agent: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", agent)
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.setRequestProperty("Cookie", "CONSENT=YES+1; SOCS=CAI")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun post(address: String, body: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", APP_AGENT)
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
