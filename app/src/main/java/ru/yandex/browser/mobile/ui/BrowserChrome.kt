package ru.yandex.browser.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.yandex.browser.mobile.core.Settings
import ru.yandex.browser.mobile.core.Tab
import ru.yandex.browser.mobile.core.TrafficStats
import ru.yandex.browser.mobile.core.Urls
import ru.yandex.browser.mobile.ui.icons.BIcons
import ru.yandex.browser.mobile.ui.theme.AccentGreen
import ru.yandex.browser.mobile.ui.theme.AccentRed
import ru.yandex.browser.mobile.ui.theme.HairLine
import ru.yandex.browser.mobile.ui.theme.PureBlack
import ru.yandex.browser.mobile.ui.theme.SurfaceDeep
import ru.yandex.browser.mobile.ui.theme.TextDim
import ru.yandex.browser.mobile.ui.theme.TextMuted
import ru.yandex.browser.mobile.ui.theme.TextPrimary

/** Фирменный знак: красный круг с белой «Я». */
@Composable
fun BrandMark(size: Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AccentRed),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Я",
            color = Color.White,
            fontSize = (size.value * 0.58f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun BarIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    enabled: Boolean = true,
    tint: Color = TextPrimary,
    size: Dp = 24.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else TextDim,
            modifier = Modifier.size(size),
        )
    }
}

/** Верхняя панель: адресная строка + (на главной) чип «Экономия трафика». */
@Composable
fun TopBar(
    tab: Tab?,
    showChip: Boolean,
    onOpenOmnibox: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onVoice: () -> Unit,
    onToggleTrafficSaver: () -> Unit,
) {
    val url = tab?.url.orEmpty()
    val isHome = url.isBlank() || Urls.isAbout(url)
    val secure = url.startsWith("https://")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack)
            .padding(start = 10.dp, end = 10.dp, top = 8.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(SurfaceDeep)
            .clickable(onClick = onOpenOmnibox)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(26.dp)
        Spacer(Modifier.width(10.dp))

        if (secure) {
            Icon(
                imageVector = BIcons.Lock,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }

        Text(
            text = if (isHome) "Поиск или адрес" else Urls.prettyHost(url) + pathOf(url),
            color = if (isHome) TextMuted else TextPrimary,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        BarIcon(
            icon = if (tab?.loading == true) BIcons.Stop else BIcons.Reload,
            enabled = tab != null && !isHome,
            size = 20.dp,
            tint = TextMuted,
            onClick = { if (tab?.loading == true) onStop() else onReload() },
        )
        BarIcon(
            icon = BIcons.Mic,
            size = 20.dp,
            tint = TextMuted,
            onClick = onVoice,
        )
    }

    if (tab?.loading == true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(HairLine),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((tab.progress.coerceIn(0, 100)) / 100f)
                    .fillMaxHeight()
                    .background(AccentRed),
            )
        }
    } else {
        Spacer(Modifier.height(6.dp))
    }

    if (showChip) {
        TrafficSaverChip(onClick = onToggleTrafficSaver)
    }
}

/** Чип режима экономии: щит + подпись + индикатор. */
@Composable
fun TrafficSaverChip(onClick: () -> Unit) {
    val on = Settings.trafficSaver
    Row(
        modifier = Modifier
            .padding(start = 14.dp, top = 2.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (on) BIcons.Bolt else BIcons.Shield,
            contentDescription = null,
            tint = if (on) AccentRed else TextMuted,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (on) "Экономия трафика · вкл" else "Экономия трафика · выкл",
            color = if (on) TextPrimary else TextMuted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (on) AccentGreen else TextDim),
        )
    }
}

/** Нижняя панель управления. */
@Composable
fun BottomBar(
    tab: Tab?,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onTabs: () -> Unit,
    onTrafficSaver: () -> Unit,
    onMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(BIcons.Back, "Назад", enabled = tab?.canGoBack == true, onClick = onBack)
        BarIcon(BIcons.Forward, "Вперёд", enabled = tab?.canGoForward == true, onClick = onForward)

        Box {
            BarIcon(BIcons.Tabs, "Вкладки", onClick = onTabs)
            if (tabCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(AccentRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (tabCount > 99) "99" else tabCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        BarIcon(
            icon = BIcons.Bolt,
            contentDescription = "Экономия трафика",
            tint = if (Settings.trafficSaver) AccentRed else TextMuted,
            onClick = onTrafficSaver,
        )
        BarIcon(BIcons.More, "Меню", onClick = onMenu)
    }
}

/** Строка статистики экономии. */
@Composable
fun TrafficStatsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDeep)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = BIcons.Gauge,
            contentDescription = null,
            tint = AccentRed,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Заблокировано: ${TrafficStats.blockedRequests}",
            color = TextPrimary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "≈ ${TrafficStats.formatBytes(Settings.savedBytesTotal + TrafficStats.savedBytes)}",
            color = AccentGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Путь страницы без хоста (для адресной строки). */
private fun pathOf(url: String): String {
    val withoutScheme = url.substringAfter("://", url)
    val slash = withoutScheme.indexOf('/')
    if (slash < 0) return ""
    val path = withoutScheme.substring(slash)
    return if (path.length > 28) path.take(28) + "…" else path
}
