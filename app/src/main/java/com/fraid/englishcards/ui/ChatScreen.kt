@file:OptIn(ExperimentalMaterial3Api::class)

package com.fraid.englishcards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val GREETING = "Hi! I am your English partner. What did you do today?"

/** A simple English conversation with the AI; tap a word in its replies to save a card. */
@Composable
fun ChatScreen(
    aiEnabled: Boolean,
    onSend: suspend (List<ChatMessage>) -> String,
    actions: WordActions,
    onBack: () -> Unit,
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<PickedWord?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, waiting) {
        listState.animateScrollToItem(messages.size + 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || waiting) return
        messages.add(ChatMessage(fromUser = true, text = text))
        input = ""
        error = null
        waiting = true
        scope.launch {
            try {
                val reply = onSend(messages.toList())
                messages.add(ChatMessage(fromUser = false, text = reply))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Помилка ШІ."
            } finally {
                waiting = false
            }
        }
    }

    Scaffold(topBar = { AppTopBar(title = "Розмова з ШІ", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "greeting") {
                    Bubble(fromUser = false) {
                        TappableText(text = GREETING, onWord = { picked = it })
                    }
                }
                itemsIndexed(messages) { _, message ->
                    Bubble(fromUser = message.fromUser) {
                        if (message.fromUser) {
                            Text(message.text)
                        } else {
                            TappableText(text = message.text, onWord = { picked = it })
                        }
                    }
                }
                item(key = "status") {
                    val problem = error
                    when {
                        !aiEnabled -> Text(
                            text = "Щоб розмовляти, додай API-ключ у вкладці «Налаштування».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        waiting -> Text(
                            text = "ШІ пише…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        problem != null -> Text(
                            text = problem,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Напиши англійською…") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = { send() },
                    enabled = aiEnabled && !waiting && input.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Надіслати")
                }
            }
        }
    }

    val current = picked
    if (current != null) {
        WordDialog(
            picked = current,
            topic = "Розмова",
            actions = actions,
            onDismiss = { picked = null },
        )
    }
}

@Composable
private fun Bubble(fromUser: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                content()
            }
        }
    }
}
