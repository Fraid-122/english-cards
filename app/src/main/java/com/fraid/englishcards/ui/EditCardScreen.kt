@file:OptIn(ExperimentalMaterial3Api::class)

package com.fraid.englishcards.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.AiSuggestion
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Difficulty
import com.fraid.englishcards.data.LanguageDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EditCardScreen(
    existing: Card?,
    topics: List<String>,
    aiEnabled: Boolean,
    onTranslate: suspend (String) -> String,
    onSuggest: suspend (String) -> AiSuggestion,
    onSave: (Card) -> Unit,
    onBack: () -> Unit,
) {
    var word by rememberSaveable { mutableStateOf(existing?.word ?: "") }
    var translation by rememberSaveable { mutableStateOf(existing?.translation ?: "") }
    var example by rememberSaveable { mutableStateOf(existing?.example ?: "") }
    var topic by rememberSaveable { mutableStateOf(existing?.topic ?: "") }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "") }
    var difficultyName by rememberSaveable {
        mutableStateOf((existing?.difficulty ?: Difficulty.EASY).name)
    }
    var known by rememberSaveable { mutableStateOf(existing?.known ?: false) }

    // Fields the user filled by hand are kept when the word changes;
    // everything generated automatically is erased and generated again.
    var exampleManual by rememberSaveable { mutableStateOf(false) }
    var topicManual by rememberSaveable { mutableStateOf(false) }
    var emojiManual by rememberSaveable { mutableStateOf(false) }
    var difficultyManual by rememberSaveable { mutableStateOf(false) }

    // True when the user is typing Ukrainian into the translation field
    // and the English word is the generated side.
    var sourceIsTranslation by remember { mutableStateOf(false) }

    // A new request (id + text) starts a new generation after typing pauses.
    var requestId by remember { mutableStateOf(0) }
    var requestText by remember { mutableStateOf("") }

    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val difficulty = Difficulty.valueOf(difficultyName)
    val canSave = word.isNotBlank() && translation.isNotBlank()

    fun clearGenerated() {
        if (!exampleManual) example = ""
        if (!topicManual) topic = ""
        if (!emojiManual) emoji = ""
        if (!difficultyManual) difficultyName = Difficulty.EASY.name
        error = null
    }

    fun requestGeneration(text: String) {
        requestText = text
        requestId += 1
    }

    /** Translates [source] offline, then asks the AI (when enabled) for the rest. */
    suspend fun generate(source: String) {
        error = null
        status = "Перекладаю…"
        try {
            val fromUkrainian = LanguageDetector.isUkrainian(source)

            // 1. Offline translation.
            var translated: String? = null
            var translateError: String? = null
            try {
                coroutineScope {
                    val slowHint = launch {
                        delay(2500)
                        status = "Завантажую мовний пакет (лише вперше, потрібен інтернет)…"
                    }
                    try {
                        translated = onTranslate(source)
                    } finally {
                        slowHint.cancel()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                translateError = e.message ?: "Не вдалося перекласти."
            }

            val result = translated
            if (result != null) {
                when {
                    fromUkrainian && sourceIsTranslation -> word = result
                    fromUkrainian -> {
                        // Ukrainian typed into the word field: move it to the translation.
                        translation = source
                        word = result
                    }
                    sourceIsTranslation -> {
                        // English typed into the translation field: move it to the word.
                        word = source
                        translation = result
                        sourceIsTranslation = false
                    }
                    else -> translation = result
                }
            }

            // 2. AI: picture, topic, difficulty, example (and the translation if offline failed).
            if (aiEnabled) {
                status = "ШІ підбирає картинку й тему…"
                try {
                    val englishWord = if (fromUkrainian && result != null) result else source
                    val suggestion = onSuggest(englishWord)
                    if (result == null) {
                        if (fromUkrainian) {
                            if (!sourceIsTranslation) translation = source
                            if (suggestion.word.isNotBlank()) word = suggestion.word
                        } else {
                            if (sourceIsTranslation) {
                                word = source
                                sourceIsTranslation = false
                            }
                            if (suggestion.translation.isNotBlank()) translation = suggestion.translation
                        }
                        if (word.isNotBlank() && translation.isNotBlank()) translateError = null
                    }
                    if (!emojiManual && suggestion.emoji.isNotBlank()) emoji = suggestion.emoji
                    if (!topicManual && suggestion.topic.isNotBlank()) topic = suggestion.topic
                    if (!exampleManual && suggestion.example.isNotBlank()) example = suggestion.example
                    if (!difficultyManual) difficultyName = suggestion.difficulty.name
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val aiMessage = e.message ?: "Помилка ШІ."
                    translateError = listOfNotNull(translateError, aiMessage).joinToString("\n")
                }
            }
            error = translateError
        } finally {
            status = null
        }
    }

    // Typing has paused: translate and generate.
    LaunchedEffect(requestId) {
        if (requestId == 0) return@LaunchedEffect
        val source = requestText.trim()
        if (source.length < 2) return@LaunchedEffect
        delay(800)
        generate(source)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (existing == null) "Нова картка" else "Редагувати картку",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Впиши слово англійською або українською, і переклад з'явиться сам. " +
                    "Якщо змінити чи стерти слово, усе згенероване зітреться і створиться заново.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = word,
                onValueChange = { text ->
                    word = text
                    sourceIsTranslation = false
                    translation = ""
                    clearGenerated()
                    requestGeneration(text)
                },
                label = { Text("Слово (англійською або українською)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = translation,
                onValueChange = { text ->
                    translation = text
                    if (word.isBlank()) sourceIsTranslation = true
                    if (sourceIsTranslation) {
                        word = ""
                        clearGenerated()
                        requestGeneration(text)
                    }
                },
                label = { Text("Переклад") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier.fillMaxWidth(),
            )

            val currentStatus = status
            if (currentStatus != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(currentStatus, style = MaterialTheme.typography.bodySmall)
                }
            }
            val currentError = error
            if (currentError != null) {
                Text(
                    text = currentError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(
                onClick = {
                    val source = if (sourceIsTranslation) translation else word
                    exampleManual = false
                    topicManual = false
                    emojiManual = false
                    difficultyManual = false
                    if (sourceIsTranslation) word = "" else translation = ""
                    clearGenerated()
                    requestGeneration(source)
                },
                enabled = status == null && (word.isNotBlank() || translation.isNotBlank()),
            ) {
                Text("Згенерувати заново")
            }
            if (!aiEnabled) {
                Text(
                    text = "Картинку, тему й приклад підбирає ШІ. Щоб увімкнути його, додай " +
                        "API-ключ на головному екрані: «Увімкнути ШІ (картинки й теми)».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = emoji,
                onValueChange = { text ->
                    emoji = text
                    emojiManual = text.isNotBlank()
                },
                label = { Text("Картинка-емодзі") },
                supportingText = { Text("ШІ підбирає її сам, але можна вписати свою.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { text ->
                    topic = text
                    topicManual = text.isNotBlank()
                },
                label = { Text("Тема") },
                placeholder = { Text(Card.DEFAULT_TOPIC) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (topics.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    topics.forEach { name ->
                        SuggestionChip(
                            onClick = {
                                topic = name
                                topicManual = true
                            },
                            label = { Text(name) },
                        )
                    }
                }
            }

            Text("Складність", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = {
                            difficultyName = level.name
                            difficultyManual = true
                        },
                        label = { Text(level.label) },
                    )
                }
            }

            OutlinedTextField(
                value = example,
                onValueChange = { text ->
                    example = text
                    exampleManual = text.isNotBlank()
                },
                label = { Text("Приклад речення (необов'язково)") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (existing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Я знаю цю картку", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = known, onCheckedChange = { known = it })
                }
            }

            Button(
                onClick = {
                    val base = existing ?: Card(
                        id = UUID.randomUUID().toString(),
                        word = "",
                        translation = "",
                        createdAt = System.currentTimeMillis(),
                    )
                    onSave(
                        base.copy(
                            word = word.trim(),
                            translation = translation.trim(),
                            example = example.trim(),
                            topic = topic.trim().ifBlank { Card.DEFAULT_TOPIC },
                            difficulty = difficulty,
                            emoji = emoji.trim(),
                            known = known,
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("Зберегти", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
