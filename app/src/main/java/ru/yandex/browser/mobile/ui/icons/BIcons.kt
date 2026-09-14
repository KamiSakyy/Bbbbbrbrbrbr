package ru.yandex.browser.mobile.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Собственный набор иконок браузера.
 *
 * Намеренно НЕ используем material-icons-extended (это лишние ~2 МБ в APK).
 * Все иконки нарисованы вручную по одному правилу: квадрат 24×24,
 * одинаковая толщина штриха 1.8, скруглённые концы — отсюда цельный,
 * «дорогой» минималистичный вид.
 */
private const val VIEWPORT = 24f
private const val STROKE = 1.8f

private fun stroke(
    name: String,
    width: Float = STROKE,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = VIEWPORT,
    viewportHeight = VIEWPORT,
).path(
    fill = null,
    stroke = SolidColor(Color.White),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = pathBuilder,
).build()

private fun filled(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = VIEWPORT,
    viewportHeight = VIEWPORT,
).path(
    fill = SolidColor(Color.White),
    stroke = null,
    pathBuilder = pathBuilder,
).build()

// --- Геометрические помощники -------------------------------------------------

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
    arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
    close()
}

private fun PathBuilder.dot(cx: Float, cy: Float, r: Float) = circle(cx, cy, r)

object BIcons {

    /** Назад */
    val Back: ImageVector by lazy {
        stroke("Back") {
            moveTo(19.5f, 12f)
            lineTo(5f, 12f)
            moveTo(11.5f, 5.5f)
            lineTo(5f, 12f)
            lineTo(11.5f, 18.5f)
        }
    }

    /** Вперёд */
    val Forward: ImageVector by lazy {
        stroke("Forward") {
            moveTo(4.5f, 12f)
            lineTo(19f, 12f)
            moveTo(12.5f, 5.5f)
            lineTo(19f, 12f)
            lineTo(12.5f, 18.5f)
        }
    }

    /** Закрыть */
    val Close: ImageVector by lazy {
        stroke("Close") {
            moveTo(6.5f, 6.5f)
            lineTo(17.5f, 17.5f)
            moveTo(17.5f, 6.5f)
            lineTo(6.5f, 17.5f)
        }
    }

