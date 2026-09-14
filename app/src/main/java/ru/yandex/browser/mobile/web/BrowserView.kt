package ru.yandex.browser.mobile.web

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoView
import ru.yandex.browser.mobile.core.Tab
import ru.yandex.browser.mobile.core.Urls
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Хост веб-контента.
 *
 * Это GeckoView — встроенный движок Gecko (не Android WebView!).
 * Вьюха ВСЕГДА остаётся в дереве композиции: при переходе на главный экран
 * или в список вкладок страница продолжает жить и не перезагружается.
 */
@Composable
fun GeckoViewHost(
    tab: Tab?,
    modifier: Modifier = Modifier,
) {
    if (tab == null) return
    val sessionKey = remember { mutableStateOf<String?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GeckoView(ctx).apply {
                // Пока страница не нарисована — показываем чистый чёрный,
                // без белой вспышки (важно для тёмной темы).
                coverUntilFirstPaint(AndroidColor.BLACK)
                setAutofillEnabled(true)
            }
        },
        update = { view ->
            val key = "${tab.id}:${System.identityHashCode(tab.session)}"
            if (sessionKey.value != key) {
                runCatching { view.setSession(tab.session) }
                sessionKey.value = key
            }
        },
        onRelease = { view ->
            runCatching { view.releaseSession() }
        },
    )
}

/**
 * Мини-загрузчик иконок сайтов: память + диск (cacheDir) + сеть.
 * Своя реализация — чтобы не тащить в APK лишнюю библиотеку изображений.
 */
object Favicons {

    private val memory = LruCache<String, Bitmap>(96)

    fun cached(host: String): Bitmap? = memory.get(host)

    suspend fun load(context: Context, host: String): Bitmap? {
        if (host.isBlank()) return null
        memory.get(host)?.let { return it }

        val file = File(context.cacheDir, "favicons/$host.png")
        if (file.exists()) {
            val cached = BitmapFactory.decodeFile(file.absolutePath)
            if (cached != null) {
                memory.put(host, cached)
                return cached
            }
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(Urls.faviconUrl("https://$host")).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) YandexBrowser/1.0")
                }
                connection.inputStream.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        memory.put(host, bitmap)
                        runCatching {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                        }
                    }
                    bitmap
                }
            }.getOrNull()
        }
    }
}

@Composable
fun rememberFavicon(url: String): ImageBitmap? {
    val context = LocalContext.current
    val host = remember(url) { Urls.host(url) }
    var bitmap by remember(host) { mutableStateOf(Favicons.cached(host)?.asImageBitmap()) }

    LaunchedEffect(host) {
        if (bitmap == null && host.isNotBlank()) {
            Favicons.load(context, host)?.let { bitmap = it.asImageBitmap() }
        }
    }
    return bitmap
}
