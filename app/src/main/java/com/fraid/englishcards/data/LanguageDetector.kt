package com.fraid.englishcards.data

/** Decides which way to translate: Ukrainian text goes to English, anything else to Ukrainian. */
object LanguageDetector {

    fun isUkrainian(text: String): Boolean =
        text.any { it in 'А'..'я' || it in "ІіЇїЄєҐґЁё" }
}
