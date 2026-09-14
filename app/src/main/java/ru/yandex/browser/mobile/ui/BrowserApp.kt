package ru.yandex.browser.mobile.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import org.mozilla.geckoview.GeckoSession
import ru.yandex.browser.mobile.core.BrowserEngine
import ru.yandex.browser.mobile.core.KeepAliveService
import ru.yandex.browser.mobile.core.SearchEngine
import ru.yandex.browser.mobile.core.Settings
import ru.yandex.browser.mobile.core.TabManager
import ru.yandex.browser.mobile.core.Urls
import ru.yandex.browser.mobile.ui.icons.BIcons
import ru.yandex.browser.mobile.ui.theme.AccentRed
import ru.yandex.browser.mobile.ui.theme.PureBlack
import ru.yandex.browser.mobile.web.GeckoViewHost

/** Состояние интерфейса браузера (панели, поиск, тосты). */
class BrowserUiState {
    var showTabs by mutableStateOf(false)
    var showMenu by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var omnibox by mutableStateOf(false)
    var findVisible by mutableStateOf(false)
    var library by mutableStateOf<LibraryKind?>(null)
    var editTiles by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    var showDiagnostics by mutableStateOf(false)
}

@Composable
fun BrowserApp(manager: TabManager) {
    val ui = remember { BrowserUiState() }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val tab = manager.activeTab
    val isHome = tab == null || tab.url.isBlank() || Urls.isAbout(tab.url)

    var findQuery by remember { mutableStateOf("") }
    var findResult by remember { mutableStateOf("") }

    LaunchedEffect(ui.toast) {
        if (ui.toast != null) {
            delay(1800)
            ui.toast = null
        }
    }

    // Голосовой поиск через системный распознаватель речи.
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            manager.load(Urls.resolve(spoken, Settings.searchEngine))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (!manager.fullscreen) {
                Column(Modifier.statusBarsPadding()) {
                    TopBar(
                        tab = tab,
                        showChip = isHome,
                        onOpenOmnibox = { ui.omnibox = true },
                        onReload = { manager.reload() },
                        onStop = { manager.stop() },
                        onVoice = { startVoiceSearch(speechLauncher) { ui.toast = it } },
                        onToggleTrafficSaver = { Settings.applyTrafficSaver(!Settings.trafficSaver) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // GeckoView живёт здесь всегда: главная — просто оверлей поверх,
                // поэтому сайт не перезагружается при возврате к нему.
                GeckoViewHost(tab = tab, modifier = Modifier.fillMaxSize())

                val errorText = tab?.errorText
                if (tab != null && !isHome && errorText != null) {
                    ErrorOverlay(
                        title = if (tab.crashed) "Вкладка перестала работать" else "Не удалось открыть страницу",
                        message = errorText,
                        url = tab.url,
                        onRetry = { manager.reviveTab(tab) },
                        onOpenHome = { manager.load(Urls.ABOUT_HOME) },
                    )
                }

                if (isHome) {
                    HomeScreen(
                        editMode = ui.editTiles,
                        onOpen = { url -> manager.load(url) },
                        onRemoveTile = { url -> Settings.removeSpeedDial(url) },
                        onToggleEdit = { ui.editTiles = !ui.editTiles },
                        onOpenBookmarks = { ui.library = LibraryKind.BOOKMARKS },
                    )
                }
            }

            if (!manager.fullscreen) {
                Column(Modifier.navigationBarsPadding()) {
                    if (ui.findVisible) {
                        FindBar(
                            query = findQuery,
                            resultText = findResult,
                            onQueryChange = { value ->
                                findQuery = value
                                if (value.isBlank()) {
                                    tab?.session?.finder?.clear()
                                    findResult = ""
                                } else {
                                    findInPage(tab?.session, value, forward = true) { findResult = it }
                                }
                            },
                            onPrev = { findInPage(tab?.session, findQuery, forward = false) { findResult = it } },
                            onNext = { findInPage(tab?.session, findQuery, forward = true) { findResult = it } },
                            onClose = {
                                tab?.session?.finder?.clear()
                                findQuery = ""
                                findResult = ""
                                ui.findVisible = false
                            },
                        )
                    }
                    BottomBar(
                        tab = tab,
                        tabCount = manager.tabs.size,
                        onBack = { manager.goBack() },
                        onForward = { manager.goForward() },
                        onTabs = { ui.showTabs = true },
                        onTrafficSaver = {
                            Settings.applyTrafficSaver(!Settings.trafficSaver)
                            ui.toast = if (Settings.trafficSaver) "Экономия трафика включена" else "Экономия трафика выключена"
                        },
                        onMenu = { ui.showMenu = true },
                    )
                }
            }
        }

        // ---------------- Оверлеи ----------------

        if (ui.omnibox) {
            OmniboxOverlay(
                initialText = if (isHome) "" else tab?.url.orEmpty(),
                onDismiss = { ui.omnibox = false },
                onSubmit = { input ->
                    ui.omnibox = false
                    val target = Urls.resolve(input, Settings.searchEngine)
                    android.util.Log.i("YBBrowser", "SEARCH \"$input\" -> $target")
                    manager.load(target)
                },
            )
        }

        if (ui.showTabs) {
            TabSwitcherPanel(
                manager = manager,
                onSelect = { selected ->
                    manager.selectTab(selected.id)
                    ui.showTabs = false
                },
                onCloseTab = { closed -> manager.closeTab(closed) },
                onNewTab = { private ->
                    manager.newTab(url = Urls.ABOUT_HOME, private = private)
                    ui.showTabs = false
                },
                onDismiss = { ui.showTabs = false },
            )
        }

        if (ui.showMenu) {
            MenuPanel(
                entries = buildMenu(manager, ui, clipboard, context) { ui.showMenu = false; manager.persist() },
                onDismiss = { ui.showMenu = false },
            )
        }

        if (ui.showDiagnostics) {
            DiagnosticsScreen(
                manager = manager,
                onBack = { ui.showDiagnostics = false },
                onRunCheck = {
                    ui.showDiagnostics = false
                    val target = "https://ya.ru"
                    Log.i("YBBrowser", "SELFTEST: проверяем загрузку $target")
                    manager.load(target)
                    ui.toast = "Проверка запущена — смотрите, откроется ли сайт"
                },
            )
        }

        ui.library?.let { kind ->
            LibraryScreen(
                kind = kind,
                onBack = { ui.library = null },
                onOpen = { url ->
                    ui.library = null
                    manager.load(url)
                },
            )
        }

        if (ui.showSettings) {
            SettingsScreen(
                onBack = { ui.showSettings = false },
                onClearData = { flags, message ->
                    BrowserEngine.clearData(flags) { ui.toast = message }
                },
            )
        }

        ui.toast?.let { message ->
            ToastBar(text = message, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Меню браузера. */
private fun buildMenu(
    manager: TabManager,
    ui: BrowserUiState,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context,
    dismiss: () -> Unit,
): List<MenuEntry> {
    val tab = manager.activeTab
    val url = tab?.url.orEmpty()
    val entries = mutableListOf<MenuEntry>()

    entries += MenuEntry(BIcons.Plus, "Новая вкладка") {
        dismiss()
        manager.newTab(Urls.ABOUT_HOME)
    }
    entries += MenuEntry(BIcons.PrivateEye, "Новая приватная вкладка") {
        dismiss()
        manager.newTab(Urls.ABOUT_HOME, private = true)
    }
    entries += MenuEntry(BIcons.Reload, "Обновить") {
        dismiss()
        manager.reload()
    }
    if (!Urls.isAbout(url) && url.isNotBlank()) {
        if (Settings.isBookmarked(url)) {
            entries += MenuEntry(BIcons.BookmarkFilled, "Убрать из закладок") {
                dismiss()
                Settings.removeBookmark(url)
            }
        } else {
            entries += MenuEntry(BIcons.Bookmark, "В закладки") {
                dismiss()
                Settings.addBookmark(tab?.title.orEmpty(), url)
            }
        }
        entries += MenuEntry(BIcons.Share, "Поделиться") {
            dismiss()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, "Поделиться ссылкой")) }
        }
        entries += MenuEntry(BIcons.Copy, "Копировать ссылку") {
            dismiss()
            clipboard.setText(AnnotatedString(url))
            ui.toast = "Ссылка скопирована"
        }
    }
    entries += MenuEntry(BIcons.Bookmark, "Закладки") {
        dismiss()
        ui.library = LibraryKind.BOOKMARKS
    }
    entries += MenuEntry(BIcons.History, "История") {
        dismiss()
        ui.library = LibraryKind.HISTORY
    }
    entries += MenuEntry(BIcons.Search, "Найти на странице") {
        dismiss()
        ui.findVisible = true
    }
    entries += MenuEntry(BIcons.Desktop, if (Settings.desktopMode) "Мобильная версия" else "Версия для ПК") {
        dismiss()
        Settings.applyDesktopMode(!Settings.desktopMode)
    }
    entries += MenuEntry(BIcons.Shield, "Диагностика движка") {
        dismiss()
        ui.showDiagnostics = true
    }
    entries += MenuEntry(BIcons.Settings, "Настройки") {
        dismiss()
        ui.showSettings = true
    }
    entries += MenuEntry(BIcons.Stop, "Выход") {
        dismiss()
        KeepAliveService.stop(context)
        manager.persist()
        (context as? Activity)?.finishAffinity()
    }
    return entries
}

private fun startVoiceSearch(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onError: (String) -> Unit,
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Голосовой поиск")
    }
    runCatching { launcher.launch(intent) }.onFailure { onError("Голосовой поиск недоступен") }
}

/** Поиск по странице средствами движка (без WebView и JS-костылей). */
private fun findInPage(
    session: GeckoSession?,
    query: String,
    forward: Boolean,
    onResult: (String) -> Unit,
) {
    if (session == null || query.isBlank()) {
        onResult("")
        return
    }
    val flags = if (forward) {
        GeckoSession.FINDER_FIND_FORWARD
    } else {
        GeckoSession.FINDER_FIND_BACKWARDS
    }
    runCatching {
        session.finder.find(query, flags).accept(
            { result ->
                val found = result?.total ?: 0
                val current = if (found > 0) (result?.current ?: 0) else 0
                onResult(if (found > 0) "$current/$found" else "0/0")
            },
            { onResult("") },
        )
    }.onFailure { onResult("") }
}
