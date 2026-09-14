package ru.yandex.browser.mobile.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import ru.yandex.browser.mobile.R
import ru.yandex.browser.mobile.ui.MainActivity

/**
 * Фоновый сервис браузера.
 *
 * Задача — держать процесс с движком Gecko живым, когда приложение свёрнуто:
 * сайты продолжают работать (загрузки, музыка, веб-приложения, сокеты),
 * вкладки не выгружаются и НЕ перезагружаются при возврате.
 *
 * Работает как foreground service с уведомлением — это единственный честный
 * способ, который Android не убивает в фоне.
 */
class KeepAliveService : Service() {

    companion object {
        const val CHANNEL_ID = "browser_background"
        const val NOTIFICATION_ID = 4711

        const val ACTION_START = "ru.yandex.browser.mobile.KEEPALIVE_START"
        const val ACTION_STOP = "ru.yandex.browser.mobile.KEEPALIVE_STOP"

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        android.util.Log.i("YBBrowser", "Фоновый сервис запущен: вкладки продолжают работать")
        return START_STICKY
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_background),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notif_channel_background)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, KeepAliveService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val tabs = runCatching { TabsHolder.manager?.tabs?.size ?: 0 }.getOrDefault(0)

        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, tabs))
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_action_open), openIntent)
            .addAction(0, getString(R.string.notif_action_stop), stopIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}

/**
 * Мостик между UI и сервисом: уведомление должно знать, сколько вкладок открыто,
 * но сервис не должен тянуть за собой весь UI.
 */
object TabsHolder {
    @Volatile
    var manager: TabManager? = null
}
