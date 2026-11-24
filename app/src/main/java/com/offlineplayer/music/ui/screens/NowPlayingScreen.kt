package com.offlineplayer.music.ui.screens

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.offlineplayer.music.playback.PlayerState
import com.offlineplayer.music.playback.QueueState
import com.offlineplayer.music.ui.components.Artwork
import com.offlineplayer.music.ui.components.LibraryItem
import com.offlineplayer.music.ui.components.appBackground
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.Background
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont
import com.offlineplayer.music.ui.util.formatDuration
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    state: PlayerState,
    queue: QueueState,
    getPositionMs: () -> Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onQueueItemClick: (Int) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var showQueue by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    // Позиция обновляется несколько раз в секунду, пока открыт экран.
    LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) position = getPositionMs()
            delay(250)
        }
    }

    val duration = state.durationMs
    val maxValue = if (duration > 0) duration.toFloat() else 1f
    val shownValue = (if (dragging) dragValue else position.toFloat()).coerceIn(0f, maxValue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .appBackground()
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Верхняя строка: закрыть, заголовок, очередь
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(text = "⌄", size = 44.dp, textSize = 26, onClick = onClose)
            Text(
                text = "СЕЙЧАС ИГРАЕТ",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            RoundButton(
                text = "☰",
                size = 44.dp,
                textSize = 20,
                tint = if (showQueue) Accent else Color.White,
                onClick = { showQueue = !showQueue }
            )
        }

        // Центр: обложка или очередь
        if (showQueue) {
            val listState = rememberLazyListState()

            LaunchedEffect(queue.currentIndex) {
                if (queue.currentIndex >= 0) listState.scrollToItem(queue.currentIndex)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(queue.items) { index, item ->
                    LibraryItem(
                        title = item.title,
                        subtitle = item.artist,
                        isCurrent = index == queue.currentIndex,
                        onClick = { onQueueItemClick(index) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Artwork(
                    trackId = state.mediaId?.toLongOrNull(),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    color = Color.White,
                    fontFamily = TrackTitleFont,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(state.artist, state.album).filter { it.isNotBlank() }.joinToString(" • "),
                    color = SecondaryText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Добавить текущий трек в избранное или убрать из него.
            RoundButton(
                text = "♥",
                size = 48.dp,
                textSize = 26,
                tint = if (isFavorite) Accent else SecondaryText,
                onClick = onToggleFavorite
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Полоса перемотки и время
        Slider(
            value = shownValue,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                val target = dragValue.toLong()
                onSeek(target)
                position = target
                dragging = false
            },
            valueRange = 0f..maxValue,
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = CardColor,
                disabledThumbColor = SecondaryText,
                disabledActiveTrackColor = SecondaryText,
                disabledInactiveTrackColor = CardColor
            )
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(formatDuration(shownValue.toLong()), color = SecondaryText, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(formatDuration(duration), color = SecondaryText, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Основные кнопки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(
                text = "⇄",
                size = 48.dp,
                textSize = 22,
                tint = if (state.shuffle) Accent else SecondaryText,
                onClick = onToggleShuffle
            )
            RoundButton(text = "⏮", size = 52.dp, textSize = 24, onClick = onPrevious)

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.isPlaying) "❚❚" else "▶",
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            RoundButton(text = "⏭", size = 52.dp, textSize = 24, onClick = onNext)
            RoundButton(
                text = if (state.repeatMode == Player.REPEAT_MODE_ONE) "↻1" else "↻",
                size = 48.dp,
                textSize = 22,
                tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) SecondaryText else Accent,
                onClick = onCycleRepeat
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RoundButton(
    text: String,
    size: Dp,
    textSize: Int,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = tint, fontSize = textSize.sp)
    }
}
