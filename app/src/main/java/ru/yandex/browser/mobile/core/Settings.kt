package ru.yandex.browser.mobile.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Ссылка (плитка быстрого доступа / закладка). */
data class SiteLink(
    val title: String,
    val url: String,
    val addedAt: Long = 0L,
)

/** Запись истории. */
data class HistoryItem(
    val title: String,
    val url: String,
    val visitedAt: Long,
)

/**
 * Все пользовательские настройки и локальные данные (плитки, закладки, история).
 *
 * Реализовано как Compose-наблюдаемое состояние (mutableStateOf) — интерфейс
 * перерисовывается автоматически, без ручных вызовов notifyDataSetChanged.
 * Хранение — SharedPreferences + JSON (минимум зависимостей, мгновенный старт).
 */
object Settings {

    private const val FILE = "yb_settings"
    private const val K_ENGINE = "search_engine"
    private const val K_TRAFFIC = "traffic_saver"
    private const val K_DARK = "dark_mode"
    private const val K_BACKGROUND = "background_mode"
    private const val K_DESKTOP = "desktop_mode"
    private const val K_SPEED_DIAL = "speed_dial"
    private const val K_BOOKMARKS = "bookmarks"
    private const val K_HISTORY = "history"
    private const val K_SAVED_BYTES = "saved_bytes"

    private lateinit var sp: SharedPreferences
    private var ready = false

    // ---------------- Настройки ----------------

    var searchEngine by mutableStateOf(SearchEngine.YANDEX)
        private set
    var trafficSaver by mutableStateOf(true)
        private set
    var darkMode by mutableStateOf(true)
        private set
    var backgroundMode by mutableStateOf(true)
        private set
    var desktopMode by mutableStateOf(false)
        private set

    /** Суммарная оценка сэкономленного трафика (байты), живёт между запусками. */
    var savedBytesTotal by mutableStateOf(0L)
        private set

    // ---------------- Локальные данные ----------------

    val speedDial = mutableStateListOf<SiteLink>()
    val bookmarks = mutableStateListOf<SiteLink>()
    val history = mutableStateListOf<HistoryItem>()

    /** Колбэки для применения настроек к движку (без перезапуска). */
    var onTrafficSaverChanged: ((Boolean) -> Unit)? = null
    var onDarkModeChanged: ((Boolean) -> Unit)? = null
    var onDesktopModeChanged: ((Boolean) -> Unit)? = null

    // ---------------- Инициализация ----------------

    fun init(context: Context) {
        if (ready) return
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        searchEngine = SearchEngine.fromId(sp.getString(K_ENGINE, null))
        trafficSaver = sp.getBoolean(K_TRAFFIC, true)
        darkMode = sp.getBoolean(K_DARK, true)
        backgroundMode = sp.getBoolean(K_BACKGROUND, true)
        desktopMode = sp.getBoolean(K_DESKTOP, false)
        savedBytesTotal = sp.getLong(K_SAVED_BYTES, 0L)

        speedDial.clear()
        speedDial.addAll(loadLinks(K_SPEED_DIAL).ifEmpty { defaultSpeedDial() })
        bookmarks.clear()
        bookmarks.addAll(loadLinks(K_BOOKMARKS))
        history.clear()
        history.addAll(loadHistory())
        ready = true
    }

    // ---------------- Сеттеры ----------------

    fun setSearchEngine(engine: SearchEngine) {
        searchEngine = engine
        sp.edit().putString(K_ENGINE, engine.id).apply()
    }

    fun setTrafficSaver(value: Boolean) {
        trafficSaver = value
        sp.edit().putBoolean(K_TRAFFIC, value).apply()
        onTrafficSaverChanged?.invoke(value)
    }

    fun setDarkMode(value: Boolean) {
        darkMode = value
        sp.edit().putBoolean(K_DARK, value).apply()
        onDarkModeChanged?.invoke(value)
    }

    fun setBackgroundMode(value: Boolean) {
        backgroundMode = value
        sp.edit().putBoolean(K_BACKGROUND, value).apply()
    }

    fun setDesktopMode(value: Boolean) {
        desktopMode = value
        sp.edit().putBoolean(K_DESKTOP, value).apply()
        onDesktopModeChanged?.invoke(value)
    }

    fun addSavedBytes(bytes: Long) {
        if (bytes <= 0) return
        savedBytesTotal += bytes
        sp.edit().putLong(K_SAVED_BYTES, savedBytesTotal).apply()
    }

    fun resetSavedBytes() {
        savedBytesTotal = 0L
        sp.edit().putLong(K_SAVED_BYTES, 0L).apply()
    }

