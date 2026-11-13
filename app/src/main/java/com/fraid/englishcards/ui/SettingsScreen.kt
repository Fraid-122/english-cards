package com.fraid.englishcards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.fraid.englishcards.data.AiClient
import com.fraid.englishcards.data.AiSuggestion
import com.fraid.englishcards.data.VoiceClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val KEY_PAGE = "https://aistudio.google.com/apikey"

@Composable
fun SettingsScreen(
    apiKey: String,
    model: String,
    onSave: (key: String, model: String) -> Unit,
    onTest: suspend (key: String, model: String) -> AiSuggestion,
    voiceKey: String,
    voiceId: String,
    voiceModel: String,
    onSaveVoice: (key: String, voice: String, model: String) -> Unit,
    onTestVoice: suspend (key: String, voice: String, model: String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var voiceKeyText by rememberSaveable { mutableStateOf(voiceKey) }
    var voiceIdText by rememberSaveable { mutableStateOf(voiceId) }
    var voiceModelText by rememberSaveable { mutableStateOf(voiceModel) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }
    var voiceStatusIsError by remember { mutableStateOf(false) }
    var voiceTesting by remember { mutableStateOf(false) }

    var keyText by rememberSaveable { mutableStateOf(apiKey) }
    var modelText by rememberSaveable { mutableStateOf(model) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = { AppTopBar(title = "Налаштування", onBack = onBack) },
    ) { padding ->
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
            Text("ШІ для картинок і тем", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Переклад працює офлайн і без ключа. ШІ потрібен лише для картинки, теми, " +
                    "складності та прикладу речення. Він працює через безкоштовний API-ключ " +
                    "Google Gemini і потребує інтернету.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Як отримати ключ:\n" +
                    "1. Натисни кнопку нижче і увійди в акаунт Google.\n" +
                    "2. Створи API-ключ (Create API key) і скопіюй його.\n" +
                    "3. Встав ключ сюди і натисни «Зберегти».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { runCatching { uriHandler.openUri(KEY_PAGE) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Відкрити сторінку ключів Google")
            }

            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = { Text("API-ключ Gemini") },
                supportingText = { Text("Ключ зберігається лише на цьому телефоні.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = modelText,
                onValueChange = { modelText = it },
                label = { Text("Модель") },
                supportingText = { Text("Зазвичай міняти не треба. Стандартна: ${AiClient.DEFAULT_MODEL}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        testing = true
                        status = "Перевіряю…"
                        statusIsError = false
                        scope.launch {
                            try {
                                val result = onTest(keyText, modelText)
                                status = "Працює: apple → ${result.translation} ${result.emoji}"
                                statusIsError = false
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                status = e.message ?: "Помилка ШІ."
                                statusIsError = true
                            } finally {
                                testing = false
                            }
                        }
                    },
                    enabled = !testing && keyText.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Перевірити")
                }
                Button(
                    onClick = {
                        onSave(keyText, modelText)
                        status = "Збережено."
                        statusIsError = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Зберегти")
                }
            }

            val message = status
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusIsError) MaterialTheme.colorScheme.error else KnownGreen,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Озвучення ElevenLabs", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Якісний голос замість вбудованого голосу телефона. Кожна фраза озвучується один раз " +
                    "і зберігається на телефоні, тож повторне прослуховування не витрачає символи підписки " +
                    "і працює без інтернету. Без ключа лишається голос телефона.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Ключ створюється на сайті ElevenLabs у розділі API Keys. " +
                    "Йому потрібен дозвіл на Text to Speech.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { runCatching { uriHandler.openUri("https://elevenlabs.io/app/settings/api-keys") } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Відкрити ключі ElevenLabs")
            }
            OutlinedTextField(
                value = voiceKeyText,
                onValueChange = { voiceKeyText = it },
                label = { Text("API-ключ ElevenLabs") },
                supportingText = { Text("Ключ зберігається лише на цьому телефоні.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = voiceIdText,
                onValueChange = { voiceIdText = it },
                label = { Text("ID голосу") },
                supportingText = { Text("Скопіюй ID улюбленого голосу з ElevenLabs. Стандартний: George.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = voiceModelText,
                onValueChange = { voiceModelText = it },
                label = { Text("Модель") },
                supportingText = {
                    Text("Стандартна: ${VoiceClient.DEFAULT_MODEL}. Дешевша: eleven_flash_v2_5.")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        voiceTesting = true
                        voiceStatus = "Озвучую приклад…"
                        voiceStatusIsError = false
                        scope.launch {
                            try {
                                onTestVoice(voiceKeyText, voiceIdText, voiceModelText)
                                voiceStatus = "Працює. Якщо голос подобається, натисни «Зберегти»."
                                voiceStatusIsError = false
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                voiceStatus = e.message ?: "Помилка озвучення."
                                voiceStatusIsError = true
                            } finally {
                                voiceTesting = false
                            }
                        }
                    },
                    enabled = !voiceTesting && voiceKeyText.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Послухати")
                }
                Button(
                    onClick = {
                        onSaveVoice(voiceKeyText, voiceIdText, voiceModelText)
                        voiceStatus = "Збережено."
                        voiceStatusIsError = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Зберегти")
                }
            }
            val voiceMessage = voiceStatus
            if (voiceMessage != null) {
                Text(
                    text = voiceMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (voiceStatusIsError) MaterialTheme.colorScheme.error else KnownGreen,
                )
            }
        }
    }
}
