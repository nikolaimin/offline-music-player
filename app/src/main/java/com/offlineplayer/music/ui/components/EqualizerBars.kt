package com.offlineplayer.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Три полоски эквалайзера. Пока трек играет, они двигаются; на паузе замирают.
 * Значения анимации читаются только при рисовании, поэтому перекомпоновки нет,
 * а в списке такая анимация одна, у активного трека.
 */
@Composable
fun EqualizerBars(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val first by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val second by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(680, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val third by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(440, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Canvas(modifier = modifier.size(width = 16.dp, height = 14.dp)) {
        val barWidth = size.width / 5f
        val radius = CornerRadius(barWidth / 2f)
        val levels = if (isPlaying) {
            floatArrayOf(first, second, third)
        } else {
            floatArrayOf(0.35f, 0.65f, 0.45f)
        }

        levels.forEachIndexed { index, level ->
            val barHeight = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(x = index * barWidth * 2f, y = size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        }
    }
}
