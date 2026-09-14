package ru.yandex.browser.mobile.core

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse

/**
 * Вкладка = отдельная GeckoSession (собственный процесс рендеринга, куки-контекст,
 * история, состояние). Сессии не закрываются при сворачивании приложения —
 * сайт продолжает работать без перезагрузки.
 */
class Tab(
    val id: Long,
    val session: GeckoSession,
    val isPrivate: Boolean,
) {
    var title by mutableStateOf("")
    var url by mutableStateOf("")
    var progress by mutableStateOf(0)
    var loading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var crashed by mutableStateOf(false)

    /** Сериализованное состояние сессии (история, формы, скролл) для восстановления. */
    var stateString: String? = null

    val host: String get() = Urls.prettyHost(url)
}

/**
 * Менеджер вкладок: создание/закрытие/переключение + сохранение сессии на диск.
 *
 * Ключевая особенность: состояние вкладок (включая историю и формы) сохраняется
 * через GeckoSession.SessionState, поэтому после сворачивания/перезапуска
 * страницы НЕ перезагружаются.
 */
class TabManager(private val context: Context) {

    companion object {
        private const val TAG = "TabManager"
        private const val FILE = "yb_tabs"
        private const val KEY_STATE = "tabs_state"
    }

    val tabs = mutableStateListOf<Tab>()

    var activeId by mutableStateOf(-1L)
        private set

    var fullscreen by mutableStateOf(false)
        private set

    private var nextId = 1L
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    val activeTab: Tab? get() = tabs.firstOrNull { it.id == activeId }

    init {
        restoreTabs()
    }

    // ------------------------------------------------------------------
    //  Вкладки
    // ------------------------------------------------------------------

    fun newTab(url: String? = null, private: Boolean = false, select: Boolean = true): Tab {
        val runtime = BrowserEngine.bootstrap(context)
        val session = GeckoSession(createSessionSettings(private))
        val tab = Tab(nextId++, session, private)
        wireDelegates(tab)
        session.open(runtime)
        session.setActive(false)
        session.setFocused(false)
        tabs.add(tab)
        if (select) {
            selectTab(tab.id)
        }
        if (!url.isNullOrBlank() && !Urls.isAbout(url)) {
            tab.session.loadUri(url)
        }
        persist()
        return tab
    }

    fun selectTab(id: Long) {
        activeId = id
        tabs.firstOrNull { it.id == id }?.let { tab ->
            tab.session.setActive(true)
            tab.session.setFocused(true)
            // Остальные вкладки остаются ОТКРЫТЫМИ: контент не выгружается,
            // сайты продолжают жить (это и есть фишка фонового режима).
            tabs.filter { it.id != id }.forEach { other ->
                other.session.setFocused(false)
                if (!Settings.backgroundMode) other.session.setActive(false)
            }
        }
        persist()
    }

    fun closeTab(tab: Tab) {
        val wasActive = tab.id == activeId
        tabs.remove(tab)
        runCatching {
            tab.session.setActive(false)
            tab.session.close()
        }
        if (tabs.isEmpty()) {
            newTab(Urls.ABOUT_HOME)
        } else if (wasActive) {
            selectTab(tabs.last().id)
        }
        persist()
    }

    fun closeAll(keepPrivate: Boolean = false) {
        tabs.toList().forEach { tab ->
            if (keepPrivate && tab.isPrivate) return@forEach
            tabs.remove(tab)
            runCatching {
                tab.session.setActive(false)
                tab.session.close()
            }
        }
        if (tabs.isEmpty()) newTab(Urls.ABOUT_HOME)
        selectTab(tabs.last().id)
        persist()
    }

    fun load(url: String) {
        val tab = activeTab ?: newTab(url)
        if (Urls.isAbout(url)) {
            tab.url = url
        } else {
            tab.session.loadUri(url)
        }
    }

    fun reload() {
        activeTab?.session?.reload()
    }

    fun stop() {
        activeTab?.session?.stop()
    }

