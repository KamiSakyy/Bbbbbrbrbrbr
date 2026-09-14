package ru.yandex.browser.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import ru.yandex.browser.mobile.core.KeepAliveService
import ru.yandex.browser.mobile.core.Settings
import ru.yandex.browser.mobile.core.TabManager
import ru.yandex.browser.mobile.core.TabsHolder
import ru.yandex.browser.mobile.core.Urls
import ru.yandex.browser.mobile.ui.theme.YandexBrowserTheme

/**
 * Единственная Activity: весь интерфейс браузера — на Compose,
 * веб-контент — на GeckoView.
 */
class MainActivity : ComponentActivity() {

    private val manager: TabManager
        get() = TabsHolder.manager ?: TabManager(applicationContext).also { TabsHolder.manager = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        askNotificationPermission()
        openFromIntent(intent)

        // Фоновый режим: сервис с уведомлением поднимаем, пока приложение ещё
        // на переднем плане. Запуск сервиса из onStop на Android 12+ падает с
        // ForegroundServiceStartNotAllowedException — так надёжнее.
        if (Settings.backgroundMode) {
            runCatching { KeepAliveService.start(this) }
                .onFailure { Log.w("YBBrowser", "Не удалось поднять фоновый сервис", it) }
        }

        setContent {
            YandexBrowserTheme {
                BrowserApp(manager)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    /**
     * Браузер на переднем плане — фоновый сервис не нужен и его уведомление
     * не должно висеть. Как только пользователь свернул приложение —
     * сервис поднимается и держит движок живым.
     */
    /** Приложение на переднем плане: сохраняем вкладки, движок продолжает жить. */
    override fun onStop() {
        super.onStop()
        manager.persist()
        Log.i("YBBrowser", "ON_STOP: вкладки сохранены (${manager.tabs.size})")
    }

    override fun onStart() {
        super.onStart()
        // Сервис уже запущен с onCreate — здесь только фиксируем возврат.
        Log.i("YBBrowser", "ON_START")
    }

    private fun openFromIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val url = data.toString()
        val current = manager.activeTab
        if (current != null && current.url == url) return
        manager.newTab(url = url, select = true)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }
}
