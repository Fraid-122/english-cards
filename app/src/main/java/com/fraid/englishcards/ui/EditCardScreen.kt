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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.fraid.englishcards.data.AiSuggestion
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Difficulty
import com.fraid.englishcards.data.LanguageDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EditCardScreen(
    existing: Card?,
    topics: List<String>,
    aiEnabled: Boolean,
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

    val difficulty = Difficulty.valueOf(difficultyName)
    val canSave = word.isNotBlank() && translation.isNotBlank()

    // The AI should not overwrite a difficulty the user picked by hand.
    var difficultyTouched by rememberSaveable { mutableStateOf(existing != null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    /**
     * Asks the AI about [source] and fills the card.
     * English text gets a Ukrainian translation; Ukrainian text gets the English word.
     * With [overwrite] off, only empty fields are filled.
     */
    suspend fun fillWithAi(source: String, overwrite: Boolean) {
        aiLoading = true
        aiError = null
        try {
            val suggestion = onSuggest(source)
            if (LanguageDetector.isUkrainian(source)) {
                translation = source
                if (suggestion.word.isNotBlank()) word = suggestion.word
            } else {
                word = source
                if (suggestion.translation.isNotBlank()) translation = suggestion.translation
            }
            if (suggestion.example.isNotBlank() && (overwrite || example.isBlank())) {
                example = suggestion.example
            }
            if (suggestion.topic.isNotBlank() && (overwrite || topic.isBlank())) {
                topic = suggestion.topic
            }
            if (suggestion.emoji.isNotBlank() && (overwrite || emoji.isBlank())) {
                emoji = suggestion.emoji
            }
            if (overwrite || !difficultyTouched) {
                difficultyName = suggestion.difficulty.name
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            aiError = e.message ?: "Помилка ШІ."
        } finally {
            aiLoading = false
        }
    }

    // Auto-translate: when only one side is filled and typing has paused, ask the AI.
    LaunchedEffect(word, translation, aiEnabled) {
        if (!aiEnabled) return@LaunchedEffect
        if (word.isBlank() == translation.isBlank()) return@LaunchedEffect
        val source = (if (word.isNotBlank()) word else translation).trim()
        if (source.length < 2) return@LaunchedEffect
        delay(1200)
        fillWithAi(source, overwrite = false)
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
            if (!aiEnabled) {
                Text(
                    text = "Автопереклад вимкнено. Додай API-ключ на головному екрані: «Увімкнути ШІ-переклад».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                label = { Text("Слово або фраза англійською") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = translation,
                onValueChange = { translation = it },
                label = { Text("Переклад українською") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (aiEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val source = (if (word.isNotBlank()) word else translation).trim()
                            scope.launch { fillWithAi(source, overwrite = true) }
                        },
                        enabled = !aiLoading && (word.isNotBlank() || translation.isNotBlank()),
                    ) {
                        Text("Заповнити через ШІ")
                    }
                    if (aiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("ШІ думає…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    text = "Впиши слово англійською або українською, і переклад, тема та картинка з'являться самі.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val error = aiError
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = example,
                onValueChange = { example = it },
                label = { Text("Приклад речення (необов'язково)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
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
                        SuggestionChip(onClick = { topic = name }, label = { Text(name) })
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
                            difficultyTouched = true
                        },
                        label = { Text(level.label) },
                    )
                }
            }

            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("Картинка-емодзі") },
                supportingText = { Text("ШІ підбирає її сам, але можна вписати свою.") },
                singleLine = true,
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
