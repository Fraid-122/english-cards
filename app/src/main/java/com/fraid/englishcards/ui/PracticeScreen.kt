@file:OptIn(ExperimentalMaterial3Api::class)

package com.fraid.englishcards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.AnswerChecker
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.TextTools

enum class ExerciseMode(val title: String) {
    GAP("Встав слово"),
    LISTEN("Слухання"),
}

@Composable
fun PracticeScreen(
    cards: List<Card>,
    aiEnabled: Boolean,
    onExercise: (ExerciseMode) -> Unit,
    onChat: () -> Unit,
) {
    val gapCount = cards.count { TextTools.gapSentence(it.example, it.word) != null }
    Scaffold(topBar = { AppTopBar(title = "Практика") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HubRow(
                emoji = "✍️",
                title = "Встав слово",
                subtitle = if (gapCount > 0) "Речення з пропуском · підходить карток: $gapCount"
                else "Потрібні картки з прикладом речення",
                onClick = { if (gapCount > 0) onExercise(ExerciseMode.GAP) },
            )
            HubRow(
                emoji = "🎧",
                title = "Слухання",
                subtitle = if (cards.isNotEmpty()) "Послухай слово і впиши його"
                else "Спершу додай картки",
                onClick = { if (cards.isNotEmpty()) onExercise(ExerciseMode.LISTEN) },
            )
            HubRow(
                emoji = "💬",
                title = "Розмова з ШІ",
                subtitle = if (aiEnabled) "Чат англійською на твоїх словах"
                else "Потрібен API-ключ у вкладці «Налаштування»",
                onClick = onChat,
            )
            Text(
                text = "Вправи не змінюють розклад повторень, це просто тренування.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private const val QUESTIONS_PER_ROUND = 10

@Composable
fun ExerciseScreen(
    cards: List<Card>,
    mode: ExerciseMode,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
) {
    // A new round is picked when `round` changes.
    var round by remember { mutableStateOf(0) }
    val questions = remember(round) {
        cards
            .filter { mode == ExerciseMode.LISTEN || TextTools.gapSentence(it.example, it.word) != null }
            .shuffled()
            .take(QUESTIONS_PER_ROUND)
    }
    var index by remember(round) { mutableStateOf(0) }
    var correct by remember(round) { mutableStateOf(0) }
    var input by remember(round, index) { mutableStateOf("") }
    // null = not checked yet, true / false = the result.
    var result by remember(round, index) { mutableStateOf<Boolean?>(null) }

    val card = questions.getOrNull(index)

    if (mode == ExerciseMode.LISTEN && card != null) {
        LaunchedEffect(round, index) { onSpeak(card.word) }
    }

    Scaffold(topBar = { AppTopBar(title = mode.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (card == null) {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = if (questions.isEmpty()) "Немає карток для цієї вправи." else "Готово!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (questions.isNotEmpty()) {
                    Text(
                        text = "Правильно: $correct з ${questions.size}",
                        style = MaterialTheme.typography.titleLarge,
                        color = KnownGreen,
                    )
                    Button(
                        onClick = { round += 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) { Text("Ще раз") }
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) { Text("Назад") }
            } else {
                Text(
                    text = "Питання ${index + 1} з ${questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                LinearProgressIndicator(
                    progress = { index.toFloat() / questions.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                if (mode == ExerciseMode.GAP) {
                    Text(
                        text = TextTools.gapSentence(card.example, card.word) ?: card.example,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Підказка: ${card.translation}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    FilledTonalIconButton(
                        onClick = { onSpeak(card.word) },
                        modifier = Modifier.size(88.dp),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Послухати ще раз",
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    Text(
                        text = "Послухай і впиши слово англійською",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { if (result == null) input = it },
                    label = { Text("Твоя відповідь") },
                    singleLine = true,
                    isError = result == false,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                val checked = result
                if (checked == null) {
                    Button(
                        onClick = {
                            val ok = AnswerChecker.isCorrect(input, card.word)
                            result = ok
                            if (ok) correct += 1
                        },
                        enabled = input.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) { Text("Перевірити") }
                    OutlinedButton(
                        onClick = { result = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) { Text("Не знаю") }
                } else {
                    Text(
                        text = if (checked) "Правильно!" else "Правильно: ${card.word}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (checked) KnownGreen else UnknownRed,
                        textAlign = TextAlign.Center,
                    )
                    if (mode == ExerciseMode.LISTEN) {
                        Text(
                            text = card.translation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { index += 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) { Text("Далі") }
                }
            }
        }
    }
}
