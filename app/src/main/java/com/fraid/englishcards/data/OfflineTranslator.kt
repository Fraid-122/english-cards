package com.fraid.englishcards.data

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A translation problem with a message that can be shown to the user. */
class TranslateException(message: String) : Exception(message)

/**
 * Translates on the phone with Google ML Kit: free, no key, no limits.
 * The language pack is downloaded once (internet needed), then it works offline.
 */
class OfflineTranslator {

    private var englishToUkrainian: Translator? = null
    private var ukrainianToEnglish: Translator? = null

    /** English text becomes Ukrainian; Ukrainian text becomes English. */
    suspend fun translate(text: String): String {
        val source = text.trim()
        if (source.isEmpty()) return ""
        try {
            val translator = translatorFor(LanguageDetector.isUkrainian(source))
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            val translated = translator.translate(source).await().orEmpty()
            if (translated.isBlank()) throw TranslateException("Перекладач повернув порожню відповідь.")
            return TranslationText.tidy(source, translated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TranslateException) {
            throw e
        } catch (e: Exception) {
            val detail = e.message?.take(140)?.let { " ($it)" }.orEmpty()
            throw TranslateException(
                "Офлайн-переклад не спрацював. Уперше потрібен інтернет, щоб завантажити мовний пакет.$detail"
            )
        }
    }

    fun close() {
        englishToUkrainian?.close()
        ukrainianToEnglish?.close()
        englishToUkrainian = null
        ukrainianToEnglish = null
    }

    private fun translatorFor(fromUkrainian: Boolean): Translator =
        if (fromUkrainian) {
            ukrainianToEnglish ?: create(TranslateLanguage.UKRAINIAN, TranslateLanguage.ENGLISH)
                .also { ukrainianToEnglish = it }
        } else {
            englishToUkrainian ?: create(TranslateLanguage.ENGLISH, TranslateLanguage.UKRAINIAN)
                .also { englishToUkrainian = it }
        }

    private fun create(from: String, to: String): Translator =
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
        )

    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }
}
