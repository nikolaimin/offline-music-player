package com.offlineplayer.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont

// Палитра «Premium Neon Glass»: тёмно-фиолетовое стекло и сиреневые акценты.
private val CardCorner = 22.dp
private val GlassTop = Color(0xFF241638).copy(alpha = 0.78f)
private val GlassBottom = Color(0xFF140C24).copy(alpha = 0.72f)
private val ActiveTop = Color(0xFF3A2266).copy(alpha = 0.86f)
private val ActiveBottom = Color(0xFF1D1136).copy(alpha = 0.80f)
private val GlowColor = Color(0xFF8B5CF6)
private val MenuColor = Color(0xFF1B1230)
private val GlassBorder = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
)
private val ActiveBorder = Brush.linearGradient(
    listOf(Color(0xFFD2C2FF), Color(0xFF8B5CF6))
)

/**
 * Карточка трека: обложка слева, название и исполнитель по центру,
 * длительность и меню «⋮» справа. Играющий трек выделяется свечением и эквалайзером.
 * Все анимации (нажатие, подсветка) рисуются без перекомпоновки списка.
 */
@Composable
fun TrackCard(
    trackId: Long,
    title: String,
    subtitle: String,
    duration: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "cardScale"
    )
    val active by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "cardActive"
    )
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind { drawCardBackground(active) }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCover(trackId = trackId, size = 68.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Эквалайзер плавно появляется у играющего трека и исчезает при переключении.
                AnimatedVisibility(
                    visible = isCurrent,
                    enter = fadeIn(tween(250)) + expandHorizontally(tween(250)),
                    exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EqualizerBars(isPlaying = isPlaying, color = Accent)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = TrackTitleFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))
        Text(text = duration, color = SecondaryText, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(2.dp))

        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                MoreDots(color = SecondaryText)
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MenuColor
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                            color = Color.White
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "♥",
                            color = if (isFavorite) Accent else SecondaryText,
                            fontSize = 18.sp
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleFavorite()
                    }
                )
            }
        }
    }
}

/** Три вертикальные точки (значок меню). Рисуются сами, без набора иконок. */
@Composable
private fun MoreDots(color: Color) {
    Canvas(modifier = Modifier.size(width = 4.dp, height = 18.dp)) {
        val radius = size.width / 2f
        listOf(radius, size.height / 2f, size.height - radius).forEach { centerY ->
            drawCircle(color = color, radius = radius, center = Offset(size.width / 2f, centerY))
        }
    }
}

/** Фон карточки. [active] от 0 до 1 плавно переводит обычное стекло в подсвеченное. */
private fun DrawScope.drawCardBackground(active: Float) {
    val radius = CornerRadius(CardCorner.toPx())

    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(lerp(GlassTop, ActiveTop, active), lerp(GlassBottom, ActiveBottom, active))
        ),
        cornerRadius = radius
    )
    drawRoundRect(
        brush = GlassBorder,
        cornerRadius = radius,
        style = Stroke(width = 1.dp.toPx()),
        alpha = 1f - active
    )

    if (active > 0f) {
        // Мягкое свечение вокруг рамки, затем тонкая светящаяся рамка.
        drawRoundRect(
            color = GlowColor.copy(alpha = 0.10f * active),
            cornerRadius = radius,
            style = Stroke(width = 11.dp.toPx())
        )
        drawRoundRect(
            color = GlowColor.copy(alpha = 0.20f * active),
            cornerRadius = radius,
            style = Stroke(width = 6.dp.toPx())
        )
        drawRoundRect(
            brush = ActiveBorder,
            cornerRadius = radius,
            style = Stroke(width = 1.4.dp.toPx()),
            alpha = active
        )
    }
}
