package com.fraid.englishcards.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.Scheduler
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    cards: List<Card>,
    onStudy: () -> Unit,
    onAdd: () -> Unit,
) {
    // The clock ticks so cards on minute-long pauses come back without reopening the app.
    var now by remember(cards) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(cards) {
        while (true) {
            delay(10_000)
            now = System.currentTimeMillis()
        }
    }
    val due = cards.count { Scheduler.isDue(it, now) }
    val inProgress = cards.count { Scheduler.isInProgress(it, now) }
    val learned = cards.count { Scheduler.isLearned(it, now) }
    val nextDueAt = cards.filter { !Scheduler.isDue(it, now) && it.dueAt != Long.MAX_VALUE }
        .minOfOrNull { it.dueAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "English Cards",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Вивчай англійські слова картками",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp)) {
                Stat("ВЧИТИ", due, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                Stat("У ПРОЦЕСІ", inProgress, InProgressAmber, Modifier.weight(1f))
                Stat("ВИВЧЕНО", learned, KnownGreen, Modifier.weight(1f))
            }
        }

        Button(
            onClick = onStudy,
            enabled = due > 0,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            Text("Почати навчання", style = MaterialTheme.typography.titleMedium)
        }
        if (due == 0) {
            Text(
                text = when {
                    cards.isEmpty() -> "Додай першу картку, щоб почати."
                    nextDueAt != null ->
                        "Зараз усе повторено. Наступна картка повернеться через ${Scheduler.formatWait(nextDueAt - now)}."
                    else -> "Усі картки вивчено. Додай нові!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(
            onClick = onAdd,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(
                text = "Додати картку",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
    }
}
