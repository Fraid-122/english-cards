package com.fraid.englishcards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fraid.englishcards.data.Article

@Composable
fun ArticlesScreen(
    articles: List<Article>,
    onOpen: (Article) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Article) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { AppTopBar(title = "Статті", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Додати текст")
            }
        },
    ) { padding ->
        if (articles.isEmpty()) {
            Text(
                text = "Поки немає текстів. Натисни «+», щоб вставити свій.",
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(articles, key = { it.id }) { article ->
                    val words = article.text.split(Regex("\\s+")).count { it.isNotBlank() }
                    HubRow(
                        emoji = "📖",
                        title = article.title,
                        subtitle = listOf(article.level, "слів: $words").filter { it.isNotBlank() }.joinToString(" · "),
                        onClick = { onOpen(article) },
                        trailing = {
                            IconButton(onClick = { onDelete(article) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Видалити")
                            }
                        },
                    )
                }
            }
        }
    }
}

/** A form to paste the user's own text. */
@Composable
fun ArticleEditScreen(
    onSave: (title: String, text: String) -> Unit,
    onBack: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }

    Scaffold(topBar = { AppTopBar(title = "Новий текст", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Текст англійською") },
                supportingText = { Text("Скопіюй статтю, пост чи текст пісні й встав сюди.") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(title.trim().ifBlank { "Без назви" }, text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("Зберегти", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** The reader: tap any word to see its translation and save it as a card. */
@Composable
fun ArticleScreen(
    article: Article,
    actions: WordActions,
    onBack: () -> Unit,
) {
    var picked by remember { mutableStateOf<PickedWord?>(null) }

    Scaffold(topBar = { AppTopBar(title = "Читання", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Торкнись слова, щоб побачити переклад і зберегти картку.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TappableText(
                text = article.text,
                onWord = { picked = it },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 30.sp),
            )
        }
    }

    val current = picked
    if (current != null) {
        WordDialog(
            picked = current,
            topic = article.title,
            actions = actions,
            onDismiss = { picked = null },
        )
    }
}
