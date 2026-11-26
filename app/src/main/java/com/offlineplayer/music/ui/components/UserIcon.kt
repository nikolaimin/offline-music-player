package com.offlineplayer.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Иконка пользователя (голова и плечи). Рисуется сама, без набора иконок. */
@Composable
fun UserIcon(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round)

        drawCircle(
            color = color,
            radius = w * 0.2f,
            center = Offset(w / 2f, w * 0.3f),
            style = stroke
        )
        // Плечи: верхняя половина широкого овала.
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.14f, w * 0.6f),
            size = Size(w * 0.72f, w * 0.7f),
            style = stroke
        )
    }
}

/** Значок меню: три горизонтальные полоски. */
@Composable
fun MenuIcon(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.095f
        listOf(0.27f, 0.5f, 0.73f).forEach { fraction ->
            drawLine(
                color = color,
                start = Offset(w * 0.16f, w * fraction),
                end = Offset(w * 0.84f, w * fraction),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
