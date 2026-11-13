package com.fraid.englishcards.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** A text to read; tapping a word in it makes a card. */
@Serializable
data class Article(
    val id: String,
    val title: String,
    val text: String,
    val level: String = "",
    val createdAt: Long = 0L,
)

/** One subtitle line of a video. */
@Serializable
data class VideoLine(val startMs: Long, val text: String)

/** A YouTube video with its subtitles (when they could be loaded) or pasted text. */
@Serializable
data class Video(
    val id: String,
    val videoId: String,
    val title: String,
    val lines: List<VideoLine> = emptyList(),
    val text: String = "",
    val createdAt: Long = 0L,
)

/** A ready-made set of cards that can be added to the user's cards. */
class Deck(
    val id: String,
    val title: String,
    val level: String,
    val emoji: String,
    val difficulty: Difficulty,
    private val entries: List<String>,
) {
    val size: Int get() = entries.size

    /** Entries are written as "word|translation|example|emoji". */
    fun cards(now: Long = System.currentTimeMillis()): List<Card> =
        entries.mapIndexed { index, entry ->
            val parts = entry.split('|')
            Card(
                id = UUID.randomUUID().toString(),
                word = parts[0],
                translation = parts.getOrElse(1) { "" },
                example = parts.getOrElse(2) { "" },
                emoji = parts.getOrElse(3) { "" },
                topic = title,
                difficulty = difficulty,
                createdAt = now - index,
            )
        }
}

object Decks {
    val all: List<Deck> = listOf(
        Deck(
            "first-words", "Перші слова", "A1", "👋", Difficulty.EASY,
            listOf(
                "hello|привіт|Hello, how are you?|👋",
                "thank you|дякую|Thank you for your help.|🙏",
                "please|будь ласка|Can I have some water, please?|🙂",
                "yes|так|Yes, I like it.|✅",
                "no|ні|No, I am not tired.|❌",
                "water|вода|I drink water every morning.|💧",
                "food|їжа|The food is very good.|🍽️",
                "house|будинок|My house is small.|🏠",
                "friend|друг|He is my best friend.|🤝",
                "family|сім'я|My family is big.|👪",
                "day|день|Today is a good day.|☀️",
                "night|ніч|It is cold at night.|🌙",
            ),
        ),
        Deck(
            "food-drinks", "Їжа та напої", "A1", "🍞", Difficulty.EASY,
            listOf(
                "bread|хліб|She bought fresh bread.|🍞",
                "milk|молоко|I drink milk with breakfast.|🥛",
                "cheese|сир|I like cheese on my bread.|🧀",
                "egg|яйце|I eat an egg in the morning.|🥚",
                "meat|м'ясо|He does not eat meat.|🥩",
                "fish|риба|We have fish for dinner.|🐟",
                "tea|чай|Would you like a cup of tea?|🍵",
                "coffee|кава|I drink coffee every day.|☕",
                "sugar|цукор|I drink tea without sugar.|🍬",
                "salt|сіль|Pass me the salt, please.|🧂",
                "soup|суп|This soup is hot.|🍲",
                "breakfast|сніданок|Breakfast is at eight.|🍳",
            ),
        ),
        Deck(
            "travel", "Подорожі", "A2", "🧳", Difficulty.MEDIUM,
            listOf(
                "ticket|квиток|I need a ticket to Kyiv.|🎫",
                "airport|аеропорт|The airport is far from the city.|✈️",
                "train|потяг|The train leaves at nine.|🚆",
                "luggage|багаж|Where is my luggage?|🧳",
                "map|карта|Look at the map.|🗺️",
                "hotel|готель|Our hotel is near the sea.|🏨",
                "passport|паспорт|Show your passport, please.|🛂",
                "journey|подорож|The journey took three hours.|🧭",
                "delay|затримка|There is a delay of one hour.|⏳",
                "arrival|прибуття|The arrival time is six o'clock.|🛬",
                "departure|відправлення|The departure is from platform two.|🛫",
                "to book|бронювати|I want to book a room.|📅",
            ),
        ),
        Deck(
            "home", "Дім і побут", "A2", "🏠", Difficulty.MEDIUM,
            listOf(
                "kitchen|кухня|We eat in the kitchen.|🍳",
                "bedroom|спальня|My bedroom is quiet.|🛏️",
                "window|вікно|Open the window, please.|🪟",
                "door|двері|Close the door behind you.|🚪",
                "key|ключ|I lost my key.|🔑",
                "fridge|холодильник|The milk is in the fridge.|🧊",
                "to clean|прибирати|I clean my room on Saturday.|🧹",
                "to cook|готувати|She likes to cook dinner.|👩‍🍳",
                "charger|зарядний пристрій|Where is my phone charger?|🔌",
                "power outage|відключення світла|We had a power outage last night.|💡",
                "candle|свічка|Light a candle, please.|🕯️",
                "blanket|ковдра|Take a warm blanket.|🛌",
            ),
        ),
        Deck(
            "work-study", "Робота й навчання", "B1", "💼", Difficulty.HARD,
            listOf(
                "deadline|крайній термін|The deadline is on Friday.|⏰",
                "meeting|зустріч, нарада|We have a meeting at ten.|👥",
                "experience|досвід|She has a lot of experience.|🎓",
                "skill|навичка|Listening is an important skill.|🛠️",
                "to improve|покращувати|I want to improve my English.|📈",
                "to apply|подавати заявку|He decided to apply for the job.|📝",
                "salary|зарплата|The salary is paid every month.|💵",
                "schedule|розклад|My schedule is full this week.|🗓️",
                "to manage|керувати, справлятися|She can manage a big team.|🧑‍💼",
                "goal|мета|My goal is to speak freely.|🎯",
                "challenge|виклик, складне завдання|This project is a real challenge.|🧗",
                "feedback|відгук, зворотний зв'язок|Thank you for your feedback.|💬",
            ),
        ),
        Deck(
            "phrasal-verbs", "Фразові дієслова", "B1", "🧩", Difficulty.HARD,
            listOf(
                "to figure out|з'ясувати, розібратися|Let's figure out how it works.|🧩",
                "to give up|здаватися|Do not give up so early.|🏳️",
                "to look for|шукати|I am going to look for my keys.|🔎",
                "to find out|дізнатися|I want to find out the truth.|💡",
                "to turn on|вмикати|Please turn on the light.|🔛",
                "to turn off|вимикати|Do not forget to turn off the TV.|📴",
                "to run out of|закінчитися (про запас)|We may run out of time.|⌛",
                "to get along|ладнати|I get along with my neighbours.|🤗",
                "to put off|відкладати|Do not put off your homework.|🗓️",
                "to come up with|придумати|We need to come up with a plan.|🧠",
                "to pick up|забрати, підняти|I will pick up the kids at five.|🚗",
                "to carry on|продовжувати|Carry on with your work.|➡️",
            ),
        ),
    )
}

