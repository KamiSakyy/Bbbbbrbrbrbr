package ru.yandex.browser.mobile

import android.app.Application
import ru.yandex.browser.mobile.core.BrowserEngine
import ru.yandex.browser.mobile.core.Settings
import ru.yandex.browser.mobile.core.TabManager
import ru.yandex.browser.mobile.core.TabsHolder
import ru.yandex.browser.mobile.core.Urls

/**
 * Точка входа процесса.
 *
 * Движок GeckoView и менеджер вкладок живут в процессе приложения, а не в
 * Activity: поэтому открытые вкладки и сайты продолжают работать в фоне,
 * а при возврате в браузер страницы не перезагружаются.
 */
class YandexBrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Settings.init(this)

        // Прогреваем движок заранее — первая вкладка открывается мгновенно.
        BrowserEngine.bootstrap(this)

        // Настройки, влияющие на движок «на лету»:
        Settings.onDarkModeChanged = { BrowserEngine.pushConfig() }
        Settings.onTrafficSaverChanged = { BrowserEngine.pushConfig() }
        Settings.onDesktopModeChanged = { desktop -> TabsHolder.manager?.applyUserAgentMode(desktop) }

        // Восстанавливаем вкладки процесса (сессии Gecko) сразу на старте.
        val manager = TabManager(this)
        android.util.Log.i("YBBrowser", "APP_START: вкладок ${manager.tabs.size}")
        TabsHolder.manager = manager
        if (manager.tabs.isEmpty()) {
            manager.newTab(Urls.ABOUT_HOME)
        }
    }
}
