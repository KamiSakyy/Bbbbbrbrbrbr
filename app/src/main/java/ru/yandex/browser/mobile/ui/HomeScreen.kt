package ru.yandex.browser.mobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.yandex.browser.mobile.core.Settings
import ru.yandex.browser.mobile.core.SiteLink
import ru.yandex.browser.mobile.core.Urls
import ru.yandex.browser.mobile.ui.icons.BIcons
import ru.yandex.browser.mobile.ui.theme.AccentRed
import ru.yandex.browser.mobile.ui.theme.PureBlack
import ru.yandex.browser.mobile.ui.theme.SurfaceDeep
import ru.yandex.browser.mobile.ui.theme.TextMuted
import ru.yandex.browser.mobile.ui.theme.TextPrimary
import ru.yandex.browser.mobile.web.rememberFavicon

/**
 * Главный экран: сетка быстрого доступа 4×2, статистика экономии трафика
 * и частые сайты из истории. Флэт, чистый чёрный, крупные тап-зоны.
 */
@Composable
fun HomeScreen(
    editMode: Boolean,
    onOpen: (String) -> Unit,
    onRemoveTile: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onOpenBookmarks: () -> Unit,
) {
    val tiles = Settings.speedDial.take(8)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 8.dp,
            bottom = 18.dp,
        ),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            TrafficStatsRow()
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Быстрый доступ",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = BIcons.Edit,
                        contentDescription = "Изменить",
                        tint = if (editMode) AccentRed else TextMuted,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        items(tiles.chunked(4)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { link ->
                    SpeedDialTile(
                        link = link,
                        editMode = editMode,
                        onOpen = { onOpen(link.url) },
                        onRemove = { onRemoveTile(link.url) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDeep)
                    .clickable { onOpen("https://yandex.ru/alice/chat") }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(26.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Яндекс Собеседник", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Поиск с ИИ и быстрые ответы", color = TextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(BIcons.Forward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(20.dp))
        }

        val frequent = Settings.history.take(6)
        if (frequent.isNotEmpty()) {
            item {
                Text(
                    text = "Часто посещаемые",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
            }
            items(frequent) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpen(item.url) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FaviconOrMonogram(url = item.url, size = 30.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.title.ifBlank { Urls.prettyHost(item.url) },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = Urls.prettyHost(item.url),
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(BIcons.Forward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun SpeedDialTile(
    link: SiteLink,
    editMode: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDeep)
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            FaviconOrMonogram(url = link.url, size = 30.dp)
            if (editMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = BIcons.Close,
                        contentDescription = "Удалить",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = link.title,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Иконка сайта: фавиконка из кэша, иначе — монограмма первой буквы. */
@Composable
fun FaviconOrMonogram(url: String, size: androidx.compose.ui.unit.Dp, tint: Color = TextPrimary) {
    val bitmap = rememberFavicon(url)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4))
                .background(Color(0xFF232427)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = Urls.monogram(url),
                color = tint,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
