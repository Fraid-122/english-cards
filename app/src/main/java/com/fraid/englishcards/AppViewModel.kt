package com.fraid.englishcards

import android.app.Application
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fraid.englishcards.data.AiClient
import com.fraid.englishcards.data.AiSuggestion
import com.fraid.englishcards.data.Article
import com.fraid.englishcards.data.Card
import com.fraid.englishcards.data.CardRepository
import com.fraid.englishcards.data.ChatMessage
import com.fraid.englishcards.data.Deck
import com.fraid.englishcards.data.Difficulty
import com.fraid.englishcards.data.JsonFileStore
import com.fraid.englishcards.data.OfflineTranslator
import com.fraid.englishcards.data.Scheduler
import com.fraid.englishcards.data.SettingsStore
import com.fraid.englishcards.data.StarterArticles
import com.fraid.englishcards.data.Streak
import com.fraid.englishcards.data.TextTools
import com.fraid.englishcards.data.Video
import com.fraid.englishcards.data.VoiceClient
import com.fraid.englishcards.data.YouTubeCaptions
import com.fraid.englishcards.ui.ExerciseMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

sealed interface Screen {
    data object Home : Screen
    data class Edit(val cardId: String?) : Screen
    data class CardList(val topic: String? = null, val difficulty: Difficulty? = null) : Screen
    data object Collections : Screen
    data object Study : Screen
    data object Settings : Screen
    data object Library : Screen
    data object Practice : Screen
    data object Decks : Screen
    data class DeckSort(val deckId: String) : Screen
    data object Articles : Screen
    data object ArticleEdit : Screen
    data class ArticleView(val articleId: String) : Screen
    data object Videos : Screen
    data class VideoView(val id: String) : Screen
    data class Exercise(val mode: ExerciseMode) : Screen
    data object Chat : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CardRepository(File(app.filesDir, "cards"))

    private val settings = SettingsStore(app)
    private val translator = OfflineTranslator()

    private val articleStore = JsonFileStore(
        File(app.filesDir, "articles"), Article.serializer(), { it.id }, { it.createdAt },
    )
    private val videoStore = JsonFileStore(
        File(app.filesDir, "videos"), Video.serializer(), { it.id }, { it.createdAt },
    )

    val cards: StateFlow<List<Card>> = repository.cards
    val articles: StateFlow<List<Article>> = articleStore.items
    val videos: StateFlow<List<Video>> = videoStore.items

    /** True while a video and its subtitles are being loaded. */
    var videoBusy by mutableStateOf(false)
        private set

    /** Days in a row with study. */
    var streakDays by mutableStateOf(
        Streak.current(settings.lastStudyDay, settings.streak, Streak.today())
    )
        private set

    /** The user's own Gemini API key; empty means AI features are off. */
    var apiKey by mutableStateOf(settings.apiKey)
        private set

    var aiModel by mutableStateOf(settings.model)
        private set

    val aiEnabled: Boolean
        get() = apiKey.isNotBlank()

    var stack by mutableStateOf(listOf<Screen>(Screen.Home))
        private set

    /** Cards still to answer in this study session; the first one is on screen. */
    var studyQueue by mutableStateOf(listOf<Card>())
        private set

    /** Grows with every answer so the study screen starts each card fresh. */
    var studyStep by mutableStateOf(0)
        private set

    /** How many different cards the session started with. */
    var studyTotal by mutableStateOf(0)
        private set

