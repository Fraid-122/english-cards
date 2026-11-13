package com.fraid.englishcards

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fraid.englishcards.ui.ArticleEditScreen
import com.fraid.englishcards.ui.ArticleScreen
import com.fraid.englishcards.ui.ArticlesScreen
import com.fraid.englishcards.ui.CardListScreen
import com.fraid.englishcards.ui.ChatScreen
import com.fraid.englishcards.ui.CollectionsScreen
import com.fraid.englishcards.data.Decks
import com.fraid.englishcards.ui.DeckSortScreen
import com.fraid.englishcards.ui.DecksScreen
import com.fraid.englishcards.ui.EditCardScreen
import com.fraid.englishcards.ui.EnglishCardsTheme
import com.fraid.englishcards.ui.ExerciseScreen
import com.fraid.englishcards.ui.HomeScreen
import com.fraid.englishcards.ui.LibraryScreen
import com.fraid.englishcards.ui.PracticeScreen
import com.fraid.englishcards.ui.SettingsScreen
import com.fraid.englishcards.ui.StudyScreen
import com.fraid.englishcards.ui.VideoScreen
import com.fraid.englishcards.ui.VideosScreen
import com.fraid.englishcards.ui.WordActions

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark only, so the system bars always use light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            EnglishCardsTheme {
                App(viewModel)
            }
        }
    }
}

