package com.fraid.englishcards.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.TextTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** A word (or phrase) picked from a text, with the sentence it came from. */
data class PickedWord(val word: String, val sentence: String)

/** What the word dialog needs from the app. */
class WordActions(
    /** Translates [text]; [context] is the sentence it came from (may be empty). */
    val translate: suspend (text: String, context: String) -> String,
    val speak: (String) -> Unit,
    val save: (word: String, translation: String, sentence: String, topic: String) -> Unit,
)

/** Text in which every word can be tapped. */
@Composable
fun TappableText(
    text: String,
    onWord: (PickedWord) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier.pointerInput(text) {
            detectTapGestures { position ->
                val result = layout ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(position)
                val word = TextTools.wordAt(text, offset) ?: return@detectTapGestures
                onWord(PickedWord(word, TextTools.sentenceAt(text, offset)))
            }
        },
    )
}

/** Shows the translation of a picked word and saves it as a card. */
@Composable
fun WordDialog(
    picked: PickedWord,
    topic: String,
    actions: WordActions,
    onDismiss: () -> Unit,
) {
    var word by remember(picked) { mutableStateOf(picked.word) }
    var translation by remember(picked) { mutableStateOf("") }
    var status by remember(picked) { mutableStateOf<String?>(null) }

    LaunchedEffect(word) {
        val source = word.trim()
        if (source.length < 2) return@LaunchedEffect
        delay(400)
        status = "Перекладаю…"
        try {
            translation = actions.translate(source, if (picked.sentence == source) "" else picked.sentence)
            status = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = e.message ?: "Не вдалося перекласти."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нова картка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Слово або фраза") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("Переклад") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = status
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (picked.sentence.isNotBlank()) {
                    Text(
                        text = picked.sentence,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { actions.speak(word) }) { Text("Послухати") }
                    if (picked.sentence.isNotBlank() && picked.sentence != word) {
                        TextButton(onClick = { word = picked.sentence }) { Text("Уся фраза") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    actions.save(word.trim(), translation.trim(), picked.sentence, topic)
                    onDismiss()
                },
                enabled = word.isNotBlank() && translation.isNotBlank(),
            ) { Text("Зберегти") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрити") }
        },
    )
}
