package ru.yandex.browser.mobile.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Обработчик кнопки «Открыть» в уведомлении фонового режима. */
class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val open = Intent(context, ru.yandex.browser.mobile.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(open)
    }
}
