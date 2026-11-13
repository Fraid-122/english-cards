package com.fraid.englishcards.data

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** A speech problem with a message that can be shown to the user. */
class VoiceException(message: String) : Exception(message)

/**
 * High-quality speech from ElevenLabs with the user's own API key.
 * Every phrase is generated once and kept as an mp3 file on the phone, so
 * repeating it costs no credits and works offline.
 */
object VoiceClient {

    /** "George", the voice used in the ElevenLabs quick-start examples. */
    const val DEFAULT_VOICE = "JBFqnCBsd6RMkjVDRZzb"
    const val DEFAULT_MODEL = "eleven_multilingual_v2"

    /** The file with the spoken [text]; generated on first use. Call off the main thread. */
    fun fileFor(dir: File, apiKey: String, voiceId: String, model: String, text: String): File {
        val voice = voiceId.trim().ifBlank { DEFAULT_VOICE }
        val modelId = model.trim().ifBlank { DEFAULT_MODEL }
        dir.mkdirs()
        val file = File(dir, cacheName(voice, modelId, text))
        if (file.exists() && file.length() > 0) return file

        val audio = synthesize(apiKey, voice, modelId, text.trim())
        val tmp = File(dir, file.name + ".tmp")
        tmp.writeBytes(audio)
        if (!tmp.renameTo(file)) {
            file.writeBytes(audio)
            tmp.delete()
        }
        return file
    }

    /** The same phrase with the same voice and model always maps to the same file. */
    fun cacheName(voiceId: String, model: String, text: String): String {
        val key = voiceId.trim() + "|" + model.trim() + "|" + text.trim().lowercase()
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(40) + ".mp3"
    }

    private fun synthesize(apiKey: String, voiceId: String, model: String, text: String): ByteArray {
        if (apiKey.isBlank()) throw VoiceException("Додай ключ ElevenLabs у налаштуваннях.")
        val body = buildJsonObject {
            put("text", text)
            put("model_id", model)
        }.toString()

        val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=mp3_44100_128")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "audio/mpeg")
            connection.setRequestProperty("xi-api-key", apiKey.trim())
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw VoiceException(errorMessage(code, error))
            }
            val audio = connection.inputStream.use { it.readBytes() }
            if (audio.isEmpty()) throw VoiceException("ElevenLabs повернув порожній звук.")
            return audio
        } catch (e: IOException) {
            throw VoiceException("Немає зв'язку з ElevenLabs. Перевір інтернет.")
        } finally {
            connection.disconnect()
        }
    }

    /** Turns an ElevenLabs error response into a short message for the user. */
    fun errorMessage(code: Int, body: String): String = when {
        "quota_exceeded" in body -> "Вичерпано ліміт символів ElevenLabs у твоїй підписці."
        "missing_permissions" in body -> "Ключ ElevenLabs не має дозволу на Text to Speech."
        "voice_not_found" in body || code == 404 -> "Голос ElevenLabs не знайдено. Перевір ID голосу."
        "model" in body && code == 400 -> "Модель ElevenLabs не підходить. Перевір назву моделі."
        code == 401 || code == 403 -> "Ключ ElevenLabs не підходить."
        code == 429 -> "Забагато запитів до ElevenLabs. Спробуй за хвилину."
        code in 500..599 -> "ElevenLabs тимчасово недоступний."
        else -> "Помилка ElevenLabs (код $code)."
    }
}