    fun goBack() {
        activeTab?.session?.goBack()
    }

    fun goForward() {
        activeTab?.session?.goForward()
    }

    /**
     * «Версия для ПК» применяется на лету ко всем открытым вкладкам:
     * GeckoSessionSettings изменяется на месте (setUserAgentMode), затем
     * страница перезагружается, чтобы сервер отдал десктопную версию.
     */
    fun applyUserAgentMode(desktop: Boolean) {
        tabs.forEach { tab ->
            runCatching {
                tab.session.settings.userAgentMode =
                    if (desktop) {
                        GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    } else {
                        GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                    }
                if (!Urls.isAbout(tab.url) && tab.url.isNotBlank()) {
                    tab.session.reload()
                }
            }.onFailure { Log.w(TAG, "applyUserAgentMode failed", it) }
        }
    }

    // ------------------------------------------------------------------
    //  Настройки сессии
    // ------------------------------------------------------------------

    private fun createSessionSettings(private: Boolean): GeckoSessionSettings =
        GeckoSessionSettings.Builder()
            .usePrivateMode(private)
            .useTrackingProtection(Settings.trafficSaver)
            .contextId(if (private) "private" else "default")
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .userAgentMode(
                if (Settings.desktopMode) {
                    GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                } else {
                    GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                },
            )
            // Медиа в фоне не должно останавливаться — это часть фишки браузера.
            .suspendMediaWhenInactive(false)
            .build()

    // ------------------------------------------------------------------
    //  Делегаты: связываем движок и UI
    // ------------------------------------------------------------------

    private fun wireDelegates(tab: Tab) {
        val session = tab.session

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(s: GeckoSession, title: String?) {
                if (!title.isNullOrBlank()) {
                    tab.title = title
                    Settings.addHistory(title, tab.url)
                    persistSoon()
                }
            }

            override fun onFullScreen(s: GeckoSession, fullScreen: Boolean) {
                fullscreen = fullScreen
            }

            override fun onCrash(s: GeckoSession) {
                tab.crashed = true
                tab.loading = false
            }

            override fun onKill(s: GeckoSession) {
                tab.crashed = true
                tab.loading = false
            }

            override fun onExternalResponse(s: GeckoSession, response: WebResponse) {
                // В GeckoView 155 ответ отдаётся как WebResponse: адрес + заголовки.
                val mime = response.headers["Content-Type"]?.substringBefore(';')?.trim()
                val filename = Filenames.fromHeaders(response.headers, response.uri)
                Downloads.enqueue(context, response.uri, mime, filename)
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                tab.loading = true
                tab.crashed = false
                tab.progress = 5
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                tab.loading = false
                tab.progress = 100
                if (success && !Urls.isAbout(tab.url)) {
                    Settings.addHistory(tab.title, tab.url)
                }
                persistSoon()
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                tab.progress = progress.coerceIn(0, 100)
            }

            override fun onSessionStateChange(s: GeckoSession, state: GeckoSession.SessionState) {
                // Здесь и живёт «магия»: состояние вкладки сериализуется и
                // восстанавливается без перезагрузки страницы.
                tab.stateString = runCatching { state.toString() }.getOrNull()
                persistSoon()
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                s: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                url?.let { tab.url = it }
            }

            override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                tab.canGoBack = canGoBack
            }

            override fun onCanGoForward(s: GeckoSession, canGoForward: Boolean) {
                tab.canGoForward = canGoForward
            }

