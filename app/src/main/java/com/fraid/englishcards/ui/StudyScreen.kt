package com.fraid.englishcards.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.AnswerChecker
import com.fraid.englishcards.data.Card
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StudyScreen(
    queue: List<Card>,
    session: Int,
    index: Int,
    knownCount: Int,
    missed: List<Card>,
    onAnswer: (Card, Boolean) -> Unit,
    onRepeatMissed: () -> Unit,
    onExit: () -> Unit,
) {
    Scaffold(
        topBar = { AppTopBar(title = "Навчання", onBack = onExit) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(16.dp),
        ) {
            val card = queue.getOrNull(index)
            if (card == null) {
                StudySummary(
                    knownCount = knownCount,
                    missedCount = missed.size,
                    onRepeatMissed = onRepeatMissed,
                    onExit = onExit,
                )
            } else {
                Text(
                    text = "Картка ${index + 1} з ${queue.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { index.toFloat() / queue.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                // A fresh state (position, typed text) for every card of every session.
                key(session, card.id) {
                    StudyCard(
                        card = card,
                        onAnswer = { known -> onAnswer(card, known) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyCard(
    card: Card,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var revealed by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    // 0 = follow the finger, 1 = answered "know" (green), -1 = answered "don't know" (red).
    var forced by remember { mutableStateOf(0) }
    var answered by remember { mutableStateOf(false) }

    val widthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val threshold = widthPx * 0.28f

    fun fling(right: Boolean, pauseMillis: Long = 0L) {
        if (answered) return
        answered = true
        forced = if (right) 1 else -1
        scope.launch {
            if (pauseMillis > 0) delay(pauseMillis)
            offsetX.animateTo(
                targetValue = if (right) widthPx * 1.4f else -widthPx * 1.4f,
                animationSpec = tween(durationMillis = 280),
            )
            onAnswer(right)
        }
    }

    val progress = if (forced != 0) forced.toFloat()
    else (offsetX.value / threshold).coerceIn(-1f, 1f)
    val tint = if (progress >= 0f) KnownGreen else UnknownRed
    val container = lerp(MaterialTheme.colorScheme.surfaceVariant, tint, abs(progress) * 0.9f)
    val content = if (abs(progress) > 0.45f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 14.dp),
        ) {
            Surface(
                onClick = { if (!answered) revealed = !revealed },
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .graphicsLayer { rotationZ = offsetX.value / widthPx * 12f }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    answered -> Unit
                                    offsetX.value > threshold -> fling(right = true)
                                    offsetX.value < -threshold -> fling(right = false)
                                    else -> scope.launch { offsetX.animateTo(0f) }
                                }
                            },
                            onDragCancel = {
                                if (!answered) scope.launch { offsetX.animateTo(0f) }
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            if (!answered) {
                                scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                            }
                        }
                    },
                shape = RoundedCornerShape(28.dp),
                color = container,
                contentColor = content,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    CardAvatar(card = card, size = 88.dp)
                    Text(
                        text = card.translation,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${card.topic} · ${card.difficulty.label}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (revealed || forced != 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = card.word,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (card.example.isNotBlank()) {
                            Text(
                                text = card.example,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Text(
                            text = "Торкнись, щоб побачити відповідь",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "← не знаю      знаю →",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        val typed = input.isNotBlank()
        val lettersOk = AnswerChecker.isPrefixOk(input, card.word)
        OutlinedTextField(
            value = input,
            onValueChange = { text ->
                if (!answered) {
                    input = text
                    if (AnswerChecker.isCorrect(text, card.word)) {
                        fling(right = true, pauseMillis = 350L)
                    }
                }
            },
            label = { Text("Впиши слово англійською") },
            supportingText = {
                when {
                    !typed -> Text("Правильне слово саме перенесе картку вправо")
                    lettersOk -> Text("Поки все правильно", color = KnownGreen)
                    else -> Text("Є помилка в літерах")
                }
            },
            isError = typed && !lettersOk,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { fling(right = false) },
                colors = ButtonDefaults.buttonColors(containerColor = UnknownRed, contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) { Text("Не знаю") }
            Button(
                onClick = { fling(right = true) },
                colors = ButtonDefaults.buttonColors(containerColor = KnownGreen, contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) { Text("Знаю") }
        }
    }
}

@Composable
private fun StudySummary(
    knownCount: Int,
    missedCount: Int,
    onRepeatMissed: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Сесію завершено",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Знаю: $knownCount",
            style = MaterialTheme.typography.titleLarge,
            color = KnownGreen,
        )
        Text(
            text = "Не знаю: $missedCount",
            style = MaterialTheme.typography.titleLarge,
            color = UnknownRed,
        )
        if (missedCount > 0) {
            Button(
                onClick = onRepeatMissed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) { Text("Повторити ті, що не знаю") }
        }
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) { Text("На головну") }
    }
}
