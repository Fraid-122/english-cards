package com.fraid.englishcards.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Deck
import com.fraid.englishcards.data.Decks

/** A row with an emoji, a title and a subtitle, used by the hub screens. */
@Composable
fun HubRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = emoji, fontSize = 26.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) trailing()
        }
    }
}

@Composable
fun LibraryScreen(
    cardCount: Int,
    articleCount: Int,
    videoCount: Int,
    onCards: () -> Unit,
    onGroups: () -> Unit,
    onDecks: () -> Unit,
    onArticles: () -> Unit,
    onVideos: () -> Unit,
) {
    Scaffold(topBar = { AppTopBar(title = "Бібліотека") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HubRow("🗂️", "Мої картки", "Усього: $cardCount", onCards)
            HubRow("🏷️", "Теми та складність", "Картки за групами", onGroups)
            HubRow("📦", "Готові набори", "Наборів: ${Decks.all.size}, рівні A1–B1", onDecks)
            HubRow("📖", "Статті", "Текстів: $articleCount · тап по слову робить картку", onArticles)
            HubRow("🎬", "Відео", "Відео: $videoCount · YouTube із субтитрами", onVideos)
        }
    }
}

@Composable
fun DecksScreen(
    cards: List<Card>,
    onOpen: (Deck) -> Unit,
    onBack: () -> Unit,
) {
    val known = cards.map { it.word.lowercase() }.toSet()
    Scaffold(topBar = { AppTopBar(title = "Готові набори", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Decks.all, key = { it.id }) { deck ->
                val missing = deck.cards().count { it.word.lowercase() !in known }
                HubRow(
                    emoji = deck.emoji,
                    title = deck.title,
                    subtitle = "Рівень ${deck.level} · слів: ${deck.size}" +
                        if (missing == 0) " · усі вже у тебе" else " · нових: $missing",
                    onClick = { onOpen(deck) },
                    trailing = {
                        TextButton(onClick = { onOpen(deck) }) {
                            Text(if (missing == 0) "Готово" else "Розібрати")
                        }
                    },
                )
            }
        }
    }
}
