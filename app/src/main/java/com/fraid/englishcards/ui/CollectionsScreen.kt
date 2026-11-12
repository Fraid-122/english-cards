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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Difficulty

@Composable
fun CollectionsScreen(
    cards: List<Card>,
    onBack: () -> Unit,
    onOpen: (topic: String?, difficulty: Difficulty?) -> Unit,
    onStudy: (topic: String?, difficulty: Difficulty?) -> Unit,
) {
    val topics = cards.map { it.topic }.distinct().sorted()

    Scaffold(
        topBar = { AppTopBar(title = "Теми та складність", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header-topics") { SectionHeader("За темою") }
            if (topics.isEmpty()) {
                item(key = "empty-topics") {
                    Text(
                        text = "Поки немає карток.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(topics, key = { "topic-$it" }) { name ->
                val group = cards.filter { it.topic == name }
                GroupRow(
                    name = name,
                    total = group.size,
                    known = group.count { it.known },
                    onOpen = { onOpen(name, null) },
                    onStudy = { onStudy(name, null) },
                )
            }

            item(key = "header-difficulty") { SectionHeader("За складністю") }
            items(Difficulty.entries.toList(), key = { "difficulty-${it.name}" }) { level ->
                val group = cards.filter { it.difficulty == level }
                GroupRow(
                    name = level.label,
                    total = group.size,
                    known = group.count { it.known },
                    onOpen = { onOpen(null, level) },
                    onStudy = { onStudy(null, level) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun GroupRow(
    name: String,
    total: Int,
    known: Int,
    onOpen: () -> Unit,
    onStudy: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onOpen),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Карток: $total · знаю: $known · вивчити: ${total - known}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onStudy, enabled = total - known > 0) {
                Text("Вчити")
            }
        }
    }
}
