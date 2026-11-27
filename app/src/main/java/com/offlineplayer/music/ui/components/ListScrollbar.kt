package com.offlineplayer.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
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
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 */
@Composable
fun ListScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    // Какую долю списка занимает видимая часть (0..1). Единица = всё помещается.
    val visibleFraction by remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.totalItemsCount == 0 || info.visibleItemsInfo.isEmpty()) 1f
            else (info.visibleItemsInfo.size.toFloat() / info.totalItemsCount).coerceIn(0f, 1f)
        }
    }

    // Насколько список прокручен (0..1).
    val scrollProgress by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()
            val range = 1f - visibleFraction
            if (first == null || info.totalItemsCount == 0 || range <= 0f) 0f
            else {
                val index = state.firstVisibleItemIndex +
                    state.firstVisibleItemScrollOffset / max(first.size, 1).toFloat()
                (index / info.totalItemsCount / range).coerceIn(0f, 1f)
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

    // Предыдущая перемотка отменяется новой, чтобы при быстром движении пальца
    // не копилась очередь и список не отставал от ползунка.
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    fun scrollToProgress(progress: Float) {
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        val maxFirst = (info.totalItemsCount - visible.size).coerceAtLeast(0)
        // Позиция дробная: целая часть — номер карточки, дробная — сдвиг внутри неё.
        // Так список едет плавно, а не прыгает сразу на целую карточку.
        val position = (progress * maxFirst).coerceIn(0f, maxFirst.toFloat())
        val index = position.toInt()
        val itemSize = if (visible.isEmpty()) 0 else visible.sumOf { it.size } / visible.size
        val offset = ((position - index) * itemSize).roundToInt()
        scrollJob?.cancel()
        scrollJob = scope.launch { state.scrollToItem(index, offset) }
    }

    fun progressFor(y: Float): Float {
        val free = metrics[0] - metrics[1]
        return if (free <= 0f) 0f else ((y - metrics[1] / 2f) / free).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(TouchWidth)
            .pointerInput(state) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragProgress = progressFor(offset.y)
                        scrollToProgress(dragProgress)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        dragProgress = progressFor(change.position.y)
                        scrollToProgress(dragProgress)
                    }
                )
            }
            .drawBehind {
                val thumbHeightPx = max(visibleFraction * size.height, MinThumbHeight.toPx())
                    .coerceAtMost(size.height)
                metrics[0] = size.height
                metrics[1] = thumbHeightPx

                val progress = if (dragging) dragProgress else scrollProgress
                val top = progress * (size.height - thumbHeightPx)

                val trackW = TrackWidth.toPx()
                val thumbW = TrackWidth.toPx() +
                    (ThumbWidth.toPx() - TrackWidth.toPx()) * widthAnim
                val right = size.width - 2.dp.toPx()

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
