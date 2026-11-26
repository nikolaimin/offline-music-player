package com.offlineplayer.music.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val NeonPurple = Color(0xFF8B5CF6)
private val NeonCyan = Color(0xFF22D3EE)
private val NeonPink = Color(0xFFEC4899)

/**
 * Аватар пользователя: фото или, если его нет, первая буква имени на градиенте.
 * Вокруг — неоновое кольцо со свечением (если [ring] включён).
 */
@Composable
fun UserAvatar(
    name: String,
    avatarPath: String?,
    avatarVersion: Long,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Boolean = true
) {
    val bitmap by produceState<ImageBitmap?>(null, avatarPath, avatarVersion) {
        value = if (avatarPath == null) {
            null
        } else {
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(avatarPath)?.asImageBitmap() }
        }
    }

    val inset = if (ring) size * 0.09f else size * 0f

    Box(
        modifier = modifier
            .size(size)
            .drawBehind { if (ring) drawNeonRing() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .clip(CircleShape)
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF6D3FD9), Color(0xFFB69CFF)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = (size.value * 0.4f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonRing() {
    val radius = size.minDimension / 2f - size.minDimension * 0.045f
    val center = Offset(size.width / 2f, size.height / 2f)
    val sweep = Brush.sweepGradient(
        listOf(NeonCyan, NeonPurple, NeonPink, NeonPurple, NeonCyan),
        center = center
    )

    // Свечение: два широких полупрозрачных кольца, затем тонкая яркая линия.
    drawCircle(
        color = NeonPurple.copy(alpha = 0.14f),
        radius = radius,
        center = center,
        style = Stroke(width = size.minDimension * 0.085f)
    )
    drawCircle(
        color = NeonPurple.copy(alpha = 0.24f),
        radius = radius,
        center = center,
        style = Stroke(width = size.minDimension * 0.055f)
    )
    drawCircle(
        brush = sweep,
        radius = radius,
        center = center,
        style = Stroke(width = size.minDimension * 0.03f)
    )
}
