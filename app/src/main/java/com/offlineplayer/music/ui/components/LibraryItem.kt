package com.offlineplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.IconTile
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont

/** Лёгкая тень под текстом, чтобы он читался поверх обложки. */
private val TextOverImageShadow = Shadow(
    color = Color.Black.copy(alpha = 0.7f),
    offset = Offset(0f, 1f),
    blurRadius = 6f
)

/**
 * Строка списка. Если передан [trackId], фоном служит обложка трека (или логотип приложения,
 * когда обложки нет), а значок ноты не показывается. Без [trackId] строка выглядит как раньше.
 */
@Composable
fun LibraryItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    isCurrent: Boolean = false,
    trackId: Long? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val hasCover = trackId != null
    val textStyle = if (hasCover) TextStyle(shadow = TextOverImageShadow) else TextStyle.Default

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (trackId != null) {
            // Фон лежит под содержимым и занимает всю строку.
            TrackBackground(trackId = trackId, modifier = Modifier.matchParentSize())
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasCover) Modifier.heightIn(min = 82.dp) else Modifier)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasCover) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(IconTile, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♫", color = Accent, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (isCurrent) Accent else Color.White,
                    fontFamily = TrackTitleFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(trailing, color = SecondaryText, fontSize = 12.sp, style = textStyle)
            }
        }
    }
}
