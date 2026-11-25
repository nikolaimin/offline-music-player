package com.offlineplayer.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont

/** Высота строки: обложка 56 dp и вертикальные отступы по 9 dp. */
val QueueItemHeight = 74.dp

/**
 * Строка очереди на экране проигрывателя: номер, обложка, название и исполнитель.
 * Играющий трек выделен стеклянной подсветкой и эквалайзером вместо номера.
 */
@Composable
fun QueueItemCard(
    trackId: Long,
    index: Int,
    title: String,
    artist: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "queueScale"
    )
    val active by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "queueActive"
    )

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
            .height(QueueItemHeight)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Номер в очереди; у играющего трека вместо него эквалайзер.
        Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                EqualizerBars(isPlaying = isPlaying, color = Accent)
            } else {
                Text(
                    text = "${index + 1}",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        TrackCover(trackId = trackId, size = 56.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.92f),
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = artist,
                color = if (isCurrent) Accent else SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
