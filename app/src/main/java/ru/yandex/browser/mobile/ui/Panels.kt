package ru.yandex.browser.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.yandex.browser.mobile.core.SearchEngine
import ru.yandex.browser.mobile.core.Settings
import ru.yandex.browser.mobile.core.Tab
import ru.yandex.browser.mobile.core.TabManager
import ru.yandex.browser.mobile.core.Urls
import ru.yandex.browser.mobile.ui.icons.BIcons
import ru.yandex.browser.mobile.ui.theme.AccentRed
import ru.yandex.browser.mobile.ui.theme.HairLine
import ru.yandex.browser.mobile.ui.theme.PureBlack
import ru.yandex.browser.mobile.ui.theme.SurfaceDeep
import ru.yandex.browser.mobile.ui.theme.SurfaceRaised
import ru.yandex.browser.mobile.ui.theme.TextDim
import ru.yandex.browser.mobile.ui.theme.TextMuted
import ru.yandex.browser.mobile.ui.theme.TextPrimary

/** Полноэкранная адресная строка с подсказками. */
@Composable
fun OmniboxOverlay(
    initialText: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    val engine = Settings.searchEngine
    val suggestions = remember(text) { Settings.suggestions(text) }
    val isUrl = remember(text) { Urls.looksLikeUrl(text) }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(SurfaceDeep)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(26.dp)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text("Поиск или адрес", color = TextMuted, fontSize = 15.sp)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(AccentRed),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Uri,
                    ),
                    // Клавиатуры на разных телефонах присылают разные действия
                    // (Go / Done / Search) — реагируем на все, иначе «нажал
                    // кнопку и ничего не произошло».
                    keyboardActions = KeyboardActions(
                        onGo = { submitOmnibox(text, onSubmit) },
                        onDone = { submitOmnibox(text, onSubmit) },
                        onSearch = { submitOmnibox(text, onSubmit) },
                        onSend = { submitOmnibox(text, onSubmit) },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            BarIcon(BIcons.Close, "Закрыть", size = 20.dp, tint = TextMuted, onClick = onDismiss)
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        ) {
            if (text.isNotBlank()) {
                item {
                    OmniboxRow(
                        icon = BIcons.Search,
                        title = text,
                        subtitle = "Найти в ${engine.label}",
                        accentSubtitle = true,
                        onClick = { onSubmit(text) },
                    )
                }
                if (isUrl) {
                    item {
                        OmniboxRow(
                            icon = BIcons.Globe,
                            title = text,
                            subtitle = "Открыть сайт",
                            onClick = { onSubmit(text) },
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Поисковая система: ${engine.label}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                    )
                }
            }

            items(suggestions) { suggestion ->
                OmniboxRow(
                    icon = BIcons.History,
                    title = suggestion.title.ifBlank { Urls.prettyHost(suggestion.url) },
                    subtitle = Urls.prettyHost(suggestion.url),
                    faviconUrl = suggestion.url,
                    onClick = { onSubmit(suggestion.url) },
                )
            }
        }
    }
}

/** Отправка запроса из адресной строки (одна точка входа для всех действий IME). */
private fun submitOmnibox(text: String, onSubmit: (String) -> Unit) {
    val query = text.trim()
    if (query.isEmpty()) return
    android.util.Log.i("YBBrowser", "OMNIBOX submit=\"$query\"")
    onSubmit(query)
}