    // ---------------- Плитки быстрого доступа ----------------

    fun addSpeedDial(title: String, url: String) {
        if (speedDial.any { it.url == url }) return
        speedDial.add(SiteLink(title = title, url = url, addedAt = System.currentTimeMillis()))
        saveLinks(K_SPEED_DIAL, speedDial)
    }

    fun removeSpeedDial(url: String) {
        speedDial.removeAll { it.url == url }
        saveLinks(K_SPEED_DIAL, speedDial)
    }

    // ---------------- Закладки ----------------

    fun isBookmarked(url: String): Boolean = bookmarks.any { it.url == url }

    fun addBookmark(title: String, url: String) {
        if (url.isBlank() || isBookmarked(url)) return
        bookmarks.add(0, SiteLink(title = title.ifBlank { Urls.prettyHost(url) }, url = url, addedAt = System.currentTimeMillis()))
        saveLinks(K_BOOKMARKS, bookmarks)
    }

    fun removeBookmark(url: String) {
        bookmarks.removeAll { it.url == url }
        saveLinks(K_BOOKMARKS, bookmarks)
    }

    // ---------------- История ----------------

    fun addHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val existing = history.indexOfFirst { it.url == url }
        if (existing >= 0) history.removeAt(existing)
        history.add(0, HistoryItem(title = title.ifBlank { Urls.prettyHost(url) }, url = url, visitedAt = System.currentTimeMillis()))
        while (history.size > 1500) history.removeAt(history.size - 1)
        sp.edit().putString(K_HISTORY, historyToJson(history).toString()).apply()
    }

    fun clearHistory() {
        history.clear()
        sp.edit().remove(K_HISTORY).apply()
    }

    fun clearBookmarks() {
        bookmarks.clear()
        saveLinks(K_BOOKMARKS, bookmarks)
    }

    /** Подсказки для адресной строки: закладки + история, без дублей. */
    fun suggestions(query: String, limit: Int = 7): List<HistoryItem> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        val out = LinkedHashMap<String, HistoryItem>()
        bookmarks.filter { it.url.lowercase().contains(q) || it.title.lowercase().contains(q) }
            .forEach { out[it.url] = HistoryItem(it.title, it.url, it.addedAt) }
        history.filter { it.url.lowercase().contains(q) || it.title.lowercase().contains(q) }
            .forEach { if (!out.containsKey(it.url)) out[it.url] = it }
        return out.values.take(limit)
    }

    // ---------------- JSON ----------------

    private fun linksToJson(list: List<SiteLink>): JSONArray {
        val arr = JSONArray()
        list.forEach { link ->
            arr.put(
                JSONObject()
                    .put("title", link.title)
                    .put("url", link.url)
                    .put("addedAt", link.addedAt),
            )
        }
        return arr
    }

    private fun saveLinks(key: String, list: List<SiteLink>) {
        if (!ready) return
        sp.edit().putString(key, linksToJson(list).toString()).apply()
    }

    private fun loadLinks(key: String): List<SiteLink> = runCatching {
        val raw = sp.getString(key, null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SiteLink(
                title = o.optString("title"),
                url = o.optString("url"),
                addedAt = o.optLong("addedAt"),
            )
        }
    }.getOrElse { emptyList() }

    private fun historyToJson(list: List<HistoryItem>): JSONArray {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject()
                    .put("title", item.title)
                    .put("url", item.url)
                    .put("visitedAt", item.visitedAt),
            )
        }
        return arr
    }

    private fun loadHistory(): List<HistoryItem> = runCatching {
        val raw = sp.getString(K_HISTORY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HistoryItem(
                title = o.optString("title"),
                url = o.optString("url"),
                visitedAt = o.optLong("visitedAt"),
            )
        }
    }.getOrElse { emptyList() }

    /** Плитки по умолчанию — самые нужные сайты русскоязычного интернета. */
    private fun defaultSpeedDial(): List<SiteLink> = listOf(
        SiteLink("Яндекс", "https://ya.ru"),
        SiteLink("YouTube", "https://m.youtube.com"),
        SiteLink("ВКонтакте", "https://vk.com"),
        SiteLink("Википедия", "https://ru.wikipedia.org"),
        SiteLink("Авито", "https://www.avito.ru"),
        SiteLink("Ozon", "https://www.ozon.ru"),
        SiteLink("Госуслуги", "https://www.gosuslugi.ru"),
        SiteLink("Почта", "https://e.mail.ru"),
    )
}
