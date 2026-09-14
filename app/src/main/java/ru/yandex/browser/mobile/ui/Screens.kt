package ru.yandex.browser.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.yandex.browser.mobile.BuildConfig
import ru.yandex.browser.mobile.core.SearchEngine
import ru.yandex.browser.mobile.core.Settings
import ru.yandex.browser.mobile.core.TrafficStats
import ru.yandex.browser.mobile.core.Urls
import ru.yandex.browser.mobile.ui.icons.BIcons
import ru.yandex.browser.mobile.ui.theme.AccentRed
import ru.yandex.browser.mobile.ui.theme.HairLine
import ru.yandex.browser.mobile.ui.theme.PureBlack
import ru.yandex.browser.mobile.ui.theme.SurfaceDeep
import ru.yandex.browser.mobile.ui.theme.TextDim
import ru.yandex.browser.mobile.ui.theme.TextMuted
import ru.yandex.browser.mobile.ui.theme.TextPrimary

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(BIcons.Back, "Назад", size = 21.dp, onClick = onBack)
        Spacer(Modifier.width(6.dp))
        Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (checked) AccentRed else TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp)
            if (!description.isNullOrBlank()) {
                Text(description, color = TextMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = AccentRed,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceDeep,
                uncheckedBorderColor = HairLine,
            ),
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Icon(BIcons.Forward, contentDescription = null, tint = TextDim, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(230.dp),
        )
    }
}

/** Экран настроек. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearData: (Long, String) -> Unit,
) {
    var enginesExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = "Настройки", onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { SectionHeader("Просмотр") }
            item {
                ToggleRow(
                    icon = BIcons.Bolt,
                    title = "Экономия трафика",
                    description = "Режет рекламу, трекеры, веб-шрифты и лишние запросы",
                    checked = Settings.trafficSaver,
                    onCheckedChange = { Settings.setTrafficSaver(it) },
                )
            }
            item {
                ToggleRow(
                    icon = BIcons.Moon,
                    title = "Тёмная тема сайтов",
                    description = "Сайты с тёмной темой — сразу тёмные, остальные принудительно",
                    checked = Settings.darkMode,
                    onCheckedChange = { Settings.setDarkMode(it) },
                )
            }
            item {
                ToggleRow(
                    icon = BIcons.Globe,
                    title = "Фоновый режим",
                    description = "Сайты работают, пока браузер свёрнут, и не перезагружаются",
                    checked = Settings.backgroundMode,
                    onCheckedChange = { Settings.setBackgroundMode(it) },
                )
            }
            item {
                ToggleRow(
                    icon = BIcons.Desktop,
                    title = "Версия для ПК",
                    description = "Сайты отдают десктопную вёрстку",
                    checked = Settings.desktopMode,
                    onCheckedChange = { Settings.setDesktopMode(it) },
                )
            }

            item { SectionHeader("Поиск") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enginesExpanded = !enginesExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(BIcons.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Поисковая система", color = TextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    Text(Settings.searchEngine.label, color = AccentRed, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = BIcons.ChevronDown,
                        contentDescription = null,
                        tint = TextDim,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            if (enginesExpanded) {
                items(SearchEngine.entries) { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Settings.setSearchEngine(engine)
                                enginesExpanded = false
                            }
                            .padding(start = 52.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(engine.label, color = TextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        if (Settings.searchEngine == engine) {
                            Icon(BIcons.Check, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { SectionHeader("Приватность и данные") }
            item {
                ActionRow(BIcons.Trash, "Очистить кэш", "Освобождает место на устройстве") {
                    onClearData(org.mozilla.geckoview.StorageController.ClearFlags.ALL_CACHES, "Кэш очищен")
                }
            }
            item {
                ActionRow(BIcons.Trash, "Очистить cookie", "Разлогинит со всех сайтов") {
                    onClearData(org.mozilla.geckoview.StorageController.ClearFlags.COOKIES, "Cookie очищены")
                }
            }
            item {
                ActionRow(BIcons.Trash, "Очистить все данные", "Кэш, cookie, история, разрешения") {
                    onClearData(org.mozilla.geckoview.StorageController.ClearFlags.ALL, "Все данные очищены")
                }
            }

            item { TrafficStatsRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

            item { SectionHeader("О браузере") }
            item { InfoRow("Движок", "GeckoView 155 · Gecko + SpiderMonkey + WebRender") }
            item { InfoRow("Версия", BuildConfig.VERSION_NAME) }
            item { InfoRow("Оболочка", "Jetpack Compose · R8 · минимальный размер") }
        }
    }
}

enum class LibraryKind { BOOKMARKS, HISTORY }

/** Списки закладок и истории. */
@Composable
fun LibraryScreen(
    kind: LibraryKind,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val title = if (kind == LibraryKind.BOOKMARKS) "Закладки" else "История"
    // Единый тип для двух источников: пара «заголовок — адрес».
    val entries: List<Pair<String, String>> = if (kind == LibraryKind.BOOKMARKS) {
        Settings.bookmarks.map { it.title to it.url }
    } else {
        Settings.history.map { it.title to it.url }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = if (entries.isEmpty()) title else "$title · ${entries.size}",
            onBack = onBack,
            action = {
                if (entries.isNotEmpty()) {
                    BarIcon(BIcons.Trash, "Удалить всё", size = 19.dp, tint = TextMuted) {
                        if (kind == LibraryKind.BOOKMARKS) Settings.clearBookmarks() else Settings.clearHistory()
                    }
                }
            },
        )

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (kind == LibraryKind.BOOKMARKS) BIcons.Bookmark else BIcons.History,
                        contentDescription = null,
                        tint = TextDim,
                        modifier = Modifier.size(34.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (kind == LibraryKind.BOOKMARKS) "Пока нет закладок" else "История пуста",
                        color = TextMuted,
                        fontSize = 14.sp,
                    )
                }
            }
            return
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
            items(entries) { item ->
                val (itemTitle, itemUrl) = item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpen(itemUrl) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FaviconOrMonogram(url = itemUrl, size = 30.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = itemTitle.ifBlank { Urls.prettyHost(itemUrl) },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = Urls.prettyHost(itemUrl),
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (kind == LibraryKind.BOOKMARKS) {
                                    Settings.removeBookmark(itemUrl)
                                } else {
                                    Settings.history.removeAll { it.url == itemUrl }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(BIcons.Close, contentDescription = "Удалить", tint = TextDim, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}