/** Short texts so the reader is not empty on first launch. */
object StarterArticles {
    fun create(now: Long = System.currentTimeMillis()): List<Article> = listOf(
        Article(
            id = UUID.randomUUID().toString(),
            title = "My Morning",
            level = "A1",
            createdAt = now,
            text = "I wake up at seven o'clock. First, I open the window and look at the street. " +
                "The air is fresh and cold. Then I go to the kitchen and make tea. " +
                "I eat bread with cheese and one egg. After breakfast I wash the dishes.\n\n" +
                "At eight I take my bag and my keys. I close the door and walk to the bus stop. " +
                "The bus is often late, so I listen to music and wait. " +
                "I like mornings because the city is quiet and the day is new.",
        ),
        Article(
            id = UUID.randomUUID().toString(),
            title = "A Trip to the Mountains",
            level = "A2",
            createdAt = now - 1,
            text = "Last summer my friends and I decided to visit the mountains. " +
                "We booked train tickets a week before the trip and packed our luggage the night before. " +
                "The journey took six hours, but nobody was bored: we played cards and looked out of the window.\n\n" +
                "On the first day the weather was perfect, so we climbed to a small lake. " +
                "The water was very cold, and only one of us was brave enough to swim. " +
                "In the evening we cooked soup on a fire and talked until midnight.\n\n" +
                "On the way home our train had a delay of two hours. We were tired, but everyone agreed " +
                "that we should come back next year.",
        ),
        Article(
            id = UUID.randomUUID().toString(),
            title = "How to Remember New Words",
            level = "B1",
            createdAt = now - 2,
            text = "Many learners give up because new words disappear from memory after a few days. " +
                "This is normal: the brain forgets information that it does not use. " +
                "The solution is to meet each word again just before you forget it.\n\n" +
                "This method is called spaced repetition. At first you review a word after a few minutes, " +
                "then after a day, then after a week, and finally after several months. " +
                "Each successful review makes the memory stronger, so the pause can become longer.\n\n" +
                "It also helps to learn words in context. A single word is easy to forget, " +
                "but a sentence from a video or an article gives it a meaning and a feeling. " +
                "When you come across an unknown word, save it together with its sentence " +
                "and try to use it in your own example the same day.",
        ),
    )
}
