package com.fraid.englishcards.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Deck
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A card that is answered by swiping: right is "know" (green), left is "don't know" (red).
 * It sits on two tilted sheets so it looks like a deck.
 */
@Composable
fun SwipeCard(
    onSwiped: (right: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var forced by remember { mutableStateOf(0) }
    var answered by remember { mutableStateOf(false) }

    val widthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val threshold = widthPx * 0.28f

    fun fling(right: Boolean) {
        if (answered) return
        answered = true
        forced = if (right) 1 else -1
        scope.launch {
            offsetX.animateTo(
                targetValue = if (right) widthPx * 1.4f else -widthPx * 1.4f,
                animationSpec = tween(durationMillis = 260),
            )
            onSwiped(right)
        }
    }

    val progress = if (forced != 0) forced.toFloat()
    else (offsetX.value / threshold).coerceIn(-1f, 1f)
    val tint = if (progress >= 0f) KnownGreen else UnknownRed
    val container = lerp(MaterialTheme.colorScheme.surface, tint, abs(progress) * 0.9f)
    val contentColor = if (abs(progress) > 0.45f) AccentForeground else MaterialTheme.colorScheme.onSurface
    val shape = MaterialTheme.shapes.extraLarge
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Box(modifier = modifier.padding(vertical = 14.dp, horizontal = 6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .graphicsLayer { rotationZ = -2.5f },
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = border,
        ) {}
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .graphicsLayer { rotationZ = 2.5f },
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = border,
        ) {}
        Surface(
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
            shape = shape,
            color = container,
            contentColor = contentColor,
            border = border,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                content = content,
            )
        }
    }
}

/** Goes through a ready-made set card by card: left adds the word to study, right marks it as known. */
@Composable
fun DeckSortScreen(
    deck: Deck,
    cards: List<Card>,
    onSorted: (card: Card, known: Boolean) -> Unit,
    onAddAll: (List<Card>) -> Unit,
    onBack: () -> Unit,
) {
    // The words of the set the user does not have yet, fixed when the screen opens.
    val queue = remember(deck.id) {
        val existing = cards.map { it.word.lowercase() }.toSet()
        deck.cards().filter { it.word.lowercase() !in existing }
    }
    var index by remember(deck.id) { mutableStateOf(0) }
    var toStudy by remember(deck.id) { mutableStateOf(0) }
    var known by remember(deck.id) { mutableStateOf(0) }

    Scaffold(topBar = { AppTopBar(title = deck.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            val card = queue.getOrNull(index)
            if (card == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        text = if (queue.isEmpty()) "Усі слова цього набору вже у тебе" else "Набір розібрано",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    if (queue.isNotEmpty()) {
                        Text("У навчання: $toStudy", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Уже знаю: $known", style = MaterialTheme.typography.titleLarge, color = KnownGreen)
                    }
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) { Text("Готово") }
                }
            } else {
                Text(
                    text = "Слово ${index + 1} з ${queue.size} · у навчання: $toStudy · знаю: $known",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { index.toFloat() / queue.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                key(index) {
                    SwipeCard(
                        onSwiped = { right ->
                            onSorted(card, right)
                            if (right) known += 1 else toStudy += 1
                            index += 1
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        CardAvatar(card = card, size = 96.dp)
                        Text(
                            text = card.word,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = card.translation,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        if (card.example.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = card.example,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("← не знаю, вчити", style = MaterialTheme.typography.labelLarge, color = UnknownRed)
                    Text("уже знаю →", style = MaterialTheme.typography.labelLarge, color = KnownGreen)
                }
                OutlinedButton(
                    onClick = {
                        val rest = queue.drop(index)
                        onAddAll(rest)
                        toStudy += rest.size
                        index = queue.size
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) { Text("Додати решту в навчання без розбору") }
            }
        }
    }
}
