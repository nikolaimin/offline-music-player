package com.offlineplayer.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.CardColor
import kotlin.math.max
import kotlinx.coroutines.delay

private val TouchWidth = 20.dp
private val TrackWidth = 4.dp
private val ThumbWidth = 6.dp
private val MinThumbHeight = 48.dp

/** Через сколько миллисекунд после остановки прокрутки ползунок начинает гаснуть. */
private const val HideDelayMs = 1000L

/**
 * Ползунок прокрутки для [LazyListState]: показывает положение в списке
 * и позволяет быстро перемотать список, потянув за него пальцем.
 * Размещается справа от списка (например, в Box через Modifier.align).
 *
 * Положение и перемотка считаются в пикселях по оценке полной высоты списка
 * (типичная высота строки × число строк), а не по номерам треков, поэтому список
 * скользит плавно, даже если строки разной высоты.
 */
@Composable
fun ListScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }

    // Типичная высота строки с промежутком. Берётся медиана видимых строк, чтобы одна
    // высокая строка (плитки папок, заголовок) не сбивала оценку.
    val itemPx by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val sizes = info.visibleItemsInfo.map { it.size }.sorted()
            if (sizes.isEmpty()) 0f else (sizes[sizes.size / 2] + info.mainAxisItemSpacing).toFloat()
        }
    }

    fun viewportPx(): Float {
        val info = state.layoutInfo
        return (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    }

    fun contentPx(): Float = itemPx * state.layoutInfo.totalItemsCount

    // Какую долю списка занимает видимая часть (0..1). Единица = всё помещается.
    val visibleFraction by remember {
        derivedStateOf {
            val content = contentPx()
            if (content <= 0f) 1f else (viewportPx() / content).coerceIn(0f, 1f)
        }
    }

    // Насколько список прокручен (0..1). На краях значение точное, в середине — по оценке.
    val scrollProgress by remember {
        derivedStateOf {
            val range = contentPx() - viewportPx()
            when {
                range <= 0f -> 0f
                !state.canScrollBackward -> 0f
                !state.canScrollForward -> 1f
                else -> {
                    val current = state.firstVisibleItemIndex * itemPx + state.firstVisibleItemScrollOffset
                    (current / range).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Ползунок виден, пока список листают или тянут за ползунок, и гаснет через
    // HideDelayMs после остановки.
    var visible by remember { mutableStateOf(false) }
    val active = dragging || state.isScrollInProgress
    LaunchedEffect(active) {
        if (active) {
            visible = true
        } else {
            delay(HideDelayMs)
            visible = false
        }
    }
    val alpha by animateFloatAsState(
        targetValue = when {
            !visible -> 0f
            dragging -> 1f
            else -> 0.85f
        },
        animationSpec = tween(durationMillis = if (visible) 350 else 700, easing = FastOutSlowInEasing),
        label = "scrollbarAlpha"
    )
    val trackAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 350 else 700, easing = FastOutSlowInEasing),
        label = "scrollbarTrackAlpha"
    )
    val widthAnim by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "scrollbarWidth"
    )

    // Размеры дорожки и ползунка в пикселях: [0] = дорожка, [1] = ползунок.
    // Лежат в remember, чтобы жест и отрисовка видели одни и те же значения.
    val metrics = remember { FloatArray(2) }

    // Если весь список помещается на экране, ползунок не нужен.
    if (visibleFraction >= 1f) return

    /** Во сколько пикселей списка превращается один пиксель хода ползунка. */
    fun listPxPerThumbPx(): Float {
        val free = metrics[0] - metrics[1]
        return if (free <= 0f) 0f else (contentPx() - viewportPx()) / free
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(TouchWidth)
            .pointerInput(state) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        // Нажатие мимо ползунка: список сразу едет так, чтобы ползунок
                        // оказался под пальцем. Нажатие на сам ползунок ничего не сдвигает.
                        val free = metrics[0] - metrics[1]
                        if (free > 0f) {
                            val top = scrollProgress * free
                            if (offset.y < top || offset.y > top + metrics[1]) {
                                state.dispatchRawDelta(
                                    (offset.y - (top + metrics[1] / 2f)) * listPxPerThumbPx()
                                )
                            }
                        }
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // Сдвиг в пикселях напрямую, без привязки к границам строк.
                        state.dispatchRawDelta(dragAmount * listPxPerThumbPx())
                    }
                )
            }
            .drawBehind {
                val thumbHeightPx = max(visibleFraction * size.height, MinThumbHeight.toPx())
                    .coerceAtMost(size.height)
                metrics[0] = size.height
                metrics[1] = thumbHeightPx

                val top = scrollProgress * (size.height - thumbHeightPx)

                val trackW = TrackWidth.toPx()
                val thumbW = TrackWidth.toPx() +
                    (ThumbWidth.toPx() - TrackWidth.toPx()) * widthAnim
                val right = size.width - 3.dp.toPx()

                // Тонкая дорожка
                drawRoundRect(
                    color = CardColor.copy(alpha = trackAlpha),
                    topLeft = Offset(right - trackW, 0f),
                    size = Size(trackW, size.height),
                    cornerRadius = CornerRadius(trackW / 2f)
                )
                // Сам ползунок
                drawRoundRect(
                    color = Accent.copy(alpha = alpha),
                    topLeft = Offset(right - thumbW, top),
                    size = Size(thumbW, thumbHeightPx),
                    cornerRadius = CornerRadius(thumbW / 2f)
                )
            }
    )
}
