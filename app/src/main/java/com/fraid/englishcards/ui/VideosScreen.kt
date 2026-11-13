package com.fraid.englishcards.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fraid.englishcards.data.TextSpan
import com.fraid.englishcards.data.Video

private const val PLAYER_ORIGIN = "https://com.fraid.englishcards"

@Composable
fun VideosScreen(
    videos: List<Video>,
    busy: Boolean,
    onOpen: (Video) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (Video) -> Unit,
    onBack: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }

    Scaffold(
        topBar = { AppTopBar(title = "Відео", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = "Додати відео")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "hint") {
                Text(
                    text = when {
                        busy -> "Завантажую відео й субтитри…"
                        videos.isEmpty() -> "Натисни «+» і встав посилання на відео з YouTube англійською."
                        else -> "Торкайся слів у субтитрах, щоб зберігати картки."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(videos, key = { it.id }) { video ->
                HubRow(
                    emoji = "🎬",
                    title = video.title,
                    subtitle = when {
                        video.lines.isNotEmpty() -> "Субтитри: ${video.lines.size} рядків"
                        video.text.isNotBlank() -> "Текст додано вручну"
                        else -> "Без субтитрів"
                    },
                    onClick = { onOpen(video) },
                    trailing = {
                        IconButton(onClick = { onDelete(video) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Видалити")
                        }
                    },
                )
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Додати відео") },
            text = {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Посилання на YouTube") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAdd(link)
                        link = ""
                        adding = false
                    },
                    enabled = link.isNotBlank(),
                ) { Text("Додати") }
            },
            dismissButton = {
                TextButton(onClick = { adding = false }) { Text("Скасувати") }
            },
        )
    }
}

/** Receives the player's position and state from the page inside the WebView. */
private class PlayerBridge(
    private val onPosition: (Long) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onTime(seconds: Double) {
        main.post { onPosition((seconds * 1000).toLong()) }
    }

    @JavascriptInterface
    fun onReady() {
        main.post { onStatus("") }
    }

    @JavascriptInterface
    fun onPlayerError(code: Int) {
        val reason = when (code) {
            2 -> "неправильне посилання на відео"
            5 -> "плеєр не зміг відтворити це відео"
            100 -> "відео видалене або приватне"
            101, 150 -> "автор заборонив показувати це відео поза YouTube"
            else -> "помилка плеєра $code"
        }
        main.post { onStatus("Відео не відкрилось: $reason.") }
    }
}

/**
 * The page shown in the WebView. The video is a plain YouTube embed, so it appears even if
 * the control script fails to load; the script only adds pausing, seeking and the position.
 */
private fun playerPage(videoId: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <meta name="referrer" content="strict-origin-when-cross-origin">
      <style>
        html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #0e0f13; overflow: hidden; }
        iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
      </style>
    </head>
    <body>
      <iframe id="player"
        src="https://www.youtube.com/embed/$videoId?enablejsapi=1&playsinline=1&rel=0&cc_load_policy=1&cc_lang_pref=en&origin=$PLAYER_ORIGIN"
        allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
        referrerpolicy="strict-origin-when-cross-origin"
        allowfullscreen></iframe>
      <script>
        var player;
        function onYouTubeIframeAPIReady() {
          player = new YT.Player('player', {
            events: {
              onReady: function () {
                try { Android.onReady(); } catch (e) {}
                setInterval(function () {
                  try {
                    if (player && player.getCurrentTime) { Android.onTime(player.getCurrentTime()); }
                  } catch (e) {}
                }, 400);
              },
              onError: function (event) {
                try { Android.onPlayerError(event.data); } catch (e) {}
              }
            }
          });
        }
        function pauseVideo() { try { player.pauseVideo(); } catch (e) {} }
        function seekTo(seconds) { try { player.seekTo(seconds, true); player.playVideo(); } catch (e) {} }
        var tag = document.createElement('script');
        tag.src = 'https://www.youtube.com/iframe_api';
        document.head.appendChild(tag);
      </script>
    </body>
    </html>
""".trimIndent()

/** The player with selectable subtitles under it. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoScreen(
    video: Video,
    busy: Boolean,
    aiEnabled: Boolean,
    actions: WordActions,
    onSaveText: (String) -> Unit,
    onReloadSubtitles: () -> Unit,
    onTranscribe: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var positionMs by remember { mutableStateOf(0L) }
    var playerStatus by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<PickedWord?>(null) }
    var quickWord by remember { mutableStateOf("") }
    var pasted by remember { mutableStateOf("") }
    // The selected piece of the subtitles: which line (or -1 for the pasted text) and where in it.
    var selectedLine by remember { mutableStateOf(-1) }
    var selection by remember { mutableStateOf<TextSpan?>(null) }

    val webView = remember(video.videoId) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(0xFF0E0F13.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true) {
                        playerStatus = "Плеєр не завантажився: ${error?.description ?: "невідома помилка"}."
                    }
                }
            }
            addJavascriptInterface(
                PlayerBridge(onPosition = { positionMs = it }, onStatus = { playerStatus = it }),
                "Android",
            )
            loadDataWithBaseURL(PLAYER_ORIGIN, playerPage(video.videoId), "text/html", "utf-8", null)
        }
    }
    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    fun pick(word: PickedWord) {
        webView.evaluateJavascript("pauseVideo()", null)
        picked = word
    }

    fun select(line: Int, span: TextSpan) {
        if (selection == null) webView.evaluateJavascript("pauseVideo()", null)
        selectedLine = line
        selection = span
    }

    Scaffold(topBar = { AppTopBar(title = video.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = playerStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        runCatching { uriHandler.openUri("https://www.youtube.com/watch?v=${video.videoId}") }
                    },
                ) { Text("Відкрити в YouTube") }
            }

            // Quick add: type a word you heard or saw in the player's own subtitles.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = quickWord,
                    onValueChange = { quickWord = it },
                    placeholder = { Text("Слово чи фраза з відео") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        pick(PickedWord(quickWord.trim(), ""))
                        quickWord = ""
                    },
                    enabled = quickWord.isNotBlank(),
                ) { Text("У картку") }
            }

            when {
                video.lines.isNotEmpty() -> {
                    val current = video.lines.indexOfLast { it.startMs <= positionMs }
                    val listState = rememberLazyListState()
                    LaunchedEffect(current) {
                        if (current >= 0) listState.animateScrollToItem(maxOf(current - 1, 0))
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(video.lines) { index, line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "▶",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        webView.evaluateJavascript("seekTo(${line.startMs / 1000.0})", null)
                                    },
                                )
                                SelectableText(
                                    text = line.text,
                                    selection = if (index == selectedLine) selection else null,
                                    onSelection = { select(index, it) },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                                    color = if (index == current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                video.text.isNotBlank() -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        SelectableText(
                            text = video.text,
                            selection = if (selectedLine == -1) selection else null,
                            onSelection = { select(-1, it) },
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp),
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Субтитрів немає" +
                                (if (video.note.isNotBlank()) " (${video.note})" else "") +
                                ". Є три способи їх отримати:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (busy) {
                            Text(
                                text = "Працюю… Розшифровка довгого відео може тривати кілька хвилин.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        OutlinedButton(
                            onClick = onReloadSubtitles,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("1. Спробувати завантажити з YouTube ще раз") }
                        Button(
                            onClick = onTranscribe,
                            enabled = !busy && aiEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("2. Створити субтитри через ШІ") }
                        Text(
                            text = if (aiEnabled) "ШІ слухає відео й записує текст. Найкраще працює для відео до 15 хвилин."
                            else "Для цього потрібен API-ключ Gemini у вкладці «Опції».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "3. Вставити текст субтитрів чи розшифровку вручну:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = pasted,
                            onValueChange = { pasted = it },
                            label = { Text("Текст субтитрів") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { onSaveText(pasted.trim()) },
                            enabled = pasted.isNotBlank(),
                        ) { Text("Зберегти текст") }
                    }
                }
            }

            val span = selection
            val selectedText = if (selectedLine >= 0) video.lines.getOrNull(selectedLine)?.text else video.text
            if (span != null && selectedText != null) {
                SelectionPanel(
                    text = selectedText,
                    span = span,
                    onSpan = { selection = it },
                    topic = "Відео",
                    actions = actions,
                    onClose = { selection = null },
                )
            }
        }
    }

    val current = picked
    if (current != null) {
        WordDialog(
            picked = current,
            topic = "Відео",
            actions = actions,
            onDismiss = { picked = null },
        )
    }
}