    /** Плюс */
    val Plus: ImageVector by lazy {
        stroke("Plus") {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    /** Вкладки */
    val Tabs: ImageVector by lazy {
        stroke("Tabs") {
            moveTo(4f, 9f)
            lineTo(15f, 9f)
            lineTo(15f, 20f)
            lineTo(4f, 20f)
            close()
            moveTo(8.5f, 9f)
            lineTo(8.5f, 4.5f)
            lineTo(20f, 4.5f)
            lineTo(20f, 15.5f)
            lineTo(15f, 15.5f)
        }
    }

    /** Щит (приватность / экономия) */
    val Shield: ImageVector by lazy {
        stroke("Shield") {
            moveTo(12f, 3.2f)
            lineTo(19.5f, 6.2f)
            lineTo(19.5f, 12.2f)
            curveTo(19.5f, 16.6f, 16.4f, 19.8f, 12f, 21f)
            curveTo(7.6f, 19.8f, 4.5f, 16.6f, 4.5f, 12.2f)
            lineTo(4.5f, 6.2f)
            close()
            moveTo(9f, 11.8f)
            lineTo(11.2f, 14f)
            lineTo(15.4f, 9.4f)
        }
    }

    /** Три точки (меню) */
    val More: ImageVector by lazy {
        filled("More") {
            dot(12f, 5.2f, 1.7f)
            dot(12f, 12f, 1.7f)
            dot(12f, 18.8f, 1.7f)
        }
    }

    /** Обновить */
    val Reload: ImageVector by lazy {
        stroke("Reload") {
            moveTo(18.4f, 9.6f)
            arcToRelative(7f, 7f, 0f, true, false, 0.9f, 4.2f)
            moveTo(19.5f, 4.5f)
            lineTo(19.5f, 10f)
            lineTo(14f, 10f)
        }
    }

    /** Домой */
    val Home: ImageVector by lazy {
        stroke("Home") {
            moveTo(3.8f, 10.8f)
            lineTo(12f, 4f)
            lineTo(20.2f, 10.8f)
            moveTo(6.2f, 9.6f)
            lineTo(6.2f, 20f)
            lineTo(17.8f, 20f)
            lineTo(17.8f, 9.6f)
        }
    }

    /** Закладка (контур) */
    val Bookmark: ImageVector by lazy {
        stroke("Bookmark") {
            moveTo(6.5f, 3.8f)
            lineTo(17.5f, 3.8f)
            lineTo(17.5f, 20.5f)
            lineTo(12f, 16.2f)
            lineTo(6.5f, 20.5f)
            close()
        }
    }

    /** Закладка (заполненная) */
    val BookmarkFilled: ImageVector by lazy {
        filled("BookmarkFilled") {
            moveTo(6.5f, 3.8f)
            lineTo(17.5f, 3.8f)
            lineTo(17.5f, 20.5f)
            lineTo(12f, 16.2f)
            lineTo(6.5f, 20.5f)
            close()
        }
    }

    /** История */
    val History: ImageVector by lazy {
        stroke("History") {
            circle(12f, 12f, 8.2f)
            moveTo(12f, 7.2f)
            lineTo(12f, 12.2f)
            lineTo(16f, 14.2f)
        }
    }

    /** Настройки (слайдеры — минималистично и без шестерёнки) */
    val Settings: ImageVector by lazy {
        stroke("Settings") {
            moveTo(4f, 7.5f)
            lineTo(20f, 7.5f)
            moveTo(4f, 16.5f)
            lineTo(20f, 16.5f)
            circle(9f, 7.5f, 2.2f)
            circle(15.5f, 16.5f, 2.2f)
        }
    }

    /** Замок (защищённое соединение) */
    val Lock: ImageVector by lazy {
        stroke("Lock") {
            moveTo(6.2f, 10.5f)
            lineTo(17.8f, 10.5f)
            lineTo(17.8f, 20f)
            lineTo(6.2f, 20f)
            close()
            moveTo(9f, 10.5f)
            lineTo(9f, 7.8f)
            curveTo(9f, 5.9f, 10.3f, 4.5f, 12f, 4.5f)
            curveTo(13.7f, 4.5f, 15f, 5.9f, 15f, 7.8f)
            lineTo(15f, 10.5f)
        }
    }

    /** Поиск */
    val Search: ImageVector by lazy {
        stroke("Search") {
            circle(10.8f, 10.8f, 6.3f)
            moveTo(15.5f, 15.5f)
            lineTo(20f, 20f)
        }
    }

    /** Микрофон (голосовой поиск) */
    val Mic: ImageVector by lazy {
        stroke("Mic") {
            moveTo(12f, 3.5f)
            curveTo(13.4f, 3.5f, 14.5f, 4.6f, 14.5f, 6f)
            lineTo(14.5f, 11f)
            curveTo(14.5f, 12.4f, 13.4f, 13.5f, 12f, 13.5f)
            curveTo(10.6f, 13.5f, 9.5f, 12.4f, 9.5f, 11f)
            lineTo(9.5f, 6f)
            curveTo(9.5f, 4.6f, 10.6f, 3.5f, 12f, 3.5f)
            close()
            moveTo(6.5f, 11.5f)
            curveTo(6.5f, 14.5f, 9f, 16.8f, 12f, 16.8f)
            curveTo(15f, 16.8f, 17.5f, 14.5f, 17.5f, 11.5f)
            moveTo(12f, 16.8f)
            lineTo(12f, 20.5f)
        }
    }

    /** Удалить (корзина) */
    val Trash: ImageVector by lazy {
        stroke("Trash") {
            moveTo(4.5f, 6.8f)
            lineTo(19.5f, 6.8f)
            moveTo(9.5f, 6.8f)
            lineTo(9.5f, 4.2f)
            lineTo(14.5f, 4.2f)
            lineTo(14.5f, 6.8f)
            moveTo(6.5f, 6.8f)
            lineTo(7.6f, 20.2f)
            lineTo(16.4f, 20.2f)
            lineTo(17.5f, 6.8f)
            moveTo(10.5f, 10.4f)
            lineTo(10.5f, 16.6f)
            moveTo(13.5f, 10.4f)
            lineTo(13.5f, 16.6f)
        }
    }

    /** Загрузка */
    val Download: ImageVector by lazy {
        stroke("Download") {
            moveTo(12f, 4f)
            lineTo(12f, 14.5f)
            moveTo(7.8f, 10.5f)
            lineTo(12f, 14.8f)
            lineTo(16.2f, 10.5f)
            moveTo(4.5f, 19.5f)
            lineTo(19.5f, 19.5f)
        }
    }

    /** Поделиться */
    val Share: ImageVector by lazy {
        stroke("Share") {
            moveTo(12f, 16f)
            lineTo(12f, 4f)
            moveTo(8f, 7.8f)
            lineTo(12f, 3.8f)
            lineTo(16f, 7.8f)
            moveTo(5f, 13.5f)
            lineTo(5f, 20f)
            lineTo(19f, 20f)
            lineTo(19f, 13.5f)
        }
    }

    /** Версия для ПК */
    val Desktop: ImageVector by lazy {
        stroke("Desktop") {
            moveTo(3.5f, 5.5f)
            lineTo(20.5f, 5.5f)
            lineTo(20.5f, 15.5f)
            lineTo(3.5f, 15.5f)
            close()
            moveTo(9f, 19.5f)
            lineTo(15f, 19.5f)
            moveTo(12f, 15.5f)
            lineTo(12f, 19.5f)
        }
    }

    /** Луна (тёмная тема) */
    val Moon: ImageVector by lazy {
        stroke("Moon") {
            moveTo(20f, 14.2f)
            curveTo(18.9f, 14.8f, 17.6f, 15.2f, 16.2f, 15.2f)
            curveTo(11.7f, 15.2f, 8f, 11.5f, 8f, 7f)
            curveTo(8f, 5.9f, 8.2f, 4.9f, 8.5f, 4f)
            curveTo(5.4f, 5.2f, 3.2f, 8.2f, 3.2f, 11.7f)
            curveTo(3.2f, 16.3f, 6.9f, 20f, 11.5f, 20f)
            curveTo(15.1f, 20f, 18.2f, 17.8f, 19.4f, 14.6f)
            close()
        }
    }

    /** Молния — символ экономии трафика */
    val Bolt: ImageVector by lazy {
        filled("Bolt") {
            moveTo(13.4f, 2.5f)
            lineTo(5.6f, 13.6f)
            lineTo(11.2f, 13.6f)
            lineTo(10.2f, 21.5f)
            lineTo(18.4f, 10.2f)
            lineTo(12.6f, 10.2f)
            close()
        }
    }

    /** Глобус */
    val Globe: ImageVector by lazy {
        stroke("Globe") {
            circle(12f, 12f, 8.5f)
            moveTo(3.5f, 12f)
            lineTo(20.5f, 12f)
            moveTo(12f, 3.5f)
            curveToRelative(2.6f, 2.6f, 2.6f, 14.4f, 0f, 17f)
            moveTo(12f, 3.5f)
            curveToRelative(-2.6f, 2.6f, -2.6f, 14.4f, 0f, 17f)
        }
    }

    /** Галочка */
    val Check: ImageVector by lazy {
        stroke("Check") {
            moveTo(5f, 12.6f)
            lineTo(9.6f, 17.2f)
            lineTo(19f, 7.2f)
        }
    }

    /** Шеврон вниз */
    val ChevronDown: ImageVector by lazy {
        stroke("ChevronDown") {
            moveTo(6f, 9.5f)
            lineTo(12f, 15.5f)
            lineTo(18f, 9.5f)
        }
    }

    /** Карандаш (редактировать) */
    val Edit: ImageVector by lazy {
        stroke("Edit") {
            moveTo(4f, 20f)
            lineTo(8.2f, 19f)
            lineTo(19.4f, 7.8f)
            curveTo(20.2f, 7f, 20.2f, 5.8f, 19.4f, 5f)
            lineTo(19f, 4.6f)
            curveTo(18.2f, 3.8f, 17f, 3.8f, 16.2f, 4.6f)
            lineTo(5f, 15.8f)
            close()
        }
    }

    /** Приватный режим */
    val PrivateEye: ImageVector by lazy {
        stroke("PrivateEye") {
            moveTo(3f, 12f)
            curveTo(3f, 12f, 6.6f, 6.4f, 12f, 6.4f)
            curveTo(17.4f, 6.4f, 21f, 12f, 21f, 12f)
            curveTo(21f, 12f, 17.4f, 17.6f, 12f, 17.6f)
            curveTo(6.6f, 17.6f, 3f, 12f, 3f, 12f)
            close()
            moveTo(4.8f, 19.2f)
            lineTo(19.2f, 4.8f)
        }
    }

    /** Спидометр — статистика экономии */
    val Gauge: ImageVector by lazy {
        stroke("Gauge") {
            moveTo(3.5f, 17.5f)
            arcToRelative(8.5f, 8.5f, 0f, true, true, 17f, 0f)
            moveTo(12f, 17.5f)
            lineTo(16f, 11.5f)
        }
    }

    /** Копировать */
    val Copy: ImageVector by lazy {
        stroke("Copy") {
            moveTo(8.5f, 8.5f)
            lineTo(20f, 8.5f)
            lineTo(20f, 20f)
            lineTo(8.5f, 20f)
            close()
            moveTo(15.5f, 8.5f)
            lineTo(15.5f, 4f)
            lineTo(4f, 4f)
            lineTo(4f, 15.5f)
            lineTo(8.5f, 15.5f)
        }
    }

    /** Стоп */
    val Stop: ImageVector by lazy {
        stroke("Stop") {
            circle(12f, 12f, 8.5f)
            moveTo(9f, 9f)
            lineTo(15f, 15f)
            moveTo(15f, 9f)
            lineTo(9f, 15f)
        }
    }
}
