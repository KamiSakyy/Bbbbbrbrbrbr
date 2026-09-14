package ru.yandex.browser.mobile.core

import java.net.URLEncoder
import java.util.Locale

/**
 * Поисковые системы. По умолчанию — Яндекс (как и просили).
 */
enum class SearchEngine(
    val id: String,
    val label: String,
    val searchTemplate: String,
    val homePage: String,
) {
    YANDEX(
        id = "yandex",
        label = "Яндекс",
        searchTemplate = "https://yandex.ru/search/?text=%s",
        homePage = "https://ya.ru",
    ),
    GOOGLE(
        id = "google",
        label = "Google",
        searchTemplate = "https://www.google.com/search?q=%s",
        homePage = "https://www.google.com",
    ),
    BING(
        id = "bing",
        label = "Bing",
        searchTemplate = "https://www.bing.com/search?q=%s",
        homePage = "https://www.bing.com",
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        label = "DuckDuckGo",
        searchTemplate = "https://duckduckgo.com/?q=%s",
        homePage = "https://duckduckgo.com",
    ),
    ;

    companion object {
        fun fromId(id: String?): SearchEngine =
            entries.firstOrNull { it.id == id } ?: YANDEX
    }
}

/**
 * Разбор того, что пользователь ввёл в адресную строку:
 * это адрес или поисковый запрос.
 */
object Urls {

    private val SCHEME_RE = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val DOMAIN_RE = Regex(
        "^([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(:\\d{1,5})?(/.*)?$",
    )
    private val LOCALHOST_RE = Regex("^localhost(:\\d{1,5})?(/.*)?$", RegexOption.IGNORE_CASE)

    /** Внутренние страницы браузера. */
    const val ABOUT_BLANK = "about:blank"
    const val ABOUT_HOME = "about:home"

    fun isAbout(url: String): Boolean = url.startsWith("about:")

    /** Похоже ли на адрес (а не на поисковый запрос). */
    fun looksLikeUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.contains(' ')) return false
        if (SCHEME_RE.containsMatchIn(trimmed)) return true
        if (LOCALHOST_RE.matches(trimmed)) return true
        return DOMAIN_RE.matches(trimmed)
    }

    /** Превращает ввод пользователя в URL для загрузки. */
    fun resolve(input: String, engine: SearchEngine): String {
        val trimmed = input.trim()
        return when {
            trimmed.isEmpty() -> ABOUT_HOME
            isAbout(trimmed) -> trimmed
            SCHEME_RE.containsMatchIn(trimmed) -> trimmed
            looksLikeUrl(trimmed) -> "https://$trimmed"
            else -> engine.searchTemplate.format(URLEncoder.encode(trimmed, "UTF-8"))
        }
    }

    /** Поисковый запрос как отдельный URL (для «Яндекс Собеседник» и т.п.). */
    fun search(query: String, engine: SearchEngine): String =
        engine.searchTemplate.format(URLEncoder.encode(query, "UTF-8"))

    /** Хост сайта: https://www.ozon.ru/cart -> ozon.ru */
    fun host(url: String): String = runCatching {
        val noScheme = url.substringAfter("://", url)
        val host = noScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        host.substringBefore(':').lowercase(Locale.ROOT)
    }.getOrDefault("")

    /** Человеческое имя хоста: ozon.ru, ya.ru */
    fun prettyHost(url: String): String {
        val h = host(url)
        if (h.isEmpty()) return url
        return h.removePrefix("www.").removePrefix("m.")
    }

    /** Первая буква для монограммы плитки. */
    fun monogram(url: String): String {
        val h = prettyHost(url)
        return h.firstOrNull()?.uppercase(Locale.ROOT) ?: "?"
    }

    /** URL иконки сайта (сервис фавиконок Яндекса). */
    fun faviconUrl(url: String): String =
        "https://favicon.yandex.net/favicon/${host(url)}?size=64"
}