            override fun onNewSession(
                s: GeckoSession,
                uri: String,
            ): org.mozilla.geckoview.GeckoResult<GeckoSession>? {
                // window.open / target=_blank открываем как новую вкладку.
                val fresh = newTab(url = null, private = tab.isPrivate, select = true)
                fresh.url = uri
                return org.mozilla.geckoview.GeckoResult.fromValue(fresh.session)
            }
        }

        session.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(s: GeckoSession, event: ContentBlocking.BlockEvent) {
                val estimated = TrafficStats.estimate(
                    antiTracking = event.antiTrackingCategory,
                    safeBrowsing = event.safeBrowsingCategory,
                    cookie = event.cookieBehaviorCategory,
                    uri = event.uri,
                )
                TrafficStats.onBlocked(estimated)
                Settings.addSavedBytes(estimated)
            }
        }
    }

    // ------------------------------------------------------------------
    //  Сохранение / восстановление
    // ------------------------------------------------------------------

    private var persistScheduled = false

    private fun persistSoon() {
        if (persistScheduled) return
        persistScheduled = true
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            {
                persistScheduled = false
                persist()
            },
            700,
        )
    }

    fun persist() {
        runCatching {
            val arr = JSONArray()
            tabs.forEach { tab ->
                arr.put(
                    JSONObject()
                        .put("url", tab.url)
                        .put("title", tab.title)
                        .put("private", tab.isPrivate)
                        .put("state", tab.stateString ?: ""),
                )
            }
            val root = JSONObject()
                .put("active", tabs.indexOfFirst { it.id == activeId })
                .put("tabs", arr)
            prefs.edit().putString(KEY_STATE, root.toString()).apply()
        }.onFailure { Log.w(TAG, "persist failed", it) }
    }

    private fun restoreTabs() {
        val raw = prefs.getString(KEY_STATE, null)
        val restored = runCatching {
            val root = JSONObject(raw ?: return@runCatching emptyList<Triple<String, String, Boolean>>())
            val arr = root.optJSONArray("tabs") ?: JSONArray()
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Triple(o.optString("url"), o.optString("state"), o.optBoolean("private", false))
            }
        }.getOrElse { emptyList() }

        if (restored.isEmpty()) {
            newTab(Urls.ABOUT_HOME)
            return
        }

        val runtime = BrowserEngine.bootstrap(context)
        restored.forEach { (url, stateJson, private) ->
            runCatching {
                val session = GeckoSession(createSessionSettings(private))
                val tab = Tab(nextId++, session, private)
                tab.url = url
                if (stateJson.isNotBlank()) {
                    GeckoSession.SessionState.fromString(stateJson)?.let { session.restoreState(it) }
                    tab.stateString = stateJson
                }
                wireDelegates(tab)
                session.open(runtime)
                session.setActive(false)
                tabs.add(tab)
            }.onFailure { Log.w(TAG, "Не удалось восстановить вкладку $url", it) }
        }

        if (tabs.isEmpty()) {
            newTab(Urls.ABOUT_HOME)
        } else {
            val activeIndex = (raw?.let { runCatching { JSONObject(it).optInt("active", 0) }.getOrNull() } ?: 0)
                .coerceIn(0, tabs.size - 1)
            selectTab(tabs[activeIndex].id)
        }
    }

    /** Полная остановка движка (выход из браузера). */
    fun shutdown() {
        persist()
        tabs.forEach { tab -> runCatching { tab.session.close() } }
        tabs.clear()
    }
}

/**
 * Загрузки: отдаём системному DownloadManager — он умеет докачку,
 * уведомления и корректно переживает сворачивание приложения.
 */
/** Имя файла для загрузки: из Content-Disposition, иначе из адреса. */
object Filenames {
    private val DISP = Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?")

    fun fromHeaders(headers: Map<String, String>, url: String): String? {
        val disp = headers.entries.firstOrNull { it.key.equals("Content-Disposition", true) }?.value
        disp?.let { header ->
            DISP.find(header)?.groupValues?.getOrNull(1)?.let { name ->
                if (name.isNotBlank()) return name.trim()
            }
        }
        val tail = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
        return tail.takeIf { it.isNotBlank() }
    }
}

object Downloads {
    private const val TAG = "Downloads"

    fun enqueue(context: Context, url: String, mime: String?, filename: String?) {
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mime)
                filename?.let { setTitle(it) } ?: setTitle(Urls.prettyHost(url))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }.onFailure { Log.w(TAG, "Не удалось поставить загрузку $url", it) }
    }
}