    /** How many of them have been answered right. */
    var studyDone by mutableStateOf(0)
        private set

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.load()
            articleStore.load { StarterArticles.create() }
            videoStore.load()
        }
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    // ---- Speech: ElevenLabs when a key is set, otherwise the phone's built-in voice ----

    var voiceKey by mutableStateOf(settings.voiceKey)
        private set

    var voiceId by mutableStateOf(settings.voiceId)
        private set

    var voiceModel by mutableStateOf(settings.voiceModel)
        private set

    private val speechDir = File(app.filesDir, "speech")
    private var player: MediaPlayer? = null

    fun saveVoiceSettings(key: String, voice: String, model: String) {
        settings.voiceKey = key
        settings.voiceId = voice
        settings.voiceModel = model
        voiceKey = settings.voiceKey
        voiceId = settings.voiceId
        voiceModel = settings.voiceModel
    }

    private fun play(file: File) {
        player?.release()
        val next = MediaPlayer()
        player = next
        next.setOnCompletionListener { finished ->
            finished.release()
            if (player === finished) player = null
        }
        next.setDataSource(file.absolutePath)
        next.prepare()
        next.start()
    }

    /** Says the English word or phrase aloud. */
    fun speak(text: String) {
        val phrase = text.trim()
        if (phrase.isEmpty()) return
        if (voiceKey.isBlank()) {
            speakWithPhoneVoice(phrase)
            return
        }
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    VoiceClient.fileFor(speechDir, voiceKey, voiceId, voiceModel, phrase)
                }
                play(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Fall back to the phone's voice so the user still hears the word.
                toast(e.message ?: "Не вдалося озвучити через ElevenLabs.")
                speakWithPhoneVoice(phrase)
            }
        }
    }

    /** Generates and plays a sample with the given ElevenLabs settings; throws when it fails. */
    suspend fun testVoice(key: String, voice: String, model: String) {
        val file = withContext(Dispatchers.IO) {
            VoiceClient.fileFor(speechDir, key, voice, model, "Hello! This is how your cards will sound.")
        }
        play(file)
    }

    private fun speakWithPhoneVoice(text: String) {
        val engine = tts
        if (engine == null || !ttsReady || text.isBlank()) {
            Toast.makeText(
                getApplication<Application>(),
                "Озвучення недоступне: на телефоні немає англійського голосу.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "card")
    }

    private fun toast(text: String) {
        Toast.makeText(getApplication<Application>(), text, Toast.LENGTH_SHORT).show()
    }

    // ---- Content: ready-made sets, articles, videos ----

    /** Adds the cards of a ready-made set, skipping words the user already has. */
    fun addDeck(deck: Deck) {
        val existing = cards.value.map { it.word.lowercase() }.toSet()
        val fresh = deck.cards().filter { it.word.lowercase() !in existing }
        viewModelScope.launch(Dispatchers.IO) { fresh.forEach { repository.save(it) } }
        toast("Додано карток: ${fresh.size}")
    }

    /** Adds one card of a set: to study, or straight to the learned ones when the user knows it. */
    fun addSortedCard(card: Card, known: Boolean) {
        save(if (known) Scheduler.markKnown(card, true) else card)
    }

    /** Adds cards of a set to study without sorting them. */
    fun addCards(cards: List<Card>) {
        viewModelScope.launch(Dispatchers.IO) { cards.forEach { repository.save(it) } }
    }

    /** Saves a word picked from an article, a video or the chat as a card. */
    fun addCardFromContent(word: String, translation: String, sentence: String, topic: String) {
        if (word.isBlank() || translation.isBlank()) return
        if (cards.value.any { it.word.equals(word, ignoreCase = true) }) {
            toast("Така картка вже є.")
            return
        }
        val card = Card(
            id = UUID.randomUUID().toString(),
            word = word,
            translation = translation,
            example = sentence.takeIf { it.contains(word, ignoreCase = true) && it != word }.orEmpty(),
            topic = topic.ifBlank { Card.DEFAULT_TOPIC },
            createdAt = System.currentTimeMillis(),
        )
        toast("Картку збережено.")
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.save(card) }
            // The AI, when it is on, adds a picture and a difficulty in the background.
            if (aiEnabled) {
                val suggestion = runCatching { suggest(word) }.getOrNull() ?: return@launch
                val latest = cards.value.firstOrNull { it.id == card.id } ?: return@launch
                withContext(Dispatchers.IO) {
                    repository.save(
                        latest.copy(
                            emoji = suggestion.emoji.ifBlank { latest.emoji },
                            difficulty = suggestion.difficulty,
                            example = latest.example.ifBlank { suggestion.example },
                        )
                    )
                }
            }
        }
    }

    fun saveArticle(title: String, text: String) {
        val article = Article(
            id = UUID.randomUUID().toString(),
            title = title,
            text = text,
            createdAt = System.currentTimeMillis(),
        )
        viewModelScope.launch(Dispatchers.IO) { articleStore.save(article) }
    }

    fun deleteArticle(id: String) {
        viewModelScope.launch(Dispatchers.IO) { articleStore.delete(id) }
    }

    /** Adds a YouTube video by its link and tries to load its English subtitles. */
    fun addVideo(link: String) {
        val videoId = TextTools.youtubeId(link)
        if (videoId == null) {
            toast("Це не схоже на посилання YouTube.")
            return
        }
        videoBusy = true
        viewModelScope.launch {
            val loaded = runCatching { YouTubeCaptions.load(videoId) }.getOrNull()
            val video = Video(
                id = UUID.randomUUID().toString(),
                videoId = videoId,
                title = loaded?.title?.takeIf { it.isNotBlank() } ?: "Відео $videoId",
                lines = loaded?.lines.orEmpty(),
                note = loaded?.note ?: "не вдалося звернутися до YouTube",
                createdAt = System.currentTimeMillis(),
            )
            withContext(Dispatchers.IO) { videoStore.save(video) }
            videoBusy = false
            toast(
                if (video.lines.isEmpty()) "Відео додано, але субтитри завантажити не вдалося."
                else "Відео додано із субтитрами."
            )
        }
    }

    /** Tries once more to load the video's own subtitles from YouTube. */
    fun reloadSubtitles(id: String) {
        val video = videos.value.firstOrNull { it.id == id } ?: return
        videoBusy = true
        viewModelScope.launch {
            val loaded = runCatching { YouTubeCaptions.load(video.videoId) }.getOrNull()
            val updated = video.copy(
                lines = loaded?.lines.orEmpty(),
                note = loaded?.note ?: "не вдалося звернутися до YouTube",
            )
            withContext(Dispatchers.IO) { videoStore.save(updated) }
            videoBusy = false
            toast(if (updated.lines.isEmpty()) "Субтитри не завантажились: ${updated.note}." else "Субтитри завантажено.")
        }
    }

    /** Makes subtitles with the AI: it listens to the video and writes down the speech. */
    fun transcribeVideo(id: String) {
        val video = videos.value.firstOrNull { it.id == id } ?: return
        videoBusy = true
        viewModelScope.launch {
            try {
                val lines = AiClient.transcribe(apiKey, aiModel, video.videoId)
                withContext(Dispatchers.IO) { videoStore.save(video.copy(lines = lines, note = "")) }
                toast("Субтитри створено.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast(e.message ?: "Не вдалося створити субтитри.")
            } finally {
                videoBusy = false
            }
        }
    }

    fun saveVideoText(id: String, text: String) {
        val video = videos.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) { videoStore.save(video.copy(text = text)) }
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch(Dispatchers.IO) { videoStore.delete(id) }
    }

    /** Continues the English conversation, nudging the AI to use the user's words. */
    suspend fun chat(messages: List<ChatMessage>): String =
        AiClient.chat(
            apiKey = apiKey,
            model = aiModel,
            messages = messages,
            words = cards.value.filter { !it.known }.shuffled().take(25).map { it.word },
        )

    /** Switches between the bottom tabs. */
    fun selectTab(screen: Screen) {
        stack = listOf(screen)
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

    /** Translates on the phone: English to Ukrainian, Ukrainian to English. */
    suspend fun translate(text: String): String = translator.translate(text)

    /**
     * The best translation available for a piece of a text: the AI, which understands the
     * sentence around it, when a key is set; otherwise (or if it fails) the offline translator.
     */
    suspend fun translateInContext(text: String, context: String): String {
        if (aiEnabled) {
            try {
                return AiClient.translate(apiKey, aiModel, text, context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Fall through to the offline translator.
            }
        }
        return translator.translate(text)
    }

    override fun onCleared() {
        translator.close()
        tts?.shutdown()
        tts = null
        player?.release()
        player = null
        super.onCleared()
    }

    /** Asks the AI for the picture, topic, difficulty and example of a new card. */
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

    /** Starts a session with the cards that are due now, optionally only one topic or difficulty. */
    fun startStudy(topic: String? = null, difficulty: Difficulty? = null) {
        val now = System.currentTimeMillis()
        val queue = cards.value
            .filter { Scheduler.isDue(it, now) }
            .filter { topic == null || it.topic == topic }
            .filter { difficulty == null || it.difficulty == difficulty }
            .shuffled()
        startStudyWith(queue)
    }

    private fun startStudyWith(queue: List<Card>) {
        if (queue.isEmpty()) return
        studyQueue = queue
        studyStep += 1
        studyTotal = queue.size
        studyDone = 0
        if (stack.last() != Screen.Study) navigate(Screen.Study)
    }

    /**
     * Records the answer. A card swiped left goes to the end of the queue and
     * is asked again; the session ends when every card has been answered right.
     */
    fun answer(card: Card, known: Boolean) {
        val latest = cards.value.firstOrNull { it.id == card.id } ?: card
        save(Scheduler.afterAnswer(latest, known, System.currentTimeMillis()))

        val today = Streak.today()
        val streak = Streak.afterStudy(settings.lastStudyDay, settings.streak, today)
        settings.lastStudyDay = today
        settings.streak = streak
        streakDays = streak

        val rest = studyQueue.filterNot { it.id == card.id }
        studyQueue = if (known) rest else rest + card
        if (known) studyDone += 1
        studyStep += 1
        if (studyQueue.isEmpty()) goHome()
    }
}
