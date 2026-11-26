package com.offlineplayer.music.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GlassTop = Color(0xFF241638).copy(alpha = 0.78f)
private val GlassBottom = Color(0xFF140C24).copy(alpha = 0.72f)
private val GlassBorder = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
)

/** Стеклянная поверхность в стиле карточек треков: тёмно-фиолетовый градиент и светлая рамка. */
fun Modifier.glassSurface(corner: Dp = 22.dp): Modifier {
    val shape = RoundedCornerShape(corner)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))
        .border(1.dp, GlassBorder, shape)
}
