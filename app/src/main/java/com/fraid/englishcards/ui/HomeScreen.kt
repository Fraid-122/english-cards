package com.fraid.englishcards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.Card

@Composable
fun HomeScreen(
    cards: List<Card>,
    onStudy: () -> Unit,
    onAdd: () -> Unit,
    onList: () -> Unit,
    onCollections: () -> Unit,
    onSettings: () -> Unit,
    aiEnabled: Boolean,
) {
    val toLearn = cards.count { !it.known }
    val known = cards.count { it.known }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "English Cards",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Вивчай англійські слова картками",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Вивчити", toLearn, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatTile("Знаю", known, KnownGreen, Modifier.weight(1f))
            }

            Button(
                onClick = onStudy,
                enabled = toLearn > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Text("Почати навчання", style = MaterialTheme.typography.titleMedium)
            }
            if (toLearn == 0) {
                Text(
                    text = if (cards.isEmpty()) "Додай першу картку, щоб почати."
                    else "Усі картки вивчено. Додай нові!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HomeButton("Додати картку", onAdd)
            HomeButton("Переглянути картки", onList)
            HomeButton("Теми та складність", onCollections)
            HomeButton(if (aiEnabled) "Налаштування ШІ" else "Увімкнути ШІ (картинки й теми)", onSettings)
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
