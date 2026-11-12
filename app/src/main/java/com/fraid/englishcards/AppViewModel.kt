package com.fraid.englishcards

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fraid.englishcards.data.AiClient
import com.fraid.englishcards.data.AiSuggestion
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.CardRepository
import com.fraid.englishcards.data.Difficulty
import com.fraid.englishcards.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface Screen {
    data object Home : Screen
    data class Edit(val cardId: String?) : Screen
    data class CardList(val topic: String? = null, val difficulty: Difficulty? = null) : Screen
    data object Collections : Screen
    data object Study : Screen
    data object Settings : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CardRepository(File(app.filesDir, "cards"))

    private val settings = SettingsStore(app)

    val cards: StateFlow<List<Card>> = repository.cards

    /** The user's own Gemini API key; empty means AI features are off. */
    var apiKey by mutableStateOf(settings.apiKey)
        private set

    var aiModel by mutableStateOf(settings.model)
        private set

    val aiEnabled: Boolean
        get() = apiKey.isNotBlank()

    var stack by mutableStateOf(listOf<Screen>(Screen.Home))
        private set

    /** Cards of the current study session, fixed when the session starts. */
    var studyQueue by mutableStateOf(listOf<Card>())
        private set

    /** Changes with every new session so the study screen starts fresh. */
    var studySession by mutableStateOf(0)
        private set

    var studyIndex by mutableStateOf(0)
        private set

    var studyKnownCount by mutableStateOf(0)
        private set

    /** Cards swiped left in the current session. */
    var studyMissed by mutableStateOf(listOf<Card>())
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) { repository.load() }
    }

    fun navigate(screen: Screen) {
        stack = stack + screen
    }

    fun back() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun goHome() {
        stack = listOf(Screen.Home)
    }

    fun save(card: Card) {
        viewModelScope.launch(Dispatchers.IO) { repository.save(card) }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.delete(id) }
    }

    fun saveAiSettings(key: String, model: String) {
        settings.apiKey = key
        settings.model = model
        apiKey = settings.apiKey
        aiModel = settings.model
    }

    /** Asks the AI for the translation, topic, difficulty and picture of a new card. */
    suspend fun suggest(text: String): AiSuggestion =
        AiClient.suggest(
            apiKey = apiKey,
            model = aiModel,
            text = text,
            topics = cards.value.map { it.topic }.distinct(),
        )

    /** Tries the given key and model with a sample word. */
    suspend fun testAi(key: String, model: String): AiSuggestion =
        AiClient.suggest(apiKey = key, model = model, text = "apple", topics = emptyList())

    /** Starts a session with the cards still to learn, optionally only one topic or difficulty. */
    fun startStudy(topic: String? = null, difficulty: Difficulty? = null) {
        val queue = cards.value
            .filter { !it.known }
            .filter { topic == null || it.topic == topic }
            .filter { difficulty == null || it.difficulty == difficulty }
            .shuffled()
        startStudyWith(queue)
    }

    fun startStudyWith(queue: List<Card>) {
        if (queue.isEmpty()) return
        studyQueue = queue
        studySession += 1
        studyIndex = 0
        studyKnownCount = 0
        studyMissed = emptyList()
        if (stack.last() != Screen.Study) navigate(Screen.Study)
    }

    /** Records the answer: swiped right (known) or left (not known). */
    fun answer(card: Card, known: Boolean) {
        val latest = cards.value.firstOrNull { it.id == card.id } ?: card
        save(
            latest.copy(
                known = known,
                correctCount = latest.correctCount + if (known) 1 else 0,
                wrongCount = latest.wrongCount + if (known) 0 else 1,
            )
        )
        if (known) studyKnownCount += 1 else studyMissed = studyMissed + card
        studyIndex += 1
    }
}