@Composable
private fun App(vm: AppViewModel) {
    val cards by vm.cards.collectAsState()
    val articles by vm.articles.collectAsState()
    val videos by vm.videos.collectAsState()
    val wordActions = remember(vm) {
        WordActions(
            translate = { text -> vm.translate(text) },
            speak = { text -> vm.speak(text) },
            save = { word, translation, sentence, topic ->
                vm.addCardFromContent(word, translation, sentence, topic)
            },
        )
    }
    val isRoot = vm.stack.size == 1
    val root = vm.stack.first()

    BackHandler(enabled = !isRoot || root != Screen.Home) {
        if (isRoot) vm.selectTab(Screen.Home) else vm.back()
    }

    Scaffold(
        bottomBar = {
            // The tabs are shown only on the three main screens.
            if (isRoot) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = root == Screen.Home,
                        onClick = { vm.selectTab(Screen.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Навчання") },
                    )
                    NavigationBarItem(
                        selected = root == Screen.Practice,
                        onClick = { vm.selectTab(Screen.Practice) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        label = { Text("Практика") },
                    )
                    NavigationBarItem(
                        selected = root == Screen.Library,
                        onClick = { vm.selectTab(Screen.Library) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Бібліотека") },
                    )
                    NavigationBarItem(
                        selected = root == Screen.Settings,
                        onClick = { vm.selectTab(Screen.Settings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Опції") },
                    )
                }
            }
        },
    ) { outerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .consumeWindowInsets(outerPadding),
        ) {
            when (val screen = vm.stack.last()) {
                Screen.Home -> HomeScreen(
                    cards = cards,
                    streakDays = vm.streakDays,
                    onStudy = { vm.startStudy() },
                    onAdd = { vm.navigate(Screen.Edit(cardId = null)) },
                )

                is Screen.Edit -> EditCardScreen(
                    existing = cards.firstOrNull { it.id == screen.cardId },
                    topics = cards.map { it.topic }.distinct().sorted(),
                    aiEnabled = vm.aiEnabled,
                    onTranslate = { text -> vm.translate(text) },
                    onSuggest = { text -> vm.suggest(text) },
                    onSave = { card ->
                        vm.save(card)
                        vm.back()
                    },
                    onBack = { vm.back() },
                )

                is Screen.CardList -> CardListScreen(
                    cards = cards,
                    topic = screen.topic,
                    difficulty = screen.difficulty,
                    onBack = { vm.back() },
                    onAdd = { vm.navigate(Screen.Edit(cardId = null)) },
                    onEdit = { card -> vm.navigate(Screen.Edit(cardId = card.id)) },
                    onDelete = { card -> vm.delete(card.id) },
                )

                Screen.Library -> LibraryScreen(
                    cardCount = cards.size,
                    articleCount = articles.size,
                    videoCount = videos.size,
                    onCards = { vm.navigate(Screen.CardList()) },
                    onGroups = { vm.navigate(Screen.Collections) },
                    onDecks = { vm.navigate(Screen.Decks) },
                    onArticles = { vm.navigate(Screen.Articles) },
                    onVideos = { vm.navigate(Screen.Videos) },
                )

                Screen.Decks -> DecksScreen(
                    cards = cards,
                    onOpen = { deck -> vm.navigate(Screen.DeckSort(deck.id)) },
                    onBack = { vm.back() },
                )

                is Screen.DeckSort -> {
                    val deck = Decks.all.firstOrNull { it.id == screen.deckId }
                    if (deck != null) {
                        DeckSortScreen(
                            deck = deck,
                            cards = cards,
                            onSorted = { card, known -> vm.addSortedCard(card, known) },
                            onAddAll = { rest -> vm.addCards(rest) },
                            onBack = { vm.back() },
                        )
                    }
                }

                Screen.Articles -> ArticlesScreen(
                    articles = articles,
                    onOpen = { article -> vm.navigate(Screen.ArticleView(article.id)) },
                    onAdd = { vm.navigate(Screen.ArticleEdit) },
                    onDelete = { article -> vm.deleteArticle(article.id) },
                    onBack = { vm.back() },
                )

                Screen.ArticleEdit -> ArticleEditScreen(
                    onSave = { title, text ->
                        vm.saveArticle(title, text)
                        vm.back()
                    },
                    onBack = { vm.back() },
                )

                is Screen.ArticleView -> {
                    val article = articles.firstOrNull { it.id == screen.articleId }
                    if (article != null) {
                        ArticleScreen(article = article, actions = wordActions, onBack = { vm.back() })
                    }
                }

                Screen.Videos -> VideosScreen(
                    videos = videos,
                    busy = vm.videoBusy,
                    onOpen = { video -> vm.navigate(Screen.VideoView(video.id)) },
                    onAdd = { link -> vm.addVideo(link) },
                    onDelete = { video -> vm.deleteVideo(video.id) },
                    onBack = { vm.back() },
                )

                is Screen.VideoView -> {
                    val video = videos.firstOrNull { it.id == screen.id }
                    if (video != null) {
                        VideoScreen(
                            video = video,
                            busy = vm.videoBusy,
                            aiEnabled = vm.aiEnabled,
                            actions = wordActions,
                            onSaveText = { text -> vm.saveVideoText(video.id, text) },
                            onReloadSubtitles = { vm.reloadSubtitles(video.id) },
                            onTranscribe = { vm.transcribeVideo(video.id) },
                            onBack = { vm.back() },
                        )
                    }
                }

                Screen.Practice -> PracticeScreen(
                    cards = cards,
                    aiEnabled = vm.aiEnabled,
                    onExercise = { mode -> vm.navigate(Screen.Exercise(mode)) },
                    onChat = { vm.navigate(Screen.Chat) },
                )

                is Screen.Exercise -> ExerciseScreen(
                    cards = cards,
                    mode = screen.mode,
                    onSpeak = { text -> vm.speak(text) },
                    onBack = { vm.back() },
                )

                Screen.Chat -> ChatScreen(
                    aiEnabled = vm.aiEnabled,
                    onSend = { messages -> vm.chat(messages) },
                    actions = wordActions,
                    onBack = { vm.back() },
                )

                Screen.Collections -> CollectionsScreen(
                    cards = cards,
                    onBack = { vm.back() },
                    onOpen = { topic, difficulty -> vm.navigate(Screen.CardList(topic, difficulty)) },
                    onStudy = { topic, difficulty -> vm.startStudy(topic, difficulty) },
                )

                Screen.Settings -> SettingsScreen(
                    apiKey = vm.apiKey,
                    model = vm.aiModel,
                    onSave = { key, model -> vm.saveAiSettings(key, model) },
                    onTest = { key, model -> vm.testAi(key, model) },
                    voiceKey = vm.voiceKey,
                    voiceId = vm.voiceId,
                    voiceModel = vm.voiceModel,
                    onSaveVoice = { key, voice, model -> vm.saveVoiceSettings(key, voice, model) },
                    onTestVoice = { key, voice, model -> vm.testVoice(key, voice, model) },
                )

                Screen.Study -> StudyScreen(
                    queue = vm.studyQueue,
                    step = vm.studyStep,
                    total = vm.studyTotal,
                    done = vm.studyDone,
                    onAnswer = { card, known -> vm.answer(card, known) },
                    onSpeak = { text -> vm.speak(text) },
                    onExit = { vm.goHome() },
                )
            }
        }
    }
}
