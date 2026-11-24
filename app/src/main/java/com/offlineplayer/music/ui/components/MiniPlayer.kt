package com.offlineplayer.music.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.R
import com.offlineplayer.music.data.ArtworkLoader
import com.offlineplayer.music.playback.PlayerState
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Маска фона: слева изображение видно, к середине блока плавно исчезает, правая часть чистая. */
private val BackgroundFadeMask = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to Color.Black,
        0.12f to Color.Black.copy(alpha = 0.92f),
        0.30f to Color.Black.copy(alpha = 0.50f),
        0.42f to Color.Black.copy(alpha = 0.15f),
        0.52f to Color.Transparent,
        1.00f to Color.Transparent
    )
)

/** Лёгкая тень под текстом, чтобы он читался и поверх светлой обложки. */
private val TextOverImageShadow = Shadow(
    color = Color.Black.copy(alpha = 0.7f),
    offset = Offset(0f, 1f),
    blurRadius = 6f
)

@Composable
fun MiniPlayer(
    state: PlayerState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape)
            .background(CardColor)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Accent.copy(alpha = 0.55f), Accent.copy(alpha = 0.15f))
                ),
                shape = shape
            )
            .clickable(onClick = onOpen)
    ) {
        // Фон лежит под содержимым и занимает весь блок.
        MiniPlayerBackground(
            trackId = state.mediaId?.toLongOrNull(),
            modifier = Modifier.matchParentSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.title,
                    color = Color.White,
                    fontFamily = TrackTitleFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    style = TextStyle(shadow = TextOverImageShadow),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    state.artist,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    style = TextStyle(shadow = TextOverImageShadow),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Временная диагностика: состояние плеера и текст ошибки.
                Text(
                    text = state.error ?: state.status,
                    color = if (state.error != null) Color(0xFFFF8A80) else SecondaryText,
                    fontSize = 10.sp,
                    style = TextStyle(shadow = TextOverImageShadow),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.isPlaying) "❚❚" else "▶",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Text("⏭", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}

/**
 * Фон мини-плеера: обложка текущего трека или, если обложки нет, фирменная картинка
 * (res/drawable-nodpi/mini_player_fallback.webp). При смене трека плавно перекрашивается,
 * пока грузится новая обложка, остаётся предыдущая.
 */
@Composable
private fun MiniPlayerBackground(trackId: Long?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val cover by produceState<ImageBitmap?>(initialValue = null, trackId) {
        value = if (trackId == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                ArtworkLoader.loadCached(context, trackId)?.asImageBitmap()
            }
        }
    }

    Crossfade(
        targetState = cover,
        modifier = modifier
            // Offscreen нужен, чтобы маска (DstIn) действовала только на этот слой.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(brush = BackgroundFadeMask, blendMode = BlendMode.DstIn)
            },
        animationSpec = tween(durationMillis = 400),
        label = "miniPlayerBackground"
    ) { bitmap ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
                // Небольшое затемнение: текст остаётся читаемым даже на светлой обложке.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.mini_player_fallback),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }
        }
    }
}
