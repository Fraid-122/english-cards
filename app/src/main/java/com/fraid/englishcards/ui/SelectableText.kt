package com.fraid.englishcards.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

/** Centres of the two round handles under the ends of the selection, or null when they can't be placed. */
private fun handleCentres(
    layout: TextLayoutResult,
    textLength: Int,
    selection: TextSpan,
    radius: Float,
): Pair<Offset, Offset>? {
    // The layout may still belong to an older text for a moment.
    if (textLength == 0 || layout.layoutInput.text.length != textLength) return null
    if (selection.end <= selection.start) return null
    val first = selection.start.coerceIn(0, textLength - 1)
    val last = (selection.end - 1).coerceIn(first, textLength - 1)
    val startBox = layout.getBoundingBox(first)
    val endBox = layout.getBoundingBox(last)
    return Offset(startBox.left, startBox.bottom + radius) to Offset(endBox.right, endBox.bottom + radius)
}

/**
 * Text in which the user chooses exactly what to translate:
 * a tap selects one word, the two round handles under the selection stretch or shrink it
 * word by word, and a long press and drag selects a range in one move.
 *
 * [emphasis] marks a piece of the text in the accent colour (the subtitle line being spoken).
 */
@Composable
fun SelectableText(
    text: String,
    selection: TextSpan?,
    onSelection: (TextSpan) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    emphasis: TextSpan? = null,
    onLayout: (TextLayoutResult) -> Unit = {},
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val select by rememberUpdatedState(onSelection)
    val currentSelection by rememberUpdatedState(selection)
    // True while a handle is being dragged, so the long-press gesture stays out of the way.
    val handleActive = remember { booleanArrayOf(false) }

    val accent = MaterialTheme.colorScheme.primary
    val highlight = accent.copy(alpha = 0.35f)
    val density = LocalDensity.current
    val handleRadius = with(density) { 9.dp.toPx() }
    val touchRadius = with(density) { 28.dp.toPx() }

    val annotated = remember(text, selection, emphasis, accent) {
        buildAnnotatedString {
            append(text)
            if (emphasis != null) {
                val start = emphasis.start.coerceIn(0, text.length)
                val end = emphasis.end.coerceIn(start, text.length)
                addStyle(SpanStyle(color = accent), start, end)
            }
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
        onTextLayout = {
            layout = it
            onLayout(it)
        },
        modifier = modifier
            // A tap selects the word under the finger.
            .pointerInput(text) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    val span = TextTools.wordSpanAt(text, result.getOffsetForPosition(position))
                    if (span != null) select(span)
                }
            }
            // A long press and drag selects from the first word to the word under the finger.
            .pointerInput(text) {
                var anchor: TextSpan? = null
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        val result = layout
                        val start = if (result == null || handleActive[0]) null
                        else TextTools.wordSpanAt(text, result.getOffsetForPosition(position))
                        anchor = start
                        if (start != null) select(start)
                    },
                    onDrag = { change, _ ->
                        val result = layout
                        val from = anchor
                        if (result != null && from != null && !handleActive[0]) {
                            change.consume()
                            val to = TextTools.wordSpanAt(text, result.getOffsetForPosition(change.position))
                            if (to != null) {
                                select(TextSpan(minOf(from.start, to.start), maxOf(from.end, to.end)))
                            }
                        }
                    },
                )
            }
            // Dragging a handle moves that end of the selection. This is the innermost
            // gesture, so it sees the touch first and keeps it from the other two.
            .pointerInput(text) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    handleActive[0] = false
                    val result = layout ?: return@awaitEachGesture
                    val current = currentSelection ?: return@awaitEachGesture
                    val centres = handleCentres(result, text.length, current, handleRadius)
                        ?: return@awaitEachGesture
                    val toStart = (down.position - centres.first).getDistance()
                    val toEnd = (down.position - centres.second).getDistance()
                    if (toStart > touchRadius && toEnd > touchRadius) return@awaitEachGesture

                    val movingEnd = toEnd <= toStart
                    handleActive[0] = true
                    down.consume()
                    // The finger rests on the handle below the line; aim at the line itself.
                    val lift = Offset(0f, handleRadius * 2.5f)
                    drag(down.id) { change ->
                        change.consume()
                        val now = currentSelection
                        val latest = layout
                        if (now != null && latest != null &&
                            latest.layoutInput.text.length == text.length &&
                            now.start >= 0 && now.end > now.start && now.end <= text.length
                        ) {
                            val offset = latest.getOffsetForPosition(change.position - lift)
                            val word = TextTools.wordSpanAt(text, offset)
                            if (movingEnd) {
                                val end = (word?.end ?: offset).coerceIn(now.start + 1, text.length)
                                select(TextSpan(now.start, end))
                            } else {
                                val start = (word?.start ?: offset).coerceIn(0, now.end - 1)
                                select(TextSpan(start, now.end))
                            }
                        }
                    }
                    handleActive[0] = false
                }
            }
            .drawWithContent {
                drawContent()
                val result = layout
                val current = currentSelection
                if (result != null && current != null) {
                    val centres = handleCentres(result, text.length, current, handleRadius)
                    if (centres != null) {
                        drawCircle(color = accent, radius = handleRadius, center = centres.first)
                        drawCircle(color = accent, radius = handleRadius, center = centres.second)
                    }
                }
            }
            // Room under the last line so its handles can be drawn and touched.
            .padding(bottom = 30.dp),
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
    val sentenceSpan = TextTools.sentenceSpanAt(text, start)
    val sentence = text.substring(sentenceSpan.start, sentenceSpan.end)

    var translation by remember(selected) { mutableStateOf("") }
    var status by remember(selected) { mutableStateOf<String?>("Перекладаю…") }

    LaunchedEffect(selected) {
        if (selected.isEmpty()) return@LaunchedEffect
        // Wait until the user stops moving the handles.
        delay(500)
        try {
            translation = actions.translate(selected, if (sentence == selected) "" else sentence)
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
                TextButton(onClick = { onSpan(sentenceSpan) }) {
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
                        actions.save(selected, translation.trim(), if (sentence == selected) "" else sentence, topic)
                        onClose()
                    },
                    enabled = selected.isNotEmpty() && translation.isNotBlank(),
                ) { Text("Додати картку") }
            }
        }
    }
}
