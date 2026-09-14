package ru.yandex.browser.mobile.core

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension

/**
 * Ядро браузера: единственный экземпляр GeckoRuntime на процесс.
 *
 * Здесь собирается вся конфигурация движка:
 *  • ContentBlocking  — реклама, трекеры, куки, криптомайнинг, фингерпринтинг
 *  • webFontsEnabled  — режим экономии трафика отключает веб-шрифты (десятки КБ на страницу)
 *  • preferredColorScheme — тёмная тема для сайтов, которые её поддерживают
 *  • Global Privacy Control
 *
 * Плюс подключение фирменного WebExtension (блокировка + принудительная тёмная тема
 * для сайтов без собственной).
 */
object BrowserEngine {

    private const val TAG = "BrowserEngine"
    const val EXTENSION_URI = "resource://android/assets/extensions/yandex-assistant/"
    const val EXTENSION_ID = "assistant@yandex.browser"
    const val NATIVE_APP = "browser"

    @Volatile
    private var runtime: GeckoRuntime? = null

    @Volatile
    private var extension: WebExtension? = null

    @Volatile
    private var extensionPort: WebExtension.Port? = null

    /** GeckoRuntime может быть null только до первого bootstrap(). */
    val instance: GeckoRuntime? get() = runtime

    // ------------------------------------------------------------------
    //  Инициализация движка
    // ------------------------------------------------------------------

    fun bootstrap(context: Context): GeckoRuntime {
        runtime?.let { return it }
        synchronized(this) {
            runtime?.let { return it }
            val appContext = context.applicationContext
            val rt = GeckoRuntime.create(appContext, buildSettings())
            runtime = rt
            installAssistant(rt)
            Log.i(LOG_TAG, "ENGINE_READY: GeckoRuntime поднят (GeckoView, не WebView)")
            return rt
        }
    }

    private fun buildSettings(): GeckoRuntimeSettings {
        // --- Блокировка контента: агрессивно, но без ломки сайтов ---
        val contentBlocking = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
            // Первопартийные куки оставляем (иначе логины отвалятся),
            // сторонние — режутся, это и приватность, и меньше трафика.
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY)
            .cookiePurging(true)
            .queryParameterStrippingEnabled(true)
            .build()

        val builder = GeckoRuntimeSettings.Builder()
            .contentBlocking(contentBlocking)
            .javaScriptEnabled(true)
            .webFontsEnabled(!Settings.trafficSaver)          // экономия трафика: без веб-шрифтов
            .preferredColorScheme(
                if (Settings.darkMode) {
                    GeckoRuntimeSettings.COLOR_SCHEME_DARK     // сайты с тёмной темой — сразу тёмные
                } else {
                    GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
                },
            )
            .globalPrivacyControlEnabled(true)
            .aboutConfigEnabled(false)
            .remoteDebuggingEnabled(false)
            .loginAutofillEnabled(true)

        return builder.build()
    }

    // ------------------------------------------------------------------
    //  Фирменное расширение: блокировка + тёмная тема
    // ------------------------------------------------------------------

    private fun installAssistant(rt: GeckoRuntime) {
        try {
            rt.webExtensionController
                .ensureBuiltIn(EXTENSION_URI, EXTENSION_ID)
                .accept(
                    { ext ->
                        extension = ext
                        try {
                            ext?.setMessageDelegate(messageDelegate, NATIVE_APP)
                        } catch (t: Throwable) {
                            Log.w(LOG_TAG, "Не удалось назначить message delegate расширения", t)
                        }
                    },
                    { error -> Log.w(LOG_TAG, "Расширение не установлено: ${error?.message}") },
                )
        } catch (t: Throwable) {
            // Расширение — важное, но не критичное: без него браузер всё равно работает.
            Log.w(LOG_TAG, "Ошибка установки расширения", t)
        }
    }

    private val messageDelegate = object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            extensionPort = port
            pushConfig()
        }
    }

    /** Передаём расширению текущие настройки (тёмная тема / блокировка). */
    fun pushConfig() {
        val port = extensionPort ?: return
        runCatching {
            port.postMessage(
                JSONObject()
                    .put("type", "config")
                    .put("darkMode", Settings.darkMode)
                    .put("blockAds", Settings.trafficSaver),
            )
        }.onFailure { Log.w(LOG_TAG, "Не удалось отправить конфиг расширению", it) }
    }

    // ------------------------------------------------------------------
    //  Данные
    // ------------------------------------------------------------------

    /** Очистка данных браузера (кэш / куки / всё). */
    fun clearData(flags: Long, onDone: (() -> Unit)? = null) {
        val rt = runtime ?: run {
            onDone?.invoke()
            return
        }
        runCatching {
            rt.storageController
                .clearData(flags)
                .accept({ onDone?.invoke() }, { onDone?.invoke() })
        }.onFailure {
            Log.w(LOG_TAG, "clearData failed", it)
            onDone?.invoke()
        }
    }

    fun clearCache(onDone: (() -> Unit)? = null) =
        clearData(StorageController.ClearFlags.ALL_CACHES, onDone)

    fun clearCookies(onDone: (() -> Unit)? = null) =
        clearData(StorageController.ClearFlags.COOKIES, onDone)

    fun clearEverything(onDone: (() -> Unit)? = null) =
        clearData(StorageController.ClearFlags.ALL, onDone)
}
