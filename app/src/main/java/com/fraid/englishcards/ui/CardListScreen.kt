@file:OptIn(ExperimentalMaterial3Api::class)

package com.fraid.englishcards.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Difficulty

@Composable
fun CardListScreen(
    cards: List<Card>,
    topic: String?,
    difficulty: Difficulty?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Card) -> Unit,
    onDelete: (Card) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Card?>(null) }

    val needle = query.trim().lowercase()
    val filtered = cards
        .filter { topic == null || it.topic == topic }
        .filter { difficulty == null || it.difficulty == difficulty }
        .filter {
            needle.isEmpty() ||
                it.word.lowercase().contains(needle) ||
                it.translation.lowercase().contains(needle)
        }

    val title = when {
        topic != null -> "Тема: $topic"
        difficulty != null -> "Складність: ${difficulty.label}"
        else -> "Усі картки"
    }

    Scaffold(
        topBar = { AppTopBar(title = title, onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Додати картку")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Пошук") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (filtered.isEmpty()) {
                Text(
                    text = "Карток не знайдено.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(filtered, key = { it.id }) { card ->
                        CardRow(
                            card = card,
                            onClick = { onEdit(card) },
                            onDelete = { toDelete = card },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    val pending = toDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Видалити картку?") },
            text = { Text("«${pending.word}» буде видалено назавжди.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(pending)
                    toDelete = null
                }) { Text("Видалити") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Скасувати") }
            },
        )
    }
}

@Composable
private fun CardRow(card: Card, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardAvatar(card = card, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.word,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.translation,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${card.topic} · ${card.difficulty.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (card.known) {
            Icon(Icons.Default.Check, contentDescription = "Знаю", tint = KnownGreen)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Видалити")
        }
    }
}