@Composable
private fun OmniboxRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    accentSubtitle: Boolean = false,
    faviconUrl: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (faviconUrl != null) {
            FaviconOrMonogram(url = faviconUrl, size = 24.dp)
        } else {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = if (accentSubtitle) AccentRed else TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Экран вкладок: сетка карточек 2×N, как в мокапе. */
@Composable
fun TabSwitcherPanel(
    manager: TabManager,
    onSelect: (Tab) -> Unit,
    onCloseTab: (Tab) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Вкладки · ${manager.tabs.size}",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            BarIcon(BIcons.Trash, "Закрыть все", size = 20.dp, tint = TextMuted) { manager.closeAll() }
            BarIcon(BIcons.Close, "Закрыть", size = 20.dp, tint = TextMuted, onClick = onDismiss)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(manager.tabs.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { tab ->
                        TabCard(
                            tab = tab,
                            isActive = tab.id == manager.activeId,
                            onSelect = { onSelect(tab) },
                            onClose = { onCloseTab(tab) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionCard("Новая вкладка", BIcons.Plus, Modifier.weight(1f)) { onNewTab(false) }
                    ActionCard("Приватная", BIcons.PrivateEye, Modifier.weight(1f)) { onNewTab(true) }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: Tab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActive) SurfaceRaised else SurfaceDeep)
            .clickable(onClick = onSelect)
            .padding(12.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FaviconOrMonogram(url = tab.url.ifBlank { "https://ya.ru" }, size = 22.dp)
                Spacer(Modifier.width(8.dp))
                if (tab.isPrivate) {
                    Icon(BIcons.PrivateEye, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Приватно", color = TextMuted, fontSize = 10.sp)
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentRed),
                    )
                }
                Spacer(Modifier.weight(1f))
                BarIcon(BIcons.Close, "Закрыть вкладку", size = 14.dp, tint = TextMuted, onClick = onClose)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = tab.title.ifBlank { if (Urls.isAbout(tab.url) || tab.url.isBlank()) "Новая вкладка" else Urls.prettyHost(tab.url) },
                color = TextPrimary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = Urls.prettyHost(tab.url).ifBlank { "поиск" },
                color = TextDim,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDeep)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = AccentRed, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

/** Пункт меню браузера. */
data class MenuEntry(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/** Меню — плоская «шторка» снизу, без градиентов. */
@Composable
fun MenuPanel(entries: List<MenuEntry>, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(SurfaceDeep)
                // пустой обработчик — «съедаем» тап, чтобы шторка не закрывалась
                .clickable { }
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HairLine),
            )
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { entry.onClick() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(entry.icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(entry.label, color = TextPrimary, fontSize = 15.sp)
                }
            }
        }
    }
}

/** Полоска поиска по странице. */
@Composable
fun FindBar(
    query: String,
    resultText: String,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDeep)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BIcons.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Найти на странице", color = TextDim, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(AccentRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (resultText.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(resultText, color = TextMuted, fontSize = 12.sp)
        }
        BarIcon(BIcons.Back, "Предыдущее", size = 18.dp, tint = TextMuted, onClick = onPrev)
        BarIcon(BIcons.Forward, "Следующее", size = 18.dp, tint = TextMuted, onClick = onNext)
        BarIcon(BIcons.Close, "Закрыть поиск", size = 18.dp, tint = TextMuted, onClick = onClose)
    }
}

/** Простой тост-бар (без Snackbar: он тянет лишние зависимости). */
@Composable
fun ToastBar(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceRaised)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(BIcons.Check, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = TextPrimary, fontSize = 13.sp)
        }
    }
}

/** Заголовок секции в списках. */
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .padding(start = 6.dp, top = 14.dp, bottom = 6.dp),
    )
}

/**
 * Карточка ошибки загрузки: показывается поверх GeckoView, когда движок
 * не смог открыть страницу (нет сети, нет DNS, SSL, падение процесса).
 */
@Composable
fun ErrorOverlay(
    title: String,
    message: String,
    url: String,
    onRetry: () -> Unit,
    onOpenHome: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = BIcons.Shield,
                contentDescription = null,
                tint = AccentRed,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Urls.prettyHost(url).ifBlank { url },
                color = TextDim,
                fontSize = 12.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlatButton("Обновить", primary = true, onClick = onRetry)
                FlatButton("На главную", primary = false, onClick = onOpenHome)
            }
        }
    }
}

@Composable
private fun FlatButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) AccentRed else SurfaceRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Поисковые системы для экрана настроек. */
val searchEngines: List<SearchEngine> = SearchEngine.entries
