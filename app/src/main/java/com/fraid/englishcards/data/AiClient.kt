package com.fraid.englishcards.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** What the AI suggests for a new card. */
data class AiSuggestion(
    val word: String,
    val translation: String,
    val example: String,
    val topic: String,
    val difficulty: Difficulty,
    val emoji: String,
)

/** One message of the conversation with the tutor. */
data class ChatMessage(val fromUser: Boolean, val text: String)

/** An AI problem with a message that can be shown to the user. */
class AiException(message: String) : Exception(message)

/** Talks to the Google Gemini API with the user's own free API key. */
object AiClient {

    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun suggest(
        apiKey: String,
        model: String,
        text: String,
        topics: List<String>,
    ): AiSuggestion = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", buildPrompt(text, topics)) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
        }.toString()
        parseResponse(post(apiKey, model, body))
    }

    /** Continues a conversation with the tutor; returns the tutor's next message. */
    suspend fun chat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        words: List<String>,
    ): String = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    addJsonObject { put("text", chatInstruction(words)) }
                }
            }
            putJsonArray("contents") {
                messages.forEach { message ->
                    addJsonObject {
                        put("role", if (message.fromUser) "user" else "model")
                        putJsonArray("parts") {
                            addJsonObject { put("text", message.text) }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.7)
            }
        }.toString()
        extractText(post(apiKey, model, body))?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw AiException("ШІ не повернув відповідь. Спробуй ще раз.")
    }

    fun chatInstruction(words: List<String>): String {
        val list = words.filter { it.isNotBlank() }.distinct().take(25).joinToString(", ")
        return """
            You are a friendly English conversation partner for a Ukrainian speaker who is learning English.
            Reply only in simple English, in one to three short sentences, and end with a question to keep the talk going.
            If the learner makes a mistake, first show the corrected sentence in one line starting with "Better:", then continue.
            When it fits naturally, use some of the words the learner is studying: [$list].
        """.trimIndent()
    }

    /** Sends the request and returns the raw response; throws [AiException] with a readable message. */
    private fun post(apiKey: String, model: String, body: String): String {
        if (apiKey.isBlank()) throw AiException("Додай API-ключ у налаштуваннях ШІ.")
        val modelId = model.trim().ifBlank { DEFAULT_MODEL }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("x-goog-api-key", apiKey.trim())
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw AiException(errorMessage(code))
            return response
        } catch (e: IOException) {
            throw AiException("Немає зв'язку з ШІ. Перевір інтернет.")
        } finally {
            connection.disconnect()
        }
    }

    fun buildPrompt(text: String, topics: List<String>): String {
        val input = text.trim().replace('"', '\'')
        val language = if (LanguageDetector.isUkrainian(input)) "Ukrainian" else "English"
        val existing = topics.filter { it.isNotBlank() }.distinct().take(30)
            .joinToString(", ") { it.replace('"', '\'') }
        return """
            You create English flashcards for a Ukrainian speaker.
            The user typed: "$input" (language: $language).
            If it is English, translate it to Ukrainian. If it is Ukrainian, translate it to English.
            Return ONLY a JSON object with these string fields:
            "word": the English word or phrase in dictionary form, lowercase unless it is a proper noun.
            "translation": the Ukrainian translation, the one or two most common meanings separated by a comma.
            "example": one short, simple English sentence that uses the word.
            "topic": a short topic name in Ukrainian, one or two words, starting with a capital letter. Use one of the existing topics when it fits: [$existing].
            "difficulty": "EASY" for CEFR A1-A2, "MEDIUM" for B1-B2, "HARD" for C1-C2.
            "emoji": exactly one emoji that best pictures the meaning.
        """.trimIndent()
    }

    /** The text the model wrote, taken out of the Gemini API response envelope. */
    fun extractText(response: String): String? =
        try {
            json.parseToJsonElement(response).jsonObject["candidates"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray
                ?.map { it.jsonObject }
                ?.filter { it["thought"]?.jsonPrimitive?.booleanOrNull != true }
                ?.mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull }
                ?.joinToString("")
        } catch (e: IllegalArgumentException) {
            null
        }

    /** Reads the Gemini API response envelope and the card suggestion inside it. */
    fun parseResponse(response: String): AiSuggestion {
        val text = extractText(response)
        if (text.isNullOrBlank()) throw AiException("ШІ не повернув відповідь. Спробуй ще раз.")
        return parseSuggestion(text)
    }

    /** Reads the JSON object the model wrote, tolerating code fences or extra text around it. */
    fun parseSuggestion(text: String): AiSuggestion {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) throw AiException("ШІ повернув незрозумілу відповідь.")
        try {
            val obj = json.parseToJsonElement(text.substring(start, end + 1)).jsonObject
            fun field(name: String): String =
                obj[name]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

            val difficulty = Difficulty.entries
                .firstOrNull { it.name == field("difficulty").uppercase() }
                ?: Difficulty.EASY
            return AiSuggestion(
                word = field("word"),
                translation = field("translation"),
                example = field("example"),
                topic = field("topic"),
                difficulty = difficulty,
                emoji = field("emoji"),
            )
        } catch (e: IllegalArgumentException) {
            throw AiException("ШІ повернув незрозумілу відповідь.")
        }
    }

    private fun errorMessage(code: Int): String = when (code) {
        400, 401, 403 -> "API-ключ не підходить. Перевір його в налаштуваннях ШІ."
        404 -> "Модель ШІ не знайдено. Перевір назву моделі в налаштуваннях."
        429 -> "Вичерпано безкоштовний ліміт запитів. Спробуй пізніше."
        in 500..599 -> "Сервіс ШІ тимчасово недоступний. Спробуй пізніше."
        else -> "Помилка ШІ (код $code)."
    }
}
