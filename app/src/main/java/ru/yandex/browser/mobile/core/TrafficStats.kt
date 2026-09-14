package ru.yandex.browser.mobile.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Счётчики режима «Экономия трафика».
 *
 * Блокировка запросов измеряется точно (движок сообщает о каждом заблокированном
 * элементе), а сэкономленные байты считаются как оценка по типу ресурса —
 * честная метрика для интерфейса (помечается как «≈»).
 */
object TrafficStats {

    /** Средний вес одного заблокированного ресурса по категориям (байты). */
    private const val BYTES_TRACKER = 28_000L
    private const val BYTES_AD = 95_000L
    private const val BYTES_FINGERPRINT = 14_000L
    private const val BYTES_CRYPTOMINING = 65_000L
    private const val BYTES_SOCIAL = 30_000L
    private const val BYTES_COOKIE = 3_000L
    private const val BYTES_SAFE_BROWSING = 1_500L

    var blockedRequests by mutableStateOf(0L)
        private set
    var savedBytes by mutableStateOf(0L)
        private set

    fun onBlocked(estimatedBytes: Long) {
        blockedRequests += 1
        savedBytes += estimatedBytes
    }

    fun estimate(antiTracking: Int, safeBrowsing: Int, cookie: Int, uri: String? = null): Long {
        var bytes = 0L
        if (antiTracking and org.mozilla.geckoview.ContentBlocking.AntiTracking.AD != 0) bytes += BYTES_AD
        if (antiTracking and org.mozilla.geckoview.ContentBlocking.AntiTracking.ANALYTIC != 0) bytes += BYTES_TRACKER
        if (antiTracking and org.mozilla.geckoview.ContentBlocking.AntiTracking.SOCIAL != 0) bytes += BYTES_SOCIAL
        if (antiTracking and org.mozilla.geckoview.ContentBlocking.AntiTracking.FINGERPRINTING != 0) bytes += BYTES_FINGERPRINT
        if (antiTracking and org.mozilla.geckoview.ContentBlocking.AntiTracking.CRYPTOMINING != 0) bytes += BYTES_CRYPTOMINING
        if (antiTracking and org.mozilla.geckoview.ContentBlocking.AntiTracking.CONTENT != 0) bytes += BYTES_AD
        if (antiTracking and org.mozilla.geckoview.ContentBlocking.AntiTracking.EMAIL != 0) bytes += BYTES_TRACKER
        if (safeBrowsing != 0) bytes += BYTES_SAFE_BROWSING
        if (cookie != 0) bytes += BYTES_COOKIE
        if (bytes == 0L) bytes = BYTES_TRACKER
        return bytes
    }

    fun reset() {
        blockedRequests = 0
        savedBytes = 0
    }

    /** 12345678 -> «11,8 МБ» */
    fun formatBytes(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.ROOT, "%.2f ГБ", bytes / gb)
            bytes >= mb -> String.format(Locale.ROOT, "%.1f МБ", bytes / mb)
            bytes >= kb -> String.format(Locale.ROOT, "%.0f КБ", bytes / kb)
            else -> "$bytes Б"
        }
    }
}
