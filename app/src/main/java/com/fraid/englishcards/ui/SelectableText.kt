package com.fraid.englishcards.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.TextSpan
import com.fraid.englishcards.data.TextTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Text in which the user can select a word, several words or a sentence:
 * a tap selects one word, a long press and drag selects a range.
 */
@Composable
fun SelectableText(
    text: String,
    selection: TextSpan?,
    onSelection: (TextSpan) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val select by rememberUpdatedState(onSelection)
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    val annotated = remember(text, selection, highlight) {
        buildAnnotatedString {
            append(text)
            if (selection != null) {
                val start = selection.start.coerceIn(0, text.length)
                val end = selection.end.coerceIn(start, text.length)
                addStyle(SpanStyle(background = highlight), start, end)
            }
        }
    }

    Text(
        text = annotated,
        style = style,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier
            .pointerInput(text) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    val span = TextTools.wordSpanAt(text, result.getOffsetForPosition(position))
                    if (span != null) select(span)
                }
            }
            .pointerInput(text) {
                var anchor: TextSpan? = null
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        val result = layout
                        val start = if (result == null) null
                        else TextTools.wordSpanAt(text, result.getOffsetForPosition(position))
                        anchor = start
                        if (start != null) select(start)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val result = layout
                        val from = anchor
                        if (result != null && from != null) {
                            val to = TextTools.wordSpanAt(text, result.getOffsetForPosition(change.position))
                            if (to != null) {
                                select(TextSpan(minOf(from.start, to.start), maxOf(from.end, to.end)))
                            }
                        }
                    },
                )
            },
    )
}

/** The panel under a text: shows the translation of the selection and saves it as a card. */
@Composable
fun SelectionPanel(
    text: String,
    span: TextSpan,
    onSpan: (TextSpan) -> Unit,
    topic: String,
    actions: WordActions,
    onClose: () -> Unit,
) {
    val start = span.start.coerceIn(0, text.length)
    val end = span.end.coerceIn(start, text.length)
    val selected = text.substring(start, end).trim()

    var translation by remember(selected) { mutableStateOf("") }
    var status by remember(selected) { mutableStateOf<String?>("Перекладаю…") }

    LaunchedEffect(selected) {
        if (selected.isEmpty()) return@LaunchedEffect
        delay(300)
        try {
            translation = actions.translate(selected)
            status = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = e.message ?: "Не вдалося перекласти."
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = selected,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status ?: translation,
                style = MaterialTheme.typography.titleMedium,
                color = if (status == null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onSpan(TextSpan(TextTools.expandLeft(text, start), end)) }) {
                    Text("← ширше")
                }
                TextButton(onClick = { onSpan(TextSpan(start, TextTools.expandRight(text, end))) }) {
                    Text("ширше →")
                }
                TextButton(onClick = { onSpan(TextTools.sentenceSpanAt(text, start)) }) {
                    Text("Речення")
                }
                TextButton(onClick = { actions.speak(selected) }) {
                    Text("Послухати")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("Закрити") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        val sentenceSpan = TextTools.sentenceSpanAt(text, start)
                        val sentence = text.substring(sentenceSpan.start, sentenceSpan.end)
                        actions.save(selected, translation.trim(), if (sentence == selected) "" else sentence, topic)
                        onClose()
                    },
                    enabled = selected.isNotEmpty() && translation.isNotBlank(),
                ) { Text("Додати картку") }
            }
        }
    }
}
