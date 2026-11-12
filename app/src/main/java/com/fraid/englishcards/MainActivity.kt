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
import androidx.compose.ui.Modifier
import com.fraid.englishcards.ui.CardListScreen
import com.fraid.englishcards.ui.CollectionsScreen
import com.fraid.englishcards.ui.EditCardScreen
import com.fraid.englishcards.ui.EnglishCardsTheme
import com.fraid.englishcards.ui.HomeScreen
import com.fraid.englishcards.ui.SettingsScreen
import com.fraid.englishcards.ui.StudyScreen

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
                        selected = root == Screen.Collections,
                        onClick = { vm.selectTab(Screen.Collections) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Бібліотека") },
                    )
                    NavigationBarItem(
                        selected = root == Screen.Settings,
                        onClick = { vm.selectTab(Screen.Settings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Налаштування") },
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

                Screen.Collections -> CollectionsScreen(
                    cards = cards,
                    onOpen = { topic, difficulty -> vm.navigate(Screen.CardList(topic, difficulty)) },
                    onStudy = { topic, difficulty -> vm.startStudy(topic, difficulty) },
                )

                Screen.Settings -> SettingsScreen(
                    apiKey = vm.apiKey,
                    model = vm.aiModel,
                    onSave = { key, model -> vm.saveAiSettings(key, model) },
                    onTest = { key, model -> vm.testAi(key, model) },
                )

                Screen.Study -> StudyScreen(
                    queue = vm.studyQueue,
                    session = vm.studySession,
                    index = vm.studyIndex,
                    knownCount = vm.studyKnownCount,
                    missed = vm.studyMissed,
                    onAnswer = { card, known -> vm.answer(card, known) },
                    onSpeak = { text -> vm.speak(text) },
                    onRepeatMissed = { vm.startStudyWith(vm.studyMissed.shuffled()) },
                    onExit = { vm.goHome() },
                )
            }
        }
    }
}
