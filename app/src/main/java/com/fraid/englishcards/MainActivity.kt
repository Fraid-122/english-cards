package com.fraid.englishcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fraid.englishcards.ui.CardListScreen
import com.fraid.englishcards.ui.CollectionsScreen
import com.fraid.englishcards.ui.EditCardScreen
import com.fraid.englishcards.ui.EnglishCardsTheme
import com.fraid.englishcards.ui.HomeScreen
import com.fraid.englishcards.ui.StudyScreen

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    BackHandler(enabled = vm.stack.size > 1) { vm.back() }

    when (val screen = vm.stack.last()) {
        Screen.Home -> HomeScreen(
            cards = cards,
            onStudy = { vm.startStudy() },
            onAdd = { vm.navigate(Screen.Edit(cardId = null)) },
            onList = { vm.navigate(Screen.CardList()) },
            onCollections = { vm.navigate(Screen.Collections) },
        )

        is Screen.Edit -> EditCardScreen(
            existing = cards.firstOrNull { it.id == screen.cardId },
            topics = cards.map { it.topic }.distinct().sorted(),
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
            onBack = { vm.back() },
            onOpen = { topic, difficulty -> vm.navigate(Screen.CardList(topic, difficulty)) },
            onStudy = { topic, difficulty -> vm.startStudy(topic, difficulty) },
        )

        Screen.Study -> StudyScreen(
            queue = vm.studyQueue,
            session = vm.studySession,
            index = vm.studyIndex,
            knownCount = vm.studyKnownCount,
            missed = vm.studyMissed,
            onAnswer = { card, known -> vm.answer(card, known) },
            onRepeatMissed = { vm.startStudyWith(vm.studyMissed.shuffled()) },
            onExit = { vm.goHome() },
        )
    }
}
